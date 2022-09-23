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

import java.io.File;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.StringTokenizer;

import common.UnitFactory;
import common.util.MWLogger;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SPlayer;


public class UnenrollCommand implements Command {
	
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
		
		if (Username.startsWith("Nobody")) {
			CampaignMain.cm.toUser("AM:Nobodies can't enroll, hence they can't unenroll. Nice try though.", Username, true);
			return;
		}
		
		//load the player
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		if (p == null) {
			CampaignMain.cm.toUser("AM:Couldn't find your player to unenroll. Contact an admin immediately.", Username, true);
			return;
		}
		
		//check for confirmation
		if (!command.hasMoreTokens()) {
			CampaignMain.cm.toUser("AM:You didn't confirm the Unenroll command. Enter /c unenroll#confirm if you're absolutely sure you want to quit." , Username, true);
			return;
		}
		
		String confirmString = command.nextToken();
		if (!confirmString.equalsIgnoreCase("confirm")) {
			CampaignMain.cm.toUser("AM:You didn't confirm the Unenroll Command. Enter /c unenroll#confirm if you're absolutely sure you want to quit.", Username, true);
			return;
		}
		
		if (CampaignMain.cm.getOpsManager().getShortOpForPlayer(p) != null
				|| p.getDutyStatus() == SPlayer.STATUS_FIGHTING) {
			CampaignMain.cm.toUser("AM:You cannot unenroll while in a game.", Username, true);
			return;
		}
		
		if (p.getExperience() == 0) {
			CampaignMain.cm.toUser("AM:You cannot unenroll with 0 XP. Ask an admin or mod to remove your account.", Username, true);
			return;
		}
		
		if (CampaignMain.cm.getMarket().hasActiveListings(p)) {
			CampaignMain.cm.toUser("AM:You cannot unenroll while you have units on the Market. Recall them and try again.", Username, true);
			return;
		}
		
		if (p.hasRepairingUnits(false)) {
			CampaignMain.cm.toUser("AM:You cannot unenroll while repairing units. Cancel the repairs and try again.", Username, true);
			return;
		}
		
		SHouse hisfaction = CampaignMain.cm.getHouseForPlayer(Username);
		if (hisfaction == null) {
			CampaignMain.cm.toUser("AM:Couldn't find faction to unenroll. Contact an admin immediately.", Username, true);
			return;
		}
		
		//checks passed. do the actual removal.
		hisfaction.removePlayer(p, CampaignMain.cm.getBooleanConfig("DonateUnitsUponUnenrollment"));

		
		//tell the user
		CampaignMain.cm.toUser("AM:You've been unenrolled.", Username, true);
        removeFaction(hisfaction);
		
		//delete the player's saved info, if a pfile exists
		File fp = new File("./campaign/players/" + p.getName().toLowerCase() + ".dat");
		if (fp.exists())
			fp.delete();

		//tell the mods and add to iplog.0
		InetAddress ip = CampaignMain.cm.getServer().getIP(Username);
		//MWLogger.modLog(Username + " unenrolled from the campaign (IP: " + ip + ").");
		MWLogger.ipLog("UNENROLL: " + Username + " IP: " + ip);
		CampaignMain.cm.doSendModMail("NOTE",Username + " unenrolled from the campaign (IP: " + ip + ").");
	}//end process
	
	private void removeFaction(SHouse faction) {
	    
	    if ( !CampaignMain.cm.getBooleanConfig("AllowSinglePlayerFactions") )
	        return;
	    
	    Enumeration<SPlanet> planets = faction.getPlanets().elements();
	    while ( planets.hasMoreElements() ) {
	        SPlanet planet = planets.nextElement();
	        planet.doGainInfluence(CampaignMain.cm.getHouseById(-1), faction, Integer.MAX_VALUE, true);
	        Enumeration<UnitFactory> factories = planet.getUnitFactories().elements();
	        while ( factories.hasMoreElements() ) {
	            UnitFactory factory = factories.nextElement();
	            if ( factory.getFounder().equalsIgnoreCase(faction.getName()) )
	                planet.getUnitFactories().removeElement(factory);
	        }
	        planet.setBaysProvided(0);
	        planet.updated();
	        planet.updateInfluences();
	    }
	    CampaignMain.cm.getData().removeHouse(faction.getId());
	    CampaignMain.cm.updateHousePlanetUpdate();
	    CampaignMain.cm.doSendToAllOnlinePlayers("PL|RPF|"+faction.getId(), false);
	    
	}
}//end UnenrollCommand