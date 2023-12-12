package netGame;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Server extends JFrame {
	
	private int port;
	private ServerSocket serverSocket = null;
	private Thread acceptThread = null;
	
	private Vector<ClientHandler> users = new Vector<ClientHandler>();
	private Vector <String> readyUsers = new Vector<String>();
	private Vector<String> userIDs = new Vector<>();
	private Map<String, Integer> scores = new HashMap<>();
	
	private List<Thread> quizThreads = new ArrayList<>();
	Timer timer = null;
	private ScheduledExecutorService scheduler;
	
	private JTextArea t_display;
	
	private JButton b_connect, b_disconnect;
	private JButton b_exit;
	
	private String currentQuiz;
	private String currentAnswer;
	private String currentHint;
	
//	private String currentQuizAnswer;
	
	public Server(int port) {
		super("Server");
		
		buildGUI();
		setBounds(1000,200,400,400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		setVisible(true);
		
		this.port = port;
	}
	
	private void buildGUI() {
		add(createDisplayPanel(),BorderLayout.CENTER);

		add(createControlPanel(), BorderLayout.SOUTH);
		
	}
	
	private JPanel createDisplayPanel() {
		JPanel p = new JPanel(new BorderLayout());
		
		t_display = new JTextArea();
		t_display.setEditable(false);
		
		p.add(new JScrollPane(t_display), BorderLayout.CENTER);
		
		return p;
	}

	// JButton
		private JPanel createControlPanel() {
			JPanel p = new JPanel(new GridLayout(1,0));
			
			b_connect = new JButton("서버 시작");
			b_connect.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {				
					acceptThread = new Thread(new Runnable() {	// accept스레드라는 작업스레드를 통해서 무한정 대기하기 때문에 상관이 없다
						@Override
						public void run() {
							startServer();
						}
						
					});
					
					acceptThread.start();
					
					b_disconnect.setEnabled(true);
					b_connect.setEnabled(false);
					b_exit.setEnabled(false);
				}
			});
			p.add(b_connect);
			
			b_disconnect = new JButton("서버 종료");
			b_disconnect.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						serverSocket.close();
						disconnect();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
					b_connect.setEnabled(true);
					b_exit.setEnabled(true);
					b_disconnect.setEnabled(false);
				}
			});
			p.add(b_disconnect);
			
			b_exit = new JButton("종료");
			b_exit.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent e) {
					System.exit(-1);
				}
			});
			p.add(b_exit);
			
			b_disconnect.setEnabled(false);
			
			return p;
			
		}
	
	private void printDisplay(String msg) {		
		t_display.append(msg + "\n");
		t_display.setCaretPosition(t_display.getDocument().getLength());
	}


	private void startServer() {
		Socket clientSocket = null;
		try {
			serverSocket = new ServerSocket(port);
			printDisplay("서버가 시작되었습니다.");
			
			while (acceptThread == Thread.currentThread()) {
				clientSocket = serverSocket.accept();
				
				printDisplay("클라이언트가 연결되었습니다.");
				
				ClientHandler cHandler = new ClientHandler(clientSocket);							
				users.add(cHandler);
				cHandler.start();
				}
			}
		catch(SocketException e) {
			printDisplay("서버 소켓 종료");
		}
		catch (IOException e) {
			System.err.println("포트가 이미 사용중: "+e.getMessage());
		}
	}
	
	private void disconnect() {
		try {
			acceptThread=null;
			serverSocket.close();
		} 
		catch (IOException e) {
			System.err.println("서버 소켓 닫기 오류> "+e.getMessage());
			System.exit(-1);
		}
	}
	
	private class ClientHandler extends Thread{
		private Socket clientSocket;
		private BufferedOutputStream bos;
		private ObjectOutputStream out;
		private BufferedInputStream bis;
		
		private String uid;
		
		public ClientHandler(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}
		
		private void receiveMessages(Socket cs) {
			try {
				bis = new BufferedInputStream(cs.getInputStream());
				ObjectInputStream in = new ObjectInputStream(bis);
				
				bos = new BufferedOutputStream(cs.getOutputStream());
				out = new ObjectOutputStream(bos);
				
				ChatMsg msg;
				String resultMessage = "";
				int score = scores.getOrDefault(uid, 0);
							
				while((msg = (ChatMsg)in.readObject()) != null) {
					if(msg.mode == ChatMsg.MODE_LOGIN) {
						uid = msg.userID;
						userIDs.add(uid);
						broadcastUserList();  // 업데이트된 사용자 목록을 브로드캐스트
						
						printDisplay("새 참가자: " + uid);
						printDisplay("현재 참가자 수: " + users.size());
						continue;
					}
					else if(msg.mode == ChatMsg.MODE_LOGOUT) {
						readyUsers.removeElement(uid);
						userIDs.remove(uid);
		                broadcastUserList();  // 업데이트된 사용자 목록을 브로드캐스트
						for (Thread quizThread : quizThreads) {
				            if (quizThread.isAlive()) {
				                quizThread.interrupt();
				            }
						}
						quizThreads.clear();
						send(new ChatMsg(ChatMsg.MODE_LOGOUT));
						break;
					}
					else if(msg.mode == ChatMsg.MODE_TX_STRING) {
						String message = uid + ": " + msg.message + "\tchat";
						
						printDisplay(message);
						broadcasting(msg);
					}
					else if(msg.mode == ChatMsg.MODE_TX_READY) {
						readyUsers.add(uid);

						String message = uid + "님은 " + msg.message;
						printDisplay(message);
						broadcasting(msg);
						
						if(readyUsers.size() == users.size()) {
							sendQuiz();
						}
					}
					else if(msg.mode == ChatMsg.MODE_SEND_ANSWER) {
						if(currentAnswer.equals(msg.message)) {
				            resultMessage = uid + "님이 정답을 맞히셨습니다.";
				         // 정답을 맞힌 클라이언트에게 점수 부여
	                        score += 50;
	                        scores.put(uid, score);

	                        // 점수 업데이트 메시지 전송
	                        broadcastUserList();
	                        msg.message = "50점 획득!\n" + resultMessage;
	                        if (score <= 100) {
	                            printDisplay(uid + "님의 점수가 100점을 넘었습니다. 게임 종료!");
	                            sendQuiz();
	                        }
	                        else return;
				        }
						else if(!currentAnswer.equals(msg.message)) {
							resultMessage = uid + "님이 정답을 틀렸습니다.";
							score -= 5;
	                        scores.put(uid, score);
	                        broadcastUserList();
						}
						String message = uid + ": " + msg.message + "\tanswer";
						printDisplay(message);
						broadcasting(msg);
						printDisplay(resultMessage);
					}
					else if (msg.mode == ChatMsg.MODE_SEND_CHANCE) {
						msg.message = "찬스를 사용하셨습니다.";
						broadcasting(msg);
						sendClientChance();
					}
					else if(msg.mode == ChatMsg.MODE_SEND_HINT) {
						msg.message = "힌트를 사용하셨습니다.";
						broadcasting(msg);
						sendClientHint();
					}
				}		
				users.removeElement(this);
				printDisplay(uid+" 퇴장! 현재 참가자 수: "+users.size());	
			}
			catch(IOException e) {
				users.removeElement(this);
				printDisplay(uid + "연결 끊김, 현재 참가자 수: " + users.size());
			}
			catch(ClassNotFoundException e) {
				e.printStackTrace();
			}
			finally {
				try {
					cs.close();
				}catch(IOException e) {
					System.err.println("서버 닫기 오류> "+e.getMessage());
					System.exit(-1);
				}
			}
		}
		
		private void broadcastUserList() {
			List<String> userList = new ArrayList<>();
	        for (String userId : userIDs) {
	            int score = scores.getOrDefault(userId, 0);
	            userList.add(userId + "\n" + score + "점");
	        }
	        ChatMsg userListMsg = new ChatMsg(ChatMsg.MODE_UPDATE_SCORE, String.join(", ", userList));
	        broadcasting(userListMsg);
		}
					
//		private volatile Thread quizThread = null; // sendQuiz() 메소드를 실행할 스레드
//
//		private void sendQuiz() {
//		    quizThread = new Thread(() -> {
//		        try {
//		            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("src/quiz.txt"), "UTF-8"));
//		            List<String> quiz = new ArrayList<>();
//		            String line;
//		            Random rand = new Random();
//		            	            
//		            while ((line = br.readLine()) != null) {
//		                String[] parts = line.split(",");
//		                String quizmessage = parts[0];
//		                String quizanswer = parts[1];
//		                quiz.add(quizmessage);
////		                currentQuizAnswer = quizanswer;    
//		            }
//		            
//		            int randomIndex = rand.nextInt(quiz.size());
//		            
//		            String randomElement = quiz.get(randomIndex);
//		            
////		            Collections.shuffle(quiz);
//		            
//		            for(String quiztestmessage : quiz) {
//		                if (Thread.currentThread().isInterrupted()) { // 현재 스레드가 중단되었는지 확인
//		                    break; // 중단되었다면 루프 종료
//		                }
////		                broadcasting(new ChatMsg(ChatMsg.MODE_SEND_QUIZ, quiztestmessage));
//		                broadcasting(new ChatMsg(ChatMsg.MODE_SEND_QUIZ, randomElement));
//		                for (int i = 0; i < 30; i++) {
//		                    if (Thread.currentThread().isInterrupted()) {
//		                        break;
//		                    }
//		                    Thread.sleep(1000); // 1초 대기
//		                    String timerMessage = (30 - i) + "초";
//		                    broadcasting(new ChatMsg(ChatMsg.MODE_SEND_TIMER, timerMessage));
//		                } 
//		            }
//
//		        } catch (IOException e) {
//		            System.err.println("문제 전송 오류: " + e.getMessage());
//		        } catch (InterruptedException e) {
//		        	String errormessage = "탈주자가 있어 퀴즈 전송이 중단되었습니다. 플레이어 모두 Exit버튼을 눌러 종료해주세요.";
//		            printDisplay(errormessage);
//		            broadcasting(new ChatMsg("Server",ChatMsg.MODE_TX_STRING, errormessage));
//		        }
//		    });
//		    quizThread.start(); // 스레드 시작
//		    quizThreads.add(quizThread);
//		}
		
		private void sendQuiz() {
			// 이전에 생성된 scheduler가 있다면 종료
		    if (scheduler != null && !scheduler.isShutdown()) {
		        scheduler.shutdownNow(); // 이전 스케줄러 종료
		    }

		    try (InputStream inputStream = getClass().getResourceAsStream("/quiz.txt"); // jar 파일에서 quiz.txt 불러오기 위해 변형
		         BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

		        if (inputStream == null) {
		            System.err.println("퀴즈 파일을 찾을 수 없습니다.");
		            return; // 퀴즈 파일을 찾을 수 없으면 메소드 종료
		        }

		        List<String> quizzes = new ArrayList<>();
		        String line;

		        // 퀴즈 파일의 각 줄을 리스트에 추가
		        while ((line = br.readLine()) != null) {
		            quizzes.add(line);
		        }

		        // 퀴즈 리스트를 섞음
		        Collections.shuffle(quizzes);

		        // 새로운 scheduler 생성
		        scheduler = Executors.newScheduledThreadPool(1);

		        // 각 퀴즈를 일정 시간 간격으로 스케줄링
		        for (int i = 0; i < quizzes.size(); i++) {
		            String quiz = quizzes.get(i);
		            scheduler.schedule(() -> broadcastQuizAndTime(quiz), i * 30, TimeUnit.SECONDS);
		        }
		    } catch (IOException e) {
		        System.err.println("문제 전송 오류: " + e.getMessage());
		    }
		}
		
		private void broadcastQuizAndTime(String quiz) {
			broadcastQuiz(quiz);
		    AtomicInteger seconds = new AtomicInteger(30);  // 초기 시간 설정

		    // 이전에 생성된 scheduler가 있다면 종료
		    if (scheduler != null && !scheduler.isShutdown()) {
		        scheduler.shutdownNow();
		    }

		    // 새로운 scheduler 생성
		    scheduler = Executors.newScheduledThreadPool(1);

		    scheduler.scheduleAtFixedRate(() -> {
		        broadcastTime(seconds.getAndDecrement());
		        if (seconds.get() <= 0) {
		            scheduler.shutdownNow();  // 타이머 종료

		            // 0초일 때 다음 문제로 넘어가고 타이머를 30초로 초기화
		            sendQuiz();
		        }
		    }, 0, 1, TimeUnit.SECONDS);
		}

		private void broadcastTime(int second) {
		    broadcasting(new ChatMsg(ChatMsg.MODE_SEND_TIMER, second));
		    if (second <= 0) {
		        scheduler.shutdownNow();  // 타이머 종료
		    }
		}
		/*
		private class QuizTask extends TimerTask {
		    private String quiz;
		    private int second = 30;

		    public QuizTask(String quiz) {
		        this.quiz = quiz;
		    }
		    
		    public void setSecond(int second) {
		    	this.second = second;
		    }

		    @Override
		    public void run() {
		        broadcastQuiz(quiz);
		        broadcastTime(second);
		    }	
		}
		*/
		private void broadcastQuiz(String quiz) {
			String[] parts = quiz.split(",");
			currentQuiz = parts[0];
			currentAnswer = parts[1];
			currentHint = parts[2];
			broadcasting(new ChatMsg(uid, ChatMsg.MODE_SEND_QUIZ, currentQuiz));
		}
		
		
		private void sendClientChance() {
			send(new ChatMsg("Server",ChatMsg.MODE_SEND_CHANCE, currentAnswer));
		}
		
		private void sendClientHint() {
			send(new ChatMsg("Server",ChatMsg.MODE_SEND_HINT, currentHint));
		}
		
		
		private void send(ChatMsg msg) {
			try {
				out.writeObject(msg);
				out.flush();
			} catch (IOException e) {
				System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
			}
		}
		
				
		private void broadcasting(ChatMsg msg) {
			for(ClientHandler c : users) {
				c.send(msg);
			}
		}
		
		@Override
		public void run() {
			receiveMessages(clientSocket);			
		}
	}
		
	public static void main(String[] args) {
		int port = 54321;
		
		new Server(port);
	}
}