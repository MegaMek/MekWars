package mekwars.server.campaign.commands;

import mekwars.server.campaign.CampaignMain;
import server.campaign.util.ChristmasHandler;

public class IsItChristmasCommand implements Command {

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
        boolean isChristmas = ChristmasHandler.getInstance().isItChristmas();
        java.util.Date start = ChristmasHandler.getInstance().getStartDate();
        java.util.Date end = ChristmasHandler.getInstance().getEndDate();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");

        if (isChristmas) {
            CampaignMain.campaignMain.toUser("AM:Tis the season! The Christmas season this year runs from " +
                                                   sdf.format(start) +
                                                   " to " +
                                                   sdf.format(end) +
                                                   ".", Username);
        } else {
            CampaignMain.campaignMain.toUser("AM:Anxious, aren't you? The Christmas season this year runs from " +
                                                   sdf.format(start) +
                                                   " to " +
                                                   sdf.format(end) +
                                                   ".", Username);
        }
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
