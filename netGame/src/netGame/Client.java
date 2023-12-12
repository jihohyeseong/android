package netGame;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;

public class Client extends JFrame {
	
	private JLabel gameStart, time;
	private JTextField userID, hostAddr, portNum, input;
	private JButton startButton, endButton, sendButton, chanceButton, hintButton, readyButton, exitButton;
	private JTextPane chatPane, quizPane, score1, score2, score3, score4;
	private DefaultStyledDocument chatDocument, quizDocument;
	private JFrame f;
	
	private String serverAddress;
	private int serverPort;
	private String uid;
	
	private boolean clickready = false;
	
	private Socket socket;
//	private Writer out;
//	private Reader in;
	
	private ObjectOutputStream out;
	private BufferedOutputStream bos;
	
	private List<String> userIDs = new ArrayList<>();
	
	private Thread receiveThread = null;
	
	private boolean isReady = false;
	
	public Client(String serverAddress, int serverPort) {
		super("Client");
		
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		
		buildGUI();
		
		setSize(500,300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	private void buildGUI() {
		add(createStartDisplay(), BorderLayout.CENTER);
		
		JPanel p = new JPanel(new GridLayout(2,0));
		p.add(createInfoPanel());
		p.add(createControlPanel());
		
		add(p, BorderLayout.SOUTH);
	}
	
	private JPanel createStartDisplay() {
		JPanel p = new JPanel(new BorderLayout());
		gameStart = new JLabel("프로그래밍 퀴즈배틀");
		p.add(gameStart, BorderLayout.CENTER);
		
		return p;
	}
	
	private JPanel createInfoPanel() {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		userID =  new JTextField(7);
		hostAddr = new JTextField(12);
		portNum = new JTextField(5);
		
		hostAddr.setText(this.serverAddress);
		portNum.setText(String.valueOf(this.serverPort));
		portNum.setHorizontalAlignment(JTextField.CENTER);
		
		p.add(new JLabel("참가 이름: "));
		p.add(userID);
		
		p.add(new JLabel("서버주소: "));
		p.add(hostAddr);
		
		p.add(new JLabel("포트번호: "));
		p.add(portNum);
		
		return p;
	}
	
	private JPanel createControlPanel() {
		JPanel p = new JPanel(new GridLayout(1,0));
		
		startButton = new JButton("게임 시작");
		startButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {		
				
				gotoReadyFrame();
				try {
					connectToServer();
					sendUserID();
				} catch (IOException e1) {
					printDisplay("서버와의 연결오류: " + e1.getMessage());
				}
			}
			
		});
		endButton = new JButton("게임 종료");
		endButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(-1);
			}
		});
		
		p.add(startButton);
		p.add(endButton);
		
		return p;
	}
	
	private JPanel createScorePanel() {
		JPanel p = new JPanel(new GridLayout(4,0,0,20));
		score1 = new JTextPane();
		score2 = new JTextPane();
		score3 = new JTextPane();
		score4 = new JTextPane();
		
		score1.setEditable(false);
		score2.setEditable(false);
		score3.setEditable(false);
		score4.setEditable(false);
		
		p.add(score1);
		p.add(score2);
		p.add(score3);
		p.add(score4);
		
		return p;
	}
	
	private JPanel createInputPanel() {
		JPanel p = new JPanel(new BorderLayout());
		
		input = new JTextField();
		input.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(clickready){
					sendAnswerMessage();
				}
				sendMessage();
				
			}
		});
		
		sendButton = new JButton("입력");
		sendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(clickready){
					sendAnswerMessage();
				}
				sendMessage();
				
			}
		});
		
		chanceButton = new JButton("CHANCE");
		chanceButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendChance();
				chanceButton.setEnabled(false);
			}
		});
		
		hintButton = new JButton("HINT");
		hintButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendHint();
				hintButton.setEnabled(false);
			}
		});
		
		readyButton = new JButton("READY");
		readyButton.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
		    	sendReadyMessage();
		    	clickready = true;
		    	readyButton.setEnabled(false);
		    }
		});
		
		JPanel p_button = new JPanel(new GridLayout(1,4,5,0));
		
		p_button.add(sendButton);
		p_button.add(chanceButton);
		p_button.add(hintButton);
		p_button.add(readyButton);
		
		chanceButton.setEnabled(false);
		hintButton.setEnabled(false);
		
		p.add(input, BorderLayout.CENTER);
		p.add(p_button, BorderLayout.EAST);
		
		return p;
	}
	
	private JPanel createChatDisplay() {
		JPanel p = new JPanel(new BorderLayout());
		
		chatDocument = new DefaultStyledDocument();
		chatPane = new JTextPane(chatDocument);
		chatPane.setEditable(false);
		
		p.add(new JScrollPane(chatPane), BorderLayout.CENTER);
		
		return p;
	}
	
	private JPanel createQuizDisplay() {
		JPanel p = new JPanel(new BorderLayout());
		
		quizDocument = new DefaultStyledDocument();
		quizPane = new JTextPane(quizDocument);
		quizPane.setEditable(false);
		
		p.add(quizPane, BorderLayout.CENTER);
		
		return p;
	}
	
	private JPanel createTimePanel() {
		JPanel p = new JPanel(new BorderLayout());
		time = new JLabel();
		
		exitButton = new JButton("EXIT");
		exitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				disconnect();
				gotoStartFrame();
			}
			
		});
		
		p.add(time, BorderLayout.CENTER);
		p.add(exitButton, BorderLayout.EAST);
		
		return p;
	}
	
	private void gotoReadyFrame() {
		this.setVisible(false);
		f = new JFrame("Quiz Battle");
		f.setSize(750, 450);
		
		JPanel p1 = new JPanel(new BorderLayout());
		p1.add(createChatDisplay(), BorderLayout.CENTER);
		p1.add(createInputPanel(), BorderLayout.SOUTH);
		
		JPanel p2 = new JPanel(new BorderLayout());
		p2.add(createQuizDisplay(), BorderLayout.CENTER);
		p2.add(createTimePanel(), BorderLayout.NORTH);
		
		JPanel p3 = new JPanel(new GridLayout(2,0));
		p3.add(p2);
		p3.add(p1);
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createScorePanel(), p3);
		splitPane.setDividerLocation(120);  // 초기 분할 위치 설정
        splitPane.setDividerSize(5);       // 분할선의 크기 설정
        splitPane.setResizeWeight(0.3);  // 좌우 크기 비율 조절 (0.0 ~ 1.0)
        
        f.add(splitPane, BorderLayout.CENTER);
        f.setVisible(true);
	}
	
	private void gotoStartFrame() {
		this.setVisible(true);
		f.dispose();
	}
	
	private void printDisplay(String msg) {	
		Document doc = chatPane.getDocument();
		
		try {
			doc.insertString(doc.getLength(), msg + "\n", null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		 chatPane.setCaretPosition(chatPane.getDocument().getLength());
	}
		
	private void connectToServer() throws UnknownHostException, IOException {
		socket = new Socket(serverAddress, serverPort);
		
		bos = new BufferedOutputStream(socket.getOutputStream());
		out = new ObjectOutputStream(bos);
			
			receiveThread = new Thread(new Runnable() {
				private ObjectInputStream in;
				private BufferedInputStream bis;
				
				private void receiveMessage() {
					try {
						ChatMsg inMsg = (ChatMsg)in.readObject();
											
						if(inMsg == null) {
							disconnect();
							printDisplay("서버 연결 끊김");
							return;
						}
						
						switch(inMsg.mode) {
						case ChatMsg.MODE_TX_STRING:	
							printDisplay(inMsg.userID + ": " + inMsg.message);
							break;
						case ChatMsg.MODE_TX_READY:
							printDisplay(inMsg.userID + ": " + inMsg.message);
							break;
						case ChatMsg.MODE_SEND_QUIZ:
							receiveQuiz(inMsg.message);
							break;
						case ChatMsg.MODE_SEND_TIMER:
							displayTimeMessage(inMsg.second);
							break;
						case ChatMsg.MODE_SEND_ANSWER:
							printDisplay(inMsg.userID + ": " + inMsg.message);
							break;
						case ChatMsg.MODE_SEND_CHANCE:
							printDisplay(inMsg.userID + ": " + inMsg.message);
							break;
						case ChatMsg.MODE_SEND_HINT:
							printDisplay(inMsg.userID + ": " + inMsg.message);
							break;
						case ChatMsg.MODE_UPDATE_SCORE:  // 사용자 목록 업데이트 처리
			                updateScoreDisplay(inMsg.message);
			                break;
						case ChatMsg.MODE_LOGOUT:
							updateScoreDisplay(inMsg.message);
				        }
					} 
					catch (IOException e) {
						printDisplay("연결을 종료했습니다.");
					}
					catch(ClassNotFoundException e) {
						printDisplay("잘못된 객체가 전달되었습니다.");
					}
				}

				@Override
				public void run() {
					
					try {
						bis = new BufferedInputStream(socket.getInputStream());
						in = new ObjectInputStream(bis);
						
					} catch (IOException e) {
						printDisplay("입력 스트림이 열리지 않음");
					}
					
					while(receiveThread == Thread.currentThread()) {
						receiveMessage();
					}
				}
			});

			receiveThread.start();
	}
	
	private void disconnect() {
		send(new ChatMsg(uid, ChatMsg.MODE_LOGOUT));
		
		try {
			receiveThread = null;
			socket.close();
		}
		catch (IOException e) {
			System.err.println("클라이언트 닫기 오류> "+e.getMessage());
			System.exit(-1);
		}
	}
	
	private void send(ChatMsg msg) {
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException e) {
			System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
		}
	}
	
	private void sendMessage() {
		String message = input.getText();
		if(message.isEmpty()) return;
		
		send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, message));
		
		input.setText("");
	}
	
	private void sendUserID() {
		uid = userID.getText();
		send(new ChatMsg(uid, ChatMsg.MODE_LOGIN));
	}
	
	private void updateScoreDisplay(String userList) {
		String[] users = userList.split(", ");
	    score1.setText(users.length > 0 ? users[0] : "");
	    score2.setText(users.length > 1 ? users[1] : "");
	    score3.setText(users.length > 2 ? users[2] : "");
	    score4.setText(users.length > 3 ? users[3] : "");
	}
	
	private void sendReadyMessage() {
	    send(new ChatMsg(uid, ChatMsg.MODE_TX_READY, "준비가 완료되었습니다", true));
	    chanceButton.setEnabled(true);
		hintButton.setEnabled(true);
	}
		
	private void receiveQuiz(String quizMessage) {
		quizPane.setText("");		
		displayQuiz("QUIZ: " + quizMessage);
	}
	
	private void displayQuiz(String quiz) {
	    Document doc = quizDocument;
	    try {
	        doc.insertString(doc.getLength(), quiz + "\n", null);
	    } catch (BadLocationException e) {
	        e.printStackTrace();
	    }
	    quizPane.setCaretPosition(quizPane.getDocument().getLength());
	}
	
	private void displayTimeMessage(int timeMessage) {
	    int remainingTime = timeMessage;
	    time.setText("남은 시간:" + remainingTime);
	}
	
	private void sendAnswerMessage() {
		String message = input.getText();
		if(message.isEmpty()) return;
		send(new ChatMsg(uid, ChatMsg.MODE_SEND_ANSWER, message));
		input.setText("");
	}
	
	private void sendChance() {
		String message = uid + "님이 찬스를 요청했습니다.";
		send(new ChatMsg(uid, ChatMsg.MODE_SEND_CHANCE, message));
	}
	
	private void sendHint() {
		String message = uid + "님이 힌트를 요청했습니다.";
		send(new ChatMsg(uid, ChatMsg.MODE_SEND_HINT, message));
	}
			
	public static void main(String[] args) {
		String serverAddress = "localhost";
		int serverPort = 54321;
		
		new Client(serverAddress, serverPort);
		}
}