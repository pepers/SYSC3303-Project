
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
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
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
 * @version	4
 */
public class Client 
{	
	DatagramPacket sendPacket;										// to send data to server 
	DatagramPacket receivePacket;									// to receive data in
	DatagramSocket sendReceiveSocket;								// to send to and receive packets from server
	private static final int TIMEOUT = 2000;						// sendReceiveSocket's timeout when receiving
	private static Scanner input;									// scans user input in the simple console ui()
	String filename = "test0.txt";									// the file to be sent/received
	public static final String fileDirectory = "files\\client\\";	// directory for test files
	String mode = "octet";											// the mode in which to send/receive the file
	private BufferedInputStream in;									// input stream to read data from file
	public static final int MAX_DATA = 512;							// max number of bytes for data field in packet
	InetAddress addr;												// InetAddress of host responding to request
	int port;														// port number of host responding to request
	private byte req;												// request type sent by user 
	private boolean blockNumberWrap = false;						// true if block number wraps back to zero
	
	/**
	 * opcodes for the different DatagramPackets in TFTP
	 */
	public enum Opcode 
	{
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
	
	public Client() 
	{
		try {
			// new socket to send requests and receive responses
			sendReceiveSocket = new DatagramSocket();	
		} catch (SocketException se) {   // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Creates a new Client, starts the user interface, and continues a connection
	 * to the Server or ErrorSim.
	 * 
	 * @param args			command line arguments
	 * @throws Exception	 
	 */
	public static void main (String args[]) throws Exception 
	{
		Client c = new Client();
		System.out.println("***** Welcome to Group #2's SYSC3303 TFTP Client Program *****\n");
		// loop until user chooses to not send another request and quit
		while(true) {
			c.ui();	// start the user interface to send request
			c.connection();	// receive and send packets with Server or ErrorSim
		}
	}
	
	/**
	 * The simple console text user interface for the client program.  User navigates 
	 * through menus to send request DatagramPacket.
	 * 
	 */
	public void ui() 
	{	
		// determine if user wants to send a read request or a write request
		Opcode op;	// the user's choice of request to send
		input = new Scanner(System.in);		// scans user input
		while (true) {
			System.out.println("\nWould you like to make a (R)ead Request, (W)rite Request, or (Q)uit?");
			String choice = input.nextLine();	// user's choice
			if (choice.equalsIgnoreCase("R")) {			// read request
				op = Opcode.RRQ;
				System.out.println("\nClient: You have chosen to send a read request.");
				break;
			} else if (choice.equalsIgnoreCase("W")) {	// write request
				op = Opcode.WRQ;
				System.out.println("\nClient: You have chosen to send a write request.");
				break;
			} else if (choice.equalsIgnoreCase("Q")) {	// quit
				System.out.println("\nGoodbye!");
				System.exit(0);
			} else {
				System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
			}
		}
		
		// determines where the user wants to send the request
		int dest; // the port destination of the user's request
		while (true) {
			System.out.println("Where would you like to send your request: ");
			System.out.println("- directly to the (S)erver ");
			System.out.println("- to the Server, but through the (E)rror Simulator first");
			System.out.println("- I've changed my mind, I want to (Q)uit instead");
			String choice = input.nextLine();	// user's choice
			if (choice.equalsIgnoreCase("S")) {			// request to Server
				dest = 69;
				System.out.println("\nClient: You have chosen to send your request to the Server.");
				break;
			} else if (choice.equalsIgnoreCase("E")) {	// request to Error Simulator
				dest = 68;
				System.out.println("\nClient: You have chosen to send your request to the Error Simulator.");
				break;
			} else if (choice.equalsIgnoreCase("Q")) {	// quit
				System.out.println("\nGoodbye!");
				System.exit(0);
			} else {
				System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
			}
		}
		
		// determine which file the user wants to modify
		while(true) {
			System.out.println("Please choose a file to modify.  Type in a file name: ");
			filename = input.nextLine();	// user's choice
			
			// deal with user's choice of request
			if (op == Opcode.RRQ) {
				if (!(Files.exists(Paths.get(fileDirectory + filename)))) {	// file doesn't exist
					System.out.println("\nClient: You have chosen the file: " + 
							filename + ", to be received in " + mode + " mode.");	
					break;
				} else{														// file already exists
					System.out.println("\nClient: I'm sorry, " + fileDirectory + 
							filename + " already exists:");
					while(true) {
						System.out.println("(T)ry another file, or (Q)uit: ");
						String choice = input.nextLine();	// user's choice
						if (choice.equalsIgnoreCase("Q")) {			// quit
							System.out.println("\nGoodbye!");
							System.exit(0);
						} else if (choice.equalsIgnoreCase("T")) {	// try another file
							break;
						} else {									// invalid choice
							System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
						}
					}
				}
			} else if (op == Opcode.WRQ) {					
				if (Files.isReadable(Paths.get(fileDirectory + filename))) {	// file exists and is readable
					System.out.println("\nClient: You have chosen the file: " + 
							fileDirectory + filename + ", to be sent in " + 
							mode + " mode.");
					break;
				} else {														// file does not exist
					System.out.println("\nClient: I'm sorry, " + fileDirectory +
							filename + " does not exist, or is not readable:");
					while(true) {
						System.out.println("(T)ry another file, or (Q)uit: ");
						String choice = input.nextLine();	// user's choice
						if (choice.equalsIgnoreCase("Q")) {			// quit
							System.out.println("\nGoodbye!");
							System.exit(0);
						} else if (choice.equalsIgnoreCase("T")) {	// try another file
							break;
						} else {									// invalid choice
							System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
						}
					}
				}
			}
		}
		
		req = op.op(); // 1 for RRQ, 2 for WRQ
		byte[] request = createRequest(req, filename, mode);	// get the request byte[] to send
				
		// send request to correct port destination
		try{
			send(request, InetAddress.getLocalHost(), dest);
		} catch (UnknownHostException e) {
			System.out.println("\nClient: Error, InetAddress could not be found. Shutting Down...");
			System.exit(1);			
		}
	}
	
	/**
	 * Continues connection with Server or ErrorSim, to transfer DatagramPackets.
	 * 
	 */
	public void connection () 
	{
		while (true) {
			// no packet will be larger than DATA packet
			// room for a possible maximum of 512 bytes of data + 4 bytes opcode and block number
			byte data[] = new byte[MAX_DATA + 4];
		
			DatagramPacket packet = new DatagramPacket(data, data.length);  // gets received DatagramPacket
			
			try {
				// set timeout to receive response
				sendReceiveSocket.setSoTimeout(TIMEOUT); 
				
				// block until a DatagramPacket is received via sendReceiveSocket 
				sendReceiveSocket.receive(packet);
			} catch(IOException e) {
				// receiving response to read/write request has timed out
				System.out.println("\nClient: Did not receive a response to your request.");
				System.out.println("You may try again, but the Server may not be running...");				
				return;
			} 
			
			addr = packet.getAddress();
			port = packet.getPort();
		
			// checks if the received packet is a valid TFTP packet
			if (!isValidPacket(packet)) {
				// create and send error response packet for "Illegal TFTP operation."
				byte[] error = createError((byte)4, "Invalid packet.");
				send(error, addr, port);
			} else {
				// print out thread and port info, from which the packet was sent
				System.out.println("\nClient: packet received: ");
				System.out.println("From host: " + packet.getAddress() + " : " + 
						packet.getPort());
				System.out.println("Containing " + packet.getLength() + 
						" bytes: ");			
			
				byte[] received = processDatagram(packet);	// received packet turned into byte[]
			
				// DATA && RRQ was sent && block 01
				if ((received[1] == 3) && (req == 1) && received[3] == 1) {         
					byte[] justData = parseData(received);  // print DATA info to user		
					try {
						// gets space left on the drive that we can use
						long spaceOnDrive = Files.getFileStore(
								Paths.get("")).getUsableSpace();	
						
						// checks if there is enough usable space on the disk
						if (spaceOnDrive > data.length) {
							// writes data to file (creates file first, if it doesn't exist yet)
							Files.write(Paths.get(fileDirectory + filename), 
									justData, StandardOpenOption.CREATE, 
									StandardOpenOption.APPEND);
							System.out.println("\nClient: writing data to file: "
									+ filename);
						} else {
							// create and send error response packet for "Disk full or allocation exceeded."
							byte[] error = createError((byte)3, "File (" + 
									filename + ") too large for disk.");
							send(error, addr, port);
							return;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}					
					
					if (justData.length < MAX_DATA) {
						byte[] ack = createAck(received[3]);  // create initial ACK
						send(ack, addr, port);            // send ACK
						System.out.println("\nClient: RRQ File Transfer Complete.");
						break;
					}
					
					readReq();                        // start file transfer for RRQ
					break;
				
				// ACK && it is block number 0 && WRQ was sent
				} else if ((received[1] == 4) && (received[3] == 0) && (req == 2)) {  
					parseAck(received);  // print ACK info to user				
					writeReq();  // start file transfer for WRQ
					break;
			
				// ERROR - in case we sent to the wrong place, we want to know
				} else if (received[1] == 5) {  
					parseError(received);  // print error info to user
					break;
				
				// useless packet
				} else {                        
					// create and send error response packet for "Unknown transfer ID."
					byte[] error = createError((byte)5, 
							"Your packet was sent to the wrong place.");
					send(error, addr, port);
				}
			}
		}
	}
	
	/**
	 * RRQ file transfer.
	 * 
	 */
	public void readReq() 
	{
		byte blockNumber = 1;  // block number for ACK and DATA during transfer
		byte[] data = new byte[0];    // data from DATA packet
		byte[] dataPacket = new byte[0];
		
		byte[] ack = createAck(blockNumber);  // create initial ACK
		send(ack, addr, port);            // send ACK
		
		blockNumber++; // increment ACK block number
		
		// blockNumber goes from 0-127, and then wraps to back to 0
		if (blockNumber < 0) { 
			blockNumber = 0;
			blockNumberWrap = true;
		}
		
		do {	// DATA transfer from server
			
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
						System.out.println("\nClient: Socket Timeout: Resending last ACK...");
						send(ack, addr, port);
						timedOut = true;  // have timed out once on this packet
					} else {
						// have timed out a second time after re-sending the last packet
						System.out.println("\nClient: Socket Timeout Again: Aborting file transfer:");
						System.out.println("\nClient: You may try again, but the Server may not be running...");
						return;
					}
				}
			}
			
			// invalid packet received
			if (receivePacket == null) {
				return;
			}
			
			dataPacket = processDatagram(receivePacket);	// read the DatagramPacket
			
			if (dataPacket[1] == 3) {        // received DATA
				if (dataPacket[3] == blockNumber) { // correct block number
					blockNumber++; // increment ACK block number
					
					// blockNumber goes from 0-127, and then wraps to back to 0
					if (blockNumber < 0) { 
						blockNumber = 0;
						blockNumberWrap = true;
					}
					data = parseData(dataPacket);   // get data from packet
					// if last packet was empty, no need to write, end of transfer
					if (data.length == 0) { 
						ack = createAck(dataPacket[3]);   // create ACK
						send(ack, addr, port);          // send ACK
						break;
					}
					try {
						// gets space left on the drive that we can use
						long spaceOnDrive = Files.getFileStore(
								Paths.get("")).getUsableSpace();	
					
						// checks if there is enough usable space on the disk
						if (spaceOnDrive > data.length + 1024) { // +1024 bytes for safety
							// writes data to file (creates file first, if it doesn't exist yet)
							Files.write(Paths.get(fileDirectory + filename), data, 
									StandardOpenOption.CREATE, 
									StandardOpenOption.APPEND);
							System.out.println("\nClient: writing data to file: " + 
									filename);
						} else {
							// create and send error response packet for "Disk full or allocation exceeded."
							byte[] error = createError((byte)3, "File (" + filename 
									+ ") too large for disk.");
							send(error, addr, port);
							return;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("\nClient: Received DATA packet out of order: Not writing to file.");
					data = new byte[MAX_DATA]; // so we don't quit yet
				}
				ack = createAck(dataPacket[3]);   // create ACK
				send(ack, addr, port);          // send ACK
			} else if (dataPacket[1] == 5) { // ERROR received instead of DATA
				parseError(dataPacket);         // print ERROR info
				return;
			} else {
				// create and send error response packet for "Illegal TFTP operation."
				byte[] error = createError((byte)4, "Was expecting DATA packet.");
				send(error, addr, port);
				return;		
			}
		} while (data.length == MAX_DATA);
		
		// dallying - in case final ACK is not received by Server
		System.out.println("\nClient: Dallying in case final ACK was not received...");
		DatagramPacket receivePacket = null;			
		while (true) {
			try {
				receivePacket = receive(); // receive the DatagramPacket
				
				// invalid packet received
				if (receivePacket == null) {
					System.out.println("\nClient: RRQ File Transfer Complete.");
					return;
				}
				dataPacket = processDatagram(receivePacket);	// read the DatagramPacket
				
				if (dataPacket[1] == 3) {        // received DATA					
					ack = createAck(dataPacket[3]);   // create ACK
					send(ack, addr, port);          // send ACK
				} else if (dataPacket[1] == 5) { // ERROR received instead of DATA
					parseError(dataPacket);         // print ERROR info
					System.out.println("\nClient: RRQ File Transfer Complete.");
					return;
				} else {
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError((byte)4, "Was expecting DATA packet.");
					send(error, addr, port);
					System.out.println("\nClient: RRQ File Transfer Complete.");
					return;		
				}
			} catch (SocketTimeoutException e1) {
				System.out.println("\nClient: RRQ File Transfer Complete.");
				break;
			}
		}
	}
	
	/**
	 * WRQ file transfer.
	 * 
	 */
	public void writeReq() 
	{
		try {
			in = new BufferedInputStream(new FileInputStream(fileDirectory + 
					filename));	// reads from file during WRQ
		} catch (FileNotFoundException e) {
			// create and send error response packet for "Access violation."
			byte[] error = createError((byte)2, "File (" + filename + 
					") exists on client, but is not readable.");
			send(error, addr, port);
			return;
		}		
			
		byte[] read = new byte[MAX_DATA];  // to hold bytes read
		int bytes = 0;                     // number of bytes read
		byte blockNumber = 1;              // DATA block number
		
		// read up to 512 bytes from file
		try {			
			while ((bytes = in.read(read)) != -1) {
				System.out.println("\nClient: Read " + bytes + " bytes, from " 
						+ fileDirectory + filename);
				
				// get rid of extra buffer
				byte[] temp = new byte[bytes];
				System.arraycopy(read, 0, temp, 0, bytes);
				read = temp;
				
				// create DATA packet of file being read
				byte[] data = createData(blockNumber, read);
				send(data, addr, port); // send DATA
				
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
								System.out.println("\nClient: Socket Timeout: Resending DATA and continuing to wait for ACK...");
								timedOut = true;  // have timed out once on this packet
								send(data, addr, port); // send DATA
							} else {
								// have timed out a second time
								System.out.println("\nClient: Socket Timeout Again: Aborting file transfer:");
								System.out.println("Client: You may try again, but the Server may not be running...");
								return;
							}
						}		
					}
				
					// invalid packet received
					if (receivePacket == null) {
						System.out.println("\nClient: Invalid packet received: Aborting file transfer:");
						System.out.println("Client: You may try to send another request...");
						return;
					}
				
					byte[] ackPacket = processDatagram(receivePacket);  // read the expected ACK
					if (ackPacket[1] == 5) {                            // ERROR received instead of ACK
						parseError(ackPacket);	// print ERROR info and close connection
						System.out.println("\nClient: Aborting Transfer.");
						return;
					} else if (ackPacket[1] == 4) {
						parseAck(ackPacket);	// print ACK info
						if (ackPacket[3] == 0) { // received block numbers wrapped
							blockNumberWrap = false;
						}
						if (!blockNumberWrap){ // block number hasn't wrapped yet
							if (ackPacket[3] < blockNumber){ // duplicate ACK) {
								System.out.println("\nClient: Received Duplicate ACK: Ignoring and waiting for correct ACK...");
							} else if (ackPacket[3] == blockNumber) {
								break;  // got ACK with correct block number, continuing
							} else { // ACK with weird block number 
								// create and send error response packet for "Illegal TFTP operation."
								byte[] error = createError((byte)4, "Received ACK with invalid block number.");
								send(error, addr, port);
								return;		
							} 
						} else {
							if (ackPacket[3] > blockNumber){ // duplicate ACK
								System.out.println("\nClient: Received Duplicate ACK: Ignoring and waiting for correct ACK...");
							} else {
								if (ackPacket[3] == blockNumber) {
									break;  // got ACK with correct block number, continuing
								} else { // ACK with weird block number 
									// create and send error response packet for "Illegal TFTP operation."
									byte[] error = createError((byte)4, "Received ACK with invalid block number.");
									send(error, addr, port);
									return;		
								}
							}
						}
					} else {
						// create and send error response packet for "Illegal TFTP operation."
						byte[] error = createError((byte)4, "Expected ACK as response.");
						send(error, addr, port);
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
		} catch (FileNotFoundException e) {
			// create and send error response packet for "File not found."
			byte[] error = createError((byte)1, "File (" + filename + 
					") does not exist.");
			send(error, addr, port);
			return;		
		} catch (IOException e) {
			System.out.println("\nError: could not read from BufferedInputStream.");
			System.exit(1);
		}
			
		/* last packet */
		// check if file was a multiple of 512 bytes in size, send 0 byte DATA
		if (read.length == MAX_DATA) {
			read = new byte[0];                           // create 0 byte read file data
			
			byte[] data = createData(blockNumber, read);  // create DATA packet of file being read
			send(data, addr, port);                       // send DATA
			
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
							System.out.println("\nClient: Socket Timeout: Resending DATA and continuing to wait for ACK...");
							timedOut = true;  // have timed out once on this packet
							send(data, addr, port); // send DATA
						} else {
							// have timed out a second time
							System.out.println("\nClient: Socket Timeout Again: Aborting file transfer:");
							System.out.println("Client: You may try again, but the Server may not be running...");
							return;
						}
					}		
				}
			
