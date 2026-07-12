/*
 * MekWars - Copyright (C) 2005
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
import megamek.logging.MMLogger;

/**
 * Command which calcaulates and returns the timestamp of the current OpList.txt file.
 */
public class OpListTimestamp implements server.dataProvider.ServerCommand {
    private static final MMLogger LOGGER = MMLogger.create(OpListTimestamp.class);

    public void execute(java.util.Date timestamp, BinWriter out, CampaignData data) throws Exception {

        String oplistTimestamp = "-1";
        java.io.File opList = new java.io.File("./data/operations/OpList.txt");
        if (opList.exists()) {

            try {
                java.io.FileInputStream in = new java.io.FileInputStream("./data/operations/OpList.txt");
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in));
                String tempTime = br.readLine();
                br.close();
                in.close();

                oplistTimestamp = tempTime.substring(11);//remove "#Timestamp="
            } catch (Exception e) {
                LOGGER.info("Error reading first line from OpList.txt");
            }

        }//end if(oplist exists)

        else {LOGGER.info("OpList.txt didn't exist. Returning falsified timestamp to requesting client.");}

        out.println(oplistTimestamp, "OpListTimestamp");
        out.flush();
    }
}
