/*
 * MekWars - Copyright (C) 2004 
 * 
 * Original Author - Nathan Morris (urgru@users.sourceforge.net)
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
package client.cmd;

import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import client.MWClient;

/**
 * Command that sends a player's unit list.
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class LPU extends Command {
	
	/**
	 * @param client
	 */
	public LPU(MWClient mwclient) {
		super(mwclient);
	}
	
	/**
	 * @see client.cmd.Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {
		StringTokenizer command = decode(input);
		String commandName = command.nextToken();
		String username = command.nextToken();
		String unitList = command.nextToken();
		String receivingPlayer = null;
		
		if( command.hasMoreTokens() )
			receivingPlayer = command.nextToken();
		
		StringTokenizer units = new StringTokenizer(unitList,"#");
		
		TreeSet<String> list = new TreeSet<String>();
		
		while ( units.hasMoreElements() ){
			list.add("#"+units.nextToken());
		}
		JComboBox combo = new JComboBox(list.toArray());
		combo.setEditable(false);
		JOptionPane jop = new JOptionPane(combo, JOptionPane.QUESTION_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION);
		
		JDialog dlg = jop.createDialog(mwclient.getMainFrame(), "Select a unit.");
		combo.grabFocus();
		combo.getEditor().selectAll();
		
		dlg.setVisible(true);
		String unit = (String) combo.getSelectedItem();
		
		unit = unit.substring(0,unit.indexOf(" "));
		
		int value = ((Integer) jop.getValue()).intValue();
		
		if (value == JOptionPane.CANCEL_OPTION)
			return;
		
		if ( receivingPlayer != null ){
			if( commandName.equalsIgnoreCase("admintransfer") )
				mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c "+commandName+"#"+username+"#"+receivingPlayer+unit);
			else if ( commandName.equalsIgnoreCase("viewplayerunit") )
				mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c "+commandName+"#"+username+unit+"#"+receivingPlayer);
		}
		else
			mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c "+commandName+"#"+username+unit);
	}
}
