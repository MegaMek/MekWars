package mekwars.server.campaign.commands.admin;

import mekwars.server.campaign.CampaignMain;
import server.campaign.util.ChristmasHandler;

/**
 * Ends the Christmas season
 * <p>
 * Ends the Christmas season, doing some cleanup, including deleting the list of units handed out
 *
 * @author Spork
 * @version 2016.10.26
 */
public class EndChristmasCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

    @Override
    public void process(java.util.StringTokenizer command, String Username) {
        //access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }
        ChristmasHandler.getInstance().endChristmas();
        CampaignMain.campaignMain.doSendModMail("SERVER", Username + " ended the Christmas season.");
        CampaignMain.campaignMain.doSendToAllOnlinePlayers("AM: The Christmas season has officially ended.",
              true);
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
