package server.campaign.commands.admin;

import java.util.StringTokenizer;

import common.Unit;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;

/**
 * @author Torren (Jason Tighe)
 */
public class RemovePilotCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Player Name#Type/ALL#weight/ALL#Position[Not used if ALL is selected]";
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
		
        String player = "";
        String type = "";
        String weight = "";
        String position = "";

        try{
            player = command.nextToken();
            type = command.nextToken();
            weight = command.nextToken();
            position = command.nextToken();
        }catch (Exception ex){
            CampaignMain.cm.toUser("Syntanx RemovePilot#Player#Type/ALL#weight/ALL#Position[Not used if ALL is selected].",Username);
            return;
        }
        SPlayer p =  CampaignMain.cm.getPlayer(player);
        
        if ( type.equalsIgnoreCase("all") ){
           if ( weight.equalsIgnoreCase("all") ){
                   p.getPersonalPilotQueue().flushQueue();
           }
           else{
               p.getPersonalPilotQueue().getPilotQueue(Unit.MEK,Unit.getWeightIDForName(weight)).clear();
               p.getPersonalPilotQueue().getPilotQueue(Unit.PROTOMEK,Unit.getWeightIDForName(weight)).clear();
               p.getPersonalPilotQueue().getPilotQueue(Unit.AERO,Unit.getWeightIDForName(weight)).clear();
           }
        }else if (weight.equalsIgnoreCase("all") ){
            for ( int weightClass = 0; weightClass <= Unit.ASSAULT; weightClass++ ){
                p.getPersonalPilotQueue().getPilotQueue(Unit.getTypeIDForName(type),weightClass).clear();
            }
        }//Ok so lets try a position
        else{
            if ( position.equalsIgnoreCase("all") ){
                p.getPersonalPilotQueue().getPilotQueue(Unit.getTypeIDForName(type),Unit.getWeightIDForName(weight)).clear();
            }
            else if ( position.indexOf("-") > 0){
                int end = Integer.parseInt(position.substring(0,position.indexOf("-")));
                int start = Integer.parseInt(position.substring(position.indexOf("-")+1));
                //search backwards through the queue so you stay ahead of the shrinkinage.
                for (int pos = start ;pos >= end; pos--){
                    p.getPersonalPilotQueue().getPilot(Unit.getTypeIDForName(type),Unit.getWeightIDForName(weight),pos);
                }
            }
            else {
                p.getPersonalPilotQueue().getPilot(Unit.getTypeIDForName(type),Unit.getWeightIDForName(weight),Integer.parseInt(position));
            }
        }

        CampaignMain.cm.toUser("PL|PPQ|"+p.getPersonalPilotQueue().toString(true),player,false);
		CampaignMain.cm.doSendModMail("NOTE",Username+" has removed pilots from "+player+"'s PPQ");
        CampaignMain.cm.toUser(Username+" has removed pilots from your PPQ",player);

		
	}
}

