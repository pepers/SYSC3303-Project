import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.nio.file.FileSystems;//import this to search for the file

public class Server {	
	// requests we can receive
	public static enum Opcode {RRQ, WRQ, DATA, ACK, ERROR}; 
	
	DatagramPacket receivePacket;
	DatagramSocket receiveSocket;

	public static void main(String[] args) {
		// create new thread to wait for and verify TFTP packets
		Server s = new Server();
		
		s.listener();
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
	
	// listens for new requests on port 69
	public void listener() {
		String fileName;
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
					default: // invalid request						
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
				//save the filename in a variable for later use
				fileName=new String(receivePacket.getData(), 2, fLen-2, Charset.forName("utf-8")) + 
						"\t\tMode: " + new String(receivePacket.getData(), fLen+1, mLen-(fLen+1), Charset.forName("utf-8")) + "\n";
				// process the received datagram and print data
				System.out.println("From host: " + receivePacket.getAddress() + " : " + receivePacket.getPort());
				System.out.print("Containing " + receivePacket.getLength() + " bytes: \n");
				System.out.println(Arrays.toString(received));
				System.out.print("\tFilename: " + fileName);
				
				// create new thread to communicate with Client and transfer file
				// pass it datagram that was received				
				Thread clientConnectionThread = new Thread(new ClientConnection(receivePacket, op,fileName));
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
	String fileName;
	
	// responses for valid requests
	public static final byte[] readResp = {1,0};
	public static final byte[] writeResp = {0};
	
	DatagramPacket receivePacket;
	DatagramPacket sendPacket;
	DatagramSocket sendSocket;	

	public ClientConnection(DatagramPacket receivePacket, Server.Opcode opServer, String fn) {
		// pass in the received datagram packet from the Server
		// in order to facilitate file transfers with the Client
		this.receivePacket = receivePacket;
		this.op = opServer;
		this.fileName=fn;
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
		byte response[] = new byte[516];
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

			
		//This is the part where we try to send the actual file(as in create the data)
		
		/*
         * A FileInputStream object is created to read the file
         * as a byte stream. A BufferedInputStream object is wrapped
         * around the FileInputStream, which may increase the
         * efficiency of reading from the stream.
         */
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName));

        /*
         * A FileOutputStream object is created to write the file
         * as a byte stream. A BufferedOutputStream object is wrapped
         * around the FileOutputStream, which may increase the
         * efficiency of writing to the stream.
         */
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));

        byte[] data = new byte[512];
        int n;
        
        /* Read the file in 512 byte chunks. */
        while ((n = in.read(data)) != -1) {
            /* 
             * We just read "n" bytes into array data. 
             * Now write them to the output file. 
             */
            out.write(data, 0, n);
            //check to see if you dont have a full 512 byte chunk, se we can trim the extra zeroes
            if (n!=512){
            	//adding the data to the response array
            	System.arraycopy(data,0,response,response.length,data.length);
            	sendPacket = new DatagramPacket(response, 4+n, receivePacket.getAddress(), receivePacket.getPort());
            }
            	
            	//adding the data to the response array
            	System.arraycopy(data,0,response,response.length,data.length);
            	sendPacket = new DatagramPacket(response, response.length, receivePacket.getAddress(), receivePacket.getPort());
            		
        }
        
        try {
			in.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        try {
			out.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
