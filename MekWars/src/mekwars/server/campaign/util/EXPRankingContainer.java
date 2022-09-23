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

package server.campaign.util;

import server.campaign.SHouse;
import server.campaign.SmallPlayer;

public class EXPRankingContainer implements Comparable<Object> {
	
	SmallPlayer player;
	public EXPRankingContainer(SmallPlayer p) {
		player = p;
	}
	
	public SHouse getMyHouse() {
		return player.getMyHouse();
	}
	
	public String getFluffText() {
		return player.getFluffText();
	}
	
	public int getExperience() {
		return player.getExperience();
	}
	
	public String getName() {
		return player.getName();
	}
	
	public int compareTo(Object o) {
		EXPRankingContainer p = (EXPRankingContainer)o;
		if (this.getExperience() > p.getExperience())
			return 1;
		else if (this.getExperience() < p.getExperience())
			return -1;
		return p.getName().compareTo(this.getName());
	}
}
