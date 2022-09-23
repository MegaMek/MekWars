package server.campaign.util.scheduler;

import java.util.Date;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

/**
 * An interface describing the Scheduler for MekWars
 * 
 * This will be essentially a wrapper for the Quartz Scheduler
 * @author Spork
 * @version 2016.10.10
 */
public interface ScheduleHandler {
	/**
	 * Job Types
	 */
	public final int TYPE_ACTIVITY_INFLUENCE = 0;
	public final int TYPE_ACTIVITY_COMPONENTS = 1;
	public final int TYPE_CHRISTMAS_START = 2;
	public final int TYPE_CHRISTMAS_END = 3;
	
	/**
	 * Interval types - currently, only seconds are supported, but I'll change that when I have need
	 */
	public final int INTERVAL_MILLISECONDS = 0;
	public final int INTERVAL_SECONDS = 1;
	public final int INTERVAL_MINUTES = 2;
	public final int INTERVAL_HOURS = 3;
	public final int INTERVAL_DAYS = 4;
	public final int INTERVAL_WEEKS = 5;
	public final int INTERVAL_MONTHS = 6;
	public final int INTERVAL_YEARS = 7;
	
	/**
	 * Create activation jobs
	 * 
	 * This will create a UserActivityInfluenceJob and a UserActivityComponentsJob
	 * when a user activates.  This is called from ActivateCommand
	 * 
	 * @param userName the user activating
	 * @param armyWeight the weight of the user's armies
	 * @param houseName the faction the user fights for
	 */
	public void activateUser(String userName, double armyWeight, String houseName);
	
	/**
	 * Delete activation jobs
	 * 
	 * This will delete the user's UserActivityInfluenceJob and UserActivityComponentsJob.
	 * 
	 * Called from DeactivateCommand
	 * 
	 * @param userName the user deactivating
	 */
	public void deactivateUser(String userName);
	
	/**
	 * Change the frequency of a job
	 * 
	 * @param scheduleType the type of Job being rescheduled
	 * @param frequency The number of Intervals between job runs
	 * @param interval how long each interval is (second, day, hour, etc)
	 */
	public void changeFrequency(int scheduleType, int frequency, int interval);
	
	/**
	 * Create and start the scheduler
	 */
	public void start();
	
    /**
     * Create starting and ending Christmas jobs
     * @param startDate The start of Christmas season.  Christmas units will start being
     *                  handed out at 00:00:01 of this date
     * @param endDate   The end of Christmas season.  Christmas units will stop being handed out at 23:59:59 of this date 
     */
	public void scheduleChristmas(Date startDate, Date endDate);
	
	/**
	 * Schedule a generic job.  
	 * 
	 * This should be called from the submit() methods on all jobs
	 * 
	 * @param job     The job being scheduled
	 * @param trigger The trigger for the job 
	 */
	public void scheduleJob(JobDetail job, Trigger trigger);
	
	/**
	 * Unschedule a generic job.
	 * 
	 * @param key The TriggerKey identifying the job being unscheduled
	 */
	public void unscheduleJob(TriggerKey key);
	
	/**
	 * Get a JobExecutionContext given a TriggerKey describing the job
	 * 
	 * @param key the TriggerKey identifying the job
	 * @return the JobExecutionContext
	 */
	public JobExecutionContext getContext(TriggerKey key);
	
	/**
	 * Kill the scheduler
	 */
	public void shutdown();
}
