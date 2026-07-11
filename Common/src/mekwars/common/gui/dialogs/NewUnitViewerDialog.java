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

public class NewUnitViewerDialog extends JDialog implements Runnable, KeyListener, ActionListener {
    public static final int UNIT_VIEWER = 0;
    public static final int OMNI_VARIANT_SELECTOR = 1;
    public static final int UNIT_SELECTOR = 2;
    public static final int UNIT_RESEARCH = 3;
    @Serial
    private static final long serialVersionUID = 8144354264100884817L;
    private static final MMLogger LOGGER = MMLogger.create(NewUnitViewerDialog.class);
    private final static int KEY_TIMEOUT = 1000;
    private final MekTableModel unitModel;
    private final UnitLoadingDialog unitLoadingDialog;
    private final Client mmClient = new Client("temp", "None", 0);
    private final int viewerType;
    private final IClient client;
    private final AdvancedSearchDialog advancedSearchDialog;
    private JTextField txtFilter;
    private JButton btnSelectClose;
    private JButton btnSelect;
    private JButton btnClose;
    private JButton btnShowBV;
    private JButton btnAdvSearch;
    private JButton btnResetSearch;
    private JComboBox<String> comboType;
    private JComboBox<String> comboUnitType;
    private JComboBox<String> comboWeight;
    private JLabel lblImage;
    private JTable tableUnits;
    private ConfigurableMekViewPanel panelMekView;
    private StringBuffer searchBuffer = new StringBuffer();
    private long lastSearch = 0;
    private MekSummary[] meks;
    private MekSearchFilter searchFilter;
    private TableRowSorter<MekTableModel> sorter;
    private int selectedUnitType;
    private int selectedUnitWeight;
    private int selectedUnitRulesLevel;
    private int selectorSizeHeight;
    private int selectorSizeWidth;

    /** Creates new form UnitSelectorDialog */
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

    public void keyTyped(KeyEvent keyEvent) {
    }

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

    private void saveComboBoxSettings() {

        client.getConfig().setParam("UNIT_VIEWER_WEIGHT", (String) comboWeight.getSelectedItem());
        client.getConfig().setParam("UNIT_VIEWER_TECH", (String) comboType.getSelectedItem());
        client.getConfig().setParam("UNIT_VIEWER_TYPE", (String) comboUnitType.getSelectedItem());
        client.getConfig().saveConfig();
        client.setConfig();
    }

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

    @Override
    public void keyReleased(KeyEvent keyEvent) {

    }

    public void enableResetButton(boolean b) {
        btnResetSearch.setEnabled(b);
    }

    public int getSelectorSizeHeight() {
        return selectorSizeHeight;
    }

    public void setSelectorSizeHeight(int selectorSizeHeight) {
        this.selectorSizeHeight = selectorSizeHeight;
    }

    public int getSelectorSizeWidth() {
        return selectorSizeWidth;
    }

    public void setSelectorSizeWidth(int selectorSizeWidth) {
        this.selectorSizeWidth = selectorSizeWidth;
    }
}


