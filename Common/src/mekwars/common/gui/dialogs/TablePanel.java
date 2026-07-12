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
 * A JPanel containing a JTable representing a BuildTable.
 * <p>
 * This panel is the display half of the "build table viewer" tool: it works together with a
 * {@link SelectorPanel} (which lets the user pick a faction/weight/type combination) and a
 * {@link BuildTableViewer} (the parent container) to show which units can be randomly rolled
 * for a given faction/weight/unit-type build table, and lets the user drill into an individual
 * unit's stats by double-clicking a row.
 * <p>
 * Build tables are pre-loaded eagerly for every known faction (plus a synthetic "Common" faction)
 * across every weight class and unit type in {@link #prepTables()}, and cached in {@link #tables}
 * keyed by file name so that switching selections in the {@link SelectorPanel} is just a map
 * lookup rather than a re-parse of the table file.
 *
 * @author Spork
 *
 */
public class TablePanel extends JPanel implements ActionListener {
    private static final MMLogger LOGGER = MMLogger.create(TablePanel.class);

    @Serial
    private static final long serialVersionUID = 1348587767892438630L;
    /** Combo boxes for choosing faction/weight class/unit type; this panel listens to it. */
    private final SelectorPanel selector;
    /** The parent viewer, refreshed after the displayed table changes. */
    private final BuildTableViewer viewer;
    /** The client, used to look up known factions and to show unit-detail windows. */
    private final IClient client;

    /** Cache of every loaded build table, keyed by its generated file name (e.g. "Faction_Light.txt"). */
    private final HashMap<String, BuildTable> tables = new HashMap<>();

    /** Panel that currently holds the visible table/unit view; swapped out on each selection change. */
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

    /**
     * Loads a single build table for the given faction, weight class, and unit type. The file
     * name is derived as {@code <factionName>_<WeightClassDesc>[<TypeClassDesc>].txt}, e.g.
     * "FactionName_Light.txt" for Meks (Mek is the implicit/default type so its type descriptor
     * is omitted) or "FactionName_LightVehicle.txt" for a non-Mek type.
     *
     * @param factionName name of the faction the table belongs to (or "Common")
     * @param unitWeight  one of the {@code Unit} weight class constants (e.g. LIGHT..ASSAULT)
     * @param unitType    one of the {@code Unit} type constants (e.g. MEK); appended to the file
     *                    name only when not MEK
     * @return the loaded {@link BuildTable} (never null; loading failures are handled internally
     *         by {@link BuildTable#loadTable()})
     */
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
     * The Selector changed, so display the new table.
     * <p>
     * Looks up the currently selected faction/weight/type combination's cached
     * {@link BuildTable}, swaps out {@link #displayPanel} for a fresh panel containing that
     * table, and wires up a double-click listener on the table (if it's a {@link JTable}) so the
     * user can drill into a specific row via {@link #actOnCell(String)}. Non-JTable components
     * (e.g. an error/placeholder component) are added directly without scroll pane or listener.
     * Finally asks the parent {@link BuildTableViewer} to refresh.
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
     * The user double-clicked a cell. Either display the selected table or the selected unit.
     * <p>
     * Distinguishes the two cases by whether {@code cellContents} contains a '.': a bare faction
     * name (no dot) is treated as a nested table reference and selects that faction in the
     * {@link SelectorPanel}; a string containing a dot is treated as a unit file name (e.g.
     * "SomeUnit.mtf"), whose last 4 characters (the extension) are stripped before looking the
     * unit up in {@link MekSummaryCache} and opening a standalone {@link JFrame} with a
     * {@link MWUnitDisplay} showing its stat sheet.
     * <p>
     * Quirk: a brand new {@code JFrame} is created every time a unit row is double-clicked and
     * is never tracked or disposed by this class, so repeated double-clicks accumulate detached
     * windows. Also, if {@code MekSummaryCache.getInstance().getMek(fileName)} returns null (unit
     * not found in the cache), the subsequent {@code mekSummary.getSourceFile()} call will throw
     * an unhandled {@code NullPointerException} rather than being caught by the
     * {@code EntityLoadingException} handler below.
     *
     * @param cellContents A string indicating either a table (faction name) or a unit (file name)
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
