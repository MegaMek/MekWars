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
import java.io.Serial;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.*;

import jakarta.annotation.Nullable;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.codeUtilities.MathUtility;
import megamek.common.loaders.MULParser;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekwars.common.House;
import mekwars.common.Unit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.MWUnitDisplay;
import mekwars.common.gui.TableSorter;
import mekwars.common.util.SpringLayoutHelper;

/**
 * SolFreeBuildDialog
 * <p>
 * August 2017 Duplicated and modified TableViewerDialog in an attempt to create a new dialog for SOL players to create
 * any mek/vee on a pre-defined build table. This is part of a Larger system to change how SOL works in general.
 *
 * @author Salient (mwosux@gmail.com)
 */
public class SolFreeBuildDialog extends JFrame implements ItemListener {
    private static final MMLogger LOGGER = MMLogger.create(SolFreeBuildDialog.class);

    @Serial
    private static final long serialVersionUID = -5449999786199993020L;
    private final TableViewerModel tvModel;
    private final TreeMap<Object, TableUnit> currentUnits;
    private final IClient client;
    private final JTable generalTable = new JTable();
    private final JButton refreshButton = new JButton("Reload Data");
    private final JButton createButton = new JButton("Create (ALT+C)");
    private final JComboBox<String> weightClassCombo;
    private final JComboBox<String> factionCombo;
    private final JComboBox<String> unitTypeCombo;
    private String[] unitTypeArray = { "Mek", "Vehicle", "BattleArmor", "Infantry", "ProtoMek", "Aero" };
    private int factionSort = 0;
    private int unitSort = 0;
    private int weightSort = 0;

