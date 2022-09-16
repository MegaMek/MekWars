/*
 * MekWars - Copyright (C) 2005
 * 
 * Original Author - Nathan Morris (urgru)
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
 * A simple helper which takes in data for 2 commands, and fires
 * their process() methods. Used to provide html/links which hire
 * and maintain units simultaneousnly (previously had to present
 * the users with 2 links, one for each command operation.
 * 
 * @urgru
 */

package server.campaign.commands.helpers;

//imports
import java.util.StringTokenizer;

import server.campaign.CampaignMain;
import server.campaign.commands.BuyBaysCommand;
import server.campaign.commands.Command;
import server.campaign.commands.HireTechsCommand;
import server.campaign.commands.RequestCommand;

public class HireAndRequestNewHelper implements Command {
	
	int accessLevel = 0;
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	String syntax = "";
	public String getSyntax() { return syntax;}

	public void process(StringTokenizer command,String Username) {
		
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
        //find out if your are using advanced repair if so buy bays instead of hiring techs.
        boolean useBays = CampaignMain.cm.isUsingAdvanceRepair();

		//get number of techs to hire from the command
		String numtohire = command.nextToken();
		
		/*
		 * Assemble a new StringTokenize which contains only the
		 * Tokens needed by RequestCommand. Its safe to assume that
		 * the Request would have failed initially if any of the
		 * inputs were wrong, so just pass them along without checking.
		 */
		String requestText = "";
		while (command.hasMoreTokens())
			requestText += command.nextToken() + "#";
		
		StringTokenizer requestTokenizer = new StringTokenizer(requestText, "#");
		
        if ( useBays ){
    		//fire the buy
    		BuyBaysCommand buyCommand = new BuyBaysCommand();
    		buyCommand.process(new StringTokenizer(numtohire), Username);
        }
        else{
            //fire the hire
            HireTechsCommand hireCommand = new HireTechsCommand();
            hireCommand.process(new StringTokenizer(numtohire), Username);
        }
		//fire the request
		RequestCommand requestCommand = new RequestCommand();
		requestCommand.process(requestTokenizer, Username);
		
	}//end process()
	
}//end HireAndRequestNewHelper