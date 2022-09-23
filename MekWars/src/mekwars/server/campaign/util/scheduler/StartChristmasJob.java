package server.campaign.util.scheduler;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import common.util.MWLogger;
import server.campaign.CampaignMain;
import server.campaign.util.ChristmasHandler;

/**
 * Starts the Christmas season
 * 
 * @author Spork
 * @version 2016.10.26
 */
public class StartChristmasJob implements Job {
	public StartChristmasJob() {
	}
	
	@Override
	public void execute(JobExecutionContext context)
		{
		MWLogger.debugLog("Starting Christmas");
		ChristmasHandler.getInstance().startChristmas();
	}
	
	/**
	 * Get the StartChristmasJob into the scheduler
	 */
	public static void submit() {
		JobDetail job = newJob(StartChristmasJob.class)
				.withIdentity("StartChristmas", "ChristmasGroup")
				.build();
		
		String startDateString = CampaignMain.cm.getConfig("Christmas_StartDate");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		Date date = new Date();
		try {
			date = sdf.parse(startDateString);
		} catch (ParseException e) {
			MWLogger.errLog(e);
		}
		
		boolean inThePast = false;
		if(date.before(new Date())) {
			inThePast = true;
		}
		Trigger trigger = null;
		if (inThePast) {
			trigger = newTrigger()
					.withIdentity("StartChristmasTrigger", "ChristmasGroup")
					.startNow()
					.build();
		} else {		
			trigger = newTrigger()
				.withIdentity("StartChristmasTrigger", "ChristmasGroup")
				.startAt(date)
				.build();
		}
        MWScheduler.getInstance().scheduleJob(job, trigger);
	}
	
	/**
	 * A method to stop execution of this job and remove it from the scheduler.  Called when rescheduling Christmas.
	 */
	public static void stop() {
		TriggerKey key = new TriggerKey("StartChristmasTrigger", "ChristmasGroup");
		MWScheduler.getInstance().unscheduleJob(key);
	}
}
