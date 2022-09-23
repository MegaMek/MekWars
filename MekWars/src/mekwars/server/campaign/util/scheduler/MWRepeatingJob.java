package server.campaign.util.scheduler;

import org.quartz.JobExecutionContext;

/**
 * An interface describing a Quartz job which repeats after execution
 * @author Spork
 * @version 2016.10.10
 */
public interface MWRepeatingJob {
	public void reschedule(int frequency, int interval, JobExecutionContext context);
	
	public void rescheduleByContext(int seconds, JobExecutionContext context);
}
