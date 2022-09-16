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
 *
 * Basic and advanced dialog for converting components into crits
 */

package client.gui.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import client.MWClient;
import common.BMEquipment;
import common.House;
import common.Unit;
import common.util.ComponentToCritsConverter;
import megamek.common.TechConstants;

public final class ComponentConverterDialog implements ActionListener {

    private final static String okayCommand = "okay";
    private final static String cancelCommand = "cancel";
    private final static String selectorButtonCommand = "selectorbuttoncommand";
    private final static String windowName = "Component Crit Converter";

    protected JPanel mainPanel = new JPanel(); // main Panel for everything
    protected JPanel critPanel = new JPanel();
    protected JScrollPane scrollPane = new JScrollPane(); // the scrolly thingy
    protected JPanel masterPanel = new JPanel();

    private JTextField baseTextField = new JTextField(5);

    String[] units = { Unit.getTypeClassDesc(Unit.MEK), Unit.getTypeClassDesc(Unit.VEHICLE), Unit.getTypeClassDesc(Unit.INFANTRY), Unit.getTypeClassDesc(Unit.PROTOMEK), Unit.getTypeClassDesc(Unit.BATTLEARMOR), Unit.getTypeClassDesc(Unit.AERO) };
    String[] weight = { Unit.getWeightClassDesc(Unit.LIGHT), Unit.getWeightClassDesc(Unit.MEDIUM), Unit.getWeightClassDesc(Unit.HEAVY), Unit.getWeightClassDesc(Unit.ASSAULT) };
    protected JComboBox weightCombo;
    protected JComboBox typeCombo;
    protected JComboBox factionCombo;

    private final JButton okayButton = new JButton("OK");
    private final JButton cancelButton = new JButton("Cancel");
    private final JButton modeButton = new JButton("Advanced");
    private boolean isAdvanced = false;

    private int basicWeight = Unit.LIGHT;
    private int basicType = Unit.MEK;
    private int basicAmount = 100;
    private boolean isMod = false;

    private JDialog dialog;
    private JOptionPane pane;

    MWClient mwclient = null;

    public ComponentConverterDialog(MWClient mwclient) {

        this.mwclient = mwclient;

        isMod = mwclient.isMod() || mwclient.isAdmin();

        Collection<House> factions = mwclient.getData().getAllHouses();
        TreeSet<String> factionNames = new TreeSet<String>();// tree to alpha sort
        for (Iterator<House> it = factions.iterator(); it.hasNext();) {
            House house = it.next();
            factionNames.add(house.getName());
        }
        factionCombo = new JComboBox(factionNames.toArray());
        factionCombo.addActionListener(this);

        if (isMod) {
            masterPanel.add(factionCombo);
        }

        scrollPane.add(mainPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setViewportView(mainPanel);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        masterPanel.setLayout(new BoxLayout(masterPanel, BoxLayout.Y_AXIS));

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
        pane = new JOptionPane(masterPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options, null);

        Dimension maxSize = new Dimension(120, 50);

        factionCombo.setMaximumSize(maxSize);
        factionCombo.setPreferredSize(maxSize);
        // Create the main dialog and set the default button
        dialog = pane.createDialog(scrollPane, windowName);
        dialog.getRootPane().setDefaultButton(cancelButton);

        // Show the dialog and get the user's input
        dialog.setLocationRelativeTo(mwclient.getMainFrame());
        dialog.setModal(true);
        dialog.setResizable(true);
        if (isMod) {
            factionCombo.setSelectedIndex(0);
        } else {
            requestComponents(mwclient.getPlayer().getHouse());
        }
        dialog.pack();
        dialog.setVisible(true);

        if (pane.getValue() == okayButton) {

            for (int pos = mainPanel.getComponentCount() - 1; pos >= 0; pos--) {
                JPanel panel = (JPanel) mainPanel.getComponent(pos);
                findAndSaveConfigs(panel);
            }
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c getcomponentconversion");
        } else {
            dialog.dispose();
        }
    }

    /**
     * This method will tunnel through all of the panels of the config UI to
     * find any changed text fields or checkboxes. Then it will send the new
     * configs to the server.
     *
     * @param panel
     */
    public void findAndSaveConfigs(JPanel panel) {
        String crit = null;
        String amount = null;
        int weight = 0;
        int type = 0;
        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            // found another JPanel keep digging!
            if (field instanceof JPanel) {
                findAndSaveConfigs((JPanel) field);
            } else if (field instanceof JTextField) {
                JTextField textBox = (JTextField) field;

                if (textBox.getName().equals("amount")) {
                    amount = textBox.getText();
                } else {
                    crit = textBox.getName();
                }
            } else if (field instanceof JComboBox) {

                JComboBox combo = (JComboBox) field;
                if (combo.getName().equals("weight")) {
                    weight = combo.getSelectedIndex();
                } else {
                    type = combo.getSelectedIndex();
                }

            }
        }

        ComponentToCritsConverter converter = mwclient.getCampaign().getComponentConverter().get(crit);

        if ( converter == null || converter.getComponentUsedType() != type
                || converter.getComponentUsedWeight() != weight
                || converter.getMinCritLevel() != Integer.parseInt(amount) ){

            if (isMod) {
                mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c Setcomponentconversion#" + crit + "#" + weight + "#" + type + "#" + amount + "#" + factionCombo.getSelectedItem().toString());
            } else {
                mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c Setcomponentconversion#" + crit + "#" + weight + "#" + type + "#" + amount);
            }
        }

    }

