package mekwars.server.campaign.commands.admin;

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
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }

        ChristmasHandler.getInstance().startChristmas();
        server.campaign.CampaignMain.cm.getConfig().setProperty("Christmas_ManuallyStarted", "true");

        server.campaign.CampaignMain.cm.doSendModMail("SERVER",
              "Happy Holidays! " + Username + " started the Christmas season.");
        server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers("AM: The Christmas season is upon us.  Happy Holidays!",
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
