

import java.io.*;
import java.net.*;
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
 * @version 5
 */
public class ErrorSim 
{   
	private static DatagramPacket receivePacket;  // packet received from client
	private static DatagramSocket receiveSocket;  // socket to receive packets from client

	private Scanner input;                   // scans user input in ui()
	public static final int MAX_DATA = 512;  // max number of bytes for data field in packet

	// choices when entering Error Simulation Mode
	public enum PacketType { RRQ, WRQ, ACK, DATA, ERROR }
	private static PacketType packetType = null;
	public enum PacketDo { lose, delay, duplicate, send, edit}
	private static PacketDo packetDo = null;
	private static boolean sendToServer = false; // true if we are sending to server, false if client
	private static int choiceInt = 0; // the number of the packet to be manipulated
	private static boolean eOpFlag = false; // make opcode invalid
	private static boolean eFnFlag = false; // change filename
	private static boolean eMdFlag = false; // make mode invalid
	private static int eBlockNumber = 70000;   // block number to change to
	private static boolean eDfFlag = false; // delete the data field in DATA
	private static int errorCode = 10;      // change error code
	private static String filename = null;  // filename for RRQ or WRQ to send
	private static int delay = 0;  // milliseconds to delay packet
	private static boolean unknownTID = false; // if true, send packet of choice from a different port

	public ErrorSim() 
	{		
		try {
			// create new socket to receive TFTP packets from Client
			receiveSocket = new DatagramSocket(68);

		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}   
	}

	public static void main( String args[] ) 
	{
		System.out.println("***** Welcome to Group #2's SYSC3303 TFTP Error Simulator Program *****\n");
		ErrorSim es = new ErrorSim();

		// start the user interface to determine how the user wants to start ErrorSim
		boolean errorSim = es.ui(); 

		// starts user input (for quitting error simulator)
		Quit quit = new Quit();		
		quit.start(); 

		Thread ConnectionThread;  // to facilitate transfer of files between client and server

		// receive packets and start new connections 
		while (true) {
			receivePacket = receive(receiveSocket); // receive packet on port 68, from Client			
			if (receivePacket == null) { return; }  // user pressed q to quit ErrorSim		

			int sendPort = receivePacket.getPort(); // port to send to during connection

			if (errorSim) {
				// start new connection to server in error simulation mode			
				ConnectionThread = new Thread(new ToServer(
						receivePacket, packetType, packetDo, sendToServer, 
						choiceInt, eOpFlag, eFnFlag, eMdFlag, eBlockNumber, eDfFlag,
						errorCode,filename, null, null, sendPort, delay, unknownTID), "TransferToServer");
				System.out.println("\nError Simulator: " + ConnectionThread.getName() + 
						ConnectionThread.getId() + " will connect to the server, in Error Simulation Mode...");
			} else {
				// start new connection to server in normal mode			
				ConnectionThread = new Thread(new ToServer(receivePacket, 
						null, null, sendPort), "TransferToServer" );
				System.out.println("\nError Simulator: " + ConnectionThread.getName() + 
						ConnectionThread.getId() + " will connect to the server, in Normal Mode...");
			}

			ConnectionThread.start();	// start new connection thread
		}
	}

	/**
	 * Gets receive socket.
	 * 
	 * @return receive socket
	 */
	public static DatagramSocket getSocket() 
	{
		return receiveSocket;
	}

