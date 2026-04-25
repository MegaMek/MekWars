package mekwars.server.campaign.commands;

import common.util.MWLogger;

//@salient a command to make sure hosts are using same quirk files
public class QuirkCheckCommand implements Command {
    int accessLevel = 0;
    String syntax = "/c quirkCheck#canon#custom";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    private server.campaign.SPlayer player;
    private String username;
    private int userLevel;
    private long clientCanonQuirkLength;
    private long clientCustomQuirkLength;
    private boolean enableQuirks;

    public void process(java.util.StringTokenizer command, String Username) {
        username = Username;

        initVars();

        if (checkAccess() == false) {return;}

        if (command.hasMoreTokens() == false) {
            player.toSelf("AM:Missing Quirk File Lengths. Syntax: " + syntax);
            return;
        } else {clientCanonQuirkLength = Long.parseLong(command.nextToken());}

        if (command.hasMoreTokens() == false) {
            player.toSelf("AM:Missing Custom Quirk File Length. Syntax: " + syntax);
            return;
        } else {clientCustomQuirkLength = Long.parseLong(command.nextToken());}

        compareLengths();
    }


    private void initVars() {
        enableQuirks = server.campaign.CampaignMain.cm.getBooleanConfig("EnableQuirks");
        player = server.campaign.CampaignMain.cm.getPlayer(username);
        userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(username);
    }

    private boolean checkAccess() {
        if (accessLevel != 0) {
            if (userLevel < getExecutionLevel()) {
                player.toSelf("AM:Insufficient access level. Level: " + userLevel + ". Required: " + accessLevel + ".");
                return false;
            }
        }

        if (enableQuirks == false) {
            player.toSelf("AM:Quirks have been disabled, the SO does NOT like fun.");
            return false;
        }

        return true;
    }

    private void compareLengths() {
        if (clientCanonQuirkLength == 0L) {
            player.toSelf("AM: canon quirk file is missing!");
            MWLogger.modLog(username + " is missing canon quirk file on client!");
            return;
        }

        if (clientCustomQuirkLength == 0L) {
            player.toSelf("AM: canon quirk file is missing!");
            MWLogger.modLog(username + " is missing canon quirk file on client!");
            return;
        }

        java.io.File canon = new java.io.File("data" + java.io.File.separator + "canonUnitQuirks.xml");
        java.io.File custom = new java.io.File("data" +
                                                     java.io.File.separator +
                                                     "mmconf" +
                                                     java.io.File.separator +
                                                     "unitQuirksOverride.xml");
        long serverCanonQuirkLength = canon.length(); // returns 0L if does not exist
        long serverCustomQuirkLength = custom.length();

        if (serverCanonQuirkLength != clientCanonQuirkLength || serverCustomQuirkLength != clientCustomQuirkLength) {
            server.campaign.CampaignMain.cm.doSendModMail(username,
                  " is hosting with quirk files that do not match server!");
            MWLogger.errLog(username + " is hosting with quirk files that do not match server!");
            server.campaign.CampaignMain.cm.doSendErrLog(username +
                                                               " is hosting with quirk files that do not match server!");
            player.toSelf("AM: Your files do not match the server, run autoupdate before hosting a match!");
        }

        //player.toSelf("AM: DEBUG: " + serverCanonQuirkLength + clientCanonQuirkLength + serverCustomQuirkLength + clientCustomQuirkLength);
    }
}


