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
import java.util.regex.PatternSyntaxException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

import megamek.client.Client;
import megamek.client.ui.dialogs.UnitFailureDialog;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.dialogs.advancedsearch.AdvancedSearchDialog;
import megamek.client.ui.dialogs.advancedsearch.MekSearchFilter;
import megamek.client.ui.dialogs.unitSelectorDialogs.ConfigurableMekViewPanel;
import megamek.common.TechConstants;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.MWLogger;
import mekwars.common.util.UnitUtils;

public class NewUnitViewerDialog extends JDialog implements Runnable, KeyListener, ActionListener {
    public static final int UNIT_VIEWER = 0;
    public static final int OMNI_VARIANT_SELECTOR = 1;
    public static final int UNIT_SELECTOR = 2;
    public static final int UNIT_RESEARCH = 3;
    @Serial
    private static final long serialVersionUID = 8144354264100884817L;
    // how long after a key is typed does a new search begin
    private final static int KEY_TIMEOUT = 1000;
    private final MekTableModel unitModel;
    private final UnitLoadingDialog unitLoadingDialog;
    private final Client mmClient = new Client("temp", "None", 0);
    private final int viewerType;
    JTextField txtFilter;
    IClient client;
    AdvancedSearchDialog asd;
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
        if (viewerType == mekwars.common.gui.dialogs.NewUnitViewerDialog.OMNI_VARIANT_SELECTOR) {
            setTitle("Omni Variant Selector");
        } else if (viewerType == mekwars.common.gui.dialogs.NewUnitViewerDialog.UNIT_SELECTOR) {
            setTitle("Unit Selector");
        }
        unitLoadingDialog = uld;

