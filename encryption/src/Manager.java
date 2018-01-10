/**
    Manager.java

    @author Jason Tu jwt8264@rit.edu

    This class serves as a manager for other classes of the application.
    It will handle taking commands from the user, and routing that data
    through encryption routines as necessary, maintaining state about
    this instance of the chat, and forwarding data to/from the networking
    module.
*/

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.io.*;
import java.net.*;

public class Manager
{

    private boolean debug = false;

    private Network network;
    private boolean connected;
    private boolean waitingForConnect;
    private Encryption encryption;
    private GUI gui;

    /**
        Constructor to initialize fields
    */
    public Manager()
    {
        network = null;
        connected = false;
        waitingForConnect = false;
        encryption = null;
    }

    /**
        Set the GUI that this manager reports to
        @param g - the GUI
    */
    public void setGUI(GUI g)
    {
        gui = g;
    }

    /**
        Connect to a friend.
        This will connect to an IP and establish everything necessary so that
        messages can be sent and received after this is called.
        @param ip - the IP address to connect to
        @param port - not used
        @return - true if connection successfully established
    */
    private boolean connectToIP(String ip, int port) throws Exception
    {
        try
        {
            // asd
            // ==========================
            //       CLIENT SIDE
            // ==========================
            network = new Network(port, this);
            encryption = new Encryption();
            network.connect(ip);
            // i am the client
            network.setChannelTimeout(2000);
            network.listen(); // bypass the hello
            network.setChannelTimeout(0);
            // RSA exchange
            print("client side");
            byte[] myPublicKeyBytes = encryption.getMyPublicKey().getEncoded();
            print("public key size locally is "+myPublicKeyBytes.length);
            print("connected...sending size and key");
            network.sendKeySize(myPublicKeyBytes);
            print("sent. waiting for their key size");

            network.receiveKeySize(); // listen for key size, vars set internally to network
            network.sendKey(myPublicKeyBytes);
            print("got the key size");
            byte[] theirPublicKeyBytes = network.receiveKey(); // listen for key itself

            print("got the key ");
            encryption.setTheirPublicKey(theirPublicKeyBytes);

            // AES exchange 
            byte[] aesKeyBytes = encryption.generateAESKey();
            network.sendKeySize(aesKeyBytes);
            network.sendKey(aesKeyBytes);

            // start up listening thread
            network.startListening();
            //print("Connected to "+ip+":"+port+" and started listening.");
            connected = true;

            return true;
        }
        catch (UnknownHostException e)
        {
            return false;
        }
        catch (ConnectException e)
        {
            return false;
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
        catch (SocketTimeoutException e)
        {
            return false;
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /**
     *  Send input from the user to this manager.
     *
     *  @param id - the id of the user you want to send to
     *  @param input - the plaintext message you are sending
     */
    public boolean sendInput(int id, String input)
    {
        try
        {
            if (input.length() > 0 && input.charAt(0) == '/')
            {
                String[] tokens = input.split("\\s+");
                String command = tokens[0].substring(1,tokens[0].length());
                if ("help".equals(command))
                {
                    String help =
                        "Commands:\n" +
                        "/connect <IP Address> <port>\n" +
                        "\tConnect to this IP\n" +
                        "/listen <port>\n" +
                        "\tListen on this port\n" +
                        "/disconnect\n" +
                        "\tDisconnect from the other user\n" +
                        "/exit\n" +
                        "\tDisconnect and quit the application\n" ;

                    gui.print(help);
                    return true;

                }
                else if ("listen".equals(command))
                {
                    // attempt to start listening on a port
                    if (connected)
                    {
                        gui.printAlert("You are already talking to someone");
                        return false;
                    }
                    if (tokens.length < 2)
                    {
                        gui.printAlert("You must provide a port number to listen on");
                        return false;
                    }
                    int port = Integer.parseInt(tokens[1]);
                    // check for a valid port number
                    if (port < 1024)
                    {
                        gui.printAlert("Port number too small, pick a bigger one");
                        return false;
                    }
                    if (port > 65535)
                    {
                        gui.printAlert("Port number too big, pick a smaller one");
                        return false;
                    }
                    if (waitingForConnect)
                    {
                        // if user is already listening, clear old stuff
                        if (network != null)
                        {
                            network.quit();
                        }
                    }
                    network = new Network(port, this);
                    
                    // start up a thread to initialize a server listening on the port
                    new Thread(new Runnable(){
                        public void run()
                        {
                            try
                            {
                                // sdf
                                // ==========================
                                //       SERVER SIDE
                                // ==========================
                                waitingForConnect = true;
                                encryption = new Encryption();
                                network.connect();
                                print("server side");
                                // RSA exchange 
                                print("waiting for key size");
                                network.receiveKeySize(); // first listen for the key size
                                byte[] myPublicKeyBytes = encryption.getMyPublicKey().getEncoded();
                                network.sendKeySize(myPublicKeyBytes);
                                print("waiting for key");
                                byte[] theirPub = network.receiveKey(); // the clients public key

                                encryption.setTheirPublicKey(theirPub);

                                print("sending key");
                                network.sendKey(myPublicKeyBytes);
                                print("key sent");

                                // AES exchange 
                                network.receiveKeySize();
                                byte[] aesKeyBytes = network.receiveKey();
                                encryption.setAESKey(aesKeyBytes);


                                network.startListening();
                                gui.printAlert("Someone has connected! Start chatting...");
                                connected = true;
                                waitingForConnect = false;
                            }
                            catch (BindException e)
                            {
                                gui.printAlert("Port unavailable, pick another one.");
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            
                            } 
                            catch (Exception e) 
                            {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    gui.printAlert("Now listening on port "+port);
                }
                else if ("connect".equals(command))
                {
                    // try to connect to someone
                    if (connected)
                    {
                        gui.printAlert("You are already connected to someone!");
                        return false;
                    }
                    // try to connect to someone
                    if (tokens.length < 3)
                    {
                        // not enough args provided
                        gui.printAlert("Connect failed, not enough parameters");
                        return false;
                    }
                    else
                    {
                        connected = connectToIP(tokens[1], Integer.parseInt(tokens[2]));
                        if (!connected){
                            gui.printAlert("Connection failed");
                            return false;
                        }
                        gui.printAlert("Connection successful! Start chatting...");
                        waitingForConnect = false;
                    }
                }
                else if ("disconnect".equals(command))
                {
                    // try to disconnect from the ohter user
                    if (!connected)
                    {
                        gui.printAlert("No one to disconnect from.");
                        return false;
                    }
                    // notify of disconnect and close connection
                    disconnect();
                    gui.printAlert("Disconnected.");
                }
                else if ("exit".equals(command) || "quit".equals(command))
                {
                    // disconnect and exit
                    disconnect();
                    shutdown();
                }
                else
                {
                    // unrecognized command
                    gui.printAlert("Unrecognized command... type /help for help");
                    return false;
                }
                return true;
            }
            else
            {
                // try to send a message
                if (connected)
                {
                    try
                    {
                        // send a message
                        byte[] ciphertext;
                        ciphertext = input.getBytes("UTF8");
                        ciphertext = encryption.encrypt(input);
                        if (ciphertext.length > Network.DATA_SIZE)
                        {
                            gui.printAlert("Message too long, message not sent.");
                            return false;
                        }
                        return network.send(ciphertext);
                    }
                    catch (SocketException e)
                    {
                        gui.printAlert("Error sending message; connection dropped");
                        network = null;
                        connected = false;
                        return false;
                    }
                }
                else
                {
                    //user tried to send a message but hasn't connected to anyone
                    gui.printAlert("use /connect <IP addr> <port> to connect to someone");
                    gui.printAlert("use /listen <port> to wait for a connection");
                }
            }

        }
        catch (NumberFormatException e)
        {
            gui.printAlert("Invalid number parameter");
        }
        catch (IndexOutOfBoundsException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /**
        Send input from the user to this manager.
        @param input- the plaintext message you are sending
        @return true if successful, false if error
    */
    public boolean sendInput(String input)
    {
        return sendInput(0, input);
    }

    /**
        Receive a message from the network and give it to the user.
        This method receives a message from the network, decrypts it,
        and gives it to the user.
        @param message - the encrypted message received
    */
    public void receiveMessage(byte[] message)
    {

        try
        {
            if (!connected) 
            {
                gui.printAlert("Partner disconnected.");
                disconnect();  
                return;
            }
            String plaintext;
            plaintext = encryption.decrypt(message);
            gui.printIncoming(plaintext);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
        partnerDisconnected()
        Notify this manager that the partner has disconnected. This should
        only be called by Network under certain circumstances.
    */
    public void partnerDisconnected()
    {
        connected = false;
    }

    /**
        disconnect()
        Notify this manager to disconnect from the partner and reset state.
    */
    public boolean disconnect()
    {
        try
        {
            if (connected) 
            {
                network.quit();
                encryption.reset();
                network = null;
                encryption = null;
                connected = false;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }

    /**
        Shut down the manager.
        Disconnect all connections
    */
    public void shutdown()
    {
        disconnect();
        System.exit(0);
    }

    /**
        print()
        Print a debug message to the console
        @param s - message to print
    */
    private void print(String s)
    {
        if (debug)
        {
            System.out.println("Manager.java: "+s);
        }
    }



}
