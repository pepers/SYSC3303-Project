

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

/**
 * The error simulation program for the SYSC3303 TFTP Group Project.
 * 
 * @author  Adhiraj Chakraborty
 * @author  Anuj Dalal
 * @author  Hidara Abdallah
 * @author  Matthew Pepers
 * @author  Mohammed Hamza
 * @author  Scott Savage
 * @version 4
 */
public class ErrorSim 
{   
	private static DatagramPacket receivePacket;  // packet received from client
	private static DatagramSocket receiveSocket;  // socket to receive packets from client
	
	private Scanner input;                   // scans user input in ui()
	public static final int MAX_DATA = 512;  // max number of bytes for data field in packet
	
	// choices when entering Error Simulation Mode
	public enum PacketType { RRQ, WRQ, ACK, DATA, ERROR }
	private static PacketType packetType = null;
	public enum PacketDo { lose, delay, duplicate, send, edit}
	private static PacketDo packetDo = null;
	private static boolean choiceIsServer = true; // true if choice is server, false if client
   
	public ErrorSim() 
	{		
		try {
			// create new socket to receive TFTP packets from Client
			receiveSocket = new DatagramSocket(68);
			
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}   
	}
   
	public static void main( String args[] ) 
	{
		System.out.println("***** Welcome to Group #2's SYSC3303 TFTP Error Simulator Program *****\n");
		ErrorSim es = new ErrorSim();
		
		// start the user interface to determine how the user wants to start ErrorSim
		boolean errorSim = es.ui(); 
		
		// starts user input (for quitting error simulator)
		Quit quit = new Quit();		
		quit.start(); 
		
		receivePacket = receive(receiveSocket); // receive packet on port 68, from Client			
		if (receivePacket == null) { return; }  // user pressed q to quit ErrorSim		
		
		Thread ConnectionThread;  // to facilitate transfer of files between client and server
		
		if (errorSim) {
			// start new connection between client and server in normal mode			
			ConnectionThread = new Thread(new Connection(
					receivePacket, packetType, packetDo, choiceIsServer),
					"ErrorSim Connection Thread");
			System.out.println("\nError Simulator: New File Transfer Connection Started, in Error Simulation Mode... ");			
		} else {
			// start new connection between client and server in normal mode			
			ConnectionThread = new Thread(new Connection(
					receivePacket, null, null, false), 
					"Normal Connection Thread");
			System.out.println("\nError Simulator: New File Transfer Connection Started, in Normal Mode... ");		
		}
		
		ConnectionThread.start();	// start new connection thread
	}
	
	/**
	 * Gets receive socket.
	 * 
	 * @return receive socket
	 */
	public static DatagramSocket getSocket() 
	{
		return receiveSocket;
	}
	
