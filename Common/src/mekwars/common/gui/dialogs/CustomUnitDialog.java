/*
 * MekWars - Copyright (C) 2004
 *
 * Original author jtighe (torren)
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

/*
 * CustomUnitDialog.java
 *
 * Created on January 19, 2005
 */

/*
 * Thanks to the MM guys for the majority of the code we needed for this.
 *
 * Substantial changes where made to work with the MW code base but the base
 * can be found in megamek.common.CustomMechDialog.java in megamek 0.29.59
 */

package mekwars.common.gui.dialogs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Vector;
import javax.swing.*;

import megamek.client.Client;
import megamek.codeUtilities.MathUtility;
import megamek.common.CriticalSlot;
import megamek.common.OffBoardDirection;
import megamek.common.TechConstants;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.logging.MMLogger;
import mekwars.common.House;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.campaign.pilot.skills.PilotSkill;
import mekwars.common.gui.dialogs.customUnits.MachineGunChoicePanel;
import mekwars.common.gui.dialogs.customUnits.MunitionChoicePanel;
import mekwars.common.gui.dialogs.customUnits.ProtoMekMunitionChoicePanel;
import mekwars.common.util.SpringLayoutHelper;

/**
 * Dialog that lets a player customize a single unit ({@link #entity}/{@link #unit}) before it is
 * deployed into battle. Despite the class-level summary this originally shipped with ("only
 * changing pilots is supported"), pilots are not actually edited here; the panels this dialog
 * actually builds — conditionally, based on the unit's type and the server's configured game
 * options — are:
 * <ul>
 *   <li>Auto-eject toggle (Meks only).</li>
 *   <li>Edge-skill reroll preferences (TAC, KO, Head Hit, Explosion), if the pilot has the
 *       Edge skill.</li>
 *   <li>Off-board artillery deployment and distance (if the unit carries artillery weapons).</li>
 *   <li>Per-ammo-bin munition selection (for non-Infantry units, or BattleArmor), respecting
 *       tech level, faction/server ammo bans, mixed-tech and Clan equipment rules, and the
 *       server's parts/crit-based repair economy if enabled.</li>
 *   <li>Machine gun burst-fire settings, if the "tacops_burst" option is enabled (Meks/Vehicles
 *       only).</li>
 *   <li>Targeting system selection.</li>
 * </ul>
 * Confirming with "Okay" applies all selections directly to the {@link Entity} and sends the
 * corresponding change(s) to the server as chat commands; "Cancel" simply hides the dialog
 * without applying anything (see {@link #actionPerformed(ActionEvent)}).
 *
 * @author Ben
 */

public class CustomUnitDialog extends JDialog implements ActionListener {
    private static final MMLogger LOGGER = MMLogger.create(CustomUnitDialog.class);

    @Serial
    private static final long serialVersionUID = 4035217132830883530L;
    /** "Disable Auto-eject" checkbox for Meks; note the checkbox's meaning is the logical inverse of {@link Mek#isAutoEject()}. */
    private final JCheckBox chAutoEject = new JCheckBox();
    /** "Deploy Offboard" checkbox, only shown when the unit carries at least one artillery weapon. */
    private final JCheckBox chOffBoard = new JCheckBox();
    /** Offboard deployment distance in hexes; must be at least 17 (one map sheet) or the change is rejected. */
    private final JTextField fldOffBoardDistance = new JTextField(4);

    private final JButton butCancel = new JButton("Cancel");

    /** One {@link MunitionChoicePanel} (or {@link ProtoMekMunitionChoicePanel}) per ammo bin mounted on the unit. */
    private final Vector<MunitionChoicePanel> m_vMunitions = new Vector<>(1, 1);
    /** Container panel holding all entries of {@link #m_vMunitions}. */
    private final JPanel panMunitions = new JPanel();

    /** One {@link MachineGunChoicePanel} per machine gun critical slot found on the unit. */
    private final Vector<MachineGunChoicePanel> m_vMachineGuns = new Vector<>(1, 1);
    /** Container panel holding all entries of {@link #m_vMachineGuns}. */
    private final JPanel panMachineGuns = new JPanel();

