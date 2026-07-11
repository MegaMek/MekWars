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

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.Serial;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.ui.dialogs.UnitFailureDialog;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.dialogs.unitSelectorDialogs.ConfigurableMekViewPanel;
import megamek.codeUtilities.MathUtility;
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
import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.CMainFrame;
import mekwars.common.gui.MekInfo;
import mekwars.common.util.SpringLayoutHelper;
import mekwars.common.util.UnitUtils;

/*
 * Allows a user to sort through a list of MechSummaries and select one
 */

public class UnitViewerDialog extends JFrame
      implements ActionListener, KeyListener, ListSelectionListener, Runnable, WindowListener, ItemListener {

    public static final MMLogger LOGGER = MMLogger.create(UnitViewerDialog.class);

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
    private final String[] saSorts = { "Name", "Ref", "Weight", "BV" };// , "Year"
    // frame which owns the dialog
    private final CMainFrame clientGUI;
    private final UnitLoadingDialog unitLoadingDialog;
    private final JComboBox<String> chType = new JComboBox<>();
    private final JComboBox<String> chUnitType = new JComboBox<>();
    private final JComboBox<String> chWeightClass = new JComboBox<>();
    private final JComboBox<String> chSort = new JComboBox<>();
    private final JPanel textBoxSpring = new JPanel(new SpringLayout());
    private final JPanel springHolder = new JPanel(new SpringLayout());
    private final JPanel fluffBoxSpring = new JPanel(new SpringLayout());
    private final JButton bCancel = new JButton("Close");
    private final JButton bSelect = new JButton("Select");
    private final JTextPane mekViewLeft;
    private final JTextPane mekViewRight;
    private final JTextPane unitFluff;
    private final JPanel pUpper = new JPanel();
    private final IClient client;
    private final int viewerType;
    private final boolean viewFluff;
    private final JPanel m_pOpenAdvanced = new JPanel();
    private final JButton m_bToggleAdvanced = new JButton("< Show Advanced Search >");
    private final JComboBox<String> m_cWalk = new JComboBox<>();
    private final JTextField m_tWalk = new JTextField(2);
    private final JComboBox<String> m_cJump = new JComboBox<>();
    private final JTextField m_tJump = new JTextField(2);
    private final JComboBox<String> m_cArmor = new JComboBox<>();
    private final JTextField m_tWeapons1 = new JTextField(2);

    // private String selectedUnit = null;
    private final JComboBox<String> m_cWeapons1 = new JComboBox<>();
    private final JComboBox<String> m_cOrAnd = new JComboBox<>();
    private final JTextField m_tWeapons2 = new JTextField(2);
    private final JComboBox<String> m_cWeapons2 = new JComboBox<>();
    private final JCheckBox m_chkEquipment = new JCheckBox();
    private final JComboBox<String> m_cEquipment = new JComboBox<>();
    private final JButton m_bSearch = new JButton("Search");
    private final JButton m_bReset = new JButton("Reset");
    private final JLabel m_lCount = new JLabel();
    private final DefaultListModel<String> defaultModel;
    private final JList<String> mekList;
    private JScrollPane listScrollPane = null;
    private JScrollPane leftScrollPane = null;
    private JScrollPane rightScrollPane = null;
    private JScrollPane fluffScrollPane = null;
    private MekSummary[] meksCurrent;
    private StringBuilder m_sbSearch = new StringBuilder();
    private long m_nLastSearch = 0;
    private JPanel pPreview = new JPanel();
    private JPanel m_pSouthParams = new JPanel();
    private int m_count;
    private int m_old_nType;
    private int m_old_nUnitType;

    public UnitViewerDialog(CMainFrame cMainFrame, UnitLoadingDialog uld, IClient client, int viewer) {
        super("Unit Viewer");

        viewerType = viewer;
        if (viewerType == UnitViewerDialog.OMNI_VARIANT_SELECTOR) {
            setTitle("Omni Variant Selector");
        } else if (viewerType == UnitViewerDialog.UNIT_SELECTOR) {
            setTitle("Unit Selector");
        }

        // save params
        clientGUI = cMainFrame;
        unitLoadingDialog = uld;
        this.client = client;
        viewFluff = MathUtility.parseBoolean(client.getConfigParam("VIEW_FLUFF"), false);

        // construct 2 text boxes
        mekViewLeft = new JTextPane();
        mekViewLeft.setContentType("text/html");
        mekViewRight = new JTextPane();
        mekViewRight.setContentType("text/html");
        unitFluff = new JTextPane();
        unitFluff.setContentType("text/html");

        // construct a model and list
        defaultModel = new DefaultListModel<>();
        mekList = new JList<>(defaultModel);
        ListSelectionModel listSelectionModel = mekList.getSelectionModel();
        mekList.setVisibleRowCount(17);// give the list same number of rows as
        // the text boxes
        listSelectionModel.addListSelectionListener(this);

        // place the list and text boxes in scroll panes
        listScrollPane = new JScrollPane(mekList);
        leftScrollPane = new JScrollPane(mekViewLeft);
        rightScrollPane = new JScrollPane(mekViewRight);
        fluffScrollPane = new JScrollPane(unitFluff);

        // set list/scroll options
        listScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        listScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        leftScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        rightScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        fluffScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        fluffScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        fluffScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        mekList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // set fonts
        mekViewLeft.setFont(new Font("Monospaced", Font.PLAIN, 11));
        mekViewRight.setFont(new Font("Monospaced", Font.PLAIN, 11));
        mekList.setFont(new Font("Monospaced", Font.PLAIN, 11));

        for (String saSort : saSorts) {
            chSort.addItem(saSort);
        }

        String sort = client.getConfigParam("UNIT_VIEWER_SORT");
        chSort.setSelectedItem(sort);

        // set up the upper panel (combo boxes, preview image)
        pPreview = new MekInfo(client);
        pPreview.setVisible(false);
        pPreview.setMinimumSize(new Dimension(86, 74));
        pPreview.setMaximumSize(new Dimension(86, 74));
        pUpper.setLayout(new BorderLayout());
        JPanel pParams = new JPanel();
        pUpper.add(pParams, BorderLayout.WEST);
        pUpper.add(pPreview, BorderLayout.CENTER);
        pUpper.add(m_pSouthParams, BorderLayout.SOUTH);

        // lay out the combo boxes
        pParams.setLayout(new GridLayout(4, 2));
        JLabel labelWeightClass = new JLabel("Class: ", SwingConstants.RIGHT);
        pParams.add(labelWeightClass);
        pParams.add(chWeightClass);
        JLabel labelType = new JLabel("Tech: ", SwingConstants.RIGHT);
        pParams.add(labelType);
        pParams.add(chType);
        JLabel labelUnitType = new JLabel("Type: ", SwingConstants.RIGHT);
        pParams.add(labelUnitType);
        pParams.add(chUnitType);
        JLabel labelSort = new JLabel("Sort: ", SwingConstants.RIGHT);
        pParams.add(labelSort);
        pParams.add(chSort);
        pParams.doLayout();

        populateChoices();
        populateJComboBoxes();
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
        mekList.addListSelectionListener(this);
        mekList.addKeyListener(this);
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

    private void populateJComboBoxes() {

        /*
         * If you change any of the strings below, be sure to check the
         * filterMeks method below as some strings may need to be changed there
         * as well.
         */

        String weight = client.getConfigParam("UNIT_VIEWER_WEIGHT");
        chWeightClass.setSelectedItem(weight);
        String tech = client.getConfigParam("UNIT_VIEWER_TECH");
        chType.setSelectedItem(tech);
        String type = client.getConfigParam("UNIT_VIEWER_TYPE");
        chUnitType.setSelectedItem(type);
    }

    private void buildSouthParams(boolean showAdvanced) {
        if (showAdvanced) {
            m_bToggleAdvanced.setText("> Hide Advanced Search <");
            m_pOpenAdvanced.add(m_bToggleAdvanced);

            m_pSouthParams.setLayout(new GridLayout(5, 1));
            m_pSouthParams.add(m_pOpenAdvanced);

            JPanel row1 = new JPanel();
            row1.setLayout(new FlowLayout(FlowLayout.CENTER));
            row1.add(new JLabel("Walk"));
            row1.add(m_cWalk);
            row1.add(m_tWalk);
            row1.add(new JLabel("Jump"));
            row1.add(m_cJump);
            row1.add(m_tJump);
            row1.add(new JLabel("Armor"));
            row1.add(m_cArmor);
            m_pSouthParams.add(row1);

            JPanel row2 = new JPanel();
            row2.setLayout(new FlowLayout(FlowLayout.LEFT));
            row2.add(new JLabel("Weapons:"));
            row2.add(new JLabel("At least"));
            row2.add(m_tWeapons1);
            row2.add(m_cWeapons1);
            row2.add(m_cOrAnd);
            row2.add(new JLabel("At least"));
            row2.add(m_tWeapons2);
            row2.add(m_cWeapons2);
            m_pSouthParams.add(row2);

            JPanel row3 = new JPanel();
            row3.setLayout(new FlowLayout(FlowLayout.CENTER));
            row3.add(new JLabel("Equipment"));
            row3.add(m_chkEquipment);
            row3.add(m_cEquipment);
            m_pSouthParams.add(row3);

            JPanel row4 = new JPanel();
            row4.add(m_bSearch);
            row4.add(m_bReset);
            row4.add(m_lCount);
            m_pSouthParams.add(row4);
        } else {
            m_bToggleAdvanced.setText("< Show Advanced Search >");
            m_pOpenAdvanced.add(m_bToggleAdvanced);

            m_pSouthParams.setLayout(new GridLayout(2, 1));
            m_pSouthParams.add(m_pOpenAdvanced);
        }
        paintScreen(false);

    }

    void clearMechPreview() {
        mekViewLeft.setEditable(false);
        mekViewRight.setEditable(false);
        unitFluff.setEditable(false);
        mekViewLeft.setText("");
        mekViewRight.setText("");
        unitFluff.setText("");

        // Remove preview image.
        previewMech(null);

    }

    private void populateWeaponsAndEquipmentChoices() {
        int year = MathUtility.parseInt(client.getServerConfigs("CampaignYear"), 2045);
        m_cWeapons1.removeAllItems();
        m_cWeapons2.removeAllItems();
        m_cEquipment.removeAllItems();
        m_tWeapons1.setText("");
        m_tWeapons2.setText("");
        m_chkEquipment.setSelected(false);
        int nType = chType.getSelectedIndex();
        int nUnitType = chUnitType.getSelectedIndex();
        for (Enumeration<EquipmentType> equipmentTypeEnumeration = EquipmentType.getAllTypes();
              equipmentTypeEnumeration.hasMoreElements(); ) {
            EquipmentType equipmentType = equipmentTypeEnumeration.nextElement();
            if ((equipmentType instanceof WeaponType) &&
                      ((equipmentType.getTechLevel(year) == nType) ||
                             (nType == TechConstants.T_ALL) ||
                             ((nType == TechConstants.T_IS_TW_ALL) &&
                                    ((equipmentType.getTechLevel(year) <= TechConstants.T_IS_TW_NON_BOX) ||
                                           (equipmentType.getTechLevel(year) == TechConstants.T_IS_ADVANCED) ||
                                           (equipmentType.getTechLevel(year) == TechConstants.T_CLAN_ADVANCED))) ||
                             (((nType == TechConstants.T_IS_TW_ALL) || (nType == TechConstants.T_IS_ADVANCED)) &&
                                    ((equipmentType.getTechLevel(year) <= TechConstants.T_IS_TW_NON_BOX) ||
                                           (equipmentType.getTechLevel(year) == TechConstants.T_IS_ADVANCED))))) {
                if (!(nUnitType == UnitType.SIZE) &&
                          ((UnitType.getTypeName(nUnitType).equals("Mek") ||
                                  UnitType.getTypeName(nUnitType).equals("Tank")) &&
                                 (equipmentType.hasFlag(WeaponType.F_INFANTRY) ||
                                        equipmentType.hasFlag(WeaponType.F_INFANTRY_ONLY)))) {
                    continue;
                }

                m_cWeapons1.addItem(equipmentType.getName());
                m_cWeapons2.addItem(equipmentType.getName());
            }

            if ((equipmentType instanceof MiscType) &&
                      ((equipmentType.getTechLevel(year) == nType) ||
                             (nType == TechConstants.T_ALL) ||
                             ((nType == TechConstants.T_TW_ALL) &&
                                    ((equipmentType.getTechLevel(year) <= TechConstants.T_IS_TW_NON_BOX) ||
                                           (equipmentType.getTechLevel(year) == TechConstants.T_IS_ADVANCED) ||
                                           (equipmentType.getTechLevel(year) == TechConstants.T_CLAN_ADVANCED))) ||
                             (((nType == TechConstants.T_IS_TW_ALL) || (nType == TechConstants.T_IS_ADVANCED)) &&
                                    ((equipmentType.getTechLevel(year) <= TechConstants.T_IS_TW_NON_BOX) ||
                                           (equipmentType.getTechLevel(year) == TechConstants.T_IS_ADVANCED))))) {
                m_cEquipment.addItem(equipmentType.getName());
            }
        }
        try {
            m_cWeapons1.setSelectedIndex(0);
            m_cWeapons2.setSelectedIndex(0);
            m_cEquipment.setSelectedIndex(0);
        } catch (IllegalArgumentException ex) {
            LOGGER.error(ex, "Error in Unit Viewer. Could not set slider indices to 0");
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
        JPanel buttonHolder = new JPanel();

        if (viewerType != UnitViewerDialog.UNIT_VIEWER) {
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

        mekList.grabFocus();

    }

    void previewMech(Entity entity) {
        Entity currEntity = entity;
        boolean populateTextFields = true;

        paintScreen((entity != null) && (currEntity.getFluff() != null) && viewFluff);

        // null entity, so load a default unit.
        if (entity == null) {
            currEntity = UnitUtils.createOMG();// new
            populateTextFields = false;
        }

        ConfigurableMekViewPanel mekView = null;

        try {
            mekView = new ConfigurableMekViewPanel(currEntity);
        } catch (Exception e) {
            LOGGER.error(e, "Unit didn't load. {}", e.getLocalizedMessage());
            populateTextFields = false;
        }

        mekViewLeft.setEditable(false);
        mekViewRight.setEditable(false);
        if (populateTextFields) {
            LOGGER.debug("UnitViewDialog Readouts Disabled..");
            // mekViewLeft.setText(mekView.getMekReadoutBasic());
            // mekViewRight.setText(mekView.getMekReadoutLoadout());

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
            LOGGER.error(ex, "Error in Unit Viewer. Could not set preview image");
        }
    }

    public void run() {

        // Loading meks can take a while, so it will have its own thread.
        // This prevents the UI from freezing and allows the
        // "Please wait..." dialog to behave properly on various Java VMs.

        filterMeks();
        unitLoadingDialog.setVisible(false);

        final java.util.Map<String, String> hFailedFiles = MekSummaryCache.getInstance().getFailedFiles();
        if ((hFailedFiles != null) && (!hFailedFiles.isEmpty())) {
            new UnitFailureDialog(clientGUI, hFailedFiles); // self-showing
            // dialog
        }

        try {
            String previousIndex = client.getConfigParam("UNIT_VIEWER_UNIT");
            mekList.setSelectedValue(previousIndex, true);
        } catch (Exception e) {
            mekList.setSelectedIndex(-1);
        }

        pPreview.setVisible(true);
        setVisible(true);
        mekList.requestFocus();
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
        Vector<MekSummary> vMeks = new Vector<>(1, 1);

        int nType = chType.getSelectedIndex();
        int nUnitType = chUnitType.getSelectedIndex();
        int nClass = chWeightClass.getSelectedIndex();

        MekSummary[] meks = MekSummaryCache.getInstance().getAllMeks();

        // break out if there are no units to filter
        if (meks == null) {
            LOGGER.debug("No units to filter!");
            return;
        }

        try {
            for (MekSummary mek : meks) {
                /*
                 * A hacky check that prevents errors units from being added to
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
            LOGGER.error(ex, String.format("meks size: %s", meks.length));
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
        Arrays.sort(meksCurrent, new MekSummaryComparator(chSort.getSelectedIndex()));
        defaultModel.clear();

        try {
            mekList.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            for (MekSummary mekSummary : meksCurrent) {
                defaultModel.addElement(formatMek(mekSummary));
            }
        } finally {
            setCursor(Cursor.getDefaultCursor());
            mekList.setEnabled(true);
        }
        m_lCount.setText(String.format("%s/%s", meksCurrent.length, m_count));
        repaint();
    }

    private String formatMek(MekSummary mekSummary) {
        String result =
              String.format("%s %s %s %s", makeLength(mekSummary.getModel(), 12), makeLength(mekSummary.getChassis(), 10), makeLength(
                    Double.toString(
                          mekSummary.getTons()),
                    3), makeLength(Integer.toString(mekSummary.getBV()), 5));

        if (Boolean.parseBoolean(client.getServerConfigs("UseCalculatedCosts"))) {
            result += makeLength(NumberFormat.getInstance().format(mekSummary.getCost()), 10);
        }

        return result;
    }

    private String makeLength(String string, int nLength) {
        if (string.length() == nLength) {
            return string;
        } else if (string.length() > nLength) {
            return String.format("%s..", string.substring(0, nLength - 2));
        } else {
            return string + SPACES.substring(0, nLength - string.length());
        }
    }

    private void toggleAdvanced() {
        pUpper.remove(m_pSouthParams);
        m_pSouthParams = new JPanel();
        buildSouthParams(m_bToggleAdvanced.getText().equals("< Show Advanced Search >"));
        pUpper.add(m_pSouthParams, java.awt.BorderLayout.SOUTH);
        // invalidate();
        pack();
        repaint();
    }

    private void searchFor(String search) {
        for (int i = 0; i < meksCurrent.length; i++) {
            if (meksCurrent[i].getName().toLowerCase().startsWith(search)) {
                mekList.setSelectedIndex(i);
                mekList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == bCancel) {
            saveComboBoxSettings();
            dispose();
        }
        if (actionEvent.getSource() == bSelect) {
            saveComboBoxSettings();
            if (viewerType == UnitViewerDialog.OMNI_VARIANT_SELECTOR) {
                try {
                    MekSummary mekSummary = meksCurrent[mekList.getSelectedIndex()];
                    String unit = mekSummary.getName();
                    setVisible(false);
                    String moneyMod = JOptionPane.showInputDialog(clientGUI, String.format("Money Mod for %s", unit), 0);

                    if ((moneyMod == null) || (moneyMod.isEmpty())) {
                        dispose();
                        return;
                    }

                    String compMod = JOptionPane.showInputDialog(clientGUI, String.format("Comp Mod for %s", unit), 0);

                    if ((compMod == null) || (compMod.isEmpty())) {
                        dispose();
                        return;
                    }

                    String fluMod = javax.swing.JOptionPane.showInputDialog(clientGUI, String.format("Flu Mod for %s", unit), 0);

                    if ((fluMod == null) || (fluMod.isEmpty())) {
                        dispose();
                        return;
                    }

                    client.sendChat(
                          String.format("%sc AddOmniVariantMod#%s#%s$%s$%s", IClient.CAMPAIGN_PREFIX, unit, moneyMod, compMod, fluMod));

                    dispose();
                } catch (Exception ex) {
                    LOGGER.error(ex, "Error wile performing action: AddOmniVariantMod - {}", ex.getLocalizedMessage());
                }
            } else if (viewerType == UnitViewerDialog.UNIT_SELECTOR) {
                try {
                    MekSummary mekSummary = meksCurrent[mekList.getSelectedIndex()];
                    String unitFile;
                    String unit = mekSummary.getName();
                    setVisible(false);
                    int weightClass = chWeightClass.getSelectedIndex();
                    // Item "All" takes up Weight Class 0, so this is usually 1 off.

                    if (weightClass > 0) {
                        weightClass -= 1;
                    }

                    unitFile = UnitUtils.getMekSummaryFileName(mekSummary);


                    String fluff = JOptionPane.showInputDialog(clientGUI, String.format("Fluff text for %s", unit));

                    if ((fluff == null) || (fluff.isEmpty())) {
                        dispose();
                        return;
                    }

                    String gunnery = JOptionPane.showInputDialog(clientGUI, String.format("Gunnery skill for %s", unit), 99);

                    if ((gunnery == null) || (gunnery.isEmpty())) {
                        dispose();
                        return;
                    }

                    String piloting = JOptionPane.showInputDialog(clientGUI, String.format("Piloting Mod for %s", unit), 99);

                    if ((piloting == null) || (piloting.isEmpty())) {
                        dispose();
                        return;
                    }

                    String skills = JOptionPane.showInputDialog(clientGUI,
                          String.format("Skills Mod for %s (comma delimited)", unit));

                    if (skills == null) {
                        dispose();
                        return;
                    }

                    client.sendChat(
                          String.format("%sc createunit#%s#%s#%s#%s#%s#%s", IClient.CAMPAIGN_PREFIX, unitFile, fluff, gunnery, piloting, weightClass, skills));

                    dispose();
                } catch (Exception ex) {
                    LOGGER.error(ex, "Error wile performing action: UnitSelector - {}", ex.getLocalizedMessage());
                }
            } else if (viewerType == UnitViewerDialog.UNIT_RESEARCH) {
                MekSummary mekSummary = meksCurrent[mekList.getSelectedIndex()];

                String unitFile;
                unitFile = UnitUtils.getMekSummaryFileName(mekSummary);
                setVisible(false);

                if (!unitFile.equals("null")) {
                    client.sendChat(String.format("%sc researchunit#%s", IClient.CAMPAIGN_PREFIX, unitFile));
                }

                dispose();

            } else {
                dispose();
            }
        } else if (actionEvent.getSource().equals(m_bSearch)) {
            advancedSearch();
        } else if (actionEvent.getSource().equals(m_bReset)) {
            resetSearch();
        } else if (actionEvent.getSource().equals(m_bToggleAdvanced)) {
            toggleAdvanced();
        }
    }

    private void advancedSearch() {
        String s = m_lCount.getText();
        int first = MathUtility.parseInt(s.substring(0, s.indexOf('/')), 0);
        int second = MathUtility.parseInt(s.substring(s.indexOf('/') + 1), 0);
        if (first != second) {
            // Search already active, reset list before starting new one.
            filterMeks(true);
        }

        ArrayList<MekSummary> vMatches = new ArrayList<>();
        for (MekSummary mekSummary : meksCurrent) {
            try {
                Entity entity = new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName()).getEntity();
                if (isMatch(entity)) {
                    vMatches.add(mekSummary);
                }
            } catch (EntityLoadingException ex) {
                LOGGER.error(ex, "Error in Unit Viewer. Could not load entity");
            }
        }

        meksCurrent = vMatches.toArray(new MekSummary[0]);
        clearMechPreview();
        sortMeks();
        paintScreen(false);
    }

    private boolean isMatch(Entity entity) {

        int walk = MathUtility.parseInt(m_tWalk.getText(), -1);
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

        int jump = MathUtility.parseInt(m_tJump.getText(), -1);
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
        int weapon1 = MathUtility.parseInt(m_tWeapons1.getText(), -1);

        if (weapon1 > -1) {
            weaponLine1Active = true;
            for (int i = 0; i < entity.getWeaponList().size(); i++) {
                WeaponType weaponType = (entity.getWeaponList().get(i)).getType();
                if (weaponType.getName().equals(m_cWeapons1.getSelectedItem())) {
                    count++;
                }
            }
            if (count >= weapon1) {
                foundWeapon1 = true;
            }
        }

        count = 0;
        int weapon2 = MathUtility.parseInt(m_tWeapons2.getText(), -1);
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
            for (MiscMounted miscMounted : entity.getMisc()) {
                MiscType miscType = miscMounted.getType();
                if (miscType.getName().equals(m_cEquipment.getSelectedItem())) {
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
    public void valueChanged(ListSelectionEvent event) {

        int selected = mekList.getSelectedIndex();
        if (selected == -1) {
            clearMechPreview();
            return;
        }
        // else
        MekSummary mekSummary = meksCurrent[selected];
        try {
            Entity entity = new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName()).getEntity();
            previewMech(entity);
        } catch (EntityLoadingException ex) {
            LOGGER.error(ex,
                  String.format("Unable to load mech: %s: %s: %s", mekSummary.getSourceFile(), mekSummary.getEntryName(), ex.getMessage()));
            clearMechPreview();
        }
    }

    public void itemStateChanged(ItemEvent itemEvent) {

        Object currSelection = mekList.getSelectedValue();

        if (itemEvent.getSource() == chSort) {
            sortMeks();
        } else if ((itemEvent.getSource() == chWeightClass) ||
                         (itemEvent.getSource() == chType) ||
                         (itemEvent.getSource() == chUnitType)) {
            filterMeks();
        }

        // try to reselect the previous choice. if the choice cant be found,
        // the list automatically reverts to -1 (no selection)
        mekList.setSelectedValue(currSelection, true);
    }

    public void keyTyped(KeyEvent keyEvent) {
    }

    public void keyPressed(KeyEvent keyEvent) {
        if ((keyEvent.getKeyCode() == KeyEvent.VK_ENTER) || (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE)) {
            ActionEvent event = new ActionEvent(bCancel, ActionEvent.ACTION_PERFORMED, "");
            actionPerformed(event);
        }

        long curTime = System.currentTimeMillis();

        if ((curTime - m_nLastSearch) > KEY_TIMEOUT) {
            m_sbSearch = new StringBuilder();
        }

        m_nLastSearch = curTime;
        m_sbSearch.append(keyEvent.getKeyChar());
        searchFor(m_sbSearch.toString().toLowerCase());
    }

    public void keyReleased(KeyEvent keyEvent) {
        // no action on release
    }

    public void windowOpened(WindowEvent windowEvent) {
    }

    public void windowClosing(WindowEvent windowEvent) {
        saveComboBoxSettings();
        dispose();
    }

    private void saveComboBoxSettings() {
        client.getConfig().setParam("UNIT_VIEWER_WEIGHT", (String) chWeightClass.getSelectedItem());
        client.getConfig().setParam("UNIT_VIEWER_TECH", (String) chType.getSelectedItem());
        client.getConfig().setParam("UNIT_VIEWER_TYPE", (String) chUnitType.getSelectedItem());
        client.getConfig().setParam("UNIT_VIEWER_SORT", (String) chSort.getSelectedItem());

        if (mekList.getSelectedValue() != null) {
            client.getConfig().setParam("UNIT_VIEWER_UNIT", mekList.getSelectedValue());
        }

        client.getConfig().saveConfig();
        client.setConfig();
    }

    public void windowClosed(WindowEvent windowEvent) {
    }

    public void windowIconified(WindowEvent windowEvent) {
    }

    public void windowDeiconified(WindowEvent windowEvent) {
    }

    // WindowListener
    public void windowActivated(WindowEvent windowEvent) {
    }

    public void windowDeactivated(WindowEvent windowEvent) {
    }
}
