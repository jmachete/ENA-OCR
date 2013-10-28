
package enaocr;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
/**
 *
 * @author Joao Machete
 */
/**
 * The main class of the application.
 */
public class EnaOCRApp extends SingleFrameApplication {
    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        show(new EnaOCRView(this));
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
     * @return the instance of EnaOCRApp
     */
    public static EnaOCRApp getApplication() {
        return Application.getInstance(EnaOCRApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        SplashScreen splash = new SplashScreen(11000);
        splash.showSplashAndExit();
        launch(EnaOCRApp.class, args);
    }
}
