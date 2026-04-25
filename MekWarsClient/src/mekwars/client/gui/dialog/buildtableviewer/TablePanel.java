/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package mekwars.client.gui.dialog.buildtableviewer;

import common.House;
import common.Unit;
import common.util.MWLogger;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.loaders.EntityLoadingException;

/**
 * A JPanel containing a JTable representing a BuildTable
 *
 * @author Spork
 *
 */
public class TablePanel extends javax.swing.JPanel implements java.awt.event.ActionListener {

    private static final long serialVersionUID = 1348587767892438630L;
    private SelectorPanel selector;
    private BuildTableViewer viewer;
    private client.MWClient client;

    private java.util.HashMap<String, mekwars.client.gui.dialog.buildtableviewer.BuildTable> tables = new java.util.HashMap<String, mekwars.client.gui.dialog.buildtableviewer.BuildTable>();

    private javax.swing.JPanel displayPanel = new javax.swing.JPanel();

    /**
     * Build a TablePanel containing a JTable representing a BuildTable
     *
     * @param v the BuildTableViewer itself
     * @param s the SelectorPanel containing the faction/type/weight JCombos
     * @param c the client
     */
    public TablePanel(BuildTableViewer v, SelectorPanel s, client.MWClient c) {
        viewer = v;
        selector = s;
        client = c;
        selector.addActionListener(this);
        //this.setBorder(BorderFactory.createLineBorder(Color.black));

        prepTables();
        add(displayPanel);
        selector.setDefaultSelectedFaction(client.getPlayer().getHouse());
    }

    /**
     * The Selector changed, so display the new table
     */
    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        String tableToDisplay = selector.getSelectionString();
        BuildTable bt = tables.get(tableToDisplay);

        remove(displayPanel);

        displayPanel = new javax.swing.JPanel();
        //displayPanel.setLayout(new BorderLayout());
        java.awt.Component table = bt.getTable();
        if (table instanceof javax.swing.JTable) {
            //displayPanel.add(((JTable)table).getTableHeader(), BorderLayout.NORTH);
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        javax.swing.JTable target = (javax.swing.JTable) e.getSource();
                        int row = target.getSelectedRow();
                        String unit = (String) target.getValueAt(row, 1);
                        actOnCell(unit);
                    }
                }
            });
            javax.swing.JScrollPane pane = new javax.swing.JScrollPane(table);
            pane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            displayPanel.add(pane);
        } else {
            displayPanel.add(bt.getTable(), java.awt.BorderLayout.CENTER);
        }


        add(displayPanel);
        this.revalidate();
        viewer.refresh();
    }

    /**
     * Get the tables ready.  This currently does not handle anything that does not contain a faction name.  For
     * instance, on MMNet, we have a table called Contest_Light.txt.  That's a valid table, but the viewer will not
     * currently display it.  On the to-do list
     */
    private void prepTables() {
        //TODO: Make the viewer deal with non-faction tables
        java.util.TreeSet<String> factionNamesOrdered = new java.util.TreeSet<String>();
        java.util.Iterator<House> i = client.getData().getAllHouses().iterator();
        while (i.hasNext()) {
            House house = i.next();

            if (house.getId() > -1) {
                factionNamesOrdered.add(house.getName());
            }
        }
        java.util.LinkedHashSet<String> factionNames = new java.util.LinkedHashSet<String>(factionNamesOrdered);
        factionNames.add("Common");

        java.util.Iterator<String> iterator = factionNames.iterator();
        while (iterator.hasNext()) {
            String factionName = iterator.next();
            for (int unitWeight = Unit.LIGHT; unitWeight <= Unit.ASSAULT; unitWeight++) {
                for (int unitType = Unit.MEK; unitType <= Unit.MAXBUILD; unitType++) {
                    BuildTable bt = new BuildTable();

                    //Build the file name
                    StringBuilder sb = new StringBuilder();
                    sb.append(factionName);
                    sb.append("_");
                    sb.append(Unit.getWeightClassDesc(unitWeight));
                    if (unitType != Unit.MEK) {
                        sb.append(Unit.getTypeClassDesc(unitType));
                    }
                    sb.append(".txt");

                    bt.setName(sb.toString());
                    bt.loadTable();
                    tables.put(bt.getName(), bt);
                }
            }
        }
    }

    /**
     * The user double-clicked a cell. Either display the selected table or the selected unit
     *
     * @param cellContents A string indicating either a table or a unit
     */
    private void actOnCell(String cellContents) {
        Entity entity;

        if (cellContents.indexOf('.') == -1) {
            // Table
            selector.setSelectedFaction(cellContents);
        } else {
            // Unit
            // Show a unit dialog
            String fileName = cellContents.trim();
            fileName = fileName.substring(0, fileName.length() - 4);

            MechSummary ms = MechSummaryCache.getInstance().getMech(fileName);
            try {
                entity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();

                javax.swing.JFrame infoWindow = new javax.swing.JFrame();
                UnitDisplay unitdisplay = new client.gui.MWUnitDisplay(null, client);
                entity.loadAllWeapons();
                infoWindow.getContentPane().add(unitdisplay);
                infoWindow.setSize(300, 400);
                infoWindow.setResizable(false);
                infoWindow.setTitle(entity.getModel());
                infoWindow.setLocationRelativeTo(null);
                infoWindow.setVisible(true);
                unitdisplay.displayEntity(entity);

            } catch (EntityLoadingException e) {
                MWLogger.errLog(e);
            }
        }
    }
}
