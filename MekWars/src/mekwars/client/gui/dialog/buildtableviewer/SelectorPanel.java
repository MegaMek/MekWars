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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import client.MWClient;
import common.House;
import common.Unit;
import common.util.SpringLayoutHelper;

/**
 * A JPanel containing several JComboBoxes to control what table is displayed in the {@link TablePanel}
 * @author Spork
 *
 */
public class SelectorPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = -7776384437283951081L;

	private MWClient client;
	
	private JLabel factionLabel = new JLabel("Faction: ", SwingConstants.TRAILING);
	private JLabel typeLabel = new JLabel("Type: ", SwingConstants.TRAILING);
	private JLabel weightLabel = new JLabel("Weight: ", SwingConstants.TRAILING);
	
	private String[] factionArray = {};
	private String[] typeArray = {};
	private String[] weightArray = {};
	
	private JComboBox<String> weightCombo;
	private JComboBox<String> typeCombo;
	private JComboBox<String> factionCombo;
	
	private Vector<ActionListener> listeners = new Vector<ActionListener>();
	
	/**
	 * Constructor
	 * @param c client
	 */
	public SelectorPanel(MWClient c) {
		this.client = c;
		
		prepComponents();
		
		//this.setBorder(BorderFactory.createLineBorder(Color.black));
		setLayout (new SpringLayout());
		add(factionLabel);
		add(factionCombo);
		add(typeLabel);
        add(typeCombo);
        add(weightLabel);
        add(weightCombo);
        SpringLayoutHelper.setupSpringGrid(this, 2);
	}
	
	/**
	 * Creates all the combos, depending on what units the server allows
	 */
	private void prepComponents() {
		TreeSet<String> typeNames = new TreeSet<String>();
		for(int i = 0; i < Unit.MAXBUILD; i++) {
			if ( (i == Unit.VEHICLE) && !(Boolean.parseBoolean(client.getserverConfigs("UseVehicle"))) ) {
				continue;
			}
			if ( (i == Unit.INFANTRY) && !(Boolean.parseBoolean(client.getserverConfigs("UseInfantry"))) ) {
				continue;
			}
			if ( (i == Unit.BATTLEARMOR) && !(Boolean.parseBoolean(client.getserverConfigs("UseBattleArmor"))) ) {
				continue;
			}
			if ( (i == Unit.PROTOMEK) && !(Boolean.parseBoolean(client.getserverConfigs("UseProtoMek"))) ) {
				continue;
			}
			if ( (i == Unit.AERO) && !(Boolean.parseBoolean(client.getserverConfigs("UseAero"))) ) {
				continue;
			}

			typeNames.add(Unit.getTypeClassDesc(i));
		}
		typeArray = typeNames.toArray(typeArray);
		
		LinkedHashSet<String> weightNames = new LinkedHashSet<String>();
		for(int i = Unit.LIGHT; i <= Unit.ASSAULT; i++) {
			weightNames.add(Unit.getWeightClassDesc(i));
		}
		weightArray = weightNames.toArray(weightArray);
		
		TreeSet<String> factionNamesOrdered = new TreeSet<String>();
		Iterator<House> i = client.getData().getAllHouses().iterator();
        while (i.hasNext()) {
            House house = i.next();

            if (house.getId() > -1) {
                factionNamesOrdered.add(house.getName());
            }
        }
        LinkedHashSet<String> factionNames = new LinkedHashSet<String>(factionNamesOrdered);
        factionNames.add("Common");
        factionArray =  factionNames.toArray(factionArray);
        
        weightCombo = new JComboBox<String>(weightArray);
        typeCombo = new JComboBox<String>(typeArray);
        factionCombo = new JComboBox<String>(factionArray);
        
        weightCombo.addActionListener(this);
        typeCombo.addActionListener(this);
        factionCombo.addActionListener(this);
        
	}
	
	/**
	 * Combines the JComboBoxes into a String that can be used to pick a build table
	 * @return the build table to display
	 */
	public String getSelectionString() {
		StringBuilder sb = new StringBuilder();
		sb.append(factionCombo.getSelectedItem());
		sb.append("_");
		sb.append(weightCombo.getSelectedItem());
		
		String type = (String)typeCombo.getSelectedItem();
		if(!type.equalsIgnoreCase("Mek")) {
			sb.append(typeCombo.getSelectedItem());
		}
		sb.append(".txt");
		return sb.toString();
	}

	/**
	 * A change was made to a combo
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		for (ActionListener listener : listeners) {
			listener.actionPerformed(e);
		}
	}
	
	/**
	 * Add an Object listening for changes
	 * @param listener the ActionListener to add
	 */
	public void addActionListener(ActionListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Remove a listening Object
	 * @param listener the ActionListener to remove
	 */
	public void removeActionListener(ActionListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Called by {@link TablePanel} when a table cell is double-clicked 
	 * @param house the faction to change to
	 */
	public void setSelectedFaction(String house) {
		factionCombo.setSelectedItem(house);
	}
	
	/**
	 * Called by {@link TablePanel} when the tables are first built.  Sets
	 * the selected table to the user's faction
	 * @param house the faction to change to
	 */
	public void setDefaultSelectedFaction(String house) {
		factionCombo.setSelectedItem(house);
		typeCombo.setSelectedItem("Mek");
	}
}
