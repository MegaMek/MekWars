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

import java.util.Objects;

import mekwars.common.House;
import mekwars.common.Planet;
import mekwars.common.Unit;
import mekwars.common.UnitFactory;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.SpringLayoutHelper;
import mekwars.common.util.UnitUtils;

public final class RewardPointsDialog implements java.awt.event.ActionListener, java.awt.event.KeyListener {

    private final static String okayCommand = "Okay";
    private final static String cancelCommand = "Cancel";
    private final static String unitCommand = "Units";
    private final static String weightCommand = "Weight";
    private final static String rewardCommand = "Reward";
    private final static String factionCommand = "House";
    private final static String rePodCommand = "RePod";
    private final static String refreshCommand = "Refresh";
    private final static String techComboCommand = "TechCombo";
    private final static String repairCommand = "Repair";
    //store the client backlink for other things to use
    private final IClient client;
    private final javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");

    //TEXT FIELDS
    //tab names
    private final javax.swing.JLabel costLabel = new javax.swing.JLabel();
    private final javax.swing.JLabel factionLabel = new javax.swing.JLabel("House Table:",
          javax.swing.SwingConstants.TRAILING);
    private final javax.swing.JLabel unitLabel = new javax.swing.JLabel("Unit Type:",
          javax.swing.SwingConstants.TRAILING);
    private final javax.swing.JLabel weightLabel = new javax.swing.JLabel("Weight Class:",
          javax.swing.SwingConstants.TRAILING);
    private final javax.swing.JLabel repodLabel = new javax.swing.JLabel("Repod Selection:",
          javax.swing.SwingConstants.TRAILING);
    private final javax.swing.JLabel pUnitsLabel = new javax.swing.JLabel("Unit:", javax.swing.SwingConstants.TRAILING);
    private final javax.swing.JLabel refreshLabel = new javax.swing.JLabel("Refresh:",
          javax.swing.SwingConstants.TRAILING);
    private final javax.swing.JLabel techComboLabel = new javax.swing.JLabel("Tech Type:",
          javax.swing.SwingConstants.TRAILING);
    private final javax.swing.JLabel repairLabel = new javax.swing.JLabel("Repair:",
          javax.swing.SwingConstants.TRAILING);
    private final String[] weightChoices = { "Light", "Medium", "Heavy", "Assault" };
    private final javax.swing.JComboBox<String> weightComboBox = new javax.swing.JComboBox<>(weightChoices);
    private final javax.swing.JComboBox<String> rewardsComboBox;
    private final javax.swing.JComboBox<String> factionComboBox;
    private final String[] techChoices = { "Green", "Reg", "Vet", "Elite" };
    private final javax.swing.JComboBox<String> techComboBox = new javax.swing.JComboBox<>(techChoices);
    private final javax.swing.JTextField amountText = new javax.swing.JTextField(5);
    private final javax.swing.JLabel amountLabel;
    //STOCK DIALOG AND PANE
    private final javax.swing.JDialog dialog;
    private final javax.swing.JOptionPane pane;
    int cost;
    private javax.swing.JComboBox<String> unitComboBox = new javax.swing.JComboBox<>();
    private javax.swing.JComboBox<String> rePodComboBox = new javax.swing.JComboBox<>();
    private javax.swing.JComboBox<String> pUnitsComboBox = new javax.swing.JComboBox<>();
    private javax.swing.JComboBox<String> refreshComboBox = new javax.swing.JComboBox<>();
    private javax.swing.JComboBox<String> repairComboBox = new javax.swing.JComboBox<>();

