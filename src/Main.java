import java.awt.Point;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class Main {
	
	private static ServerSocket server;
	private static DatagramSocket initSock;
	
	private static Point[] p;
	private static Semaphore sem;
	private static LinkedList<Connection> cons;
	
	public static void main(String[] args) throws InterruptedException {
		p = new Point[256];
		System.out.println("Выделена память под 256 точек");
		
		for(int i = 0; i < 256; i++)
			p[i] = new Point();
		System.out.println("Точки инициализированны");
		
		sem = new Semaphore(1);
		System.out.println("Семафор создан");
		
		cons = new LinkedList<Connection>();
		System.out.println("Пустой список соединений создан");
		
		ClientThread otherThread = new ClientThread(sem, p, cons);
		System.out.println("Создан отдельный поток");
		otherThread.start();
		System.out.println("Отдельный поток запущен");
		
		try {
			server = new ServerSocket(60000);
			System.out.println("Создан серверный сокет");
			
			initSock = new DatagramSocket();
			System.out.println("Создан UDP сокет");
			
			while(true) {
				System.out.println("Ожидается подключение");
				Socket sock = server.accept();
				System.out.println("Клинет подключен. IP: " + sock.getInetAddress());
				
				DataInputStream dis = new DataInputStream(sock.getInputStream());
				
				
				int port = dis.readInt();
				int id = dis.readInt();
				System.out.println("\tПорт: " + port);
				System.out.println("\tID: " + id);
				
				System.out.println("Ожидание семафора");
				sem.acquire();
				System.out.println("Семафор получен");
				
				System.out.println("Размер списка соединений до добавления клиента: " + cons.size());
				cons.add(new Connection(sock, port, id));
				System.out.println("Размер списка соединений после добавления: " + cons.size());
				
				DatagramPacket initPack = new DatagramPacket(new byte[12], 12, sock.getInetAddress(), port);
				ByteBuffer data;
				
				for(int i = 0; i < cons.size(); i++) {
					data = ByteBuffer.allocate(12);
					data.putInt(cons.get(i).getID());
					data.putInt(p[cons.get(i).getID()].x);
					data.putInt(p[cons.get(i).getID()].y);
					initPack.setData(data.array());
					System.out.println("Информация о пакете:");
					System.out.println("\tIP: " + initPack.getAddress());
					System.out.println("\tPORT: " + initPack.getPort());
					System.out.println("\tLEN: " + initPack.getLength());
					System.out.println("\tID: " + cons.get(i).getID());
					System.out.println("\tX: " + p[cons.get(i).getID()].x);
					System.out.println("\tY: " + p[cons.get(i).getID()].y);
					initSock.send(initPack);
					
				}
				
				sem.release();
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class ClientThread extends Thread {
	
	private Semaphore sem;
	private Point[] p;
	private LinkedList<Connection> cons;
	private DatagramPacket pack;
	private DatagramSocket sock;
	
	public ClientThread(Semaphore sem, Point[] p, LinkedList<Connection> cons) {
		this.sem = sem;
		this.p = p;
		this.cons = cons;
		try {
			this.sock = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		
		while(true) {
			try {
				System.out.println("Ожидается семафор");
				sem.acquire();
				System.out.println("Семафор получен");
				
				for(int i = 0; i < cons.size(); i++) {
					
					if(cons.get(i).getSocket().getInputStream().available() > 0) {
						System.out.print("Пришла команда от клиента: ");
						
						int command = cons.get(i).getSocket().getInputStream().read();
						System.out.println("Команда прочитана: " + command);
						
						switch(command) {
						case 0:
							break;
						case 1:
							p[cons.get(i).getID()].y -= 10;
							break;
						case 2:
							p[cons.get(i).getID()].y += 10;
							break;
						case 3:
							p[cons.get(i).getID()].x += 10;
							break;
						case 4:
							p[cons.get(i).getID()].x -= 10;
							break;
						}
						System.out.println("Актуальные координаты точки после команды:");
						System.out.println("\tX: " + p[cons.get(i).getID()].x);
						System.out.println("\tY: " + p[cons.get(i).getID()].y);
						
						System.out.println("Формируется пакет для обратной отправки:");
						ByteBuffer data = ByteBuffer.allocate(12);
						data.putInt(cons.get(i).getID());
						data.putInt(p[cons.get(i).getID()].x);
						data.putInt(p[cons.get(i).getID()].y);
						
						pack = new DatagramPacket(data.array(), 12);
						
						
						
						for(Connection c : cons) {
							pack.setAddress(c.getSocket().getInetAddress());
							pack.setPort(c.getPort());
							System.out.println("\tIP: " + pack.getAddress());
							System.out.println("\tPORT: " + pack.getPort());
							System.out.println("\tLEN: " + pack.getLength());
							System.out.println("\tID: " + cons.get(i).getID());
							System.out.println("\tX: " + p[cons.get(i).getID()].x);
							System.out.println("\tY: " + p[cons.get(i).getID()].y);
							sock.send(pack);
						}
					}
				}
				
				sem.release();
			} catch (InterruptedException | IOException e) {
				sem.release();
				e.printStackTrace();
			}
			
		}
		
	}
	
}