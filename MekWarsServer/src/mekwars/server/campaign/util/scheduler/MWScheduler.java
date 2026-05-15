package mekwars.server.campaign.util.scheduler;

import common.CampaignData;
import common.House;
import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

/**
 * A wrapper for a Quartz scheduler.  This class handles all the ancillary stuff that comes with scheduling -
 * rescheduling, starting and stopping jobs, etc.
 *
 * @author Spork
 * @version 2016.10.10
 */
public class MWScheduler implements ScheduleHandler {

    private static mekwars.server.campaign.util.scheduler.MWScheduler handler = null;
    private static Scheduler scheduler = null;

    /**
     * Exists solely to defeat instantiation.
     *
     * @return
     */
    protected MWScheduler() {}

    /**
     * This is a singleton, so get the existant instance
     *
     * @return the scheduler
     */
    public static mekwars.server.campaign.util.scheduler.MWScheduler getInstance() {
        if (handler == null) {
            handler = new mekwars.server.campaign.util.scheduler.MWScheduler();
        }
        return handler;
    }

    /**
     * Create activation jobs
     * <p>
     * This will create a UserActivityInfluenceJob and a UserActivityComponentsJob when a user activates.  This is
     * called from ActivateCommand
     *
     * @param userName   the user activating
     * @param armyWeight the weight of the user's armies
     * @param houseName  the faction the user fights for
     */
    @Override
    public void activateUser(String userName, double armyWeight, String houseName) {
        UserActivityInfluenceJob.submit(userName, armyWeight, houseName);
        UserActivityComponentsJob.submit(userName, armyWeight, houseName);
    }

    /**
     * Delete activation jobs
     * <p>
     * This will delete the user's UserActivityInfluenceJob and UserActivityComponentsJob.
     * <p>
     * Called from DeactivateCommand
     *
     * @param userName the user deactivating
     */
    @Override
    public void deactivateUser(String userName) {
        UserActivityInfluenceJob.stop(userName);
        UserActivityComponentsJob.stop(userName);
    }

    /**
     * Change the frequency of a job
     *
     * @param scheduleType the type of Job being rescheduled
     * @param frequency    The number of Intervals between job runs
     * @param interval     how long each interval is (second, day, hour, etc)
     */
    @Override
    public void changeFrequency(int scheduleType, int frequency, int interval) {
        // TODO Auto-generated method stub
        if (scheduleType == ScheduleHandler.TYPE_ACTIVITY_COMPONENTS) {

        } else if (scheduleType == ScheduleHandler.TYPE_ACTIVITY_INFLUENCE) {

        } else if (scheduleType == ScheduleHandler.TYPE_CHRISTMAS_START) {

        } else if (scheduleType == ScheduleHandler.TYPE_CHRISTMAS_END) {

        } else {
            MWLogger.errLog("Unknown ScheduleType in changeFrequency: " + scheduleType);
            CampaignMain.campaignMain.doSendModMail("SERVER",
                  "Unknown ScheduleType in changeFrequency: " + scheduleType);
        }
    }

    /**
     * Create and start the scheduler
     */
    @Override
    public void start() {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            MWLogger.errLog(e);
            MWLogger.errLog("Unable to start scheduler!");
        }
    }

    /**
     * Create starting and ending Christmas jobs
     *
     * @param startDate The start of Christmas season.  Christmas units will start being handed out at 00:00:01 of this
     *                  date
     * @param endDate   The end of Christmas season.  Christmas units will stop being handed out at 23:59:59 of this
     *                  date
     */
    @Override
    public void scheduleChristmas(java.util.Date startDate, java.util.Date endDate) {
        // TODO Auto-generated method stub
        MWLogger.infoLog("Scheduling Christmas");
    }

    /**
     * Schedule a generic job.
     * <p>
     * This should be called from the submit() methods on all jobs
     *
     * @param job     The job being scheduled
     * @param trigger The trigger for the job
     */
    @Override
    public void scheduleJob(JobDetail job, Trigger trigger) {
        try {
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            MWLogger.errLog(e);
        }
    }

    /**
     * Unschedule a generic job.
     *
     * @param key The TriggerKey identifying the job being unscheduled
     */
    @Override
    public void unscheduleJob(TriggerKey key) {
        try {
            scheduler.unscheduleJob(key);
        } catch (SchedulerException e) {
            MWLogger.errLog(e);
        }
    }

    /**
     * Get a JobExecutionContext given a TriggerKey describing the job
     *
     * @param key the TriggerKey identifying the job
     *
     * @return the JobExecutionContext
     */
    @Override
    public JobExecutionContext getContext(TriggerKey key) {
        return null;
    }

    /**
     * Kill the scheduler
     */
    @Override
    public void shutdown() {
        try {
            scheduler.shutdown();
        } catch (SchedulerException e) {
            CampaignMain.campaignMain.doSendModMail("SERVER", e.getStackTrace().toString());
            MWLogger.errLog(e);
        }
    }

    /**
     * Change the schedule of a job
     *
     * @param oldKey     the TriggerKey identifying the job in question
     * @param newTrigger the new scheduled Trigger for the job
     */
    public void rescheduleJob(TriggerKey oldKey, Trigger newTrigger) {
        try {
            scheduler.rescheduleJob(oldKey, newTrigger);
        } catch (SchedulerException e) {
            MWLogger.errLog(e);
        }
    }

    /**
     * Change all active players' job schedules
     * <p>
     * This function iterates over active and fighting players and reschedules their UserActivityComponentsJob and
     * UserActivityInfluenceJob That way, the players do not have to deactivate to get the new schedules
     * <p>
     * Called when an admin changes the values of Scheduler_PlayerActivity_comps or Scheduler_PlayerActivity_flu through
     * the ServerConfig dialog
     */
    public void rescheduleAllActivePlayers() {
        UserActivityComponentsJob cj = new UserActivityComponentsJob();
        UserActivityInfluenceJob ij = new UserActivityInfluenceJob();
        int compsFrequency = CampaignMain.campaignMain.getIntegerConfig("Scheduler_PlayerActivity_comps");
        int fluFrequency = CampaignMain.campaignMain.getIntegerConfig("Scheduler_PlayerActivity_flu");

        for (House h : CampaignData.cd.getAllHouses()) {
            server.campaign.SHouse s = (server.campaign.SHouse) h;
            for (server.campaign.SPlayer p : s.getActivePlayers().values()) {
                cj.rescheduleJob(compsFrequency, p.getName());
                ij.rescheduleJob(fluFrequency, p.getName());
            }
            for (server.campaign.SPlayer p : s.getFightingPlayers().values()) {
                cj.rescheduleJob(compsFrequency, p.getName());
                ij.rescheduleJob(fluFrequency, p.getName());
            }
        }
    }
}
