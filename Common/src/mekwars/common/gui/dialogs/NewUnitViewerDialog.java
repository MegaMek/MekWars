package mekwars.common.gui.dialogs;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.PatternSyntaxException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import jakarta.annotation.Nullable;
import megamek.client.Client;
import megamek.client.ui.dialogs.UnitFailureDialog;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.dialogs.advancedsearch.AdvancedSearchDialog;
import megamek.client.ui.dialogs.advancedsearch.MekSearchFilter;
import megamek.client.ui.dialogs.unitSelectorDialogs.ConfigurableMekViewPanel;
import megamek.client.ui.enums.DialogResult;
import megamek.codeUtilities.MathUtility;
import megamek.common.TechConstants;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.UnitUtils;

/**
 * Modal dialog for browsing the full MegaMek unit database ({@link MekSummaryCache}) and picking a single unit,
 * used in several different contexts depending on {@link #viewerType}:
 * <ul>
 *     <li>{@link #UNIT_VIEWER} &mdash; plain browse/inspect mode (e.g. from a "view unit" menu action).</li>
 *     <li>{@link #OMNI_VARIANT_SELECTOR} &mdash; pick a unit to attach an OmniMech variant price/BV/fluff
 *     modifier to; on selection prompts for money/component/fluff modifiers and sends an
 *     {@code AddOmniVariantMod} campaign command.</li>
 *     <li>{@link #UNIT_SELECTOR} &mdash; pick a unit to actually create as a new in-game unit; on selection
 *     prompts for fluff text, gunnery/piloting skill, and a skills list, then sends a {@code createUnit}
 *     campaign command.</li>
 *     <li>{@link #UNIT_RESEARCH} &mdash; pick a unit to mark as "researched" for the player's house; sends a
 *     {@code researchunit} campaign command.</li>
 * </ul>
 * The dialog offers filtering by tech level, weight class, unit type, a free-text name filter, and MegaMek's
 * {@link AdvancedSearchDialog}, plus a live {@link ConfigurableMekViewPanel} preview and a "Show BV Calculation"
 * popup. Because scanning the unit cache can be slow, the actual data load happens on a background thread via
 * {@link #run()} (the dialog implements {@link Runnable}), while an {@link UnitLoadingDialog} is shown to the
 * user in the meantime.
 */
