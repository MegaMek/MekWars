/*
 * MekWars - Copyright (C) 2007 
 * 
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
import common.Equipment;
import common.util.BinWriter;
import server.campaign.CampaignMain;
import server.dataProvider.ServerCommand;

public class BMSetting implements ServerCommand {

    /**
     * @see server.dataProvider.ServerCommand#execute(java.util.Date,
     *      java.io.PrintWriter, common.CampaignData)
     */
    public void execute(Date timestamp, BinWriter out, CampaignData data)
            throws Exception {
    	out.println(CampaignMain.cm.getBlackMarketEquipmentTable().size(), "BMSetting");
    	
        for ( String key : CampaignMain.cm.getBlackMarketEquipmentTable().keySet() ){
        	Equipment bme = CampaignMain.cm.getBlackMarketEquipmentTable().get(key);
        	
            out.println(key,"BMSetting");
            out.println(bme.getMinCost(),"BMSetting");
            out.println(bme.getMaxCost(),"BMSetting");
            out.println(bme.getMinProduction(),"BMSetting");
            out.println(bme.getMaxProduction(),"BMSetting");
        }
    }
}
