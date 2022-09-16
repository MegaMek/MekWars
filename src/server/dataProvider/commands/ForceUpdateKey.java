/*
 * MekWars - Copyright (C) 2004 
 * 
 * Original Author: nmorris (urgru@users.sourceforge.net)
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

import java.util.Date;

import common.CampaignData;
import common.util.BinWriter;
import server.campaign.CampaignMain;
import server.dataProvider.ServerCommand;

/**
 * Retrieve the MD5 of the current campaignconfig file.
 */
public class ForceUpdateKey implements ServerCommand {

    public void execute(Date timestamp, BinWriter out, CampaignData data) throws Exception {
    	
        out.println(CampaignMain.cm.getConfig("ForceUpdateKey"), "ForceUpdateKey");
    }

}
