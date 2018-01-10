/**
 * Network.java
 *
 * Version:     $Id$
 *
 * Revisions:   $Log$
 *
 */
import java.io.*;
import java.lang.Runnable;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * handles all network traffic.
 *
 * @author      Jonathan Lo <jonathan.c.lo.us@gmail.com>
 * @version
 * @since
 */
public class Network
{
    // global statics
    public static final boolean NET_DEBUG	= false;
    public static final int HEADER_SIZE		= 1;
    public static final int DATA_SIZE = 1024;
    public static final int KEYHEADER_SIZE	= 4;
    public static final int DATAHEADER_SIZE = 4;

    /* Header Constants */
    public static final byte NOOP	 = 0;
    public static final byte HELLO	 = 1;
    public static final byte MSG	 = 2;
    public static final byte QUIT	 = 3;
    public static final byte KEYSIZE = 4;
    public static final byte KEY	 = 5;

    /* Network information */
    private boolean isServer;     // is the network instance a server?
    private ServerSocket DOORMAN; // simple buffer to wait for a client connection
    private Socket CHANNEL;       // communication channel for all packets

    /* I/O operators*/
    private DataInputStream READER;  //
    private DataOutputStream WRITER; //
    private Manager manager;

    /* */
    private int PORT = -1;
    private int KEY_SIZE = 0;

    /**
     * PUBLIC FUNCTION PROTOTYPES
     * these are provided for a quick glance
     */

    // public Network ( );                      // only creates network interface
    // public boolean connect ( );              // starts the server, returns true on completion
    // public boolean connect ( String );       // connects to a server, returns true on completion
    // public boolean quit ( );                 // signals a QUIT, returns true on completion
    // public boolean send ( byte [] );         // sends the provided, returns true on completion
    // public boolean sendKeySize ( byte [] );  // sends a size of the key, must be sent first
    // public boolean sendKey ( byte [] );      // sends a key
    // public byte [] receiveKeySize ( );       // receives a key sendKeySize
    // public byte [] receiveKey ( );           // receives a key
    // public byte [] listen ( );               // receives a packet and returns it

    /**
     *   Network
     *   Constructor to initialize this object
     *   @param port - the port to listen on
     */
    public Network ( int port )
    {
        this.PORT = port;
    }

    /**
     *   Network
     *   Constructor to initialize this object
     *   @param port - the port to listen on
     *   @param m - the manager to report to
     */
    public Network ( int	 port,
                     Manager m )
    {
        this( port );
        this.manager = m;
    }

    /**
     * Network
     * setChannelTimeout
     *
     *
     * $(gavaparam)
     * @throws
     */
    public void setChannelTimeout ( int t ) throws SocketException
    {
        CHANNEL.setSoTimeout ( t );
    }

    /**
     * connect ( )
     * <p>
     * establishs the sockets and connections for the "server", establishes the readers and writers
     * <p>
     *
     * @return true on completion
     * @throws IOException on I/O error
     */
    public boolean connect ( ) throws IOException
    {
        this.isServer = true;
        printf ( "Establishing CHANNEL..." );
        DOORMAN = new ServerSocket ( PORT );
        printf ( "Awaiting connection..." );
        CHANNEL = DOORMAN.accept ( );
        printf ( "Established CHANNEL." );

        String s = String.format ( "Connected client %s:%d\n",
                                   CHANNEL.getInetAddress ( ),
                                   CHANNEL.getPort ( ) );

        printf ( s );
        READER = new DataInputStream ( CHANNEL.getInputStream ( ) );
        WRITER = new DataOutputStream ( CHANNEL.getOutputStream ( ) );
        hello ( );

        return true;
    } /* connect */

    /**
     * connect ( )
     * <p>
     * connects to a client (acting as server).
     * Server MUST be initiated before the client can connect
     * <p>
     *
     * @param address the string address to connect to
     * @return true on completion
     * @throws IOException on I/O error
     */
    public boolean connect ( String address ) throws IOException
    {
        this.isServer = false;
        printf ( "Establishing CHANNEL..." );
        if ( address.equalsIgnoreCase ( "localhost" ) ) CHANNEL = new Socket (
                "localhost",
                PORT );

        else
            CHANNEL = new Socket ( address,
                                   PORT,
                                   null,
                                   0 );
        printf ( "Established CHANNEL" );

        // handshake
        String s = String.format ( "Connected client %s:%d\n",
                                   CHANNEL.getInetAddress ( ),
                                   CHANNEL.getPort ( ) );

        printf ( s );
        READER = new DataInputStream ( CHANNEL.getInputStream ( ) );
        WRITER = new DataOutputStream ( CHANNEL.getOutputStream ( ) );
        return true;
    } /* connect */

