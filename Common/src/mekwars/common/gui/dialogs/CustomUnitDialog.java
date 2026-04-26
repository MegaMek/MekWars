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

import java.io.Serial;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Vector;

import megamek.client.Client;
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
import mekwars.common.House;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.campaign.pilot.skills.PilotSkill;
import mekwars.common.gui.dialogs.customUnits.MachineGunChoicePanel;
import mekwars.common.gui.dialogs.customUnits.MunitionChoicePanel;
import mekwars.common.gui.dialogs.customUnits.ProtoMekMunitionChoicePanel;
import mekwars.common.util.MWLogger;
import mekwars.common.util.SpringLayoutHelper;

/**
 * A dialog that a player can use to customize his mech before battle. Currently, only changing pilots is supported.
 *
 * @author Ben
 */

public class CustomUnitDialog extends javax.swing.JDialog implements java.awt.event.ActionListener {
    @Serial
    private static final long serialVersionUID = 4035217132830883530L;
    private final javax.swing.JCheckBox chAutoEject = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox chOffBoard = new javax.swing.JCheckBox();
    private final javax.swing.JTextField fldOffBoardDistance = new javax.swing.JTextField(4);

    private final javax.swing.JButton butCancel = new javax.swing.JButton("Cancel");

    private final Vector<MunitionChoicePanel> m_vMunitions = new java.util.Vector<>(1, 1);
    private final javax.swing.JPanel panMunitions = new javax.swing.JPanel();

    private final Vector<MachineGunChoicePanel> m_vMachineGuns = new java.util.Vector<>(1, 1);
    private final javax.swing.JPanel panMachineGuns = new javax.swing.JPanel();

    private final javax.swing.JPanel panEdgeSkills = new javax.swing.JPanel();
    private final javax.swing.JCheckBox tacCB = new javax.swing.JCheckBox("TAC Rolls");
    private final javax.swing.JCheckBox koCB = new javax.swing.JCheckBox("KO Rolls");
    private final javax.swing.JCheckBox headHitsCB = new javax.swing.JCheckBox("Head Hit Rolls");
    private final javax.swing.JCheckBox explosionsCB = new javax.swing.JCheckBox("Explosion Rolls");
    private javax.swing.JComboBox<String> targetSelection = new javax.swing.JComboBox<>();
    private final javax.swing.JPanel panTargeting = new javax.swing.JPanel();

    private final Entity entity;
    private boolean okay = false;

    private final IClient client;
    private boolean canDump = false;

    private final Client mmClient = new Client("temp", "None", 0);
    private final Pilot pilot;
    private final boolean usingCrits;

    private final CUnit unit;

    /** Creates new CustomMechDialog */
    public CustomUnitDialog(IClient client, Entity entity, Pilot pilot, CUnit unit) {
        super(client.getMainFrame());

        this.entity = entity;
        this.client = client;
        this.pilot = pilot;
        this.unit = unit;
        usingCrits = Boolean.parseBoolean(client.getServerConfigs("UsePartsRepair"));

        mmClient.getGame().getOptions().loadOptions();
        setTitle("Customize Unit");

        // refresh all ammo data
        loadAmmo();

        /*
         * Dialog Layout.
         *
         * Generally speaking, dialog's content pane is a holder for a
         * ScrollPane, which itself wraps around a vertical BoxLayout which
         * holds 3 major sub-panels, and a flowpanel containing Okay/Cancel.
         *
         * ScrollPane panels are as follows: - checkboxes and offboard - ammo
         * loads - machinegun settings
         */
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        // scroll pane
        javax.swing.JPanel scrollPanel = new javax.swing.JPanel();
        scrollPanel.setLayout(new javax.swing.BoxLayout(scrollPanel, javax.swing.BoxLayout.Y_AXIS));
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(scrollPanel);

        // add scroll pane and buttons to content pane
        getContentPane().add(scrollPane);
        javax.swing.JPanel panButtons = new javax.swing.JPanel();
        getContentPane().add(panButtons);

        // button setup
        javax.swing.JButton butOkay = new javax.swing.JButton("Okay");
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        getRootPane().setDefaultButton(butOkay);
        panButtons.add(butOkay);
        panButtons.add(butCancel);
        /*
         * Build 1st major subpanel - checkboxes.
         */
        javax.swing.JPanel boxPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());

