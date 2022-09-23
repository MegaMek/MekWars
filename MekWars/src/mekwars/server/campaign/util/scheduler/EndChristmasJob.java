package server.campaign.util.scheduler;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import common.util.MWLogger;
import server.campaign.CampaignMain;
import server.campaign.util.ChristmasHandler;

/**
 * Ends the Christmas season
 * 
 * @author Spork
 * @version 2016.10.26
 */
public class EndChristmasJob implements Job {
	public EndChristmasJob() {
		
	}
	
	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		ChristmasHandler.getInstance().endChristmas();
	}
	
	/**
	 * Get the StartChristmasJob into the scheduler
	 */
	public static void submit() {
		JobDetail job = newJob(EndChristmasJob.class)
				.withIdentity("EndChristmas", "ChristmasGroup")
				.build();
		
		String endDateString = CampaignMain.cm.getConfig("Christmas_EndDate") + " 23:59:59";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		
		Date date = new Date();
		try {
			date = sdf.parse(endDateString);
		} catch (ParseException e) {
			MWLogger.errLog(e);
		}
		
		Trigger trigger = newTrigger()
				.withIdentity("endChristmasTrigger", "ChristmasGroup")
				.startAt(date)
				.build();
        
		MWScheduler.getInstance().scheduleJob(job, trigger);
	}
	
	/**
	 * A method to stop execution of this job and remove it from the scheduler.  Called when rescheduling Christmas.
	 * @param userName
	 */
	public static void stop() {
		TriggerKey key = new TriggerKey("endChristmasTrigger", "ChristmasGroup");
		MWScheduler.getInstance().unscheduleJob(key);
	}
}
