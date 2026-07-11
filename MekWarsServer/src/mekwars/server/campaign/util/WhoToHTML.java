package mekwars.server.campaign.util;
import megamek.logging.MMLogger;

public class WhoToHTML {
    private static final MMLogger LOGGER = MMLogger.create(WhoToHTML.class);
    private java.util.HashMap<String, mekwars.server.campaign.util.WhoToHTML.DataEntry> players = null;
    private String outputPath;

    public WhoToHTML(String outputPath) {
        players = new java.util.HashMap<String, mekwars.server.campaign.util.WhoToHTML.DataEntry>();
        this.outputPath = outputPath;
    }

    public void addPlayer(server.campaign.SPlayer p) {
        players.put(p.getName(), new mekwars.server.campaign.util.WhoToHTML.DataEntry(p));
    }

    public void outputHTML() {
        StringBuilder output = new StringBuilder();

        output.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
        output.append(
              "<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"><title>Who is Online</title>");
        output.append("<LINK href='mw_who.css' rel='stylesheet' type='text/css'></head><body>");
        output.append(
              "<div class='header'><div class='playerStatusHeader'>Status</div><div class='playerNameHeader'>Player</div></div>");

        java.util.Map<String, mekwars.server.campaign.util.WhoToHTML.DataEntry> pMap = new java.util.TreeMap<String, mekwars.server.campaign.util.WhoToHTML.DataEntry>(
              players);

        for (String s : pMap.keySet()) {
            output.append(pMap.get(s).getHTML());
        }

        output.append("<p class='footer'>" +
                            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) +
                            "</p>");
        output.append("</body>");

        // Now write it to a file

        try {
            java.io.FileWriter fw;
            java.io.BufferedWriter out;
            fw = new java.io.FileWriter(outputPath);
            out = new java.io.BufferedWriter(fw);
            out.write(output.toString());
            out.close();
            fw.close();
        } catch (java.io.IOException e) {
            // TODO Auto-generated catch block
            LOGGER.error("IOException in WhoToHTML");
            LOGGER.error(e, "");
        }

        return;
    }

    private class DataEntry {
        private String html;

        public DataEntry(server.campaign.SPlayer p) {
            setHTML(p.getName(), p.getDutyStatus(), p.getMyHouse().getAbbreviation());
        }

        private void setHTML(String name, int status, String houseName) {
            StringBuilder output = new StringBuilder();
            output.append("<div class='playerLine'><div class='playerStatus'><img src='");
            if (status == server.campaign.SPlayer.STATUS_FIGHTING) {
                output.append("fighting_colored.gif");
            } else {
                output.append("reserve_colored.gif");
            }
            output.append("'></div><div class='playerEntry'><span class='" + houseName + "player'>");
            output.append(name);
            output.append("</span></div></div>");
            html = output.toString();
        }

        public String getHTML() {
            return html;
        }
    }
}
