package netGame;

import java.io.Serializable;

public class ChatMsg implements Serializable {
	
	public final static int MODE_LOGIN 			= 0x1;
	public final static int MODE_LOGOUT 		= 0x2;
	public final static int MODE_TX_STRING 		= 0x10;
	public final static int MODE_TX_READY  		= 0x20;
	public final static int MODE_SEND_QUIZ   	= 0x40;
	public final static int MODE_SEND_TIMER		= 0x80;
	public final static int MODE_SEND_ANSWER	= 0x100;
	public final static int MODE_SEND_CHANCE 	= 0x120;
	public final static int MODE_SEND_HINT		= 0x140;
	public final static int MODE_UPDATE_SCORE	= 0x160;
	
	
	String userID;
	int mode;
	String message;
	boolean ready = false;
	int second;
	
	
	public ChatMsg(String userID, int code, String message) {
		this.userID = userID;
		this.mode = code;
		this.message = message;
	}
		
	public ChatMsg(String userID, int code) {
		this.userID = userID;
		this.mode = code;
	}
	
	public ChatMsg(String userID, int code, String message, boolean ready) {
		this.userID = userID;
		this.mode = code;
		this.message = message;
		this.ready = true;
	}
	
	public ChatMsg(int code) {
		this.mode = code;
	}
	
	public ChatMsg(int code, String message) {
		this.mode = code;
		this.message = message;
	}
	
	public ChatMsg(String message) {
		this.message = message;
	}
	
	public ChatMsg(int code, int second) {
		this.mode = code;
		this.second = second;
	}
}

