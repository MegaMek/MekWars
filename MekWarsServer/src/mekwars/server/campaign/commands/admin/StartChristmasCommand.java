package mekwars.server.campaign.commands.admin;

import mekwars.server.campaign.CampaignMain;
import server.campaign.util.ChristmasHandler;

/**
 * A command to start the Christmas Season
 *
 * @author Spork
 * @version 2016.10.26
 */
public class StartChristmasCommand implements server.campaign.commands.Command {

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

        ChristmasHandler.getInstance().startChristmas();
        CampaignMain.campaignMain.getConfig().setProperty("Christmas_ManuallyStarted", "true");

        CampaignMain.campaignMain.doSendModMail("SERVER",
              "Happy Holidays! " + Username + " started the Christmas season.");
        CampaignMain.campaignMain.doSendToAllOnlinePlayers("AM: The Christmas season is upon us.  Happy Holidays!",
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
