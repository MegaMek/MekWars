package server.campaign.commands;

import java.util.StringTokenizer;

import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.util.SPlayerToJSON;

public class SPlayerToJsonCommand implements Command 
{

	int accessLevel = 1;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}

	public void process(StringTokenizer command,String Username) 
	{
		//access level checks
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		
		if(userLevel < getExecutionLevel()) 
		{
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}

		if(!Boolean.parseBoolean(CampaignMain.cm.getConfig("Enable_BotPlayerInfo"))) 
		{
			CampaignMain.cm.toUser("AM:This command is disabled on this server.",Username,true);
			return;
		}

		SPlayer p = CampaignMain.cm.getPlayer(Username);
		SPlayerToJSON.writeToFile(p);
		p.toSelf("AM: JSON player data updated.");
	}
}
