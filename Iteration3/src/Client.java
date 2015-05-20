
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
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
 * @version	2
 */
public class Client {
	
	DatagramPacket sendPacket;										// to send data to server 
	DatagramPacket receivePacket;									// to receive data in
	DatagramSocket sendReceiveSocket;								// to send to and receive packets from server
	private static Scanner input;									// scans user input in the simple console ui()
	String filename = "test0.txt";									// the file to be sent/received
	public static final String fileDirectory = "files\\client\\";	// directory for test files
	String mode = "octet";											// the mode in which to send/receive the file
	private BufferedInputStream in;									// input stream to read data from file
	public static final int MAX_DATA = 512;							// max number of bytes for data field in packet
	
	/**
	 * opcodes for the different DatagramPackets in TFTP
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
	 * Creates a new Client, starts the user interface, and continues a connection
	 * to the Server or ErrorSim.
	 * 
	 * @param args			command line arguments
	 * @throws Exception	 
	 */
	public static void main (String args[]) throws Exception {
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
	public void ui() {		
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
					System.out.println("\nClient: You have chosen the file: " + filename + 
							", to be received in " + mode + " mode.");	
					break;
				} else{														// file already exists
					System.out.println("\nClient: I'm sorry, " + fileDirectory + filename + " already exists:");
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
				if (Files.isWritable(Paths.get(fileDirectory + filename))) {	// file exists and is writable
					System.out.println("\nClient: You have chosen the file: " + fileDirectory + filename + ", to be sent in " + 
							mode + " mode.");
					break;
				} else {														// file does not exist
					System.out.println("\nClient: I'm sorry, " + fileDirectory + filename + " does not exist:");
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
		
		byte[] request = createRequest(op.op(), filename, mode);	// get the request byte[] to send
				
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
	 * @throws Exception 
	 */
	public void connection () throws Exception {
		DatagramPacket datagram = receive();			// gets received DatagramPacket
		byte[] received = processDatagram(datagram);	// received packet turned into byte[]
		
		// parse received packet, based on opcode
		// Acknowledge packet received (response to WRQ)
		if (received[1] == Opcode.ACK.op()) {			
			parseAck(received);						// parse the acknowledgment and print info to user
			byte[] fileData = new byte[MAX_DATA];	// data to read in from file
			byte blockNumber = 1;					// DATA block number
			
			// reads the file in 512 byte chunks
			try {
				// stream to read data from file
				in = new BufferedInputStream(new FileInputStream(fileDirectory + filename));
				int bytes = 0;	// number of bytes read from file
				while ((bytes = in.read(fileData)) != -1) {
					System.out.println("\nClient: Read " + bytes + " bytes, from " + fileDirectory + filename);
					
					// get rid of extra buffer
					byte[] temp = new byte[bytes];
					System.arraycopy(fileData, 0, temp, 0, bytes);
					fileData = temp;
					System.out.println(Arrays.toString(fileData));
					
					byte[] data = createData(blockNumber, fileData);		// create DATA packet
					send(data, datagram.getAddress(), datagram.getPort());	// send DATA packet
					datagram = receive();									// gets received DatagramPacket
					received = datagram.getData();							// received packet turned into byte[]
					
					// check response 
					if (received[1] == Opcode.ACK.op()) {			// deal with received ACK
						parseAck(received);		
						if (data.length < (MAX_DATA + 4)) {	// done sending file
							return;				
						}	
					} else if (received[1] == Opcode.ERROR.op()) {	// deal with ERROR
						parseError(received);
						return;
					} else {										// deal with malformed packet
						throw new Exception ("Improperly formatted packet received.");
					}
					
					blockNumber++;	// increase blockNumber for next DATA packet to be sent
					// blockNumber goes from 0-127, and then wraps to back to 0
					if (blockNumber < 0) { 
						blockNumber = 0;
					}
				}	
			} catch (FileNotFoundException e) {
				// create and send error response packet for "File not found."
				byte[] error = createError((byte)1, "File (" + filename + ") does not exist.");
				send(error, datagram.getAddress(), datagram.getPort() );
				return;	// stop transfer
			} catch (IOException e) {
				System.out.println("\nError: could not read from BufferedInputStream.");
				System.exit(1);
			}			
			return;	// done transferring file
		// Data packet received (response to RRQ)	
		} else if (received[1] == Opcode.DATA.op()) {	
			byte[] data = null;	// new byte[] to hold data portion of DATA packet
			
			// do while there is still another DATA packet to receive
			while (true) {
				data = parseData(received);		// parse the DATA packet and print info to user
				try {
					writeToFile(fileDirectory + filename, data);	// write the received data to file
				} catch (IOException e) {
					// create and send error response packet for "Access violation."
					byte[] error = createError((byte)2, "File (" + filename + ") can not be written to.");
					send(error, datagram.getAddress(), datagram.getPort());	// send ERROR packet
					return;
				}
				
				// create and send ACK packet
				byte[] ack = createAck(received[3]);
				send(ack, datagram.getAddress(), datagram.getPort());
				if(data.length < MAX_DATA) {	// if last DATA packet was received
					break;
				}
				datagram = receive();					// gets received DatagramPacket
				received = processDatagram(datagram);	// received packet turned into byte[]
				if (received[1] == Opcode.ERROR.op()) {			// deal with ERROR
					parseError(received);	
					return;
				} else if (received[1] != Opcode.DATA.op()) {	// deal with malformed packet
					throw new Exception ("Improperly formatted packet received.");
				}
			}
			
		// Error packet received	
		} else if (received[1] == Opcode.ERROR.op()) {	
			parseError(received);	
			return;

		}
		else {
			throw new Exception ("Improperly formatted packet received.");
		}
	}
	
	/**
	 * Creates the request byte[] to be later sent as a DatagramPacket.
	 * 
	 * @param opcode	differentiates between read (1) and write (2) requests
	 * @param filename	name of file to be sent/requested to/from server
	 * @param mode		mode of file transfer in TFTP
	 * @return			the read/write request byte[]
	 */
	public static byte[] createRequest(byte opcode, String filename, String mode) {
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
	public byte[] createAck (byte blockNumber) {
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
	 * @param data			the data to be sent
	 * @return				the data byte[]
	 */
	public byte[] createData (byte blockNumber, byte[] data) {
		byte[] temp = new byte[4+data.length];
		temp[0] = 0;
		temp[1] = 3;
		temp[2] = 0;
		temp[3] = blockNumber;
		for(int i=4; i < data.length; i++) {
			temp[i] = data[i];
		}
		return temp;
	}
	
	/**
	 * Creates the byte[] to be sent as an error DatagramPacket.
	 * 
	 * @param errorCode	the code signifying what type of error
	 * @param errorMsg	the message string that will give more detail on the error
	 * @return			the error byte[]
	 */
	public byte[] createError (byte errorCode, String errorMsg) {
		byte[] error = new byte[4 + errorMsg.length() + 1];	// new error to eventually be sent to server
		
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
	public void parseAck (byte[] ack) {
		System.out.println("\nClient: Recieved packet is ACK: ");
		System.out.println("Block#: " + ack[2] + ack[3]);
	}
	
	/**
	 * Parse the data byte[] and display info to user.
	 * 
	 * @param data	the DATA byte[]
	 * @return		just the data portion of a DATA packet byte[]
	 */
	public byte[] parseData (byte[] data) {
		// byte[] for the data portion of DATA packet byte[]
		byte[] justData = new byte[data.length - 4];	
		System.arraycopy(data, 4, justData, 0, data.length-4);
		
		// print info to user
		System.out.println("\nClient: Recieved packet is DATA: ");
		System.out.println("Block#: " + data[2] + data[3] + ", and containing data: ");
		System.out.println(Arrays.toString(justData));
		
		return justData;
	}
	
	/**
	 * Parse the error byte[] and display info to user.
	 * 
	 * @param error	the error byte[]
	 * @return 		the TFTP Error Code byte value
	 */
	public void parseError (byte[] error) {
		System.out.println("\nClient: Recieved packet is ERROR: ");		

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
		if (errorCode == 0) {
			System.out.println("Error Code: 00: Not defined, see error message (if any). ");
		} else if (errorCode == 1) {
			System.out.println("Error Code: 01: File not found. ");
		} else if (errorCode == 2) {
			System.out.println("Error Code: 02: Access violation. ");
		} else if (errorCode == 3) {
			System.out.println("Error Code: 03: Disk full or allocation exceeded. ");
		} else if (errorCode == 6) {
			System.out.println("Error Code: 06: File already exists. ");
		} else {
			System.out.println("Error Code: " + errorCode);
		}
		
		// display error message to user
		System.out.println("Error message:" + message);
	}
	
	/**
	 * Sends DatagramPacket.
	 * 
	 * @param data	data byte[] to be included in DatagramPacket
	 * @param addr	internet address to send DatagramPacket to 
	 * @param port	port number to send DatagramPacket to
	 */
	public void send (byte[] data, InetAddress addr, int port) {
		sendPacket = new DatagramPacket(data, data.length, addr, port);

		System.out.println("\nClient: Sending packet:");
		System.out.println("To host: " + sendPacket.getAddress() + " : " + sendPacket.getPort());
		System.out.print("Containing " + sendPacket.getLength() + " bytes: \n");
		System.out.println(Arrays.toString(data) + "\n"); 
		
		// send the DatagramPacket to the server via the send/receive socket
		try {
			sendReceiveSocket.send(sendPacket);
			System.out.println("Client: Packet sent");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}		
	}
	
	/**
	 * Receives DatagramPacket.
	 * 
	 * @return DatagramPacket received
	 */
	public DatagramPacket receive () {
		// no packet will be larger than DATA packet
		// room for a possible maximum of 512 bytes of data + 4 bytes opcode and block number
		byte data[] = new byte[MAX_DATA + 4];
		receivePacket = new DatagramPacket(data, data.length);
		
		try {
			// block until a DatagramPacket is received via sendReceiveSocket
			sendReceiveSocket.receive(receivePacket);
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		System.out.println("\nClient: DatagramPacket received:");
		System.out.println("From host: " + receivePacket.getAddress() + " : " + receivePacket.getPort());
		System.out.print("Containing " + receivePacket.getLength() + " bytes");
		
		return receivePacket;
	}
	
	/**
	 * Makes an appropriately sized byte[] from a DatagramPacket
	 * 
	 * @param packet	the received DatagramPacket
	 * @return			the data from the DatagramPacket
	 */
	public byte[] processDatagram (DatagramPacket packet) {
		byte[] data = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
		
		// display info to user
		System.out.println("\n" + Arrays.toString(data));
		
		return data;
	}

	/**
	 * Writes the received data to a file.
	 * 
	 * @param filename	name of file to write data to
	 * @param data		data to be written to file			
	 * @throws IOException 
	 */
	public void writeToFile (String filename, byte[] data) throws IOException {
		// if data received was not an empty block
		if (!(data.length < MAX_DATA)) {
			Files.write(Paths.get(filename), data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			System.out.println("\nClient: reading data to file: " + filename);
		} else {
			System.out.println("\nClient: receiving " + filename + " complete");
		}
	}
}
