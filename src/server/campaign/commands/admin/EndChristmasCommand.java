package server.campaign.commands.admin;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;
import server.campaign.util.ChristmasHandler;
/**
 * Ends the Christmas season
 * <p>
 * Ends the Christmas season, doing some cleanup, including deleting the list of 
 * units handed out
 * 
 * @author Spork
 * @version 2016.10.26
 */
public class EndChristmasCommand implements Command {

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
		ChristmasHandler.getInstance().endChristmas();
		CampaignMain.cm.doSendModMail("SERVER", Username + " ended the Christmas season.");
		CampaignMain.cm.doSendToAllOnlinePlayers("AM: The Christmas season has officially ended.", true);
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
