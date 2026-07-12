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

/**
 * Window that lets a player configure and kick off unit repairs (or salvage) in bulk, without
 * having to open the full {@code AdvancedRepairDialog} and assign techs crit-by-crit.
 * <p>
 * Despite extending {@link JFrame} rather than {@link JDialog}, this window functions like a
 * modal dialog: its content is a single {@link JOptionPane} (with "Start Repairs"/"Start
 * Salvage" and "Close" buttons) embedded in the frame's content pane, shown via
 * {@code setVisible(true)} at the end of the constructor.
 * <p>
 * The dialog operates in one of three mutually-exclusive modes, selected by the
 * {@code repairType} constructor argument:
 * <ul>
 *   <li>{@link #TYPE_BULK} - queue up "real" repair work orders (submitted to the client's
 *       Repair Master Table via {@code client.getRMT()}) for all damaged locations/crits on the
 *       unit(s), using per-category tech skill + target roll choices.</li>
 *   <li>{@link #TYPE_SIMPLE} - a lighter-weight variant that doesn't queue individual work
 *       orders; instead it sends a single {@code "simplerepair"} chat/campaign command per unit,
 *       and additionally estimates completion time (not just cost) per category.</li>
 *   <li>{@link #TYPE_SALVAGE} - queue up salvage work orders (via {@code client.getSMT()}, the
 *       Salvage Master Table) to strip/recover parts from a wrecked/destroyed unit rather than
 *       repair it; roll-based cost/time modifiers are not applied since salvage isn't a
 *       pass/fail roll in the same sense as a repair.</li>
 * </ul>
 * Independently of the repair type, {@code unitRepairType} selects whether the settings chosen
 * in this dialog apply to just the single unit passed to the constructor
 * ({@link #UNIT_TYPE_SINGLE}) or to every eligible (Mek/Vehicle, damaged) unit currently in the
 * player's hangar ({@link #UNIT_TYPE_ALL}). In the "all units" case, per-category cost/time
 * estimates cannot be computed in advance (since they depend on each unit's actual damage) and
 * are simply displayed as "?????".
 * <p>
 * For each of six repair categories - Armor, (Internal) Structure, Weapons, Equipment, Systems,
 * and Engines - the dialog shows a row with: a checkbox to include/exclude that category (Bulk
 * mode only; Simple/Salvage always act on everything), a combo box to pick the repairing tech's
 * skill level, a spinner for the target roll needed to succeed, and a computed cost (plus, in
 * Simple mode, an estimated time). A running total across all six categories is shown at the
 * bottom.
 */
public class BulkRepairDialog extends JFrame implements ActionListener, KeyListener, ChangeListener {

    @Serial
    private static final long serialVersionUID = 2053155152906533410L;
    /** Action command for the "Start Repairs"/"Start Salvage" button. */
    private final static String okayCommand = "Add";
    /** Action command for the "Close" button. */
    private final static String cancelCommand = "Close";
    /** Repair mode: queue individual work orders on the Repair Master Table for damaged parts. */
    public static int TYPE_BULK = 0;
    /** Repair mode: send a single "simplerepair" command per unit; also estimates repair time. */
    public static int TYPE_SIMPLE = 1;
    /** Repair mode: queue salvage work orders on the Salvage Master Table to recover parts. */
    public static int TYPE_SALVAGE = 2;
    /** Apply the chosen settings to only the single unit passed into the constructor. */
    public static int UNIT_TYPE_SINGLE = 0;
    /** Apply the chosen settings to every eligible damaged unit in the player's hangar. */
    public static int UNIT_TYPE_ALL = 1;
    // store the client backlink for other things to use
    /** Client connection, used to read/write config, server settings, and submit work orders/chat. */
    private final IClient client;
    /** The CUnit wrapper for the unit this dialog was opened for (used for pilot/AsTech skill lookups). */
    private final CUnit playerUnit;
    /**
     * Category index for Armor. Doubles as the child-component index (via
     * {@code getComponent(ARMOR)}) into each of {@link #repairBox}, {@link #techBox},
     * {@link #rollBox}, {@link #costBox}, and {@link #timeBox} — this only works because
     * {@link #loadPanel()} adds a header label first (index 0) and then adds the six category
     * rows in the same ARMOR..ENGINES order, so the indices below must stay in sync with the
     * add order in {@link #loadPanel()}.
     */
    private final int ARMOR = 1;
    /** Category/component index for (internal) Structure. See {@link #ARMOR} for how indices work. */
    private final int INTERNAL = 2;

