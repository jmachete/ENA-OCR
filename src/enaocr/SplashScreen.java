/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package enaocr;

/**
 *
 * @author Joao Machete
 */
import java.awt.*;
import javax.swing.*;

public class SplashScreen extends JWindow {

    private int duration;

    public SplashScreen(int d) {
        duration = d;
    }

    // A simple little method to show a title screen in the center
    // of the screen for the amount of time given in the constructor
    public void showSplash() {

            JPanel content = (JPanel)getContentPane();
            content.setBackground(Color.white);

            // Set the window's bounds, centering the window
            int width = 494;
            int height =294;
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            int x = (screen.width-width)/2;
            int y = (screen.height-height)/2;
            setBounds(x,y,width,height);

            // Build the splash screen
            JLabel label = new JLabel(new ImageIcon("splash.png"));
//            JLabel copyrt = new JLabel("2010 Copyright, Joao Machete", JLabel.CENTER);
//            copyrt.setFont(new Font("Sans-Serif", Font.BOLD, 12));
            content.add(label, BorderLayout.CENTER);
//            content.add(copyrt, BorderLayout.SOUTH);
                // Display it
             setVisible(true);

        SwingWorker worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {

                // Wait a little while, maybe while loading resources
                try { Thread.sleep(duration); } catch (Exception e) {}
                setVisible(false);
                return null;
            }

            @Override
            protected void done() {
            setVisible(false);
            }
        };

        worker.execute();

    }

    public void showSplashAndExit() {

        showSplash();

    }
}
