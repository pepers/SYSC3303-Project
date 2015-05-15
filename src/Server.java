
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

/**
 * The server program for the SYSC3303 TFTP Group Project.
 * 
 * @author	Adhiraj Chakraborty
 * @author	Anuj Dalal
 * @author	Hidara Abdallah
 * @author	Matthew Pepers
 * @author	Mohammed Hamza
 * @author	Scott Savage
 * @version	2
 */
public class Server {
	
	DatagramPacket receivePacket;	// to receive DatagramPackets from Client
	DatagramSocket receiveSocket;	// Client sends to port 69
	private Scanner input;			// scans user input when determining if Server should shut down
	
	/**
	 * opcodes for the different DatagramPackets packets in TFTP
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
	
	public Server() {
		// create new socket to receive TFTP packets from Client
		try {
			receiveSocket = new DatagramSocket(69);
			receiveSocket.setSoTimeout(5000);		// socket timeout in 5 seconds
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}   
	}
	
	public static void main(String[] args) throws Exception {
		Server s = new Server();		
		s.listener();	// start listening for DatagramPackets
	}
	
	/**
	 * Listens for new DatagramPackets on port 69, and verifies them.
	 * 
	 * @throws Exception	invalid packet received 
	 */
	public void listener() throws Exception {
		while (true) {	// keep listening on port 69 for new requests 
			DatagramPacket datagram = null;	// DatagramPacket to eventually receive
			try {
				datagram = receive();	// gets received DatagramPacket
			} catch (SocketTimeoutException e) {	// haven't received packet in 5 seconds
				serverQuit();			// find out if user wants to quit	
				datagram = receive();	// tries to receive DatagramPackets again
			}	
			byte[] request = process(datagram);		// received request packet turned into byte[]
			Opcode op = parse(request);				// check type and validity of request
			
			// deal with request based on opcode
			if (op == Opcode.RRQ || op == Opcode.WRQ) {	// request was RRQ or WRQ
				makeConnection(datagram);				// set up new connection thread to transfer file
			} else if (op == Opcode.ERROR) {			// ERROR packet was received instead
				byte errorCode = parseError(request);	// determine Error Code
				// deal with ERROR based on Error Code
				if (errorCode == 1) {
					
				}
			} else {									// invalid packet received
				throw new Exception ("Improperly formatted packet received.");
			}
		}		
	}
	
	/**
	 * Starts and sends packet to new ClientConnection thread,
	 * so server can go back to listening for new packets.
	 * 
	 * @param receivePacket	DatagramPacket received by server on port 69
	 */
	public void makeConnection (DatagramPacket receivePacket) {
		// create new thread to communicate with Client and transfer file
		// pass it DatagramPacket that was received				
		Thread clientConnectionThread = new Thread(
				new ClientConnection(receivePacket), "Client Connection Thread");
		System.out.println("\nServer: New File Transfer Connection Started ");
		clientConnectionThread.start();	// start new connection thread
	}
	
	/**
	 * Determines if user wants to quit, and performs actions accordingly.
	 * 
	 * @throws SocketException	when checking the socket timeout
	 */
	public void serverQuit() throws SocketException {
		input = new Scanner(System.in);
		int seconds = receiveSocket.getSoTimeout()/1000;	// seconds until socket timeout
		while (true) {
			System.out.println("\nServer: Have not received new packet in the last " +
					seconds + " seconds: ");
			System.out.println("Would you like to (Q)uit?  Or would you like to (C)ontinue?");
			String choice = input.nextLine();			// user's choice
			if (choice.equalsIgnoreCase("Q")) {			// Quit
				receiveSocket.close();	// close socket listening for requests
				System.exit(0);			// exit server
			} else if (choice.equalsIgnoreCase("C")) {	// Continue
				break;
			} else {									// invalid user choice
				System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
			}
		}
	}
	
	/**
	 * Receives DatagramPacket.
	 * 
	 * @return DatagramPacket received
	 */
	public DatagramPacket receive() {
		// TODO
		return null;
	}
	
	/**
	 * Gets byte[] from DatagramPacket.
	 * 
	 * @param receivePacket	DatagramPacket received
	 * @return				byte[] containing the data from the DatagramPacket
	 */
	public byte[] process (DatagramPacket receivePacket) {
		// TODO return contents of DatagramPacket as byte[]
		return null;
	}
	
	/**
	 * Parses the received byte[], and determines what type of packet it was from.
	 * 
	 * @param received	the byte[] received from a DatagramPacket
	 * @return			the Opcode pertaining to the received byte[]
	 */
	public Opcode parse (byte[] received) {
		// TODO return Opcode for the type of byte[] received
		return null;
	}
	
	/**
	 * Parses the error byte[] and determines the type of error received.
	 * 
	 * @param error	the error byte[] that was received in a DatagramPacket
	 * @return		the byte pertaining to the Error Code of the received packet
	 */
	public byte parseError (byte[] error) {
		// TODO return TFTP Error Code byte
		return (byte)0;
	}
}

/**
 * A thread to deal with a specific file transfer request.
 *
 */
class ClientConnection implements Runnable {
	
	public static final int MAX_DATA = 512;	//maximum number of bytes in data block
	
	DatagramPacket requestPacket;			// request received on port69 from Client
	DatagramPacket sendPacket;				// DatagramPacket to send in response to the Client
	DatagramPacket receivePacket;			// DatagramPacket received from Client during file transfer
	DatagramSocket sendReceiveSocket;		// new socket connection with Client for file transfer
	
	byte op;			// opcode from request DatagramPacket
	String filename;	// filename from request DatagramPacket
	
	public ClientConnection(DatagramPacket requestPacket) {
		this.requestPacket = requestPacket;	// get request DatagramPacket
		this.op = getOpcode(requestPacket);	// get opcode from request packet
		this.filename = getFilename(requestPacket);
	}
	
	public void run() {
		
	}
	
	public byte getOpcode(DatagramPacket requestPacket) {
		// TODO return the requestPacket opcode as a single byte
		return (byte)0;
	}
	
	public String getFilename(DatagramPacket requestPacket) {
		// TODO return the requestPacket filename as a string
		return null;
	}
}
	
	