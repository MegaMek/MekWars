package mekwars.server.campaign.commands;

import megamek.logging.MMLogger;
import mekwars.server.campaign.CampaignMain;

/**
 * @author urgru
 *       <p>
 *       private thread. simple minactivetime wait which runs a checkattack for the activating player.
 */
class CheckAttackThread extends Thread {
    private static final MMLogger LOGGER = MMLogger.create(CheckAttackThread.class);

    // vars
    server.campaign.SPlayer p;
    long duration;

    public CheckAttackThread(server.campaign.SPlayer p, int duration) {
        super("ActivationThread-" + p.getName());
        this.duration = duration; // set length when thread is spun
        this.p = p;
    }

    @Override
    public synchronized void run() {
        try {
            wait(duration + 250);// buffer by a quarter second

            // make sure the player is still active (hasnt logged out,
            // been forcedeactivated, attacked or joined a game).
            if (p.getDutyStatus() == server.campaign.SPlayer.STATUS_ACTIVE) {
                CheckAttackCommand ca = new CheckAttackCommand();
                CampaignMain.campaignMain.toUser("<br>You have arrived on the front lines!", p.getName(), true);
                ca.process(new java.util.StringTokenizer(""), p.getName());
            }
            p.leechCount = 0;
            // ran once. kill the thread by returning.
            return;
        } catch (Exception ex) {
            LOGGER.error(ex, "");
        }
    }// end run()
}// end CheckAttackThread
