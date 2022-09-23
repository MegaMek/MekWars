package server.campaign.commands.admin;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;

public class AdminRecalcHangarBvCommandMC implements Command 
{
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "/c adminrecalchangarbvmc#name";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}

	public void process(StringTokenizer command,String Username) 
	{
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}

		SPlayer p = null;

		try {
			p = CampaignMain.cm.getPlayer(command.nextToken());
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c adminrecalchangarbvmc#name", Username, true);
			return;
		}

		if(p == null) {
			CampaignMain.cm.toUser("Couldn't find a player with that name.", Username, true);
			return;
		}


		p.setBVTracker(p.getHangarBVforMC());
		
		CampaignMain.cm.toUser("You recalculated " + p.getName() + "'s hangar bv.", Username, true);
		CampaignMain.cm.toUser(Username + " recalculated your hangar bv.", p.getName(), true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " recalculated hangar bv for " + p.getName());
	}
}

