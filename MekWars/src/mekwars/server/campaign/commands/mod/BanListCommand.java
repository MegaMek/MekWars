/*
 * MekWars - Copyright (C) 2006
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

package server.campaign.commands.mod;

import java.net.InetAddress;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

/**
 * Moving the BanList command from MWServ into the normal command structure.
 * 
 * Syntax /c BanList
 */
public class BanListCommand implements Command {

	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "";
	public String getSyntax() { return syntax;}


	public int getExecutionLevel() {
		return accessLevel;
	}

	public void setExecutionLevel(int i) {
		accessLevel = i;
	}

	public void process(StringTokenizer command, String Username) {

		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if (userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser(
						"AM:Insufficient access level for command. Level: "
								+ userLevel + ". Required: " + accessLevel
								+ ".", Username, true);
				return;
			}
		}

		String result = "Banned accounts:<br>";
		int i = 1;

		ConcurrentHashMap<String, String> banHash = new ConcurrentHashMap<String, String>(
				CampaignMain.cm.getServer().getBanAccounts());
		for (String banName : banHash.keySet()) {
			String banTime = banHash.get(banName);

			// Banned Accounts Username Check
			Long until = Long.valueOf(banTime);

			// If they are no longer banned remove them from the list and don't
			// display them
			if (until.longValue() < System.currentTimeMillis()
					|| until.longValue() == 0) {
				CampaignMain.cm.getServer().getBanAccounts().remove(banName);
				CampaignMain.cm.getServer().bansUpdate();
				continue;
			}

			Long l = Long.valueOf(banTime);
			result += Integer.toString(i++);
			result += ") ";
			result += banName;
			result += " [unban at ";
			result += new Date(l).toString();
			result += "]<br>";
		}

		i = 1;
		result += "Banned IPs:<br>";
		ConcurrentHashMap<InetAddress, Long> banIpHash = new ConcurrentHashMap<InetAddress, Long>(
				CampaignMain.cm.getServer().getBanIps());

		for (InetAddress currAddress : banIpHash.keySet()) {
			Long until = CampaignMain.cm.getServer().getBanIps().get(currAddress);

			// If they are no longer banned remove them from the list and don't
			// display them
			if (until.longValue() < System.currentTimeMillis()
					|| until.longValue() == 0) {
				CampaignMain.cm.getServer().getBanIps().remove(currAddress);
				CampaignMain.cm.getServer().bansUpdate();
				continue;
			}

			result += i++ + ") " + currAddress.toString() + " [unban at "
					+ new Date(until.longValue()).toString() + "]<br>";
		}

		CampaignMain.cm.toUser(result, Username);
		CampaignMain.cm.doSendModMail("NOTE", Username
				+ " checked the ban list.");
		// MWLogger.modLog(Username + " checked the ban list.");
	}
}