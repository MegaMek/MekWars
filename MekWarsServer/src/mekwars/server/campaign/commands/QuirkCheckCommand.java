package mekwars.server.campaign.commands;
import megamek.logging.MMLogger;
import mekwars.server.campaign.CampaignMain;

//@salient a command to make sure hosts are using same quirk files
public class QuirkCheckCommand implements Command {
    private static final MMLogger LOGGER = MMLogger.create(QuirkCheckCommand.class);
    int accessLevel = 0;
    String syntax = "/c quirkCheck#canon#custom";
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

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    private void initVars() {
        enableQuirks = CampaignMain.campaignMain.getBooleanConfig("EnableQuirks");
        player = CampaignMain.campaignMain.getPlayer(username);
        userLevel = CampaignMain.campaignMain.getServer().getUserLevel(username);
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
            LOGGER.info(username + " is missing canon quirk file on client!");
            return;
        }

        if (clientCustomQuirkLength == 0L) {
            player.toSelf("AM: canon quirk file is missing!");
            LOGGER.info(username + " is missing canon quirk file on client!");
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
            CampaignMain.campaignMain.doSendModMail(username,
                  " is hosting with quirk files that do not match server!");
            LOGGER.error(username + " is hosting with quirk files that do not match server!");
            CampaignMain.campaignMain.doSendErrLog(username +
                                                         " is hosting with quirk files that do not match server!");
            player.toSelf("AM: Your files do not match the server, run autoupdate before hosting a match!");
        }

        //player.toSelf("AM: DEBUG: " + serverCanonQuirkLength + clientCanonQuirkLength + serverCustomQuirkLength + clientCustomQuirkLength);
    }
}


