
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;


public class Server {
	
	DatagramPacket receivePacket;	// to receive DatagramPackets from Client
	DatagramSocket receiveSocket;	// Client sends to port 69
	
	/**
	 * opcodes for the different DatagramPackets packets in TFTP
	 */
	public enum Opcode {
		RRQ ((byte)1),
		WRQ ((byte)2),
		DATA ((byte)3),
		ACK ((byte)4),
		ERROR ((byte)5);
		
		private final byte op;
		
		Opcode (byte op) {
			this.op = op;
		}
		
		private byte op () { return op; }		
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
	
	public static void main(String[] args) throws Exception {
		Server s = new Server();		
		s.listener();	// start listening for DatagramPackets
	}
	
	/**
	 * Listens for new DatagramPackets on port 69, and verifies them.
	 * 
	 * @throws Exception	invalid packet received 
	 */
	public void listener() throws Exception {
		while (true) {	// keep listening on port 69 for new requests 
			DatagramPacket datagram = receive();	// gets received DatagramPacket
			byte[] request = process(datagram);		// received request packet turned into byte[]
			Opcode op = parse(request);				// check type and validity of request
			
			// deal with request based on opcode
			if (op == Opcode.RRQ || op == Opcode.WRQ) {	// request was RRQ or WRQ
				makeConnection(datagram);				// set up new connection thread to transfer file
			} else if (op == Opcode.ERROR) {			// ERROR packet was received instead
				byte errorCode = parseError(request);	// determine Error Code
				// deal with ERROR based on Error Code
				if (errorCode == 1) {
					
				}
			} else {									// invalid packet received
				throw new Exception ("Improperly formatted packet received.");
			}
		}		
	}
	
	/**
	 * Starts and sends packet to new ClientConnection thread,
	 * so server can go back to listening for new packets.
	 * 
	 * @param receivePacket	DatagramPacket received by server on port 69
	 */
	public void makeConnection (DatagramPacket receivePacket) {
		// create new thread to communicate with Client and transfer file
		// pass it DatagramPacket that was received				
		Thread clientConnectionThread = new Thread(
				new ClientConnection(receivePacket), "Client Connection Thread");
		System.out.println("\nServer: Packet Sent for Processing: ");
		clientConnectionThread.start();	// start new connection thread
	}
	
	/**
	 * Receives DatagramPacket.
	 * 
	 * @return DatagramPacket received
	 */
	public DatagramPacket receive() {
		// TODO
		return null;
	}
	
	/**
	 * Gets byte[] from DatagramPacket.
	 * 
	 * @param receivePacket	DatagramPacket received
	 * @return				byte[] containing the data from the DatagramPacket
	 */
	public byte[] process (DatagramPacket receivePacket) {
		// TODO return contents of DatagramPacket as byte[]
		return null;
	}
	
	/**
	 * Parses the received byte[], and determines what type of packet it was from.
	 * 
	 * @param received	the byte[] received from a DatagramPacket
	 * @return			the Opcode pertaining to the received byte[]
	 */
	public Opcode parse (byte[] received) {
		// TODO return Opcode for the type of byte[] received
		return null;
	}
	
	/**
	 * Parses the error byte[] and determines the type of error received.
	 * 
	 * @param error	the error byte[] that was received in a DatagramPacket
	 * @return		the byte pertaining to the Error Code of the received packet
	 */
	public byte parseError (byte[] error) {
		// TODO return TFTP Error Code byte
		return (byte)0;
	}
}

class ClientConnection implements Runnable {
	
	public ClientConnection(DatagramPacket receivePacket) {
		// TODO
	}
	
	public void run() {
		// TODO
	}
}
	
	