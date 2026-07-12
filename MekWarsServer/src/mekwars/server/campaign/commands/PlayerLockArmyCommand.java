package mekwars.server.campaign.commands;

import mekwars.server.campaign.CampaignMain;

public class PlayerLockArmyCommand implements Command {
    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);
        if (p == null) {
            CampaignMain.campaignMain.toUser("AM:Null Player while locking army. Report This!.", Username, true);
            return;
        }
        int aid = -1;
        try {
            aid = Integer.parseInt((String) command.nextElement());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper format. Try: /c playerlockarmy#ID", Username, true);
            return;
        }
        server.campaign.SArmy army = p.getArmy(aid);
        if (army == null) {
            CampaignMain.campaignMain.toUser("AM:Could not find an Army #" + aid + ".", Username, true);
            return;
        }
        army.setPlayerLock(aid, true);
        CampaignMain.campaignMain.toUser("AM:Army " + aid + " locked.", Username, true);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