	/**
	 * The simple console text user interface for the ErrorSim program.  
	 * User navigates through menus to create and send error packets.
	 * 
	 * @return	false for normal mode, true for error simulation
	 */
	public boolean ui() 
	{		
		// determine if user wants to start in normal mode or error simulation mode
		input = new Scanner(System.in);  // scans user input
		while (true) {
			System.out.println("\nWould you like to start in (N)ormal mode, or (E)rror simulation mode, or (Q)uit?");
			String choice = input.nextLine();  // user's choice
			if (choice.equalsIgnoreCase("N")) {         // normal mode
				System.out.println("\nError Simulator: You have chosen to start in Normal Mode.");
				return false;
			} else if (choice.equalsIgnoreCase("E")) {  // error simulation mode
				while (true) {
					// choose how to manipulate packet
					System.out.println("\nError Simulator: How would you like to manipulate packets?");
					System.out.println("\t 1. Lose a packet.");
					System.out.println("\t 2. Delay a packet.");
					System.out.println("\t 3. Duplicate a packet.");
					System.out.println("\t 4. Send a packet.");
					System.out.println("\t 5. Edit a packet.");
					System.out.println("(type the number corresponding to your choice...)");
					choice = input.nextLine();  // user's choice
					if (choice.equals("1")) { 
						packetDo = PacketDo.lose;
						break;
					} else if (choice.equals("2")) {
						packetDo = PacketDo.delay;
						break;
					} else if (choice.equals("3")) {
						packetDo = PacketDo.duplicate;
						break;
					} else if (choice.equals("4")) {
						packetDo = PacketDo.send;
						break;
					} else if (choice.equals("5")) {
						packetDo = PacketDo.edit;
						break;
					} else {
						System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
					}
				}
				while (true) {
					// choose type of packet
					System.out.println("\nError Simulator: What type of packet do you want to "
							+ packetDo.name() + "?");
					System.out.println("\t 1. RRQ packet.");
					System.out.println("\t 2. WRQ packet.");
					System.out.println("\t 3. DATA packet.");
					System.out.println("\t 4. ACK packet.");
					System.out.println("\t 5. ERROR packet.");
					System.out.println("(type the number corresponding to your choice...)");
					choice = input.nextLine();  // user's choice
					if (choice.equals("1")) { 
						packetType = PacketType.RRQ;
						break;
					} else if (choice.equals("2")) {
						packetType = PacketType.WRQ;
						break;
					} else if (choice.equals("3")) {
						packetType = PacketType.DATA;
						break;
					} else if (choice.equals("4")) {
						packetType = PacketType.ACK;
						break;
					} else if (choice.equals("5")) {
						packetType = PacketType.ERROR;
						break;
					} else {
						System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
					}
				}
				// delaying or duplicating a file
				if (packetDo == PacketDo.delay ||
						packetDo == PacketDo.duplicate) {
					while(true) {
						if (packetDo == PacketDo.delay) {
							System.out.println("\nError Simulator: How long of a delay do you want on your delayed packet? (in milliseconds)");
						} else {
							System.out.println("\nError Simulator: How long of a delay do you want between your duplicated packets? (in milliseconds)");
						}
						try {
							delay = Integer.parseInt(input.nextLine());  // user's choice
							break;
						} catch (NumberFormatException n) {
							System.out.println("\nI'm sorry, you must enter a number.  Please try again...");
						}				
					}
				}
				// sending a file
				if (packetDo == PacketDo.send) {
					if (packetType == PacketType.RRQ || 
							packetType == PacketType.WRQ) {
						System.out.println("\nError Simulator: What would you like the Filename to be?");
						filename = input.nextLine();	// user's choice
					} else if (packetType == PacketType.DATA ||
							packetType == PacketType.ACK) {
						while(true) {
							System.out.println("\nError Simulator: Enter a Block Number between 0-65535 for your "
									+ packetType.name() + " packet...");
							try {
								int bnInt = Integer.parseInt(input.nextLine());  // user's choice
								if (bnInt < 0 || bnInt > 65535) { 
									System.out.println("\nI'm sorry, " + bnInt + " is not a valid choice.  Please try again...");
								} else {
									eBlockNumber = bnInt;
									break;
								}	
							} catch (NumberFormatException n) {
								System.out.println("\nI'm sorry, you must enter a number.  Please try again...");
							}				
							break;
						}
					} else if (packetType == PacketType.ERROR) {
						while(true) {
							System.out.println("\nError Simulator: Enter an Error Code between 0-8 for your "
									+ packetType.name() + " packet (8 is an invalid TFTP Error Code)...");
							try {
								int ecInt = Integer.parseInt(input.nextLine());  // user's choice
								if (ecInt < 0 || ecInt > 8) { 
									System.out.println("\nI'm sorry, " + ecInt + " is not a valid choice.  Please try again...");
								} else {
									errorCode = ecInt;
									break;
								}	
							} catch (NumberFormatException n) {
								System.out.println("\nI'm sorry, you must enter a number.  Please try again...");
							}				
							break;
						}
					}
					// editing a file
				} else if (packetDo == PacketDo.edit) {
					// RRQ or WRQ menu
					if (packetType == PacketType.RRQ || 
							packetType == PacketType.WRQ) {
						while (true) {
							System.out.println("\nError Simulator: What would you like to edit in the " 
									+ packetType.name() + " packet?");
							System.out.println("\t 1. Make Opcode invalid.");
							System.out.println("\t 2. Replace first character of filename with a space (invalidating filename).");
							System.out.println("\t 3. Make Mode invalid.");
							System.out.println("\t 4. Change TID.");
							System.out.println("(type the number corresponding to your choice...)");
							choice = input.nextLine();  // user's choice
							if (choice.equals("1")) { 
								eOpFlag = true;
								break;
							} else if (choice.equals("2")) {
								eFnFlag = true;
								break;
							} else if (choice.equals("3")) {
								eMdFlag = true;
								break;
							} else if (choice.equals("4")) {
								unknownTID = true;
								break;
							} else {
								System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
							}
						}
						// DATA menu
					} else if (packetType == PacketType.DATA) {
						while (true) {
							System.out.println("\nError Simulator: What would you like to edit in the DATA packet?");
							System.out.println("\t 1. Make Opcode invalid.");
							System.out.println("\t 2. Change Block Number.");
							System.out.println("\t 3. Delete Data field.");
							System.out.println("\t 4. Change TID.");
							System.out.println("(type the number corresponding to your choice...)");
							choice = input.nextLine();  // user's choice
							if (choice.equals("1")) { 
								eOpFlag = true;
								break;
							} else if (choice.equals("2")) {
								// choose the block number
								while (true) {
									System.out.println("\nError Simulator: Enter a number between 0-65535 to change the Block Number to...");
									try {
										int bnInt = Integer.parseInt(input.nextLine());  // user's choice
										if (bnInt < 0 || bnInt > 65535) { 
											System.out.println("\nI'm sorry, " + bnInt + " is not a valid choice.  Please try again...");
										} else {
											eBlockNumber = bnInt;
											break;
										}	
									} catch (NumberFormatException n) {
										System.out.println("\nI'm sorry, you must enter a number.  Please try again...");
									}				
									break;
								}
								break;
							} else if (choice.equals("3")) {
								eDfFlag = true;
								break;
							} else if (choice.equals("4")) {
								unknownTID = true;
								break;
							} else {
								System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
							}
						}					
						// ACK menu
					} else if (packetType == PacketType.ACK) {
						while (true) {
							System.out.println("\nError Simulator: What would you like to edit in the ACK packet?");
							System.out.println("\t 1. Make Opcode invalid.");
							System.out.println("\t 2. Change Block Number.");
							System.out.println("\t 3. Change TID.");
							System.out.println("(type the number corresponding to your choice...)");
							choice = input.nextLine();  // user's choice
							if (choice.equals("1")) { 
								eOpFlag = true;
								break;
							} else if (choice.equals("2")) {
								// choose the block number
								while (true) {
									System.out.println("\nError Simulator: Enter a number between 0-65535 to change the Block Number to...");
									try {
										int bnInt = Integer.parseInt(input.nextLine());  // user's choice
										if (bnInt < 0 || bnInt > 65535) { 
											System.out.println("\nI'm sorry, " + bnInt + " is not a valid choice.  Please try again...");
										} else {
											eBlockNumber = bnInt;
											break;
										}	
									} catch (NumberFormatException n) {
										System.out.println("\nI'm sorry, you must enter a number.  Please try again...");
									}				
									break;
								}
								break;
							} else if (choice.equals("3")) {
								unknownTID = true;
								break;
							} else {
								System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
							}
						}
						// ERROR menu
					} else if (packetType == PacketType.ERROR) {
						while (true) {
							System.out.println("\nError Simulator: What would you like to edit in the ERROR packet?");
							System.out.println("\t 1. Make Opcode invalid.");
							System.out.println("\t 2. Change Error Code.");
							System.out.println("\t 3. Change TID.");
							System.out.println("(type the number corresponding to your choice...)");
							choice = input.nextLine();  // user's choice
							if (choice.equals("1")) { 
								eOpFlag = true;
								break;
							} else if (choice.equals("2")) {
								// choose the error code
								while (true) {
									System.out.println("\nError Simulator: Enter a number between 0-8 to change the Error Code to (8 is an invalid TFTP Error Code)...");
									try {
										int ecInt = Integer.parseInt(input.nextLine());  // user's choice
										if (ecInt < 0 || ecInt > 8) { 
											System.out.println("\nI'm sorry, " + ecInt + " is not a valid choice.  Please try again...");
										} else {
											errorCode = ecInt;
											break;
										}	
									} catch (NumberFormatException n) {
										System.out.println("\nI'm sorry, you must enter a number.  Please try again...");
									}				
									break;
								}
								break;
							} else if (choice.equals("3")) {
								unknownTID = true;
								break;
							} else {
								System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
							}
						}
					}
				}
				if ((packetDo == PacketDo.delay || 
						packetDo == PacketDo.duplicate ||
						packetDo == PacketDo.lose ||
						packetDo == PacketDo.edit) &&
						(packetType == PacketType.RRQ || 
						packetType == PacketType.WRQ)) {
					choiceInt = 1;
				} else if (packetDo == PacketDo.send) {
					while (true) {
						// choose when to send packet
						System.out.println("\nError Simulator: Enter a number to indicate when the Error Simulator should send the " 
								+ packetType + " packet.");
						System.out.println("(eg: enter '1' for the first packet sent to a specific host, etc.)");
						try {
							choiceInt = Integer.parseInt(input.nextLine());  // user's choice
							if (choiceInt < 1) { 
								System.out.println("\nI'm sorry, " + choiceInt + " is not a valid choice.  Please try again...");
							} else {
								break;
							}	
						} catch (NumberFormatException n) {
							System.out.println("\nI'm sorry, you must enter a number.  Please try again...");
						}				
					}
				} else {
					while (true) {
						// choose which packet to manipulate
						System.out.println("\nError Simulator: Enter a number to indicate which " 
								+ packetType + " packet to manipulate.");
						System.out.println("(eg: enter '1' for the first packet, etc.)");
						try {
							choiceInt = Integer.parseInt(input.nextLine());  // user's choice
							if (choiceInt < 1) { 
								System.out.println("\nI'm sorry, " + choiceInt + " is not a valid choice.  Please try again...");
							} else {
								break;
							}	
						} catch (NumberFormatException n) {
							System.out.println("\nI'm sorry, you must enter a number.  Please try again...");
						}				
					}
				}
				while (true) {
					// choose where the packets to manipulated are from and going
					if (packetDo != PacketDo.send) {
						System.out.println("\nError Simulator: Is the packet to be manipulated coming from the (C)lient, or the (S)erver?");
					} else { // word it differently if sending a packet
						System.out.println("\nError Simulator: Is the packet to be sent to the (C)lient, or to the (S)erver?");
					}
					choice = input.nextLine();  // user's choice
					if (choice.equalsIgnoreCase("C")) {
						sendToServer = true; // choice is Client 
						break;
					} else if (choice.equalsIgnoreCase("S")) {
						sendToServer = false; // choice is Server
						break;
					} else {
						System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
					}				
				}
				// print choice info to user
				System.out.println("\nError Simulator: You have chosen to start in Error Simulation Mode.");
				String host = null; // client or server
				if (sendToServer) {
					host = "Client";
				} else {
					host = "Server";
				}
				if (packetDo == PacketDo.delay ||
						packetDo == PacketDo.edit) { 
					System.out.println("\t The #" + choiceInt + " " + 
							packetType.name() + " packet, from the " + host + 
							", will be " + packetDo.name() + "ed.");
				} else if (packetDo == PacketDo.duplicate) {
					System.out.println("\t The #" + choiceInt + " " + 
							packetType.name() + " packet, from the " + host + 
							", will be duplicated.");
				} else if (packetDo == PacketDo.lose) {
					System.out.println("\t The #" + choiceInt + " " + 
							packetType.name() + " packet, from the " + host + 
							", will be lost.");
				} else if (packetDo == PacketDo.send) {
					System.out.println("\t The #" + choiceInt + 
							" packet sent to the " + host + " will be a " + 
							packetType.name() + " packet.");
				} 
				// because of wording differences between send and the rest of
				// the manipulation types
				if (packetDo == PacketDo.send) {
					sendToServer = !sendToServer;
				}
				
				return true; // error simulation mode was chosen

			} else if (choice.equalsIgnoreCase("Q")) {  // quit
				System.out.println("\nGoodbye!");
				System.exit(0);
			} else {
				System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
			}
		}
	}

