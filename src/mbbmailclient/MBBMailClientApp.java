/*
 * MBBMailClientApp.java
 */

package mbbmailclient;

import javax.swing.SwingUtilities;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class MBBMailClientApp extends SingleFrameApplication {

    private static Model model;

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        
        //new Thread(model).start();
        show(new MBBMailClientView(this, model));
        
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of MBBMailClientApp
     */
    public static MBBMailClientApp getApplication() {
        return Application.getInstance(MBBMailClientApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(final String[] args) {

        model = new Model();


        SwingUtilities.invokeLater(new Runnable(){
           public void run(){
               launch(MBBMailClientApp.class, args);
           }
        });
        
    }

}