	/**
	 * The simple console text user interface for the ErrorSim program.  
	 * User navigates through menus to create and send error packets.
	 * 
	 * @return	false for normal mode, true for error simulation
	 */
	public boolean ui() 
	{		
		// determine if user wants to start in normal mode or error simulation mode
		input = new Scanner(System.in);  // scans user input
		while (true) {
			System.out.println("\nWould you like to start in (N)ormal mode, or (E)rror simulation mode, or (Q)uit?");
			String choice = input.nextLine();  // user's choice
			if (choice.equalsIgnoreCase("N")) {         // normal mode
				System.out.println("\nError Simulator: You have chosen to start in Normal Mode.");
				return false;
			} else if (choice.equalsIgnoreCase("E")) {  // error simulation mode
				while (true) {
					System.out.println("\nError Simulator: How would you like to manipulate packets?");
					System.out.println("\t 1. Lose a packet.");
					System.out.println("\t 2. Delay a packet.");
					System.out.println("\t 3. Duplicate a packet.");
					System.out.println("\t 4. Send a packet.");
					System.out.println("\t 5. Edit a packet.");
					System.out.println("(type the number corresponding to your choice...");
					choice = input.nextLine();  // user's choice
					if (choice.equals("1")) { 
						packetDo = PacketDo.lose;
						break;
					} else if (choice.equals("2")) {
						packetDo = PacketDo.delay;
						break;
					} else if (choice.equals("3")) {
						packetDo = PacketDo.duplicate;
						break;
					} else if (choice.equals("4")) {
						packetDo = PacketDo.send;
						break;
					} else if (choice.equals("5")) {
						packetDo = PacketDo.edit;
						break;
					} else {
						System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
					}
				}
				while (true) {
					System.out.println("\nError Simulator: What type of packet do you want to manipulate?");
					System.out.println("\t 1. RRQ packet.");
					System.out.println("\t 2. WRQ packet.");
					System.out.println("\t 3. DATA packet.");
					System.out.println("\t 4. ACK packet.");
					System.out.println("\t 5. ERROR packet.");
					System.out.println("(type the number corresponding to your choice...");
					choice = input.nextLine();  // user's choice
					if (choice.equals("1")) { 
						packetType = PacketType.RRQ;
						break;
					} else if (choice.equals("2")) {
						packetType = PacketType.WRQ;
						break;
					} else if (choice.equals("3")) {
						packetType = PacketType.DATA;
						break;
					} else if (choice.equals("4")) {
						packetType = PacketType.ACK;
						break;
					} else if (choice.equals("5")) {
						packetType = PacketType.ERROR;
						break;
					} else {
						System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
					}
				}
				while (true) {
					// choose where the packets to manipulated are from and going
					if (packetDo == PacketDo.lose || 
							packetDo == PacketDo.delay || 
							packetDo == PacketDo.duplicate ||
							packetDo == PacketDo.edit) { 
						System.out.println("\nError Simulator: Would you like to " + packetDo.name() + " the first " 
								+ packetType.name() + " packet ");
						System.out.println("\t received from the (C)lient, or from the (S)erver?");
					} else if (packetDo == PacketDo.send) {
						System.out.println("\nError Simulator: Would you like to send a " 
								+ packetType.name() + " packet ");
						System.out.println("\t to the (C)lient, or to the (S)erver?");
					} 
					choice = input.nextLine();  // user's choice
					if (choice.equalsIgnoreCase("C")) {
						choiceIsServer = false; // choice is Client 
						break;
					} else if (choice.equalsIgnoreCase("S")) {
						choiceIsServer = true; // choice is Server
						break;
					} else {
						System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
					}				
				}
				// print choice info to user
				System.out.println("\nError Simulator: You have chosen to start in Error Simulation Mode.");
				String host = null; // client or server
				if (choiceIsServer) {
					host = "Server";
				} else {
					host = "Client";
				}
				if (packetDo == PacketDo.delay || 
						packetDo == PacketDo.duplicate ||
						packetDo == PacketDo.edit) { 
					System.out.println("\t The first " + packetType.name() + 
							" packet, from the " + host + ", will be " +
							packetDo.name() + "ed.");
				} else if (packetDo == PacketDo.lose) {
					System.out.println("\t The first " + packetType.name() + 
							" packet, from the " + host + ", will be lost.");
				} else if (packetDo == PacketDo.send) {
					System.out.println("\t A " + packetType.name() + 
							" packet will be sent to the " + host + ".");
				} 				
				return true; // error simulation mode was chosen
				
			} else if (choice.equalsIgnoreCase("Q")) {  // quit
				System.out.println("\nGoodbye!");
				System.exit(0);
			} else {
				System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
			}
		}
	}
	
	/**
	 * Receives DatagramPacket packets.
	 * 
	 * @param socket			the DatagramSocket to be receiving packets from
	 * @return DatagramPacket 	received
	 */
	public static DatagramPacket receive(DatagramSocket socket) 
	{
		// no packet will be larger than DATA packet
		// room for a possible maximum of 512 bytes of data + 4 bytes opcode 
		// and block number
		byte data[] = new byte[MAX_DATA + 4]; 
		DatagramPacket receivePacket = new DatagramPacket(data, data.length);
		
		while (true){
			try {
				// block until a DatagramPacket is received via sendSocket 
				System.out.println("\nError Simulator: Listening for packets...");
				socket.receive(receivePacket);
				
				// print out thread and port info
				System.out.println("\nError Simulator: packet received: ");
				System.out.println("From host: " + receivePacket.getAddress() + 
						" : " + receivePacket.getPort());
				System.out.print("Containing " + receivePacket.getLength() + 
						" bytes: \n");
				
				break;
			} catch(IOException e) {
				return null;  // socket was closed, return null
			}
		}
		
		return receivePacket;
	}
}



/**
 * Continues the connection with Client and Server
 * in normal or error simulation mode.
 *
 */
class Connection implements Runnable 
{	
	// UDP DatagramPackets and sockets used to send/receive
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket serverSocket, clientSocket;
	
	// choices when entering Error Simulation Mode
	private ErrorSim.PacketType packetType;
	private ErrorSim.PacketDo packetDo;
	private boolean choiceIsServer; // true if choice is server, false if client
	
	// max number of bytes for data field in packet
	public static final int MAX_DATA = 512;  
	
	public Connection (DatagramPacket receivePacket, 
						ErrorSim.PacketType packetType, 
						ErrorSim.PacketDo packetDo, 
						boolean choiceIsServer) 
	{
		try {			
			// create new socket to send/receive TFTP packets to/from Server
			serverSocket = new DatagramSocket();
			
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}   
		
		// the original packet received on ErrorSim's port 68, from client
		this.receivePacket = receivePacket;  
		
		// choices made for error simulation
		this.packetType = packetType;
		this.packetDo = packetDo;
		this.choiceIsServer = choiceIsServer;
	}
	
