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

import javax.swing.table.DefaultTableModel;

public class BuildTableModel extends DefaultTableModel {
	
	private static final long serialVersionUID = -203607769972828592L;

	/**
	 * Constructor
	 */
	public BuildTableModel() {
		super();
	}
	
	/**
	 * Disable editing the cells of the table
	 */
	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}
}
