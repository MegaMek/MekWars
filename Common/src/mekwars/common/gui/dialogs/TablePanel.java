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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.TreeSet;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import jakarta.annotation.Nonnull;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekwars.common.House;
import mekwars.common.Unit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.MWUnitDisplay;
import mekwars.common.gui.dialogs.buildtableviewer.BuildTable;
import mekwars.common.gui.dialogs.buildtableviewer.BuildTableViewer;
import mekwars.common.gui.dialogs.buildtableviewer.SelectorPanel;

/**
 * A JPanel containing a JTable representing a BuildTable
 *
 * @author Spork
 *
 */
public class TablePanel extends JPanel implements ActionListener {
    private static final MMLogger LOGGER = MMLogger.create(TablePanel.class);

    @Serial
    private static final long serialVersionUID = 1348587767892438630L;
    private final SelectorPanel selector;
    private final BuildTableViewer viewer;
    private final IClient client;

    private final HashMap<String, BuildTable> tables = new HashMap<>();

    private JPanel displayPanel = new JPanel();

    /**
     * Build a TablePanel containing a JTable representing a BuildTable
     *
     * @param buildTableViewer the BuildTableViewer itself
     * @param selectorPanel    the SelectorPanel containing the faction/type/weight JCombos
     * @param iClient          the client
     */
    public TablePanel(BuildTableViewer buildTableViewer, SelectorPanel selectorPanel, IClient iClient) {
        viewer = buildTableViewer;
        selector = selectorPanel;
        client = iClient;
        selector.addActionListener(this);

        prepTables();
        add(displayPanel);
        selector.setDefaultSelectedFaction(client.getPlayer().getHouse());
    }

    private static @Nonnull BuildTable getBuildTable(String factionName, int unitWeight, int unitType) {
        BuildTable buildTable = new BuildTable();

        //Build the file name
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(factionName);
        stringBuilder.append("_");
        stringBuilder.append(Unit.getWeightClassDesc(unitWeight));

        if (unitType != Unit.MEK) {
            stringBuilder.append(Unit.getTypeClassDesc(unitType));
        }

        stringBuilder.append(".txt");

        buildTable.setName(stringBuilder.toString());
        buildTable.loadTable();

        return buildTable;
    }

    /**
     * Get the tables ready.  This currently does not handle anything that does not contain a faction name.  For
     * instance, on MMNet, we have a table called Contest_Light.txt.  That's a valid table, but the viewer will not
     * currently display it.  On the to-do list
     */
    private void prepTables() {
        //TODO: Make the viewer deal with non-faction tables
        TreeSet<String> factionNamesOrdered = new TreeSet<>();

        for (House house : client.getData().getAllHouses()) {
            if (house.getId() > -1) {
                factionNamesOrdered.add(house.getName());
            }
        }

        LinkedHashSet<String> factionNames = new LinkedHashSet<>(factionNamesOrdered);
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

    /**
     * The Selector changed, so display the new table
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        String tableToDisplay = selector.getSelectionString();
        BuildTable buildTable = tables.get(tableToDisplay);

        remove(displayPanel);

        displayPanel = new JPanel();
        Component table = buildTable.getTable();
        if (table instanceof JTable) {
            table.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent mouseEvent) {
                    if (mouseEvent.getClickCount() == 2) {
                        JTable target = (JTable) mouseEvent.getSource();
                        int row = target.getSelectedRow();
                        String unit = (String) target.getValueAt(row, 1);
                        actOnCell(unit);
                    }
                }
            });

            JScrollPane pane = new JScrollPane(table);
            pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            displayPanel.add(pane);
        } else {
            displayPanel.add(buildTable.getTable(), BorderLayout.CENTER);
        }

        add(displayPanel);
        this.revalidate();
        viewer.refresh();
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

            MekSummary mekSummary = MekSummaryCache.getInstance().getMek(fileName);

            try {
                entity = new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName()).getEntity();

                JFrame infoWindow = new JFrame();
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
                LOGGER.error(e, "Unable to load Entity: {}", e.getLocalizedMessage());
            }
        }
    }
}
