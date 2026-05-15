package mekwars.server.campaign.util.scheduler;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

/**
 * Starts the Christmas season
 *
 * @author Spork
 * @version 2016.10.26
 */
public class StartChristmasJob implements Job {
    public StartChristmasJob() {
    }

    /**
     * Get the StartChristmasJob into the scheduler
     */
    public static void submit() {
        JobDetail job = newJob(mekwars.server.campaign.util.scheduler.StartChristmasJob.class)
                              .withIdentity("StartChristmas", "ChristmasGroup")
                              .build();

        String startDateString = CampaignMain.campaignMain.getConfig("Christmas_StartDate");
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");

        java.util.Date date = new java.util.Date();
        try {
            date = sdf.parse(startDateString);
        } catch (java.text.ParseException e) {
            MWLogger.errLog(e);
        }

        boolean inThePast = false;
        if (date.before(new java.util.Date())) {
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

    @Override
    public void execute(JobExecutionContext context) {
        MWLogger.debugLog("Starting Christmas");
        server.campaign.util.ChristmasHandler.getInstance().startChristmas();
    }
}
