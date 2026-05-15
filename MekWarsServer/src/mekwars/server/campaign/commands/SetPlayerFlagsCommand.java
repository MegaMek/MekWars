package mekwars.server.campaign.commands;

import mekwars.server.campaign.CampaignMain;

public class SetPlayerFlagsCommand implements Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    ;
    String syntax = "/c SetPlayerFlags#Player#FlagName#[true|false|toggle]...";

    public void process(java.util.StringTokenizer command, String Username) {

        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        String pName = command.nextToken();
        if (pName == null) {
            CampaignMain.campaignMain.toUser("AM: missing user name, use syntax " + getSyntax(), Username, true);
            return;
        }

        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(pName);
        if (p == null) {
            CampaignMain.campaignMain.toUser("AM: unable to load player " + pName, Username, true);
            return;
        }

        while (command.hasMoreTokens()) {
            String fName = command.nextToken();
            String value;
            if (command.hasMoreTokens()) {
                value = command.nextToken();
            } else {
                CampaignMain.campaignMain.toUser("AM: Missing value for flag " + fName, Username, true);
                return;
            }
            if (value.equalsIgnoreCase("toggle")) {
                value = Boolean.toString(!p.getFlagStatus(fName));
            }
            String userCommand = "PF|SF|" + fName + "|" + value + "|";
            p.setFlagStatus(fName, value);
            CampaignMain.campaignMain.toUser(userCommand, pName, false);
        }
        CampaignMain.campaignMain.toUser("AM: Flags set for " + pName, Username, true);
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }


}
