/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
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

package mekwars.common.gui.dialogs;

import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * The MekWars client "Login" dialog — the modal prompt shown when connecting to a server that asks for a
 * username/password and (re)confirms the server's IP address and chat/data ports. This is typically the very
 * first dialog a player sees when starting the client.
 * <p>
 * Like the other simple dialogs in this package ({@link RegisterNameDialog}, {@link ConfigurationDialog}), all
 * of its behavior lives in the constructor: it builds the form, pre-fills it from the client's saved config,
 * shows it modally, and on completion either saves the entered values back into the config (OK) or terminates
 * the entire JVM via {@link System#exit(int)} (Cancel) -- there is no "just close the login dialog and keep
 * running" option.
 * <p>
 * Pressing Enter in any field advances focus to the next field via {@link #actionPerformed(java.awt.event.ActionEvent)};
 * pressing Enter in the last field (Data Port) automatically triggers the OK button.
 */
public final class SignOnDialog implements java.awt.event.ActionListener {

    // Action-command strings identifying which field/button fired an ActionEvent; used only to decide which
    // field to move focus to next (see actionPerformed).
    private final String usernameCommand = "user";
    private final String passwordCommand = "password";
    private final String okayCommand = "okay";
    private final String cancelCommand = "cancel";
    private final String ipaddressCommand = "ip address";
    private final String chatPortCommand = "chatport";
    private final String dataPortCommand = "dataport";

    private final javax.swing.JPasswordField passwordField = new javax.swing.JPasswordField();
    private final javax.swing.JTextField ipaddressField = new javax.swing.JTextField();
    private final javax.swing.JTextField chatPortField = new javax.swing.JTextField();
    private final javax.swing.JTextField dataPortField = new javax.swing.JTextField();

    private final javax.swing.JButton okayButton = new javax.swing.JButton("OK");
    private final javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");

    private final javax.swing.JDialog dialog;
    private final javax.swing.JOptionPane pane;

    /**
     * Builds and shows the sign-on/login dialog modally, pre-filled with the username, password, server IP,
     * chat port, and data port currently stored in the client's config. Blocks until the user presses OK or
     * Cancel (or triggers OK by pressing Enter in the Data Port field).
     * <p>
     * If OK is pressed, the entered username, IP address, and ports are written back into the client's config
     * and the client's in-memory username/password are updated; the password itself is <b>not</b> persisted to
     * config here (only the username is saved via {@code setParam("NAME", ...)}).
     * <p>
     * If Cancel is pressed, the entire application is terminated immediately via {@code System.exit(0)} --
     * there is no way to dismiss this dialog and continue running without signing on.
     *
     * @param client the client whose config supplies the pre-filled defaults and which receives the entered
     *               username/password/connection settings on OK
     */
    public SignOnDialog(IClient client) {

        // Create the labels and buttons
        javax.swing.JLabel usernameLabel = new javax.swing.JLabel("Username: ", javax.swing.SwingConstants.LEFT);
        javax.swing.JLabel passwordLabel = new javax.swing.JLabel("Password (none if unregistered): ",
              javax.swing.SwingConstants.LEFT);
        javax.swing.JLabel ipaddressLabel = new javax.swing.JLabel("IP Address: ", javax.swing.SwingConstants.LEFT);
        javax.swing.JLabel chatPortLabel = new javax.swing.JLabel("Chat Port: ", javax.swing.SwingConstants.LEFT);
        javax.swing.JLabel dataPortLabel = new javax.swing.JLabel("Data Port: ", javax.swing.SwingConstants.LEFT);

        // Set the actions to generate
        javax.swing.JTextField usernameField = new javax.swing.JTextField();
        usernameField.setActionCommand(usernameCommand);
        passwordField.setActionCommand(passwordCommand);
        chatPortField.setActionCommand(chatPortCommand);
        dataPortField.setActionCommand(dataPortCommand);
        ipaddressField.setActionCommand(ipaddressCommand);
        okayButton.setActionCommand(okayCommand);
        cancelButton.setActionCommand(cancelCommand);

        // Set the listeners to this object
        usernameField.addActionListener(this);
        passwordField.addActionListener(this);
        ipaddressField.addActionListener(this);
        chatPortField.addActionListener(this);
        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);

        // Set tool tips (balloon help)
        usernameLabel.setToolTipText("Username for remote systems");
        passwordLabel.setToolTipText("Password for remote systems");
        ipaddressLabel.setToolTipText("IP address for remote systems");
        chatPortLabel.setToolTipText("Port which server uses to host chat");
        dataPortLabel.setToolTipText("Port which server uses to host data");
        okayButton.setToolTipText("Use this username and password");
        cancelButton.setToolTipText("Quit MekWars");
        ipaddressField.setToolTipText("IP address for remote systems");

        // Create the panel holding the labels and text fields
        javax.swing.JPanel textPanel = new javax.swing.JPanel(new java.awt.GridLayout(5, 4), false);
        textPanel.add(usernameLabel);
        textPanel.add(usernameField);
        textPanel.add(passwordLabel);
        textPanel.add(passwordField);
        textPanel.add(ipaddressLabel);
        textPanel.add(ipaddressField);
        textPanel.add(chatPortLabel);
        textPanel.add(chatPortField);
        textPanel.add(dataPortLabel);
        textPanel.add(dataPortField);

        // Create the panel that will hold the entire UI
        javax.swing.JPanel mainPanel = new javax.swing.JPanel(false);

        // Set the user's options
        Object[] options = { okayButton, cancelButton };

        // Create the pane containing the buttons
        pane = new javax.swing.JOptionPane(textPanel, javax.swing.JOptionPane.PLAIN_MESSAGE,
              javax.swing.JOptionPane.DEFAULT_OPTION, null, options,
              usernameField);

        // Create the main dialog and set the default button
        String windowName = "MekWars Login";
        dialog = pane.createDialog(mainPanel, windowName);
        dialog.getRootPane().setDefaultButton(okayButton);

        //Is a username known? if so, show it..
        usernameField.setText(client.getConfig().getParam("NAME"));
        passwordField.setText(client.getConfig().getParam("NAMEPASSWORD"));
        ipaddressField.setText(client.getConfig().getParam("SERVERIP"));
        chatPortField.setText(client.getConfig().getParam("SERVERPORT"));
        dataPortField.setText(client.getConfig().getParam("DATAPORT"));

        // Show the dialog and get the user's input
        dialog.setVisible(true);
        dialog.requestFocus();
        usernameField.requestFocus();
        dialog.setLocationRelativeTo(client.getMainFrame());
        if (pane.getValue() == okayButton) {
            client.getConfig().setParam("NAME", usernameField.getText());
            client.setUsername(usernameField.getText());
            client.setPassword(new String(passwordField.getPassword()));
            client.getConfig().setParam("SERVERPORT", chatPortField.getText());
            client.getConfig().setParam("DATAPORT", dataPortField.getText());
            client.getConfig().setParam("SERVERIP", ipaddressField.getText());
        }

        //not ok with signing on? ok. quit!
        else {System.exit(0);}
    }

    /**
     * Advances keyboard focus from one field to the next when Enter is pressed in a text field, mimicking Tab
     * navigation; reaching the last field (Data Port) instead auto-clicks OK and closes the dialog. Also
     * handles the OK/Cancel buttons themselves by recording the chosen value and disposing the dialog so the
     * blocking {@code dialog.setVisible(true)} call in the constructor can return.
     *
     * @param e the event fired by one of the text fields or the OK/Cancel buttons
     */
    public void actionPerformed(java.awt.event.ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case usernameCommand -> passwordField.requestFocus();
            case passwordCommand -> ipaddressField.requestFocus();
            case ipaddressCommand -> chatPortField.requestFocus();
            case chatPortCommand -> dataPortField.requestFocus();
            case dataPortCommand -> {
                okayButton.doClick(200);
                pane.setValue(okayButton);
                dialog.dispose();
            }
            case okayCommand -> {
                pane.setValue(okayButton);
                dialog.dispose();
            }
            case cancelCommand -> {
                pane.setValue(cancelButton);
                dialog.dispose();
            }
        }
    }
}
