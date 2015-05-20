import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;	// printing out byte array
import java.util.Scanner;

public class Client implements KeyListener{

   DatagramPacket sendPacket, receivePacket;
   DatagramSocket sendReceiveSocket;
   
   // start in normal (send requests straight to Server), or test (send through ErrorSim) mode
   public static enum Mode {NORMAL, TEST};
   
   // choose between read and write requests
   public static enum Decision {RRQ, WRQ};
   
   public static final int MAX_DATA = 512;	//maximum number of bytes in data block
   
   private BufferedInputStream in;	// stream to read in file
   private Scanner input;			// get user choices in UId
   

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
	   System.out.println("***** Welcome to Group #2's SYSC3303 TFTP Client Program! *****\n");
	   c.ui();	// start the user interface
   }
   
   // simple user interface for Client
   public void ui() throws IOException {	
	   String fileName = "test0.txt";	// the file to be sent/received
	   String mode = "netascii";		// the mode in which to send/receive the file
	   Decision request = Decision.RRQ;	// the user's choice of request to send
	   input = new Scanner(System.in);
	   
	   System.out.println("Would you like to make a (R)ead Request, (W)rite Request, or (Q)uit?");
	   String choice = input.nextLine();	// user's choice
	   
	   if (choice.equalsIgnoreCase("R")) {			// read request
		   request = Decision.RRQ;
		   System.out.println("Client: You have chosen to send a read request.");
	   } else if (choice.equalsIgnoreCase("W")) {	// write request
		   request = Decision.WRQ;
		   System.out.println("Client: You have chosen to send a write request.");
	   } else if (choice.equalsIgnoreCase("Q")) {	// quit
		   System.out.println("Goodbye!");
		   System.exit(1);
	   } else {										// not valid
		   System.out.println("I'm sorry, that is not a valid choice.  Please try again...");
		   ui();
	   }	   
	   
	   System.out.println("Please choose a file to modify.  Type in a file name: ");
	   fileName = input.nextLine();	// user's choice
	   
	   // send user's choice of request
	   if (request == Decision.RRQ) {  		   
		   System.out.println("Client: You have chosen the file: " + fileName + ", to be received in " + 
				   mode + " mode.\n");
		   read(fileName, mode);	// send read request
		   
	   } else if (request == Decision.WRQ) {
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
	   sendPacket = new DatagramPacket(request, request.length, InetAddress.getLocalHost(), 69);
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
	   
	   byte[] received = new byte[MAX_DATA]; // initialize for do-while loop
	   
	   do {
		   // prepare for receiving data packet
		   byte[] data = new byte[MAX_DATA + 4];
		   receivePacket = new DatagramPacket(data, data.length);
		   System.out.println("\nClient: Waiting for DATA.\n");
	   
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
	   
		   // cut off zero bytes
		   int size = 4;
		   while (size < data.length) {
			   if (data[size] == 0) {
				   break;
			   }
			   size++;
		   }
		   received = new byte[size - 4];
		   System.arraycopy(data, 4, received, 0, size-4);
	   
		   // process the received DATA 
		   System.out.println("\nClient: DATA received: ");
		   System.out.println("From host: " + receivePacket.getAddress() + " : " + receivePacket.getPort());
		   System.out.print("Containing " + received.length + " bytes: ");
		   System.out.print(Arrays.toString(received));
	   
		   // read DATA to file
		   Files.write(Paths.get(fileName), received, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		   System.out.println("\nClient: reading data to file: " + fileName);
	   
		   byte[] response = new byte[4];		// data opcode and block number
		   response[0] = 0;
		   response[1] = 4;
		   response[2] = 0;
		   response[3] = data[3];
		   
		   // form the ACK packet and send it
		   sendPacket = new DatagramPacket(response, 4, receivePacket.getAddress(), receivePacket.getPort());
		   try {
			   sendReceiveSocket.send(sendPacket);
			   System.out.println("\nClient: ACK sent using port " + 
					   sendReceiveSocket.getLocalPort() + ".");
			   // print byte info on packet being sent
			   System.out.print("Containing " + sendPacket.getLength() + " bytes: " + Arrays.toString(response));
		   } catch (IOException e) {
			   e.printStackTrace();
			   System.exit(1);
		   }
		   
	   } while (!(received.length < MAX_DATA));
	   ui();
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
	   byte[] response = new byte[4];		// data opcode and block number
	   response[0] = 0;
	   response[1] = 3;
	   response[2] = 0;
	   response[3] = 1;
	   
	   in = new BufferedInputStream(new FileInputStream(fileName));
		
	   // reads the file in 512 byte chunks
       while ((in.read(data)) != -1) {
    	   // cut off zero bytes
    	   int size = 0;
    	   while (size < data.length) {
    		   if (data[size] == 0) {
    			   break;
    		   }
    		   size++;
    	   }
    	   
    	   byte[] transfer = new byte[response.length + size];	// byte array to send to Server
				        	
			// copy opcode, blocknumber, and data into array to send to Server
			System.arraycopy(response, 0, transfer, 0, 4);
			System.arraycopy(data, 0, transfer, 4, size);			
			
			        	
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
			
			// increase the block number after each block is sent
			if (response[3] == 127) {
				response[3] = 0;
			} else {
				response[3] = (byte)(response[3] + 1);
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
       in.close(); // close the stream
       ui();
   }

@Override
public void keyTyped(KeyEvent e) {
	// TODO Auto-generated method stub
	if (e.getKeyChar()=='q'){System.exit(0);}
}

@Override
public void keyPressed(KeyEvent e) {
	// TODO Auto-generated method stub
	
}

@Override
public void keyReleased(KeyEvent e) {
	// TODO Auto-generated method stub
	
}
}