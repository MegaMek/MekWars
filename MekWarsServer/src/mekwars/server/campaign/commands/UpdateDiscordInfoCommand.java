package mekwars.server.campaign.commands;

import mekwars.server.campaign.CampaignMain;

public class UpdateDiscordInfoCommand implements Command {
    int accessLevel = 1;
    String syntax = "updatediscordinfo id";

    public void process(java.util.StringTokenizer command, String Username) {
        //access level checks
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);

        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        if (!Boolean.parseBoolean(CampaignMain.campaignMain.getConfig("Enable_BotPlayerInfo"))) {
            CampaignMain.campaignMain.toUser("AM:This command is disabled on this server.", Username, true);
            return;
        }

        server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(Username);

        player.toSelf("AM:Discord ID currently set to: " + player.getDiscordID());

        String discordName = "";
        int discordIdNumber = 0;
        if (command.hasMoreTokens()) {
            discordName = command.nextToken();
        }

        if (command.hasMoreTokens()) {
            discordIdNumber = Integer.parseInt(command.nextToken());
        }

        if (discordIdNumber != 0) {
            player.setDiscordID(discordName + "#" + discordIdNumber);
            player.toSelf("AM:Discord ID set to " + player.getDiscordID());
        } else {player.toSelf("AM:Error occured, please try again. Ex: /updatediscordinfo myname#0023");}
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}

