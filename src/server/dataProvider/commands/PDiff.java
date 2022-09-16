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

package server.dataProvider.commands;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import common.CampaignData;
import common.House;
import common.Planet;
import common.util.BinWriter;
import server.campaign.CampaignMain;
import server.campaign.data.TimeUpdatePlanet;
import server.dataProvider.ServerCommand;

/**
 * Request for data diff.
 * 
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class PDiff implements ServerCommand {

	public void execute(Date timestamp, BinWriter out, CampaignData data)
			throws Exception {

	    boolean fullUpdate = false;
		// System.err.println("PDiff Timestamp: "+timestamp.toString());
		if (timestamp == null || CampaignMain.cm.getHousePlanetUpdate().compareTo(timestamp)  > 0 ) {
			// make a date far in the past to retrieve all..
			timestamp = new Date(-1);
			fullUpdate = true;
		}
		ArrayList<House> houses = new ArrayList<House>();
		for (House e : data.getAllHouses()) {
				houses.add(e);
		}
		data.binHousesOut(houses, out);
		
		ArrayList<Planet> planets = new ArrayList<Planet>();
		synchronized (data.getAllPlanets()) {

			for (Planet e : data.getAllPlanets()) {
				TimeUpdatePlanet tPlanet = (TimeUpdatePlanet) e;
				// System.err.println("Planet time:
				// "+tPlanet.getLastChanged().toString());
				if (tPlanet.getLastChanged() != null
						&& tPlanet.getLastChanged().compareTo(timestamp) > 0) {
					planets.add(e);
					// ids.add(new Integer(e.getId()));
				}
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			out.println(sdf.format(new Date()), "lasttimestamp");
			out.println(fullUpdate, "FullUpdate");
			
			data.binPlanetsOut(planets, out);

		}
	}
}