	/**
	 * Receives DatagramPacket packets.
	 * 
	 * @param socket			the DatagramSocket to be receiving packets from
	 * @return DatagramPacket 	received
	 */
	public static DatagramPacket receive(DatagramSocket socket) 
	{
		// no packet will be larger than DATA packet
		// room for a possible maximum of 512 bytes of data + 4 bytes opcode 
		// and block number
		byte data[] = new byte[MAX_DATA + 4]; 
		DatagramPacket receivePacket = new DatagramPacket(data, data.length);

		while (true){
			try {
				// block until a DatagramPacket is received via sendSocket 
				System.out.println("\nError Simulator: Listening for packets...");
				socket.receive(receivePacket);
				
				byte[] packetData = receivePacket.getData();  // the received packet's data
				int op = twoBytesToInt(packetData[0], packetData[1]); // get the opcode
				String type = "";  // type of packet
				
				// get packet info based on type of TFTP packet
				switch (op) {
					case 1: type = "Read Request packet";
						break;
					case 2: type = "Write Request packet";
						break;
					case 3: type = "Data packet";
						break;
					case 4: type = "Acknowledgment packet";
						break;
					case 5: type = "Error packet";
						break;
					default: type = "Invalid TFTP packet";
						break;
				}				
				
				// print out packet info
				String direction = "<--";
				System.out.printf("Error Simulator: %30s %3s %-30s   bytes: %3d   - %s \n", 
						socket.getLocalSocketAddress(), direction, receivePacket.getSocketAddress(),
						receivePacket.getLength(), type);
				
				break;
			} catch(IOException e) {
				return null;  // socket was closed, return null
			}
		}

		return receivePacket;
	}
	
	/**
	 * Deals with two signed bytes and combines them into one int.
	 * 
	 * @param one	first byte
	 * @param two	second byte
	 * @return		two bytes combined into an int (value: 0 to 65535)
	 */
	public static int twoBytesToInt(byte one, byte two) 
	{
		// add 256 if needed to compensate for signed bytes in Java
		int value1 = one < 0 ? one + 256 : one;
		int value2 = two < 0 ? two + 256 : two;
		
		// left shift value1 and combine with value2 into one int
		return (value2 | (value1 << 8));
	}
}



/**
 * Receive from Client, send to Server.
 *
 */
class ToServer implements Runnable 
{	
	// UDP DatagramPackets and sockets used to send/receive
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket serverSocket, clientSocket, unknownSocket;

	int sendPort = 0;  // port to send to
	InetAddress saddr;

	// choices when entering Error Simulation Mode
	private ErrorSim.PacketType packetType = null;
	private ErrorSim.PacketDo packetDo = null;
	private boolean sendToServer = false; // true if sending to server, false if client
	private int packetNumber = 0; // number of packet to be manipulated
	boolean eOpFlag = false;   	  // change opcode 
	boolean eFnFlag = false;      // change filename
	boolean eMdFlag = false;      // change mode
	int eBlockNumber = 0;         // change block number
	boolean eDfFlag = false;      // delete data field
	int errorCode = 0;            // change error code
	String filename = "";         // change the filename in RRQ or WRQ
	private int delay = 0;        // milliseconds to delay packet
	boolean unknownTID = false;   // if true, send from a different port
	
	// counters
	private int packetCount = 0;  // number of packets received
	private int typeCount = 0;    // number of packets received of packetType type
	private InetAddress clientAddress;

	
	// max number of bytes for data field in packet
	public static final int MAX_DATA = 512;  

	/**
	 * ErrorSim started in Error Simulation Mode.
	 * This method is overloaded.
	 * 
	 * @param receivePacket		packet received by ErrorSim on port 68
	 * @param packetType		type of packet to manipulate
	 * @param packetDo			how to manipulate packet
	 * @param sendToServer		where to manipulate packet
	 * @param packetNumber		which packet to manipulate
	 * @param eOpFlag			change opcode to invalid
	 * @param eFnFlag			change filename to 'DOESNTEXIST'
	 * @param eMdFlag			change mode to invalid
	 * @param eBlockNumber		change block number to invalid
	 * @param eDfFlag			delete data field
	 * @param errorCode			change error code to this
	 * @param filename			change filename  to this
	 * @param serverSocket		socket to send to Server
	 * @param clientSocket		socket to receive from Client
	 * @param sendPort			port on Server to send packets to
	 * @param delay				milliseconds to delay packet for
	 * @param unknownTID 		if true, send from a different port
	 */
	public ToServer (DatagramPacket receivePacket, 
			ErrorSim.PacketType packetType, 
			ErrorSim.PacketDo packetDo, 
			boolean sendToServer,
			int packetNumber,
			boolean eOpFlag, 
			boolean eFnFlag, 
			boolean eMdFlag, 
			int eBlockNumber, 
			boolean eDfFlag, 
			int errorCode,
			String filename,
			DatagramSocket serverSocket,
			DatagramSocket clientSocket,
			int sendPort,
			int delay,
			boolean unknownTID) 
	{
		// create sockets for server and client connections		
		if (serverSocket == null) {
			try {			
				// create new socket to send/receive TFTP packets to/from Server
				serverSocket = new DatagramSocket();			
				// open new socket to send/receive to/from Client
				clientSocket = new DatagramSocket();			
			} catch (SocketException se) {
				se.printStackTrace();
				System.exit(1);
			} 
		}
		
		try {
			this.saddr = InetAddress.getByName("127.0.0.1");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// sockets
		this.serverSocket = serverSocket;
		this.clientSocket = clientSocket;

		this.sendPort = sendPort;  // port to send to

		// the original packet received on ErrorSim's port 68, from client
		this.receivePacket = receivePacket;  

		// choices made for error simulation
		this.packetType = packetType;
		this.packetDo = packetDo;
		this.sendToServer = sendToServer;
		this.packetNumber = packetNumber;
		this.eOpFlag = eOpFlag;
		this.eFnFlag = eFnFlag;
		this.eMdFlag = eMdFlag;
		this.eBlockNumber = eBlockNumber;
		this.eDfFlag = eDfFlag;
		this.errorCode = errorCode;
		this.filename = filename;
		this.delay = delay;
		this.unknownTID = unknownTID;
		
		if (unknownTID) {
			try {			
				// create new socket to send/receive TFTP packets to/from Server
				// using an unknown TID
				unknownSocket = new DatagramSocket();		
			} catch (SocketException se) {
				se.printStackTrace();
				System.exit(1);
			} 
		}
	}

	/**
	 * ErrorSim started in Normal Mode.
	 * This method is overloaded.
	 * 
	 * @param receivePacket	packet received by ErrorSim on port 68
	 * @param serverSocket	socket to send to Server
	 * @param clientSocket	socket to receive from Client
	 * @param sendPort		port on Server to send packets to
	 */
	public ToServer (DatagramPacket receivePacket,
			DatagramSocket serverSocket,
			DatagramSocket clientSocket,
			int sendPort) 
	{
		// create sockets for server and client connections			
		if (serverSocket == null) {
			try {			
				// create new socket to send/receive TFTP packets to/from Server
				serverSocket = new DatagramSocket();			
				// open new socket to send/receive to/from Client
				clientSocket = new DatagramSocket();			
			} catch (SocketException se) {
				se.printStackTrace();
				System.exit(1);
			} 
		}
		
		try {
			this.saddr = InetAddress.getByName("127.0.0.1");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// sockets
		this.serverSocket = serverSocket;
		this.clientSocket = clientSocket;

		this.sendPort = sendPort;  // port to send to

		// the original packet received on ErrorSim's port 68, from client
		this.receivePacket = receivePacket; 
	}

	public void run () 
	{
		/*
		 * NORMAL MODE
		 */
		if (!sendToServer) {
			// received data from DatagramPacket					
			byte[] received = new byte[receivePacket.getLength()];
			System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), received, 0, 
					receivePacket.getLength());

			// get port for ToClient
			int clientPort = receivePacket.getPort();
			clientAddress = receivePacket.getAddress();
			
			// passes Client's packet to Server
			send(received, saddr, 69, serverSocket); 

			// receive response from Server, in order to get port to send to later
			receivePacket = receive(serverSocket);
			received = processDatagram(receivePacket); // gets received data without extra null bytes
			sendPort = receivePacket.getPort();  // get port on Server to send to
			
			Thread ConnectionThread = null;
			
			if (packetDo == null) {
				// start new ToClient connection in normal mode			
				ConnectionThread = new Thread(new ToClient(
						receivePacket, serverSocket, clientSocket, clientPort, clientAddress), 
						"TransferToClient");
				System.out.println("\n" + threadName() + ": " + ConnectionThread.getName() + 
						ConnectionThread.getId() + " will connect to the client, in Normal Mode...");
			} else {
				// start new ToClient connection in error simulation mode			
				ConnectionThread = new Thread(new ToClient(
						receivePacket, packetType, packetDo, sendToServer, 
						packetNumber, eOpFlag, eFnFlag, eMdFlag, eBlockNumber, 
						eDfFlag, errorCode,filename, serverSocket, clientSocket, 
						clientPort,clientAddress, delay, unknownTID), "TransferToClient");
				System.out.println("\n" + threadName() + ": " + ConnectionThread.getName() + 
						ConnectionThread.getId() + " will connect to the client, in Error Simulation Mode...");	
			}


			ConnectionThread.start();	// start new connection ToClient thread 

			while (true) {	
				receivePacket = receive(clientSocket); // receive packet from Client
				received = processDatagram(receivePacket); // gets received data without extra null bytes

				// passes Client's packet to Server
				send(received, saddr, sendPort, serverSocket);


			}

			/*
			 * ERROR SIMULATION MODE
			 */
		} else {
			// received data from DatagramPacket					
			byte[] received = new byte[receivePacket.getLength()];
			System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), received, 0, 
					receivePacket.getLength());

