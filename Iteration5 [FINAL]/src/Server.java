
import java.io.BufferedInputStream;
import java.io.File;
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
import java.nio.file.Path;
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
 * @version	5
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
			DatagramPacket datagram = null; // DatagramPacket to eventually receive
			datagram = receive();             // gets received DatagramPacket
			if (datagram == null) {          // receive socket was closed, return from listener
				break;
			}
			if (!isValidPacket(datagram)) {				// check if packet was valid, if not: send error
				// error packet created and sent within isValidPacket method
			} else {
				byte[] request = processDatagram(datagram);	// received request packet turned into byte[]
				int op = twoBytesToInt(request[0], request[1]);	// check type of packet received
				String type = "";         // packet info for user
				String direction = "<--"; // receiving packets
				
				// deal with request based on opcode
				switch (op) {
					case 1: case 2:
						type = parseRequest(request);
						// print out packet info
						System.out.printf("Server: %30s %3s %-30s   bytes: %3d   - %s \n", 
								receiveSocket.getLocalSocketAddress(), direction, datagram.getSocketAddress(),
								datagram.getLength(), type);
						makeConnection(datagram);
						break;
					case 5: 
						type = parseError(request);
						// print out packet info
						System.out.printf("Server: %30s %3s %-30s   bytes: %3d   - %s \n", 
							receiveSocket.getLocalSocketAddress(), direction, datagram.getSocketAddress(),
							datagram.getLength(), type);
						break;
					default: 
						System.out.println("Server: Unexpected TFTP packet received.");
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
		System.out.println("Server: " + clientConnectionThread.getName() + 
				clientConnectionThread.getId() + " will continue the file transfer.");
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
		// create new thread to communicate with Client and transfer file
		// pass it DatagramPacket that was received	
		Thread clientConnectionThread = new Thread(
				new ClientConnection(receivePacket, error), "ErrorSendingThread");
		System.out.println("Server: " + clientConnectionThread.getName() + 
				clientConnectionThread.getId() + " will send the error packet.");
		clientConnectionThread.start();	// start new connection thread
	}
	
	/**
	 * Parse the read/write request byte[].
	 * 
	 * @param request	the read/write byte[]
	 * @return			String representation of RRQ or WRQ
	 */
	public String parseRequest(byte[] request) 
	{
		String req = "";  // request type
		
		// get opcode
		int op = twoBytesToInt(request[0], request[1]);
		
		// determine if RRQ or WRQ
		if (op == 1) {
			req = "Read Request   filename=\"";
		} else {
			req = "Write Request   filename=\"";
		}	
		
		String filename = getFilename(request);  // gets the filename
		String mode = getMode(request); // gets the mode
		
		return req + filename + "\"   mode=\"" + mode + "\"";
	}
	
	/**
	 * Gets the filename from the request packet.
	 * 
	 * @param received	the received data
	 * @return			the filename from the request packet as a String
	 */
	public String getFilename(byte[] received) 
	{		
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
	 * Gets the mode from the request packet.
	 * 
	 * @param received	the received data
	 * @return			the mode from the request packet as a String
	 */
	public String getMode(byte[] received) 
	{		
		int len = received.length; // length of data
		int f;	// filename finding index
		for (f = 2; f < len; f++) {
			if (received[f] == 0) {	// end of filename
				break;
			}
		}
		int m;	// mode finding index
		for (m = f + 1; m < len; m++) {
			if (received[m] == 0) {	// end of mode
				break;
			}
		}
		
		// byte[] to copy mode into
		byte[] md = new byte[m - f - 1];
		System.arraycopy(received, f + 1, md, 0, m - f - 1);
		
		// make a String out of byte[] for mode
		String mode = null;
		try {
			mode = new String(md, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return mode;
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
		
		return data;
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
			// create and send error response packet for "Illegal TFTP operation."
			byte[] error = createError(4, "Packet is less than 4 bytes in size.");
			makeConnection(received, error);
			return false;
		} 
		
		int op = twoBytesToInt(data[0], data[1]);	// get opcode
		
		// organize by opcode
		switch (op) {
			case 1: case 2:						// read or write request
				int f;	// filename finding index
				for (f = 2; f < len; f++) {
					if (data[f] == 0) {	// filename is valid
						break;
					}
				}
				if (f == len) {			// didn't find 0 byte after filename
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError(4, "Request does not contain null terminated filename.");
					makeConnection(received, error);
					return false;
				}
				int m;	// mode finding index
				for (m = f + 1; m < len; m++) {
					if (data[m] == 0) {	// mode is valid
						break;
					}
				}
				if (m == len) {			// didn't find 0 byte after mode
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError(4, "Request does not contain null terminated mode.");
					makeConnection(received, error);
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
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError(4, "Mode is not a valid TFTP mode choice.");
					makeConnection(received, error);
					return false;
				}
				break;
			case 3:								// DATA packet
				if (len > MAX_DATA + 4) {
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError(4, "Data packet is too large.");
					makeConnection(received, error);
					return false;
				}
				break;
			case 4:								// ACK packet
				if (len != 4) { 
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError(4, "Ack packet is not 4 bytes in size.");
					makeConnection(received, error);
					return false; 
				}
				break;
			case 5:								// ERROR packet
				if (data[len - 1] != 0) {
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError(4, "Error message is not null terminated.");
					makeConnection(received, error);
					return false;	// error message not terminated with 0 byte
				}
				int ec = twoBytesToInt(data[2], data[3]); // get error code
				for (int i = 0; i<8; i++) {
					if (ec == i) {
						return true;	// found a valid error code
					}
				}
				// create and send error response packet for "Illegal TFTP operation."
				byte[] error = createError(4, "Error code is not a valid TFTP error code.");
				makeConnection(received, error);
				return false; 			// not a valid error code
			default: 
				// create and send error response packet for "Illegal TFTP operation."
				error = createError(4, "Invalid TFTP opcode.");
				makeConnection(received, error);
				return false;					// invalid opcode
		}
		
		return true;
	}
	
	/**
	 * Parse the error byte[].
	 * 
	 * @param error	the error byte[]
	 * @return 		String representation of ERROR
	 */
	public String parseError (byte[] error) 
	{
		// get error code
		int errorCode = twoBytesToInt(error[2], error[3]);

		// get the error message
		byte[] errorMsg = new byte[error.length - 5];
		System.arraycopy(error, 4, errorMsg , 0, error.length - 5);
		String message = null;
		try {
			message = new String(errorMsg, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}		
		
		return "Error " + Integer.toString(errorCode) + ": \"" + message + "\"";
	}
	
	/**
	 * Deals with two signed bytes and combines them into one int.
	 * 
	 * @param one	first byte
	 * @param two	second byte
	 * @return		two bytes combined into an int (value: 0 to 65535)
	 */
	public int twoBytesToInt(byte one, byte two) 
	{
		// add 256 if needed to compensate for signed bytes in Java
		int value1 = one < 0 ? one + 256 : one;
		int value2 = two < 0 ? two + 256 : two;
		
		// left shift value1 and combine with value2 into one int
		return (value2 | (value1 << 8));
	}
	
	/**
	 * Creates the byte[] to be sent as an error DatagramPacket.
	 * 
	 * @param errorCode	the code signifying what type of error
	 * @param errorMsg	the message string that will give more detail on the error
	 * @return			the error byte[]
	 */
	public byte[] createError (int errorCode, String errorMsg) 
	{
		byte[] error = new byte[4 + errorMsg.length() + 1];	// new error to eventually be sent to server
		
		// add opcode
		int opcode = 5;
		error[0] = (byte)(opcode / 256);
		error[1] = (byte)(opcode % 256);
		
		// add error code
		error[2] = (byte)(errorCode / 256);
		error[3] = (byte)(errorCode % 256);
		
		byte[] message = new byte[errorMsg.length()];	// new array for errorMsg, to be joined with codes
		
		// convert errorMsg to byte[], with proper encoding
		try {
			message = errorMsg.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// add error message to error byte[]
		System.arraycopy(message, 0, error, 4, message.length);
		error[error.length-1] = 0;	// make last element a 0 byte, according to TFTP
				
		return error; //return full error message with opcodes and type of error
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
	
	public static final String fileDirectory = "files\\server\\";	// directory for test files
	int op;															// opcode from request DatagramPacket
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
		byte[] receivedData = requestPacket.getData(); // get data from request packet
		this.op = twoBytesToInt(receivedData[0], receivedData[1]); // get opcode from request packet
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
		System.out.println("\n"); // for formatting
		if (op == 1) {			// received a RRQ
			readReq();
		} else if (op == 2) {	// received a WRQ
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
		Path p = Paths.get(fileDirectory + filename); // get path to file
		File f = new File(p.toString()); // turn path to string
		if (f.exists()) {  // file exists
			try {
				in = new BufferedInputStream(new FileInputStream(fileDirectory 
						+ filename));	// reads from file during RRQ
			} catch (FileNotFoundException e) {
				// create and send error response packet for "Access violation."
				byte[] error = createError(2, "File (" + filename + 
						") exists on server, but is not readable.");
				send(error);
				return;	// quit client connection thread
			}		
				
			byte[] read = new byte[MAX_DATA];  // to hold bytes read
			int bytes = 0;                      // number of bytes read
			int blockNumber = 1;               // DATA block number
			
			// read up to 512 bytes from file starting at offset
			try {			
				while ((bytes = in.read(read)) != -1) {
					System.out.println(threadName() + ": Read " + bytes + 
							" bytes, from " + fileDirectory + filename);
					
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
									System.out.println(threadName() + 
											": Socket Timeout: Resending DATA, and continuing to wait for ACK...");
									send(data);       // send DATA
									timedOut = true;  // have timed out once on this packet
								} else {
									// have timed out a second time
									System.out.println(threadName() + 
											": Socket Timeout Again: Aborting file transfer:");
									System.out.println(threadName() + 
											": There may be a problem with the Client's connection.");
									try {
										in.close(); // close buffered reader after RRQ
									} catch (IOException e) { } 
									return;
								}
							}		
						}
					
						// invalid packet received
						if (receivePacket == null) {
							System.out.println(threadName() + 
									": Invalid packet received: Aborting file transfer:");
							try {
								in.close(); // close buffered reader after RRQ
							} catch (IOException e) { } 
							return;
						}
					
						byte[] ackPacket = processDatagram(receivePacket);  // read the expected ACK
						int op = twoBytesToInt(ackPacket[0], ackPacket[1]); // get opcode
						
						if (op == 5) {                            // ERROR received instead of ACK
							parseError(ackPacket);	// print ERROR info and close connection
							System.out.println(threadName() + 
									": Aborting Transfer.");
							try {
								in.close(); // close buffered reader after RRQ
							} catch (IOException e) { } 
							return;
						} else if (op == 4) {
							parseAck(ackPacket);	// print ACK info
							int bn = twoBytesToInt(ackPacket[2], ackPacket[3]); // get block number
							if (bn == 0) { // received block numbers wrapped
								blockNumberWrap = false;
							}
							if (!blockNumberWrap){ // block number hasn't wrapped yet
								if (bn < blockNumber){ // duplicate ACK) {
									System.out.println(threadName() + 
											": Received Duplicate ACK: Ignoring and waiting for correct ACK...");
								} else if (bn == blockNumber) {
									break;  // got ACK with correct block number, continuing
								} else { // ACK with weird block number 
									// create and send error response packet for "Illegal TFTP operation."
									byte[] error = createError(4, "Received ACK with invalid block number.");
									send(error);
									try {
										in.close(); // close buffered reader after RRQ
									} catch (IOException e) { } 
									return;		
								}
							} else {
								if (bn > blockNumber){ // duplicate ACK
									System.out.println(threadName() + 
											": Received Duplicate ACK: Ignoring and waiting for correct ACK...");
								} else if (bn == blockNumber) {
									break;  // got ACK with correct block number, continuing
								} else { // ACK with weird block number 
									// create and send error response packet for "Illegal TFTP operation."
									byte[] error = createError(4, "Received ACK with invalid block number.");
									send(error);
									try {
										in.close(); // close buffered reader after RRQ
									} catch (IOException e) { } 
									return;		
								}
							}
						} else {
							// create and send error response packet for "Illegal TFTP operation."
							byte[] error = createError(4, "Expected ACK as response.");
							send(error);
							try {
								in.close(); // close buffered reader after RRQ
							} catch (IOException e) { } 
							return;		
						}
					}
					blockNumber++; // increment DATA block number
					
					// blockNumber goes from 0-65535, and then wraps to back to 0
					if (blockNumber > 65535) { 
						blockNumber = 0;
						blockNumberWrap = true;
					}
				}
				
				in.close();  // done reading from file
				
			} catch (FileNotFoundException e) {
				// create and send error response packet for "File not found."
				byte[] error = createError(1, "File (" + filename + 
						") does not exist.");
				send(error);
				try {
					in.close(); // close buffered reader after RRQ
				} catch (IOException e1) { } 
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
								System.out.println(threadName() + 
										": Socket Timeout: Resending DATA, and continuing to wait for ACK...");
								timedOut = true;  // have timed out once on this packet
								send(data); // send DATA
							} else {
								// have timed out a second time
								System.out.println(threadName() + 
										": Socket Timeout Again: Aborting file transfer:");
								System.out.println(threadName() + 
										": There may be a problem with the Client's connection.");
								try {
									in.close(); // close buffered reader after RRQ
								} catch (IOException e) { } 
								return;
							}
						}		
					}
				
					// invalid packet received
					if (receivePacket == null) {
						System.out.println(threadName() + 
								": Invalid packet received: Aborting file transfer:");
						try {
							in.close(); // close buffered reader after RRQ
						} catch (IOException e) { } 
						return;
					}
				
					byte[] ackPacket = processDatagram(receivePacket);  // read the expected ACK
					int op = twoBytesToInt(ackPacket[0], ackPacket[1]); // get opcode
					
					if (op == 5) {                            // ERROR received instead of ACK
						parseError(ackPacket);	// print ERROR info and close connection
						System.out.println(threadName() + 
								": Aborting Transfer.");
						try {
							in.close(); // close buffered reader after RRQ
						} catch (IOException e) { } 
						return;
					} else if (op == 4) {
						parseAck(ackPacket);	// print ACK info
						int bn = twoBytesToInt(ackPacket[2], ackPacket[3]); // get block number
						if (bn == 0) { // received block numbers wrapped
							blockNumberWrap = false;
						}
						if (!blockNumberWrap){ // block number hasn't wrapped yet
							if (bn < blockNumber){ // duplicate ACK) {
								System.out.println(threadName() + 
										": Received Duplicate ACK: Ignoring and waiting for correct ACK...");
							} else if (bn == blockNumber) {
								break;  // got ACK with correct block number, continuing
							} else { // ACK with weird block number 
								// create and send error response packet for "Illegal TFTP operation."
								byte[] error = createError(4, "Received ACK with invalid block number.");
								send(error);
								try {
									in.close(); // close buffered reader after RRQ
								} catch (IOException e) { } 
								return;		
							} 
						} else {
							if (bn > blockNumber){ // duplicate ACK
								System.out.println(threadName() + 
										": Received Duplicate ACK: Ignoring and waiting for correct ACK...");
							} else if (bn == blockNumber) {
								break;  // got ACK with correct block number, continuing
							} else { // ACK with weird block number 
								// create and send error response packet for "Illegal TFTP operation."
								byte[] error = createError(4, "Received ACK with invalid block number.");
								send(error);
								try {
									in.close(); // close buffered reader after RRQ
								} catch (IOException e) { } 
								return;		
							}
						}
					} else {
						// create and send error response packet for "Illegal TFTP operation."
						byte[] error = createError(4, "Expected ACK as response.");
						send(error);
						try {
							in.close(); // close buffered reader after RRQ
						} catch (IOException e) { } 
						return;		
					}
				}
			}			
			/* done sending last packet */
			
		/* file doesn't exist */
		} else {
			// create and send error response packet for "File not found."
			byte[] error = createError(1, "File (" + filename + ") does not exist.");
			send(error);
			return;
		}
		System.out.println("\n" + threadName() + ": RRQ File Transfer Complete");
		try {
			in.close(); // close buffered reader after RRQ
		} catch (IOException e) { } 
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
			int blockNumber = 0;					// block number for ACK and DATA during transfer
			byte[] ack = createAck(blockNumber);	// create initial ACK
			send(ack);								// send initial ACK
			byte[] data = new byte[0];		// to hold received data portion of DATA packet
			byte[] dataPacket = new byte[0];
			
			blockNumber++; // increment DATA block number
			
			// blockNumber goes from 0-65535, and then wraps to back to 0
			if (blockNumber > 65535) { 
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
							System.out.println(threadName() + 
									": Resending last ACK...");
							send(ack);
							timedOut = true;  // have timed out once on this packet
						} else {
							// have timed out a second time after re-sending the last packet
							System.out.println(threadName() + 
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
				int op = twoBytesToInt(dataPacket[0], dataPacket[1]); // get opcode
				
				if (op == 3) {						// received DATA
					int bn = twoBytesToInt(dataPacket[2], dataPacket[3]); // get block number
					if (bn == blockNumber) { // correct block number
						blockNumber++; // increment ACK block number
						
						// blockNumber goes from 0-65535, and then wraps to back to 0
						if (blockNumber > 65535) { 
							blockNumber = 0;
							blockNumberWrap = true;
						}
						
						data = getData(dataPacket);	// get data from packet
						// if last packet was empty, no need to write, end of transfer
						if (data.length == 0) { 
							ack = createAck(bn); // create ACK
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
								System.out.println(threadName() + 
										": writing data to file: " + filename);
							} else {
								// create and send error response packet for "Disk full or allocation exceeded."
								byte[] error = createError(3, "File (" + 
										filename + ") too large for disk.");
								send(error);
								return;
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						System.out.println(threadName() + ": Received DATA packet out of order: Not writing to file.");
						data = new byte[MAX_DATA]; // so we don't quit yet
					}
					ack = createAck(bn);	// create ACK
					send(ack);						// send ACK
				} else if (op == 5) { // ERROR received instead of DATA
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
						System.out.println("\n" + threadName() + ": WRQ File Transfer Incomplete");
						return;
					}
					dataPacket = processDatagram(receivePacket);	// read the DatagramPacket
					int op = twoBytesToInt(dataPacket[0], dataPacket[1]); // get opcode
					
					if (op == 3) {        // received DATA	
						int bn = twoBytesToInt(dataPacket[2], dataPacket[3]); // get block number
						ack = createAck(bn);   // create ACK
						send(ack);          // send ACK
					} else if (op == 5) { // ERROR received instead of DATA
						parseError(dataPacket);         // print ERROR info
						System.out.println("\n" + threadName() + ": WRQ File Transfer Incomplete");
						return;
					} else {
						// create and send error response packet for "Illegal TFTP operation."
						byte[] error = createError(4, "Was expecting DATA packet.");
						send(error);
						System.out.println("\n" + threadName() + ": WRQ File Transfer Incomplete");
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
				": Closing connection and shutting down thread.");
		
		// close ClientConnection thread to stop transfer
		Thread.currentThread().interrupt();	
	}
	
	/**
	 * Deals with two signed bytes and combines them into one int.
	 * 
	 * @param one	first byte
	 * @param two	second byte
	 * @return		two bytes combined into an int (value: 0 to 65535)
	 */
	public int twoBytesToInt(byte one, byte two) 
	{
		// add 256 if needed to compensate for signed bytes in Java
		int value1 = one < 0 ? one + 256 : one;
		int value2 = two < 0 ? two + 256 : two;
		
		// left shift value1 and combine with value2 into one int
		return (value2 | (value1 << 8));
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
	 * Get the data field from the DATA packet.
	 * 
	 * @param data	the DATA byte[]
	 * @return		just the data portion of a DATA packet byte[]
	 */
	public byte[] getData (byte[] data) 
	{
		// byte[] for the data portion of DATA packet byte[]
		byte[] justData = new byte[data.length - 4];	
		System.arraycopy(data, 4, justData, 0, data.length-4);
		
		return justData;
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
		
		// type of packet being sent 
		int op = twoBytesToInt(data[0], data[1]);
		String type = "";
		switch (op) {
			case 3: type = parseData(data);
				break;
			case 4: type = parseAck(data);
				break;
			case 5: type = parseError(data);
				break;
			default: 
				break;
		}
		
		// send the packet
		try {
			sendReceiveSocket.send(sendPacket);
			// print out packet info
			String direction = "-->";
			System.out.printf(threadName() + ": %30s %3s %-30s   bytes: %3d   - %s \n", 
					sendReceiveSocket.getLocalSocketAddress(), direction, sendPacket.getSocketAddress(),
					sendPacket.getLength(), type);
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
		
			System.out.println("\n" + threadName() + ": Waiting for packet...");
			
			// block until a DatagramPacket is received via sendReceiveSocket 
			try {
				// set timeout to receive response
				sendReceiveSocket.setSoTimeout(TIMEOUT);
				sendReceiveSocket.receive(packet);
			} catch (IOException e1) {
				throw new SocketTimeoutException();  // timed out
			}
			
			// checks if the received packet is a valid TFTP packet
			if (!isValidPacket(packet)) {
				// error packet is created and sent from within the isValidPacket method
				return null;
				
			// valid packet
			} else {
				
				byte[] packetData = packet.getData();  // the received packet's data
				int op = twoBytesToInt(packetData[0], packetData[1]); // get the opcode
				String type = "";  // type of packet
				
				// get packet info based on type of TFTP packet
				switch (op) {
					case 3: type = parseData(packetData);
						break;
					case 4: type = parseAck(packetData);
						break;
					case 5: type = parseError(packetData);
						break;
					default:
						break;
				}
				
				// print out packet info
				String direction = "<--";
				System.out.printf(threadName() + ": %30s %3s %-30s   bytes: %3d   - %s \n", 
						sendReceiveSocket.getLocalSocketAddress(), direction, packet.getSocketAddress(),
						packet.getLength(), type);
				
				// check for wrong transfer ID 
				if (!((packet.getAddress().equals(addr)) && 
						(packet.getPort() == port))) {				
					// create and send error response packet for "Unknown transfer ID."
					byte[] error = createError(5, 
							"Your packet was sent to the wrong place.");
					
					// create new DatagramPacket to send to unknown host
					sendPacket = new DatagramPacket(error, error.length, 
							packet.getAddress(), packet.getPort());
					
					type = parseError(error);
					
					// send the packet
					try {
						sendReceiveSocket.send(sendPacket);
						// print out packet info
						direction = "-->";
						System.out.printf(threadName() + ": %30s %3s %-30s   bytes: %3d   - %s \n", 
								sendReceiveSocket.getLocalSocketAddress(), direction, sendPacket.getSocketAddress(),
								sendPacket.getLength(), type);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					
				// packet came from the right place	
				} else {
					break;  // correct transfer ID and valid packet
				}
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
		
		return data;
	}
	
	/**
	 * Parse the acknowledgment byte[].
	 * 
	 * @param ack	the acknowledge byte[]
	 * @return		String representation of ACK
	 */
	public String parseAck (byte[] ack) 
	{
		// get block number
		int blockNumber = twoBytesToInt(ack[2], ack[3]);
		
		return "Acknowledgment #" + Integer.toString(blockNumber);
	}
	
	/**
	 * Parse the data byte[].
	 * 
	 * @param data	the data byte[]
	 * @return		String representation of DATA
	 */
	public String parseData (byte[] data) 
	{
		// get block number
		int blockNumber = twoBytesToInt(data[2], data[3]);
		
		return "Data packet    #" + Integer.toString(blockNumber);
	}
	
	/**
	 * Parse the error byte[].
	 * 
	 * @param error	the error byte[]
	 * @return 		String representation of ERROR
	 */
	public String parseError (byte[] error) 
	{
		// get error code
		int errorCode = twoBytesToInt(error[2], error[3]);

		// get the error message
		byte[] errorMsg = new byte[error.length - 5];
		System.arraycopy(error, 4, errorMsg , 0, error.length - 5);
		String message = null;
		try {
			message = new String(errorMsg, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}		
		
		return "Error " + Integer.toString(errorCode) + ": \"" + message + "\"";
	}
	
	/**
	 * Creates the byte[] to be sent as an acknowledgment DatagramPacket.
	 * 
	 * @param blockNumber	the data block number that is being acknowledged
	 * @return				the acknowledgment byte[]
	 */
	public byte[] createAck (int blockNumber) 
	{
		byte[] ack = new byte[4];
		
		// add opcode
		int opcode = 4;
		ack[0] = (byte)(opcode / 256);
		ack[1] = (byte)(opcode % 256);
		
		// add block number
		ack[2] = (byte)(blockNumber / 256);
		ack[3] = (byte)(blockNumber % 256);
		
		return ack;
	}
	
	/**
	 * Creates the byte[] to be sent as a data DatagramPacket.
	 * 
	 * @param blockNumber	the data block number 
	 * @param passedData	the data to be sent
	 * @return				the data byte[]
	 */
	public byte[] createData (int blockNumber, byte[] passedData) 
	{
		byte[] data = new byte[4 + passedData.length]; // new byte[] to be sent in DATA packet
		
		// add opcode
		int opcode = 3;
		data[0] = (byte)(opcode / 256);
		data[1] = (byte)(opcode % 256);
				
		// add block number
		data[2] = (byte)(blockNumber / 256);
		data[3] = (byte)(blockNumber % 256);
		
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
	public byte[] createError (int errorCode, String errorMsg) 
	{
		byte[] error = new byte[4 + errorMsg.length() + 1];	// new error to eventually be sent to server
		
		// add opcode
		int opcode = 5;
		error[0] = (byte)(opcode / 256);
		error[1] = (byte)(opcode % 256);
		
		// add error code
		error[2] = (byte)(errorCode / 256);
		error[3] = (byte)(errorCode % 256);
		
		byte[] message = new byte[errorMsg.length()];	// new array for errorMsg, to be joined with codes
		
		// convert errorMsg to byte[], with proper encoding
		try {
			message = errorMsg.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// add error message to error byte[]
		System.arraycopy(message, 0, error, 4, message.length);
		error[error.length-1] = 0;	// make last element a 0 byte, according to TFTP
				
		return error; //return full error message with opcodes and type of error
	}
	
	/**
	 * Determines if the packet is a valid TFTP packet.
	 * 
	 * @param received	the received DatagramPacket to be verified
	 * @return			true if valid packet, false if invalid
	 */
	public boolean isValidPacket (DatagramPacket received) 
	{
		byte[] data = received.getData(); // get data from received packet
		int len = received.getLength(); // number of data bytes in packet
		
		// check size of packet
		if (len < 4) {
			// create and send error response packet for "Illegal TFTP operation."
			byte[] error = createError(4, "Packet is less than 4 bytes in size.");
			send(error);
			return false;
		} 
		
		int op = twoBytesToInt(data[0], data[1]);	// get opcode
		
		// organize by opcode
		switch (op) {
			case 1: case 2:						// read or write request
				// create and send error response packet for "Illegal TFTP operation."
				byte[] error = createError(4, "Request packets are not accepted at this port.");
				send(error);
				return false;
			case 3:								// DATA packet
				if (len > MAX_DATA + 4) {
					// create and send error response packet for "Illegal TFTP operation."
					error = createError(4, "Data packet is too large.");
					send(error);
					return false;
				}
				break;
			case 4:								// ACK packet
				if (len != 4) { 
					// create and send error response packet for "Illegal TFTP operation."
					error = createError(4, "Ack packet is not 4 bytes in size.");
					send(error);
					return false; 
				}
				break;
			case 5:								// ERROR packet
				if (data[len - 1] != 0) {
					// create and send error response packet for "Illegal TFTP operation."
					error = createError(4, "Error message is not null terminated.");
					send(error);
					return false;	// error message not terminated with 0 byte
				}
				int ec = twoBytesToInt(data[2], data[3]); // get error code
				for (int i = 0; i<8; i++) {
					if (ec == i) {
						return true;	// found a valid error code
					}
				}
				// create and send error response packet for "Illegal TFTP operation."
				error = createError(4, "Error code is not a valid TFTP error code.");
				send(error);
				return false; 			// not a valid error code
			default: 
				// create and send error response packet for "Illegal TFTP operation."
				error = createError(4, "Invalid TFTP opcode.");
				send(error);
				return false;					// invalid opcode
		}		
		return true;
	}
}
