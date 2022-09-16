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
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;


/**
 * Moving the Ban command from MWServ into the normal command structure.
 *
 * Syntax  /c Ban
 */
public class BanCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Type Ban with No Arguments for Syntax";
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
		
		try {
			
			long minute = 60000;
			long hour = minute*60;
			long day = hour * 24;
			long week = day * 7;
			long month = day * 30;
			long year = day * 365;
			long howlong = year;
			String timeName = "minutes";
			
			String toKill = command.nextToken().trim();
			String banTime = "";
			if ( command.hasMoreTokens() ){
				banTime = command.nextToken();
				if (banTime.equalsIgnoreCase("perm")){
					howlong = Long.MAX_VALUE/1000;
					timeName = "permanently";
				}
				else{
					String banValue = banTime.substring(banTime.length()-1);
					if ( banValue.trim().equalsIgnoreCase("h") ){
						howlong = Long.parseLong(banTime.substring(0,banTime.length()-1))*hour;
						if ( howlong/hour == 1)
							timeName = "for 1 hour";
						else
							timeName = "for "+Long.toString(howlong/hour)+" hours";
					}
					else if ( banValue.trim().equalsIgnoreCase("d") ){
						howlong = Long.parseLong(banTime.substring(0,banTime.length()-1))*day;
						if ( howlong/day == 1)
							timeName = "for 1 day";
						else
							timeName = "for "+Long.toString(howlong/day)+" days";
					}
					else if ( banValue.trim().equalsIgnoreCase("w") ){
						howlong = Long.parseLong(banTime.substring(0,banTime.length()-1))*week;
						if ( howlong/week == 1)
							timeName = "for 1 week";
						else
							timeName = "for "+Long.toString(howlong/week)+" weeks";
						
					}
					else if ( banValue.trim().equalsIgnoreCase("m") ){
						howlong = Long.parseLong(banTime.substring(0,banTime.length()-1))*month;
						if ( howlong/month == 1)
							timeName = "for 1 month";
						else
							timeName = "for "+Long.toString(howlong/month)+" months";
					}
					else if ( banValue.trim().equalsIgnoreCase("y") ){
						howlong = Long.parseLong(banTime.substring(0,banTime.length()-1))*year;
						if ( howlong/year == 1)
							timeName = "for 1 year";
						else
							timeName = "for "+Long.toString(howlong/year)+" years";
					}else{
						howlong = Long.parseLong(banTime)*minute;
						if ( howlong/minute == 1)
							timeName = "for 1 minute";
						else
							timeName = "for "+Long.toString(howlong/minute)+" minutes";
					}
					
				}
			} else {
				howlong = year;
				timeName = "year";
			}
			
			if (CampaignMain.cm.getServer().isAdmin(toKill) && !Username.startsWith("[Dedicated]")) {
				CampaignMain.cm.toUser("AM:You may not ban an admin.", Username);
				return;
			}
			
			//howlong = Long.parseLong(str.nextToken());
			InetAddress banip = CampaignMain.cm.getServer().getIP(toKill);
			if (banip.equals(InetAddress.getLocalHost()) )
				banip = null;
			
			long until = System.currentTimeMillis() + howlong;
			CampaignMain.cm.toUser("AM:You were banned by " + Username + " " +timeName+".", toKill);
			CampaignMain.cm.toUser("PL|GBB|Banned!", toKill,false);
			
			try {
				Thread.sleep(125);
			} catch (Exception ex){}
			
			if (banip != null)
				CampaignMain.cm.getServer().getBanIps().put(banip, until);
			CampaignMain.cm.getServer().getBanAccounts().put(toKill.toLowerCase(), Long.toString(until));
			
			//CampaignMain.cm.getServer().ISPlog.put(CampaignMain.cm.getServer().myCommunicator.getClient(toKill).getClientVersion(),until);
			//retreiveISPS(until,toKill);
			CampaignMain.cm.getServer().bansUpdate();
			//MWLogger.modLog(Username + " banned " + toKill + " " +timeName+".");
			CampaignMain.cm.getServer().sendChat("AM:"+Username + " banned " + toKill + " " +timeName+".");
			
			CampaignMain.cm.getOpsManager().doDisconnectCheckOnPlayer(toKill);
			CampaignMain.cm.getServer().getCampaign().doLogoutPlayer(toKill);
			
			if (CampaignMain.cm.getServer().getClient(toKill) != null)
				CampaignMain.cm.getServer().killClient(toKill,Username);
		} catch(Exception ex) {
			CampaignMain.cm.toUser("AM:Incorrect Syntax: Syntax is as follows<br>/ban playername#time" +
					"<br>Please note the time argument can be left off and will default to 1 year" +
					"<br>also note that the time argument can be made in different ways" +
					"<br>/ban playername#10 (player is banned for 10 mins)" +
					"<br>/ban playername#10h (player is banned for 10 hours)" +
					"<br>/ban playername#10d (player is banned for 10 days)" +
					"<br>/ban playername#10w (player is banned for 10 weeks)" +
					"<br>/ban playername#10m (player is banned for 10 months)" +
					"<br>/ban playername#10y (player is banned for 10 years)" +
					"<br>/ban playername#perm (player is banned for a very long time)", Username);
		}
	}
}