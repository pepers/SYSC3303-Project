
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
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
 * @version	4
 */
public class Server 
{	
	DatagramPacket receivePacket;                       // to receive DatagramPackets from Client
	static DatagramSocket receiveSocket;                // Client sends to port 69
	public static final int MAX_DATA = 512;             // maximum size of data block
	public enum Opcode { RRQ, WRQ, ACK, DATA, ERROR }   // opcodes for different DatagramPackets in TFTP
	
	public Server() 
	{
		// create new socket to receive TFTP packets from Client
		try {
			receiveSocket = new DatagramSocket(69);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}   
	}
	public static void main (String args[]) 
	{
		System.out.println("***** Welcome to Group #2's SYSC3303 TFTP Server Program *****\n");
		Server s = new Server();
		
		// starts user input (for quitting server)
		UserInput ui = new UserInput();		
		ui.start(); 
		
		s.listener();	// start listening for DatagramPackets		
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
	 * Listens for new DatagramPackets on port 69, and verifies them.
	 * 
	 */
	public void listener() 
	{
		while (true) {	// keep listening on port 69 for new requests 
			DatagramPacket datagram = null;				// DatagramPacket to eventually receive
			datagram = receive();						// gets received DatagramPacket
			if (datagram == null) {                     // receive socket was closed, return from listener
				break;
			}
			byte[] request = processDatagram(datagram);	// received request packet turned into byte[]
			if (!isValidPacket(datagram)) {				// check if packet was valid, if not: send error
				byte[] error = createError((byte)4, "Invalid packet.");
				makeConnection(datagram, error);
			} else {
				Opcode op = getOpcode(datagram);				// check type of packet received
			
				// deal with request based on opcode
				switch (op) {
					case RRQ: System.out.println("\nServer: Read request received.");
						makeConnection(datagram);
						break;
					case WRQ: System.out.println("\nServer: Write request received.");
						makeConnection(datagram);
						break;
					case ERROR: System.out.println("\nServer: ERROR received.");
						parseError(request);
						break;
					default: System.out.println("\nServer: " + op + " packet received.");
						byte[] error = createError((byte)5, "Was expecting a RRQ or WRQ.");
						makeConnection(datagram, error);
						break;
				}
			} 
		}		
	}
	
	/**
	 * Starts and sends RRQ/WRQ packet to new ClientConnection thread,
	 * so server can go back to listening for new packets.
	 * 
	 * @param receivePacket	DatagramPacket received by server on port 69
	 */
	public void makeConnection (DatagramPacket receivePacket) 
	{
		// create new thread to communicate with Client and transfer file
		// pass it DatagramPacket that was received	
		Thread clientConnectionThread = new Thread(
				new ClientConnection(receivePacket), "ClientConnection");
		System.out.println("Server: New File Transfer Connection Started ");
		clientConnectionThread.start();	// start new connection thread
	}
	
	/**
	 * Starts and sends an packet to new ClientConnection thread,
	 * along with an error to be sent to the sender of the packet,
	 * so server can go back to listening for new packets.
	 * 
	 * @param receivePacket	DatagramPacket received by server on port 69
	 * @param error			a byte[] to be turned into an ERROR packet
	 */
	public void makeConnection (DatagramPacket receivePacket, byte[] error) 
	{
		System.out.println("Server: New ERROR packet to be created and sent. ");
		// create new thread to communicate with Client and transfer file
		// pass it DatagramPacket that was received	
		Thread clientConnectionThread = new Thread(
				new ClientConnection(receivePacket, error), "ErrorSendingThread");
		clientConnectionThread.start();	// start new connection thread
	}
	
	/**
	 * Receives DatagramPacket.
	 * 
	 * @return DatagramPacket received
	 */
	public DatagramPacket receive() 
	{
		// no packet will be larger than DATA packet
		// room for a possible maximum of 512 bytes of data + 4 bytes opcode and block number
		byte data[] = new byte[MAX_DATA + 4]; 
		DatagramPacket receivePacket = new DatagramPacket(data, data.length);
		
		while (true){
			try {
				// block until a DatagramPacket is received via sendReceiveSocket 
				System.out.println("\nServer: Listening for new requests...");
				receiveSocket.receive(receivePacket);
				
				// print out thread and port info
				System.out.println("\nServer: packet received: ");
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
	 * Gets the opcode from the received packet.
	 * 
	 * @param received	the DatagramPacket that was received
	 * @return			the Opcode of the received DatagramPacket
	 */
	public Opcode getOpcode (DatagramPacket received) 
	{
		// get data stored in received DatagramPacket
		byte[] data = received.getData(); 
		// get second byte of opcode to determine packet type
		byte opcode = data[1];
		// opcode to return
		Opcode op = null;					
		
		// determine if correctly formed opcode
		if (data[0] != 0) {
			return null;
		}
		
		// determine which type of packet was received
		switch (opcode) {
			case 1: op = Opcode.RRQ;	// RRQ
				break;
			case 2: op = Opcode.WRQ;	// WRQ
				break;
			case 3:	op = Opcode.DATA;	// DATA
				break;
			case 4:	op = Opcode.ACK;	// ACK
				break;
			case 5:	op = Opcode.ERROR;	// ERROR
				break;
			default: op = null;			// invalid opcode
				break;
		}
		
		return op;
	}
	
	/**
	 * Determines if the packet is a valid TFTP packet.
	 * 
	 * @param received	the received DatagramPacket to be verified
	 * @return			true if valid packet, false if invalid
	 */
	public boolean isValidPacket (DatagramPacket received) 
	{
		byte[] data = received.getData();	// get packet data
		int len = data.length;				// number of data bytes in packet
		
		// check size of packet
		if (len < 4) {
			return false;
		} 
		
		Opcode op = getOpcode(received);	// get opcode
		if (op == null)
			return false;
		
		// organize by opcode
		switch (op) {
			case RRQ: case WRQ:						// read or write request
				int f;	// filename finding index
				for (f = 2; f < len; f++) {
					if (data[f] == 0) {	// filename is valid
						break;
					}
				}
				if (f == len) {			// didn't find 0 byte after filename
					return false;
				}
				int m;	// mode finding index
				for (m = f + 1; m < len; m++) {
					if (data[m] == 0) {	// mode is valid
						break;
					}
				}
				if (m == len) {			// didn't find 0 byte after mode
					return false;
				}
				
				// byte[] to copy mode into
				byte[] md = new byte[m - f - 1];
				System.arraycopy(data, f + 1, md, 0, m - f - 1);
				
				// make a String out of byte[] for mode
				String mode = null;
				try {
					mode = new String(md, "US-ASCII");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
				// checks if mode is a valid TFTP mode
				if (!(mode.equalsIgnoreCase("netascii") || 
						mode.equalsIgnoreCase("octet") ||
						mode.equalsIgnoreCase("mail"))) {
					return false;
				}
				break;
			case DATA:								// DATA packet
				if (len > MAX_DATA + 4) {
					return false;
				}
				// TODO: Can first byte of block number be anything but 0?
				break;
			case ACK:								// ACK packet
				if (len != 4) { 
					return false; 
				}
				// TODO: Can first byte of block number be anything but 0?
				break;
			case ERROR:								// ERROR packet
				if (data[len - 1] != 0) {
					return false;	// error message not terminated with 0 byte
				}
				for (int i = 0; i<8; i++) {
					if (data[3] == (byte)i) {
						return true;	// found a valid error code
					}
				}
				return false; 			// not a valid error code
			default: return false;					// invalid opcode
		}		
		
		return true;
	}
	
	/**
	 * Parses the error byte[] and determines the type of error received.
	 * 
	 * @param error	the error byte[] that was received in a DatagramPacket
	 */
	public void parseError (byte[] error) 
	{
		// get the error message
		byte[] errorMsg = new byte[error.length - 5];
		System.arraycopy(error, 4, errorMsg , 0, error.length - 5);
		String message = null;
		try {
			message = new String(errorMsg, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}		
				
		// display error code to user
		byte errorCode = error[3];	// get error code
		switch (errorCode) {
			case 0:
				System.out.println("Error Code: 00: Not defined, see error message (if any). ");
				break;
			case 1:
				System.out.println("Error Code: 01: File not found. ");
				break;
			case 2:
				System.out.println("Error Code: 02: Access violation. ");
				break;
			case 3:
				System.out.println("Error Code: 03: Disk full or allocation exceeded. ");
				break;
			case 4:
				System.out.println("Error Code: 04: Illegal TFTP operation. ");
				break;
			case 5:
				System.out.println("Error Code: 05: Unknown transfer ID. ");
				break;
			case 6:
				System.out.println("Error Code: 06: File already exists. ");
				break;
			case 7:
				System.out.println("Error Code: 07: No such user. ");
				break;
		}
		
		// display error message to user
		System.out.println("Error message:" + message);
	}
	
	/**
	 * Creates the byte[] to be sent as an error DatagramPacket.
	 * 
	 * @param errorCode	the code signifying what type of error
	 * @param errorMsg	the message string that will give more detail on the error
	 * @return			the error byte[]
	 */
	public byte[] createError (byte errorCode, String errorMsg) 
	{
		// inform user
		System.out.println("\nServer: Error Code: 0" + errorCode);
		System.out.println("Error Message: " + errorMsg);
		
		// new error to eventually be sent to client
		byte[] error = new byte[4 + errorMsg.length() + 1];	
		
		// add opcode
		error[0] = 0;
		error[1] = 5;
		
		// add error code
		error[2] = 0;
		error[3] = errorCode;
		
		byte[] message = new byte[errorMsg.length()]; // new array for errorMsg
		
		// convert errorMsg to byte[]
		try {
			message = errorMsg.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// add error message to error byte[]
		System.arraycopy(message, 0, error, 4, message.length);
		error[error.length-1] = 0;	// make last element a 0 byte, according to TFTP
				
		return error;
	}
}

/**
 * Deal with user input while Server is listening for requests on port 69.
 *
 */
class UserInput extends Thread 
{	
	private Scanner input;  // scans user input when determining if Server should shut down
	
	public UserInput() 
	{
		System.out.println("Press Q at any time to quit.");		
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
		System.out.println("\nServer: Goodbye!");
		DatagramSocket socket = Server.getSocket(); // get server's receive socket
		socket.close();                             // close server's receive socket
		Thread.currentThread().interrupt();         // close user input thread
	}
}

/**
 * A thread to deal with a specific file transfer request.
 *
 */
class ClientConnection implements Runnable 
{
	
	public static final int MAX_DATA = 512;	//maximum number of bytes in data block
	
	DatagramPacket requestPacket;			// request received on port69 from Client
	DatagramPacket sendPacket;				// DatagramPacket to send in response to the Client
	DatagramPacket receivePacket;			// DatagramPacket received from Client during file transfer
	DatagramSocket sendReceiveSocket;		// new socket connection with Client for file transfer
	private static final int TIMEOUT = 2000;// sendReceiveSocket's timeout when receiving
	private boolean blockNumberWrap = false;// true if block number wraps back to zero
	
	public enum Opcode { RRQ, WRQ, ACK, DATA, ERROR }	// opcodes for different DatagramPackets in TFTP
	
	public static final String fileDirectory = "files\\server\\";	// directory for test files
	Opcode op = null;												// opcode from request DatagramPacket
	String filename;												// filename from request DatagramPacket	
	InetAddress addr;												// InetAddress of client that sent request
	int port;														// port number of client that sent request
	byte[] error = null;

	private BufferedInputStream in;	
	
	/** 
	 * For file transfer.
	 * This method is overloaded.
	 * 
	 * @param requestPacket	RRQ or WRQ received
	 */
	public ClientConnection(DatagramPacket requestPacket) 
	{
		// open new socket to send and receive responses
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
		
		this.requestPacket = requestPacket;			// get request DatagramPacket
		this.op = getOpcode(requestPacket);			// get opcode from request packet
		this.filename = getFilename(requestPacket);	// get filename from request packet
		this.addr = requestPacket.getAddress();		// get the InetAddress of client
		this.port = requestPacket.getPort();		// get the port number of client
	}
	
	/**
	 * For sending an ERROR packet.
	 * This method is overloaded.
	 * 
	 * @param requestPacket	packet received by server, which caused an error 
	 * @param error			to be turned into ERROR packet and sent
	 */
	public ClientConnection(DatagramPacket requestPacket, byte[] error) 
	{
		// open new socket to send and receive responses
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
		
		this.requestPacket = requestPacket;			// get request DatagramPacket
		this.addr = requestPacket.getAddress();		// get the InetAddress of client
		this.port = requestPacket.getPort();		// get the port number of client
		this.error = error;							// ERROR received from server
	}
	
	public void run() 
	{
		if (op == Opcode.RRQ) {			// received a RRQ
			readReq();
		} else if (op == Opcode.WRQ) {	// received a WRQ
			writeReq();
		} else {						// ERROR received from server
			send(error);
		}
		closeConnection(); // close client connection thread
	}
	
	/**
	 * Gets a nicer looking thread name and id combo.
	 * 
	 * @return	thread name/id
	 */
	public String threadName () {
		return Thread.currentThread().getName() + Thread.currentThread().getId();
	}
	
	/**
	 * Deal with RRQ received.
	 * 
	 */
	public void readReq() 
	{
		if (Files.exists(Paths.get(fileDirectory + filename))) {  // file exists
			try {
				in = new BufferedInputStream(new FileInputStream(fileDirectory 
						+ filename));	// reads from file during RRQ
			} catch (FileNotFoundException e) {
				// create and send error response packet for "Access violation."
				byte[] error = createError((byte)2, "File (" + filename + 
						") exists on server, but is not readable.");
				send(error);
				return;	// quit client connection thread
			}		
				
			byte[] read = new byte[MAX_DATA];  // to hold bytes read
			int bytes = 0;                         // number of bytes read
			byte blockNumber = 1;              // DATA block number
			
			// read up to 512 bytes from file starting at offset
			try {			
				while ((bytes = in.read(read)) != -1) {
					System.out.println("\n" + threadName() + 
							": Read " + bytes + " bytes, from " + fileDirectory 
							+ filename);
					
					// get rid of extra buffer
					byte[] temp = new byte[bytes];
					System.arraycopy(read, 0, temp, 0, bytes);
					read = temp;
					
					// create DATA packet of file being read
					byte[] data = createData(blockNumber, read);
					send(data);     // send DATA
					// loop until received ACK is for correct block number
					while (true) {
						boolean timedOut = false;  // have already timed out once
						while (true) {
							try {
								receivePacket = receive(); // receive the DatagramPacket
								break;
							} catch (SocketTimeoutException e1) {
								if (!timedOut) {
									// response timeout, 
									System.out.println("\n" + threadName() + 
											": Socket Timeout: Resending DATA, and continuing to wait for ACK...");
									send(data);       // send DATA
									timedOut = true;  // have timed out once on this packet
								} else {
									// have timed out a second time
									System.out.println("\n" + threadName() + 
											": Socket Timeout Again: Aborting file transfer:");
									System.out.println(threadName() + 
											": There may be a problem with the Client's connection.");
									return;
								}
							}		
						}
					
						// invalid packet received
						if (receivePacket == null) {
							System.out.println("\n" + threadName() + 
									": Invalid packet received: Aborting file transfer:");
							return;
						}
					
						byte[] ackPacket = processDatagram(receivePacket);  // read the expected ACK
						if (ackPacket[1] == 5) {                            // ERROR received instead of ACK
							parseError(ackPacket);	// print ERROR info and close connection
							System.out.println("\n" + threadName() + 
									": Aborting Transfer.");
							return;
						} else if (ackPacket[1] == 4) {
							parseAck(ackPacket);	// print ACK info
							if (ackPacket[3] == 0) { // received block numbers wrapped
								blockNumberWrap = false;
							}
							if (!blockNumberWrap){ // block number hasn't wrapped yet
								if (ackPacket[3] < blockNumber){ // duplicate ACK) {
									System.out.println("\n" + threadName() + 
											": Received Duplicate ACK: Ignoring and waiting for correct ACK...");
								} else if (ackPacket[3] == blockNumber) {
									break;  // got ACK with correct block number, continuing
								} else { // ACK with weird block number 
									// create and send error response packet for "Illegal TFTP operation."
									byte[] error = createError((byte)4, "Received ACK with invalid block number.");
									send(error);
									return;		
								}
							} else {
								if (ackPacket[3] > blockNumber){ // duplicate ACK
									System.out.println("\n" + threadName() + 
											": Received Duplicate ACK: Ignoring and waiting for correct ACK...");
								} else if (ackPacket[3] == blockNumber) {
									break;  // got ACK with correct block number, continuing
								} else { // ACK with weird block number 
									// create and send error response packet for "Illegal TFTP operation."
									byte[] error = createError((byte)4, "Received ACK with invalid block number.");
									send(error);
									return;		
								}
							}
						} else {
							// create and send error response packet for "Illegal TFTP operation."
							byte[] error = createError((byte)4, "Expected ACK as response.");
							send(error);
							return;		
						}
					}
					blockNumber++; // increment DATA block number
					
					// blockNumber goes from 0-127, and then wraps to back to 0
					if (blockNumber < 0) { 
						blockNumber = 0;
						blockNumberWrap = true;
					}
				}
				
				in.close();  // done reading from file
				
			} catch (FileNotFoundException e) {
				// create and send error response packet for "File not found."
				byte[] error = createError((byte)1, "File (" + filename + 
						") does not exist.");
				send(error);
				return; // quit client connection thread
			} catch (IOException e) {
				System.out.println("\nError: could not read from BufferedInputStream.");
				System.exit(1);
			}
			
			/* last packet */
			// check if file was a multiple of 512 bytes in size, send 0 byte DATA
			if (read.length == MAX_DATA) {
				read = new byte[0];                         // create 0 byte read file data
				
				byte[] data = createData(blockNumber, read);  // create DATA packet of file being read
				send(data);                                   // send DATA
				
				// loop until received ACK is for correct block number
				while (true) {
					boolean timedOut = false;  // have already timed out once
					while (true) {
						try {
							receivePacket = receive(); // receive the DatagramPacket
							break;
						} catch (SocketTimeoutException e1) {
							if (!timedOut) {
								// response timeout, 
								System.out.println("\n" + threadName() + 
										": Socket Timeout: Resending DATA, and continuing to wait for ACK...");
								timedOut = true;  // have timed out once on this packet
								send(data); // send DATA
							} else {
								// have timed out a second time
								System.out.println("\n" + threadName() + 
										": Socket Timeout Again: Aborting file transfer:");
								System.out.println(threadName() + 
										": There may be a problem with the Client's connection.");
								return;
							}
						}		
					}
				
					// invalid packet received
					if (receivePacket == null) {
						System.out.println("\n" + threadName() + 
								": Invalid packet received: Aborting file transfer:");
						return;
					}
				
					byte[] ackPacket = processDatagram(receivePacket);  // read the expected ACK
					if (ackPacket[1] == 5) {                            // ERROR received instead of ACK
						parseError(ackPacket);	// print ERROR info and close connection
						System.out.println("\n" + threadName() + 
								": Aborting Transfer.");
						return;
					} else if (ackPacket[1] == 4) {
						parseAck(ackPacket);	// print ACK info
						if (ackPacket[3] == 0) { // received block numbers wrapped
							blockNumberWrap = false;
						}
						if (!blockNumberWrap){ // block number hasn't wrapped yet
							if (ackPacket[3] < blockNumber){ // duplicate ACK) {
								System.out.println("\n" + threadName() + 
										": Received Duplicate ACK: Ignoring and waiting for correct ACK...");
							} else if (ackPacket[3] == blockNumber) {
								break;  // got ACK with correct block number, continuing
							} else { // ACK with weird block number 
								// create and send error response packet for "Illegal TFTP operation."
								byte[] error = createError((byte)4, "Received ACK with invalid block number.");
								send(error);
								return;		
							} 
						} else {
							if (ackPacket[3] > blockNumber){ // duplicate ACK
								System.out.println("\n" + threadName() + 
										": Received Duplicate ACK: Ignoring and waiting for correct ACK...");
							} else if (ackPacket[3] == blockNumber) {
								break;  // got ACK with correct block number, continuing
							} else { // ACK with weird block number 
								// create and send error response packet for "Illegal TFTP operation."
								byte[] error = createError((byte)4, "Received ACK with invalid block number.");
								send(error);
								return;		
							}
						}
					} else {
						// create and send error response packet for "Illegal TFTP operation."
						byte[] error = createError((byte)4, "Expected ACK as response.");
						send(error);
						return;		
					}
				}
			}			
			/* done sending last packet */
			
		/* file doesn't exist */
		} else {
			// create and send error response packet for "File not found."
			byte[] error = createError((byte)1, "File (" + filename + 
					") does not exist.");
			send(error);
			return;
		}
		System.out.println("\n" + threadName() + ": RRQ File Transfer Complete");
	}
	
	/**
	 * Deal with WRQ received.
	 * 
	 */
	public void writeReq() 
	{
		if (Files.exists(Paths.get(fileDirectory + filename))) {	// file exists
			// create and send error response packet for "File already exists."
			byte[] error = createError((byte)6, "File (" + filename + 
					") already exists on server.");
			send(error);
		} else {									// file does not exist
			byte blockNumber = 0;					// block number for ACK and DATA during transfer
			byte[] ack = createAck(blockNumber);	// create initial ACK
			send(ack);								// send initial ACK
			byte[] data = new byte[0];		// to hold received data portion of DATA packet
			byte[] dataPacket = new byte[0];
			
			blockNumber++; // increment DATA block number
			
			// blockNumber goes from 0-127, and then wraps to back to 0
			if (blockNumber < 0) { 
				blockNumber = 0;
				blockNumberWrap = true;
			}
			
			do {	// DATA transfer from client
				
				// dealing with timeout on receiving a packet
				DatagramPacket receivePacket = null;			
				boolean timedOut = false;  // have already timed out once			
				while (true) {
					try {
						receivePacket = receive(); // receive the DatagramPacket
						break;
					} catch (SocketTimeoutException e1) {
						if (!timedOut) {
							// response timeout, re-send last ACK
							System.out.println("\n" + threadName() + 
									": Resending last ACK...");
							send(ack);
							timedOut = true;  // have timed out once on this packet
						} else {
							// have timed out a second time after re-sending the last packet
							System.out.println("\n" + threadName() + 
									": Socket Timeout Again: Aborting file transfer.");
							return;
						}
					}
				}
				
				// invalid packet received
				if (receivePacket == null) {
					return;
				}
				
				dataPacket = processDatagram(receivePacket);	// read the DatagramPacket
				if (dataPacket[1] == 3) {						// received DATA
					if (dataPacket[3] == blockNumber) { // correct block number
						blockNumber++; // increment ACK block number
						
						// blockNumber goes from 0-127, and then wraps to back to 0
						if (blockNumber < 0) { 
							blockNumber = 0;
							blockNumberWrap = true;
						}
						data = parseData(dataPacket);	// get data from packet
						// if last packet was empty, no need to write, end of transfer
						if (data.length == 0) { 
							ack = createAck(dataPacket[3]); // create ACK
							send(ack);                      // send ACK
							break;
						}
						try {
							// gets space left on the drive that we can use
							long spaceOnDrive = Files.getFileStore(
									Paths.get("")).getUsableSpace();	
						
							// checks if there is enough usable space on the disk
							if (spaceOnDrive > data.length + 1024) { // +1024 bytes for safety
								// writes data to file (creates file first, if it doesn't exist yet)
								Files.write(Paths.get(fileDirectory + filename), 
										data, StandardOpenOption.CREATE, 
										StandardOpenOption.APPEND);
								System.out.println("\n" + threadName() + 
										": writing data to file: " + filename);
							} else {
								// create and send error response packet for "Disk full or allocation exceeded."
								byte[] error = createError((byte)3, "File (" + 
										filename + ") too large for disk.");
								send(error);
								return;
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						System.out.println("\n" + threadName() + ": Received DATA packet out of order: Not writing to file.");
						data = new byte[MAX_DATA]; // so we don't quit yet
					}
					ack = createAck(dataPacket[3]);	// create ACK
					send(ack);						// send ACK
				} else if (dataPacket[1] == 5) { // ERROR received instead of DATA
					parseError(dataPacket);			// print ERROR info
				}						
			} while (data.length == MAX_DATA);
			
			// dallying - in case final ACK is not received by Client
			System.out.println("\n" + threadName() + ": Dallying in case final ACK was not received...");
			DatagramPacket receivePacket = null;			
			while (true) {
				try {
					receivePacket = receive(); // receive the DatagramPacket
					
					// invalid packet received
					if (receivePacket == null) {
						System.out.println("\n" + threadName() + ": WRQ File Transfer Complete");
						return;
					}
					dataPacket = processDatagram(receivePacket);	// read the DatagramPacket
					
					if (dataPacket[1] == 3) {        // received DATA					
						ack = createAck(dataPacket[3]);   // create ACK
						send(ack);          // send ACK
					} else if (dataPacket[1] == 5) { // ERROR received instead of DATA
						parseError(dataPacket);         // print ERROR info
						System.out.println("\n" + threadName() + ": WRQ File Transfer Complete");
						return;
					} else {
						// create and send error response packet for "Illegal TFTP operation."
						byte[] error = createError((byte)4, "Was expecting DATA packet.");
						send(error);
						System.out.println("\n" + threadName() + ": WRQ File Transfer Complete");
						return;		
					}
				} catch (SocketTimeoutException e1) {
					System.out.println("\n" + threadName() + ": WRQ File Transfer Complete");
					break;
				}
			}
		}
	}
	
	/**
	 * Shut down this client connection thread.
	 */
	public void closeConnection() 
	{
		System.out.println("\n" + threadName() + 
				": closing connection and shutting down thread.");
		Thread.currentThread().interrupt();	// close ClientConnection thread to stop transfer
	}
	
	/**
	 * Gets the opcode from the received packet.
	 * 
	 * @param received	the DatagramPacket that was received
	 * @return			the Opcode of the received DatagramPacket
	 */
	public Opcode getOpcode (DatagramPacket received) 
	{
		byte[] data = received.getData();	// get data stored in received DatagramPacket
		byte opcode = data[1];				// get second byte of opcode to determine packet type
		Opcode op = null;					// opcode to return
		
		// determine if correctly formed opcode
		if (data[0] != 0) {
			return null;
		}
		
		// determine which type of packet was received
		switch (opcode) {
			case 1: op = Opcode.RRQ;	// RRQ
				break;
			case 2: op = Opcode.WRQ;	// WRQ
				break;
			case 3:	op = Opcode.DATA;	// DATA
				break;
			case 4:	op = Opcode.ACK;	// ACK
				break;
			case 5:	op = Opcode.ERROR;	// ERROR
				break;
			default: op = null;			// invalid opcode
				break;
		}
		
		return op;
	}
	
	/**
	 * Gets the filename from the request packet.
	 * 
	 * @param requestPacket	the DatagramPacket that was received by the server.
	 * @return				the filename from the request packet as a String
	 */
	public String getFilename(DatagramPacket requestPacket) 
	{
		// byte[] to copy packet data into
		byte[] received = new byte[requestPacket.getLength()];	
		System.arraycopy(requestPacket.getData(), 0, received, 0, 
				requestPacket.getLength());
		
		// find the end of filename
		int end = 2;	// end index of filename bytes
		for (int i = 2; i < received.length - 1; i++) {
			if  (received[i] == 0) {
				end = i;
			}
		}
		
		// byte[] to copy filename into
		byte[] file = new byte[end - 2];
		System.arraycopy(received, 2, file, 0, end - 2);
		
		// make a String out of byte[] for filename
		String filename = null;
		try {
			filename = new String(file, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}	
		
		return filename;
	}
	
	/**
	 * Sends DatagramPacket.
	 * 
	 * @param data	data byte[] to be included in DatagramPacket
	 */
	public void send (byte[] data) 
	{
		// create new DatagramPacket to send to client
		sendPacket = new DatagramPacket(data, data.length, addr, port);
		
		// tell user what type of packet is being sent 
		switch (data[1]) {
			case 1: System.out.println("\n" + threadName() + ": Sending RRQ: ");
				break;
			case 2: System.out.println("\n" + threadName() + ": Sending WRQ: ");
				break;
			case 3: System.out.println("\n" + threadName() + ": Sending DATA: ");
				break;
			case 4: System.out.println("\n" + threadName() + ": Sending ACK: ");
				break;
			case 5: System.out.println("\n" + threadName() + ": Sending ERROR: ");
				break;
			default: System.out.println("\n" + threadName() + ": Sending packet: ");
				break;
		}
		
		// print out packet info to user
		System.out.println("To host: " + addr + " : " + port);
		System.out.print("Containing " + sendPacket.getLength() + " bytes: \n");
		System.out.println(Arrays.toString(data) + "\n");
		
		// send the packet
		try {
			sendReceiveSocket.send(sendPacket);
			System.out.println(threadName() + ": Packet sent ");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Receives DatagramPacket, checks that packet came from right place,
	 * and checks if packet is a valid TFTP packet.
	 * 
	 * @return DatagramPacket received
	 * @throws SocketTimeoutException 
	 */
	public DatagramPacket receive() throws SocketTimeoutException 
	{
		// no packet will be larger than DATA packet
		// room for a possible maximum of 512 bytes of data + 4 bytes opcode 
		// and block number
		byte data[] = new byte[MAX_DATA + 4];
		
		DatagramPacket packet = null;  // new DatagramPacket to be received
		
		// loop until packet received from expected host
		while (true) {
			packet = new DatagramPacket(data, data.length);
		
			System.out.println("\n" + threadName() + " : Waiting for packet...");
			
			// block until a DatagramPacket is received via sendReceiveSocket 
			try {
				// set timeout to receive response
				sendReceiveSocket.setSoTimeout(TIMEOUT);
				sendReceiveSocket.receive(packet);
			} catch (IOException e1) {
				throw new SocketTimeoutException();  // timed out
			}
			
			// check for wrong transfer ID 
			if (!((packet.getAddress().equals(addr)) && (packet.getPort() == port))) {
				// create and send error response packet for "Unknown transfer ID."
				byte[] error = createError((byte)5, "Your packet was sent to the wrong place.");
				
				// create new DatagramPacket to send to send error
				sendPacket = new DatagramPacket(error, error.length, 
						packet.getAddress(), packet.getPort());
				
				// print out packet info to user
				System.out.println("\n" + threadName() + ": Sending ERROR: ");
				System.out.println("To host: " + sendPacket.getAddress() + 
						" : " + sendPacket.getPort());
				System.out.print("Containing " + sendPacket.getLength() + 
						" bytes: \n");
				System.out.println(Arrays.toString(error) + "\n");
				
				// send the packet
				try {
					sendReceiveSocket.send(sendPacket);
					System.out.println(threadName() + ": Packet sent ");
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			} else {
				// checks if the received packet is a valid TFTP packet
				if (!isValidPacket(packet)) {
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError((byte)4, "Invalid packet.");
					send(error);
					closeConnection();  // close this ClientConnection thread
				}
				
				// print out thread and port info, from which the packet was sent to Client
				System.out.println("\n" + threadName() + ": packet received: ");
				System.out.println("From host: " + packet.getAddress() + " : " 
						+ packet.getPort());
				System.out.println("Containing " + packet.getLength() + 
						" bytes: ");
				
				break;  // correct transfer ID and valid packet
			}
		}
		
		return packet;
	}
	
	/**
	 * Makes an appropriately sized byte[] from a DatagramPacket
	 * 
	 * @param packet	the received DatagramPacket
	 * @return			the data from the DatagramPacket
	 */
	public byte[] processDatagram (DatagramPacket packet) 
	{
		int len = packet.getLength(); // number of data bytes in packet
		byte[] data = new byte[len];  // new byte[] for storing received data 
		System.arraycopy(packet.getData(), 0, data, 0, len);  // copy over data
		
		// display info to user
		System.out.println(Arrays.toString(data));
		
		return data;
	}
	
	/**
	 * Parse the acknowledgment byte[] and display info to user.
	 * 
	 * @param ack	the acknowledge byte[]
	 */
	public void parseAck (byte[] ack) 
	{
		System.out.println("\n" + threadName() + ": Recieved packet is ACK: ");
		System.out.println("Block#: " + ack[2] + ack[3]);
	}
	
	/**
	 * Parse the data byte[] and display info to user.
	 * 
	 * @param data	the DATA byte[]
	 * @return		just the data portion of a DATA packet byte[]
	 */
	public byte[] parseData (byte[] data) 
	{
		// byte[] for the data portion of DATA packet byte[]
		byte[] justData = new byte[data.length - 4];	
		System.arraycopy(data, 4, justData, 0, data.length-4);
				
		// print info to user
		System.out.println("\n" + threadName() + ": Recieved packet is DATA: ");
		System.out.println("Block#: " + data[2] + data[3] + 
				", and containing data: ");
		System.out.println(Arrays.toString(justData));
				
		return justData;
	}
	
	/**
	 * Checks if an ERROR packet was received, and deals with it.
	 * 
	 * @param data	the data from the received DatagramPacket
	 */
	public void parseError (byte[] error) 
	{
		System.out.println("\n" + threadName() + ": Received packet is ERROR: ");		

		// get the error message
		byte[] errorMsg = new byte[error.length - 5];
		System.arraycopy(error, 4, errorMsg , 0, error.length - 5);
		String message = null;
		try {
			message = new String(errorMsg, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}		
				
		// display error code to user
		byte errorCode = error[3];	// get error code
		switch (errorCode) {
			case 0:
				System.out.println("Error Code: 00: Not defined, see error message (if any). ");
				break;
			case 1:
				System.out.println("Error Code: 01: File not found. ");
				break;
			case 2:
				System.out.println("Error Code: 02: Access violation. ");
				break;
			case 3:
				System.out.println("Error Code: 03: Disk full or allocation exceeded. ");
				break;
			case 4:
				System.out.println("Error Code: 04: Illegal TFTP operation. ");
				break;
			case 5:
				System.out.println("Error Code: 05: Unknown transfer ID. ");
				break;
			case 6:
				System.out.println("Error Code: 06: File already exists. ");
				break;
			case 7:
				System.out.println("Error Code: 07: No such user. ");
				break;
		}
		
		// display error message to user
		System.out.println("Error message: " + message);
		
		closeConnection();	// deal with error and close thread
	}
	
	/**
	 * Creates the byte[] to be sent as an ACK DatagramPacket.
	 * 
	 * @param blockNumber	the data block number that is being acknowledged
	 * @return				the acknowledgment byte[]
	 */
	public byte[] createAck (byte blockNumber) 
	{
		byte[] ack = new byte[4];	// new byte[] to be sent in ACK packet
		
		// add opcode
		ack[0] = 0;
		ack[1] = 4;
				
		// add block number
		ack[2] = 0;
		ack[3] = blockNumber;		
		
		return ack;
	}
	
	/**
	 * Creates the byte[] to be sent as a DATA DatagramPacket.
	 * 
	 * @param blockNumber	the data block number 
	 * @param data			the data to be sent
	 * @return				the data byte[]
	 */
	public byte[] createData (byte blockNumber, byte[] passedData) 
	{
		// new byte[] to be sent in DATA packet
		byte[] data = new byte[4 + passedData.length]; 
		
		// add opcode
		data[0] = 0;
		data[1] = 3;
				
		// add block number
		data[2] = 0;
		data[3] = blockNumber;
		
		// copy data, being passed in, to data byte[]
		System.arraycopy(passedData, 0, data, 4, passedData.length);
		
		return data;
	}
	
	/**
	 * Creates the byte[] to be sent as an error DatagramPacket.
	 * 
	 * @param errorCode	the code signifying what type of error
	 * @param errorMsg	the message string that will give more detail on the error
	 * @return			the error byte[]
	 */
	public byte[] createError (byte errorCode, String errorMsg) 
	{
		// inform user
		System.out.println("\n" + threadName() + ": 0" + errorCode + 
				" Error: informing host: ");
		System.out.println("Error Message: " + errorMsg);
		
		// new error to eventually be sent to client
		byte[] error = new byte[4 + errorMsg.length() + 1];	
		
		// add opcode
		error[0] = 0;
		error[1] = 5;
		
		// add error code
		error[2] = 0;
		error[3] = errorCode;
		
		byte[] message = new byte[errorMsg.length()]; // new array for errorMsg
		
		// convert errorMsg to byte[]
		try {
			message = errorMsg.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// add error message to error byte[]
		System.arraycopy(message, 0, error, 4, message.length);
		error[error.length-1] = 0;	// make last element a 0 byte, according to TFTP
				
		return error;
	}
	
	/**
	 * Determines if the packet is a valid TFTP packet.
	 * 
	 * @param received	the received DatagramPacket to be verified
	 * @return			true if valid packet, false if invalid
	 */
	public boolean isValidPacket (DatagramPacket received) 
	{
		int len = received.getLength(); // number of data bytes in packet
		byte[] data = new byte[len];    // new byte[] for storing received data 
		System.arraycopy(received.getData(), 0, data, 0, len); // copy over data
		
		// check size of packet
		if (len < 4) {
			return false;
		} 
		
		Opcode op = getOpcode(received);	// get opcode
		if(op == null)
			return false;
		
		// organize by opcode
		switch (op) {
			case RRQ: case WRQ:						// read or write request
				return false;
			case DATA:								// DATA packet
				if (len > MAX_DATA + 4) {
					return false;
				}
				// TODO: Can first byte of block number be anything but 0?
				break;
			case ACK:								// ACK packet
				if (len != 4) { 
					return false; 
				}
				// TODO: Can first byte of block number be anything but 0?
				break;
			case ERROR:								// ERROR packet
				if (data[len - 1] != 0) {
					return false;	// error message not terminated with 0 byte
				}
				for (int i = 0; i<8; i++) {
					if (data[3] == (byte)i) {
						return true;	// found a valid error code
					}
				}
				return false; 			// not a valid error code
			default: return false;					// invalid opcode
		}		
		return true;
	}
}
