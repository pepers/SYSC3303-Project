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
 |_|   |_|  \___/| |\___|\___|\__|   iteration 2                
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
	Iteration2ClassDiag.jpg
	Iteration2TimingDiagram.jpg
	Iteration 2 Team members and division of tasks.docx


INSTALL/RUN
	- create a new Java Project in Eclipse, named 
	'SYSC3303 Project' (without the quotes)
	- add Client.java, Server.java, and ErrorSim.java
	to the project
	- run Server.java, then ErrorSim.java, then Client.java
	from within Eclipse

	- if the .java files are in your project \src\ folder, 
	put your \files\ folder at the same level as \src\
	- the Client's files are in '\files\client\'
	- the Server' files are in '\files\server'
	- you should put the files that you want to send and 
	receive in their respective folders


USE	
	CLIENT
	- follow onscreen console prompts from the Client
	- first choose whether you want to send a (R)ead 
	Request, a (W)rite Request, or (Q)uit
	- if you chose to read/write, next type in the name
	of the file that you want to read/write

	SERVER
	- when the Server doesn't receive any Datagram Packets
	for 20 seconds, it will ask the user if they want to 
	(Q)uit or (C)ontinue
