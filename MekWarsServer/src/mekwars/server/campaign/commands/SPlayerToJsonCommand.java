package mekwars.server.campaign.commands;

import server.util.SPlayerToJSON;

public class SPlayerToJsonCommand implements Command {

    int accessLevel = 1;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {
        //access level checks
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);

        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }

        if (!Boolean.parseBoolean(server.campaign.CampaignMain.cm.getConfig("Enable_BotPlayerInfo"))) {
            server.campaign.CampaignMain.cm.toUser("AM:This command is disabled on this server.", Username, true);
            return;
        }

        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);
        SPlayerToJSON.writeToFile(p);
        p.toSelf("AM: JSON player data updated.");
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
