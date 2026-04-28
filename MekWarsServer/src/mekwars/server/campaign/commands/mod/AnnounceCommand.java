package mekwars.server.campaign.commands.mod;

import common.CampaignData;
import common.House;


public class AnnounceCommand implements server.campaign.commands.Command {
    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Set for Faction: /announce [FactionName]#Message ('clear' to unset)<br>Set for all Factions: /announce All#Message ('clear' to unset)";

    @Override
    public void process(java.util.StringTokenizer command, String Username) {
        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser(
                      "AM:Insufficient access level for command. Level: "
                            + userLevel + ". Required: " + accessLevel
                            + ".", Username, true);
                return;
            }
        }
        if (!command.hasMoreTokens()) {
            server.campaign.CampaignMain.cm.toUser("Invalid Sytax: <br>" + getSyntax(), Username, true);
            return;
        }

        String scope = command.nextToken();
        if (scope.equalsIgnoreCase("all")) {
            // Set for all factions
            String announcement = "";
            try {
                //there may be #'s in HTML. Use all tokens and restore #'s.
                announcement = command.nextToken();
                while (command.hasMoreTokens()) {announcement += "#" + command.nextToken();}
            } catch (Exception e) {
                server.campaign.CampaignMain.cm.toUser("AM:nvalid Syntax: <br> " + getSyntax(), Username, true);
                return;
            }
            server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);
            if (announcement.trim().equals("") || announcement.trim().equalsIgnoreCase("clear")) {
                announcement = "";
            }
            for (House h : CampaignData.cd.getAllHouses()) {
                server.campaign.SHouse house = (server.campaign.SHouse) h;
                house.setAnnouncement(announcement);
            }
            server.campaign.CampaignMain.cm.toUser("Announcement set for all factions.", Username, true);

        } else {
            // Set for a single faction
            server.campaign.SHouse h = (server.campaign.SHouse) server.campaign.CampaignMain.cm.getData()
                                                                      .getHouseByName(scope);
            if (h == null) {
                server.campaign.CampaignMain.cm.toUser("Invalid Syntax: <br> " + getSyntax(), Username, true);
            }
            String announcement = "";
            try {
                //there may be #'s in HTML. Use all tokens and restore #'s.
                announcement = command.nextToken();
                while (command.hasMoreTokens()) {announcement += "#" + command.nextToken();}
            } catch (Exception e) {
                server.campaign.CampaignMain.cm.toUser("AM:nvalid Syntax: <br> " + getSyntax(), Username, true);
                return;
            }
            server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);
            if (announcement.trim().equals("") || announcement.trim().equalsIgnoreCase("clear")) {
                p.getMyHouse().setAnnouncement("");
                server.campaign.CampaignMain.cm.toUser("AM:" + scope + " announcement cleared.", Username, true);
                return;
            }
            p.getMyHouse().setAnnouncement(announcement + "<p> -- Set by " + p.getName());
            server.campaign.CampaignMain.cm.toUser("AM:MOTD set. Use /c motd to review.", Username, true);
        }
    }

    @Override
    public int getExecutionLevel() {
        return accessLevel;
    }

    @Override
    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    @Override
    public String getSyntax() {
        return syntax;
    }


}
