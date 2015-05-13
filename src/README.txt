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
 |_|   |_|  \___/| |\___|\___|\__|   iteration 1                
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
	Iteration 1 - UCM.pdf
	Iteration 1 Class Diagram.jpg


INSTALL/RUN
	- create a new Java Project in Eclipse, named 
	'SYSC3303 Project' (without the quotes)
	- add Client.java, Server.java, and ErrorSim.java
	to the project
	- run Server.java, then ErrorSim.java, then Client.java


USE
	- follow onscreen console prompts from the Client
	- first choose whether you want to send a (R)ead 
	Request, a (W)rite Request, or (Q)uit
	- if you chose to read/write, next type in the name
	of the file that you want to read/write
