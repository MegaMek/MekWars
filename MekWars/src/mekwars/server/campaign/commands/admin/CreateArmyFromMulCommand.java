/*
 * MekWars - Copyright (C) 2007
 * 
 * Original author - Jason Tighe (torren@users.sourceforge.net)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package server.campaign.commands.admin;

import java.io.File;
import java.util.StringTokenizer;
import java.util.Vector;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.commands.Command;

public class CreateArmyFromMulCommand implements Command {

	int accessLevel = IAuthenticator.ADMIN;

	String syntax = "Filename#Army Name#[Target Player]";

	public int getExecutionLevel() {
		return accessLevel;
	}

	public void setExecutionLevel(int i) {
		accessLevel = i;
	}

	public String getSyntax() {
		return syntax;
	}

	public void process(StringTokenizer command, String Username) {

		// access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if (userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser(
					"AM:Insufficient access level for command. Level: "
							+ userLevel + ". Required: " + accessLevel + ".",
					Username, true);
			return;
		}

		SPlayer p = CampaignMain.cm.getPlayer(Username);
		String filename;
		String armyname;

		try {
			filename = command.nextToken();
			armyname = command.nextToken();
			if (command.hasMoreTokens())
				p = CampaignMain.cm.getPlayer(command.nextToken());
		} catch (Exception ex) {
			CampaignMain.cm.toUser("Syntax Error: /createarmyfrommul " + syntax, Username);
			return;
		}

		if (p == null) {
			CampaignMain.cm.toUser("Unable to find target player", Username);
			return;
		}

		if (p.getArmies().size() >= CampaignMain.cm.getIntegerConfig("MaxLancesPerPlayer")) {
			CampaignMain.cm.toUser(p.getName()+ " has too many armies already!", Username);
			return;
		}

		if (!new File("./data/armies").exists()) {
			CampaignMain.cm.toUser("directory ./data/armies does not exist",Username);
			new File("./data/armies").mkdir();
			return;
		}

		Vector<SUnit> units = new Vector<SUnit>(1,1);
		units.addAll(SUnit.createMULUnits(filename));

		SArmy army = new SArmy(p.getName());

		army.setID(p.getFreeArmyId());
		army.setName(armyname);
		for ( SUnit cm : units ) {
			cm.setProducer("Mul Army Unit "+armyname);
			p.addUnit(cm, true);
			army.addUnit(cm);
		}
		p.getArmies().add(army);
		army.getBV();
		army.setOpForceSize(army.getAmountOfUnits());
		
		CampaignMain.cm.toUser("PL|SAD|" + army.toString(true, "%"), p.getName(),false);
		CampaignMain.cm.toUser("army created: " + armyname, p.getName(), true);
		CampaignMain.cm.doSendModMail("NOTE", Username+ " has created an army from file " + filename);

	}
}
