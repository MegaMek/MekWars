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

/*
 * Created on 13.04.2004
 *
 */
package server.campaign;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import common.Unit;
import common.util.MWLogger;


/**
 * Class which read in tables and returns random filenames.
 * 
 * The BuildTable class does NOT construct any units. Factories
 * or direct building (welfare, rewards) are used to create actual
 * SUnits and add them to players.
 * 
 * @originalauthor Helge Richter
 */
 
public class BuildTable {

	//VARIABLES
	public static final String STANDARD = "standard";
	public static final String RARE = "rare";
	public static final String REWARD = "reward";
	public static final String TECH = "tech";
	
	//NO CONSTRUCTOR. ALL METHODS STATIC
	
	/**
	 * Method which randomly selects a unit to build (filename), given a producer,
	 * unit size, mode (rare, etc) and other necessary data. This is used for all
	 * standard factory production.
	 */
	public static String getUnitFilename(String unitProducer, String size, int type_id, String dir) {
		boolean fileFound = false;
		String Filename = "";
		Vector<String> table = null;
		while (!fileFound){
			String fileToRead = getFileName(unitProducer,size,dir,type_id);
			table = getListFromFile(new File(fileToRead));

			if ( table.size() < 1)
			    return "Error OMG URFD.mtf";
			
			int ran = CampaignMain.cm.getRandomNumber(table.size());

			Filename = table.elementAt(ran);
			if (Filename.indexOf(".") == -1)
				unitProducer = Filename;
			else
				fileFound = true;

		}
		return Filename;
	}
	
	/**
	 * Method which randomly selects a unit to build (filename), given only
	 * a filename. This is used only to produce welfare units.
	 */
	public static String getUnitFilename(String unitFileName) {
		boolean fileFound = false;
		String Filename = "";
		Vector<String> table = null;
		while (!fileFound){
			table = getListFromFile(new File(unitFileName));

			if ( table.size() < 1) {
			    return "Error OMG URFD.mtf";
			}
			
			int ran = CampaignMain.cm.getRandomNumber(table.size());
			Filename = table.elementAt(ran);
			if (Filename.indexOf(".") == -1)
				unitFileName= Filename;
			else
				fileFound = true;
		}
		return Filename;
	}
	
	/**
	 * @param faction The Faction (i.e. - founder, Davion)
	 * @param size The weight class (i.e. - Light)
	 * @param dir  The Directory (i.e. - standard, rare)
	 * @param Type The Type of Entity (i.e. - Unit.MEK, Unit.VEHICLE)
	 * @return the Productionlist
	 */
	public static String getFileName(String faction, String weightclass,String dir,int Type) {
		/*
		 * Build the Filename, using patterns:
		 * FACTION_SIZE.txt or FACTION_SIZE&TYPE.txt
		 *  ex: Davion_Assault.txt
		 *  ex: Marik_LightVehicle
		 *  ex: WardenClan_HeavyBattleArmor.txt
		 *  ex: FadeFalcon_MediumProtoMek.txt
		 *  
		 * and the path, the same way: ./data/buildtables/YEAR/
		 *  ex: ./data/buildtables/3025/
		 *  ex: ./data/buildtables/3130/
		 */
		String addon = "";
		if (Type != Unit.MEK)
			addon = Unit.getTypeClassDesc(Type);
		String result = "./data/buildtables/"+dir+"/" + faction + "_" + weightclass + addon + ".txt";
		if (!new File(result).exists()){
			if ( !result.trim().toLowerCase().endsWith(".txt") )
				MWLogger.errLog("Unable to find build table file "+result+" using ./data/buildtables/"+dir+"/Common_" + weightclass + addon + ".txt");
			result = "./data/buildtables/"+dir+"/Common_" + weightclass + addon + ".txt";
		}
		return result;
	}
	
	public static String getTechFileName(String faction, String weightclass,int Type) {
		/*
		 * Build the Filename, using patterns:
		 * FACTION_SIZE.txt or FACTION_SIZE&TYPE.txt
		 *  ex: Davion_Assault.txt
		 *  ex: Marik_LightVehicle
		 *  ex: WardenClan_HeavyBattleArmor.txt
		 *  ex: FadeFalcon_MediumProtoMek.txt
		 *  
		 * and the path, the same way: ./data/buildtables/YEAR/
		 *  ex: ./data/buildtables/3025/
		 *  ex: ./data/buildtables/3130/
		 */
		String addon = "";
		if (Type != Unit.MEK)
			addon = Unit.getTypeClassDesc(Type);
		String result = "./data/buildtables/"+BuildTable.TECH+"/" + faction + "_" + weightclass + addon + ".txt";
		if (!new File(result).exists()){
			result = "";
		}
		return result;
	}
	
