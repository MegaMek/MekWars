/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

import java.util.StringTokenizer;

import jakarta.annotation.Nonnull;
import megamek.codeUtilities.MathUtility;

/**
 *
 * @author McWizard
 *       <p>
 *       Represents a MechStats Entry
 */
public class MekStatistics implements Cloneable, Comparable<MekStatistics> {
    private Long ID;
    private String mekFileName;
    private int mekSize;
    private int gamesWon = 0;
    private int gamesPlayed = 0;
    private int timesScrapped = 0;
    private long lastTimeUpdated = 0;
    private int OriginalBV = 0;
    private int currentGamesWon = 0;
    private int currentGamesPlayed = 0;
    private int timesDestroyed = 0;
    private int DBID = 0;

    public MekStatistics(String Filename, int mekSize) {
        this.mekFileName = Filename;
        this.mekSize = mekSize;
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.timesDestroyed = 0;
    }

    public MekStatistics(String s) {
        StringTokenizer stringTokenizer = new StringTokenizer(s, "*");
        this.mekFileName = stringTokenizer.nextToken();
        this.mekSize = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
        this.gamesWon = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
        this.gamesPlayed = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
        this.timesScrapped = MathUtility.parseInt(stringTokenizer.nextToken(), 0);

        if (stringTokenizer.hasMoreElements()) {
            this.lastTimeUpdated = Long.parseLong(stringTokenizer.nextToken());
        }

        if (stringTokenizer.hasMoreElements()) {
            this.currentGamesWon = Integer.parseInt(stringTokenizer.nextToken());
        }

        if (stringTokenizer.hasMoreElements()) {
            this.currentGamesPlayed = Integer.parseInt(stringTokenizer.nextToken());
        }

        if (stringTokenizer.hasMoreElements()) {
            this.OriginalBV = Integer.parseInt(stringTokenizer.nextToken());
        }

        if (stringTokenizer.hasMoreElements()) {
            this.timesDestroyed = Integer.parseInt(stringTokenizer.nextToken());
        }
    }

    @Override
    public String toString() {
        String result = "";
        result += this.mekFileName;
        result += "*";
        result += this.mekSize;
        result += "*";
        result += this.gamesWon;
        result += "*";
        result += this.gamesPlayed;
        result += "*";
        result += this.timesScrapped;
        result += "*";
        result += this.lastTimeUpdated;
        result += "*";
        result += this.currentGamesWon;
        result += "*";
        result += this.currentGamesPlayed;
        result += "*";
        result += this.OriginalBV;
        result += "*";
        result += this.timesDestroyed;
        return result;
    }

    public int getDBId() {
        return this.DBID;
    }

    public void setDBId(int ID) {
        this.DBID = ID;
    }

    public int getBV() {
        int baseBV = 0;


        currentGamesPlayed = gamesPlayed;
        currentGamesWon = gamesWon;

        // OK, we want the BV to modify slowly through each 100 battles
        // starting at the actual unit BV, or the ModBV
        // and varying by the stated Percentage from the campaignconfig.txt

        baseBV = getOriginalBV();

        return baseBV;

    }

    /**
     * @return Integer
     *
     * @hibernate.property
     */
    public int getOriginalBV() {
        if (OriginalBV == 0 && getMekFileName() != null) {
            // make a MegaMek entity and get it's BV
            OriginalBV = server.campaign.SUnit.loadMech(getMekFileName()).calculateBattleValue();
        }
        return OriginalBV;
    }

    public void setOriginalBV(int OriginalBV) {
        this.OriginalBV = OriginalBV;
    }

    public String getMekFileName() {
        return mekFileName;
    }

    public void setMekFileName(String mekFileName) {
        this.mekFileName = mekFileName;
    }

    public void addStats(int gamesPlayed, int gamesWon, int originalBV) {
        this.setGamesPlayed(getGamesPlayed() + gamesPlayed);
        this.setGamesWon(getGamesWon() + gamesWon);

        if (getOriginalBV() == 0) {
            setOriginalBV(originalBV);
        }
    }

    /**
     * @return Integer
     *
     * @hibernate.property
     */
    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        //Update the last time this int was changed
        if (gamesPlayed != this.gamesPlayed) {
            this.lastTimeUpdated = System.currentTimeMillis();
        }

        this.gamesPlayed = gamesPlayed;
    }

    /**
     * @return Integer
     *
     * @hibernate.property
     */
    public int getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public int compareTo(@Nonnull MekStatistics mekStatistics) {
        return (this.getMekFileName().compareTo(mekStatistics.getMekFileName()));
    }

    /**
     * @return Integer
     *
     * @hibernate.property
     */
    public int getMekSize() {
        return mekSize;
    }

    public void setMekSize(int mekSize) {
        this.mekSize = mekSize;
    }

    /**
     * @return Integer
     *
     * @hibernate.property
     */
    public int getTimesScrapped() {
        return timesScrapped;
    }

    public void setTimesScrapped(int timesScrapped) {
        this.timesScrapped = timesScrapped;
    }

    public int getTimesDestroyed() {
        return timesDestroyed;
    }

    public void setTimesDestroyed(int timesDestroyed) {
        this.timesDestroyed = timesDestroyed;
    }

    /**
     * @return Integer
     *
     * @hibernate.property
     */
    public long getLastTimeUpdated() {
        return lastTimeUpdated;
    }

    public void setLastTimeUpdated(long lastTimeUpdated) {
        this.lastTimeUpdated = lastTimeUpdated;
    }

    /**
     * @return Integer
     */
    public Long getID() {
        return ID;
    }

    public void setID(Long id) {
        this.ID = id;
    }

    /**
     * @return Integer
     *
     * @hibernate.property
     */
    public int getCurrentGamesPlayed() {
        return currentGamesPlayed;
    }

    public void setCurrentGamesPlayed(int currentGamesPlayed) {
        this.currentGamesPlayed = currentGamesPlayed;
    }

    /**
     * @return Integer
     *
     * @hibernate.property
     */
    public int getCurrentGamesWon() {
        return currentGamesWon;
    }

    public void setCurrentGamesWon(int currentGamesWon) {
        this.currentGamesWon = currentGamesWon;
    }

    @Override
    public MekStatistics clone() {
        try {
            MekStatistics clone = (MekStatistics) super.clone();
            clone.setID(this.ID);
            clone.setMekFileName(this.mekFileName);
            clone.setMekSize(this.mekSize);
            clone.setGamesWon(this.gamesWon);
            clone.setGamesPlayed(this.gamesPlayed);
            clone.setTimesScrapped(this.timesScrapped);
            clone.setLastTimeUpdated(this.lastTimeUpdated);
            clone.setOriginalBV(this.OriginalBV);
            clone.setCurrentGamesWon(this.currentGamesWon);
            clone.setCurrentGamesPlayed(this.currentGamesPlayed);
            clone.setTimesDestroyed(this.timesDestroyed);
            clone.setDBId(this.DBID);

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
