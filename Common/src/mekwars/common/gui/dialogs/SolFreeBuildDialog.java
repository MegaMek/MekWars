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


package mekwars.common.gui.dialogs;

import java.io.Serial;

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

public class SolFreeBuildDialog extends javax.swing.JFrame implements java.awt.event.ItemListener {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -5449999786199993020L;
    // ivars
    javax.swing.JComboBox<String> weightClassCombo;
    javax.swing.JComboBox<String> factionCombo;
    javax.swing.JComboBox<String> unitTypeCombo;

    javax.swing.JLabel factionLabel = new javax.swing.JLabel("Faction: ", javax.swing.SwingConstants.RIGHT);
    javax.swing.JLabel typeLabel = new javax.swing.JLabel("Type: ", javax.swing.SwingConstants.RIGHT);
    javax.swing.JLabel weightLabel = new javax.swing.JLabel("Class: ", javax.swing.SwingConstants.RIGHT);
    javax.swing.JLabel percentageLabel = new javax.swing.JLabel("Please Select a Unit To Create",
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
    javax.swing.JButton createButton = new javax.swing.JButton("Create (ALT+C)");

    // model and whatnot for refreshing
    TableViewerModel tvModel;

    // maps and sorts
    java.util.TreeMap<Object, TableUnit> currentUnits;
    TableUnit[] sortedUnits = {};// sorts generated from the map.

    //@Salient adding this to capture unit selection
    TableUnit selectedUnit;

    IClient client;

    // constructor
    public SolFreeBuildDialog(IClient client) {
        super("Free Unit Browser");

        this.client = client;
        currentUnits = new java.util.TreeMap<>();
        generalScrollPane = new javax.swing.JScrollPane();

        // alpha sorted faction array. hacky and evil.
        java.util.TreeSet<String> factionNames = new java.util.TreeSet<>();// tree to alpha

        // if freebuild use all option is checked, and player is in SOL, all houses are loaded into the dialog
        if (this.client.getServerConfigs("Sol_FreeBuild_UseAll").equalsIgnoreCase("true") &&
                  this.client.getPlayer()
                        .getHouse()
                        .equalsIgnoreCase(this.client.getServerConfigs("NewbieHouseName"))) {

            for (House house : this.client.getData().getAllHouses()) {
                if (house.getId() > -1) {
                    factionNames.add(house.getName());
                }
            }

            //check if build table is set to common, if not add it
            if (!this.client.getServerConfigs("Sol_FreeBuild_BuildTable").equalsIgnoreCase("Common")) {
                factionNames.add("Common");
            }
        }

        // Only going to allow SOL to build from a table defined by DSO
        factionNames.add(this.client.getServerConfigs("Sol_FreeBuild_BuildTable"));

        // If player is not in newbie house AND post defection is true add only their house to list
        if (!this.client.getPlayer().getHouse().equalsIgnoreCase(this.client.getServerConfigs("NewbieHouseName")) &&
                  this.client.getServerConfigs("FreeBuild_PostDefection").equalsIgnoreCase("true")) {
            factionNames.clear();
            factionNames.add(this.client.getPlayer().getHouse().trim());
        }

        factionArray = factionNames.toArray(factionArray);

        // CONSTRUCT GUI
        // make combo boxes
        weightClassCombo = new javax.swing.JComboBox<>(weightClassArray);
        factionCombo = new javax.swing.JComboBox<>(factionArray);
        unitTypeCombo = new javax.swing.JComboBox<>();

        for (int type = Unit.MEK; type < Unit.MAX_BUILD; type++) {
            unitTypeCombo.addItem(Unit.getTypeClassDesc(type));
        }

        // set max combo heights
        java.awt.Dimension comboDim = new java.awt.Dimension();
        comboDim.setSize(factionCombo.getMinimumSize().getWidth() * 1.5, factionLabel.getMinimumSize().getHeight() + 2);

        factionCombo.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        factionCombo.setMaximumSize(comboDim);
        weightClassCombo.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        weightClassCombo.setMaximumSize(comboDim);
        unitTypeCombo.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        unitTypeCombo.setMaximumSize(comboDim);

        // put the combos and their labels into a spring
        javax.swing.JPanel comboPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());
        comboPanel.add(factionLabel);
        comboPanel.add(factionCombo);
        comboPanel.add(typeLabel);
        comboPanel.add(unitTypeCombo);
        comboPanel.add(weightLabel);
        comboPanel.add(weightClassCombo);
        SpringLayoutHelper.setupSpringGrid(comboPanel, 3, 2);

        weightClassCombo.setSelectedIndex(0);
        unitTypeCombo.setSelectedIndex(0);
        factionCombo.setSelectedItem(0);


        // add listeners to the combo boxes
        factionCombo.addItemListener(this);
        unitTypeCombo.addItemListener(this);
        weightClassCombo.addItemListener(this);

        // allow the close button to actually close things ...
        closeButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        closeButton.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(_ -> dispose());

        refreshButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        refreshButton.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
        refreshButton.addActionListener(_ -> refreshButton_ActionPerformed());

        createButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        createButton.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
        createButton.setMnemonic(java.awt.event.KeyEvent.VK_C);
        createButton.addActionListener(_ -> createUnit_ActionPerformed());

        // set up the BM-style table
        tvModel = new TableViewerModel(this.client, currentUnits, sortedUnits);
        TableSorter sorter = new TableSorter(tvModel, client, TableSorter.SORTER_BUILD_TABLES);
        generalTable.setModel(sorter);

        // make it possible to double-click for unit info
        generalTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TableUnit u = getUnitAtRow(generalTable.getSelectedRow());
                    if (u == null) {
                        return;
                    }

                    Entity theEntity = u.getEntity();
                    theEntity.loadAllWeapons();

                    javax.swing.JFrame infoWindow = new javax.swing.JFrame();
                    UnitDisplayPanel unitDetailInfo = new MWUnitDisplay(null, client);

                    infoWindow.getContentPane().add(unitDetailInfo);
                    infoWindow.setSize(300, 400);
                    infoWindow.setResizable(false);

                    infoWindow.setTitle(u.getModelName());
                    infoWindow.setLocationRelativeTo(SolFreeBuildDialog.this.client.getMainFrame());// center
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
        generalTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

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
              javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 10, 0),
                    javax.swing.BorderFactory.createLineBorder(
                          java.awt.Color.BLACK, 1)));

        // make a box layout to hold the combos and table
        javax.swing.JPanel boxPanel = new javax.swing.JPanel();
        boxPanel.setLayout(new javax.swing.BoxLayout(boxPanel, javax.swing.BoxLayout.Y_AXIS));
        javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());

        // center the percentage label
        percentageLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        percentageLabel.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
        percentageLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0));

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
        boxPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // add the box to the main panel
        getContentPane().add(boxPanel);

        // load the default tables/units
        loadTables();
        refresh();
        setLocationRelativeTo(this.client.getMainFrame());
        setVisible(true);
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

        String filename = (String) generalTable.getModel()
                                         .getValueAt(row,
                                               mekwars.common.gui.dialogs.TableViewerModel.FILENAME);
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
        java.io.File buildTablePath;
        buildTablePath = new java.io.File("./data/buildtables/standard");
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
        // System.out.println("Attempting to find base table entry.");
        java.io.File tableEntry = new java.io.File(buildTablePath.getPath() +
                                                         java.io.File.separatorChar +
                                                         factionString +
                                                         addOnString);
        if (tableEntry == null) {
            tableEntry = new java.io.File((buildTablePath.getPath() +
                                                 java.io.File.separatorChar +
                                                 factionString +
                                                 addOnString).toLowerCase());
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
            tableEntry = new java.io.File(buildTablePath.getPath() +
                                                java.io.File.separatorChar +
                                                "Common" +
                                                addOnString);
        }

        /*
         * If cased common is also null, try lower case. If this fails, return.
         */
        if (tableEntry == null) {
            // System.out.println("Didn't find Common table with standard
            // casing. Retrying in lower case.");
            tableEntry = new java.io.File((buildTablePath.getPath() +
                                                 java.io.File.separatorChar +
                                                 "Common" +
                                                 addOnString).toLowerCase());
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
         * Table, and 3 degrees of seperation, processed as well as possible.
         * Holes may exist if linked tables are given bad pointers on the
         * tables, or if linkages are pervasive and 3 hops are insufficient to
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
    }

    // refresh
    public void refresh() {
        tvModel.refreshModel();
        generalTable.setPreferredSize(new java.awt.Dimension(generalTable.getWidth(),
              generalTable.getRowHeight() * (generalTable.getRowCount())));
        generalTable.revalidate();
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
                tableEntry = new java.io.File(buildTablePath.getPath() + java.io.File.separatorChar + "Common" + add);
            } else {
                tableEntry = new java.io.File(buildTablePath.getPath() +
                                                    java.io.File.separatorChar +
                                                    currTableName +
                                                    add);
            }

            if (!tableEntry.exists()) {
                if (commonOverride) {
                    tableEntry = new java.io.File((buildTablePath.getPath() +
                                                         java.io.File.separatorChar +
                                                         "Common" +
                                                         add).toLowerCase());
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
                 * the ONI's actual frequncy is 5%.
                 */
                double tablemultiplier = curr.get(currTableName);

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
                            double frequency = (weight / totaltableweight) * tablemultiplier;

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
                                    double newTableWeight = d + ((weight / totaltableweight) * tablemultiplier);
                                    next.remove(crossTableName.toString());
                                    next.put(crossTableName.toString(), newTableWeight);
                                } else {
                                    next.put(crossTableName.toString(), (weight / totaltableweight) * tablemultiplier);
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
     * Helper which checks strings to see if they end with a known-good unit file extension.
     */
    public boolean hasValidExtension(String l) {
        String lc = l.toLowerCase();
        return lc.endsWith(".blk") || lc.endsWith(".mtf") || lc.endsWith(".mul");
    }

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

    public void createButton_ActionPerformed() {


        int userLevel = client.getUserLevel();

        refreshButton.setEnabled(false);
        if (userLevel >= client.getData().getAccessLevel("AdminRequestBuildTable")) {
            client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c AdminRequestBuildTable#list#true");
        } else if (userLevel >= client.getData().getAccessLevel("RequestBuildTable")) {
            client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c RequestBuildTable#list#true");
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

    //@Salient
    public void createUnit_ActionPerformed() {

        selectedUnit = getUnitAtRow(generalTable.getSelectedRow());
        Entity tempEntity = selectedUnit.getEntity();
        //why does mekwars use 0-3 and megamek uses 1-4 for weight classes? ... :(

        if (selectedUnit != null) {
            createButton.setEnabled(false);

            if (client.getServerConfigs("Sol_FreeBuild_UseAll").equalsIgnoreCase("true") ||
                      client.getServerConfigs("FreeBuild_PostDefection")
                            .equalsIgnoreCase("true")) //may not need this, command will always check house table if postdefection is enabled.
            {
                client.sendChat(
                      STR."\{IClient.CAMPAIGN_PREFIX}SOLCREATEUNIT \{selectedUnit.getRealFilename()}#\{TableUnit.getEntityWeight(
                            tempEntity)}#\{factionCombo.getSelectedItem().toString()}");
            } else {
                client.sendChat(
                      STR."\{IClient.CAMPAIGN_PREFIX}SOLCREATEUNIT \{selectedUnit.getRealFilename()}#\{TableUnit.getEntityWeight(
                            tempEntity)}");
            }

            createButton.setEnabled(true);
            loadTables();
            refresh();

        }
    }

}// end SolFreeBuildDialog class
