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

package client.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import client.MWClient;
import client.campaign.CUnit;
import common.House;
import common.campaign.pilot.Pilot;
import common.campaign.pilot.skills.PilotSkill;
import common.util.SpringLayoutHelper;
import common.util.UnitUtils;
import megamek.client.Client;
import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.TechConstants;

public class AdvancedRepairDialog extends JFrame implements ActionListener, MouseListener, KeyListener, ChangeListener {

    /**
         *
         */
    private static final long serialVersionUID = 381067715464633969L;
    // store the client backlink for other things to use
    private MWClient mwclient = null;
    private Entity unit = null;
    private CUnit playerUnit = null;

    private final static String okayCommand = "Add";
    private final static String cancelCommand = "Close";
    private final static String techComboCommand = "TechCombo";

    private String windowName = "Repair Dialog";

    // private final static String delimiter = "*";

    private int critLocation = -1;
    private int critSlot = -1;
    private int selectedSlot = -1;
    private boolean armor = false;
    private int tablocation = 0;
    private Vector<Integer> techs = new Vector<Integer>(1, 1);
    private int techType = UnitUtils.TECH_GREEN;
    private int baseLineCost = 0;
    private int techWorkMod = 0;
    private int retries = 0;
    private boolean salvage = false;
    // BUTTONS

    private final JButton okayButton = new JButton("Repair");
    private final JButton cancelButton = new JButton("Close");

    // STOCK DIALOUG AND PANE
    // private JDialog dialog;
    private JOptionPane pane;
    private JPanel MasterPanel = new JPanel(new SpringLayout());
    private JPanel contentPane;
    private JPanel techPanel = new JPanel(new SpringLayout());

    // Text boxes
    private JTextField costField = new JTextField(3);
    // private JTextField workHoursField = new JTextField(3);
    private SpinnerListModel workHoursModel = new SpinnerListModel();
    private JSpinner workHoursField = new JSpinner();
    private JTextField baseRollField = new JTextField(3);
    private SpinnerNumberModel numberOfRetriesEditor = new SpinnerNumberModel();
    private JSpinner numberOfRetriesField = new JSpinner(numberOfRetriesEditor);

    private JComboBox<String> techComboBox = new JComboBox<String>();

    JTabbedPane ConfigPane = new JTabbedPane(SwingConstants.TOP);

    int year;

    public AdvancedRepairDialog(MWClient c, int unitID, boolean salvage) {
        CUnit pUnit = c.getPlayer().getUnit(unitID);
        Entity unit = null;
        synchronized (pUnit.getEntity()) {
            unit = pUnit.getEntity();
        }
        new AdvancedRepairDialog(c, pUnit, unit, salvage);
    }

    public AdvancedRepairDialog(MWClient c, CUnit playerUnit, Entity unit, boolean salvage) {

        this.playerUnit = playerUnit;
        this.unit = unit;
        mwclient = c;
        year = Integer.parseInt(mwclient.getserverConfigs("CampaignYear"));
        tablocation = c.getPlayer().getRepairLocation();
        techs.addAll(c.getPlayer().getAvailableTechs());
        techType = c.getPlayer().getRepairTechType();
        retries = c.getPlayer().getRepairRetries();
        this.salvage = salvage;

        if (!Boolean.parseBoolean(mwclient.getserverConfigs("UsePartsRepair"))) {
            this.salvage = false;
        }

        if (this.salvage) {
            windowName = unit.getShortNameRaw() + " Salvage Dialog";
            okayButton.setText("Salvage");
        } else {
            windowName = unit.getShortNameRaw() + " Repair Dialog";
        }

        addKeyListener(this);

        // stored values.

        // Set the tooltips and actions for dialouge buttons
        okayButton.setActionCommand(okayCommand);
        cancelButton.setActionCommand(cancelCommand);

        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);
        okayButton.setToolTipText("Set a tech to repair the selected location.");
        okayButton.setMnemonic('R');
        cancelButton.setMnemonic(KeyEvent.VK_ESCAPE);
        cancelButton.setToolTipText("Close the repair dialog");

        ConfigPane = new JTabbedPane();
        ConfigPane.addMouseListener(this);

        // CREATE THE PANELS
        loadPanel();

        loadTechPanel();

        // Set the user's options
        Object[] options = { okayButton, cancelButton };

        // Create the pane containing the buttons
        pane = new JOptionPane(MasterPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options, null);

