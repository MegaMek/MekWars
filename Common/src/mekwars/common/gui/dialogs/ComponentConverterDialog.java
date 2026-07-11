/*
 * MekWars - Copyright (C) 2008
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
 *       <p>
 *       Basic and advanced dialog for converting components into crits
 */

package mekwars.common.gui.dialogs;

import java.util.Objects;

import megamek.common.TechConstants;
import mekwars.common.BMEquipment;
import mekwars.common.House;
import mekwars.common.Unit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.ComponentToCritsConverter;

public final class ComponentConverterDialog implements java.awt.event.ActionListener {

    private final static String okayCommand = "okay";
    private final static String cancelCommand = "cancel";
    private final static String selectorButtonCommand = "selectorbuttoncommand";
    private final static String windowName = "Component Crit Converter";

    private final javax.swing.JPanel mainPanel = new javax.swing.JPanel(); // main Panel for everything
    private final javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(); // the scrolly thingy
    private final javax.swing.JPanel masterPanel = new javax.swing.JPanel();
    private final javax.swing.JComboBox factionCombo;
    private final javax.swing.JButton okayButton = new javax.swing.JButton("OK");
    private final javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
    private final javax.swing.JButton modeButton = new javax.swing.JButton("Advanced");
    private final boolean isMod;
    private final javax.swing.JDialog dialog;
    private final javax.swing.JOptionPane pane;
    String[] units = { Unit.getTypeClassDesc(Unit.MEK), Unit.getTypeClassDesc(Unit.VEHICLE),
                       Unit.getTypeClassDesc(Unit.INFANTRY), Unit.getTypeClassDesc(Unit.PROTOMEK),
                       Unit.getTypeClassDesc(Unit.BATTLEARMOR), Unit.getTypeClassDesc(Unit.AERO) };
    String[] weight = { Unit.getWeightClassDesc(Unit.LIGHT), Unit.getWeightClassDesc(Unit.MEDIUM),
                        Unit.getWeightClassDesc(Unit.HEAVY), Unit.getWeightClassDesc(Unit.ASSAULT) };
    IClient client;
    private boolean isAdvanced = false;
    private int basicWeight = Unit.LIGHT;
    private int basicType = Unit.MEK;
    private int basicAmount = 100;

    public ComponentConverterDialog(IClient client) {

        this.client = client;

        isMod = client.isMod() || client.isAdmin();

        java.util.Collection<House> factions = client.getData().getAllHouses();
        java.util.TreeSet<String> factionNames = new java.util.TreeSet<>();// tree to alpha sort
        for (House house : factions) {
            factionNames.add(house.getName());
        }
        factionCombo = new javax.swing.JComboBox(factionNames.toArray());
        factionCombo.addActionListener(this);

        if (isMod) {
            masterPanel.add(factionCombo);
        }

        scrollPane.add(mainPanel);
        scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setViewportView(mainPanel);

        mainPanel.setLayout(new javax.swing.BoxLayout(mainPanel, javax.swing.BoxLayout.Y_AXIS));
        masterPanel.setLayout(new javax.swing.BoxLayout(masterPanel, javax.swing.BoxLayout.Y_AXIS));

        masterPanel.add(scrollPane);


        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);

        cancelButton.addActionListener(this);
        cancelButton.setActionCommand(cancelCommand);

        modeButton.addActionListener(this);
        modeButton.setActionCommand(selectorButtonCommand);

        // Set the user's options
        Object[] options = { okayButton, cancelButton, modeButton };

        // Create the pane containing the buttons
        pane = new javax.swing.JOptionPane(masterPanel,
              javax.swing.JOptionPane.PLAIN_MESSAGE,
              javax.swing.JOptionPane.DEFAULT_OPTION,
              null,
              options,
              null);

        java.awt.Dimension maxSize = new java.awt.Dimension(120, 50);

        factionCombo.setMaximumSize(maxSize);
        factionCombo.setPreferredSize(maxSize);
        // Create the main dialog and set the default button
        dialog = pane.createDialog(scrollPane, windowName);
        dialog.getRootPane().setDefaultButton(cancelButton);

