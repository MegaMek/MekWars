/*
 * MekWars - Copyright (C) 2004, 2005
 *
 * Original author - nmorris (urgru@users.sourceforge.net)
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
 * SolFreeBuildDialog
 * @Author Salient (mwosux@gmail.com) August 2017
 * Duplicated and modified TableViewerDialog in an attempt to create new dialog 
 * for SOL players to create any mek/vee on a pre defined build table. This is part 
 * of a Larger system to change how SOL works in general.
 * 
 */

 

package client.gui.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

//import server.campaign.CampaignMain;
import client.MWClient;
import client.campaign.CUnit;
import client.gui.MWUnitDisplay;
import client.gui.TableSorter;
import client.util.CUnitComparator;
import common.House;
//import common.House;
import common.Unit;
import common.campaign.pilot.Pilot;
import common.util.MWLogger;
import common.util.SpringLayoutHelper;
import common.util.UnitUtils;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MULParser;

public class SolFreeBuildDialog extends JFrame implements ItemListener {

    /**
     *
     */
    private static final long serialVersionUID = -5449999786199993020L;
    // ivars
    JComboBox<String> weightClassCombo;
    JComboBox<String> factionCombo;
    JComboBox<String> unitTypeCombo;

    JLabel factionLabel = new JLabel("Faction: ", SwingConstants.RIGHT);
    JLabel typeLabel = new JLabel("Type: ", SwingConstants.RIGHT);
    JLabel weightLabel = new JLabel("Class: ", SwingConstants.RIGHT);
    JLabel percentageLabel = new JLabel("Please Select a Unit To Create", SwingConstants.CENTER);

    String[] factionArray = {};
    String[] unitTypeArray = { "Mek", "Vehicle", "BattleArmor", "Infantry", "ProtoMek", "Aero" };
    String[] weightClassArray = { "Light", "Medium", "Heavy", "Assault" };

    int factionSort = 0;
    int unitSort = 0;
    int weightSort = 0;

    JTable generalTable = new JTable();
    JScrollPane generalScrollPane = new JScrollPane();

    JButton closeButton = new JButton("Close");
    JButton refreshButton = new JButton("Reload Data");
    JButton createButton = new JButton("Create (ALT+C)");

    // model and whatnot for refreshing
    TableViewerModel tvModel;

    // maps and sorts
    TreeMap<Object, TableUnit> currentUnits;
    TableUnit[] sortedUnits = {};// sorts generated from the map.
    
    //@Salient adding this to capture unit selection
    TableUnit selectedUnit;

    MWClient mwclient;

