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
				System.out.println("Waiting...");
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				System.out.print("IO Exception: likely:");
				System.out.println("Receive Socket Timed Out.\n" + e);
				e.printStackTrace();
				System.exit(1);
			}
			
			// check for valid TFTP request packet
			if (data[0] != 0) { // invalid Opcode
				System.out.println("\nServer: Invalid Opcode Received:");
				break;
			} else {
				byte op = data[1];
				switch (op) {
					// valid request
					case 1: case 2:
						System.out.println("\nServer: Valid Reqest Received:");					
						// create new thread to communicate with Client and transfer file
						// pass it datagram that was received
						Thread clientConnectionThread = new Thread(
								new ClientConnection(receivePacket));
						clientConnectionThread.start();	
						System.out.println("\nServer: Packet Sent for Processing:");
						break;
					// invalid request
					default:
						System.out.println("\nServer: Invalid Request Received:");
						break;
				}
			}
		} // end of while
	}
}

class ClientConnection implements Runnable {
	// requests we can receive
	public static enum Opcode {RRQ, WRQ}; 
	
	DatagramPacket receivePacket;
	DatagramPacket sendPacket;
	DatagramSocket sendSocket;
	

	public ClientConnection(DatagramPacket receivePacket) {
		// pass in the received datagram packet from the Server
		// in order to facilitate file trasfers with the Client
		this.receivePacket = receivePacket;
	}
	
	public void run() {
		// TODO Auto-generated method stub
		
	}	
}