        unitModel = new MekTableModel();
        initComponents();
        int width = 800;
        int height = 600;
        setSize(width, height);
        setLocationRelativeTo(mainFrame);
        asd = new AdvancedSearchDialog(mainFrame, Integer.parseInt(this.client.getServerConfigs("CampaignYear")));
    }

    private void initComponents() {
        setMinimumSize(new Dimension(640, 480));

        GridBagConstraints c;

        JPanel selectionPanel = new JPanel(new GridBagLayout());
        selectionPanel.setMinimumSize(new Dimension(500, 500));
        selectionPanel.setPreferredSize(new Dimension(500, 600));

        JPanel panelFilterButtons = new JPanel();
        JPanel panelSearchButtons = new JPanel();
        JPanel panelOKButtons = new JPanel();

        JScrollPane scrTableUnits = new JScrollPane();
        tableUnits = new JTable();
        tableUnits.addKeyListener(this);
        tableUnits.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
              KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "");
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
        javax.swing.table.TableColumn column;
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

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        selectionPanel.add(scrTableUnits, c);

        panelFilterButtons.setMinimumSize(new Dimension(300, 120));
        panelFilterButtons.setPreferredSize(new Dimension(300, 120));
        panelFilterButtons.setLayout(new GridBagLayout());

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(lblType, c);

        DefaultComboBoxModel<String> techModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < TechConstants.SIZE; i++) {
            techModel.addElement(TechConstants.getLevelDisplayableName(i));
        }
        techModel.setSelectedItem(TechConstants.getLevelDisplayableName(0));
        comboType.setModel(techModel);
        comboType.setMinimumSize(new Dimension(200, 27));
        comboType.setPreferredSize(new Dimension(200, 27));
        comboType.addActionListener(this);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(comboType, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(lblWeight, c);

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
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(comboWeight, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(lblUnitType, c);

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
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(comboUnitType, c);

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
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(txtFilter, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(lblFilter, c);

        lblImage.setHorizontalAlignment(SwingConstants.CENTER);
        lblImage.setText(""); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.gridheight = 4;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        panelFilterButtons.add(lblImage, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 0.0;
        c.insets = new Insets(10, 10, 10, 0);
        selectionPanel.add(panelFilterButtons, c);

        panelSearchButtons.setLayout(new GridBagLayout());

        btnAdvSearch.setText("Advanced Search"); //$NON-NLS-1$
        btnAdvSearch.addActionListener(this);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        panelSearchButtons.add(btnAdvSearch, c);

        btnResetSearch.setText("Reset"); //$NON-NLS-1$
        btnResetSearch.addActionListener(this);
        btnResetSearch.setEnabled(false);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridwidth = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        panelSearchButtons.add(btnResetSearch, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 0.0;
        c.insets = new Insets(10, 10, 10, 0);
        selectionPanel.add(panelSearchButtons, c);

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
        c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = c.weighty = 1;
        getContentPane().add(splitPane, c);
        c.insets = new Insets(5, 0, 5, 0);
        c.weightx = c.weighty = 0;
        c.gridy = 1;
        getContentPane().add(panelOKButtons, c);

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
        javax.swing.RowFilter<MekTableModel, Integer> unitTypeFilter;
        final int nType = comboType.getSelectedIndex();
        final int nClass = comboWeight.getSelectedIndex();
        final int nUnit = comboUnitType.getSelectedIndex() - 1;
        //If current expression doesn't parse, don't update.
        try {
            unitTypeFilter = new javax.swing.RowFilter<>() {
                @Override
                public boolean include(RowFilter.Entry<? extends MekTableModel, ? extends Integer> entry) {
                    MekTableModel mechModel = entry.getModel();
                    MekSummary mech = mechModel.getMechSummary(entry.getIdentifier());
                    if (/* Weight */
                          ((nClass == EntityWeightClass.SIZE) || (mech.getWeightClass() == nClass)) &&
                                /*Canon*/
                                (!mmClient.getGame().getOptions().booleanOption("canon_only") || mech.isCanon()) &&
                                /*Technology Level*/
                                ((nType == TechConstants.T_ALL)
                                       || (nType == mech.getType())
                                       || ((nType == TechConstants.T_IS_TW_ALL)
                                                 && ((mech.getType() <= TechConstants.T_IS_TW_NON_BOX)
                                                           || (mech.getType() == TechConstants.T_INTRO_BOX_SET)))
                                       || ((nType == TechConstants.T_TW_ALL)
                                                 && ((mech.getType() <= TechConstants.T_IS_TW_NON_BOX)
                                                           || (mech.getType() <= TechConstants.T_INTRO_BOX_SET)
                                                           || (mech.getType() <= TechConstants.T_CLAN_TW)))
                                       || ((nType == TechConstants.T_ALL_IS)
                                                 && ((mech.getType() <= TechConstants.T_IS_TW_NON_BOX)
                                                           || (mech.getType() == TechConstants.T_INTRO_BOX_SET)
                                                           || (mech.getType() == TechConstants.T_IS_ADVANCED)
                                                           || (mech.getType() == TechConstants.T_IS_EXPERIMENTAL)
                                                           || (mech.getType() == TechConstants.T_IS_UNOFFICIAL)))
                                       || ((nType == TechConstants.T_ALL_CLAN)
                                                 && ((mech.getType() == TechConstants.T_CLAN_TW)
                                                           || (mech.getType() == TechConstants.T_CLAN_ADVANCED)
                                                           || (mech.getType() == TechConstants.T_CLAN_EXPERIMENTAL)
                                                           || (mech.getType() == TechConstants.T_CLAN_UNOFFICIAL))))
                                && ((nUnit == -1) || mech.getUnitType().equals(UnitType.getTypeName(nUnit)))
                                /*Advanced Search*/
                                && ((searchFilter == null) || MekSearchFilter.isMatch(mech, searchFilter))
                                && !(mech.getYear() > Integer.parseInt(client.getServerConfigs("CampaignYear")))) {
                        if (!txtFilter.getText().isEmpty()) {
                            String text = txtFilter.getText();
                            return mech.getName().toLowerCase().contains(text.toLowerCase());
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

    public Entity getSelectedEntity() {
        int view = tableUnits.getSelectedRow();
        if (view < 0) {
            // selection got filtered away
            return null;
        }
        int selected = tableUnits.convertRowIndexToModel(view);
        // else
        MekSummary ms = meks[selected];
        try {
            // For some unknown reason the base path gets screwed up after you
            // print so this sets the source file to the full path.
            return new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
        } catch (EntityLoadingException ex) {
            System.out.println(STR."Unable to load mech: \{ms.getSourceFile()}: \{ms.getEntryName()}: \{ex.getMessage()}");
            ex.printStackTrace();
            return null;
        }
    }

    public void run() {
        // Loading meks can take a while, so it will have its own thread.
        // This prevents the UI from freezing and allows the
        // "Please wait..." dialog to behave properly on various Java VMs.
        MekSummaryCache mscInstance = MekSummaryCache.getInstance();
        meks = mscInstance.getAllMeks();

        // break out if there are no units to filter
        if (meks == null) {
            System.err.println("No units to filter!");
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
        if (mscInstance.isInitialized()) {
            final java.util.Map<String, String> hFailedFiles = MekSummaryCache.getInstance().getFailedFiles();
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
        asd.clearSearches();
        searchFilter = null;
        btnResetSearch.setEnabled(false);

        filterUnits();
        super.setVisible(visible);
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_DEACTIVATED) {
            selectedUnitType = comboUnitType.getSelectedIndex();
            selectedUnitWeight = comboWeight.getSelectedIndex();
            selectedUnitRulesLevel = comboType.getSelectedIndex();
            setSelectorSizeHeight(getSize().height);
            setSelectorSizeWidth(getSize().width);
        }
    }

    public void keyTyped(KeyEvent ke) {
    }

    public void keyPressed(KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
            ActionEvent event = new ActionEvent(btnSelect, ActionEvent.ACTION_PERFORMED, ""); //$NON-NLS-1$
            actionPerformed(event);
        }
        long curTime = System.currentTimeMillis();
        if ((curTime - lastSearch) > KEY_TIMEOUT) {
            searchBuffer = new StringBuffer();
        }
        lastSearch = curTime;
        searchBuffer.append(ke.getKeyChar());
        searchFor(searchBuffer.toString().toLowerCase());
    }

    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource().equals(comboType) ||
                  ev.getSource().equals(comboWeight) ||
                  ev.getSource().equals(comboUnitType)) {
            filterUnits();
        } else if (ev.getSource().equals(btnClose)) {
            setVisible(false);
        } else if (ev.getSource().equals(btnShowBV)) {
            JEditorPane tEditorPane = new JEditorPane();
            tEditorPane.setContentType("text/html");
            tEditorPane.setEditable(false);
            Entity entity = getSelectedEntity();

            if (null == entity) {
                return;
            }

            entity.calculateBattleValue();
            tEditorPane.setText(STR."\{entity.getUseManualBV() ? entity.getManualBV() : entity.getInitialBV()}");
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
        } else if (ev.getSource().equals(btnAdvSearch)) {
            searchFilter = asd.showDialog();
            btnResetSearch.setEnabled((searchFilter != null) && !searchFilter.isDisabled);
            filterUnits();
        } else if (ev.getSource().equals(btnResetSearch)) {
            asd.clearSearches();
            searchFilter = null;
            btnResetSearch.setEnabled(false);
            filterUnits();
        } else if (ev.getSource().equals(btnSelect) || ev.getSource().equals(btnSelectClose)) {
            saveComboBoxSettings();
            if (viewerType == NewUnitViewerDialog.OMNI_VARIANT_SELECTOR) {
                try {
                    MekSummary ms = getSelectedMechSummary();
                    String unit = ms.getName();
                    setVisible(false);
                    String moneyMod = JOptionPane.showInputDialog(client.getMainFrame(),
                          STR."Money Mod for \{unit}",
                          0);

                    if ((moneyMod == null) || (moneyMod.isEmpty())) {
                        dispose();
                        return;
                    }

                    String compMod = JOptionPane.showInputDialog(client.getMainFrame(),
                          STR."Comp Mod for \{unit}",
                          0);

                    if ((compMod == null) || (compMod.isEmpty())) {
                        dispose();
                        return;
                    }

                    String fluMod = JOptionPane.showInputDialog(client.getMainFrame(),
                          STR."Flu Mod for \{unit}",
                          0);

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
            else if (viewerType == NewUnitViewerDialog.UNIT_SELECTOR) {
                try {
                    MekSummary ms = getSelectedMechSummary();
                    String unitFile;
                    String unit = ms.getName();
                    setVisible(false);
                    int weightClass = comboWeight.getSelectedIndex();
                    // Item "All" takes up Weight Class 0, so this is usually 1 off.
                    if (weightClass > 0) {
                        weightClass -= 1;
                    }
                    unitFile = UnitUtils.getMekSummaryFileName(ms);


                    String fluff = JOptionPane.showInputDialog(client.getMainFrame(),
                          STR."Fluff text for \{unit}");

                    if ((fluff == null) || (fluff.isEmpty())) {
                        dispose();
                        return;
                    }

                    String gunnery = JOptionPane.showInputDialog(client.getMainFrame(),
                          STR."Gunnery skill for \{unit}",
                          99);

                    if ((gunnery == null) || (gunnery.isEmpty())) {
                        dispose();
                        return;
                    }

                    String piloting = JOptionPane.showInputDialog(client.getMainFrame(),
                          STR."Piloting Mod for \{unit}",
                          99);

                    if ((piloting == null) || (piloting.isEmpty())) {
                        dispose();
                        return;
                    }

                    String skills;
                    skills = JOptionPane.showInputDialog(client.getMainFrame(),
                          STR."Skills Mod for \{unit} (comma delimited)");

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
            } else if (viewerType == NewUnitViewerDialog.UNIT_RESEARCH) {
                MekSummary ms = getSelectedMechSummary();

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

        client.getConfig().setParam("UNITVIEWERWEIGHT", (String) comboWeight.getSelectedItem());
        client.getConfig().setParam("UNITVIEWERTECH", (String) comboType.getSelectedItem());
        client.getConfig().setParam("UNITVIEWERTYPE", (String) comboUnitType.getSelectedItem());
        client.getConfig().saveConfig();
        client.setConfig();
    }

    public MekSummary getSelectedMechSummary() {
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
    public void keyReleased(java.awt.event.KeyEvent e) {

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


