
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
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
 * @version	5
 */
public class Client 
{	
	DatagramPacket sendPacket;										// to send data to server 
	DatagramPacket receivePacket;									// to receive data in
	static DatagramSocket sendReceiveSocket;								// to send to and receive packets from server
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
				
		input = new Scanner(System.in);		// scans user input
		
		// determines the InetAddress to send the request to
		InetAddress inet;
		while (true) {
			System.out.println("What is the Internet Address of the Server that you want to send a request to?");
			System.out.println("(enter the ip address or host name)");
			String choice = input.nextLine();	// user's choice
			try {
				inet = InetAddress.getByName(choice); // get InetAddress from choice
				break;
			} catch (UnknownHostException e) {
				System.out.println("\nI'm sorry, no IP Address could be found for the host \"" 
						+ choice + "\".  Please try again...");
			}			
		}
		
		// determines the port to send the request to
		int dest; // the port destination of the user's request
		while (true) {
			System.out.println("\nWhere would you like to send your request: ");
			System.out.println("- directly to the (S)erver ");
			System.out.println("- to the Server, but through the (E)rror Simulator first");
			System.out.println("- I've changed my mind, I want to (Q)uit instead");
			String choice = input.nextLine();	// user's choice
			if (choice.equalsIgnoreCase("S")) {			// request to Server
				dest = 69;
				break;
			} else if (choice.equalsIgnoreCase("E")) {	// request to Error Simulator
				dest = 68;
				break;
			} else if (choice.equalsIgnoreCase("Q")) {	// quit
				System.out.println("\nGoodbye!");
				System.exit(0);
			} else {
				System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
			}
		}
		
		// String representation of port destination
		String sDest = ", directly.";
		if (dest == 68) {
			sDest = ", through the Error Simulator.";
		}		

		System.out.println("\nClient: You have chosen to send your request to the Server at " 
				+ inet.getHostAddress() + sDest);
		
