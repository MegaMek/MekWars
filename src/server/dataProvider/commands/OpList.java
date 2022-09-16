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

package server.dataProvider.commands;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Date;

import common.CampaignData;
import common.util.BinWriter;
import server.dataProvider.ServerCommand;

/**
 * Retreive, line by line, the whole of OpList.txt
 */
public class OpList implements ServerCommand {

    /**
     * @see server.dataProvider.ServerCommand#execute(java.util.Date,
     *      java.io.PrintWriter, common.CampaignData)
     */
    public void execute(Date timestamp, BinWriter out, CampaignData data) throws Exception {
        FileInputStream configFile = new FileInputStream("./data/operations/OpList.txt");
        BufferedReader config = new BufferedReader(new InputStreamReader(configFile));
        while (config.ready())
            out.println(config.readLine(),"ListLine");
        
        config.close();
    }
    
}