			// get port for ToClient
			
			int clientPort = receivePacket.getPort();
			clientAddress=receivePacket.getAddress();
			
			// determines if packet received is the type of packet the user
			// wants to manipulate
			int op = twoBytesToInt(received[0], received[1]); // get opcode
			if (matchType(op)) {
				typeCount++;
				// user wants to manipulate this particular packet
				if (packetNumber == typeCount) {
					// manipulate packet
					received = action(received, 69);
				}
			} else if (packetDo == ErrorSim.PacketDo.send && 
					packetNumber == packetCount) {
				// create packet according to user specifications, to send
				byte[] createdPacket = createPacket();
				
				// passes Client's packet to Server
				if (createdPacket != null){
					send(createdPacket, saddr, 69, serverSocket); 
				}
			}	
			
			// passes Client's packet to Server
			if (received != null){
				send(received, saddr, 69, serverSocket); 
			}
			
			// receive response from Server, in order to get port to send to later
			receivePacket = receive(serverSocket);
			received = processDatagram(receivePacket); // gets received data without extra null bytes
			sendPort = receivePacket.getPort();  // get port on Server to send to
			
			// start new ToClient connection in error simulation mode			
			Thread ConnectionThread = new Thread(new ToClient(
					receivePacket, packetType, packetDo, sendToServer, 
					packetNumber, eOpFlag, eFnFlag, eMdFlag, eBlockNumber, 
					eDfFlag, errorCode,filename, serverSocket, clientSocket, 
					clientPort, clientAddress, delay, unknownTID), "TransferToClient");
			
			System.out.println("\n" + threadName() + ": " + ConnectionThread.getName() + 
					ConnectionThread.getId() + " will connect to the client, in Error Simulation Mode...");		

			ConnectionThread.start();	// start new connection ToClient thread 
			
