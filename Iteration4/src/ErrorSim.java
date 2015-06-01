

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
 * @version 4
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
	private static boolean choiceIsServer = true; // true if choice is server, false if client
	private static int choiceInt = 0; // the number of the packet to be manipulated
	private static boolean eOpFlag = false; // make opcode invalid
	private static boolean eFnFlag = false; // change filename
	private static boolean eMdFlag = false; // make mode invalid
	private static byte eBlockNumber = -1;   // block number to change to
	private static boolean eDfFlag = false; // delete the data field in DATA
	private static byte errorCode = -1;      // change error code
	private static String filename = null;  // filename for RRQ or WRQ to send
   
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
					receivePacket, packetType, packetDo, choiceIsServer, 
					choiceInt, eOpFlag, eFnFlag, eMdFlag, eBlockNumber, eDfFlag,
					errorCode,filename, null, null, sendPort), "TransferToServer");
				System.out.println("\nError Simulator: New File Transfer Starting to Server, in Error Simulation Mode... ");			
			} else {
				// start new connection to server in normal mode			
				ConnectionThread = new Thread(new ToServer(receivePacket, 
						null, null, sendPort), "TransferToServer" );
				System.out.println("\nError Simulator: New File Transfer Starting to Server, in Normal Mode... ");		
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
					System.out.println("(type the number corresponding to your choice...");
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
					System.out.println("(type the number corresponding to your choice...");
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
				// sending a file
				if (packetDo == PacketDo.send) {
					if (packetType == PacketType.RRQ || 
							packetType == PacketType.WRQ) {
						System.out.println("\nError Simulator: What would you like the Filename to be?");
						filename = input.nextLine();	// user's choice
					} else if (packetType == PacketType.DATA ||
							packetType == PacketType.ACK) {
						while(true) {
							System.out.println("\nError Simulator: Enter a Block Number between 0-127 for your "
									+ packetType.name() + " packet...");
							try {
								int bnInt = Integer.parseInt(input.nextLine());  // user's choice
								if (bnInt < 0 || bnInt > 127) { 
									System.out.println("\nI'm sorry, " + bnInt + " is not a valid choice.  Please try again...");
								} else {
									eBlockNumber = (byte)bnInt;
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
									errorCode = (byte)ecInt;
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
							System.out.println("\t 2. Change filename to 'DOESNTEXIST'.");
							System.out.println("\t 3. Make Mode invalid.");
							System.out.println("(type the number corresponding to your choice...");
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
							System.out.println("(type the number corresponding to your choice...");
							choice = input.nextLine();  // user's choice
							if (choice.equals("1")) { 
								eOpFlag = true;
								break;
							} else if (choice.equals("2")) {
								// choose the block number
								while (true) {
									System.out.println("\nError Simulator: Enter a number between 0-127 to change the Block Number to...");
									try {
										int bnInt = Integer.parseInt(input.nextLine());  // user's choice
										if (bnInt < 0 || bnInt > 127) { 
											System.out.println("\nI'm sorry, " + bnInt + " is not a valid choice.  Please try again...");
										} else {
											eBlockNumber = (byte)bnInt;
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
							System.out.println("(type the number corresponding to your choice...");
							choice = input.nextLine();  // user's choice
							if (choice.equals("1")) { 
								eOpFlag = true;
								break;
							} else if (choice.equals("2")) {
								// choose the block number
								while (true) {
									System.out.println("\nError Simulator: Enter a number between 0-127 to change the Block Number to...");
									try {
										int bnInt = Integer.parseInt(input.nextLine());  // user's choice
										if (bnInt < 0 || bnInt > 127) { 
											System.out.println("\nI'm sorry, " + bnInt + " is not a valid choice.  Please try again...");
										} else {
											eBlockNumber = (byte)bnInt;
											break;
										}	
									} catch (NumberFormatException n) {
										System.out.println("\nI'm sorry, you must enter a number.  Please try again...");
									}				
									break;
								}
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
							System.out.println("(type the number corresponding to your choice...");
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
											errorCode = (byte)ecInt;
											break;
										}	
									} catch (NumberFormatException n) {
										System.out.println("\nI'm sorry, you must enter a number.  Please try again...");
									}				
									break;
								}
								break;
							} else {
								System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
							}
						}
					}
				}
				while (true) {
					// choose which packet to manipulate
					System.out.println("\nError Simulator: Enter a number to indicate which " 
							+ packetType + " packet to manipulate.");
					System.out.println("(eg: enter '1' for the first packet, etc.");
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
				while (true) {
					// choose where the packets to manipulated are from and going
					System.out.println("\nError Simulator: Is the packet to be manipulated coming from the (C)lient, or the (S)erver?");
					choice = input.nextLine();  // user's choice
					if (choice.equalsIgnoreCase("C")) {
						choiceIsServer = false; // choice is Client 
						break;
					} else if (choice.equalsIgnoreCase("S")) {
						choiceIsServer = true; // choice is Server
						break;
					} else {
						System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
					}				
				}
				// print choice info to user
				System.out.println("\nError Simulator: You have chosen to start in Error Simulation Mode.");
				String host = null; // client or server
				if (choiceIsServer) {
					host = "Server";
				} else {
					host = "Client";
				}
				if (packetDo == PacketDo.delay || 
						packetDo == PacketDo.duplicate ||
						packetDo == PacketDo.edit) { 
					System.out.println("\t The #" + choiceInt + " " + 
							packetType.name() + " packet, from the " + host + 
							", will be " + packetDo.name() + "ed.");
				} else if (packetDo == PacketDo.lose) {
					System.out.println("\t The #" + choiceInt + " " + 
							packetType.name() + " packet, from the " + host + 
							", will be lost.");
				} else if (packetDo == PacketDo.send) {
					System.out.println("\t The #" + choiceInt + 
							" packet sent to the " + host + " will be a " + 
							packetType.name() + " packet.");
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
				
				// print out thread and port info
				System.out.println("\nError Simulator: packet received: ");
				System.out.println("From host: " + receivePacket.getAddress() + 
						" : " + receivePacket.getPort());
				System.out.print("Containing " + receivePacket.getLength() + 
						" bytes: \n");
				
				// get data from packet
				byte[] packetData = new byte[receivePacket.getLength()];
				System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), 
						packetData, 0, receivePacket.getLength());
				
				// display data to user
				System.out.println(Arrays.toString(packetData));
				
				
				break;
			} catch(IOException e) {
				return null;  // socket was closed, return null
			}
		}
		
		return receivePacket;
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
	private DatagramSocket serverSocket, clientSocket;
	
	int sendPort;  // port to send to
	
	// choices when entering Error Simulation Mode
	private ErrorSim.PacketType packetType = null;
	private ErrorSim.PacketDo packetDo = null;
	private boolean choiceIsServer; // true if choice is server, false if client
	private int packetNumber; // number of packet to be manipulated
	private int actionCount; // count packets of one type, in order to tell when to take action
	boolean eOpFlag;   // change opcode 
	boolean eFnFlag;   // change filename
	boolean eMdFlag;   // change mode
	byte eBlockNumber; // change block number
	boolean eDfFlag;   // delete data field
	byte errorCode;    // change error code
	String filename;   // change the filename in RRQ or WRQ
	
	// check if ErrorSim action took place, so it is only done once
	private boolean actionFlag = false;
	
	// max number of bytes for data field in packet
	public static final int MAX_DATA = 512;  
	
	/**
	 * ErrorSim started in Error Simulation Mode.
	 * This method is overloaded.
	 * 
	 * @param receivePacket		packet received by ErrorSim on port 68
	 * @param packetType		type of packet to manipulate
	 * @param packetDo			how to manipulate packet
	 * @param choiceIsServer	where to manipulate packet
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
	 */
	public ToServer (DatagramPacket receivePacket, 
						ErrorSim.PacketType packetType, 
						ErrorSim.PacketDo packetDo, 
						boolean choiceIsServer,
						int packetNumber,
						boolean eOpFlag, 
						boolean eFnFlag, 
						boolean eMdFlag, 
						byte eBlockNumber, 
						boolean eDfFlag, 
						byte errorCode,
						String filename,
						DatagramSocket serverSocket,
						DatagramSocket clientSocket,
						int sendPort) 
	{
		/* If this Connection thread is to send to Server, the sockets will be
		   null and must be created.
		   
		   If this Connection thread is to send to Client, sockets will have 
		   been passed to it, and will not need to be created.
		*/ 		
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
		
		// sockets
		this.serverSocket = serverSocket;
		this.clientSocket = clientSocket;
		
		this.sendPort = sendPort;  // port to send to
		
		// the original packet received on ErrorSim's port 68, from client
		this.receivePacket = receivePacket;  
		
		// choices made for error simulation
		this.packetType = packetType;
		this.packetDo = packetDo;
		this.choiceIsServer = choiceIsServer;
		this.packetNumber = packetNumber;
		this.eOpFlag = eOpFlag;
		this.eFnFlag = eFnFlag;
		this.eMdFlag = eMdFlag;
		this.eBlockNumber = eBlockNumber;
		this.eDfFlag = eDfFlag;
		this.errorCode = errorCode;
		this.filename = filename;
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
		/* If this Connection thread is to send to Server, the sockets will be
			null and must be created.

		   If this Connection thread is to send to Client, sockets will have 
			been passed to it, and will not need to be created.
		 */ 		
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
		if (packetDo == null) {
			// received data from DatagramPacket					
			byte[] received = new byte[receivePacket.getLength()];
			System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), received, 0, 
					receivePacket.getLength());
		
			// get port for ToClient
			int clientPort = receivePacket.getPort();
					
			// passes Client's packet to Server
			send(received, receivePacket.getAddress(), 69, serverSocket); 
		
			// receive response from Server, in order to get port to send to later
			receivePacket = receive(serverSocket);
			received = processDatagram(receivePacket);  // print packet data to user
			sendPort = receivePacket.getPort();  // get port on Server to send to
			
			// start new ToClient connection in normal mode			
			Thread ConnectionThread = new Thread(new ToClient(
					receivePacket, serverSocket, clientSocket, clientPort), 
					"TransferToClient");
			System.out.println("\n" + threadName() + 
					": File Transfer Continuing to Client, in Normal Mode... ");			
		
	
			ConnectionThread.start();	// start new connection ToClient thread 
		
			while (true) {	
				receivePacket = receive(clientSocket); // receive packet from Client
				received = processDatagram(receivePacket); // print packet data to user
			
				// passes Client's packet to Server
				send(received, receivePacket.getAddress(), sendPort, serverSocket);
				
				
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
					
			// passes Client's packet to Server
			send(received, receivePacket.getAddress(), 69, serverSocket); 
		
			// receive response from Server, in order to get port to send to later
			receivePacket = receive(serverSocket);
			received = processDatagram(receivePacket);  // print packet data to user
			sendPort = receivePacket.getPort();  // get port on Server to send to
				
			// start new ToClient connection in error simulation mode			
			Thread ConnectionThread = new Thread(new ToClient(
					receivePacket, packetType, packetDo, choiceIsServer, 
					packetNumber, eOpFlag, eFnFlag, eMdFlag, eBlockNumber, 
					eDfFlag, errorCode,filename, serverSocket, clientSocket, 
					clientPort), "TransferToClient");
			System.out.println("\n" + threadName() + 
					": File Transfer Continuing to Client, in Error Simulation Mode... ");			
		
			ConnectionThread.start();	// start new connection ToClient thread 
			
			//TODO - PART OF TEST 
			boolean happenOnce = false;
			
			while (true) {	
				receivePacket = receive(clientSocket); // receive packet from Client
				received = processDatagram(receivePacket); // print packet data to user
				
				// passes Client's packet to Server
				//send(received, receivePacket.getAddress(), sendPort, serverSocket);
				
				//this is where the action method will be called
				if (matchType(received[1])) {
					action(received);
				}
				
				//TODO - TEST - sending delayed packet once:
				if (!happenOnce) {
					Thread DelayThread = new Thread(new Delay(received, 
						receivePacket.getAddress(), sendPort, serverSocket),
						"DelayThread");
					DelayThread.start();
					happenOnce = true;
				} else {
					send(received, receivePacket.getAddress(), sendPort, serverSocket);
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
	
	public void action (byte[] received)
	{
		if (packetDo == ErrorSim.PacketDo.delay) {
			// TODO: call the delay thread
		} else if (packetDo == ErrorSim.PacketDo.duplicate) {
			// TODO: resend data by calling the send method twice
		} else if (packetDo == ErrorSim.PacketDo.edit) {
			if (eOpFlag) {
				// change opcode
			} else if (eFnFlag) {
				// change filename to 'DOESNTEXIST'
			} else if (eMdFlag) {
				// change mode
			} else if (eBlockNumber != -1) {
				// change block number to eBlockNumber
			} else if (eDfFlag) {
				// delete data field
			} else if (errorCode != -1) {
				// change error code to errorCode
			}
		} else if (packetDo == ErrorSim.PacketDo.lose) {
			return;
		} else if (packetDo == ErrorSim.PacketDo.send) {
			if (packetType == ErrorSim.PacketType.RRQ ||
					packetType == ErrorSim.PacketType.WRQ) {
				// change filename to filename
			} else if (packetType == ErrorSim.PacketType.DATA || 
					packetType == ErrorSim.PacketType.ACK) {
				// change blocknumber to eBlockNumber
			} else if (packetType == ErrorSim.PacketType.ERROR) {
				// change error code to errorCode
			}
		}			
	}
	
	public boolean matchType (byte op) {
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
				socket.receive(receivePacket);
				
				// print out thread and port info
				System.out.println("\n" + threadName() + ": packet received: ");
				System.out.println("From host: " + receivePacket.getAddress() + 
						" : " + receivePacket.getPort());
				System.out.print("Containing " + receivePacket.getLength() + 
						" bytes: \n");
				
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
		
		// display info to user
		System.out.println(Arrays.toString(data));
		
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
		
		// print out packet info to user
		System.out.println("\n" + threadName() + ": Sending packet: ");
		System.out.println("To host: " + addr + " : " + port);
		System.out.print("Containing " + sendPacket.getLength() + " bytes: \n");
		System.out.println(Arrays.toString(data) + "\n");
		
		// send the packet
		try {
			socket.send(sendPacket);
			System.out.println(threadName() + ": Packet sent using port " 
					+ socket.getLocalPort() + ".");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}	
		
	/**
	 * Creates the data for a new packet to send.  Packet type and contents are
	 * based on what the user entered as choices.
	 * 
	 * @return	new packet data
	 */
	public byte[] createPacket() {
		// write bytes to stream, and then convert to byte[] at end of method
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		
		byte[] packet = null;  // the packet data to return
		
		byteStream.write(0); // first byte of opcode
		
		// create packet data based on packet type
		switch (packetType) {
			case RRQ : byteStream.write(1); // second byte of opcode
				try {
					// write filename
					byteStream.write(filename.getBytes("US-ASCII"));
				} catch (IOException e) {
					System.out.println("\nError: could not create WRQ.");
					return null;
				}
				byteStream.write(0);
				try {
					// write mode
					byteStream.write("octet".getBytes("US-ASCII"));
				} catch (IOException e) {
					System.out.println("\nError: could not create WRQ.");
					return null;
				}
				byteStream.write(0);
				break;
			case WRQ : byteStream.write(2); // second byte of opcode
				try {
					// write filename
					byteStream.write(filename.getBytes("US-ASCII"));
				} catch (IOException e) {
					System.out.println("\nError: could not create WRQ.");
					return null;
				}
				byteStream.write(0);
				try {
					// write mode
					byteStream.write("octet".getBytes("US-ASCII"));
				} catch (IOException e) {
					System.out.println("\nError: could not create WRQ.");
					return null;
				}
				byteStream.write(0);
				break;
			case DATA : byteStream.write(3); // second byte of opcode
				byteStream.write(0);
				byteStream.write(eBlockNumber);
				try {
					// write some data in data field
					byteStream.write("*** THIS IS IN THE DATA FIELD ***".getBytes(
							"US-ASCII"));
				} catch (IOException e) {
					System.out.println("\nError: could not create DATA.");
					return null;
				}
				break;
			case ACK : byteStream.write(4); // second byte of opcode
				byteStream.write(0);
				byteStream.write(eBlockNumber);
				break;
			case ERROR : byteStream.write(5); // second byte of opcode
				byteStream.write(0);
				byteStream.write(errorCode);
				try {
					// write some data in data field
					byteStream.write("*** THIS IS YOUR ERROR MESSAGE ***".getBytes(
							"US-ASCII"));
				} catch (IOException e) {
					System.out.println("\nError: could not create ERROR.");
					return null;
				}
				byteStream.write(0);
				break;
			default :
				return null;
		}
		
		// convert stream to byte[] to return
		packet = byteStream.toByteArray();
		
		// make opcode invalid
		if (eOpFlag) {
			packet[1] = 0;
		}
		
		return packet;
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
	private DatagramSocket serverSocket, clientSocket;
	
	int sendPort;  // port to send to
	
	// choices when entering Error Simulation Mode
	private ErrorSim.PacketType packetType = null;
	private ErrorSim.PacketDo packetDo = null;
	private boolean choiceIsServer; // true if choice is server, false if client
	private int packetNumber; // number of packet to be manipulated
	private int actionCount; // count packets of one type, in order to tell when to take action
	boolean eOpFlag;   // change opcode 
	boolean eFnFlag;   // change filename
	boolean eMdFlag;   // change mode
	byte eBlockNumber; // change block number
	boolean eDfFlag;   // delete data field
	byte errorCode;    // change error code
	String filename;   // change the filename in RRQ or WRQ
	
	// check if ErrorSim action took place, so it is only done once
	private boolean actionFlag = false;
	
	// max number of bytes for data field in packet
	public static final int MAX_DATA = 512;  
	
	/**
	 * ErrorSim started in Error Simulation Mode.
	 * This method is overloaded.
	 * 
	 * @param receivePacket		packet received by ErrorSim on port 68
	 * @param packetType		type of packet to manipulate
	 * @param packetDo			how to manipulate packet
	 * @param choiceIsServer	where to manipulate packet
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
	 */
	public ToClient (DatagramPacket receivePacket, 
						ErrorSim.PacketType packetType, 
						ErrorSim.PacketDo packetDo, 
						boolean choiceIsServer,
						int packetNumber,
						boolean eOpFlag, 
						boolean eFnFlag, 
						boolean eMdFlag, 
						byte eBlockNumber, 
						boolean eDfFlag, 
						byte errorCode,
						String filename,
						DatagramSocket serverSocket,
						DatagramSocket clientSocket,
						int sendPort) 
	{
		/* If this Connection thread is to send to Server, the sockets will be
		   null and must be created.
		   
		   If this Connection thread is to send to Client, sockets will have 
		   been passed to it, and will not need to be created.
		*/ 		
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
		
		// sockets
		this.serverSocket = serverSocket;
		this.clientSocket = clientSocket;
		
		this.sendPort = sendPort;  // port to send to
		
		// the original packet received on ErrorSim's port 68, from client
		this.receivePacket = receivePacket;  
		
		// choices made for error simulation
		this.packetType = packetType;
		this.packetDo = packetDo;
		this.choiceIsServer = choiceIsServer;
		this.packetNumber = packetNumber;
		this.eOpFlag = eOpFlag;
		this.eFnFlag = eFnFlag;
		this.eMdFlag = eMdFlag;
		this.eBlockNumber = eBlockNumber;
		this.eDfFlag = eDfFlag;
		this.errorCode = errorCode;
		this.filename = filename;
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
			int sendPort) 
	{
		/* If this Connection thread is to send to Server, the sockets will be
			null and must be created.

		   If this Connection thread is to send to Client, sockets will have 
			been passed to it, and will not need to be created.
		 */ 		
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
		 *  NORMAL MODE
		 */
		if (packetDo == null) {
			// received data from DatagramPacket	
			byte[] received = new byte[receivePacket.getLength()];
			System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), 
					received, 0, receivePacket.getLength());
		
			while (true) {
				// passes Server's packet to Client
				send(received, receivePacket.getAddress(), sendPort, clientSocket);
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
				// passes Server's packet to Client
				send(received, receivePacket.getAddress(), sendPort, clientSocket);
				receivePacket = receive(serverSocket);  // receive packet from Server
				received = processDatagram(receivePacket); // print packet data to user
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
				socket.receive(receivePacket);
				
				// print out thread and port info
				System.out.println("\n" + threadName() + ": packet received: ");
				System.out.println("From host: " + receivePacket.getAddress() + 
						" : " + receivePacket.getPort());
				System.out.print("Containing " + receivePacket.getLength() + 
						" bytes: \n");
				
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
		
		// display info to user
		System.out.println(Arrays.toString(data));
		
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
		
		// print out packet info to user
		System.out.println("\n" + threadName() + ": Sending packet: ");
		System.out.println("To host: " + addr + " : " + port);
		System.out.print("Containing " + sendPacket.getLength() + " bytes: \n");
		System.out.println(Arrays.toString(data) + "\n");
		
		// send the packet
		try {
			socket.send(sendPacket);
			System.out.println(threadName() + ": Packet sent using port " 
					+ socket.getLocalPort() + ".");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}	
		
	/**
	 * Creates the data for a new packet to send.  Packet type and contents are
	 * based on what the user entered as choices.
	 * 
	 * @return	new packet data
	 */
	public byte[] createPacket() {
		// write bytes to stream, and then convert to byte[] at end of method
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		
		byte[] packet = null;  // the packet data to return
		
		byteStream.write(0); // first byte of opcode
		
		// create packet data based on packet type
		switch (packetType) {
			case RRQ : byteStream.write(1); // second byte of opcode
				try {
					// write filename
					byteStream.write(filename.getBytes("US-ASCII"));
				} catch (IOException e) {
					System.out.println("\nError: could not create WRQ.");
					return null;
				}
				byteStream.write(0);
				try {
					// write mode
					byteStream.write("octet".getBytes("US-ASCII"));
				} catch (IOException e) {
					System.out.println("\nError: could not create WRQ.");
					return null;
				}
				byteStream.write(0);
				break;
			case WRQ : byteStream.write(2); // second byte of opcode
				try {
					// write filename
					byteStream.write(filename.getBytes("US-ASCII"));
				} catch (IOException e) {
					System.out.println("\nError: could not create WRQ.");
					return null;
				}
				byteStream.write(0);
				try {
					// write mode
					byteStream.write("octet".getBytes("US-ASCII"));
				} catch (IOException e) {
					System.out.println("\nError: could not create WRQ.");
					return null;
				}
				byteStream.write(0);
				break;
			case DATA : byteStream.write(3); // second byte of opcode
				byteStream.write(0);
				byteStream.write(eBlockNumber);
				try {
					// write some data in data field
					byteStream.write("*** THIS IS IN THE DATA FIELD ***".getBytes(
							"US-ASCII"));
				} catch (IOException e) {
					System.out.println("\nError: could not create DATA.");
					return null;
				}
				break;
			case ACK : byteStream.write(4); // second byte of opcode
				byteStream.write(0);
				byteStream.write(eBlockNumber);
				break;
			case ERROR : byteStream.write(5); // second byte of opcode
				byteStream.write(0);
				byteStream.write(errorCode);
				try {
					// write some data in data field
					byteStream.write("*** THIS IS YOUR ERROR MESSAGE ***".getBytes(
							"US-ASCII"));
				} catch (IOException e) {
					System.out.println("\nError: could not create ERROR.");
					return null;
				}
				byteStream.write(0);
				break;
			default :
				return null;
		}
		
		// convert stream to byte[] to return
		packet = byteStream.toByteArray();
		
		// make opcode invalid
		if (eOpFlag) {
			packet[1] = 0;
		}
		
		return packet;
	}
}


/**
 * Delays and sends a packet.
 * 
 */
class Delay implements Runnable
{	
	public static final int DELAY = 2500;  // milliseconds to delay packet
	byte[] data;                           // data to put in packet
	InetAddress addr;                      // InetAddress to send packet to
	int port;                              // port to send packet to
	DatagramSocket socket;                 // socket to send packet from
	DatagramPacket sendPacket;             // packet to delay and send
	
	public Delay (byte[] data, InetAddress addr, int port, 
			DatagramSocket socket)
	{
		this.data = data;
		this.addr = addr;
		this.port = port;
		this.socket = socket;
	}
	
	public void run() 
	{
		// delay the packet
		try {
            Thread.sleep(DELAY);
            System.out.println("\n" + threadName() + ": Packet Delayed.");
        } catch (InterruptedException e) {
        	Thread.currentThread().interrupt();
        }
		
		// send the delayed packet
		
		// create new DatagramPacket to send to client
		sendPacket = new DatagramPacket(data, data.length, addr, port);
		
		// print out packet info to user
		System.out.println("\n" + threadName() + ": Sending packet: ");
		System.out.println("To host: " + addr + " : " + port);
		System.out.print("Containing " + sendPacket.getLength() + " bytes: \n");
		System.out.println(Arrays.toString(data) + "\n");
		
		// send the packet
		try {
			socket.send(sendPacket);
			System.out.println("\n" + threadName() + ": Packet sent using port " 
					+ socket.getLocalPort() + ".");
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




