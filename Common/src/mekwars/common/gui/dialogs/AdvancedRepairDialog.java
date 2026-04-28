/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - Torren (torren@users.sourceforge.net)
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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serial;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import megamek.client.Client;
import megamek.common.CriticalSlot;
import megamek.common.TechConstants;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import mekwars.common.House;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.campaign.pilot.skills.PilotSkill;
import mekwars.common.util.SpringLayoutHelper;
import mekwars.common.util.UnitUtils;

public class AdvancedRepairDialog extends JFrame implements ActionListener, MouseListener, KeyListener, ChangeListener {

    @Serial
    private static final long serialVersionUID = 381067715464633969L;
    private final static String okayCommand = "Add";
    private final static String cancelCommand = "Close";
    private final static String techComboCommand = "TechCombo";
    private final Vector<Integer> techs = new Vector<>(1, 1);
    private final JPanel masterPanel = new JPanel(new SpringLayout());
    private final JPanel techPanel = new JPanel(new SpringLayout());

    // private final static String delimiter = "*";
    // Text boxes
    private final JTextField costField = new JTextField(3);
    private final SpinnerListModel workHoursModel = new SpinnerListModel();
    private final JSpinner workHoursField = new JSpinner();
    private final JTextField baseRollField = new JTextField(3);
    private final SpinnerNumberModel numberOfRetriesEditor = new SpinnerNumberModel();
    private final JSpinner numberOfRetriesField = new JSpinner(numberOfRetriesEditor);
    JTabbedPane configPane = new JTabbedPane(SwingConstants.TOP);
    int year;
    // store the client backlink for other things to use
    private IClient client = null;
    private Entity unit = null;
    private CUnit playerUnit = null;
    // BUTTONS
    private int critLocation = -1;
    private int critSlot = -1;
    private int selectedSlot = -1;
    private boolean armor = false;
    private int tabLocation = 0;
    private int techType = UnitUtils.TECH_GREEN;
    private int baseLineCost = 0;
    private int techWorkMod = 0;
    private int retries = 0;
    private boolean salvage = false;
    private JComboBox<String> techComboBox = new JComboBox<>();

    public AdvancedRepairDialog(IClient client, int unitID, boolean salvage) {
        CUnit pUnit = client.getPlayer().getUnit(unitID);
        Entity unit;

        synchronized (pUnit.getEntity()) {
            unit = pUnit.getEntity();
        }

        new AdvancedRepairDialog(client, pUnit, unit, salvage);
    }

    public AdvancedRepairDialog(IClient client, CUnit playerUnit, Entity unit, boolean salvage) {
        this.playerUnit = playerUnit;
        this.unit = unit;
        this.client = client;
        year = Integer.parseInt(this.client.getServerConfigs("CampaignYear"));
        tabLocation = client.getPlayer().getRepairLocation();
        techs.addAll(client.getPlayer().getAvailableTechs());
        techType = client.getPlayer().getRepairTechType();
        retries = client.getPlayer().getRepairRetries();
        this.salvage = salvage;

        if (!Boolean.parseBoolean(this.client.getServerConfigs("UsePartsRepair"))) {
            this.salvage = false;
        }

        String windowName;
        JButton okayButton = new JButton("Repair");

        if (this.salvage) {
            windowName = STR."\{unit.getShortNameRaw()} Salvage Dialog";
            okayButton.setText("Salvage");
        } else {
            windowName = STR."\{unit.getShortNameRaw()} Repair Dialog";
        }

        addKeyListener(this);

        // stored values.

        // Set the tooltips and actions for dialogue buttons
        okayButton.setActionCommand(okayCommand);
        JButton cancelButton = new JButton("Close");
        cancelButton.setActionCommand(cancelCommand);

        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);
        okayButton.setToolTipText("Set a tech to repair the selected location.");
        okayButton.setMnemonic('R');
        cancelButton.setMnemonic(KeyEvent.VK_ESCAPE);
        cancelButton.setToolTipText("Close the repair dialog");

        configPane = new JTabbedPane();
        configPane.addMouseListener(this);

        // CREATE THE PANELS
        loadPanel();

        loadTechPanel();

        // Set the user's options
        Object[] options = { okayButton, cancelButton };

        // Create the pane containing the buttons
        // STOCK DIALOG AND PANE
        // private JDialog dialog;
        JOptionPane pane = new JOptionPane(masterPanel,
              JOptionPane.PLAIN_MESSAGE,
              JOptionPane.DEFAULT_OPTION,
              null,
              options,
              null);

        setIconImage(this.client.getConfig().getImage("REPAIR").getImage());

