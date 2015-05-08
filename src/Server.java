import java.io.IOException;
import java.net.*;

public class Server {
	DatagramPacket receivePacket;
	DatagramSocket receiveSocket;

	public static void main(String[] args) {
		// create new thread to wait for and verify TFTP packets
		Server c = new Server();
	    c.receive();
	}
	
	public Server() {
		// create new socket to receive TFTP packets from Client
	    try {         
	    	receiveSocket = new DatagramSocket(69);	         
	    } catch (SocketException se) {
	    	se.printStackTrace();
	        System.exit(1);
	    }   
	}
	
	public void receive() {
		while (true) {
			// prepare for receiving packet
			byte data[] = new byte[100];
			receivePacket = new DatagramPacket(data, data.length);		      
	    	System.out.println("Server: Waiting for Packet.\n");

	    	// block until a datagram packet is received from receiveSocket
	    	try {        
	    		System.out.println("Waiting..."); // so we know we're waiting
	    		receiveSocket.receive(receivePacket);
	    	} catch (IOException e) {
	    		System.out.print("IO Exception: likely:");
	    		System.out.println("Receive Socket Timed Out.\n" + e);
	    		e.printStackTrace();
	    		System.exit(1);
	    	}
	    	
	    	// check for valid TFTP request packet
	    	if (data[1] == 1 && data[0] == 0) { 		// valid RRQ
	    		System.out.println("\nServer: RRQ Received:");
	    		
	    		// create new thread to communicate with Client and transfer file
		    	// pass it datagram that was received
				Thread clientConnectionThread = new Thread(new ClientConnection(data));
				clientConnectionThread.start();
				
	    	} else if (data[1] == 2 && data[0] == 0) {	// valid WRQ
	    		System.out.println("\nServer: WRQ Received:");
	    		
	    		// create new thread to communicate with Client and transfer file
		    	// pass it datagram that was received
				Thread clientConnectionThread = new Thread(new ClientConnection(data));
				clientConnectionThread.start();
				
	    	} else {									// invalid packet
	    		System.out.println("\nServer: Invalid Request Packet Received:");
	    	} 
		}
	}
}

class ClientConnection implements Runnable {
	DatagramPacket sendPacket;
	DatagramSocket sendSocket;
	

	public ClientConnection(byte data[]) {
	
	}
	
	public void run() {
		// TODO Auto-generated method stub
		
	}	
}