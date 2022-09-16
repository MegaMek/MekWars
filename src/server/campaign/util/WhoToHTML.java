package server.campaign.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import common.util.MWLogger;
import server.campaign.SPlayer;

public class WhoToHTML {
	private HashMap<String,DataEntry> players = null;
	private String outputPath;
	
	public void addPlayer(SPlayer p) {
		players.put(p.getName(), new DataEntry(p));
	}
	
	public void outputHTML() {
		StringBuilder output = new StringBuilder();
		
		output.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
		output.append("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"><title>Who is Online</title>");
		output.append("<LINK href='mw_who.css' rel='stylesheet' type='text/css'></head><body>");
		output.append("<div class='header'><div class='playerStatusHeader'>Status</div><div class='playerNameHeader'>Player</div></div>");
		
		Map<String, DataEntry> pMap = new TreeMap<String, DataEntry>(players);
		
		for (String s : pMap.keySet()) {
			output.append(pMap.get(s).getHTML());
		}
		
		output.append("<p class='footer'>" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "</p>");
		output.append("</body>");
		
		// Now write it to a file

		try {
			FileWriter fw;
			BufferedWriter out;
			fw = new FileWriter(outputPath);
			out = new BufferedWriter(fw);
			out.write(output.toString());
			out.close();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			MWLogger.errLog("IOException in WhoToHTML");
			MWLogger.errLog(e);
		} 
		
		return;
	}
	
	public WhoToHTML(String outputPath) {
		players = new HashMap<String, DataEntry>();
		this.outputPath = outputPath;
	}
	
	private class DataEntry {
		private String html;
		
		public String getHTML() {
			return html;
		}
		
		private void setHTML(String name, int status, String houseName) {
			StringBuilder output = new StringBuilder();
			output.append("<div class='playerLine'><div class='playerStatus'><img src='");
			if (status == SPlayer.STATUS_FIGHTING) {
				output.append("fighting_colored.gif");
			} else {
				output.append("reserve_colored.gif");
			}
			output.append("'></div><div class='playerEntry'><span class='" + houseName + "player'>");
			output.append(name);
			output.append("</span></div></div>");
			html = output.toString();
		}
		
		public DataEntry(SPlayer p) {
			setHTML(p.getName(), p.getDutyStatus(), p.getMyHouse().getAbbreviation());
		}
	}
}
