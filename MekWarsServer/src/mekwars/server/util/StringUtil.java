package mekwars.server.util;


import java.lang.ref.Cleaner;

import megamek.logging.MMLogger;
import mekwars.server.campaign.CampaignMain;

public class StringUtil {
    private static final MMLogger LOGGER = MMLogger.create(StringUtil.class);

    private static Cleaner HTMLCleaner = null;

    /**
     * Returns a string sanitized of potentially harmful HTML
     *
     * @param Unsanitized string
     *
     * @return Sanitized string
     *
     * @author Spork
     */
    public static String sanitize(String unclean) {
        Document doc = Jsoup.parse(unclean);
        doc = HTMLCleaner.clean(doc);
        String toReturn = doc.body().toString().replace("<body>", "").replace("</body>", "");
        boolean allowPlanets = CampaignMain.campaignMain.getBooleanConfig("AllowPlanetsInMOTD");
        if (allowPlanets) {
            toReturn = mekwars.server.util.StringUtil.replacePlanetTags(toReturn);
        }

        return toReturn;
    }

    public static String replacePlanetTags(String input) {
        String regex = "<planet name=\"([^\"]+)\">([^<]+)</planet>";
        String replacement = "<a href=\"JUMPTOPLANET$1\">$2</a>";
        java.util.regex.Pattern planetPattern = java.util.regex.Pattern.compile(regex,
              java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = planetPattern.matcher(input);
        return matcher.replaceAll(replacement);
    }

    public static void reloadSanitizer() {
        loadSanitizer();
    }

    public static void loadSanitizer() {
        Whitelist whitelist = Whitelist.relaxed();
        if (!CampaignMain.campaignMain.getBooleanConfig("AllowLinksInMOTD")) {
            whitelist.addEnforcedAttribute("a", "rel", "nofollow");
        }

        whitelist.addTags("planet");
        whitelist.addAttributes("planet", "name");

        java.util.Vector<String> allowedTags = new java.util.Vector<String>();
        java.util.HashMap<String, java.util.Vector<String>> allowedAttributes = new java.util.HashMap<String, java.util.Vector<String>>();

        try {
            java.io.FileInputStream fstream = new java.io.FileInputStream("./data/HTMLSanitizer.cfg");
            java.io.DataInputStream in = new java.io.DataInputStream(fstream);
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.toLowerCase().trim();
                // Process it here
                if (line.startsWith("#") || line.length() < 5) {
                    // Comment
                } else if (line.startsWith("tag")) {
                    // Add next token to allowed tags
                    java.util.StringTokenizer st = new java.util.StringTokenizer(line, " ");
                    st.nextToken();  // Eat the "Tag" token
                    allowedTags.add(st.nextToken());
                } else if (line.startsWith("att")) {
                    java.util.StringTokenizer st = new java.util.StringTokenizer(line, " ");
                    st.nextToken(); // Eat the "Att" token
                    String tag = st.nextToken();
                    while (st.hasMoreTokens()) {
                        if (allowedAttributes.keySet().contains(tag)) {
                            allowedAttributes.get(tag).add(st.nextToken());
                        } else {
                            java.util.Vector<String> newTag = new java.util.Vector<String>();
                            newTag.add(st.nextToken());
                            allowedAttributes.put(tag, newTag);
                        }
                    }
                }
            }
            br.close();
            in.close();
            fstream.close();
        } catch (java.io.FileNotFoundException e) {
            // TODO Auto-generated catch block
            LOGGER.error("No HTMLSanitizer.cfg found.");
        } catch (java.io.IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for (String tag : allowedTags) {
            LOGGER.error("Adding to whitelist: " + tag);
            whitelist.addTags(tag);
        }

        for (String att : allowedAttributes.keySet()) {
            java.util.Vector<String> attributes = allowedAttributes.get(att);

            for (String attribute : attributes) {
                whitelist.addAttributes(att, attribute);
            }
        }
        LOGGER.error(whitelist.toString());
        Cleaner c = new Cleaner(whitelist);
        HTMLCleaner = c;
    }

    /**
     * Method which generates human readible times from miliseconds. Useful only for times which are known to be minutes
     * or seconds in length. Produces full-word output.
     */
    public static String readableTimeWithSeconds(long elapsed) {

        // to return
        String result = "";

        long elapsedMinutes = elapsed / 60000;
        long elapsedSeconds = (elapsed % 60000) / 1000;

        if (elapsedMinutes > 0) {
            result += elapsedMinutes + " min";
        }

        if (elapsedSeconds > 0 && elapsedMinutes > 0) {
            result += ", " + elapsedSeconds + " sec";
        } else if (elapsedSeconds > 0) {
            result += elapsedSeconds + " sec";
        }

        return result;
    }

    /*
     * Replace original readible time (which oddly adjusted times from MechStats
     * into seconds, but used ms from System.currentTime() for comparison) with
     * similar code from MWTracker.java. This produces abbreviated timenames.
     *
     * @urgru 8.6.05
     */
    public static String readableTime(long elapsed) {

        // to return
        String result = "";

        long elapsedDays = (elapsed / 86400000);
        long elapsedHours = (elapsed % 86400000) / 3600000;
        long elapsedMinutes = (elapsed % 3600000) / 60000;

        if (elapsedDays > 0) {
            result += elapsedDays + "d ";
        }

        if (elapsedHours > 0 || elapsedDays > 0) {
            result += elapsedHours + "h ";
        }

        result += elapsedMinutes + "m";

        return result;
    }

    /**
     * Returns TRUE if the passed in text is either a NULL value or is an empty string.
     *
     * @param text The string to be evaluated.
     */
    public static boolean isNullOrEmpty(StringBuilder text) {
        return (text == null) || isNullOrEmpty(text.toString());
    }

    /**
     * Returns TRUE if the passed in text is either a NULL value or is an empty string.
     *
     * @param text The string to be evaluated.
     */
    public static boolean isNullOrEmpty(String text) {
        return (text == null) || (text.trim().isEmpty());
    }

}