    /** Panel of Edge-skill reroll-category checkboxes; only populated/shown if the pilot has the Edge skill. */
    private final JPanel panEdgeSkills = new JPanel();
    /** Whether an available Edge point may be spent to reroll a "Through Armor Critical" (TAC) result. */
    private final JCheckBox tacCB = new JCheckBox("TAC Rolls");
    /** Whether an available Edge point may be spent to reroll a pilot knockout (KO) roll. */
    private final JCheckBox koCB = new JCheckBox("KO Rolls");
    /** Whether an available Edge point may be spent to reroll a head-hit result. */
    private final JCheckBox headHitsCB = new JCheckBox("Head Hit Rolls");
    /** Whether an available Edge point may be spent to reroll an ammo/equipment explosion. */
    private final JCheckBox explosionsCB = new JCheckBox("Explosion Rolls");
    /** Panel holding the targeting-system selector built by {@link #setupTargetSystems()}. */
    private final JPanel panTargeting = new JPanel();
    /** The MegaMek unit (Mek, Vehicle, Aero, ProtoMek, Infantry, etc.) being customized. */
    private final Entity entity;
    /** Client back-link used to query server configs/bans and to send chat commands applying the user's choices. */
    private final IClient client;
    /** A throwaway, never-connected MegaMek {@link Client} instance created solely so this dialog can read the shared {@code GameOptions} (tech level, house rules like "tacops_burst", "clan_ignore_eq_limits", etc.); it is not attached to any real game session. */
    private final Client mmClient = new Client("temp", "None", 0);
    /** The pilot assigned to {@link #entity}, consulted for existing Edge preferences and whether Edge is available at all. */
    private final Pilot pilot;
    /** Whether the server uses the parts/critical-based repair economy ("UsePartsRepair"); affects which munitions are offered as legal choices. */
    private final boolean usingCrits;
    /** The MekWars campaign wrapper for {@link #entity}, used for target-system lookups and reporting the change back to the server. */
    private final CUnit unit;
    /** Combo box of legal (non-banned) targeting systems for {@link #unit}. */
    private JComboBox<String> targetSelection = new JComboBox<>();
    /** Set to {@code true} once the user confirms the dialog via the "Okay" button; queried by {@link #isOkay()}. */
    private boolean okay = false;
    /** Whether the server allows dumping ammo from the lobby ("lobby_ammo_dump"); recomputed (redundantly, to the same value) on every ammo bin processed in {@link #setupMunitions()}. */
    private boolean canDump = false;

