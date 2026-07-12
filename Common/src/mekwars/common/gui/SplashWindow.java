/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original File: GraphicGimicks.java
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package mekwars.common.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import mekwars.common.threads.AnimationThread;

/**
 * The MekWars client's startup splash/loading screen: an undecorated {@link JFrame} showing the client logo/splash
 * image, a status label, and a progress bar, displayed while the client fetches data, connects to the server, and
 * builds its main GUI. Progress is driven externally (via {@link #setStatus(int)}/{@link #getProgressBar()}) by the
 * client bootstrap code, while a background {@link AnimationThread} animates the window (e.g. pulsing/cycling
 * the display) for as long as {@link #shouldAnimate()} returns {@code true}.
 * <p>
 * Note: the {@code STATUS_*} constants are declared as non-static instance fields, so distinct status values are
 * only guaranteed to be internally consistent between {@link #getStatus()}/{@link #setStatus(int)} calls on the
 * same instance; they behave like an enum in practice since only one {@code SplashWindow} is normally created.
 */
public class SplashWindow {

    /** Status code: client is starting up. */
    public final int STATUS_INITIALIZING = 0;
    /** Status code: client is retrieving campaign/game data from the server. */
    public final int STATUS_FETCHING_DATA = 1;
    /** Status code: client is building its main Swing GUI. */
    public final int STATUS_CONSTRUCTING_GUI = 2;
    /** Status code: client is connecting to the server. */
    public final int STATUS_CONNECTING = 3;
    /** Status code: client is waiting on user input (e.g. login credentials). */
    public final int STATUS_INPUT_WAIT = 4;
    /** Status code: an error occurred while fetching data. */
    public final int STATUS_DATA_ERROR = 5;
    /** Status code: the connection attempt to the server failed. */
    public final int STATUS_CONNECT_FAILED = 6;
    /** Label showing the current status text (despite the name, it doubles as the status message label). */
    private final JLabel versionLabel;
    /** Progress bar shown at the bottom of the splash window. */
    private final JProgressBar progressBar;
    /** The undecorated top-level frame that hosts the splash content. */
    public JFrame splashWindow;
    /** Whether the background {@link AnimationThread} should keep animating; cleared by {@link #dispose()}. */
    private boolean continueAnimating;
    /** The current status code, one of the {@code STATUS_*} constants. */
    private int currentStatus;

    /**
     * Builds and displays the splash window: loads and scales the splash image (preferring a JPG over a GIF if
     * both/either exists at the hardcoded {@code data/images/} paths), lays out the image, a separator, and a
     * status label in a bordered panel alongside a progress bar, centers the window on screen, makes it visible,
     * and starts a background {@link AnimationThread} to animate it.
     */
    public SplashWindow() {

        continueAnimating = true;
        currentStatus = STATUS_INITIALIZING;

        splashWindow = new JFrame();
        splashWindow.setUndecorated(true);

        progressBar = new JProgressBar(0, 9);
        progressBar.setMaximumSize(new Dimension(350, 10));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setAlignmentY(Component.LEFT_ALIGNMENT);


        //load and scale the splash image
        ImageIcon splashImage;
        boolean useJPGImage = new File("data/images/mekwarssplash.jpg").exists();

        if (useJPGImage) {
            splashImage = new ImageIcon("data/images/mekwarssplash.jpg");
        } else {
            splashImage = new ImageIcon("data/images/mekwarssplash.gif");
        }

        Image tempImage = splashImage.getImage().getScaledInstance(350, 350, Image.SCALE_SMOOTH);
        splashImage.setImage(tempImage);

        //format the label
        JLabel imageLabel = new JLabel("<HTML><CENTER>MekWars client " +
                                             "Update With Version Capture" +
                                             "</CENTER></HTML>", splashImage, SwingConstants.CENTER);
        splashWindow.setTitle("MekWars client " + "Update With Version Capture");
        imageLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        imageLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        imageLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        imageLabel.setIconTextGap(6);

        //create a version label
        versionLabel = new JLabel("<HTML><CENTER><b>Initializing</b></CENTER></HTML>",
              SwingConstants.CENTER);

        //place the labels in a panel
        JPanel windowPanel = new JPanel();

        //give the labels a fixed amount of buffer space
        imageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        versionLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        //use a box layout to align panel components vertically
        windowPanel.setLayout(new BoxLayout(windowPanel, BoxLayout.Y_AXIS));

        //format the panel - Colors, JLabels and a divider
        windowPanel.setBackground(Color.WHITE);
        windowPanel.add(imageLabel);
        windowPanel.add(new JSeparator());
        windowPanel.add(versionLabel);

        windowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        windowPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        //give the panel an attractive border
        windowPanel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(java.awt.Color.BLACK, 1),
              BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        splashWindow.getContentPane().add(windowPanel);
        splashWindow.getContentPane().add(progressBar, BorderLayout.SOUTH);
        splashWindow.getContentPane().setBackground(Color.WHITE);
        splashWindow.pack();
        splashWindow.setLocationRelativeTo(null);

        splashWindow.setVisible(true);
        AnimationThread animator = new AnimationThread(this);
        animator.start();
    }

    /**
     * Stops the background animation thread (by clearing the flag checked by {@link #shouldAnimate()}), hides the
     * splash window, and releases its native resources.
     */
    public void dispose() {
        continueAnimating = false;
        splashWindow.setVisible(false);
        splashWindow.dispose();
    }

    /**
     * @return {@code true} if the {@link AnimationThread} started in the constructor should keep running.
     */
    public boolean shouldAnimate() {
        return continueAnimating;
    }

    /**
     * @return the status label. Despite the method name ("image label"), this returns the text status label
     *       ({@link #versionLabel}), not the image/logo label built locally in the constructor.
     */
    public javax.swing.JLabel getImageLabel() {
        return versionLabel;
    }

    /**
     * @return the progress bar shown in the splash window, so external bootstrap code can drive its value.
     */
    public javax.swing.JProgressBar getProgressBar() {
        return progressBar;
    }

    /**
     * @return the current status code, one of the {@code STATUS_*} constants.
     */
    public int getStatus() {
        return currentStatus;
    }

    /**
     * Sets the current status code (one of the {@code STATUS_*} constants). Note this only updates the stored
     * value - it does not update the displayed label text; callers are expected to also update the label (e.g. via
     * {@link #getImageLabel()}) separately.
     */
    public void setStatus(int i) {
        currentStatus = i;
    }


}

