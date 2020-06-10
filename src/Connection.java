

import java.net.Socket;

public class Connection {
	private Socket socket;
	private int port;
	private int id;
	
	public Connection(Socket socket, int port, int id) {
		this.socket = socket;
		this.id = id;
		this.port = port;
	}
	
	int getID() {return this.id;}
	int getPort() {return this.port;}
	Socket getSocket() {return this.socket;}
}
