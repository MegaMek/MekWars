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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Date;

import common.CampaignData;
import common.util.BinWriter;
import server.campaign.CampaignMain;
import server.dataProvider.ServerCommand;

/**
 * Retrieve all planet information (if the data cache is lost at client side)
 * 
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class ServerConfig implements ServerCommand {

    /**
     * @see server.dataProvider.ServerCommand#execute(java.util.Date,
     *      java.io.PrintWriter, common.CampaignData)
     */
    public void execute(Date timestamp, BinWriter out, CampaignData data)
            throws Exception {
        FileInputStream configFile = new FileInputStream(CampaignMain.cm.getServer().getConfigParam("CAMPAIGNCONFIG"));
        BufferedReader config = new BufferedReader(new InputStreamReader(configFile));
         
        while (config.ready()) {
            out.println(config.readLine(),"ConfigLine");
        }
        
        config.close();
    }
}
