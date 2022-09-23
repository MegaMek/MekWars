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

import common.campaign.operations.Operation;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SPlayer;

public class CheckArmyEligibilityCommand implements Command {
	
	
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
		
		int armyid = -1;
		String opName = "";
		try {
			armyid = Integer.parseInt(command.nextToken());
			opName = command.nextToken();
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Improper command. Try: /c checkarmyeligibility#id#operation", Username, true);
			return;
		}
		
		SArmy currA = p.getArmy(armyid);
		if (currA == null) {
			CampaignMain.cm.toUser("AM:Could not find Army #" + armyid + ".", Username, true);
			return;
		}
		
		Operation currO = CampaignMain.cm.getOpsManager().getOperation(opName);
		if (currO == null) {
			CampaignMain.cm.toUser("AM:Operation Type: " + opName + " does not exist.",Username,true);
			return;
		}
		
		//breaks passed. check the op.
		String s = CampaignMain.cm.getOpsManager().validateShortAttack(p, currA, currO, null, -1,false);
		if (s != null && !s.trim().equals("")) {
			CampaignMain.cm.toUser("AM:"+opName + " is illegal for Army #" + armyid + " " + s,Username,true);
			return;
		} 
		
		//else
		CampaignMain.cm.toUser("AM:"+opName + " is legal for Army #" + armyid + ".", Username, true);
					
	}//end process
	
}//end CheckAttackCommand