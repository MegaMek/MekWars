package mekwars.server.campaign.commands.admin;

public class AdminRenamePlanetCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "PlanetID#OldName#NewName";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
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
            server.campaign.CampaignMain.cm.toUser("Improper command. Try: /adminrenameplanet planetID#oldName#newName",
                  Username,
                  true);
            return;
        }

        server.campaign.SPlanet p = server.campaign.CampaignMain.cm.getPlanetFromPartialString(oldName, Username);

        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("Could not find a matching planet.", Username, true);
            return;
        }

        p.setName(newName);
        java.io.File fp = new java.io.File("./campaign/planets/" + oldName.toLowerCase().trim() + ".dat");
        if (fp.exists()) {fp.delete();}
        server.campaign.CampaignMain.cm.savePlanetData();

        server.campaign.CampaignMain.cm.updateHousePlanetUpdate();
        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " renamed " + oldName + " to " + newName);

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

}
