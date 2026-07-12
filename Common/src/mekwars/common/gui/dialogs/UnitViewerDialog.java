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

/**
 * Multi-purpose unit browser window built on top of {@link MekSummaryCache} (MegaMek's cache of
 * every loadable unit file). Despite its name and the "dialog" package, this is a top-level
 * {@link JFrame}, not a modal dialog. Depending on the {@code viewer} mode passed to the
 * constructor, the same UI serves four different purposes (see the {@code UNIT_*}/
 * {@code OMNI_VARIANT_SELECTOR} constants below):
 * <ul>
 * <li>{@link #UNIT_VIEWER} — pure browsing, no "Select" button.</li>
 * <li>{@link #OMNI_VARIANT_SELECTOR} — picking a base chassis to register an OmniMech variant
 * cost/BV/fluff modifier against.</li>
 * <li>{@link #UNIT_SELECTOR} — picking a unit file to spawn as a new in-campaign unit, prompting
 * for fluff text and pilot skills.</li>
 * <li>{@link #UNIT_RESEARCH} — picking a unit to submit as a "research" request.</li>
 * </ul>
 * The window shows a fixed-width text list of matching units (see {@link #formatMek(MekSummary)})
 * alongside basic filters (weight class, tech level, unit type, sort order) and a togglable
 * "Advanced Search" panel (movement, armor percentage, weapon counts, required equipment). It also
 * hosts a small preview image and (largely disabled — see {@link #previewMech(Entity)}) unit
 * readout panes, plus an optional fluff/history pane.
 * <p>
 * Because populating {@link #meksCurrent} from {@link MekSummaryCache} can be slow the first time
 * it runs, this class implements {@link Runnable}: callers are expected to construct the dialog,
 * show an owning {@link UnitLoadingDialog} "please wait" indicator, and invoke {@link #run()} on a
 * background thread, which performs the initial filter pass and then makes this frame visible.
 */