public class NewUnitViewerDialog extends JDialog implements Runnable, KeyListener, ActionListener {
    /** Plain unit browser/inspector mode; selecting a unit has no side effect beyond closing the dialog. */
    public static final int UNIT_VIEWER = 0;
    /** Mode for picking a unit to attach an OmniMech variant price/BV/fluff modifier to. */
    public static final int OMNI_VARIANT_SELECTOR = 1;
    /** Mode for picking a unit to create as a brand-new in-game unit (prompts for fluff/skills). */
    public static final int UNIT_SELECTOR = 2;
    /** Mode for picking a unit to mark as researched/unlocked for the player's house. */
    public static final int UNIT_RESEARCH = 3;
    @Serial
    private static final long serialVersionUID = 8144354264100884817L;
    private static final MMLogger LOGGER = MMLogger.create(NewUnitViewerDialog.class);
    /** Milliseconds of inactivity after which the type-ahead search buffer ({@link #searchBuffer}) resets. */
    private final static int KEY_TIMEOUT = 1000;
    /** Table model exposing {@link #meks} (chassis/model/weight/cost/BV/etc columns) to {@link #tableUnits}. */
    private final MekTableModel unitModel;
    /** "Please wait" dialog shown to the user while {@link #run()} loads the unit cache in the background. */
    private final UnitLoadingDialog unitLoadingDialog;
    /** Throwaway MegaMek {@link Client} instance, used only to read game options (e.g. "canon_only") for filtering. */
    private final Client mmClient = new Client("temp", "None", 0);
    /** Which mode this dialog instance is operating in; one of {@link #UNIT_VIEWER}, {@link #OMNI_VARIANT_SELECTOR},
     *  {@link #UNIT_SELECTOR}, or {@link #UNIT_RESEARCH}. */
    private final int viewerType;
    /** Back-link to the campaign client, used to read server configs and send campaign commands on selection. */
    private final IClient client;
    /** MegaMek's built-in advanced unit search dialog, layered on top of the basic combo-box/text filters. */
    private final AdvancedSearchDialog advancedSearchDialog;
    /** Free-text chassis/model name filter applied in addition to the combo box filters. */
    private JTextField txtFilter;
    /** Confirms the current selection and immediately closes the dialog. */
    private JButton btnSelectClose;
    /** Confirms the current selection without closing the dialog (allows repeated selection). */
    private JButton btnSelect;
    /** Closes the dialog without making a selection. */
    private JButton btnClose;
    /** Opens a popup showing the Battle Value calculation breakdown for the selected unit. */
    private JButton btnShowBV;
    /** Opens MegaMek's {@link AdvancedSearchDialog}. */
    private JButton btnAdvSearch;
    /** Clears any active advanced search filter; disabled unless one is currently applied. */
    private JButton btnResetSearch;
    /** Tech level filter (Introductory/Standard/Advanced/etc, from {@link TechConstants}). */
    private JComboBox<String> comboType;
    /** Unit type filter (Mek/Vehicle/Aero/etc, from {@link UnitType}), with a leading "All" option. */
    private JComboBox<String> comboUnitType;
    /** Weight class filter (from {@link EntityWeightClass}), with a trailing "All" option. */
    private JComboBox<String> comboWeight;
    /** Displays a preview image/icon for the selected unit. */
    private JLabel lblImage;
    /** Sortable/filterable table of all units in {@link #meks}. */
    private JTable tableUnits;
    /** Read-only MegaMek unit summary/record sheet panel showing full details of the selected unit. */
    private ConfigurableMekViewPanel panelMekView;
    /** Accumulates recently typed characters for type-ahead "jump to unit" searching (see {@link #searchFor}). */
    private StringBuffer searchBuffer = new StringBuffer();
    /** Timestamp (ms) of the last keystroke, used to decide when to reset {@link #searchBuffer}. */
    private long lastSearch = 0;
    /** All known units loaded from {@link MekSummaryCache}; indices correspond to {@link #unitModel}'s rows. */
    private MekSummary[] meks;
    /** Active advanced search filter built by {@link #advancedSearchDialog}, or {@code null} if none is applied. */
    private MekSearchFilter searchFilter;
    /** Row sorter/filter driving {@link #tableUnits}; combines combo-box, text, and advanced-search filtering. */
    private TableRowSorter<MekTableModel> sorter;
    /** Persisted selected index of {@link #comboUnitType}, restored the next time the dialog is shown. */
    private int selectedUnitType;
    /** Persisted selected index of {@link #comboWeight}, restored the next time the dialog is shown. */
    private int selectedUnitWeight;
    /** Persisted selected index of {@link #comboType} (tech/rules level), restored the next time the dialog is shown. */
    private int selectedUnitRulesLevel;
    /** Last known dialog height, captured on deactivation; currently only stored, never re-applied on show. */
    private int selectorSizeHeight;
    /** Last known dialog width, captured on deactivation; currently only stored, never re-applied on show. */
    private int selectorSizeWidth;

    /**
     * Creates new form UnitSelectorDialog
     * <p>
     * Builds the dialog chrome via {@link #initComponents()}, sets its title based on {@code viewer} mode, sizes
     * and centers it over {@code mainFrame}, and constructs the {@link AdvancedSearchDialog} using the server's
     * configured campaign year (defaulting to 3055 if unset/unparsable). Note that this constructor does not load
     * any unit data or show the dialog &mdash; the caller is expected to run this object (it implements
     * {@link Runnable}) on a background thread and then call {@link #setVisible(boolean)}.
     *
     * @param mainFrame the owning application frame, used for modality and centering
     * @param uld       loading-indicator dialog to hide once background unit loading completes in {@link #run()}
     * @param client    the campaign client used for server configs and sending selection commands
     * @param viewer    one of {@link #UNIT_VIEWER}, {@link #OMNI_VARIANT_SELECTOR}, {@link #UNIT_SELECTOR}, or
     *                  {@link #UNIT_RESEARCH}, selecting this dialog's behavior on unit selection
     */
    public NewUnitViewerDialog(JFrame mainFrame, UnitLoadingDialog uld, IClient client, int viewer) {
        super(mainFrame, "Unit Viewer", true); //$NON-NLS-1$
        this.client = client;

        viewerType = viewer;

        if (viewerType == NewUnitViewerDialog.OMNI_VARIANT_SELECTOR) {
            setTitle("Omni Variant Selector");
        } else if (viewerType == NewUnitViewerDialog.UNIT_SELECTOR) {
            setTitle("Unit Selector");
        }

        unitLoadingDialog = uld;

        unitModel = new MekTableModel();
        initComponents();
        int width = 800;
        int height = 600;
        setSize(width, height);
        setLocationRelativeTo(mainFrame);
        advancedSearchDialog = new AdvancedSearchDialog(mainFrame,
              MathUtility.parseInt(this.client.getServerConfigs("CampaignYear"),
                    3055));
    }