        // only show autoeject for meks
        if (entity instanceof Mek mek) {
            chAutoEject.setSelected(!mek.isAutoEject());

            // add autoeject label and cbox
            javax.swing.JLabel labAutoEject = new javax.swing.JLabel("Disable Autoeject",
                  javax.swing.SwingConstants.TRAILING);
            boxPanel.add(labAutoEject);
            boxPanel.add(chAutoEject);

        }

        if (pilot.getSkills().has(PilotSkill.EdgeSkillID)) {
            setupEdgeSkills();
            boxPanel.add(new javax.swing.JLabel("Edge Selections", javax.swing.SwingConstants.TRAILING));
            boxPanel.add(panEdgeSkills);
        }

        // look for offboard weapons by looping through all weapons
        boolean eligibleForOffBoard = false;
        for (WeaponMounted mounted : entity.getWeaponList()) {
            WeaponType wtype = mounted.getType();
            if (wtype.hasFlag(WeaponType.F_ARTILLERY)) {
                eligibleForOffBoard = true;
            }
        }

        // set up the offboard box and text field if appropriate
        if (eligibleForOffBoard) {

            // checkbox for offboard
            javax.swing.JLabel labOffBoard = new javax.swing.JLabel("Deploy Offboard",
                  javax.swing.SwingConstants.TRAILING);
            boxPanel.add(labOffBoard);
            boxPanel.add(chOffBoard);
            chOffBoard.setSelected(entity.isOffBoard());

            // distance
            javax.swing.JLabel labOffBoardDistance = new javax.swing.JLabel("Offboard Distance (Hexes):",
                  javax.swing.SwingConstants.TRAILING);
            boxPanel.add(labOffBoardDistance);
            fldOffBoardDistance.setText(Integer.toString(entity.getOffBoardDistance()));
            boxPanel.add(fldOffBoardDistance);

            // rowcount
        }

        // layout the boxpanel. 2 columns. Counted the rows.
        SpringLayoutHelper.setupSpringGrid(boxPanel, 2);

        // add the boxPanel to the scroll panel. We know it has contents b/c all
        // units can mount lights.
        scrollPanel.add(boxPanel);

        /*
         * Build second major subpanel - muntions; however, only for non-inf
         * units.
         */
        if (!(entity instanceof Infantry) || (entity instanceof BattleArmor)) {
            setupMunitions();
            javax.swing.JPanel centeringPanel = new javax.swing.JPanel();
            centeringPanel.setLayout(new javax.swing.BoxLayout(centeringPanel, javax.swing.BoxLayout.Y_AXIS));
            centeringPanel.add(panMunitions);
            scrollPanel.add(centeringPanel);

            // hide the ammo cost in titlebar if no ammo to set
            if (panMunitions.getComponentCount() == 0) {
                setTitle("Customize Unit");
            }

        }

        /*
         * Build the third major subpanel - burst MGs - for Meks and Vehicles.
         * No BA/Inf/Proto/Aero bursts!
         *
         * Only doso if the server has enabled "maxtech_burst"
         */
        if (mmClient.getGame().getOptions().booleanOption("tacops_burst") &&
                  !((entity instanceof Infantry) || (entity instanceof Aero))) {
            setupMachineGuns();
            scrollPanel.add(panMachineGuns);
        }

        /*
         * Build fouth subpanel - Target System
         */
        setupTargetSystems();
        scrollPanel.add(panTargeting);