    public RewardPointsDialog(IClient client) {

        //save the client
        this.client = client;
        String windowName = this.client.getServerConfigs("RPLongName");
        amountLabel = new javax.swing.JLabel(String.format("%s to use:", this.client.getServerConfigs("RPShortName")),
              javax.swing.SwingConstants.TRAILING);
        //COMBO BOXES
        java.util.TreeSet<String> names = new java.util.TreeSet<>();
        names.add("Common"); //start with the common faction
        for (House house : this.client.getData().getAllHouses()) {
            names.add(house.getName());
        }

        //check for the use of rare and add if used
        if (Boolean.parseBoolean(this.client.getServerConfigs("AllowRareUnitsForRewards"))) {names.add("Rare");}

        factionComboBox = new javax.swing.JComboBox<>();
        names.forEach(factionComboBox::addItem);

        names = new java.util.TreeSet<>();
        if (Boolean.parseBoolean(this.client.getServerConfigs("AllowTechsForRewards"))) {
            names.add("Techs");
        }

        if (Boolean.parseBoolean(this.client.getServerConfigs("AllowInfluenceForRewards"))) {
            names.add(this.client.getServerConfigs("FluLongName"));
        }

        if (Boolean.parseBoolean(this.client.getServerConfigs("AllowCBillsForRewards"))) {
            names.add(this.client.getServerConfigs("MoneyLongName"));
        }

        if (Boolean.parseBoolean(this.client.getServerConfigs("AllowUnitsForRewards"))) {
            names.add(unitCommand);

            java.util.Vector<String> unitList = new java.util.Vector<>(5, 1);

            unitList.add("Mek");
            if (Boolean.parseBoolean(this.client.getServerConfigs("UseVehicle"))) {unitList.add("Vehicle");}
            if (Boolean.parseBoolean(this.client.getServerConfigs("UseInfantry"))) {unitList.add("Infantry");}
            if (Boolean.parseBoolean(this.client.getServerConfigs("UseProtoMek"))) {unitList.add("ProtoMek");}
            if (Boolean.parseBoolean(this.client.getServerConfigs("UseBattleArmor"))) {unitList.add("BattleArmor");}
            if (Boolean.parseBoolean(this.client.getServerConfigs("UseAero"))) {unitList.add("Aero");}

            unitComboBox = new javax.swing.JComboBox<>();
            unitList.forEach(unitComboBox::addItem);
        }

        if (Boolean.parseBoolean(this.client.getServerConfigs("GlobalRepodAllowed"))) {
            names.add(rePodCommand);
            java.util.TreeSet<String> repodOptions = new java.util.TreeSet<>();

            if (Boolean.parseBoolean(this.client.getServerConfigs("RandomRepodOnly"))) {
                repodOptions.add("Random");
            } else {
                if (Boolean.parseBoolean(this.client.getServerConfigs("RandomRepodAllowed"))) {
                    repodOptions.add("Random");
                }
                repodOptions.add("Select");
            }

            rePodComboBox = new javax.swing.JComboBox<>();
            repodOptions.forEach(rePodComboBox::addItem);
            repodOptions.clear();
            for (CUnit unit : client.getPlayer().getHangar()) {
                if (!unit.isOmni()) {continue;}
                repodOptions.add(String.format("#%s %s", unit.getId(), unit.getModelName()));
            }
            pUnitsComboBox = new javax.swing.JComboBox<>();
            repodOptions.forEach(pUnitsComboBox::addItem);
        }
        if (Boolean.parseBoolean(this.client.getServerConfigs("AllowRepairsForRewards"))) {
            names.add(repairCommand);
            java.util.TreeSet<String> damagedUnits = new java.util.TreeSet<>();
            for (CUnit unit : this.client.getPlayer().getHangar()) {
                if (UnitUtils.hasArmorDamage(unit.getEntity()) || UnitUtils.hasCriticalDamage(unit.getEntity())) {
                    damagedUnits.add(String.format("#%s %s", unit.getId(), unit.getModelName()));
                }
            }
            repairComboBox = new javax.swing.JComboBox<>();
            damagedUnits.forEach(repairComboBox::addItem);
        }

        if (Boolean.parseBoolean(this.client.getServerConfigs("AllowFactoryRefreshForRewards"))) {
            java.util.TreeSet<String> factories = new java.util.TreeSet<>();
            House faction = this.client.getData().getHouseByName(this.client.getPlayer().getHouse());
            java.util.Iterator<Planet> planets = this.client.getData().getAllPlanets().iterator();
            names.add(refreshCommand);
            while (planets.hasNext()) {
                Planet planet = planets.next();
                if (!planet.isOwner(faction.getId())) {continue;}
                for (UnitFactory factory : planet.getUnitFactories()) {
                    if (factory.getTicksUntilRefresh() > 0) {
                        factories.add(String.format("%s: %s(%s)", planet.getName(), factory.getName(), factory.getTicksUntilRefresh()));
                    }
                }
            }
            refreshComboBox = new javax.swing.JComboBox<>();
            factories.forEach(refreshComboBox::addItem);
        }

        rewardsComboBox = new javax.swing.JComboBox<>();
        names.forEach(rewardsComboBox::addItem);

        //stored values.
        cost = 0;

        //Set the tooltips and actions for dialouge buttons
        //BUTTONS
        javax.swing.JButton okayButton = new javax.swing.JButton("OK");
        okayButton.setActionCommand(okayCommand);
        cancelButton.setActionCommand(cancelCommand);
        factionComboBox.setActionCommand(factionCommand);
        rewardsComboBox.setActionCommand(rewardCommand);
        weightComboBox.setActionCommand(weightCommand);
        unitComboBox.setActionCommand(unitCommand);
        rePodComboBox.setActionCommand(rePodCommand);
        refreshComboBox.setActionCommand(refreshCommand);
        techComboBox.setActionCommand(techComboCommand);
        repairComboBox.setActionCommand(repairCommand);
        //amountText.setActionCommand(amountCommand);

        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);
        okayButton.setToolTipText("Save Options");
        cancelButton.setToolTipText("Exit without saving changes");
        factionComboBox.addActionListener(this);
        rewardsComboBox.addActionListener(this);
        weightComboBox.addActionListener(this);
        unitComboBox.addActionListener(this);
        rePodComboBox.addActionListener(this);
        pUnitsComboBox.addActionListener(this);
        refreshComboBox.addActionListener(this);
        techComboBox.addActionListener(this);
        repairComboBox.addActionListener(this);

