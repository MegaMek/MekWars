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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import javax.swing.*;

import mekwars.common.House;
import mekwars.common.SubFaction;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.MWLogger;
import mekwars.common.util.SpringLayoutHelper;

public final class SubFactionConfigurationDialog implements ActionListener {

    private final static String okayCommand = "okay";
    private final static String cancelCommand = "cancel";

    private final JButton okayButton = new JButton("OK");
    private final JButton cancelButton = new JButton("Cancel");

    private JDialog dialog;
    private JOptionPane pane;

    private SubFaction subFactionConfig = null;

    private final Hashtable<String, String> configChanges = new Hashtable<>();

    JTabbedPane ConfigPane = new JTabbedPane(SwingConstants.TOP);

    IClient client;

    /**
     * @param client
     *
     * @author jtighe
     *       <p>
     *       Opens the server config page in the client.
     */

    public SubFactionConfigurationDialog(IClient client, String houseName, String subFactionName) {

        this.client = client;
        String windowName = "MekWars SubFaction Configuration";
        House faction = client.getData().getHouseByName(houseName);

        if (faction == null) {return;}

        if (faction.getSubFactionList().containsKey(subFactionName)) {
            this.subFactionConfig = faction.getSubFactionList().get(subFactionName);
        } else {
            this.subFactionConfig = new SubFaction(subFactionName, "0");
            this.subFactionConfig.setConfig("MinELO", "0");
            this.subFactionConfig.setConfig("MinExp", "0");
            client.sendChat(IClient.CAMPAIGN_PREFIX +
                                  "c CreateSubFaction#" +
                                  this.subFactionConfig.getConfig("Name") +
                                  "#0#" +
                                  houseName);
        }

        //TAB PANELS (these are added to the root pane as tabs)
        JPanel mainPanel = new JPanel();

        /*
         * REPOD PANEL CONSTRUCTION
         *
         * Repod contols. Costs, factory usage, table options, etc.
         *
         * Use nested layouts. A Box containing a Flow and 3 Springs.
         */
        JPanel mainBoxPanel = new JPanel();
        JPanel mainCBoxGridPanel = new JPanel(new SpringLayout());
        JPanel mainTextBoxSpring = new JPanel(new SpringLayout());
        mainBoxPanel.setLayout(new BoxLayout(mainBoxPanel, BoxLayout.Y_AXIS));
        mainBoxPanel.add(mainCBoxGridPanel);
        mainBoxPanel.add(mainTextBoxSpring);

        //set up the flow panel

        //and then the various springs. MU first.
        JTextField baseTextField = new JTextField(5);
        mainTextBoxSpring.add(new JLabel("Name:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Sub faction name.");
        baseTextField.setName("Name");
        mainTextBoxSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        mainTextBoxSpring.add(new JLabel("Access Level:", SwingConstants.TRAILING));
        baseTextField.setToolTipText(
              "<html>Sub faciton access level<br>This is used to determine what ops can be accessed</html>");
        baseTextField.setName("AccessLevel");
        mainTextBoxSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        mainTextBoxSpring.add(new JLabel("Min Elo:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Min ELO needed to join this subfaction");
        baseTextField.setName("MinELO");
        mainTextBoxSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        mainTextBoxSpring.add(new JLabel("Min Exp:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Min Exp required to join this subfaciton");
        baseTextField.setName("MinExp");
        mainTextBoxSpring.add(baseTextField);

        JCheckBox baseCheckBox = new JCheckBox();
        for (int type = 0; type < mekwars.common.campaign.CUnit.MAX_BUILD; type++) {
            for (int weight = 0; weight <= mekwars.common.campaign.CUnit.ASSAULT; weight++) {
                baseCheckBox = new JCheckBox(STR."Can buy new \{CUnit.getWeightClassDesc(weight)} \{CUnit.getTypeClassDesc(
                      type)}");
                baseCheckBox.setToolTipText(STR."<html>Check to allow subfaction memebers to buy new<br>\{CUnit.getWeightClassDesc(
                      weight)} \{CUnit.getTypeClassDesc(type)}</html>");
                baseCheckBox.setName(STR."CanBuyNew\{CUnit.getWeightClassDesc(weight)}\{CUnit.getTypeClassDesc(type)}");
                mainCBoxGridPanel.add(baseCheckBox);
            }
        }

        for (int type = 0; type < mekwars.common.campaign.CUnit.MAX_BUILD; type++) {
            for (int weight = 0; weight <= mekwars.common.campaign.CUnit.ASSAULT; weight++) {
                baseCheckBox = new JCheckBox(STR."Can buy used \{CUnit.getWeightClassDesc(weight)} \{CUnit.getTypeClassDesc(
                      type)}");
                baseCheckBox.setToolTipText(STR."<html>Check to allow subfaction memebers to buy used<br>\{CUnit.getWeightClassDesc(
                      weight)} \{CUnit.getTypeClassDesc(type)}</html>");
                baseCheckBox.setName(STR."CanBuyUsed\{CUnit.getWeightClassDesc(weight)}\{CUnit.getTypeClassDesc(type)}");
                mainCBoxGridPanel.add(baseCheckBox);
            }
        }

