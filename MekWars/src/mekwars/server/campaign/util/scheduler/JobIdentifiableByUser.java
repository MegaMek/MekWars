package server.campaign.util.scheduler;

import org.quartz.TriggerKey;

/**
 * An interface describing jobs identifiable via user name.
 * 
 * @author Spork
 * @version 2016.10.10
 */
public interface JobIdentifiableByUser {
	
	/**
	 * Get the key that identifies the job, based on the user name
	 * @param userName the user we are searching for
	 * @return the TriggerKey identifying the user's job
	 */
	public TriggerKey getKey(String userName);
	
	/**
	 * Reschedule the job at the given frequency.  The job should use getKey to find the identifying TriggerKey
	 * @param frequency how often (in seconds) the job should fire
	 * @param userName the user we are searching for
	 */
	public void rescheduleJob(int frequency, String userName);
}
