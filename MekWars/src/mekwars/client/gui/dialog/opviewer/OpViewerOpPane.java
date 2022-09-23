/*
 * MekWars - Copyright (C) 2013 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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
 *
 */
package client.gui.dialog.opviewer;

import javax.swing.JTextPane;


public class OpViewerOpPane extends JTextPane implements IOpViewerPane {

	private static final long serialVersionUID = 1L;

	public void setHTMLContents(String s) {
		this.setContentType("text/html");
		this.setText(s);
	}
	
	public OpViewerOpPane(String s) {
		super();
		setHTMLContents(s);
	}
	
}
