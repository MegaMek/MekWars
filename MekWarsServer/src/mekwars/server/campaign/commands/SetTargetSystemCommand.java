package mekwars.server.campaign.commands;

public class SetTargetSystemCommand implements Command {

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

        server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(Username);

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
        server.campaign.CampaignMain.cm.toUser("PL|STS|" + unitID + "|" + newTargetSystem + "|", Username, false);
    }


}
