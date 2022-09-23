/*
 * MekWars - Copyright (C) 2005 
 * 
 * Original author - Jason Tighe (urgru@users.sourceforge.net)
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

/**
 * Sends Access Levels for each command back to the user.
 * 
 * @author Torren (Jason Tighe) 8.15.05 
 * 
 */
package server.dataProvider.commands;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import common.CampaignData;
import common.util.BinWriter;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;
import server.dataProvider.ServerCommand;

/**
 * Retrieve all planet information (if the data cache is lost at client side)
 * 
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class CommandAccessLevels implements ServerCommand {

    /**
     * @see server.dataProvider.ServerCommand#execute(java.util.Date,
     *      java.io.PrintWriter, common.CampaignData)
     */
    public void execute(Date timestamp, BinWriter out, CampaignData data)
            throws Exception {
		Hashtable<String,Command> commandTable = CampaignMain.cm.getServerCommands();
		Enumeration<String> commands = commandTable.keys();

		out.println(commandTable.size(),"CommandSize");
		while (commands.hasMoreElements())
		{
			String commandName = commands.nextElement();
			Command commandMethod = commandTable.get(commandName);
			
			out.println(commandName,"CommandName");
	        out.println(commandMethod.getExecutionLevel(),"AccessLevel");
		}
    }
}
