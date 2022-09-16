/*
 * MekWars - Copyright (C) 2016
 *
 * Original author - Bob Eldred (billypinhead@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package server.campaign.util.scheduler;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import common.util.MWLogger;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;

/**
 * A class to handle distribution of Influence due to player activity
 * 
 * @author Spork
 * @version 2016.10.10
 */
public class UserActivityInfluenceJob implements Job, MWRepeatingJob, JobIdentifiableByUser {

	// parameter names specific to this job
    public static final String PLAYER_NAME = "player name";
    public static final String FACTION_NAME = "faction name";
    public static final String ARMY_WEIGHT = "army weight";
	
    public UserActivityInfluenceJob(){}
    
    /**
     * This method is called every X seconds, where X is defined by the server config variable
     * "Scheduler_PlayerActivity_flu." 
     * 
     * @param JobExecutionContext - data provided by the Quartz Scheduler
     */
	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {

        JobDataMap data = context.getJobDetail().getJobDataMap();
        String playerName = data.getString(PLAYER_NAME);
        //String factionName = data.getString(FACTION_NAME);
        //Double armyWeight = data.getDoubleFromString(ARMY_WEIGHT);
        
        SPlayer p = CampaignMain.cm.getPlayer(playerName);
        if (p == null) {
        	MWLogger.errLog("Null player " + playerName + " in UserActivityInfluenceJob.");
        	UserActivityInfluenceJob.stop(playerName);
        	return;
        }
        if(userGetsInfluence(p)) {
        	int flu = calculateInfluence(p);
        	p.addInfluence(flu);
        	String fluMessage = getInfluenceMessage(p, flu);
            CampaignMain.cm.toUser(fluMessage, playerName);
        }
	}

	/**
	 * A method to build the Influence Job with a specified interval
	 * @param userName - the name of the user going active
	 * @param weightedArmyValue the value of the player's armies
	 * @param factionName the faction the player fights for
	 * @Wparam frequency how often (in seconds) the player is granted flu
	 */
	public static void submit(String userName, Double weightedArmyValue, String factionName, int frequency) {
		JobDetail job = newJob(UserActivityInfluenceJob.class)
				.withIdentity(userName + "_flu", "ActivityGroup")
				.build();
		
		Trigger trigger = newTrigger()
				.withIdentity(userName + "_fluTrigger", "ActivityGroup")
				.startAt(new Date(Calendar.getInstance().getTimeInMillis() + frequency*1000))
				.withSchedule(simpleSchedule()
						.withIntervalInSeconds(frequency)
						.repeatForever())
				.build();
		
		// pass initialization parameters into the job
        job.getJobDataMap().put(UserActivityComponentsJob.ARMY_WEIGHT, Double.toString(weightedArmyValue));
        job.getJobDataMap().put(UserActivityComponentsJob.FACTION_NAME, factionName);
        job.getJobDataMap().put(UserActivityComponentsJob.PLAYER_NAME, userName);
        
        // Bug: jobs not getting removed after games - check if there is one hanging out
        stop(userName);
        
		MWScheduler.getInstance().scheduleJob(job, trigger);
	}
	
	/**
	 * A method to build the Influence Job and get it into the scheduler.  Called when the user
	 * issues an Activate command
	 * @param userName - the name of the user going active
	 * @param weightedArmyValue the value of the player's armies
	 * @param factionName the faction the player fights for
	 */
	public static void submit(String userName, Double weightedArmyValue, String factionName) {
		int frequency = CampaignMain.cm.getIntegerConfig("Scheduler_PlayerActivity_flu");
		
		submit(userName, weightedArmyValue, factionName, frequency);
	}
	
	/**
	 * A method to stop execution of this job and remove it from the scheduler.  Called when the player deactivates.
	 * @param userName
	 */
	public static void stop(String userName) {
		TriggerKey key = new TriggerKey(userName + "_fluTrigger", "ActivityGroup");
		MWScheduler.getInstance().unscheduleJob(key);
	}
	
	/**
	 * A method to determine if the player qualifies for influence distribution.
	 * @param p - the player
	 * @return true if the player qualifies, false if not
	 */
	private boolean userGetsInfluence(SPlayer p) {
		// cant get any inf beyond ceiling, so no reason to do the math
        int fluCeiling = Integer.parseInt(p.getMyHouse().getConfig("InfluenceCeiling"));
        int influence = p.getInfluence();
        if (influence >= fluCeiling) {
        	return false;
        }
        
        // mercs who are active but w/o contract get no flu
        if (p.getHouseFightingFor().isMercHouse()) {
            return false;
        }
        
        boolean activeLongEnough = (System.currentTimeMillis() - p.getActiveSince()) > Integer.parseInt(p.getMyHouse().getConfig("InfluenceTimeMin"));
        if(!activeLongEnough) {
        	return false;
        }
        
        if (p.getDutyStatus() != SPlayer.STATUS_ACTIVE) {
        	return false;
        }
        
        if (p.getWeightedArmyNumber() <= 0) {
        	return false;
        }
        
        if (p.getDutyStatus() != SPlayer.STATUS_ACTIVE) {
        	return false;
        }
        
        return true;
	}
	