        // add window listener which hides the window on close.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                setVisible(false);
            }
        });

        // scrollPane.setMinimumSize(new Dimension(150, 150));
        // scrollPane.setMaximumSize(new Dimension(780, 580));
        pack();

        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void setupTargetSystems() {
        String[] names = unit.getTargetSystem().getNonBannedNameArray(client.getData().getBannedTargetingSystems());

        targetSelection = new javax.swing.JComboBox<>(names);
        String currentTargetSystemName = unit.getTargetSystemTypeDesc();
        for (int i = 0; i < names.length; i++) {
            if (targetSelection.getItemAt(i).equalsIgnoreCase(currentTargetSystemName)) {
                targetSelection.setSelectedIndex(i);
            }
        }
        panTargeting.add(new javax.swing.JLabel("Targeting System:"));
        panTargeting.add(targetSelection);
    }

    private void setupMunitions() {

        int munitionsRows = 0;
        panMunitions.setLayout(new javax.swing.SpringLayout());
        MunitionChoicePanel mcp;// replaced repeatedly w/i while loop
        int year = Integer.parseInt(client.getServerConfigs("CampaignYear"));
        MWLogger.errLog(STR."Year: \{year}");
        // int row = 0;
        int location = -1;// also repeatedly replaced

        /*
         * Loop through all ammo?
         */
        for (AmmoMounted m : entity.getAmmo()) {
            AmmoType at = m.getType();

            java.util.Vector<AmmoType> vTypes = new java.util.Vector<>(1, 1);
            java.util.Vector<AmmoType> vAllTypes = AmmoType.getMunitionsFor(at.getAmmoType());
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

                if ((atCheck.getRackSize() != at.getRackSize()) ||
                          (atCheck.getTonnage(entity) != at.getTonnage(entity))) {
                    continue;
                }

                int techlvl = java.util.Arrays.binarySearch(TechConstants.T_SIMPLE_NAMES,
                      mmClient.getGame().getOptions().stringOption("techlevel")); //$NON-NLS-1$
                techlvl = Math.max(0, techlvl);
                int legalLevel = TechConstants.convertFromSimpleLevel(techlvl, entity.isClan());
                boolean bTechMatch = TechConstants.isLegal(legalLevel,
                      atCheck.getTechLevel(year), true, entity.isMixedTech());

                EnumSet<AmmoType.Munitions> munition = atCheck.getMunitionType();
                House faction = client.getData().getHouseByName(client.getPlayer().getHouse());

                // check banned ammo
                if (client.getData().getServerBannedAmmo().stream().anyMatch(munition::contains) ||
                          faction.getBannedAmmo().stream().anyMatch(munition::contains) ||
                          ((client.getAmmoCost(atCheck.getInternalName()) < 0) && !usingCrits)) {
                    continue;
                }

                if (usingCrits &&
                          (client.getPlayer().getPartsCache().getPartsCritCount(atCheck.getInternalName()) < 1) &&
                          !ammoAlreadyLoaded(atCheck) &&
                          (// !client.getPlayer().getAutoReorder()
                                // &&
                                client.getBlackMarketEquipmentList().get(atCheck.getInternalName()) == null)) {
                    //MWLogger.debugLog("Player out of ammo.");
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
                    //MWLogger.debugLog("bTechMatch now true, because all L2 units can use L1 ammo");
                }

                // if is_eq_limits is unchecked allow L1 units to use L2
                // munitions
                //                MWLogger.debugLog("Entity Tech Level: " + entity.getTechLevel());
                //                MWLogger.debugLog("Ammo tech level: " + atCheck.getTechLevel(year));
                //                if (!entity.isClan() && entity.getTechLevel() == TechConstants.T_INTRO_BOXSET && (atCheck.getTechLevel(year) == TechConstants.T_IS_TW_NON_BOX || atCheck.getTechLevel(year) == TechConstants.T_IS_ADVANCED)) {
                //                	bTechMatch = true;
                //                	MWLogger.debugLog("bTechMatch is true, because I said so");
                //                }

                // Possibly allow level 3 ammos, possibly not.
                //                if ((((atCheck.getTechLevel(year) == TechConstants.T_IS_EXPERIMENTAL) || (atCheck.getTechLevel(year) == TechConstants.T_IS_ADVANCED) || (atCheck.getTechLevel(year) == TechConstants.T_IS_UNOFFICIAL)) && (entity.getTechLevel() != TechConstants.T_IS_EXPERIMENTAL) && (entity.getTechLevel() != TechConstants.T_IS_ADVANCED)) || (((atCheck.getTechLevel(year) == TechConstants.T_CLAN_EXPERIMENTAL) || (atCheck.getTechLevel(year) == TechConstants.T_CLAN_ADVANCED) || (atCheck.getTechLevel(year) == TechConstants.T_CLAN_UNOFFICIAL)) && (entity.getTechLevel() != TechConstants.T_CLAN_EXPERIMENTAL) && (entity.getTechLevel() != TechConstants.T_CLAN_ADVANCED))) {
                //
                //                	bTechMatch = false;
                //                }


                // allow mixed Tech Mechs to use both IS and Clan Ammo
                if (entity.isMixedTech()) {
                    bTechMatch = true;
                }

                // If clan_ignore_eq_limits is unchecked,
                // do NOT allow Clans to use IS-only ammo.
                // N.B. play bit-shifting games to allow "incendiary"
                // to be combined to other munition types.
                EnumSet<AmmoType.Munitions> muniType = atCheck.getMunitionType();
                muniType.add(AmmoType.Munitions.M_INCENDIARY_LRM);
                if (!mmClient.getGame().getOptions().booleanOption("clan_ignore_eq_limits") &&
                          entity.isClan() &&
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
                    MWLogger.debugLog("Minefields disabled");
                    continue;
                }
                // MWLogger.errLog("4.Ammo: "+atCheck.getInternalName()+" MType: "+atCheck.getMunitionType());

                // Only Protos can use Proto-specific ammo
                if (atCheck.hasFlag(AmmoType.F_PROTOMEK) && !(entity instanceof ProtoMek)) {
                    continue;
                }
                // MWLogger.errLog("5.Ammo: "+atCheck.getInternalName()+" MType: "+atCheck.getMunitionType());

                // When dealing with machine guns, Protos can only
                // use proto-specific machine gun ammo
                if ((entity instanceof ProtoMek) &&
                          atCheck.hasFlag(AmmoType.F_MG) &&
                          !atCheck.hasFlag(AmmoType.F_PROTOMEK)) {
                    continue;
                }
                // MWLogger.errLog("6.Ammo: "+atCheck.getInternalName()+" MType: "+atCheck.getMunitionType());

                // Restrict Aero to ATM
                if ((entity instanceof Aero) && !(atCheck.getAmmoType() == AmmoType.AmmoTypeEnum.ATM)) {
                    continue;
                }

                // All other ammo types need to match on rack size and tech.
                //MWLogger.debugLog("bTechMatch at end: " + bTechMatch);

                if (bTechMatch) {
                    vTypes.addElement(atCheck);
                }
            }

            // ProtoMek need special choice panels.
            if (entity instanceof ProtoMek) {
                mcp = new ProtoMekMunitionChoicePanel(this, m, vTypes, location);
            } else if (!(entity instanceof Aero)) {
                mcp = new MunitionChoicePanel(this, m, vTypes, location);
            } else {
                // Aero.  We can only give them default ammos, unless it's an ATM

                // Sweet.  Erroring out on Aeros, because they're specifically
                // being excluded.  Why?
                mcp = new MunitionChoicePanel(this, m, vTypes, location);
                // NOTE: This is a straight copy for testing purposes.  If this
                // Works, we'll just get rid of the "if (!(entity instanceof Aero))
                // above
            }

            // get a location name
            int loc;
            if (m.getLocation() == Entity.LOC_NONE) {// oneshot weapons don't
                // have a location of their
                // own
                Mounted<?> linkedBy = m.getLinkedBy();
                loc = linkedBy.getLocation();
            } else {
                loc = m.getLocation();
            }

            // add location label
            panMunitions.add(new javax.swing.JLabel(entity.getLocationAbbr(loc) + ":",
                  javax.swing.SwingConstants.TRAILING));

            panMunitions.add(mcp);
            m_vMunitions.addElement(mcp);

            // increment the rowcount
            munitionsRows++;

        }// end while(ammo remains in enumeration)

        /*
         * setup the spring grid. If there are > 10 combo boxes in play, split
         * into two columns.
         */
        if (munitionsRows > 10) {
            SpringLayoutHelper.setupSpringGrid(panMunitions, 4);
        } else {
            SpringLayoutHelper.setupSpringGrid(panMunitions, 2);
        }
    }

    private void setupEdgeSkills() {

        panEdgeSkills.setLayout(new javax.swing.SpringLayout());

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

    private void setupMachineGuns() {

        int mgRows = 0;
        panMachineGuns.setLayout(new javax.swing.SpringLayout());

        for (int location = Mek.LOC_HEAD; location <= Mek.LOC_LEFT_LEG; location++) {
            for (int slot = 0; slot < entity.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot crit = entity.getCritical(location, slot);

                if ((crit == null) || (crit.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                    continue;
                }

                Mounted<?> m = crit.getMount();

                if ((m == null) || !(m.getType() instanceof WeaponType wt)) {
                    continue;
                }

                if (!wt.hasFlag(WeaponType.F_MG)) {
                    continue;
                }

                // Protomeks need special choice panels.
                MachineGunChoicePanel mgcp = new MachineGunChoicePanel(this, m, location, slot);
                panMachineGuns.add(mgcp);
                m_vMachineGuns.addElement(mgcp);

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

    public boolean isOkay() {
        return okay;
    }

    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        if (actionEvent.getSource() != butCancel) {
            // get values
            // String name = fldName.getText();
            int offBoardDistance;
            boolean autoEject = chAutoEject.isSelected();

            if (chOffBoard.isSelected()) {
                try {
                    offBoardDistance = Integer.parseInt(fldOffBoardDistance.getText());
                } catch (NumberFormatException e) {
                    client.showInfoWindow("Please enter valid numbers for off board distance.");
                    return;
                }
                if (offBoardDistance < 17) {
                    client.showInfoWindow("Offboard units need to be at least one mapsheet (17 hexes) away.");
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
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c setautoeject#\{mek.getExternalId()}#\{!autoEject}");
                }
                if (pilot.getSkills().has(PilotSkill.EdgeSkillID)) {
                    client.sendChat(
                          STR."\{IClient.CAMPAIGN_PREFIX}c setedgeSkills#\{mek.getExternalId()}#\{tacCB.isSelected()}#\{koCB.isSelected()}#\{headHitsCB.isSelected()}#\{explosionsCB.isSelected()}");
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
            int newTargetSystem = unit.getTargetSystem()
                                        .getTypeByName(Objects.requireNonNull(targetSelection.getSelectedItem())
                                                             .toString());
            MWLogger.errLog(STR."Targeting Selected: \{newTargetSystem}");
            if (newTargetSystem != unit.getTargetSystem().getCurrentType()) {
                // Change in targeting - send server notification
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c setTargetSystem#\{unit.getId()}#\{newTargetSystem}");
            }
        }

        setVisible(false);
    }

    private void loadAmmo() {
        client.loadBannedAmmo();
    }

    private boolean ammoAlreadyLoaded(AmmoType ammo) {

        for (AmmoMounted mounted : entity.getAmmo()) {
            AmmoType currAmmo = mounted.getType();

            if (currAmmo.equals(ammo)) {
                return true;
            }
        }

        return false;
    }

    public Entity getEntity() {
        return entity;
    }

    public IClient getClient() {
        return client;
    }

    public boolean isUsingCrits() {
        return usingCrits;
    }

    public boolean canDump() {
        return canDump;
    }

    public Client getMMClient() {
        return mmClient;
    }
}
