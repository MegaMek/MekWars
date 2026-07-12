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

package mekwars.server.dataProvider.commands;

import java.util.Date;

import common.CampaignData;
import common.util.BinWriter;
import server.dataProvider.ServerCommand;

/**
 * Retrieve all planet information (if the data cache is lost at client side)
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class BannedAmmo implements server.dataProvider.ServerCommand {

    /**
     * @see ServerCommand#execute(Date, java.io.PrintWriter, common.CampaignData)
     */
    public void execute(java.util.Date timestamp, BinWriter out, CampaignData data)
          throws Exception {
        java.io.FileInputStream configFile;
        try {
            configFile = new java.io.FileInputStream("./campaign/banammo.dat");
        } catch (java.io.FileNotFoundException FNFE) {
            java.io.FileOutputStream ban = new java.io.FileOutputStream("./campaign/banammo.dat");
            java.io.PrintStream p = new java.io.PrintStream(ban);

            //server banned ammo
            p.println(System.currentTimeMillis());
            p.println("server#");
            ban.close();
            p.close();
            configFile = new java.io.FileInputStream("./campaign/banammo.dat");
        }
        java.io.BufferedReader config = new java.io.BufferedReader(new java.io.InputStreamReader(configFile));

        while (config.ready()) {
            out.println(config.readLine(), "BannedAmmo");
        }
        config.close();
        configFile.close();
    }
}
