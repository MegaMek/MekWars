package server.campaign.commands.admin;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;
import server.campaign.util.ChristmasHandler;

/**
 * A command to start the Christmas Season
 * 
 * @author Spork
 * @version 2016.10.26
 */
public class StartChristmasCommand implements Command {

	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "";
	
	@Override
	public void process(StringTokenizer command, String Username) {
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		ChristmasHandler.getInstance().startChristmas();
		CampaignMain.cm.getConfig().setProperty("Christmas_ManuallyStarted", "true");
		
		CampaignMain.cm.doSendModMail("SERVER", "Happy Holidays! " + Username + " started the Christmas season.");
		CampaignMain.cm.doSendToAllOnlinePlayers("AM: The Christmas season is upon us.  Happy Holidays!", true);
	}

	@Override
	public int getExecutionLevel() {
		return accessLevel;
	}

	@Override
	public void setExecutionLevel(int i) {
		accessLevel = i;
	}

	@Override
	public String getSyntax() {
		return syntax;
	}

}
