/*
 * MekWars - Copyright (C) 2007
 *
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

/**
 * @author jtighe
 *
 */

package mekwars.admin.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Hashtable;
import java.util.Objects;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import mekwars.common.House;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.SpringLayoutHelper;


public final class FactionToFactionRewardPointMultiplierDialog implements ActionListener, KeyListener {

    private final static String okayCommand = "okay";
    private final static String cancelCommand = "cancel";

    private final JTextField multiplierText = new JTextField(5);

    private final JButton okayButton = new JButton("OK");
    private final JButton cancelButton = new JButton("Cancel");

    private final JDialog dialog;
    private final JOptionPane pane;

    private final JComboBox<String> faction1;
    private JComboBox<String> faction2 = null;
    private final Hashtable<String, String> configChanges = new Hashtable<>();

    IClient client;

    /**
     * @author jtighe
     *       <p>
     *       Opens the server config page in the client.
     */

    public FactionToFactionRewardPointMultiplierDialog(IClient client) {

        this.client = client;
        String windowName = "MekWars Faction to Faction Reward Point Multiplier";

        //TAB PANELS (these are added to the root pane as tabs)
        JPanel mainPanel = new JPanel();
        JPanel mainBoxPanel = new JPanel(new SpringLayout());

        TreeSet<String> factionNames = new TreeSet<>();

        for (House faction : client.getData().getAllHouses()) {factionNames.add(faction.getName());}

        faction1 = new JComboBox(factionNames.toArray());
        faction1.setSelectedIndex(0);
        faction1.addActionListener(this);
        faction2 = new JComboBox(factionNames.toArray());
        faction2.addActionListener(this);
        faction2.setSelectedIndex(0);

        multiplierText.addKeyListener(this);

        //finalize the layout.
        mainBoxPanel.add(faction1);
        mainBoxPanel.add(new JLabel(" to "));
        mainBoxPanel.add(faction2);
        mainBoxPanel.add(new JLabel(" multipler "));
        mainBoxPanel.add(multiplierText);

        SpringLayoutHelper.setupSpringGrid(mainBoxPanel, 5);
        mainPanel.add(mainBoxPanel);

        // Set the actions to generate
        okayButton.setActionCommand(okayCommand);
        cancelButton.setActionCommand(cancelCommand);
        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);

        /*
         * NEW OPTIONS - need to be sorted into proper menus.
         */

        // Set tool tips (balloon help)
        okayButton.setToolTipText("Save Options");
        cancelButton.setToolTipText("Exit without saving options");

        //Create the panel that will hold the entire UI
        JPanel mainConfigPanel = new JPanel();

        mainConfigPanel.add(mainPanel);
        // Set the user's options
        Object[] options = { okayButton, cancelButton };

        // Create the pane containing the buttons
        pane = new JOptionPane(mainConfigPanel, JOptionPane.PLAIN_MESSAGE,
              JOptionPane.DEFAULT_OPTION, null, options, null);

        // Create the main dialog and set the default button
        dialog = pane.createDialog(mainConfigPanel, windowName);
        dialog.getRootPane().setDefaultButton(cancelButton);


        //Show the dialog and get the user's input
        dialog.setLocationRelativeTo(client.getMainFrame());
        dialog.setModal(true);
        dialog.pack();
        dialog.setVisible(true);

        if (pane.getValue() == okayButton) {

            if (!configChanges.isEmpty()) {
                StringBuilder changes = new StringBuilder();
                for (String key : configChanges.keySet()) {
                    changes.append(key);
                    changes.append("#");
                    changes.append(configChanges.get(key));
                    client.sendChat(IClient.CAMPAIGN_PREFIX +
                                          "c SetFactionToFactionRewardPointMultiplier#" +
                                          changes);
                    changes.setLength(0);
                }

            }
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c adminsaveserverconfigs");
            client.getServerConfigData();

        } else {
            dialog.dispose();
            if (!configChanges.isEmpty()) {client.getServerConfigData();}
        }
    }


    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals(okayCommand)) {
            saveChanges();
            pane.setValue(okayButton);
            dialog.dispose();
        } else if (command.equals(cancelCommand)) {
            pane.setValue(cancelButton);
            dialog.dispose();
        } else {
            String config = faction1.getSelectedItem().toString() +
                                  "To" +
                                  faction2.getSelectedItem().toString() +
                                  "RewardPointMultiplier";
            multiplierText.setText(client.getServerConfigs(config));
        }
    }

    public void saveChanges() {
        String config = Objects.requireNonNull(faction1.getSelectedItem()) +
                              "To" +
                              Objects.requireNonNull(faction2.getSelectedItem()) +
                              "RewardPointMultiplier";
        client.putServerConfigs(config, multiplierText.getText());
        configChanges.put(faction1.getSelectedItem().toString() + "#" + faction2.getSelectedItem().toString(),
              multiplierText.getText());
    }


    public void keyPressed(KeyEvent e) {
        saveChanges();

    }


    public void keyReleased(KeyEvent e) {
        saveChanges();

    }


    public void keyTyped(KeyEvent e) {
        saveChanges();
    }
}
