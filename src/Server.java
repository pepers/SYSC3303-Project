
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
	
	Server.Opcode op;						// opcode from request DatagramPacket
	String filename;						// filename from request DatagramPacket
	
	public ClientConnection(DatagramPacket requestPacket) {
		this.requestPacket = requestPacket;			// get request DatagramPacket
		this.op = getOpcode(requestPacket);			// get opcode from request packet
		this.filename = getFilename(requestPacket);	// get filename from request packet
		
		// open new socket to send and receive responses
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	public void run() {
		if (op == Server.Opcode.RRQ) {			// received a RRQ
			if (Files.exists(Paths.get(filename))) {			// file exists
				if (Files.isReadable(Paths.get(filename))) {	// file is readable
					// TODO read from file on server
				} else {										// file is not readable
					// create and send error response packet for "Access violation."
					byte[] error = createError((byte)2, "File (" + filename + ") exists on server, but is not readable.");
					send(error, requestPacket.getAddress(), requestPacket.getPort());
					closeConnection();	// quit client connection thread
				}
			} else {											// file does not exist
				// create and send error response packet for "File not found."
				byte[] error = createError((byte)1, "File (" + filename + ") does not exist.");
				send(error, requestPacket.getAddress(), requestPacket.getPort());
				closeConnection();	// quit client connection thread
			}
		} else if (op == Server.Opcode.WRQ) {	// received a WRQ
			if (Files.exists(Paths.get(filename))) {	// file exists
				// create and send error response packet for "File already exists."
				byte[] error = createError((byte)6, "File (" + filename + ") already exists on server.");
				send(error, requestPacket.getAddress(), requestPacket.getPort());
				closeConnection();	// quit client connection thread
			} else {									// file does not exist
				// TODO write to file on server
			}
		}
	}
	
	/**
	 * Shut down this client connection thread.
	 */
	public void closeConnection() {
		System.out.println("\n" + Thread.currentThread() + ": closing connection and shutting down thread.");
		sendReceiveSocket.close();	// close socket, we are done
		System.exit(0);				// quit thread
	}
	
	/**
	 * Gets the opcode from the request packet.
	 * 
	 * @param requestPacket	the DatagramPacket that was received by the server
	 * @return				the opcode of the request packet
	 */
	public Server.Opcode getOpcode(DatagramPacket requestPacket) {
		// TODO return the requestPacket opcode as a single byte
		return null;
	}
	
	/**
	 * Gets the filename from the request packet.
	 * 
	 * @param requestPacket	the DatagramPacket that was received by the server.
	 * @return				the filename from the request packet as a String
	 */
	public String getFilename(DatagramPacket requestPacket) {
		// TODO return the requestPacket filename as a string
		return null;
	}
	
	/**
	 * Sends DatagramPacket.
	 * 
	 * @param data	data byte[] to be included in DatagramPacket
	 * @param addr	internet address to send DatagramPacket to 
	 * @param port	port number to send DatagramPacket to
	 */
	public void send (byte[] data, InetAddress addr, int port) {
		// TODO
	}
	
	/**
	 * Creates the byte[] to be sent as an error DatagramPacket.
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
	 * Writes the received data to a file.
	 * 
	 * @param filename	name of file to write data to
	 * @param data		data to be written to file			
	 * @throws IOException 
	 */
	public void writeToFile (byte[] data) throws IOException {		
		// gets space left on the drive that we can use
		long spaceOnDrive = Files.getFileStore(Paths.get("")).getUsableSpace();	
		
		// checks if there is enough usable space on the disk
		if (spaceOnDrive < filename.length() + 1024) { // +1024 bytes for safety
			// writes data to file (creates file first, if it doesn't exist yet)
			Files.write(Paths.get(filename), data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			System.out.println("\nClient: reading data to file: " + filename);
		} else {
			// create and send error response packet for "Disk full or allocation exceeded."
			byte[] error = createError((byte)3, "File (" + filename + ") too large for disk.");
			send(error, requestPacket.getAddress(), requestPacket.getPort());
			closeConnection();	// quit client connection thread
		}
	}
}
	
	