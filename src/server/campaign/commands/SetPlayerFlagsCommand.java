package server.campaign.commands;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;

public class SetPlayerFlagsCommand implements Command {

	int accessLevel = IAuthenticator.MODERATOR;;
	String syntax = "/c SetPlayerFlags#Player#FlagName#[true|false|toggle]...";
	
	public int getExecutionLevel() {
		return accessLevel;
	}

	public String getSyntax() {
		return syntax;
	}

	public void setExecutionLevel(int i) {
		accessLevel = i;
	}
	
	public void process(StringTokenizer command, String Username) {

		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		String pName = command.nextToken();
		if (pName == null) {
			CampaignMain.cm.toUser("AM: missing user name, use syntax " + getSyntax(), Username, true);
			return;
		}
		
		SPlayer p = CampaignMain.cm.getPlayer(pName);
		if (p == null) {
			CampaignMain.cm.toUser("AM: unable to load player " + pName, Username, true);
			return;
		}
		
		while(command.hasMoreTokens()) {
			String fName = command.nextToken();
			String value;
			if (command.hasMoreTokens()) {
				value = command.nextToken();
			} else {
				CampaignMain.cm.toUser("AM: Missing value for flag " + fName, Username, true);
				return;
			}
			if (value.equalsIgnoreCase("toggle")) {
				value = Boolean.toString(!p.getFlagStatus(fName));
			}
			String userCommand = "PF|SF|" + fName + "|" + value + "|";
			p.setFlagStatus(fName, value);
			CampaignMain.cm.toUser(userCommand, pName, false);
		}
		CampaignMain.cm.toUser("AM: Flags set for " + pName, Username, true);
	}


}
