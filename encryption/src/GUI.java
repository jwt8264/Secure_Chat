/**
 * Created by Sam on 3/26/2017.
 */


import java.awt.event.*;
import java.util.LinkedList;
import javax.swing.*;

public class GUI extends JFrame {
    private LinkedList<String> buffer;
    private Manager manager;
    private JTextArea textArea;

    /**
     * Constructor for GUI object. Messages are placed in a buffer for the Manager
     * to access, instead of being pushed to the manager with sendInput()
     */
    public GUI() {
        buffer = new LinkedList<>();
        manager = null;
    }

    /**
     * Constructor for GUI object. Accepts a manager reference for returning user messages.
     * @param man
     */
    public GUI(Manager man) {
        this();
        manager = man;
    }

    /**
        Start this GUI.
        will create and show the GUI
    */
    public void start()
    {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
        try { //waits one second before returning so swing thread runs
            Thread.sleep(1000);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Prints the given string to the textArea with an appended newline character.
     * @param output Text to be printed in the textArea
     */
    public void println(String output) {
        print("> " + output + "\n");
    }

    /**
        print an alert to the user, prepended by "!!!"
        @param alert the alert to print
    */
    public void printAlert(String alert)
    {
        print("!!! " + alert + "\n");
    }

    /**
        print a message that has been received from another user
        @param message the message received
    */
    public void printIncoming(String message)
    {
        print("< " + message + "\n");
    }

    /**
     * Prints the given string to the textArea.
     * @param output Text to be printed in the textArea
     */
    public void print(String output) {
        textArea.append(output);
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    /**
     * getMessage returns the oldest String entered by the user that has not been gotten
     * @return Oldest user message on success, null on failure
     */
    public String getMessage() {
        if (buffer.size() > 0) {
            return buffer.remove(); //removes first
        } else {
            return null;
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private void createAndShowGUI() {
        String msg = "<Welcome to the Little Secure Bits Messaging Application>\n";

        //Create and set up the window
        JFrame frame = new JFrame("Little Secure Bits");
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // handled by the below listener
        frame.addWindowListener(new WindowAdapter(){
        
            @Override
            public void windowClosing(WindowEvent e)
            {
                System.out.println("Closing Little Secure Bits");
                manager.disconnect();
                System.exit(0);
            }
        });


        //Add contents to the window

        JTextArea myArea = new JTextArea(msg, 20, 40);
        myArea.setEditable(false);
        myArea.setLineWrap(true);
        myArea.setWrapStyleWord(true);

        JTextField myField = new JTextField();
        myField.addActionListener(new AbstractAction()
        {
            /**
             * Action performed activates when a user presses the "enter" key while
             * the textField is in focus. The textField will be cleared, and the text
             * appended to the textArea
             * @param e
             */
            public void actionPerformed(ActionEvent e)
            {
                String newMsg = myField.getText();
                myField.setText("");
                //myArea.append("> " + newMsg + "\n");
                println(newMsg);
                /*
                If a manager was provided to the constructor, the input will be passed out
                via the managers sendInput function.
                If no manager was provided, the message is added to the queue of messages.
                These can be accessed with "getMessage()". If there are no messages,
                the function will return null.
                 */
                if (manager != null) {
                    manager.sendInput(newMsg);
                } else {
                    buffer.add(newMsg);
                }
            }
        });

        JSplitPane myPane = new JSplitPane();
        JScrollPane scrollPane = new JScrollPane(myArea);
        scrollPane.setWheelScrollingEnabled(true);
        myPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        myPane.setTopComponent(scrollPane);
        myPane.setBottomComponent(myField);
        frame.add(myPane);

        //Display the window
        frame.pack();
        frame.setVisible(true);
        textArea = myArea; //stores the JTextArea for printing functions
    }

    /*
    Testing psvm. I added some basic styling to the window, but it's super easy to change.
    If we want to display time stamps, or where the message came from, we can easily add that
    The current implementation is a splitpane, so only two containers are allowed.
    However, I can nest 2 containers within two containers to get around this if we want
    more buttons, fields, or whatever. (warning - nesting will make it ugly)
     */
    /*
    public static void main(String[] args) throws Exception {
        GUI myGui = new GUI(); //warning, this will sleep for a second when called
        myGui.println("YO WHATS UP???????");
        myGui.print("This is how you print to the user. Easy right?\n");
        Thread.sleep(5000);
        System.out.println(myGui.getMessage());
    }
    */
}
