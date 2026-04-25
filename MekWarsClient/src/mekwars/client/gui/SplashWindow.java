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

package mekwars.client.gui;

public class SplashWindow {

    public javax.swing.JFrame splashWindow;
    private boolean continueAnimating;
    private javax.swing.JLabel imageLabel;
    private javax.swing.JLabel versionLabel;
    private mekwars.client.gui.AnimationThread animator;
    private int currentStatus;
    private javax.swing.JProgressBar progressBar;

    public final int STATUS_INITIALIZING = 0;
    public final int STATUS_FETCHINGDATA = 1;
    public final int STATUS_CONSTRUCTINGGUI = 2;
    public final int STATUS_CONNECTING = 3;
    public final int STATUS_INPUTWAIT = 4;
    public final int STATUS_DATAERROR = 5;
    public final int STATUS_CONNECTFAILED = 6;

    public SplashWindow() {

        continueAnimating = true;
        currentStatus = STATUS_INITIALIZING;

        splashWindow = new javax.swing.JFrame();
        splashWindow.setUndecorated(true);

        progressBar = new javax.swing.JProgressBar(0, 9);
        progressBar.setMaximumSize(new java.awt.Dimension(350, 10));
        progressBar.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        progressBar.setAlignmentY(java.awt.Component.LEFT_ALIGNMENT);


        //load and scale the splash image
        javax.swing.ImageIcon splashImage = null;
        boolean useJPGImage = new java.io.File("data/images/mekwarssplash.jpg").exists();
        if (useJPGImage) {splashImage = new javax.swing.ImageIcon("data/images/mekwarssplash.jpg");} else {
            splashImage = new javax.swing.ImageIcon("data/images/mekwarssplash.gif");
        }
        java.awt.Image tempImage = splashImage.getImage().getScaledInstance(350, 350, java.awt.Image.SCALE_SMOOTH);
        splashImage.setImage(tempImage);

        //format the label
        imageLabel = new javax.swing.JLabel("<HTML><CENTER>MekWars Client " +
                                                  client.MWClient.CLIENT_VERSION +
                                                  "</CENTER></HTML>", splashImage, javax.swing.SwingConstants.CENTER);
        splashWindow.setTitle("MekWars Client " + client.MWClient.CLIENT_VERSION);
        imageLabel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        imageLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        imageLabel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        imageLabel.setIconTextGap(6);

        //create a version label
        versionLabel = new javax.swing.JLabel("<HTML><CENTER><b>Initializing</b></CENTER></HTML>",
              javax.swing.SwingConstants.CENTER);

        //place the labels in a panel
        javax.swing.JPanel windowPanel = new javax.swing.JPanel();

        //give the labels a fixed amount of buffer space
        imageLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 4, 0));
        versionLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 0, 0, 0));

        //use a box layout to align panel components vertically
        windowPanel.setLayout(new javax.swing.BoxLayout(windowPanel, javax.swing.BoxLayout.Y_AXIS));

        //format the panel - Colours, JLabels and a divider
        windowPanel.setBackground(java.awt.Color.WHITE);
        windowPanel.add(imageLabel);
        windowPanel.add(new javax.swing.JSeparator());
        windowPanel.add(versionLabel);

        windowPanel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        windowPanel.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);

        //give the panel an attractive border
        windowPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
              javax.swing.BorderFactory.createLineBorder(java.awt.Color.BLACK, 1),
              javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        splashWindow.getContentPane().add(windowPanel);
        splashWindow.getContentPane().add(progressBar, java.awt.BorderLayout.SOUTH);
        splashWindow.getContentPane().setBackground(java.awt.Color.WHITE);
        splashWindow.pack();
        splashWindow.setLocationRelativeTo(null);

        splashWindow.setVisible(true);
        animator = new mekwars.client.gui.AnimationThread(this);
        animator.start();
    }

    public void dispose() {
        continueAnimating = false;
        splashWindow.setVisible(false);
        splashWindow.dispose();
    }

    public boolean shouldAnimate() {
        return continueAnimating;
    }

    public javax.swing.JLabel getImageLabel() {
        return versionLabel;
    }

    public javax.swing.JProgressBar getProgressBar() {
        return progressBar;
    }

    public void setStatus(int i) {
        currentStatus = i;
    }

    public int getStatus() {
        return currentStatus;
    }


}

