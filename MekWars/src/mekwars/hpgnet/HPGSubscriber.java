/*
 * MekWars - Copyright (C) 2018
 * 
 * Original author - Bob Eldred (spork@mekwars.org)  
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package hpgnet;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Iterator;

import com.google.common.collect.EvictingQueue;
import com.google.gson.annotations.Expose;

/**
 * A container for information about the client servers 
 * 
 * @author Spork
 * @version 1.0
 * 
 */

public class HPGSubscriber implements Comparable<HPGSubscriber> {

	@Expose(serialize = false, deserialize = false)
	private static final long serialVersionUID = -6353737452488309978L;
	@Expose(serialize = true, deserialize = true)
	private String name;
	@Expose(serialize = true, deserialize = true)
	private String url;
	@Expose(serialize = true, deserialize = true)
	private String description;
	@Expose(serialize = true, deserialize = true)
	private EvictingQueue<Integer> historicalPlayers;
	@Expose(serialize = true, deserialize = true)
	private EvictingQueue<Integer> historicalGames;
	@Expose(serialize = true, deserialize = true)
	private EvictingQueue<Integer> historicalCompletedGames;
	@Expose(serialize = true, deserialize = true)
	private int maxPlayers;
	@Expose(serialize = true, deserialize = true)
	private int maxGames;
	@Expose(serialize = false, deserialize = false)
	private int currentPlayers;
	@Expose(serialize = false, deserialize = false)
	private int currentGames;
	@Expose(serialize = true, deserialize = true)
	private int port;  // The port over which inter-server mail will happen
	@Expose(serialize = true, deserialize = true)
	private String ipAddress;
	@Expose(serialize = true, deserialize = true)
	private String domain; // the part after the user name when sending inter-server mail
	@Expose(serialize = true, deserialize = true)
	private Date lastUpdated;
	@Expose(serialize = false, deserialize = false)
	private String trackerEntry;
	@Expose(serialize = true, deserialize = true)
	private String MWVersion;
	@Expose(serialize = true, deserialize = true)
	private String uuid;
	@Expose(serialize = true, deserialize = true)
	private String password;
	@Expose(serialize = true, deserialize = true)
	private boolean isLegacy = false;
	@Expose(serialize = true, deserialize = true)
	private int totalGames;
	@Expose(serialize = false, deserialize = false)
	private int threatLevel;
	@Expose(serialize = false, deserialize = false)
	private HPGNet tracker;
	
	

	public static int THREAT_LEVEL_NONE = 0;
	public static int THREAT_LEVEL_YELLOW = 1;
	public static int THREAT_LEVEL_RED = 2;
	public static int THREAT_LEVEL_PURGE = 3;

	/**
	 * @return the threatLevel
	 */
	public int getThreatLevel() {
		return threatLevel;
	}

	/**
	 * @param threatLevel the threatLevel to set
	 */
	public void setThreatLevel(int threatLevel) {
		this.threatLevel = threatLevel;
	}
	
	/**
	 * @return the totalGames
	 */
	public int getTotalGames() {
		return totalGames;
	}

	/**
	 * @param totalGames the totalGames to set
	 */
	public void setTotalGames(int totalGames) {
		this.totalGames = totalGames;
	}

	/**
	 * @return the isLegacy
	 */
	public boolean isLegacy() {
		return isLegacy;
	}

	/**
	 * @param isLegacy the isLegacy to set
	 */
	public void setLegacy(boolean isLegacy) {
		this.isLegacy = isLegacy;
	}

	/**
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the mWVersion
	 */
	public String getMWVersion() {
		return MWVersion;
	}
	
	/**
	 * @param mWVersion the mWVersion to set
	 */
	public void setMWVersion(String mWVersion) {
		MWVersion = mWVersion;
	}
	
	/**
	 * @return the trackerEntry
	 */
	public String getTrackerEntry() {
		return trackerEntry;
	}
	
	/**
	 * @param htmlString the trackerEntry to set
	 */
	public void setTrackerEntry(String htmlString) {
		this.trackerEntry = htmlString;
	}
	
	/**
	 * @return the lastUpdated
	 */
	public Date getLastUpdated() {
		return lastUpdated;
	}
	
