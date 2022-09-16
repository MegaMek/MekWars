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

import java.util.Collections;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import common.Planet;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlanet;
//import server.campaign.operations.ShortOperation;
import server.campaign.util.PlanetNameComparator;

//BarukKahzad 20151129 with much copy and paste from HouseCommand.java
public class FindContestedPlanetsCommand implements Command {
	
	int accessLevel = 0;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		String sret = "";
		String Name1="";
		String Name2="";
		SHouse h1 = null;
		SHouse h2 = null;
		int PercentAmount = -1;

		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
		try {
			sret = "AM:Could not find attacking faction.<br>...Try /FindCP#AttackingHouse#DefendingHouse#%Owned";
			Name1 = (String)command.nextToken();
    		h1 = (SHouse) CampaignMain.cm.getData().getHouseByName(Name1);
    		if (h1 == null || h1.getId() < 0) {
    			CampaignMain.cm.toUser(sret ,Username,true);
    			return;
    		}
			sret = "AM:Could not find defending faction.<br>...Try /FindCP#AttackingHouse#DefendingHouse#%Owned";
			Name2 = (String)command.nextToken();
    		h2 = (SHouse) CampaignMain.cm.getData().getHouseByName(Name2);
    		if (h2 == null || h2.getId() < 0) {
    			CampaignMain.cm.toUser(sret ,Username,true);
    			return;
    		}
			sret = "AM:Could not resolve %Owned to a postive integer.<br>...Try /FindCP#AttackingHouse#DefendingHouse#%Owned";
			PercentAmount = Integer.parseInt(command.nextToken());
			if (PercentAmount<= 0) {
				CampaignMain.cm.toUser(sret,Username,true);
				return;
			}
			 
			//cleared breaks. start assembling a status return.
			String s = "<br><b><u>Contested Planet list with minimum " + PercentAmount + "% owned for " + h1.getColoredName() + " attacking " + h2.getColoredName() + ":</u></b><br>";
		
			//sort out planets owned/fighting on, etc.
			Vector<SPlanet> contestedWorlds = new Vector<SPlanet>(1,1);

			Iterator<Planet> it = CampaignMain.cm.getData().getAllPlanets().iterator();
			while (it.hasNext()) {
				SPlanet p = (SPlanet)it.next();
				if (p.getInfluence().getInfluence(h1.getId())>= 0.01*PercentAmount*p.getConquestPoints() && p.getInfluence().getInfluence(h2.getId())>0) {
					//update lists
					contestedWorlds.add(p);
				}
			}
		//sort planets by alpha, instead of ID
		Collections.sort(contestedWorlds, new PlanetNameComparator());
		Iterator<SPlanet> i = contestedWorlds.iterator();
		if (!i.hasNext())
			s += " none<br>";
		
		while (i.hasNext()) {
			SPlanet currPlanet = (SPlanet)i.next();
			s += currPlanet.getNameAsColoredLink();
			if (currPlanet.getFactoryCount() > 0)
				s += "*"; 
			s +="(";
			//show Max cp
    		s += currPlanet.getConquestPoints() + ", ";
			//show Attacker and Defender % owned
			s += h1.getColoredAbbreviation(false) + " " + currPlanet.getInfluence().getInfluence(h1.getId()) + "cp, " + h2.getColoredAbbreviation(false) + " " + currPlanet.getInfluence().getInfluence(h2.getId()) + "cp)";
     		s += "<br>";
		}
		CampaignMain.cm.toUser("SM|" + s,Username,false);
	} catch (Exception e) {
		CampaignMain.cm.toUser(sret ,Username,true);
		return;
	}
	}
}