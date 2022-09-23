package server.campaign.commands;

import java.util.StringTokenizer;

import server.campaign.CampaignMain;
import server.campaign.SPlayer;

public class UpdateDiscordInfoCommand implements Command 
{
	int accessLevel = 1;
	String syntax = "updatediscordinfo id";
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
		
		SPlayer player = CampaignMain.cm.getPlayer(Username);
		
		player.toSelf("AM:Discord ID currently set to: " + player.getDiscordID());
		
		String discordName = "";
		int discordIdNumber = 0;
		if (command.hasMoreTokens()) 
		{
			discordName = command.nextToken();
		}
		
		if (command.hasMoreTokens()) 
		{
			discordIdNumber = Integer.parseInt(command.nextToken());
		}
		
		if(discordIdNumber != 0) 
		{
			player.setDiscordID(discordName+"#"+discordIdNumber);
			player.toSelf("AM:Discord ID set to " + player.getDiscordID());			
		}
		else
			player.toSelf("AM:Error occured, please try again. Ex: /updatediscordinfo myname#0023");
	}
}