	/**
	 * @param lastUpdated the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	
	/**
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}
	
	/**
	 * @param domain the domain to set
	 */
	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}
	
	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * @return the currentPlayers
	 */
	public int getCurrentPlayers() {
		return currentPlayers;
	}
	
	/**
	 * @param currentPlayers the currentPlayers to set
	 */
	public void setCurrentPlayers(int currentPlayers) {
		this.currentPlayers = currentPlayers;
	}
	
	/**
	 * @return the currentGames
	 */
	public int getCurrentGames() {
		return currentGames;
	}
	
	/**
	 * @param currentGames the currentGames to set
	 */
	public void setCurrentGames(int currentGames) {
		this.currentGames = currentGames;
	}
	
	/**
	 * @return the historicalPlayers
	 */
	public EvictingQueue<Integer> getHistoricalPlayers() {
		return historicalPlayers;
	}
	
	/**
	 * @param historicalPlayers the historicalPlayers to set
	 */
	public void setHistoricalPlayers(EvictingQueue<Integer> historicalPlayers) {
		this.historicalPlayers = historicalPlayers;
	}
	
	/**
	 * @return the historicalGames
	 */
	public EvictingQueue<Integer> getHistoricalGames() {
		return historicalGames;
	}
	
	/**
	 * @param historicalGames the historicalGames to set
	 */
	public void setHistoricalGames(EvictingQueue<Integer> historicalGames) {
		this.historicalGames = historicalGames;
	}
	
	/**
	 * @return the maxPlayers
	 */
	public int getMaxPlayers() {
		return maxPlayers;
	}
	
	/**
	 * @param maxPlayers the maxPlayers to set
	 */
	public void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}
	
	/**
	 * @return the maxGames
	 */
	public int getMaxGames() {
		return maxGames;
	}
	
	/**
	 * @param maxGames the maxGames to set
	 */
	public void setMaxGames(int maxGames) {
		this.maxGames = maxGames;
	}
	
	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	/**
	 * @return the ipAddress
	 */
	public String getIpAddress() {
		return ipAddress;
	}
	
	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	/**
	 * Create a new HPGSubscriber
	 */
	public HPGSubscriber() {
		// Servers update every 10 minutes.  We want to keep 7 days of history
		// 7 days = 1008 entries
		historicalGames = EvictingQueue.create(1008);
		historicalPlayers = EvictingQueue.create(1008);
		historicalCompletedGames = EvictingQueue.create(1008);
	}
	
	/**
	 * @param t the tracker to set
	 */
	public void setTracker(HPGNet t) {
		tracker = t;
	}
	
	/**
	 * Adds a game to the CompletedGames EvictingQueue
	 * @param completedGames
	 */
	public void addHistoricalCompletedGamesElement(int completedGames) {
		historicalCompletedGames.add(completedGames);
		calculateCompletedGames();
	}
	
	/**
	 * Adds a game to the EvictingQueue
	 * @param games
	 */
	public void addHistoricalGamesElement(int games) {
		historicalGames.add(games);
		calculateMaxGames();
	}
	
	/**
	 * Adds a player entry to the EvictingQueue
	 * @param players
	 */
	public void addHistoricalPlayersElement(int players) {
		historicalPlayers.add(players);
		calculateMaxPlayers();
	}

	/**
	 * Iterates through the currentPlayers queue, calculating the max value
	 */
	private void calculateMaxPlayers() {
		int max = 0;
		Iterator<Integer> iter = historicalPlayers.iterator();
		while(iter.hasNext()) {
			max = Math.max(max, iter.next());
		}
		setMaxPlayers(max);
	}
	
	/**
	 * Iterates through the currentGames queue, calculating the max value
	 */
	private void calculateMaxGames() {
		int max = 0;
		Iterator<Integer> iter = historicalGames.iterator();
		while (iter.hasNext()) {
			max = Math.max(max, iter.next());
		}
		setMaxGames(max);
	}
	
	/**
	 * Iterates through the completed games queue, calculating the total
	 */
	private void calculateCompletedGames() {
		int total = 0;
		Iterator<Integer> iter = historicalCompletedGames.iterator();
		while (iter.hasNext()) {
			total += iter.next();
		}
		setTotalGames(total);
	}
	
	/**
	 * Called when a server updates its statistics
	 * @param players the number of players on the server
	 * @param games the number of games in progress
	 */
	public void update(int players, int games, int completedGames) {
		addHistoricalPlayersElement(players);
		setCurrentPlayers(players);
		addHistoricalGamesElement(games);
		setCurrentGames(games);
		addHistoricalCompletedGamesElement(completedGames);
		setLastUpdated(new Date());
		calculateThreatLevel();
		generateHTMLString();
	}
	
	/**
	 * Creates the tracker entry
	 */
	public void generateHTMLString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<tr class='");
		if(getThreatLevel() == THREAT_LEVEL_RED) {
			sb.append("red");
		} else if (getThreatLevel() == THREAT_LEVEL_YELLOW) {
			sb.append("yellow");
		} else if (getThreatLevel() == THREAT_LEVEL_PURGE) {
			sb.append("red");  // We're going to purge within the next 12 hours anyway
		} else {
			sb.append("green");
		}
		sb.append("'>");
		sb.append("<td><a href=\"" + getUrl() + "\">" + getName() + "</a></td>");
		sb.append(buildColumn(getMWVersion()));
		sb.append(buildColumn(Integer.toString(getCurrentPlayers())));
		sb.append(buildColumn(Integer.toString(getCurrentGames())));
		sb.append(buildColumn(Integer.toString(getMaxPlayers())));
		sb.append(buildColumn(Integer.toString(getTotalGames())));
		sb.append(buildColumn(getDescription()));
		//sb.append(buildColumn(getLastUpdated().toString()));
		sb.append(buildDateColumn(getLastUpdated().toString()));
		sb.append("</tr>\n");
		setTrackerEntry(sb.toString());
	}
	
	/**
	 * A helper method so I don't have to type as much
	 * @param data the piece between &lt;td&gt;&lt;/td&gt; tags
	 * @return the completed &lt;td&gt; entry
	 */
	private String buildColumn(String data) {
		return "<td>" + data + "</td>";
	}
	
	/**
	 * Games completed since last update.  This should force a calculation of total games
	 */
	public void setCompletedGames(int completedGames) {
		addHistoricalCompletedGamesElement(completedGames);
	}
	
	/**
	 * Another helper method. This puts in images that will get hidden based on what the threat
	 * level is.
	 * @param data
	 * @return
	 */
	private String buildDateColumn(String data) {
		int purgeDays = Integer.parseInt((String)tracker.getConfig().get("purgedays"));
		
		Instant instant = Instant.now();
		ZoneId zoneId = ZoneId.systemDefault();
		ZonedDateTime subDate = ZonedDateTime.ofInstant(getLastUpdated().toInstant(), zoneId);
		ZonedDateTime today = ZonedDateTime.ofInstant(instant, zoneId);
		ZonedDateTime purgeDate = subDate.plus(purgeDays, ChronoUnit.DAYS); 
		
		Period period = Period.between(today.toLocalDate(), purgeDate.toLocalDate()); // How long until the purge
		String dayOrDays = period.getDays() > 1 ? "days" : "day";
		StringBuilder sb = new StringBuilder("<td><div class='date'>");
		sb.append(data);
		sb.append("</div><div class='yellow'><image src='images/yellow.png' title='This entry will be removed from the tracker in ");
		sb.append(period.getDays());
		sb.append(" ");
		sb.append(dayOrDays);
		sb.append("'></div>");
		sb.append("<div class='red'><image src='images/red.png' title='This entry will be removed from the tracker in ");
		sb.append(period.getDays());
		sb.append(" ");
		sb.append(dayOrDays);
		sb.append("'></div></td>");
		return sb.toString();
	}

	/**
	 * Sorts two HPGSubscribers by name
	 */
	@Override
	public int compareTo(HPGSubscriber o) {
		return getName().compareTo(o.getName());
	}
	
	/**
	 * Sets threatLevel based on how soon the entry will be deleted from the tracker
	 */
	public void calculateThreatLevel() {
		int firstWarnDays = Integer.parseInt((String)tracker.getConfig().get("firstwarndays"));
		int lastWarnDays = Integer.parseInt((String)tracker.getConfig().get("lastwarndays"));
		int purgeDays = Integer.parseInt((String)tracker.getConfig().get("purgedays"));
		
		Instant instant = Instant.now();
		ZoneId zoneId = ZoneId.systemDefault();
		ZonedDateTime subDate = ZonedDateTime.ofInstant(getLastUpdated().toInstant(), zoneId);
		ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, zoneId);
		ZonedDateTime firstWarn = zdt.minus(firstWarnDays, ChronoUnit.DAYS); 
		ZonedDateTime lastWarn = zdt.minus(lastWarnDays, ChronoUnit.DAYS); 
		ZonedDateTime purge = zdt.minus(purgeDays, ChronoUnit.DAYS); 
		
		if(subDate.isBefore(purge)) {
			setThreatLevel(THREAT_LEVEL_PURGE);
		} else if (subDate.isBefore(lastWarn)) {
			setThreatLevel(THREAT_LEVEL_RED);
		} else if (subDate.isBefore(firstWarn)) {
			setThreatLevel(THREAT_LEVEL_YELLOW);
		} else {
			setThreatLevel(THREAT_LEVEL_NONE);
		}
		
	}
	
}