        amountText.addKeyListener(this);

        //CREATE THE PANELS
        javax.swing.JPanel rewardPanel = new javax.swing.JPanel();//player name, etc

        /*
         * Format the Reward Points panel. Spring layout.
         */
        rewardPanel.setLayout(new javax.swing.BoxLayout(rewardPanel, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.JPanel comboPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());
        javax.swing.JPanel costPanel = new javax.swing.JPanel();

        javax.swing.JLabel rewardLabel = new javax.swing.JLabel("Reward Type:",
              javax.swing.SwingConstants.TRAILING);
        comboPanel.add(rewardLabel);
        rewardsComboBox.setToolTipText("Select your Reward Type");
        comboPanel.add(rewardsComboBox);

        comboPanel.add(factionLabel);
        factionComboBox.setToolTipText("Select the faction build table you wish to use");
        comboPanel.add(factionComboBox);

        comboPanel.add(unitLabel);
        unitComboBox.setToolTipText("Select the Unit Type");
        comboPanel.add(unitComboBox);

        comboPanel.add(weightLabel);
        weightComboBox.setToolTipText("Unit Weight Class");
        comboPanel.add(weightComboBox);

        comboPanel.add(pUnitsLabel);
        pUnitsComboBox.setToolTipText("Unit");
        comboPanel.add(pUnitsComboBox);

        comboPanel.add(repodLabel);
        rePodComboBox.setToolTipText("Repod Selection Type");
        comboPanel.add(rePodComboBox);

        comboPanel.add(refreshLabel);
        refreshComboBox.setToolTipText("Refresh Factory");
        comboPanel.add(refreshComboBox);

        if (this.client.isUsingAdvanceRepairs()) {
            comboPanel.add(techComboLabel);
            techComboBox.setToolTipText("Tech Selection Type");
            techComboBox.setSelectedIndex(0);
            comboPanel.add(techComboBox);
        }

        comboPanel.add(repairLabel);
        repairComboBox.setToolTipText(String.format("Repair Unit with %ss", this.client.getServerConfigs("RPShortName")));
        comboPanel.add(repairComboBox);

        comboPanel.add(amountLabel);
        comboPanel.add(amountText);

        //run the spring layout
        SpringLayoutHelper.setupSpringGrid(comboPanel, 2);

        rewardPanel.add(comboPanel);
        costPanel.add(costLabel);
        rewardPanel.add(costPanel);

        costLabel.setText("Result: no expenditure");

        try {
            factionComboBox.setSelectedItem(this.client.getPlayer().getHouse());
        } catch (Exception ex) {
            factionComboBox.setSelectedIndex(0);
        }
        techComboBox.setSelectedIndex(0);

        if (Boolean.parseBoolean(this.client.getServerConfigs("AllowUnitsForRewards"))) {
            rewardsComboBox.setSelectedItem("Units");
            weightComboBox.setSelectedIndex(0);
            unitComboBox.setSelectedIndex(0);
        } else {rewardsComboBox.setSelectedIndex(0);}

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
            if (!selection.equals("Units")) {
                if (selection.equals("Techs")) {
                    if (!client.isUsingAdvanceRepairs()) {
                        int total = cost * Integer.parseInt(client.getServerConfigs("TechsForARewardPoint"));
                        costLabel.setText(String.format("Result: Hire %s Techs", total));
                    }
                } else if (selection.equals("RePod")) {
                    cost = Integer.parseInt(client.getServerConfigs("GlobalRepodWithRPCost"));
                    if (Objects.equals(rePodComboBox.getSelectedItem(), "Random")) {cost /= 2;}
                    costLabel.setText(String.format("%s Required: %s %s", client.getServerConfigs("RPLongName"), cost, client.getServerConfigs(
                          "RPShortName")));
                } else if (selection.equals(refreshCommand)) {
                    cost = Integer.parseInt(client.getServerConfigs("RewardPointToRefreshFactory"));
                    costLabel.setText(String.format("%s Required: %s %s", client.getServerConfigs("RPShortName"), cost, client.getServerConfigs(
                          "RPShortName")));
                    dialog.repaint();
                }
                // @Author Salient (mwosux@gmail.com) , Add RP for CBills
                else if (selection.equals(client.getServerConfigs("MoneyLongName"))) {
                    int total = cost * Integer.parseInt(client.getServerConfigs("CBillsForARewardPoint"));
                    costLabel.setText(String.format("Result: Gain %s", client.moneyOrFluMessage(true, true, total)));
                } else {
                    int total = cost * Integer.parseInt(client.getServerConfigs("InfluenceForARewardPoint"));
                    costLabel.setText(String.format("Result: Gain %s", client.moneyOrFluMessage(false, true, total)));
                }
            }
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        String command = e.getActionCommand();

        switch (command) {
            case okayCommand -> {
                String selection = (String) rewardsComboBox.getSelectedItem();

                assert selection != null;
                if (selection.equals("Units")) {
                    String type = (String) unitComboBox.getSelectedItem();
                    String weight = (String) weightComboBox.getSelectedItem();
                    String faction = (String) factionComboBox.getSelectedItem();
                    client.sendChat(String.format("%sc userewardpoints#2#%s#%s#%s", IClient.CAMPAIGN_PREFIX, type, weight, faction));
                } else if (selection.equals("Techs")) {
                    if (client.isUsingAdvanceRepairs()) {
                        client.sendChat(String.format("%sc userewardpoints#0#%s", IClient.CAMPAIGN_PREFIX, techComboBox.getSelectedIndex()));
                    } else {
                        client.sendChat(String.format("%sc userewardpoints#0#%s", IClient.CAMPAIGN_PREFIX, amountText.getText()));
                    }
                } else if (selection.equals("RePod")) {
                    if (pUnitsComboBox.getComponentCount() < 1) {dialog.dispose();}
                    String options = "#GLOBAL";
                    if (Objects.equals(rePodComboBox.getSelectedItem(), "Random")) {options += "#RANDOM";}
                    java.util.StringTokenizer unitid = new java.util.StringTokenizer((String) Objects.requireNonNull(
                          pUnitsComboBox.getSelectedItem()),
                          " ");
                    client.sendChat(String.format("%sc repod%s%s", IClient.CAMPAIGN_PREFIX, unitid.nextToken(), options));
                } else if (selection.equals(refreshCommand)) {
                    if (refreshComboBox.getComponentCount() < 1) {dialog.dispose();}
                    String factoryInfo = (String) refreshComboBox.getSelectedItem();

                    if (factoryInfo != null) {
                        String planet = factoryInfo.substring(0, factoryInfo.indexOf(":")).trim();
                        String factory = factoryInfo.substring(planet.length() + 2, factoryInfo.indexOf("(")).trim();
                        client.sendChat(String.format("%sc refreshFactory#%s#%s", IClient.CAMPAIGN_PREFIX, planet, factory));
                    }
                } else if (selection.equals(repairCommand)) {
                    String selectionName = (String) repairComboBox.getSelectedItem();
                    if (selectionName != null) {
                        client.sendChat(String.format("%sc userewardpoints#3#%s", IClient.CAMPAIGN_PREFIX, selectionName.trim()
                                                                                                   .substring(0,
                                                                                                         selectionName.indexOf(
                                                                                                               " "))));
                    }
                } else if (selection.equals(client.getServerConfigs("MoneyLongName"))) {
                    client.sendChat(String.format("%sc userewardpoints#4#%s", IClient.CAMPAIGN_PREFIX, amountText.getText()));
                } else {//flu
                    client.sendChat(String.format("%sc userewardpoints#1#%s", IClient.CAMPAIGN_PREFIX, amountText.getText()));
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
                    if (selection.equals("Units")) {
                        makeVisible(true, false, false);
                        unitComboBox.setSelectedIndex(0);
                        weightComboBox.setSelectedIndex(0);
                        try {
                            factionComboBox.setSelectedItem(client.getPlayer().getHouse());
                        } catch (Exception ex) {
                            factionComboBox.setSelectedIndex(0);
                        }

                        cost = getUnitRPCost();
                        costLabel.setText(String.format("%s Required: %s %s", client.getServerConfigs("RPShortName"), cost, client.getServerConfigs(
                              "RPShortName")));
                    } else if (selection.equals("Techs")) {
                        makeVisible(false, false, false);
                        if (client.isUsingAdvanceRepairs()) {
                            int type = techComboBox.getSelectedIndex();
                            int total = Integer.parseInt(client.getServerConfigs(String.format("RewardPointsFor%s", UnitUtils.techDescription(
                                  type))));
                            costLabel.setText(String.format("Hire 1 %s tech for %s %s", UnitUtils.techDescription(type), total, client.getServerConfigs(
                                  "RPShortName")));
                            techComboBox.setVisible(true);
                            techComboLabel.setVisible(true);

                            amountText.setVisible(false);
                            amountLabel.setVisible(false);
                        } else {
                            amountText.setText("0");
                            cost = Integer.parseInt(amountText.getText());
                            int total = cost * Integer.parseInt(client.getServerConfigs("TechsForARewardPoint"));
                            costLabel.setText(String.format("Result: Hire %s Techs", total));
                            costLabel.repaint();
                        }
                    } else if (selection.equals("RePod")) {
                        if (pUnitsComboBox.getItemCount() >= 1) {pUnitsComboBox.setSelectedIndex(0);}
                        cost = Integer.parseInt(client.getServerConfigs("GlobalRepodWithRPCost"));
                        if (Objects.equals(rePodComboBox.getSelectedItem(), "Random")) {cost /= 2;}
                        costLabel.setText(String.format("%s Required: %s %s", client.getServerConfigs("RPShortName"), cost, client.getServerConfigs(
                              "RPShortName")));
                        makeVisible(false, true, false);
                    } else if (selection.equals(refreshCommand)) {
                        if (refreshComboBox.getItemCount() >= 1) {refreshComboBox.setSelectedIndex(0);}
                        cost = Integer.parseInt(client.getServerConfigs("RewardPointToRefreshFactory"));
                        costLabel.setText(String.format("%s Required: %s %s", client.getServerConfigs("RPShortName"), cost, client.getServerConfigs(
                              "RPShortName")));
                        makeVisible(false, false, true);
                    } else if (selection.equals(repairCommand)) {
                        makeVisible(false, false, false);

                        if (repairComboBox.getItemCount() > 0) {repairComboBox.setSelectedIndex(0);}
                        costLabel.setText(String.format("Repair Cost: %s", client.getServerConfigs("RewardPointsForRepair")));
                        repairComboBox.setVisible(true);
                        repairLabel.setVisible(true);

                        amountText.setVisible(false);
                        amountLabel.setVisible(false);
                    } else if (selection.equals(client.getServerConfigs("MoneyLongName"))) {
                        amountText.setText("0");
                        cost = Integer.parseInt(amountText.getText());
                        int total = cost * Integer.parseInt(client.getServerConfigs("CBillsForARewardPoint"));
                        costLabel.setText(String.format("Result: Gain %s", client.moneyOrFluMessage(true, true, total)));
                        makeVisible(false, false, false);
                    } else {
                        amountText.setText("0");
                        cost = Integer.parseInt(amountText.getText());
                        int total = cost * Integer.parseInt(client.getServerConfigs("InfluenceForARewardPoint"));
                        costLabel.setText(String.format("Result: Gain %s", client.moneyOrFluMessage(false, true, total)));
                        makeVisible(false, false, false);
                    }
                }
            }
            case rePodCommand -> {
                cost = Integer.parseInt(client.getServerConfigs("GlobalRepodWithRPCost"));
                if (Objects.equals(rePodComboBox.getSelectedItem(), "Random")) {cost /= 2;}
                costLabel.setText(String.format("%s Required: %s %s", client.getServerConfigs("RPShortName"), cost, client.getServerConfigs(
                      "RPShortName")));
            }
            case weightCommand, unitCommand, factionCommand -> {
                cost = getUnitRPCost();
                costLabel.setText(String.format("%s Required: %s %s", client.getServerConfigs("RPShortName"), cost, client.getServerConfigs(
                      "RPShortName")));
            }
            case techComboCommand -> {
                makeVisible(false, false, false);

                int type = techComboBox.getSelectedIndex();
                int total = Integer.parseInt(client.getServerConfigs(String.format("RewardPointsFor%s", UnitUtils.techDescription(
                      type))));
                costLabel.setText(String.format("Hire 1 %s tech for %s %s", UnitUtils.techDescription(type), total, client.getServerConfigs(
                      "RPShortName")));
                techComboBox.setVisible(true);
                techComboLabel.setVisible(true);

                amountText.setVisible(false);
                amountLabel.setVisible(false);
            }
            case repairCommand -> {
                makeVisible(false, false, false);

                costLabel.setText(String.format("Repair Cost: %s", client.getServerConfigs("RewardPointsForRepair")));
                repairComboBox.setVisible(true);
                repairLabel.setVisible(true);

                amountText.setVisible(false);
                amountLabel.setVisible(false);
            }
        }
    }

