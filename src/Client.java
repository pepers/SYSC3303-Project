import java.io.*;
import java.net.*;
public class Client {


//TFTPClient.java
//This class is the client side for a very simple assignment based on TFTP on
//UDP/IP. The client uses one port and sends a read or write request and gets 
//the appropriate response from the server.  No actual file transfer takes place.   


private DatagramPacket sendPacket, receivePacket;
private DatagramSocket sendReceiveSocket;

// we can run in normal (send directly to server) or test
// (send to simulator) mode
public static enum Mode { NORMAL, TEST};
public static enum decision{read,write};

public Client()
{
   try {
      // Construct a datagram socket and bind it to any available
      // port on the local host machine. This socket will be used to
      // send and receive UDP Datagram packets.
      sendReceiveSocket = new DatagramSocket();
   } catch (SocketException se) {   // Can't create the socket.
      se.printStackTrace();
      System.exit(1);
   }
}

public void sendAndReceive(decision request)
{
   byte[] msg = new byte[100], // message we send
          fn, // filename as an array of bytes
          md, // mode as an array of bytes
          data; // reply as array of bytes
   String filename, mode; // filename and mode as Strings
   int j, len, sendPort;
   sendPort = 68; 

      System.out.println("Client: creating packet");
      
      // Prepare a DatagramPacket and send it via sendReceiveSocket
      // to sendPort on the destination host (also on this machine).

     // next we have a file name -- let's just pick one
     filename = "newone.txt";
     // convert to bytes
     fn = filename.getBytes();
     
     // and copy into the msg
     System.arraycopy(fn,0,msg,2,fn.length);
     // format is: source array, source index, dest array,
     // dest index, # array elements to copy
     // i.e. copy fn from 0 to fn.length to msg, starting at
     // index 2
     
     // now add a 0 byte
     msg[fn.length+2] = 0;

     // now add "octet" (or "netascii")
     mode = "octet";
     // convert to bytes
     md = mode.getBytes();
     
     // and copy into the msg
     System.arraycopy(md,0,msg,fn.length+3,md.length);
     
     len = fn.length+md.length+4; // length of the message
     // length of filename + length of mode + opcode (2) + two 0s (2)
     // second 0 to be added next:

     // end with another 0 byte 
     msg[len-1] = 0;

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
     try {
        sendPacket = new DatagramPacket(msg, len,
                            InetAddress.getLocalHost(), sendPort);
     } catch (UnknownHostException e) {
        e.printStackTrace();
        System.exit(1);
     }

     System.out.println("Client: sending packet ");
     System.out.println("Containing: ");
     for (j=0;j<len;j++) {
         System.out.println("byte " + j + " " + msg[j]);
     }

     // Send the datagram packet to the server via the send/receive socket.

     try {
        sendReceiveSocket.send(sendPacket);
     } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
     }

     System.out.println("Client: Packet sent.");

     // Construct a DatagramPacket for receiving packets up
     // to 100 bytes long (the length of the byte array).

     data = new byte[100];
     receivePacket = new DatagramPacket(data, data.length);

     System.out.println("Client: Waiting for packet.");
     try {
        // Block until a datagram is received via sendReceiveSocket.
        sendReceiveSocket.receive(receivePacket);
     } catch(IOException e) {
        e.printStackTrace();
        System.exit(1);
     }

     // Process the received datagram.
     System.out.println("Client: Packet received:");
     System.out.println("Containing: ");
     for (j=0;j<receivePacket.getLength();j++) {
         System.out.println("byte " + j + " " + data[j]);
     }
     byte receivedata[]=receivePacket.toByteArray();
     byte writeout[];
     System.out.println();
     int counter;
     int ackcount=0;
     byte[] ackmsg={0,4,0,(byte)ackcount};
     //for read request, we have to send the ACK packets with a 0 block and as stated in TFTP protocol
     //so for every write request, there 
     if (request==decision.read){
    	 //let us check the data packets
    	for(;;){
    	if ((receivedata[0]==0)&&(receivedata[1]==3)&&(receivedata[2]==(byte)0)&&(receivedata[3]==(byte)counter)){
    		BufferedOutputStream out =
    	            new BufferedOutputStream(new FileOutputStream("filename.txt"));
    				for(int i=3;i<receivedata.length;i++){writeout[i-3]=receivedata[i];}
    				out.write(writeout, (counter-1)*512,writeout.length );
    				ackcount++;
    	}else{break;}
    	
     }
     }
     

   

   // We're finished, so close the socket.
   sendReceiveSocket.close();
}

public static void main(String args[])
{
   Client c = new Client();
   c.sendAndReceive(decision.read);//send the type of request you want
}
}

	}


