package mekwars.server.campaign.commands.admin;

import mekwars.server.campaign.CampaignMain;

public class AdminRecalcHangarBvCommandMC implements server.campaign.commands.Command {
    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "/c adminrecalchangarbvmc#name";

    public String getSyntax() {return syntax;}

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

        server.campaign.SPlayer p = null;

        try {
            p = CampaignMain.campaignMain.getPlayer(command.nextToken());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("Improper command. Try: /c adminrecalchangarbvmc#name",
                  Username,
                  true);
            return;
        }

        if (p == null) {
            CampaignMain.campaignMain.toUser("Couldn't find a player with that name.", Username, true);
            return;
        }


        p.setBVTracker(p.getHangarBVforMC());

        CampaignMain.campaignMain.toUser("You recalculated " + p.getName() + "'s hangar bv.", Username, true);
        CampaignMain.campaignMain.toUser(Username + " recalculated your hangar bv.", p.getName(), true);
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " recalculated hangar bv for " + p.getName());
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}

