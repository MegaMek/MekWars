package mekwars.server.campaign.commands.admin;

import common.flags.PlayerFlags;
import mekwars.server.campaign.CampaignMain;


public class AdminUpdateDefaultPlayerFlagsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "[D or S]#flagname[#value if action is Set]...";

    public String getSyntax() {return syntax;}

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
        PlayerFlags flags = CampaignMain.campaignMain.getDefaultPlayerFlags();
        String action;
        String flagName;
        boolean value = false;

        while (command.hasMoreTokens()) {
            action = command.nextToken();
            flagName = command.nextToken();

            if (action.equalsIgnoreCase("S")) {
                value = Boolean.parseBoolean(command.nextToken());
                if (!flags.getFlagNames().contains(flagName)) {
                    int id = flags.getAvailableID();
                    flags.addFlag(flagName, id, value);
                    CampaignMain.campaignMain.doSendToAllOnlinePlayers("PF|AF|" +
                                                                             flagName +
                                                                             "|" +
                                                                             Integer.toString(id) +
                                                                             "|" +
                                                                             Boolean.toString(value) +
                                                                             "|", false);
                    CampaignMain.campaignMain.toUser("Added DefaultPlayerFlag " +
                                                           flagName +
                                                           " with a value of " +
                                                           Boolean.toString(value), Username, true);
                } else {
                    flags.setFlag(flagName, value);
                    CampaignMain.campaignMain.doSendToAllOnlinePlayers("PF|SSDF|" +
                                                                             flagName +
                                                                             "|" +
                                                                             Boolean.toString(value) +
                                                                             "|", false);
                    CampaignMain.campaignMain.toUser("Setting DefaultPlayerFlag " +
                                                           flagName +
                                                           " to a value of " +
                                                           Boolean.toString(value), Username, true);
                }
            } else if (action.equalsIgnoreCase("D")) {
                flags.clearFlag(flagName);
                CampaignMain.campaignMain.doSendToAllOnlinePlayers("PF|DF|" + flagName + "|", false);
                CampaignMain.campaignMain.toUser("Removed DefaultPlayerFlag " + flagName, Username, true);
            }
        }
        CampaignMain.campaignMain.getDefaultPlayerFlags().save();
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

}


