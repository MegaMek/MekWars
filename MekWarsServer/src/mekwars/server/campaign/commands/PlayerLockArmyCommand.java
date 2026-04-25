package mekwars.server.campaign.commands;

public class PlayerLockArmyCommand implements Command {
    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                             userLevel +
                                                             ". Required: " +
                                                             accessLevel +
                                                             ".", Username, true);
                return;
            }
        }

        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);
        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Null Player while locking army. Report This!.", Username, true);
            return;
        }
        int aid = -1;
        try {
            aid = Integer.parseInt((String) command.nextElement());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("AM:Improper format. Try: /c playerlockarmy#ID", Username, true);
            return;
        }
        server.campaign.SArmy army = p.getArmy(aid);
        if (army == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Could not find an Army #" + aid + ".", Username, true);
            return;
        }
        army.setPlayerLock(aid, true);
        server.campaign.CampaignMain.cm.toUser("AM:Army " + aid + " locked.", Username, true);
    }
}
