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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.Serial;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import megamek.common.CriticalSlot;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekwars.common.Unit;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.campaign.pilot.skills.PilotSkill;
import mekwars.common.util.SpringLayoutHelper;
import mekwars.common.util.UnitUtils;

public class BulkRepairDialog extends JFrame implements ActionListener, KeyListener, ChangeListener {

    @Serial
    private static final long serialVersionUID = 2053155152906533410L;
    private final static String okayCommand = "Add";
    private final static String cancelCommand = "Close";
    public static int TYPE_BULK = 0;
    public static int TYPE_SIMPLE = 1;
    public static int TYPE_SALVAGE = 2;
    public static int UNIT_TYPE_SINGLE = 0;
    public static int UNIT_TYPE_ALL = 1;
    // store the client backlink for other things to use
    private final IClient client;
    private final CUnit playerUnit;
    private final int ARMOR = 1;
    private final int INTERNAL = 2;

    // BUTTONS
    private final int WEAPONS = 3;
    private final int EQUIPMENT = 4;
    private final int SYSTEMS = 5;
    private final int ENGINES = 6;
    private final JPanel masterPanel = new JPanel();
    private final JPanel masterBox = new JPanel(new SpringLayout());
    private final JPanel repairBox = new JPanel();
    private final JPanel techBox = new JPanel();
    private final JPanel rollBox = new JPanel();
    private final JPanel costBox = new JPanel();
    private final JPanel timeBox = new JPanel();
    private final JPanel blankPanel1 = new JPanel();
    private final JPanel blankPanel2 = new JPanel();
    private final JPanel totalTextPanel = new JPanel();
    private final JPanel totalPanel = new JPanel();
    private final JPanel timePanel = new JPanel();
    private final int repairType;
    private final int unitRepairType;
    private Entity unit;

    public BulkRepairDialog(IClient client, int unitID, int repairType, int unitRepairType) {
        // save the client
        this.client = client;
        playerUnit = client.getPlayer().getUnit(unitID);
        this.repairType = repairType;
        this.unitRepairType = unitRepairType;

        synchronized (playerUnit.getEntity()) {
            unit = playerUnit.getEntity();
        }

        String windowName = String.format("%s Repair Dialog", unit.getShortNameRaw());

        addKeyListener(this);

        // stored values.

        JButton okayButton = new JButton("Start Repairs");
        if (isSalvage()) {
            okayButton.setText("Start Salvage");
        }
        // Set the tooltips and actions for dialogue buttons
        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);
        okayButton.setToolTipText("Start Bulk Repairs.");
        okayButton.setMnemonic('S');

        JButton cancelButton = new JButton("Close");
        cancelButton.setActionCommand(cancelCommand);
        cancelButton.addActionListener(this);
        cancelButton.setToolTipText("Close the bulk repair dialog");
        cancelButton.setDefaultCapable(true);

        // CREATE THE PANELS
        masterPanel.setLayout(new BoxLayout(masterPanel, BoxLayout.Y_AXIS));

        repairBox.setLayout(new BoxLayout(repairBox, BoxLayout.Y_AXIS));
        costBox.setLayout(new BoxLayout(costBox, BoxLayout.Y_AXIS));
        techBox.setLayout(new BoxLayout(techBox, BoxLayout.Y_AXIS));
        rollBox.setLayout(new BoxLayout(rollBox, BoxLayout.Y_AXIS));
        timeBox.setLayout(new BoxLayout(timeBox, BoxLayout.Y_AXIS));

        loadPanel();

        // Set the user's options
        Object[] options = { okayButton, cancelButton };

        // Create the pane containing the buttons
        // STOCK DIALOG AND PANE
        // private JDialog dialog;
        JOptionPane pane = new JOptionPane(masterPanel,
              JOptionPane.PLAIN_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION,
              null,
              options,
              null);

        setIconImage(this.client.getConfig().getImage("REPAIR").getImage());
        setTitle(windowName);

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(pane, BorderLayout.CENTER);
        setResizable(true);

        if (!isBulk()) {
            this.setSize(new Dimension(440, 287));
        } else {
            this.setSize(new Dimension(369, 287));
        }

        setExtendedState(Frame.NORMAL);
        contentPane.addKeyListener(this);
        pane.addKeyListener(this);
        masterPanel.addKeyListener(this);
        cancelButton.addKeyListener(this);
        masterBox.addKeyListener(this);
        repairBox.addKeyListener(this);
        costBox.addKeyListener(this);
        techBox.addKeyListener(this);
        rollBox.addKeyListener(this);
        timeBox.addKeyListener(this);

        addKeyListener(this);

        this.repaint();
        setLocationRelativeTo(this.client.getMainFrame());

