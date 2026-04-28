/*
 * MekWars - Copyright (C) 2004, 2005
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
 * MechSelectorJDialog.java - Copyright (C) 2002,2004 Josh Yockey
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/*
 * Thanks to the MegaMek Crew for the Code base
 * Modified by Torren (Jason Tighe)
 * From Megamek.client.MechSelectorDialgo.java
 */

package mekwars.common.gui.dialogs;

import java.io.Serial;

import megamek.client.ui.dialogs.UnitFailureDialog;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.dialogs.unitSelectorDialogs.ConfigurableMekViewPanel;
import megamek.common.TechConstants;
import megamek.common.comparators.MekSummaryComparator;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.CMainFrame;
import mekwars.common.gui.MekInfo;
import mekwars.common.util.MWLogger;
import mekwars.common.util.SpringLayoutHelper;
import mekwars.common.util.UnitUtils;

/*
 * Allows a user to sort through a list of MechSummaries and select one
 */

public class UnitViewerDialog extends javax.swing.JFrame
      implements java.awt.event.ActionListener, java.awt.event.KeyListener, javax.swing.event.ListSelectionListener,
                 Runnable, java.awt.event.WindowListener, java.awt.event.ItemListener {

    public static final int UNIT_VIEWER = 0;
    public static final int OMNI_VARIANT_SELECTOR = 1;
    public static final int UNIT_SELECTOR = 2;
    public static final int UNIT_RESEARCH = 3;
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -7210333306969855153L;
    // how long after a key is typed does a new search begin
    private final static int KEY_TIMEOUT = 1000;
    private static final String SPACES = "                        ";
    // };
    // these indices should match up with the static values in the
    // MekSummaryComparator
    private final String[] saSorts =
          { "Name", "Ref", "Weight", "BV" };// , "Year"
    // frame which owns the dialog
    private final CMainFrame clientGUI;
    private final UnitLoadingDialog unitLoadingDialog;
    private final javax.swing.JComboBox<String> chType = new javax.swing.JComboBox<>();
    private final javax.swing.JComboBox<String> chUnitType = new javax.swing.JComboBox<>();
    private final javax.swing.JComboBox<String> chWeightClass = new javax.swing.JComboBox<>();
    private final javax.swing.JComboBox<String> chSort = new javax.swing.JComboBox<>();
    private final javax.swing.JPanel textBoxSpring = new javax.swing.JPanel(new javax.swing.SpringLayout());
    private final javax.swing.JPanel springHolder = new javax.swing.JPanel(new javax.swing.SpringLayout());
    private final javax.swing.JPanel fluffBoxSpring = new javax.swing.JPanel(new javax.swing.SpringLayout());
    private final javax.swing.JButton bCancel = new javax.swing.JButton("Close");
    private final javax.swing.JButton bSelect = new javax.swing.JButton("Select");
    private final javax.swing.JTextPane mekViewLeft;
    private final javax.swing.JTextPane mekViewRight;
    private final javax.swing.JTextPane unitFluff;
    private final javax.swing.JPanel pUpper = new javax.swing.JPanel();
    private final IClient client;
    private final int viewerType;
    private final boolean viewFluff;
    private final javax.swing.JPanel m_pOpenAdvanced = new javax.swing.JPanel();
    private final javax.swing.JButton m_bToggleAdvanced = new javax.swing.JButton("< Show Advanced Search >");
    private final javax.swing.JComboBox<String> m_cWalk = new javax.swing.JComboBox<>();
    private final javax.swing.JTextField m_tWalk = new javax.swing.JTextField(2);
    private final javax.swing.JComboBox<String> m_cJump = new javax.swing.JComboBox<>();
    private final javax.swing.JTextField m_tJump = new javax.swing.JTextField(2);
    private final javax.swing.JComboBox<String> m_cArmor = new javax.swing.JComboBox<>();
    private final javax.swing.JTextField m_tWeapons1 = new javax.swing.JTextField(2);

    // private String selectedUnit = null;
    private final javax.swing.JComboBox<String> m_cWeapons1 = new javax.swing.JComboBox<>();
    private final javax.swing.JComboBox<String> m_cOrAnd = new javax.swing.JComboBox<>();
    private final javax.swing.JTextField m_tWeapons2 = new javax.swing.JTextField(2);
    private final javax.swing.JComboBox<String> m_cWeapons2 = new javax.swing.JComboBox<>();
    private final javax.swing.JCheckBox m_chkEquipment = new javax.swing.JCheckBox();
    private final javax.swing.JComboBox<String> m_cEquipment = new javax.swing.JComboBox<>();
    private final javax.swing.JButton m_bSearch = new javax.swing.JButton("Search");
    private final javax.swing.JButton m_bReset = new javax.swing.JButton("Reset");
    private final javax.swing.JLabel m_lCount = new javax.swing.JLabel();
    javax.swing.DefaultListModel<String> defaultModel;
    javax.swing.ListSelectionModel listSelectionModel;
    javax.swing.JList<String> mechList;
    javax.swing.JScrollPane listScrollPane = null;
    javax.swing.JScrollPane leftScrollPane = null;
    javax.swing.JScrollPane rightScrollPane = null;
    javax.swing.JScrollPane fluffScrollPane = null;
    private MekSummary[] meksCurrent;
    private StringBuilder m_sbSearch = new StringBuilder();
    private long m_nLastSearch = 0;
    private javax.swing.JPanel pPreview = new javax.swing.JPanel();
    private javax.swing.JPanel m_pSouthParams = new javax.swing.JPanel();
    private int m_count;
    private int m_old_nType;
    private int m_old_nUnitType;

    public UnitViewerDialog(CMainFrame cMainFrame, UnitLoadingDialog uld, IClient client, int viewer) {
        super("Unit Viewer");

        viewerType = viewer;
        if (viewerType == mekwars.common.gui.dialogs.UnitViewerDialog.OMNI_VARIANT_SELECTOR) {
            setTitle("Omni Variant Selector");
        } else if (viewerType == mekwars.common.gui.dialogs.UnitViewerDialog.UNIT_SELECTOR) {
            setTitle("Unit Selector");
        }

        // save params
        clientGUI = cMainFrame;
        unitLoadingDialog = uld;
        this.client = client;
        viewFluff = Boolean.parseBoolean(client.getConfigParam("VIEWFLUFF"));

        // construct 2 text boxes
        mekViewLeft = new javax.swing.JTextPane();
        mekViewLeft.setContentType("text/html");
        mekViewRight = new javax.swing.JTextPane();
        mekViewRight.setContentType("text/html");
        unitFluff = new javax.swing.JTextPane();
        unitFluff.setContentType("text/html");

        // construct a model and list
        defaultModel = new javax.swing.DefaultListModel<>();
        mechList = new javax.swing.JList<>(defaultModel);
        listSelectionModel = mechList.getSelectionModel();
        mechList.setVisibleRowCount(17);// give the list same number of rows as
        // the text boxes
        listSelectionModel.addListSelectionListener(this);

        // place the list and text boxes in scroll panes
        listScrollPane = new javax.swing.JScrollPane(mechList);
        leftScrollPane = new javax.swing.JScrollPane(mekViewLeft);
        rightScrollPane = new javax.swing.JScrollPane(mekViewRight);
        fluffScrollPane = new javax.swing.JScrollPane(unitFluff);

        // set list/scroll options
        listScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        listScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        leftScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        rightScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        fluffScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        fluffScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        fluffScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        mechList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        // set fonts
        mekViewLeft.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 11));
        mekViewRight.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 11));
        mechList.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 11));

        for (String saSort : saSorts) {
            chSort.addItem(saSort);
        }

        String sort = client.getConfigParam("UNITVIEWERSORT");
        chSort.setSelectedItem(sort);

        // set up the upper panel (combo boxes, preview image)
        pPreview = new MekInfo(client);
        pPreview.setVisible(false);
        pPreview.setMinimumSize(new java.awt.Dimension(86, 74));
        pPreview.setMaximumSize(new java.awt.Dimension(86, 74));
        // pUpper.setLayout(new FlowLayout(FlowLayout.CENTER));
        pUpper.setLayout(new java.awt.BorderLayout());
        javax.swing.JPanel pParams = new javax.swing.JPanel();
        pUpper.add(pParams, java.awt.BorderLayout.WEST);
        pUpper.add(pPreview, java.awt.BorderLayout.CENTER);
        pUpper.add(m_pSouthParams, java.awt.BorderLayout.SOUTH);

        // lay out the combo boxes
        pParams.setLayout(new java.awt.GridLayout(4, 2));
        javax.swing.JLabel labelWeightClass = new javax.swing.JLabel("Class: ", javax.swing.SwingConstants.RIGHT);
        pParams.add(labelWeightClass);
        pParams.add(chWeightClass);
        javax.swing.JLabel labelType = new javax.swing.JLabel("Tech: ", javax.swing.SwingConstants.RIGHT);
        pParams.add(labelType);
        pParams.add(chType);
        javax.swing.JLabel labelUnitType = new javax.swing.JLabel("Type: ", javax.swing.SwingConstants.RIGHT);
        pParams.add(labelUnitType);
        pParams.add(chUnitType);
        javax.swing.JLabel labelSort = new javax.swing.JLabel("Sort: ", javax.swing.SwingConstants.RIGHT);
        pParams.add(labelSort);
        pParams.add(chSort);
        pParams.doLayout();

        populateChoices();
        populateJComboBoxs();
        buildSouthParams(false);

        setLocationRelativeTo(client.getMainFrame());
        getContentPane().add(springHolder);

        clearMechPreview();
        setSize(785, 560);
        setResizable(false);

        // add all the listeners
        chWeightClass.addItemListener(this);
        chType.addItemListener(this);
        chUnitType.addItemListener(this);
        chSort.addItemListener(this);
        mechList.addListSelectionListener(this);
        mechList.addKeyListener(this);
        bCancel.addActionListener(this);
        bSelect.addActionListener(this);
        m_bSearch.addActionListener(this);
        m_bReset.addActionListener(this);
        m_bToggleAdvanced.addActionListener(this);
        addWindowListener(this);
    }

    private void populateChoices() {

        for (int i = 0; i < EntityWeightClass.SIZE; i++) {
            chWeightClass.addItem(EntityWeightClass.getClassName(i));
        }
        chWeightClass.addItem("All"); //$NON-NLS-1$
        chWeightClass.setSelectedIndex(0);

        for (int i = 0; i < TechConstants.SIZE; i++) {
            chType.addItem(TechConstants.getLevelDisplayableName(i));
        }
        chType.setSelectedIndex(0);

        for (int i = 0; i < UnitType.SIZE; i++) {
            chUnitType.addItem(UnitType.getTypeDisplayableName(i));
        }
        chUnitType.addItem("All"); //$NON-NLS-1$
        chUnitType.setSelectedIndex(0);

        m_cWalk.addItem("At Least");
        m_cWalk.addItem("Equal To");
        m_cWalk.addItem("No More Than");
        m_cJump.addItem("At Least");
        m_cJump.addItem("Equal To");
        m_cJump.addItem("No More Than");
        m_cArmor.addItem("Any");
        m_cArmor.addItem("%25 maximum");
        m_cArmor.addItem("%50 maximum");
        m_cArmor.addItem("%75 maximum");
        m_cArmor.addItem("%90 maximum");
        m_cOrAnd.addItem("or");
        m_cOrAnd.addItem("and");
        populateWeaponsAndEquipmentChoices();
    }

    private void populateJComboBoxs() {

        /*
         * If you change any of the strings below, be sure to check the
         * filterMeks method below as some strings may need to be changed there
         * as well.
         */

        String weight = client.getConfigParam("UNITVIEWERWEIGHT");
        chWeightClass.setSelectedItem(weight);
        String tech = client.getConfigParam("UNITVIEWERTECH");
        chType.setSelectedItem(tech);
        String type = client.getConfigParam("UNITVIEWERTYPE");
        chUnitType.setSelectedItem(type);
    }

    private void buildSouthParams(boolean showAdvanced) {
        if (showAdvanced) {
            m_bToggleAdvanced.setText("> Hide Advanced Search <");
            m_pOpenAdvanced.add(m_bToggleAdvanced);

            m_pSouthParams.setLayout(new java.awt.GridLayout(5, 1));
            m_pSouthParams.add(m_pOpenAdvanced);

            javax.swing.JPanel row1 = new javax.swing.JPanel();
            row1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
            row1.add(new javax.swing.JLabel("Walk"));
            row1.add(m_cWalk);
            row1.add(m_tWalk);
            row1.add(new javax.swing.JLabel("Jump"));
            row1.add(m_cJump);
            row1.add(m_tJump);
            row1.add(new javax.swing.JLabel("Armor"));
            row1.add(m_cArmor);
            m_pSouthParams.add(row1);

            javax.swing.JPanel row2 = new javax.swing.JPanel();
            row2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
            row2.add(new javax.swing.JLabel("Weapons:"));
            row2.add(new javax.swing.JLabel("At least"));
            row2.add(m_tWeapons1);
            row2.add(m_cWeapons1);
            row2.add(m_cOrAnd);
            row2.add(new javax.swing.JLabel("At least"));
            row2.add(m_tWeapons2);
            row2.add(m_cWeapons2);
            m_pSouthParams.add(row2);

            javax.swing.JPanel row3 = new javax.swing.JPanel();
            row3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
            row3.add(new javax.swing.JLabel("Equipment"));
            row3.add(m_chkEquipment);
            row3.add(m_cEquipment);
            m_pSouthParams.add(row3);

            javax.swing.JPanel row4 = new javax.swing.JPanel();
            row4.add(m_bSearch);
            row4.add(m_bReset);
            row4.add(m_lCount);
            m_pSouthParams.add(row4);
        } else {
            m_bToggleAdvanced.setText("< Show Advanced Search >");
            m_pOpenAdvanced.add(m_bToggleAdvanced);

            m_pSouthParams.setLayout(new java.awt.GridLayout(2, 1));
            m_pSouthParams.add(m_pOpenAdvanced);
        }
        paintScreen(false);

    }

    void clearMechPreview() {
        mekViewLeft.setEditable(false);
        mekViewRight.setEditable(false);
        unitFluff.setEditable(false);
        // fluffScrollPane.setVisible(false);
        mekViewLeft.setText("");
        mekViewRight.setText("");
        unitFluff.setText("");

        // Remove preview image.
        previewMech(null);

    }

    private void populateWeaponsAndEquipmentChoices() {
        int year = Integer.parseInt(client.getServerConfigs("CampaignYear"));
        m_cWeapons1.removeAllItems();
        m_cWeapons2.removeAllItems();
        m_cEquipment.removeAllItems();
        m_tWeapons1.setText("");
        m_tWeapons2.setText("");
        m_chkEquipment.setSelected(false);
        int nType = chType.getSelectedIndex();
        int nUnitType = chUnitType.getSelectedIndex();
        for (java.util.Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements(); ) {
            EquipmentType et = e.nextElement();
            if ((et instanceof WeaponType) &&
                      ((et.getTechLevel(year) == nType) ||
                             (nType == TechConstants.T_ALL) ||
                             ((nType == TechConstants.T_IS_TW_ALL) &&
                                    ((et.getTechLevel(year) <= TechConstants.T_IS_TW_NON_BOX) ||
                                           (et.getTechLevel(year) == TechConstants.T_IS_ADVANCED) ||
                                           (et.getTechLevel(year) == TechConstants.T_CLAN_ADVANCED))) ||
                             (((nType == TechConstants.T_IS_TW_ALL) || (nType == TechConstants.T_IS_ADVANCED)) &&
                                    ((et.getTechLevel(year) <= TechConstants.T_IS_TW_NON_BOX) ||
                                           (et.getTechLevel(year) == TechConstants.T_IS_ADVANCED))))) {
                if (!(nUnitType == UnitType.SIZE) &&
                          ((UnitType.getTypeName(nUnitType).equals("Mek") ||
                                  UnitType.getTypeName(nUnitType).equals("Tank")) &&
                                 (et.hasFlag(WeaponType.F_INFANTRY) || et.hasFlag(WeaponType.F_INFANTRY_ONLY)))) {
                    continue;
                }
                m_cWeapons1.addItem(et.getName());
                m_cWeapons2.addItem(et.getName());
            }
            if ((et instanceof MiscType) &&
                      ((et.getTechLevel(year) == nType) ||
                             (nType == TechConstants.T_ALL) ||
                             ((nType == TechConstants.T_TW_ALL) &&
                                    ((et.getTechLevel(year) <= TechConstants.T_IS_TW_NON_BOX) ||
                                           (et.getTechLevel(year) == TechConstants.T_IS_ADVANCED) ||
                                           (et.getTechLevel(year) == TechConstants.T_CLAN_ADVANCED))) ||
                             (((nType == TechConstants.T_IS_TW_ALL) || (nType == TechConstants.T_IS_ADVANCED)) &&
                                    ((et.getTechLevel(year) <= TechConstants.T_IS_TW_NON_BOX) ||
                                           (et.getTechLevel(year) == TechConstants.T_IS_ADVANCED))))) {
                m_cEquipment.addItem(et.getName());
            }
        }
        try {
            m_cWeapons1.setSelectedIndex(0);
            m_cWeapons2.setSelectedIndex(0);
            m_cEquipment.setSelectedIndex(0);
        } catch (IllegalArgumentException ex) {
            MWLogger.errLog("Error in Unit Viewer. Could not set slider indices to 0");
            MWLogger.errLog(ex);
        }
        m_cWeapons1.invalidate();
        m_cWeapons2.invalidate();
        m_cEquipment.invalidate();
    }

    private void paintScreen(boolean fluff) {

        springHolder.removeAll();
        textBoxSpring.removeAll();
        fluffBoxSpring.removeAll();

        if (fluff) {
            // panel w/ 1x4 SpringLayout for the mechView bits
            textBoxSpring.add(listScrollPane);
            textBoxSpring.add(leftScrollPane);
            textBoxSpring.add(rightScrollPane);
            SpringLayoutHelper.setupSpringGrid(textBoxSpring, 3);
            fluffBoxSpring.add(fluffScrollPane);
            SpringLayoutHelper.setupSpringGrid(fluffBoxSpring, 1);
        } else {
            // panel w/ 1x3 SpringLayout for the mechView bits
            textBoxSpring.add(listScrollPane);
            textBoxSpring.add(leftScrollPane);
            textBoxSpring.add(rightScrollPane);
            SpringLayoutHelper.setupSpringGrid(textBoxSpring, 3);
        }

        // set up a formatting holder for the cancel button
        javax.swing.JPanel buttonHolder = new javax.swing.JPanel();
        if (viewerType != mekwars.common.gui.dialogs.UnitViewerDialog.UNIT_VIEWER) {
            buttonHolder.add(bSelect);
        }
        buttonHolder.add(bCancel);

        // set up the overall SpringLayout
        springHolder.add(pUpper);
        // springHolder.add(flowHolder);
        springHolder.add(textBoxSpring);
        if (fluff) {
            springHolder.add(fluffBoxSpring);
        }
        springHolder.add(buttonHolder);
        SpringLayoutHelper.setupSpringGrid(springHolder, 1);
        pack();
        this.repaint();

        mechList.grabFocus();

    }

    void previewMech(Entity entity) {

        Entity currEntity = entity;
        boolean populateTextFields = true;

        paintScreen((entity != null) && (currEntity.getFluff() != null) && viewFluff);

        // null entity, so load a default unit.
        if (entity == null) {
            try {
                // MekSummary ms =
                // MekSummaryCache.getInstance().getMech("Error OMG-UR-FD");
                currEntity = UnitUtils.createOMG();// new
                // MekFileParser(ms.getSourceFile(),
                // ms.getEntryName()).getEntity();
                populateTextFields = false;
            } catch (Exception e) {
                // this would be very very bad ...
            }
        }

        ConfigurableMekViewPanel mechView = null;
        try {
            mechView = new ConfigurableMekViewPanel(currEntity);
        } catch (Exception e) {
            // error unit didn't load right. this is bad news.
            populateTextFields = false;
        }

        mekViewLeft.setEditable(false);
        mekViewRight.setEditable(false);
        if (populateTextFields) {
            mekViewLeft.setText(mechView.getMechReadoutBasic());
            mekViewRight.setText(mechView.getMechReadoutLoadout());
            if ((currEntity.getFluff() != null) && viewFluff) {
                unitFluff.setEditable(false);
                unitFluff.setText(currEntity.getFluff().getHistory());
                unitFluff.setCaretPosition(0);

            } else {
                unitFluff.setText("");
            }
        } else {
            mekViewLeft.setText("No unit selected");
            mekViewRight.setText("No unit selected");
        }
        mekViewLeft.setCaretPosition(0);
        mekViewRight.setCaretPosition(0);

        // Preview image of the unit...
        try {
            ((MekInfo) pPreview).setUnit(currEntity);
            ((MekInfo) pPreview).setImageVisible(true);
            pPreview.paint(pPreview.getGraphics());
        } catch (Exception ex) {
            // shouldnt ever get here ...
        }
    }

    public void run() {

        // Loading mechs can take a while, so it will have its own thread.
        // This prevents the UI from freezing, and allows the
        // "Please wait..." dialog to behave properly on various Java VMs.

        filterMeks();
        unitLoadingDialog.setVisible(false);

        final java.util.Map<String, String> hFailedFiles = MekSummaryCache.getInstance().getFailedFiles();
        if ((hFailedFiles != null) && (!hFailedFiles.isEmpty())) {
            new UnitFailureDialog(clientGUI, hFailedFiles); // self-showing
            // dialog
        }

        try {
            String previousIndex = client.getConfigParam("UNITVIEWERUNIT");
            mechList.setSelectedValue(previousIndex, true);
        } catch (Exception e) {
            mechList.setSelectedIndex(-1);
        }

        pPreview.setVisible(true);
        setVisible(true);
        mechList.requestFocus();
    }

    private void filterMeks() {
        filterMeks(false);
    }

    @Override
    public void setVisible(boolean show) {
        setLocationRelativeTo(null);
        super.setVisible(show);
        pack();
    }

    private void filterMeks(boolean calledByAdvancedSearch) {

        java.util.Vector<MekSummary> vMeks = new java.util.Vector<>(1, 1);

        int nType = chType.getSelectedIndex();
        int nUnitType = chUnitType.getSelectedIndex();
        int nClass = chWeightClass.getSelectedIndex();

        MekSummary[] meks = MekSummaryCache.getInstance().getAllMeks();

        // break out if there are no units to filter
        if (meks == null) {
            System.err.println("No units to filter!");
            return;
        }

        try {
            for (MekSummary mek : meks) {
                /*
                 * A hacky check which prevents errors units from being added to
                 * the unit viewer lists, leaving only "valid" units from the
                 * dataset.
                 */
                if (mek.getName().startsWith("Error")) {
                    continue;
                }

                if (/* Weight */
                      ((nClass == EntityWeightClass.SIZE) || (mek.getWeightClass() == nClass)) &&
                            /*
                             * Technology Level
                             */
                            ((nType == TechConstants.T_ALL) ||
                                   (nType == mek.getType()) ||
                                   ((nType == TechConstants.T_IS_TW_ALL) &&
                                          ((mek.getType() <= TechConstants.T_IS_TW_NON_BOX) ||
                                                 (mek.getType() == TechConstants.T_INTRO_BOX_SET))) ||
                                   ((nType == TechConstants.T_TW_ALL) &&
                                          ((mek.getType() <= TechConstants.T_IS_TW_NON_BOX) ||
                                                 (mek.getType() <= TechConstants.T_INTRO_BOX_SET) ||
                                                 (mek.getType() <= TechConstants.T_CLAN_TW)))) &&
                            /*
                             * Unit Type (Mek, Infantry, etc.)
                             */
                            ((nUnitType == UnitType.SIZE) ||
                                   mek.getUnitType().equals(UnitType.getTypeName(nUnitType)))) {
                    vMeks.add(mek);
                }
            }
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            System.err.println("meks size: " + meks.length);
        }
        meksCurrent = new MekSummary[vMeks.size()];
        m_count = vMeks.size();

        if (!calledByAdvancedSearch && ((m_old_nType != nType) || (m_old_nUnitType != nUnitType))) {
            populateWeaponsAndEquipmentChoices();
        }
        m_old_nType = nType;
        m_old_nUnitType = nUnitType;
        vMeks.copyInto(meksCurrent);
        sortMeks();
    }

    private void sortMeks() {
        java.util.Arrays.sort(meksCurrent, new MekSummaryComparator(chSort.getSelectedIndex()));
        defaultModel.clear();
        try {
            mechList.setEnabled(false);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
            for (MekSummary mekSummary : meksCurrent) {
                defaultModel.addElement(formatMek(mekSummary));
            }
        } finally {
            setCursor(java.awt.Cursor.getDefaultCursor());
            mechList.setEnabled(true);
        }
        m_lCount.setText(meksCurrent.length + "/" + m_count);
        repaint();
    }

    private String formatMek(MekSummary ms) {

        String result =
              STR."\{makeLength(ms.getModel(), 12)} \{makeLength(ms.getChassis(), 10)} \{makeLength(Double.toString(
                    ms.getTons()), 3)} \{makeLength(Integer.toString(ms.getBV()), 5)}";

        if (Boolean.parseBoolean(client.getServerConfigs("UseCalculatedCosts"))) {
            result += makeLength(java.text.NumberFormat.getInstance().format(ms.getCost()), 10);
        }

        return result;
    }

    private String makeLength(String s, int nLength) {
        if (s.length() == nLength) {
            return s;
        } else if (s.length() > nLength) {
            return s.substring(0, nLength - 2) + "..";
        } else {
            return s + SPACES.substring(0, nLength - s.length());
        }
    }

    private void toggleAdvanced() {
        pUpper.remove(m_pSouthParams);
        m_pSouthParams = new javax.swing.JPanel();
        buildSouthParams(m_bToggleAdvanced.getText().equals("< Show Advanced Search >"));
        pUpper.add(m_pSouthParams, java.awt.BorderLayout.SOUTH);
        // invalidate();
        pack();
        repaint();
    }

    private void searchFor(String search) {
        for (int i = 0; i < meksCurrent.length; i++) {
            if (meksCurrent[i].getName().toLowerCase().startsWith(search)) {
                mechList.setSelectedIndex(i);
                mechList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent ae) {
        if (ae.getSource() == bCancel) {
            saveComboBoxSettings();
            dispose();
        }
        if (ae.getSource() == bSelect) {
            saveComboBoxSettings();
            if (viewerType == mekwars.common.gui.dialogs.UnitViewerDialog.OMNI_VARIANT_SELECTOR) {
                try {
                    MekSummary ms = meksCurrent[mechList.getSelectedIndex()];
                    String unit = ms.getName();
                    setVisible(false);
                    String moneyMod = javax.swing.JOptionPane.showInputDialog(clientGUI,
                          STR."Money Mod for \{unit}",
                          0);

                    if ((moneyMod == null) || (moneyMod.isEmpty())) {
                        dispose();
                        return;
                    }

                    String compMod = javax.swing.JOptionPane.showInputDialog(clientGUI, "Comp Mod for " + unit, 0);

                    if ((compMod == null) || (compMod.isEmpty())) {
                        dispose();
                        return;
                    }

                    String fluMod = javax.swing.JOptionPane.showInputDialog(clientGUI, "Flu Mod for " + unit, 0);

                    if ((fluMod == null) || (fluMod.isEmpty())) {
                        dispose();
                        return;
                    }

                    client.sendChat(
                          STR."\{IClient.CAMPAIGN_PREFIX}c AddOmniVariantMod#\{unit}#\{moneyMod}$\{compMod}$\{fluMod}");

                    dispose();
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
            }// end omni selector if
            else if (viewerType == mekwars.common.gui.dialogs.UnitViewerDialog.UNIT_SELECTOR) {
                try {
                    MekSummary ms = meksCurrent[mechList.getSelectedIndex()];
                    String unitFile;
                    String unit = ms.getName();
                    setVisible(false);
                    int weightClass = chWeightClass.getSelectedIndex();
                    // Item "All" takes up Weight Class 0, so this is usually 1 off.
                    if (weightClass > 0) {
                        weightClass -= 1;
                    }
                    unitFile = UnitUtils.getMekSummaryFileName(ms);


                    String fluff = javax.swing.JOptionPane.showInputDialog(clientGUI, "Fluff text for " + unit);

                    if ((fluff == null) || (fluff.isEmpty())) {
                        dispose();
                        return;
                    }

                    String gunnery = javax.swing.JOptionPane.showInputDialog(clientGUI,
                          "Gunnery skill for " + unit,
                          99);

                    if ((gunnery == null) || (gunnery.isEmpty())) {
                        dispose();
                        return;
                    }

                    String piloting = javax.swing.JOptionPane.showInputDialog(clientGUI,
                          "Piloting Mod for " + unit,
                          99);

                    if ((piloting == null) || (piloting.isEmpty())) {
                        dispose();
                        return;
                    }

                    String skills = javax.swing.JOptionPane.showInputDialog(clientGUI,
                          "Skills Mod for " + unit + " (comma delimited)");

                    if (skills == null) {
                        dispose();
                        return;
                    }

                    client.sendChat(
                          STR."\{IClient.CAMPAIGN_PREFIX}c createunit#\{unitFile}#\{fluff}#\{gunnery}#\{piloting}#\{weightClass}#\{skills}");

                    dispose();
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
            } else if (viewerType == mekwars.common.gui.dialogs.UnitViewerDialog.UNIT_RESEARCH) {
                MekSummary ms = meksCurrent[mechList.getSelectedIndex()];

                String unitFile;
                unitFile = UnitUtils.getMekSummaryFileName(ms);
                setVisible(false);

                if (!unitFile.equals("null")) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c researchunit#\{unitFile}");
                }

                dispose();

            }
            // end unit selector if.
            else {
                dispose();
            }
        } else if (ae.getSource().equals(m_bSearch)) {
            advancedSearch();
        } else if (ae.getSource().equals(m_bReset)) {
            resetSearch();
        } else if (ae.getSource().equals(m_bToggleAdvanced)) {
            toggleAdvanced();
        }
    }

    private void advancedSearch() {
        String s = m_lCount.getText();
        int first = Integer.parseInt(s.substring(0, s.indexOf('/')));
        int second = Integer.parseInt(s.substring(s.indexOf('/') + 1));
        if (first != second) {
            // Search already active, reset list before starting new one.
            filterMeks(true);
        }

        java.util.ArrayList<MekSummary> vMatches = new java.util.ArrayList<>();
        for (MekSummary ms : meksCurrent) {
            try {
                Entity entity = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                if (isMatch(entity)) {
                    vMatches.add(ms);
                }
            } catch (EntityLoadingException ex) {
                // do nothing, I guess
            }
        }
        meksCurrent = vMatches.toArray(new MekSummary[0]);
        clearMechPreview();
        sortMeks();
        paintScreen(false);
    }

    private boolean isMatch(Entity entity) {
        int walk = -1;
        try {
            walk = Integer.parseInt(m_tWalk.getText());
        } catch (NumberFormatException ne) {
            // never get here
        }
        if (walk > -1) {
            if (m_cWalk.getSelectedIndex() == 0) { // at least
                if (entity.getWalkMP() < walk) {
                    return false;
                }
            } else if (m_cWalk.getSelectedIndex() == 1) { // equal to
                if (walk != entity.getWalkMP()) {
                    return false;
                }
            } else if (m_cWalk.getSelectedIndex() == 2) { // not more than
                if (entity.getWalkMP() > walk) {
                    return false;
                }
            }
        }

        int jump = -1;
        try {
            jump = Integer.parseInt(m_tJump.getText());
        } catch (NumberFormatException ne) {
            // never get here
        }
        if (jump > -1) {
            if (m_cJump.getSelectedIndex() == 0) { // at least
                if (entity.getJumpMP() < jump) {
                    return false;
                }
            } else if (m_cJump.getSelectedIndex() == 1) { // equal to
                if (jump != entity.getJumpMP()) {
                    return false;
                }
            } else if (m_cJump.getSelectedIndex() == 2) { // not more than
                if (entity.getJumpMP() > jump) {
                    return false;
                }
            }
        }

        int sel = m_cArmor.getSelectedIndex();
        if (sel > 0) {
            int armor = entity.getTotalArmor();
            int maxArmor = (entity.getTotalInternal() * 2) + 3;
            if (sel == 1) {
                if (armor < (maxArmor * .25)) {
                    return false;
                }
            } else if (sel == 2) {
                if (armor < (maxArmor * .5)) {
                    return false;
                }
            } else if (sel == 3) {
                if (armor < (maxArmor * .75)) {
                    return false;
                }
            } else if (sel == 4) {
                if (armor < (maxArmor * .9)) {
                    return false;
                }
            }
        }

        boolean weaponLine1Active = false;
        boolean weaponLine2Active = false;
        boolean foundWeapon1 = false;
        boolean foundWeapon2 = false;

        int count = 0;
        int weapon1 = -1;
        try {
            weapon1 = Integer.parseInt(m_tWeapons1.getText());
        } catch (NumberFormatException ne) {
            // never get here
        }
        if (weapon1 > -1) {
            weaponLine1Active = true;
            for (int i = 0; i < entity.getWeaponList().size(); i++) {
                WeaponType wt = (entity.getWeaponList().get(i)).getType();
                if (wt.getName().equals(m_cWeapons1.getSelectedItem())) {
                    count++;
                }
            }
            if (count >= weapon1) {
                foundWeapon1 = true;
            }
        }

        count = 0;
        int weapon2 = -1;
        try {
            weapon2 = Integer.parseInt(m_tWeapons2.getText());
        } catch (NumberFormatException ne) {
            // never get here
        }
        if (weapon2 > -1) {
            weaponLine2Active = true;
            for (int i = 0; i < entity.getWeaponList().size(); i++) {
                WeaponType wt = (entity.getWeaponList().get(i)).getType();
                if (wt.getName().equals(m_cWeapons2.getSelectedItem())) {
                    count++;
                }
            }
            if (count >= weapon2) {
                foundWeapon2 = true;
            }
        }

        if (weaponLine1Active && !weaponLine2Active && !foundWeapon1) {
            return false;
        }
        if (weaponLine2Active && !weaponLine1Active && !foundWeapon2) {
            return false;
        }
        if (weaponLine1Active && weaponLine2Active) {
            if (m_cOrAnd.getSelectedIndex() == 0 /* 0 is "or" choice */) {
                if (!foundWeapon1 && !foundWeapon2) {
                    return false;
                }
            } else { // "and" choice in effect
                if (!foundWeapon1 || !foundWeapon2) {
                    return false;
                }
            }
        }

        count = 0;
        if (m_chkEquipment.isSelected()) {
            for (MiscMounted m : entity.getMisc()) {
                MiscType mt = m.getType();
                if (mt.getName().equals(m_cEquipment.getSelectedItem())) {
                    count++;
                }
            }
            return count >= 1;
        }

        return true;
    }

    private void resetSearch() {
        m_cWalk.setSelectedIndex(0);
        m_tWalk.setText("");
        m_cJump.setSelectedIndex(0);
        m_tJump.setText("");
        m_cArmor.setSelectedIndex(0);
        m_cOrAnd.setSelectedIndex(0);

        populateWeaponsAndEquipmentChoices();
        filterMeks(false);
        paintScreen(false);

    }

    /**
     * for compliance with ListSelectionListener
     */
    public void valueChanged(javax.swing.event.ListSelectionEvent event) {

        int selected = mechList.getSelectedIndex();
        if (selected == -1) {
            clearMechPreview();
            return;
        }
        // else
        MekSummary ms = meksCurrent[selected];
        try {
            Entity entity = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            previewMech(entity);
        } catch (EntityLoadingException ex) {
            System.out.println("Unable to load mech: " +
                                     ms.getSourceFile() +
                                     ": " +
                                     ms.getEntryName() +
                                     ": " +
                                     ex.getMessage());
            MWLogger.errLog(ex);
            clearMechPreview();
        }
    }

    public void itemStateChanged(java.awt.event.ItemEvent ie) {

        Object currSelection = mechList.getSelectedValue();

        if (ie.getSource() == chSort) {
            sortMeks();
        } else if ((ie.getSource() == chWeightClass) || (ie.getSource() == chType) || (ie.getSource() == chUnitType)) {
            filterMeks();
        }

        // try to reselect the previous choice. if the choice cant be found,
        // the list automatically reverts to -1 (no selection)
        mechList.setSelectedValue(currSelection, true);
    }

    public void keyTyped(java.awt.event.KeyEvent ke) {
    }

    public void keyPressed(java.awt.event.KeyEvent ke) {
        if ((ke.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) ||
                  (ke.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE)) {
            java.awt.event.ActionEvent event = new java.awt.event.ActionEvent(bCancel,
                  java.awt.event.ActionEvent.ACTION_PERFORMED,
                  "");
            actionPerformed(event);
        }
        long curTime = System.currentTimeMillis();
        if ((curTime - m_nLastSearch) > KEY_TIMEOUT) {
            m_sbSearch = new StringBuilder();
        }
        m_nLastSearch = curTime;
        m_sbSearch.append(ke.getKeyChar());
        searchFor(m_sbSearch.toString().toLowerCase());
    }

    public void keyReleased(java.awt.event.KeyEvent ke) {
        // no action on release
    }

    public void windowOpened(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        saveComboBoxSettings();
        dispose();
    }

    private void saveComboBoxSettings() {

        client.getConfig().setParam("UNITVIEWERWEIGHT", (String) chWeightClass.getSelectedItem());
        client.getConfig().setParam("UNITVIEWERTECH", (String) chType.getSelectedItem());
        client.getConfig().setParam("UNITVIEWERTYPE", (String) chUnitType.getSelectedItem());
        client.getConfig().setParam("UNITVIEWERSORT", (String) chSort.getSelectedItem());
        if (mechList.getSelectedValue() != null) {
            client.getConfig().setParam("UNITVIEWERUNIT", mechList.getSelectedValue());
        }

        client.getConfig().saveConfig();
        client.setConfig();
    }

    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowIconified(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {
    }

    // WindowListener
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {
    }
}