    /**
     * Lays out every Swing component in the dialog: the unit table and its scroll pane, the tech/weight/unit-type
     * filter combos, the free-text filter field, the preview image label, the advanced-search buttons, the
     * OK/close/BV buttons, and the {@link ConfigurableMekViewPanel} detail view, all wired up with their listeners.
     * Called once from the constructor; contains no unit data loading (that happens later in {@link #run()}).
     */
    private void initComponents() {
        setMinimumSize(new Dimension(640, 480));

        GridBagConstraints gridBagConstraints;

        JPanel selectionPanel = new JPanel(new GridBagLayout());
        selectionPanel.setMinimumSize(new Dimension(500, 500));
        selectionPanel.setPreferredSize(new Dimension(500, 600));

        JPanel panelFilterButtons = new JPanel();
        JPanel panelSearchButtons = new JPanel();
        JPanel panelOKButtons = new JPanel();

        JScrollPane scrTableUnits = new JScrollPane();
        tableUnits = new JTable();
        tableUnits.addKeyListener(this);
        tableUnits.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "");
        panelMekView = new ConfigurableMekViewPanel();
        panelMekView.setMinimumSize(new Dimension(300, 500));
        panelMekView.setPreferredSize(new Dimension(300, 600));

        comboType = new JComboBox<>();
        comboWeight = new JComboBox<>();
        comboUnitType = new JComboBox<>();
        txtFilter = new JTextField();

        btnSelect = new JButton();
        btnSelectClose = new JButton();
        btnClose = new JButton();
        btnShowBV = new JButton();
        btnAdvSearch = new JButton();
        btnResetSearch = new JButton();

        JLabel lblType = new JLabel("Type");
        JLabel lblWeight = new JLabel("Weight Class");
        JLabel lblUnitType = new JLabel("Unit Type");
        JLabel lblFilter = new JLabel("Filter");
        lblImage = new JLabel();

        getContentPane().setLayout(new GridBagLayout());

        scrTableUnits.setMinimumSize(new Dimension(500, 400));
        scrTableUnits.setPreferredSize(new Dimension(500, 400));

        tableUnits.setModel(unitModel);
        tableUnits.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorter = new TableRowSorter<>(unitModel);
        tableUnits.setRowSorter(sorter);

        tableUnits.getSelectionModel().addListSelectionListener(
              evt -> {
                  if (!evt.getValueIsAdjusting()) {
                      refreshUnitView();
                  }
              });

        TableColumn column;
        for (int i = 0; i < MekTableModel.N_COL; i++) {
            column = tableUnits.getColumnModel().getColumn(i);

            if (i == MekTableModel.COL_CHASSIS) {
                column.setPreferredWidth(125);
            } else if ((i == MekTableModel.COL_MODEL) || (i == MekTableModel.COL_COST)) {
                column.setPreferredWidth(75);
            } else if ((i == MekTableModel.COL_WEIGHT) || (i == MekTableModel.COL_BV)) {
                column.setPreferredWidth(50);
            } else {
                column.setPreferredWidth(25);
            }
        }

        tableUnits.setFont(new Font("Monospaced", Font.PLAIN, 12)); //$NON-NLS-1$
        scrTableUnits.setViewportView(tableUnits);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        selectionPanel.add(scrTableUnits, gridBagConstraints);

        panelFilterButtons.setMinimumSize(new Dimension(300, 120));
        panelFilterButtons.setPreferredSize(new Dimension(300, 120));
        panelFilterButtons.setLayout(new GridBagLayout());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(lblType, gridBagConstraints);

