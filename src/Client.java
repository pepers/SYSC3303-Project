
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
	 * Creates a new Client, starts the user interface, and continues a connection
	 * to the Server or ErrorSim.
	 * 
	 * @param args			command line arguments
	 * @throws Exception	 
	 */
	public static void main (String args[]) throws Exception {
		Client c = new Client();
		System.out.println("***** Welcome to Group #2's SYSC3303 TFTP Client Program *****\n");
		c.ui();	// start the user interface to send request
		c.connection();	// receive and send packets with Server or ErrorSim
	}
	
	/**
	 * The simple console text user interface for the client program.  User navigates 
	 * through menus to send request datagram packet.
	 * @throws FileNotFoundException 
	 * 
	 */
	public void ui() throws FileNotFoundException {		
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
		else //if file name was not recognized.
		{
			op = Opcode.ERROR;
			createError((byte)1,(filename+" does not exist").getBytes()); //send error params 
			throw new FileNotFoundException("File not found: "+filename); //throw exception 
			
		}
		byte[] request = createRequest(op.op(), filename, mode);	// get the request byte[] to send
		
		// send request to correct port destination
		try {
			send(request, InetAddress.getLocalHost(), dest);
		} catch (UnknownHostException e) {
			System.out.println("\nClient: Error, InetAddress could not be found. Shutting Down...");
			System.exit(1);			
		}
	}
	
	/**
	 * Continues connection with Server or ErrorSim, to transfer datagram packets.
	 * 
	 * @throws Exception 
	 */
	public void connection () throws Exception {
		DatagramPacket datagram = receive();	// gets received DatagramPacket
		byte[] received = process(datagram);	// received packet turned into byte[]
		
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
				received = process(datagram);							// received packet turned into byte[]
				
				// check response 
				if (received[1] == Opcode.ACK.op()) {			// deal with received ACK
					parseAck(received);		
					if (data.length < (MAX_DATA + 4)) {	// done sending file
						break; 				
					}	
				} else if (received[1] == Opcode.ERROR.op()) {	// deal with ERROR
					byte ErrorCode = parseError(received);	
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
				writeToFile(filename, data);	// write the received data to file
				// create and send ACK packet
				byte[] ack = createAck(received[3]);
				send(ack, datagram.getAddress(), datagram.getPort());
				if(data.length < MAX_DATA) {	// if last DATA packet was received
					break;
				}
				datagram = receive();			// gets received DatagramPacket
				received = process(datagram);	// received packet turned into byte[]
				if (received[1] == Opcode.ERROR.op()) {			// deal with ERROR
					byte ErrorCode = parseError(received);	
				} else if (received[1] != Opcode.DATA.op()) {	// deal with malformed packet
					throw new Exception ("Improperly formatted packet received.");
				}
			} while (!(data.length < MAX_DATA));
			
		// Error packet received	
		} else if (received[1] == Opcode.ERROR.op()) {	
			byte ErrorCode = parseError(received);
			
		} else {
			throw new Exception ("Improperly formatted packet received.");
		}
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
	  public byte[] createError(byte errorCode,byte[]errMsg)
	  {
		   byte error[] = new byte[50];
		   if(errorCode == (byte)1) //file not found
		   {
			   error[0] = (byte)1;
			   System.arraycopy(errMsg,0,error,1,errMsg.length); //create one byte array containing the error message and code
		   }
		   else if(errorCode == (byte)2) // access violation
		   {
			   
		   }
		   else if(errorCode == (byte)3) //disk full 
		   {
			   
		   }
		   else //file already exists
		   {
			   
		   }
		   return error; //return to be sent as an error message
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
	 * @param data	the DATA byte[]
	 * @return		just the data portion of a DATA packet byte[]
	 */
	public byte[] parseData (byte[] data) {
		// TODO return byte[] with just the data portion
		return null;
	}
	
	/**
	 * Parse the error byte[] and display info to user.
	 * 
	 * @param error	the error byte[]
	 * @return 		the TFTP Error Code byte value
	 */
	public byte parseError (byte[] error) {
		// TODO return Error Code byte value
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
		// TODO
	}
	
	/**
	 * Receives DatagramPacket.
	 * 
	 * @return DatagramPacket received
	 */
	public DatagramPacket receive () {
		// TODO return DatagramPacket that was received
		return null;
	}
	
	/**
	 * Gets byte[] from DatagramPacket.
	 * 
	 * @param receivePacket	DatagramPacket received
	 * @return				byte[] containing the data from the DatagramPacket
	 */
	public byte[] process (DatagramPacket receivePacket) {
		// TODO return byte[] contained in received DatagramPacket
		return null;
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
