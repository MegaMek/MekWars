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

package mekwars.common.gui.dialogs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.Serial;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;

import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.common.loaders.MULParser;
import megamek.common.units.Entity;
import mekwars.common.House;
import mekwars.common.Unit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.MWUnitDisplay;
import mekwars.common.gui.TableSorter;
import mekwars.common.util.MWLogger;
import mekwars.common.util.SpringLayoutHelper;

/**
 *
 * @deprecated As of Client v0.7.0.1, replaced by BuildTableViewer.java
 *
 */
@Deprecated
public class TableViewerDialog extends javax.swing.JFrame implements java.awt.event.ItemListener {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -5449211786198003020L;
    // ivars
    javax.swing.JComboBox<String> weightClassCombo;
    javax.swing.JComboBox<String> factionCombo;
    javax.swing.JComboBox<String> unitTypeCombo;

    javax.swing.JLabel factionLabel = new javax.swing.JLabel("Faction: ", javax.swing.SwingConstants.RIGHT);
    javax.swing.JLabel typeLabel = new javax.swing.JLabel("Type: ", javax.swing.SwingConstants.RIGHT);
    javax.swing.JLabel weightLabel = new javax.swing.JLabel("Class: ", javax.swing.SwingConstants.RIGHT);
    javax.swing.JLabel percentageLabel = new javax.swing.JLabel("Total Percentage: ",
          javax.swing.SwingConstants.CENTER);

    String[] factionArray = {};
    String[] unitTypeArray = { "Mek", "Vehicle", "BattleArmor", "Infantry", "ProtoMek", "Aero" };
    String[] weightClassArray = { "Light", "Medium", "Heavy", "Assault" };

    int factionSort = 0;
    int unitSort = 0;
    int weightSort = 0;

    javax.swing.JTable generalTable = new javax.swing.JTable();
    javax.swing.JScrollPane generalScrollPane;

    javax.swing.JButton closeButton = new javax.swing.JButton("Close");
    javax.swing.JButton refreshButton = new javax.swing.JButton("Reload Data");

    // model and whatnot for refreshing
    TableViewerModel tvModel;

    // maps and sorts
    java.util.TreeMap<Object, TableUnit> currentUnits;
    TableUnit[] sortedUnits = {};// sorts generated from the map.

    IClient client;