    // constructor
    public SolFreeBuildDialog(MWClient client) {
        super("Free Unit Browser");
        
        mwclient = client;
        currentUnits = new TreeMap<Object, TableUnit>();
        generalScrollPane = new JScrollPane();

        // alpha sorted faction array. hacky and evil.
        TreeSet<String> factionNames = new TreeSet<String>();// tree to alpha
        
        // if freebuild use all option is checked, and player is in SOL, all houses are loaded into the dialog
        if( mwclient.getserverConfigs("Sol_FreeBuild_UseAll").equalsIgnoreCase("true") &&
        	mwclient.getPlayer().getHouse().equalsIgnoreCase(mwclient.getserverConfigs("NewbieHouseName")))
        {
        	Iterator<House> i = mwclient.getData().getAllHouses().iterator();
        	while (i.hasNext()) 
        	{
        		House house = i.next();
        
        		if (house.getId() > -1) 
        		{
        			factionNames.add(house.getName());
        		}
        	}
        	
        	//check if build table is set to common, if not add it
        	if(!mwclient.getserverConfigs("Sol_FreeBuild_BuildTable").equalsIgnoreCase("Common"))
        	{
        		factionNames.add("Common");
        	}
        }
        
        // Only going to allow SOL to build from a table defined by DSO
        factionNames.add(mwclient.getserverConfigs("Sol_FreeBuild_BuildTable"));
        
        // If player is not in newbie house AND post defection is true add only their house to list
        if(!mwclient.getPlayer().getHouse().equalsIgnoreCase(mwclient.getserverConfigs("NewbieHouseName")) &&
        	mwclient.getserverConfigs("FreeBuild_PostDefection").equalsIgnoreCase("true"))
        {
        	factionNames.clear();
        	factionNames.add(mwclient.getPlayer().getHouse().trim());
        }
        
        factionArray = factionNames.toArray(factionArray);

        // CONSTRUCT GUI
        // make combo boxes
        weightClassCombo = new JComboBox<String>(weightClassArray);
        factionCombo = new JComboBox<String>(factionArray);
        unitTypeCombo = new JComboBox<String>();

        for ( int type = Unit.MEK; type < Unit.MAXBUILD; type++ ){
            unitTypeCombo.addItem(Unit.getTypeClassDesc(type));
        }

        // set max combo heights
        Dimension comboDim = new Dimension();
        comboDim.setSize(factionCombo.getMinimumSize().getWidth() * 1.5, factionLabel.getMinimumSize().getHeight() + 2);

        factionCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        factionCombo.setMaximumSize(comboDim);
        weightClassCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        weightClassCombo.setMaximumSize(comboDim);
        unitTypeCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        unitTypeCombo.setMaximumSize(comboDim);

        // put the combos and their labels into a spring
        JPanel comboPanel = new JPanel(new SpringLayout());
        comboPanel.add(factionLabel);
        comboPanel.add(factionCombo);
        comboPanel.add(typeLabel);
        comboPanel.add(unitTypeCombo);
        comboPanel.add(weightLabel);
        comboPanel.add(weightClassCombo);
        SpringLayoutHelper.setupSpringGrid(comboPanel, 3, 2);

        /*
         * Load preserved combo selection settings.
         */
        // In the absence of a saved faction, load the player's own.
        
        /*
        try {
            String previousItem = mwclient.getConfigParam("TABLEVIEWERFACTION");
            factionCombo.setSelectedItem(previousItem);
            factionSort = factionCombo.getSelectedIndex();
        } catch (Exception e) {
            factionCombo.setSelectedItem(mwclient.getPlayer().getHouse());
        }

        // If type/weight data are missing, select the first item in the combo.
        try {
            String previousItem = mwclient.getConfigParam("TABLEVIEWERTYPE");
            unitTypeCombo.setSelectedItem(previousItem);
            unitSort = unitTypeCombo.getSelectedIndex();
        } catch (Exception e) {
            unitTypeCombo.setSelectedIndex(-1);
        }

        try {
            String previousItem = mwclient.getConfigParam("TALEVIEWERWEIGHT");
            weightClassCombo.setSelectedItem(previousItem);
            weightSort = weightClassCombo.getSelectedIndex();
        } catch (Exception e) {
            weightClassCombo.setSelectedIndex(-1);
        }
		*/
        weightClassCombo.setSelectedIndex(0);
        unitTypeCombo.setSelectedIndex(0);
        factionCombo.setSelectedItem(0);
        
        
        // add listeners to the combo boxes
        factionCombo.addItemListener(this);
        unitTypeCombo.addItemListener(this);
        weightClassCombo.addItemListener(this);

        // allow the close button to actually close things ...
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        refreshButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        refreshButton.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent e) {
                refreshButton_ActionPerformed();
            }
        });
        
        createButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        createButton.setMnemonic(KeyEvent.VK_C);
        createButton.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent e) {
            	createUnit_ActionPerformed();
            }
        });

        // set up the BM-style table
        tvModel = new TableViewerModel(mwclient, currentUnits, sortedUnits);
        TableSorter sorter = new TableSorter(tvModel, client, TableSorter.SORTER_BUILDTABLES);
        generalTable.setModel(sorter);

        // make it possible to double click for unit info
        generalTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TableUnit u = getUnitAtRow(generalTable.getSelectedRow());
                    if (u == null) {
                        return;
                    }

                    Entity theEntity = u.getEntity();
                    theEntity.loadAllWeapons();

                    JFrame infoWindow = new JFrame();
                    UnitDisplay unitDetailInfo = new MWUnitDisplay(null, client);

                    infoWindow.getContentPane().add(unitDetailInfo);
                    infoWindow.setSize(300, 400);
                    infoWindow.setResizable(false);

                    infoWindow.setTitle(u.getModelName());
                    infoWindow.setLocationRelativeTo(mwclient.getMainFrame());// center
                    // it
                    infoWindow.setVisible(true);
                    unitDetailInfo.displayEntity(theEntity);
                }
            }
        });// end addMouseListener();

        // set the proper cell renderers
        for (int j = 0; j < tvModel.getColumnCount(); j++) {
            generalTable.getColumnModel().getColumn(j).setCellRenderer(tvModel.getRenderer());
        }

        // add sort listener to column heads
        sorter.addMouseListenerToHeaderInTable(generalTable);

        // allow only single selections
        generalTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        /*
         * Unlike the BM table, the BuildTableTable (huh?) doesn't need a
         * ListSelectionListener. No buttons to activate/deactivate and no
         * images to update w/ proper .gifs.
         */

        // make the table double buffered
        generalTable.setDoubleBuffered(true);

        // add the table to the scroll pane
        generalScrollPane.setToolTipText("Click on column header to sort.");
        generalScrollPane.setViewportView(generalTable);
        generalScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0), BorderFactory.createLineBorder(Color.BLACK, 1)));

        // make a box layout to hold the combos and table
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.Y_AXIS));
        JPanel buttonPanel = new JPanel(new SpringLayout());

        // center the percentage label
        percentageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        percentageLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        percentageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // add combo boxes scrollPane and percentage info to the boxpanel
        boxPanel.add(comboPanel);
        boxPanel.add(generalScrollPane);
        boxPanel.add(percentageLabel);
        buttonPanel.add(createButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);
        

        SpringLayoutHelper.setupSpringGrid(buttonPanel, 3);

        boxPanel.add(buttonPanel);

        // give the box a small border
        boxPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // add the box to the main panel
        getContentPane().add(boxPanel);

        // load the default tables/units
        loadTables();
        refresh();
        setLocationRelativeTo(mwclient.getMainFrame());
        setVisible(true);
    }

    // refresh
    public void refresh() {
        tvModel.refreshModel();
        generalTable.setPreferredSize(new Dimension(generalTable.getWidth(), generalTable.getRowHeight() * (generalTable.getRowCount())));
        generalTable.revalidate();
    }

    // Override show to center on screen.
    @Override
    public void setVisible(boolean show) {

        pack();
        setLocationRelativeTo(mwclient.getMainFrame());

        this.setSize(720, 575);
        setResizable(false);

        super.setVisible(show);
    }

    // methods
    public TableUnit getUnitAtRow(int row) {

        String filename = (String) generalTable.getModel().getValueAt(row, TableViewerModel.FILENAME);
        if (filename != null) {
            return currentUnits.get(filename);
        }

        // else
        return null;
    }

    /**
     * Method to conform with ItemListener. Takes item events from the combo
     * boxes and triggers table loads.
     */
    @Override
	public void itemStateChanged(ItemEvent i) {

        /*
         * Do not re-load tables and units if there is no actual change in the
         * selection.
         */
        @SuppressWarnings("unchecked")
		JComboBox<String> source = (JComboBox<String>) i.getSource();
        if ((source == unitTypeCombo) && (unitSort == unitTypeCombo.getSelectedIndex())) {
            return;
        } else if ((source == weightClassCombo) && (weightSort == weightClassCombo.getSelectedIndex())) {
            return;
        } else if ((source == factionCombo) && (factionSort == factionCombo.getSelectedIndex())) {
            return;
        }

        // fails passed. reload the tables.
        loadTables();

        // save the current sort modes locally
        unitSort = unitTypeCombo.getSelectedIndex();
        weightSort = weightClassCombo.getSelectedIndex();
        factionSort = factionCombo.getSelectedIndex();

        // save the new sort to the config
        mwclient.getConfig().setParam("TABLEVIEWERFACTION", (String) factionCombo.getSelectedItem());
        mwclient.getConfig().setParam("TABLEVIEWERTYPE", (String) unitTypeCombo.getSelectedItem());
        mwclient.getConfig().setParam("TALEVIEWERWEIGHT", (String) weightClassCombo.getSelectedItem());
        mwclient.getConfig().saveConfig();
        mwclient.setConfig();

        // refresh the display
        refresh();
    }

    /**
     * Helper which checks strings to see if they end with a known-good unit
     * file extension.
     */
    public boolean hasValidExtension(String l) {
        String lc = l.toLowerCase();
        if (lc.endsWith(".blk") || lc.endsWith(".mtf") || lc.endsWith(".hmp") || lc.endsWith(".xml") || lc.endsWith(".hmv") || lc.endsWith(".mep") || lc.endsWith(".mul")) {
            return true;
        }
        // else
        return false;
    }

    /**
     * Helper which takes a File entry and returns an input stream. Handles
     * errors, etc. to reduce clutter in loadTables().
     */
    public InputStream getEntryInputStream(File bf) {
        InputStream is = null;
        try {
            is = new FileInputStream(bf);
            return is;
        } catch (IOException io) {
            return null;
        }
    }

    /**
     * Helper which loops through a table, ignoring filenames and tablenames.
     * Returns total table weighting for use when analyzing names.
     */
    public int getTotalWeightForTable(File bf) {

        int totalweight = 0;

        try {
            FileInputStream fis = new FileInputStream(bf);
            BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
            while (dis.ready()) {
                // read the line and remove excess whitespace
                String l = dis.readLine();

                if ((l == null) || (l.trim().length() == 0)) {
                    continue;
                }

                l = l.trim();
                l = l.replaceAll("\\s+", " ");
                if (l.indexOf(" ") == 0) {
                    l = l.substring(1, l.length());
                }

                StringTokenizer ST = new StringTokenizer(l);
                totalweight += Integer.parseInt((String) ST.nextElement());
            }
            fis.close();
            dis.close();
        } catch (Exception e) {
            // nothing
        }

        // System.out.println("totalweight of current table: " + totalweight);
        return totalweight;
    }

    /**
     * Helper method which reads a given layer of tables. Extracted from
     * loadTables to reduce repetition; however, doing do actually makes each
     * check (inparticular, the first and last map levels) more complex than
     * they would otherwise.
     */
    public void doTableLayer(TreeMap<String, Double> curr, TreeMap<String, Double> next, String add, File buildTablePath, boolean commonOverride) {

        /*
         * Set up an iterator of target tables. Note that the first level (base
         * table) is put into a dummy treemap in order to have an iterator.
         */
        Iterator<String> it = curr.keySet().iterator();
        while (it.hasNext()) {
            String currTableName = it.next();

            // get zip entry for the new file.
            File tableEntry = null;
            if (commonOverride) {
                tableEntry = new File(buildTablePath.getPath() + File.separatorChar + "Common" + add);
            } else {
                tableEntry = new File(buildTablePath.getPath() + File.separatorChar + currTableName + add);
            }

            if (!tableEntry.exists()) {
                if (commonOverride) {
                    tableEntry = new File((buildTablePath.getPath() + File.separatorChar + "Common" + add).toLowerCase());
                } else {
                    tableEntry = new File((buildTablePath.getPath() + File.separatorChar + currTableName + add).toLowerCase());
                }
            }

            // ignore missing links
            if (tableEntry.exists()) {

                /*
                 * Loop through the target table once to determine the total
                 * weighting of all entries. This total is used to determine the
                 * fractional values of each line on a second pass.
                 */
                int totaltableweight = getTotalWeightForTable(tableEntry);

                /*
                 * InputStream and Buffered reader for a second pass through the
                 * file.
                 */
                InputStream is = getEntryInputStream(tableEntry);
                BufferedReader dis = new BufferedReader(new InputStreamReader(is));

                /*
                 * TableMultiplier is used to determine the relative value of
                 * each entry. For example, if the Orion ONI-1K appears on a
                 * target table at 50%, and the tableweight is .10 (aka - 10%),
                 * the ONI's actual frequncy is 5%.
                 */
                double tablemultiplier = curr.get(currTableName).doubleValue();
                // System.out.println("TableMultiplier for " + currTableName +
                // ": " + tablemultiplier);

                try {
                    while (dis.ready()) {

                        // read the line. make sure it's not empty.
                        String l = dis.readLine();
                        if ((l == null) || (l.trim().length() == 0)) {
                            continue;
                        }

                        // remove excess whitespace
                        l = l.trim();
                        l = l.replaceAll("\\s+", " ");
                        if (l.indexOf(" ") == 0) {
                            l = l.substring(1, l.length());
                        }

                        /*
                         * All lines should have weights. Set up a
                         * StringTokenizer and grab common data before seperate
                         * file/table work is done.
                         */
                        StringTokenizer ST = new StringTokenizer(l);
                        double weight = Double.parseDouble((String) ST.nextElement());

                        /*
                         * Determine whether this line is a cross-linked table
                         * or an actual unit file. Assume a valid file if the
                         * entry ends with a known unit file extension. If no
                         * known extension is present, assume a crosslinked
                         * table.
                         */
                        if (hasValidExtension(l) && (weight != 0)) {

                            String Filename = "";
                            while (ST.hasMoreElements()) {
                                Filename += ST.nextToken();
                                if (ST.hasMoreElements()) {
                                    Filename += " ";
                                }
                            }

                            /*
                             * Now that we have a filename, create a TableUnit.
                             * Check for duplication before adding to
                             * currentUnits. If the file in question is a dupe,
                             * simply add its frequency to that of the existing
                             * unit.
                             */
                            double frequency = (weight / totaltableweight) * tablemultiplier;

                            if (Filename.toLowerCase().endsWith(".mul")) {

                                Vector<Entity> loadedUnits = null;
                                File entityFile = new File("data/armies/" + Filename);

                                try {
                                    loadedUnits = new MULParser(entityFile,null).getEntities();
                                    loadedUnits.trimToSize();
                                    frequency /= loadedUnits.size();
                                } catch (Exception ex) {
                                    MWLogger.errLog("Unable to load file " + entityFile.getName());
                                    MWLogger.errLog(ex);
                                    continue;
                                }

                                for (Entity en : loadedUnits) {
                                    TableUnit tu = new TableUnit(en, frequency);
                                    if (tu != null) {


                                    TableUnit eu = currentUnits.get(tu.getRealFilename());// existing
                                    // unit
                                    if (eu != null) {
                                        eu.addFrequencyFrom(tu);
                                    } else {
                                        currentUnits.put(tu.getRealFilename(), tu);
                                    }

                                    /*
                                     * Add this table as a source.
                                     */
                                    eu = currentUnits.get(tu.getRealFilename());// existing
                                    // unit
                                    if (eu.getTables().get(currTableName) == null) {
                                        eu.getTables().put(currTableName, frequency);
                                    } else {
                                        Double currFreq = eu.getTables().get(currTableName);
                                        Double newFreq = currFreq + frequency;
                                        eu.getTables().remove(currTableName);
                                        eu.getTables().put(currTableName, newFreq);
                                    }
                                    }
                                }
                            } else {
                                TableUnit tu = new TableUnit(Filename, frequency);
                                if (tu != null) {


                                TableUnit eu = currentUnits.get(Filename);// existing
                                // unit
                                if (eu != null) {
                                    eu.addFrequencyFrom(tu);
                                } else {
                                    currentUnits.put(Filename, tu);
                                }

                                /*
                                 * Add this table as a source.
                                 */
                                eu = currentUnits.get(Filename);// existing
                                // unit
                                if (eu.getTables().get(currTableName) == null) {
                                    eu.getTables().put(currTableName, frequency);
                                } else {
                                    Double currFreq = eu.getTables().get(currTableName);
                                    Double newFreq = currFreq.doubleValue() + frequency;
                                    eu.getTables().remove(currTableName);
                                    eu.getTables().put(currTableName, newFreq);
                                }
                                }
                            }
                        } else if (weight != 0) {// is a crosslink table
                            String crossTableName = "";
                            while (ST.hasMoreElements()) {
                                crossTableName += ST.nextToken();
                                if (ST.hasMoreElements()) {
                                    crossTableName += " ";
                                }
                            }

                            /*
                             * Put the crosslink into the map, if another layer
                             * exists. Check for duplication. If next is null
                             * there are no more crosslink hops to be mode,
                             * which means sorting would be a waste of time.
                             */
                            if (next != null) {
                                if (next.containsKey("crossTableName")) {
                                    Double d = next.get(crossTableName);
                                    double newTableWeight = d.doubleValue() + ((weight / totaltableweight) * tablemultiplier);
                                    next.remove(crossTableName);
                                    next.put(crossTableName, newTableWeight);
                                } else {
                                    next.put(crossTableName, (weight / totaltableweight) * tablemultiplier);
                                }
                            }
                        }
                    }
                    is.close();// close input stream
                    dis.close();// close buffer
                } catch (Exception e) {
                    return;
                }

            }// end if(Entry != null)
        }// end while(more tables in iterator)
    }// end doTableLayer()

    /**
     * Method which loads tables and TableUnits, based on current ComboBox
     * selections. This is the beef of the class ...
     */
    @SuppressWarnings("unused")
	public void loadTables() {

        // System.out.println("loadTables() called");

        String factionString = "";
        String addOnString = "";

        /*
         * First, determine faction.
         */
        factionString += (String) factionCombo.getSelectedItem();
        // System.out.println("Faction String: " + factionString);

        /*
         * Next, determine the weightclass.
         */
        addOnString += "_" + (String) weightClassCombo.getSelectedItem();

        /*
         * Finally, determine the type of unit to look at.
         */
        String type = (String) unitTypeCombo.getSelectedItem();
        if (!type.equals("Mek")) {
            addOnString += type;
        }

        // always look for a .txt
        addOnString += ".txt";
        // System.out.println("AddOn String: " + addOnString);

        /*
         * Look for the build tables
         */
        File buildTablePath = null;
        // System.out.println("Attempting to find ./data/buildtables/standard");
        buildTablePath = new File("./data/buildtables/standard");
        if (!buildTablePath.exists()) {
            MWLogger.errLog("Could not find build tables.");
            return;
        }

        /*
         * Reset currentUnits.
         */
        // System.out.println("Clearing currentUnits");
        currentUnits.clear();

        /*
         * Found the zip. Now extract an appropriate entry. Try normal casing
         * and lowercasing/
         */
        boolean overrideWithCommon = false;
        // System.out.println("Attempting to find base table entry.");
        File tableEntry = new File(buildTablePath.getPath() + File.separatorChar + factionString + addOnString);
        if (tableEntry == null) {
            tableEntry = new File((buildTablePath.getPath() + File.separatorChar + factionString + addOnString).toLowerCase());
        }

        /*
         * Server defaults to common if a table isnt present. For example, if
         * Davion_AssaultBattleArmor isn't present,
         * Common_AssaultBattleArmor.txt is used instead. So, check that here as
         * well.
         */
        if (tableEntry == null) {
            // System.out.println("Didn't find Faction table in lower case
            // either. Retrying with Common.");
            overrideWithCommon = true;
            tableEntry = new File(buildTablePath.getPath() + File.separatorChar + "Common" + addOnString);
        }

        /*
         * If cased common is also null, try lower case. If this fails, return.
         */
        if (tableEntry == null) {
            // System.out.println("Didn't find Common table with standard
            // casing. Retrying in lower case.");
            tableEntry = new File((buildTablePath.getPath() + File.separatorChar + "Common" + addOnString).toLowerCase());
        }

        if (tableEntry == null) {
            // System.out.println("Didn't find Common table with lowercase.
            // Returning.");
            return;
        }

        /*
         * A clutch of treemaps. These are used to store info on crosslinked
         * tables. Note that linkage hops could extend in perpetuity, so
         * stopping after 3 hops will generate some minor rounding errors.
         */
        TreeMap<String, Double> crossMap1 = new TreeMap<String, Double>();
        TreeMap<String, Double> crossMap2 = new TreeMap<String, Double>();
        // TreeMap<String, Double> crossMap3 = new TreeMap<String, Double>();

        /*
         * Original Table. A dummy treemap is used here in order to pass a
         * treemap to the doTableLayer method. The initial table is the only
         * value and carries a 100% weight.
         */
        TreeMap<String, Double> temp = new TreeMap<String, Double>();
        temp.put(factionString, 100.0);// using 100 makes things
        // %'s instead of decimals
        // ...
        // System.out.println("this.doTableLayer - base");
        doTableLayer(temp, crossMap1, addOnString, buildTablePath, overrideWithCommon);

        while (true) {
            // 1st cross-linkages (2nd degree)
            // System.out.println("this.doTableLayer - map1");
            doTableLayer(crossMap1, crossMap2, addOnString, buildTablePath, false);

            if (crossMap2.size() < 1) {
                break;
            }

            crossMap1 = crossMap2;
            crossMap2 = new TreeMap<String, Double>();
        }
        // 2nd layer cross linkages (3rd degree)
        // System.out.println("this.doTableLayer - map2");
        // this.doTableLayer(crossMap2, crossMap3, addOnString, buildTablePath,
        // false);

        // 3rd layer cross linkages (4th degree)
        // System.out.println("this.doTableLayer - map3");
        // this.doTableLayer(crossMap3, null, addOnString, buildTablePath,
        // false);

        /*
         * Table, and 3 degrees of seperation, processed as well as possible.
         * Holes may exist if linked tables are given bad pointers on the
         * tables, or if linkages are pervasive and 3 hops are insufficient to
         * cover most of the crosstalk.
         */

        /*
         * Update the total percentage counter.
         */
        double totalPercent = 0;
        Iterator<TableUnit> it = currentUnits.values().iterator();
        while (it.hasNext()) {
            TableUnit currUnit = it.next();
            totalPercent += currUnit.getFrequency();
        }
        DecimalFormat myFormatter = new DecimalFormat("###.#####");
        //percentageLabel.setText("Total Percentage: " + myFormatter.format(totalPercent) + "%");
    }

    // inner classes
    /*
     * TableViewerModel is a model extention which sets up proper table viewer
     * sorting columns - name, weight, model, % frequency, etc. Modeled along
     * the BlackMarketModel from client.gui
     */
    static class TableViewerModel extends AbstractTableModel {

        /**
         *
         */
        private static final long serialVersionUID = 4544599978221999391L;
        // IVARS
        // static ints
        public final static int UNIT = 0;// model/name
        public final static int WEIGHT = 1;
        public final static int BATTLEVALUE = 2;
        public final static int FREQUENCY = 3;
        public final static int FILENAME = 4;

        TreeMap<Object, TableUnit> currentUnits;
        TableUnit[] sortedUnits;

        int currentSortMode = TableViewerModel.FREQUENCY;

        // column name array
        //String[] columnNames = { "Unit", "Weight", "BV", "Frequency" };
        String[] columnNames = { "Unit", "Weight", "BV" };
        // client reference
        MWClient mwclient;

        // CONSTRUCTOR
        public TableViewerModel(MWClient c, TreeMap<Object, TableUnit> current, TableUnit[] sorted) {
            mwclient = c;
            currentUnits = current;
            sortedUnits = sorted;
        }

        // column count, for AbstractModel
        @Override
		public int getColumnCount() {
            return columnNames.length;
        }

        // rowcount, for AbstractModel
        @Override
		public int getRowCount() {
            return sortedUnits.length;
        }

        // override naming
        @Override
        public String getColumnName(int col) {
            return (columnNames[col]);
        }

        // isEditable, overridden from AbstractModel
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public void setSortMode(int sortMode) {
            currentSortMode = sortMode;
        }

        /*
         * getRenderer, overridden from AbstractModel in order to use custom
         * renderer.
         */
        public TableViewerModel.TableViewerRenderer getRenderer() {
            return new TableViewerRenderer();
        }

        /*
         * refresh model in order to draw new contents, reorder existin
         * contents.
         */
        public void refreshModel() {
            sortedUnits = new TableUnit[] {};
            sortedUnits = sortUnits(currentSortMode);
            fireTableDataChanged();
        }

        // getValueAt, for AbstractModel
        @Override
		public Object getValueAt(int row, int col) {

            // invalid row
            if ((row < 0) || (row >= sortedUnits.length)) {
                return "";
            }

            TableUnit currU = sortedUnits[row];

            if (currU == null) {
                return "";
            }

            switch (col) {
            case UNIT:

                try {
                    if ((currU.getType() == Unit.MEK) && (currU.getEntity() != null) && !currU.getEntity().isOmni()) {
                        return "<html><body>" + currU.getEntity().getChassis() + ", " + currU.getModelName();
                    }
                    // else
                    return "<html><body>" + currU.getModelName();
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                    return "";
                }
            case WEIGHT:
                return (int) currU.getEntity().getWeight();

            case BATTLEVALUE:

                return currU.getEntity().calculateBattleValue();

            case FREQUENCY:

                DecimalFormat myFormatter = new DecimalFormat("##0.00");
                String val = myFormatter.format(currU.getFrequency());
                //Double returnVal = Double.parseDouble(val);
                Double returnVal = 0.0;
                try {
						returnVal = NumberFormat.getNumberInstance().parse(val).doubleValue();
					} catch (ParseException e) {
						e.printStackTrace();
					}

            	return returnVal;

            case FILENAME:
                return currU.getRealFilename();

            }

            return "";
        }

		@Override
		public Class<? extends Object> getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}

        /*
         * Method which sorts the units in currentUnits.
         */
        public TableUnit[] sortUnits(int sortMode) {

            // a comparator
            CUnitComparator comparator = null;

            switch (sortMode) {
            case TableViewerModel.UNIT:

                sortedUnits = currentUnits.values().toArray(sortedUnits);
                comparator = new CUnitComparator(CUnitComparator.HQSORT_NAME);
                Arrays.sort(sortedUnits, comparator);
                return sortedUnits;

            case TableViewerModel.WEIGHT:

                sortedUnits = currentUnits.values().toArray(sortedUnits);
                comparator = new CUnitComparator(CUnitComparator.HQSORT_WEIGHTTONS);
                Arrays.sort(sortedUnits, comparator);
                return sortedUnits;

            case TableViewerModel.BATTLEVALUE:

                sortedUnits = currentUnits.values().toArray(sortedUnits);
                comparator = new CUnitComparator(CUnitComparator.HQSORT_BV);
                Arrays.sort(sortedUnits, comparator);
                return sortedUnits;

            case TableViewerModel.FREQUENCY:

                sortedUnits = currentUnits.values().toArray(sortedUnits);
                Arrays.sort(sortedUnits, new Comparator<TableUnit>() {
                    @Override
					public int compare(TableUnit o1, TableUnit o2) {

                        try {
                            TableUnit t1 = o1;
                            TableUnit t2 = o2;
                            Double d1 = 0.0;
                            Double d2 = 0.0;
                            if (t1 != null) {
                                d1 = t1.getFrequency();
                            }
                            if (t2 != null) {
                                d2 = t2.getFrequency();
                            }
                            return d1.compareTo(d2);
                        } catch (Exception ex) {
                            MWLogger.errLog(ex);
                            return 0;
                        }
                    }
                });
                return sortedUnits;

            }// end switch

            // failsafe return
            return new TableUnit[] {};
        }

        /*
         * TableViewerRenderer ... This needs quite a bit more polish ...
         */
        private class TableViewerRenderer extends DefaultTableCellRenderer {

            /**
             *
             */
            private static final long serialVersionUID = -8249928299962506117L;

            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component d = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                JLabel c = new JLabel(); // use a new label for everything
                // (should be made better later)
                c.setOpaque(true);

                if ((row >= currentUnits.size()) || (row < 0)) {
                    return c;
                }

                if (table.getModel().getValueAt(row, column) != null) {
                    c.setText(table.getModel().getValueAt(row, column).toString());
                }
                c.setToolTipText("");

                // get the unit from the tree
                Object unit = table.getModel().getValueAt(row, TableViewerModel.FILENAME);
                TableUnit currU = currentUnits.get(unit);

                if (currU == null) {
                    return null;
                }

                // set up description
                StringBuilder description = new StringBuilder();

                if ((currU.getType() == Unit.MEK) && !currU.getEntity().isOmni()) {
                    description.append("<html><body><u>" + currU.getEntity().getChassis() + ", " + currU.getModelName() + "</u><br>");
                } else {
                    description.append("<html><body><u>" + currU.getModelName() + "</u><br>");
                }

                // show the percent frequency for each table
                description.append("Sources:");

                Iterator<String> i = currU.getTables().keySet().iterator();
                //DecimalFormat formatter = new DecimalFormat("##0.0##");
                while (i.hasNext()) {
                    String tableName = i.next();
                    //Double freq = currU.getTables().get(tableName);
                    description.append("<br>- " + tableName);
                    //description.append("<br>- " + tableName + ": " + formatter.format(freq.doubleValue()) + "%");
                }

                c.setToolTipText(description.toString());

                if (isSelected) {
                    c.setForeground(d.getForeground());
                    c.setBackground(d.getBackground());
                    return c;
                }

                // always a white background
                c.setBackground(Color.white);

                return c;
            }
        }

    }// end TableViewerModel class

    /**
     * TableUnit is a CUnit with added stat-tracking for ongoing frequency
     * calculations. Much like the BMUnit; however, the TableUnit is less
     * complex.
     */
    static class TableUnit extends CUnit {

        // IVARS
        double frequency;
        String realFilename;
        TreeMap<String, Double> tables;

        // CONSTRUCTOR
        public TableUnit(String fn, double f) {
            super();

            /*
             * Since the TableUnit has no data string to set things with,
             * hardflag necessary values.
             */
            setUnitFilename(fn.trim());
            setPilot(new Pilot("Autopilot", 4, 5));

            /*
             * Try to get an entity from the unit cache, given a filename. This
             * makes it possible to use the build table viewer with unzipped
             * file structures (like that in use on the new MMNET). If this
             * fails, use normal server-style zip loading.
             */
            try {

                // remove .MTF, .blk, etc.
                String modfn = fn.trim();
                modfn = modfn.substring(0, modfn.length() - 4);

                // get the unit from the summary cache
                MechSummary ms = MechSummaryCache.getInstance().getMech(modfn);
                unitEntity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();

            } catch (Exception e) {
                // MWLogger.errLog(e);
                createEntityFromFileNameWithCache(fn.trim());// make the
                // entity
            }

            realFilename = fn;
            frequency = f;

            tables = new TreeMap<String, Double>();
        }

        public TableUnit(Entity en, double f) {
            super();

            realFilename = UnitUtils.getEntityFileName(en);


            setUnitFilename(realFilename);
            setPilot(new Pilot("Autopilot", 4, 5));

            // get the unit from the summary cache
            unitEntity = en;


            frequency = f;

            tables = new TreeMap<String, Double>();
        }

        // METHODS
        public double getFrequency() {
            return frequency;
        }

        public void addFrequencyFrom(TableUnit u) {
            frequency += u.getFrequency();
        }

        public String getRealFilename() {
            return realFilename;
        }

        public TreeMap<String, Double> getTables() {
            return tables;
        }

        private void createEntityFromFileNameWithCache(String fn) {

            unitEntity = UnitUtils.createEntity(fn);

            if (unitEntity == null) {
                createEntityFromFilename(fn);
            }
        }

        /**
         * Tries to setUnitEntity from a filename w/ extension. This used to be
         * the default way of getting units, but CUnit was changed to use the
         * MegaMek summary cache. Because the table viewer reads the tables the
         * same way the server does, it needs a server-style loading cascade,
         * ugly as it may be :-(
         */
        private void createEntityFromFilename(String fn) {

            unitEntity = null;
            try {
                unitEntity = new MechFileParser(new File("./data/mechfiles/Meks.zip"), fn).getEntity();
            } catch (Exception e) {
                try {
                    unitEntity = new MechFileParser(new File("./data/mechfiles/Vehicles.zip"), fn).getEntity();
                } catch (Exception ex) {
                    try {
                        unitEntity = new MechFileParser(new File("./data/mechfiles/Infantry.zip"), fn).getEntity();
                    } catch (Exception exc) {
                        try {
                            MWLogger.errLog("Error loading unit: " + fn + ". Try replacing with OMG.");
                            unitEntity = UnitUtils.createOMG();// new
                        } catch (Exception exepe) {
                            MWLogger.errLog("Error unit failed to load. Exiting.");
                            System.exit(1);
                        }
                    }
                }
            }

            setType(getEntityType(unitEntity));
            getC3Type(unitEntity);
        }

    }// end TableUnit

    public void refreshButton_ActionPerformed() {

        int userLevel = mwclient.getUserLevel();

        refreshButton.setEnabled(false);
        if (userLevel >= mwclient.getData().getAccessLevel("AdminRequestBuildTable")) {
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminRequestBuildTable#list#true");
        }
        else if (userLevel >= mwclient.getData().getAccessLevel("RequestBuildTable")) {
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c RequestBuildTable#list#true");
        }

        mwclient.setWaiting(true);
        while (mwclient.isWaiting()) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {

            }
        }
        loadTables();
        refresh();
        refreshButton.setEnabled(true);
    }
    
    public void createButton_ActionPerformed() {
    	

        int userLevel = mwclient.getUserLevel();

        refreshButton.setEnabled(false);
        if (userLevel >= mwclient.getData().getAccessLevel("AdminRequestBuildTable")) {
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminRequestBuildTable#list#true");
        }
        else if (userLevel >= mwclient.getData().getAccessLevel("RequestBuildTable")) {
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c RequestBuildTable#list#true");
        }

        mwclient.setWaiting(true);
        while (mwclient.isWaiting()) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {

            }
        }
        loadTables();
        refresh();
        refreshButton.setEnabled(true);
    }
    
    //@Salient
    public void createUnit_ActionPerformed() {
    	
    	selectedUnit = getUnitAtRow(generalTable.getSelectedRow());
    	Entity tempEntity = selectedUnit.getEntity();
    	//why does mekwars use 0-3 and megamek uses 1-4 for weight classes? ... :(
    	
        if (selectedUnit != null) 
        {
        	createButton.setEnabled(false);
        	
        	if(mwclient.getserverConfigs("Sol_FreeBuild_UseAll").equalsIgnoreCase("true") ||
        	   mwclient.getserverConfigs("FreeBuild_PostDefection").equalsIgnoreCase("true")) //may not need this, command will always check house table if postdefection is enabled.
        	{
        		mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "SOLCREATEUNIT " + selectedUnit.getRealFilename() + "#" + TableUnit.getEntityWeight(tempEntity) + "#" + factionCombo.getSelectedItem().toString());
        	}
        	// else if this isn't a sol player, but post defection free build is enabled
        	/*else if(!mwclient.getPlayer().getHouse().equalsIgnoreCase(mwclient.getserverConfigs("NewbieHouseName")) &&
                	mwclient.getserverConfigs("FreeBuild_PostDefection").equalsIgnoreCase("true"))
        	{
        		mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "SOLCREATEUNIT " + selectedUnit.getRealFilename() + "#" + TableUnit.getEntityWeight(tempEntity) + "#" + (String) factionCombo.getSelectedItem());
        	} ... dont need this?*/
        	else 
        	{
        		mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "SOLCREATEUNIT " + selectedUnit.getRealFilename() + "#" + TableUnit.getEntityWeight(tempEntity));
        	}
       	
        	createButton.setEnabled(true);
        	loadTables();
        	refresh();

        }
    }

}// end SolFreeBuildDialog class
