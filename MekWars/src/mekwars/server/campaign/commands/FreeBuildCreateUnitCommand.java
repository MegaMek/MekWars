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

package server.campaign.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.stream.Collectors;

import common.House;
import common.util.MWLogger;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.pilot.SPilot;


/**
 * A command to create a unit
 * <p>
 * This command allows a player to freely create a unit, which is then dropped into his hangar.
 * It also checks the build table to ensure that a legal unit has been requested.
 * This was originally created for new players only, however it has been expanded to allow
 * the player to create their starting hangar after defecting to a new faction.
 *
 * @Salient
 * 2017.9.01
 */
public class FreeBuildCreateUnitCommand implements Command
{

	int accessLevel = 1;
	String syntax = "filename#weightclass#houseTable";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}

	private Boolean solFreeBuild;
	private Boolean postDefectionFreeBuild;
	private Boolean useAllBuildTables;
	private Boolean limitOnlyPostDefection;
	private Boolean allowDupes;
	private Boolean useDupeLimits;
	private int userlvl;
	private int freeBuildLimit;
	private int dupeLimitMeks, dupeLimitVees, dupeLimitInf, dupeLimitBA, dupeLimitAero;
	private String username;
	private String newbieHouseName;
	private String buildTableForFreeBuild;
	private String houseTable;
	private List<String> houseList = new ArrayList<String>();
	private SPlayer player;
	private SHouse house;
	private SUnit unit;

	public void process(StringTokenizer command,String Username)
	{
		
		initVars(Username);

		if(!accessChecks())
			return;

		unit = readCommandReturnSUnit(command);
		if(unit == null)
			return;		

		houseTable = null; //used if 'useall' flag or post defection is set to true in SO
		if(command.hasMoreElements())
			houseTable = command.nextToken();

		if(!playerUnitLimitChecks())
			return;

		//debug
		//CampaignMain.cm.toUser("DEBUG: First Faction to Search: " + houseTable,Username,true);

		initHouseList();
        addOtherBuildTables();

		if(!isUnitLegal())
			return;

		player.addUnit(unit, true);

	    calcRemainingFreeMeks(); // if there is no limit set, this will do nothing.

		CampaignMain.cm.toUser("Unit created: " + unit.getSmallDescription() + "  ID #" + unit.getId(),Username,true);

	}

	private void initVars(String Username)
	{
		username = Username;
		userlvl = CampaignMain.cm.getServer().getUserLevel(username);
		player = CampaignMain.cm.getPlayer(username);
		house = player.getMyHouse();
		solFreeBuild = Boolean.parseBoolean(CampaignMain.cm.getConfig("Sol_FreeBuild"));
		postDefectionFreeBuild = Boolean.parseBoolean(CampaignMain.cm.getConfig("FreeBuild_PostDefection"));
		newbieHouseName = CampaignMain.cm.getConfig("NewbieHouseName");
		useAllBuildTables = Boolean.parseBoolean(CampaignMain.cm.getConfig("Sol_FreeBuild_UseAll"));
		buildTableForFreeBuild = CampaignMain.cm.getConfig("Sol_FreeBuild_BuildTable");
		limitOnlyPostDefection = Boolean.parseBoolean(CampaignMain.cm.getConfig("FreeBuild_LimitPostDefOnly"));
		freeBuildLimit = Integer.parseInt(house.getConfig("FreeBuild_Limit"));
		allowDupes = Boolean.parseBoolean(house.getConfig("FreeBuild_AllowDuplicates"));
		useDupeLimits = Boolean.parseBoolean(house.getConfig("FreeBuild_DupeLimits"));
		dupeLimitMeks = Integer.parseInt(house.getConfig("FreeBuild_NumOfDuplicateMeks"));
		dupeLimitVees = Integer.parseInt(house.getConfig("FreeBuild_NumOfDuplicateVees"));
		dupeLimitInf = Integer.parseInt(house.getConfig("FreeBuild_NumOfDuplicateInf"));
		dupeLimitBA = Integer.parseInt(house.getConfig("FreeBuild_NumOfDuplicateBA"));
		dupeLimitAero = Integer.parseInt(house.getConfig("FreeBuild_NumOfDuplicateAero"));
		
	}

	/**
	 *  make sure that this command can be run with current server settings and user access levels
	 */
    private Boolean accessChecks()
    {
    	if(userlvl < getExecutionLevel())
    	{
    		CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userlvl + ". Required: " + accessLevel + ".",username,true);
    		return false;
    	}

    	// if Sol_FreeBuild and post defection are set to false, return
    	if(!solFreeBuild && !postDefectionFreeBuild)
    	{
    		CampaignMain.cm.toUser("AM:This command is disabled on this server.",username,true);
    		return false;
    	}

    	// if build limit set to 0, return
    	if(freeBuildLimit == 0)
    	{
    		CampaignMain.cm.toUser("AM:Build limit set to 0, was this intentional? If so, uncheck Sol Free Build instead.",username,true);
    		return false;
    	}

    	// if the player isn't in SOL and FreeBuild_PostDefection is false, return
    	if(!house.getName().equalsIgnoreCase(newbieHouseName) && !postDefectionFreeBuild)
    	{
    		CampaignMain.cm.toUser("AM: Only players in " + newbieHouseName + " can use this command.",username,true);
    		return false;
    	}

    	// if a limit has been set, check to make sure player has not exceeded limit
    	if(freeBuildLimit > 0 && player.getMekToken() == freeBuildLimit)
    	{
    		CampaignMain.cm.toUser("AM:You have reached the server limit of free units.",username,true);
    		return false;
    	}

    	return true;
    }

	private SUnit readCommandReturnSUnit(StringTokenizer command)
	{

		String filename;
		String FlavorText = "Built by " + player.getName();
		String skillTokens = null;

		try
		{
			filename = command.nextToken();
		}
		catch(Exception ex)
		{
			CampaignMain.cm.toUser(syntax, username);
			return null;
		}

		int weight = SUnit.LIGHT;

		if(command.hasMoreElements())
			weight = Integer.parseInt(command.nextToken());

		//Note that if you look at the create method of SUnit, it appears you dont really need to pass the weight...
		SUnit tempUnit = SUnit.create(filename, FlavorText, house.getBaseGunner(), house.getBasePilot(), weight, skillTokens);
        //This will create a pilot based on faction settings defined by server operator
		SPilot tempPilot = house.getNewPilot(tempUnit.getType());
        tempUnit.setPilot(tempPilot);

        return tempUnit;
	}

	private Boolean playerUnitLimitChecks()
	{
		if(!player.hasRoomForUnit(unit.getType(), unit.getWeightclass()))
		{
			CampaignMain.cm.toUser("AM:You have reached the limit for this type of unit at this weight class.",username,true);
			return false;
		}

		if(SUnit.getHangarSpaceRequired(unit, house) > player.getFreeBays())
		{
			if( !house.getName().equalsIgnoreCase(newbieHouseName))
			{
				CampaignMain.cm.toUser("AM:You do not have enough free bays to create this unit.",username,true);
				return false;
			}
			else
			{
				CampaignMain.cm.toUser("AM:You do not have enough free bays to create this unit. You can delete an existing unit by right clicking on it and choosing transactions -> delete to make some room.",username,true);
				return false;
			}
		}
		
		if(!allowDupes)
		{
			Vector<SUnit> playersUnits = player.getUnits();
			
			for(SUnit aUnit : playersUnits)
			{
				if(aUnit.getVerboseModelName().equalsIgnoreCase(unit.getVerboseModelName()))
				{
					CampaignMain.cm.toUser("AM:Freebuild duplicates are not allowed on this server! Please choose a different variant or unit.",username,true);
					return false;
				}
			}			
		}
		
		if(useDupeLimits)
		{
			Vector<SUnit> playersUnits = player.getUnits();
			int unitCount = 0;
			
			for(SUnit aUnit : playersUnits)
			{
				if(aUnit.getVerboseModelName().equalsIgnoreCase(unit.getVerboseModelName()))
				{
					unitCount++;
				}
			}
			
			if(unitCount != 0)
			{
				return checkDupeLimits(unitCount);
			}
		}

		return true;
	}

	private Boolean checkDupeLimits(int unitCount) 
	{
		switch(unit.getType())
		{
		case 0:
		case 6:
			if(dupeLimitMeks == -1)
				return true;
			if(dupeLimitMeks > unitCount)
				return true;
			else
			{
				player.toSelf("AM: You have reached the limit for duplicate meks! Please choose a different mek or variant.");
				return false;
			}
		case 1:
			if(dupeLimitVees == -1)
				return true;
			if(dupeLimitVees > unitCount)
				return true;
			else
			{
				player.toSelf("AM: You have reached the limit for duplicate vees! Please choose a different vee or variant.");
				return false;
			}
		case 2:
			if(dupeLimitInf == -1)
				return true;
			if(dupeLimitInf > unitCount)
				return true;
			else
			{
				player.toSelf("AM: You have reached the limit for duplicate infantry! Please choose a different vee or variant.");
				return false;
			}
		case 3:
		case 4:
			if(dupeLimitBA == -1)
				return true;
			if(dupeLimitBA > unitCount)
				return true;
			else
			{
				player.toSelf("AM: You have reached the limit for duplicate BA! Please choose a different BA or variant.");
				return false;
			}
		case 5:
			if(dupeLimitAero == -1)
				return true;
			if(dupeLimitAero > unitCount)
				return true;
			else
			{
				player.toSelf("AM: You have reached the limit for duplicate Aero! Please choose a different Aero or variant.");
				return false;
			}
				
		}
		
		MWLogger.errLog("No case for this unit type in FreeBuildCreateUnit.java -> checkDupeLimits. Type is.. " + unit.getType());
		return false;
	}
	
	/**
	 * 	Get a collection of all houses in game and covert it to a list
     *  Need to set the list of houses before calling checkiflegal method
	 */
	private void initHouseList()
	{
		houseList.clear();
        Iterator<House> i = CampaignMain.cm.getData().getAllHouses().iterator();

        //debug
        //CampaignMain.cm.toUser("-------------------------------------------------------------------------", player.getName() ,true);
		//CampaignMain.cm.toUser("DEBUG: Sever Side Faction List ( CampaignMain.cm.getData().getAllHouses() )", player.getName() ,true);

        while (i.hasNext())
        {
           House aHouse = i.next();

           if (aHouse.getId() > -1)
           {
        	   houseList.add(aHouse.getName().trim());
        	   //debug
       		   //CampaignMain.cm.toUser("DEBUG: " + aHouse.getName() , player.getName() ,true);
           }
        }
        //debug
        //CampaignMain.cm.toUser("-------------------------------------------------------------------------", player.getName() ,true);
	}

	/**
	 *  checks if the requested unit exists in proper unit build tables, if so, returns true.
	 */
	private Boolean isUnitLegal()
	{
		try
		{
			if(useAllBuildTables && house.getName().equalsIgnoreCase(newbieHouseName))
			{
				if(!CheckIfLegal(houseTable,unit))
				{
					CampaignMain.cm.toUser("AM:This is not a legal unit!",username,true);
					//add some logging here, mod mail possible cheating attempt or BT error
					MWLogger.errLog("User: " + username + "  tried to create " + unit.getUnitFilename() + " unit was not found in build tables");
					MWLogger.modLog("User: " + username + "  tried to create " + unit.getUnitFilename() + " unit was not found in build tables");
					return false;
				}
			}
			else if (postDefectionFreeBuild && !house.getName().equalsIgnoreCase(newbieHouseName))
			{
				if(!CheckIfLegal(house.getName().trim(),unit))
				{
					CampaignMain.cm.toUser("AM:This is not a legal unit!",username,true);
					//add some logging here, mod mail possible cheating attempt or BT error
					MWLogger.errLog("User: " + username + "  tried to create " + unit.getUnitFilename() + " unit was not found in build tables");
					MWLogger.modLog("User: " + username + "  tried to create " + unit.getUnitFilename() + " unit was not found in build tables");
					return false;
				}
			}
			else
			{
				if(!CheckIfLegal(buildTableForFreeBuild,unit))
				{
					CampaignMain.cm.toUser("AM:This is not a legal unit!",username,true);
					//add some logging here, mod mail possible cheating attempt or BT error
					MWLogger.errLog("User: " + username + "  tried to create " + unit.getUnitFilename() + " unit was not found in build tables");
					MWLogger.modLog("User: " + username + "  tried to create " + unit.getUnitFilename() + " unit was not found in build tables");
					return false;
				}
			}
		}
		catch (IOException e)
		{
			MWLogger.errLog(e);
			return false;
		}

		return true;
	}

	/**
	 * add non-faction build tables to list if they aren't already present
	 * also add the table that is set by the SO
	 */
	private void addOtherBuildTables()
	{

		houseList.add(buildTableForFreeBuild);

        if(!buildTableForFreeBuild.equalsIgnoreCase("Common") &&
           !houseList.contains("Common"))
        {
        	houseList.add("Common");
        }

        if(!buildTableForFreeBuild.equalsIgnoreCase("Rare") &&
           !houseList.contains("Rare"))
        {
        	houseList.add("Rare");
        }

        if(!buildTableForFreeBuild.equalsIgnoreCase("Contest") &&
           !houseList.contains("Contest"))
        {
        	houseList.add("Contest");
        }


        //debug list contents of houseList
        //houseList.forEach(x->{
        //	CampaignMain.cm.toUser("DEBUG: houseList = " + x , p.getName() ,true);
        //});
	}

	/**
	 * @Return a Boolean as true if the SUnit is found in available build tables
	 */
	private Boolean CheckIfLegal(String buildTableName, SUnit unitToCheck) throws IOException
	{

		Boolean result = false;

        buildTableName += "_" + SUnit.getWeightClassDesc(unitToCheck.getWeightclass());

        //debug
        //CampaignMain.cm.toUser("DEBUG: Unit Type is... " + SUnit.getTypeClassDesc(unitToCheck.getType()), p.getName(), true);

		if(unitToCheck.getType() != 0)
        {
        	buildTableName += SUnit.getTypeClassDesc(unitToCheck.getType());
        }

        buildTableName += ".txt";

        //debug
		//CampaignMain.cm.toUser("DEBUG: Searching in: " + buildTableName + ", For Unit: " + unitToCheck.getUnitFilename().trim(), player.getName(), true);

        //we should now have the correct path.
        Path path = Paths.get("data/buildtables/standard/" + buildTableName).toAbsolutePath();

        //make sure this build table file exists
        if(Files.notExists(path))
        {
			CampaignMain.cm.toUser("Error Build Table file " + buildTableName + " does not exist",username,true);
			return false;
        }

        //create list of allowedUnits
        List<String> allowedUnits = Files.lines(path).collect(Collectors.toList());

        //debug
        //CampaignMain.cm.toUser("DEBUG: allowedUnits.size()  = " + allowedUnits.size() , p.getName() ,true);

        //debug list contents of houseList
        //allowedUnits.forEach(x->{
        //	CampaignMain.cm.toUser("DEBUG: allowedUnits = " + x , p.getName() ,true);
        //});


        //remove frequency numbers
        for(int i = 0; i < allowedUnits.size(); i++)
        {
        	String temp = allowedUnits.get(i);

        	//Catch blank lines and continue
        	if(temp.equals("") || temp == null || temp.trim().isEmpty() || temp.equals("\n") || temp.equals("\r\n"))
        	{
        		temp = "error.mtf";
        		allowedUnits.set(i, temp);
        		//CampaignMain.cm.toUser("DEBUG: Allowed Unit = " + temp , p.getName() ,true);
        		continue;
        	}

        	//Check for space, if we assume a BT with no errors this isn't needed, until it is...
        	if(temp.contains(" "))
        	{
        		int firstSpace = temp.indexOf(" ");
        		temp = temp.substring(firstSpace);
        	}

        	temp = temp.trim();
        	allowedUnits.set(i, temp);

        	//debug
    		//CampaignMain.cm.toUser("DEBUG: Allowed Unit = " + temp, p.getName() ,true);
        }

        result = allowedUnits.contains(unitToCheck.getUnitFilename().trim());

        //is stream any faster than contains?
        //result = allowedUnits.stream().anyMatch(p->p.equalsIgnoreCase(unitToCheck.getUnitFilename().trim()));

        //debug
        //CampaignMain.cm.toUser("DEBUG: Pre Faction Search... Result = " + result , p.getName() ,true);


        if(!result) //If the result is false, check to see if there were any other BTs in the list
        {
        	//^^debug
    		//CampaignMain.cm.toUser("DEBUG: Check 1", player.getName(), true);

            for(int i = 0; i < allowedUnits.size(); i++) //iterate and check if unit or built table
            {
            	if(!allowedUnits.get(i).contains(".")) //if its another build table it wont have the . character
            	{
            		//^^debug
            		//CampaignMain.cm.toUser("DEBUG: Check 2 " + allowedUnits.get(i), player.getName() , true);
            		for(int z = 0; z < houseList.size(); z++) //found another BT, find out which house
            		{
            			//^^debug
            			//CampaignMain.cm.toUser("DEBUG: Check 3 .." + allowedUnits.get(i).trim() + ".. ?= .." + houseList.get(z)+ "..", player.getName(), true);
            			if(houseList.get(z).equalsIgnoreCase(allowedUnits.get(i)))
            			{
            	    		//^^debug
            				//CampaignMain.cm.toUser("DEBUG: Check 4 " + houseList.get(z), player.getName(), true);

            				//once we find it, it's important to remove it from the list
            				String temp = houseList.get(z);
            				houseList.remove(z);
            				//search other BTs recursively
            				result = CheckIfLegal(temp,unit);
            				//if it's true, stop searching, return it.
            				if(result) {
            					//debug
            					//CampaignMain.cm.toUser("DEBUG: result = " + result + " BT = " + buildTableName + " Unit = " + unitToCheck.getUnitFilename(), p.getName() ,true);
            					return result;
            				}
            			}
            		}
            	}
            }
        }

		//debug
        //CampaignMain.cm.toUser("DEBUG: result = " + result + " BT = " + buildTableName + " Unit = " + unitToCheck.getUnitFilename(), p.getName() ,true);

        return result;


	}//end checkiflegal

	/**
	 *  makes sure that infinite newbie faction free build can co-exist with limited post defection freebuild
	 */
	private void calcRemainingFreeMeks()
	{

		//user is not in newbie faction, a limit is set and it applies only to post defection unit creation
		if( limitOnlyPostDefection && !house.getName().equalsIgnoreCase(newbieHouseName) && freeBuildLimit > 0)
		{
			player.addMekToken(1);
			int remaining = freeBuildLimit - player.getMekToken();
			CampaignMain.cm.toUser(remaining + " free units remain.",username,true);
		}

		//user is in newbie faction, a limit is set and it applies to newbie faction only
		if( !limitOnlyPostDefection && house.getName().equalsIgnoreCase(newbieHouseName) && freeBuildLimit > 0)
		{
			player.addMekToken(1);
			int remaining = freeBuildLimit - player.getMekToken();
			CampaignMain.cm.toUser(remaining + " free units remain.",username,true);
		}
	}
}

//thought of alternate method of doing this that updates a data container class with legal unit lists that lives in
//memory attached to campaignmain.cm. That object will hold legal lists for each weight class
//the lists will update only when a failure occurs (to make sure that this wasnt an intentional
//server side build table update) and only then fail to produce a unit (likely cheating attempt or bad BT).
//Could be more stable and faster especially if this is a very popular mekwars server.
//Or it may be overkill.