public class UnitViewerDialog extends JFrame
      implements ActionListener, KeyListener, ListSelectionListener, Runnable, WindowListener, ItemListener {

    public static final MMLogger LOGGER = MMLogger.create(UnitViewerDialog.class);

    /** Viewer mode: plain unit browser with no "Select" action. */
    public static final int UNIT_VIEWER = 0;
    /** Viewer mode: pick a chassis to attach an OmniMech variant cost/BV/fluff modifier to. */
    public static final int OMNI_VARIANT_SELECTOR = 1;
    /** Viewer mode: pick a unit file to spawn as a new campaign unit (prompts for fluff/skills). */
    public static final int UNIT_SELECTOR = 2;
    /** Viewer mode: pick a unit to submit a "research unit" request for. */
    public static final int UNIT_RESEARCH = 3;
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -7210333306969855153L;
    // how long after a key is typed does a new search begin
    /** Max gap, in milliseconds, between keystrokes for the type-ahead list search to be treated as a continuation rather than a new search. */
    private final static int KEY_TIMEOUT = 1000;
    /** Padding used by {@link #makeLength(String, int)} to right-pad fixed-width list columns. */
    private static final String SPACES = "                        ";
    // };
    // these indices should match up with the static values in the
    // MekSummaryComparator
    /** Labels for the sort combo box; "Year" is commented out/unused, a leftover from an earlier version. */
    private final String[] saSorts = { "Name", "Ref", "Weight", "BV" };// , "Year"
    // frame which owns the dialog
    /** Owning main frame; used to anchor other dialogs spawned from here (e.g. failure/input dialogs). */
    private final CMainFrame clientGUI;
    /** "Please wait" indicator shown by the caller while units load; hidden once {@link #run()} finishes filtering. */
    private final UnitLoadingDialog unitLoadingDialog;
    /** Tech level filter combo (see {@link #populateChoices()}). */
    private final JComboBox<String> chType = new JComboBox<>();
    /** Unit type filter combo (Mek, Tank, etc., plus "All"). */
    private final JComboBox<String> chUnitType = new JComboBox<>();
    /** Weight class filter combo, plus "All". */
    private final JComboBox<String> chWeightClass = new JComboBox<>();
    /** Sort order combo; indices correspond to {@link #saSorts} and to {@code MekSummaryComparator}'s sort constants. */
    private final JComboBox<String> chSort = new JComboBox<>();
    /** SpringLayout container for the list + readout pane(s); rebuilt on every {@link #paintScreen(boolean)} call. */
    private final JPanel textBoxSpring = new JPanel(new SpringLayout());
    /** Top-level SpringLayout container for the whole window's contents; rebuilt on every {@link #paintScreen(boolean)} call. */
    private final JPanel springHolder = new JPanel(new SpringLayout());
    /** SpringLayout container for the fluff pane, shown only when fluff is being displayed. */
    private final JPanel fluffBoxSpring = new JPanel(new SpringLayout());
    private final JButton bCancel = new JButton("Close");
    /** "Select" button; only shown when {@link #viewerType} != {@link #UNIT_VIEWER} (see {@link #paintScreen(boolean)}). */
    private final JButton bSelect = new JButton("Select");
    /** HTML pane intended to show the unit's basic readout; population is currently disabled (see {@link #previewMech(Entity)}), so this pane is effectively always blank. */
    private final JTextPane mekViewLeft;
    /** HTML pane intended to show the unit's loadout readout; population is currently disabled (see {@link #previewMech(Entity)}), so this pane is effectively always blank. */
    private final JTextPane mekViewRight;
    /** HTML pane showing the selected unit's fluff/history text, when {@link #viewFluff} is enabled and the unit has fluff. */
    private final JTextPane unitFluff;
    /** Top panel: filter combos, preview image, and (when expanded) the advanced search rows. */
    private final JPanel pUpper = new JPanel();
    private final IClient client;
    /** Which of {@link #UNIT_VIEWER}, {@link #OMNI_VARIANT_SELECTOR}, {@link #UNIT_SELECTOR}, {@link #UNIT_RESEARCH} this instance is running as. */
    private final int viewerType;
    /** Whether the fluff pane should be shown; sourced from the {@code VIEW_FLUFF} client config parameter. */
    private final boolean viewFluff;
    /** Small panel holding just {@link #m_bToggleAdvanced}. */
    private final JPanel m_pOpenAdvanced = new JPanel();
    /** Button toggling the advanced search panel's visibility; its own label text doubles as the collapsed/expanded state flag (see {@link #toggleAdvanced()}). */
    private final JButton m_bToggleAdvanced = new JButton("< Show Advanced Search >");
    /** Comparison mode ("At Least"/"Equal To"/"No More Than") for the walk MP advanced-search filter. */
    private final JComboBox<String> m_cWalk = new JComboBox<>();
    /** Walk MP value for the advanced-search filter. */
    private final JTextField m_tWalk = new JTextField(2);
    /** Comparison mode for the jump MP advanced-search filter. */
    private final JComboBox<String> m_cJump = new JComboBox<>();
    /** Jump MP value for the advanced-search filter. */
    private final JTextField m_tJump = new JTextField(2);
    /** Armor threshold combo (Any/25%/50%/75%/90% of theoretical max armor) for the advanced-search filter. */
    private final JComboBox<String> m_cArmor = new JComboBox<>();
    /** Minimum count for the first "must carry weapon X" advanced-search row. */
    private final JTextField m_tWeapons1 = new JTextField(2);

    // private String selectedUnit = null;
    /** Weapon choice for the first "must carry weapon X" advanced-search row. */
    private final JComboBox<String> m_cWeapons1 = new JComboBox<>();
    /** Combines the two weapon-count rows: "or" (either sufficient) vs "and" (both required). */
    private final JComboBox<String> m_cOrAnd = new JComboBox<>();
    /** Minimum count for the second "must carry weapon X" advanced-search row. */
    private final JTextField m_tWeapons2 = new JTextField(2);
    /** Weapon choice for the second "must carry weapon X" advanced-search row. */
    private final JComboBox<String> m_cWeapons2 = new JComboBox<>();
    /** Whether the "must carry equipment X" advanced-search filter is active. */
    private final JCheckBox m_chkEquipment = new JCheckBox();
    /** Equipment choice for the "must carry equipment X" advanced-search filter. */
    private final JComboBox<String> m_cEquipment = new JComboBox<>();
    private final JButton m_bSearch = new JButton("Search");
    private final JButton m_bReset = new JButton("Reset");
    /** Shows "filtered/total" unit counts after a basic or advanced search. */
    private final JLabel m_lCount = new JLabel();
    /** Backing model for {@link #mekList}; one formatted line (see {@link #formatMek(MekSummary)}) per unit currently passing the active filters. */
    private final DefaultListModel<String> defaultModel;
    /** List widget showing the currently-filtered/sorted units; rows are index-aligned with {@link #meksCurrent}. */
    private final JList<String> mekList;
    private JScrollPane listScrollPane = null;
    private JScrollPane leftScrollPane = null;
    private JScrollPane rightScrollPane = null;
    private JScrollPane fluffScrollPane = null;
    /** The units currently passing the active basic (and, if run, advanced) filters, in sorted order; index-aligned with {@link #mekList}'s rows. */
    private MekSummary[] meksCurrent;
    /** Accumulates keystrokes for the type-ahead "jump to unit starting with..." search in {@link #keyPressed(KeyEvent)}. */
    private StringBuilder m_sbSearch = new StringBuilder();
    /** Timestamp (ms) of the last keystroke fed into {@link #m_sbSearch}; used to detect a stale/expired search via {@link #KEY_TIMEOUT}. */
    private long m_nLastSearch = 0;
    /** Small preview-image panel for the currently-selected unit; actually a {@link MekInfo} instance (cast where used). */
    private JPanel pPreview = new JPanel();
    /** Panel holding the basic/advanced search rows below the filter combos; rebuilt each time {@link #buildSouthParams(boolean)} runs. */
    private JPanel m_pSouthParams = new JPanel();
    /** Total number of units passing the basic filters (before any advanced search), used for the "n/total" count label. */
    private int m_count;
    /** Tech level filter index as of the last {@link #filterMeks(boolean)} call; used to detect when the weapon/equipment combos need repopulating. */
    private int m_old_nType;
    /** Unit type filter index as of the last {@link #filterMeks(boolean)} call; used to detect when the weapon/equipment combos need repopulating. */
    private int m_old_nUnitType;

    /**
     * Builds the unit viewer frame: sets the title according to {@code viewer}, wires up all the
     * filter/preview components, restores previously-saved filter selections from
     * {@code client}'s config, and registers this instance as the listener for every interactive
     * component. Does not make the frame visible — callers typically invoke {@link #run()} (on a
     * background thread, since it performs the first, potentially slow, unit filter pass) to do
     * that once units are ready.
     *
     * @param cMainFrame owning main client frame
     * @param uld        "please wait" dialog the caller is showing while units load; hidden by {@link #run()}
     * @param client     connection used to read config/server settings and send resulting chat commands
     * @param viewer     one of {@link #UNIT_VIEWER}, {@link #OMNI_VARIANT_SELECTOR}, {@link #UNIT_SELECTOR}, {@link #UNIT_RESEARCH}
     */
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

    /**
     * Fills the basic filter combos (weight class, tech level, unit type — each gaining a
     * trailing "All" entry) from MegaMek's constant tables, plus the static option lists for the
     * advanced-search movement/armor/or-and combos, then delegates to
     * {@link #populateWeaponsAndEquipmentChoices()} for the tech/type-dependent weapon and
     * equipment lists.
     */
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

    /**
     * Restores the user's previously-saved weight/tech/unit-type filter selections from
     * {@code client}'s config (persisted by {@link #saveComboBoxSettings()}), if any.
     */
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

    /**
     * (Re)builds the search panel below the filter combos. When collapsed, only the toggle
     * button is shown; when expanded, adds 4 rows: movement filters (walk/jump/armor), weapon
     * filters, the equipment filter, and the search/reset buttons plus count label. Always
     * finishes by re-laying-out the whole window via {@link #paintScreen(boolean)}.
     *
     * @param showAdvanced whether to build the expanded (advanced) layout or the collapsed one
     */
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

    /** Blanks all 3 preview text panes and clears the small preview image (equivalent to no unit selected). */
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

    /**
     * Rebuilds the weapon1/weapon2/equipment advanced-search combo contents to match the
     * currently-selected tech level and unit type filters (resetting the associated count fields
     * and the equipment checkbox in the process). Iterates every {@link EquipmentType} known to
     * MegaMek, applying tech-level-compatibility rules that mirror {@link #filterMeks(boolean)}'s
     * own tech-level logic, and — for weapons specifically — excluding infantry-only weapons when
     * the unit type filter is Mek or Tank (since those unit types can't mount them).
     */
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

    /**
     * Tears down and rebuilds the window's overall SpringLayout. With {@code fluff} true, adds a
     * 4th column (the fluff pane) alongside the list/left-readout/right-readout columns; with it
     * false, only the 3-column layout is built. Also conditionally includes the "Select" button
     * (omitted for pure {@link #UNIT_VIEWER} mode). Finishes by packing/repainting the window and
     * returning keyboard focus to {@link #mekList}.
     *
     * @param fluff whether the fluff/history column should be part of the layout
     */
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

    /**
     * Populates (or clears) the unit preview area for {@code entity}. When {@code entity} is
     * {@code null}, substitutes a placeholder unit from {@link UnitUtils#createOMG()} purely so
     * the layout has something valid to lay out, while suppressing actual readout text
     * ({@code populateTextFields} is forced false in that case).
     * <p>
     * Note: a {@link ConfigurableMekViewPanel} is constructed here mainly to verify the unit
     * loads without throwing; its actual readout text is <b>not</b> currently written into
     * {@link #mekViewLeft}/{@link #mekViewRight} — those two calls are commented out (see the
     * "Readouts Disabled" debug log below) — so those two panes are effectively always blank in
     * the current build regardless of which unit is selected. The fluff pane and preview image
     * are still populated normally.
     *
     * @param entity the unit to preview, or {@code null} to clear the preview
     */
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

    /**
     * {@link Runnable} entry point, intended to be executed on a background thread after the
     * caller has already made {@link #unitLoadingDialog} visible. Performs the (potentially slow)
     * initial basic filter pass via {@link #filterMeks()}, hides the loading dialog, surfaces any
     * per-file load failures encountered by {@link MekSummaryCache} via a self-showing
     * {@link UnitFailureDialog}, attempts to restore whichever unit was previously selected
     * (falling back to no selection if that unit no longer exists or config lookup fails), then
     * finally shows the preview panel and this frame itself.
     */
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

    /** Convenience overload: runs the basic filter pass without the "called from advanced search" flag. */
    private void filterMeks() {
        filterMeks(false);
    }

    /**
     * Overridden so every visibility change re-centers the window on screen and re-packs it; as a
     * side effect, the window will always snap back to the center of the screen rather than
     * respecting any position the user may have manually moved it to.
     */
    @Override
    public void setVisible(boolean show) {
        setLocationRelativeTo(null);
        super.setVisible(show);
        pack();
    }

    /**
     * The "basic" filter pass across every {@link MekSummary} in {@link MekSummaryCache}. Skips
     * entries whose name starts with "Error" (a hacky guard, per the comment below, against
     * corrupt/failed unit files leaking into the list). Applies the weight class / tech level /
     * unit type combo selections, including several special-cased "ALL"/"TW_ALL" branches that
     * mirror MegaMek's {@link TechConstants} groupings. On completion, updates
     * {@link #meksCurrent}/{@link #m_count}, refreshes the weapon/equipment combos only if the
     * tech or unit-type filter actually changed since the previous call (skipped when invoked
     * from the advanced search, since that reuses the current basic-filtered set), and re-sorts
     * the results via {@link #sortMeks()}.
     *
     * @param calledByAdvancedSearch true when re-run as part of {@link #advancedSearch()} resetting
     *                               to the full basic-filtered set, to avoid needlessly repopulating
     *                               the weapon/equipment combos
     */
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

    /**
     * Sorts {@link #meksCurrent} in place using {@code MekSummaryComparator} keyed by the selected
     * sort combo index (see the project-wide deprecation note on that comparator), then rebuilds
     * {@link #mekList}'s model from the sorted array via {@link #formatMek(MekSummary)}. Briefly
     * disables the list and shows a wait cursor while doing so (defensive against larger unit
     * caches, though typically fast) and refreshes the "n/total" count label.
     */
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

    /**
     * Builds one fixed-width display row for {@code mekSummary}: model, chassis, tonnage, BV,
     * and — only when the server config {@code UseCalculatedCosts} is enabled — a formatted cost
     * column. Column widths/padding are enforced via {@link #makeLength(String, int)}.
     */
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

    /**
     * Pads {@code string} with trailing spaces to exactly {@code nLength} characters, or truncates
     * and appends ".." if longer. (An equivalent private method exists in {@code ArmyViewerDialog};
     * they are not shared, just independently duplicated.)
     */
    private String makeLength(String string, int nLength) {
        if (string.length() == nLength) {
            return string;
        } else if (string.length() > nLength) {
            return String.format("%s..", string.substring(0, nLength - 2));
        } else {
            return string + SPACES.substring(0, nLength - string.length());
        }
    }

    /**
     * Swaps {@link #m_pSouthParams} for a freshly-built panel in the opposite (basic/advanced)
     * state. Determines the current state by comparing {@link #m_bToggleAdvanced}'s label text to
     * the literal collapsed-state string, rather than tracking a boolean flag — a somewhat
     * fragile mechanism should that label text ever be changed without updating this check.
     */
    private void toggleAdvanced() {
        pUpper.remove(m_pSouthParams);
        m_pSouthParams = new JPanel();
        buildSouthParams(m_bToggleAdvanced.getText().equals("< Show Advanced Search >"));
        pUpper.add(m_pSouthParams, java.awt.BorderLayout.SOUTH);
        // invalidate();
        pack();
        repaint();
    }

    /**
     * Implements the incremental type-ahead behavior for {@link #mekList}: jumps the list
     * selection to the first currently-visible unit whose name starts with {@code search}
     * (case-insensitive), scrolling it into view. No-op if nothing matches.
     */
    private void searchFor(String search) {
        for (int i = 0; i < meksCurrent.length; i++) {
            if (meksCurrent[i].getName().toLowerCase().startsWith(search)) {
                mekList.setSelectedIndex(i);
                mekList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    /**
     * Central command dispatcher for every button in this window.
     * <ul>
     * <li><b>Cancel</b>: persists the current filter/sort selections then disposes the window.</li>
     * <li><b>Select</b>: persists filter/sort selections, then branches on {@link #viewerType}:
     * <ul>
     * <li>{@link #OMNI_VARIANT_SELECTOR}: prompts, via a sequence of blocking
     * {@link JOptionPane#showInputDialog} calls on the EDT, for money/comp/fluff modifiers, then
     * sends an {@code AddOmniVariantMod} chat command. Backing out (cancel/empty) of any prompt
     * aborts the whole flow via early {@code dispose()} with no chat sent.</li>
     * <li>{@link #UNIT_SELECTOR}: similarly prompts for fluff text / gunnery / piloting / skills,
     * computes a weight-class index shifted down by 1 (since index 0 in {@link #chWeightClass} is
     * "All" — see the comment inline), then sends a {@code createunit} chat command.</li>
     * <li>{@link #UNIT_RESEARCH}: sends a {@code researchunit} chat command for the selected
     * unit's summary file, unless that file resolves to the literal string {@code "null"}.</li>
     * <li>otherwise ({@link #UNIT_VIEWER}): just disposes (this branch is normally unreachable
     * since the Select button isn't shown in that mode — see {@link #paintScreen(boolean)}).</li>
     * </ul>
     * </li>
     * <li><b>Search / Reset / Toggle Advanced</b>: delegate to {@link #advancedSearch()},
     * {@link #resetSearch()}, {@link #toggleAdvanced()} respectively.</li>
     * </ul>
     */
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

    /**
     * Runs the advanced-search predicate ({@link #isMatch(Entity)}) across the current candidate
     * set. Determines whether a previous advanced search already narrowed the list by parsing the
     * "n/total" text out of {@link #m_lCount} (rather than comparing {@link #meksCurrent}.length
     * to {@link #m_count} directly) and, if so, re-runs the basic filter first to reset back to
     * the full basic-filtered set before narrowing again.
     * <p>
     * For every remaining candidate, actually loads the full {@link Entity} from disk via
     * {@link MekFileParser} — this can be slow for a large candidate set, since it parses every
     * candidate's unit file rather than relying on the lighter-weight {@link MekSummary} data —
     * and keeps only those for which {@link #isMatch(Entity)} returns true. Replaces
     * {@link #meksCurrent} with the matches, clears the preview, and re-sorts/repaints.
     */
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

    /**
     * Evaluates the advanced-search predicate against a fully-loaded {@code entity}. Each filter
     * section below is only checked if the corresponding field/checkbox is actually populated;
     * every section can independently veto the match by returning {@code false}.
     * <ul>
     * <li><b>Walk/Jump MP</b>: only checked if the text field parses to a non-negative int;
     * compared using "at least" / "equal to" / "no more than" per the paired combo selection.</li>
     * <li><b>Armor</b>: only checked if the combo selection isn't "Any"; compares the entity's
     * total armor against a percentage of a theoretical max armor computed as
     * {@code 2 * totalInternal + 3} — an approximation, not an exact rules-accurate max armor
     * value.</li>
     * <li><b>Weapons</b>: two independent "at least N copies of weapon X" conditions. If only one
     * row is active, it alone must match. If both are active, they're combined via
     * {@link #m_cOrAnd} ("or" = either sufficient, "and" = both required).</li>
     * <li><b>Equipment</b>: if {@link #m_chkEquipment} is checked, requires at least one matching
     * {@link MiscType} and returns that result immediately — meaning when this checkbox is
     * checked, the equipment check is evaluated last and its outcome is the method's final
     * answer (a mismatch here can override an otherwise-passing walk/jump/armor/weapon match).
     * If unchecked, execution falls through to the trailing {@code return true}.</li>
     * </ul>
     *
     * @param entity the fully-loaded unit to test
     * @return true if {@code entity} satisfies every active advanced-search filter
     */
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

    /**
     * Clears every advanced-search field back to its default index/blank state, repopulates the
     * weapon/equipment combos, and re-runs the basic filter — discarding any advanced-search
     * narrowing currently in effect.
     */
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
     * ListSelectionListener callback: whenever the highlighted unit in {@link #mekList} changes,
     * loads the full {@link Entity} for the newly-selected {@link MekSummary} (via
     * {@link MekFileParser}, so this happens once per click) and refreshes the preview via
     * {@link #previewMech(Entity)}. Clears the preview if nothing is selected or the load fails.
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

    /**
     * ItemListener callback shared by {@link #chSort}, {@link #chWeightClass}, {@link #chType},
     * and {@link #chUnitType}: re-sorts if the sort combo changed, or re-filters if any of the 3
     * basic filter combos changed. Afterward, always attempts to restore whichever list entry was
     * previously selected by value; if that entry no longer exists after the filter/sort change,
     * the list automatically reverts to no selection.
     */
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

    /** KeyListener callback; intentionally unused. */
    public void keyTyped(KeyEvent keyEvent) {
    }

    /**
     * KeyListener callback. Enter or Escape synthesize a click on {@link #bCancel} (so either key
     * closes the window the same way Close would). Every keypress also feeds the type-ahead
     * search buffer {@link #m_sbSearch} — resetting it first if more than {@link #KEY_TIMEOUT} ms
     * have elapsed since the previous keystroke — and re-runs {@link #searchFor(String)}.
     */
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

    /** KeyListener callback; intentionally unused. */
    public void keyReleased(KeyEvent keyEvent) {
        // no action on release
    }

    /** WindowListener callback; intentionally unused. */
    public void windowOpened(WindowEvent windowEvent) {
    }

    /**
     * WindowListener callback: handles the OS window-close ("X") button by persisting the current
     * filter/sort selections and disposing, mirroring the same path taken by the Close button in
     * {@link #actionPerformed(ActionEvent)}.
     */
    public void windowClosing(WindowEvent windowEvent) {
        saveComboBoxSettings();
        dispose();
    }

    /**
     * Persists the current weight/tech/type/sort filter selections, and (if any) the currently
     * selected unit's list label, into {@code client}'s config, then saves and pushes the config.
     */
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

    /** WindowListener callback; intentionally unused. */
    public void windowClosed(WindowEvent windowEvent) {
    }

    /** WindowListener callback; intentionally unused. */
    public void windowIconified(WindowEvent windowEvent) {
    }

    /** WindowListener callback; intentionally unused. */
    public void windowDeiconified(WindowEvent windowEvent) {
    }

    // WindowListener
    /** WindowListener callback; intentionally unused. */
    public void windowActivated(WindowEvent windowEvent) {
    }

    /** WindowListener callback; intentionally unused. */
    public void windowDeactivated(WindowEvent windowEvent) {
    }
}
