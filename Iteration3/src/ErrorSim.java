

import java.io.*;
import java.net.*;

/**
 * The error simulation program for the SYSC3303 TFTP Group Project.
 * 
 * @author	Adhiraj Chakraborty
 * @author	Anuj Dalal
 * @author	Hidara Abdallah
 * @author	Matthew Pepers
 * @author	Mohammed Hamza
 * @author	Scott Savage
 * @version	3
 */
public class ErrorSim {
   
   // UDP datagram packets and sockets used to send / receive
   private DatagramPacket sendPacket, receivePacket;
   private DatagramSocket receiveSocket, sendReceiveSocket;
   
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

   public void ForwardPacket()
   {

      byte[] data;
      
      int clientPort, j=0;
      /*
       * Create a basic UI that will ask the user if they want to generate an error
       * and what kind of error and if they want to test it on the client or server
       * See other methods below for what exactly the error codes are and how much the client
       * will be prompted for
       */
      public void UI ()
      {
    	  
      }
      
      /*
       * Receives the packet on a specified port, there are 3 ports on error sim
       * They are: Port 68,the sendandreceive port on the server side and the
       * sendandreceive port on the client side
       */
      public void receivePacket()
      {
    	  
      }
      
      /*
       * Once the packet has been received(see method receivePacket())
       * this method will be used to parse it
       */
      public void processDatagramPacket()
      {
    	  
      }
      
      /*
       * Method to build the packet and send it
       */
      public void sendPacket()
      {
    	  
      }
      
      /*
       * Test to see if the file exists
       * For this we will have to extract the filename in the original request
       * then alter it before forwarding along
       */
      public void createError1()
      {
    	  
      }
      
      /*
       * Error for Access Violation
       * Checks for correct opcode, also makes sure the file is writable
       * for a write request,maybe more scenarios
       */
      public void createError2()
      {
    	  
      }
      
      /*
       * Error for disk full or allocation exceeded
       * Recommend testing this on a full USB
       */
      
      public void createError3()
      {
    	  
      }
      
      /*
       * Error code for Illegal TFTP operation
       */
      
      public void createError4()
      {
    	  
      }
      
      /*
       * Error code for Unknown transfer ID
       * This is basically if a second client randonly sends a block of
       * data from an unknown port
       * 
       */
      public void createError5()
      {
    	  
      }
      
      /*
       * Error code for File already exists
       * Check Client.java and Server.java for how this is implemented
       */
      
      public void createError6()
      {
    	  
      }
      
      /*
       * The other methods were for if the client/server can detect an error during a file transfer
       * This will just generate an error that the user asks for (in the UI method)
       * and it will create the packet for it
       */
      
      public void genericError()
      {
    	  
      }
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