        // Show the dialog and get the user's input
        dialog.setLocationRelativeTo(client.getMainFrame());
        dialog.setModal(true);
        dialog.setResizable(true);
        if (isMod) {
            factionCombo.setSelectedIndex(0);
        } else {
            requestComponents(client.getPlayer().getHouse());
        }
        dialog.pack();
        dialog.setVisible(true);

        if (pane.getValue() == okayButton) {

            for (int pos = mainPanel.getComponentCount() - 1; pos >= 0; pos--) {
                javax.swing.JPanel panel = (javax.swing.JPanel) mainPanel.getComponent(pos);
                findAndSaveConfigs(panel);
            }
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c getcomponentconversion");
        } else {
            dialog.dispose();
        }
    }

    /**
     * This method will tunnel through all of the panels of the config UI to find any changed text fields or checkboxes.
     * Then it will send the new configs to the server.
     *
     */
    public void findAndSaveConfigs(javax.swing.JPanel panel) {
        String crit = null;
        String amount = null;
        int weight = 0;
        int type = 0;
        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            // found another JPanel keep digging!
            if (field instanceof javax.swing.JPanel) {
                findAndSaveConfigs((javax.swing.JPanel) field);
            } else if (field instanceof javax.swing.JTextField textBox) {

                if (textBox.getName().equals("amount")) {
                    amount = textBox.getText();
                } else {
                    crit = textBox.getName();
                }
            } else if (field instanceof javax.swing.JComboBox combo) {

                if (combo.getName().equals("weight")) {
                    weight = combo.getSelectedIndex();
                } else {
                    type = combo.getSelectedIndex();
                }

            }
        }

        ComponentToCritsConverter converter = client.getCampaign().getComponentConverter().get(crit);

        if (converter == null || converter.getComponentUsedType() != type
                  || converter.getComponentUsedWeight() != weight
                  || converter.getMinCritLevel() != Integer.parseInt(Objects.requireNonNull(amount))) {

            if (isMod) {
                client.sendChat(
                      String.format("%sc Setcomponentconversion#%s#%s#%s#%s#%s", IClient.CAMPAIGN_PREFIX, crit, weight, type, amount, Objects.requireNonNull(
                            factionCombo.getSelectedItem()).toString()));
            } else {
                client.sendChat(
                      String.format("%sc Setcomponentconversion#%s#%s#%s#%s", IClient.CAMPAIGN_PREFIX, crit, weight, type, amount));
            }
        }

    }

    public void findAndBasicConfigs(javax.swing.JPanel panel) {
        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            // found another JPanel keep digging!
            if (field instanceof javax.swing.JPanel) {
                findAndBasicConfigs((javax.swing.JPanel) field);
            } else if (field instanceof javax.swing.JTextField textBox) {

                if (textBox.getName().equals("amount")) {
                    basicAmount = Integer.parseInt(textBox.getText());
                }
            } else if (field instanceof javax.swing.JComboBox combo) {

                if (combo.getName().equals("weight")) {
                    basicWeight = combo.getSelectedIndex();
                } else {
                    basicType = combo.getSelectedIndex();
                }

            }
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals(okayCommand)) {
            pane.setValue(okayButton);
            dialog.dispose();
        } else if (command.equals(cancelCommand)) {
            pane.setValue(cancelButton);
            dialog.dispose();
        } else if (command.equals(selectorButtonCommand)) {
            switchView();
        } else if (e.getSource() instanceof javax.swing.JComboBox box) {
            if (isMod) {
                requestComponents(Objects.requireNonNull(box.getSelectedItem()).toString());
            }
        }

    }

    private void requestComponents(String faction) {
        client.setWaiting(true);
        client.sendChat(String.format("%sc getcomponentconversion#%s", IClient.CAMPAIGN_PREFIX, faction));
        while (client.isWaiting()) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {

            }
        }
        isAdvanced = !client.getCampaign().getComponentConverter().containsKey("All");
        switchView();
    }


    public void switchView() {

        javax.swing.JPanel critPanel;
        javax.swing.JTextField baseTextField;
        javax.swing.JComboBox weightCombo;
        javax.swing.JComboBox typeCombo;
        if (!isAdvanced) {
            mainPanel.removeAll();
            scrollPane.setSize(300, 40);
            scrollPane.setPreferredSize(scrollPane.getSize());
            scrollPane.setMinimumSize(scrollPane.getSize());
            dialog.setSize(400, 140);
            dialog.setPreferredSize(dialog.getSize());
            dialog.setMinimumSize(dialog.getSize());

            ComponentToCritsConverter converter = client.getCampaign().getComponentConverter().get("All");

            if (converter == null) {
                converter = new ComponentToCritsConverter();
                converter.setCritName("All");
                converter.setComponentUsedType(Unit.MEK);
                converter.setComponentUsedWeight(Unit.LIGHT);
                converter.setMinCritLevel(100);
            }

            critPanel = new javax.swing.JPanel();
            baseTextField = new javax.swing.JTextField(5);
            baseTextField.setEditable(false);
            baseTextField.setName(converter.getCritName());
            baseTextField.setText(converter.getCritName());
            critPanel.add(baseTextField);

            weightCombo = new javax.swing.JComboBox(weight);
            weightCombo.setName("weight");
            weightCombo.setSelectedIndex(converter.getComponentUsedWeight());
            critPanel.add(weightCombo);

            typeCombo = new javax.swing.JComboBox(units);
            typeCombo.setName("type");
            typeCombo.setSelectedIndex(converter.getComponentUsedType());
            critPanel.add(typeCombo);

            baseTextField = new javax.swing.JTextField(5);
            baseTextField.setName("amount");
            baseTextField.setText(Integer.toString(converter.getMinCritLevel()));
            critPanel.add(baseTextField);

            mainPanel.add(critPanel);
            modeButton.setText("Advanced");
        } else {
            findAndBasicConfigs(mainPanel);
            mainPanel.removeAll();
            for (BMEquipment eq : client.getCampaign().getBlackMarketParts().values()) {

                if ((Boolean.parseBoolean(client.getServerConfigs("AllowCrossOverTech"))
                           || client.getPlayer().getHouseFightingFor().getTechLevel() == TechConstants.T_ALL
                           || eq.getTechLevel() == TechConstants.T_ALL
                           || client.getPlayer().getHouseFightingFor().getTechLevel() >= eq.getTechLevel())
                          && eq.getCost() > 0) {

                    ComponentToCritsConverter converter = client.getCampaign()
                                                                .getComponentConverter()
                                                                .get(eq.getEquipmentInternalName());

                    if (converter == null) {
                        converter = new ComponentToCritsConverter();
                        converter.setCritName(eq.getEquipmentInternalName());
                        converter.setComponentUsedType(basicType);
                        converter.setComponentUsedWeight(basicWeight);
                        converter.setMinCritLevel(basicAmount);
                    }

                    critPanel = new javax.swing.JPanel();
                    baseTextField = new javax.swing.JTextField(25);
                    baseTextField.setEditable(false);
                    baseTextField.setName(eq.getEquipmentInternalName());
                    baseTextField.setText(eq.getEquipmentName());
                    critPanel.add(baseTextField);

                    weightCombo = new javax.swing.JComboBox(weight);
                    weightCombo.setName("weight");
                    weightCombo.setSelectedIndex(converter.getComponentUsedWeight());
                    critPanel.add(weightCombo);

                    typeCombo = new javax.swing.JComboBox(units);
                    typeCombo.setSelectedIndex(converter.getComponentUsedType());
                    typeCombo.setName("type");
                    critPanel.add(typeCombo);

                    baseTextField = new javax.swing.JTextField(5);
                    baseTextField.setName("amount");
                    baseTextField.setText(Integer.toString(converter.getMinCritLevel()));
                    critPanel.add(baseTextField);

                    mainPanel.add(critPanel);
                }
            }
            scrollPane.setSize(400, 400);
            scrollPane.setPreferredSize(scrollPane.getSize());
            scrollPane.setMinimumSize(scrollPane.getSize());
            dialog.setSize(500, 500);
            dialog.setPreferredSize(dialog.getSize());
            dialog.setMinimumSize(dialog.getSize());
            modeButton.setText("Basic");
        }
        isAdvanced = !isAdvanced;
        masterPanel.setVisible(false);
        masterPanel.setVisible(true);
    }
}
