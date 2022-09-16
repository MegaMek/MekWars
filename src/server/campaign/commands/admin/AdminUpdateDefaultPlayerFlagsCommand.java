package server.campaign.commands.admin;

import java.util.StringTokenizer;

import common.flags.PlayerFlags;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;


public class AdminUpdateDefaultPlayerFlagsCommand implements Command {

	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "[D or S]#flagname[#value if action is Set]...";
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
		PlayerFlags flags = CampaignMain.cm.getDefaultPlayerFlags();
		String action;
		String flagName;
		boolean value = false;
		
		while(command.hasMoreTokens()) {
			action = command.nextToken();
			flagName = command.nextToken();
			
			if (action.equalsIgnoreCase("S")) {
				value = Boolean.parseBoolean(command.nextToken());
				if (!flags.getFlagNames().contains(flagName)) {
					int id = flags.getAvailableID();
					flags.addFlag(flagName, id, value);
					CampaignMain.cm.doSendToAllOnlinePlayers("PF|AF|" + flagName + "|" + Integer.toString(id) + "|" + Boolean.toString(value) + "|", false);
					CampaignMain.cm.toUser("Added DefaultPlayerFlag " + flagName + " with a value of " + Boolean.toString(value), Username, true);
				} else {
					flags.setFlag(flagName, value);
					CampaignMain.cm.doSendToAllOnlinePlayers("PF|SSDF|" + flagName + "|" + Boolean.toString(value) + "|", false);
					CampaignMain.cm.toUser("Setting DefaultPlayerFlag " + flagName + " to a value of " + Boolean.toString(value), Username, true);
				}
			} else if(action.equalsIgnoreCase("D")) {
				flags.clearFlag(flagName);
				CampaignMain.cm.doSendToAllOnlinePlayers("PF|DF|" + flagName + "|", false);
				CampaignMain.cm.toUser("Removed DefaultPlayerFlag " + flagName, Username, true);
			}
		}
		CampaignMain.cm.getDefaultPlayerFlags().save();
	}
		
}


