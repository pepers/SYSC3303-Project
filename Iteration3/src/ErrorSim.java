

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
  public static int port; //port of client
  public static final int MAX_DATA = 512; 


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
   */
  public void UI()
  {

    String err;
    String dest;
    Scanner m = new Scanner(System.in);
    System.out.println("Hello, I am an error simulator :) ");
    System.out.println("Would you like to:  ");

    System.out.println("1. Create a generic error? ");
    System.out.println("2. Simulate a transfer error? ");

    err = m.nextLine();

    if(err.equals("1"))
    {
      System.out.println("Where would you like to send this error? (S)erver or (C)lient? ");
      dest = m.nextLine();
      System.out.println("What kind of generic error would you like to create? (1,2,3,4,5,6)");
      err = m.nextLine();
      genericError(err,dest);




    }
    else if(err.equals("2")){
      System.out.println("Would you like to test error code (4) or (5)?  ");
      err = m.nextLine();
      if(err.equals("4"))
      {
        // createError4();
      }
      else if(err.equals("5"))
      {
        createError5();
      }

    }
  }









  /*******************MUST MAKE LISTENER METHOD********************************************************/

  public void ErrsimQuit() {
    Scanner input = new Scanner(System.in); // scan user input
    int seconds = 0;          // seconds until socket timeout
    try {
      seconds = receiveSocket.getSoTimeout()/1000;
    } catch (SocketException e) {
      e.printStackTrace();
    } 
    while (true) {
      System.out.println("\nError Simuator: Have not received new packet in the last " +
          seconds + " seconds: ");
      System.out.println("Would you like to (Q)uit?  Or would you like to (C)ontinue?");
      String choice = input.nextLine();     // user's choice
      if (choice.equalsIgnoreCase("Q")) {     // Quit
        System.out.println("\nError Simulator: Goodbye!");
        receiveSocket.close();  // close socket listening for requests
        System.exit(0);     // exit server
      } else if (choice.equalsIgnoreCase("C")) {  // Continue
        break;
      } else {                  // invalid user choice
        System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
      }
    }
  }



  /*
   * Receives the packet on a specified port, there are 3 ports on error sim
   * They are: Port 68,the sendandreceive port on the server side and the
   * sendandreceive port on the client side
   * 
   */
  public DatagramPacket receivePacket()
  {
    byte data[] = new byte[MAX_DATA + 4]; 
    DatagramPacket receivePacket = new DatagramPacket(data, data.length);

    while (true){
      try {
        // block until a DatagramPacket is received via sendReceiveSocket 
        receiveSocket.receive(receivePacket);

        // print out thread and port info, from which the packet was sent to Client
        System.out.println("\nServer: packet received: ");
        System.out.println("From host: " + receivePacket.getAddress() + " : " + receivePacket.getPort());
        System.out.print("Containing " + receivePacket.getLength() + " bytes: \n");
        addr=receivePacket.getAddress();
        port=receivePacket.getPort();
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
  public void sendPack(byte[] data)
  {

    // create new DatagramPacket to send to client
    sendPacket = new DatagramPacket(data, data.length, addr, port);

    // print out packet info to user
    System.out.println("Error Simulator: Sending packet: ");
    System.out.println("To host: " + addr + " : " + port);
    System.out.print("Containing " + sendPacket.getLength() + " bytes: \n");
    System.out.println(Arrays.toString(data) + "\n");

    // send the packet
    try {
      sendReceiveSocket.send(sendPacket);
      System.out.println("ErrSim: Packet sent");
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }


  /*
   * Error code for Illegal TFTP operation
   */
  /*
   public byte[] createError4(byte errorCode, String errorMsg)
   {

   }

   /*
   * Error code for Unknown transfer ID
   * This is basically if a second client randomly sends a block of
   * data from an unknown port
   * 
   */

  /*
   * Error code for Unknown transfer ID
   * This code generates a error code 3 message (disk spcae full) to both client and server
   * to view how they process the packet from an unknown source
   * 
   * This code should be executed only AFTER a packet is recieved from the client.
   * OR the client port to send to number is statically set, and to sim the error simply replace the receivePacket.getPort() 
   * with that static port number eg 50000 
   * 
   */
  public void createError5()
	{
		DatagramPacket errorPacketClient = null, errorPacketServer = null; //Initialize a intentional error packet for both client and server
		DatagramSocket errorSocket = null; //New Socket initialized to simulate unknown TID
		try {errorSocket = new DatagramSocket();} //New socket generated to simulate unknown TID
		catch (SocketException se)
		{
			se.printStackTrace();
			System.exit(1);
		}
		//Creating an Error Message which when normally received would close a connection.
		//Error Packet is designed as a code 3 (disk space full) error.
		String errorMessage = "Disk full or allocation exceeded.";
		byte[] data = new byte[5+errorMessage.length()];
		data[0] = (byte) 0;
		data[1] = (byte) 5;
		data[2] = (byte) 0;
		data[3] = (byte) 3;
		byte[] temp = errorMessage.getBytes();
		for(int i=0; i < temp.length; i++) {
			data[i+4] = temp[i];
		}
		data[data.length-1] = (byte) 0;
		//Create error packets to be sent to both client and server.
		try
		{
			//Client Packet
			errorPacketClient = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 50000);
			//Server Packet
			errorPacketServer = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 69);
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		// Send the error datagram packet to the server and client both via the (unknown) error socket.
		try {
			errorSocket.send(errorPacketClient);
			errorSocket.send(errorPacketServer);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		//Close Error Socket
		errorSocket.close();
	}


  /*
   * The other methods were for if the client/server can detect an error during a file transfer
   * This will just generate an error that the user asks for (in the UI method)
   * and it will create the packet for it
   * 
   */

  public void genericError(String Errtype, String destination)
  {

    byte[] error= new byte[MAX_DATA+4];
    // add opcode
    error[0] = 0;
    error[1] = 5;
    error[2] = 0;
    String message=null;
    byte[] messageBytes= new byte[MAX_DATA];

    if(Errtype.equals("1")){
      //Create error message
      message="File not found";

      // add error code
      error[3] = 1;


      // convert message to byte[], with proper encoding
      try {
        messageBytes = message.getBytes("US-ASCII");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      // add error message to error byte[]
      System.arraycopy(messageBytes, 0, error, 4, message.length());
      error[error.length-1] = 0;  // make last element a 0 byte, according to TFTP
       if (destination.equals("C")){
        sendPacket = new DatagramPacket(error, error.length, addr, port);
      }else if (destination.equals("S")){
        sendPacket = new DatagramPacket(error, error.length, addr, 69 );
      }
      
    }
    else if(Errtype.equals("2"))
    {
      //Create error message
      message="Access violation";

      // add error code
      error[3] = 2;
      
      // convert message to byte[], with proper encoding
      try {
        messageBytes = message.getBytes("US-ASCII");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      // add error message to error byte[]
      System.arraycopy(messageBytes, 0, error, 4, message.length());
      error[error.length-1] = 0;  // make last element a 0 byte, according to TFTP
      if (destination.equalsIgnoreCase("C")){
        sendPacket = new DatagramPacket(error, error.length, addr, port);
      }else if (destination.equalsIgnoreCase("S")){
        sendPacket = new DatagramPacket(error, error.length, addr, 69);
        sendError(sendPacket);
      }
      
    }
    else if(Errtype.equals("3"))
    {
      //Create error message
      message="Disk full or allocation exceeded";

      // add error code
      error[3] = 3;


      // convert message to byte[], with proper encoding
      try {
        messageBytes = message.getBytes("US-ASCII");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      // add error message to error byte[]
      System.arraycopy(messageBytes, 0, error, 4, message.length());
      error[error.length-1] = 0;  // make last element a 0 byte, according to TFTP
      if (destination.equalsIgnoreCase("C")){
        sendPacket = new DatagramPacket(error, error.length, addr, port);
      }else if (destination.equalsIgnoreCase("S")){
        sendPacket = new DatagramPacket(error, error.length, addr, 69);
      }
    }
    else if(Errtype.equals("4"))
    {   //Create error message
      message="Illegal TFTP operation";

      // add error code
      error[3] = 4;


      // convert message to byte[], with proper encoding
      try {
        messageBytes = message.getBytes("US-ASCII");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      // add error message to error byte[]
      System.arraycopy(messageBytes, 0, error, 4, message.length());
      error[error.length-1] = 0;  // make last element a 0 byte, according to TFTP
      if (destination.equalsIgnoreCase("C")){
        sendPacket = new DatagramPacket(error, error.length, addr, port);
      }else if (destination.equalsIgnoreCase("S")){
        sendPacket = new DatagramPacket(error, error.length, addr, 69);
      }
    }
    else if(Errtype.equals("5"))
    {
      //Create error message
      message="Unknown transfer ID";

      // add error code
      error[3] = 5;


      // convert message to byte[], with proper encoding
      try {
        messageBytes = message.getBytes("US-ASCII");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      // add error message to error byte[]
      System.arraycopy(messageBytes, 0, error, 4, message.length());
      error[error.length-1] = 0;  // make last element a 0 byte, according to TFTP
      if (destination.equalsIgnoreCase("C")){
        sendPacket = new DatagramPacket(error, error.length, addr, port);
      }else if (destination.equalsIgnoreCase("S")){
        sendPacket = new DatagramPacket(error, error.length, addr, 69);
      }
      
    }else if (Errtype.equals("6")){
      //Create error message
      message="File already exists";
      // add error code
      error[3] = 6;
      // convert message to byte[], with proper encoding
      try {
        messageBytes = message.getBytes("US-ASCII");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      // add error message to error byte[]
      System.arraycopy(messageBytes, 0, error, 4, message.length());
      error[error.length-1] = 0;  // make last element a 0 byte, according to TFTP
      if (destination.equalsIgnoreCase("C")){
        sendPacket = new DatagramPacket(error, error.length, addr, port);

      }else if (destination.equalsIgnoreCase("S")){
        sendPacket = new DatagramPacket(error, error.length, addr, 69);
      }
      
      
    }else{
      System.exit(1);
    }
    
    

  }
  
  public void sendError(DatagramPacket pack)
  {
    try {
      sendReceiveSocket.send(pack);
      System.out.println("Errsim: Packet sent");
    } catch (IOException e) {
      System.out.println("omg");
      e.printStackTrace();
      System.exit(1);
    }   
  }



  /****************MAIN******** 
   * Asks user whether they want normal mode or error sim mode, normal mode simply forwards packets to server, Errsim
   * runs the user interface to get info on what kind of errors to simulate
   * Please note that createError5 cannot test properly until a packet is received from the client***/

  public static void main( String args[] )
  {
    while(true)
    {
      ErrorSim e = new ErrorSim();
      Scanner s = new Scanner(System.in); 
      System.out.println("Would you like to operate in (N)ormal mode or (E)rror sim mode? ");
      String userch = s.nextLine();
      if(userch.equalsIgnoreCase("N"))//simply forward the packet along (receive, process,send)
      {
        System.out.println("Waiting to receive packets for forwarding.. ");
        DatagramPacket data = e.receivePacket();
        byte[] sendpack = e.processDatagramPacket(data); 
        e.sendPack(sendpack);
      }
      else if(userch.equalsIgnoreCase("E"))//go to UI, send or test appropriate error packets. 
      {
        e.UI();
        e.receivePacket();

      }
    }


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




