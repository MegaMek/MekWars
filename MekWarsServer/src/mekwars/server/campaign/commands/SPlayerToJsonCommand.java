package mekwars.server.campaign.commands;

import mekwars.server.campaign.CampaignMain;
import server.util.SPlayerToJSON;

public class SPlayerToJsonCommand implements Command {

    int accessLevel = 1;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {
        //access level checks
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);

        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        if (!Boolean.parseBoolean(CampaignMain.campaignMain.getConfig("Enable_BotPlayerInfo"))) {
            CampaignMain.campaignMain.toUser("AM:This command is disabled on this server.", Username, true);
            return;
        }

        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);
        SPlayerToJSON.writeToFile(p);
        p.toSelf("AM: JSON player data updated.");
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
