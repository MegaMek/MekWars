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

public class EXPRankingContainer implements Comparable<Object> {

    server.campaign.SmallPlayer player;

    public EXPRankingContainer(server.campaign.SmallPlayer p) {
        player = p;
    }

    public server.campaign.SHouse getMyHouse() {
        return player.getMyHouse();
    }

    public String getFluffText() {
        return player.getFluffText();
    }

    public int compareTo(Object o) {
        mekwars.server.campaign.util.EXPRankingContainer p = (mekwars.server.campaign.util.EXPRankingContainer) o;
        if (this.getExperience() > p.getExperience()) {return 1;} else if (this.getExperience() < p.getExperience()) {
            return -1;
        }
        return p.getName().compareTo(this.getName());
    }

    public int getExperience() {
        return player.getExperience();
    }

    public String getName() {
        return player.getName();
    }
}