	public void run () 
	{		
		int clientPort;    // the port from which the Client is sending from
		int serverPort;    // the port from which the Server is sending from
		byte[] received;   // received data from DatagramPacket
			
		received = processDatagram(receivePacket);  // print packet data to user
		clientPort = receivePacket.getPort(); // so we can send response later
		
		try {
			// open new socket to send to Client
			clientSocket = new DatagramSocket(); 
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}   
		
		// passes Client's packet to Server
		send(received, receivePacket.getAddress(), 69, serverSocket);  
		
		while(true) {          
			receivePacket = receive(serverSocket); // receive packet from Server
			received = processDatagram(receivePacket); // print packet data to user	
			serverPort = receivePacket.getPort(); // so we can send response
			
			// passes Server's packet to Client
			send(received, receivePacket.getAddress(), clientPort, clientSocket);  
			
			receivePacket = receive(clientSocket); // receive packet from Client
			received = processDatagram(receivePacket); // print packet data to user
			
			// passes Client's packet to Server			
			send(received, receivePacket.getAddress(), serverPort, serverSocket);  
		} 
	}
	
	/**
	 * Receives DatagramPacket packets.
	 * 
	 * @param socket			the DatagramSocket to be receiving packets from
	 * @return DatagramPacket 	received
	 */
	public DatagramPacket receive(DatagramSocket socket) 
	{
		// no packet will be larger than DATA packet
		// room for a possible maximum of 512 bytes of data + 4 bytes opcode 
		// and block number
		byte data[] = new byte[MAX_DATA + 4]; 
		DatagramPacket receivePacket = new DatagramPacket(data, data.length);
		
		while (true){
			try {
				// block until a DatagramPacket is received via sendSocket 
				System.out.println("\nError Simulator: Listening for packets...");
				socket.receive(receivePacket);
				
				// print out thread and port info
				System.out.println("\nError Simulator: packet received: ");
				System.out.println("From host: " + receivePacket.getAddress() + 
						" : " + receivePacket.getPort());
				System.out.print("Containing " + receivePacket.getLength() + 
						" bytes: \n");
				
				break;
			} catch(IOException e) {
				return null;  // socket was closed, return null
			}
		}
		
		return receivePacket;
	}
	
	
	/**
	 * Makes an appropriately sized byte[] from a DatagramPacket
	 * 
	 * @param packet	the received DatagramPacket
	 * @return			the data from the DatagramPacket
	 */
	public byte[] processDatagram (DatagramPacket packet) 
	{
		byte[] data = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), packet.getOffset(), data, 0, 
				packet.getLength());
		
		// display info to user
		System.out.println(Arrays.toString(data));
		
		return data;
	}
	
	/**
	 * Sends DatagramPackets.
	 * 
	 * @param data		data byte[] to be included in DatagramPacket
	 * @param addr		InetAddress to send packet to
	 * @param port		port to send packet to
	 * @param socket	DatagramSocket to send packets with
	 */
	public void send (byte[] data, InetAddress addr, int port, 
			DatagramSocket socket) 
	{
		// create new DatagramPacket to send to client
		sendPacket = new DatagramPacket(data, data.length, addr, port);
		
		// print out packet info to user
		System.out.println("\nError Simulator: Sending packet: ");
		System.out.println("To host: " + addr + " : " + port);
		System.out.print("Containing " + sendPacket.getLength() + " bytes: \n");
		System.out.println(Arrays.toString(data) + "\n");
		
		// send the packet
		try {
			socket.send(sendPacket);
			System.out.println("Error Simulator: Packet sent using port " + 
					socket.getLocalPort() + ".");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}



/**
 * Deal with user input to quit while ErrorSim is listening for requests on 
 * port 68.
 *
 */
class Quit extends Thread 
{
	// scans user input when determining if ErrorSim should shut down
	private Scanner input;  
	
	public Quit() 
	{
		System.out.println("\nPress 'Q' at any time to quit.");		
	}
	
	public void run() 
	{
		input = new Scanner(System.in);	// scan user input
		while (true) {			
			String choice = input.nextLine();			// user's choice
			if (choice.equalsIgnoreCase("Q")) {			// Quit
				break;
			}
		}
		System.out.println("\nError Simulator: Goodbye!");
		// get ErrorSim's receive socket
		DatagramSocket socket = ErrorSim.getSocket();
		// close ErrorSim's receive socket
		socket.close(); 
		// close user input thread
		Thread.currentThread().interrupt();           
	}
}




