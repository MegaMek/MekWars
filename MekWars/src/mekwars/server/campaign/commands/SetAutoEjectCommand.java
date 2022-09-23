/*
 * MekWars - Copyright (C) 2004 
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
 */

package server.campaign.commands;


import java.util.StringTokenizer;

import megamek.common.Entity;
import megamek.common.Mech;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;

public class SetAutoEjectCommand implements Command {
	
	int accessLevel = 0;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		
		int unitid= 0;//ID# of the mech which is to set autoeject;
		boolean autoEject = false;
		
		try {
			unitid= Integer.parseInt(command.nextToken());
		}//end try
		catch (NumberFormatException ex) {
			CampaignMain.cm.toUser("AM:SetAutoEject command failed. Check your input. It should be something like this: /c setAutoEject#unitid#true/false",Username,true);
			return;
		}//end catch
		
		try {
			autoEject = Boolean.parseBoolean(command.nextToken());
		}//end try
		catch (Exception ex){
			CampaignMain.cm.toUser("AM:SetAutoEject Command failed. Check your input. It should be something like this: /c setAutoEject#unitid#true/false",Username,true);
			return;
		}//end catch
		
		SUnit unit = p.getUnit(unitid);
		Entity en = unit.getEntity();
		((Mech)en).setAutoEject(autoEject);
		unit.setEntity(en);
		CampaignMain.cm.toUser("AM:AutoEject set for "+ unit.getModelName(),Username,true);
		
	}//end process() 
}//end SetAutoEjectCommand class

