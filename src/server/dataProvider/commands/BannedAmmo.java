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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Date;

import common.CampaignData;
import common.util.BinWriter;
import server.dataProvider.ServerCommand;

/**
 * Retrieve all planet information (if the data cache is lost at client side)
 * 
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class BannedAmmo implements ServerCommand {

    /**
     * @see server.dataProvider.ServerCommand#execute(java.util.Date,
     *      java.io.PrintWriter, common.CampaignData)
     */
    public void execute(Date timestamp, BinWriter out, CampaignData data)
            throws Exception {
    		FileInputStream configFile; 
    	    try{
    	    	configFile = new FileInputStream("./campaign/banammo.dat");
    	    }catch (FileNotFoundException FNFE){
    			FileOutputStream ban = new FileOutputStream("./campaign/banammo.dat");
    			PrintStream p = new PrintStream(ban);
    			
    			//server banned ammo
    			p.println(System.currentTimeMillis());
    			p.println("server#");
    			ban.close();
    			p.close();
    			configFile = new FileInputStream("./campaign/banammo.dat");
    	    }
            BufferedReader config = new BufferedReader(new InputStreamReader(configFile));
            
            while (config.ready()) {
                out.println(config.readLine(),"BannedAmmo");
            }
            config.close();
            configFile.close();
        }
}
