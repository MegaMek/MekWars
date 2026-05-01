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

package mekwars.updaters;

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

public class SplashWindow {

    public final int STATUS_INITIALIZING = 0;
    public final int STATUS_FETCHING_DATA = 1;
    public final int STATUS_CONSTRUCTING_GUI = 2;
    public final int STATUS_CONNECTING = 3;
    public final int STATUS_INPUT_WAIT = 4;
    public final int STATUS_DATA_ERROR = 5;
    public final int STATUS_CONNECT_FAILED = 6;
    private final JLabel versionLabel;
    private final AnimationThread animator;
    private final JProgressBar progressBar;
    public JFrame splashWindow;
    private boolean continueAnimating;
    private int currentStatus;

    public SplashWindow() {

        continueAnimating = true;
        currentStatus = STATUS_INITIALIZING;

        splashWindow = new JFrame();
        splashWindow.setUndecorated(true);
        splashWindow.setTitle("MekWars client Update");
        progressBar = new JProgressBar(0, 9);
        progressBar.setMaximumSize(new Dimension(350, 10));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setAlignmentY(Component.LEFT_ALIGNMENT);

        // load and scale the splash image
        ImageIcon splashImage;
        boolean useJPGImage = new File("data/images/mekwarssplash.jpg").exists();
        if (useJPGImage) {splashImage = new ImageIcon("data/images/mekwarssplash.jpg");} else {
            splashImage = new ImageIcon("data/images/mekwarssplash.gif");
        }
        Image tempImage = splashImage.getImage().getScaledInstance(350, 350, Image.SCALE_SMOOTH);
        splashImage.setImage(tempImage);

        // format the label
        JLabel imageLabel = new JLabel("<HTML><CENTER>Updating MekWars client<br>Please Wait</CENTER></HTML>",
              splashImage,
              SwingConstants.CENTER);
        imageLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        imageLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        imageLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        imageLabel.setIconTextGap(6);

        // create a version label
        versionLabel = new JLabel("<HTML><CENTER><b>Initializing<br>●</b></CENTER></HTML>", SwingConstants.CENTER);

        // place the labels in a panel
        JPanel windowPanel = new JPanel();

        // give the labels a fixed amount of buffer space
        imageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        versionLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        // use a box layout to align panel components vertically
        windowPanel.setLayout(new BoxLayout(windowPanel, BoxLayout.Y_AXIS));

        // format the panel - Colours, JLabels and a divider
        windowPanel.setBackground(Color.WHITE);
        windowPanel.add(imageLabel);
        windowPanel.add(new JSeparator());
        windowPanel.add(versionLabel);

        windowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        windowPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        // give the panel an attractive border
        windowPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 1),
              BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        splashWindow.getContentPane().add(windowPanel);
        splashWindow.getContentPane().add(progressBar, BorderLayout.SOUTH);
        splashWindow.getContentPane().setBackground(Color.WHITE);
        splashWindow.pack();
        splashWindow.setLocationRelativeTo(null);

        splashWindow.setVisible(true);
        animator = new AnimationThread(this);
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

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JLabel getImageLabel() {
        return versionLabel;
    }

    public int getStatus() {
        return currentStatus;
    }

    public void setStatus(int i) {
        currentStatus = i;
    }

    public AnimationThread getAnimator() {
        return animator;
    }

}

