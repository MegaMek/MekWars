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

/**
 * SmallPlayer is a lightweight player file which is NOT paged out of memory. Mirrors basic player information which is
 * needed when players are out of memoy (XP, Rating, Name) in order to publish complete rankings and give accurate
 * faction size information.
 *
 * @author Nathan Morris (12/9/04)
 */

//imports
package mekwars.server.campaign;

public final class SmallPlayer implements Comparable<Object> {

    //VARIABLES
    private int experience = 0;
    private long lastonline = 0;
    private double rating = 0;
    private String name = "";
    private String fluff = "";
    private SHouse faction = null;

    //CONSTRUCTORS
    public SmallPlayer() {
        //empty
    }

    public SmallPlayer(int i, long l, double d, String s, String f, SHouse h) {
        experience = i;
        lastonline = l;
        rating = d;
        name = s;
        fluff = f;
        faction = h;
    }

    //METHODS
    public int getExperience() {
        return experience;
    }

    public void setExperience(int i) {
        experience = i;
    }

    public long getLastOnline() {
        return lastonline;
    }

    public void setLastOnline(long l) {
        lastonline = l;
    }

    public SHouse getMyHouse() {
        return faction;
    }

    public String getFluffText() {
        return fluff;
    }

    public void setFluffText(String s) {
        fluff = s;
    }

    @Override
    public boolean equals(Object o) {
        try {
            mekwars.server.campaign.SmallPlayer smallp = (mekwars.server.campaign.SmallPlayer) o;
            if (smallp.getName().equals(this.getName())) {return true;}
            return false;
        } catch (Exception ex) {
            return false;
        }
    }//end equals(Object o)

    public String getName() {
        return name;
    }

    public void setName(String s) {
        name = s;
    }

    /*
     * Comparable - used in Statistics to construct ranking
     * pages. If ratings are equal, sort in alpha order.
     */
    public int compareTo(Object o) {
        mekwars.server.campaign.SmallPlayer p = (mekwars.server.campaign.SmallPlayer) o;
        if (this.getRating() > p.getRating()) {return 1;} else if (this.getRating() < p.getRating()) {return -1;}
        return p.getName().compareTo(this.getName());
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double d) {
        rating = d;
    }

    //rounded rating for HTML output. copied from splayer.
    public double getRatingRounded() {
        double r = getRating();
        java.math.BigDecimal bd = new java.math.BigDecimal(r);
        bd = bd.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
        r = bd.doubleValue();
        return r;
    }


}//end SmallPlayer class
