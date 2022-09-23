package server.campaign.commands;

import java.util.StringTokenizer;

import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;

public class SetTargetSystemCommand implements Command {

	int accessLevel = 0;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command, String Username) {
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
		SPlayer player = CampaignMain.cm.getPlayer(Username);
        
		if (! command.hasMoreTokens()) {
			
			return;
		} 
		int unitID = Integer.parseInt(command.nextToken());
		if (!command.hasMoreTokens()) {
			
			return;
		}
		int newTargetSystem = Integer.parseInt(command.nextToken());
		SUnit unit = player.getUnit(unitID);
		unit.setTargetSystem(newTargetSystem);
		CampaignMain.cm.toUser("PL|STS|" + unitID + "|" + newTargetSystem + "|", Username, false);
	}
	
	

}
