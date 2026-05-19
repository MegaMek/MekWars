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

package mekwars.server.campaign.util;

import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;
import server.campaign.util.scheduler.EndChristmasJob;
import server.campaign.util.scheduler.StartChristmasJob;

/**
 * A class to handle scheduling of distribution of meks during the Christmas season.  Historically, we've had to hand
 * them out manually.
 *
 * @author Spork
 * @version 2016.10.26
 */
public class ChristmasHandler {
    public static final int UNIT_METHOD_ONEOFEACH = 0;
    public static final int UNIT_METHOD_XOFEACH = 1;
    public static final int UNIT_METHOD_XTOTAL = 2;
    private static mekwars.server.campaign.util.ChristmasHandler handler;
    /**
     * Start date of the Christmas season
     */
    private java.util.Date startDate;
    /**
     * End date of the Christmas season
     */
    private java.util.Date endDate;
    /**
     * Whether or not it is the Christmas Season
     */
    private boolean isChristmasSeason = false;
    /**
     * A collection containing a list of names of folks who have received their gifts already
     * <p>
     * At some point, I may make this just a list, rather than a map.  Right now, I'm not really using it for anything
     * that a list wouldn't serve for, but that may change.
     */
    private java.util.concurrent.ConcurrentHashMap<String, Boolean> gifts = null;
    /**
     * Do we celebrate the holiday?
     */
    private boolean celebrateChristmas = false;
    /**
     * A list of all the units we are handing out as gifts.  Thread-safe.
     *
     */
    private java.util.concurrent.CopyOnWriteArrayList<String> christmasList = null;
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

    /**
     * Exists solely to defeat instantiation.
     *
     * @return
     */
    protected ChristmasHandler() {
        celebrateChristmas = CampaignMain.campaignMain.getBooleanConfig("Celebrate_Christmas");
        if (!celebrateChristmas) {
            return;
        }
        gifts = new java.util.concurrent.ConcurrentHashMap<String, Boolean>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        try {
            startDate = sdf.parse(CampaignMain.campaignMain.getConfig("Christmas_StartDate"));
            endDate = sdf.parse(CampaignMain.campaignMain.getConfig("Christmas_EndDate"));
        } catch (java.text.ParseException e) {
            MWLogger.errLog(e);
        }
        java.util.Date today = new java.util.Date();
        if (today.after(startDate) && today.before(endDate)) {
            isChristmasSeason = true;
        } else {
            isChristmasSeason = false;
        }

        // Maybe it was manually started?
        if (CampaignMain.campaignMain.getBooleanConfig("Christmas_ManuallyStarted")) {
            isChristmasSeason = true;
        }

        // Populate the Christmas List
        populateChristmasList(CampaignMain.campaignMain.getConfig("Christmas_List"));

        if (CampaignMain.campaignMain.getBooleanConfig("Christmas_Units_Method_OneOfEach")) {
            unitMethod = UNIT_METHOD_ONEOFEACH;
        } else if (CampaignMain.campaignMain.getBooleanConfig("Christmas_Units_Method_XOfEach")) {
            unitMethod = UNIT_METHOD_XOFEACH;
        } else if (CampaignMain.campaignMain.getBooleanConfig("Christmas_Units_Method_XTotal")) {
            unitMethod = UNIT_METHOD_XTOTAL;
        }
        numberOfUnits = CampaignMain.campaignMain.getIntegerConfig("Christmas_Units_X");
        loadGiftList();
    }

    /**
     * Instantiates the ChristmasHandler if it is not yet instantiated.  Returns the ChristmasHandler if it is
     *
     * @return the ChristmasHandler
     */
    public static mekwars.server.campaign.util.ChristmasHandler getInstance() {
        if (handler == null) {
            handler = new mekwars.server.campaign.util.ChristmasHandler();
        }
        return handler;
    }

    /**
     * Loads the list of Christmas gifts
     *
     * @param list a $-delimited list of units.
     */
    public void populateChristmasList(String list) {
        java.util.ArrayList<String> al = new java.util.ArrayList<String>();
        String[] arr = list.split("\\$");
        for (int i = 0; i < arr.length; i++) {
            al.add(arr[i]);
        }
        christmasList = new java.util.concurrent.CopyOnWriteArrayList<String>(al);
    }

