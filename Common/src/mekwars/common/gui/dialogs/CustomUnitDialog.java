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
 * A dialog that a player can use to customize his mech before battle. Currently, only changing pilots is supported.
 *
 * @author Ben
 */

public class CustomUnitDialog extends JDialog implements ActionListener {
    private static final MMLogger LOGGER = MMLogger.create(CustomUnitDialog.class);

    @Serial
    private static final long serialVersionUID = 4035217132830883530L;
    private final JCheckBox chAutoEject = new JCheckBox();
    private final JCheckBox chOffBoard = new JCheckBox();
    private final JTextField fldOffBoardDistance = new JTextField(4);

    private final JButton butCancel = new JButton("Cancel");

    private final Vector<MunitionChoicePanel> m_vMunitions = new Vector<>(1, 1);
    private final JPanel panMunitions = new JPanel();

    private final Vector<MachineGunChoicePanel> m_vMachineGuns = new Vector<>(1, 1);
    private final JPanel panMachineGuns = new JPanel();

    private final JPanel panEdgeSkills = new JPanel();
    private final JCheckBox tacCB = new JCheckBox("TAC Rolls");
    private final JCheckBox koCB = new JCheckBox("KO Rolls");
    private final JCheckBox headHitsCB = new JCheckBox("Head Hit Rolls");
    private final JCheckBox explosionsCB = new JCheckBox("Explosion Rolls");
    private final JPanel panTargeting = new JPanel();
    private final Entity entity;
    private final IClient client;
    private final Client mmClient = new Client("temp", "None", 0);
    private final Pilot pilot;
    private final boolean usingCrits;
    private final CUnit unit;
    private JComboBox<String> targetSelection = new JComboBox<>();
    private boolean okay = false;
    private boolean canDump = false;

    /** Creates new CustomMechDialog */
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

    private void loadAmmo() {
        client.loadBannedAmmo();
    }

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

    private void setupMunitions() {
        int munitionsRows = 0;
        panMunitions.setLayout(new SpringLayout());
        MunitionChoicePanel mcp;// replaced repeatedly w/i while loop
        int year = MathUtility.parseInt(client.getServerConfigs("CampaignYear"), 3045);
        LOGGER.info(STR."Year: \{year}");
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
            panMunitions.add(new JLabel(STR."\{entity.getLocationAbbr(loc)}:", SwingConstants.TRAILING));

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

    private boolean ammoAlreadyLoaded(AmmoType ammo) {
        for (AmmoMounted mounted : entity.getAmmo()) {
            AmmoType currAmmo = mounted.getType();

            if (currAmmo.equals(ammo)) {
                return true;
            }
        }

        return false;
    }

    public boolean isOkay() {
        return okay;
    }

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
            Object selectedItem = targetSelection.getSelectedItem();
            if (selectedItem instanceof String targetedSystem) {
                int newTargetSystem = unit.getTargetSystem().getTypeByName(targetedSystem);
                LOGGER.debug(STR."Targeting Selected: \{newTargetSystem}");
                if (newTargetSystem != unit.getTargetSystem().getCurrentType()) {
                    // Change in targeting - send server notification
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c setTargetSystem#\{unit.getId()}#\{newTargetSystem}");
                }
            }
        }

        setVisible(false);
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
