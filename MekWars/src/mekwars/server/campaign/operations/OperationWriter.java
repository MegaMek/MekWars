/*
 * MekWars - Copyright (C) 2005 
 * 
 * Original author - nmorris (urgru@users.sourceforge.net)
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

/*
 * A utility which writes operation information.
 * 
 * Has methods to write
 *  - LongOperation SAVES
 * 
 * NOTE: The loader/writer DO NOT handle the reading and writing
 * of all the Operation/ModifyingOperation params. Modification of
 * these files must be done by hand, or with an external utility
 */
package server.campaign.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import common.House;
import common.campaign.operations.Operation;
import common.util.MWLogger;
import server.campaign.CampaignMain;

//IMPORTS

/*
 * The OperationWriter is called after loading and
 * writes out an OpList.txt which contains name,
 * range, short/long and colour information for all
 * Operations.
 * 
 * This text file is sent to the clients (MD5) when
 * they connect and used to build menus and maps.
 */
public class OperationWriter {
	
	//IVARS
	
	//CONSTRUCTORS
	public OperationWriter() {
		//contents
	}

	//METHODS
	public void writeOpList(TreeMap<String,Operation> ops) {
		
		//path to our file
		String path = "./data/operations/OpList.txt";
		
		//if there is an old file, delete before rewriting
		File oldFile = new File(path);
		if (oldFile.exists())
			oldFile.delete();
	
		//construct a new file
		try{
			PrintStream ps = new PrintStream(new FileOutputStream(path));
			ps.println("#Timestamp=" + System.currentTimeMillis());
			
			for (Operation currO : ops.values()){
				String name = currO.getName() + "*";
				String range = currO.getValue("OperationRange") + "*";
				String color = currO.getValue("OperationColor") + "*";
				String hasLong = currO.getTypeIndicator() + "*";//actually an INT. 0 == short. 1 == long.
				String launchOn = currO.getValue("PercentageToAttackOnWorld") + "*";
				String launchFrom = currO.getValue("PercentageToAttackOffWorld") + "*";
				String minOwn = currO.getValue("MinPlanetOwnership") + "*";
				String minOwnIBD = currO.getValue("MinPlanetOwnershipIgnoredByDefender") + "*"; //Baruk Khazad! - 20151003
				String maxOwn = currO.getValue("MaxPlanetOwnership") + "*";
				String reserveOnly = currO.getValue("OnlyAllowedFromReserve") + "*";
				String activeOnly = currO.getValue("OnlyAllowedFromActive") + "*";
				String minSubFactionLevel = currO.getValue("MinSubFactionAccessLevel")+"*";
				
				String facInfo = "any*";
				if (Boolean.parseBoolean(currO.getValue("OnlyAgainstFactoryWorlds")))
					facInfo = "only*";
				else if (Boolean.parseBoolean(currO.getValue("OnlyAgainstNonFactoryWorlds")))
					facInfo = "none*";
				
                String homeworldInfo = "any*";
                if (Boolean.parseBoolean(currO.getValue("OnlyAgainstHomeWorlds")))
                    homeworldInfo = "only*";
                else if (Boolean.parseBoolean(currO.getValue("OnlyAgainstNonHomeWorlds")))
                    homeworldInfo = "none*";
                
				StringBuffer legalDefenders = new StringBuffer();
				if (!currO.getValue("LegalDefendFactions").trim().equals("")) {
					legalDefenders.append(currO.getValue("LegalDefendFactions").trim() + "*");
				} else if (!currO.getValue("IllegalDefendFactions").trim().equals("")) {
					
					//determine which factions *can't* use the type
					StringTokenizer illegalTokenizer = new StringTokenizer(currO.getValue("IllegalDefendFactions").trim(), "$");
					TreeSet<String> illegals = new TreeSet<String>();
					while (illegalTokenizer.hasMoreTokens())
						illegals.add(illegalTokenizer.nextToken());
					
					//compare all houses to the treeset and look for matches
					for (House currH : CampaignMain.cm.getData().getAllHouses()) {
						if (!illegals.contains(currH.getName()))
							legalDefenders.append(currH.getName() + "$");
					}
					legalDefenders.append("*");
					
				} else
					legalDefenders.append("allFactions*");
				
                StringBuffer allowPlanetFlags = new StringBuffer(currO.getValue("AllowPlanetFlags"));
                if ( allowPlanetFlags.length() < 1 )
                    allowPlanetFlags.append("^ ^*");
                else
                    allowPlanetFlags.append("*");
                    
                StringBuffer disallowPlanetFlags = new StringBuffer(currO.getValue("DisallowPlanetFlags"));
                if ( disallowPlanetFlags.length() < 1)
                    disallowPlanetFlags.append("^ ^*");
                else
                    disallowPlanetFlags.append("*");
                
				ps.println(name
						+ range
						+ color
						+ hasLong
						+ facInfo
						+ homeworldInfo
						+ launchOn
						+ launchFrom
						+ minOwn
						+ maxOwn
						+ reserveOnly
						+ activeOnly
						+ legalDefenders.toString().trim()
						+ allowPlanetFlags.toString().trim()
						+ disallowPlanetFlags.toString().trim()
						+ minSubFactionLevel
						+ minOwnIBD);      //Baruk Khazad! - 20151003
				
			}
			ps.close();
		} catch (FileNotFoundException fe) {
			MWLogger.errLog("Error: could not find " + path);
		} catch (Exception ex) {
			MWLogger.errLog(ex);
		}
		
	}//end writeOpList
}//end OperationsManager class