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

package mekwars.client.gui.dialog;

public final class SignonDialog implements java.awt.event.ActionListener {

    private final String usernameCommand = "user";
    private final String passwordCommand = "password";
    private final String okayCommand = "okay";
    private final String cancelCommand = "cancel";
    private final String windowName = "MekWars Login";
    private final String ipaddressCommand = "ip address";
    private final String chatPortCommand = "chatport";
    private final String dataPortCommand = "dataport";

    private final javax.swing.JTextField usernameField = new javax.swing.JTextField();
    private final javax.swing.JPasswordField passwordField = new javax.swing.JPasswordField();
    private final javax.swing.JTextField ipaddressField = new javax.swing.JTextField();
    private final javax.swing.JTextField chatPortField = new javax.swing.JTextField();
    private final javax.swing.JTextField dataPortField = new javax.swing.JTextField();

    private final javax.swing.JButton okayButton = new javax.swing.JButton("OK");
    private final javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");

    private javax.swing.JDialog dialog;
    private javax.swing.JOptionPane pane;

    public SignonDialog(client.MWClient mwclient) {

        // Create the labels and buttons
        javax.swing.JLabel usernameLabel = new javax.swing.JLabel("Username: ", javax.swing.SwingConstants.LEFT);
        javax.swing.JLabel passwordLabel = new javax.swing.JLabel("Password (none if unregistered): ",
              javax.swing.SwingConstants.LEFT);
        javax.swing.JLabel ipaddressLabel = new javax.swing.JLabel("IP Address: ", javax.swing.SwingConstants.LEFT);
        javax.swing.JLabel chatPortLabel = new javax.swing.JLabel("Chat Port: ", javax.swing.SwingConstants.LEFT);
        javax.swing.JLabel dataPortLabel = new javax.swing.JLabel("Data Port: ", javax.swing.SwingConstants.LEFT);

        // Set the actions to generate
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
        dialog = pane.createDialog(mainPanel, windowName);
        dialog.getRootPane().setDefaultButton(okayButton);

        //Is a username known? if so, show it..
        usernameField.setText(mwclient.getConfig().getParam("NAME"));
        passwordField.setText(mwclient.getConfig().getParam("NAMEPASSWORD"));
        ipaddressField.setText(mwclient.getConfig().getParam("SERVERIP"));
        chatPortField.setText(mwclient.getConfig().getParam("SERVERPORT"));
        dataPortField.setText(mwclient.getConfig().getParam("DATAPORT"));

        // Show the dialog and get the user's input
        dialog.setVisible(true);
        dialog.requestFocus();
        usernameField.requestFocus();
        dialog.setLocationRelativeTo(mwclient.getMainFrame());
        if (pane.getValue() == okayButton) {
            mwclient.getConfig().setParam("NAME", usernameField.getText());
            mwclient.setUsername(usernameField.getText());
            mwclient.setPassword(new String(passwordField.getPassword()));
            mwclient.getConfig().setParam("SERVERPORT", chatPortField.getText());
            mwclient.getConfig().setParam("DATAPORT", dataPortField.getText());
            mwclient.getConfig().setParam("SERVERIP", ipaddressField.getText());
        }

        //not ok with signing on? ok. quit!
        else {System.exit(0);}
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals(usernameCommand)) {
            passwordField.requestFocus();
        } else if (command.equals(passwordCommand)) {
            ipaddressField.requestFocus();
        } else if (command.equals(ipaddressCommand)) {
            chatPortField.requestFocus();
        } else if (command.equals(chatPortCommand)) {
            dataPortField.requestFocus();
        } else if (command.equals(dataPortCommand)) {
            okayButton.doClick(200);
            pane.setValue(okayButton);
            dialog.dispose();
        } else if (command.equals(okayCommand)) {
            pane.setValue(okayButton);
            dialog.dispose();
        } else if (command.equals(cancelCommand)) {
            pane.setValue(cancelButton);
            dialog.dispose();
        }
    }
}
