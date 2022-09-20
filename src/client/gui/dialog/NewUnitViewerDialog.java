package client.gui.dialog;

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
import java.util.ArrayList;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter.SortKey;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import client.MWClient;
import common.util.MWLogger;
import common.util.UnitUtils;
import megamek.client.Client;
import megamek.client.ui.swing.AdvancedSearchDialog;
import megamek.client.ui.swing.MechViewPanel;
import megamek.client.ui.swing.UnitFailureDialog;
import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.MechFileParser;
import megamek.common.MechSearchFilter;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MechView;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import megamek.common.loaders.EntityLoadingException;

	public class NewUnitViewerDialog extends JDialog implements Runnable,
	    KeyListener, ActionListener {

	    /**
	     *
	     */
	    private static final long serialVersionUID = 8144354264100884817L;

	    private JButton btnSelectClose;
	    private JButton btnSelect;
	    private JButton btnClose;
	    private JButton btnShowBV;
	    private JButton btnAdvSearch;
	    private JButton btnResetSearch;
	    private JComboBox<String> comboType;
	    private JComboBox<String> comboUnitType;
	    private JComboBox<String> comboWeight;
	    private JLabel lblFilter;
	    private JLabel lblImage;
	    private JLabel lblType;
	    private JLabel lblUnitType;
	    private JLabel lblWeight;
	    private JPanel panelFilterBtns;
	    private JPanel panelSearchBtns;
	    private JPanel panelOKBtns;
	    private JScrollPane scrTableUnits;
	    private JTable tableUnits;
	    JTextField txtFilter;
	    private MechViewPanel panelMekView;

	    private JPanel selectionPanel;
	    private JSplitPane splitPane;

	    private StringBuffer searchBuffer = new StringBuffer();
	    private long lastSearch = 0;
	    // how long after a key is typed does a new search begin
	    private final static int KEY_TIMEOUT = 1000;

	    private MechSummary[] mechs;

	    private MechTableModel unitModel;
	    private MechSearchFilter searchFilter;

	    MWClient client;
	    private UnitLoadingDialog unitLoadingDialog;
	    AdvancedSearchDialog asd;

	    private TableRowSorter<MechTableModel> sorter;
	    
	    private int selectedUnitType;
	    private int selectedUnitWeight;
	    private int selectedUnitRulesLevel;
	    private int selectorSizeHeight;
	    private int selectorSizeWidth;
	    
	    private Client mmClient = new Client("temp", "None", 0);
	    
	    public static final int UNIT_VIEWER = 0;
	    public static final int OMNI_VARIANT_SELECTOR = 1;
	    public static final int UNIT_SELECTOR = 2;
	    public static final int UNIT_RESEARCH = 3;

	    private int viewerType = NewUnitViewerDialog.UNIT_VIEWER;
//	    private boolean viewFluff = false;
	    
	    /** Creates new form UnitSelectorDialog */
	    public NewUnitViewerDialog (JFrame mainFrame, UnitLoadingDialog uld, MWClient c, int viewer) {
	        super(mainFrame, "Unit Viewer", true); //$NON-NLS-1$
	        client = c;
	        
	        viewerType = viewer;
	        if (viewerType == NewUnitViewerDialog.OMNI_VARIANT_SELECTOR) {
	            setTitle("Omni Variant Selector");
	        } else if (viewerType == NewUnitViewerDialog.UNIT_SELECTOR) {
	            setTitle("Unit Selector");
	        }
	        unitLoadingDialog = uld;

	        unitModel = new MechTableModel();
	        initComponents();
	        int width = 800;
	        int height = 600;
	        setSize(width,height);
	        setLocationRelativeTo(mainFrame);
	        asd = new AdvancedSearchDialog(mainFrame,
	                Integer.parseInt(client.getserverConfigs("CampaignYear")));
	    }

	    private void initComponents() {
	    	setMinimumSize(new java.awt.Dimension(640, 480));

	        GridBagConstraints c;

	        selectionPanel = new JPanel(new GridBagLayout());
	        selectionPanel.setMinimumSize(new java.awt.Dimension(500, 500));
	        selectionPanel.setPreferredSize(new java.awt.Dimension(500, 600));

	        panelFilterBtns = new JPanel();
	        panelSearchBtns = new JPanel();
	        panelOKBtns = new JPanel();

	        scrTableUnits = new JScrollPane();
	        tableUnits = new JTable();
	        tableUnits.addKeyListener(this);
	        tableUnits.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
	                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "");
	        panelMekView = new MechViewPanel();
	        panelMekView.setMinimumSize(new java.awt.Dimension(300, 500));
	        panelMekView.setPreferredSize(new java.awt.Dimension(300, 600));

	        comboType = new JComboBox<String>();
	        comboWeight = new JComboBox<String>();
	        comboUnitType = new JComboBox<String>();
	        txtFilter = new JTextField();

	        btnSelect = new JButton();
	        btnSelectClose = new JButton();
	        btnClose = new JButton();
	        btnShowBV = new JButton();
	        btnAdvSearch = new JButton();
	        btnResetSearch = new JButton();

	        lblType = new JLabel("Type");
	        lblWeight = new JLabel("Weight Class");
	        lblUnitType = new JLabel("Unit Type");
	        lblFilter = new JLabel("Filter");
	        lblImage = new JLabel();

	        getContentPane().setLayout(new GridBagLayout());

	        scrTableUnits.setMinimumSize(new java.awt.Dimension(500, 400));
	        scrTableUnits.setPreferredSize(new java.awt.Dimension(500, 400));

	        tableUnits.setModel(unitModel);
	        tableUnits.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	        sorter = new TableRowSorter<MechTableModel>(unitModel);
	        tableUnits.setRowSorter(sorter);
	        tableUnits.getSelectionModel().addListSelectionListener(
	                new javax.swing.event.ListSelectionListener() {
	                    public void valueChanged(
	                            javax.swing.event.ListSelectionEvent evt) {
	                        // There can be multiple events for one selection. Check
	                        // to
	                        // see if this is the last.
	                        if (!evt.getValueIsAdjusting()) {
	                            refreshUnitView();
	                        }
	                    }
	                });
	        TableColumn column = null;
	        for (int i = 0; i < MechTableModel.N_COL; i++) {
	            column = tableUnits.getColumnModel().getColumn(i);
	            if (i == MechTableModel.COL_CHASSIS) {
	                column.setPreferredWidth(125);
	            } else if ((i == MechTableModel.COL_MODEL)
	                    || (i == MechTableModel.COL_COST)) {
	                column.setPreferredWidth(75);
	            } else if ((i == MechTableModel.COL_WEIGHT)
	                    || (i == MechTableModel.COL_BV)) {
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

	        panelFilterBtns.setMinimumSize(new java.awt.Dimension(300, 120));
	        panelFilterBtns.setPreferredSize(new java.awt.Dimension(300, 120));
	        panelFilterBtns.setLayout(new GridBagLayout());

	        c = new GridBagConstraints();
	        c.gridx = 0;
	        c.gridy = 2;
	        c.anchor = GridBagConstraints.WEST;
	        panelFilterBtns.add(lblType, c);

	        DefaultComboBoxModel<String> techModel = new DefaultComboBoxModel<String>();
	        for (int i = 0; i < TechConstants.SIZE; i++) {
	            techModel.addElement(TechConstants.getLevelDisplayableName(i));
	        }
	        techModel.setSelectedItem(TechConstants.getLevelDisplayableName(0));
	        comboType.setModel(techModel);
	        comboType.setMinimumSize(new java.awt.Dimension(200, 27));
	        comboType.setPreferredSize(new java.awt.Dimension(200, 27));
	        comboType.addActionListener(this);
	        c = new GridBagConstraints();
	        c.gridx = 1;
	        c.gridy = 2;
	        c.anchor = GridBagConstraints.WEST;
	        panelFilterBtns.add(comboType, c);

	        c = new GridBagConstraints();
	        c.gridx = 0;
	        c.gridy = 1;
	        c.anchor = GridBagConstraints.WEST;
	        panelFilterBtns.add(lblWeight, c);

	        DefaultComboBoxModel<String> weightModel = new DefaultComboBoxModel<String>();
	        for (int i = 0; i < EntityWeightClass.SIZE; i++) {
	            weightModel.addElement(EntityWeightClass.getClassName(i));
	        }
	        weightModel.addElement("All"); //$NON-NLS-1$
	        weightModel.setSelectedItem(EntityWeightClass.getClassName(0));
	        comboWeight.setModel(weightModel);
	        comboWeight.setSelectedItem("All");
	        comboWeight.setMinimumSize(new java.awt.Dimension(200, 27));
	        comboWeight.setPreferredSize(new java.awt.Dimension(200, 27));
	        comboWeight.addActionListener(this);
	        c = new GridBagConstraints();
	        c.gridx = 1;
	        c.gridy = 1;
	        c.anchor = GridBagConstraints.WEST;
	        panelFilterBtns.add(comboWeight, c);

	        c = new GridBagConstraints();
	        c.gridx = 0;
	        c.gridy = 0;
	        c.fill = GridBagConstraints.HORIZONTAL;
	        c.anchor = GridBagConstraints.WEST;
	        panelFilterBtns.add(lblUnitType, c);

	        DefaultComboBoxModel<String> unitTypeModel = new DefaultComboBoxModel<String>();
	        unitTypeModel.addElement("All");
	        unitTypeModel.setSelectedItem("All");
	        for (int i = 0; i < UnitType.SIZE; i++) {
	            unitTypeModel.addElement(UnitType.getTypeDisplayableName(i));
	        }
	        comboUnitType.setModel(unitTypeModel);
	        comboUnitType.setMinimumSize(new java.awt.Dimension(200, 27));
	        comboUnitType.setPreferredSize(new java.awt.Dimension(200, 27));
	        comboUnitType.addActionListener(this);
	        c = new GridBagConstraints();
	        c.gridx = 1;
	        c.gridy = 0;
	        c.anchor = GridBagConstraints.WEST;
	        panelFilterBtns.add(comboUnitType, c);

	        txtFilter.setText("");
	        txtFilter.setMinimumSize(new java.awt.Dimension(200, 28));
	        txtFilter.setPreferredSize(new java.awt.Dimension(200, 28));
	        txtFilter.getDocument().addDocumentListener(new DocumentListener() {
	            public void changedUpdate(DocumentEvent e) {
	                filterUnits();
	            }

	            public void insertUpdate(DocumentEvent e) {
	                filterUnits();
	            }

	            public void removeUpdate(DocumentEvent e) {
	                filterUnits();
	            }
	        });
	        c = new GridBagConstraints();
	        c.gridx = 1;
	        c.gridy = 3;
	        c.anchor = GridBagConstraints.WEST;
	        panelFilterBtns.add(txtFilter, c);

	        c = new GridBagConstraints();
	        c.gridx = 0;
	        c.gridy = 3;
	        c.anchor = GridBagConstraints.WEST;
	        panelFilterBtns.add(lblFilter, c);

	        lblImage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
	        lblImage.setText(""); // NOI18N
	        c = new GridBagConstraints();
	        c.gridx = 2;
	        c.gridy = 0;
	        c.gridheight = 4;
	        c.fill = GridBagConstraints.BOTH;
	        c.weightx = 1.0;
	        c.weighty = 1.0;
	        panelFilterBtns.add(lblImage, c);

	        c = new GridBagConstraints();
	        c.gridx = 0;
	        c.gridy = 0;
	        c.fill = GridBagConstraints.HORIZONTAL;
	        c.anchor = GridBagConstraints.NORTHWEST;
	        c.weightx = 0.0;
	        c.insets = new java.awt.Insets(10, 10, 10, 0);
	        selectionPanel.add(panelFilterBtns, c);

	        panelSearchBtns.setLayout(new GridBagLayout());

	        btnAdvSearch
	                .setText("Advanced Search"); //$NON-NLS-1$
	        btnAdvSearch.addActionListener(this);
	        c = new GridBagConstraints();
	        c.gridx = 0;
	        c.gridwidth = 1;
	        c.gridy = 0;
	        c.anchor = GridBagConstraints.WEST;
	        panelSearchBtns.add(btnAdvSearch, c);

	        btnResetSearch.setText("Reset"); //$NON-NLS-1$
	        btnResetSearch.addActionListener(this);
	        btnResetSearch.setEnabled(false);
	        c = new GridBagConstraints();
	        c.gridx = 1;
	        c.gridwidth = 1;
	        c.gridy = 0;
	        c.anchor = GridBagConstraints.WEST;
	        panelSearchBtns.add(btnResetSearch, c);

	        c = new GridBagConstraints();
	        c.gridx = 0;
	        c.gridy = 1;
	        c.fill = GridBagConstraints.HORIZONTAL;
	        c.anchor = GridBagConstraints.NORTHWEST;
	        c.weightx = 0.0;
	        c.insets = new java.awt.Insets(10, 10, 10, 0);
	        selectionPanel.add(panelSearchBtns, c);

	        panelOKBtns.setLayout(new GridBagLayout());

	        btnSelect.setText("Select");
	        btnSelect.addActionListener(this);
	        panelOKBtns.add(btnSelect, new GridBagConstraints());

	        btnSelectClose.setText("Select & Close");
	        btnSelectClose.addActionListener(this);
	        panelOKBtns.add(btnSelectClose, new GridBagConstraints());

	        btnClose.setText("Close");
	        btnClose.addActionListener(this);
	        panelOKBtns.add(btnClose, new GridBagConstraints());

	        btnShowBV.setText("Show BV Calculation"); //$NON-NLS-1$
	        btnShowBV.addActionListener(this);
	        panelOKBtns.add(btnShowBV, new GridBagConstraints());

	        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
	                selectionPanel, panelMekView);
	        splitPane.setResizeWeight(0);
	        c = new GridBagConstraints();
	        c.gridx = c.gridy = 0;
	        c.fill = GridBagConstraints.BOTH;
	        c.weightx = c.weighty = 1;
	        getContentPane().add(splitPane, c);
	        c.insets = new Insets(5,0,5,0);
	        c.weightx = c.weighty = 0;
	        c.gridy = 1;
	        getContentPane().add(panelOKBtns, c);

	        pack();
	    }

	    void filterUnits() {
	        RowFilter<MechTableModel, Integer> unitTypeFilter = null;
	        final int nType = comboType.getSelectedIndex();
	        final int nClass = comboWeight.getSelectedIndex();
	        final int nUnit = comboUnitType.getSelectedIndex() - 1;
	        //If current expression doesn't parse, don't update.
	        try {
	            unitTypeFilter = new RowFilter<MechTableModel,Integer>() {
	                @Override
	                public boolean include(Entry<? extends MechTableModel, ? extends Integer> entry) {
	                    MechTableModel mechModel = entry.getModel();
	                    MechSummary mech = mechModel.getMechSummary(entry.getIdentifier());
	                    if (/* Weight */
	                            ((nClass == EntityWeightClass.SIZE) || (mech.getWeightClass() == nClass)) &&
	                            /*Canon*/
	                            (!mmClient.getGame().getOptions().booleanOption("canon_only") || mech.isCanon()) &&
	                            /*Technology Level*/
	                            ((nType == TechConstants.T_ALL)
	                                || (nType == mech.getType())
	                                || ((nType == TechConstants.T_IS_TW_ALL)
	                                    && ((mech.getType() <= TechConstants.T_IS_TW_NON_BOX)
	                                     || (mech.getType() == TechConstants.T_INTRO_BOXSET)))
	                                || ((nType == TechConstants.T_TW_ALL)
	                                    && ((mech.getType() <= TechConstants.T_IS_TW_NON_BOX)
	                                     || (mech.getType() <= TechConstants.T_INTRO_BOXSET)
	                                     || (mech.getType() <= TechConstants.T_CLAN_TW)))
	                                || ((nType == TechConstants.T_ALL_IS)
	                                    && ((mech.getType() <= TechConstants.T_IS_TW_NON_BOX)
	                                     || (mech.getType() == TechConstants.T_INTRO_BOXSET)
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
	                            && ((searchFilter==null) || MechSearchFilter.isMatch(mech, searchFilter))
	                            && !(mech.getYear() > Integer.parseInt(client.getserverConfigs("CampaignYear")))) {
	                        if(txtFilter.getText().length() > 0) {
	                            String text = txtFilter.getText();
	                            return mech.getName().toLowerCase().contains(text.toLowerCase());
	                    }
	                    return true;
	                }
	                return false;
	                }
	            };
	        } catch (java.util.regex.PatternSyntaxException e) {
	            return;
	        }
	        sorter.setRowFilter(unitTypeFilter);
	    }

	    void refreshUnitView() {
	        boolean populateTextFields = true;

	        Entity selectedUnit = getSelectedEntity();
	        // null entity, so load a default unit.
	        if (selectedUnit == null) {
	            panelMekView.reset();
	            lblImage.setIcon(null);
	            return;
	        }

	        MechView mechView = null;
	        try {
	            mechView = new MechView(selectedUnit, false);
	        } catch (Exception e) {
	            e.printStackTrace();
	            // error unit didn't load right. this is bad news.
	            populateTextFields = false;
	        }
	        if (populateTextFields && (mechView != null)) {
	            panelMekView.setMech(selectedUnit, mechView);
	        } else {
	            panelMekView.reset();
	        }

	        //client.getMainFrame().loadPreviewImage(lblImage, selectedUnit, client.getLocalPlayer());
	    }

	    public Entity getSelectedEntity() {
	        int view = tableUnits.getSelectedRow();
	        if (view < 0) {
	            // selection got filtered away
	            return null;
	        }
	        int selected = tableUnits.convertRowIndexToModel(view);
	        // else
	        MechSummary ms = mechs[selected];
	        try {
	            // For some unknown reason the base path gets screwed up after you
	            // print so this sets the source file to the full path.
	            Entity entity = new MechFileParser(ms.getSourceFile(),
	                    ms.getEntryName()).getEntity();
	            return entity;
	        } catch (EntityLoadingException ex) {
	            System.out.println("Unable to load mech: " + ms.getSourceFile()
	                    + ": " + ms.getEntryName() + ": " + ex.getMessage());
	            ex.printStackTrace();
	            return null;
	        }
	    }

	    public MechSummary getSelectedMechSummary() {
	        int view = tableUnits.getSelectedRow();
	        if (view < 0) {
	            // selection got filtered away
	            return null;
	        }
	        int selected = tableUnits.convertRowIndexToModel(view);
	        // else
	        return  mechs[selected];
	        
	    }
	    
	     public void run() {
	         // Loading mechs can take a while, so it will have its own thread.
	         // This prevents the UI from freezing, and allows the
	         // "Please wait..." dialog to behave properly on various Java VMs.
	         MechSummaryCache mscInstance = MechSummaryCache.getInstance();
	         mechs = mscInstance.getAllMechs();

	         // break out if there are no units to filter
	         if (mechs == null) {
	             System.err.println("No units to filter!");
	         } else {
	             unitModel.setData(mechs);
	         }
	         filterUnits();

	         //initialize with the units sorted alphabetically by chassis
	         ArrayList<SortKey> sortlist = new ArrayList<SortKey>();
	         sortlist.add(new SortKey(MechTableModel.COL_CHASSIS,SortOrder.ASCENDING));
	         //sortlist.add(new RowSorter.SortKey(MechTableModel.COL_MODEL,SortOrder.ASCENDING));
	         tableUnits.getRowSorter().setSortKeys(sortlist);
	         ((DefaultRowSorter<?, ?>)tableUnits.getRowSorter()).sort();

	         tableUnits.invalidate(); // force re-layout of window
	         pack();
	         //setLocation(computeDesiredLocation());

	         unitLoadingDialog.setVisible(false);

	         // In some cases, it's possible to get here without an initialized
	         // instance (loading a saved game without a cahce).  In these cases,
	         // we dn't care about the failed loads.
	         if (mscInstance.isInitialized())
	         {
	             final Map<String, String> hFailedFiles =
	                 MechSummaryCache.getInstance().getFailedFiles();
	             if ((hFailedFiles != null) && (hFailedFiles.size() > 0)) {
	                 // self-showing dialog
	                 new UnitFailureDialog(client.getMainFrame(), hFailedFiles);
	             }
	         }
	         
	         int width = 800;
	         int height = 600;
	         setSize(width,height);
	         setVisible(true);
	     }

	     @Override
	     public void setVisible(boolean visible) {
	         if (visible){
	             comboUnitType.setSelectedIndex(selectedUnitType);
	             comboWeight.setSelectedIndex(selectedUnitWeight);
	             comboType.setSelectedIndex(selectedUnitRulesLevel);
	         }
	         asd.clearValues();
	         searchFilter=null;
	         btnResetSearch.setEnabled(false);

	         filterUnits();
	         super.setVisible(visible);
	     }

	     @Override
	    protected void processWindowEvent(WindowEvent e){
	         super.processWindowEvent(e);
	         if (e.getID() == WindowEvent.WINDOW_DEACTIVATED){
	             selectedUnitType = comboUnitType.getSelectedIndex();
	             selectedUnitWeight = comboWeight.getSelectedIndex();
	             selectedUnitRulesLevel = comboType.getSelectedIndex();
	             setSelectorSizeHeight(getSize().height);
	             setSelectorSizeWidth(getSize().width);
	         }
	     }


	    /**
	     * A table model for displaying work items
	     */
	    public class MechTableModel extends AbstractTableModel {

	            /**
	             *
	             */
	            private static final long serialVersionUID = -5457068129532709857L;
	            private final static int COL_CHASSIS = 0;
	            private final static int COL_MODEL = 1;
	            private final static int COL_WEIGHT = 2;
	            private final static int COL_BV = 3;
	            private final static int COL_YEAR = 4;
	            private final static int COL_COST = 5;
	            private final static int COL_LEVEL = 6;
	            private final static int N_COL = 7;

	            private MechSummary[] data = new MechSummary[0];

	            public int getRowCount() {
	                return data.length;
	            }

	            public int getColumnCount() {
	                return N_COL;
	            }

	            @Override
	            public String getColumnName(int column) {
	                switch(column) {
	                    case COL_MODEL:
	                        return "Model";
	                    case COL_CHASSIS:
	                        return "Chassis";
	                    case COL_WEIGHT:
	                        return "Weight";
	                    case COL_BV:
	                        return "BV";
	                    case COL_YEAR:
	                        return "Year";
	                    case COL_COST:
	                        return "Price";
	                    case COL_LEVEL:
	                         return "Level";
	                    default:
	                        return "?";
	                }
	            }

	            @Override
	            public Class<?> getColumnClass(int c) {
	                return getValueAt(0, c).getClass();
	            }

	            @Override
	            public boolean isCellEditable(int row, int col) {
	                return false;
	            }

	            public MechSummary getMechSummary(int i) {
	                return data[i];
	            }

	            //fill table with values
	            public void setData(MechSummary[] ms) {
	                data = ms;
	                fireTableDataChanged();
	            }

	            public Object getValueAt(int row, int col) {
	                if (data.length <= row) {
	                    return "?";
	                }

	                MechSummary ms = data[row];
	                if(col == COL_MODEL) {
	                    return ms.getModel();
	                }
	                if(col == COL_CHASSIS) {
	                    return ms.getChassis();
	                }
	                if(col == COL_WEIGHT) {
	                    return ms.getTons();
	                }
	                if(col == COL_BV) {
	                    return ms.getBV();
	                }
	                if(col == COL_YEAR) {
	                    return ms.getYear();
	                }
	                if(col == COL_COST) {
	                    //return NumberFormat.getInstance().format(ms.getCost());
	                    return ms.getCost();
	                }
	                if (col == COL_LEVEL) {
	                    return ms.getLevel();
	                }
	                return "?";
	            }

	    }

	    public void keyPressed(KeyEvent ke) {
	        if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
	            ActionEvent event = new ActionEvent(btnSelect,
	                    ActionEvent.ACTION_PERFORMED, ""); //$NON-NLS-1$
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

	    public void keyTyped(KeyEvent ke) {
	    }

	    public void actionPerformed(ActionEvent ev) {
	        if (ev.getSource().equals(comboType)
	                || ev.getSource().equals(comboWeight)
	                || ev.getSource().equals(comboUnitType)) {
	            filterUnits();
	        } else if (ev.getSource().equals(btnClose)) {
	            setVisible(false);
	        } else if (ev.getSource().equals(btnShowBV)) {
	            JEditorPane tEditorPane = new JEditorPane();
	            tEditorPane.setContentType("text/html");
	            tEditorPane.setEditable(false);
	            Entity e = getSelectedEntity();
	            if (null == e) {
	                return;
	            }
	            e.calculateBattleValue();
	            tEditorPane.setText((e.getUseManualBV() ? e.getManualBV() : e.getInitialBV())+"");
	            tEditorPane.setCaretPosition(0);
	            JScrollPane tScroll = new JScrollPane(tEditorPane,
	                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
	                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	            Dimension size = new Dimension(550, 300);
	            tScroll.setPreferredSize(size);
	            JOptionPane.showMessageDialog(null, tScroll, "BV", JOptionPane.INFORMATION_MESSAGE, null);
	        } else if(ev.getSource().equals(btnAdvSearch)) {
	            searchFilter = asd.showDialog();
	            btnResetSearch.setEnabled((searchFilter != null) && !searchFilter.isDisabled);
	            filterUnits();
	        } else if(ev.getSource().equals(btnResetSearch)) {
	            asd.clearValues();
	            searchFilter=null;
	            btnResetSearch.setEnabled(false);
	            filterUnits();
	        } else if (ev.getSource().equals(btnSelect) || ev.getSource().equals(btnSelectClose)) {
	            saveComboBoxSettings();
	            if (viewerType == NewUnitViewerDialog.OMNI_VARIANT_SELECTOR) {
	                try {
	                    MechSummary ms = getSelectedMechSummary();
	                    String unit = ms.getName();
	                    setVisible(false);
	                    String moneyMod = JOptionPane.showInputDialog(client.getMainFrame(), "Money Mod for " + unit, 0);

	                    if ((moneyMod == null) || (moneyMod.length() == 0)) {
	                        dispose();
	                        return;
	                    }

	                    String compMod = JOptionPane.showInputDialog(client.getMainFrame(), "Comp Mod for " + unit, 0);

	                    if ((compMod == null) || (compMod.length() == 0)) {
	                        dispose();
	                        return;
	                    }

	                    String fluMod = JOptionPane.showInputDialog(client.getMainFrame(), "Flu Mod for " + unit, 0);

	                    if ((fluMod == null) || (fluMod.length() == 0)) {
	                        dispose();
	                        return;
	                    }

	                    client.sendChat(MWClient.CAMPAIGN_PREFIX + "c AddOmniVariantMod#" + unit + "#" + moneyMod + "$" + compMod + "$" + fluMod);

	                    dispose();
	                } catch (Exception ex) {
	                    MWLogger.errLog(ex);
	                    // MMClient.mwClientLog.clientErrLog("Problem with
	                    // actionPerformed in RepodDialog");
	                }
	            }// end omni selector if
	            else if (viewerType == NewUnitViewerDialog.UNIT_SELECTOR) {
	                try {
	                    MechSummary ms = getSelectedMechSummary();
	                    String unitFile = ms.getEntryName();
	                    String unit = ms.getName();
	                    setVisible(false);
	                    int weightClass = comboWeight.getSelectedIndex();
	                    // Item "All" takes up Weight Class 0, so this is usually 1 off.
	                    if (weightClass > 0) {
	                    	weightClass -= 1;
	                    }
	                    unitFile = UnitUtils.getMechSummaryFileName(ms);


	                    String fluff = JOptionPane.showInputDialog(client.getMainFrame(), "Fluff text for " + unit);

	                    if ((fluff == null) || (fluff.length() == 0)) {
	                        dispose();
	                        return;
	                    }

	                    String gunnery = JOptionPane.showInputDialog(client.getMainFrame(), "Gunnery skill for " + unit, 99);

	                    if ((gunnery == null) || (gunnery.length() == 0)) {
	                        dispose();
	                        return;
	                    }

	                    String piloting = JOptionPane.showInputDialog(client.getMainFrame(), "Piloting Mod for " + unit, 99);

	                    if ((piloting == null) || (piloting.length() == 0)) {
	                        dispose();
	                        return;
	                    }

	                    String skills = null;
	                    skills = JOptionPane.showInputDialog(client.getMainFrame(), "Skills Mod for " + unit + " (comma delimited)");

	                    if (skills == null) {
	                        dispose();
	                        return;
	                    }

	                    client.sendChat(MWClient.CAMPAIGN_PREFIX + "c createunit#" + unitFile + "#" + fluff + "#" + gunnery + "#" + piloting + "#" + weightClass + "#" + skills);

	                    dispose();
	                } catch (Exception ex) {
	                    MWLogger.errLog(ex);
	                    // MMClient.mwClientLog.clientErrLog("Problem with
	                    // actionPerformed in RepodDialog");
	                }
	            } else if (viewerType == NewUnitViewerDialog.UNIT_RESEARCH) {
	                MechSummary ms = getSelectedMechSummary();

	                String unitFile;
	                unitFile = UnitUtils.getMechSummaryFileName(ms);
	                setVisible(false);

	                if ((unitFile != null) && !unitFile.equals("null")) {
	                    client.sendChat(MWClient.CAMPAIGN_PREFIX + "c researchunit#" + unitFile);
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
	        for (int i = 0; i < mechs.length; i++) {
	            if (mechs[i].getName().toLowerCase().startsWith(search)) {
	                int selected = tableUnits.convertRowIndexToView(i);
	                if (selected > -1) {
	                    tableUnits.changeSelection(selected, 0, false, false);
	                    break;
	                }
	            }
	        }
	    }

	    public void enableResetButton(boolean b) {
	        btnResetSearch.setEnabled(b);
	    }

		@Override
		public void keyReleased(KeyEvent e) {
			
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
		
	    private void saveComboBoxSettings() {

	    	client.getConfig().setParam("UNITVIEWERWEIGHT", (String) comboWeight.getSelectedItem());
	    	client.getConfig().setParam("UNITVIEWERTECH", (String) comboType.getSelectedItem());
	    	client.getConfig().setParam("UNITVIEWERTYPE", (String) comboUnitType.getSelectedItem());
//	    	client.getConfig().setParam("UNITVIEWERSORT", (String) combos.getSelectedItem());
//	        if (mechList.getSelectedValue() != null) {
//	        	client.getConfig().setParam("UNITVIEWERUNIT", mechList.getSelectedValue().toString());
//	        }

	        client.getConfig().saveConfig();
	        client.setConfig();
	    }
	 }


