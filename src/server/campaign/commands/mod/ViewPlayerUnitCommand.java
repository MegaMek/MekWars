/*
 * MekWars - Copyright (C) 2007 
 *
 * Original author - jtighe (torren@users.sourceforge.net)
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

package server.campaign.commands.mod;

import java.util.StringTokenizer;

import common.util.UnitUtils;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.commands.Command;

/**
 * Sends a Players Unit data to a Mod/Admin
 */
public class ViewPlayerUnitCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Player Name#Unit ID#Show Damage[true/false]";
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
		
		//get the player you wish to use
		SPlayer p;
		SUnit unit;
		int unitId;
		boolean damage = false;
		try{
			p = CampaignMain.cm.getPlayer(command.nextToken());
			unitId = Integer.parseInt(command.nextToken());
			damage = Boolean.parseBoolean(command.nextToken());
		}
        catch(Exception ex){
            CampaignMain.cm.toUser("Syntax: ViewPlayerUnit#Name#UnitID#ShowDamage[true/false]",Username);
            return;
        }
		
        if ( p == null ){
            CampaignMain.cm.toUser("Player does not exist!",Username);
            return;
        }
        unit = p.getUnit(unitId);
        
        if ( unit == null ){
        	CampaignMain.cm.toUser(p.getName()+" does not have unit #"+unitId, Username);
        	return;
        }
        
        String fileName = unit.getEntity().getChassis() + " " +  unit.getEntity().getModel();
        if ( !damage)
        	CampaignMain.cm.toUser("PL|VUI|"+fileName+ "#" + unit.getBVForMatch() + "#" + unit.getPilot().getGunnery() + "#" + unit.getPilot().getPiloting()+"#"+UnitUtils.unitBattleDamage(unit.getEntity(), true), Username,false);
        else
        	CampaignMain.cm.toUser("PL|VURD|"+fileName+ "#"+UnitUtils.unitBattleDamage(unit.getEntity(), true), Username,false);
        CampaignMain.cm.doSendModMail("NOTE",Username+" has viewed "+p.getName()+"'s "+unit.getModelName());
        CampaignMain.cm.toUser(Username+" has viewed your "+unit.getModelName()+".", p.getName());
        
	}
}