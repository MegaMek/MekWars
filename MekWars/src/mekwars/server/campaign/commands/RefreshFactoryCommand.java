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

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.SUnitFactory;

//refreshfactory#planet#factory#useflu(true/false)
public class RefreshFactoryCommand implements Command {

	int accessLevel = IAuthenticator.GUEST;
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

		SPlayer player = CampaignMain.cm.getPlayer(Username);
		String planetName;
		String factoryName;
		Boolean useFlu = false; //@salient

		try
		{
			planetName = command.nextToken();
			factoryName = command.nextToken();
			if(command.hasMoreTokens())
				useFlu = Boolean.parseBoolean(command.nextToken());
		}
		catch (Exception e)
		{
			CampaignMain.cm.toUser("AM:Improper format. Try: /c refreshfactory#planetname#factoryname#useflu(true/false)",Username,true);
			return;
		}

		if (!CampaignMain.cm.getBooleanConfig("AllowFactoryRefreshForRewards") && !useFlu)
		{
			CampaignMain.cm.toUser("AM:You may not use " + CampaignMain.cm.getConfig("RPShortName") + " to refresh a factory on this server.",Username,true);
			return;
		}

		if (CampaignMain.cm.getIntegerConfig("FluToRefreshFactory") == 0 && useFlu)
		{
			CampaignMain.cm.toUser("AM:You may not use " + CampaignMain.cm.getConfig("FluShortName") + " to refresh a factory on this server.",Username,true);
			return;
		}

		SPlanet p = (SPlanet)CampaignMain.cm.getData().getPlanetByName(planetName);
		if (p == null)
		{
			CampaignMain.cm.toUser("AM:Could not find planet: " + planetName + ".",Username,true);
			return;
		}

		SUnitFactory uf = (SUnitFactory)CampaignMain.cm.getData().getFactoryByName(p,factoryName);
		if (uf == null)
		{
			CampaignMain.cm.toUser("AM:Could not find factory: " + factoryName + ".",Username,true);
			return;
		}

		int rpCost = CampaignMain.cm.getIntegerConfig("RewardPointToRefreshFactory");
		int fluCost = CampaignMain.cm.getIntegerConfig("FluToRefreshFactory");
		int playerRP = player.getReward();
		int playerFlu = player.getInfluence();

		if (playerRP < rpCost && !useFlu)
		{
			CampaignMain.cm.toUser(rpCost + " " + CampaignMain.cm.getConfig("RPLongName") + " required to refresh "+ uf.getName()+ ". You only have " + playerRP + ".",Username,true);
			return;
		}

		if (playerFlu < fluCost && useFlu)
		{
			CampaignMain.cm.toUser(fluCost + " " + CampaignMain.cm.getConfig("FluLongName") + " required to refresh "+ uf.getName()+ ". You only have " + playerFlu + ".",Username,true);
			return;
		}

		if(!useFlu)
			player.addReward(-rpCost);
		else
			player.addInfluence(-fluCost);

		int ticksToRemove = uf.getTicksUntilRefresh();
		String refresh = uf.addRefresh(-ticksToRemove, true);//use get and add instead of set b/c add sends HS update

		CampaignMain.cm.doSendToAllOnlinePlayers(player.getMyHouse(), "HS|" + refresh, false);

		CampaignMain.cm.toUser("AM:You refreshed "+ uf.getName()+" on planet "+p.getName(),Username,true);
		CampaignMain.cm.doSendHouseMail(player.getMyHouse(), "NOTE",player.getName()+" refreshed "+ uf.getName()+" on planet "+p.getName());
	}
}
