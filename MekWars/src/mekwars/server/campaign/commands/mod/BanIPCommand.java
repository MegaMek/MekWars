/*
 * MekWars - Copyright (C) 2006
 * 
 * Original author - nmorris (urgru@users.sourceforge.net)
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
 * Ban a specific IP for a period of time. Banning by name while a player
 * is online automatically bans the IP he is connected from, but if he is
 * banned while offline an explicit banip must be used because /c ban detects
 * the player's IP as "localhost."
 * 
 * Note that banip will NOT disconnect any player's currently online from the
 * targetted address. Always use /c ban for online players.
 *
 * Syntax  /c banip#address#duration
 */
public class BanIPCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Type banip with no arguments for syntax";
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
		
		long minute = 60000;
		long hour = minute*60;
		long day = hour * 24;
		long week = day * 7;
		long month = day * 30;
		long year = day * 365;
		long howlong = year;
		
		String timeName = "minutes";
		String toKill = "";
		String banTime = "";
		
		//make sure that the an acceptably formatted ip was sent
		InetAddress ip = null;
		try {
			
			toKill = command.nextToken().trim();//ip to ban
			ip = InetAddress.getByName(toKill);
			if (ip.equals(InetAddress.getLocalHost())) {
				CampaignMain.cm.toUser("AM:You may not ban the localhost.", Username);
				return;
			}
			
			//check length of the ban. time must be given.
			banTime = command.nextToken();
			if (banTime.equalsIgnoreCase("perm")){
				howlong = Long.MAX_VALUE/1000;
				timeName = "permanently";
			}
			
			else {
				
				String banValue = banTime.substring(banTime.length()-1);
				
				if ( banValue.trim().equalsIgnoreCase("h") ){
					howlong = Long.parseLong(banTime.substring(0,banTime.length()-1))*hour;
					if ( howlong/hour == 1)
						timeName = "for 1 hour";
					else
						timeName = "for "+Long.toString(howlong/hour)+" hours";
				} else if ( banValue.trim().equalsIgnoreCase("d") ){
					howlong = Long.parseLong(banTime.substring(0,banTime.length()-1))*day;
					if ( howlong/day == 1)
						timeName = "for 1 day";
					else
						timeName = "for "+Long.toString(howlong/day)+" days";
				} else if ( banValue.trim().equalsIgnoreCase("w") ){
					howlong = Long.parseLong(banTime.substring(0,banTime.length()-1))*week;
					if ( howlong/week == 1)
						timeName = "for 1 week";
					else
						timeName = "for "+Long.toString(howlong/week)+" weeks";	
				} else if ( banValue.trim().equalsIgnoreCase("m") ){
					howlong = Long.parseLong(banTime.substring(0,banTime.length()-1))*month;
					if ( howlong/month == 1)
						timeName = "for 1 month";
					else
						timeName = "for "+Long.toString(howlong/month)+" months";
				} else if ( banValue.trim().equalsIgnoreCase("y") ){
					howlong = Long.parseLong(banTime.substring(0,banTime.length()-1))*year;
					if ( howlong/year == 1)
						timeName = "for 1 year";
					else
						timeName = "for "+Long.toString(howlong/year)+" years";
				} else {
					howlong = Long.parseLong(banTime)*minute;
					if ( howlong/minute == 1)
						timeName = "for 1 minute";
					else
						timeName = "for "+Long.toString(howlong/minute)+" minutes";
				}	
			}//end else(not perm)
			
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Incorrect Syntax: Syntax is as follows<br>/c banip ip#time" +
					"<br>Please note the time argument can be left off and will default to 1 year" +
					"<br>also note that the time argument can be made in different ways" +
					"<br>/c banip X.X.X.X#10 (address is banned for 10 mins)" +
					"<br>/c banip X.X.X.X#10h (address is banned for 10 hours)" +
					"<br>/c banip X.X.X.X#10d (address is banned for 10 days)" +
					"<br>/c banip X.X.X.X#10w (address is banned for 10 weeks)" +
					"<br>/c banip X.X.X.X#10m (address is banned for 10 months)" +
					"<br>/c banip X.X.X.X#10y (address is banned for 10 years)" +
					"<br>/c banip X.X.X.X#perm (address is banned for a very long time)", Username);
		}
		
		long until = System.currentTimeMillis() + howlong;
		if (ip != null)
			CampaignMain.cm.getServer().getBanIps().put(ip, until);

		CampaignMain.cm.getServer().bansUpdate();
		//MWLogger.modLog(Username + " banned " + toKill + " " +timeName+".");
		CampaignMain.cm.getServer().sendChat(Username + " banned " + toKill + " " +timeName+".");
	}
	
}//end banipcommand.java