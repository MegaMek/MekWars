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

package admin.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;

import client.MWClient;
import common.Equipment;
import common.util.MWLogger;
import common.util.SpringLayoutHelper;
import common.util.UnitUtils;
import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.TechConstants;
import megamek.common.WeaponType;

public final class ComponentDisplayDialog extends JDialog implements ActionListener {

    // store the client backlink for other things to use
    private static final long serialVersionUID = 8839724432360797850L;
    private MWClient mwclient = null;

    public final static int WEAPON_TYPE = 0;
    public final static int MISC_TYPE = 1;
    public final static int AMMO_TYPE = 2;
    public final static int AMMO_COSTS_TYPE = 3;

    private final static String okayCommand = "Add";
    private final static String cancelCommand = "Close";

    private String windowName = "Component Display Dialog";

    // BUTTONS
    private final JButton okayButton = new JButton("Ok");
    private final JButton cancelButton = new JButton("Close");

    // STOCK DIALOUG AND PANE
    private JDialog dialog;
    private JOptionPane pane;
    private JScrollPane MasterPanel = new JScrollPane();

    private int displayType = 0;
    
    // Text boxes
    JTabbedPane ConfigPane = new JTabbedPane();

    public ComponentDisplayDialog(MWClient c, int type) {
    	
        super(c.getMainFrame(), "Component Display Dialog", true);

        // save the client
        mwclient = c;

        // stored values.
        displayType = type;

        MWLogger.errLog("Year: " + mwclient.getserverConfigs("CampaignYear"));
        int year = Integer.parseInt(mwclient.getserverConfigs("CampaignYear"));

        
        // Set the tooltips and actions for dialouge buttons
        okayButton.setActionCommand(okayCommand);
        cancelButton.setActionCommand(cancelCommand);

        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);
        okayButton.setToolTipText("Save");
        cancelButton.setToolTipText("Exit without saving changes");

        ConfigPane = new JTabbedPane();

        // Pull data from the server.
        mwclient.getBlackMarketSettings();

        // CREATE THE PANELS

        if (displayType == WEAPON_TYPE) {
            loadWeaponPanel(year);
            windowName += " (Weapons)";
        } else if (displayType == AMMO_TYPE) {
            loadAmmoPanel(year);
            windowName += " (Ammo)";
        } else if (displayType == AMMO_COSTS_TYPE) {
            loadAmmoCostPanel(year);
            windowName += " (Ammo Costs)";
        } else {
            loadMiscPanel(year);
            windowName += " (Misc)";
        }

        for (int pos = ConfigPane.getComponentCount() - 1; pos >= 0; pos--) {
            JPanel panel = (JPanel) ConfigPane.getComponent(pos);
            findAndPopulateTextAndCheckBoxes(panel);

        }

        // Set the user's options
        Object[] options = { okayButton, cancelButton };

        Dimension dim = new Dimension(100, 200);

        ConfigPane.setMaximumSize(dim);

        // Create the pane containing the buttons
        pane = new JOptionPane(ConfigPane, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options, null);

        pane.setMaximumSize(dim);
        MasterPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        MasterPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        MasterPanel.setMaximumSize(dim);

        // Create the main dialog and set the default button
        dialog = pane.createDialog(MasterPanel, windowName);
        dialog.getRootPane().setDefaultButton(cancelButton);

