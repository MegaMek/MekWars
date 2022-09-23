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
package client.gui.dialog.buildtableviewer;

import java.awt.Component;
import java.io.IOException;
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
 * @author Spork
 */
public class BuildTable {
	private double weight = 0.0;
	private HashMap<String, BuildTableEntry> entries = new HashMap<String, BuildTableEntry>();
	private String name = "";
	
	private JScrollPane pane = new JScrollPane();
	private JTable table = new JTable();
	private JLabel unusedLabel = new JLabel("This table is not used on this server");
	
	private boolean isUsed;
	
	/**
	 * Constructor with no arguments.  Everything must be set after the fact
	 */
	public BuildTable() {
		
	}
	
	/**
	 * Constructor with only the name
	 * @param name the name to set
	 */
	public BuildTable(String name) {
		this.name = name;
	}
	
	/**
	 * Set the name of the build table
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Set the build table weight.  Not used yet, but when I do recursed build tables, it will
	 * @param weight How strongly the contents play into overall percentages
	 */
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	/**
	 * Manually set the lines of the build table
	 * @param entries the BuildTableEntries
	 */
	public void setEntries(HashMap<String, BuildTableEntry> entries) {
		this.entries = entries;
	}
	
	/**
	 * Get the name of the build table
	 * @return the name of the build table
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Get the table weight
	 * @return the table weight (double)
	 */
	public double getWeight() {
		return weight;
	}
	
	/**
	 * Get the actual table.
	 * @return a JTable if there is such a table.  Otherwise a JLabel indicating that the table does not exist
	 */
	public Component getTable() {
		if(isUsed) {
			return table;
		} else {
			return unusedLabel;
		}
	}
	
	/**
	 * Get a Map of the table entries, sorted first by type (table, then unit), then by frequency
	 * @return the sorted entries
	 */
	private Map<String, BuildTableEntry> getSortedTable() {
		Map<String, BuildTableEntry> result = entries.entrySet().stream()
				.sorted(Map.Entry.comparingByValue())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(oldValue, newValue) -> oldValue, LinkedHashMap::new));
		return result;
	}
	
	/**
	 * Get the BuildTableEntries
	 * @return the entries
	 */
	public HashMap<String, BuildTableEntry> getEntries() {
		return entries;
	}
	
	/**
	 * Add an entry to the Map
	 * @param entry the BuildTableEntry to add
	 */
	public void addEntry(BuildTableEntry entry) {
		if(!entries.containsKey(entry.getEntry())) {
			entries.put(entry.getEntry(), entry);
		} else {
			int chance = entry.getChance();
			chance += entries.get(entry.getEntry()).getChance();
			entries.get(entry.getEntry()).setChance(chance);
		}
	}
	
	/**
	 * Load the table from disk.  Please note this currently only does standard, not reward or rare
	 */
	public void loadTable() {
		//TODO: Make it read from reward and rare
		
		if(name.length() < 1) {
			return;
		}
		if(!name.endsWith(".txt")) {
			return;
		}
		
		String fileName = "./data/buildtables/standard/" + name;
		
		try {
			Files.lines(Paths.get(fileName)).forEach((line) -> {
				String number = line.substring(0, line.indexOf(' '));
				String entry = line.substring(line.indexOf(' ') + 1);
				int type;
				BuildTableEntry bte = new BuildTableEntry();
				
				if(entry.indexOf('.') == -1) {
					type = BuildTableEntry.ENTRYTYPE_TABLE;
				} else {
					type = BuildTableEntry.ENTRYTYPE_UNIT;
				}
				bte.setChance(Integer.parseInt(number));
				bte.setEntry(entry);
				bte.setType(type);
				addEntry(bte);
			});
			buildPanel();
			isUsed = true;
		} catch (IOException e) {
			isUsed = false;
		}
	}
	
	/**
	 * Builds the JPanel containing the table
	 */
	private void buildPanel() {
		String[] columns = new String[] {
				"Chance", "Entry"
		};
		
		Map<String, BuildTableEntry> entries = getSortedTable();
		
		Object[][] data = new Object[entries.size()][2];
		
		int row = 0;
		for (String name : entries.keySet()) {
			//data[row][0] = new JLabel(Integer.toString(entries.get(name).getChance()));
			//data[row][1] = new JLabel(name);
			data[row][0] = entries.get(name).getChance();
			data[row][1] = name;
			row++;
		}
		
		table = new JTable(data, columns) {
			private static final long serialVersionUID = 6521858792316925120L;

			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		
		//table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		//table.setModel(new BuildTableModel());
		TableColumn column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(25);
		
		column = table.getColumnModel().getColumn(1);
		
		int maxWidth = 0;
		for(int trow = 0; trow < table.getRowCount(); trow++) {
			TableCellRenderer cellRenderer = table.getCellRenderer(row, 1);
			Component c = table.prepareRenderer(cellRenderer, trow, 1);
			int width = c.getPreferredSize().width + table.getIntercellSpacing().width;
			maxWidth = Math.max(maxWidth, width);
		}
		column.setPreferredWidth(maxWidth+25);
		//table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		pane.add(table);
	}
}
