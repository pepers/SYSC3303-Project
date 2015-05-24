

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

/**
 * The error simulation program for the SYSC3303 TFTP Group Project.
 * 
 * @author  Adhiraj Chakraborty
 * @author  Anuj Dalal
 * @author  Hidara Abdallah
 * @author  Matthew Pepers
 * @author  Mohammed Hamza
 * @author  Scott Savage
 * @version 3
 */
public class ErrorSim {
   
   // UDP datagram packets and sockets used to send / receive
   private DatagramPacket sendPacket, receivePacket;
   private DatagramSocket receiveSocket, sendReceiveSocket;
   InetAddress addr;                        // InetAddress of client that sent request
   int port;
   
   public ErrorSim()
   {
      try {
         // Construct a datagram socket and bind it to port 68
         // on the local host machine. This socket will be used to
         // receive UDP Datagram packets from clients.
         receiveSocket = new DatagramSocket(68);
         // Construct a datagram socket and bind it to any available
         // port on the local host machine. This socket will be used to
         // send and receive UDP Datagram packets from the server.
         sendReceiveSocket = new DatagramSocket();
      } catch (SocketException se) {
         se.printStackTrace();
         System.exit(1);
      }
   }
   
   /*
    * Create a basic UI that will ask the user if they want to generate an error
    * and what kind of error and if they want to test it on the client or server
    * See other methods below for what exactly the error codes are and how much the client
    * will be prompted for
    * 
    * HYDE WILL DO THIS SHIT 
    * 
    */
   public void UI()
   {
     while(true)
     {
       String s;
       String answer;
       Scanner in = new Scanner(System.in);
       System.out.println("Hello, I am an error simulator :) ");
       System.out.println("Would you like to generate an error packet? (Y/N) ");
       s = in.nextLine();
       if(s.equals("Y")||s.equals("y"))
       {
         
         System.out.println("What kind of error code would you like to create? (1,2,3,4,5,6)");
         s = in.nextLine();
         System.out.println("Enter S for server side and C for client side");
         answer=in.nextLine();
         if(s == "1")
         { if (answer=="C"){
           byte[] error = createErrorClient1((byte)1, "File  does not exist.");
         }if (answer=="S"){
        	 byte[] error = createErrorServer1((byte)1, "File  does not exist.");
        			 }
         }
         else if(s == "2")
         {
        	 if (answer=="C"){
           byte[] error = createErrorClient2((byte)2, "File  can not be written to.");
         }if (answer=="S"){
        	 byte[] error = createErrorServer2((byte)2, "File  can not be written to."); 
         }
         }
         else if(s == "3")
         {
        	 if (answer=="C"){
           byte[] error = createErrorClient3((byte)3, "Disk full.");
         }if (answer=="S"){
        	 byte[] error = createErrorServer3((byte)3, "Disk full."); }
         }
         else if(s == "4")
         {
        	 if(answer=="C"){
        		 byte[] error = createErrorClient4((byte) 4, "Illegal TFTP Operation.");
         }if (answer=="S"){byte[] error = createErrorServer4((byte) 4, "Illegal TFTP Operation.");}
         else if(s == "5")
         {
        	
         if (answer=="C"){
           byte[] error = createErrorClient5((byte)5, "Unknown TID");
         }if (answer=="S"){
        	 byte[] error = createErrorServer5((byte)5, "Unknown TID"); }
         }
         else
         {
           byte[] error = createError6((byte)6, "File already exists.");
         }
          
       }
       else 
         System.exit(1);
     }
   }
   
   /*
    * Receives the packet on a specified port, there are 3 ports on error sim
    * They are: Port 68,the sendandreceive port on the server side and the
    * sendandreceive port on the client side
    * 
    */
   
   private byte[] createErrorServer2(byte b, String string) {
	// TODO Auto-generated method stub
	return null;
}

private byte[] createErrorServer3(byte b, String string) {
	// TODO Auto-generated method stub
	return null;
}

private byte[] createErrorServer4(byte b, String string) {
	// TODO Auto-generated method stub
	return null;
}

private byte[] createErrorServer5(byte b, String string) {
	// TODO Auto-generated method stub
	return null;
}

private byte[] createErrorClient5(byte b, String string) {
	// TODO Auto-generated method stub
	return null;
}

private byte[] createErrorClient4(byte b, String string) {
	// TODO Auto-generated method stub
	return null;
}

private byte[] createErrorClient3(byte b, String string) {
	// TODO Auto-generated method stub
	return null;
}

private byte[] createErrorServer1(byte b, String string) {
	// TODO Auto-generated method stub
	return null;
}

private byte[] createErrorClient2(byte b, String string) {
	// TODO Auto-generated method stub
	return null;
}

private byte[] createErrorClient1(byte b, String string) {
	// TODO Auto-generated method stub
	return null;
}

public void ErrsimQuit() {
    Scanner input = new Scanner(System.in); // scan user input
    int seconds = 0;          // seconds until socket timeout
    try {
      seconds = receiveSocket.getSoTimeout()/1000;
    } catch (SocketException e) {
      e.printStackTrace();
    } 
    while (true) {
      System.out.println("\nServer: Have not received new packet in the last " +
          seconds + " seconds: ");
      System.out.println("Would you like to (Q)uit?  Or would you like to (C)ontinue?");
      String choice = input.nextLine();     // user's choice
      if (choice.equalsIgnoreCase("Q")) {     // Quit
        System.out.println("\nServer: Goodbye!");
        receiveSocket.close();  // close socket listening for requests
        System.exit(0);     // exit server
      } else if (choice.equalsIgnoreCase("C")) {  // Continue
        break;
      } else {                  // invalid user choice
        System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
      }
    }
  }
   public DatagramPacket receivePacket()
   {
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
        
        break;
      } catch (SocketTimeoutException e) {  // haven't received packet in 5 seconds
        ErrsimQuit(); // find out if user wants to quit, if not while loop will re-try
      } catch(IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
    
    return receivePacket;
   }
   
   /*
    * Once the packet has been received(see method receivePacket())
    * this method will be used to parse it
    */
   public byte[] processDatagramPacket(DatagramPacket packet)
   {
     byte[] data = new byte[packet.getLength()];
    System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
    
    // display info to user
    System.out.println(Arrays.toString(data));
    
    return data;
   }
   
   /*
    * Method to build the packet and send it
    */
   public void sendPacket(byte[] data)
   {
  // create new DatagramPacket to send to client
      sendPacket = new DatagramPacket(data, data.length, addr, port);
      
      // print out packet info to user
      System.out.println("\n" + Thread.currentThread() + ": Sending packet: ");
      System.out.println("To host: " + addr + " : " + port);
      System.out.print("Containing " + sendPacket.getLength() + " bytes: \n");
      System.out.println(Arrays.toString(data) + "\n");
      
      // send the packet
      try {
        sendReceiveSocket.send(sendPacket);
        System.out.println(Thread.currentThread() + ": Packet sent ");
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
   }
   
   /*
    * Test to see if the file exists
    * For this we will have to extract the filename in the original request
    * then alter it before forwarding along
    */
   
   
   public byte[] createError1(byte errorCode, String errorMsg)
   {
     byte[] error = new byte[4 + errorMsg.length() + 1];  // new error to eventually be sent to server
    
    // add opcode
    error[0] = 0;
    error[1] = 5;
    
    // add error code
    error[2] = 0;
    error[3] = errorCode;
    
    byte[] message = new byte[errorMsg.length()]; // new array for errorMsg, to be joined with codes
    
    // convert errorMsg to byte[], with proper encoding
    try {
      message = errorMsg.getBytes("US-ASCII");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    
    // add error message to error byte[]
    System.arraycopy(message, 0, error, 4, message.length);
    error[error.length-1] = 0;  // make last element a 0 byte, according to TFTP
        
    return error; //return full error message with opcodes and type of error
    
   }
   
   /*
    * Error for Access Violation
    * Checks for correct opcode, also makes sure the file is writable
    * for a write request,maybe more scenarios
    */
   public byte[] createError2(byte errorCode, String errorMsg)
   {
     byte[] error = new byte[4 + errorMsg.length() + 1];  // new error to eventually be sent to server
    
    // add opcode
    error[0] = 0;
    error[1] = 5;
    
    // add error code
    error[2] = 0;
    error[3] = errorCode;
    
    byte[] message = new byte[errorMsg.length()]; // new array for errorMsg, to be joined with codes
    
    // convert errorMsg to byte[], with proper encoding
    try {
      message = errorMsg.getBytes("US-ASCII");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    
    // add error message to error byte[]
    System.arraycopy(message, 0, error, 4, message.length);
    error[error.length-1] = 0;  // make last element a 0 byte, according to TFTP
        
    return error; //return full error message with opcodes and type of error
   }
   
   /*
    * Error for disk full or allocation exceeded
    * Recommend testing this on a full USB
    */
   
   public byte[] createError3(byte errorCode, String errorMsg)
   {
     byte[] error = new byte[4 + errorMsg.length() + 1];  // new error to eventually be sent to server
    
    // add opcode
    error[0] = 0;
    error[1] = 5;
    
    // add error code
    error[2] = 0;
    error[3] = errorCode;
    
    byte[] message = new byte[errorMsg.length()]; // new array for errorMsg, to be joined with codes
    
    // convert errorMsg to byte[], with proper encoding
    try {
      message = errorMsg.getBytes("US-ASCII");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    
    // add error message to error byte[]
    System.arraycopy(message, 0, error, 4, message.length);
    error[error.length-1] = 0;  // make last element a 0 byte, according to TFTP
        
    return error; //return full error message with opcodes and type of error
   }
   
   /*
    * Error code for Illegal TFTP operation
    */
   
   public byte[] createError4(byte errorCode, String errorMsg)
   {
     byte[] error = new byte[4 + errorMsg.length() + 1];  // new error to eventually be sent to server
    
    // add opcode
    error[0] = 0;
    error[1] = 5;
    
    // add error code
    error[2] = 0;
    error[3] = errorCode;
    
    byte[] message = new byte[errorMsg.length()]; // new array for errorMsg, to be joined with codes
    
    // convert errorMsg to byte[], with proper encoding
    try {
      message = errorMsg.getBytes("US-ASCII");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    
    // add error message to error byte[]
    System.arraycopy(message, 0, error, 4, message.length);
    error[error.length-1] = 0;  // make last element a 0 byte, according to TFTP
        
    return error; //return full error message with opcodes and type of error
   }
   
   /*
    * Error code for Unknown transfer ID
    * This is basically if a second client randonly sends a block of
    * data from an unknown port
    * 
    */
   public byte[] createError5(byte errorCode, String errorMsg)
   {
     byte[] error = new byte[4 + errorMsg.length() + 1];  // new error to eventually be sent to server
    
    // add opcode
    error[0] = 0;
    error[1] = 5;
    
    // add error code
    error[2] = 0;
    error[3] = errorCode;
    
    byte[] message = new byte[errorMsg.length()]; // new array for errorMsg, to be joined with codes
    
    // convert errorMsg to byte[], with proper encoding
    try {
      message = errorMsg.getBytes("US-ASCII");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    
    // add error message to error byte[]
    System.arraycopy(message, 0, error, 4, message.length);
    error[error.length-1] = 0;  // make last element a 0 byte, according to TFTP
        
    return error; //return full error message with opcodes and type of error
   }
   
   /*
    * Error code for File already exists
    * Check Client.java and Server.java for how this is implemented
    */
   
   public byte[] createError6(byte errorCode, String errorMsg)
   {
     byte[] error = new byte[4 + errorMsg.length() + 1];  // new error to eventually be sent to server
    
    // add opcode
    error[0] = 0;
    error[1] = 5;
    
    // add error code
    error[2] = 0;
    error[3] = errorCode;
    
    byte[] message = new byte[errorMsg.length()]; // new array for errorMsg, to be joined with codes
    
    // convert errorMsg to byte[], with proper encoding
    try {
      message = errorMsg.getBytes("US-ASCII");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    
    // add error message to error byte[]
    System.arraycopy(message, 0, error, 4, message.length);
    error[error.length-1] = 0;  // make last element a 0 byte, according to TFTP
        
    return error; //return full error message with opcodes and type of error
   }
   
   /*
    * The other methods were for if the client/server can detect an error during a file transfer
    * This will just generate an error that the user asks for (in the UI method)
    * and it will create the packet for it
    * 
    * ................/´¯/) 
....................,/¯../ 
.................../..../ 
............./´¯/'...'/´¯¯`·¸ 
........../'/.../..../......./¨¯\ 
........('(...´...´.... ¯~/'...') 
.........\.................'...../ 
..........''...\.......... _.·´ 
............\..............( 
..............\.............\...
    * MOE THIS SHOULDVE BEEN MENTIONED AT THE BEGINNING OF THE UI METHOD
    */
   
   public void genericError()
   {
    
   }
   public void ForwardPacket()
   {

      byte[] data;
      
      int clientPort, j=0;
  
      for(;;) { // loop forever
         // Construct a DatagramPacket for receiving packets up
         // to 100 bytes long (the length of the byte array).
         
         data = new byte[100];
         receivePacket = new DatagramPacket(data, data.length);

         System.out.println("Simulator: Waiting for packet.");
         // Block until a datagram packet is received from receiveSocket.
         try {
            receiveSocket.receive(receivePacket);
         } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
         }

         // Process the received datagram.
         System.out.println("Simulator: Packet received:");
         System.out.println("From host: " + receivePacket.getAddress());
         clientPort = receivePacket.getPort();
         System.out.println("Host port: " + clientPort);
         System.out.println("Length: " + receivePacket.getLength());
         System.out.println("Containing: " );
         
         // print the bytes
         for (j=0;j<receivePacket.getLength();j++) {
            System.out.println("byte " + j + " " + data[j]);
         }

         // Form a String from the byte array.
         String received = new String(data,0,receivePacket.getLength());
         System.out.println(received);
         
         
         Runnable serverConnection = new serverConnection(sendPacket, receivePacket, sendReceiveSocket);
         new Thread(serverConnection).start();
         
      } // end of loop

   }
   public static void main( String args[] )
   {
      ErrorSim s = new ErrorSim();
      s.ForwardPacket();
      
   }
} 
 class serverConnection implements Runnable
 {
     private DatagramPacket receivePacket;
     private DatagramPacket sendPacket;
     private DatagramSocket sendReceiveSocket;
     private DatagramSocket sendSocket;
     
     //constructor for the connection handler class
     public serverConnection (DatagramPacket sendPacket, DatagramPacket recievePacket, DatagramSocket sendReceiveSocket)
     {
         this.sendReceiveSocket = sendReceiveSocket;
         this.sendPacket = sendPacket;
         this.receivePacket = recievePacket;
     }
     //this is assuming that the multiple thread action for the ErrSim is that 
     //it keeps multiple connections open to the server, so as to forward client packets
     //may also need to implement multiple connection threading in the client direction 
     public void run()
     {
         int  j=0;
         byte[] data = new byte[100];
         
         // Now pass it on to the server (to port 69)
         // Construct a datagram packet that is to be sent to a specified port
         // on a specified host.
         // The arguments are:
         //  msg - the message contained in the packet (the byte array)
         //  the length we care about - k+1
         //  InetAddress.getLocalHost() - the Internet address of the
         //     destination host.
         //     In this example, we want the destination to be the same as
         //     the source (i.e., we want to run the client and server on the
         //     same computer). InetAddress.getLocalHost() returns the Internet
         //     address of the local host.
         //  69 - the destination port number on the destination host.

         sendPacket = new DatagramPacket(data, receivePacket.getLength(),
                                        receivePacket.getAddress(), 69);
        
         System.out.println("Simulator: sending packet.");
         System.out.println("To host: " + sendPacket.getAddress());
         System.out.println("Destination host port: " + sendPacket.getPort());
         System.out.println("Length: " + sendPacket.getLength());
         System.out.println("Containing: ");
         for (j=0;j<sendPacket.getLength();j++) {
             System.out.println("byte " + j + " " + data[j]);
         }

         // Send the datagram packet to the server via the send/receive socket.

         try {
            sendReceiveSocket.send(sendPacket);
         } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
         }
         
      // Process the received datagram.
         System.out.println("Simulator: Packet received:");
         System.out.println("From host: " + receivePacket.getAddress());
         System.out.println("Host port: " + receivePacket.getPort());
         System.out.println("Length: " + receivePacket.getLength());
         System.out.println("Containing: ");
         for (j=0;j<receivePacket.getLength();j++) {
            System.out.println("byte " + j + " " + data[j]);
         }

         // Construct a datagram packet that is to be sent to a specified port
         // on a specified host.
         // The arguments are:
         //  data - the packet data (a byte array). This is the response.
         //  receivePacket.getLength() - the length of the packet data.
         //     This is the length of the msg we just created.
         //  receivePacket.getAddress() - the Internet address of the
         //     destination host. Since we want to send a packet back to the
         //     client, we extract the address of the machine where the
         //     client is running from the datagram that was sent to us by
         //     the client.
         //  receivePacket.getPort() - the destination port number on the
         //     destination host where the client is running. The client
         //     sends and receives datagrams through the same socket/port,
         //     so we extract the port that the client used to send us the
         //     datagram, and use that as the destination port for the TFTP
         //     packet.
         

         sendPacket = new DatagramPacket(data, receivePacket.getLength(),
                               receivePacket.getAddress(), receivePacket.getPort());

         System.out.println( "Simulator: Sending packet:");
         System.out.println("To host: " + sendPacket.getAddress());
         System.out.println("Destination host port: " + sendPacket.getPort());
         System.out.println("Length: " + sendPacket.getLength());
         System.out.println("Containing: ");
         for (j=0;j<sendPacket.getLength();j++) {
            System.out.println("byte " + j + " " + data[j]);
         }

         // Send the datagram packet to the client via a new socket.

         try {
            // Construct a new datagram socket and bind it to any port
            // on the local host machine. This socket will be used to
            // send UDP Datagram packets.
            sendSocket = new DatagramSocket();
         } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
         }

         try {
            sendSocket.send(sendPacket);
         } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
         }

         System.out.println("Simulator: packet sent using port " + sendSocket.getLocalPort());
         System.out.println();

         // We're finished with this socket, so close it.
         sendSocket.close();
         
     }
    
     
 }




