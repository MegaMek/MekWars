/*
 * MekWars - Copyright (C) 2016
 * 
 * original author: Bob Eldred (billypinhead@users.sourceforge.net)
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

package server.campaign.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import common.util.MWLogger;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.util.scheduler.EndChristmasJob;
import server.campaign.util.scheduler.StartChristmasJob;

/**
 * A class to handle scheduling of distribution of meks during the Christmas season.  Historically,
 * we've had to hand them out manually.
 * 
 * @author Spork
 * @version 2016.10.26
 */
public class ChristmasHandler {
	private static ChristmasHandler handler;
	
	/**
	 * Start date of the Christmas season
	 */
	private Date startDate;

	/**
	 * End date of the Christmas season
	 */
	private Date endDate;
	
	/** 
	 * Whether or not it is the Christmas Season
	 */
	private boolean isChristmasSeason = false;
	
	/**
	 * A collection containing a list of names of folks who have received their gifts already
	 * <p>
	 * At some point, I may make this just a list, rather than a map.  Right now, I'm not really using
	 * it for anything that a list wouldn't serve for, but that may change.
	 */
	private ConcurrentHashMap<String, Boolean> gifts = null;
	
	/**
	 * Do we celebrate the holiday?
	 */
	private boolean celebrateChristmas = false;
	
	/**
	 * A list of all the units we are handing out as gifts.  Thread-safe.
	 * 
	 */
	private CopyOnWriteArrayList<String> christmasList = null;
	
	/**
	 * What method are we using to hand out gifts?
	 */
	private int unitMethod;
	
	/**
	 * How many units are handed out in the case of XTotal and XofEach methods
	 */
	private int numberOfUnits = 0;
	
	/**
	 * Where do we store the recipients' list
	 */
	private String giftRecipientsFile = "./campaign/giftRecipients.txt";
	
	public static final int UNIT_METHOD_ONEOFEACH = 0;
	public static final int UNIT_METHOD_XOFEACH = 1;
	public static final int UNIT_METHOD_XTOTAL = 2;
	
	/**
	 * Exists solely to defeat instantiation.
	 * @return
	 */
	protected ChristmasHandler() {
		celebrateChristmas = CampaignMain.cm.getBooleanConfig("Celebrate_Christmas");
		if(!celebrateChristmas) {
			return;
		}
		gifts = new ConcurrentHashMap<String, Boolean>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			startDate = sdf.parse(CampaignMain.cm.getConfig("Christmas_StartDate"));
			endDate = sdf.parse(CampaignMain.cm.getConfig("Christmas_EndDate"));
			} catch (ParseException e) {
			MWLogger.errLog(e);
		}
		Date today = new Date();
		if(today.after(startDate) && today.before(endDate)) {
			isChristmasSeason = true;
		} else {
			isChristmasSeason = false;
		}
		
		// Maybe it was manually started?
		if(CampaignMain.cm.getBooleanConfig("Christmas_ManuallyStarted")) {
			isChristmasSeason = true;
		}
		
		// Populate the Christmas List
		populateChristmasList(CampaignMain.cm.getConfig("Christmas_List"));
		
