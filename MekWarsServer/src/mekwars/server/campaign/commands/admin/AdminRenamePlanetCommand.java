package mekwars.server.campaign.commands.admin;

import mekwars.server.campaign.CampaignMain;

public class AdminRenamePlanetCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "PlanetID#OldName#NewName";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        int pID = -1;
        String oldName = "";
        String newName = "";

        if (command.hasMoreElements()) {pID = Integer.parseInt(command.nextToken());}
        if (command.hasMoreElements()) {oldName = command.nextToken();}
        if (command.hasMoreElements()) {newName = command.nextToken();}

        if (pID == -1 || oldName.equals("") || newName.equals("")) {
            // Incorrect command format
            CampaignMain.campaignMain.toUser("Improper command. Try: /adminrenameplanet planetID#oldName#newName",
                  Username,
                  true);
            return;
        }

        server.campaign.SPlanet p = CampaignMain.campaignMain.getPlanetFromPartialString(oldName, Username);

        if (p == null) {
            CampaignMain.campaignMain.toUser("Could not find a matching planet.", Username, true);
            return;
        }

        p.setName(newName);
        java.io.File fp = new java.io.File("./campaign/planets/" + oldName.toLowerCase().trim() + ".dat");
        if (fp.exists()) {fp.delete();}
        CampaignMain.campaignMain.savePlanetData();

        CampaignMain.campaignMain.updateHousePlanetUpdate();
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " renamed " + oldName + " to " + newName);

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

}
