/*
 * MekWars - Copyright (C) 2005 
 * 
 * original author: nmorris (urgru@users.sourceforge.net)
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

package server.campaign.market2;

import server.campaign.SUnit;

/**
 * Interface which defines the methods needed by an actor
 * who is able to buy units from the market. At this time,
 * only the SPlayer should be an IBuyer; however, buying may
 * be expanded to SHouse once leadership is implemented.
 */
public interface IBuyer {
	
	public abstract void addMoney(int amountToAdd);
	public abstract int getMoney();
	public abstract String addUnit(SUnit toAdd, boolean isNew, boolean sendUpdate);
	public abstract boolean isHuman();
	
}