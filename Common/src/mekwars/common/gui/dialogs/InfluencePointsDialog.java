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

//@ Salient , copy of RewardPointsDialog

package mekwars.common.gui.dialogs;


import mekwars.common.House;
import mekwars.common.Planet;
import mekwars.common.UnitFactory;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.SpringLayoutHelper;

public final class InfluencePointsDialog implements java.awt.event.ActionListener, java.awt.event.KeyListener {

    private final static String okayCommand = "Okay";
    private final static String cancelCommand = "Cancel";
    private final static String rewardCommand = "Reward";
    private final static String refreshCommand = "Refresh";
    //store the client backlink for other things to use
    private final IClient client;
    private final javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");

    //TEXT FIELDS
    //tab names
    private final javax.swing.JLabel costLabel = new javax.swing.JLabel();
    private final javax.swing.JLabel refreshLabel = new javax.swing.JLabel("Refresh:",
          javax.swing.SwingConstants.TRAILING);

    private final javax.swing.JComboBox<String> rewardsComboBox;
    private final javax.swing.JTextField amountText = new javax.swing.JTextField(5);
    private final javax.swing.JLabel amountLabel;
    //STOCK DIALOUG AND PANE
    private final javax.swing.JDialog dialog;
    private final javax.swing.JOptionPane pane;
    int cost;
    private javax.swing.JComboBox<String> refreshComboBox = new javax.swing.JComboBox<>();
    //	private int fluToRepod;

    public InfluencePointsDialog(IClient client) {

        //save the client
        this.client = client;
        String windowName = this.client.getServerConfigs("FluLongName");
        amountLabel = new javax.swing.JLabel(this.client.getServerConfigs("FluShortName") + " to use:",
              javax.swing.SwingConstants.TRAILING);

        //COMBO BOXES
        java.util.TreeSet<String> names = new java.util.TreeSet<>();

        if (Integer.parseInt(this.client.getServerConfigs("Cbills_Per_Flu")) > 0) {
            names.add(this.client.getServerConfigs("MoneyLongName"));
        }

        //creates a list of factories that can be refreshed
        if (Integer.parseInt(this.client.getServerConfigs("FluToRefreshFactory")) > 0) {
            java.util.TreeSet<String> factories = new java.util.TreeSet<>();
            House faction = this.client.getData().getHouseByName(this.client.getPlayer().getHouse());
            java.util.Iterator<Planet> planets = this.client.getData().getAllPlanets().iterator();
            names.add(refreshCommand);

            while (planets.hasNext()) {
                Planet planet = planets.next();

                if (!planet.isOwner(faction.getId())) {continue;}

                for (UnitFactory factory : planet.getUnitFactories()) {
                    if (factory.getTicksUntilRefresh() > 0) {
                        factories.add(STR."\{planet.getName()}: \{factory.getName()}(\{factory.getTicksUntilRefresh()})");
                    }
                }
            }
            refreshComboBox = new javax.swing.JComboBox<>();
            factories.forEach(refreshComboBox::addItem);
        }

        if (names.isEmpty()) {names.add("None Available");}

        rewardsComboBox = new javax.swing.JComboBox<>();
        names.forEach(rewardsComboBox::addItem);
        //stored values.
        cost = 0;

        //Set the tooltips and actions for dialogue buttons
        //BUTTONS
        javax.swing.JButton okayButton = new javax.swing.JButton("OK");
        okayButton.setActionCommand(okayCommand);
        cancelButton.setActionCommand(cancelCommand);
        rewardsComboBox.setActionCommand(rewardCommand);
        refreshComboBox.setActionCommand(refreshCommand);

        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);
        okayButton.setToolTipText("Save Options");
        cancelButton.setToolTipText("Exit without saving changes");
        rewardsComboBox.addActionListener(this);
        refreshComboBox.addActionListener(this);

        amountText.addKeyListener(this);

        //CREATE THE PANELS
        javax.swing.JPanel rewardPanel = new javax.swing.JPanel();//player name, etc

