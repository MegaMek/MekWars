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

/**
 * Detailed, per-location repair/salvage dialog for a single unit ({@link Entity}/{@link CUnit} pair).
 * <p>
 * This is the "advanced" counterpart to the simpler repair UIs: it lays the unit's crit-slot table and armor
 * table out location-by-location (head, torsos, arms, legs) as two tabs ("Crits" and "Armor") inside a
 * {@link JTabbedPane}, color-coded green/yellow/red per location depending on whether that location has any
 * damaged/missing/breached critical slots or damaged armor/internal structure. The player clicks an entry in a
 * location's crit or armor list to select exactly one damaged item, picks a tech (or the unit's own pilot, or
 * "reward points" if the server allows it) and a number of work hours from a combo box/spinner pair, and the
 * dialog live-computes the C-bill (or influence-point) cost, the roll needed to succeed, and the number of
 * work-hours available at each roll-modifier tier.
 * <p>
 * Pressing "Repair"/"Salvage" (labelled via {@link #okayCommand}, whose displayed text differs based on
 * {@link #salvage}) either sends a {@code repairunit}/{@code salvageunit} campaign chat command immediately (if
 * enough techs of the chosen type are currently free), or - if no tech is immediately available - queues the job
 * with the client's Repair/Maintenance Tracker ({@code client.getRMT()}) and redraws the dialog so the player can
 * queue additional jobs. Pressing "Close" resets any in-progress repair-flag state on the player and disposes
 * the window without sending anything.
 * <p>
 * The dialog is opened either directly (server- or player-initiated) via {@link #AdvancedRepairDialog(IClient,
 * int, boolean)}, or by {@link mekwars.common.commands.AdvancedRepairDialogCommand} re-opening it with a
 * refreshed unit snapshot after server-side unit state changes (e.g. a previous repair job completing).
 */
public class AdvancedRepairDialog extends JFrame implements ActionListener, MouseListener, KeyListener, ChangeListener {

    /** Serialization id for this {@link JFrame} subclass. */
    @Serial
    private static final long serialVersionUID = 381067715464633969L;
    /** Action command for the "Repair"/"Salvage" button. */
    private final static String okayCommand = "Add";
    /** Action command for the "Close" button. */
    private final static String cancelCommand = "Close";
    /** Action command for the tech-selection combo box, fired when the player picks a different tech/skill tier. */
    private final static String techComboCommand = "TechCombo";
    /** Number of techs the player currently has available at each {@code UnitUtils.TECH_*} skill tier, indexed
     *  by tier (see {@link UnitUtils#TECH_GREEN} etc.); populated from {@code client.getPlayer().getAvailableTechs()}. */
    private final Vector<Integer> techs = new Vector<>(1, 1);
    /** Top-level panel holding the tabbed crit/armor pane plus the tech-selection panel. */
    private final JPanel masterPanel = new JPanel(new SpringLayout());
    /** Panel holding the tech/hours/cost/roll/attempts controls shown below the crit/armor tabs. */
    private final JPanel techPanel = new JPanel(new SpringLayout());

