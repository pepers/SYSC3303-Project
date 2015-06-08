   _______     _______  _____ ____ ____   ___ ____  
  / ____\ \   / / ____|/ ____|___ \___ \ / _ \___ \ 
 | (___  \ \_/ / (___ | |      __) |__) | | | |__) |
  \___ \  \   / \___ \| |     |__ <|__ <| | | |__ < 
  ____) |  | |  ____) | |____ ___) |__) | |_| |__) |
 |_____/   |_| |_____/ \_____|____/____/ \___/____/ 
 |  __ \         (_)         | |                    
 | |__) | __ ___  _  ___  ___| |_                   
 |  ___/ '__/ _ \| |/ _ \/ __| __|                  
 | |   | | | (_) | |  __/ (__| |_                   
 |_|   |_|  \___/| |\___|\___|\__|   iteration 5                
                _/ |                                
               |__/                                 
        

README
	This project is a file transfer system based on 
	the TFTP specification (RFC 1350).  The project
	will consist of three separate programs, a client,
	an error simulator, and a server. The user will
	send file requests for specific files, from the
	client to the server program.


AUTHORS
	Adhiraj Chakraborty
	Anuj Dalal
	Hidara Abdallah
	Matthew Pepers
	Mohammed Hamza
	Scott Savage


FILES
	Client.java
	Server.java
	ErrorSim.java
	README.txt
	/files/client/
		test0.txt
	/files/server/
		test1.txt
		test2.txt
		test3.txt
		test4.txt
	Error Code 1-Timing Diagram.png
	Error Code 2-Timing Diagram.png
	Error Code 3-Timing Diagram.png
	Error Code 4-Timing Diagram.png
	Error Code 5-Timing Diagram.png
	Error Code 6-Timing Diagram.png



INSTALL/RUN
	- create a new Java Project in Eclipse
	- add Client.java, Server.java, and ErrorSim.java
	to the project
	- compile and run Server.java, then ErrorSim.java, 
	then Client.java from within Eclipse

	- if the .java files are in your project \src\ folder, 
	put your \files\ folder at the same level as \src\
	- the Client's files are in '\files\client\'
	- the Server' files are in '\files\server'
	- you should put the files that you want to send and 
	receive in their respective folders


USE	
	CLIENT
	- follow onscreen console prompts from the Client
	- first enter the InetAddress of the Server.  This 
	may also be entered as a machine name.
	- next choose whether you want to send the read/
	write request directly to the (S)erver, or through
	the (E)rror Simulator
	- then choose whether you want to send a (R)ead 
	Request, a (W)rite Request, (E)nter a new destination,
	or (Q)uit
	- next type in the name of the file that you want to
	read/write
	- test text files for the Client and Server are 
	included in their respective folders to facilitate
	testing, but any file may also be put into those 
	folders in order to transfer between client/server
	- once a file transfer has ended, the onscreen 
	console prompts will ask you if you want to send 
	another request or quit

	SERVER
	- when the Server starts, it listens on port 69 for 
	request packets
	- you can (Q)uit any time by typing 'Q' and pressing 
	'Enter'.  This will quit the Server, but any file
	transfers that are still ongoing will continue until
	they complete or timeout

	ERROR SIMULATOR
	- when ErrorSim starts, a console prompt within Eclipse
	will ask the user if they want to start in (N)ormal
	mode, or (E)rror Simulator mode
	- Normal Mode facilitates the file transfers by sending
	packets back and forth between the Client and Server
	and not interfering with them
	- Error Simulator Mode allows the user to simulate
	errors happening during the file transfer, and the
	user will be prompted to choose a variety of settings
	with which to simulate those errors
	- the Error Simulator must be closed and restarted 
	between each transfer
	


TESTING
	- the Client and Server both have socket timeouts of
	2 seconds, in order to be both short enough for testing,
	and long enough to notice when reading console output
	- most testing can be done through following the 
	onscreen console prompts in the Error Simulator,
	but for those errors that can't be simulated through the 
	Error Simulator:

	ERROR CODE 03
	- to test Error Code 03 (Disk full), the Eclipse 
	project should be installed and run from a USB drive
	- while the Client, Server, and ErrorSim are running
	on your USB drive, check how many bytes are free and 
	usable on the USB drive
	- open a Windows Command Prompt (cmd), and type the 
	following:
	fsutil file createnew D:\hugefile.tmp SIZE
	- replace 'SIZE' with the number of bytes that you 
	found	were free on your USB drive, and replace 'D' 
	with the drive letter of your USB drive
	- this will create a new file on your USB drive to 
	take up the remaining space
	- next try sending a RRQ or WRQ, and you will get 
	Error Code 03, because the disk is full, and files
	are not able to be written

	ERROR CODE 03 (ALTERNATE)
	- if you are not able to use the fsutil command above
	because of lack of privilages, you can add a bunch of 
	files to your USB drive manually, until it is full

	PATHS THROUGH ERROR SIMULATION MENU
	- when choosing Error Simulation Mode, instead of 
	Normal Mode, the following paths through the menu can
	be taken to simulate a variety of errors:

	- 1. Lose a packet.
		- 1. RRQ
			- coming from Client? or Server?
		- 2. WRQ
			- coming from Client? or Server?
		- 3. DATA
			- which DATA (1st, 2nd, 3rd, etc.)
				- coming from Client? or Server?
		- 4. ACK
			- which ACK (1st, 2nd, 3rd, etc.)
				- coming from Client? or Server?
		- 5. ERROR
			- which ERROR (1st, 2nd, 3rd, etc.)
				- coming from Client? or Server?
	- 2. Delay a packet.
		- 1. RRQ
			- how long of a delay?
				- coming from Client? or Server?
		- 2. WRQ
			- how long of a delay?
				- coming from Client? or Server?
		- 3. DATA
			- how long of a delay?
				- which DATA (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
		- 4. ACK
			- how long of a delay?
				- which ACK (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
		- 5. ERROR
			- how long of a delay?
				- which ERROR (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
	- 3. Duplicate a packet.
		- 1. RRQ
			- how long of a delay between duplicated packets?
				- coming from Client? or Server?
		- 2. WRQ
			- how long of a delay between duplicated packets?
				- coming from Client? or Server?
		- 3. DATA
			- how long of a delay between duplicated packets?
				- which DATA (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
		- 4. ACK
			- how long of a delay between duplicated packets?
				- which ACK (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
		- 5. ERROR
			- how long of a delay between duplicated packets?
				- which ERROR (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
	- 4. Send a packet.
		- 1. RRQ
			- enter a filename
				- which packet (1st, 2nd, 3rd, etc.)
					- sending to Client? or Server?
		- 2. WRQ
			- enter a filename
				- which packet (1st, 2nd, 3rd, etc.)
					- sending to Client? or Server?
		- 3. DATA
			- enter a block number (0-65535)
				- which packet (1st, 2nd, 3rd, etc.)
					- sending to Client? or Server?
		- 4. ACK
			- enter a block number (0-65535)
				- which packet (1st, 2nd, 3rd, etc.)
					- sending to Client? or Server?
		- 5. ERROR
			- enter an error code (0-8, 8 is invalid)
				- which packet (1st, 2nd, 3rd, etc.)
					- sending to Client? or Server?
	- 5. Edit a packet.
		- 1. RRQ
			- 1. make opcode invalid
				- coming from Client? or Server?
			- 2. make filename invalid
				- coming from Client? or Server?
			- 3. make mode invalid
				- coming from Client? or Server?
			- 4. change TID
				- coming from Client? or Server?
		- 2. WRQ
			- 1. make opcode invalid
				- coming from Client? or Server?
			- 2. make filename invalid
				- coming from Client? or Server?
			- 3. make mode invalid
				- coming from Client? or Server?
			- 4. change TID
				- coming from Client? or Server?
		- 3. DATA
			- 1. make opcode invalid
				- which DATA (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
			- 2. change block number (0-65535)
				- which DATA (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
			- 3. delete data field
				- which DATA (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
			- 4. change TID
				- which DATA (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
		- 4. ACK
			1. make opcode invalid
				- which ACK (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
			2. change block number (0-65535)
				- which ACK (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
			3. change TID
				- which ACK (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
		- 5. ERROR
			1. make opcode invalid
				- which ERROR (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
			2. change error code (0-8, 8 is invalid)
				- which ERROR (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
			3. change TID
				- which ERROR (1st, 2nd, 3rd, etc.)
					- coming from Client? or Server?
		
		DISCLAIMER
		- Many of the previous paths will produce the same 
		errors, and many paths won't affect file transfer
		at all (eg: if you choose a packet number, but the
		file transfer isn't long enough to make it to that 
		packet). It is up to the user of the Error
		Simulator to understand how file transfer program 
		works, and how to correctly test for various TFTP
		errors.


	 
