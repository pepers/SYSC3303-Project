
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
	
	DatagramPacket sendPacket;				// to send data to server 
	DatagramPacket receivePacket;			// to receive data in
	DatagramSocket sendReceiveSocket;		// to send to and receive packets from server
	private static Scanner input;			// scans user input in the simple console ui()
	String filename = "test0.txt";			// the file to be sent/received
	String mode = "octet";					// the mode in which to send/receive the file
	private BufferedInputStream in;			// input stream to read data from file
	public static final int MAX_DATA = 512;	// max number of bytes for data field in packet
	
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
		try
		{
				System.out.println("Please choose a file to modify.  Type in a file name: ");
				filename = input.nextLine();	// user's choice
				
				// deal with user's choice of request
				if (op == Opcode.RRQ) {
					System.out.println("\nClient: You have chosen the file: " + filename + ", to be received in " + 
							mode + " mode.");			
				} else if (op == Opcode.WRQ) {
					System.out.println("\nClient: You have chosen the file: " + filename + ", to be sent in " + 
							mode + " mode.");
				}
				
				byte[] request = createRequest(op.op(), filename, mode);	// get the request byte[] to send
				
				// send request to correct port destination
				try{
					send(request, InetAddress.getLocalHost(), dest);
					
				} catch (UnknownHostException e) {
					System.out.println("\nClient: Error, InetAddress could not be found. Shutting Down...");
					System.exit(1);			
				}
			catch(FileNotFoundException f)
			{
				 createError((byte)1,filename + " does not exist");
			}
		}
	}
	
	/**
	 * Continues connection with Server or ErrorSim, to transfer DatagramPackets.
	 * 
	 * @throws Exception 
	 */
	public void connection () throws Exception {
		DatagramPacket datagram = receive();	// gets received DatagramPacket
		byte[] received = datagram.getData();	// received packet turned into byte[]
		
		in = new BufferedInputStream(new FileInputStream(filename));	// stream to read data from file
		
		// parse received packet, based on opcode
		// Acknowledge packet received (response to WRQ)
		if (received[1] == Opcode.ACK.op()) {			
			parseAck(received);						// parse the acknowledgment and print info to user
			byte[] fileData = new byte[MAX_DATA];	// data to read in from file
			
			// reads the file in 512 byte chunks
			while ((in.read(fileData)) != -1) {
				byte[] data = createData(received[3], fileData);		// create DATA packet
				send(data, datagram.getAddress(), datagram.getPort());	// send DATA packet
				datagram = receive();									// gets received DatagramPacket
				received = datagram.getData();							// received packet turned into byte[]
				
				// check response 
				if (received[1] == Opcode.ACK.op()) {			// deal with received ACK
					parseAck(received);		
					if (data.length < (MAX_DATA + 4)) {	// done sending file
						break; 				
					}	
				} else if (received[1] == Opcode.ERROR.op()) {	// deal with ERROR
					byte errorCode = parseError(received);	
				} else {										// deal with malformed packet
					throw new Exception ("Improperly formatted packet received.");
				}
			}			
			
		// Data packet received (response to RRQ)	
		} else if (received[1] == Opcode.DATA.op()) {	
			byte[] data = null;	// new byte[] to hold data portion of DATA packet
			
			// do while there is still another DATA packet to receive
			do {
				data = parseData(received);		// parse the DATA packet and print info to user
				try {
					writeToFile(filename, data);	// write the received data to file
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
				datagram = receive();			// gets received DatagramPacket
				received = datagram.getData();	// received packet turned into byte[]
				if (received[1] == Opcode.ERROR.op()) {			// deal with ERROR
					byte errorCode = parseError(received);	
				} else if (received[1] != Opcode.DATA.op()) {	// deal with malformed packet
					throw new Exception ("Improperly formatted packet received.");
				}
			} while (!(data.length < MAX_DATA));
			
		// Error packet received	
		} else if (received[1] == Opcode.ERROR.op()) {	
			byte errorCode = parseError(received);
			byte[] error = createError();
			
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
		byte data[]=new byte[100];
		data[1]=opcode;
		byte[] fn;
		fn = filename.getBytes();
		System.arraycopy(fn,0,data,2,fn.length);
		byte[] md;
		md = mode.getBytes();
		System.arraycopy(md,0,data,fn.length+3,md.length);
		int len = fn.length+md.length+4; 
		data[len-1] = 0;
		return data;
	}
	
	/**
	 * Creates the byte[] to be sent as an acknowledgment DatagramPacket.
	 * 
	 * @param blockNumber	the data block number that is being acknowledged
	 * @return				the acknowledgment byte[]
	 */
	public byte[] createAck (int blockNumber) {
		byte[] temp = new byte[4];
		temp[0] = (byte) 0;
		temp[1] = (byte) 4;
		temp[2] = (byte) (blockNumber / 256);
		temp[3] = (byte) (blockNumber % 256);
		return temp;
	}
	
	/**
	 * Creates the byte[] to be sent as a data DatagramPacket.
	 * 
	 * @param blockNumber	the data block number 
	 * @param data			the data to be sent
	 * @return				the data byte[]
	 */
	public byte[] createData (int blockNumber, byte[] data) {
		byte[] temp = new byte[4+data.length];
		temp[0] = (byte) 0;
		temp[1] = (byte) 3;
		temp[2] = (byte) (blockNumber / 256);
		temp[3] = (byte) (blockNumber % 256);
		for(int i=0; i < data.length; i++) {
			temp[i+4] = data[i];
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
	   //must create exceptions for access violation 
	   //create exception for disk full
	   /*create error function must be created here*/
	   /*Error Codes

	   Value     Meaning
	   1         File not found.
	   2         Access violation.
	   3         Disk full or allocation exceeded.
	   6         File already exists.
	   */
	public byte[] createError (byte errorCode, String errorMsg) {
		byte[] error = new byte[4 + errorMsg.length() + 1];	// new error to eventually be sent to server
		
		// add opcode
		error[0] = (byte)0;
		error[1] = (byte)5;
		
		// add error code
		error[2] = (byte)0;
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
		System.arraycopy(receivePacket, 0, ack, 0, (receivePacket.getLength()));
		System.out.println("Recieved Ack with Opcode " + ack[0] + ack[1]);
		
		
	}
	
	/**
	 * Parse the data byte[] and display info to user.
	 * 
	 * @param data	the DATA byte[]
	 * @return		just the data portion of a DATA packet byte[]
	 */
	public byte[] parseData (byte[] data) {
		
		//Copies the bytes from receivePacket starting from position 4(skips the Opcode and block #)
		//Copies that byte array into the data byte array
		System.arraycopy(receivePacket, 4, data, 0, (receivePacket.getLength()-4));
		return data;
	}
	
	/**
	 * Parse the error byte[] and display info to user.
	 * 
	 * @param error	the error byte[]
	 * @return 		the TFTP Error Code byte value
	 */
	public byte parseError (byte[] error) {
		String ErrorMsg = null;

		//Copies the errorcode from the received packet to the error byte array
		//this is just the error code
		System.arraycopy(receivePacket, 2, error, 0, 2);
		System.arraycopy(receivePacket, 4,ErrorMsg,0,(receivePacket.getLength()-5));
		
		System.out.println("Error Code: " + error);
		System.out.println("Error message:"+ErrorMsg);
	
		return (byte)0;
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

	      System.out.println("Client: Sending packet:");
	      System.out.println("To host: " + sendPacket.getAddress());
	      System.out.println("Destination host port: " + sendPacket.getPort());
	      System.out.println("Length: " + sendPacket.getLength());
	      System.out.print("Containing: ");
	      System.out.println(new String(sendPacket.getData())); // or could print "s"

	      // Send the datagram packet to the server via the send/receive socket. 

	      try {
	         sendReceiveSocket.send(sendPacket);
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
		byte data[] = new byte[100]; 
		receivePacket = new DatagramPacket(data, data.length);

	      try {
	         // Block until a datagram is received via sendReceiveSocket.  
	         sendReceiveSocket.receive(receivePacket);
	      } catch(IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	      }
	      
	      System.out.println("Client: Packet received:");
	      System.out.println("From host: " + receivePacket.getAddress());
	      System.out.println("Host port: " + receivePacket.getPort());
	      System.out.println("Length: " + receivePacket.getLength());
	      System.out.print("Containing: ");
	      
	      return receivePacket;
	}

	/**
	 * Writes the received data to a file.
	 * 
	 * @param filename	name of file to write data to
	 * @param data		data to be written to file			
	 * @throws IOException 
	 */
	public void writeToFile (String filename, byte[] data) throws IOException {
		Files.write(Paths.get(filename), data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		System.out.println("\nClient: reading data to file: " + filename);
	}
	  


}