    /**
     * Builds (but does not by itself show) the customize-unit dialog for a specific unit/pilot
     * pair. Loads the server's banned-ammo list, then conditionally constructs each sub-panel
     * (checkboxes, munitions, machine guns, targeting system) depending on the unit's type and
     * the server's configured game options, laying them out in a scrollable vertical stack above
     * an Okay/Cancel button row.
     *
     * @param client the client back-link (server configs, chat commands, banned ammo/targeting)
     * @param entity the MegaMek entity being customized
     * @param pilot  the pilot currently assigned to {@code entity}
     * @param unit   the MekWars campaign-unit wrapper for {@code entity}
     */
    public CustomUnitDialog(IClient client, Entity entity, Pilot pilot, CUnit unit) {
        super(client.getMainFrame());

        this.entity = entity;
        this.client = client;
        this.pilot = pilot;
        this.unit = unit;
        usingCrits = MathUtility.parseBoolean(client.getServerConfigs("UsePartsRepair"), false);

        mmClient.getGame().getOptions().loadOptions();
        setTitle("Customize Unit");

        // refresh all ammo data
        loadAmmo();

        /*
         * Dialog Layout.
         *
         * Generally speaking, the dialog's content pane is a holder for a
         * ScrollPane, which itself wraps around a vertical BoxLayout which
         * holds 3 major sub-panels, and a flow panel containing Okay/Cancel.
         *
         * ScrollPane panels are as follows: - checkboxes and offboard - ammo
         * loads - machine gun settings
         */
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        // scroll pane
        JPanel scrollPanel = new JPanel();
        scrollPanel.setLayout(new BoxLayout(scrollPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(scrollPanel);

        // add scroll pane and buttons to content pane
        getContentPane().add(scrollPane);
        JPanel panButtons = new JPanel();
        getContentPane().add(panButtons);

        // button setup
        JButton butOkay = new JButton("Okay");
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        getRootPane().setDefaultButton(butOkay);
        panButtons.add(butOkay);
        panButtons.add(butCancel);
        /*
         * Build 1st major subpanel - checkboxes.
         */
        JPanel boxPanel = new JPanel(new SpringLayout());

        // only show auto eject for meks
        if (entity instanceof Mek mek) {
            chAutoEject.setSelected(!mek.isAutoEject());

            // add auto eject label and check box
            JLabel labAutoEject = new JLabel("Disable Auto-eject", SwingConstants.TRAILING);
            boxPanel.add(labAutoEject);
            boxPanel.add(chAutoEject);

        }

        if (pilot.getSkills().has(PilotSkill.EdgeSkillID)) {
            setupEdgeSkills();
            boxPanel.add(new JLabel("Edge Selections", SwingConstants.TRAILING));
            boxPanel.add(panEdgeSkills);
        }

        // look for off board weapons by looping through all weapons
        boolean eligibleForOffBoard = false;
        for (WeaponMounted mounted : entity.getWeaponList()) {
            WeaponType weaponType = mounted.getType();
            if (weaponType.hasFlag(WeaponType.F_ARTILLERY)) {
                eligibleForOffBoard = true;
            }
        }

        // set up the offboard box and text field if appropriate
        if (eligibleForOffBoard) {
            // checkbox for off board
            JLabel labOffBoard = new JLabel("Deploy Offboard", SwingConstants.TRAILING);
            boxPanel.add(labOffBoard);
            boxPanel.add(chOffBoard);
            chOffBoard.setSelected(entity.isOffBoard());

            // distance
            JLabel labOffBoardDistance = new JLabel("Offboard Distance (Hexes):", SwingConstants.TRAILING);
            boxPanel.add(labOffBoardDistance);
            fldOffBoardDistance.setText(Integer.toString(entity.getOffBoardDistance()));
            boxPanel.add(fldOffBoardDistance);

            // rowcount
        }

        // lay out the box panel. 2 columns. Counted the rows.
        SpringLayoutHelper.setupSpringGrid(boxPanel, 2);

        // add the boxPanel to the scroll panel. We know it has contents b/c all
        // units can mount lights.
        scrollPanel.add(boxPanel);

        /*
         * Build second major subpanel - munitions; however, only for non-inf
         * units.
         */
        if (!(entity instanceof Infantry) || (entity instanceof BattleArmor)) {
            setupMunitions();
            JPanel centeringPanel = new JPanel();
            centeringPanel.setLayout(new BoxLayout(centeringPanel, BoxLayout.Y_AXIS));
            centeringPanel.add(panMunitions);
            scrollPanel.add(centeringPanel);

            // hide the ammo cost in title bar if no ammo to set
            if (panMunitions.getComponentCount() == 0) {
                setTitle("Customize Unit");
            }

        }

        /*
         * Build the third major subpanel - burst MGs - for Meks and Vehicles.
         * No BA/Inf/Proto/Aero bursts!
         *
         * Only do so if the server has enabled "maxtech_burst"
         */
        if (mmClient.getGame().getOptions().booleanOption("tacops_burst") &&
                  !((entity instanceof Infantry) || (entity instanceof Aero))) {
            setupMachineGuns();
            scrollPanel.add(panMachineGuns);
        }

        /*
         * Build forth subpanel - Target System
         */
        setupTargetSystems();
        scrollPanel.add(panTargeting);

        // add window listener which hides the window on close.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                setVisible(false);
            }
        });

        pack();

