package mekwars.server.campaign.commands;

public class UpdateDiscordInfoCommand implements Command {
    int accessLevel = 1;
    String syntax = "updatediscordinfo id";

    public void process(java.util.StringTokenizer command, String Username) {
        //access level checks
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);

        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }

        if (!Boolean.parseBoolean(server.campaign.CampaignMain.cm.getConfig("Enable_BotPlayerInfo"))) {
            server.campaign.CampaignMain.cm.toUser("AM:This command is disabled on this server.", Username, true);
            return;
        }

        server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(Username);

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