				// invalid packet received
				if (receivePacket == null) {
					System.out.println("\nClient: Invalid packet received: Aborting file transfer:");
					System.out.println("Client: You may try to send another request...");
					return;
				}
			
				byte[] ackPacket = processDatagram(receivePacket);  // read the expected ACK
				if (ackPacket[1] == 5) {                            // ERROR received instead of ACK
					parseError(ackPacket);	// print ERROR info and close connection
					System.out.println("\nClient: Aborting Transfer.");
					return;
				} else if (ackPacket[1] == 4) {
					parseAck(ackPacket);	// print ACK info
					if (ackPacket[3] == 0) { // received block numbers wrapped
						blockNumberWrap = false;
					}
					if (!blockNumberWrap){ // block number hasn't wrapped yet
						if (ackPacket[3] < blockNumber){ // duplicate ACK) {
							System.out.println("\nClient: Received Duplicate ACK: Ignoring and waiting for correct ACK...");
						} else if (ackPacket[3] == blockNumber) {
							break;  // got ACK with correct block number, continuing
						} else { // ACK with weird block number 
							// create and send error response packet for "Illegal TFTP operation."
							byte[] error = createError((byte)4, "Received ACK with invalid block number.");
							send(error, addr, port);
							return;		
						}
					} else {
						if (ackPacket[3] > blockNumber){ // duplicate ACK
							System.out.println("\nClient: Received Duplicate ACK: Ignoring and waiting for correct ACK...");
						} else if (ackPacket[3] == blockNumber) {
							break;  // got ACK with correct block number, continuing
						} else { // ACK with weird block number 
							// create and send error response packet for "Illegal TFTP operation."
							byte[] error = createError((byte)4, "Received ACK with invalid block number.");
							send(error, addr, port);
							return;		
						}
					}
				} else {
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError((byte)4, "Expected ACK as response.");
					send(error, addr, port);
					return;		
				}
			}
		}			
		/* done sending last packet */
		System.out.println("\nClient: WRQ File Transfer Complete.");
	}
	
	/**
	 * Creates the request byte[] to be later sent as a DatagramPacket.
	 * 
	 * @param opcode	differentiates between read (1) and write (2) requests
	 * @param filename	name of file to be sent/requested to/from server
	 * @param mode		mode of file transfer in TFTP
	 * @return			the read/write request byte[]
	 */
	public static byte[] createRequest(byte opcode, String filename, String mode) 
	{
		byte data[]=new byte[filename.length() + mode.length() + 4];
		
		// request opcode
		data[0] = 0;
		data[1] = opcode;
		
		// convert filename and mode to byte[], with proper encoding
		byte[] fn = null;	// filename
		byte[] md = null;	// mode
		try {
			fn = filename.getBytes("US-ASCII");
			md = mode.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// add filename and mode to request 
		data[fn.length + 3] = 0;		
		System.arraycopy(fn,0,data,2,fn.length);
		System.arraycopy(md,0,data,fn.length+3,md.length);
		data[data.length-1] = 0;
		
		return data;
	}
	
	/**
	 * Creates the byte[] to be sent as an acknowledgment DatagramPacket.
	 * 
	 * @param blockNumber	the data block number that is being acknowledged
	 * @return				the acknowledgment byte[]
	 */
	public byte[] createAck (byte blockNumber) 
	{
		byte[] temp = new byte[4];
		temp[0] = (byte) 0;
		temp[1] = (byte) 4;
		temp[2] = (byte)0;
		temp[3] = blockNumber;
		return temp;
	}
	
	/**
	 * Creates the byte[] to be sent as a data DatagramPacket.
	 * 
	 * @param blockNumber	the data block number 
	 * @param passedData	the data to be sent
	 * @return				the data byte[]
	 */
	public byte[] createData (byte blockNumber, byte[] passedData) 
	{
		byte[] data = new byte[4 + passedData.length]; // new byte[] to be sent in DATA packet
		
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
		byte[] error = new byte[4 + errorMsg.length() + 1];	// new error to eventually be sent to server
		
		// inform user
		System.out.println("\nClient: 0" + errorCode + " Error: informing host: ");
		System.out.println("Error Message: " + errorMsg);
		
		// add opcode
		error[0] = 0;
		error[1] = 5;
		
		// add error code
		error[2] = 0;
		error[3] = errorCode;
		
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
	 * Parse the acknowledgment byte[] and display info to user.
	 * 
	 * @param ack	the acknowledge byte[]
	 */
	public void parseAck (byte[] ack) 
	{
		System.out.println("\nClient: Recieved packet is ACK: ");
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
		System.out.println("\nClient: Recieved packet is DATA: ");
		System.out.println("Block#: " + data[2] + data[3] + 
				", and containing data: ");
		System.out.println(Arrays.toString(justData));
		
		return justData;
	}
	
	/**
	 * Parse the error byte[] and display info to user.
	 * 
	 * @param error	the error byte[]
	 * @return 		the TFTP Error Code byte value
	 */
	public void parseError (byte[] error) 
	{
		System.out.println("\nClient: Received packet is ERROR: ");		

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
	}
	
	/**
	 * Sends DatagramPacket.
	 * 
	 * @param data	data byte[] to be included in DatagramPacket
	 * @param addr	InetAddress to send DatagramPacket to 
	 * @param port	port number to send DatagramPacket to
	 */
	public void send (byte[] data, InetAddress iAddr, int portNum) 
	{
		sendPacket = new DatagramPacket(data, data.length, iAddr, portNum);
		
		// tell user what type of packet is being sent 
		switch (data[1]) {
			case 1: System.out.println("\nClient: Sending RRQ:");
				break;
			case 2: System.out.println("\nClient: Sending WRQ:");
				break;
			case 3: System.out.println("\nClient: Sending DATA:");
				break;
			case 4: System.out.println("\nClient: Sending ACK:");
				break;
			case 5: System.out.println("\nClient: Sending ERROR:");
				break;
			default: System.out.println("\nClient: Sending packet:");
				break;
		}

		System.out.println("To host: " + sendPacket.getAddress() + " : " + 
				sendPacket.getPort());
		System.out.println("Containing " + sendPacket.getLength() + " bytes: ");
		System.out.println(Arrays.toString(data) + "\n"); 
		
		// send the DatagramPacket to the server via the send/receive socket
		try {			
			// attempt to send DatagramPacket
			sendReceiveSocket.send(sendPacket);
			System.out.println("Client: Packet sent");
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
		// room for a possible maximum of 512 bytes of data + 4 bytes opcode and block number
		byte data[] = new byte[MAX_DATA + 4];
		
		DatagramPacket packet = null;  // new DatagramPacket to be received
				
		// loop until packet received from expected host
		while (true) {
			packet = new DatagramPacket(data, data.length);
			
			System.out.println("\nClient: Waiting for packet...");
			
			// block until a DatagramPacket is received via sendReceiveSocket 
			try {
				// set timeout to receive response
				sendReceiveSocket.setSoTimeout(TIMEOUT);
				sendReceiveSocket.receive(packet);
			} catch (IOException e1) {
				throw new SocketTimeoutException();  // timed out
			}
			
			
			// check for wrong transfer ID 
			if (!((packet.getAddress().equals(addr)) && 
					(packet.getPort() == port))) {				
				// create and send error response packet for "Unknown transfer ID."
				byte[] error = createError((byte)5, 
						"Your packet was sent to the wrong place.");
				
				// create new DatagramPacket to send to send error
				sendPacket = new DatagramPacket(error, error.length, 
						packet.getAddress(), packet.getPort());
				
				// print out packet info to user
				System.out.println("\nClient: Sending ERROR: ");
				System.out.println("To host: " + sendPacket.getAddress() + 
						" : " + sendPacket.getPort());
				System.out.print("Containing " + sendPacket.getLength() + 
						" bytes: ");
				System.out.println(Arrays.toString(error) + "\n");
				
				// send the packet
				try {
					sendReceiveSocket.send(sendPacket);
					System.out.println("Client: Packet sent ");
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			} else {
				// checks if the received packet is a valid TFTP packet
				if (!isValidPacket(packet)) {
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError((byte)4, "Invalid packet.");
					send(error, addr, port);
					return null;
				}
				
				// print out thread and port info, from which the packet was sent
				System.out.println("\nClient: packet received: ");
				System.out.println("From host: " + packet.getAddress() + 
						" : " + packet.getPort());
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
	 * Determines if the packet is a valid TFTP packet.
	 * 
	 * @param received	the received DatagramPacket to be verified
	 * @return			true if valid packet, false if invalid
	 */
	public boolean isValidPacket (DatagramPacket received) 
	{
		int len = received.getLength();				            // number of data bytes in packet
		byte[] data = new byte[len];                            // new byte[] for storing received data 
		System.arraycopy(received.getData(), 0, data, 0, len);  // copy over data
		
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
}
