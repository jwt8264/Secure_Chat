# Lil' Secure Bits
CS-455 Project, Secure Messaging Application. 

Samuel Lewis (<srl8336@rit.edu>)

Jonathan Lo (<jcl5201@rit.edu>)

Jason Tu  (<jwt8264@rit.edu>)

## Overview
Lil' Secure Bits is a secure chat application allowing you to communicate with others using the security of AES encryption. Messages are encrypted using AES-128 to ensure that they cannot be read by anyone but the recipient. 

## Files

* **docs/** - Contains documentation (\*.pdf) includes tex file, acm format framework and all other files to compile the tex file.

* **encryption/src/** - Contains all java source files.

* **README.md** - This readme file.

* **clean** - A script to clean up any unnecessary files.

* **go** - A script to compile the code.

* **run** - A script to run an instance of the application.

## Getting Started
### Using source files
Navigate to the project folder, and just use 

	./go
to compile, and then 

	./run
to start an instance of the project. 

### Using runnable
From a graphical file explorer, just double-click the jar file. From the command line, navigate to the folder containing the jar and run the command

	java -jar littlebits.jar
	

## Using The App
After starting the application, you will see a graphical interface with a large display box and an input text box at the bottom. 

First, use either `/listen <port>` or `/connect <IP address> <port>`. 

The listen command will set the application to listen on the given port for another user to connect. Once you use the listen command, the other user will use the /connect` command to connect to you.

If you want to connect to another user, use the `/connect` command, giving it the other users IP address and the port that they are listening on. The other user should have already run the `/listen` command before you connect to them. 

Once the connection has been established, you will see a message notifying you that the connection was successful, and that you can now start chatting.

When you are done talking, you can use either the `/disconnect` or `/exit` commands to end the session. Using `/exit` will also close the application. 

At any point, you can type `/help` and that will display a short summary of each of the commands. 

### Quick Help

`/connect <IP address> <port>` - Connect to this IP address at this port.

`/listen <port>` - Start listening on this port.

`/disconnect` - Disconnect from the current session. 

`/exit` - Disconnect from the current session (if there is one) and close the application.

`/help` - Display a help message.