    /*
     * Loads the list of gift recipients from disk
     */
    private void loadGiftList() {
        java.util.Scanner scanner = null;
        try {
            scanner = new java.util.Scanner(new java.io.File(giftRecipientsFile));
            gifts = new java.util.concurrent.ConcurrentHashMap<String, Boolean>();
            while (scanner.hasNextLine()) {
                gifts.put(scanner.nextLine().toLowerCase(), true);
            }
        } catch (java.io.FileNotFoundException e) {
            MWLogger.errLog(e);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    /**
     * @return the startDate
     */
    public java.util.Date getStartDate() {
        return startDate;
    }

    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(java.util.Date startDate) {
        this.startDate = startDate;
    }

    /**
     * @return the endDate
     */
    public java.util.Date getEndDate() {
        return endDate;
    }

    /**
     * @param endDate the endDate to set
     */
    public void setEndDate(java.util.Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Reschedules Christmas season.  Called when start or end dates are changed.
     */
    public void reschedule() {
        if (!doWeCelebrateChristmas()) {
            return;
        }
        StartChristmasJob.stop();
        EndChristmasJob.stop();
        schedule();
    }

    /**
     * @return celebrateChristmas
     */
    public boolean doWeCelebrateChristmas() {
        return celebrateChristmas;
    }

    /**
     * Set the starting and ending dates for the Christmas season using CampaignConfig settings
     */
    public void schedule() {
        if (!doWeCelebrateChristmas()) {
            return;
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        java.util.Date start = new java.util.Date();
        java.util.Date end = new java.util.Date();
        try {
            start = sdf.parse(CampaignMain.campaignMain.getConfig("Christmas_StartDate"));

            end = sdf.parse(CampaignMain.campaignMain.getConfig("Christmas_EndDate"));
        } catch (java.text.ParseException e) {
            MWLogger.errLog(e);
        }
        schedule(start, end);
    }

    /**
     * Set the starting and ending dates for the Christmas season
     *
     * @param start The start date of the season
     * @param end   The end date of the season
     */
    private void schedule(java.util.Date start, java.util.Date end) {
        setStartDate(start);
        setEndDate(end);
        StartChristmasJob.submit();
        EndChristmasJob.submit();
    }

    /**
     * A method to determine if we are in the Christmas season or not
     *
     * @return true if Christmas season has started
     */
    public boolean isItChristmas() {
        return isChristmasSeason;
    }

    /**
     * Start the Christmas season
     */
    public void startChristmas() {
        if (!doWeCelebrateChristmas()) {
            return;
        }
        isChristmasSeason = true;
        CampaignMain.campaignMain.getConfig().setProperty("Christmas_ManuallyStarted", "true");
    }

    /**
     * End the Christmas season and clean up
     */
    public void endChristmas() {
        isChristmasSeason = false;
        CampaignMain.campaignMain.getConfig().setProperty("Christmas_ManuallyStarted", "false");

        // Clear the gift recipients so they are not penalized next Christmas season
        java.io.File file = new java.io.File(giftRecipientsFile);
        gifts.clear();
        if (file.exists()) {
            file.delete();
        }

    }

    /**
     * Check if a user has received his gifts
     */
    public boolean userHasReceivedGifts(String userName) {
        if (!doWeCelebrateChristmas()) {
            return true;
        }
        if (CampaignMain.campaignMain.getPlayer(userName).getMyHouse().isNewbieHouse()) {
            // I suspect that Christmas Units will mess up a defection from Solaris
            return true;
        }
        if (gifts != null && gifts.containsKey(userName.toLowerCase())) {
            return gifts.get(userName.toLowerCase());
        }
        return false;
    }

    /**
     * @param celebrateChristmas the celebrateChristmas to set
     */
    public void setCelebrateChristmas(boolean celebrateChristmas) {
        this.celebrateChristmas = celebrateChristmas;
    }

    /**
     * Send units to a player
     *
     * @param p the player in question
     */
    public void sendChristmasGifts(server.campaign.SPlayer p) {
        java.util.ArrayList<String> unitList = new java.util.ArrayList<String>();
        if (unitMethod == UNIT_METHOD_ONEOFEACH) {
            java.util.Iterator<String> iterator = christmasList.iterator();
            while (iterator.hasNext()) {
                unitList.add(iterator.next());
            }
        } else if (unitMethod == UNIT_METHOD_XOFEACH) {
            java.util.Iterator<String> iterator = christmasList.iterator();
            while (iterator.hasNext()) {
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
        for (String s : unitList) {
            CampaignMain.campaignMain.toUser("AM:Under the tree, you find a " + s, p.getName(), true);
            server.campaign.SUnit u = getUnit(s);
            p.addUnit(u, true);
        }
        setUserReceivedGifts(p);
    }

    /**
     * Gets a random unit file name from the Christmas list
     *
     * @return the unit file name
     */
    private String getRandomUnitFileName() {
        int size = christmasList.size();
        return christmasList.get(CampaignMain.campaignMain.getRandom().nextInt(size));
    }

    /**
     * Instantiates a unit
     *
     * @param unitFileName the unit to be created
     *
     * @return SUnit the unit
     */
    private server.campaign.SUnit getUnit(String unitFileName) {
        server.campaign.SUnit u;
        String fluff = "Merry Christmas!";
        int gunnery = 4;
        int piloting = 5;
        String skillTokens = "";

        u = server.campaign.SUnit.create(unitFileName, fluff, gunnery, piloting, null, skillTokens);
        u.setChristmasUnit(true);
        return u;
    }

    /**
     * Note that the user has received his gifts
     *
     * @param p the user in question
     */
    private void setUserReceivedGifts(server.campaign.SPlayer p) {
        if (gifts.containsKey(p.getName().toLowerCase())) {
            return;
        }
        gifts.put(p.getName().toLowerCase(), true);
        saveGiftList();
    }

    /**
     * Saves the list of gift recipients to disk
     */
    private void saveGiftList() {
        java.io.File file = new java.io.File(giftRecipientsFile);
        if (file.exists()) {
            file.delete();
        }
        java.io.BufferedWriter writer = null;
        try {
            writer = new java.io.BufferedWriter(new java.io.FileWriter(giftRecipientsFile));
            for (String s : gifts.keySet()) {
                writer.write(s.toLowerCase() + "\n");
            }
        } catch (java.io.IOException e) {
            MWLogger.errLog(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (java.io.IOException e) {
                    MWLogger.errLog(e);
                }
            }
        }
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