    // private final static String delimiter = "*";
    // Text boxes
    /** Read-only field displaying the computed cost (in C-bills or influence points) of the selected repair job. */
    private final JTextField costField = new JTextField(3);
    /** Backing list model for {@link #workHoursField}; holds the selectable work-hour amounts for the current job. */
    private final SpinnerListModel workHoursModel = new SpinnerListModel();
    /** Spinner letting the player choose how many work hours to commit to the selected repair (affects roll
     *  difficulty via {@link #techWorkMod} - fewer hours than baseline makes the roll harder, more hours easier). */
    private final JSpinner workHoursField = new JSpinner();
    /** Read-only field displaying the target number the tech must roll (2d6) to succeed at the repair. */
    private final JTextField baseRollField = new JTextField(3);
    /** Backing numeric model for {@link #numberOfRetriesField}; bounds/steps the retry count. */
    private final SpinnerNumberModel numberOfRetriesEditor = new SpinnerNumberModel();
    /** Spinner letting the player cap how many repair attempts the tech will make (-1 = unlimited); hidden
     *  when {@link #salvage} is true, since salvage jobs aren't retried. */
    private final JSpinner numberOfRetriesField = new JSpinner(numberOfRetriesEditor);
    /** Tabbed pane holding the "Crits" and "Armor" location tables, rebuilt by {@link #loadPanel()}. */
    JTabbedPane configPane = new JTabbedPane(SwingConstants.TOP);
    /** Current campaign year (from the "CampaignYear" server config), used for tech-level/cost lookups. */
    int year;
    // store the client backlink for other things to use
    /** Back-link to the client used to read server configs/player state and to send repair/salvage commands. */
    private IClient client = null;
    /** The unit ({@link Entity}) being repaired/salvaged. */
    private Entity unit = null;
    /** The campaign-side wrapper ({@link CUnit}) for {@link #unit}, used for pilot/cost lookups. */
    private CUnit playerUnit = null;
    // BUTTONS
    /** Location index (e.g. {@code Mek.LOC_*}) of the crit/armor entry currently selected for repair. -1 = none. */
    private int critLocation = -1;
    /** Confirmed critical slot index (or one of the {@code UnitUtils.LOC_*_ARMOR} pseudo-slot constants for
     *  armor/internal-structure damage) that a repair job will target once submitted. -1 = none. */
    private int critSlot = -1;
    /** Raw slot index most recently clicked in a crit/armor list, before being normalized/copied into
     *  {@link #critSlot} by {@link #setCost()}. -1 = none. */
    private int selectedSlot = -1;
    /** True if the current selection targets armor/internal structure rather than an equipment/system crit. */
    private boolean armor = false;
    /** Which tab (0 = Crits, 1 = Armor) should be shown selected when {@link #loadPanel()} rebuilds the tabs. */
    private int tabLocation = 0;
    /** Currently selected tech skill tier/type; one of the {@code UnitUtils.TECH_*} constants (green/reg/vet/elite/
     *  pilot/reward-points). */
    private int techType = UnitUtils.TECH_GREEN;
    /** Baseline work-hour cost (before any player-chosen hour adjustment) for the currently selected repair. */
    private int baseLineCost = 0;
    /** Roll-difficulty modifier resulting from the player choosing fewer/more work hours than the baseline;
     *  negative values make the target roll easier (job takes longer), used in {@link #stateChanged}. */
    private int techWorkMod = 0;
    /** Maximum number of repair attempts the tech should make; -1 means unlimited (sent to the server as 999). */
    private int retries = 0;
    /** True if this dialog instance is operating in "salvage" mode (destructively strip/replace a part) rather
     *  than ordinary repair; disables the retries/work-hours controls and changes button/window labels. */
    private boolean salvage = false;
    /** Combo box listing the available techs/skill tiers (and pilot/reward-points options) for the selected job. */
    private JComboBox<String> techComboBox = new JComboBox<>();

    /**
     * Convenience constructor that resolves a unit by id from the player's hangar and delegates to
     * {@link #AdvancedRepairDialog(IClient, CUnit, Entity, boolean)}.
     * <p>
     * QUIRK: the delegated constructor call ({@code new AdvancedRepairDialog(...)}) creates and shows a second,
     * separate dialog instance but its result is discarded - this constructor does not itself become that dialog.
     * Since the delegate constructor already shows its own window, this still works for callers who only care
     * about the dialog appearing on screen, but this instance itself remains an empty, un-shown frame.
     *
     * @param client active client connection.
     * @param unitID id of the unit (looked up via {@code client.getPlayer().getUnit(unitID)}) to repair/salvage.
     * @param salvage true to open in salvage mode, false for ordinary repair.
     */
    public AdvancedRepairDialog(IClient client, int unitID, boolean salvage) {
        CUnit pUnit = client.getPlayer().getUnit(unitID);
        Entity unit;

        synchronized (pUnit.getEntity()) {
            unit = pUnit.getEntity();
        }

        new AdvancedRepairDialog(client, pUnit, unit, salvage);
    }

