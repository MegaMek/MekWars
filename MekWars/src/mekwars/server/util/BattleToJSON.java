package server.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.TreeMap;

import common.util.MWLogger;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.operations.OperationEntity;
import server.campaign.operations.ShortOperation;

//@salient - so for now this will be basic, it wont handle salvage or draw games.
public class BattleToJSON
{
    private static String jsonString = "";
    
    private static boolean skipComma = false;

    public static void writeToFile(ShortOperation theOp, TreeMap<Integer, OperationEntity> livingUnitsMap, 
    		TreeMap<Integer, OperationEntity> deadUnitsMap, boolean gameEndedInDraw)
    {
    	//I could just randomly pick a winner...
    	if(gameEndedInDraw)
    	{
    		MWLogger.errLog("BattleToJSON - Draw games are not saved to JSON");
    		return;
    	}
    	
//    	String datetime = "8-22-12"; //need to look this up, theOp has a function that returns a long of time of match
    	Date opDate = new Date(); //I tried using theOP get time value, didnt seem to work. so 
    								//i think this will return the current time
    	//currently only supports 1v1 , so take first entry of each tree
    	SPlayer winner = theOp.getWinners().firstEntry().getValue();
    	SPlayer loser = theOp.getLosers().firstEntry().getValue();
   	
    	startJson();
    	stringJson("datetime",opDate.toString());
    	stringJson("winner",winner.getName());
    	stringJson("loser",loser.getName());
    	stringJson("faction_won",winner.getMyHouse().getName());
    	stringJson("faction_lost",loser.getMyHouse().getName());
    	stringJson("location",theOp.getTargetWorld().getName());
    	startArray("units");

    	for(OperationEntity currOpEnt : livingUnitsMap.values())
    	{
            // load the player and unit
            String ownerName = currOpEnt.getOwnerName().toLowerCase();
            SPlayer owner = CampaignMain.cm.getPlayer(ownerName);
            /*
             * There is a note in short resolver that some sort of bug would cause null owners.
             * Not sure if it was ever resolved. Leaving the check here just in case.
             */
            if (owner == null) {
                MWLogger.errLog("Null _owner_ while processing post-game salvage for " 
                		+ " Attack #" + theOp.getShortID() + ". Needed to find Player: " + ownerName + " Unit #" 
                		+ currOpEnt.getID() + "/Type: " + currOpEnt.getType());
                continue;
            }

            SUnit currUnit = owner.getUnit(currOpEnt.getID());
            
            startArrayObject();
            intJson("unit_id",currUnit.getId());
            stringJson("owner", owner.getName());
            stringJson("chassis",currUnit.getEntity().getChassis().trim());
            String modelName = currUnit.getEntity().getModel().trim();
            modelName = modelName.replaceAll("\"", "\\\\\"");
            stringJson("model",modelName);
            intJson("type",currUnit.getType());
            intJson("battle_value",currUnit.getBaseBV());
            intJson("value",(int)currUnit.getEntity().getCost(true));
            stringJson("status","alive");
            stringJson("destroyed_by","");
            closeObject();
    	}
    	
    	for(OperationEntity currOpEnt : deadUnitsMap.values())
    	{
            // load the player and unit
            String ownerName = currOpEnt.getOwnerName().toLowerCase();
            SPlayer owner = CampaignMain.cm.getPlayer(ownerName);
            /*
             * There is a note in short resolver that some sort of bug would cause null owners.
             * Not sure if it was ever resolved. Leaving the check here just in case.
             */
            if (owner == null) {
                MWLogger.errLog("Null _owner_ while processing BattleToJson " 
                		+ " Attack #" + theOp.getShortID() + ". Needed to find Player: " + ownerName + " Unit #" 
                		+ currOpEnt.getID() + "/Type: " + currOpEnt.getType());
                continue;
            }

            SUnit currUnit = owner.getUnit(currOpEnt.getID());
            
            startArrayObject();
            intJson("unit_id",currUnit.getId());
            stringJson("owner", owner.getName());
            stringJson("chassis",currUnit.getEntity().getChassis().trim());
            String modelName = currUnit.getEntity().getModel().trim();
            modelName = modelName.replaceAll("\"", "\\\\\"");
            stringJson("model",modelName);
            intJson("type",currUnit.getType());
            intJson("battle_value",currUnit.getBaseBV());
            intJson("value",(int)currUnit.getEntity().getCost(true));
            stringJson("status","dead");
            if(owner.getName().equalsIgnoreCase(winner.getName()))
            	stringJson("destroyed_by",loser.getName());
            else
            	stringJson("destroyed_by",winner.getName());
            closeObject();
    	}
    	
    	closeArray();
    	endJson();
    	   	
		File pathCheck = new File("data/django/battles");
	
		//if the path doesn't exist, create it
		if (pathCheck.exists() == false)
		{
			if(pathCheck.mkdirs() == false)
			{
				MWLogger.errLog("error in BattleToJSON, failed to create directories");
				return;
			}			
		}
    	
		try 
		{
			//Assuming that getshortID returns a unique ID
			String fileName = opDate.toString().replaceAll(":", "_").replaceAll("\\s","_");
			Files.write(Paths.get("./data/django/battles/"+fileName+".json"), jsonString.getBytes(),
			         StandardOpenOption.CREATE,
			         StandardOpenOption.TRUNCATE_EXISTING);
						
			MWLogger.debugLog("SPlayer to json filewrite completed successfully");
		} 
		catch (IOException e) 
		{
			MWLogger.debugLog(e);
			MWLogger.errLog(e);
		}
		
		jsonString = ""; //clear for next use
    }
        
    private static void startJson() 
    {
    	skipComma = true;
    	jsonString += "{";
    }
    
    private static void startObject(String key) 
    {
		String comma = "";
		
		if (skipComma == false)
			comma = ",";
		else
			skipComma = false;
		
		jsonString += comma + "\"" + key + "\":" + "{";

    	skipComma = true;
    }
    
    private static void closeObject(){		
		jsonString += "}";
    }
    
    private static void startArray(String key) 
    {
		String comma = "";
		
		if (skipComma == false)
			comma = ",";
		else
			skipComma = false;
		
		jsonString += comma + "\"" + key + "\":" + "[";

    	skipComma = true;
    }
    
    private static void startArrayObject() 
    {
		String comma = "";
		
		if (skipComma == false)
			comma = ",";
		else
			skipComma = false;
		
		jsonString += comma + "{";

    	skipComma = true;
    }
    
    private static void closeArray(){		
		jsonString += "]";
    }
    
    private static void stringJson(String key, String item)
    {     
    		String comma = "";
    		
			if (skipComma == false)
				comma = ",";
			else
				skipComma = false;
			
			jsonString += comma + "\"" + key + "\":" + "\"" + item + "\"";
    }
    
    private static void intJson(String key, int item)
    {       
		String comma = "";
		
		if (skipComma == false)
			comma = ",";
		else
			skipComma = false;
		
		jsonString += comma + "\"" + key + "\":" + item;				
    }
    
    private static void doubleJson(String key, double item)
    {       
		String comma = "";
		
		if (skipComma == false)
			comma = ",";
		else
			skipComma = false;
		
		jsonString += comma + "\"" + key + "\":" + item;				
    }
    
    private static void endJson() 
    {
    	jsonString += "}";
    	skipComma = true;
    }
    
}