        DefaultComboBoxModel<String> techModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < TechConstants.SIZE; i++) {
            techModel.addElement(TechConstants.getLevelDisplayableName(i));
        }
        techModel.setSelectedItem(TechConstants.getLevelDisplayableName(0));
        comboType.setModel(techModel);
        comboType.setMinimumSize(new Dimension(200, 27));
        comboType.setPreferredSize(new Dimension(200, 27));
        comboType.addActionListener(this);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(comboType, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(lblWeight, gridBagConstraints);

        DefaultComboBoxModel<String> weightModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < EntityWeightClass.SIZE; i++) {
            weightModel.addElement(EntityWeightClass.getClassName(i));
        }
        weightModel.addElement("All"); //$NON-NLS-1$
        weightModel.setSelectedItem(EntityWeightClass.getClassName(0));
        comboWeight.setModel(weightModel);
        comboWeight.setSelectedItem("All");
        comboWeight.setMinimumSize(new Dimension(200, 27));
        comboWeight.setPreferredSize(new Dimension(200, 27));
        comboWeight.addActionListener(this);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(comboWeight, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(lblUnitType, gridBagConstraints);

        DefaultComboBoxModel<String> unitTypeModel = new DefaultComboBoxModel<>();
        unitTypeModel.addElement("All");
        unitTypeModel.setSelectedItem("All");

        for (int i = 0; i < UnitType.SIZE; i++) {
            unitTypeModel.addElement(UnitType.getTypeDisplayableName(i));
        }

        comboUnitType.setModel(unitTypeModel);
        comboUnitType.setMinimumSize(new Dimension(200, 27));
        comboUnitType.setPreferredSize(new Dimension(200, 27));
        comboUnitType.addActionListener(this);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(comboUnitType, gridBagConstraints);

        txtFilter.setText("");
        txtFilter.setMinimumSize(new Dimension(200, 28));
        txtFilter.setPreferredSize(new Dimension(200, 28));

        txtFilter.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterUnits();
            }

            public void removeUpdate(DocumentEvent e) {
                filterUnits();
            }

            public void changedUpdate(DocumentEvent e) {
                filterUnits();
            }
        });

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(txtFilter, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(lblFilter, gridBagConstraints);

        lblImage.setHorizontalAlignment(SwingConstants.CENTER);
        lblImage.setText(""); // NOI18N
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panelFilterButtons.add(lblImage, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.insets = new Insets(10, 10, 10, 0);
        selectionPanel.add(panelFilterButtons, gridBagConstraints);

        panelSearchButtons.setLayout(new GridBagLayout());

        btnAdvSearch.setText("Advanced Search"); //$NON-NLS-1$
        btnAdvSearch.addActionListener(this);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelSearchButtons.add(btnAdvSearch, gridBagConstraints);

        btnResetSearch.setText("Reset"); //$NON-NLS-1$
        btnResetSearch.addActionListener(this);
        btnResetSearch.setEnabled(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelSearchButtons.add(btnResetSearch, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.insets = new Insets(10, 10, 10, 0);
        selectionPanel.add(panelSearchButtons, gridBagConstraints);

        panelOKButtons.setLayout(new GridBagLayout());

        btnSelect.setText("Select");
        btnSelect.addActionListener(this);
        panelOKButtons.add(btnSelect, new GridBagConstraints());

        btnSelectClose.setText("Select & Close");
        btnSelectClose.addActionListener(this);
        panelOKButtons.add(btnSelectClose, new GridBagConstraints());

        btnClose.setText("Close");
        btnClose.addActionListener(this);
        panelOKButtons.add(btnClose, new GridBagConstraints());

        btnShowBV.setText("Show BV Calculation"); //$NON-NLS-1$
        btnShowBV.addActionListener(this);
        panelOKButtons.add(btnShowBV, new GridBagConstraints());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, selectionPanel, panelMekView);
        splitPane.setResizeWeight(0);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = gridBagConstraints.weighty = 1;
        getContentPane().add(splitPane, gridBagConstraints);

        gridBagConstraints.insets = new Insets(5, 0, 5, 0);
        gridBagConstraints.weightx = gridBagConstraints.weighty = 0;
        gridBagConstraints.gridy = 1;
        getContentPane().add(panelOKButtons, gridBagConstraints);

        pack();
    }

    /**
     * Updates the {@link #panelMekView} preview panel to match the currently selected row in {@link #tableUnits},
     * or clears the preview (and the unit image) if nothing is selected. Invoked from the table's selection
     * listener whenever the selection settles (i.e. not while the mouse drag is still adjusting it).
     */
    void refreshUnitView() {
        Entity selectedUnit = getSelectedEntity();
        // null entity, so load a default unit.
        if (selectedUnit == null) {
            panelMekView.reset();
            lblImage.setIcon(null);
            return;
        }

        panelMekView.setEntity(selectedUnit);
    }

    /**
     * Rebuilds and applies the {@link #sorter}'s row filter based on the current tech-level, weight-class, and
     * unit-type combo selections, the {@link #advancedSearchDialog}'s {@link #searchFilter} (if any), the free-text
     * {@link #txtFilter} substring match against unit name, the MegaMek "canon_only" game option, and a hard cutoff
     * excluding any unit introduced after the server's configured {@code CampaignYear} (default 3025 if unset).
     * <p>
     * The tech-level matching logic in particular is quite involved: besides an exact match, it accepts several
     * "bucket" selections (T_ALL, T_IS_TW_ALL, T_TW_ALL, T_ALL_IS, T_ALL_CLAN) each of which ORs together several
     * concrete {@link TechConstants} levels to approximate "all Inner Sphere", "all Clan", etc. If the filter text
     * fails to compile as a valid pattern (unlikely here, but guarded against), filtering is silently skipped and
     * the previous filter remains in effect.
     */
    void filterUnits() {
        RowFilter<MekTableModel, Integer> unitTypeFilter;
        final int nType = comboType.getSelectedIndex();
        final int nClass = comboWeight.getSelectedIndex();
        final int nUnit = comboUnitType.getSelectedIndex() - 1;
        //If current expression doesn't parse, don't update.

        try {
            unitTypeFilter = new RowFilter<>() {
                @Override
                public boolean include(RowFilter.Entry<? extends MekTableModel, ? extends Integer> entry) {
                    MekTableModel mekModel = entry.getModel();
                    MekSummary mek = mekModel.getMechSummary(entry.getIdentifier());
                    if (((nClass == EntityWeightClass.SIZE) || (mek.getWeightClass() == nClass)) &&
                              (!mmClient.getGame().getOptions().booleanOption("canon_only") || mek.isCanon()) &&
                              ((nType == TechConstants.T_ALL)
                                     || (nType == mek.getType())
                                     || ((nType == TechConstants.T_IS_TW_ALL)
                                               && ((mek.getType() <= TechConstants.T_IS_TW_NON_BOX)
                                                         || (mek.getType() == TechConstants.T_INTRO_BOX_SET)))
                                     || ((nType == TechConstants.T_TW_ALL)
                                               && ((mek.getType() <= TechConstants.T_IS_TW_NON_BOX)
                                                         || (mek.getType() <= TechConstants.T_INTRO_BOX_SET)
                                                         || (mek.getType() <= TechConstants.T_CLAN_TW)))
                                     || ((nType == TechConstants.T_ALL_IS)
                                               && ((mek.getType() <= TechConstants.T_IS_TW_NON_BOX)
                                                         || (mek.getType() == TechConstants.T_INTRO_BOX_SET)
                                                         || (mek.getType() == TechConstants.T_IS_ADVANCED)
                                                         || (mek.getType() == TechConstants.T_IS_EXPERIMENTAL)
                                                         || (mek.getType() == TechConstants.T_IS_UNOFFICIAL)))
                                     || ((nType == TechConstants.T_ALL_CLAN)
                                               && ((mek.getType() == TechConstants.T_CLAN_TW)
                                                         || (mek.getType() == TechConstants.T_CLAN_ADVANCED)
                                                         || (mek.getType() == TechConstants.T_CLAN_EXPERIMENTAL)
                                                         || (mek.getType() == TechConstants.T_CLAN_UNOFFICIAL))))
                              && ((nUnit == -1) || mek.getUnitType().equals(UnitType.getTypeName(nUnit)))
                              && ((searchFilter == null) || MekSearchFilter.isMatch(mek, searchFilter))
                              && !(mek.getYear() > MathUtility.parseInt(client.getServerConfigs("CampaignYear"),
                          3025))) {
                        if (!txtFilter.getText().isEmpty()) {
                            String text = txtFilter.getText();
                            return mek.getName().toLowerCase().contains(text.toLowerCase());
                        }

                        return true;
                    }

                    return false;
                }
            };
        } catch (PatternSyntaxException e) {
            return;
        }

        sorter.setRowFilter(unitTypeFilter);
    }

    /**
     * Resolves the currently selected table row to a fully-loaded MegaMek {@link Entity}, re-parsing it from disk
     * each time (rather than caching), since {@link MekSummary} only holds lightweight metadata.
     *
     * @return the loaded {@link Entity} for the selected row, or {@code null} if no row is selected (e.g. the
     *         selection was filtered away) or the unit file failed to load (logged as an error)
     */
    public @Nullable Entity getSelectedEntity() {
        int view = tableUnits.getSelectedRow();

        if (view < 0) {
            // selection got filtered away
            return null;
        }

        int selected = tableUnits.convertRowIndexToModel(view);
        // else
        MekSummary mekSummary = meks[selected];
        try {
            // For some unknown reason the base path gets screwed up after you
            //  print, so this sets the source file to the full path.
            return new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName()).getEntity();
        } catch (EntityLoadingException ex) {
            LOGGER.error(ex,
                  String.format("Unable to load mech: %s: %s: %s", mekSummary.getSourceFile(), mekSummary.getEntryName(), ex.getMessage()));
            return null;
        }
    }

    /**
     * Background-thread entry point (this dialog is handed to a {@link Thread} by its caller). Loads the full
     * MegaMek unit cache into {@link #meks}, hands it to {@link #unitModel}, applies the initial filter, sorts the
     * table alphabetically by chassis, hides the {@link #unitLoadingDialog}, surfaces a failure report dialog if
     * any unit files failed to parse, and finally sizes and shows this dialog.
     */
    public void run() {
        // Loading meks can take a while, so it will have its own thread. This prevents the UI from freezing and
        // allows the "Please wait..." dialog to behave properly on various Java VMs.
        MekSummaryCache mekSummaryCache = MekSummaryCache.getInstance();
        meks = mekSummaryCache.getAllMeks();

        // break out if there are no units to filter
        if (meks == null) {
            LOGGER.error("No units to filter!");
        } else {
            unitModel.setData(meks);
        }

        filterUnits();

        //initialize with the units sorted alphabetically by chassis
        ArrayList<RowSorter.SortKey> sortList = new ArrayList<>();
        sortList.add(new RowSorter.SortKey(MekTableModel.COL_CHASSIS, SortOrder.ASCENDING));
        tableUnits.getRowSorter().setSortKeys(sortList);
        ((DefaultRowSorter<?, ?>) tableUnits.getRowSorter()).sort();

        tableUnits.invalidate(); // force re-layout of window
        pack();

        unitLoadingDialog.setVisible(false);

        // In some cases, it's possible to get here without an initialized
        // instance (loading a saved game without a cache).  In these cases,
        // we don't care about the failed loads.
        if (mekSummaryCache.isInitialized()) {
            final Map<String, String> hFailedFiles = MekSummaryCache.getInstance().getFailedFiles();
            if ((hFailedFiles != null) && (!hFailedFiles.isEmpty())) {
                // self-showing dialog
                new UnitFailureDialog(client.getMainFrame(), hFailedFiles);
            }
        }

        int width = 800;
        int height = 600;
        setSize(width, height);
        setVisible(true);
    }

    /**
     * When showing the dialog, restores the previously-selected type/weight/tech-level combo indices (persisted
     * via {@link #processWindowEvent}), then unconditionally clears any active advanced search filter and
     * re-applies the basic filters. Note that clearing the advanced search filter on every show means an advanced
     * search does not survive closing and reopening this dialog, even though the basic combo selections do.
     *
     * @param visible {@code true} to show the dialog, {@code false} to hide it
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            comboUnitType.setSelectedIndex(selectedUnitType);
            comboWeight.setSelectedIndex(selectedUnitWeight);
            comboType.setSelectedIndex(selectedUnitRulesLevel);
        }

        advancedSearchDialog.clearSearches();
        searchFilter = null;
        btnResetSearch.setEnabled(false);

        filterUnits();
        super.setVisible(visible);
    }

    /**
     * On window deactivation (e.g. the dialog loses focus or is closed), snapshots the current combo selections
     * and the dialog's current size into {@link #selectedUnitType}/{@link #selectedUnitWeight}/
     * {@link #selectedUnitRulesLevel}/{@link #selectorSizeHeight}/{@link #selectorSizeWidth} for later reuse.
     *
     * @param windowEvent the window event; only {@link WindowEvent#WINDOW_DEACTIVATED} is acted on
     */
    @Override
    protected void processWindowEvent(WindowEvent windowEvent) {
        super.processWindowEvent(windowEvent);
        if (windowEvent.getID() == WindowEvent.WINDOW_DEACTIVATED) {
            selectedUnitType = comboUnitType.getSelectedIndex();
            selectedUnitWeight = comboWeight.getSelectedIndex();
            selectedUnitRulesLevel = comboType.getSelectedIndex();
            setSelectorSizeHeight(getSize().height);
            setSelectorSizeWidth(getSize().width);
        }
    }

    /** Unused; required by {@link KeyListener} but this dialog only reacts to key-press events. */
    public void keyTyped(KeyEvent keyEvent) {
    }

    /**
     * Handles keyboard input on the unit table: Enter acts as if {@link #btnSelect} were clicked, and any other
     * character is appended to the type-ahead {@link #searchBuffer} (reset first if more than {@link #KEY_TIMEOUT}
     * ms have elapsed since the last keystroke) to jump the selection to the first matching chassis name via
     * {@link #searchFor}.
     *
     * @param keyEvent the key press event from {@link #tableUnits}
     */
    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
            ActionEvent event = new ActionEvent(btnSelect, ActionEvent.ACTION_PERFORMED, "");
            actionPerformed(event);
        }
        long curTime = System.currentTimeMillis();

        if ((curTime - lastSearch) > KEY_TIMEOUT) {
            searchBuffer = new StringBuffer();
        }

        lastSearch = curTime;
        searchBuffer.append(keyEvent.getKeyChar());
        searchFor(searchBuffer.toString().toLowerCase());
    }

    /**
     * Central handler for every button/combo action in this dialog.
     * <ul>
     *     <li>Combo box changes re-apply filtering via {@link #filterUnits()}.</li>
     *     <li>{@link #btnClose} hides the dialog without a selection.</li>
     *     <li>{@link #btnShowBV} forces a BV recalculation on the selected entity and shows it in a popup.</li>
     *     <li>{@link #btnAdvSearch} opens the advanced search dialog and enables/disables the reset button
     *     based on whether the user confirmed a search.</li>
     *     <li>{@link #btnResetSearch} clears the advanced search filter.</li>
     *     <li>{@link #btnSelect}/{@link #btnSelectClose} persist combo settings, then branch on
     *     {@link #viewerType}: for {@link #OMNI_VARIANT_SELECTOR} it prompts (via blocking input dialogs) for
     *     money/component/fluff modifiers and sends an {@code AddOmniVariantMod} command; for
     *     {@link #UNIT_SELECTOR} it prompts for fluff text, gunnery, piloting, and a comma-delimited skills list
     *     and sends a {@code createUnit} command; for {@link #UNIT_RESEARCH} it sends a {@code researchunit}
     *     command; otherwise it just disposes the dialog. In the prompt-driven branches, cancelling (or leaving
     *     blank) any prompt aborts the whole sequence and disposes the dialog without sending a command.</li>
     * </ul>
     * Any exception raised while building/sending these commands is caught and logged rather than propagated.
     *
     * @param actionEvent the triggering UI event; its source identifies which control fired
     */
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(comboType) ||
                  actionEvent.getSource().equals(comboWeight) ||
                  actionEvent.getSource().equals(comboUnitType)) {
            filterUnits();
        } else if (actionEvent.getSource().equals(btnClose)) {
            setVisible(false);
        } else if (actionEvent.getSource().equals(btnShowBV)) {
            JEditorPane tEditorPane = new JEditorPane();
            tEditorPane.setContentType("text/html");
            tEditorPane.setEditable(false);
            Entity entity = getSelectedEntity();

            if (null == entity) {
                return;
            }

            entity.calculateBattleValue();
            tEditorPane.setText(String.format("%s", entity.getUseManualBV() ? entity.getManualBV() : entity.getInitialBV()));
            tEditorPane.setCaretPosition(0);
            JScrollPane tScroll = new JScrollPane(tEditorPane,
                  ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                  ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Dimension size = new Dimension(550, 300);
            tScroll.setPreferredSize(size);
            JOptionPane.showMessageDialog(null,
                  tScroll,
                  "BV",
                  JOptionPane.INFORMATION_MESSAGE,
                  null);
        } else if (actionEvent.getSource().equals(btnAdvSearch)) {
            DialogResult result = advancedSearchDialog.showDialog();
            btnResetSearch.setEnabled((result.isConfirmed()));
            filterUnits();
        } else if (actionEvent.getSource().equals(btnResetSearch)) {
            advancedSearchDialog.clearSearches();
            searchFilter = null;
            btnResetSearch.setEnabled(false);
            filterUnits();
        } else if (actionEvent.getSource().equals(btnSelect) || actionEvent.getSource().equals(btnSelectClose)) {
            saveComboBoxSettings();
            if (viewerType == NewUnitViewerDialog.OMNI_VARIANT_SELECTOR) {
                try {
                    MekSummary selectedMekSummary = getSelectedMekSummary();
                    if (selectedMekSummary != null) {
                        String unit = selectedMekSummary.getName();
                        setVisible(false);
                        String moneyMod = JOptionPane.showInputDialog(client.getMainFrame(),
                              String.format("Money Mod for %s", unit),
                              0);

                        if ((moneyMod == null) || (moneyMod.isEmpty())) {
                            dispose();
                            return;
                        }

                        String compMod = JOptionPane.showInputDialog(client.getMainFrame(),
                              String.format("Comp Mod for %s", unit),
                              0);

                        if ((compMod == null) || (compMod.isEmpty())) {
                            dispose();
                            return;
                        }

                        String fluMod = JOptionPane.showInputDialog(client.getMainFrame(),
                              String.format("Flu Mod for %s", unit),
                              0);

                        if ((fluMod == null) || (fluMod.isEmpty())) {
                            dispose();
                            return;
                        }

                        client.sendChat(
                              String.format("%sc AddOmniVariantMod#%s#%s$%s$%s", IClient.CAMPAIGN_PREFIX, unit, moneyMod, compMod, fluMod));
                    }
                    dispose();
                } catch (Exception ex) {
                    LOGGER.error(ex, "Unable to perform action (AddOmniVariantMod): {}", ex.getLocalizedMessage());
                }
            } else if (viewerType == NewUnitViewerDialog.UNIT_SELECTOR) {
                try {
                    MekSummary selectedMekSummary = getSelectedMekSummary();
                    if (selectedMekSummary != null) {
                        String unitFile;
                        String unit = selectedMekSummary.getName();
                        setVisible(false);
                        int weightClass = comboWeight.getSelectedIndex();
                        // Item "All" takes up Weight Class 0, so this is usually 1 off.

                        if (weightClass > 0) {
                            weightClass -= 1;
                        }

                        unitFile = UnitUtils.getMekSummaryFileName(selectedMekSummary);


                        String fluff = JOptionPane.showInputDialog(client.getMainFrame(), String.format("Fluff text for %s", unit));

                        if ((fluff == null) || (fluff.isEmpty())) {
                            dispose();
                            return;
                        }

                        String gunnery = JOptionPane.showInputDialog(client.getMainFrame(),
                              String.format("Gunnery skill for %s", unit),
                              99);

                        if ((gunnery == null) || (gunnery.isEmpty())) {
                            dispose();
                            return;
                        }

                        String piloting = JOptionPane.showInputDialog(client.getMainFrame(),
                              String.format("Piloting Mod for %s", unit),
                              99);

                        if ((piloting == null) || (piloting.isEmpty())) {
                            dispose();
                            return;
                        }

                        String skills;
                        skills = JOptionPane.showInputDialog(client.getMainFrame(),
                              String.format("Skills Mod for %s (comma delimited)", unit));

                        if (skills == null) {
                            dispose();
                            return;
                        }

                        client.sendChat(
                              String.format("%sc createUnit#%s#%s#%s#%s#%s#%s", IClient.CAMPAIGN_PREFIX, unitFile, fluff, gunnery, piloting, weightClass, skills));
                    }

                    dispose();
                } catch (Exception ex) {
                    LOGGER.error(ex, "Unable to perform action (createUnit): {}", ex.getLocalizedMessage());
                }
            } else if (viewerType == NewUnitViewerDialog.UNIT_RESEARCH) {
                MekSummary selectedMekSummary = getSelectedMekSummary();

                if (selectedMekSummary != null) {
                    String unitFile;
                    unitFile = UnitUtils.getMekSummaryFileName(selectedMekSummary);
                    setVisible(false);

                    if (!unitFile.equals("null")) {
                        client.sendChat(String.format("%sc researchunit#%s", IClient.CAMPAIGN_PREFIX, unitFile));
                    }
                }
                dispose();

            } else {
                dispose();
            }
        }

    }

    /**
     * Type-ahead helper: scans {@link #meks} in underlying-model order for the first entry whose name starts with
     * {@code search}, and if it is currently visible (not filtered out), selects it in {@link #tableUnits}.
     *
     * @param search lower-cased search prefix accumulated from recent keystrokes (see {@link #keyPressed})
     */
    private void searchFor(String search) {
        for (int i = 0; i < meks.length; i++) {
            if (meks[i].getName().toLowerCase().startsWith(search)) {
                int selected = tableUnits.convertRowIndexToView(i);

                if (selected > -1) {
                    tableUnits.changeSelection(selected, 0, false, false);
                    break;
                }
            }
        }
    }

    /**
     * Persists the current weight/tech/unit-type combo selections into the client's local config so they are
     * restored the next time a unit viewer/selector dialog is opened (across dialog instances, not just via
     * {@link #selectedUnitType} etc. which only survive show/hide of this instance).
     */
    private void saveComboBoxSettings() {

        client.getConfig().setParam("UNIT_VIEWER_WEIGHT", (String) comboWeight.getSelectedItem());
        client.getConfig().setParam("UNIT_VIEWER_TECH", (String) comboType.getSelectedItem());
        client.getConfig().setParam("UNIT_VIEWER_TYPE", (String) comboUnitType.getSelectedItem());
        client.getConfig().saveConfig();
        client.setConfig();
    }

    /**
     * Resolves the currently selected table row to its lightweight {@link MekSummary} (unlike
     * {@link #getSelectedEntity()}, this does not parse the full unit file).
     *
     * @return the {@link MekSummary} for the selected row, or {@code null} if no row is selected
     */
    public @Nullable MekSummary getSelectedMekSummary() {
        int view = tableUnits.getSelectedRow();

        if (view < 0) {
            // selection got filtered away
            return null;
        }

        int selected = tableUnits.convertRowIndexToModel(view);
        // else
        return meks[selected];

    }

    /** Unused; required by {@link KeyListener} but this dialog only reacts to key-press events. */
    @Override
    public void keyReleased(KeyEvent keyEvent) {

    }

    /**
     * Enables or disables the "Reset" advanced-search button.
     *
     * @param b {@code true} to enable the button (an advanced search filter is active), {@code false} to disable it
     */
    public void enableResetButton(boolean b) {
        btnResetSearch.setEnabled(b);
    }

    /** @return the dialog height captured the last time the window was deactivated. */
    public int getSelectorSizeHeight() {
        return selectorSizeHeight;
    }

    /** @param selectorSizeHeight new remembered dialog height. */
    public void setSelectorSizeHeight(int selectorSizeHeight) {
        this.selectorSizeHeight = selectorSizeHeight;
    }

    /** @return the dialog width captured the last time the window was deactivated. */
    public int getSelectorSizeWidth() {
        return selectorSizeWidth;
    }

    /** @param selectorSizeWidth new remembered dialog width. */
    public void setSelectorSizeWidth(int selectorSizeWidth) {
        this.selectorSizeWidth = selectorSizeWidth;
    }
}