		if(CampaignMain.cm.getBooleanConfig("Christmas_Units_Method_OneOfEach")) {
			unitMethod = UNIT_METHOD_ONEOFEACH;
		} else if (CampaignMain.cm.getBooleanConfig("Christmas_Units_Method_XOfEach")) {
			unitMethod = UNIT_METHOD_XOFEACH;
		} else if (CampaignMain.cm.getBooleanConfig("Christmas_Units_Method_XTotal")) {
			unitMethod = UNIT_METHOD_XTOTAL;
		}
		numberOfUnits = CampaignMain.cm.getIntegerConfig("Christmas_Units_X");
		loadGiftList();
	}
	
	/**
	 * Loads the list of Christmas gifts
	 * @param list a $-delimited list of units.
	 */
	public void populateChristmasList(String list) {
		ArrayList<String>al = new ArrayList<String>();
		String[] arr = list.split("\\$");
		for(int i = 0; i < arr.length; i++) {
			al.add(arr[i]);
		}
		christmasList = new CopyOnWriteArrayList<String>(al);
	}
	
	/**
	 * Instantiates the ChristmasHandler if it is not yet instantiated.  Returns
	 * the ChristmasHandler if it is
	 * 
	 * @return the ChristmasHandler
	 */
	public static ChristmasHandler getInstance() {
		if (handler == null) {
			handler = new ChristmasHandler();
		}
		return handler;
	}

	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return the endDate
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
	/**
	 * Reschedules Christmas season.  Called when start or end dates are changed.
	 */
	public void reschedule() {
		if(!doWeCelebrateChristmas()) {
			return;
		}
		StartChristmasJob.stop();
		EndChristmasJob.stop();
		schedule();
	}
	
	/**
	 * Set the starting and ending dates for the Christmas season
	 * @param start The start date of the season
	 * @param end   The end date of the season
	 */
	private void schedule(Date start, Date end) {
		setStartDate(start);
		setEndDate(end);
		StartChristmasJob.submit();
		EndChristmasJob.submit();
	}
	
	/**
	 * Set the starting and ending dates for the Christmas season using CampaignConfig settings
	 */
	public void schedule() {
		if(!doWeCelebrateChristmas()) {
			return;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date start = new Date();
		Date end = new Date();
		try {
			start = sdf.parse(CampaignMain.cm.getConfig("Christmas_StartDate"));
			
			end = sdf.parse(CampaignMain.cm.getConfig("Christmas_EndDate"));
		} catch (ParseException e) {
			MWLogger.errLog(e);
		}
		schedule(start, end);
	}
	
	/**
	 * A method to determine if we are in the Christmas season or not
	 * @return true if Christmas season has started
	 */
	public boolean isItChristmas() {
		return isChristmasSeason;
	}
	
	/**
	 * Start the Christmas season
	 */
	public void startChristmas() {
		if(!doWeCelebrateChristmas()) {
			return;
		}
		isChristmasSeason = true;
		CampaignMain.cm.getConfig().setProperty("Christmas_ManuallyStarted", "true");
	}
	
	/**
	 * End the Christmas season and clean up
	 */
	public void endChristmas() {
		isChristmasSeason = false;
		CampaignMain.cm.getConfig().setProperty("Christmas_ManuallyStarted", "false");
		
		// Clear the gift recipients so they are not penalized next Christmas season
		File file = new File(giftRecipientsFile);
		gifts.clear();
		if(file.exists()) {
			file.delete();
		}
		
	}
	
	/**
	 * Check if a user has received his gifts
	 */
	public boolean userHasReceivedGifts(String userName) {
		if(!doWeCelebrateChristmas()) {
			return true;
		}
		if(CampaignMain.cm.getPlayer(userName).getMyHouse().isNewbieHouse()) {
			// I suspect that Christmas Units will mess up a defection from Solaris
			return true;
		}
		if(gifts != null && gifts.containsKey(userName.toLowerCase())) {
			return gifts.get(userName.toLowerCase());
		}
		return false;
	}

	/**
	 * @return celebrateChristmas
	 */
	public boolean doWeCelebrateChristmas() {
		return celebrateChristmas;
	}

	/**
	 * @param celebrateChristmas the celebrateChristmas to set
	 */
	public void setCelebrateChristmas(boolean celebrateChristmas) {
		this.celebrateChristmas = celebrateChristmas;
	}
	
	/**
	 * Send units to a player
	 * @param p the player in question
	 */
	public void sendChristmasGifts(SPlayer p) {
		ArrayList<String> unitList = new ArrayList<String>(); 
		if(unitMethod == UNIT_METHOD_ONEOFEACH) {
			Iterator<String> iterator = christmasList.iterator();
			while(iterator.hasNext()) {
				unitList.add(iterator.next());
			}
		} else if (unitMethod == UNIT_METHOD_XOFEACH) {
			Iterator<String> iterator = christmasList.iterator();
			while(iterator.hasNext()) {
				String unitName = iterator.next();
				for (int i = 0; i < numberOfUnits; i++) {
					unitList.add(unitName);
				}
			}
		} else if (unitMethod == UNIT_METHOD_XTOTAL) {
			for (int i = 0; i < numberOfUnits; i++) {
				unitList.add(getRandomUnitFileName());
			}
		}
		for(String s : unitList) {
			CampaignMain.cm.toUser("AM:Under the tree, you find a " + s, p.getName(), true);
			SUnit u = getUnit(s);
			p.addUnit(u, true);
		}
		setUserReceivedGifts(p);
	}
	
	/**
	 * Note that the user has received his gifts
	 * @param p the user in question
	 */
	private void setUserReceivedGifts(SPlayer p) {
		if(gifts.containsKey(p.getName().toLowerCase())) {
			return;
		}
		gifts.put(p.getName().toLowerCase(), true);
		saveGiftList();
	}
	
	/*
	 * Loads the list of gift recipients from disk
	 */
	private void loadGiftList() {
		Scanner scanner = null;
		try {
			scanner = new Scanner(new File(giftRecipientsFile));
			gifts = new ConcurrentHashMap<String, Boolean>();
			while (scanner.hasNextLine()) {
				gifts.put(scanner.nextLine().toLowerCase(), true);
			}
		} catch (FileNotFoundException e) {
			MWLogger.errLog(e);
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}
	
	/**
	 * Saves the list of gift recipients to disk
	 */
	private void saveGiftList() {
		File file = new File(giftRecipientsFile);
		if(file.exists()) {
			file.delete();
		}
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter( new FileWriter(giftRecipientsFile));
			for (String s : gifts.keySet()) {
				writer.write(s.toLowerCase() + "\n");
			}
		} catch (IOException e) {
			MWLogger.errLog(e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					MWLogger.errLog(e);
				}
			}
		}
	}
	
	/**
	 * Instantiates a unit
	 * @param unitFileName the unit to be created
	 * @return SUnit the unit
	 */
	private SUnit getUnit(String unitFileName) {
		SUnit u;
		String fluff = "Merry Christmas!";
		int gunnery = 4;
		int piloting = 5;
		String skillTokens = "";
		
		u = SUnit.create(unitFileName, fluff, gunnery, piloting, null, skillTokens);
		u.setChristmasUnit(true);
		return u;
	}
	
	/**
	 * Gets a random unit file name from the Christmas list
	 * @return the unit file name
	 */
	private String getRandomUnitFileName() {
		int size = christmasList.size();
		return christmasList.get(CampaignMain.cm.getR().nextInt(size));
	}

	/**
	 * @return the unitMethod
	 */
	public int getUnitMethod() {
		return unitMethod;
	}

	/**
	 * @param unitMethod the unitMethod to set
	 */
	public void setUnitMethod(int unitMethod) {
		this.unitMethod = unitMethod;
	}

	/**
	 * @return the numberOfUnits
	 */
	public int getNumberOfUnits() {
		return numberOfUnits;
	}

	/**
	 * @param numberOfUnits the numberOfUnits to set
	 */
	public void setNumberOfUnits(int numberOfUnits) {
		this.numberOfUnits = numberOfUnits;
	}
}
