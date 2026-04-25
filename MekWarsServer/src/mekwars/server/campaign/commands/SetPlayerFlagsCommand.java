package mekwars.server.campaign.commands;

public class SetPlayerFlagsCommand implements Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    ;
    String syntax = "/c SetPlayerFlags#Player#FlagName#[true|false|toggle]...";

    public int getExecutionLevel() {
        return accessLevel;
    }

    public String getSyntax() {
        return syntax;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public void process(java.util.StringTokenizer command, String Username) {

        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }

        String pName = command.nextToken();
        if (pName == null) {
            server.campaign.CampaignMain.cm.toUser("AM: missing user name, use syntax " + getSyntax(), Username, true);
            return;
        }

        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(pName);
        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("AM: unable to load player " + pName, Username, true);
            return;
        }

        while (command.hasMoreTokens()) {
            String fName = command.nextToken();
            String value;
            if (command.hasMoreTokens()) {
                value = command.nextToken();
            } else {
                server.campaign.CampaignMain.cm.toUser("AM: Missing value for flag " + fName, Username, true);
                return;
            }
            if (value.equalsIgnoreCase("toggle")) {
                value = Boolean.toString(!p.getFlagStatus(fName));
            }
            String userCommand = "PF|SF|" + fName + "|" + value + "|";
            p.setFlagStatus(fName, value);
            server.campaign.CampaignMain.cm.toUser(userCommand, pName, false);
        }
        server.campaign.CampaignMain.cm.toUser("AM: Flags set for " + pName, Username, true);
    }


}