    private void makeVisible(boolean visible, boolean repod, boolean refresh) {
        unitComboBox.setVisible(visible);
        weightComboBox.setVisible(visible);
        factionComboBox.setVisible(visible);
        unitLabel.setVisible(visible);
        weightLabel.setVisible(visible);
        factionLabel.setVisible(visible);

        rePodComboBox.setVisible(repod);
        repodLabel.setVisible(repod);
        pUnitsComboBox.setVisible(repod);
        pUnitsLabel.setVisible(repod);

        refreshComboBox.setVisible(refresh);
        refreshLabel.setVisible(refresh);

        if (repod || refresh) {
            amountLabel.setVisible(false);
            amountText.setVisible(false);
        } else {
            amountLabel.setVisible(!visible);
            amountText.setVisible(!visible);
        }

        techComboBox.setVisible(false);
        techComboLabel.setVisible(false);
        repairComboBox.setVisible(false);
        repairLabel.setVisible(false);

    }

    private int getUnitRPCost() {

        if (!Boolean.parseBoolean(client.getServerConfigs("AllowUnitsForRewards"))) {return 0;}

        int type = Unit.getTypeIDForName((String) Objects.requireNonNull(unitComboBox.getSelectedItem()));
        int weight = Unit.getWeightIDForName((String) Objects.requireNonNull(weightComboBox.getSelectedItem()));
        String House = (String) factionComboBox.getSelectedItem();
        int cost;


        String configName;
        if (type == Unit.MEK) {
            configName = String.format("%sRP", Unit.getWeightClassDesc(weight));
        } else {
            configName = String.format("%s%sRP", Unit.getWeightClassDesc(weight), Unit.getTypeClassDesc(type));
        }
        cost = Integer.parseInt(client.getServerConfigs(configName));

        if (House != null) {
            if (House.equals("Rare")) {
                cost *= (int) Double.parseDouble(client.getServerConfigs("RewardPointMultiplierForRare"));
            } else if (!House.equals("Common") && !House.equals(client.getPlayer().getHouse())) {
                double multiplier = Double.parseDouble(client.getServerConfigs(String.format("%sTo%sRewardPointMultiplier", client.getPlayer()
                                                                                            .getHouse(), House)));

                if (multiplier < 0) {
                    multiplier = Double.parseDouble(client.getServerConfigs("RewardPointNonHouseMultiplier"));
                }
                cost *= (int) multiplier;
            }
        }
        return cost;
    }

}//end RewardPointsDialog.java
