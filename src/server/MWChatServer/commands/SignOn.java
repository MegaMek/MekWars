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

/*
 * Derived from NFCChat, a GPL chat client/server. 
 * Original code can be found @ http://nfcchat.sourceforge.net
 * Our thanks to the original authors.
 */ 
package server.MWChatServer.commands;

import common.util.MWLogger;
import common.util.StringUtils;
import server.ServerWrapper;
import server.MWChatServer.MWChatClient;
import server.MWChatServer.Translator;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;

/**
 * @author  Steve Hawkins
 */
//slightly different syntax than stock nfc chat, but still compatable
//sigon name password [protocol_version [chat_color]]

public class SignOn extends CommandBase implements ICommands {
	
	private int nobody_id = 0;
	
	public boolean process(MWChatClient client, String[] args) {
		args[1] = args[1].trim();
		
		if(args[1].length() < 2) { // Vertigo 20040208: single-char accounts gave problems
			client.generalError("No single-char names allowed. Go away...");
			return false;
		}
		
		args[2] = args[2].trim();
		if (args.length < 2) {
			client.generalError(getUsage(args[0]));
			return false;
		}
		
		if (client.getUserId() != null) {
			client.generalError(Translator.getMessage("already_on"));
			return false;
		}
		
		client.setUserId(args[1]);
		String username = client.getUserId(); 
		String ProtocolVersion = "4";
		String UserColor = "black";
		
		//look for completely forbidden names.
		String lowername = username.trim().toLowerCase();

		if (lowername.equals("nobody")
				|| lowername.equals("admin")
				|| lowername.equals("[dedicated]")
				|| lowername.equals("dedicated")
				|| lowername.equals("server")
				|| lowername.equals("mod")
				|| lowername.equals("moderator")) {
			client.generalError("nobody, admin, administrator, dedicated, [dedicated], mod, moderator, and server are reserved names.");
			return false;
		}
		
		if ( StringUtils.hasBadChars(lowername).trim().length() > 0){
		    client.generalError(StringUtils.hasBadChars(lowername));
		    return false;
		}
		
		try {
			
			if (args.length < 3){
				if ( !client.getServer().signOn(client, null) ){
				    client.getServer().kill(client.getUserId(), "Bye Bye");
					return false;
				}
			//megamek.net stuff
			}else {
				ProtocolVersion = args[3];
				client.setClientVersion(ProtocolVersion);
				if (args.length > 4) UserColor = args[4];
			}
			
			if ( !client.getServer().signOn(client, args[2]) ){
                client.getServer().kill(client.getUserId(), "Bye Bye");
				return false;
			}
			server.MWServ server = ((ServerWrapper)client.getServer()).getMWServ();
			server.getUser(username).setColor(UserColor);
			server.sendNewUserToAll(username, true);
			
			//hacky and evil! @urgru 2.27.05
			if (username.startsWith("Nobody")) {
				CampaignMain.cm.toUser("<font color=\"navy\"><br>---<br>It appears that you've misentered your password or tried to sign on using a name that is already taken.<br>---<br></font>", username, true);
			} else if (username.startsWith("[Dedicated]")) {
                CampaignMain.cm.toUser("SSC|UseAdvanceRepair|"+CampaignMain.cm.getConfig("UseAdvanceRepair")+"|UseSimpleRepair|"+CampaignMain.cm.getConfig("UseSimpleRepair")+"|AllowedMegaMekVersion|"+CampaignMain.cm.getConfig("AllowedMegaMekVersion")+"|MMTimeStampLogFile|"+CampaignMain.cm.getConfig("MMTimeStampLogFile"), client.getUserId(), false);
			} else {
				CampaignMain.cm.doLoginPlayer(username);
				CampaignMain.cm.getOpsManager().doReconnectCheckOnPlayer(username);
				if (client.getAccessLevel() < IAuthenticator.REGISTERED)
					CampaignMain.cm.toUser("<font color=\"navy\"><br>---<br>Warning: Your account is not password protected. [<a href=\"MWREG\">Click to register</a>]<br>---<br></font>", username, true);
			}// else { 
				//CampaignMain.cm.toUser("<font color=\"navy\"><br>---<br>It appears that you have not signed up for this server's campaign. [<a href=\"MEKWARS/c enroll\">Click to get started</a>]<br>---<br></font>", username, true);
		//	}
			
			return true;
			
		} catch (NullPointerException NPE) {
			MWLogger.errLog("Sign On Error");
			MWLogger.errLog(NPE);
		} catch (Exception e) {//even though access is denied, find an acceptable nobody
			
			if (e.getMessage() == null) {
				MWLogger.errLog("Sign On Error: Null exception message");
				MWLogger.errLog(e);
			}
			
			else if (e.getMessage().equals(ACCESS_DENIED)){
				//client.setUserId(null);
				client.error(ACCESS_DENIED, e.getMessage());
				String key;
				do {
					nobody_id++;
					key = "Nobody (" + nobody_id + ")";
				} while (client.getServer().userExists(key));
				args[1] = key;
				return this.process(client, args);
			}
			else{
				MWLogger.errLog(e);
			}
			String userId = client.getUserId();
			//client.setUserId(null);
			client.signOnError(e.getMessage(), userId);
		}
		
		return false;
	}
	
}