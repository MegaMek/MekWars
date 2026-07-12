package mekwars.server.campaign.util.scheduler;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import megamek.logging.MMLogger;
import mekwars.server.campaign.CampaignMain;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

/**
 * Ends the Christmas season
 *
 * @author Spork
 * @version 2016.10.26
 */
public class EndChristmasJob implements Job {
    private static final MMLogger LOGGER = MMLogger.create(EndChristmasJob.class);
    public EndChristmasJob() {

    }

    /**
     * Get the StartChristmasJob into the scheduler
     */
    public static void submit() {
        JobDetail job = newJob(mekwars.server.campaign.util.scheduler.EndChristmasJob.class)
                              .withIdentity("EndChristmas", "ChristmasGroup")
                              .build();

        String endDateString = CampaignMain.campaignMain.getConfig("Christmas_EndDate") + " 23:59:59";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        java.util.Date date = new java.util.Date();
        try {
            date = sdf.parse(endDateString);
        } catch (java.text.ParseException e) {
            LOGGER.error(e, "");
        }

        Trigger trigger = newTrigger()
                                .withIdentity("endChristmasTrigger", "ChristmasGroup")
                                .startAt(date)
                                .build();

        MWScheduler.getInstance().scheduleJob(job, trigger);
    }

    /**
     * A method to stop execution of this job and remove it from the scheduler.  Called when rescheduling Christmas.
     *
     * @param userName
     */
    public static void stop() {
        TriggerKey key = new TriggerKey("endChristmasTrigger", "ChristmasGroup");
        MWScheduler.getInstance().unscheduleJob(key);
    }

    @Override
    public void execute(JobExecutionContext context)
          throws JobExecutionException {
        server.campaign.util.ChristmasHandler.getInstance().endChristmas();
    }
}
