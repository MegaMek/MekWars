package mekwars.server.campaign.commands;

import mekwars.server.campaign.CampaignMain;

public class SetTargetSystemCommand implements Command {

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

        server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(Username);

        if (!command.hasMoreTokens()) {

            return;
        }
        int unitID = Integer.parseInt(command.nextToken());
        if (!command.hasMoreTokens()) {

            return;
        }
        int newTargetSystem = Integer.parseInt(command.nextToken());
        server.campaign.SUnit unit = player.getUnit(unitID);
        unit.setTargetSystem(newTargetSystem);
        CampaignMain.campaignMain.toUser("PL|STS|" + unitID + "|" + newTargetSystem + "|", Username, false);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}


}