			while (true) {	
				receivePacket = receive(clientSocket); // receive packet from Client
				received = processDatagram(receivePacket); // gets received data without extra null bytes

				// determines if packet received is the type of packet the user
				// wants to manipulate
				op = twoBytesToInt(received[0], received[1]); // get opcode
				if (matchType(op)) {
					typeCount++;
					// user wants to manipulate this particular packet
					if (packetNumber == typeCount) {
						// manipulate packet
						received = action(received, sendPort);
					}
				} else if (packetDo == ErrorSim.PacketDo.send && 
						packetNumber == packetCount) {
					// create packet according to user specifications, to send
					byte[] createdPacket = createPacket();
					
					// passes Client's packet to Server
					if (createdPacket != null){
						send(createdPacket, receivePacket.getAddress(), 
								sendPort, serverSocket); 
					}
				}
				
				// passes Client's packet to Server
				if (received != null){
					send(received, saddr, sendPort, serverSocket); 
				}
			}
		}
	}

	/**
	 * Gets a nicer looking thread name and id combo.
	 * 
	 * @return	thread name/id
	 */
	public String threadName () {
		return Thread.currentThread().getName() + Thread.currentThread().getId();
	}
	
	/**
	 * Makes an appropriately sized byte[] from a DatagramPacket
	 * 
	 * @param packet	the received DatagramPacket
	 * @return			the data from the DatagramPacket
	 */
	public byte[] processDatagram (DatagramPacket packet) 
	{
		int len = packet.getLength(); // number of data bytes in packet
		byte[] data = new byte[len];  // new byte[] for storing received data 
		System.arraycopy(packet.getData(), 0, data, 0, len);  // copy over data
		
		return data;
	}

	/**
	 * Determines what action the user wanted to take to manipulate packets, 
	 * and does it.
	 * 
	 * @param received	received data byte[]
	 * @return			received data byte[] after editing
	 */
	public byte[] action (byte[] received, int port) {
		if (packetDo == ErrorSim.PacketDo.delay) {
			// call the delay thread
			Thread DelayThread = new Thread(new Delay(received, 
					saddr, port, serverSocket, delay),
					"DelayThread");
			System.out.println(threadName() + ": " + DelayThread.getName() + 
					DelayThread.getId() + " will delay the packet for " + 
					delay/1000.0 + " seconds."); 
			DelayThread.start();
			return null;  // so delaying packet does not duplicate
		} else if (packetDo == ErrorSim.PacketDo.duplicate) {
			// re-send packet in another thread as well as in run() after this 
			// method returns
			Thread DuplicateThread = new Thread(new Delay(received, 
					saddr, port, serverSocket, delay),
					"DuplicationThread");
			System.out.println(threadName() + ": " + DuplicateThread.getName() + 
					DuplicateThread.getId() + " will send a duplicate of the packet after "
					+ delay/1000.0 + " seconds.");
			DuplicateThread.start();
		} else if (packetDo == ErrorSim.PacketDo.edit) {
			if (eOpFlag) {
				System.out.println(threadName() + ": Invalidating packet opcode.");
				received[1] = (byte)0; // change opcode to an invalid one.
			} else if (eFnFlag) {
				System.out.println(threadName() + ": Manipulating packet filename.");
				received[2] = (byte)32;// throw a space as the first letter in the filename. 
			} else if (eMdFlag) {
				// change mode
				System.out.println(threadName() + ": Invalidating packet mode.");
				byte[] errMode = new byte[0];
				String newMode = "wrongMode!";
				ByteArrayOutputStream rec = new ByteArrayOutputStream();
				try {
					rec.write(newMode.getBytes("US-ASCII"));
					rec.write(0);
				} catch (IOException e) { }
				errMode = rec.toByteArray();
				for (int i=received[1];i<=received.length-2;i++){
					if (received[i] == 0){
						byte[] errRec = new byte[i+errMode.length+1];
						System.arraycopy(received, 0, errRec, 0, i);
						System.arraycopy(errMode, 0, errRec, i+1, errMode.length);
						received = new byte [errRec.length];
						received = errRec;
					}
				}
			} else if (eBlockNumber != 70000) {
				System.out.println(threadName() + ": Changing packet's block number to "
						+ eBlockNumber + ".");
				received[2] = (byte)(eBlockNumber / 256);
				received[3] = (byte)(eBlockNumber % 256);
			} else if (eDfFlag) {
				System.out.println(threadName() + ": Deleting packet's data field.");
				// delete the DATA packet's data field
				byte[] temp = new byte[4];
				System.arraycopy(received, 0, temp, 0, 4);
				received = temp;
			} else if (errorCode != 10) {	
				System.out.println(threadName() + ": Changing packet's error code to " 
						+ errorCode + ".");
				received[2] = (byte)(errorCode / 256);
				received[3] = (byte)(errorCode % 256);
			}
		} else if (packetDo == ErrorSim.PacketDo.lose) {
			System.out.println(threadName() + ": Losing packet.");
			return null;
		} 
		
		return received;
	}
	
	/**
	 * If the user chooses to create a packet to send.
	 * 
	 * @return	packet data to send
	 */
	public byte[] createPacket() 
	{
		byte[] packetData = null; // packet data to send
		if (packetType == ErrorSim.PacketType.RRQ ||
				packetType == ErrorSim.PacketType.WRQ) {
			// change filename to filename
			String mode = "netascii";
			packetData=new byte[filename.length() + mode.length() + 4];
			
			// request opcode
			if (packetType == ErrorSim.PacketType.RRQ){
				System.out.println(threadName() + ": Creating RRQ packet.");
				packetData[0] = 0;
				packetData[1] = 1;
			} else {
				System.out.println(threadName() + ": Creating WRQ packet.");
				packetData[0] = 0;
				packetData[1] = 2;
			}
			
			// convert filename and mode to byte[], with proper encoding
			byte[] fn = null;	// filename
			byte[] md = null;	// mode
			try {
				fn = filename.getBytes("US-ASCII");
				md = mode.getBytes("US-ASCII");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			// add filename and mode to request 
			packetData[fn.length + 3] = 0;		
			System.arraycopy(fn,0,packetData,2,fn.length);
			System.arraycopy(md,0,packetData,fn.length+3,md.length);
			packetData[packetData.length-1] = 0;				
		} else if (packetType == ErrorSim.PacketType.DATA) {
			System.out.println(threadName() + ": Creating DATA packet.");
			String data = "*** ADDED SOME DATA ***";
			byte[] d = null;	// data
			try {
				d = data.getBytes("US-ASCII");
			} catch (UnsupportedEncodingException e) { }
			packetData = new byte[4 + d.length];
			packetData[0] = 0;
			packetData[1] = 3;
			packetData[2] = (byte)(eBlockNumber / 256);
			packetData[3] = (byte)(eBlockNumber % 256);
			System.arraycopy(d, 0, packetData, 4, d.length);				
		} else if (packetType == ErrorSim.PacketType.ACK) {
			System.out.println(threadName() + ": Creating ACK packet.");
			packetData = new byte[4];
			packetData[0] = 0;
			packetData[1] = 4;
			packetData[2] = (byte)(eBlockNumber / 256);
			packetData[3] = (byte)(eBlockNumber % 256);
		} else if (packetType == ErrorSim.PacketType.ERROR) {
			System.out.println(threadName() + ": Creating ERROR packet.");
			String message = "This is a message from your friendly neighbourhood Error Simulator.";
			byte[] msg = null;	// error message
			try {
				msg = message.getBytes("US-ASCII");
			} catch (UnsupportedEncodingException e) { }
			packetData = new byte[5 + msg.length];
			packetData[0] = 0;
			packetData[1] = 5;
			packetData[2] = (byte)(errorCode / 256);
			packetData[3] = (byte)(errorCode % 256);
			System.arraycopy(msg, 0, packetData, 4, msg.length);	
			packetData[packetData.length-1] = 0;
		}
	
		return packetData;
	}

	/** 
	 * Determines if the received packet type matches the type of packet that 
	 * the user wanted to manipulate.
	 * 
	 * @param op	received packet opcode byte
	 * @return		true if it matches, false if it is the wrong packet type
	 */
	public boolean matchType (int op) 
	{
		// determine which type of packet was received
		switch (op) {
		case 1: if (packetType == ErrorSim.PacketType.RRQ){ return true; }	// RRQ
		break;
		case 2: if (packetType == ErrorSim.PacketType.WRQ){ return true; }	// WRQ
		break;
		case 3:	if (packetType == ErrorSim.PacketType.DATA){ return true; }	// DATA
		break;
		case 4: if (packetType == ErrorSim.PacketType.ACK){ return true; }	// ACK
		break;
		case 5:	 if (packetType == ErrorSim.PacketType.ERROR){ return true; }	// ERROR
		break;
		default: 			// invalid opcode
			break;
		}
		return false;
	}

	/**
	 * Receives DatagramPacket packets.
	 * 
	 * @param socket			the DatagramSocket to be receiving packets from
	 * @return DatagramPacket 	received
	 */
	public DatagramPacket receive(DatagramSocket socket) 
	{
		// no packet will be larger than DATA packet
		// room for a possible maximum of 512 bytes of data + 4 bytes opcode 
		// and block number
		byte data[] = new byte[MAX_DATA + 4]; 
		DatagramPacket receivePacket = new DatagramPacket(data, data.length);

		while (true){
			try {
				// block until a DatagramPacket is received 
				System.out.println("\n" + threadName() + 
						": Listening for packets...");				
				socket.receive(receivePacket); // receive packet
			} catch(IOException e) {
				return null;  // socket was closed, return null
			}
				
				byte[] packetData = receivePacket.getData();  // the received packet's data
				int op = twoBytesToInt(packetData[0], packetData[1]); // get the opcode
				String type = "";  // type of packet
				
				// get packet info based on type of TFTP packet
				switch (op) {
					case 1: case 2: type = parseRequest(packetData);
						break;
					case 3: type = parseData(packetData);
						break;
					case 4: type = parseAck(packetData);
						break;
					case 5: type = parseError(packetData);
						break;
					default:
						break;
				}

				// print out packet info
				String direction = "<--";
				System.out.printf(threadName() + ": %30s %3s %-30s   bytes: %3d   - %s \n", 
						socket.getLocalSocketAddress(), direction, receivePacket.getSocketAddress(),
						receivePacket.getLength(), type);
				
				packetCount++;  // keep track of number of packets received

				break;	
		}
		return receivePacket;
	}

	/**
	 * Sends DatagramPackets.
	 * 
	 * @param data		data byte[] to be included in DatagramPacket
	 * @param addr		InetAddress to send packet to
	 * @param port		port to send packet to
	 * @param socket	DatagramSocket to send packets with
	 */
	public void send (byte[] data, InetAddress addr, int port, 
			DatagramSocket socket) 
	{	
		// create new DatagramPacket to send to client
		sendPacket = new DatagramPacket(data, data.length, addr, port);
		
		// type of packet being sent 
		int op = twoBytesToInt(data[0], data[1]);
		String type = "";
		switch (op) {
			case 1: case 2: type = parseRequest(data); 
				break;
			case 3: type = parseData(data);
				break;
			case 4: type = parseAck(data);
				break;
			case 5: type = parseError(data);
				break;
			default: 
				break;
		}

		// sending with unknown TID
		if ((matchType(op)) && 
				(packetDo == ErrorSim.PacketDo.edit) && 
				(packetNumber == typeCount) &&
				unknownTID == true) {
			System.out.println(threadName() + ": Changing socket to send from (will create an unknown TID).");
			socket = unknownSocket;  // change socket to send with 
		}

		// send the packet
		try {
			socket.send(sendPacket);
			// print out packet info
			String direction = "-->";
			System.out.printf(threadName() + ": %30s %3s %-30s   bytes: %3d   - %s \n", 
					socket.getLocalSocketAddress(), direction, sendPacket.getSocketAddress(),
					sendPacket.getLength(), type);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// receive with unknown TID
		if ((matchType(op)) && 
				(packetDo == ErrorSim.PacketDo.edit) && 
				(packetNumber == typeCount) &&
				unknownTID == true) {
			receive(socket); // receive error info
		}
	}
	
	/**
	 * Deals with two signed bytes and combines them into one int.
	 * 
	 * @param one	first byte
	 * @param two	second byte
	 * @return		two bytes combined into an int (value: 0 to 65535)
	 */
	public int twoBytesToInt(byte one, byte two) 
	{
		// add 256 if needed to compensate for signed bytes in Java
		int value1 = one < 0 ? one + 256 : one;
		int value2 = two < 0 ? two + 256 : two;
		
		// left shift value1 and combine with value2 into one int
		return (value2 | (value1 << 8));
	}
	
	/**
	 * Parse the read/write request byte[].
	 * 
	 * @param request	the read/write byte[]
	 * @return			String representation of RRQ or WRQ
	 */
	public String parseRequest(byte[] request) 
	{
		String req = "";  // request type
		
		// get opcode
		int op = twoBytesToInt(request[0], request[1]);
		
		// determine if RRQ or WRQ
		if (op == 1) {
			req = "Read Request   filename=\"";
		} else {
			req = "Write Request   filename=\"";
		}	
		
		String filename = getFilename(request);  // gets the filename
		String mode = getMode(request); // gets the mode
		
		return req + filename + "\"   mode=\"" + mode + "\"";
	}
	
	/**
	 * Gets the filename from the request packet.
	 * 
	 * @param received	the received data
	 * @return			the filename from the request packet as a String
	 */
	public String getFilename(byte[] received) 
	{		
		// find the end of filename
		int end = 2;	// end index of filename bytes
		for (int i = 2; i < received.length - 1; i++) {
			if  (received[i] == 0) {
				end = i;
			}
		}
		
		// byte[] to copy filename into
		byte[] file = new byte[end - 2];
		System.arraycopy(received, 2, file, 0, end - 2);
		
		// make a String out of byte[] for filename
		String filename = null;
		try {
			filename = new String(file, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}	
		
		return filename;
	}
	
	/**
	 * Gets the mode from the request packet.
	 * 
	 * @param received	the received data
	 * @return			the mode from the request packet as a String
	 */
	public String getMode(byte[] received) 
	{		
		int len = received.length; // length of data
		int f;	// filename finding index
		for (f = 2; f < len; f++) {
			if (received[f] == 0) {	// end of filename
				break;
			}
		}
		int m;	// mode finding index
		for (m = f + 1; m < len; m++) {
			if (received[m] == 0) {	// end of mode
				break;
			}
		}
		
		// byte[] to copy mode into
		byte[] md = new byte[m - f - 1];
		System.arraycopy(received, f + 1, md, 0, m - f - 1);
		
		// make a String out of byte[] for mode
		String mode = null;
		try {
			mode = new String(md, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return mode;
	}
	
	/**
	 * Parse the acknowledgment byte[].
	 * 
	 * @param ack	the acknowledge byte[]
	 * @return		String representation of ACK
	 */
	public String parseAck (byte[] ack) 
	{
		// get block number
		int blockNumber = twoBytesToInt(ack[2], ack[3]);
		
		return "Acknowledgment #" + Integer.toString(blockNumber);
	}
	
	/**
	 * Parse the data byte[].
	 * 
	 * @param data	the data byte[]
	 * @return		String representation of DATA
	 */
	public String parseData (byte[] data) 
	{
		// get block number
		int blockNumber = twoBytesToInt(data[2], data[3]);
		
		return "Data packet    #" + Integer.toString(blockNumber);
	}
	
	/**
	 * Parse the error byte[].
	 * 
	 * @param error	the error byte[]
	 * @return 		String representation of ERROR
	 */
	public String parseError (byte[] error) 
	{
		// get error code
		int errorCode = twoBytesToInt(error[2], error[3]);

		// get the error message
		byte[] errorMsg = new byte[error.length - 5];
		System.arraycopy(error, 4, errorMsg , 0, error.length - 5);
		String message = null;
		try {
			message = new String(errorMsg, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}		
		
		return "Error " + Integer.toString(errorCode) + ": \"" + message + "\"";
	}
}


/**
 * Receive from Server, send to Client.
 *
 */
class ToClient implements Runnable 
{	
	// UDP DatagramPackets and sockets used to send/receive
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket serverSocket, clientSocket, unknownSocket;

	int sendPort;  // port to send to

	// choices when entering Error Simulation Mode
	private ErrorSim.PacketType packetType = null;
	private ErrorSim.PacketDo packetDo = null;
	private boolean sendToServer = true; // true if sending to server, false if client
	private int packetNumber; // number of packet to be manipulated
	boolean eOpFlag;   // change opcode 
	boolean eFnFlag;   // change filename
	boolean eMdFlag;   // change mode
	int eBlockNumber;  // change block number
	boolean eDfFlag;   // delete data field
	int errorCode;     // change error code
	String filename;   // change the filename in RRQ or WRQ
	private int delay = 0;  // milliseconds to delay packet
	boolean unknownTID;  // if true, send from a different port
	
	// counters
	private int packetCount = 0;  // number of packets received
	private int typeCount = 0;    // number of packets received of packetType type
	private InetAddress clientAddress;

	// max number of bytes for data field in packet
	public static final int MAX_DATA = 512;  

	/**
	 * ErrorSim started in Error Simulation Mode.
	 * This method is overloaded.
	 * 
	 * @param receivePacket		packet received by ErrorSim on port 68
	 * @param packetType		type of packet to manipulate
	 * @param packetDo			how to manipulate packet
	 * @param sendToServer		where to manipulate packet
	 * @param packetNumber		which packet to manipulate
	 * @param eOpFlag			change opcode to invalid
	 * @param eFnFlag			change filename to 'DOESNTEXIST'
	 * @param eMdFlag			change mode to invalid
	 * @param eBlockNumber		change block number to invalid
	 * @param eDfFlag			delete data field
	 * @param errorCode			change error code to this
	 * @param filename			change filename  to this
	 * @param serverSocket		socket to receive from Server
	 * @param clientSocket		socket to send to Client
	 * @param sendPort			port on Client to send packets to
	 * @param delay				milliseconds to delay a packet
	 * @param unknownTID 		if true, send from a different port
	 */
	public ToClient (DatagramPacket receivePacket, 
			ErrorSim.PacketType packetType, 
			ErrorSim.PacketDo packetDo, 
			boolean sendToServer,
			int packetNumber,
			boolean eOpFlag, 
			boolean eFnFlag, 
			boolean eMdFlag, 
			int eBlockNumber, 
			boolean eDfFlag, 
			int errorCode,
			String filename,
			DatagramSocket serverSocket,
			DatagramSocket clientSocket,
			int sendPort,
			InetAddress clientAddress,
			int delay,
			boolean unknownTID) 
	{
		// sockets
		this.serverSocket = serverSocket;
		this.clientSocket = clientSocket;

		this.sendPort = sendPort;  // port to send to
		this.clientAddress=clientAddress;

		// the original packet received on ErrorSim's port 68, from client
		this.receivePacket = receivePacket;  

		// choices made for error simulation
		this.packetType = packetType;
		this.packetDo = packetDo;
		this.sendToServer = sendToServer;
		this.packetNumber = packetNumber;
		this.eOpFlag = eOpFlag;
		this.eFnFlag = eFnFlag;
		this.eMdFlag = eMdFlag;
		this.eBlockNumber = eBlockNumber;
		this.eDfFlag = eDfFlag;
		this.errorCode = errorCode;
		this.filename = filename;
		this.delay = delay;
		this.unknownTID = unknownTID;
		
		if (unknownTID) {
			try {			
				// create new socket to send/receive TFTP packets to/from Client
				// using an unknown TID
				unknownSocket = new DatagramSocket();		
			} catch (SocketException se) {
				se.printStackTrace();
				System.exit(1);
			} 
		}
	}

	/**
	 * ErrorSim started in Normal Mode.
	 * This method is overloaded.
	 * 
	 * @param receivePacket	packet received by ErrorSim on port 68
	 * @param serverSocket	socket to receive from Server
	 * @param clientSocket	socket to send to Client
	 * @param sendPort		port on Client to send packets to
	 */
	public ToClient (DatagramPacket receivePacket,
			DatagramSocket serverSocket,
			DatagramSocket clientSocket,
			int sendPort, InetAddress clientAddress) 
	{
		// sockets
		this.serverSocket = serverSocket;
		this.clientSocket = clientSocket;
		this.clientAddress=clientAddress;
		this.sendPort = sendPort;  // port to send to

		// the original packet received on ErrorSim's port 68, from client
		this.receivePacket = receivePacket; 
	}

	public void run () 
	{
		/*
		 *  NORMAL MODE
		 */
		if (packetDo == null || sendToServer) {
			// received data from DatagramPacket	
			byte[] received = new byte[receivePacket.getLength()];
			System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), 
					received, 0, receivePacket.getLength());

			while (true) {
				// passes Server's packet to Client
				send(received, clientAddress, sendPort, clientSocket);
				receivePacket = receive(serverSocket);  // receive packet from Server
				received = processDatagram(receivePacket); // print packet data to user
			}	

			/*
			 *  ERROR SIMULATION MODE
			 */
		} else {
			// received data from DatagramPacket	
			byte[] received = new byte[receivePacket.getLength()];
			System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), 
					received, 0, receivePacket.getLength());

			while (true) {				
				// determines if packet received is the type of packet the user
				// wants to manipulate
				int op = twoBytesToInt(received[0], received[1]); // get opcode
				if (matchType(op)) {
					typeCount++;
					// user wants to manipulate this particular packet
					if (packetNumber == typeCount) {
						// manipulate packet
						received = action(received, sendPort);
					}
				} else if (packetDo == ErrorSim.PacketDo.send && 
						packetNumber == packetCount) {
					// create packet according to user specifications, to send
					byte[] createdPacket = createPacket();
					
					// passes Server's packet to Client
					if (createdPacket != null){
						send(createdPacket, clientAddress, 
								sendPort, clientSocket); 
					}
				}
				
				// passes Server's packet to Client
				if (received != null){
					send(received, clientAddress, sendPort, clientSocket); 
				}
				
				receivePacket = receive(serverSocket);  // receive packet from Server
				received = processDatagram(receivePacket); 
			}
		}
	}

	/**
	 * Gets a nicer looking thread name and id combo.
	 * 
	 * @return	thread name/id
	 */
	public String threadName () {
		return Thread.currentThread().getName() + Thread.currentThread().getId();
	}

	/**
	 * Determines what action the user wanted to take to manipulate packets, 
	 * and does it.
	 * 
	 * @param received	received data byte[]
	 * @return			received data byte[] after editing
	 */
	public byte[] action (byte[] received, int port) {
		if (packetDo == ErrorSim.PacketDo.delay) {
			// call the delay thread
			Thread DelayThread = new Thread(new Delay(received, 
					clientAddress, port, clientSocket, delay),
					"DelayThread");
			System.out.println(threadName() + ": " + DelayThread.getName() + 
					DelayThread.getId() + " will delay the packet for " + 
					delay/1000.0 + " seconds."); 
			DelayThread.start();
			return null;  // so delaying packet does not duplicate
		} else if (packetDo == ErrorSim.PacketDo.duplicate) {
			// re-send packet in another thread as well as in run() after this 
			// method returns
			Thread DuplicateThread = new Thread(new Delay(received, 
					clientAddress, port, clientSocket, delay),
					"DuplicationThread");
			System.out.println(threadName() + ": " + DuplicateThread.getName() + 
					DuplicateThread.getId() + " will send a duplicate of the packet after "
					+ delay/1000.0 + " seconds.");
			DuplicateThread.start();
		} else if (packetDo == ErrorSim.PacketDo.edit) {
			if (eOpFlag) {
				System.out.println(threadName() + ": Invalidating packet opcode.");
				received[1] = (byte)0; // change opcode to an invalid one.
			} else if (eFnFlag) {
				System.out.println(threadName() + ": Manipulating packet filename.");
				received[2] = (byte)32;// throw a space as the first letter in the filename. 
			} else if (eMdFlag) {
				// change mode
				System.out.println(threadName() + ": Invalidating packet mode.");
				byte[] errMode = new byte[0];
				String newMode = "wrongMode!";
				ByteArrayOutputStream rec = new ByteArrayOutputStream();
				try {
					rec.write(newMode.getBytes("US-ASCII"));
					rec.write(0);
				} catch (IOException e) { }
				errMode = rec.toByteArray();
				for (int i=received[1];i<=received.length-2;i++){
					if (received[i] == 0){
						byte[] errRec = new byte[i+errMode.length+1];
						System.arraycopy(received, 0, errRec, 0, i);
						System.arraycopy(errMode, 0, errRec, i+1, errMode.length);
						received = new byte [errRec.length];
						received = errRec;
					}
				}
			} else if (eBlockNumber != 70000) {
				System.out.println(threadName() + ": Changing packet's block number to "
						+ eBlockNumber + ".");
				received[2] = (byte)(eBlockNumber / 256);
				received[3] = (byte)(eBlockNumber % 256);
			} else if (eDfFlag) {
				System.out.println(threadName() + ": Deleting packet's data field.");
				// delete the DATA packet's data field
				byte[] temp = new byte[4];
				System.arraycopy(received, 0, temp, 0, 4);
				received = temp;
			} else if (errorCode != 10) {	
				System.out.println(threadName() + ": Changing packet's error code to " 
						+ errorCode + ".");
				received[2] = (byte)(errorCode / 256);
				received[3] = (byte)(errorCode % 256);
			}
		} else if (packetDo == ErrorSim.PacketDo.lose) {
			System.out.println(threadName() + ": Losing packet.");
			return null;
		} 
		
		return received;
	}
	
	/**
	 * If the user chooses to create a packet to send.
	 * 
	 * @return	packet data to send
	 */
	public byte[] createPacket() 
	{
		byte[] packetData = null; // packet data to send
		if (packetType == ErrorSim.PacketType.RRQ ||
				packetType == ErrorSim.PacketType.WRQ) {
			// change filename to filename
			String mode = "netascii";
			packetData=new byte[filename.length() + mode.length() + 4];
			
			// request opcode
			if (packetType == ErrorSim.PacketType.RRQ) {
				System.out.println(threadName() + ": Creating RRQ packet.");
				packetData[0] = 0;
				packetData[1] = 1;
			} else {
				System.out.println(threadName() + ": Creating WRQ packet.");
				packetData[0] = 0;
				packetData[1] = 2;
			}
			
			// convert filename and mode to byte[], with proper encoding
			byte[] fn = null;	// filename
			byte[] md = null;	// mode
			try {
				fn = filename.getBytes("US-ASCII");
				md = mode.getBytes("US-ASCII");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			// add filename and mode to request 
			packetData[fn.length + 3] = 0;		
			System.arraycopy(fn,0,packetData,2,fn.length);
			System.arraycopy(md,0,packetData,fn.length+3,md.length);
			packetData[packetData.length-1] = 0;				
		} else if (packetType == ErrorSim.PacketType.DATA) {
			System.out.println(threadName() + ": Creating DATA packet.");
			String data = "*** ADDED SOME DATA ***";
			byte[] d = null;	// data
			try {
				d = data.getBytes("US-ASCII");
			} catch (UnsupportedEncodingException e) { }
			packetData = new byte[4 + d.length];
			packetData[0] = 0;
			packetData[1] = 3;
			packetData[2] = (byte)(eBlockNumber / 256);
			packetData[3] = (byte)(eBlockNumber % 256);
			System.arraycopy(d, 0, packetData, 4, d.length);
		} else if (packetType == ErrorSim.PacketType.ACK) {
			System.out.println(threadName() + ": Creating ACK packet.");
			packetData = new byte[4];
			packetData[0] = 0;
			packetData[1] = 4;
			packetData[2] = (byte)(eBlockNumber / 256);
			packetData[3] = (byte)(eBlockNumber % 256);
		} else if (packetType == ErrorSim.PacketType.ERROR) {
			System.out.println(threadName() + ": Creating ERROR packet.");
			String message = "This is a message from your friendly neighbourhood Error Simulator.";
			byte[] msg = null;	// error message
			try {
				msg = message.getBytes("US-ASCII");
			} catch (UnsupportedEncodingException e) { }
			packetData = new byte[5 + msg.length];
			packetData[0] = 0;
			packetData[1] = 5;
			packetData[2] = (byte)(errorCode / 256);
			packetData[3] = (byte)(errorCode % 256);
			System.arraycopy(msg, 0, packetData, 4, msg.length);	
			packetData[packetData.length-1] = 0;
		}
	
		return packetData;
	}

	/** 
	 * Determines if the received packet type matches the type of packet that 
	 * the user wanted to manipulate.
	 * 
	 * @param op	received packet opcode byte
	 * @return		true if it matches, false if it is the wrong packet type
	 */
	public boolean matchType (int op) 
	{
		// determine which type of packet was received
		switch (op) {
		case 1: if (packetType == ErrorSim.PacketType.RRQ){ return true; }	// RRQ
		break;
		case 2: if (packetType == ErrorSim.PacketType.WRQ){ return true; }	// WRQ
		break;
		case 3:	if (packetType == ErrorSim.PacketType.DATA){ return true; }	// DATA
		break;
		case 4: if (packetType == ErrorSim.PacketType.ACK){ return true; }	// ACK
		break;
		case 5:	 if (packetType == ErrorSim.PacketType.ERROR){ return true; }	// ERROR
		break;
		default: 			// invalid opcode
			break;
		}
		return false;
	}
	
	/**
	 * Receives DatagramPacket packets.
	 * 
	 * @param socket			the DatagramSocket to be receiving packets from
	 * @return DatagramPacket 	received
	 */
	public DatagramPacket receive(DatagramSocket socket) 
	{
		// no packet will be larger than DATA packet
		// room for a possible maximum of 512 bytes of data + 4 bytes opcode 
		// and block number
		byte data[] = new byte[MAX_DATA + 4]; 
		DatagramPacket receivePacket = new DatagramPacket(data, data.length);

		while (true){
			try {
				// block until a DatagramPacket is received 
				System.out.println("\n" + threadName() + 
						": Listening for packets...");
				
				socket.receive(receivePacket); // receive packet
				
				byte[] packetData = receivePacket.getData();  // the received packet's data
				int op = twoBytesToInt(packetData[0], packetData[1]); // get the opcode
				String type = "";  // type of packet
				
				// get packet info based on type of TFTP packet
				switch (op) {
					case 1: case 2: type = parseRequest(packetData);
						break;
					case 3: type = parseData(packetData);
						break;
					case 4: type = parseAck(packetData);
						break;
					case 5: type = parseError(packetData);
						break;
					default:
						break;
				}

				// print out packet info
				String direction = "<--";
				System.out.printf(threadName() + ": %30s %3s %-30s   bytes: %3d   - %s \n", 
						socket.getLocalSocketAddress(), direction, receivePacket.getSocketAddress(),
						receivePacket.getLength(), type);
				
				packetCount++;  // keep track of number of packets received

				break;
			} catch(IOException e) {
				return null;  // socket was closed, return null
			}
		}

		return receivePacket;
	}

	/**
	 * Makes an appropriately sized byte[] from a DatagramPacket
	 * 
	 * @param packet	the received DatagramPacket
	 * @return			the data from the DatagramPacket
	 */
	public byte[] processDatagram (DatagramPacket packet) 
	{
		byte[] data = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), packet.getOffset(), data, 0, 
				packet.getLength());

		return data;
	}

	/**
	 * Sends DatagramPackets.
	 * 
	 * @param data		data byte[] to be included in DatagramPacket
	 * @param addr		InetAddress to send packet to
	 * @param port		port to send packet to
	 * @param socket	DatagramSocket to send packets with
	 */
	public void send (byte[] data, InetAddress addr, int port, 
			DatagramSocket socket) 
	{	
		// create new DatagramPacket to send to client
		sendPacket = new DatagramPacket(data, data.length, addr, port);
		
		// type of packet being sent 
		int op = twoBytesToInt(data[0], data[1]);
		String type = "";
		switch (op) {
			case 1: case 2: type = parseRequest(data); 
				break;
			case 3: type = parseData(data);
				break;
			case 4: type = parseAck(data);
				break;
			case 5: type = parseError(data);
				break;
			default: 
				break;
		}

		// sending with unknown TID
		if ((matchType(op)) && 
				(packetDo == ErrorSim.PacketDo.edit) && 
				(packetNumber == typeCount) &&
				unknownTID == true) {
			System.out.println(threadName() + ": Changing socket to send from (will create an unknown TID).");
			socket = unknownSocket;  // change socket to send with 
		}

		// send the packet
		try {
			socket.send(sendPacket);
			// print out packet info
			String direction = "-->";
			System.out.printf(threadName() + ": %30s %3s %-30s   bytes: %3d   - %s \n", 
					socket.getLocalSocketAddress(), direction, sendPacket.getSocketAddress(),
					sendPacket.getLength(), type);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// receive with unknown TID
		if ((matchType(op)) && 
				(packetDo == ErrorSim.PacketDo.edit) && 
				(packetNumber == typeCount) &&
				unknownTID == true) {
			receive(socket); // receive error info
		}
	}
	
	/**
	 * Deals with two signed bytes and combines them into one int.
	 * 
	 * @param one	first byte
	 * @param two	second byte
	 * @return		two bytes combined into an int (value: 0 to 65535)
	 */
	public int twoBytesToInt(byte one, byte two) 
	{
		// add 256 if needed to compensate for signed bytes in Java
		int value1 = one < 0 ? one + 256 : one;
		int value2 = two < 0 ? two + 256 : two;
		
		// left shift value1 and combine with value2 into one int
		return (value2 | (value1 << 8));
	}
	
	/**
	 * Parse the read/write request byte[].
	 * 
	 * @param request	the read/write byte[]
	 * @return			String representation of RRQ or WRQ
	 */
	public String parseRequest(byte[] request) 
	{
		String req = "";  // request type
		
		// get opcode
		int op = twoBytesToInt(request[0], request[1]);
		
		// determine if RRQ or WRQ
		if (op == 1) {
			req = "Read Request   filename=\"";
		} else {
			req = "Write Request   filename=\"";
		}	
		
		String filename = getFilename(request);  // gets the filename
		String mode = getMode(request); // gets the mode
		
		return req + filename + "\"   mode=\"" + mode + "\"";
	}
	
	/**
	 * Gets the filename from the request packet.
	 * 
	 * @param received	the received data
	 * @return			the filename from the request packet as a String
	 */
	public String getFilename(byte[] received) 
	{		
		// find the end of filename
		int end = 2;	// end index of filename bytes
		for (int i = 2; i < received.length - 1; i++) {
			if  (received[i] == 0) {
				end = i;
			}
		}
		
		// byte[] to copy filename into
		byte[] file = new byte[end - 2];
		System.arraycopy(received, 2, file, 0, end - 2);
		
		// make a String out of byte[] for filename
		String filename = null;
		try {
			filename = new String(file, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}	
		
		return filename;
	}
	
	/**
	 * Gets the mode from the request packet.
	 * 
	 * @param received	the received data
	 * @return			the mode from the request packet as a String
	 */
	public String getMode(byte[] received) 
	{		
		int len = received.length; // length of data
		int f;	// filename finding index
		for (f = 2; f < len; f++) {
			if (received[f] == 0) {	// end of filename
				break;
			}
		}
		int m;	// mode finding index
		for (m = f + 1; m < len; m++) {
			if (received[m] == 0) {	// end of mode
				break;
			}
		}
		
		// byte[] to copy mode into
		byte[] md = new byte[m - f - 1];
		System.arraycopy(received, f + 1, md, 0, m - f - 1);
		
		// make a String out of byte[] for mode
		String mode = null;
		try {
			mode = new String(md, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return mode;
	}
	
	/**
	 * Parse the acknowledgment byte[].
	 * 
	 * @param ack	the acknowledge byte[]
	 * @return		String representation of ACK
	 */
	public String parseAck (byte[] ack) 
	{
		// get block number
		int blockNumber = twoBytesToInt(ack[2], ack[3]);
		
		return "Acknowledgment #" + Integer.toString(blockNumber);
	}
	
	/**
	 * Parse the data byte[].
	 * 
	 * @param data	the data byte[]
	 * @return		String representation of DATA
	 */
	public String parseData (byte[] data) 
	{
		// get block number
		int blockNumber = twoBytesToInt(data[2], data[3]);
		
		return "Data packet    #" + Integer.toString(blockNumber);
	}
	
	/**
	 * Parse the error byte[].
	 * 
	 * @param error	the error byte[]
	 * @return 		String representation of ERROR
	 */
	public String parseError (byte[] error) 
	{
		// get error code
		int errorCode = twoBytesToInt(error[2], error[3]);

		// get the error message
		byte[] errorMsg = new byte[error.length - 5];
		System.arraycopy(error, 4, errorMsg , 0, error.length - 5);
		String message = null;
		try {
			message = new String(errorMsg, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}		
		
		return "Error " + Integer.toString(errorCode) + ": \"" + message + "\"";
	}
}


/**
 * Delays and sends a packet.
 * 
 */
class Delay implements Runnable
{	
	private int delay = 2500;  // milliseconds to delay packet
	byte[] data;               // data to put in packet
	InetAddress addr;          // InetAddress to send packet to
	int port;                  // port to send packet to
	DatagramSocket socket;     // socket to send packet from
	DatagramPacket sendPacket; // packet to delay and send

	public Delay (byte[] data, InetAddress addr, int port, 
			DatagramSocket socket, int delay)
	{
		this.data = data;
		this.addr = addr;
		this.port = port;
		this.socket = socket;
		this.delay = delay;
	}

	public void run() 
	{
		// delay the packet
		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		// send the delayed packet

		// create new DatagramPacket to send to client
		sendPacket = new DatagramPacket(data, data.length, addr, port);

		byte[] packetData = sendPacket.getData();  // the packet's data
		int op = twoBytesToInt(packetData[0], packetData[1]); // get the opcode
		String type = "";  // type of packet
		
		// get packet info based on type of TFTP packet
		switch (op) {
			case 1: type = "Read Request packet";
				break;
			case 2: type = "Write Request packet";
				break;
			case 3: type = "Data packet";
				break;
			case 4: type = "Acknowledgment packet";
				break;
			case 5: type = "Error packet";
				break;
			default: type = "Invalid TFTP packet";
				break;
		}			
		
		// send the packet
		try {
			// packet delay info
			System.out.println("\n" + threadName() + ": Packet Delayed for "
					+ delay/1000.0 + " seconds.");
			
			socket.send(sendPacket); // send the packet
			
			// print out packet info
			String direction = "-->";
			System.out.printf(threadName() + ": %30s %3s %-30s   bytes: %3d   - %s \n", 
					socket.getLocalSocketAddress(), direction, sendPacket.getSocketAddress(),
					sendPacket.getLength(), type);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * Gets a nicer looking thread name and id combo.
	 * 
	 * @return	thread name/id
	 */
	public String threadName () 
	{
		return Thread.currentThread().getName() + Thread.currentThread().getId();
	}
	
	/**
	 * Deals with two signed bytes and combines them into one int.
	 * 
	 * @param one	first byte
	 * @param two	second byte
	 * @return		two bytes combined into an int (value: 0 to 65535)
	 */
	public static int twoBytesToInt(byte one, byte two) 
	{
		// add 256 if needed to compensate for signed bytes in Java
		int value1 = one < 0 ? one + 256 : one;
		int value2 = two < 0 ? two + 256 : two;
		
		// left shift value1 and combine with value2 into one int
		return (value2 | (value1 << 8));
	}
}



/**
 * Deal with user input to quit while ErrorSim is listening for requests on 
 * port 68.
 *
 */
class Quit extends Thread 
{
	// scans user input when determining if ErrorSim should shut down
	private Scanner input;  

	public Quit() 
	{
		System.out.println("\nPress 'Q' at any time to quit.");		
	}

	public void run() 
	{
		input = new Scanner(System.in);	// scan user input
		while (true) {			
			String choice = input.nextLine();			// user's choice
			if (choice.equalsIgnoreCase("Q")) {			// Quit
				break;
			}
		}
		System.out.println("\nError Simulator: Goodbye!");
		// get ErrorSim's receive socket
		DatagramSocket socket = ErrorSim.getSocket();
		// close ErrorSim's receive socket
		socket.close(); 
		// close user input thread
		Thread.currentThread().interrupt();           
	}
}




