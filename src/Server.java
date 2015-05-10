import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Arrays;

public class Server {	
	// requests we can receive
	public static enum Opcode {RRQ, WRQ, DATA, ACK, ERROR}; 
	
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
			
			// to get rid of trailing null bytes from buffer	      
			byte received[] = new byte[receivePacket.getLength()];
			System.arraycopy(data, 0, received, 0, receivePacket.getLength());
			
			// check for valid TFTP request packet
			Opcode op;
			if (data[0] != 0) { // invalid Opcode
				System.out.println("Server: Invalid Opcode Received: \n");
				op = Opcode.ERROR;
				break;
			} else {
				switch (data[1]) {
					case 1: 		
						op = Opcode.RRQ;
						break;
					case 2:
						op = Opcode.WRQ;
						break;
					default: // invalid reqest						
						op = Opcode.ERROR;
						break;
				}
			}
			
			// get filename length
			int fLen = 2; // filename length counter
			if (op != Opcode.ERROR) {
				for (int i=2; i<received.length; i++) {
					if (received[i] == 0) {
						break;
					} else {
						fLen += 1;
					}
				}
				// didn't find a 0 byte
				if (fLen == received.length) op = Opcode.ERROR;
			}
			
			// get mode length
			int mLen = 2+fLen+1; // mode length counter
			if (op != Opcode.ERROR) {
				for (int i=2+fLen+1; i<received.length; i++) {
					if (received[i] == 0) {
						break;
					} else {
						mLen += 1;
					}
				}
				// didn't find a 0 byte
				if (mLen == received.length) op = Opcode.ERROR;
			}
			
			// check if there is other stuff at end of packet
			if (mLen != received.length-1) {
				op = Opcode.ERROR;  
			}
			
			// deal with OPcodes
			if (op != Server.Opcode.ERROR) {
				if (op == Server.Opcode.RRQ) {
					System.out.println("Server: Read Request Received:");
				} else if (op == Server.Opcode.WRQ) {
					System.out.println("Server: Write Request Received:");
				}
				
				// process the received datagram and print data
				System.out.println("From host: " + receivePacket.getAddress() + " : " + receivePacket.getPort());
				System.out.print("Containing " + receivePacket.getLength() + " bytes: \n");
				System.out.println(Arrays.toString(received));
				System.out.print("\tFilename: " + new String(receivePacket.getData(), 2, fLen-2, Charset.forName("utf-8")) + 
				"\t\tMode: " + new String(receivePacket.getData(), 2+fLen+1, mLen-(2+fLen+1), Charset.forName("utf-8")) + "\n");
				
				// create new thread to communicate with Client and transfer file
				// pass it datagram that was received				
				Thread clientConnectionThread = new Thread(new ClientConnection(receivePacket, op));
				System.out.println("Server: Packet Sent for Processing: \n");
				clientConnectionThread.start();					
			} else {
				System.out.println("Server: Invalid Request Received: \n");
			}
		} // end of while
		receiveSocket.close();
	}
}

class ClientConnection implements Runnable {
	Server.Opcode op;
	
	// responses for valid requests
	public static final byte[] readResp = {0, 3, 0, 1};
	public static final byte[] writeResp = {0, 4, 0, 0};
	
	DatagramPacket receivePacket;
	DatagramPacket sendPacket;
	DatagramSocket sendSocket;	

	public ClientConnection(DatagramPacket receivePacket, Server.Opcode opServer) {
		// pass in the received datagram packet from the Server
		// in order to facilitate file transfers with the Client
		this.receivePacket = receivePacket;
		this.op = opServer;
	}
	
	public void run() {
		// to get rid of trailing null bytes from buffer	      
		byte received[] = new byte[receivePacket.getLength()];
		System.arraycopy(receivePacket.getData(), 0, received, 0, receivePacket.getLength());
				
		// open new socket to send response to Client
		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
		
		// create response packet
		byte response[] = new byte[4];
		if (op == Server.Opcode.RRQ) {
			response = readResp;
		} else if (op == Server.Opcode.WRQ) {
			response = writeResp;
		} else {
			try {
				throw new Exception("Not yet implemented");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		sendPacket = new DatagramPacket(response, 4, receivePacket.getAddress(), receivePacket.getPort());
		
		// Send the datagram packet to the client via the send socket.
		try {
			sendSocket.send(sendPacket);
			System.out.println("Server: packet sent using port " + sendSocket.getLocalPort() + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// we are finished sending a response, close socket
		sendSocket.close(); 
	}	
}
