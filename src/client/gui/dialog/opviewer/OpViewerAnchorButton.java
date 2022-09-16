/*
 * MekWars - Copyright (C) 2014 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Original author Helge Richter (McWizard)
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

package client.gui.dialog.opviewer;

import javax.swing.JButton;

public class OpViewerAnchorButton extends JButton {

	private static final long serialVersionUID = 1L;
	private String url;
	
	public String getUrl() {
		return url;
	}
	
	public OpViewerAnchorButton(String id, String display) {
		super(display);
		url = id;
	}
}
