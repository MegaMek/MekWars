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

package server.campaign.data;

import java.util.Date;

import common.House;



/**
 * Adds the ability to trace the last change time to a planet.
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class TimeUpdateHouse extends House {

    public TimeUpdateHouse(int id) {
        super(id);
    }

    /**
     * Constructor used for serialization
     */
    public TimeUpdateHouse() {
    }
    
    /**
     * The time at which this data was changed last.
     */
    private Date timestamp;

    /**
     * @return Returns the timestamp which this data was last changed.
     */
    public Date getLastChanged() {
        return timestamp;
    }
    
    /**
     * Mark the data as updated.
     */
    public void updated() {
    	timestamp = new Date();
    }
    
    /**
     * Writing itself into a stream
     *
    @Override
	public void binOut(TreeWriter out) {
        super.binOut(out);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        out.write(sdf.format(getLastChanged()),"lastChanged");
    }

    /**
     * Reading itself from a stream
     *
    @Override
	public void binIn(TreeReader in, CampaignData data) throws IOException {
        super.binIn(in, data);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
			timestamp = sdf.parse(in.readString("lastChanged"));
		} catch (ParseException e) {
			throw new IOException("corrupted date format");
		}
    }*/
}