        // this.setIconImage(new ImageIcon(new
        // ImageIcon("./data/images/mics/megamek-icon.gif").getImage().getScaledInstance(100,
        // 100, Image.SCALE_DEFAULT)).getImage());
        setIconImage(mwclient.getConfig().getImage("REPAIR").getImage());

        setExtendedState(Frame.NORMAL);
        setTitle(windowName);

        contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(pane, BorderLayout.CENTER);
        setResizable(true);
        this.setSize(new Dimension(268, 628));
        setExtendedState(Frame.NORMAL);

        this.repaint();
        setLocationRelativeTo(mwclient.getMainFrame());

        pack();
        setVisible(true);
        // Create the main dialog and set the default button
        /*
         * dialog = pane.createDialog(MasterPanel, windowName);
         * dialog.getRootPane().setDefaultButton(cancelButton);
         *
         * //Show the dialog and get the user's input dialog.setModal(false);
         * dialog.pack(); dialog.setVisible(true);
         */
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (command.equals(okayCommand)) {

            if ((critLocation < 0) || (critSlot < 0)) {
                JOptionPane.showMessageDialog(null, "Invaild location/Slot please try again");
                return;
            }

            // some times the slot doesn't report as 0 for internal armor.
            if (critLocation > UnitUtils.LOC_LTR) {
                critSlot = UnitUtils.LOC_INTERNAL_ARMOR;
            }

            techType = techComboBox.getSelectedIndex();

            // Need to make sure that its really a pilot as it could be a Reward
            // repair.
            if (techType == UnitUtils.TECH_PILOT) {
                techType = UnitUtils.techType((String) techComboBox.getSelectedItem());
            }

            int retries = 0;

            if (!salvage) {
                retries = Integer.parseInt(numberOfRetriesField.getValue().toString());
            }

            if (critSlot >= UnitUtils.LOC_FRONT_ARMOR) {
                mwclient.getPlayer().setRepairLocation(1);
            } else {
                mwclient.getPlayer().setRepairLocation(0);
            }
            mwclient.getPlayer().setRepairRetries(retries);
            mwclient.getPlayer().setRepairTechType(techType);

            if (retries < 0) {
                retries = 999;
            }

            int numberOfTechs = 1;

            if (techType < UnitUtils.TECH_PILOT) {
                numberOfTechs = mwclient.getPlayer().getAvailableTechs().get(techType);
            } else if ((techType == UnitUtils.TECH_PILOT) && playerUnit.getPilotIsReparing()) {
                numberOfTechs = 0;
            } else if (techType == UnitUtils.TECH_REWARD_POINTS) {
                numberOfTechs = 1;
            }

            if (salvage) {
                mwclient.sendChat("/c salvageunit#" + unit.getExternalId() + "#" + critLocation + "#" + critSlot + "#" + armor + "#" + techType + "#true");
                super.dispose();
                return;
            }

            if ((!UnitUtils.checkRepairViability(unit, critLocation, critSlot, armor) || (numberOfTechs <= 0)) && (techType != UnitUtils.TECH_REWARD_POINTS)) {

                if (!mwclient.getRMT().isQueued(critLocation, critSlot, unit.getExternalId())) {
                    String workOrder = unit.getExternalId() + "#" + critLocation + "#" + critSlot + "#" + baseRollField.getText() + "#" + retries;
                    mwclient.getRMT().addWorkOrder(techType, workOrder);
                    mwclient.systemMessage("Work placed in queue.");
                } else {
                    mwclient.systemMessage("A work order has already been placed for that job!");
                }

                if (armor) {
                    tablocation = 1;
                } else {
                    tablocation = 0;
                }

                this.retries = retries;

                loadPanel();
                loadTechPanel();
            } else {
                mwclient.sendChat("/c repairunit#" + unit.getExternalId() + "#" + critLocation + "#" + critSlot + "#" + armor + "#" + techType + "#" + retries + "#" + techWorkMod + "#true");
                super.dispose();
            }
            return;
        } else if (command.equals(cancelCommand)) {
            mwclient.getPlayer().resetRepairs();
            super.dispose();
        } else if (command.equals(techComboCommand)) {
            techType = techComboBox.getSelectedIndex();
            String techString = (String) techComboBox.getSelectedItem();

            if (UnitUtils.techType(techString) == UnitUtils.TECH_PILOT) {
                Pilot pilot = playerUnit.getPilot();
                techType = pilot.getSkills().getPilotSkill(PilotSkill.AstechSkillID).getLevel();
            } else if (UnitUtils.techType(techString) == UnitUtils.TECH_REWARD_POINTS) {
                techType = UnitUtils.TECH_REWARD_POINTS;
            }

            setCost();
            setBaseRoll();
            setWorkHours();
        }

    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent arg0) {

        if (arg0.getComponent() instanceof JList) {
            JList<String> templist = (JList<String>) arg0.getComponent();
            if (arg0.getButton() == MouseEvent.BUTTON3) {
                String component = (String) templist.getSelectedValue();

                if (component != null) {
                    if ((unit instanceof Mech) && (component.indexOf("Cockpit") > -1)) {
                        JPopupMenu popup = new JPopupMenu();

                        if (!((Mech) unit).isAutoEject()) {
                            JMenuItem info = new JMenuItem("Enable AutoEject");
                            info.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c setautoeject#" + unit.getExternalId() + "#true");
                                    ((Mech) unit).setAutoEject(true);
                                }
                            });
                            popup.add(info);
                        } else {
                            JMenuItem info = new JMenuItem("Disable AutoEject");
                            info.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c setautoeject#" + unit.getExternalId() + "#false");
                                    ((Mech) unit).setAutoEject(false);
                                }
                            });
                            popup.add(info);
                        }
                        popup.show(this, arg0.getX() + 50, arg0.getY() + 120);
                    }// end autoeject
                    else if ((component.indexOf("Ammo") > -1) || (component.indexOf("Pods") > -1)) {
                        JPopupMenu popup = new JPopupMenu();
                        Client mmClient = new Client("temp", "None", 0);
                        mmClient.getGame().getOptions().loadOptions();

                        CriticalSlot cs = unit.getCritical(critLocation, critSlot);
                        Mounted m = cs.getMount();
                        AmmoType at = (AmmoType) unit.getEquipmentType(cs);

                        Vector<AmmoType> vAllTypes = AmmoType.getMunitionsFor(at.getAmmoType());
                        // location++;

                        boolean canDump = mmClient.getGame().getOptions().booleanOption("lobby_ammo_dump");

                        if (vAllTypes == null) {
                            return;
                        }

                        if ((vAllTypes.size() < 2) && !canDump) {
                            return;
                        }

                        for (int x = 0, n = vAllTypes.size(); x < n; x++) {
                            AmmoType atCheck = vAllTypes.elementAt(x);
                            boolean bTechMatch = TechConstants.isLegal(unit.getTechLevel(), atCheck.getTechLevel(year), unit.isMixedTech());// (unit.getTechLevel()
                                                                                                                    // ==
                                                                                                                    // atCheck.getTechLevel());

                            String munition = Long.toString(atCheck.getMunitionType());
                            House faction = mwclient.getData().getHouseByName(mwclient.getPlayer().getHouse());

                            // check banned ammo
                            if (mwclient.getData().getServerBannedAmmo().containsKey(munition) || faction.getBannedAmmo().containsKey(munition)) {
                                continue;
                            }

                            // allow all lvl2 IS units to use level 1 ammo
                            // lvl1 IS units don't need to be allowed to use
                            // lvl1 ammo,
                            // because there is no special lvl1 ammo, therefore
                            // it doesn't
                            // need to show up in this display.
                            if (!bTechMatch && (unit.getTechLevel() == TechConstants.T_IS_ADVANCED) && (atCheck.getTechLevel(year) <= TechConstants.T_IS_TW_NON_BOX)) {
                                bTechMatch = true;
                            }

                            // if is_eq_limits is unchecked allow L1 units to
                            // use L2 munitions
                            if (!mmClient.getGame().getOptions().booleanOption("is_eq_limits") && (unit.getTechLevel() <= TechConstants.T_IS_TW_NON_BOX) && (atCheck.getTechLevel(year) == TechConstants.T_IS_ADVANCED)) {
                                bTechMatch = true;
                            }

                            // Possibly allow level 3 ammos, possibly not.
                            if (mmClient.getGame().getOptions().booleanOption("allow_advanced_ammo")) {
                                if (!mmClient.getGame().getOptions().booleanOption("is_eq_limits")) {
                                    if ((unit.getTechLevel() == TechConstants.T_CLAN_EXPERIMENTAL) && (atCheck.getTechLevel(year) == TechConstants.T_CLAN_EXPERIMENTAL)) {
                                        bTechMatch = true;
                                    }
                                    if (((unit.getTechLevel() <= TechConstants.T_IS_TW_NON_BOX) || (unit.getTechLevel() == TechConstants.T_IS_ADVANCED)) && (atCheck.getTechLevel(year) == TechConstants.T_IS_EXPERIMENTAL)) {
                                        bTechMatch = true;
                                    }
                                }
                            } else if ((atCheck.getTechLevel(year) == TechConstants.T_IS_EXPERIMENTAL) || (atCheck.getTechLevel(year) == TechConstants.T_CLAN_EXPERIMENTAL)) {
                                bTechMatch = false;
                            }

                            // allow mixed Tech Mechs to use both IS and Clan
                            // Ammo
                            if (unit.isMixedTech()) {
                                bTechMatch = true;
                            }

                            // If clan_ignore_eq_limits is unchecked,
                            // do NOT allow Clans to use IS-only ammo.
                            // N.B. play bit-shifting games to allow
                            // "incendiary"
                            // to be combined to other munition types.
                            long muniType = atCheck.getMunitionType();
                            muniType &= ~AmmoType.M_INCENDIARY_LRM;
                            if (!mmClient.getGame().getOptions().booleanOption("clan_ignore_eq_limits") && unit.isClan() && ((muniType == AmmoType.M_SEMIGUIDED) || (muniType == AmmoType.M_THUNDER_AUGMENTED) || (muniType == AmmoType.M_THUNDER_INFERNO) || (muniType == AmmoType.M_THUNDER_VIBRABOMB) || (muniType == AmmoType.M_THUNDER_ACTIVE) || (muniType == AmmoType.M_INFERNO_IV) || (muniType == AmmoType.M_VIBRABOMB_IV))) {
                                bTechMatch = false;
                            }

                            if (!mmClient.getGame().getOptions().booleanOption("minefields") && AmmoType.canDeliverMinefield(atCheck)) {
                                continue;
                            }

                            // Only Protos can use Proto-specific ammo
                            if (atCheck.hasFlag(AmmoType.F_PROTOMECH) && !(unit instanceof Protomech)) {
                                continue;
                            }

                            // When dealing with machine guns, Protos can only
                            // use proto-specific machine gun ammo
                            if ((unit instanceof Protomech) && atCheck.hasFlag(AmmoType.F_MG) && !atCheck.hasFlag(AmmoType.F_PROTOMECH)) {
                                continue;
                            }

                            // BattleArmor ammo can't be selected at all.
                            // All other ammo types need to match on rack size
                            // and tech.
                            if (bTechMatch && (atCheck.getRackSize() == at.getRackSize()) && !atCheck.hasFlag(AmmoType.F_BATTLEARMOR) && (atCheck.getTonnage(unit) == at.getTonnage(unit))) {
                                double ammoCost = mwclient.getAmmoCost(atCheck.getInternalName());
                                int cost = 0;
                                JMenuItem info = new JMenuItem();
                                if (m.getLocation() == Entity.LOC_NONE) {
                                    cost = (int) ammoCost;
                                    info.setText(atCheck.getName() + " (" + m.getUsableShotsLeft() + "/1) " + mwclient.moneyOrFluMessage(true, true, cost));
                                } else {
                                    int refillShots = at.getShots();
                                    if (m.byShot()) {
                                    	refillShots = m.getOriginalShots();
                                    }
                                    int shotsLeft = m.getUsableShotsLeft();
                                    if (!atCheck.getInternalName().equalsIgnoreCase(at.getInternalName())) {
                                        shotsLeft = 0;
                                    }

                                    // No reason to continue if there are not
                                    // shots to refill.
                                    if (shotsLeft == refillShots) {
                                        cost = 0;
                                    } else {
                                        cost = (int) Math.ceil(ammoCost * refillShots);
                                    }

                                    info.setText(atCheck.getName() + " (" + m.getUsableShotsLeft() + "/" + refillShots+ ") " + mwclient.moneyOrFluMessage(true, true, cost));
                                }

                                info.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c setunitammobycrit#" + unit.getExternalId() + "#" + critLocation + "#" + critSlot + "#" + e.getActionCommand());
                                    }
                                });
                                info.setActionCommand(atCheck.getAmmoType() + "#" + atCheck.getInternalName() + "#" + atCheck.getRackSize());
                                popup.add(info);
                            }
                        }// end for
                        popup.show(this, arg0.getX() + 50, arg0.getY() + 120);
                    }// end component is ammo
                }// end component != null
            }// end if Button3
        }// end if JList
    }

    public void mouseEntered(MouseEvent e) {
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

    public void mouseReleased(MouseEvent arg0) {

    }

    private void loadPanel() {
        ConfigPane.removeAll();
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
                JPanel locationPanel = new JPanel();
                locationPanel.addMouseListener(this);
                Vector<String> critNames = new Vector<String>(1, 1);
                Vector<String> armorNames = new Vector<String>(3, 1);
                boolean armorDamage = false;
                boolean critDamage = false;
                String armorName = EquipmentType.getArmorTypeName(unit.getArmorType(location));
                if (armorName.equalsIgnoreCase("Standard")) {
                    armorName = "Armor";
                }

                if (unit.getArmor(location) > unit.getOArmor(location)) {
                    UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, location);
                    armorNames.add("!!" + armorName + ": " + (unit.getArmor(location)) + "/" + unit.getOArmor(location));
                    UnitUtils.setArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, location);
                } else if (mwclient.getRMT().isQueued(location, UnitUtils.LOC_FRONT_ARMOR, unit.getExternalId())) {
                    armorNames.add("@@" + armorName + ": " + Math.max(0, unit.getArmor(location)) + "/" + unit.getOArmor(location));
                } else {
                    armorNames.add(armorName + ": " + Math.max(0, unit.getArmor(location)) + "/" + unit.getOArmor(location));
                }

                if (unit.getArmor(location) != unit.getOArmor(location)) {
                    armorDamage = true;
                }
                if (unit.hasRearArmor(location)) {
                    if (unit.getArmor(location, true) > unit.getOArmor(location, true)) {
                        UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_REAR_ARMOR, location);
                        armorNames.add("!!" + armorName + "(r): " + unit.getArmor(location, true) + "/" + unit.getOArmor(location, true));
                        UnitUtils.setArmorRepair(unit, UnitUtils.LOC_REAR_ARMOR, location);
                    } else if (mwclient.getRMT().isQueued(location, UnitUtils.LOC_REAR_ARMOR, unit.getExternalId())) {
                        armorNames.add("@@" + armorName + "(r): " + Math.max(0, unit.getArmor(location, true)) + "/" + unit.getOArmor(location, true));
                    } else {
                        armorNames.add("" + armorName + "(r): " + Math.max(0, unit.getArmor(location, true)) + "/" + unit.getOArmor(location, true));
                    }
                    if (unit.getArmor(location, true) != unit.getOArmor(location, true)) {
                        armorDamage = true;
                    }
                }

                if (unit.getInternal(location) > unit.getOInternal(location)) {
                    UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_INTERNAL_ARMOR, location);
                    armorNames.add("!!" + isName + ": " + unit.getInternal(location) + "/" + unit.getOInternal(location));
                    UnitUtils.setArmorRepair(unit, UnitUtils.LOC_INTERNAL_ARMOR, location);
                } else if (mwclient.getRMT().isQueued(location, UnitUtils.LOC_INTERNAL_ARMOR, unit.getExternalId())) {
                    armorNames.add("@@" + isName + ": " + Math.max(0, unit.getInternal(location)) + "/" + unit.getOInternal(location));
                } else {
                    armorNames.add(isName + ": " + Math.max(0, unit.getInternal(location)) + "/" + unit.getOInternal(location));
                }

                if (unit.getInternal(location) != unit.getOInternal(location)) {
                    armorDamage = true;
                }

                JList<String> ArmorSlotList = new JList<String>(armorNames);
                ArmorSlotList.addMouseListener(this);
                ArmorSlotList.addKeyListener(this);
                ArmorSlotList.setVisibleRowCount(armorNames.size());
                ArmorSlotList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                ArmorSlotList.setFont(new Font("Arial", Font.PLAIN, 10));
                ArmorSlotList.setName("armor" + location);
                switch (location) {
                case Mech.LOC_HEAD:
                    headArmorPanel.add(ArmorSlotList);
                    headArmorPanel.addMouseListener(this);
                    if (critDamage) {
                        ArmorSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        ArmorSlotList.setBackground(Color.yellow);
                    } else {
                        ArmorSlotList.setBackground(Color.green);
                    }
                    break;
                case Mech.LOC_LARM:
                    laArmorPanel.add(ArmorSlotList);
                    laArmorPanel.addMouseListener(this);
                    if (critDamage) {
                        ArmorSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        ArmorSlotList.setBackground(Color.yellow);
                    } else {
                        ArmorSlotList.setBackground(Color.green);
                    }
                    break;
                case Mech.LOC_RARM:
                    raArmorPanel.add(ArmorSlotList);
                    raArmorPanel.addMouseListener(this);
                    if (critDamage) {
                        ArmorSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        ArmorSlotList.setBackground(Color.yellow);
                    } else {
                        ArmorSlotList.setBackground(Color.green);
                    }
                    break;
                case Mech.LOC_CT:
                    ctArmorPanel.add(ArmorSlotList);
                    ctArmorPanel.addMouseListener(this);
                    if (critDamage) {
                        ArmorSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        ArmorSlotList.setBackground(Color.yellow);
                    } else {
                        ArmorSlotList.setBackground(Color.green);
                    }
                    break;
                case Mech.LOC_LT:
                    ltArmorPanel.add(ArmorSlotList);
                    ltArmorPanel.addMouseListener(this);
                    if (critDamage) {
                        ArmorSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        ArmorSlotList.setBackground(Color.yellow);
                    } else {
                        ArmorSlotList.setBackground(Color.green);
                    }
                    break;
                case Mech.LOC_RT:
                    rtArmorPanel.add(ArmorSlotList);
                    rtArmorPanel.addMouseListener(this);
                    if (critDamage) {
                        ArmorSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        ArmorSlotList.setBackground(Color.yellow);
                    } else {
                        ArmorSlotList.setBackground(Color.green);
                    }
                    break;
                case Mech.LOC_LLEG:
                    llArmorPanel.add(ArmorSlotList);
                    llArmorPanel.addMouseListener(this);
                    if (critDamage) {
                        ArmorSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        ArmorSlotList.setBackground(Color.yellow);
                    } else {
                        ArmorSlotList.setBackground(Color.green);
                    }
                    break;
                case Mech.LOC_RLEG:
                    rlArmorPanel.add(ArmorSlotList);
                    rlArmorPanel.addMouseListener(this);
                    if (critDamage) {
                        ArmorSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        ArmorSlotList.setBackground(Color.yellow);
                    } else {
                        ArmorSlotList.setBackground(Color.green);
                    }
                    break;
                }

                for (int slot = 0; slot < unit.getNumberOfCriticals(location); slot++) {
                    CriticalSlot cs = unit.getCritical(location, slot);
                    if (cs == null) {
                        if (!(unit instanceof Tank) && (location == Mech.LOC_HEAD)) {
                            critNames.add("-- Empty --");
                        }
                        continue;
                    } else if (cs.getType() == CriticalSlot.TYPE_SYSTEM) {
                        String result = "";
                        if (cs.isRepairing()) {
                            result += "!!";
                        } else if (mwclient.getRMT().isQueued(location, slot, unit.getExternalId())) {
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
                        if (unit instanceof Mech) {
                            critNames.add(result + ((Mech) unit).getSystemName(cs.getIndex()));
                        }
                    } else if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                        Mounted m = cs.getMount();
                        if (cs.isRepairing()) {
                            critNames.add("!!" + m.getDesc());
                        } else if (mwclient.getRMT().isQueued(location, slot, unit.getExternalId())) {
                            critNames.add("@@" + m.getDesc());
                            critDamage = true;
                        } else if (cs.isMissing()) {
                            critNames.add("# " + m.getDesc());
                            critDamage = true;
                        } else if (cs.isDamaged()) {
                            critDamage = true;
                            critNames.add("* " + m.getDesc());
                        } else if (cs.isBreached()) {
                            critNames.add("x " + m.getDesc());
                            critDamage = true;
                        } else {
                            critNames.add(m.getDesc());
                        }
                    }
                }
                JList<String> CriticalSlotList = new JList<String>(critNames);
                CriticalSlotList.addMouseListener(this);
                CriticalSlotList.addKeyListener(this);
                CriticalSlotList.setVisibleRowCount(critNames.size());
                CriticalSlotList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                CriticalSlotList.setFont(new Font("Arial", Font.PLAIN, 10));
                CriticalSlotList.setName(Integer.toString(location));
                switch (location) {
                case Mech.LOC_HEAD:
                    headPanel.add(CriticalSlotList);
                    headPanel.addMouseListener(this);
                    if (critDamage) {
                        CriticalSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        CriticalSlotList.setBackground(Color.yellow);
                    } else {
                        CriticalSlotList.setBackground(Color.green);
                    }
                    break;
                case Mech.LOC_LARM:
                    laPanel.add(CriticalSlotList);
                    laPanel.addMouseListener(this);
                    if (critDamage) {
                        CriticalSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        CriticalSlotList.setBackground(Color.yellow);
                    } else {
                        CriticalSlotList.setBackground(Color.green);
                    }
                    break;
                case Mech.LOC_RARM:
                    raPanel.add(CriticalSlotList);
                    raPanel.addMouseListener(this);
                    if (critDamage) {
                        CriticalSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        CriticalSlotList.setBackground(Color.yellow);
                    } else {
                        CriticalSlotList.setBackground(Color.green);
                    }
                    break;
                case Mech.LOC_CT:
                    ctPanel.add(CriticalSlotList);
                    ctPanel.addMouseListener(this);
                    if (critDamage) {
                        CriticalSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        CriticalSlotList.setBackground(Color.yellow);
                    } else {
                        CriticalSlotList.setBackground(Color.green);
                    }
                    break;
                case Mech.LOC_LT:
                    ltPanel.add(CriticalSlotList);
                    ltPanel.addMouseListener(this);
                    if (critDamage) {
                        CriticalSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        CriticalSlotList.setBackground(Color.yellow);
                    } else {
                        CriticalSlotList.setBackground(Color.green);
                    }
                    break;
                case Mech.LOC_RT:
                    rtPanel.add(CriticalSlotList);
                    rtPanel.addMouseListener(this);
                    if (critDamage) {
                        CriticalSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        CriticalSlotList.setBackground(Color.yellow);
                    } else {
                        CriticalSlotList.setBackground(Color.green);
                    }
                    break;
                case Mech.LOC_LLEG:
                    llPanel.add(CriticalSlotList);
                    llPanel.addMouseListener(this);
                    if (critDamage) {
                        CriticalSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        CriticalSlotList.setBackground(Color.yellow);
                    } else {
                        CriticalSlotList.setBackground(Color.green);
                    }
                    break;
                case Mech.LOC_RLEG:
                    rlPanel.add(CriticalSlotList);
                    rlPanel.addMouseListener(this);
                    if (critDamage) {
                        CriticalSlotList.setBackground(Color.red);
                    } else if (armorDamage) {
                        CriticalSlotList.setBackground(Color.yellow);
                    } else {
                        CriticalSlotList.setBackground(Color.green);
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

        ConfigPane.addTab("Crits", mainPanel);

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

        ConfigPane.addTab("Armor", armorPanel);
        ConfigPane.setSelectedIndex(tablocation);
    }

    private void loadTechPanel() {

        techPanel.removeAll();

        Vector<String> techString = new Vector<String>(4, 1);
        techString.add("Green - " + techs.elementAt(UnitUtils.TECH_GREEN));
        techString.add("Reg   - " + techs.elementAt(UnitUtils.TECH_REG));
        techString.add("Vet   - " + techs.elementAt(UnitUtils.TECH_VET));
        techString.add("Elite - " + techs.elementAt(UnitUtils.TECH_ELITE));

        Pilot pilot = playerUnit.getPilot();

        if (pilot.getSkills().has(PilotSkill.AstechSkillID)) {
            techString.add(UnitUtils.techDescription(UnitUtils.TECH_PILOT));
        }

        if (Boolean.parseBoolean(mwclient.getserverConfigs("AllowCritRepairsForRewards")) && !salvage) {
            techString.add(UnitUtils.techDescription(UnitUtils.TECH_REWARD_POINTS));
        }

        techComboBox = new JComboBox<String>(techString);
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
            numberOfRetriesField.setToolTipText("<html>Number of times the assigned tech will try to finish the repair<br>will stop when repair is sucessful or you run out of money or tries<br>Set to -1 or infinite retries</html>");
            numberOfRetriesField.addKeyListener(this);
            techPanel.add(numberOfRetriesField);
            SpringLayoutHelper.setupSpringGrid(techPanel, 9);
        } else {
            SpringLayoutHelper.setupSpringGrid(techPanel, 8);
        }

        workHoursField.setEnabled(!salvage);
        numberOfRetriesField.setEnabled(!salvage);

        MasterPanel.add(ConfigPane);
        MasterPanel.add(techPanel);
        SpringLayoutHelper.setupSpringGrid(MasterPanel, 2, 1);
    }

    /**
     * This method sets the cost field with the cost of the repair based on the
     * crit and the tech doing the job.
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
            techCost = Integer.parseInt(mwclient.getserverConfigs(UnitUtils.techDescription(techType) + "TechRepairCost"));
            techCostWorkMod = techWorkMod;
        }

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UseRealRepairCosts"))) {
            armor = false;
            if (critSlot >= UnitUtils.LOC_FRONT_ARMOR) {
                armor = true;
            }
            double cost = UnitUtils.getPartCost(unit, critLocation, critSlot, armor, year);
            if (Boolean.parseBoolean(mwclient.getserverConfigs("UsePartsRepair"))) {
                cost = 0;
            }

            double costMod = Double.parseDouble(mwclient.getserverConfigs("RealRepairCostMod"));
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
                double cost = CUnit.getArmorCost(unit, mwclient, critSlot);
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
                double cost = CUnit.getArmorCost(unit, mwclient, critSlot);
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
                double cost = CUnit.getStructureCost(unit, mwclient);
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
                    totalCrits = UnitUtils.getNumberOfCrits(unit, cs) - UnitUtils.getNumberOfDamagedCrits(unit, critSlot, critLocation, armor);
                } else {
                    totalCrits = UnitUtils.getNumberOfDamagedCrits(unit, critSlot, critLocation, armor);
                }
                cost = CUnit.getCritCost(unit, mwclient, cs);
                totalCost = (int) (totalCrits * cost);
                totalCost += (int) (totalCrits * techCost);
                totalCost += techCost;
                totalCost += techCost * Math.abs(techWorkMod);
                cost = Math.max(1, totalCost);
                costField.setText(Integer.toString((int) cost));

            }// end Else
        }// end real repair cost else

        if (Boolean.parseBoolean(mwclient.getserverConfigs("AllowCritRepairsForRewards")) && (UnitUtils.techType((String) techComboBox.getSelectedItem()) == UnitUtils.TECH_REWARD_POINTS)) {
            double cost = totalCrits * Double.parseDouble(mwclient.getserverConfigs("RewardPointsForCritRepair"));

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
        int roll = UnitUtils.getTechRoll(unit, critLocation, critSlot, techType, armor, mwclient.getData().getHouseByName(mwclient.getPlayer().getHouse()).getTechLevel(), salvage);

        baseRollField.setText(Integer.toString(roll));
    }

    public void keyTyped(KeyEvent arg0) {
    }

    public void keyPressed(KeyEvent arg0) {
    }

    public void keyReleased(KeyEvent arg0) {

        if (arg0.getKeyCode() == KeyEvent.VK_ESCAPE) {
            mwclient.getPlayer().resetRepairs();
            super.dispose();
        }
        if (arg0.getComponent().equals(numberOfRetriesField)) {
            if (numberOfRetriesField.getValue().toString().length() != 0) {
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
            critLocation = ConfigPane.getSelectedIndex();
            JList<String> templist = (JList<String>) arg0.getComponent();
            selectedSlot = templist.getSelectedIndex();
            setCost();
            setBaseRoll();
            setWorkHours();
        }// end if JList
    }

    private void setWorkHours() {

        if ((critLocation < 0) || (critSlot < 0)) {
            return;
        }

        int baseLine = Integer.parseInt(mwclient.getserverConfigs("TimeForEachRepairPoint"));

        if (!armor) {
            CriticalSlot cs = unit.getCritical(critLocation, critSlot);
            int totalCrits = UnitUtils.getNumberOfCrits(unit, cs);
            baseLine *= totalCrits;
        }

        if (techType == UnitUtils.TECH_PILOT) {
            techType = UnitUtils.techType((String) techComboBox.getSelectedItem());
        }
        if (techType == UnitUtils.TECH_REWARD_POINTS) {
            baseLine = 1;
        }

        int rolls = UnitUtils.getTechRoll(unit, critLocation, critSlot, techType, armor, mwclient.getData().getHouseByName(mwclient.getPlayer().getHouse()).getTechLevel(), salvage) - 3;
        int maxCost = baseLine;

        baseLineCost = baseLine;
        techWorkMod = 0;

        Vector<Integer> tempVector = new Vector<Integer>(1, 1);

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
                roll = UnitUtils.getTechRoll(unit, critLocation, critSlot, techType - 1, armor, mwclient.getData().getHouseByName(mwclient.getPlayer().getHouse()).getTechLevel(), salvage);
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

        roll = UnitUtils.getTechRoll(unit, critLocation, critSlot, techType, armor, mwclient.getData().getHouseByName(mwclient.getPlayer().getHouse()).getTechLevel());

        baseRollField.setText(Integer.toString(roll + techWorkMod));
        setCost();
    }
}// end AdvancedRepairDialog.java