    /**
     * hello ( )
     * <p>
     * send a HELLO MESSAGE, to establish second socket connection
     * <p>
     *
     * @throws IOException on I/O error
     */
    private void hello ( ) throws IOException
    {
        send ( Network.HELLO,
               new byte [ DATAHEADER_SIZE ],
               new byte [ DATA_SIZE ] );
    }

    /**
     * quit ( )
     * <p>
     * sends a QUIT message and disconnects
     * <p>
     *
     * @return true on completion
     * @throws IOException on I/O error
     */
    public boolean quit ( ) throws IOException
    {
        printf ( "QUIT CALLED" );
        send ( Network.QUIT,
               new byte [ DATAHEADER_SIZE ],
               new byte [ DATA_SIZE ] );
        disconnect ( );
        printf ( "TERMINATED" );

        return true;
    } /* quit */

    /**
     * disconnect ( )
     * <p>
     * disconnects the sockets and cleans everything.
     * <p>
     *
     * @return true on completion
     * @throws IOException on I/O error
     */
    private boolean disconnect ( ) throws IOException
    {
        if ( this.isServer == true ) { // server
            if ( DOORMAN != null )
                DOORMAN.close ( );
        }
        if ( READER != null )
            READER.close ( );
        if ( WRITER != null )
            WRITER.close ( );
        if ( CHANNEL != null )
            CHANNEL.close ( );

        return true;
    } /* disconnect */

    /**
     * send ( )
     * <p>
     * sends a MSG message with the given message
     * <p>
     *
     * @param msg byte array of message to send
     * @return true on completion
     * @throws IOException on I/O error
     */
    public boolean send ( byte[] msg ) throws IOException
    {
        byte[]  packet = Arrays.copyOf ( msg,
                                         DATA_SIZE );
        printf ( String.format ( "SEND SIZE = %d\n",
                                 msg.length ) );
        send ( Network.MSG,
               ByteBuffer.allocate ( 4 ).putInt ( msg.length ).array ( ), // the size
               packet );

        return true;
    } /* send */

    /**
     * sendKeySize ( )
     * <p>
     * method to send a key,
     * <p>
     *
     * @param key byte of key to send
     * @return true on completion
     * @throws IOException on I/O error
     */
    public boolean sendKeySize ( byte[] key ) throws IOException
    {
        /* SEND KEY SIZE */
        byte[]  keysize = ByteBuffer.allocate ( 4 ).putInt (
            key.length ).array ( );
        KEY_SIZE = key.length;
        WRITER.write ( Network.KEYSIZE );
        WRITER.write ( keysize,
                       0,
                       KEYHEADER_SIZE );

        if ( NET_DEBUG ) {
            System.out.printf ( "---> SENT HEADER: %d SIZE: %d \n",
                                Network.KEYSIZE,
                                KEY_SIZE );

            for ( byte d : keysize ) System.out.printf ( String.format ( "%d ",
                                                                         d ) );
            printf ( "\n" );
        }
        WRITER.flush ( );

        return true;
    } /* sendKeySize */

    /**
     * receiveKeySize ( )
     * <p>
     * informs the Network of the approaching packet size.
     * <p>
     *
     * @return true on completion
     * @throws IOException on I/O error
     */
    public boolean receiveKeySize ( ) throws IOException
    {
        byte[]  input = new byte [ 5 ];
        READER.readFully ( input,
                           0,
                           HEADER_SIZE + KEYHEADER_SIZE );

        if ( input [ 0 ] == KEYSIZE ) printf ( "HEADER RECEIVED KEYSIZE." );
        byte[]  output = Arrays.copyOfRange ( input,
                                              HEADER_SIZE,
                                              HEADER_SIZE + KEYHEADER_SIZE );
        KEY_SIZE = ByteBuffer.wrap ( output ).order (
            ByteOrder.BIG_ENDIAN ).getInt ( );

        if ( NET_DEBUG ) {
            System.out.printf ( "<--- RECEIVED HEADER: %d\n",
                                input [ 0 ] );
            System.out.printf ( "\tIncoming key size: %d\n",
                                KEY_SIZE );
        }

        return true;
    } /* receiveKeySize */