		// loop until user chooses to not send another request and quit
		while(true) {
			c.ui(dest, inet);	// start the user interface to send request
			c.connection();	// receive and send packets with Server or ErrorSim
			sendReceiveSocket.close();
		}
	}
	
	/**
	 * The simple console text user interface for the client program.  User navigates 
	 * through menus to send request DatagramPacket.
	 * 
	 * @param dest	the destination port (68 for ErrorSim, 69 for Server)
	 * @param inet	the InetAddress to send the request to 
	 */
	public void ui(int dest, InetAddress inet) 
	{	
		// determine if user wants to send a read request or a write request
		Opcode op;	// the user's choice of request to send
		input = new Scanner(System.in);		// scans user input
		while (true) {
			System.out.println("\nWould you like to make a (R)ead Request, a (W)rite Request, (E)nter a new destination, or (Q)uit?");
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
			} else if (choice.equalsIgnoreCase("E")) {  // enter new InetAddress of Server
				// determines the InetAddress to send the request to
				while (true) {
					System.out.println("\nWhat is the Internet Address of the Server that you want to send a request to?");
					System.out.println("(enter the ip address or host name)");
					choice = input.nextLine();	// user's choice
					try {
						inet = InetAddress.getByName(choice); // get InetAddress from choice
						break;
					} catch (UnknownHostException e) {
						System.out.println("\nI'm sorry, no IP Address could be found for the host \"" 
								+ choice + "\".  Please try again...");
					}			
				}
				
				// determines the port to send the request to
				while (true) {
					System.out.println("\nWhere would you like to send your request: ");
					System.out.println("- directly to the (S)erver ");
					System.out.println("- to the Server, but through the (E)rror Simulator first");
					System.out.println("- I've changed my mind, I want to (Q)uit instead");
					choice = input.nextLine();	// user's choice
					if (choice.equalsIgnoreCase("S")) {			// request to Server
						dest = 69;
						break;
					} else if (choice.equalsIgnoreCase("E")) {	// request to Error Simulator
						dest = 68;
						break;
					} else if (choice.equalsIgnoreCase("Q")) {	// quit
						System.out.println("\nGoodbye!");
						System.exit(0);
					} else {
						System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
					}
				}
				
				// String representation of port destination
				String sDest = ", directly.";
				if (dest == 68) {
					sDest = ", through the Error Simulator.";
				}		

				System.out.println("\nClient: You have chosen to send your request to the Server at " 
						+ inet.getHostAddress() + sDest + "\n");
			} else {
				System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
			}
		}
		
		// determine which file the user wants to modify
		while(true) {
			System.out.println("Please choose a file to modify.  Type in a file name: ");
			filename = input.nextLine();	// user's choice
			
			Path p = Paths.get(fileDirectory + filename); // get path to file
			File f = new File(p.toString()); // turn path to string
			
			// deal with user's choice of request
			if (op == Opcode.RRQ) {
				if (!(f.exists())) {	// file doesn't exist
					System.out.println("\nClient: You have chosen the file: " + 
							filename + ", to be received in " + mode + " mode. \n");	
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
				if (f.canRead()) {	// file exists and is readable
					System.out.println("\nClient: You have chosen the file: " + 
							fileDirectory + filename + ", to be sent in " + 
							mode + " mode. \n");
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
		
		// get the request byte[] to send
		byte[] request = createRequest((int)req, filename, mode);	
		
		try {
			// new socket to send requests and receive responses
			sendReceiveSocket = new DatagramSocket();	
		} catch (SocketException se) {   // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
				
		// send request to correct port and InetAddress destination
		send(request, inet, dest);
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
				System.out.println("\nClient: Waiting for packet...");
				sendReceiveSocket.receive(packet);
			} catch(IOException e) {
				// receiving response to read/write request has timed out
				System.out.println("Client: Did not receive a response to your request.");
				System.out.println("Client: You may try again, but the Server may not be running...");				
				return;
			} 
			
			addr = packet.getAddress();
			port = packet.getPort();
		
			// checks if the received packet is a valid TFTP packet
			if (!isValidPacket(packet)) {
				// error is sent from within isValidPacket method
				return;
			} else {			
				byte[] received = processDatagram(packet);	// received packet turned into byte[]
				int op = twoBytesToInt(received[0], received[1]); // get the opcode
				String type = "";  // type of packet
				
				// get packet info based on type of TFTP packet
				switch (op) {
					case 1: case 2: type = parseRequest(received);
						break;
					case 3: type = parseData(received);
						break;
					case 4: type = parseAck(received);
						break;
					case 5: type = parseError(received);
						break;
					default:
						break;
				}
				
				// print out packet info
				String direction = "<--";
				System.out.printf("Client: %30s %3s %-30s   bytes: %3d   - %s \n", 
						sendReceiveSocket.getLocalSocketAddress(), direction, packet.getSocketAddress(),
						packet.getLength(), type);
				
				// DATA && RRQ was sent && block number is 1
				if ((op == 3) && (req == 1)) {
					int blockNumber = twoBytesToInt(received[2], received[3]); // get block number
					if (blockNumber == 1) {
						byte[] justData = getData(received);  // print DATA info to user		
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
								System.out.println("Client: Writing data to file: "
										+ filename);
							} else {
								// create and send error response packet for "Disk full or allocation exceeded."
								byte[] error = createError(3, "File (" + 
										filename + ") too large for disk.");
								send(error, addr, port);
								return;
							}
						} catch (IOException e) {
							e.printStackTrace();
						}					
					
						if (justData.length < MAX_DATA) {
							byte[] ack = createAck(1);  // create initial ACK
							send(ack, addr, port);            // send ACK
							System.out.println("\nClient: RRQ File Transfer Complete.");
							break;
						}
					
						readReq();                        // start file transfer for RRQ
						break;
					} else {
						// create and send error response packet for "Illegal TFTP operation."
						byte[] error = createError(4, "Was expecting block number 1.");
						send(error, addr, port);
					}
				// ACK && WRQ was sent && block number is 0
				} else if ((op == 4) && (req == 2)) {  
					int blockNumber = twoBytesToInt(received[2], received[3]); // get block number
					if (blockNumber == 0) {
						parseAck(received);  // print ACK info to user				
						writeReq();  // start file transfer for WRQ
						break;
					} else {
						// create and send error response packet for "Illegal TFTP operation."
						byte[] error = createError(4, "Was expecting block number 0.");
						send(error, addr, port);
					}
				// ERROR - in case we sent to the wrong place, we want to know
				} else if (received[1] == 5) {  
					parseError(received);  // print error info to user
					break;
				
				// useless packet
				} else {                        
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error;
					if (req == 1) {
						error = createError(4, "Was expecting a Data Packet.");
					} else {
						error = createError(4, "Was expecting an Acknowledgement Packet.");
					}
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
		int blockNumber = 1;  // block number for ACK and DATA during transfer
		byte[] data = new byte[0];    // data from DATA packet
		byte[] dataPacket = new byte[0];
		
		byte[] ack = createAck(blockNumber);  // create initial ACK
		send(ack, addr, port);            // send ACK
		
		blockNumber++; // increment ACK block number
		
		// blockNumber goes from 0-65535, and then wraps to back to 0
		if (blockNumber > 65535) { 
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
						System.out.println("Client: Socket Timeout: Resending last ACK...");
						send(ack, addr, port);
						timedOut = true;  // have timed out once on this packet
					} else {
						// have timed out a second time after re-sending the last packet
						System.out.println("Client: Socket Timeout Again: Aborting file transfer:");
						System.out.println("Client: You may try again, but the Server may not be running...");
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
			
			if (op == 3) {        // received DATA
				int bn = twoBytesToInt(dataPacket[2], dataPacket[3]); // get block number
				if (bn == blockNumber) { // correct block number
					blockNumber++; // increment ACK block number
					
					// blockNumber goes from 0-65535, and then wraps to back to 0
					if (blockNumber > 65535) { 
						blockNumber = 0;
						blockNumberWrap = true;
					}
					
					data = getData(dataPacket);   // get data from packet
					// if last packet was empty, no need to write, end of transfer
					if (data.length == 0) { 
						ack = createAck(bn);   // create ACK
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
							System.out.println("Client: Writing data to file: " + 
									filename);
						} else {
							// create and send error response packet for "Disk full or allocation exceeded."
							byte[] error = createError(3, "File (" + filename 
									+ ") too large for disk.");
							send(error, addr, port);
							return;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("Client: Received DATA packet out of order: Not writing to file.");
					data = new byte[MAX_DATA]; // so we don't quit yet
				}
				ack = createAck(bn);   // create ACK
				send(ack, addr, port);          // send ACK
			} else if (op == 5) { // ERROR received instead of DATA
				parseError(dataPacket);         // print ERROR info
				return;
			} else {
				// create and send error response packet for "Illegal TFTP operation."
				byte[] error = createError(4, "Was expecting DATA packet.");
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
				int op = twoBytesToInt(dataPacket[0], dataPacket[1]); // get opcode
				
				if (op == 3) {        // received DATA	
					int bn = twoBytesToInt(dataPacket[2], dataPacket[3]); // get block number
					ack = createAck(bn);   // create ACK
					send(ack, addr, port);          // send ACK
				} else if (op == 5) { // ERROR received instead of DATA
					parseError(dataPacket);         // print ERROR info
					System.out.println("\nClient: RRQ File Transfer Complete.");
					return;
				} else {
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError(4, "Was expecting DATA packet.");
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
			byte[] error = createError(2, "File (" + filename + 
					") exists on client, but is not readable.");
			send(error, addr, port);
			return;
		}		
			
		byte[] read = new byte[MAX_DATA];  // to hold bytes read
		int bytes = 0;                     // number of bytes read
		int blockNumber = 1;              // DATA block number
		
		// read up to 512 bytes from file
		try {			
			while ((bytes = in.read(read)) != -1) {
				System.out.println("Client: Read " + bytes + " bytes, from " 
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
								System.out.println("Client: Socket Timeout: Resending DATA and continuing to wait for ACK...");
								timedOut = true;  // have timed out once on this packet
								send(data, addr, port); // send DATA
							} else {
								// have timed out a second time
								System.out.println("Client: Socket Timeout Again: Aborting file transfer:");
								System.out.println("Client: You may try again, but the Server may not be running...");
								try {
									in.close(); // close buffered reader after WRQ
								} catch (IOException e) { } 
								return;
							}
						}		
					}
				
					// invalid packet received
					if (receivePacket == null) {
						System.out.println("Client: Invalid packet received: Aborting file transfer:");
						System.out.println("Client: You may try to send another request...");
						try {
							in.close(); // close buffered reader after WRQ
						} catch (IOException e) { } 
						return;
					}
				
					byte[] ackPacket = processDatagram(receivePacket);  // read the expected ACK
					int op = twoBytesToInt(ackPacket[0], ackPacket[1]); // get opcode
					
					if (op == 5) {                            // ERROR received instead of ACK
						parseError(ackPacket);	// print ERROR info and close connection
						System.out.println("Client: Aborting Transfer.");
						try {
							in.close(); // close buffered reader after WRQ
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
								System.out.println("Client: Received Duplicate ACK: Ignoring and waiting for correct ACK...");
							} else if (bn == blockNumber) {
								break;  // got ACK with correct block number, continuing
							} else { // ACK with weird block number 
								// create and send error response packet for "Illegal TFTP operation."
								byte[] error = createError(4, "Received ACK with invalid block number.");
								send(error, addr, port);
								try {
									in.close(); // close buffered reader after WRQ
								} catch (IOException e) { } 
								return;		
							} 
						} else {
							if (bn > blockNumber){ // duplicate ACK
								System.out.println("Client: Received Duplicate ACK: Ignoring and waiting for correct ACK...");
							} else {
								if (bn == blockNumber) {
									break;  // got ACK with correct block number, continuing
								} else { // ACK with weird block number 
									// create and send error response packet for "Illegal TFTP operation."
									byte[] error = createError(4, "Received ACK with invalid block number.");
									send(error, addr, port);
									try {
										in.close(); // close buffered reader after WRQ
									} catch (IOException e) { } 
									return;		
								}
							}
						}
					} else {
						// create and send error response packet for "Illegal TFTP operation."
						byte[] error = createError(4, "Expected ACK as response.");
						send(error, addr, port);
						try {
							in.close(); // close buffered reader after WRQ
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
		} catch (FileNotFoundException e) {
			// create and send error response packet for "File not found."
			byte[] error = createError((byte)1, "File (" + filename + 
					") does not exist.");
			send(error, addr, port);
			try {
				in.close(); // close buffered reader after WRQ
			} catch (IOException e1) { } 
			return;		
		} catch (IOException e) {
			System.out.println("Error: could not read from BufferedInputStream.");
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
							System.out.println("Client: Socket Timeout: Resending DATA and continuing to wait for ACK...");
							timedOut = true;  // have timed out once on this packet
							send(data, addr, port); // send DATA
						} else {
							// have timed out a second time
							System.out.println("Client: Socket Timeout Again: Aborting file transfer:");
							System.out.println("Client: You may try again, but the Server may not be running...");
							try {
								in.close(); // close buffered reader after WRQ
							} catch (IOException e) { } 
							return;
						}
					}		
				}
			
				// invalid packet received
				if (receivePacket == null) {
					System.out.println("Client: Invalid packet received: Aborting file transfer:");
					System.out.println("Client: You may try to send another request...");
					try {
						in.close(); // close buffered reader after WRQ
					} catch (IOException e) { } 
					return;
				}
			
				byte[] ackPacket = processDatagram(receivePacket);  // read the expected ACK
				int op = twoBytesToInt(ackPacket[0], ackPacket[1]); // get opcode
				
				if (op == 5) {                            // ERROR received instead of ACK
					parseError(ackPacket);	// print ERROR info and close connection
					System.out.println("Client: Aborting Transfer.");
					try {
						in.close(); // close buffered reader after WRQ
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
							System.out.println("Client: Received Duplicate ACK: Ignoring and waiting for correct ACK...");
						} else if (bn == blockNumber) {
							break;  // got ACK with correct block number, continuing
						} else { // ACK with weird block number 
							// create and send error response packet for "Illegal TFTP operation."
							byte[] error = createError(4, "Received ACK with invalid block number.");
							send(error, addr, port);
							try {
								in.close(); // close buffered reader after WRQ
							} catch (IOException e) { } 
							return;		
						}
					} else {
						if (bn > blockNumber){ // duplicate ACK
							System.out.println("Client: Received Duplicate ACK: Ignoring and waiting for correct ACK...");
						} else if (bn == blockNumber) {
							break;  // got ACK with correct block number, continuing
						} else { // ACK with weird block number 
							// create and send error response packet for "Illegal TFTP operation."
							byte[] error = createError(4, "Received ACK with invalid block number.");
							send(error, addr, port);
							try {
								in.close(); // close buffered reader after WRQ
							} catch (IOException e) { } 
							return;		
						}
					}
				} else {
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError(4, "Expected ACK as response.");
					send(error, addr, port);
					try {
						in.close(); // close buffered reader after WRQ
					} catch (IOException e) { } 
					return;		
				}
			}
		}			
		/* done sending last packet */
		System.out.println("\nClient: WRQ File Transfer Complete.");
		try {
			in.close(); // close buffered reader after WRQ
		} catch (IOException e) { } 
	}
	
	/**
	 * Creates the request byte[] to be later sent as a DatagramPacket.
	 * 
	 * @param opcode	differentiates between read (1) and write (2) requests
	 * @param filename	name of file to be sent/requested to/from server
	 * @param mode		mode of file transfer in TFTP
	 * @return			the read/write request byte[]
	 */
	public static byte[] createRequest(int opcode, String filename, String mode) 
	{
		byte data[]=new byte[filename.length() + mode.length() + 4];
		
		// add opcode
		data[0] = (byte)(opcode / 256);
		data[1] = (byte)(opcode % 256);
		
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
		
		return req + filename + "\"   mode=\"" + mode + "\"";
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
	 * @param addr	InetAddress to send DatagramPacket to 
	 * @param port	port number to send DatagramPacket to
	 */
	public void send (byte[] data, InetAddress iAddr, int portNum) 
	{
		// create new DatagramPacket to send to server
		sendPacket = new DatagramPacket(data, data.length, iAddr, portNum);
		
		// type of packet being sent 
		int op = twoBytesToInt(data[0], data[1]);
		String type = "";
		switch (op) {
			case 1: case 2: type = parseRequest(data); 
				break;
			case 3: type = parseData(data);
				break;
			case 4: type = parseAck(data);
				break;
			case 5: type = parseError(data);
				break;
			default: 
				break;
		}
		
		// send the DatagramPacket to the server via the send/receive socket
		try {			
			// attempt to send DatagramPacket
			sendReceiveSocket.send(sendPacket);
			// print out packet info
			String direction = "-->";
			System.out.printf("Client: %30s %3s %-30s   bytes: %3d   - %s \n", 
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
			
			// checks if the received packet is a valid TFTP packet
			if (!isValidPacket(packet)) {
				// error is sent from within isValidPacket method
				return null;
			// valid packet
			} else {
				
				byte[] packetData = packet.getData();  // the received packet's data
				int op = twoBytesToInt(packetData[0], packetData[1]); // get the opcode
				String type = "";  // type of packet
				
				// get packet info based on type of TFTP packet
				switch (op) {
					case 1: case 2: type = parseRequest(packetData);
						break;
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
				System.out.printf("Client: %30s %3s %-30s   bytes: %3d   - %s \n", 
						sendReceiveSocket.getLocalSocketAddress(), direction, packet.getSocketAddress(),
						packet.getLength(), type);
				
				// check for wrong transfer ID 
				if (!((packet.getAddress().equals(addr)) && 
						(packet.getPort() == port))) {				
					// create and send error response packet for "Unknown transfer ID."
					byte[] error = createError(5, 
							"Your packet was sent to the wrong place.");
					send(error, packet.getAddress(), packet.getPort());
					
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
		byte[] data = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), packet.getOffset(), data, 0, 
				packet.getLength());
		
		return data;
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
	 * Determines if the packet is a valid TFTP packet.
	 * 
	 * @param received	the received DatagramPacket to be verified
	 * @return			true if valid packet, false if invalid
	 */
	public boolean isValidPacket (DatagramPacket received) 
	{
		byte[] data = received.getData();         // get data from received packet
		int len = received.getLength();				       // number of data bytes in packet
		
		// check size of packet
		if (len < 4) {
			// create and send error response packet for "Illegal TFTP operation."
			byte[] error = createError(4, "Packet is less than 4 bytes in size.");
			send(error, addr, port);
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
					send(error, addr, port);
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
					send(error, addr, port);
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
					send(error, addr, port);
					return false;
				}
				break;
			case 3:								// DATA packet
				if (len > MAX_DATA + 4) {
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError(4, "Data packet is too large.");
					send(error, addr, port);
					return false;
				}
				break;
			case 4:								// ACK packet
				if (len != 4) { 
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError(4, "Ack packet is not 4 bytes in size.");
					send(error, addr, port);
					return false; 
				}
				break;
			case 5:								// ERROR packet
				if (data[len - 1] != 0) {
					// create and send error response packet for "Illegal TFTP operation."
					byte[] error = createError(4, "Error message is not null terminated.");
					send(error, addr, port);
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
				send(error, addr, port);
				return false; 			// not a valid error code
			default: 
				// create and send error response packet for "Illegal TFTP operation."
				error = createError(4, "Invalid TFTP opcode.");
				send(error, addr, port);
				return false;					// invalid opcode
		}		
		return true;
	}
}
