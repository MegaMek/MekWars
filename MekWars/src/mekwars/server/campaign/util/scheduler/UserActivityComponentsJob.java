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

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import common.campaign.operations.Operation;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.operations.ShortOperation;


/**
 * A class to handle generation of Components due to player activity
 * 
 * @author Spork
 * @version 2016.10.06
 */
public class UserActivityComponentsJob implements Job, MWRepeatingJob, JobIdentifiableByUser {

	// parameter names specific to this job
    public static final String PLAYER_NAME = "player name";
    public static final String FACTION_NAME = "faction name";
    public static final String ARMY_WEIGHT = "army weight";
	
    public UserActivityComponentsJob(){}
    
    /**
     * This method is called every X seconds, where X is defined by the server config variable
     * "Scheduler_PlayerActivity_comps." 
     * 
     * @param JobExecutionContext - data provided by the Quartz Scheduler
     */
    @Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
        
        // Grab and print passed parameters
        JobDataMap data = context.getJobDetail().getJobDataMap();
        String playerName = data.getString(PLAYER_NAME);
        //String factionName = data.getString(FACTION_NAME);
        //Double armyWeight = data.getDoubleFromString(ARMY_WEIGHT);

        SPlayer p = CampaignMain.cm.getPlayer(playerName);
        SHouse house = p.getMyHouse();
        
        if (playerCountsForProduction(p)) {
        	Double value = howMuch(p);
            DecimalFormat myFormatter = new DecimalFormat("###.##");
            String output = myFormatter.format(value);
            String toShow = "AM:You counted towards production (" + output + " points worth)";
            CampaignMain.cm.toUser(toShow + ".", p.getName(), true);
        	house.addActivityPP(value);
        }
        
	}
    
	/**
	 * A method to build the Components Job with a specified interval
	 * @param userName - the name of the user going active
	 * @param weightedArmyValue the value of the player's armies
	 * @param factionName the faction the player fights for
	 * @param frequency how often (in seconds) a player counts for production
	 */
	public static void submit(String userName, Double weightedArmyValue, String factionName, int frequency) {
		JobDetail job = newJob(UserActivityComponentsJob.class)
				.withIdentity(userName + "_comps", "ActivityGroup")
				.build();
		
		Trigger trigger = newTrigger()
				.withIdentity(userName + "_compsTrigger", "ActivityGroup")
				.startAt(new Date(Calendar.getInstance().getTimeInMillis() + frequency*1000))
				.withSchedule(simpleSchedule()
						.withIntervalInSeconds(frequency)
						.repeatForever())
				.build();
		
		// pass initialization parameters into the job
        job.getJobDataMap().put(UserActivityComponentsJob.ARMY_WEIGHT, Double.toString(weightedArmyValue));
        job.getJobDataMap().put(UserActivityComponentsJob.FACTION_NAME, factionName);
        job.getJobDataMap().put(UserActivityComponentsJob.PLAYER_NAME, userName);
        
        // Bug: jobs not being stopped after games - check if there is one hanging out
        stop(userName);
        
        MWScheduler.getInstance().scheduleJob(job, trigger);
 	}
    

	/**
	 * A method to build the Components Job and get it into the scheduler.  Called when the user
	 * issues an Activate command
	 * @param userName - the name of the user going active
	 * @param weightedArmyValue the value of the player's armies
	 * @param factionName the faction the player fights for
	 */
    public static void submit(String userName, Double weightedArmyValue, String factionName) {
        int frequency = CampaignMain.cm.getIntegerConfig("Scheduler_PlayerActivity_comps");
		submit(userName, weightedArmyValue, factionName, frequency);
	}
	
	/**
	 * A method to stop execution of this job and remove it from the scheduler.  Called when the player deactivates.
	 * @param userName
	 */
    public static void stop(String userName) {
		TriggerKey key = new TriggerKey(userName + "_compsTrigger", "ActivityGroup");
		MWScheduler.getInstance().unscheduleJob(key);
	}
	
	/**
	 * A method to determine if the player counts for generating components, based on server settings.
	 * @param p - the player
	 * @return true if the player qualifies, false if not
	 */
    private boolean playerCountsForProduction(SPlayer p) {
		if (p.getWeightedArmyNumber() <= 0) {
			return false;
		}
		
        if (System.currentTimeMillis() < p.getActiveSince() + Long.parseLong(p.getMyHouse().getConfig("InfluenceTimeMin"))) {
            return false;
        }
        
        if ((p.getDutyStatus() != SPlayer.STATUS_ACTIVE) 
        		&& (p.getDutyStatus() != SPlayer.STATUS_FIGHTING)) {
        	return false;
        }
       
		return true;
	}
    
    /**
     * A method to determine how much a player is worth
     * @param p the Player
     * @return how much he is worth in Component Generation
     */
    private double howMuch(SPlayer p) {
    	if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE) {
    		return p.getWeightedArmyNumber();
    	}
    	
    	if (p.getDutyStatus() == SPlayer.STATUS_FIGHTING) {
            ShortOperation so = CampaignMain.cm.getOpsManager().getShortOpForPlayer(p);
            if (so == null) {
            	return 0.0;
            }
            
            Operation o = CampaignMain.cm.getOpsManager().getOperation(so.getName());
            double value = o.getDoubleValue("CountGameForProduction");
            if (value < 0) {
                value = 0.0;
            }
            return value;
    	}
    	return 0.0;
    }

	/**
	 * A method to reschedule the job given a JobExecutionContext and a flexible time constraint
	 * @param frequency the number of intervals at which the job will fire
	 * @param interval how long (second, hour, day, etc) each interval is
	 * @param context the JobExecutionContext containins the information required by the job
	 */
	@Override
	public void reschedule(int frequency, int interval,
			JobExecutionContext context) {
		if (interval == ScheduleHandler.INTERVAL_SECONDS) {
			rescheduleByContext(frequency, context);
		}
	}
	
	/**
	 * A method to reschedule the job given a JobExecutionContext
	 * @param seconds the frequency of the job
	 * @param context the JobExecutionContext containins the information required by the job
	 */
	public void rescheduleByContext(int seconds, JobExecutionContext context) {
        JobDataMap data = context.getJobDetail().getJobDataMap();
        String playerName = data.getString(PLAYER_NAME);
        String factionName = data.getString(FACTION_NAME);
        Double armyWeight = data.getDoubleFromString(ARMY_WEIGHT);
        
        
        
        UserActivityComponentsJob.stop(playerName);
        UserActivityComponentsJob.submit(playerName, armyWeight, factionName, seconds);

	}

	/**
	 * A method to reschedule a player's component job at a given frequency
	 * This is called when the admin changes the frequency of the jobs, so the
	 * users do not have to be deactivated.
	 * @param frequency how often (in seconds) the player will count for production
	 * @param userName the user we are rescheduling
	 */
	public void rescheduleJob(int frequency, String userName) {
		TriggerKey key = getKey(userName);
		Trigger trigger = newTrigger()
				.withIdentity(userName + "_compsTrigger", "ActivityGroup")
				.startAt(new Date(Calendar.getInstance().getTimeInMillis() + frequency*1000))
				.withSchedule(simpleSchedule()
						.withIntervalInSeconds(frequency)
						.repeatForever())
				.build();
		MWScheduler.getInstance().rescheduleJob(key, trigger);
	}

	/**
	 * A method to find the TriggerKey identifying the components job for a particular player
	 * @param userName the player we're searching for
	 * @return the TriggerKey identifying the player's components job
	 */
	public TriggerKey getKey(String userName) {
		return new TriggerKey(userName + "_compsTrigger", "ActivityGroup");
	}
	
}
