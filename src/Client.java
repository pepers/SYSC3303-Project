
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Scanner;

/**
 * The client program for the SYSC3303 TFTP Group Project.
 * 
 * @author	Adhiraj Chakraborty
 * @author	Anuj Dalal
 * @author	Hidara Abdallah
 * @author	Matthew Pepers
 * @author	Mohammed Hamza
 * @author	Scott Savage
 * @version	2
 */
public class Client {
	
	DatagramPacket sendPacket;			// datagram packet to send data to server 
	DatagramPacket receivePacket;		// datagram packet to receive data in
	DatagramSocket sendReceiveSocket;	// datagram socket to send and receive packets from
	private static Scanner input;		// scans user input in the simple console ui of main()
	
	/**
	 * opcodes for the different datagram packets in TFTP
	 */
	public enum Opcode {
		RRQ ((byte)1),
		WRQ ((byte)2),
		DATA ((byte)3),
		ACK ((byte)4),
		ERROR ((byte)5);
		
		private final byte op;
		
		Opcode (byte op) {
			this.op = op;
		}
		
		private byte op () { return op; }		
	}
	
	public Client() {
		try {
			// new socket to send requests and receive responses
			sendReceiveSocket = new DatagramSocket();	
		} catch (SocketException se) {   // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Creates a new Client, and starts the user interface.
	 * 
	 * @param args	command line arguments
	 */
	public static void main (String args[]) {
		Client c = new Client();
		c.ui();	// start the user interface
	}
	
	/**
	 * The simple console text user interface for the client program.
	 */
	public void ui() {
		System.out.println("***** Welcome to Group #2's SYSC3303 TFTP Client Program *****\n");
		
		// determine if user wants to send a read request or a write request
		Opcode op;	// the user's choice of request to send
		input = new Scanner(System.in);		// scans user input
		while(true) {
			System.out.println("Would you like to make a (R)ead Request, (W)rite Request, or (Q)uit?");
			String choice = input.nextLine();	// user's choice
			if (choice.equalsIgnoreCase("R")) {			// read request
				op = Opcode.RRQ;
				System.out.println("Client: You have chosen to send a read request.");
				break;
			} else if (choice.equalsIgnoreCase("W")) {	// write request
				op = Opcode.WRQ;
				System.out.println("Client: You have chosen to send a write request.");
				break;
			} else if (choice.equalsIgnoreCase("Q")) {	// quit
				System.out.println("Goodbye!");
				System.exit(0);
			} else {
				System.out.println("I'm sorry, that is not a valid choice.  Please try again...");
			}
		}
		
		// determine which file the user wants to modify		
		String fileName = "test0.txt";	// the file to be sent/received
		String mode = "netascii";		// the mode in which to send/receive the file
		System.out.println("Please choose a file to modify.  Type in a file name: ");
		fileName = input.nextLine();	// user's choice
		
		// deal with user's choice of request
		if (op == Opcode.RRQ) {
			System.out.println("Client: You have chosen the file: " + fileName + ", to be received in " + 
					mode + " mode.\n");			
		} else if (op == Opcode.WRQ) {
			System.out.println("Client: You have chosen the file: " + fileName + ", to be sent in " + 
					mode + " mode.\n");
		}
		byte[] request = createRequest(op.op(), fileName, mode);	// get the request byte[] to send
		   
	}
	
	/**
	 * Creates the request byte[] to be later sent as a datagram packet.
	 * 
	 * @param opcode	differentiates between read (1) and write (2) requests
	 * @param filename	name of file to be sent/requested to/from server
	 * @param mode		mode of file transfer in TFTP
	 * @return			the read/write request byte[]
	 */
	public static byte[] createRequest(byte opcode, String filename, String mode) {
		// TODO return byte[]
		return null;
	}
	
	/**
	 * Creates the byte[] to be sent as an acknowledgment datagram packet.
	 * 
	 * @param blockNumber	the data block number that is being acknowledged
	 * @return				the acknowledgment byte[]
	 */
	public byte[] createAck (byte blockNumber) {
		// TODO return byte[4]
		return null;
	}
	
	/**
	 * Creates the byte[] to be sent as a data datagram packet.
	 * 
	 * @param blockNumber	the data block number 
	 * @param data			the data to be sent
	 * @return				the data byte[]
	 */
	public byte[] createData (byte blockNumber, byte[] data) {
		// TODO return byte[]
		return null;
	}
	
	/**
	 * Creates the byte[] to be sent as an error datagram packet.
	 * 
	 * @param errorCode	the code signifying what type of error
	 * @param errorMsg	the message string that will give more detail on the error
	 * @return			the error byte[]
	 */
	public byte[] createError (byte errorCode, String errorMsg) {
		// TODO return byte[]
		return null;
	}
	
	/**
	 * Parse the request byte[] and display info to user.
	 * 
	 * @param request	the request byte[]
	 */
	public void parseRequest (byte[] request) {
		// TODO 
	}
	
	/**
	 * Parse the acknowledgment byte[] and display info to user.
	 * 
	 * @param ack	the acknowledge byte[]
	 */
	public void parseAck (byte[] ack) {
		// TODO 
	}
	
	/**
	 * Parse the data byte[] and display info to user.
	 * 
	 * @param data	the data byte[]
	 */
	public void parseData (byte[] data) {
		// TODO 
	}
	
	/**
	 * Parse the error byte[] and display info to user.
	 * 
	 * @param error	the error byte[]
	 */
	public void parseError (byte[] error) {
		// TODO 
	}
	
	/**
	 * Sends datagram packet.
	 * 
	 * @param data	data byte[] to be included in datagram packet
	 * @param addr	internet address to send datagram packet to 
	 * @param port	port number to send datagram packet to
	 */
	public void send (byte[] data, InetAddress addr, int port) {
		// TODO
	}
	
	/**
	 * Receives datagram packet, and processes it's data into byte[].
	 * 
	 * @return	byte[] containing data received in datagram packet
	 */
	public byte[] receive () {
		// TODO return byte[]
		return null;
	}

}
