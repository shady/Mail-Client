/*
 * MBBMailClientApp.java
 */

package mbbmailclient;

import javax.swing.SwingUtilities;

/**
 * The main class of the application.
 */
public class MBBMailClientApp {

    private static Model model;


    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */

    /**
     * A convenient static getter for the application instance.
     * @return the instance of MBBMailClientApp
     */

    /**
     * Main method launching the application.
     */
    public static void main(final String[] args) {

        model = new Model();


        SwingUtilities.invokeLater(new Runnable(){
           public void run(){
               new MBBMailClientView(model);
           }
        });
        
    }

}