    /**
     * sendKey ( )
     * <p>
     * sends a key
     * <p>
     *
     * @param key the key to send
     * @return true on completion
     * @throws IOException on I/O error
     */
    public boolean sendKey ( byte[] key ) throws IOException
    {
        /* SEND KEY SIZE */
        WRITER.write ( Network.KEY );
        WRITER.write ( key,
                       0,
                       KEY_SIZE );

        if ( NET_DEBUG ) {
            System.out.printf ( "---> SENT HEADER: %d\n",
                                Network.KEY );

            for ( byte d : key ) System.out.printf ( String.format ( "%d ",
                                                                     d ) );
            printf ( "\n" );
        }
        WRITER.flush ( );

        return true;
    } /* sendKey */

    /**
     * receiveKey ( )
     * <p>
     * get the byte array of the key
     * <p>
     *
     * @return byte array of the key
     * @throws IOException on I/O error
     */
    public byte[] receiveKey ( ) throws IOException
    {
        byte[]  input = new byte [ HEADER_SIZE + KEY_SIZE ];

        // * KEY should be sent right ater KEYSIZE, but we should check
        if ( KEY_SIZE == 0 ) return input; printf ( "HEADER RECEIVED KEY." );
        READER.readFully ( input,
                           0,
                           HEADER_SIZE + KEY_SIZE );
        byte[]  output = Arrays.copyOfRange ( input,
                                              HEADER_SIZE,
                                              HEADER_SIZE + KEY_SIZE );

        if ( NET_DEBUG ) {
            System.out.printf ( "<--- RECEIVED HEADER: %d\n",
                                input [ 0 ] );

            for ( byte d : output ) System.out.printf ( String.format ( "%d ",
                                                                        d ) );
            printf ( "\n" );
        }

        return ByteBuffer.wrap ( output ).order (
            ByteOrder.BIG_ENDIAN ).array ( );
    } /* receiveRSAKey */

    /**
     * listen ( )
     * <p>
     * reads one packet message dicated by the HEADER_SIZE and DATA_SIZE and deals with the header
     * information. Will return necessary data, if available
     * THIS IS A BLOCKING FUNCTION. THIS FUNCTION WILL WAIT UNTIL DATA IS AVAILABLE.
     * <p>
     *
     * @return byte array of data ( if available )
     * @throws IOException on I/O error
     */
    public byte[] listen ( ) throws IOException
    {
        //connecting to an already connection host hangs in this function
        //TODO
        printf ( "RECEIVING..." );
        byte[]  input = receive ( );
        byte[]  output;

        switch ( input [ 0 ] ) {
            case NOOP: // NOOP
                printf ( "HEADER RECEIVED NOOP." );
                break;
            case HELLO: // HELLO
                printf ( "HEADER RECEIVED HELLO." );
                break;
            case MSG: // MSG
                printf ( "HEADER RECEIVED MSG." );
                byte[]  d_size = Arrays.copyOfRange ( input,
                                                      HEADER_SIZE,
                                                      HEADER_SIZE +
                                                      DATAHEADER_SIZE );

                int data_size =
                    ByteBuffer.wrap ( d_size ).order (
                        ByteOrder.BIG_ENDIAN ).getInt ( );

                printf ( String.format ( "PROCESSED SIZE = %d\n",
                                         data_size ) );
                output = Arrays.copyOfRange ( input,
                                              HEADER_SIZE + DATAHEADER_SIZE,
                                              HEADER_SIZE + DATAHEADER_SIZE +
                                              data_size );

                return output;

            case QUIT: // QUIT
                printf ( "HEADER RECEIVED QUIT." );

                if ( NET_DEBUG ) printf ( "QUIT PROCESSED\nTERMINATING..." );
                disconnect ( );
                manager.partnerDisconnected ( );
                break;
            default:
                printf ( "Invalid HEADER received" );
        } /* switch */
        return input;
    } /* listen */