    /**
     * Builds and displays the repair/salvage dialog for a specific unit.
     * <p>
     * Loads the player's current repair preferences (last-used tab, available techs, tech type, retry count)
     * from {@code client.getPlayer()}, forces salvage mode off if the server disables the "UsePartsRepair"
     * option, builds the crit/armor tabs ({@link #loadPanel()}) and the tech-selection panel
     * ({@link #loadTechPanel()}), and shows the resulting frame.
     *
     * @param client     active client connection; supplies server configs, player repair preferences, and sends
     *                   the resulting repair/salvage chat command.
     * @param playerUnit campaign wrapper for the unit being repaired; used for pilot and cost lookups.
     * @param unit       the underlying {@link Entity} whose crit/armor state is displayed and repaired.
     * @param salvage    true to open in salvage mode (button/title read "Salvage", retries/hours disabled),
     *                   false for ordinary repair. Forced to false if the server's "UsePartsRepair" config is
     *                   disabled, regardless of what's passed in.
     */
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
            windowName = String.format("%s Salvage Dialog", unit.getShortNameRaw());
            okayButton.setText("Salvage");
        } else {
            windowName = String.format("%s Repair Dialog", unit.getShortNameRaw());
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

    /**
     * (Re)builds the "Crits" and "Armor" tabs of {@link #configPane} from the unit's current damage state.
     * <p>
     * For every location on the unit (head, torsos, arms, legs - {@code Mek}-style location constants are used
     * even for non-Mek units, since {@code Entity.locations()} enumerates whatever locations the specific unit
     * type has), this builds:
     * <ul>
     *     <li>An armor {@link JList} showing front armor, rear armor (if the location has any), and internal
     *     structure, each entry formatted as {@code "<name>: <current>/<original>"} with a prefix marking state:
     *     {@code !!} = actively being repaired (and briefly toggles the repair flag off/on via
     *     {@link UnitUtils#removeArmorRepair}/{@link UnitUtils#setArmorRepair} just to read the "true" damaged
     *     value without the in-progress marker skewing it), {@code @@} = already queued in the RMT (Repair/
     *     Maintenance Tracker), no prefix = damaged but not yet queued/repairing.</li>
     *     <li>A crit-slot {@link JList} showing every populated critical slot in the location (system slots for
     *     Meks, equipment slots for all types), similarly prefixed: {@code !!} repairing, {@code @@} queued,
     *     {@code #} missing, {@code *} damaged, {@code x} breached.</li>
     * </ul>
     * Each list's background is colored red (has a damaged/missing/breached crit in that location), yellow (no
     * crit damage but armor/internal-structure damage), or green (undamaged), and both lists get {@code this} as
     * their mouse/key listener so clicking an entry drives {@link #mouseClicked} to update the current
     * selection and recompute cost/roll/hours.
     * <p>
     * Called once from the constructor and again after successfully queuing a work order (see
     * {@link #actionPerformed}) so the tabs reflect the freshly-queued state.
     */
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
                // NOTE: critDamage is only ever set to true later, in the crit-slot loop further below (after
                // the ArmorSlotList for this location has already been built and colored). The ArmorSlotList
                // background-color switch that reads critDamage a few lines down therefore always sees it as
                // false, so an armor list's background can show yellow/green but never red for crit damage.
                boolean critDamage = false;
                String armorName = EquipmentType.getArmorTypeName(unit.getArmorType(location));
                if (armorName.equalsIgnoreCase("Standard")) {
                    armorName = "Armor";
                }

                // Front armor: if currently "over-repaired" (current > original, meaning a repair-in-progress
                // flag is inflating the displayed value), temporarily clear the repair flag to read the true
                // damaged value, format it with the "!!" in-progress marker, then restore the flag.
                if (unit.getArmor(location) > unit.getOArmor(location)) {
                    UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, location);
                    armorNames.add(String.format("!!%s: %s/%s", armorName, unit.getArmor(location), unit.getOArmor(location)));
                    UnitUtils.setArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, location);
                } else if (client.getRMT().isQueued(location, UnitUtils.LOC_FRONT_ARMOR, unit.getExternalId())) {
                    armorNames.add(String.format("@@%s: %s/%s", armorName, Math.max(0, unit.getArmor(location)), unit.getOArmor(
                          location)));
                } else {
                    armorNames.add(String.format("%s: %s/%s", armorName, Math.max(0,
                          unit.getArmor(location)), unit.getOArmor(location)));
                }

                if (unit.getArmor(location) != unit.getOArmor(location)) {
                    armorDamage = true;
                }
                // Rear armor (torsos only): same in-progress-flag toggle trick as front armor above.
                if (unit.hasRearArmor(location)) {
                    if (unit.getArmor(location, true) > unit.getOArmor(location, true)) {
                        UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_REAR_ARMOR, location);
                        armorNames.add(String.format("!!%s(r): %s/%s", armorName, unit.getArmor(location, true), unit.getOArmor(
                              location,
                              true)));
                        UnitUtils.setArmorRepair(unit, UnitUtils.LOC_REAR_ARMOR, location);
                    } else if (client.getRMT().isQueued(location, UnitUtils.LOC_REAR_ARMOR, unit.getExternalId())) {
                        armorNames.add(String.format("@@%s(r): %s/%s", armorName, Math.max(0,
                              unit.getArmor(location, true)), unit.getOArmor(location, true)));
                    } else {
                        armorNames.add(String.format("%s(r): %s/%s", armorName, Math.max(0,
                              unit.getArmor(location, true)), unit.getOArmor(location, true)));
                    }
                    if (unit.getArmor(location, true) != unit.getOArmor(location, true)) {
                        armorDamage = true;
                    }
                }

                // Internal structure ("isName" - Internal, or a special structure type name): same
                // in-progress-flag toggle trick as front/rear armor above.
                if (unit.getInternal(location) > unit.getOInternal(location)) {
                    UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_INTERNAL_ARMOR, location);
                    armorNames.add(String.format("!!%s: %s/%s", isName, unit.getInternal(location), unit.getOInternal(location)));
                    UnitUtils.setArmorRepair(unit, UnitUtils.LOC_INTERNAL_ARMOR, location);
                } else if (client.getRMT().isQueued(location, UnitUtils.LOC_INTERNAL_ARMOR, unit.getExternalId())) {
                    armorNames.add(String.format("@@%s: %s/%s", isName, Math.max(0, unit.getInternal(location)), unit.getOInternal(
                          location)));
                } else {
                    armorNames.add(String.format("%s: %s/%s", isName, Math.max(0, unit.getInternal(location)), unit.getOInternal(
                          location)));
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
                // Name encodes the location index (e.g. "armor3") so mouseClicked() can parse it back out.
                ArmorSlotList.setName(String.format("armor%s", location));
                // Route this location's armor list into the correct sub-panel (one per Mek location constant)
                // and color its background red/yellow/green per the damage-state booleans computed above
                // (see the NOTE above: critDamage is always false here, so red is effectively unreachable).
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

                // Walk every critical slot in this location and build its display label, prefixed to indicate
                // state: "!!" repairing, "@@" queued in the RMT, "# " missing, "* " damaged, "x " breached
                // (no prefix = undamaged). Note that only queued/missing/damaged/breached slots set critDamage
                // to true - a slot already "!!" repairing is not counted as (still) damaged for coloring purposes.
                for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
                    CriticalSlot cs = unit.getCritical(location, slot);
                    if (cs == null) {
                        // Empty slots are only shown as a placeholder for the head location on non-Tank units;
                        // empty slots elsewhere (or on Tanks) are silently omitted from the list entirely.
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
                            critNames.add(String.format("!!%s", m.getDesc()));
                        } else if (client.getRMT().isQueued(location, slot, unit.getExternalId())) {
                            critNames.add(String.format("@@%s", m.getDesc()));
                            critDamage = true;
                        } else if (cs.isMissing()) {
                            critNames.add(String.format("# %s", m.getDesc()));
                            critDamage = true;
                        } else if (cs.isDamaged()) {
                            critDamage = true;
                            critNames.add(String.format("* %s", m.getDesc()));
                        } else if (cs.isBreached()) {
                            critNames.add(String.format("x %s", m.getDesc()));
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
                // Name is just the raw location index (contrast with ArmorSlotList's "armor<n>" naming), so
                // mouseClicked() can tell crit lists and armor lists apart by whether the name starts with "armor".
                CriticalSlotList.setName(Integer.toString(location));
                // Route this location's crit list into the correct sub-panel and color its background
                // red/yellow/green - here critDamage does reflect the crit loop that just ran, unlike the
                // armor list's switch above.
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
        // Assemble the "Crits" tab: head row, then a torso row (LA/LT/CT/RT/RA left-to-right), then a leg row.
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
        // Restore whichever tab (Crits=0, Armor=1) the player had selected before, so re-selecting an item
        // after loadPanel() is rebuilt (e.g. after queuing a work order) doesn't jump back to the first tab.
        configPane.setSelectedIndex(tabLocation);
    }

    /**
     * (Re)builds the tech-selection panel: the tech/skill-tier combo box, the work-hours spinner, the
     * read-only cost/base-roll fields, and (unless {@link #salvage} is set) the retry-count spinner.
     * <p>
     * The combo box always lists the four tech skill tiers (green/regular/veteran/elite) annotated with how
     * many techs of that tier the player currently has available, and conditionally adds a "pilot as tech"
     * entry (if the unit's pilot has the AsTech skill) and/or a "reward points" entry (if the server allows
     * spending reward points on crit repairs and this isn't a salvage job). The previously-selected tech type
     * is restored if still in range, otherwise the selection resets to the first tier.
     * <p>
     * Called once from the constructor and again whenever {@link #loadPanel()} is (re-built), keeping both
     * panels in sync inside {@link #masterPanel}.
     */
    private void loadTechPanel() {

        techPanel.removeAll();

        // Base four tiers are always offered, each annotated with the player's current available-tech count.
        Vector<String> techString = new Vector<>(4, 1);
        techString.add(String.format("Green - %s", techs.elementAt(UnitUtils.TECH_GREEN)));
        techString.add(String.format("Reg   - %s", techs.elementAt(UnitUtils.TECH_REG)));
        techString.add(String.format("Vet   - %s", techs.elementAt(UnitUtils.TECH_VET)));
        techString.add(String.format("Elite - %s", techs.elementAt(UnitUtils.TECH_ELITE)));

        Pilot pilot = playerUnit.getPilot();

        // If the unit's own pilot has the AsTech skill, offer "use the pilot as tech" as an option too.
        if (pilot.getSkills().has(PilotSkill.AsTechSkillID)) {
            techString.add(UnitUtils.techDescription(UnitUtils.TECH_PILOT));
        }

        // Servers may allow crit repairs (but not salvage) to be paid for with reward/influence points instead
        // of a real tech + C-bills; offer that as a final option when enabled.
        if (Boolean.parseBoolean(client.getServerConfigs("AllowCritRepairsForRewards")) && !salvage) {
            techString.add(UnitUtils.techDescription(UnitUtils.TECH_REWARD_POINTS));
        }

        techComboBox = new JComboBox<>(techString);
        techComboBox.addActionListener(this);
        techComboBox.setActionCommand(techComboCommand);

        // Restore the previously-selected tech tier if still in range, otherwise default back to Green.
        // QUIRK: this compares techType against getMaximumRowCount() (the dropdown's visible-row count, which
        // defaults to 8) rather than getItemCount() (the actual number of entries, 4-6 depending on the
        // pilot/reward-points options above). Since techType is always well under 8, this bounds check almost
        // never trips even when techType is actually out of range for the current (shorter) item list.
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

        // Salvage jobs don't support retry limits (a salvage attempt is one-shot), so this control - and the
        // work-hours field below - are only shown/enabled for ordinary repairs.
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

    /**
     * Handles the three interactive controls that fire action events: the "Repair"/"Salvage" button
     * ({@link #okayCommand}), the "Close" button ({@link #cancelCommand}), and the tech-selection combo box
     * ({@link #techComboCommand}).
     * <ul>
     *     <li>{@link #okayCommand}: validates a location/slot is selected, resolves the chosen tech type
     *     (translating the generic "pilot" combo-box index into the pilot's actual AsTech skill level, since it
     *     could also be a reward-points selection), persists the player's repair preferences (location tab,
     *     retry count, tech type) back onto {@code client.getPlayer()} for next time, then either:
     *     <ul>
     *         <li>sends the {@code salvageunit} command immediately and disposes, if {@link #salvage} is set;</li>
     *         <li>sends the {@code repairunit} command immediately and disposes, if the repair is viable right
     *         now (a tech of the chosen type is free) or reward points are being used;</li>
     *         <li>otherwise queues a work order with the client's Repair/Maintenance Tracker
     *         ({@code client.getRMT()}) for later processing once a tech frees up, and rebuilds the crit/tech
     *         panels in place (dialog stays open) so more jobs can be queued.</li>
     *     </ul>
     *     </li>
     *     <li>{@link #cancelCommand}: clears any in-progress repair-selection state on the player and disposes.</li>
     *     <li>{@link #techComboCommand}: fired when the tech combo box selection changes; resolves the actual
     *     tech type/skill level for "pilot" and "reward points" entries, then recomputes the cost, base roll,
     *     and available work-hours for the new tech choice.</li>
     * </ul>
     */
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

                // Remember whether the player was working the Crits or Armor tab, and their retry/tech
                // preferences, so the next AdvancedRepairDialog opened for this player defaults to the same setup.
                if (critSlot >= UnitUtils.LOC_FRONT_ARMOR) {
                    client.getPlayer().setRepairLocation(1);
                } else {
                    client.getPlayer().setRepairLocation(0);
                }
                client.getPlayer().setRepairRetries(retries);
                client.getPlayer().setRepairTechType(techType);

                // -1 (from the spinner) means "unlimited attempts"; encode that as a large-but-finite number
                // for the wire command.
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
                    client.sendChat(String.format("/c salvageunit#%s#%s#%s#%s#%s#true", unit.getExternalId(), critLocation, critSlot, armor, techType));
                    super.dispose();
                    return;
                }

                // If no tech of the chosen type is free right now (and this isn't a reward-points job, which
                // doesn't need a physical tech), queue the job for later instead of executing it immediately.
                if ((!UnitUtils.isRepairViabile(unit, critLocation, critSlot, armor) || (numberOfTechs <= 0)) &&
                          (techType != UnitUtils.TECH_REWARD_POINTS)) {

                    if (!client.getRMT().isQueued(critLocation, critSlot, unit.getExternalId())) {
                        String workOrder = String.format("%s#%s#%s#%s#%s", unit.getExternalId(), critLocation, critSlot, baseRollField.getText(), retries);
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

                    // Rebuild both panels in place so the just-queued job shows up with its "@@" marker and
                    // the dialog stays open for further selections, rather than disposing.
                    loadPanel();
                    loadTechPanel();
                } else {
                    // A tech is free right now (or this is a reward-points job): execute the repair immediately.
                    client.sendChat(String.format("/c repairunit#%s#%s#%s#%s#%s#%s#%s#true", unit.getExternalId(), critLocation, critSlot, armor, techType, retries, techWorkMod));
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
                        techType = pilot.getSkills().getPilotSkill(PilotSkill.AsTechSkillID).getLevel();
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
     * Recomputes {@link #costField} (the displayed C-bill or reward-point price) for the currently selected
     * crit/armor slot ({@link #selectedSlot}, copied into {@link #critSlot} here) and tech ({@link #techType}).
     * <p>
     * Two cost models are supported, selected by the server's "UseRealRepairCosts" config:
     * <ul>
     *     <li><b>Real repair costs</b>: cost is the part's real in-universe value ({@link UnitUtils#getPartCost})
     *     scaled by "RealRepairCostMod" (or zeroed out if "UsePartsRepair" is enabled, meaning parts themselves
     *     are paid for separately), plus the tech's flat labor cost adjusted by the work-hour modifier.</li>
     *     <li><b>Crit-based repair costs</b> (the else branch): cost depends on which kind of slot is targeted -
     *     front armor, rear armor, and internal structure each have their own per-point cost via
     *     {@link CUnit#getArmorCost}/{@link CUnit#getStructureCost} multiplied by the number of damaged
     *     points (again using the armor-repair-flag toggle trick to read the true damaged amount), while an
     *     equipment/system crit's cost comes from {@link CUnit#getCritCost} multiplied by the number of crits
     *     needing repair (or, in salvage mode, the number of undamaged crits still to salvage).</li>
     * </ul>
     * In both models, a flat tech labor cost (looked up per tech tier from the server's
     * "&lt;tier&gt;TechRepairCost" config) is added, scaled by how far the player's chosen work-hour count
     * deviates from the baseline (see {@link #techWorkMod}) - note the tech labor cost only applies for actual
     * tech tiers (the check below only fires when the combo index is less than {@link UnitUtils#TECH_PILOT}),
     * not for the pilot-as-tech option.
     * <p>
     * Finally, if the server allows spending reward points on crit repairs and the reward-points "tech" option
     * is selected, the whole computed cost above is overridden with a reward-point cost based on
     * "RewardPointsForCritRepair" times the number of crits being repaired.
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
            techCost = Integer.parseInt(client.getServerConfigs(String.format("%sTechRepairCost", UnitUtils.techDescription(techType))));
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
                // Equipment/system critical slot (not armor/internal structure).
                armor = false;

                CriticalSlot cs = unit.getCritical(critLocation, critSlot);
                double cost = 1;
                if (salvage) {
                    // Salvaging strips out whatever crits of this equipment are still intact (total minus
                    // already-damaged), rather than fixing damaged ones.
                    totalCrits = UnitUtils.getNumberOfCrits(unit, cs) -
                                       UnitUtils.getNumberOfDamagedCrits(unit, critSlot, critLocation, armor);
                } else {
                    // Ordinary repair only charges for the crits of this equipment that are actually damaged.
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
     * Recomputes {@link #baseRollField}: the target number (out of 2d6) the assigned tech must roll at or above
     * to successfully complete the currently-selected repair/salvage job, based on the unit, location/slot,
     * tech skill tier, whether it's an armor job, the player's faction tech level, and salvage-vs-repair mode
     * (delegates the actual BattleTech-rules computation to {@link UnitUtils#getTechRoll}).
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

    /**
     * Rebuilds {@link #workHoursModel} (and thus the selectable values in {@link #workHoursField}) for the
     * currently-selected repair job.
     * <p>
     * The baseline work-hour cost is the server's "TimeForEachRepairPoint" config, multiplied by the number of
     * critical slots the equipment occupies for equipment/system crits (armor/internal-structure jobs use the
     * flat baseline unchanged) - reward-points jobs always use a baseline of 1 hour regardless. The selectable
     * options then span from half the baseline up through baseline, then double repeatedly one more time per
     * "roll" of headroom the tech has above the minimum possible target roll (roll - 3, since 2d6's minimum
     * useful roll is treated as 3): fewer hours than baseline makes the job harder/faster, more hours makes it
     * easier/slower (see {@link #stateChanged} for how the selected value feeds back into the roll modifier).
     */
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

    /**
     * Reacts to the player clicking an entry in one of the per-location crit or armor {@link JList}s built by
     * {@link #loadPanel()}, updating {@link #critLocation}/{@link #selectedSlot} to match the click and then
     * recomputing the cost/roll/work-hours displays.
     * <p>
     * Distinguishes armor lists from crit lists by list name (armor lists are named {@code "armor<location>"}
     * per {@link #loadPanel()}), and for armor lists maps the clicked row index (0/1/2, in the same order the
     * armor {@link Vector} was built: front armor, then rear armor or internal structure) to the corresponding
     * {@code UnitUtils.LOC_*_ARMOR} pseudo-slot constant - row index 1 means rear armor if the location has any,
     * otherwise internal structure (since the rear-armor row is only present when {@code hasRearArmor} is true).
     *
     * @param arg0 the mouse click event; only acted on when the clicked component is a {@link JList}.
     */
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

    /**
     * Handles right-clicks (BUTTON3) on a crit-slot list entry, offering context-sensitive popup menus for two
     * special cases (left-clicks are handled separately by {@link #mouseClicked}):
     * <ul>
     *     <li>Clicking a Mek's "Cockpit" slot shows an Enable/Disable AutoEject toggle
     *     (see {@link #getPopupMenuForCockpit()}).</li>
     *     <li>Clicking an "Ammo" or "Pods" (ammo bin / BA ammo pod) slot shows a menu of every compatible
     *     munition type the mounted ammo could be swapped to, each annotated with remaining shots and the
     *     C-bill (or influence) cost to refill/switch, filtered down to what's actually legal for this unit
     *     under the current tech level, mixed-tech rules, and server/faction ammo bans/limits (minefields,
     *     experimental/advanced ammo, Clan/IS restrictions, rack size and tonnage matching, BattleArmor
     *     exclusion, ProtoMek-only machine gun ammo, etc). Selecting an entry sends a
     *     {@code setunitammobycrit} campaign chat command to perform the swap. A throwaway {@link Client}
     *     instance is constructed here purely to read the current MegaMek game options
     *     (lobby ammo dump / equipment limits / etc.) used to decide which munitions are legal.</li>
     * </ul>
     *
     * @param arg0 the mouse press event; only acted on for right-clicks on a {@link JList}.
     */
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
                                    info.setText(String.format("%s (%s/1) %s", atCheck.getName(), mounted.getUsableShotsLeft(), client.moneyOrFluMessage(
                                          true,
                                          true,
                                          cost)));
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

                                    info.setText(String.format("%s (%s/%s) %s", atCheck.getName(), mounted.getUsableShotsLeft(), refillShots, client.moneyOrFluMessage(
                                          true,
                                          true,
                                          cost)));
                                }

                                info.addActionListener(new java.awt.event.ActionListener() {
                                    public void actionPerformed(java.awt.event.ActionEvent e) {
                                        client.sendChat(
                                              new StringBuilder(IClient.CAMPAIGN_PREFIX).append("c setunitammobycrit#")
                                                    .append(unit.getExternalId()).append('#')
                                                    .append(critLocation).append('#')
                                                    .append(critSlot).append('#')
                                                    .append(e.getActionCommand())
                                                    .toString());
                                    }
                                });
                                info.setActionCommand(String.format("%s#%s#%s", atCheck.getAmmoType(), atCheck.getInternalName(), atCheck.getRackSize()));
                                popup.add(info);
                            }
                        }// end for
                        popup.show(this, arg0.getX() + 50, arg0.getY() + 120);
                    }// end component is ammo
                }// end component != null
            }// end if Button3
        }// end if JList
    }

    /**
     * Builds a single-item popup menu that toggles the unit's AutoEject setting, labelled "Enable AutoEject" or
     * "Disable AutoEject" depending on the unit's current state. Selecting it sends a {@code setautoeject}
     * campaign chat command and also updates the local {@link Entity} immediately (optimistic local update, so
     * the menu label is correct if reopened before the server confirms).
     *
     * @return a new {@link JPopupMenu} containing the single AutoEject toggle menu item.
     */
    private @org.jspecify.annotations.NonNull JPopupMenu getPopupMenuForCockpit() {
        JPopupMenu popup = new JPopupMenu();

        if (!((Mek) unit).isAutoEject()) {
            JMenuItem info = new JMenuItem("Enable AutoEject");
            info.addActionListener(e -> {
                client.sendChat(
                      String.format("%sc setautoeject#%s#true", IClient.CAMPAIGN_PREFIX, unit.getExternalId()));
                ((Mek) unit).setAutoEject(true);
            });
            popup.add(info);
        } else {
            JMenuItem info = new JMenuItem("Disable AutoEject");
            info.addActionListener(e -> {
                client.sendChat(
                      String.format("%sc setautoeject#%s#false", IClient.CAMPAIGN_PREFIX, unit.getExternalId()));
                ((Mek) unit).setAutoEject(false);
            });
            popup.add(info);
        }
        return popup;
    }

    /**
     * Unused; required by {@link MouseListener} but this dialog only reacts on mouse press/click.
     */
    public void mouseReleased(MouseEvent arg0) {

    }

    /**
     * Unused; required by {@link MouseListener} but this dialog only reacts on mouse press/click.
     */
    public void mouseEntered(java.awt.event.MouseEvent e) {
    }

    /**
     * Unused; required by {@link MouseListener} but this dialog only reacts on mouse press/click.
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * Unused; required by {@link KeyListener} but this dialog only reacts on key release.
     */
    public void keyTyped(java.awt.event.KeyEvent arg0) {
    }

    /**
     * Unused; required by {@link KeyListener} but this dialog only reacts on key release.
     */
    public void keyPressed(java.awt.event.KeyEvent arg0) {
    }

    /**
     * Handles several unrelated key-release cases across different components sharing this listener:
     * <ul>
     *     <li>Escape anywhere in the dialog: clears in-progress repair-selection state and disposes, same as
     *     the Close button.</li>
     *     <li>{@link #numberOfRetriesField}: if its current text isn't parseable as an integer, resets it back
     *     to 0 (defensive validation against manual keyboard edits of the spinner's text).</li>
     *     <li>{@link #workHoursField}: always reverts to {@link JSpinner#getPreviousValue()} - i.e. direct
     *     keyboard typing into the hours spinner is effectively rejected/undone; only the spinner's up/down
     *     arrows (which fire {@link #stateChanged}, not this key handler) can actually change its value.</li>
     *     <li>Any {@link JList} (a crit/armor list) releasing a key: treats it like a fresh selection - note
     *     this branch sets {@code critLocation} from the *tab* index ({@code configPane.getSelectedIndex()},
     *     i.e. 0 or 1 for Crits/Armor) rather than the list's own location, which looks like it may not
     *     correctly identify which location list fired the key event in a multi-location tab.</li>
     * </ul>
     *
     * @param arg0 the key-release event.
     */
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

    /**
     * Reacts to the player changing {@link #workHoursField}'s value (via {@link #workHoursModel}), recomputing
     * the roll-difficulty modifier ({@link #techWorkMod}) and the displayed target roll to reflect committing
     * more or fewer work hours than the baseline ({@link #baseLineCost}) computed by {@link #setWorkHours()}.
     * <p>
     * Only three tiers of hours are actually selectable (see {@link #setWorkHours()}: baseline/2, baseline,
     * then successive doublings), so:
     * <ul>
     *     <li>Fewer hours than baseline (the "rush job" option, always exactly half): reward-points jobs are
     *     forced to a trivial roll of 1 (an "always succeeds" fast option); other jobs simulate the harder
     *     roll by looking up what the roll would be for one tech tier worse than the one selected - except for
     *     Green techs, where {@code techType != UnitUtils.TECH_GREEN} being false means neither branch runs, so
     *     rushing a Green tech's job leaves the previous roll/{@link #techWorkMod} value on screen unchanged
     *     (there's no tier below Green to simulate the penalty with).</li>
     *     <li>Exactly baseline hours: no modifier.</li>
     *     <li>More hours than baseline: for every doubling of hours above baseline, {@link #techWorkMod}
     *     becomes more negative (easier roll), added directly onto the roll computed for the current tech type.
     *     QUIRK: this final {@link UnitUtils#getTechRoll} call omits the trailing {@code salvage} argument
     *     (unlike the "fewer hours" branch and {@link #setBaseRoll()}, which both pass it), so it always
     *     computes the roll as if this were an ordinary repair even when {@link #salvage} is true.</li>
     * </ul>
     * Finally, {@link #setCost()} is called to refresh the cost field for the new work-hour modifier.
     *
     * @param arg0 the spinner change event from {@link #workHoursField}.
     */
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
