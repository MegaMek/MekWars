package mekwars.server.campaign.commands.admin;

public class AdminRecalcHangarBvCommandMC implements server.campaign.commands.Command {
    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "/c adminrecalchangarbvmc#name";

    public String getSyntax() {return syntax;}

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

        server.campaign.SPlayer p = null;

        try {
            p = server.campaign.CampaignMain.cm.getPlayer(command.nextToken());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("Improper command. Try: /c adminrecalchangarbvmc#name",
                  Username,
                  true);
            return;
        }

        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("Couldn't find a player with that name.", Username, true);
            return;
        }


        p.setBVTracker(p.getHangarBVforMC());

        server.campaign.CampaignMain.cm.toUser("You recalculated " + p.getName() + "'s hangar bv.", Username, true);
        server.campaign.CampaignMain.cm.toUser(Username + " recalculated your hangar bv.", p.getName(), true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " recalculated hangar bv for " + p.getName());
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}

