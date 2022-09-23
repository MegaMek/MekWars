package server.campaign.commands;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.util.ChristmasHandler;

public class IsItChristmasCommand implements Command {

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
		boolean isChristmas = ChristmasHandler.getInstance().isItChristmas();
		Date start = ChristmasHandler.getInstance().getStartDate();
		Date end = ChristmasHandler.getInstance().getEndDate();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		if (isChristmas) {
			CampaignMain.cm.toUser("AM:Tis the season! The Christmas season this year runs from " + sdf.format(start) + " to " + sdf.format(end) + ".", Username);
		} else {
			CampaignMain.cm.toUser("AM:Anxious, aren't you? The Christmas season this year runs from " + sdf.format(start) + " to " + sdf.format(end) + ".", Username);
		}
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
