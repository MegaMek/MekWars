package server.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

import common.util.MWLogger;
import server.campaign.CampaignMain;

public class StringUtil {
	
    private static Cleaner HTMLCleaner = null;
    


	public static String replacePlanetTags(String input) {
		String regex = "<planet name=\"([^\"]+)\">([^<]+)</planet>";
		String replacement = "<a href=\"JUMPTOPLANET$1\">$2</a>";
		Pattern planetPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher matcher = planetPattern.matcher(input);
		return matcher.replaceAll(replacement);
	}
	
	/**
	 * Returns a string sanitized of potentially harmful HTML
	 * 
	 * @param Unsanitized string
	 * @return Sanitized string
	 * 
	 * @author Spork
	 */
	public static String sanitize(String unclean) {
		Document doc = Jsoup.parse(unclean);
		doc = HTMLCleaner.clean(doc);
		String toReturn = doc.body().toString().replace("<body>", "").replace("</body>", "");
		boolean allowPlanets = CampaignMain.cm.getBooleanConfig("AllowPlanetsInMOTD");
		if (allowPlanets) {
			toReturn = StringUtil.replacePlanetTags(toReturn);
		}
		
		return toReturn;
	}

	public static void loadSanitizer() {
        Whitelist whitelist = Whitelist.relaxed();
        if(!CampaignMain.cm.getBooleanConfig("AllowLinksInMOTD")) {
        	whitelist.addEnforcedAttribute("a", "rel", "nofollow");
        }

        whitelist.addTags("planet");
        whitelist.addAttributes("planet", "name");
        
        Vector<String> allowedTags = new Vector<String>();
        HashMap<String, Vector<String>> allowedAttributes = new HashMap<String, Vector<String>>();
        
        try {
			FileInputStream fstream = new FileInputStream("./data/HTMLSanitizer.cfg");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			while((line = br.readLine()) != null) {
				line = line.toLowerCase().trim();
				// Process it here
				if (line.startsWith("#") || line.length() < 5) {
					// Comment
				} else if (line.startsWith("tag")) {
					// Add next token to allowed tags
					StringTokenizer st = new StringTokenizer(line, " ");
					st.nextToken();  // Eat the "Tag" token
					allowedTags.add(st.nextToken());
				} else if (line.startsWith("att")) {
					StringTokenizer st = new StringTokenizer(line, " ");
					st.nextToken(); // Eat the "Att" token
					String tag = st.nextToken();
					while(st.hasMoreTokens()) {
						if (allowedAttributes.keySet().contains(tag)) {
							allowedAttributes.get(tag).add(st.nextToken());
						} else {
							Vector<String> newTag = new Vector<String>();
							newTag.add(st.nextToken());
							allowedAttributes.put(tag, newTag);
						}
					}
				}
			}
			br.close();
			in.close();
			fstream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			MWLogger.errLog("No HTMLSanitizer.cfg found.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		for(String tag : allowedTags) {
			MWLogger.errLog("Adding to whitelist: " + tag);
			whitelist.addTags(tag);
		}
		
		for(String att : allowedAttributes.keySet()) {
			Vector<String> attributes = allowedAttributes.get(att);
			
			for (String attribute : attributes) {
				whitelist.addAttributes(att, attribute);
			}
		}
MWLogger.errLog(whitelist.toString());
		Cleaner c = new Cleaner(whitelist);
		HTMLCleaner = c;
	}
	
	public static void reloadSanitizer() {
		loadSanitizer();
	}

	/**
	 * Method which generates human readible times from miliseconds. Useful only
	 * for times which are known to be minutes or seconds in length. Produces
	 * full-word output.
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
     * @param text  The string to be evaluated.
     */
    public static boolean isNullOrEmpty(String text) {
        return (text == null) || (text.trim().isEmpty());
    }


    /**
     * Returns TRUE if the passed in text is either a NULL value or is an empty string.
     *
     * @param text The string to be evaluated.
     */
    public static boolean isNullOrEmpty(StringBuilder text) {
        return (text == null) || isNullOrEmpty(text.toString());
    }

}