        /*
         * Format the Reward Points panel. Spring layout.
         */
        rewardPanel.setLayout(new javax.swing.BoxLayout(rewardPanel, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.JPanel comboPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());
        javax.swing.JPanel costPanel = new javax.swing.JPanel();

        javax.swing.JLabel rewardLabel = new javax.swing.JLabel("Choose Action:",
              javax.swing.SwingConstants.TRAILING);
        comboPanel.add(rewardLabel);
        rewardsComboBox.setToolTipText("Select your Reward Type");
        comboPanel.add(rewardsComboBox);

        comboPanel.add(refreshLabel);
        refreshComboBox.setToolTipText("Refresh Factory");
        comboPanel.add(refreshComboBox);

        comboPanel.add(amountLabel);
        comboPanel.add(amountText);

        //run the spring layout
        SpringLayoutHelper.setupSpringGrid(comboPanel, 2);

        rewardPanel.add(comboPanel);
        costPanel.add(costLabel);
        rewardPanel.add(costPanel);

        costLabel.setText("Result: no expenditure");

        rewardsComboBox.setSelectedIndex(0);

        javax.swing.JPanel mainPanel = new javax.swing.JPanel();

        // Set the user's options
        Object[] options = { okayButton, cancelButton };

        // Create the pane containing the buttons
        pane = new javax.swing.JOptionPane(rewardPanel,
              javax.swing.JOptionPane.PLAIN_MESSAGE,
              javax.swing.JOptionPane.DEFAULT_OPTION,
              null,
              options,
              null);

        // Create the main dialog and set the default button
        dialog = pane.createDialog(mainPanel, windowName);
        dialog.getRootPane().setDefaultButton(cancelButton);
        dialog.setLocationRelativeTo(this.client.getMainFrame());

        //Show the dialog and get the user's input
        dialog.setModal(true);
        dialog.pack();
        dialog.setVisible(true);

        if (pane.getValue() != okayButton) {
            dialog.dispose();
        }
    }

    public void keyTyped(java.awt.event.KeyEvent e) {
    }

    public void keyPressed(java.awt.event.KeyEvent e) {
    }

    public void keyReleased(java.awt.event.KeyEvent e) {
        String selection = (String) rewardsComboBox.getSelectedItem();
        cost = Integer.parseInt(amountText.getText());
        if (selection != null) {
            if (selection.equals(refreshCommand)) {
                cost = Integer.parseInt(client.getServerConfigs("FluToRefreshFactory"));
                costLabel.setText(STR."\{client.getServerConfigs("FluLongName")} Required: \{cost} \{client.getServerConfigs(
                      "FluShortName")}");
                dialog.repaint();
            } else if (selection.equals(client.getServerConfigs("MoneyLongName"))) {
                int total = cost * Integer.parseInt(client.getServerConfigs("Cbills_Per_Flu"));
                costLabel.setText(STR."Result: Gain \{client.moneyOrFluMessage(true, true, total)}");
            }
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        String command = e.getActionCommand();

        switch (command) {
            case okayCommand -> {
                String selection = (String) rewardsComboBox.getSelectedItem();

                if (selection != null) {
                    if (selection.equals(refreshCommand)) {
                        if (refreshComboBox.getComponentCount() < 1) {
                            dialog.dispose();
                        }

                        String factoryInfo = (String) refreshComboBox.getSelectedItem();

                        if (factoryInfo != null) {
                            String planet = factoryInfo.substring(0, factoryInfo.indexOf(":")).trim();
                            String factory = factoryInfo.substring(planet.length() + 2, factoryInfo.indexOf("("))
                                                   .trim();
                            String useFlu = "true";
                            client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c refreshFactory#\{planet}#\{factory}#\{useFlu}");
                        }
                    } else if (selection.equals(client.getServerConfigs("MoneyLongName"))) {
                        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c useinfluence#4#\{amountText.getText()}");
                    } else {
                        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{client.getUsername()}No Influence Spent. Options are disabled on this server.");
                    }
                }

                dialog.dispose();
            }
            case cancelCommand -> {
                pane.setValue(cancelButton);
                dialog.dispose();
            }
            case rewardCommand -> {
                String selection = (String) rewardsComboBox.getSelectedItem();

                if (selection != null) {
                    if (selection.equals(refreshCommand)) {
                        if (refreshComboBox.getItemCount() >= 1) {refreshComboBox.setSelectedIndex(0);}
                        cost = Integer.parseInt(client.getServerConfigs("FluToRefreshFactory"));
                        costLabel.setText(STR."\{client.getServerConfigs("FluLongName")} Required: \{cost} \{client.getServerConfigs(
                              "FluShortName")}");
                        makeVisible(false, false, true);
                    } else if (selection.equalsIgnoreCase(client.getServerConfigs("MoneyLongName"))) {
                        amountText.setText("0");
                        cost = Integer.parseInt(amountText.getText());
                        int total = cost * Integer.parseInt(client.getServerConfigs("Cbills_Per_Flu"));
                        costLabel.setText(STR."Result: Gain \{client.moneyOrFluMessage(true, true, total)}");
                        makeVisible(false, false, false);
                    } else {
                        makeVisible(true, false, false);
                    }
                }
            }
        }
    }

    private void makeVisible(boolean visible, boolean rePod, boolean refresh) {

        refreshComboBox.setVisible(refresh);
        refreshLabel.setVisible(refresh);

        if (rePod || refresh) {
            amountLabel.setVisible(false);
            amountText.setVisible(false);
        } else {
            amountLabel.setVisible(!visible);
            amountText.setVisible(!visible);
        }

    }

}//end RewardPointsDialog.java
