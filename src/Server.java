
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
	public enum Opcode { RRQ, WRQ, ACK, DATA, ERROR }
	
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
			DatagramPacket datagram = null;			// DatagramPacket to eventually receive
			datagram = receive();					// gets received DatagramPacket
			byte[] request = datagram.getData();	// received request packet turned into byte[]
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
				System.out.println("\nServer: Goodbye!");
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
	 * @throws SocketException 
	 */
	public DatagramPacket receive() throws SocketException {
		byte data[] = new byte[100]; 
		DatagramPacket receivePacket = new DatagramPacket(data, data.length);
		
		while (true){
			try {
				// block until a DatagramPacket is received via sendReceiveSocket 
				receiveSocket.receive(receivePacket);
				// print out thread and port info, from which the packet was sent to Client
				System.out.println("\nServer: packet received: ");
				System.out.println("From host: " + receivePacket.getAddress() + " : " + receivePacket.getPort());
				System.out.print("Containing " + receivePacket.getLength() + " bytes: \n");
				System.out.println(Arrays.toString(data));
				break;
			} catch (SocketTimeoutException e) {	// haven't received packet in 5 seconds
				serverQuit();	// find out if user wants to quit, if not while loop will re-try
			} catch(IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		return receivePacket;
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
	InetAddress addr;						// InetAddress of client that sent request
	int port;								// port number of client that sent request
	
	private BufferedInputStream in;			// stream to read from file when RRQ received
	
	// ReadWriteLock in case multiple threads try to read/write from/to the same file
	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final Lock read  = readWriteLock.readLock();
	private final Lock write = readWriteLock.writeLock();
	
	public ClientConnection(DatagramPacket requestPacket) {
		this.requestPacket = requestPacket;			// get request DatagramPacket
		this.op = getOpcode(requestPacket);			// get opcode from request packet
		this.filename = getFilename(requestPacket);	// get filename from request packet
		this.addr = requestPacket.getAddress();		// get the InetAddress of client
		this.port = requestPacket.getPort();		// get the port number of client
		
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
			read.lock();		// gets read lock
			try {
				if (Files.exists(Paths.get(filename))) {			// file exists
					if (Files.isReadable(Paths.get(filename))) {	// file is readable
						byte blockNumber = 1;		// block number for DATA during transfer
						byte[] read = new byte[0];	// to hold received data portion of DATA packet
						int offset = 0;				// position to start reading file at
						// do while there is more data to read from the file
						do {
							read = readFromFile(offset);					// up to 512 bytes read from file
							offset = read.length;							// increase position in file
							byte[] data = createData(blockNumber, read);	// create DATA packet of file being read
							System.out.println("\n" + Thread.currentThread() + ": Sending DATA...");
							send(data);										// send DATA
							blockNumber++;									// increment DATA block number
							// blockNumber goes from 0-127, and then wraps to back to 0
							if (blockNumber < 0) { blockNumber = 0; }	
						} while (read.length > 0);
						// check if file was a multiple of 512 bytes in size, send 0 byte DATA
						if (read.length == MAX_DATA) {
							read = new byte[0];								// create 0 byte read file data
							byte[] data = createData(blockNumber, read);	// create 0 byte DATA 
							System.out.println("\n" + Thread.currentThread() + ": Sending DATA...");
							send(data);										// send 0 byte DATA
						}
					} else {										// file is not readable
						// create and send error response packet for "Access violation."
						byte[] error = createError((byte)2, "File (" + filename + ") exists on server, but is not readable.");
						System.out.println("\n" + Thread.currentThread() + ": Sending ERROR...");
						send(error);
						closeConnection();	// quit client connection thread
					}
				} else {											// file does not exist
					// create and send error response packet for "File not found."
					byte[] error = createError((byte)1, "File (" + filename + ") does not exist.");
					System.out.println("\n" + Thread.currentThread() + ": Sending ERROR...");
					send(error);
					closeConnection();	// quit client connection thread
				}
			} finally {
				read.unlock();	// gives up read lock
			}
		} else if (op == Server.Opcode.WRQ) {	// received a WRQ
			read.lock();		// gets read lock
			try {
				if (Files.exists(Paths.get(filename))) {	// file exists
					// create and send error response packet for "File already exists."
					byte[] error = createError((byte)6, "File (" + filename + ") already exists on server.");
					System.out.println("\n" + Thread.currentThread() + ": Sending ERROR...");
					send(error);
					closeConnection();	// quit client connection thread
				} else {									// file does not exist
					byte blockNumber = 0;					// block number for ACK and DATA during transfer
					byte[] ack = createAck(blockNumber);	// create initial ACK
					System.out.println("\n" + Thread.currentThread() + ": Sending ACK...");
					send(ack);								// send initial ACK
					byte[] data = new byte[0];				// to hold received data portion of DATA packet
					do {	// DATA transfer from client
						DatagramPacket receivePacket = receive();		// receive the DatagramPacket
						byte[] dataPacket = receivePacket.getData();	// read the DatagramPacket
						blockNumber = dataPacket[1];					// get the data block number
						data = parseData(dataPacket);					// get data from packet
						try {
							writeToFile(data);							// write data to file
						} catch (IOException e) {
							e.printStackTrace();
						}							
						ack = createAck(blockNumber);				// create ACK
						System.out.println("\n" + Thread.currentThread() + ": Sending ACK...");
						send(ack);									// send ACK
					} while (data.length < MAX_DATA);
				}
			} finally {
				read.unlock();	// gives up read lock
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
		// byte[] to copy packet data into
		byte[] received = new byte[1];	
		System.arraycopy(requestPacket.getData(), 1, received, 0, 1);
		
		Server.Opcode opcode = null;	// opcode to return
		
		// determine opcode of request packet
		if (received[0] == (byte)1) {
			opcode = Server.Opcode.RRQ;
		} else if (received[0] == (byte)2) {
			opcode = Server.Opcode.WRQ;
		}
		
		return opcode;
	}
	
	/**
	 * Gets the filename from the request packet.
	 * 
	 * @param requestPacket	the DatagramPacket that was received by the server.
	 * @return				the filename from the request packet as a String
	 */
	public String getFilename(DatagramPacket requestPacket) {
		// byte[] to copy packet data into
		byte[] received = new byte[requestPacket.getLength()];	
		System.arraycopy(requestPacket.getData(), 0, received, 0, requestPacket.getLength());
		
		// find the end of filename
		int end = 2;	// end index of filename bytes
		for (int i = 2; i < received.length - 1; i++) {
			if  (received[i] == (byte)0) {
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
	public void send (byte[] data) {
		// create new DatagramPacket to send to client
		sendPacket = new DatagramPacket(data, data.length, addr, port);
		
		// send the packet
		try {
			sendReceiveSocket.send(sendPacket);
			// print out thread and port info, from which the packet was sent to Client
			System.out.println("\n" + Thread.currentThread() + ": packet sent using port " + 
					sendReceiveSocket.getLocalPort());
			// print byte info on packet being sent to Client
			System.out.print("Containing " + sendPacket.getLength() + " bytes: \n");
			System.out.println(Arrays.toString(data));
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
	public DatagramPacket receive() {
		byte data[] = new byte[100]; 
		DatagramPacket receivePacket = new DatagramPacket(data, data.length);
		
		try {
			// block until a DatagramPacket is received via sendReceiveSocket 
			sendReceiveSocket.receive(receivePacket);
			// print out thread and port info, from which the packet was sent to Client
			System.out.println("\n" + Thread.currentThread() + ": packet received: ");
			System.out.println("From host: " + receivePacket.getAddress() + " : " + receivePacket.getPort());
			System.out.print("Containing " + receivePacket.getLength() + " bytes: \n");
			System.out.println(Arrays.toString(data));
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		return receivePacket;
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
	 * Creates the byte[] to be sent as an ACK DatagramPacket.
	 * 
	 * @param blockNumber	the data block number that is being acknowledged
	 * @return				the acknowledgment byte[]
	 */
	public byte[] createAck (byte blockNumber) {
		byte[] ack = new byte[4];	// new byte[] to be sent in ACK packet
		
		// add opcode
		ack[0] = (byte)0;
		ack[1] = (byte)4;
				
		// add block number
		ack[2] = (byte)0;
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
	public byte[] createData (byte blockNumber, byte[] passedData) {
		byte[] data = new byte[4 + passedData.length]; // new byte[] to be sent in DATA packet
		
		// add opcode
		data[0] = (byte)0;
		data[1] = (byte)3;
				
		// add block number
		data[2] = (byte)0;
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
	public byte[] createError (byte errorCode, String errorMsg) {
		byte[] error = new byte[4 + errorMsg.length() + 1];	// new error to eventually be sent to client
		
		// add opcode
		error[0] = (byte)0;
		error[1] = (byte)5;
		
		// add error code
		error[2] = (byte)0;
		error[3] = errorCode;
		
		byte[] message = new byte[errorMsg.length()];	// new array for errorMsg
		
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
	 * Writes the received data to a file.
	 * 
	 * @param filename	name of file to write data to
	 * @param data		data to be written to file			
	 * @throws IOException 
	 */
	public void writeToFile (byte[] data) throws IOException {		
		// gets space left on the drive that we can use
		long spaceOnDrive = Files.getFileStore(Paths.get("")).getUsableSpace();	
		
		read.lock();	// gets read lock
		try {
			// checks if there is enough usable space on the disk
			if (spaceOnDrive < filename.length() + 1024) { // +1024 bytes for safety
				write.lock();		// gets write lock
				try {
					// writes data to file (creates file first, if it doesn't exist yet)
					Files.write(Paths.get(filename), data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
					System.out.println("\nClient: reading data to file: " + filename);
				} finally {
					write.unlock();	// gives up write lock
				}
			} else {
				// create and send error response packet for "Disk full or allocation exceeded."
				byte[] error = createError((byte)3, "File (" + filename + ") too large for disk.");
				send(error);
				closeConnection();	// quit client connection thread
			}
		} finally {
			read.unlock();	// gives up read lock
		}
	}
	
	/**
	 * Reads data from file (filename) to byte[] in 512 byte chunks.
	 * 
	 * @return	512 byte chunk of data from file
	 */
	public byte[] readFromFile(int offset) {
		byte[] read = new byte[0]; 	// to hold bytes read
		try {
			in = new BufferedInputStream(new FileInputStream(filename));	// to read from file
			int bytes = in.read(read, offset, 512);	// read up to 512 bytes from file starting at offset
			System.out.println("/n" + Thread.currentThread() + ": Read " + bytes + " bytes, from " + filename);
		} catch (FileNotFoundException e) {
			// create and send error response packet for "File not found."
			byte[] error = createError((byte)1, "File (" + filename + ") does not exist.");
			send(error);
			closeConnection();	// quit client connection thread
		} catch (IOException e) {
			System.out.println("\nError: could not read from BufferedInputStream.");
			System.exit(1);
		}
		return read;	// return bytes read
	}
}
	
	