    // constructor
    public SolFreeBuildDialog(IClient client) {
        super("Free Unit Browser");

        this.client = client;
        currentUnits = new TreeMap<>();
        JScrollPane generalScrollPane = new JScrollPane();

        // alpha sorted faction array. hacky and evil.
        TreeSet<String> factionNames = new TreeSet<>();// tree to alpha

        // if free build use all option is checked, and player is in SOL, all houses are loaded into the dialog
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

        String[] factionArray = factionNames.toArray(new String[0]);

        // CONSTRUCT GUI
        // make combo boxes
        String[] weightClassArray = { "Light", "Medium", "Heavy", "Assault" };
        weightClassCombo = new JComboBox<>(weightClassArray);
        factionCombo = new JComboBox<>(factionArray);
        unitTypeCombo = new JComboBox<>();

        for (int type = Unit.MEK; type < Unit.MAX_BUILD; type++) {
            unitTypeCombo.addItem(Unit.getTypeClassDesc(type));
        }

        // set max combo heights
        Dimension comboDim = new Dimension();
        JLabel factionLabel = new JLabel("Faction: ", SwingConstants.RIGHT);
        comboDim.setSize(factionCombo.getMinimumSize().getWidth() * 1.5, factionLabel.getMinimumSize().getHeight() + 2);

        factionCombo.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        factionCombo.setMaximumSize(comboDim);
        weightClassCombo.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        weightClassCombo.setMaximumSize(comboDim);
        unitTypeCombo.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        unitTypeCombo.setMaximumSize(comboDim);

        // put the combos and their labels into a spring
        JPanel comboPanel = new JPanel(new SpringLayout());
        comboPanel.add(factionLabel);
        comboPanel.add(factionCombo);
        JLabel typeLabel = new JLabel("Type: ", SwingConstants.RIGHT);
        comboPanel.add(typeLabel);
        comboPanel.add(unitTypeCombo);
        JLabel weightLabel = new JLabel("Class: ", SwingConstants.RIGHT);
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
        JButton closeButton = new JButton("Close");
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(actionEvent -> dispose());

        refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        refreshButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        refreshButton.addActionListener(actionEvent -> refreshButton_ActionPerformed());

        createButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        createButton.setMnemonic(KeyEvent.VK_C);
        createButton.addActionListener(actionEvent -> createUnit_ActionPerformed());

        // set up the BM-style table
        // sorts generated from the map.
        TableUnit[] sortedUnits = {};
        tvModel = new TableViewerModel(this.client, currentUnits, sortedUnits);
        TableSorter sorter = new TableSorter(tvModel, client, TableSorter.SORTER_BUILD_TABLES);
        generalTable.setModel(sorter);

        // make it possible to double-click for unit info
        generalTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
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
              BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0),
                    BorderFactory.createLineBorder(Color.BLACK, 1)));

        // make a box layout to hold the combos and table
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.Y_AXIS));
        JPanel buttonPanel = new JPanel(new SpringLayout());

        // center the percentage label
        JLabel percentageLabel = new JLabel("Please Select a Unit To Create", SwingConstants.CENTER);
        percentageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        percentageLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        percentageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // add combo boxes scrollPane and percentage info to the box panel
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
    public @Nullable TableUnit getUnitAtRow(int row) {
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
    public void itemStateChanged(ItemEvent itemEvent) {

        /*
         * Do not re-load tables and units if there is no actual change in the
         * selection.
         */
        Object source = itemEvent.getSource();

        if (source instanceof JComboBox<?> comboSource) {
            if ((comboSource == unitTypeCombo) && (unitSort == unitTypeCombo.getSelectedIndex())) {
                return;
            } else if ((comboSource == weightClassCombo) && (weightSort == weightClassCombo.getSelectedIndex())) {
                return;
            } else if ((comboSource == factionCombo) && (factionSort == factionCombo.getSelectedIndex())) {
                return;
            }
        } else {
            return;
        }

        // fails passed. reload the tables.
        loadTables();

        // save the current sort modes locally
        unitSort = unitTypeCombo.getSelectedIndex();
        weightSort = weightClassCombo.getSelectedIndex();
        factionSort = factionCombo.getSelectedIndex();

        // save the new sort to the config
        client.getConfig().setParam("TABLE_VIEWER_FACTION", (String) factionCombo.getSelectedItem());
        client.getConfig().setParam("TABLE_VIEWER_TYPE", (String) unitTypeCombo.getSelectedItem());
        client.getConfig().setParam("TABLE_VIEWER_WEIGHT", (String) weightClassCombo.getSelectedItem());
        client.getConfig().saveConfig();
        client.setConfig();

        // refresh the display
        refresh();
    }

    /**
     * Method that loads tables and TableUnits, based on current ComboBox selections. This is the beef of the class ...
     */
    public void loadTables() {
        String factionString = "";
        String addOnString = "";

        /*
         * First, determine faction.
         */
        factionString += (String) factionCombo.getSelectedItem();

        /*
         * Next, determine the weight class.
         */
        addOnString += String.format("_%s", weightClassCombo.getSelectedItem());

        /*
         * Finally, determine the type of unit to look at.
         */
        String type = (String) unitTypeCombo.getSelectedItem();
        if (type != null && !type.equals("Mek")) {
            addOnString += type;
        }

        // always look for a .txt
        addOnString += ".txt";

        /*
         * Look for the build tables
         */
        File buildTablePath;
        buildTablePath = new File("./data/buildtables/standard");
        if (!buildTablePath.exists()) {
            LOGGER.debug("Could not find build tables.");
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

        /*
         * A clutch of treetops. These are used to store info on cross linked
         * tables. Note that linkage hops could extend in perpetuity, so
         * stopping after 3 hops will generate some minor rounding errors.
         */
        TreeMap<String, Double> crossMap1 = new TreeMap<>();
        TreeMap<String, Double> crossMap2 = new TreeMap<>();

        /*
         * Original Table. A dummy treemap is used here in order to pass a
         * treemap to the doTableLayer method. The initial table is the only
         * value and carries a 100% weight.
         */
        TreeMap<String, Double> temp = new TreeMap<>();
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
    }

    // refresh
    public void refresh() {
        tvModel.refreshModel();
        generalTable.setPreferredSize(new Dimension(generalTable.getWidth(),
              generalTable.getRowHeight() * (generalTable.getRowCount())));
        generalTable.revalidate();
    }

    /**
     * Helper method which reads a given layer of tables. Extracted from loadTables to reduce repetition; however, doing
     * do actually makes each check (in particular, the first and last map levels) more complex than they would
     * otherwise.
     */
    public void doTableLayer(TreeMap<String, Double> curr, TreeMap<String, Double> next, String add,
          File buildTablePath, boolean commonOverride) {

        /*
         * Set up an iterator of target tables. Note that the first level (base
         * table) is put into a dummy treemap to have an iterator.
         */
        for (String currTableName : curr.keySet()) {
            // get zip entry for the new file.
            File tableEntry;
            if (commonOverride) {
                tableEntry = new File(String.format("%s%sCommon%s", buildTablePath.getPath(), File.separatorChar, add));
            } else {
                tableEntry = new File(buildTablePath.getPath() + File.separatorChar + currTableName + add);
            }

            if (!tableEntry.exists()) {
                if (commonOverride) {
                    tableEntry = new File((String.format("%s%sCommon%s", buildTablePath.getPath(), File.separatorChar, add)).toLowerCase());
                } else {
                    tableEntry = new File((buildTablePath.getPath() +
                                                 File.separatorChar +
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
                int totalWeightForTable = getTotalWeightForTable(tableEntry);

                /*
                 * InputStream and Buffered reader for a second pass through the
                 * file.
                 */
                InputStream is = getEntryInputStream(tableEntry);
                BufferedReader dis = new BufferedReader(new InputStreamReader(is));

                /*
                 * TableMultiplier is used to determine the relative value of
                 * each entry. For example, if the Orion ONI-1K appears on a
                 * target table at 50%, and the table weight is .10 (aka - 10%),
                 * the ONI's actual frequency is 5%.
                 */
                double tableMultiplier = curr.get(currTableName);

                try {
                    while (dis.ready()) {
                        // read the line. make sure it's not empty.
                        String line = dis.readLine();
                        if ((line == null) || (line.trim().isEmpty())) {
                            continue;
                        }

                        // remove excess whitespace
                        line = line.trim();
                        line = line.replaceAll("\\s+", " ");
                        if (line.indexOf(" ") == 0) {
                            line = line.substring(1);
                        }

                        /*
                         * All lines should have weights. Set up a
                         * StringTokenizer and grab common data before separate
                         * file/table work is done.
                         */
                        StringTokenizer stringTokenizer = new StringTokenizer(line);
                        double weight = MathUtility.parseDouble((String) stringTokenizer.nextElement(), 0.0);

                        /*
                         * Determine whether this line is a cross-linked table
                         * or an actual unit file. Assume a valid file if the
                         * entry ends with a known unit file extension. If no
                         * known extension is present, assume a cross linked
                         * table.
                         */
                        if (hasValidExtension(line) && (weight != 0)) {
                            StringBuilder Filename = new StringBuilder();
                            while (stringTokenizer.hasMoreElements()) {
                                Filename.append(stringTokenizer.nextToken());

                                if (stringTokenizer.hasMoreElements()) {
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
                            double frequency = (weight / totalWeightForTable) * tableMultiplier;

                            if (Filename.toString().toLowerCase().endsWith(".mul")) {
                                Vector<Entity> loadedUnits;
                                File entityFile = new File(String.format("data/armies/%s", Filename));

                                try {
                                    loadedUnits = new MULParser(entityFile, null).getEntities();
                                    loadedUnits.trimToSize();
                                    frequency /= loadedUnits.size();
                                } catch (Exception ex) {
                                    LOGGER.error(ex, "Unable to load file {}", entityFile.getName());
                                    continue;
                                }

                                for (Entity entity : loadedUnits) {
                                    TableUnit tableUnit = new TableUnit(entity, frequency);
                                    TableUnit existingUnit = currentUnits.get(tableUnit.getRealFilename());// existing
                                    // unit
                                    if (existingUnit != null) {
                                        existingUnit.addFrequencyFrom(tableUnit);
                                    } else {
                                        currentUnits.put(tableUnit.getRealFilename(), tableUnit);
                                    }

                                    /*
                                     * Add this table as a source.
                                     */
                                    existingUnit = currentUnits.get(tableUnit.getRealFilename());// existing
                                    // unit
                                    if (existingUnit.getTables().get(currTableName) == null) {
                                        existingUnit.getTables().put(currTableName, frequency);
                                    } else {
                                        Double currFreq = existingUnit.getTables().get(currTableName);
                                        Double newFreq = currFreq + frequency;
                                        existingUnit.getTables().remove(currTableName);
                                        existingUnit.getTables().put(currTableName, newFreq);
                                    }
                                }
                            } else {
                                TableUnit tableUnit = new TableUnit(Filename.toString(), frequency);
                                TableUnit existingUnit = currentUnits.get(Filename.toString());// existing
                                // unit
                                if (existingUnit != null) {
                                    existingUnit.addFrequencyFrom(tableUnit);
                                } else {
                                    currentUnits.put(Filename.toString(), tableUnit);
                                }

                                /*
                                 * Add this table as a source.
                                 */
                                existingUnit = currentUnits.get(Filename.toString());// existing
                                // unit
                                if (existingUnit.getTables().get(currTableName) == null) {
                                    existingUnit.getTables().put(currTableName, frequency);
                                } else {
                                    Double currFreq = existingUnit.getTables().get(currTableName);
                                    Double newFreq = currFreq + frequency;
                                    existingUnit.getTables().remove(currTableName);
                                    existingUnit.getTables().put(currTableName, newFreq);
                                }
                            }
                        } else if (weight != 0) {// is a crossing table
                            StringBuilder crossTableName = new StringBuilder();
                            while (stringTokenizer.hasMoreElements()) {
                                crossTableName.append(stringTokenizer.nextToken());
                                if (stringTokenizer.hasMoreElements()) {
                                    crossTableName.append(" ");
                                }
                            }

                            /*
                             * Put the cross link into the map, if another layer
                             * exists. Check for duplication. If next is null
                             * there are no more cross link hops to be mode,
                             * which means sorting would be a waste of time.
                             */
                            if (next != null) {
                                if (next.containsKey("crossTableName")) {
                                    Double aDouble = next.get(crossTableName.toString());
                                    double newTableWeight = aDouble +
                                                                  ((weight / totalWeightForTable) * tableMultiplier);
                                    next.remove(crossTableName.toString());
                                    next.put(crossTableName.toString(), newTableWeight);
                                } else {
                                    next.put(crossTableName.toString(),
                                          (weight / totalWeightForTable) * tableMultiplier);
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
     * Helper that loops through a table, ignoring filenames and table names. Returns total table weighting for use when
     * analyzing names.
     */
    public int getTotalWeightForTable(File file) {
        int totalweight = 0;

        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader dis = new BufferedReader(new InputStreamReader(fis));

            while (dis.ready()) {
                // read the line and remove excess whitespace
                String line = dis.readLine();

                if ((line == null) || (line.trim().isEmpty())) {
                    continue;
                }

                line = line.trim();
                line = line.replaceAll("\\s+", " ");
                if (line.indexOf(" ") == 0) {
                    line = line.substring(1);
                }

                StringTokenizer stringTokenizer = new StringTokenizer(line);
                totalweight += MathUtility.parseInt((String) stringTokenizer.nextElement(), 0);
            }
            fis.close();
            dis.close();
        } catch (Exception e) {
            // nothing
        }

        return totalweight;
    }

    /**
     * Helper that takes a File entry and returns an input stream. Handles errors, etc. to reduce clutter in
     * loadTables().
     */
    public @Nullable InputStream getEntryInputStream(File file) {
        InputStream inputStream;

        try {
            inputStream = new FileInputStream(file);
            return inputStream;
        } catch (IOException io) {
            return null;
        }
    }

    /**
     * Helper that checks strings to see if they end with a known-good unit file extension.
     */
    public boolean hasValidExtension(String line) {
        String lowerCase = line.toLowerCase();
        return lowerCase.endsWith(".blk") || lowerCase.endsWith(".mtf") || lowerCase.endsWith(".mul");
    }

    public void refreshButton_ActionPerformed() {
        int userLevel = client.getUserLevel();

        refreshButton.setEnabled(false);
        if (userLevel >= client.getData().getAccessLevel("AdminRequestBuildTable")) {
            client.sendChat(String.format("%sc AdminRequestBuildTable#list#true", IClient.CAMPAIGN_PREFIX));
        } else if (userLevel >= client.getData().getAccessLevel("RequestBuildTable")) {
            client.sendChat(String.format("%sc RequestBuildTable#list#true", IClient.CAMPAIGN_PREFIX));
        }

        client.setWaiting(true);
        while (client.isWaiting()) {
            try {
                Thread.sleep(100);
            } catch (Exception ignored) {

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
            client.sendChat(String.format("%sc AdminRequestBuildTable#list#true", IClient.CAMPAIGN_PREFIX));
        } else if (userLevel >= client.getData().getAccessLevel("RequestBuildTable")) {
            client.sendChat(String.format("%sc RequestBuildTable#list#true", IClient.CAMPAIGN_PREFIX));
        }

        client.setWaiting(true);
        while (client.isWaiting()) {
            try {
                Thread.sleep(100);
            } catch (Exception ignored) {

            }
        }
        loadTables();
        refresh();
        refreshButton.setEnabled(true);
    }

    //@Salient
    public void createUnit_ActionPerformed() {

        TableUnit selectedUnit = getUnitAtRow(generalTable.getSelectedRow());
        //why does mekwars use 0-3 and megamek uses 1-4 for weight classes? ... :(

        if (selectedUnit != null) {
            Entity tempEntity = selectedUnit.getEntity();
            createButton.setEnabled(false);

            Object factionComboSelectedItem = factionCombo.getSelectedItem();

            if ((factionComboSelectedItem instanceof String selectedFaction) &&
                      (client.getServerConfigs("Sol_FreeBuild_UseAll").equalsIgnoreCase("true") ||
                             client.getServerConfigs("FreeBuild_PostDefection")
                                   .equalsIgnoreCase("true"))) //may not need this, command will always check house table if
            // postdefection is enabled.
            {
                client.sendChat(
                      String.format("%sSOLCREATEUNIT %s#%s#%s", IClient.CAMPAIGN_PREFIX, selectedUnit.getRealFilename(), TableUnit.getEntityWeight(
                            tempEntity), selectedFaction));
            } else {
                client.sendChat(
                      String.format("%sSOLCREATEUNIT %s#%s", IClient.CAMPAIGN_PREFIX, selectedUnit.getRealFilename(), TableUnit.getEntityWeight(
                            tempEntity)));
            }

            createButton.setEnabled(true);
            loadTables();
            refresh();

        }
    }

}// end SolFreeBuildDialog class
