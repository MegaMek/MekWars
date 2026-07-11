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
package mekwars.common.gui.dialogs.buildtableviewer;

import java.awt.Component;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * A representation of a server-side BuildTable, used to generate the tables shown to users
 *
 * @author Spork
 */
public class BuildTable {
    private final JScrollPane pane = new JScrollPane();
    private final JLabel unusedLabel = new JLabel("This table is not used on this server");
    private double weight = 0.0;
    private HashMap<String, BuildTableEntry> entries = new HashMap<>();
    private String name = "";
    private JTable table = new JTable();
    private boolean isUsed;

    /**
     * Constructor with no arguments.  Everything must be set after the fact
     */
    public BuildTable() {

    }

    /**
     * Constructor with only the name
     *
     * @param name the name to set
     */
    public BuildTable(String name) {
        this.name = name;
    }

    /**
     * Get the name of the build table
     *
     * @return the name of the build table
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the build table
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the table weight
     *
     * @return the table weight (double)
     */
    public double getWeight() {
        return weight;
    }

    /**
     * Set the build table weight.  Not used yet, but when I do recursed build tables, it will
     *
     * @param weight How strongly the contents play into overall percentages
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * Get the actual table.
     *
     * @return a JTable if there is such a table.  Otherwise a JLabel indicating that the table does not exist
     */
    public java.awt.Component getTable() {
        if (isUsed) {
            return table;
        } else {
            return unusedLabel;
        }
    }

    /**
     * Get the BuildTableEntries
     *
     * @return the entries
     */
    public java.util.HashMap<String, BuildTableEntry> getEntries() {
        return entries;
    }

    /**
     * Manually set the lines of the build table
     *
     * @param entries the BuildTableEntries
     */
    public void setEntries(java.util.HashMap<String, BuildTableEntry> entries) {
        this.entries = entries;
    }

    /**
     * Load the table from disk.  Please note this currently only does standard, not reward or rare
     */
    public void loadTable() {
        //TODO: Make it read from reward and rare

        if (name.isEmpty()) {
            return;
        }
        if (!name.endsWith(".txt")) {
            return;
        }

        String fileName = String.format("./data/buildtables/standard/%s", name);

        try {
            Files.lines(Paths.get(fileName)).forEach((line) -> {
                String number = line.substring(0, line.indexOf(' '));
                String entry = line.substring(line.indexOf(' ') + 1);
                int type;
                BuildTableEntry buildTableEntry = new BuildTableEntry();

                if (entry.indexOf('.') == -1) {
                    type = BuildTableEntry.ENTRY_TYPE_TABLE;
                } else {
                    type = BuildTableEntry.ENTRY_TYPE_UNIT;
                }

                buildTableEntry.setChance(Integer.parseInt(number));
                buildTableEntry.setEntry(entry);
                buildTableEntry.setType(type);
                addEntry(buildTableEntry);
            });
            buildPanel();
            isUsed = true;
        } catch (java.io.IOException e) {
            isUsed = false;
        }
    }

    /**
     * Add an entry to the Map
     *
     * @param entry the BuildTableEntry to add
     */
    public void addEntry(BuildTableEntry entry) {
        if (!entries.containsKey(entry.getEntry())) {
            entries.put(entry.getEntry(), entry);
        } else {
            int chance = entry.getChance();
            chance += entries.get(entry.getEntry()).getChance();
            entries.get(entry.getEntry()).setChance(chance);
        }
    }

    /**
     * Builds the JPanel containing the table
     */
    private void buildPanel() {
        String[] columns = new String[] { "Chance", "Entry" };

        Map<String, BuildTableEntry> entries = getSortedTable();

        Object[][] data = new Object[entries.size()][2];

        int row = 0;
        for (String name : entries.keySet()) {
            data[row][0] = entries.get(name).getChance();
            data[row][1] = name;
            row++;
        }

        table = new JTable(data, columns) {
            @Serial
            private static final long serialVersionUID = 6521858792316925120L;

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        TableColumn column = table.getColumnModel().getColumn(0);
        column.setPreferredWidth(25);

        column = table.getColumnModel().getColumn(1);

        int maxWidth = 0;
        for (int trow = 0; trow < table.getRowCount(); trow++) {
            TableCellRenderer cellRenderer = table.getCellRenderer(row, 1);
            Component component = table.prepareRenderer(cellRenderer, trow, 1);
            int width = component.getPreferredSize().width + table.getIntercellSpacing().width;
            maxWidth = Math.max(maxWidth, width);
        }
        column.setPreferredWidth(maxWidth + 25);
        pane.add(table);
    }

    /**
     * Get a Map of the table entries, sorted first by type (table, then unit), then by frequency
     *
     * @return the sorted entries
     */
    private Map<String, BuildTableEntry> getSortedTable() {
        return entries.entrySet().stream()
                     .sorted(Map.Entry.comparingByValue())
                     .collect(Collectors.toMap(Map.Entry::getKey,
                           Map.Entry::getValue,
                           (oldValue, ignored) -> oldValue,
                           LinkedHashMap::new));
    }
}
