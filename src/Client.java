
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * The Client part of the SYSC3303 TFTP Group Project.
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
	
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendReceiveSocket;

	
	public Client() {
		try {
			// new socket to send requests and receive responses
			sendReceiveSocket = new DatagramSocket();	
		} catch (SocketException se) {   // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		// TODO 

	}
	
	/**
	 * The simple console text user interface for the client program.
	 */
	public void ui() {
		// TODO 

	}
	
	/**
	 * Creates the request byte[] to be later sent as a datagram packet.
	 * 
	 * @param opcode	differentiates between read (1) and write (2) requests
	 * @param filename	name of file to be sent/requested to/from server
	 * @param mode		mode of file transfer in TFTP
	 * @return			the read/write request byte[]
	 */
	public byte[] createRequest(byte opcode, String filename, String mode) {
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
	public byte[] createError (byte errorCode, String errorMsg) {
		// TODO return byte[]
		return null;
	}
	
	/**
	 * Parse the request byte[] and display info to user.
	 * 
	 * @param request	the request byte[]
	 */
	public void parseRequest (byte[] request) {
		// TODO 
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
	 * @param data	the data byte[]
	 */
	public void parseData (byte[] data) {
		// TODO 
	}
	
	/**
	 * Parse the error byte[] and display info to user.
	 * 
	 * @param error	the error byte[]
	 */
	public void parseError (byte[] error) {
		// TODO 
	}
	
	/**
	 * Sends datagram packet.
	 * 
	 * @param data	data byte[] to be included in datagram packet
	 * @param addr	internet address to send datagram packet to 
	 * @param port	port number to send datagram packet to
	 */
	public void send (byte[] data, InetAddress addr, int port) {
		// TODO
	}
	
	/**
	 * Receives datagram packet, and processes it's data into byte[].
	 * 
	 * @return	byte[] containing data received in datagram packet
	 */
	public byte[] receive () {
		// TODO return byte[]
		return null;
	}

}
