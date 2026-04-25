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

package mekwars.server.dataProvider.commands;

import common.CampaignData;
import common.util.BinWriter;
import common.util.MWLogger;

//import java.io.BufferedReader;
//import java.io.FileInputStream;
//import java.io.InputStreamReader;
//import server.campaign.CampaignMain;

/**
 * Retrieve the MD5 of the current campaignconfig file.
 */
public class ConfigTimestamp implements server.dataProvider.ServerCommand {

    public void execute(java.util.Date timestamp, BinWriter out, CampaignData data) throws Exception {

        String serverConfigTimestamp = "-1";
        java.io.File serverConfig = new java.io.File("./data/campaignconfig.txt");

        if (serverConfig.exists()) {

            try {
                java.io.FileInputStream in = new java.io.FileInputStream("./data/campaignconfig.txt");
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in));
                String tempTime = br.readLine();
                br.close();
                in.close();

                serverConfigTimestamp = tempTime.substring(11);//remove "#Timestamp="
            } catch (Exception e) {
                MWLogger.infoLog("Error reading first line from campaignconfig.txt");
            }
        } else {
            MWLogger.infoLog("campaignconfig.txt didn't exist. returning ficticious timestamp to requesting client.");
        }

        out.println(serverConfigTimestamp, "ConfigTimestamp");
    }
}