    // constructor
    public TableViewerDialog(IClient client) {
        super("Table Browser");

        this.client = client;
        currentUnits = new TreeMap<>();
        generalScrollPane = new JScrollPane();

        // alpha sorted faction array. hacky and evil.
        TreeSet<String> factionNames = new TreeSet<>();// tree to alpha
        // sort
        for (House house : this.client.getData().getAllHouses()) {
            if (house.getId() > -1) {
                factionNames.add(house.getName());
            }
        }

        factionNames.add("Common");
        factionArray = factionNames.toArray(factionArray);

        // CONSTRUCT GUI
        // make combo boxes
        weightClassCombo = new JComboBox<>(weightClassArray);
        factionCombo = new JComboBox<>(factionArray);
        unitTypeCombo = new JComboBox<>();

        for (int type = Unit.MEK; type < Unit.MAX_BUILD; type++) {
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
        try {
            String previousItem = this.client.getConfigParam("TABLEVIEWERFACTION");
            factionCombo.setSelectedItem(previousItem);
            factionSort = factionCombo.getSelectedIndex();
        } catch (Exception e) {
            factionCombo.setSelectedItem(this.client.getPlayer().getHouse());
        }

        // If type/weight data are missing, select the first item in the combo.
        try {
            String previousItem = this.client.getConfigParam("TABLEVIEWERTYPE");
            unitTypeCombo.setSelectedItem(previousItem);
            unitSort = unitTypeCombo.getSelectedIndex();
        } catch (Exception e) {
            unitTypeCombo.setSelectedIndex(-1);
        }

        try {
            String previousItem = this.client.getConfigParam("TALEVIEWERWEIGHT");
            weightClassCombo.setSelectedItem(previousItem);
            weightSort = weightClassCombo.getSelectedIndex();
        } catch (Exception e) {
            weightClassCombo.setSelectedIndex(-1);
        }

        // add listeners to the combo boxes
        factionCombo.addItemListener(this);
        unitTypeCombo.addItemListener(this);
        weightClassCombo.addItemListener(this);

        // allow the close button to actually close things ...
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(_ -> dispose());

        refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        refreshButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        refreshButton.addActionListener(_ -> refreshButton_ActionPerformed());

        // set up the BM-style table
        tvModel = new TableViewerModel(this.client, currentUnits, sortedUnits);
        TableSorter sorter = new TableSorter(tvModel,
              client,
              TableSorter.SORTER_BUILD_TABLES);
        generalTable.setModel(sorter);

        // make it possible to double-click for unit info
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
                    UnitDisplayPanel unitDetailInfo = new MWUnitDisplay(null, client);

                    infoWindow.getContentPane().add(unitDetailInfo);
                    infoWindow.setSize(300, 400);
                    infoWindow.setResizable(false);

                    infoWindow.setTitle(u.getModelName());
                    infoWindow.setLocationRelativeTo(TableViewerDialog.this.client.getMainFrame());// center
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
        generalScrollPane.setBorder(
              BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0),
                    BorderFactory.createLineBorder(
                          Color.BLACK, 1)));

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
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);

        SpringLayoutHelper.setupSpringGrid(buttonPanel, 2);

        boxPanel.add(buttonPanel);

        // give the box a small border
        boxPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // add the box to the main panel
        getContentPane().add(boxPanel);

        // load the default tables/units
        loadTables();
        refresh();
        setLocationRelativeTo(this.client.getMainFrame());
        setVisible(true);
    }

    // refresh
    public void refresh() {
        tvModel.refreshModel();
        generalTable.setPreferredSize(new java.awt.Dimension(generalTable.getWidth(),
              generalTable.getRowHeight() * (generalTable.getRowCount())));
        generalTable.revalidate();
    }

    // Override show to center on screen.
    @Override
    public void setVisible(boolean show) {

        pack();
        setLocationRelativeTo(client.getMainFrame());

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
     * Method to conform with ItemListener. Takes item events from the combo boxes and triggers table loads.
     */
    @Override
    public void itemStateChanged(java.awt.event.ItemEvent i) {

        /*
         * Do not re-load tables and units if there is no actual change in the
         * selection.
         */
        @SuppressWarnings("unchecked")
        javax.swing.JComboBox<String> source = (javax.swing.JComboBox<String>) i.getSource();
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
        client.getConfig().setParam("TABLEVIEWERFACTION", (String) factionCombo.getSelectedItem());
        client.getConfig().setParam("TABLEVIEWERTYPE", (String) unitTypeCombo.getSelectedItem());
        client.getConfig().setParam("TALEVIEWERWEIGHT", (String) weightClassCombo.getSelectedItem());
        client.getConfig().saveConfig();
        client.setConfig();

        // refresh the display
        refresh();
    }

    /**
     * Helper which checks strings to see if they end with a known-good unit file extension.
     */
    public boolean hasValidExtension(String l) {
        String lc = l.toLowerCase();
        return lc.endsWith(".blk") || lc.endsWith(".mtf") || lc.endsWith(".mul");
    }

    /**
     * Helper which takes a File entry and returns an input stream. Handles errors, etc. to reduce clutter in
     * loadTables().
     */
    public java.io.InputStream getEntryInputStream(java.io.File bf) {
        java.io.InputStream is;
        try {
            is = new java.io.FileInputStream(bf);
            return is;
        } catch (java.io.IOException io) {
            return null;
        }
    }

    /**
     * Helper which loops through a table, ignoring filenames and tablenames. Returns total table weighting for use when
     * analyzing names.
     */
    public int getTotalWeightForTable(java.io.File bf) {

        int totalweight = 0;

        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(bf);
            java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
            while (dis.ready()) {
                // read the line and remove excess whitespace
                String l = dis.readLine();

                if ((l == null) || (l.trim().isEmpty())) {
                    continue;
                }

                l = l.trim();
                l = l.replaceAll("\\s+", " ");
                if (l.indexOf(" ") == 0) {
                    l = l.substring(1);
                }

                java.util.StringTokenizer ST = new java.util.StringTokenizer(l);
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
     * Helper method which reads a given layer of tables. Extracted from loadTables to reduce repetition; however, doing
     * do actually makes each check (inparticular, the first and last map levels) more complex than they would
     * otherwise.
     */
    public void doTableLayer(java.util.TreeMap<String, Double> curr, java.util.TreeMap<String, Double> next, String add,
          java.io.File buildTablePath, boolean commonOverride) {

        /*
         * Set up an iterator of target tables. Note that the first level (base
         * table) is put into a dummy treemap in order to have an iterator.
         */
        for (String currTableName : curr.keySet()) {
            // get zip entry for the new file.
            java.io.File tableEntry;
            if (commonOverride) {
                tableEntry = new java.io.File(STR."\{buildTablePath.getPath()}\{File.separatorChar}Common\{add}");
            } else {
                tableEntry = new java.io.File(buildTablePath.getPath() +
                                                    java.io.File.separatorChar +
                                                    currTableName +
                                                    add);
            }

            if (!tableEntry.exists()) {
                if (commonOverride) {
                    tableEntry = new java.io.File((STR."\{buildTablePath.getPath()}\{File.separatorChar}Common\{add}").toLowerCase());
                } else {
                    tableEntry = new java.io.File((buildTablePath.getPath() +
                                                         java.io.File.separatorChar +
                                                         currTableName +
                                                         add).toLowerCase());
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
                java.io.InputStream is = getEntryInputStream(tableEntry);
                java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(is));

                /*
                 * TableMultiplier is used to determine the relative value of
                 * each entry. For example, if the Orion ONI-1K appears on a
                 * target table at 50%, and the tableweight is .10 (aka - 10%),
                 * the ONI's actual frequency is 5%.
                 */
                double tableMultiplier = curr.get(currTableName);

                try {
                    while (dis.ready()) {

                        // read the line. make sure it's not empty.
                        String l = dis.readLine();
                        if ((l == null) || (l.trim().isEmpty())) {
                            continue;
                        }

                        // remove excess whitespace
                        l = l.trim();
                        l = l.replaceAll("\\s+", " ");
                        if (l.indexOf(" ") == 0) {
                            l = l.substring(1);
                        }

                        /*
                         * All lines should have weights. Set up a
                         * StringTokenizer and grab common data before seperate
                         * file/table work is done.
                         */
                        java.util.StringTokenizer ST = new java.util.StringTokenizer(l);
                        double weight = Double.parseDouble((String) ST.nextElement());

                        /*
                         * Determine whether this line is a cross-linked table
                         * or an actual unit file. Assume a valid file if the
                         * entry ends with a known unit file extension. If no
                         * known extension is present, assume a crosslinked
                         * table.
                         */
                        if (hasValidExtension(l) && (weight != 0)) {

                            StringBuilder Filename = new StringBuilder();
                            while (ST.hasMoreElements()) {
                                Filename.append(ST.nextToken());
                                if (ST.hasMoreElements()) {
                                    Filename.append(" ");
                                }
                            }

                            /*
                             * Now that we have a filename, create a TableUnit.
                             * Check for duplication before adding to
                             * currentUnits. If the file in question is a dupe,
                             * simply add its frequency to that of the existing
                             * unit.
                             */
                            double frequency = (weight / totaltableweight) * tableMultiplier;

                            if (Filename.toString().toLowerCase().endsWith(".mul")) {

                                java.util.Vector<Entity> loadedUnits;
                                java.io.File entityFile = new java.io.File("data/armies/" + Filename);

                                try {
                                    loadedUnits = new MULParser(entityFile, null).getEntities();
                                    loadedUnits.trimToSize();
                                    frequency /= loadedUnits.size();
                                } catch (Exception ex) {
                                    MWLogger.errLog("Unable to load file " + entityFile.getName());
                                    MWLogger.errLog(ex);
                                    continue;
                                }

                                for (Entity en : loadedUnits) {
                                    TableUnit tu = new TableUnit(en, frequency);
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
                            } else {
                                TableUnit tu = new TableUnit(Filename.toString(), frequency);
                                TableUnit eu = currentUnits.get(Filename.toString());// existing
                                // unit
                                if (eu != null) {
                                    eu.addFrequencyFrom(tu);
                                } else {
                                    currentUnits.put(Filename.toString(), tu);
                                }

                                /*
                                 * Add this table as a source.
                                 */
                                eu = currentUnits.get(Filename.toString());// existing
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
                        } else if (weight != 0) {// is a crosslink table
                            StringBuilder crossTableName = new StringBuilder();
                            while (ST.hasMoreElements()) {
                                crossTableName.append(ST.nextToken());
                                if (ST.hasMoreElements()) {
                                    crossTableName.append(" ");
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
                                    Double d = next.get(crossTableName.toString());
                                    double newTableWeight = d + ((weight / totaltableweight) * tableMultiplier);
                                    next.remove(crossTableName.toString());
                                    next.put(crossTableName.toString(), newTableWeight);
                                } else {
                                    next.put(crossTableName.toString(), (weight / totaltableweight) * tableMultiplier);
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
     * Method which loads tables and TableUnits, based on current ComboBox selections. This is the beef of the class
     * ...
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
        addOnString += STR."_\{weightClassCombo.getSelectedItem()}";

        /*
         * Finally, determine the type of unit to look at.
         */
        String type = (String) unitTypeCombo.getSelectedItem();
        if (type != null && !type.equals("Mek")) {
            addOnString += type;
        }

        // always look for a .txt
        addOnString += ".txt";
        // System.out.println("AddOn String: " + addOnString);

        /*
         * Look for the build tables
         */
        java.io.File buildTablePath = new java.io.File("./data/buildtables/standard");
        if (!buildTablePath.exists()) {
            MWLogger.errLog("Could not find build tables.");
            return;
        }

        /*
         * Reset currentUnits.
         */
        currentUnits.clear();

        /*
         * Found the zip. Now extract an appropriate entry. Try normal casing
         * and lowercasing/
         */
        boolean overrideWithCommon = false;
        File tableEntry = new File(buildTablePath.getPath() +
                                         File.separatorChar +
                                         factionString +
                                         addOnString);

        /*
         * A clutch of treemaps. These are used to store info on crosslinked
         * tables. Note that linkage hops could extend in perpetuity, so
         * stopping after 3 hops will generate some minor rounding errors.
         */
        java.util.TreeMap<String, Double> crossMap1 = new java.util.TreeMap<>();
        java.util.TreeMap<String, Double> crossMap2 = new java.util.TreeMap<>();

        /*
         * Original Table. A dummy treemap is used here in order to pass a
         * treemap to the doTableLayer method. The initial table is the only
         * value and carries a 100% weight.
         */
        java.util.TreeMap<String, Double> temp = new java.util.TreeMap<>();
        temp.put(factionString, 100.0);// using 100 makes things
        // %'s instead of decimals
        // ...
        // System.out.println("this.doTableLayer - base");
        doTableLayer(temp, crossMap1, addOnString, buildTablePath, overrideWithCommon);

        while (true) {
            // 1st cross-linkages (2nd degree)
            // System.out.println("this.doTableLayer - map1");
            doTableLayer(crossMap1, crossMap2, addOnString, buildTablePath, false);

            if (crossMap2.isEmpty()) {
                break;
            }

            crossMap1 = crossMap2;
            crossMap2 = new java.util.TreeMap<>();
        }

        /*
         * Table, and 3 degrees of separation, processed as well as possible.
         * Holes may exist if linked tables are given bad pointers on the
         * tables, or if linkages are pervasive and 3 hops are not enough to
         * cover most of the crosstalk.
         */

        /*
         * Update the total percentage counter.
         */
        double totalPercent = 0;
        for (TableUnit currUnit : currentUnits.values()) {
            totalPercent += currUnit.getFrequency();
        }
        java.text.DecimalFormat myFormatter = new java.text.DecimalFormat("###.#####");
        percentageLabel.setText("Total Percentage: " + myFormatter.format(totalPercent) + "%");
    }

    // inner classes


    public void refreshButton_ActionPerformed() {

        int userLevel = client.getUserLevel();

        refreshButton.setEnabled(false);
        if (userLevel >= client.getData().getAccessLevel("AdminRequestBuildTable")) {
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c AdminRequestBuildTable#list#true");
        } else if (userLevel >= client.getData().getAccessLevel("RequestBuildTable")) {
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c RequestBuildTable#list#true");
        }

        client.setWaiting(true);
        while (client.isWaiting()) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {

            }
        }
        loadTables();
        refresh();
        refreshButton.setEnabled(true);
    }
}// end TableViewerDialog class