    // BUTTONS
    /** Category/component index for Weapons. See {@link #ARMOR} for how indices work. */
    private final int WEAPONS = 3;
    /** Category/component index for Equipment (non-weapon gear). See {@link #ARMOR} for how indices work. */
    private final int EQUIPMENT = 4;
    /** Category/component index for Systems (non-engine system crits). See {@link #ARMOR} for how indices work. */
    private final int SYSTEMS = 5;
    /** Category/component index for Engines. See {@link #ARMOR} for how indices work. */
    private final int ENGINES = 6;
    /** Outermost panel stacking the option grid inside the {@link JOptionPane}. */
    private final JPanel masterPanel = new JPanel();
    /** Grid (SpringLayout) panel arranging the per-category columns side by side. */
    private final JPanel masterBox = new JPanel(new SpringLayout());
    /** Column of "include this category" checkboxes (Bulk mode) plus its header label. */
    private final JPanel repairBox = new JPanel();
    /** Column of tech-skill-level combo boxes, one per category, plus its header label. */
    private final JPanel techBox = new JPanel();
    /** Column of target-roll spinners, one per category, plus its header label. */
    private final JPanel rollBox = new JPanel();
    /** Column of computed cost labels, one per category, plus its header label. */
    private final JPanel costBox = new JPanel();
    /** Column of computed time-estimate labels (Simple mode only), plus its header label. */
    private final JPanel timeBox = new JPanel();
    /** Spacer panel used purely for grid layout padding. */
    private final JPanel blankPanel1 = new JPanel();
    /** Spacer panel used purely for grid layout padding. */
    private final JPanel blankPanel2 = new JPanel();
    /** Holds the "Estimated Total:" caption label. */
    private final JPanel totalTextPanel = new JPanel();
    /** Holds the running total cost label. */
    private final JPanel totalPanel = new JPanel();
    /** Holds the running total time label (Simple mode only). */
    private final JPanel timePanel = new JPanel();
    /** Which of {@link #TYPE_BULK}, {@link #TYPE_SIMPLE}, {@link #TYPE_SALVAGE} this dialog is running as. */
    private final int repairType;
    /** Which of {@link #UNIT_TYPE_SINGLE}, {@link #UNIT_TYPE_ALL} the chosen settings apply to. */
    private final int unitRepairType;
    /** The MegaMek {@link Entity} currently being repaired/costed; reassigned per-unit when iterating "all" units. */
    private Entity unit;

    /**
     * Builds and immediately displays the repair dialog for a given unit.
     * <p>
     * Looks up the target unit from the player's hangar, snapshots its {@link Entity} (under a
     * lock on the entity, presumably to avoid racing with concurrent combat/repair updates),
     * builds the category grid via {@link #loadPanel()}, sizes the window differently depending
     * on whether this is Bulk mode (narrower, since the per-category checkboxes are hidden) or
     * not, wires up key listeners for the Escape-to-close shortcut, and finally calls
     * {@link #setRepair()} to auto-populate sensible tech/roll defaults based on the unit's
     * actual damage before showing itself.
     *
     * @param client         the client connection (used for player/hangar/config/server-setting access)
     * @param unitID         ID of the unit (within the player's hangar) to repair
     * @param repairType     one of {@link #TYPE_BULK}, {@link #TYPE_SIMPLE}, {@link #TYPE_SALVAGE}
     * @param unitRepairType one of {@link #UNIT_TYPE_SINGLE}, {@link #UNIT_TYPE_ALL}
     */
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

