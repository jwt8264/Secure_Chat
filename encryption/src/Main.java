/**
    Main.java

    This class contains the main method for starting an instance of
    the lil' bits secure chat application
*/

public class Main
{
    public static void main(String[] args)
    {
        Manager manager = new Manager();
        GUI gui = new GUI(manager);
        manager.setGUI(gui);
        gui.start();
    }

}