        setExtendedState(java.awt.Frame.NORMAL);
        setTitle(windowName);

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new java.awt.BorderLayout());
        contentPane.add(pane, java.awt.BorderLayout.CENTER);
        setResizable(true);
        this.setSize(new java.awt.Dimension(268, 628));
        setExtendedState(java.awt.Frame.NORMAL);

        this.repaint();
        setLocationRelativeTo(this.client.getMainFrame());

        pack();
        setVisible(true);
    }

    private void loadPanel() {
        configPane.removeAll();
        JPanel mainPanel = new JPanel();
        JPanel armorPanel = new JPanel();

        mainPanel.addMouseListener(this);
        armorPanel.addMouseListener(this);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        armorPanel.setLayout(new BoxLayout(armorPanel, BoxLayout.Y_AXIS));

        JPanel headPanel = new JPanel();
        JPanel torsoPanel = new JPanel();
        JPanel legPanel = new JPanel();

        JPanel laPanel = new JPanel();
        JPanel raPanel = new JPanel();
        JPanel llPanel = new JPanel();
        JPanel rlPanel = new JPanel();
        JPanel ltPanel = new JPanel();
        JPanel rtPanel = new JPanel();
        JPanel ctPanel = new JPanel();

        JPanel headArmorPanel = new JPanel();
        JPanel laArmorPanel = new JPanel();
        JPanel raArmorPanel = new JPanel();
        JPanel llArmorPanel = new JPanel();
        JPanel rlArmorPanel = new JPanel();
        JPanel ltArmorPanel = new JPanel();
        JPanel rtArmorPanel = new JPanel();
        JPanel ctArmorPanel = new JPanel();

        headPanel.setLayout(new BoxLayout(headPanel, BoxLayout.X_AXIS));
        headArmorPanel.setLayout(new BoxLayout(headArmorPanel, BoxLayout.X_AXIS));
        torsoPanel.setLayout(new BoxLayout(torsoPanel, BoxLayout.X_AXIS));
        legPanel.setLayout(new BoxLayout(legPanel, BoxLayout.X_AXIS));

        synchronized (unit) {
            String isName = EquipmentType.getStructureTypeName(unit.getStructureType());

            if (isName.equalsIgnoreCase("Standard")) {
                isName = "Internal";
            }
            for (int location = 0; location < unit.locations(); location++) {
                javax.swing.JPanel locationPanel = new javax.swing.JPanel();
                locationPanel.addMouseListener(this);
                Vector<String> critNames = new Vector<>(1, 1);
                Vector<String> armorNames = new Vector<>(3, 1);
                boolean armorDamage = false;
                boolean critDamage = false;
                String armorName = EquipmentType.getArmorTypeName(unit.getArmorType(location));
                if (armorName.equalsIgnoreCase("Standard")) {
                    armorName = "Armor";
                }

                if (unit.getArmor(location) > unit.getOArmor(location)) {
                    UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, location);
                    armorNames.add(STR."!!\{armorName}: \{unit.getArmor(location)}/\{unit.getOArmor(location)}");
                    UnitUtils.setArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, location);
                } else if (client.getRMT().isQueued(location, UnitUtils.LOC_FRONT_ARMOR, unit.getExternalId())) {
                    armorNames.add(STR."@@\{armorName}: \{Math.max(0, unit.getArmor(location))}/\{unit.getOArmor(
                          location)}");
                } else {
                    armorNames.add(STR."\{armorName}: \{Math.max(0,
                          unit.getArmor(location))}/\{unit.getOArmor(location)}");
                }

                if (unit.getArmor(location) != unit.getOArmor(location)) {
                    armorDamage = true;
                }
                if (unit.hasRearArmor(location)) {
                    if (unit.getArmor(location, true) > unit.getOArmor(location, true)) {
                        UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_REAR_ARMOR, location);
                        armorNames.add(STR."!!\{armorName}(r): \{unit.getArmor(location, true)}/\{unit.getOArmor(
                              location,
                              true)}");
                        UnitUtils.setArmorRepair(unit, UnitUtils.LOC_REAR_ARMOR, location);
                    } else if (client.getRMT().isQueued(location, UnitUtils.LOC_REAR_ARMOR, unit.getExternalId())) {
                        armorNames.add(STR."@@\{armorName}(r): \{Math.max(0,
                              unit.getArmor(location, true))}/\{unit.getOArmor(location, true)}");
                    } else {
                        armorNames.add(STR."\{armorName}(r): \{Math.max(0,
                              unit.getArmor(location, true))}/\{unit.getOArmor(location, true)}");
                    }
                    if (unit.getArmor(location, true) != unit.getOArmor(location, true)) {
                        armorDamage = true;
                    }
                }

                if (unit.getInternal(location) > unit.getOInternal(location)) {
                    UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_INTERNAL_ARMOR, location);
                    armorNames.add(STR."!!\{isName}: \{unit.getInternal(location)}/\{unit.getOInternal(location)}");
                    UnitUtils.setArmorRepair(unit, UnitUtils.LOC_INTERNAL_ARMOR, location);
                } else if (client.getRMT().isQueued(location, UnitUtils.LOC_INTERNAL_ARMOR, unit.getExternalId())) {
                    armorNames.add(STR."@@\{isName}: \{Math.max(0, unit.getInternal(location))}/\{unit.getOInternal(
                          location)}");
                } else {
                    armorNames.add(STR."\{isName}: \{Math.max(0, unit.getInternal(location))}/\{unit.getOInternal(
                          location)}");
                }

                if (unit.getInternal(location) != unit.getOInternal(location)) {
                    armorDamage = true;
                }

                javax.swing.JList<String> ArmorSlotList = new javax.swing.JList<>(armorNames);
                ArmorSlotList.addMouseListener(this);
                ArmorSlotList.addKeyListener(this);
                ArmorSlotList.setVisibleRowCount(armorNames.size());
                ArmorSlotList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                ArmorSlotList.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10));
                ArmorSlotList.setName(STR."armor\{location}");
                switch (location) {
                    case Mek.LOC_HEAD:
                        headArmorPanel.add(ArmorSlotList);
                        headArmorPanel.addMouseListener(this);
                        if (critDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            ArmorSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                    case Mek.LOC_LEFT_ARM:
                        laArmorPanel.add(ArmorSlotList);
                        laArmorPanel.addMouseListener(this);
                        if (critDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            ArmorSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                    case Mek.LOC_RIGHT_ARM:
                        raArmorPanel.add(ArmorSlotList);
                        raArmorPanel.addMouseListener(this);
                        if (critDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            ArmorSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                    case Mek.LOC_CENTER_TORSO:
                        ctArmorPanel.add(ArmorSlotList);
                        ctArmorPanel.addMouseListener(this);
                        if (critDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            ArmorSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                    case Mek.LOC_LEFT_TORSO:
                        ltArmorPanel.add(ArmorSlotList);
                        ltArmorPanel.addMouseListener(this);
                        if (critDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            ArmorSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                    case Mek.LOC_RIGHT_TORSO:
                        rtArmorPanel.add(ArmorSlotList);
                        rtArmorPanel.addMouseListener(this);
                        if (critDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            ArmorSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                    case Mek.LOC_LEFT_LEG:
                        llArmorPanel.add(ArmorSlotList);
                        llArmorPanel.addMouseListener(this);
                        if (critDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            ArmorSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                    case Mek.LOC_RIGHT_LEG:
                        rlArmorPanel.add(ArmorSlotList);
                        rlArmorPanel.addMouseListener(this);
                        if (critDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            ArmorSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            ArmorSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                }

                for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
                    CriticalSlot cs = unit.getCritical(location, slot);
                    if (cs == null) {
                        if (!(unit instanceof Tank) && (location == Mek.LOC_HEAD)) {
                            critNames.add("-- Empty --");
                        }
                    } else if (cs.getType() == CriticalSlot.TYPE_SYSTEM) {
                        String result = "";
                        if (cs.isRepairing()) {
                            result += "!!";
                        } else if (client.getRMT().isQueued(location, slot, unit.getExternalId())) {
                            result += "@@";
                            critDamage = true;
                        } else if (cs.isMissing()) {
                            result += "# ";
                            critDamage = true;
                        } else if (cs.isDamaged()) {
                            result += "* ";
                            critDamage = true;
                        } else if (cs.isBreached()) {
                            result += "x ";
                            critDamage = true;
                        }
                        if (unit instanceof Mek) {
                            critNames.add(result + ((Mek) unit).getSystemName(cs.getIndex()));
                        }
                    } else if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                        Mounted<?> m = cs.getMount();
                        if (cs.isRepairing()) {
                            critNames.add(STR."!!\{m.getDesc()}");
                        } else if (client.getRMT().isQueued(location, slot, unit.getExternalId())) {
                            critNames.add(STR."@@\{m.getDesc()}");
                            critDamage = true;
                        } else if (cs.isMissing()) {
                            critNames.add(STR."# \{m.getDesc()}");
                            critDamage = true;
                        } else if (cs.isDamaged()) {
                            critDamage = true;
                            critNames.add(STR."* \{m.getDesc()}");
                        } else if (cs.isBreached()) {
                            critNames.add(STR."x \{m.getDesc()}");
                            critDamage = true;
                        } else {
                            critNames.add(m.getDesc());
                        }
                    }
                }
                JList<String> CriticalSlotList = new JList<>(critNames);
                CriticalSlotList.addMouseListener(this);
                CriticalSlotList.addKeyListener(this);
                CriticalSlotList.setVisibleRowCount(critNames.size());
                CriticalSlotList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                CriticalSlotList.setFont(new Font("Arial", java.awt.Font.PLAIN, 10));
                CriticalSlotList.setName(Integer.toString(location));
                switch (location) {
                    case Mek.LOC_HEAD:
                        headPanel.add(CriticalSlotList);
                        headPanel.addMouseListener(this);
                        if (critDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            CriticalSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                    case Mek.LOC_LEFT_ARM:
                        laPanel.add(CriticalSlotList);
                        laPanel.addMouseListener(this);
                        if (critDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            CriticalSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                    case Mek.LOC_RIGHT_ARM:
                        raPanel.add(CriticalSlotList);
                        raPanel.addMouseListener(this);
                        if (critDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            CriticalSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                    case Mek.LOC_CENTER_TORSO:
                        ctPanel.add(CriticalSlotList);
                        ctPanel.addMouseListener(this);
                        if (critDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            CriticalSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                    case Mek.LOC_LEFT_TORSO:
                        ltPanel.add(CriticalSlotList);
                        ltPanel.addMouseListener(this);
                        if (critDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            CriticalSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                    case Mek.LOC_RIGHT_TORSO:
                        rtPanel.add(CriticalSlotList);
                        rtPanel.addMouseListener(this);
                        if (critDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            CriticalSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                    case Mek.LOC_LEFT_LEG:
                        llPanel.add(CriticalSlotList);
                        llPanel.addMouseListener(this);
                        if (critDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            CriticalSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                    case Mek.LOC_RIGHT_LEG:
                        rlPanel.add(CriticalSlotList);
                        rlPanel.addMouseListener(this);
                        if (critDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.red);
                        } else if (armorDamage) {
                            CriticalSlotList.setBackground(java.awt.Color.yellow);
                        } else {
                            CriticalSlotList.setBackground(java.awt.Color.green);
                        }
                        break;
                }
            }
        }
        mainPanel.add(headPanel);

        torsoPanel.add(laPanel);
        torsoPanel.add(ltPanel);
        torsoPanel.add(ctPanel);
        torsoPanel.add(rtPanel);
        torsoPanel.add(raPanel);
        mainPanel.add(torsoPanel);

        legPanel.add(llPanel);
        legPanel.add(rlPanel);
        mainPanel.add(legPanel);

        configPane.addTab("Crits", mainPanel);

        torsoPanel = new JPanel();
        legPanel = new JPanel();
        torsoPanel.setLayout(new BoxLayout(torsoPanel, BoxLayout.X_AXIS));
        legPanel.setLayout(new BoxLayout(legPanel, BoxLayout.X_AXIS));

        armorPanel.add(headArmorPanel);

        torsoPanel.add(laArmorPanel);
        torsoPanel.add(ltArmorPanel);
        torsoPanel.add(ctArmorPanel);
        torsoPanel.add(rtArmorPanel);
        torsoPanel.add(raArmorPanel);
        armorPanel.add(torsoPanel);

        legPanel.add(llArmorPanel);
        legPanel.add(rlArmorPanel);
        armorPanel.add(legPanel);

        configPane.addTab("Armor", armorPanel);
        configPane.setSelectedIndex(tabLocation);
    }

    private void loadTechPanel() {

        techPanel.removeAll();

        Vector<String> techString = new Vector<>(4, 1);
        techString.add(STR."Green - \{techs.elementAt(UnitUtils.TECH_GREEN)}");
        techString.add(STR."Reg   - \{techs.elementAt(UnitUtils.TECH_REG)}");
        techString.add(STR."Vet   - \{techs.elementAt(UnitUtils.TECH_VET)}");
        techString.add(STR."Elite - \{techs.elementAt(UnitUtils.TECH_ELITE)}");

        Pilot pilot = playerUnit.getPilot();

        if (pilot.getSkills().has(PilotSkill.AstechSkillID)) {
            techString.add(UnitUtils.techDescription(UnitUtils.TECH_PILOT));
        }

        if (Boolean.parseBoolean(client.getServerConfigs("AllowCritRepairsForRewards")) && !salvage) {
            techString.add(UnitUtils.techDescription(UnitUtils.TECH_REWARD_POINTS));
        }

        techComboBox = new JComboBox<>(techString);
        techComboBox.addActionListener(this);
        techComboBox.setActionCommand(techComboCommand);

        try {
            if (techType >= techComboBox.getMaximumRowCount()) {
                techComboBox.setSelectedIndex(0);
            } else {
                techComboBox.setSelectedIndex(techType);
            }
        } catch (Exception ex) {
        }

        techPanel.add(new JLabel("Techs: ", SwingConstants.TRAILING));
        techPanel.add(techComboBox);

        techPanel.add(new JLabel("Hours: ", SwingConstants.TRAILING));
        workHoursField.setToolTipText("One work hour equals 1 RL second");
        workHoursField.addChangeListener(this);
        workHoursField.addKeyListener(this);
        techPanel.add(workHoursField);

        techPanel.add(new JLabel("Cost/Roll ", SwingConstants.TRAILING));
        costField.setText("0");
        costField.setEditable(false);
        techPanel.add(costField);
        baseRollField.setText("12");
        baseRollField.setEditable(false);
        techPanel.add(baseRollField);

        if (!salvage) {
            techPanel.add(new JLabel("Attempts: ", SwingConstants.TRAILING));
            numberOfRetriesEditor.setMaximum(100);
            numberOfRetriesEditor.setMinimum(-1);
            numberOfRetriesEditor.setStepSize(1);
            try {
                numberOfRetriesEditor.setValue(retries);
                numberOfRetriesField.setValue(retries);
            } catch (Exception ex) {
                numberOfRetriesEditor.setValue(0);
                numberOfRetriesField.setValue(0);
            }
            numberOfRetriesField.setToolTipText(
                  "<html>Number of times the assigned tech will try to finish the repair<br>will stop when repair is successful or you run out of money or tries<br>Set to -1 or infinite retries</html>");
            numberOfRetriesField.addKeyListener(this);
            techPanel.add(numberOfRetriesField);
            SpringLayoutHelper.setupSpringGrid(techPanel, 9);
        } else {
            SpringLayoutHelper.setupSpringGrid(techPanel, 8);
        }

        workHoursField.setEnabled(!salvage);
        numberOfRetriesField.setEnabled(!salvage);

        masterPanel.add(configPane);
        masterPanel.add(techPanel);
        SpringLayoutHelper.setupSpringGrid(masterPanel, 2, 1);
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        switch (command) {
            case okayCommand -> {

                if ((critLocation < 0) || (critSlot < 0)) {
                    JOptionPane.showMessageDialog(null, "Invalid location/Slot please try again");
                    return;
                }

                // sometimes the slot doesn't report as 0 for internal armor.
                if (critLocation > UnitUtils.LOC_LTR) {
                    critSlot = UnitUtils.LOC_INTERNAL_ARMOR;
                }

                techType = techComboBox.getSelectedIndex();

                // Need to make sure that its really a pilot as it could be a Reward repair.
                if (techType == UnitUtils.TECH_PILOT) {
                    techType = UnitUtils.techType((String) Objects.requireNonNull(techComboBox.getSelectedItem()));
                }

                int retries = 0;

                if (!salvage) {
                    retries = Integer.parseInt(numberOfRetriesField.getValue().toString());
                }

                if (critSlot >= UnitUtils.LOC_FRONT_ARMOR) {
                    client.getPlayer().setRepairLocation(1);
                } else {
                    client.getPlayer().setRepairLocation(0);
                }
                client.getPlayer().setRepairRetries(retries);
                client.getPlayer().setRepairTechType(techType);

                if (retries < 0) {
                    retries = 999;
                }

                int numberOfTechs = 1;

                if (techType < UnitUtils.TECH_PILOT) {
                    numberOfTechs = client.getPlayer().getAvailableTechs().get(techType);
                } else if ((techType == UnitUtils.TECH_PILOT) && playerUnit.getPilotIsRepairing()) {
                    numberOfTechs = 0;
                }

                if (salvage) {
                    client.sendChat(STR."/c salvageunit#\{unit.getExternalId()}#\{critLocation}#\{critSlot}#\{armor}#\{techType}#true");
                    super.dispose();
                    return;
                }

                if ((!UnitUtils.checkRepairViability(unit, critLocation, critSlot, armor) || (numberOfTechs <= 0)) &&
                          (techType != UnitUtils.TECH_REWARD_POINTS)) {

                    if (!client.getRMT().isQueued(critLocation, critSlot, unit.getExternalId())) {
                        String workOrder = STR."\{unit.getExternalId()}#\{critLocation}#\{critSlot}#\{baseRollField.getText()}#\{retries}";
                        client.getRMT().addWorkOrder(techType, workOrder);
                        client.systemMessage("Work placed in queue.");
                    } else {
                        client.systemMessage("A work order has already been placed for that job!");
                    }

                    if (armor) {
                        tabLocation = 1;
                    } else {
                        tabLocation = 0;
                    }

                    this.retries = retries;

                    loadPanel();
                    loadTechPanel();
                } else {
                    client.sendChat(STR."/c repairunit#\{unit.getExternalId()}#\{critLocation}#\{critSlot}#\{armor}#\{techType}#\{retries}#\{techWorkMod}#true");
                    super.dispose();
                }
            }
            case cancelCommand -> {
                client.getPlayer().resetRepairs();
                super.dispose();
            }
            case techComboCommand -> {
                techType = techComboBox.getSelectedIndex();
                String techString = (String) techComboBox.getSelectedItem();

                if (techString != null) {
                    if (UnitUtils.techType(techString) == UnitUtils.TECH_PILOT) {
                        Pilot pilot = playerUnit.getPilot();
                        techType = pilot.getSkills().getPilotSkill(PilotSkill.AstechSkillID).getLevel();
                    } else if (UnitUtils.techType(techString) == UnitUtils.TECH_REWARD_POINTS) {
                        techType = UnitUtils.TECH_REWARD_POINTS;
                    }
                }

                setCost();
                setBaseRoll();
                setWorkHours();
            }
        }

    }

    /**
     * This method sets the cost field with the cost of the repair based on the crit and the tech doing the job.
     *
     */
    public void setCost() {

        if ((critLocation < 0) || (selectedSlot < 0)) {
            return;
        }

        int totalCost = 1;
        int techCost = 0;
        int techCostWorkMod = 0;
        double totalCrits = 1;// For Armor
        critSlot = selectedSlot;

        if (techComboBox.getSelectedIndex() < UnitUtils.TECH_PILOT) {
            techCost = Integer.parseInt(client.getServerConfigs(STR."\{UnitUtils.techDescription(techType)}TechRepairCost"));
            techCostWorkMod = techWorkMod;
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseRealRepairCosts"))) {
            armor = critSlot >= UnitUtils.LOC_FRONT_ARMOR;
            double cost = UnitUtils.getPartCost(unit, critLocation, critSlot, armor, year);
            if (Boolean.parseBoolean(client.getServerConfigs("UsePartsRepair"))) {
                cost = 0;
            }

            double costMod = Double.parseDouble(client.getServerConfigs("RealRepairCostMod"));
            // modify the cost
            if (costMod > 0) {
                cost *= costMod;
            }

            cost += (techCost * Math.abs(techCostWorkMod)) + techCost;
            costField.setText(Integer.toString((int) cost));
        }// Use Crit based Repairs!
        else {

            if (critSlot == UnitUtils.LOC_FRONT_ARMOR) {
                armor = true;
                double cost = CUnit.getArmorCost(unit, client, critSlot);
                if (unit.getArmor(critLocation) > unit.getOArmor(critLocation)) {
                    // remove the repairing armor so we can get the real cost.
                    UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, critLocation);
                    cost *= unit.getOArmor(critLocation) - unit.getArmor(critLocation);
                    // Add the repairing armor flag back on.
                    UnitUtils.setArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, critLocation);
                } else {
                    cost *= unit.getOArmor(critLocation) - unit.getArmor(critLocation);
                }

                cost += techCost * Math.abs(techCostWorkMod);
                cost += techCost;
                cost = Math.max(1, cost);
                costField.setText(Integer.toString((int) cost));
                critSlot = UnitUtils.LOC_FRONT_ARMOR;
            } else if (critSlot == UnitUtils.LOC_REAR_ARMOR) {
                armor = true;
                // tell the repair command its using rear external armor
                double cost = CUnit.getArmorCost(unit, client, critSlot);
                if (unit.getArmor(critLocation, true) > unit.getOArmor(critLocation, true)) {
                    // remove the repairing armor so we can get the real cost.
                    UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_REAR_ARMOR, critLocation);
                    cost *= unit.getOArmor(critLocation, true) - unit.getArmor(critLocation, true);
                    // Add the repairing armor flag back on.
                    UnitUtils.setArmorRepair(unit, UnitUtils.LOC_REAR_ARMOR, critLocation);
                } else {
                    cost *= unit.getOArmor(critLocation, true) - unit.getArmor(critLocation, true);
                }

                cost += techCost * Math.abs(techCostWorkMod);
                cost += techCost;
                cost = Math.max(1, cost);
                costField.setText(Integer.toString((int) cost));
                critSlot = UnitUtils.LOC_REAR_ARMOR;
            } else if (critSlot == UnitUtils.LOC_INTERNAL_ARMOR) {
                armor = true;
                double cost = CUnit.getStructureCost(unit, client);
                if (unit.getInternal(critLocation) > unit.getOInternal(critLocation)) {
                    // remove the repairing armor so we can get the real cost.
                    UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_INTERNAL_ARMOR, critLocation);
                    cost *= unit.getOInternal(critLocation) - unit.getInternal(critLocation);
                    // Add the repairing armor flag back on.
                    UnitUtils.setArmorRepair(unit, UnitUtils.LOC_INTERNAL_ARMOR, critLocation);
                } else {
                    cost *= unit.getOInternal(critLocation) - unit.getInternal(critLocation);
                }

                cost += techCost * Math.abs(techCostWorkMod);
                cost += techCost;
                cost = Math.max(1, cost);
                costField.setText(Integer.toString((int) cost));
                critSlot = UnitUtils.LOC_INTERNAL_ARMOR;
            } else {
                armor = false;

                CriticalSlot cs = unit.getCritical(critLocation, critSlot);
                double cost = 1;
                if (salvage) {
                    totalCrits = UnitUtils.getNumberOfCrits(unit, cs) -
                                       UnitUtils.getNumberOfDamagedCrits(unit, critSlot, critLocation, armor);
                } else {
                    totalCrits = UnitUtils.getNumberOfDamagedCrits(unit, critSlot, critLocation, armor);
                }
                cost = CUnit.getCritCost(unit, client, cs);
                totalCost = (int) (totalCrits * cost);
                totalCost += (int) (totalCrits * techCost);
                totalCost += techCost;
                totalCost += techCost * Math.abs(techWorkMod);
                cost = Math.max(1, totalCost);
                costField.setText(Integer.toString((int) cost));

            }// end Else
        }// end real repair cost else

        if (Boolean.parseBoolean(client.getServerConfigs("AllowCritRepairsForRewards")) &&
                  (UnitUtils.techType((String) Objects.requireNonNull(techComboBox.getSelectedItem())) ==
                         UnitUtils.TECH_REWARD_POINTS)) {
            double cost = totalCrits * Double.parseDouble(client.getServerConfigs("RewardPointsForCritRepair"));

            cost = Math.ceil(cost);
            cost = Math.max(cost, 1);

            costField.setText(Integer.toString((int) cost));
        }

    }

    /**
     * this method sets the roll needed to be made to accomplish the repair.
     *
     */
    public void setBaseRoll() {

        if ((critLocation < 0) || (critSlot < 0)) {
            return;
        }
        int roll = UnitUtils.getTechRoll(unit,
              critLocation,
              critSlot,
              techType,
              armor,
              client.getData().getHouseByName(client.getPlayer().getHouse()).getTechLevel(),
              salvage);

        baseRollField.setText(Integer.toString(roll));
    }

    private void setWorkHours() {

        if ((critLocation < 0) || (critSlot < 0)) {
            return;
        }

        int baseLine = Integer.parseInt(client.getServerConfigs("TimeForEachRepairPoint"));

        if (!armor) {
            CriticalSlot cs = unit.getCritical(critLocation, critSlot);
            int totalCrits = UnitUtils.getNumberOfCrits(unit, cs);
            baseLine *= totalCrits;
        }

        if (techType == UnitUtils.TECH_PILOT) {
            techType = UnitUtils.techType((String) Objects.requireNonNull(techComboBox.getSelectedItem()));
        }
        if (techType == UnitUtils.TECH_REWARD_POINTS) {
            baseLine = 1;
        }

        int rolls = UnitUtils.getTechRoll(unit,
              critLocation,
              critSlot,
              techType,
              armor,
              client.getData().getHouseByName(client.getPlayer().getHouse()).getTechLevel(),
              salvage) - 3;
        int maxCost = baseLine;

        baseLineCost = baseLine;
        techWorkMod = 0;

        java.util.Vector<Integer> tempVector = new java.util.Vector<>(1, 1);

        tempVector.add(baseLine / 2);
        tempVector.add(baseLine);

        for (int x = 0; x < rolls; x++) {
            maxCost *= 2;
            tempVector.add(maxCost);
        }

        workHoursModel.setList(tempVector);
        workHoursModel.setValue(baseLine);
        workHoursField.setModel(workHoursModel);
    }

    public void mouseClicked(MouseEvent arg0) {

        if (arg0.getComponent() instanceof JList) {
            JList<String> templist = (JList<String>) arg0.getComponent();
            if (templist.getName().startsWith("armor")) {
                critLocation = Integer.parseInt(templist.getName().substring(5));
                selectedSlot = templist.getSelectedIndex();

                if (selectedSlot == 0) {
                    selectedSlot = UnitUtils.LOC_FRONT_ARMOR;
                } else if (selectedSlot == 2) {
                    selectedSlot = UnitUtils.LOC_INTERNAL_ARMOR;
                } else {
                    if (unit.hasRearArmor(critLocation)) {
                        selectedSlot = UnitUtils.LOC_REAR_ARMOR;
                    } else {
                        selectedSlot = UnitUtils.LOC_INTERNAL_ARMOR;
                    }
                }
            } else {
                selectedSlot = templist.getSelectedIndex();
                critLocation = Integer.parseInt(templist.getName());
            }

            setCost();
            setBaseRoll();
            setWorkHours();
        }// end if JList
    }

    public void mousePressed(MouseEvent arg0) {
        if (arg0.getComponent() instanceof JList) {
            JList<String> templist = (JList<String>) arg0.getComponent();
            if (arg0.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                String component = templist.getSelectedValue();

                if (component != null) {
                    if ((unit instanceof Mek) && (component.contains("Cockpit"))) {
                        JPopupMenu popup = getPopupMenuForCockpit();
                        popup.show(this, arg0.getX() + 50, arg0.getY() + 120);
                    }// end auto eject
                    else if ((component.contains("Ammo")) || (component.contains("Pods"))) {
                        javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
                        Client mmClient = new Client("temp", "None", 0);
                        mmClient.getGame().getOptions().loadOptions();

                        CriticalSlot criticalSlot = unit.getCritical(critLocation, critSlot);
                        Mounted<?> mounted = criticalSlot.getMount();
                        AmmoType ammoType = (AmmoType) unit.getEquipmentType(criticalSlot);

                        Vector<AmmoType> vAllTypes = AmmoType.getMunitionsFor(ammoType.getAmmoType());

                        boolean canDump = mmClient.getGame().getOptions().booleanOption("lobby_ammo_dump");

                        if (vAllTypes == null) {
                            return;
                        }

                        if ((vAllTypes.size() < 2) && !canDump) {
                            return;
                        }

                        for (int x = 0, n = vAllTypes.size(); x < n; x++) {
                            AmmoType atCheck = vAllTypes.elementAt(x);
                            boolean bTechMatch = TechConstants.isLegal(unit.getTechLevel(),
                                  atCheck.getTechLevel(year),
                                  unit.isMixedTech());

                            EnumSet<AmmoType.Munitions> munition = atCheck.getMunitionType();
                            House faction = client.getData().getHouseByName(client.getPlayer().getHouse());

                            // check banned ammo
                            if (client.getData().getServerBannedAmmo().stream().anyMatch(munition::contains) ||
                                      faction.getBannedAmmo().stream().anyMatch(munition::contains)) {
                                continue;
                            }

                            // allow all lvl2 IS units to use level 1 ammo
                            // lvl1 IS units don't need to be allowed to use
                            // lvl1 ammo,
                            // because there is no special lvl1 ammo, therefore
                            // it doesn't
                            // need to show up in this display.
                            if (!bTechMatch &&
                                      (unit.getTechLevel() == TechConstants.T_IS_ADVANCED) &&
                                      (atCheck.getTechLevel(year) <= TechConstants.T_IS_TW_NON_BOX)) {
                                bTechMatch = true;
                            }

                            // if is_eq_limits is unchecked allow L1 units to
                            // use L2 munitions
                            if (!mmClient.getGame().getOptions().booleanOption("is_eq_limits") &&
                                      (unit.getTechLevel() <= TechConstants.T_IS_TW_NON_BOX) &&
                                      (atCheck.getTechLevel(year) == TechConstants.T_IS_ADVANCED)) {
                                bTechMatch = true;
                            }

                            // Possibly allow level 3 ammos, possibly not.
                            if (mmClient.getGame().getOptions().booleanOption("allow_advanced_ammo")) {
                                if (!mmClient.getGame().getOptions().booleanOption("is_eq_limits")) {
                                    if ((unit.getTechLevel() == TechConstants.T_CLAN_EXPERIMENTAL) &&
                                              (atCheck.getTechLevel(year) == TechConstants.T_CLAN_EXPERIMENTAL)) {
                                        bTechMatch = true;
                                    }
                                    if (((unit.getTechLevel() <= TechConstants.T_IS_TW_NON_BOX) ||
                                               (unit.getTechLevel() == TechConstants.T_IS_ADVANCED)) &&
                                              (atCheck.getTechLevel(year) == TechConstants.T_IS_EXPERIMENTAL)) {
                                        bTechMatch = true;
                                    }
                                }
                            } else if ((atCheck.getTechLevel(year) == TechConstants.T_IS_EXPERIMENTAL) ||
                                             (atCheck.getTechLevel(year) == TechConstants.T_CLAN_EXPERIMENTAL)) {
                                bTechMatch = false;
                            }

                            // allow mixed Tech Meks to use both IS and Clan
                            // Ammo
                            if (unit.isMixedTech()) {
                                bTechMatch = true;
                            }

                            // If clan_ignore_eq_limits is unchecked, do NOT allow Clans to use IS-only ammo. N.B.
                            // play bit-shifting games to allow "incendiary" to be combined with other munition types.
                            EnumSet<AmmoType.Munitions> muniType = atCheck.getMunitionType();

                            muniType.add(AmmoType.Munitions.M_INCENDIARY_LRM);
                            if (!mmClient.getGame().getOptions().booleanOption("clan_ignore_eq_limits") &&
                                      unit.isClan() &&
                                      ((muniType.contains(AmmoType.Munitions.M_SEMIGUIDED)) ||
                                             (muniType.contains(AmmoType.Munitions.M_THUNDER_AUGMENTED)) ||
                                             (muniType.contains(AmmoType.Munitions.M_THUNDER_INFERNO)) ||
                                             (muniType.contains(AmmoType.Munitions.M_THUNDER_VIBRABOMB)) ||
                                             (muniType.contains(AmmoType.Munitions.M_THUNDER_ACTIVE)) ||
                                             (muniType.contains(AmmoType.Munitions.M_INFERNO_IV)) ||
                                             (muniType.contains(AmmoType.Munitions.M_VIBRABOMB_IV)))) {
                                bTechMatch = false;
                            }

                            if (!mmClient.getGame().getOptions().booleanOption("minefields") &&
                                      AmmoType.canDeliverMinefield(atCheck)) {
                                continue;
                            }

                            // Only Protos can use Proto-specific ammo
                            if (atCheck.hasFlag(AmmoType.F_PROTOMEK) && !(unit instanceof ProtoMek)) {
                                continue;
                            }

                            // When dealing with machine guns, Protos can only
                            // use proto-specific machine gun ammo
                            if ((unit instanceof ProtoMek) &&
                                      atCheck.hasFlag(AmmoType.F_MG) &&
                                      !atCheck.hasFlag(AmmoType.F_PROTOMEK)) {
                                continue;
                            }

                            // BattleArmor ammo can't be selected ammoType all.
                            // All other ammo types need to match on rack size
                            // and tech.
                            if (bTechMatch &&
                                      (atCheck.getRackSize() == ammoType.getRackSize()) &&
                                      !atCheck.hasFlag(AmmoType.F_BATTLEARMOR) &&
                                      (atCheck.getTonnage(unit) == ammoType.getTonnage(unit))) {
                                double ammoCost = client.getAmmoCost(atCheck.getInternalName());
                                int cost;
                                javax.swing.JMenuItem info = new javax.swing.JMenuItem();
                                if (mounted.getLocation() == Entity.LOC_NONE) {
                                    cost = (int) ammoCost;
                                    info.setText(STR."\{atCheck.getName()} (\{mounted.getUsableShotsLeft()}/1) \{client.moneyOrFluMessage(
                                          true,
                                          true,
                                          cost)}");
                                } else {
                                    int refillShots = ammoType.getShots();

                                    if (mounted.getUsableShotsLeft() == 0) {
                                        refillShots = mounted.getOriginalShots();
                                    }

                                    int shotsLeft = mounted.getUsableShotsLeft();

                                    if (!atCheck.getInternalName().equalsIgnoreCase(ammoType.getInternalName())) {
                                        shotsLeft = 0;
                                    }

                                    // No reason to continue if there are not
                                    // shots to refill.
                                    if (shotsLeft == refillShots) {
                                        cost = 0;
                                    } else {
                                        cost = (int) Math.ceil(ammoCost * refillShots);
                                    }

                                    info.setText(STR."\{atCheck.getName()} (\{mounted.getUsableShotsLeft()}/\{refillShots}) \{client.moneyOrFluMessage(
                                          true,
                                          true,
                                          cost)}");
                                }

                                info.addActionListener(new java.awt.event.ActionListener() {
                                    public void actionPerformed(java.awt.event.ActionEvent e) {
                                        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c setunitammobycrit#\{unit.getExternalId()}#\{critLocation}#\{critSlot}#\{e.getActionCommand()}");
                                    }
                                });
                                info.setActionCommand(STR."\{atCheck.getAmmoType()}#\{atCheck.getInternalName()}#\{atCheck.getRackSize()}");
                                popup.add(info);
                            }
                        }// end for
                        popup.show(this, arg0.getX() + 50, arg0.getY() + 120);
                    }// end component is ammo
                }// end component != null
            }// end if Button3
        }// end if JList
    }

    private @org.jspecify.annotations.NonNull JPopupMenu getPopupMenuForCockpit() {
        JPopupMenu popup = new JPopupMenu();

        if (!((Mek) unit).isAutoEject()) {
            JMenuItem info = new JMenuItem("Enable AutoEject");
            info.addActionListener(e -> {
                client.sendChat(
                      STR."\{IClient.CAMPAIGN_PREFIX}c setautoeject#\{unit.getExternalId()}#true");
                ((Mek) unit).setAutoEject(true);
            });
            popup.add(info);
        } else {
            JMenuItem info = new JMenuItem("Disable AutoEject");
            info.addActionListener(e -> {
                client.sendChat(
                      STR."\{IClient.CAMPAIGN_PREFIX}c setautoeject#\{unit.getExternalId()}#false");
                ((Mek) unit).setAutoEject(false);
            });
            popup.add(info);
        }
        return popup;
    }

    public void mouseReleased(MouseEvent arg0) {

    }

    public void mouseEntered(java.awt.event.MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void keyTyped(java.awt.event.KeyEvent arg0) {
    }

    public void keyPressed(java.awt.event.KeyEvent arg0) {
    }

    public void keyReleased(KeyEvent arg0) {

        if (arg0.getKeyCode() == KeyEvent.VK_ESCAPE) {
            client.getPlayer().resetRepairs();
            super.dispose();
        }
        if (arg0.getComponent().equals(numberOfRetriesField)) {
            if (!numberOfRetriesField.getValue().toString().isEmpty()) {
                try {
                    Integer.parseInt(numberOfRetriesField.getValue().toString());
                } catch (Exception ex) {
                    numberOfRetriesEditor.setValue(0);
                    numberOfRetriesField.setValue(0);
                }
            }
        }

        if (arg0.getComponent().equals(workHoursField)) {
            workHoursField.setValue(workHoursField.getPreviousValue());
        }

        if (arg0.getComponent() instanceof JList) {
            critLocation = configPane.getSelectedIndex();
            JList<String> templist = (JList<String>) arg0.getComponent();
            selectedSlot = templist.getSelectedIndex();
            setCost();
            setBaseRoll();
            setWorkHours();
        }// end if JList
    }

    public void stateChanged(ChangeEvent arg0) {

        int value = Integer.parseInt(workHoursModel.getValue().toString());
        int roll = 0;

        if (value < baseLineCost) {
            if (techType == UnitUtils.TECH_REWARD_POINTS) {
                techWorkMod = 0;
                roll = 1;
                baseRollField.setText(Integer.toString(roll));
            } else if (techType != UnitUtils.TECH_GREEN) {
                techWorkMod = 1;
                roll = UnitUtils.getTechRoll(unit,
                      critLocation,
                      critSlot,
                      techType - 1,
                      armor,
                      client.getData().getHouseByName(client.getPlayer().getHouse()).getTechLevel(),
                      salvage);
                baseRollField.setText(Integer.toString(roll));
            }
            return;
        }

        if (value == baseLineCost) {
            techWorkMod = 0;
        } else {
            techWorkMod = 0;
            while (value > baseLineCost) {
                techWorkMod--;
                value /= 2;
            }
        }

        roll = UnitUtils.getTechRoll(unit,
              critLocation,
              critSlot,
              techType,
              armor,
              client.getData().getHouseByName(client.getPlayer().getHouse()).getTechLevel());

        baseRollField.setText(Integer.toString(roll + techWorkMod));
        setCost();
    }
}// end AdvancedRepairDialog.java