    /**
     * Central handler for every button and tech-combo-box event in this dialog.
     * <ul>
     *   <li>{@link #okayCommand} ("Add"/"Start Repairs"/"Start Salvage"): dispatches based on
     *       {@link #repairType}. Simple mode builds a "#techIndex#roll" segment for each of the
     *       six categories and sends one {@code "simplerepair"} campaign chat command per
     *       affected unit (either just {@link #playerUnit} or every damaged Mek/Vehicle in the
     *       hangar, depending on {@link #unitRepairType}). Salvage mode clears any existing work
     *       orders for the unit on the Salvage Master Table and then calls each
     *       {@code checkXxx()} method to (re-)queue salvage work orders. Bulk mode does the same
     *       against the Repair Master Table, but for the "all units" case, iterates every
     *       damaged Mek/Vehicle in the hangar, clearing and re-queuing work orders per unit.
     *       In all cases, the client config is saved and this window is disposed afterward.</li>
     *   <li>{@link #cancelCommand} ("Close"): simply disposes the window, discarding any
     *       selections.</li>
     *   <li>Anything else: assumed to be a per-category tech combo box, whose action command was
     *       set (in {@link #loadPanel()}) to the category's component index as a string. Parsing
     *       that index, this re-rolls a fresh default target roll for the newly selected tech
     *       (via {@link UnitUtils#techBaseRoll}, special-cased when the "Pilot/AsTech" option is
     *       chosen), and if that category's checkbox is checked, recomputes its cost; otherwise
     *       resets its displayed cost to "0". Any exception here (e.g. the command wasn't a
     *       parseable integer) is silently swallowed.</li>
     * </ul>
     *
     * @param e the button click or combo-box selection event
     */
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

    /**
     * Builds the six-column category grid ({@link #repairBox}, {@link #techBox},
     * {@link #rollBox}, {@link #costBox}, and, in Simple mode, {@link #timeBox}) and assembles
     * it into {@link #masterBox}/{@link #masterPanel}.
     * <p>
     * For each of the six repair categories (added in fixed order: Armor, Structure, Weapons,
     * Equipment, Systems, Engines — matching {@link #ARMOR} through {@link #ENGINES}) this adds:
     * a tech-level combo box (including a "Pilot" option if the pilot has the AsTech skill,
     * letting the pilot fix their own unit), a target-roll spinner (3-12, default 8, disabled
     * only for Salvage mode since salvage has no roll to pick), a cost label, and (Simple mode
     * only) a time label. A checkbox per category is also added to {@link #repairBox}: it starts
     * unchecked in Bulk mode (the player opts categories in) and checked in Simple/Salvage mode
     * (defaulting to "act on everything"), and is only user-editable outside of Simple mode
     * (Simple mode shows the checkboxes but disables them, since Simple repair always acts on
     * every category regardless of checkbox state).
     * <p>
     * Finally, if this is Simple mode, immediately computes and displays the initial cost for
     * every category and the running total.
     */
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

