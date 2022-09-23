package server.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import common.util.MWLogger;
import server.campaign.SPlayer;


//@salient - util class saves SPlayer as simple json file
//			 for use with discord bot
public class SPlayerToJSON 
{
    private static String jsonString = "";
    
    private static boolean jsonStart = false;
        
    public static void writeToFile(SPlayer player)
    {
    	startJson();
    	stringJson("myLogo",player.getMyLogo());
    	stringJson("name",player.getName());
    	stringJson("discordID",player.getDiscordID());
    	stringJson("house",player.getMyHouse().getName());
    	stringJson("fluffText",player.getFluffText());
    	stringJson("subFaction",player.getSubFaction().getName());
    	doubleJson("rating",player.getRating());
    	intJson("experience",player.getExperience());
    	intJson("money",player.getMoney());
    	intJson("influence",player.getInfluence());
    	intJson("rewardPoints",player.getReward());
    	endJson();
    	   	
		File pathCheck = new File("data/discord/players");
	
		//if the path doesn't exist, create it
		if (pathCheck.exists() == false)
		{
			if(pathCheck.mkdirs() == false)
			{
				MWLogger.errLog("error in SPlayerToJSON, failed to create directories");
				return;
			}			
		}
    	
		try 
		{
			Files.write(Paths.get("./data/discord/players/"+player.getName()+".json"), jsonString.getBytes(),
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
    	jsonStart = true;
    	jsonString += "{";
    }
    
    private static void stringJson(String key, String item)
    {     
    		String comma = "";
    		
			if (jsonStart == false)
				comma = ",";
			else
				jsonStart = false;
			
			jsonString += comma + "\"" + key + "\":" + "\"" + item + "\"";
    }
    
    private static void intJson(String key, int item)
    {       
		String comma = "";
		
		if (jsonStart == false)
			comma = ",";
		else
			jsonStart = false;
		
		jsonString += comma + "\"" + key + "\":" + item;				
    }
    
    private static void doubleJson(String key, double item)
    {       
		String comma = "";
		
		if (jsonStart == false)
			comma = ",";
		else
			jsonStart = false;
		
		jsonString += comma + "\"" + key + "\":" + item;				
    }
    
    private static void endJson() 
    {
    	jsonString += "}";
    	jsonStart = true;
    }
    
}