    /**
     *   startListening()
     *   start a thread to listen for incoming messages and pipe them to the manager
     */
    public void startListening ( )
    {
        /**
         *
         *
         * @author      Jonathan Lo <jonathan.c.lo.us@gmail.com>
         * @version
         * @since
         */
        class MessageListener extends Thread implements Runnable
        {
            private Manager manager;

            public MessageListener ( Manager m )
            {
                this.manager = m;
            }

            /**
             * run ( )
             * <p>
             * runs the thread to listen for a message to appear
             * <p>
             *
             */
            public void run ( )
            {
                try {
                    while ( true ) {
                        this.manager.receiveMessage ( listen ( ) );
                    }
                } catch ( SocketException e ) {
                    return;
                } catch ( NullPointerException e ) {
                    return;
                } catch ( EOFException e ) {
                    return;     // other side disconnected
                } catch ( IOException e ) {
                    e.printStackTrace ( );
                } finally{
                    this.manager.disconnect ( );
                }

            } /* run */

        }

        MessageListener listener = new MessageListener ( manager );
        listener.start ( );

        if ( NET_DEBUG ) printf ( "now listening for messages" ); } /* startListening */

    /**
     * receive ( )
     * <p>
     * receives data from the socket output stream.
     * <p>
     *
     * @return received byte array from socket stream.
     * @throws IOException on I/O error
     */
    private byte[] receive ( ) throws IOException
    {
        byte[]  packet = new byte [ HEADER_SIZE + DATAHEADER_SIZE +
                                    DATA_SIZE ];
        READER.readFully ( packet,
                           0,
                           HEADER_SIZE + DATAHEADER_SIZE + DATA_SIZE );
        byte[]  data_p = Arrays.copyOfRange ( packet,
                                              HEADER_SIZE + DATAHEADER_SIZE,
                                              HEADER_SIZE + DATAHEADER_SIZE +
                                              DATA_SIZE );

        if ( NET_DEBUG ) {
            System.out.printf ( "<--- RECEIVED HEADER: %d\n",
                                packet [ 0 ] );

            for ( byte d : data_p ) System.out.printf ( String.format ( "%d ",
                                                                        d ) );
            printf ( "\n" );
        }

        return packet;
    } /* receive */

    /**
     * send ( )
     * <p>
     * internal functions, unprovided to the others
     * <p>
     *
     * @param header single byte header information
     * @param size the byte array converted value of the size of the data
     * @param msg  byte array of message to send.
     * @throws IOException on I/O error
     */
    private void send ( byte   header,
                        byte[] size,
                        byte[] msg ) throws IOException
    {
        WRITER.write ( header );
        WRITER.write ( size,
                       0,
                       DATAHEADER_SIZE );
        WRITER.write ( msg,
                       0,
                       msg.length );

        if ( NET_DEBUG ) {
            System.out.printf ( "---> SENT HEADER: %d\n",
                                header );

            for ( byte d : msg ) System.out.printf ( String.format ( "%d ",
                                                                     d ) );
            printf ( "\n" );
        }
        WRITER.flush ( );
    } /* send */

    /**
     * printf ( )
     * <p>
     * performs a System.out.printf with a newline
     * <p>
     *
     * @param msg string to print out
     */
    private static void printf ( String msg )
    {
        if ( Network.NET_DEBUG ) System.out.printf ( "%s\n",
                                                     msg );
    }

    /**
     * main ( )
     * <p>
     * example code
     * <p>
     *
     * @param args runtime console commands
     * @throws
     */
    public static void main ( String[] args )
    {
        // connect
        Network connection = new Network ( 5201 );


        if ( args.length == 0 ) { // server
            try {
                byte msg[] = {
                    'o',
                    'i',
                    '\n'
                };

                connection.connect ( );
                connection.listen ( );
                connection.send ( msg );
                connection.listen ( );
            } catch ( Exception e ) {
                e.printStackTrace ( );
            }
        } else if ( args.length == 1 ) { // client
            boolean eof	 = false;
            String input = null;

            try {
                byte msg[] = {
                    'o',
                    'i',
                    '\n'
                };

                connection.connect ( args [ 0 ] );
                connection.listen ( );
                connection.send ( msg );
                connection.listen ( );
                connection.quit ( );

                // connection.send ( msg );
            } catch ( ConnectException e ) {
                System.err.println ( "Connection does not exist" );
                System.exit ( 0 );
            } catch ( IOException e ) {
                e.printStackTrace ( );
                System.exit ( 0 );
            } catch ( Exception e ) {
                System.out.println ( "" );
                System.exit ( 0 );
            }
            System.exit ( 1 );
        } else {
            printf ( "Usage: java Network [address]" );
            System.exit ( 1 );
        }
    } /* main */

}

// Network.java END