    /**
     * Populates the dialog's default selections and cost estimates based on the actual damage
     * state of {@link #unit}, so the player doesn't have to manually enable every category.
     * <p>
     * In Salvage mode, every category's tech level is restored from the client's saved
     * "SALVAGE&lt;CATEGORY&gt;TECH" config parameters (there is no roll for salvage) and every
     * category's cost is (re)computed unconditionally, since salvage always considers every
     * salvageable part.
     * <p>
     * Otherwise (Bulk/Simple mode): if the unit has armor damage, the Armor checkbox is checked
     * and its tech/roll are restored from the saved "REPAIRARMORTECH"/"REPAIRARMORROLL" config
     * params; similarly for internal structure damage. Then every critical slot on the unit is
     * scanned once: damaged/breached equipment slots enable Weapons or Equipment (depending on
     * whether the mounted item is a {@link WeaponType}) with their respective saved tech/roll;
     * damaged/breached non-engine system slots enable Systems; and damaged engine slots
     * (detected via {@link UnitUtils#isEngineCrit}) enable Engines. Finally, the running total
     * cost is recomputed.
     */
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
     * Dispatches to the appropriate {@code setXxxCost()} helper based on the category, or does
     * nothing if {@code repairType} matches none of the known category constants.
     *
     * @param repairType one of {@link #ARMOR}, {@link #INTERNAL}, {@link #SYSTEMS},
     *                    {@link #ENGINES}, {@link #WEAPONS}, {@link #EQUIPMENT} (despite the
     *                    parameter's name, this is a category index, not a {@link #TYPE_BULK}
     *                    style repair-mode constant)
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

    /** Unused; part of {@link KeyListener}. */
    public void keyTyped(KeyEvent arg0) {
    }

    /** Unused; part of {@link KeyListener}. */
    public void keyPressed(KeyEvent arg0) {
    }

    /**
     * Closes the dialog without saving anything when the player presses Escape.
     *
     * @param arg0 the key-release event; only {@link KeyEvent#VK_ESCAPE} is handled
     */
    public void keyReleased(KeyEvent arg0) {

        if (arg0.getKeyCode() == KeyEvent.VK_ESCAPE) {
            super.dispose();
        }
    }

    /**
     * Fired when a target-roll spinner's value changes. The spinner's component name (set in
     * {@link #loadPanel()}) encodes which category it belongs to; if that category's checkbox is
     * currently checked, its cost (and the grand total) is recomputed to reflect the new roll.
     *
     * @param arg0 the spinner change event
     */
    public void stateChanged(ChangeEvent arg0) {

        int location = Integer.parseInt(((JSpinner) arg0.getSource()).getName());

        if (((JCheckBox) repairBox.getComponent(location)).isSelected()) {
            setCost(location);
            setTotalCost();
        }
    }

    /**
     * Queues repair (Bulk/Simple mode) or salvage (Salvage mode) work orders for every armor
     * facet (front, and rear where applicable) on {@link #unit} that needs it, using the Armor
     * row's currently-selected tech level and target roll. Does nothing if the Armor checkbox is
     * unchecked. Also persists the chosen tech/roll back into the client config for next time
     * (skipped when the "Pilot/AsTech" tech option is selected, and using separate
     * "SALVAGEARMORTECH" vs "REPAIRARMORTECH"/"REPAIRARMORROLL" keys depending on mode).
     * <p>
     * In Salvage mode, any location with remaining front or rear armor gets a work order; in
     * Repair mode, only locations where current armor is below the original ({@code getOArmor})
     * get one, and the work order string additionally encodes the target roll and a trailing
     * literal "999" field (format shared with the other {@code checkXxx} methods; meaning defined
     * by the server-side work order parser, not this class).
     */
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

    /**
     * Queues repair/salvage work orders for internal structure on every location of {@link #unit}
     * that needs it (below original structure for repair; any remaining structure for salvage),
     * using the Structure row's tech/roll. Does nothing if the Structure checkbox is unchecked.
     * See {@link #checkArmor()} for the shared persistence and work-order string conventions.
     */
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

    /**
     * Queues a single repair/salvage work order for the unit's engine, using the Engines row's
     * tech/roll. Does nothing if the Engines checkbox is unchecked, or (repair mode only) if the
     * unit has no damaged engine crits at all.
     * <p>
     * Unlike the other {@code checkXxx} methods, this only ever queues one work order: it scans
     * torso locations (center/side torsos, per {@link UnitUtils#LOC_CENTER_TORSO}..
     * {@link UnitUtils#LOC_LT}) for engine crit slots and, in Salvage mode, queues the first
     * intact engine crit found and returns immediately; in Repair mode, queues the first
     * damaged/breached engine crit found and returns immediately — it does not continue to queue
     * additional engine crits beyond the first match.
     */
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
        // Likely bug: the inner loop bounds slot by unit.locations() (the unit's total number of
        // locations, e.g. 8 for a Mek) rather than the number of critical slots in this location
        // (as every other checkXxx()/setXxxCost() method does via getNumberOfCriticalSlots).
        // Since a location typically has 12 critical slots, this can under-scan and miss engine
        // crits in higher slot indices; it happens to work often enough in practice because
        // engine crits are conventionally placed in low slot numbers.
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

    /**
     * Queues repair/salvage work orders for every non-engine system critical slot on
     * {@link #unit} that is damaged/breached (repair mode) or intact (salvage mode), using the
     * Systems row's tech/roll. Does nothing if the Systems checkbox is unchecked. Skips crits
     * deemed non-repairable via {@link UnitUtils#isNonRepairableCrit} and engine crits (handled
     * separately by {@link #checkEngines()}). When a multi-slot system crit is found, the slot
     * cursor is advanced past its remaining slots via {@link UnitUtils#getNumberOfCrits} to avoid
     * re-processing the same system multiple times.
     */
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

    /**
     * Queues repair/salvage work orders for damaged/intact weapons on {@link #unit}, using the
     * Weapons row's tech/roll. Does nothing if the Weapons checkbox is unchecked. Iterates every
     * critical slot per location; for equipment crits whose mounted item is a {@link WeaponType},
     * queues at most one work order per distinct {@link Mounted} instance per location (tracked
     * via {@code lastWeapon}) to avoid submitting duplicate orders for a weapon that occupies
     * multiple critical slots. In salvage mode, only intact (not destroyed/missing) weapons are
     * considered; in repair mode, only destroyed/missing ones are.
     */
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

    /**
     * Queues repair/salvage work orders for damaged/intact non-weapon equipment on {@link #unit},
     * using the Equipment row's tech/roll. Does nothing if the Equipment checkbox is unchecked.
     * Mirror image of {@link #checkWeapons()}: same per-location, per-slot, dedupe-by-Mounted
     * logic, but selects equipment crits whose mounted item is <em>not</em> a {@link WeaponType}.
     */
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

    /**
     * Recomputes and displays the estimated C-bill cost of repairing/salvaging armor.
     * <p>
     * If {@link #unitRepairType} is {@link #UNIT_TYPE_ALL}, the exact cost can't be known ahead
     * of time (it depends on each unit's actual damage), so the label is set to "?????" and the
     * method returns early.
     * <p>
     * Otherwise: for a non-pilot tech, looks up that tech grade's flat per-job cost from server
     * config ("&lt;TechGrade&gt;TechRepairCost") and how far its typical roll would fall short of
     * the player's chosen target roll ({@code techWorkMod}, via {@link UnitUtils#getTechRoll},
     * floored at 0). For the "Pilot/AsTech" option, the tech type is instead resolved to the
     * pilot's actual AsTech skill level (no roll-shortfall premium is computed for that case,
     * so {@code techWorkMod} stays 0). In Salvage mode, every location with remaining front/rear
     * armor adds a flat per-job tech cost. In Repair mode, each location's under-armor deficit
     * (front and rear separately) adds: per-point armor material cost, an extra charge scaled by
     * how far short of the target roll the tech is expected to be, and a flat per-job tech cost;
     * work-hour estimates are also accumulated via {@link #setWorkHours}. Repair-mode totals are
     * finally scaled up by {@link #payOutIncreaseBasedOnRoll} to price in the risk of a failed
     * roll, then floored at 0 and displayed as a whole number of C-bills.
     * <p>
     * Likely bugs in the repair-mode loop below: {@code pointsToRepair} is declared once outside
     * the per-location loop and is never reset to 0 between locations, so it accumulates the
     * total damaged-armor-point count seen so far across all locations; each subsequent
     * {@code cost += armorCost * pointsToRepair} therefore multiplies the per-point armor cost by
     * an ever-growing cumulative total rather than just that location's own deficit, inflating
     * the estimate for any unit damaged in more than one location. Separately, the front-armor
     * branch uses {@code armorCost}, but that variable is only ever assigned (via
     * {@link CUnit#getArmorCost}) inside the rear-armor branch below it — so the very first
     * front-armor cost computed in the loop uses the initial value 0.0 (and subsequent ones use
     * whatever the previous location's rear-armor cost happened to be) rather than that
     * location's own front-armor unit cost.
     */
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

    /**
     * Recomputes and displays the estimated cost of repairing/salvaging internal structure,
     * following the same overall approach as {@link #setArmorCost()} (early-out to "?????" for
     * whole-hangar mode; flat per-location tech cost in Salvage mode; per-point structure cost +
     * roll-shortfall premium + flat tech cost per damaged location in Repair mode; final scaling
     * via {@link #payOutIncreaseBasedOnRoll}). Unlike {@link #setArmorCost()}, the per-point
     * structure cost ({@link CUnit#getStructureCost}) is computed once outside the loop (it does
     * not vary by location) and {@code pointsToRepair} is reassigned (not accumulated) each
     * location, so this method does not exhibit the cumulative-points bug noted there.
     */
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

    /**
     * Recomputes and displays the estimated cost of repairing/salvaging non-engine system crits,
     * following the same "?????" early-out for whole-hangar mode as the other cost methods.
     * Walks every critical slot per location (skipping non-repairable crits via
     * {@link UnitUtils#isNonRepairableCrit} and engine crits, which are costed separately by
     * {@link #setEngineCost()}); for each qualifying system crit, adds a per-crit cost
     * ({@link CUnit#getCritCost}) times the number of slots it occupies, plus a roll-shortfall
     * premium and flat tech fee (repair mode only — salvage mode just charges a flat fee per
     * remaining undamaged crit), and advances the slot cursor past a multi-slot crit's remaining
     * slots so it isn't double-counted. Repair-mode totals are scaled by
     * {@link #payOutIncreaseBasedOnRoll} before display.
     */
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

    /**
     * Recomputes and displays the estimated cost of repairing/salvaging weapons, mirroring
     * {@link #setSystemCost()}'s per-crit costing approach but restricted to equipment crits
     * whose mounted item is a {@link WeaponType}, and deduplicated per distinct {@link Mounted}
     * instance per location (via {@code lastWeapon}) so a multi-crit weapon is only priced once.
     * Salvage mode only considers intact, unmounted-but-present weapons (not destroyed/missing);
     * repair mode only considers destroyed/missing ones. Repair-mode totals are scaled by
     * {@link #payOutIncreaseBasedOnRoll} before display.
     */
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

    /**
     * Recomputes and displays the estimated cost of repairing/salvaging non-weapon equipment.
     * Mirror image of {@link #setWeaponCost()}: identical per-crit costing, dedupe-by-Mounted,
     * and salvage/repair intact-vs-destroyed selection logic, but for equipment crits whose
     * mounted item is <em>not</em> a {@link WeaponType}.
     */
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

    /**
     * Recomputes and displays the estimated cost (and, in Simple mode, resets the time estimate)
     * of repairing/salvaging the engine.
     * <p>
     * In Salvage mode: as soon as the labeled {@code top_loop} finds any intact engine crit, the
     * cost is computed unit-wide from {@link UnitUtils#getNumberOfEngineCrits} minus
     * {@link UnitUtils#getNumberOfDamagedEngineCrits} (i.e. how many engine crits remain to
     * salvage) times the flat per-crit tech cost, and the method returns immediately — the loop
     * is really just being used to confirm the unit has at least one engine crit at all.
     * <p>
     * In Repair mode: the {@code top_loop} instead searches for the first damaged/breached engine
     * crit and records its location/slot, then (outside the loop) computes a roll-shortfall
     * premium, per-crit cost, and flat tech fee for that single slot, scaled by
     * {@link #payOutIncreaseBasedOnRoll}. If no damaged engine crit was found ({@code found}
     * stays false), the cost is zeroed out afterward — but note this happens <em>after</em> the
     * cost formula already runs using whatever {@code cs}/{@code location}/{@code slot} were last
     * left by the search loop (potentially a null {@code CriticalSlot} if the very last slot
     * checked was empty), so {@link CUnit#getCritCost} could theoretically be invoked with a null
     * critical slot in that edge case rather than skipping the calculation outright.
     */
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

    /**
     * Converts a chosen target roll into a price multiplier: the easier the player sets the roll
     * (i.e. the lower the target number a tech needs to hit), the more the repair costs, since an
     * easy roll is far more likely to succeed and thus is priced at a premium; a hard roll (near
     * the 2d6 maximum of 12) is cheap because it is unlikely to succeed. Values below 2 or above
     * 12 are clamped to the array's first/last entries (1.0 and 36.0 respectively) since 2d6 only
     * produces results in [2, 12] and the backing {@code payout} array has exactly 13 entries
     * (indices 0-12).
     *
     * @param roll the player-chosen target roll (nominally 2-12)
     * @return a multiplier applied to the base repair cost
     */
    private double payOutIncreaseBasedOnRoll(int roll) {
        if (roll <= 2) {
            return 1.0;
        } else if (roll > 12) {
            return 36.0;
        }
        final double[] payout = { 1.0, 1.0, 1.0, 1.03, 1.09, 1.20, 1.38, 1.72, 2.40, 3.60, 5.92, 12.0, 36.0 };
        return payout[roll];
    }

    /**
     * Estimates and displays elapsed repair time (in seconds, before {@link #setTotalCost()}
     * later converts the total into h/m/s) for a given category, used only in Simple mode. Starts
     * from a server-configured baseline ("TimeForEachRepairPoint"), multiplied by the number of
     * crit slots involved when repairing a non-armor crit, then doubled once per roll of shortfall
     * between the tech's expected roll and the player's chosen target roll (i.e. an easier target
     * roll costs more time in exchange for a better success chance), then scaled again by
     * {@link #payOutIncreaseBasedOnRoll}. Unless {@code clear} is true, the newly computed time is
     * added on top of whatever the category's time label already showed, allowing multiple
     * locations' work-hour estimates to accumulate across repeated calls within one cost pass.
     *
     * @param type         the repair category (one of {@link #ARMOR}..{@link #ENGINES})
     * @param critLocation the location index being priced, or a negative value to skip entirely
     * @param critSlot     the critical slot index within that location, or a negative value to skip
     * @param armor        true when pricing an armor facet (no crit-slot count multiplier applies)
     * @param clear        true to overwrite the displayed time, false to add to it
     */
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

    /**
     * Sums the six per-category cost labels into the displayed grand total, and (Simple mode
     * only) sums the per-category time labels into an "Xh Ym Zs" total with a matching tooltip.
     * If {@link #unitRepairType} is {@link #UNIT_TYPE_ALL}, per-category costs/times are already
     * "?????" placeholders, so the total is likewise displayed as "?????" and the method returns
     * before attempting to parse those labels as numbers.
     */
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

    /** @return true if this dialog is running in {@link #TYPE_SIMPLE} mode. */
    private boolean isSimple() {
        return repairType == mekwars.common.gui.dialogs.BulkRepairDialog.TYPE_SIMPLE;
    }

    /** @return true if this dialog is running in {@link #TYPE_BULK} mode. */
    private boolean isBulk() {
        return repairType == mekwars.common.gui.dialogs.BulkRepairDialog.TYPE_BULK;
    }

    /** @return true if this dialog is running in {@link #TYPE_SALVAGE} mode. */
    private boolean isSalvage() {
        return repairType == mekwars.common.gui.dialogs.BulkRepairDialog.TYPE_SALVAGE;
    }
}// end BulkRepairDialog.java
