/*
 * MekWars - Copyright (C) 2005  
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

import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SPlayer;
import server.campaign.operations.OperationManager;
import server.campaign.operations.ShortOperation;
import server.campaign.operations.newopmanager.I_OperationManager;

/**
 * JoinAttackCommand is used to join a
 * faction ShortOperations. Checks the 
 * validity of the attacking force.
 * syntax JoinAttack attackingPlayerName#JoiningPlayersArmyId
 */
public class JoinAttackCommand implements Command {
	
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
		
		I_OperationManager manager = CampaignMain.cm.getOpsManager();
		SPlayer jp = CampaignMain.cm.getPlayer(Username);

		if (jp == null) {
			CampaignMain.cm.toUser("AM:Null player. Contact an administrator to report this, immediately!",Username,true);
			return;
		}
		
		//throw up if the player is not active or fighting
		if (jp.getDutyStatus() < SPlayer.STATUS_ACTIVE) {
			CampaignMain.cm.toUser("AM:You aren't on the front lines! (You are currently in Reserve. Activate in order to attack.)",Username,true);
			return;
		}
		
		//can't attack while in a game
		if(jp.getDutyStatus() == SPlayer.STATUS_FIGHTING) {
			CampaignMain.cm.toUser("AM:You are already fighting!", Username, true);
			return;
		}
		
		//can only attack once
		int altID = CampaignMain.cm.getOpsManager().playerIsAnAttacker(jp);
		if (altID >= 0) {
			CampaignMain.cm.toUser("AM:You're only allowed to attack once, and are already in Attack #" + altID + ".", Username, true);
			return;	
		}
		
		//cant only defend once
		altID = CampaignMain.cm.getOpsManager().playerIsADefender(jp);
		if (altID >= 0) {
			CampaignMain.cm.toUser("AM:You're already defending against Attack #" + altID + ".", Username, true);
			return;	
		}
		
		//narc if the player hasn't been active long enough to attack
		boolean minActiveMet = (System.currentTimeMillis() - jp.getActiveSince()) >=
			(Long.parseLong(CampaignMain.cm.getConfig("MinActiveTime")) * 1000);
		if (!minActiveMet) {
			CampaignMain.cm.toUser("AM:You're still on your way to the frontline. You cannot attack until you arrive.",Username,true);
			return;
		}
		
		SPlayer ap; 
		
		try {
			ap = CampaignMain.cm.getPlayer(command.nextToken());
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Attacking Player not found.",Username,true);
			return;
		}
		
	
		ShortOperation o = manager.getShortOpForPlayer(ap);
		
		if (o == null) {
			CampaignMain.cm.toUser("AM:Short Operation not found for "+ap.getName()+".",Username);
			return;
		}
		
		//get the army being used to attack
		int armyID = -1;
		try {
			armyID = Integer.parseInt(command.nextToken());
		} catch (Exception e){
			CampaignMain.cm.toUser("AM:Non-number given for Army ID. Try again.",Username,true);
			return;
		}
		
		SArmy aa = ap.getArmy(armyID);
		if (aa == null) {
			CampaignMain.cm.toUser("AM:An error occured while creating your Army (The Army was null. This usually means " +
					"the army doesn't exist. Example: you tried to use Army 1, but you only have Armies 0 and 2.)",Username,true);
			return;
		} else if (aa.getBV() == 0) {
			CampaignMain.cm.toUser("AM:Army #" + armyID + " has a BV of 0 and may not be used to attack.",Username,true);
			return;
		}

		CampaignMain.cm.toUser("AM:"+o.checkTeam(ap.getTeamNumber(),aa.getBV(),true), Username);
		/*
		 * Breaks passed. lets validate the attack =)
		 * 
		 * The validator and manager will handle
		 * everything from this point on, assuming
		 * that no failure reasons are returned.
		 */
		
		o.addAttacker(jp, aa, "");

		CampaignMain.cm.toUser("PL|STN|"+ap.getTeamNumber(), Username,false);
		CampaignMain.cm.toUser("AM:You have been assigned to team #"+ap.getTeamNumber(), Username);
		jp.setTeamNumber(ap.getTeamNumber());
	}//end process
	
}//end AttackCommand	