	/**
	 * This reads the unit tables. Format should be:
	 * 
	 * 1  UnitA.hmp
	 * 4  UnitB.hmp
	 * 5  UnitC.hmp
	 * 10 UnitD.hmp
	 * 
	 * This table would produce 50% D's, 25% C's,
	 * 20% B's and 5% A's (Weight/20).
	 * 
	 * @param prodFile tHe File to load from
	 * @param Type the type of unit to load
	 * 
	 * @return buildtable in a Hashtable that also contains a "TotalEntries" key
	 */
	private static Vector<String> getListFromFile(File prodFile){
		Vector<String>  result = new Vector<String>();
		ConcurrentHashMap<String, Integer> unitHolder = new ConcurrentHashMap<String, Integer>();
		try {
			
			FileInputStream fis = new FileInputStream(prodFile);
			BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
			while (dis.ready()) {

				/*
				 * Read the line and remove excess whitespace. Removing whitespace
				 * sensitivity will allow ops to make more readable files; however,
				 * any unit file name that contains 2 spaces consecutively (is there
				 * such a thing?) will be broken. 
				 */
				String l = dis.readLine();
				if (l == null || l.trim().length() == 0)
					continue;
				
				l = l.trim();
				l = l.replaceAll("\\s+"," ");//reduce multi-spaces to one space
				if (l.indexOf(" ") == 0)
					l = l.substring(1,l.length());
				
				StringTokenizer ST = new StringTokenizer(l.trim());
				if (ST.hasMoreElements()) {
					int amount = Integer.parseInt((String)ST.nextElement());
					StringBuilder filename = new StringBuilder();
					while (ST.hasMoreElements()) {
						filename.append(ST.nextToken());
						if (ST.hasMoreTokens())
							filename.append(" ");
					}
					if ( amount > 0 )
						unitHolder.put(filename.toString(), amount);
				}
			}
			dis.close();
			fis.close();

			while ( unitHolder.size() > 0 ){
				for (String filename : unitHolder.keySet() ){
					result.add(filename);
					int count = unitHolder.get(filename);
					if ( --count < 1)
						unitHolder.remove(filename);
					else
						unitHolder.put(filename, count);
						
				}
			}
			result.trimToSize();
		} catch (Exception ex) {
			MWLogger.errLog(ex);
		}
		return result;
	}
	
	public static void saveBuildTableFile(String name, String folder, ConcurrentHashMap<String,Integer> unitHolder) {
	    File prodFile = new File("./data/buildtables/"+folder+"/"+name);
	    
	    BuildTable.saveBuildTableFile(prodFile, unitHolder);
	}

	public static void saveBuildTableFile(File file, ConcurrentHashMap<String,Integer> unitHolder){
		try{
			FileOutputStream fos = new FileOutputStream(file);
			PrintStream ps = new PrintStream(fos);
			
			for (String key : unitHolder.keySet())
				ps.println(unitHolder.get(key)+" "+key);
			
			ps.close();
			fos.close();
		}
		catch(Exception ex){
			MWLogger.errLog(ex);
		}

	}
	
	public static ConcurrentHashMap<String, Integer> loadBuildTable(File prodFile){
        ConcurrentHashMap<String, Integer> unitHolder = new ConcurrentHashMap<String, Integer>();
        try {
            
            FileInputStream fis = new FileInputStream(prodFile);
            BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
            while (dis.ready()) {

                /*
                 * Read the line and remove excess whitespace. Removing whitespace
                 * sensitivity will allow ops to make more readable files; however,
                 * any unit file name that contains 2 spaces consecutively (is there
                 * such a thing?) will be broken. 
                 */
                String l = dis.readLine();
                if (l == null || l.trim().length() == 0)
                    continue;
                
                l = l.trim();
                l = l.replaceAll("\\s+"," ");//reduce multi-spaces to one space
                if (l.indexOf(" ") == 0)
                    l = l.substring(1,l.length());
                
                StringTokenizer ST = new StringTokenizer(l.trim());
                if (ST.hasMoreElements()) {
                    int amount = Integer.parseInt((String)ST.nextElement());
                    StringBuilder filename = new StringBuilder();
                    while (ST.hasMoreElements()) {
                        filename.append(ST.nextToken());
                        if (ST.hasMoreTokens())
                            filename.append(" ");
                    }
                    if ( amount > 0 )
                        unitHolder.put(filename.toString(), amount);
                }
            }
            dis.close();
            fis.close();

        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
        return unitHolder;
	}
}