        //finalize the layout.
        SpringLayoutHelper.setupSpringGrid(mainTextBoxSpring, 4);
        SpringLayoutHelper.setupSpringGrid(mainCBoxGridPanel, 4);
        mainBoxPanel.add(mainTextBoxSpring);
        mainBoxPanel.add(mainCBoxGridPanel);
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

        ConfigPane.addTab("Configs", null, mainBoxPanel, "Configs");

        //Create the panel that will hold the entire UI
        JPanel mainConfigPanel = new JPanel();

        // Set the user's options
        Object[] options = { okayButton, cancelButton };

        // Create the pane containing the buttons
        pane = new JOptionPane(ConfigPane, JOptionPane.PLAIN_MESSAGE,
              JOptionPane.DEFAULT_OPTION, null, options, null);

        // Create the main dialog and set the default button
        dialog = pane.createDialog(mainConfigPanel, windowName);
        dialog.getRootPane().setDefaultButton(cancelButton);


        for (int pos = ConfigPane.getComponentCount() - 1; pos >= 0; pos--) {
            JPanel panel = (JPanel) ConfigPane.getComponent(pos);
            findAndPopulateTextAndCheckBoxes(panel);

        }


        //Show the dialog and get the user's input
        dialog.setLocationRelativeTo(client.getMainFrame());
        dialog.setModal(true);
        dialog.pack();
        dialog.setVisible(true);

        if (pane.getValue() == okayButton) {

            for (int pos = ConfigPane.getComponentCount() - 1; pos >= 0; pos--) {
                JPanel panel = (JPanel) ConfigPane.getComponent(pos);
                findAndSaveConfigs(panel);
            }

            if (!configChanges.isEmpty()) {
                StringBuilder configPairs = new StringBuilder();

                for (String key : configChanges.keySet()) {
                    configPairs.append(key);
                    configPairs.append("#");
                    configPairs.append(configChanges.get(key));
                    configPairs.append("#");
                }

                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c SetSubFactionConfig#\{this.subFactionConfig.getConfig(
                      "Name")}#\{houseName}#\{configPairs.toString()}");
            }
            client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsave");
            client.refreshData();

        } else {dialog.dispose();}
    }

    /**
     * This Method tunnels through all of the panels to find the textfields and checkboxes. Once it find one it grabs
     * the Name() param of the object and uses that to find out what the setting should be from the
     * client.getserverConfigs() method.
     *
     * @param panel
     */
    public void findAndPopulateTextAndCheckBoxes(JPanel panel) {
        String key;

        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            if (field instanceof JPanel) {
                findAndPopulateTextAndCheckBoxes((JPanel) field);
            } else if (field instanceof JTextField textBox) {
                key = textBox.getName();
                if (key == null) {continue;}

                textBox.setMaximumSize(new Dimension(100, 10));
                textBox.setText(this.subFactionConfig.getConfig(key));
            } else if (field instanceof JCheckBox checkBox) {

                key = checkBox.getName();
                if (key == null) {
                    MWLogger.errLog(STR."Null Checkbox: \{checkBox.getToolTipText()}");
                    continue;
                }
                checkBox.setSelected(Boolean.parseBoolean(this.subFactionConfig.getConfig(key)));

            } else if (field instanceof JRadioButton radioButton) {

                key = radioButton.getName();
                if (key == null) {
                    MWLogger.errLog(STR."Null RadioButton: \{radioButton.getToolTipText()}");
                    continue;
                }
                radioButton.setSelected(Boolean.parseBoolean(this.subFactionConfig.getConfig(key)));

            }//else continue
        }
    }

    /**
     * This method will tunnel through all of the panels of the config UI to find any changed text fields or checkboxes.
     * Then it will send the new configs to the server.
     *
     * @param panel
     */
    public void findAndSaveConfigs(JPanel panel) {
        String key = null;
        String value = null;
        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            //found another JPanel keep digging!
            if (field instanceof JPanel) {
                findAndSaveConfigs((JPanel) field);
            } else if (field instanceof JTextField textBox) {

                value = textBox.getText();
                key = textBox.getName();

                if (key == null || value == null) {
                    continue;
                }

                //don't need to save this the system does it on its own
                // --Torren.
                if (key.equals("LastAutomatedBackup")) {continue;}

                //reduce bandwidth only send things that have changed.
                if (!this.subFactionConfig.getConfig(key).equalsIgnoreCase(value)) {configChanges.put(key, value);}
            } else if (field instanceof JCheckBox checkBox) {

                value = Boolean.toString(checkBox.isSelected());
                key = checkBox.getName();

                if (key == null || value.isEmpty()) {
                    continue;
                }

                //reduce bandwidth only send things that have changed.
                if (!this.subFactionConfig.getConfig(key).equalsIgnoreCase(value)) {configChanges.put(key, value);}
            } else if (field instanceof JRadioButton radioButton) {

                value = Boolean.toString(radioButton.isSelected());
                key = radioButton.getName();

                if (key == null || value.isEmpty()) {
                    continue;
                }
                //reduce bandwidth only send things that have changed.
                if (!this.subFactionConfig.getConfig(key).equalsIgnoreCase(value)) {configChanges.put(key, value);}
            }//else continue
        }

    }

    public void actionPerformed(ActionEvent e) {
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