        // this.pack();
        setVisible(true);
        setRepair();
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (command.equals(okayCommand)) {
            if (isSimple()) {
                StringBuilder sb = new StringBuilder();
                for (int type = ARMOR; type <= ENGINES; type++) {
                    sb.append(String.format("#%s", ((JComboBox<?>) techBox.getComponent(type)).getSelectedIndex()));
                    sb.append(String.format("#%s", ((JSpinner) rollBox.getComponent(type)).getValue().toString()));
                }
                if (unitRepairType == mekwars.common.gui.dialogs.BulkRepairDialog.UNIT_TYPE_ALL) {
                    for (CUnit repairUnit : client.getPlayer().getHangar()) {
                        if (((repairUnit.getType() == Unit.MEK) || (repairUnit.getType() == Unit.VEHICLE)) &&
                                  (UnitUtils.hasArmorDamage(unit) ||
                                         UnitUtils.hasCriticalDamage(unit) ||
                                         UnitUtils.hasISDamage(unit))) {
                            client.sendChat(String.format("%sc simplerepair#%s%s", IClient.CAMPAIGN_PREFIX, repairUnit.getId(), sb.toString()));
                        }
                    }
                } else {
                    client.sendChat(String.format("%sc simplerepair#%s%s", IClient.CAMPAIGN_PREFIX, playerUnit.getId(), sb.toString()));
                }
            } else if (isSalvage()) {
                client.getSMT().removeAllWorkOrders(unit.getExternalId());
                checkArmor();
                checkWeapons();
                checkEquipment();
                checkSystems();
                checkEngines();
                checkInternal();
            } else {

                if (unitRepairType == mekwars.common.gui.dialogs.BulkRepairDialog.UNIT_TYPE_ALL) {
                    for (CUnit repairUnit : client.getPlayer().getHangar()) {

                        unit = repairUnit.getEntity();

                        if (((repairUnit.getType() == Unit.MEK) || (repairUnit.getType() == Unit.VEHICLE)) &&
                                  (UnitUtils.hasArmorDamage(unit) ||
                                         UnitUtils.hasCriticalDamage(unit) ||
                                         UnitUtils.hasISDamage(unit))) {

                            client.getRMT().removeAllWorkOrders(unit.getExternalId());

                            checkInternal();
                            checkArmor();
                            checkWeapons();
                            checkEquipment();
                            checkSystems();
                            checkEngines();
                        }
                    }
                } else {
                    client.getRMT().removeAllWorkOrders(unit.getExternalId());

                    checkInternal();
                    checkArmor();
                    checkWeapons();
                    checkEquipment();
                    checkSystems();
                    checkEngines();
                }
            }

            client.getConfig().saveConfig();
            super.dispose();
        } else if (command.equals(cancelCommand)) {
            super.dispose();
        } else {
            try {
                int location = Integer.parseInt(command);
                int techType = ((JComboBox<?>) techBox.getComponent(location)).getSelectedIndex();
                if (techType == UnitUtils.TECH_PILOT) {
                    techType = playerUnit.getPilot().getSkills().getPilotSkill(PilotSkill.AsTechSkillID).getLevel();
                }
                ((JSpinner) rollBox.getComponent(location)).setValue(UnitUtils.techBaseRoll(techType));

                if (((JCheckBox) repairBox.getComponent(location)).isSelected()) {
                    setCost(location);
                } else {
                    ((JLabel) costBox.getComponent(location)).setText("0");
                }
                setTotalCost();
            } catch (Exception ex) {
            }
        }
    }

    private void loadPanel() {
        String[] techList;

        Pilot pilot = playerUnit.getPilot();

        if (pilot.getSkills().has(PilotSkill.AsTechSkillID)) {
            techList = new String[] { "Green", "Reg", "Vet", "Elite", "Pilot" };
        } else {
            techList = new String[] { "Green", "Reg", "Vet", "Elite" };
        }

        repairBox.add(new JLabel("Repair Type"));
        techBox.add(new JLabel("Tech Type", SwingConstants.LEFT));
        rollBox.add(new JLabel("Base Roll", SwingConstants.LEFT));
        costBox.add(new JLabel("Cost", SwingConstants.LEFT));
        if (isSimple()) {
            timeBox.add(new JLabel("Time", SwingConstants.LEFT));
        }

        // Text boxes
        JLabel costField;
        JLabel timeField;
        for (int x = 0; x < 6; x++) {
            JComboBox<String> techComboBox = new JComboBox<>(techList);
            techComboBox.addActionListener(this);
            techComboBox.setActionCommand(Integer.toString(x + 1));
            techComboBox.addKeyListener(this);
            techBox.add(techComboBox);

            SpinnerNumberModel baseRollEditor = new SpinnerNumberModel();
            baseRollEditor.setMaximum(12);
            baseRollEditor.setMinimum(3);
            baseRollEditor.setStepSize(1);
            baseRollEditor.setValue(8);

            JSpinner baseRollField = new JSpinner(baseRollEditor);
            baseRollField.addKeyListener(this);
            baseRollField.addChangeListener(this);
            baseRollField.setName(Integer.toString(x + 1));
            baseRollField.setEnabled(!isSalvage());
            rollBox.add(baseRollField);

            costField = new JLabel("0");
            costField.setAlignmentX(Component.CENTER_ALIGNMENT);
            costField.setPreferredSize(new Dimension(50, 100));
            costField.setToolTipText("This is an estimated cost based on tech type and base roll");
            costBox.add(costField);

            timeField = new JLabel("0", SwingConstants.LEFT);
            timeField.setPreferredSize(new Dimension(50, 100));
            timeField.setToolTipText("This is an estimated time based on tech type and base roll");
            timeBox.add(timeField);
        }

        JCheckBox repairCB = new JCheckBox("Armor");
        repairCB.addKeyListener(this);
        repairCB.addActionListener(this);
        repairCB.setActionCommand(Integer.toString(ARMOR));
        repairCB.setSelected(!isBulk());
        repairCB.setEnabled(!isSimple());
        repairBox.add(repairCB);

        repairCB = new javax.swing.JCheckBox("Structure");
        repairCB.addKeyListener(this);
        repairCB.addActionListener(this);
        repairCB.setActionCommand(Integer.toString(INTERNAL));
        repairCB.setSelected(!isBulk());
        repairCB.setEnabled(!isSimple());
        repairBox.add(repairCB);

        repairCB = new javax.swing.JCheckBox("Weapons");
        repairBox.add(repairCB);
        repairCB.addKeyListener(this);
        repairCB.setActionCommand(Integer.toString(WEAPONS));
        repairCB.addActionListener(this);
        repairCB.setSelected(!isBulk());
        repairCB.setEnabled(!isSimple());

        repairCB = new javax.swing.JCheckBox("Equipment");
        repairBox.add(repairCB);
        repairCB.addKeyListener(this);
        repairCB.addActionListener(this);
        repairCB.setActionCommand(Integer.toString(EQUIPMENT));
        repairCB.setSelected(!isBulk());
        repairCB.setEnabled(!isSimple());

        repairCB = new javax.swing.JCheckBox("Systems");
        repairBox.add(repairCB);
        repairCB.addKeyListener(this);
        repairCB.addActionListener(this);
        repairCB.setActionCommand(Integer.toString(SYSTEMS));
        repairCB.setSelected(!isBulk());
        repairCB.setEnabled(!isSimple());

        repairCB = new javax.swing.JCheckBox("Engines");
        repairCB.addKeyListener(this);
        repairCB.addActionListener(this);
        repairBox.add(repairCB);
        repairCB.setActionCommand(Integer.toString(ENGINES));
        repairCB.setSelected(!isBulk());
        repairCB.setEnabled(!isSimple());

        masterBox.add(repairBox);
        masterBox.add(techBox);
        masterBox.add(rollBox);
        masterBox.add(costBox);

        if (isSimple()) {
            masterBox.add(timeBox);
        }

        blankPanel1.add(new JLabel(" "));
        blankPanel2.add(new JLabel(" "));

        costField = new JLabel("0");
        costField.setToolTipText("This is an estimated cost based on tech type and base roll");
        totalPanel.add(costField);
        totalTextPanel.add(new JLabel("Estimated Total:"));

        masterBox.add(blankPanel1);
        masterBox.add(blankPanel2);
        masterBox.add(totalTextPanel);
        masterBox.add(totalPanel);

        if (isSimple()) {
            timeField = new JLabel("0");
            timePanel.add(timeField);
            masterBox.add(timePanel);
        }

        if (isSimple()) {
            SpringLayoutHelper.setupSpringGrid(masterBox, 5);
        } else {
            SpringLayoutHelper.setupSpringGrid(masterBox, 4);
        }
        masterPanel.add(masterBox);

        if (isSimple()) {
            for (int type = ARMOR; type <= ENGINES; type++) {
                setCost(type);
            }
            setTotalCost();
        }
    }

    public void setRepair() {

        int tech;
        int roll;

        if (isSalvage()) {
            ((JComboBox<?>) techBox.getComponent(ARMOR)).setSelectedIndex(Integer.parseInt(client.getConfigParam(
                  ("SALVAGEARMORTECH"))));
            ((JComboBox<?>) techBox.getComponent(INTERNAL)).setSelectedIndex(Integer.parseInt(client.getConfigParam(
                  ("SALVAGEINTERNALTECH"))));
            ((JComboBox<?>) techBox.getComponent(SYSTEMS)).setSelectedIndex(Integer.parseInt(client.getConfigParam(
                  ("SALVAGESYSTEMSTECH"))));
            ((JComboBox<?>) techBox.getComponent(ENGINES)).setSelectedIndex(Integer.parseInt(client.getConfigParam(
                  ("SALVAGEENGINESTECH"))));
            ((JComboBox<?>) techBox.getComponent(WEAPONS)).setSelectedIndex(Integer.parseInt(client.getConfigParam(
                  ("SALVAGEWEAPONSTECH"))));
            ((JComboBox<?>) techBox.getComponent(EQUIPMENT)).setSelectedIndex(Integer.parseInt(client.getConfigParam(
                  ("SALVAGEEQUIPMENTTECH"))));
            setArmorCost();
            setInternalCost();
            setSystemCost();
            setEngineCost();
            setWeaponCost();
            setEquipmentCost();
            setTotalCost();
            return;
        }

        if (UnitUtils.hasArmorDamage(unit)) {
            ((JCheckBox) repairBox.getComponent(ARMOR)).setSelected(true);
            tech = Integer.parseInt(client.getConfigParam(("REPAIRARMORTECH")));
            roll = Integer.parseInt(client.getConfigParam(("REPAIRARMORROLL")));
            ((JComboBox<?>) techBox.getComponent(ARMOR)).setSelectedIndex(tech);
            ((JSpinner) rollBox.getComponent(ARMOR)).setValue(roll);
            setArmorCost();
        }

        if (UnitUtils.hasISDamage(unit)) {
            ((JCheckBox) repairBox.getComponent(INTERNAL)).setSelected(true);
            tech = Integer.parseInt(client.getConfigParam(("REPAIRINTERNALTECH")));
            roll = Integer.parseInt(client.getConfigParam(("REPAIRINTERNALROLL")));
            ((JComboBox<?>) techBox.getComponent(INTERNAL)).setSelectedIndex(tech);
            ((JSpinner) rollBox.getComponent(INTERNAL)).setValue(roll);
            setInternalCost();
        }

        if (UnitUtils.hasCriticalDamage(unit)) {
            for (int location = 0; location < unit.locations(); location++) {
                for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
                    CriticalSlot cs = unit.getCritical(location, slot);
                    if (cs == null) {
                        continue;
                    }
                    if (!cs.isBreached() && !cs.isDamaged()) {
                        continue;
                    }
                    if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                        Mounted<?> mounted = cs.getMount();

                        if (!mounted.isDestroyed() && !mounted.isMissing()) {
                            continue;
                        }

                        // Only want to set a Tech to work on the Mounted object
                        // that is destroyed don't need to
                        // add multiple techs to a single weapon
                        if (mounted.getType() instanceof WeaponType) {
                            ((JCheckBox) repairBox.getComponent(WEAPONS)).setSelected(true);
                            tech = Integer.parseInt(client.getConfigParam(("REPAIRWEAPONSTECH")));
                            roll = Integer.parseInt(client.getConfigParam(("REPAIRWEAPONSROLL")));
                            ((JComboBox<?>) techBox.getComponent(WEAPONS)).setSelectedIndex(tech);
                            ((JSpinner) rollBox.getComponent(WEAPONS)).setValue(roll);
                            setWeaponCost();
                        } else if (!(mounted.getType() instanceof WeaponType)) {
                            ((JCheckBox) repairBox.getComponent(EQUIPMENT)).setSelected(true);
                            tech = Integer.parseInt(client.getConfigParam(("REPAIREQUIPMENTTECH")));
                            roll = Integer.parseInt(client.getConfigParam(("REPAIREQUIPMENTROLL")));
                            ((JComboBox<?>) techBox.getComponent(EQUIPMENT)).setSelectedIndex(tech);
                            ((JSpinner) rollBox.getComponent(EQUIPMENT)).setValue(roll);
                            setEquipmentCost();
                        }
                    } else if ((cs.getType() == CriticalSlot.TYPE_SYSTEM) && (cs.getIndex() != Mek.SYSTEM_ENGINE)) {
                        ((JCheckBox) repairBox.getComponent(SYSTEMS)).setSelected(true);
                        tech = Integer.parseInt(client.getConfigParam(("REPAIRSYSTEMSTECH")));
                        roll = Integer.parseInt(client.getConfigParam(("REPAIRSYSTEMSROLL")));
                        ((JComboBox<?>) techBox.getComponent(SYSTEMS)).setSelectedIndex(tech);
                        ((JSpinner) rollBox.getComponent(SYSTEMS)).setValue(roll);
                        setSystemCost();
                    } else if (UnitUtils.isEngineCrit(cs)) {
                        ((JCheckBox) repairBox.getComponent(ENGINES)).setSelected(true);
                        tech = Integer.parseInt(client.getConfigParam(("REPAIRENGINESTECH")));
                        roll = Integer.parseInt(client.getConfigParam(("REPAIRENGINESROLL")));
                        ((JComboBox<?>) techBox.getComponent(ENGINES)).setSelectedIndex(tech);
                        ((JSpinner) rollBox.getComponent(ENGINES)).setValue(roll);
                        setEngineCost();
                    }
                }
            }
        }
        setTotalCost();
    }

    /**
     * This method sets the cost field with the cost of the repair based on the crit and the tech doing the job.
     *
     */
    public void setCost(int repairType) {

        if (repairType == ARMOR) {
            setArmorCost();
            return;
        }

        if (repairType == INTERNAL) {
            setInternalCost();
            return;
        }

        if (repairType == SYSTEMS) {
            setSystemCost();
            return;
        }

        if (repairType == ENGINES) {
            setEngineCost();
            return;
        }

        if (repairType == WEAPONS) {
            setWeaponCost();
            return;
        }

        if (repairType == EQUIPMENT) {
            setEquipmentCost();
        }

    }

    public void keyTyped(KeyEvent arg0) {
    }

    public void keyPressed(KeyEvent arg0) {
    }

    public void keyReleased(KeyEvent arg0) {

        if (arg0.getKeyCode() == KeyEvent.VK_ESCAPE) {
            super.dispose();
        }
    }

    public void stateChanged(ChangeEvent arg0) {

        int location = Integer.parseInt(((JSpinner) arg0.getSource()).getName());

        if (((JCheckBox) repairBox.getComponent(location)).isSelected()) {
            setCost(location);
            setTotalCost();
        }
    }

    private void checkArmor() {
        // check to see if the checked the box
        if (!((JCheckBox) repairBox.getComponent(ARMOR)).isSelected()) {
            return;
        }

        int techType = ((JComboBox<?>) techBox.getComponent(ARMOR)).getSelectedIndex();
        String baseRoll = ((JSpinner) rollBox.getComponent(ARMOR)).getValue().toString();

        if (isBulk() && (techType != UnitUtils.TECH_PILOT)) {
            client.getConfig().setParam("REPAIRARMORTECH", Integer.toString(techType));
            client.getConfig().setParam("REPAIRARMORROLL", baseRoll);
        } else if (isSalvage()) {
            client.getConfig().setParam("SALVAGEARMORTECH", Integer.toString(techType));
        }

        for (int location = 0; location < unit.locations(); location++) {
            if (isSalvage()) {
                if (unit.getArmor(location) > 0) {
                    String workOrder = String.format("%s#%s#%s", unit.getExternalId(), location, UnitUtils.LOC_FRONT_ARMOR);
                    client.getSMT().addWorkOrder(techType, workOrder);
                }
                if (unit.hasRearArmor(location) && (unit.getArmor(location, true) > 0)) {
                    String workOrder = String.format("%s#%s#%s", unit.getExternalId(), location, UnitUtils.LOC_REAR_ARMOR);
                    client.getSMT().addWorkOrder(techType, workOrder);
                }
            } else {
                if (unit.getArmor(location) < unit.getOArmor(location)) {
                    String workOrder = String.format("%s#%s#%s#%s#999", unit.getExternalId(), location, UnitUtils.LOC_FRONT_ARMOR, baseRoll);
                    client.getRMT().addWorkOrder(techType, workOrder);
                }
                if (unit.hasRearArmor(location) && (unit.getArmor(location, true) < unit.getOArmor(location, true))) {
                    String workOrder = String.format("%s#%s#%s#%s#999", unit.getExternalId(), location +
                                                                            7, UnitUtils.LOC_REAR_ARMOR, baseRoll);
                    client.getRMT().addWorkOrder(techType, workOrder);
                }
            }

        }

    }

    private void checkInternal() {
        // check to see if the checked the box
        if (!((JCheckBox) repairBox.getComponent(INTERNAL)).isSelected()) {
            return;
        }

        int techType = ((JComboBox<?>) techBox.getComponent(INTERNAL)).getSelectedIndex();
        String baseRoll = ((JSpinner) rollBox.getComponent(INTERNAL)).getValue().toString();

        if (isBulk() && (techType != UnitUtils.TECH_PILOT)) {
            client.getConfig().setParam("REPAIRINTERNALTECH", Integer.toString(techType));
            client.getConfig().setParam("REPAIRINTERNALROLL", baseRoll);
        } else if (isSalvage()) {
            client.getConfig().setParam("SALVAGEINTERNALTECH", Integer.toString(techType));
        }

        for (int location = 0; location < unit.locations(); location++) {
            if (isSalvage()) {
                if (unit.getInternal(location) > 0) {
                    String workOrder = String.format("%s#%s#%s#", unit.getExternalId(), location, UnitUtils.LOC_INTERNAL_ARMOR);
                    client.getSMT().addWorkOrder(techType, workOrder);
                }
            } else if (unit.getInternal(location) < unit.getOInternal(location)) {
                String workOrder = String.format("%s#%s#%s#%s#999", unit.getExternalId(), location, UnitUtils.LOC_INTERNAL_ARMOR, baseRoll);
                client.getRMT().addWorkOrder(techType, workOrder);
            }
        }
    }

    private void checkEngines() {
        // check to see if the checked the box
        if (!((JCheckBox) repairBox.getComponent(ENGINES)).isSelected()) {
            return;
        }

        // No damaged engines no reason to keep going.
        if (!isSalvage() && (UnitUtils.getNumberOfDamagedEngineCrits(unit) < 1)) {
            return;
        }

        int techType = ((JComboBox<?>) techBox.getComponent(ENGINES)).getSelectedIndex();
        String baseRoll = ((JSpinner) rollBox.getComponent(ENGINES)).getValue().toString();

        if (isBulk() && (techType != UnitUtils.TECH_PILOT)) {
            client.getConfig().setParam("REPAIRENGINESTECH", Integer.toString(techType));
            client.getConfig().setParam("REPAIRENGINESROLL", baseRoll);
        } else if (isSalvage()) {
            client.getConfig().setParam("SALVAGEENGINESTECH", Integer.toString(techType));
        }
        for (int location = UnitUtils.LOC_CENTER_TORSO; location <= UnitUtils.LOC_LT; location++) {
            for (int slot = 0; slot < unit.locations(); slot++) {
                CriticalSlot cs = unit.getCritical(location, slot);
                // make sure it a viable slot
                if (cs == null) {
                    continue;
                }
                // check for engine slot
                if (!UnitUtils.isEngineCrit(cs)) {
                    continue;
                }
                // check its damaged
                if (isSalvage()) {
                    if (!cs.isDamaged()) {
                        String workOrder = String.format("%s#%s#%s", unit.getExternalId(), location, slot);
                        client.getSMT().addWorkOrder(techType, workOrder);
                        return;
                    }
                } else {
                    if (!cs.isDamaged() && !cs.isBreached()) {
                        continue;
                    }

                    // ok we have a damaged engine slot lets queue up the repair
                    // and exit.
                    String workOrder = String.format("%s#%s#%s#%s#999", unit.getExternalId(), location, slot, baseRoll);
                    client.getRMT().addWorkOrder(techType, workOrder);
                    return;
                }
            }
        }
    }

    private void checkSystems() {
        // check to see if the checked the box
        if (!((JCheckBox) repairBox.getComponent(SYSTEMS)).isSelected()) {
            return;
        }

        int techType = ((JComboBox<?>) techBox.getComponent(SYSTEMS)).getSelectedIndex();
        String baseRoll = ((JSpinner) rollBox.getComponent(SYSTEMS)).getValue().toString();
        if (!isSalvage() && (techType != UnitUtils.TECH_PILOT)) {
            client.getConfig().setParam("REPAIRSYSTEMSTECH", Integer.toString(techType));
            client.getConfig().setParam("REPAIRSYSTEMSROLL", baseRoll);
        } else if (isSalvage()) {
            client.getConfig().setParam("SALVAGESYSTEMSTECH", Integer.toString(techType));
        }

        for (int location = 0; location < unit.locations(); location++) {
            for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot criticalSlot = unit.getCritical(location, slot);
                if (criticalSlot == null) {
                    continue;
                }
                if (UnitUtils.isNonRepairableCrit(unit, criticalSlot)) {
                    continue;
                }
                if (isSalvage()) {
                    if (!criticalSlot.isDamaged() &&
                              (criticalSlot.getType() == CriticalSlot.TYPE_SYSTEM) &&
                              (criticalSlot.getIndex() != Mek.SYSTEM_ENGINE)) {
                        String workOrder = String.format("%s#%s#%s", unit.getExternalId(), location, slot);
                        client.getSMT().addWorkOrder(techType, workOrder);
                        slot += UnitUtils.getNumberOfCrits(unit, criticalSlot) - 1;
                    }
                } else {
                    if (!criticalSlot.isBreached() && !criticalSlot.isDamaged()) {
                        continue;
                    }
                    if ((criticalSlot.getType() == CriticalSlot.TYPE_SYSTEM) &&
                              (criticalSlot.getIndex() != Mek.SYSTEM_ENGINE)) {
                        String workOrder = String.format("%s#%s#%s#%s#999", unit.getExternalId(), location, slot, baseRoll);
                        client.getRMT().addWorkOrder(techType, workOrder);
                        slot += UnitUtils.getNumberOfCrits(unit, criticalSlot) - 1;
                    }
                }
            }
        }
    }

    private void checkWeapons() {
        // check to see if the checked the box
        if (!((JCheckBox) repairBox.getComponent(WEAPONS)).isSelected()) {
            return;
        }

        int techType = ((JComboBox<?>) techBox.getComponent(WEAPONS)).getSelectedIndex();
        String baseRoll = ((JSpinner) rollBox.getComponent(WEAPONS)).getValue().toString();

        if (!isSalvage() && (techType != UnitUtils.TECH_PILOT)) {
            client.getConfig().setParam("REPAIRWEAPONSTECH", Integer.toString(techType));
            client.getConfig().setParam("REPAIRWEAPONSROLL", baseRoll);
        } else if (isSalvage()) {
            client.getConfig().setParam("SALVAGEWEAPONSTECH", Integer.toString(techType));
        }

        for (int location = 0; location < unit.locations(); location++) {
            Mounted<?> lastWeapon = null;
            for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot criticalSlot = unit.getCritical(location, slot);
                if (criticalSlot == null) {
                    continue;
                }
                if (UnitUtils.isNonRepairableCrit(unit, criticalSlot)) {
                    continue;
                }

                if (!isSalvage() && !criticalSlot.isBreached() && !criticalSlot.isDamaged()) {
                    continue;
                }

                if (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted<?> mounted = criticalSlot.getMount();
                    // Only want to set a Tech to work on the Mounted object
                    // that is destroyed don't need to
                    // add multiple techs to a single weapon
                    if (isSalvage()) {
                        if ((mounted.getType() instanceof WeaponType) &&
                                  !mounted.isDestroyed() &&
                                  !mounted.isMissing() &&
                                  !mounted.equals(lastWeapon)) {
                            lastWeapon = mounted;
                            String workOrder = String.format("%s#%s#%s", unit.getExternalId(), location, slot);
                            client.getSMT().addWorkOrder(techType, workOrder);
                        }
                    } else if ((mounted.getType() instanceof WeaponType) &&
                                     (mounted.isDestroyed() || mounted.isMissing()) &&
                                     !mounted.equals(lastWeapon)) {
                        lastWeapon = mounted;
                        String workOrder = String.format("%s#%s#%s#%s#999", unit.getExternalId(), location, slot, baseRoll);
                        client.getRMT().addWorkOrder(techType, workOrder);
                    }
                }
            }
        }
    }

    private void checkEquipment() {
        // check to see if the checked the box
        if (!((JCheckBox) repairBox.getComponent(EQUIPMENT)).isSelected()) {
            return;
        }

        int techType = ((JComboBox<?>) techBox.getComponent(EQUIPMENT)).getSelectedIndex();
        String baseRoll = ((JSpinner) rollBox.getComponent(EQUIPMENT)).getValue().toString();

        if (isBulk() && (techType != UnitUtils.TECH_PILOT)) {
            client.getConfig().setParam("REPAIREQUIPMENTTECH", Integer.toString(techType));
            client.getConfig().setParam("REPAIREQUIPMENTROLL", baseRoll);
        } else if (isSalvage()) {
            client.getConfig().setParam("SALVAGEEQUIPMENTTECH", Integer.toString(techType));
        }

        for (int location = 0; location < unit.locations(); location++) {
            Mounted<?> lastEq = null;
            for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot criticalSlot = unit.getCritical(location, slot);
                if (criticalSlot == null) {
                    continue;
                }
                if (UnitUtils.isNonRepairableCrit(unit, criticalSlot)) {
                    continue;
                }

                if (!isSalvage() && !criticalSlot.isBreached() && !criticalSlot.isDamaged()) {
                    continue;
                }

                if (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted<?> mounted = criticalSlot.getMount();

                    // Only want to set a Tech to work on the Mounted object
                    // that is destroyed don't need to
                    // add multiple techs to a single piece of equipment
                    if (isSalvage()) {
                        if (!(mounted.getType() instanceof WeaponType) &&
                                  !mounted.isDestroyed() &&
                                  !mounted.isMissing() &&
                                  !mounted.equals(lastEq)) {
                            lastEq = mounted;
                            String workOrder = unit.getExternalId() + "#" + location + "#" + slot;
                            client.getSMT().addWorkOrder(techType, workOrder);
                        }
                    } else if (!(mounted.getType() instanceof WeaponType) &&
                                     (mounted.isDestroyed() || mounted.isMissing()) &&
                                     !mounted.equals(lastEq)) {
                        lastEq = mounted;

                        String workOrder = unit.getExternalId() + "#" + location + "#" + slot + "#" + baseRoll + "#999";
                        client.getRMT().addWorkOrder(techType, workOrder);
                    }
                }
            }
        }
    }

    private void setArmorCost() {

        int techType = ((javax.swing.JComboBox<?>) techBox.getComponent(ARMOR)).getSelectedIndex();
        int baseRoll = Integer.parseInt(((javax.swing.JSpinner) rollBox.getComponent(ARMOR)).getValue().toString());
        double pointsToRepair = 0;
        double armorCost = 0.0;
        double techCost = 0;
        double techWorkMod = 0;
        double cost = 0;
        boolean clear = true;

        if (unitRepairType == mekwars.common.gui.dialogs.BulkRepairDialog.UNIT_TYPE_ALL) {
            ((javax.swing.JLabel) costBox.getComponent(ARMOR)).setText("?????");
            return;
        }

        if (techType != UnitUtils.TECH_PILOT) {
            techCost = Integer.parseInt(client.getServerConfigs(String.format("%sTechRepairCost", UnitUtils.techDescription(techType))));
            techWorkMod = UnitUtils.getTechRoll(unit,
                  0,
                  UnitUtils.LOC_FRONT_ARMOR,
                  techType,
                  true,
                  client.getData().getHouseByName(client.getPlayer().getHouse()).getTechLevel()) - baseRoll;
        } else {
            techType = playerUnit.getPilot().getSkills().getPilotSkill(PilotSkill.AsTechSkillID).getLevel();
        }

        techWorkMod = Math.max(techWorkMod, 0);

        for (int location = 0; location < unit.locations(); location++) {

            if (isSalvage()) {
                if (unit.getArmor(location) > 0) {
                    cost += techCost;
                    setWorkHours(ARMOR, location, 0, true, clear);
                    clear = false;
                }

                if (unit.hasRearArmor(location) && (unit.getArmor(location, true) > 0)) {
                    cost += techCost;
                    setWorkHours(ARMOR, location, 0, true, clear);
                    clear = false;
                }
            } else {
                if (unit.getArmor(location) < unit.getOArmor(location)) {
                    pointsToRepair += unit.getOArmor(location) - unit.getArmor(location);
                    cost += armorCost * pointsToRepair;
                    cost += techCost * Math.abs(techWorkMod);
                    cost += techCost;
                    setWorkHours(ARMOR, location, 0, true, clear);
                    clear = false;
                }

                if (unit.hasRearArmor(location)) {
                    pointsToRepair += unit.getOArmor(location, true) - unit.getArmor(location, true);
                    armorCost = CUnit.getArmorCost(unit, client, location);
                    cost += armorCost * pointsToRepair;
                    cost += techCost * Math.abs(techWorkMod);
                    cost += techCost;
                    setWorkHours(ARMOR, location, 0, true, clear);
                    clear = false;
                }
            }
        }

        // Base on what they assigned as the base roll we increase the payout so
        // that it covers the chances of failures. not the greatest but better
        // then nothing.
        if (!isSalvage()) {
            cost *= payOutIncreaseBasedOnRoll(baseRoll);
        }
        cost = Math.max(0, cost);

        ((javax.swing.JLabel) costBox.getComponent(ARMOR)).setText(Integer.toString((int) cost));
    }

    private void setInternalCost() {

        int techType = ((javax.swing.JComboBox<?>) techBox.getComponent(INTERNAL)).getSelectedIndex();
        int baseRoll = Integer.parseInt(((javax.swing.JSpinner) rollBox.getComponent(INTERNAL)).getValue().toString());
        double pointsToRepair;
        double armorCost = CUnit.getStructureCost(unit, client);
        double techCost = 0;
        double techWorkMod = 0;
        double cost = 0;
        boolean clear = true;

        if (unitRepairType == mekwars.common.gui.dialogs.BulkRepairDialog.UNIT_TYPE_ALL) {
            ((javax.swing.JLabel) costBox.getComponent(INTERNAL)).setText("?????");
            return;
        }

        if (techType != UnitUtils.TECH_PILOT) {
            techCost = Integer.parseInt(client.getServerConfigs(UnitUtils.techDescription(techType) +
                                                                      "TechRepairCost"));
        }

        for (int location = 0; location < unit.locations(); location++) {

            if (isSalvage() && (unit.getInternal(location) > 0)) {
                cost += techCost;
                setWorkHours(INTERNAL, location, UnitUtils.LOC_INTERNAL_ARMOR, true, clear);
                clear = false;
            } else {
                if (unit.getInternal(location) < unit.getOInternal(location)) {
                    if (techType != UnitUtils.TECH_PILOT) {
                        techWorkMod = UnitUtils.getTechRoll(unit,
                              location,
                              UnitUtils.LOC_INTERNAL_ARMOR,
                              techType,
                              true,
                              client.getData().getHouseByName(client.getPlayer().getHouse()).getTechLevel()) -
                                            baseRoll;
                    }

                    techWorkMod = Math.max(techWorkMod, 0);
                    pointsToRepair = unit.getOInternal(location) - unit.getInternal(location);
                    cost += armorCost * pointsToRepair;
                    cost += techCost * Math.abs(techWorkMod);
                    cost += techCost;
                    setWorkHours(INTERNAL, location, UnitUtils.LOC_INTERNAL_ARMOR, true, clear);
                    clear = false;

                }
            }
        }

        // Base on what they assigned as the base roll we increase the payout so
        // that it covers the chances of failures. not the greatest but better
        // then nothing.
        if (!isSalvage()) {
            cost *= payOutIncreaseBasedOnRoll(baseRoll);
        }
        cost = Math.max(0, cost);

        ((javax.swing.JLabel) costBox.getComponent(INTERNAL)).setText(Integer.toString((int) cost));
    }

    private void setSystemCost() {

        int techType = ((JComboBox<?>) techBox.getComponent(SYSTEMS)).getSelectedIndex();
        int baseRoll = Integer.parseInt(((javax.swing.JSpinner) rollBox.getComponent(SYSTEMS)).getValue().toString());
        double pointsToRepair;
        double critCost;
        double techCost = 0;
        double techWorkMod = 0;
        double cost = 0;
        boolean clear = true;

        if (unitRepairType == mekwars.common.gui.dialogs.BulkRepairDialog.UNIT_TYPE_ALL) {
            ((javax.swing.JLabel) costBox.getComponent(SYSTEMS)).setText("?????");
            return;
        }

        if (techType != UnitUtils.TECH_PILOT) {
            techCost = Integer.parseInt(client.getServerConfigs(UnitUtils.techDescription(techType) +
                                                                      "TechRepairCost"));
        }

        for (int location = 0; location < unit.locations(); location++) {
            for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot cs = unit.getCritical(location, slot);
                if (cs == null) {
                    continue;
                }
                if (UnitUtils.isNonRepairableCrit(unit, cs)) {
                    continue;
                }
                if (isSalvage()) {
                    if (!cs.isDamaged() &&
                              (cs.getType() == CriticalSlot.TYPE_SYSTEM) &&
                              (cs.getIndex() != Mek.SYSTEM_ENGINE)) {
                        int crits = UnitUtils.getNumberOfCrits(unit, cs) -
                                          UnitUtils.getNumberOfDamagedCrits(unit, slot, location, false);
                        cost += techCost * crits;
                        cost += techCost;
                        slot += UnitUtils.getNumberOfCrits(unit, cs) - 1;
                        clear = false;
                    }
                    continue;
                }

                if (!cs.isBreached() && !cs.isDamaged()) {
                    continue;
                }
                if ((cs.getType() == CriticalSlot.TYPE_SYSTEM) && (cs.getIndex() != Mek.SYSTEM_ENGINE)) {
                    if (techType != UnitUtils.TECH_PILOT) {
                        techWorkMod = UnitUtils.getTechRoll(unit,
                              location,
                              slot,
                              techType,
                              true,
                              client.getData().getHouseByName(client.getPlayer().getHouse()).getTechLevel()) -
                                            baseRoll;
                    }

                    critCost = CUnit.getCritCost(unit, client, cs);
                    techWorkMod = Math.max(techWorkMod, 0);
                    pointsToRepair = UnitUtils.getNumberOfCrits(unit, cs);
                    critCost += techCost;
                    cost += critCost * pointsToRepair;
                    cost += techCost * Math.abs(techWorkMod);
                    cost += techCost;
                    setWorkHours(SYSTEMS, location, slot, false, clear);
                    clear = false;

                    // move the slot ahead if the Crit is more then 1 in size.
                    slot += (int) (pointsToRepair - 1);
                }
            }
        }

        // Base on what they assigned as the base roll we increase the payout so
        // that it covers the chances of failures. not the greatest but better
        // then nothing.
        if (!isSalvage()) {
            cost *= payOutIncreaseBasedOnRoll(baseRoll);
        }
        cost = Math.max(0, cost);

        ((javax.swing.JLabel) costBox.getComponent(SYSTEMS)).setText(Integer.toString((int) cost));
    }

    private void setWeaponCost() {

        int techType = ((javax.swing.JComboBox<?>) techBox.getComponent(WEAPONS)).getSelectedIndex();
        int baseRoll = Integer.parseInt(((javax.swing.JSpinner) rollBox.getComponent(WEAPONS)).getValue().toString());
        double pointsToRepair;
        double critCost;
        double techCost = 0;
        double techWorkMod = 0;
        double cost = 0;
        boolean clear = true;


        if (unitRepairType == mekwars.common.gui.dialogs.BulkRepairDialog.UNIT_TYPE_ALL) {
            ((javax.swing.JLabel) costBox.getComponent(WEAPONS)).setText("?????");
            return;
        }

        if (techType != UnitUtils.TECH_PILOT) {
            techCost = Integer.parseInt(client.getServerConfigs(UnitUtils.techDescription(techType) +
                                                                      "TechRepairCost"));
        }

        Mounted<?> lastWeapon = null;
        for (int location = 0; location < unit.locations(); location++) {
            for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot cs = unit.getCritical(location, slot);
                if (cs == null) {
                    continue;
                }
                if (isSalvage()) {
                    if (!cs.isDamaged() && (cs.getType() == CriticalSlot.TYPE_EQUIPMENT)) {
                        Mounted<?> mounted = cs.getMount();

                        if ((mounted.getType() instanceof WeaponType) && !mounted.equals(lastWeapon)) {
                            int crits = UnitUtils.getNumberOfCrits(unit, cs) -
                                              UnitUtils.getNumberOfDamagedCrits(unit, slot, location, false);
                            cost += techCost * crits;
                            if (crits > 0) {
                                cost += techCost;
                            }
                            clear = false;
                            lastWeapon = mounted;
                        }
                    }
                } else {
                    if (!cs.isBreached() && !cs.isDamaged()) {
                        continue;
                    }
                    if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                        Mounted<?> mounted = cs.getMount();

                        if ((mounted.getType() instanceof WeaponType) && !mounted.equals(lastWeapon)) {
                            if (techType != UnitUtils.TECH_PILOT) {
                                techWorkMod = UnitUtils.getTechRoll(unit,
                                      location,
                                      slot,
                                      techType,
                                      true,
                                      client.getData()
                                            .getHouseByName(client.getPlayer().getHouse())
                                            .getTechLevel()) - baseRoll;
                            }

                            critCost = CUnit.getCritCost(unit, client, cs);
                            techWorkMod = Math.max(techWorkMod, 0);
                            pointsToRepair = UnitUtils.getNumberOfCrits(unit, cs);
                            critCost += techCost;
                            cost += critCost * pointsToRepair;
                            cost += techCost * Math.abs(techWorkMod);
                            cost += techCost;
                            setWorkHours(WEAPONS, location, slot, false, clear);
                            lastWeapon = mounted;
                            clear = false;
                        }
                    }
                }
            }
        }

        // Base on what they assigned as the base roll we increase the payout so
        // that it covers the chances of failures. not the greatest but better
        // then nothing.
        if (!isSalvage()) {
            cost *= payOutIncreaseBasedOnRoll(baseRoll);
        }
        cost = Math.max(0, cost);

        ((javax.swing.JLabel) costBox.getComponent(WEAPONS)).setText(Integer.toString((int) cost));
    }

    private void setEquipmentCost() {

        int techType = ((javax.swing.JComboBox<?>) techBox.getComponent(EQUIPMENT)).getSelectedIndex();
        int baseRoll = Integer.parseInt(((javax.swing.JSpinner) rollBox.getComponent(EQUIPMENT)).getValue().toString());
        double pointsToRepair;
        double critCost;
        double techCost = 0;
        double techWorkMod = 0;
        double cost = 0;
        boolean clear = true;
        Mounted<?> lastEq = null;

        if (unitRepairType == mekwars.common.gui.dialogs.BulkRepairDialog.UNIT_TYPE_ALL) {
            ((javax.swing.JLabel) costBox.getComponent(EQUIPMENT)).setText("?????");
            return;
        }

        if (techType != UnitUtils.TECH_PILOT) {
            techCost = Integer.parseInt(client.getServerConfigs(UnitUtils.techDescription(techType) +
                                                                      "TechRepairCost"));
        }

        for (int location = 0; location < unit.locations(); location++) {
            for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot cs = unit.getCritical(location, slot);
                if (cs == null) {
                    continue;
                }
                if (UnitUtils.isNonRepairableCrit(unit, cs)) {
                    continue;
                }
                if (isSalvage()) {
                    if (!cs.isDamaged() && (cs.getType() == CriticalSlot.TYPE_EQUIPMENT)) {
                        Mounted<?> mounted = cs.getMount();
                        if (!(mounted.getType() instanceof WeaponType) && !mounted.equals(lastEq)) {
                            int crits = UnitUtils.getNumberOfCrits(unit, cs) -
                                              UnitUtils.getNumberOfDamagedCrits(unit, slot, location, false);
                            cost += techCost * crits;
                            cost += techCost;
                            clear = false;
                            lastEq = mounted;
                        }
                    }
                    continue;
                }

                if (!cs.isBreached() && !cs.isDamaged()) {
                    continue;
                }
                if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted<?> mounted = cs.getMount();

                    if (!(mounted.getType() instanceof WeaponType) && !mounted.equals(lastEq)) {
                        if (techType != UnitUtils.TECH_PILOT) {
                            techWorkMod = UnitUtils.getTechRoll(unit,
                                  location,
                                  slot,
                                  techType,
                                  true,
                                  client.getData().getHouseByName(client.getPlayer().getHouse()).getTechLevel()) -
                                                baseRoll;
                        }

                        critCost = CUnit.getCritCost(unit, client, cs);
                        techWorkMod = Math.max(techWorkMod, 0);
                        pointsToRepair = UnitUtils.getNumberOfCrits(unit, cs);
                        critCost += techCost;
                        cost += critCost * pointsToRepair;
                        cost += techCost * Math.abs(techWorkMod);
                        cost += techCost;
                        setWorkHours(EQUIPMENT, location, slot, false, clear);
                        clear = false;
                        lastEq = mounted;
                    }
                }
            }
        }

        // Base on what they assigned as the base roll we increase the payout so
        // that it covers the chances of failures. not the greatest but better
        // then nothing.
        if (!isSalvage()) {
            cost *= payOutIncreaseBasedOnRoll(baseRoll);
        }
        cost = Math.max(0, cost);

        ((javax.swing.JLabel) costBox.getComponent(EQUIPMENT)).setText(Integer.toString((int) cost));
    }

    private void setEngineCost() {

        int techType = ((javax.swing.JComboBox<?>) techBox.getComponent(ENGINES)).getSelectedIndex();
        int baseRoll = Integer.parseInt(((javax.swing.JSpinner) rollBox.getComponent(ENGINES)).getValue().toString());
        double pointsToRepair;
        double critCost;
        double techCost = 0;
        double techWorkMod = 0;
        double cost = 0;
        boolean found = false;
        int location = 0, slot = 0;
        CriticalSlot cs = null;

        if (unitRepairType == mekwars.common.gui.dialogs.BulkRepairDialog.UNIT_TYPE_ALL) {
            ((javax.swing.JLabel) costBox.getComponent(ENGINES)).setText("?????");
            return;
        }

        if (techType != UnitUtils.TECH_PILOT) {
            techCost = Integer.parseInt(client.getServerConfigs(UnitUtils.techDescription(techType) +
                                                                      "TechRepairCost"));
        }

        top_loop:
        for (int x = UnitUtils.LOC_CENTER_TORSO; x <= UnitUtils.LOC_LT; x++) {
            for (int y = 0; y < unit.getNumberOfCriticalSlots(x); y++) {
                cs = unit.getCritical(x, y);

                if (cs == null) {
                    continue;
                }

                if (!UnitUtils.isEngineCrit(cs)) {
                    continue;
                }

                if (isSalvage()) {
                    int totalCrits = UnitUtils.getNumberOfEngineCrits(unit) -
                                           UnitUtils.getNumberOfDamagedEngineCrits(unit);
                    int totalCost = (int) (totalCrits * techCost);
                    if (totalCrits > 0) {
                        totalCost += (int) techCost;
                    }
                    cost = Math.max(0, totalCost);
                    ((javax.swing.JLabel) costBox.getComponent(ENGINES)).setText(Integer.toString((int) cost));
                    return;
                }

                if (!cs.isDamaged() && !cs.isBreached()) {
                    continue;
                }

                location = x;
                slot = y;
                found = true;
                break top_loop;

            }
        }

        if (techType != UnitUtils.TECH_PILOT) {
            techWorkMod = UnitUtils.getTechRoll(unit,
                  location,
                  slot,
                  techType,
                  true,
                  client.getData().getHouseByName(client.getPlayer().getHouse()).getTechLevel()) - baseRoll;
        }

        critCost = CUnit.getCritCost(unit, client, cs);
        techWorkMod = Math.max(techWorkMod, 0);
        pointsToRepair = UnitUtils.getNumberOfCrits(unit, cs);
        critCost += techCost;
        cost += critCost * pointsToRepair;
        cost += techCost * Math.abs(techWorkMod);
        cost += techCost;
        // System.err.println("Cost 1: "+cost);

        // Base on what they assigned as the base roll we increase the payout so
        // that it covers the chances of failures. not the greatest but better
        // then nothing.
        cost *= payOutIncreaseBasedOnRoll(baseRoll);
        cost = Math.max(0, cost);
        // System.err.println("Cost 2: "+cost);
        if (!found) {
            cost = 0;
        }
        if (isSimple()) {
            ((javax.swing.JLabel) timeBox.getComponent(ENGINES)).setText("0");
            setWorkHours(ENGINES, UnitUtils.LOC_CENTER_TORSO, 0, false, true);
        }
        ((javax.swing.JLabel) costBox.getComponent(ENGINES)).setText(Integer.toString((int) cost));
    }

    private double payOutIncreaseBasedOnRoll(int roll) {
        if (roll <= 2) {
            return 1.0;
        } else if (roll > 12) {
            return 36.0;
        }
        final double[] payout = { 1.0, 1.0, 1.0, 1.03, 1.09, 1.20, 1.38, 1.72, 2.40, 3.60, 5.92, 12.0, 36.0 };
        return payout[roll];
    }

    private void setWorkHours(int type, int critLocation, int critSlot, boolean armor, boolean clear) {

        int techType = ((javax.swing.JComboBox<?>) techBox.getComponent(type)).getSelectedIndex();
        int baseRoll = Integer.parseInt(((javax.swing.JSpinner) rollBox.getComponent(type)).getValue().toString());

        if ((critLocation < 0) || (critSlot < 0)) {
            return;
        }

        int baseLine = Integer.parseInt(client.getServerConfigs("TimeForEachRepairPoint"));

        if (!armor) {
            CriticalSlot cs = unit.getCritical(critLocation, critSlot);
            int totalCrits = UnitUtils.getNumberOfCrits(unit, cs);
            baseLine *= totalCrits;
        }

        int rolls = UnitUtils.getTechRoll(unit,
              critLocation,
              critSlot,
              techType,
              armor,
              client.getData().getHouseByName(client.getPlayer().getHouse()).getTechLevel()) - baseRoll;

        for (int count = 0; count < rolls; count++) {
            baseLine *= 2;
        }

        baseLine = (int) (baseLine * payOutIncreaseBasedOnRoll(baseRoll));

        javax.swing.JLabel textField = (javax.swing.JLabel) timeBox.getComponent(type);
        if (!clear) {
            baseLine += Integer.parseInt(textField.getText());
        }
        textField.setText(Integer.toString(baseLine));

    }

    private void setTotalCost() {

        int cost = 0;
        int seconds = 0;
        int minutes = 0;
        int hours = 0;

        if (unitRepairType == mekwars.common.gui.dialogs.BulkRepairDialog.UNIT_TYPE_ALL) {
            if (isSimple()) {
                ((javax.swing.JLabel) timePanel.getComponent(0)).setText("?????");
            }
            ((javax.swing.JLabel) totalPanel.getComponent(0)).setText("?????");
            return;
        }

        for (int x = ARMOR; x <= ENGINES; x++) {
            cost += Integer.parseInt(((javax.swing.JLabel) costBox.getComponent(x)).getText());
            if (isSimple()) {
                seconds += Integer.parseInt(((javax.swing.JLabel) timeBox.getComponent(x)).getText());
            }
        }

        if (isSimple()) {
            if (seconds > 3600) {
                hours = seconds / 3600;
                seconds %= 3600;
            }

            if (seconds > 60) {
                minutes = seconds / 60;
                seconds %= 60;
            }

            StringBuilder text = new StringBuilder();
            StringBuilder toolTip = new StringBuilder();

            if (hours > 0) {
                text.append(hours).append("h ");
                toolTip.append(hours).append(" hours ");
            }
            if (minutes > 0) {
                text.append(minutes).append("m ");
                toolTip.append(minutes).append(" minutes ");
            }
            text.append(seconds).append("s");
            toolTip.append(seconds).append(" seconds");
            ((javax.swing.JLabel) timePanel.getComponent(0)).setText(text.toString());
            ((javax.swing.JLabel) timePanel.getComponent(0)).setToolTipText(toolTip.toString());
        }
        ((javax.swing.JLabel) totalPanel.getComponent(0)).setText(Integer.toString(cost));
    }

    private boolean isSimple() {
        return repairType == mekwars.common.gui.dialogs.BulkRepairDialog.TYPE_SIMPLE;
    }

    private boolean isBulk() {
        return repairType == mekwars.common.gui.dialogs.BulkRepairDialog.TYPE_BULK;
    }

    private boolean isSalvage() {
        return repairType == mekwars.common.gui.dialogs.BulkRepairDialog.TYPE_SALVAGE;
    }
}// end BulkRepairDialog.java
