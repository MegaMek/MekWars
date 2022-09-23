package server.campaign.util.scheduler;

import java.util.Date;

/**
 * An interface describing a Quartz job meant to run at a specified time
 * @author Spork
 * @version 2016.10.10
 * 
 */
public interface MWOneTimeJob {
	public void reschedule(Date date);
}
