
import java.io.*;
import java.net.*;
import java.util.Arrays;	// printing out byte array

public class Client {

   DatagramPacket sendPacket, receivePacket;
   DatagramSocket sendReceiveSocket;
   
   // start in normal (send requests straight to Server), or test (send through ErrorSim) mode
   public static enum Mode {NORMAL, TEST};
   
   // choose between read and write requests
   public static enum Decision {RRQ, WRQ};
   
   public static final int MAX_DATA = 512;	//maximum number of bytes in data block
   
   private BufferedInputStream in;	// stream to read in file

   public Client() {
	   try {
		   sendReceiveSocket = new DatagramSocket();	// new socket to send requests and receive responses
	   } catch (SocketException se) {   // Can't create the socket.
		   se.printStackTrace();
		   System.exit(1);
	   }
   }

   public static void main(String args[]) throws IOException {
	   Client c = new Client();
	   c.ui();	// start the user interface
   }
   
   // simple user interface for Client
   public void ui() throws IOException {
	   System.out.println("***** Welcome to Group #2's SYSC3303 TFTP Client Program! *****\n");
	   String fileName = "test0.txt";	// the file to be sent/received
	   String mode = "netascii";		// the mode in which to send/receive the file
	   Decision request = Decision.WRQ;	// the user's choice of request to send
	   
	   // send user's choice of request
	   if (request == Decision.RRQ) {
		   System.out.println("Client: You have chosen to send a read request.");
		   System.out.println("Client: You have chosen the file: " + fileName + ", to be received in " + 
				   mode + " mode.\n");
		   read(fileName, mode);	// send read request
		   
	   } else if (request == Decision.WRQ) {
		   System.out.println("Client: You have chosen to send a write request.");
		   System.out.println("Client: You have chosen the file: " + fileName + ", to be sent in " + 
				   mode + " mode.\n");
		   write(fileName, mode);	// send write request
	   }
	   
   }
   
   // send a read request
   public void read (String fileName, String mode) throws IOException {
	   // new stream to write bytes to, and turn into request byte array to be sent
	   ByteArrayOutputStream req = new ByteArrayOutputStream();
	   req.reset();
	   
	   // write read request bytes to stream
	   req.write(0);
	   req.write(1);
	   req.write(fileName.getBytes());
	   req.write(0);
	   req.write(mode.getBytes());
	   req.write(0);
	   
	   // form request byte array
	   byte[] request = new byte[0];
	   request = req.toByteArray();
	   
	   // form the request packet and send it
	   sendPacket = new DatagramPacket(request, request.length, receivePacket.getAddress(), 69);
	   try {
		   sendReceiveSocket.send(sendPacket);
		   System.out.println("Client: Read Request sent using port " + 
				   sendReceiveSocket.getLocalPort() + ".");
		   // print byte info on packet being sent
		   System.out.print("Containing " + sendPacket.getLength() + " bytes: \n");
		   System.out.println(Arrays.toString(request));
	   } catch (IOException e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
   }
   
   // send a write request
   public void write (String fileName, String mode) throws IOException {
	   // new stream to write bytes to, and turn into request byte array to be sent
	   ByteArrayOutputStream req = new ByteArrayOutputStream();
	   req.reset();
	   
	   // write write request bytes to stream
	   req.write(0);
	   req.write(2);
	   req.write(fileName.getBytes());
	   req.write(0);
	   req.write(mode.getBytes());
	   req.write(0);
	   
	   // form request byte array
	   byte[] request = new byte[100];
	   request = req.toByteArray();
	   
	   // form the request packet and send it
	   sendPacket = new DatagramPacket(request, request.length, InetAddress.getLocalHost(), 69);
	   try {
		   sendReceiveSocket.send(sendPacket);
		   System.out.println("Client: Write Request sent using port " + 
				   sendReceiveSocket.getLocalPort() + ":");
		   // print byte info on packet being sent
		   System.out.print("Containing " + sendPacket.getLength() + " bytes: \n");
		   System.out.println(Arrays.toString(request));
	   } catch (IOException e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   
	   // receive ACK from server
	   byte ack[] = new byte[4];
	   receivePacket = new DatagramPacket(ack, ack.length);
	   try {
		   // Block until a datagram is received via sendReceiveSocket.
		   sendReceiveSocket.receive(receivePacket);
	   } catch(IOException e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   
	   // process the received ACK
	   System.out.println("\nClient: ACK received: ");
	   System.out.println("From host: " + receivePacket.getAddress() + " : " + receivePacket.getPort());
	   System.out.print("Containing " + receivePacket.getLength() + " bytes: " + Arrays.toString(ack) + "\n");
	   
	   byte[] data = new byte[MAX_DATA];	// the data chunk to read from the file
	   int n;								// number of bytes read from the file
	   byte[] response = new byte[4];		// data opcode and block number
	   response[0] = 0;
	   response[1] = 3;
	   response[2] = 0;
	   response[3] = 0;
	   
	   in = new BufferedInputStream(new FileInputStream(fileName));
		
	   // reads the file in 512 byte chunks
       while ((n = in.read(data)) != -1) {
    	   // increase the block number after each block is sent
    	   if (response[3] == 255) {
    		   response[3] = 0;
    	   } else {
    		   response[3] = (byte)(response[3] + 1);
    	   }
    	   
    	   byte[] transfer = new byte[response.length + data.length];	// byte array to send to Server
				        	
			// copy opcode, blocknumber, and data into array to send to Server
			System.arraycopy(response, 0, transfer, 0, 4);
			System.arraycopy(data, 0, transfer, 4, n);
				        	
			// send the data packet to the server via the send socket
			sendPacket = new DatagramPacket(transfer, transfer.length, receivePacket.getAddress(), receivePacket.getPort());
			try {
				sendReceiveSocket.send(sendPacket);
				System.out.println("\n\nClient: DATA packet sent using port " + 
						sendReceiveSocket.getLocalPort());
				// print byte info on packet being sent to Server
				System.out.print("Containing " + sendPacket.getLength() + " bytes: \n");
				System.out.println(Arrays.toString(transfer));
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}			
			
			// prepare for receiving packet with ACK
			receivePacket = new DatagramPacket(ack, ack.length);
			System.out.println("\nClient: Waiting for ACK.\n");

			// block until a ACK packet is received from sendReceiveSocket
			try {        
				System.out.println("Waiting...");
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				System.out.print("IO Exception: likely:");
				System.out.println("Receive Socket Timed Out.\n" + e);
				e.printStackTrace();
				System.exit(1);
			}
			
			// process the received ACK
			System.out.println("\nClient: ACK received: ");
			System.out.println("From host: " + receivePacket.getAddress() + " : " + receivePacket.getPort());
			System.out.print("Containing " + receivePacket.getLength() + " bytes: " + Arrays.toString(ack));
		}

   }
}