        dialog.setMaximumSize(dim);
        dialog.setLocationRelativeTo(mwclient.getMainFrame());
        // Show the dialog and get the user's input
        dialog.setModal(true);
        dialog.pack();
        dialog.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (command.equals(okayCommand)) {
            for (int pos = ConfigPane.getComponentCount() - 1; pos >= 0; pos--) {
                JPanel panel = (JPanel) ConfigPane.getComponent(pos);
                findAndSaveConfigs(panel);
            }

            transmitSettings();
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminSaveBlackMarketConfigs");
            dialog.dispose();
            return;
        } else if (command.equals(cancelCommand)) {
            // mwclient.getPlayer().resetRepairs();
            dialog.dispose();
        }
    }

    private void loadWeaponPanel(int year) {
        loadWeaponPanelType(TechConstants.T_INTRO_BOXSET, year);
        loadWeaponPanelType(TechConstants.T_IS_TW_NON_BOX, year);
        loadWeaponPanelType(TechConstants.T_IS_ADVANCED, year);
        loadWeaponPanelType(TechConstants.T_IS_EXPERIMENTAL, year);
        loadWeaponPanelType(TechConstants.T_IS_UNOFFICIAL, year);
        loadWeaponPanelType(TechConstants.T_CLAN_TW, year);
        loadWeaponPanelType(TechConstants.T_CLAN_ADVANCED, year);
        loadWeaponPanelType(TechConstants.T_CLAN_EXPERIMENTAL, year);
        loadWeaponPanelType(TechConstants.T_CLAN_UNOFFICIAL, year);
    }

    private void loadAmmoPanel(int year) {
        loadAmmoPanelType(TechConstants.T_INTRO_BOXSET, year);
        loadAmmoPanelType(TechConstants.T_IS_TW_NON_BOX, year);
        loadAmmoPanelType(TechConstants.T_IS_ADVANCED, year);
        loadAmmoPanelType(TechConstants.T_IS_EXPERIMENTAL, year);
        loadAmmoPanelType(TechConstants.T_IS_UNOFFICIAL, year);
        loadAmmoPanelType(TechConstants.T_CLAN_TW, year);
        loadAmmoPanelType(TechConstants.T_CLAN_ADVANCED, year);
        loadAmmoPanelType(TechConstants.T_CLAN_EXPERIMENTAL, year);
        loadAmmoPanelType(TechConstants.T_CLAN_UNOFFICIAL, year);
    }

    private void loadAmmoCostPanel(int year) {
        loadAmmoCostPanelType(TechConstants.T_INTRO_BOXSET, year);
        loadAmmoCostPanelType(TechConstants.T_IS_TW_NON_BOX, year);
        loadAmmoCostPanelType(TechConstants.T_IS_ADVANCED, year);
        loadAmmoCostPanelType(TechConstants.T_IS_EXPERIMENTAL, year);
        loadAmmoCostPanelType(TechConstants.T_IS_UNOFFICIAL, year);
        loadAmmoCostPanelType(TechConstants.T_CLAN_TW, year);
        loadAmmoCostPanelType(TechConstants.T_CLAN_ADVANCED, year);
        loadAmmoCostPanelType(TechConstants.T_CLAN_EXPERIMENTAL, year);
        loadAmmoCostPanelType(TechConstants.T_CLAN_UNOFFICIAL, year);
    }

    private void loadAmmoPanelType(int tech, int year) {
        Enumeration<EquipmentType> list = EquipmentType.getAllTypes();
        TreeMap<String, AmmoType> equipmentSort = new TreeMap<String, AmmoType>();

        int count = 0;
        int tabNumber = 0;
        JPanel panel = new JPanel(new SpringLayout());
        JTextField textField = null;
        Dimension dim = new Dimension(50, 10);
        JPanel masterBox = new JPanel();
        masterBox.setLayout(new BoxLayout(masterBox, BoxLayout.X_AXIS));
        panel.add(new JLabel("Component"));
        panel.add(new JLabel("Min. Cost"));
        panel.add(new JLabel("Max. Cost"));
        panel.add(new JLabel("Min. Parts"));
        panel.add(new JLabel("Max. Parts"));
        panel.add(new JLabel("Component"));
        panel.add(new JLabel("Min. Cost"));
        panel.add(new JLabel("Max. Cost"));
        panel.add(new JLabel("Min. Parts"));
        panel.add(new JLabel("Max. Parts"));

        String tabPrefix = TechConstants.T_NAMES[tech] + "-";

        while (list.hasMoreElements()) {
            EquipmentType eq = list.nextElement();

            if (!(eq instanceof AmmoType)) {
                continue;
            }

            if (((AmmoType) eq).getTechLevel(year) != tech) {
                // This is done for Unknown and all tech level. Make them all IS
                // Level 1
                if (tech == TechConstants.T_IS_TW_NON_BOX && ((AmmoType) eq).getTechLevel(year) > tech) {
                    continue;
                }
                if (tech != TechConstants.T_IS_TW_NON_BOX) {
                    continue;
                }

            }

            equipmentSort.put(eq.getInternalName(), (AmmoType) eq);

        }

        for (AmmoType eq : equipmentSort.values()) {
            String name = eq.getName();
            String intName = eq.getInternalName();
            panel.add(new JLabel(name));

            textField = new JTextField("0");
            textField.setName(intName + "|mincost");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The min. cost for this item on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|maxcost");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The max. cost for this item on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|minparts");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The min. number of items that will be on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|maxparts");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The max. number of items that will be on the BM");
            panel.add(textField);

            if (++count % 40 == 0) {
                panel.setAutoscrolls(true);
                SpringLayoutHelper.setupSpringGrid(panel, 10);
                masterBox.add(panel);

                tabNumber++;
                ConfigPane.addTab(tabPrefix + tabNumber, null, panel, tabPrefix + tabNumber);
                panel = new JPanel(new SpringLayout());
                panel.add(new JLabel("Component"));
                panel.add(new JLabel("Min. Cost"));
                panel.add(new JLabel("Max. Cost"));
                panel.add(new JLabel("Min. Parts"));
                panel.add(new JLabel("Max. Parts"));
                panel.add(new JLabel("Component"));
                panel.add(new JLabel("Min. Cost"));
                panel.add(new JLabel("Max. Cost"));
                panel.add(new JLabel("Min. Parts"));
                panel.add(new JLabel("Max. Parts"));
            }
        }

        if (panel.getComponentCount() > 0) {
            tabNumber++;
            SpringLayoutHelper.setupSpringGrid(panel, 10);
            ConfigPane.addTab(tabPrefix + tabNumber, null, panel, tabPrefix + tabNumber);
        }

        MasterPanel.add(ConfigPane);

    }

    private void loadAmmoCostPanelType(int tech, int year) {
        Enumeration<EquipmentType> list = EquipmentType.getAllTypes();

        TreeMap<String, AmmoType> equipmentSort = new TreeMap<String, AmmoType>();

        int count = 0;
        int tabNumber = 0;
        JPanel panel = new JPanel(new SpringLayout());
        JTextField textField = null;
        Dimension dim = new Dimension(50, 10);
        JPanel masterBox = new JPanel();
        masterBox.setLayout(new BoxLayout(masterBox, BoxLayout.X_AXIS));
        panel.add(new JLabel("Component"));
        panel.add(new JLabel("Cost"));
        panel.add(new JLabel("Component"));
        panel.add(new JLabel("Cost"));
        panel.add(new JLabel("Component"));
        panel.add(new JLabel("Cost"));
        panel.add(new JLabel("Component"));
        panel.add(new JLabel("Cost"));

        String tabPrefix = TechConstants.T_NAMES[tech] + "-";

        while (list.hasMoreElements()) {
            EquipmentType eq = list.nextElement();

            if (!(eq instanceof AmmoType)) {
                continue;
            }

            if (((AmmoType) eq).getTechLevel(year) != tech) {
                // This is done for Unknown and all tech level. Make them all IS
                // Level 1
                if (tech == TechConstants.T_IS_TW_NON_BOX && ((AmmoType) eq).getTechLevel(year) > tech) {
                    continue;
                }
                if (tech != TechConstants.T_IS_TW_NON_BOX) {
                    continue;
                }

            }

            equipmentSort.put(eq.getInternalName(), (AmmoType) eq);

        }

        for (AmmoType eq : equipmentSort.values()) {
            String name = eq.getName();
            String intName = eq.getInternalName();
            panel.add(new JLabel(name));

            textField = new JTextField("0");
            textField.setName(intName + "|mincost");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The cost for a shot of " + name + " ammo.");
            panel.add(textField);

            if (++count % 40 == 0) {
                panel.setAutoscrolls(true);
                SpringLayoutHelper.setupSpringGrid(panel, 8);
                masterBox.add(panel);

                tabNumber++;
                ConfigPane.addTab(tabPrefix + tabNumber, null, panel, tabPrefix + tabNumber);
                panel = new JPanel(new SpringLayout());
                panel.add(new JLabel("Component"));
                panel.add(new JLabel("Cost"));
                panel.add(new JLabel("Component"));
                panel.add(new JLabel("Cost"));
                panel.add(new JLabel("Component"));
                panel.add(new JLabel("Cost"));
                panel.add(new JLabel("Component"));
                panel.add(new JLabel("Cost"));
            }
        }

        if (panel.getComponentCount() > 0) {
            tabNumber++;
            SpringLayoutHelper.setupSpringGrid(panel, 8);
            ConfigPane.addTab(tabPrefix + tabNumber, null, panel, tabPrefix + tabNumber);
        }

        MasterPanel.add(ConfigPane);

    }

    private void loadWeaponPanelType(int tech, int year) {
        Enumeration<EquipmentType> list = EquipmentType.getAllTypes();
        TreeMap<String, WeaponType> equipmentSort = new TreeMap<String, WeaponType>();

        int count = 0;
        int tabNumber = 0;
        JPanel panel = new JPanel(new SpringLayout());
        JTextField textField = null;
        Dimension dim = new Dimension(50, 10);

        panel.add(new JLabel("Component"));
        panel.add(new JLabel("Min. Cost"));
        panel.add(new JLabel("Max. Cost"));
        panel.add(new JLabel("Min. Parts"));
        panel.add(new JLabel("Max. Parts"));
        panel.add(new JLabel("Component"));
        panel.add(new JLabel("Min. Cost"));
        panel.add(new JLabel("Max. Cost"));
        panel.add(new JLabel("Min. Parts"));
        panel.add(new JLabel("Max. Parts"));

        String tabPrefix = TechConstants.T_NAMES[tech] + "-";

        while (list.hasMoreElements()) {
            EquipmentType eq = list.nextElement();

            if (!(eq instanceof WeaponType)) {
                continue;
            }

            if (((WeaponType) eq).getTechLevel(year) != tech) {
                // This is done for Unknown and all tech level. Make them all IS
                // Level 1
                if (tech == TechConstants.T_IS_TW_NON_BOX && ((WeaponType) eq).getTechLevel(year) > tech) {
                    continue;
                }
                if (tech != TechConstants.T_IS_TW_NON_BOX) {
                    continue;
                }

            }

            equipmentSort.put(eq.getInternalName(), (WeaponType) eq);

        }

        for (WeaponType eq : equipmentSort.values()) {
            String name = eq.getName();
            if (eq.hasFlag(WeaponType.F_BA_WEAPON)) {
                name += " (BA)";
            }
            String intName = eq.getInternalName();
            panel.add(new JLabel(name));

            textField = new JTextField("0");
            textField.setName(intName + "|mincost");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The min. cost for this item on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|maxcost");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The max. cost for this item on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|minparts");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The min. number of items that will be on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|maxparts");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The max. number of items that will be on the BM");
            panel.add(textField);

            if (++count % 40 == 0) {
                SpringLayoutHelper.setupSpringGrid(panel, 10);
                tabNumber++;
                ConfigPane.addTab(tabPrefix + tabNumber, null, panel, tabPrefix + tabNumber);

                panel = new JPanel(new SpringLayout());
                panel.add(new JLabel("Component"));
                panel.add(new JLabel("Min. Cost"));
                panel.add(new JLabel("Max. Cost"));
                panel.add(new JLabel("Min. Parts"));
                panel.add(new JLabel("Max. Parts"));
                panel.add(new JLabel("Component"));
                panel.add(new JLabel("Min. Cost"));
                panel.add(new JLabel("Max. Cost"));
                panel.add(new JLabel("Min. Parts"));
                panel.add(new JLabel("Max. Parts"));
            }
        }

        if (panel.getComponentCount() > 0) {
            tabNumber++;
            SpringLayoutHelper.setupSpringGrid(panel, 10);
            ConfigPane.addTab(tabPrefix + tabNumber, null, panel, tabPrefix + tabNumber);
        }

        MasterPanel.add(ConfigPane);

    }

    private void loadMiscPanelType(int tech, int year) {
        Enumeration<EquipmentType> list = EquipmentType.getAllTypes();
        TreeMap<String, MiscType> equipmentSort = new TreeMap<String, MiscType>();

        int count = 0;
        int tabNumber = 0;
        JPanel panel = new JPanel(new SpringLayout());
        JTextField textField = null;
        Dimension dim = new Dimension(50, 10);

        panel.add(new JLabel("Component"));
        panel.add(new JLabel("Min. Cost"));
        panel.add(new JLabel("Max. Cost"));
        panel.add(new JLabel("Min. Parts"));
        panel.add(new JLabel("Max. Parts"));
        panel.add(new JLabel("Component"));
        panel.add(new JLabel("Min. Cost"));
        panel.add(new JLabel("Max. Cost"));
        panel.add(new JLabel("Min. Parts"));
        panel.add(new JLabel("Max. Parts"));

        String tabPrefix = TechConstants.T_NAMES[tech] + "-";

        if (tech == TechConstants.T_ALL) {
            String name = Mech.systemNames[Mech.SYSTEM_LIFE_SUPPORT];
            String intName = Mech.systemNames[Mech.SYSTEM_LIFE_SUPPORT];

            panel.add(new JLabel(name));

            textField = new JTextField("0");
            textField.setName(intName + "|mincost");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The min. cost for this item on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|maxcost");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The max. cost for this item on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|minparts");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The min. number of items that will be on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|maxparts");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The max. number of items that will be on the BM");
            panel.add(textField);

            name = Mech.systemNames[Mech.SYSTEM_SENSORS];
            intName = Mech.systemNames[Mech.SYSTEM_SENSORS];

            panel.add(new JLabel(name));

            textField = new JTextField("0");
            textField.setName(intName + "|mincost");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The min. cost for this item on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|maxcost");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The max. cost for this item on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|minparts");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The min. number of items that will be on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|maxparts");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The max. number of items that will be on the BM");
            panel.add(textField);

            name = "Actuator";
            intName = "Actuator";

            panel.add(new JLabel(name));

            textField = new JTextField("0");
            textField.setName(intName + "|mincost");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The min. cost for this item on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|maxcost");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The max. cost for this item on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|minparts");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The min. number of items that will be on the BM");
            panel.add(textField);

            textField = new JTextField("0");
            textField.setName(intName + "|maxparts");
            textField.setMaximumSize(dim);
            textField.setToolTipText("The max. number of items that will be on the BM");
            panel.add(textField);

            for (int pos = 0; pos <= Mech.GYRO_HEAVY_DUTY; pos++) {
                name = Mech.getGyroTypeString(pos);
                intName = Mech.getGyroTypeString(pos);

                panel.add(new JLabel(name));

                textField = new JTextField("0");
                textField.setName(intName + "|mincost");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The min. cost for this item on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|maxcost");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The max. cost for this item on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|minparts");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The min. number of items that will be on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|maxparts");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The max. number of items that will be on the BM");
                panel.add(textField);
            }

            for (int pos = 0; pos <= Mech.COCKPIT_DUAL; pos++) {
                name = Mech.getCockpitTypeString(pos);
                intName = Mech.getCockpitTypeString(pos);

                panel.add(new JLabel(name));

                textField = new JTextField("0");
                textField.setName(intName + "|mincost");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The min. cost for this item on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|maxcost");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The max. cost for this item on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|minparts");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The min. number of items that will be on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|maxparts");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The max. number of items that will be on the BM");
                panel.add(textField);
            }

            for (int pos = 0; pos <= UnitUtils.CLAN_XXL_ENGINE; pos++) {
                name = UnitUtils.ENGINE_TECH_STRING[pos];
                intName = UnitUtils.ENGINE_TECH_STRING[pos];

                panel.add(new JLabel(name));

                textField = new JTextField("0");
                textField.setName(intName + "|mincost");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The min. cost for this item on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|maxcost");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The max. cost for this item on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|minparts");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The min. number of items that will be on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|maxparts");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The max. number of items that will be on the BM");
                panel.add(textField);
            }

            SpringLayoutHelper.setupSpringGrid(panel, 10);
            ConfigPane.addTab(tabPrefix, null, panel, tabPrefix);
        } else {
            while (list.hasMoreElements()) {
                EquipmentType eq = list.nextElement();

                if (!(eq instanceof MiscType)) {
                    continue;
                }

                if (((MiscType) eq).getTechLevel(year) != tech) {
                    // This is done for Unknown and all tech level. Make them
                    // all IS Level 1
                    if (tech == TechConstants.T_IS_TW_NON_BOX && ((MiscType) eq).getTechLevel(year) > tech) {
                        continue;
                    }
                    if (tech != TechConstants.T_IS_TW_NON_BOX) {
                        continue;
                    }

                }

                equipmentSort.put(eq.getInternalName(), (MiscType) eq);

            }

            for (MiscType eq : equipmentSort.values()) {
                String name = eq.getName();
                String intName = eq.getInternalName();
                if (name.equalsIgnoreCase("standard")) {
                    name = "Armor (STD)";
                    intName = "Armor (STD)";
                }
                panel.add(new JLabel(name));

                textField = new JTextField("0");
                textField.setName(intName + "|mincost");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The min. cost for this item on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|maxcost");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The max. cost for this item on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|minparts");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The min. number of items that will be on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|maxparts");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The max. number of items that will be on the BM");
                panel.add(textField);

                if (name.equalsIgnoreCase("Armor (STD)")) {
                    count++;
                    name = "IS (STD)";
                    intName = "IS (STD)";
                    panel.add(new JLabel(name));

                    textField = new JTextField("0");
                    textField.setName(intName + "|mincost");
                    textField.setMaximumSize(dim);
                    textField.setToolTipText("The min. cost for this item on the BM");
                    panel.add(textField);

                    textField = new JTextField("0");
                    textField.setName(intName + "|maxcost");
                    textField.setMaximumSize(dim);
                    textField.setToolTipText("The max. cost for this item on the BM");
                    panel.add(textField);

                    textField = new JTextField("0");
                    textField.setName(intName + "|minparts");
                    textField.setMaximumSize(dim);
                    textField.setToolTipText("The min. number of items that will be on the BM");
                    panel.add(textField);

                    textField = new JTextField("0");
                    textField.setName(intName + "|maxparts");
                    textField.setMaximumSize(dim);
                    textField.setToolTipText("The max. number of items that will be on the BM");
                    panel.add(textField);
                }
                if (++count % 40 == 0) {
                    SpringLayoutHelper.setupSpringGrid(panel, 10);

                    tabNumber++;
                    ConfigPane.addTab(tabPrefix + tabNumber, null, panel, tabPrefix + tabNumber);

                    panel = new JPanel(new SpringLayout());
                    panel.add(new JLabel("Component"));
                    panel.add(new JLabel("Min. Cost"));
                    panel.add(new JLabel("Max. Cost"));
                    panel.add(new JLabel("Min. Parts"));
                    panel.add(new JLabel("Max. Parts"));
                    panel.add(new JLabel("Component"));
                    panel.add(new JLabel("Min. Cost"));
                    panel.add(new JLabel("Max. Cost"));
                    panel.add(new JLabel("Min. Parts"));
                    panel.add(new JLabel("Max. Parts"));
                }
            }

            if (tech == TechConstants.T_IS_TW_NON_BOX) {
                String name = "Ammo Bin";
                String intName = "Ammo Bin";

                panel.add(new JLabel(name));

                textField = new JTextField("0");
                textField.setName(intName + "|mincost");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The min. cost for this item on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|maxcost");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The max. cost for this item on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|minparts");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The min. number of items that will be on the BM");
                panel.add(textField);

                textField = new JTextField("0");
                textField.setName(intName + "|maxparts");
                textField.setMaximumSize(dim);
                textField.setToolTipText("The max. number of items that will be on the BM");
                panel.add(textField);

            }

            if (panel.getComponentCount() > 0) {
                tabNumber++;
                SpringLayoutHelper.setupSpringGrid(panel, 10);
                ConfigPane.addTab(tabPrefix + tabNumber, null, panel, tabPrefix + tabNumber);
            }
        }
        MasterPanel.add(ConfigPane);

    }

    private void loadMiscPanel(int year) {
        loadMiscPanelType(TechConstants.T_INTRO_BOXSET, year);
        loadMiscPanelType(TechConstants.T_IS_TW_NON_BOX, year);
        loadMiscPanelType(TechConstants.T_IS_ADVANCED, year);
        loadMiscPanelType(TechConstants.T_IS_EXPERIMENTAL, year);
        loadMiscPanelType(TechConstants.T_IS_UNOFFICIAL, year);
        loadMiscPanelType(TechConstants.T_CLAN_TW, year);
        loadMiscPanelType(TechConstants.T_CLAN_ADVANCED, year);
        loadMiscPanelType(TechConstants.T_CLAN_EXPERIMENTAL, year);
        loadMiscPanelType(TechConstants.T_CLAN_UNOFFICIAL, year);
        loadMiscPanelType(TechConstants.T_ALL, year);
    }

    /**
     * This Method tunnels through all of the panels to find the textfields and
     * checkboxes. Once it find one it grabs the Name() param of the object and
     * uses that to find out what the setting should be from the
     * mwclient.getserverConfigs() method.
     * 
     * @param panel
     */
    public void findAndPopulateTextAndCheckBoxes(JPanel panel) {
        String key = null;

        DecimalFormat format = new DecimalFormat("#.##");
        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            if (field instanceof JPanel) {
                findAndPopulateTextAndCheckBoxes((JPanel) field);
            } else if (field instanceof JTextField) {
                JTextField textBox = (JTextField) field;

                key = textBox.getName();
                if (key == null) {
                    continue;
                }

                textBox.setMaximumSize(new Dimension(100, 10));
                try {
                    StringTokenizer keys = new StringTokenizer(key, "|");

                    Equipment equipment = mwclient.getBlackMarketEquipmentList().get(keys.nextToken());

                    if (equipment == null) {
                        textBox.setText("0");
                    } else {
                        String type = keys.nextToken();

                        if (type.equalsIgnoreCase("mincost")) {
                            textBox.setText(format.format(equipment.getMinCost()));
                        } else if (type.equalsIgnoreCase("maxcost")) {
                            textBox.setText(format.format(equipment.getMaxCost()));
                        } else if (type.equalsIgnoreCase("minparts")) {
                            textBox.setText(Integer.toString(equipment.getMinProduction()));
                        } else {
                            textBox.setText(Integer.toString(equipment.getMaxProduction()));
                        }

                    }
                } catch (Exception ex) {
                    textBox.setText("N/A");
                }
            }
        }
    }

    public void transmitSettings() {

        for (String key : mwclient.getBlackMarketEquipmentList().keySet()) {
            Equipment bme = mwclient.getBlackMarketEquipmentList().get(key);

            if (bme.isUpdated()) {
                mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminSetBlackMarketSetting#" + key + "#" + bme.getMinCost() + "#" + bme.getMaxCost() + "#" + bme.getMinProduction() + "#" + bme.getMaxProduction());
            }
        }

    }

    /**
     * This method will tunnel through all of the panels of the config UI to
     * find any changed text fields. The data is saved to the Equipment Hashmap
     * 
     * @param panel
     */
    public void findAndSaveConfigs(JPanel panel) {
        String key = null;
        String value = null;
        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            // found another JPanel keep digging!
            if (field instanceof JPanel) {
                findAndSaveConfigs((JPanel) field);
            } else if (field instanceof JTextField) {
                JTextField textBox = (JTextField) field;

                value = textBox.getText().replaceAll(",", ".").trim();
                key = textBox.getName();

                if (key == null || value == null) {
                    continue;
                }

                StringTokenizer keys = new StringTokenizer(key, "|");

                String internalName = keys.nextToken();

                Equipment equipment = mwclient.getBlackMarketEquipmentList().get(internalName);

                if (equipment == null) {
                    equipment = new Equipment();
                    equipment.setEquipmentInternalName(key);
                }

                String fieldKey = keys.nextToken();

                if (displayType == AMMO_COSTS_TYPE) {
                    if (fieldKey.equalsIgnoreCase("mincost")) {
                        double amount = Double.parseDouble(value);
                        if (amount <= 0) {
                            equipment.setMinCost(-1);
                            equipment.setMaxCost(-1);
                            equipment.setMaxProduction(0);
                            equipment.setMinProduction(0);
                        } else {
                            equipment.setMinCost(amount);
                            equipment.setMaxCost(amount);
                            equipment.setMaxProduction(1);
                            equipment.setMinProduction(1);
                        }
                    }

                } else {
                    if (fieldKey.equalsIgnoreCase("mincost")) {
                        equipment.setMinCost(Double.parseDouble(value));
                    } else if (fieldKey.equalsIgnoreCase("maxcost")) {
                        equipment.setMaxCost(Double.parseDouble(value));
                    } else if (fieldKey.equalsIgnoreCase("minparts")) {
                        equipment.setMinProduction(Integer.parseInt(value));
                    } else {
                        equipment.setMaxProduction(Integer.parseInt(value));
                    }
                }
                mwclient.getBlackMarketEquipmentList().put(internalName, equipment);

                // reduce bandwidth only send things that have changed.
                /*
                 * if ( !mwclient.getserverConfigs(key).equalsIgnoreCase(value)
                 * ) mwclient.sendChat(MWClient.CAMPAIGN_PREFIX+
                 * "c AdminChangeBlackMarketConfig#"+key+"#"+value+"#CONFIRM");
                 */
            }
        }

    }

}// end ComponentDisplayDialog.java
