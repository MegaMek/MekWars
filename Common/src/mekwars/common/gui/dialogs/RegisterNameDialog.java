/*
 * MekWars - Copyright (C) 2006
 *
 * Original author: nmorris (urgru@users.sourceforge.net)
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
import mekwars.common.util.StringUtils;

/**
 * Modal dialog that lets a connected player register their current in-game nickname with a permanent
 * username/password, so that name can no longer be used by an unregistered player and can be signed into from
 * other clients. Prompts for a username (pre-filled with the player's current name), a password, and a password
 * confirmation.
 * <p>
 * Like {@link SignOnDialog} and {@link ConfigurationDialog}, all of the dialog's work happens inside the
 * constructor: it builds the UI, shows it modally, validates the entered passwords, and -- if valid -- sends a
 * {@code register} chat command to the server. There is no separate show()/getResult() step; simply constructing
 * this class displays the dialog and processes its result as a side effect.
 * <p>
 * Note the on-screen disclaimer: the password is transmitted and stored in plain text, not hashed or encrypted.
 */
public final class RegisterNameDialog implements java.awt.event.ActionListener {

    private final String okayCommand = "okay";
    private final String cancelCommand = "cancel";

    private final javax.swing.JButton okayButton = new javax.swing.JButton("OK");
    private final javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");

    private final javax.swing.JDialog dialog;
    private final javax.swing.JOptionPane pane;

    /**
     * Builds and shows the registration dialog modally, pre-filled with the player's current username. Blocks
     * until the user presses OK or Cancel.
     * <p>
     * If OK is pressed: the two password fields are compared and checked for invalid characters via
     * {@link StringUtils#hasBadChars(String)}. If they match and contain no bad characters, a
     * {@code /campaign register <username>,<password>} chat command is sent to the server; otherwise an error
     * message describing the problem is routed to the client via {@link IClient#doParseDataInput(String)}
     * instead of being sent to the server. Regardless of whether registration was attempted, a
     * {@code setclientversion} chat command is always sent afterward, and the dialog is disposed.
     * <p>
     * If Cancel is pressed, no chat commands related to registration are sent, but the trailing
     * {@code setclientversion} call still fires before the dialog is disposed.
     *
     * @param client the connected client used to read the current player/username, send chat commands, and
     *               report validation errors to the user
     */
    public RegisterNameDialog(IClient client) {

        // Set the actions to generate
        okayButton.setActionCommand(okayCommand);
        cancelButton.setActionCommand(cancelCommand);

        // Set the listeners to this object
        javax.swing.JTextField usernameField = new javax.swing.JTextField();
        usernameField.addActionListener(this);
        javax.swing.JPasswordField passwordField1 = new javax.swing.JPasswordField();
        passwordField1.addActionListener(this);
        javax.swing.JPasswordField passwordField2 = new javax.swing.JPasswordField();
        passwordField2.addActionListener(this);
        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);

        // set tool tips (balloon help)
        usernameField.setToolTipText("Username to register");
        passwordField1.setToolTipText("Password to set.");
        passwordField2.setToolTipText("Confirm password.");

        // Create the panel holding the labels and text fields
        int panelRows = 3;

        javax.swing.JPanel fieldPanel = new javax.swing.JPanel(new java.awt.GridLayout(panelRows, 2), false);
        fieldPanel.add(new javax.swing.JLabel("Username: ", javax.swing.SwingConstants.LEFT));
        fieldPanel.add(usernameField);
        fieldPanel.add(new javax.swing.JLabel("Password1: ", javax.swing.SwingConstants.LEFT));
        fieldPanel.add(passwordField1);
        fieldPanel.add(new javax.swing.JLabel("Password2: ", javax.swing.SwingConstants.LEFT));
        fieldPanel.add(passwordField2);

        javax.swing.JPanel messagePanel = new javax.swing.JPanel();
        messagePanel.add(new javax.swing.JLabel("<HTML><b><center>" +
                                                      "Note: password will be stored<br>" +
                                                      "and transmitted in plain text.</b></center></HTML>"));

        javax.swing.JPanel textPanel = new javax.swing.JPanel();
        textPanel.setLayout(new javax.swing.BoxLayout(textPanel, javax.swing.BoxLayout.Y_AXIS));
        textPanel.add(fieldPanel);
        textPanel.add(messagePanel);

        // Create the panel that will hold the entire UI
        javax.swing.JPanel mainPanel = new javax.swing.JPanel(false);

        // Set the user's options
        Object[] options = { okayButton, cancelButton };

        // Create the pane containing the buttons
        pane = new javax.swing.JOptionPane(textPanel,
              javax.swing.JOptionPane.PLAIN_MESSAGE,
              javax.swing.JOptionPane.DEFAULT_OPTION,
              null,
              options,
              passwordField1);

        // Create the main dialog and set the default button
        dialog = pane.createDialog(mainPanel, "Register Nickname");
        dialog.getRootPane().setDefaultButton(okayButton);

        // Fill username field with current name
        usernameField.setText(client.getPlayer().getName());

        // Show the dialog and get the user's input
        dialog.setVisible(true);
        dialog.requestFocus();
        usernameField.requestFocus();

        if (pane.getValue() == okayButton) {

            String pass1 = String.valueOf(passwordField1.getPassword());
            String pass2 = String.valueOf(passwordField2.getPassword());
            String passValid = StringUtils.hasBadChars(pass1);

            boolean passwordValid = false;
            StringBuilder toUser = new StringBuilder();

            if (!passValid.trim().isEmpty()) {
                toUser.append("CH|CLIENT: Invalid Characters in the password.  Registration failed.<br>");
                pass2 = "";
            }

            if (!pass1.equals(pass2)) {
                toUser.append("CH|CLIENT: Passwords did not match. Registration failed.<br>");
            } else {
                passwordValid = true;
            }

            if (passwordValid) {
                client.sendChat(String.format("%sregister %s,%s", IClient.CAMPAIGN_PREFIX, usernameField.getText(), String.valueOf(
                      passwordField1.getPassword())));
            } else {
                client.doParseDataInput(toUser.toString());
            }
        }
        client.sendChat(
              String.format("%sc setclientversion#%s#%s", IClient.CAMPAIGN_PREFIX, client.getUsername()
                                                                        .trim(), IClient.CLIENT_VERSION));

        dialog.dispose();
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {

        String command = e.getActionCommand();
        if (command.equals(okayCommand)) {
            pane.setValue(okayButton);
            dialog.dispose();
        } else if (command.equals(cancelCommand)) {
            pane.setValue(cancelButton);
            dialog.dispose();
        }
    }
}