	/**
	 * A method to calculate how much influence the player will receive, based on the army weight, and the
	 * settings of the server
	 * @param p - the player
	 * @return How much influence the player receives.
	 */
	private int calculateInfluence(SPlayer p) {
    	double weightedNumArmies = p.getWeightedArmyNumber();
    	double totalInfluenceGrant = 0;
    	double baseFlu = Double.parseDouble(p.getMyHouse().getConfig("BaseInfluence"));
    	totalInfluenceGrant = (baseFlu * weightedNumArmies);
    	int influence = p.getInfluence();
    	int fluCeiling = Integer.parseInt(p.getMyHouse().getConfig("InfluenceCeiling"));
    	
    	/*
    	 * Reduce flu gain for folks who have a lot of flu already.  80% yield
    	 * above half max, 60% above 3/4 max
    	 */
        if ((influence > (fluCeiling * .5)) && (influence < (fluCeiling * .75))) {
            totalInfluenceGrant = totalInfluenceGrant * .80;
        } else if (influence >= fluCeiling * .75) {
            totalInfluenceGrant = totalInfluenceGrant * .60;
        }
        
        int intFluToAdd = (int) totalInfluenceGrant;
        int newFlu = influence + intFluToAdd;
        if (newFlu > fluCeiling) {
        	intFluToAdd -= (newFlu - fluCeiling);
        }
        return intFluToAdd;
	}
	
	/**
	 * Builds a string with the message the player will receive when he gets his flu.
	 * @param p - the player
	 * @param flu - how much influence
	 * @return the message
	 */
	private String getInfluenceMessage(SPlayer p, int flu) {
		String fileName = "";
        SHouse faction = p.getHouseFightingFor();

        if (faction == null) {
            fileName = "./data/influencemessages/CommonInfluenceMessages.txt";
        } else {
            fileName = "./data/influencemessages/" + faction.getHouseFluFile() + "InfluenceMessages.txt";
        }

        File messageFile = new File(fileName);
        if (!messageFile.exists()) {
            fileName = "./data/influencemessages/CommonInfluenceMessages.txt";
            messageFile = new File(fileName);
            if (!messageFile.exists()) {
                MWLogger.errLog("A problem occured with your CommonInfluenceMessages File!");
                return "";
            }
        }

        Random rand = new Random();
		String fluMessage = "";
		FileInputStream fis = null;
		BufferedReader dis = null;
		try {
			fis = new FileInputStream(messageFile);
			dis = new BufferedReader(new InputStreamReader(fis));

			MWLogger.debugLog("getting random flu message");
			int messages = Integer.parseInt(dis.readLine());
			int messageLine = rand.nextInt(messages);
			while (dis.ready()) {
			    fluMessage = dis.readLine();
			    if (messageLine <= 0) {
			        break;
			    }
			    messageLine--;
			}

			dis.close();
			fis.close();
		} catch (NumberFormatException e) {
			MWLogger.errLog(e);
		} catch (FileNotFoundException e) {
			MWLogger.errLog(e);
		} catch (IOException e) {
			MWLogger.errLog(e);
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException e) {
					MWLogger.errLog(e);
				}
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					MWLogger.errLog(e);
				}
			}
		}

        Vector<SUnit> units = p.getUnits();
        int unitId = rand.nextInt(units.size());// random
        SUnit unitForMessages = units.elementAt(unitId);
        String fluMessageWithPilotName = fluMessage.replaceAll("PILOT", unitForMessages.getPilot().getName());
        String fluMessageWithModelName = fluMessageWithPilotName.replaceAll("UNIT", unitForMessages.getModelName());
        String fluMessageWithPlayerName = fluMessageWithModelName.replaceAll("PLAYER", p.getName());

        fluMessageWithPlayerName += " (" + CampaignMain.cm.moneyOrFluMessage(false, false, flu, true) + ")";
        return fluMessageWithPlayerName;
	}


	/**
	 * A method to reschedule the job given a JobExecutionContext and a flexible time constraint
	 * @param frequency the number of intervals at which the job will fire
	 * @param interval how long (second, hour, day, etc) each interval is
	 * @param context the JobExecutionContext containins the information required by the job
	 */
	@Override
	public void reschedule(int frequency, int interval, JobExecutionContext context) {
		if (interval == ScheduleHandler.INTERVAL_SECONDS) {
			rescheduleByContext(frequency, context);
		}
	}

	/**
	 * A method to reschedule the job given a JobExecutionContext
	 * @param seconds the frequency of the job
	 * @param context the JobExecutionContext containins the information required by the job
	 */
	@Override
	public void rescheduleByContext(int seconds, JobExecutionContext context) {
		JobDataMap data = context.getJobDetail().getJobDataMap();
        String playerName = data.getString(PLAYER_NAME);
        String factionName = data.getString(FACTION_NAME);
        Double armyWeight = data.getDoubleFromString(ARMY_WEIGHT);
        
        UserActivityInfluenceJob.stop(playerName);
        UserActivityInfluenceJob.submit(playerName, armyWeight, factionName, seconds);
	}

	/**
	 * A method to get the TriggerKey identifying the influence job for a particular user
	 * 
	 * @param userName the name of the user we're searching for
	 * @return TriggerKey
	 */
	@Override
	public TriggerKey getKey(String userName) {
		return new TriggerKey(userName + "_fluTrigger", "ActivityGroup");
	}
	
	/**
	 * A method to reschedule a particular user's influence job at a given frequency
	 * This is called if the admin changes the influence frequency, so users
	 * are not forced inactive.
	 * 
	 * @param frequency the new frequency in seconds
	 * @param userName the name of the user to change the job for
	 */
	public void rescheduleJob(int frequency, String userName) {
		TriggerKey key = getKey(userName);
		Trigger trigger = newTrigger()
				.withIdentity(userName + "_fluTrigger", "ActivityGroup")
				.startAt(new Date(Calendar.getInstance().getTimeInMillis() + frequency*1000))
				.withSchedule(simpleSchedule()
						.withIntervalInSeconds(frequency)
						.repeatForever())
				.build();
		MWScheduler.getInstance().rescheduleJob(key, trigger);
	}
}
