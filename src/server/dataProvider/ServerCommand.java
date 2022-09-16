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

package server.dataProvider;

import java.util.Date;

import common.CampaignData;
import common.util.BinWriter;



/**
 * Executes an command for the dataProvider-Server.
 * 
 * @author Imi (immanuel.scholz@gmx.de)
 */
public interface ServerCommand {
    
    /**
     * Executes the command. 
     * 
     * @param timestamp The date at which the client retrieved data last.
     * @param out The output to write the result for the client to.
     */
    public void execute(Date timestamp, BinWriter out, CampaignData data)
            throws Exception;
}
