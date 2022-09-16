/*
 * MekWars - Copyright (C) 2007 
 * 
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
 * @author jtighe
 * 
 * Command Saves the black market sales and Production data.
 */
package server.campaign.commands.admin;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import common.Equipment;
import common.util.MWLogger;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

public class AdminSaveBlackMarketConfigsCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		
		try{
			PrintStream ps = new PrintStream(new FileOutputStream("./data/blackmarketsettings.dat"));
			ps.println("#Timestamp="+System.currentTimeMillis());
            
			for ( String key : CampaignMain.cm.getBlackMarketEquipmentTable().keySet() ) {
				Equipment bme = CampaignMain.cm.getBlackMarketEquipmentTable().get(key);
				if (bme.getMaxProduction() <= 0 )
					continue;
				ps.print(bme.getEquipmentInternalName());
				ps.print("#");//if the maxCost is less then mincost set min cost to the same as max
				ps.print(Math.min(bme.getMaxCost(),bme.getMinCost()));
				ps.print("#");
				ps.print(bme.getMaxCost());
				ps.print("#");//if the maxProduction is less then minProduction set minProduction to the same as max.
				ps.print(Math.min(bme.getMaxProduction(),bme.getMinProduction()));
				ps.print("#");
				ps.print(bme.getMaxProduction());
				ps.println("#");
			}
			ps.close();
		} catch (FileNotFoundException fe) {
			MWLogger.errLog("blackmarketsettings.dat not found");
		} catch (Exception ex) {
			MWLogger.errLog(ex);
		}   

		CampaignMain.cm.toUser("AM:Black Market Settings saved!",Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " has saved the Black Market Settings");
		
	}//end process
}