        setResizable(false);
        setLocationRelativeTo(null);
    }

    /** Tells the client to (re)load the server's banned-ammo list before munition choices are computed. */
    private void loadAmmo() {
        client.loadBannedAmmo();
    }

    /**
     * Builds the Edge reroll-category checkbox panel (TAC/KO/Head Hit/Explosion), pre-checking
     * each box according to the pilot's currently stored Edge preferences.
     */
    private void setupEdgeSkills() {

        panEdgeSkills.setLayout(new SpringLayout());

        tacCB.setSelected(pilot.getTac());
        koCB.setSelected(pilot.getKO());
        explosionsCB.setSelected(pilot.getExplosion());
        headHitsCB.setSelected(pilot.getHeadHit());

        panEdgeSkills.add(tacCB);
        panEdgeSkills.add(koCB);
        panEdgeSkills.add(explosionsCB);
        panEdgeSkills.add(headHitsCB);

        SpringLayoutHelper.setupSpringGrid(panEdgeSkills, 4);
    }

    /**
     * Builds one munition-choice row per ammo bin mounted on {@link #entity}. For each bin, the
     * full set of munition types sharing its rack size/tonnage is filtered down to the choices
     * that are actually legal to load, applying (in order): tech-level legality for the
     * configured campaign year and tech level; server-wide and faction-specific ammo bans;
     * black-market/parts-cache availability when the crit-based repair economy
     * ({@link #usingCrits}) is active (unless the munition is already loaded); special-case
     * exceptions allowing IS units to use certain legacy Level 1 ammo and allowing mixed-tech
     * units to mix IS/Clan ammo; a Clan restriction on IS-only ordnance
     * (semi-guided/Thunder-family munitions) unless "clan_ignore_eq_limits" is set; a
     * "minefields" option check for minefield-delivering munitions; ProtoMek-only and
     * ProtoMek-machine-gun-only ammo restrictions; and an Aero restriction to ATM ammo only.
     * <p>
     * ProtoMeks get a {@link ProtoMekMunitionChoicePanel}; all other unit types (including, per
     * an inline TODO-style comment in this method, Aero units) get a plain
     * {@link MunitionChoicePanel}. The resulting panels are laid out in 2 columns, or 4 columns
     * if there are more than 10 ammo bins.
     */
    private void setupMunitions() {
        int munitionsRows = 0;
        panMunitions.setLayout(new SpringLayout());
        MunitionChoicePanel mcp;// replaced repeatedly w/i while loop
        int year = MathUtility.parseInt(client.getServerConfigs("CampaignYear"), 3045);
        LOGGER.info(String.format("Year: %s", year));
        int location = -1;// also repeatedly replaced

        /*
         * Loop through all ammo?
         */
        for (AmmoMounted ammoMounted : entity.getAmmo()) {
            AmmoType ammoMountedType = ammoMounted.getType();

            Vector<AmmoType> vTypes = new Vector<>(1, 1);
            Vector<AmmoType> vAllTypes = AmmoType.getMunitionsFor(ammoMountedType.getAmmoType());
            location++;

            canDump = mmClient.getGame().getOptions().booleanOption("lobby_ammo_dump");

            if (vAllTypes == null) {
                continue;
            }

            // Remove for now Torren and lets see how this works. appears to
            // cause issues with single tech weapons
            // i.e. HGR's and LGR's

            for (int x = 0, n = vAllTypes.size(); x < n; x++) {
                AmmoType atCheck = vAllTypes.elementAt(x);

                if ((atCheck.getRackSize() != ammoMountedType.getRackSize()) ||
                          (atCheck.getTonnage(entity) != ammoMountedType.getTonnage(entity))) {
                    continue;
                }

                int techlvl = Arrays.binarySearch(TechConstants.T_SIMPLE_NAMES,
                      mmClient.getGame().getOptions().stringOption("techlevel")); //$NON-NLS-1$
                techlvl = Math.max(0, techlvl);
                int legalLevel = TechConstants.convertFromSimpleLevel(techlvl, entity.isClan());
                boolean bTechMatch = TechConstants.isLegal(legalLevel,
                      atCheck.getTechLevel(year),
                      true,
                      entity.isMixedTech());

                EnumSet<AmmoType.Munitions> munition = atCheck.getMunitionType();
                House faction = client.getData().getHouseByName(client.getPlayer().getHouse());

                // check banned ammo
                if (client.getData().getServerBannedAmmo().stream().anyMatch(munition::contains) ||
                          (faction != null && faction.getBannedAmmo().stream().anyMatch(munition::contains)) ||
                          ((client.getAmmoCost(atCheck.getInternalName()) < 0) && !usingCrits)) {
                    continue;
                }

                if (usingCrits &&
                          (client.getPlayer().getPartsCache().getPartsCritCount(atCheck.getInternalName()) < 1) &&
                          !ammoAlreadyLoaded(atCheck) &&
                          (client.getBlackMarketEquipmentList().get(atCheck.getInternalName()) == null)) {
                    continue;
                }

                // allow all lvl2 IS units to use level 1 ammo
                // lvl1 IS units don't need to be allowed to use lvl1 ammo,
                // because there is no special lvl1 ammo, therefore it doesn't
                // need to show up in this display.
                if (!bTechMatch &&
                          ((entity.getTechLevel() == TechConstants.T_IS_ADVANCED) ||
                                 (entity.getTechLevel() == TechConstants.T_IS_EXPERIMENTAL)) &&
                          (atCheck.getTechLevel(year) <= TechConstants.T_IS_TW_NON_BOX)) {
                    bTechMatch = true;
                }

                // allow mixed Tech Meks to use both IS and Clan Ammo
                if (entity.isMixedTech()) {
                    bTechMatch = true;
                }

                // If clan_ignore_eq_limits is unchecked,
                // do NOT allow Clans to use IS-only ammo.
                // N.B. play bit-shifting games to allow "incendiary"
                // to be combined to other munition types.
                EnumSet<AmmoType.Munitions> munitionType = atCheck.getMunitionType();
                munitionType.add(AmmoType.Munitions.M_INCENDIARY_LRM);
                if (!mmClient.getGame().getOptions().booleanOption("clan_ignore_eq_limits") &&
                          entity.isClan() &&
                          ((munitionType.contains(AmmoType.Munitions.M_SEMIGUIDED)) ||
                                 (munitionType.contains(AmmoType.Munitions.M_THUNDER_AUGMENTED)) ||
                                 (munitionType.contains(AmmoType.Munitions.M_THUNDER_INFERNO)) ||
                                 (munitionType.contains(AmmoType.Munitions.M_THUNDER_VIBRABOMB)) ||
                                 (munitionType.contains(AmmoType.Munitions.M_THUNDER_ACTIVE)) ||
                                 (munitionType.contains(AmmoType.Munitions.M_INFERNO_IV)) ||
                                 (munitionType.contains(AmmoType.Munitions.M_VIBRABOMB_IV)))) {
                    bTechMatch = false;
                }

                if (!mmClient.getGame().getOptions().booleanOption("minefields") &&
                          AmmoType.canDeliverMinefield(atCheck)) {
                    LOGGER.debug("Minefields disabled");
                    continue;
                }

                // Only ProtoMeks can use Proto-specific ammo
                if (atCheck.hasFlag(AmmoType.F_PROTOMEK) && !(entity instanceof ProtoMek)) {
                    continue;
                }

                // When dealing with machine guns, Protos can only
                // use proto-specific machine gun ammo
                if ((entity instanceof ProtoMek) &&
                          atCheck.hasFlag(AmmoType.F_MG) &&
                          !atCheck.hasFlag(AmmoType.F_PROTOMEK)) {
                    continue;
                }

                // Restrict Aero to ATM
                if ((entity instanceof Aero) && !(atCheck.getAmmoType() == AmmoType.AmmoTypeEnum.ATM)) {
                    continue;
                }

                if (bTechMatch) {
                    vTypes.addElement(atCheck);
                }
            }

            // ProtoMek need special choice panels.
            if (entity instanceof ProtoMek) {
                mcp = new ProtoMekMunitionChoicePanel(this, ammoMounted, vTypes, location);
            } else if (!(entity instanceof Aero)) {
                mcp = new MunitionChoicePanel(this, ammoMounted, vTypes, location);
            } else {
                // Aero.  We can only give them default ammos, unless it's an ATM

                // Sweet.  Erroring out on Aeros, because they're specifically
                // being excluded.  Why?
                mcp = new MunitionChoicePanel(this, ammoMounted, vTypes, location);
                // NOTE: This is a straight copy for testing purposes.  If this
                // Works, we'll just get rid of the "if (!(entity instanceof Aero))
                // above
            }

            // get a location name
            int loc;
            if (ammoMounted.getLocation() == Entity.LOC_NONE) {// one shot weapons don't
                // have a location of their
                // own
                Mounted<?> linkedBy = ammoMounted.getLinkedBy();
                loc = linkedBy.getLocation();
            } else {
                loc = ammoMounted.getLocation();
            }

            // add location label
            panMunitions.add(new JLabel(String.format("%s:", entity.getLocationAbbr(loc)), SwingConstants.TRAILING));

            panMunitions.add(mcp);
            m_vMunitions.addElement(mcp);

            // increment the rowcount
            munitionsRows++;

        }// end while(ammo remains in enumeration)

        /*
         * set up the spring grid. If there are > 10 combo boxes in play, split
         * into two columns.
         */
        if (munitionsRows > 10) {
            SpringLayoutHelper.setupSpringGrid(panMunitions, 4);
        } else {
            SpringLayoutHelper.setupSpringGrid(panMunitions, 2);
        }
    }

    /**
     * Scans every critical slot of the unit for mounted machine guns and creates a
     * {@link MachineGunChoicePanel} (burst-fire setting) for each one found. Only invoked when
     * the server's "tacops_burst" option is enabled and the unit is not Infantry or Aero. Panels
     * are laid out in 1 column, or 2 columns if there are 6 or more machine guns.
     */
    private void setupMachineGuns() {
        int mgRows = 0;
        panMachineGuns.setLayout(new SpringLayout());

        for (int location = Mek.LOC_HEAD; location <= Mek.LOC_LEFT_LEG; location++) {
            for (int slot = 0; slot < entity.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot crit = entity.getCritical(location, slot);

                if ((crit == null) || (crit.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                    continue;
                }

                Mounted<?> critMount = crit.getMount();

                if ((critMount == null) || !(critMount.getType() instanceof WeaponType weaponType)) {
                    continue;
                }

                if (!weaponType.hasFlag(WeaponType.F_MG)) {
                    continue;
                }

                // Protomeks need special choice panels.
                MachineGunChoicePanel machineGunChoicePanel = new MachineGunChoicePanel(this,
                      critMount,
                      location,
                      slot);
                panMachineGuns.add(machineGunChoicePanel);
                m_vMachineGuns.addElement(machineGunChoicePanel);

                mgRows++;
            }
        }

        /*
         * setup the spring grid. If there are >6 combo boxes in play, split
         * into two columns.
         */
        if (mgRows >= 6) {
            SpringLayoutHelper.setupSpringGrid(panMachineGuns, 2);
        } else {
            SpringLayoutHelper.setupSpringGrid(panMachineGuns, 1);
        }
    }

    /**
     * Populates {@link #targetSelection} with the names of targeting systems that are legal for
     * this unit (i.e. not on the server's banned-targeting-systems list) and pre-selects the
     * unit's currently equipped targeting system, if found in the list.
     */
    private void setupTargetSystems() {
        String[] names = unit.getTargetSystem().getNonBannedNameArray(client.getData().getBannedTargetingSystems());

        targetSelection = new JComboBox<>(names);
        String currentTargetSystemName = unit.getTargetSystemTypeDesc();
        for (int i = 0; i < names.length; i++) {
            if (targetSelection.getItemAt(i).equalsIgnoreCase(currentTargetSystemName)) {
                targetSelection.setSelectedIndex(i);
            }
        }
        panTargeting.add(new JLabel("Targeting System:"));
        panTargeting.add(targetSelection);
    }

    /**
     * @param ammo the munition type to check
     * @return {@code true} if {@code entity} already has this exact ammo type loaded in some bin
     * (used to still allow re-selecting an already-mounted munition even when the crit-based
     * repair economy would otherwise disqualify it for lack of available parts).
     */
    private boolean ammoAlreadyLoaded(AmmoType ammo) {
        for (AmmoMounted mounted : entity.getAmmo()) {
            AmmoType currAmmo = mounted.getType();

            if (currAmmo.equals(ammo)) {
                return true;
            }
        }

        return false;
    }

    /** @return {@code true} if the user confirmed the dialog via "Okay" (as opposed to "Cancel" or closing the window). */
    public boolean isOkay() {
        return okay;
    }

    /**
     * Handles both the "Okay" and "Cancel" buttons (both register this dialog itself as their
     * {@link ActionListener}, so the two are distinguished purely by checking the event source).
     * <p>
     * On "Cancel" (source == {@link #butCancel}), does nothing but hide the dialog. On any other
     * source (i.e. "Okay"), applies every pending change directly to {@link #entity}/{@link #unit}
     * and notifies the server via chat commands: offboard deployment (validating a minimum
     * distance of 17 hexes, one map sheet), the auto-eject toggle (only sent if it actually
     * differs from the mek's current setting), Edge reroll preferences, every munition/machine-gun
     * choice panel's selection (via their {@code applyChoice()} methods), and a targeting-system
     * change (only sent if the selection differs from the unit's current targeting system). Sets
     * {@link #okay} to {@code true} before hiding the dialog.
     */
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() != butCancel) {
            // get values
            // String name = fldName.getText();
            int offBoardDistance;
            boolean autoEject = chAutoEject.isSelected();

            if (chOffBoard.isSelected()) {
                offBoardDistance = MathUtility.parseInt(fldOffBoardDistance.getText(), 0);

                if (offBoardDistance < 17) {
                    client.showInfoWindow("Offboard units need to be at least one map sheet (17 hexes) away.");
                    return;
                }

                entity.setOffBoard(offBoardDistance, OffBoardDirection.NORTH);
            } else {
                entity.setOffBoard(0, OffBoardDirection.NONE);
            }

            // change entity
            if (entity instanceof Mek mek) {
                if (mek.isAutoEject() == autoEject) {
                    mek.setAutoEject(!autoEject);
                    client.sendChat(String.format("%sc setautoeject#%s#%s", IClient.CAMPAIGN_PREFIX, mek.getExternalId(), !autoEject));
                }

                if (pilot.getSkills().has(PilotSkill.EdgeSkillID)) {
                    client.sendChat(
                          new StringBuilder(IClient.CAMPAIGN_PREFIX).append("c setedgeSkills#")
                                .append(mek.getExternalId()).append('#')
                                .append(tacCB.isSelected()).append('#')
                                .append(koCB.isSelected()).append('#')
                                .append(headHitsCB.isSelected()).append('#')
                                .append(explosionsCB.isSelected())
                                .toString());
                }
            }

            okay = true;

            for (MunitionChoicePanel munitionChoicePanel : m_vMunitions) {
                munitionChoicePanel.applyChoice();
            }

            for (MachineGunChoicePanel machineGunChoicePanel : m_vMachineGuns) {
                machineGunChoicePanel.applyChoice();
            }

            // Targeting
            Object selectedItem = targetSelection.getSelectedItem();
            if (selectedItem instanceof String targetedSystem) {
                int newTargetSystem = unit.getTargetSystem().getTypeByName(targetedSystem);
                LOGGER.debug(String.format("Targeting Selected: %s", newTargetSystem));
                if (newTargetSystem != unit.getTargetSystem().getCurrentType()) {
                    // Change in targeting - send server notification
                    client.sendChat(String.format("%sc setTargetSystem#%s#%s", IClient.CAMPAIGN_PREFIX, unit.getId(), newTargetSystem));
                }
            }
        }

        setVisible(false);
    }

    /** @return the unit being customized; used by child choice panels (e.g. {@link MunitionChoicePanel}) to query mounts/location. */
    public Entity getEntity() {
        return entity;
    }

    /** @return the client back-link, exposed so child choice panels can query server configs/bans. */
    public IClient getClient() {
        return client;
    }

    /** @return whether the server's parts/crit-based repair economy is active; affects which munitions choice panels allow selecting. */
    public boolean isUsingCrits() {
        return usingCrits;
    }

    /** @return whether the server allows ammo dumping from the lobby ("lobby_ammo_dump"). */
    public boolean canDump() {
        return canDump;
    }

    /** @return the throwaway {@link Client} instance used purely to access shared MegaMek game options. */
    public Client getMMClient() {
        return mmClient;
    }
}
