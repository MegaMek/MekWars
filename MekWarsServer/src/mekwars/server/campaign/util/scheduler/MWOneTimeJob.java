package mekwars.server.campaign.util.scheduler;

/**
 * An interface describing a Quartz job meant to run at a specified time
 *
 * @author Spork
 * @version 2016.10.10
 *
 */
public interface MWOneTimeJob {
    public void reschedule(java.util.Date date);
}
