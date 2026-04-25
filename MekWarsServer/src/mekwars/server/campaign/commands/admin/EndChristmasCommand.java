package mekwars.server.campaign.commands.admin;

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
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }
        ChristmasHandler.getInstance().endChristmas();
        server.campaign.CampaignMain.cm.doSendModMail("SERVER", Username + " ended the Christmas season.");
        server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers("AM: The Christmas season has officially ended.",
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
