package server.campaign.commands.admin;

import java.io.File;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlanet;
import server.campaign.commands.Command;

public class AdminRenamePlanetCommand implements Command {

	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "PlanetID#OldName#NewName";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
public void process(StringTokenizer command,String Username) {
		
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		int pID = -1;
		String oldName = "";
		String newName = "";
		
		if (command.hasMoreElements())
			pID = Integer.parseInt(command.nextToken());
		if (command.hasMoreElements())
			oldName = command.nextToken();
		if (command.hasMoreElements())
			newName = command.nextToken();
		
		if (pID == -1 || oldName.equals("") || newName.equals("")) {
			// Incorrect command format
			CampaignMain.cm.toUser("Improper command. Try: /adminrenameplanet planetID#oldName#newName", Username, true);
			return;
		}
	
		SPlanet p = CampaignMain.cm.getPlanetFromPartialString(oldName, Username);
		
		if (p == null) {
			CampaignMain.cm.toUser("Could not find a matching planet.",Username,true);
			return;
		}
		
		p.setName(newName);
		File fp = new File("./campaign/planets/" + oldName.toLowerCase().trim() + ".dat");
		if (fp.exists())
			fp.delete();
		CampaignMain.cm.savePlanetData();
		
		CampaignMain.cm.updateHousePlanetUpdate();
		CampaignMain.cm.doSendModMail("NOTE",Username + " renamed " + oldName + " to " + newName);
		
	}
	
}
