
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Client {
	
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendReceiveSocket;

	
	public Client() {
		   try {
			   sendReceiveSocket = new DatagramSocket();	// new socket to send requests and receive responses
		   } catch (SocketException se) {   // Can't create the socket.
			   se.printStackTrace();
			   System.exit(1);
		   }
	}

	public static void main(String[] args) {
		// TODO 

	}

	public void ui() {
		// TODO 

	}
	
	public byte[] createRequest(byte opcode, String filename, String mode) {
		// TODO return byte[]		
	}
	
	public byte[] createAck (byte blockNumber) {
		// TODO return byte[4]
	}
	
	public byte[] createData (byte blockNumber, byte[] data) {
		// TODO return byte[]
	}
	
	public byte[] createError (byte errorCode, String errorMsg) {
		// TODO return byte[]
	}
	
	public void processRequest (byte[] request) {
		// TODO 
	}
	
	public void processAck (byte[] ack) {
		// TODO 
	}
	
	public void processData (byte[] data) {
		// TODO 
	}
	
	public void processError (byte[] error) {
		// TODO 
	}

}