    public void findAndBasicConfigs(JPanel panel) {
        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            // found another JPanel keep digging!
            if (field instanceof JPanel) {
                findAndBasicConfigs((JPanel) field);
            } else if (field instanceof JTextField) {
                JTextField textBox = (JTextField) field;

                if (textBox.getName().equals("amount")) {
                    basicAmount = Integer.parseInt(textBox.getText());
                }
            } else if (field instanceof JComboBox) {

                JComboBox combo = (JComboBox) field;
                if (combo.getName().equals("weight")) {
                    basicWeight = combo.getSelectedIndex();
                } else {
                    basicType = combo.getSelectedIndex();
                }

            }
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
        } else if (command.equals(selectorButtonCommand)) {
            switchView();
        } else if (e.getSource() instanceof JComboBox) {
            if (isMod) {
                JComboBox box = (JComboBox) e.getSource();
                requestComponents(box.getSelectedItem().toString());
            }
        }

    }

    private void requestComponents(String faction) {
        mwclient.setWaiting(true);
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c getcomponentconversion#" + faction);
        while (mwclient.isWaiting()) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {

            }
        }
        isAdvanced = !mwclient.getCampaign().getComponentConverter().containsKey("All");
        switchView();
    }


    public void switchView() {

        if (!isAdvanced) {
            mainPanel.removeAll();
            scrollPane.setSize(300, 40);
            scrollPane.setPreferredSize(scrollPane.getSize());
            scrollPane.setMinimumSize(scrollPane.getSize());
            dialog.setSize(400, 140);
            dialog.setPreferredSize(dialog.getSize());
            dialog.setMinimumSize(dialog.getSize());

            ComponentToCritsConverter converter = mwclient.getCampaign().getComponentConverter().get("All");

            if (converter == null) {
                converter = new ComponentToCritsConverter();
                converter.setCritName("All");
                converter.setComponentUsedType(Unit.MEK);
                converter.setComponentUsedWeight(Unit.LIGHT);
                converter.setMinCritLevel(100);
            }

            critPanel = new JPanel();
            baseTextField = new JTextField(5);
            baseTextField.setEditable(false);
            baseTextField.setName(converter.getCritName());
            baseTextField.setText(converter.getCritName());
            critPanel.add(baseTextField);

            weightCombo = new JComboBox(weight);
            weightCombo.setName("weight");
            weightCombo.setSelectedIndex(converter.getComponentUsedWeight());
            critPanel.add(weightCombo);

            typeCombo = new JComboBox(units);
            typeCombo.setName("type");
            typeCombo.setSelectedIndex(converter.getComponentUsedType());
            critPanel.add(typeCombo);

            baseTextField = new JTextField(5);
            baseTextField.setName("amount");
            baseTextField.setText(Integer.toString(converter.getMinCritLevel()));
            critPanel.add(baseTextField);

            mainPanel.add(critPanel);
            modeButton.setText("Advanced");
        } else {
            findAndBasicConfigs(mainPanel);
            mainPanel.removeAll();
            for (BMEquipment eq : mwclient.getCampaign().getBlackMarketParts().values()) {

                if ( (Boolean.parseBoolean(mwclient.getserverConfigs("AllowCrossOverTech"))
                        || mwclient.getPlayer().getHouseFightingFor().getTechLevel() == TechConstants.T_ALL
                        || eq.getTechLevel() == TechConstants.T_ALL
                        || mwclient.getPlayer().getHouseFightingFor().getTechLevel() >= eq.getTechLevel())
                        && eq.getCost() > 0 ) {

                    ComponentToCritsConverter converter = mwclient.getCampaign().getComponentConverter().get(eq.getEquipmentInternalName());

                    if ( converter == null ) {
                        converter = new ComponentToCritsConverter();
                        converter.setCritName(eq.getEquipmentInternalName());
                        converter.setComponentUsedType(basicType);
                        converter.setComponentUsedWeight(basicWeight);
                        converter.setMinCritLevel(basicAmount);
                    }

                    critPanel = new JPanel();
                    baseTextField = new JTextField(25);
                    baseTextField.setEditable(false);
                    baseTextField.setName(eq.getEquipmentInternalName());
                    baseTextField.setText(eq.getEquipmentName());
                    critPanel.add(baseTextField);

                    weightCombo = new JComboBox(weight);
                    weightCombo.setName("weight");
                    weightCombo.setSelectedIndex(converter.getComponentUsedWeight());
                    critPanel.add(weightCombo);

                    typeCombo = new JComboBox(units);
                    typeCombo.setSelectedIndex(converter.getComponentUsedType());
                    typeCombo.setName("type");
                    critPanel.add(typeCombo);

                    baseTextField = new JTextField(5);
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