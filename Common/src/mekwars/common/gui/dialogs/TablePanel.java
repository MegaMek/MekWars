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
package mekwars.common.gui.dialogs;

import java.io.Serial;

import jakarta.annotation.Nonnull;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import mekwars.common.House;
import mekwars.common.Unit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.MWUnitDisplay;
import mekwars.common.gui.dialogs.buildtableviewer.BuildTable;
import mekwars.common.gui.dialogs.buildtableviewer.BuildTableViewer;
import mekwars.common.gui.dialogs.buildtableviewer.SelectorPanel;
import mekwars.common.util.MWLogger;

/**
 * A JPanel containing a JTable representing a BuildTable
 *
 * @author Spork
 *
 */
public class TablePanel extends javax.swing.JPanel implements java.awt.event.ActionListener {

    @Serial
    private static final long serialVersionUID = 1348587767892438630L;
    private final SelectorPanel selector;
    private final BuildTableViewer viewer;
    private final IClient client;

    private final java.util.HashMap<String, BuildTable> tables = new java.util.HashMap<>();

    private javax.swing.JPanel displayPanel = new javax.swing.JPanel();

    /**
     * Build a TablePanel containing a JTable representing a BuildTable
     *
     * @param v the BuildTableViewer itself
     * @param s the SelectorPanel containing the faction/type/weight JCombos
     * @param c the client
     */
    public TablePanel(BuildTableViewer v, SelectorPanel s, IClient c) {
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
        java.util.TreeSet<String> factionNamesOrdered = new java.util.TreeSet<>();

        for (House house : client.getData().getAllHouses()) {
            if (house.getId() > -1) {
                factionNamesOrdered.add(house.getName());
            }
        }

        java.util.LinkedHashSet<String> factionNames = new java.util.LinkedHashSet<>(factionNamesOrdered);
        factionNames.add("Common");

        for (String factionName : factionNames) {
            for (int unitWeight = Unit.LIGHT; unitWeight <= Unit.ASSAULT; unitWeight++) {
                for (int unitType = Unit.MEK; unitType <= Unit.MAX_BUILD; unitType++) {
                    BuildTable bt = getBuildTable(factionName, unitWeight, unitType);
                    tables.put(bt.getName(), bt);
                }
            }
        }
    }

    private static @Nonnull BuildTable getBuildTable(String factionName, int unitWeight, int unitType) {
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
        return bt;
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

            MekSummary ms = MekSummaryCache.getInstance().getMek(fileName);
            try {
                entity = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();

                javax.swing.JFrame infoWindow = new javax.swing.JFrame();
                UnitDisplayPanel unitDisplay = new MWUnitDisplay(null, client);
                entity.loadAllWeapons();
                infoWindow.getContentPane().add(unitDisplay);
                infoWindow.setSize(300, 400);
                infoWindow.setResizable(false);
                infoWindow.setTitle(entity.getModel());
                infoWindow.setLocationRelativeTo(null);
                infoWindow.setVisible(true);
                unitDisplay.displayEntity(entity);

            } catch (EntityLoadingException e) {
                MWLogger.errLog(e);
            }
        }
    }
}
