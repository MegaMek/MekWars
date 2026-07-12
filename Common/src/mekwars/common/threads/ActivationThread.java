package mekwars.common.threads;

import javax.swing.Icon;
import javax.swing.JButton;

import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Short-lived UI "attention getter" thread that flashes a {@link JButton}'s icon a couple of times
 * to draw the player's eye to it (e.g. to signal that some client-side activity/button has become
 * available), then leaves the button showing a final icon with a new rollover icon.
 * <p>
 * This is purely a GUI client-side helper (it directly mutates a Swing {@link JButton} from the
 * thread's {@link #run()} method rather than via the Event Dispatch Thread) and is not part of the
 * network/socket machinery used by the other classes in this package. It does not communicate with
 * the server or any reader/writer thread; the {@link IClient} reference is stored but currently
 * unused by the animation logic itself.
 */
public class ActivationThread extends Thread {

    /** Icon shown briefly to draw attention ("flashed" on/off against {@link #startIcon}). */
    Icon flashIcon = null;
    /** The button's icon before the flash animation started; restored between flashes. */
    Icon startIcon = null;
    /** Icon left on the button once the flash animation completes. */
    Icon finishIcon = null;
    /** Rollover icon applied to the button once the flash animation completes. */
    Icon rollOverIcon = null;

    /** Client reference kept for context; not currently used by the flashing animation. */
    IClient iClient = null;
    /** The button being animated. */
    JButton button = null;

    /**
     * Captures the button's current icon as the "start" icon (the state to flash back to) and
     * stores the icons to use during and after the animation. Does not start the thread; the
     * caller must invoke {@link #start()} separately.
     *
     * @param iClient        client reference, stored but unused by the animation
     * @param activityButton the button whose icon will be animated
     * @param flash          icon to alternate with the button's current icon while flashing
     * @param end            icon left on the button after the animation finishes
     * @param roll           rollover icon applied to the button after the animation finishes
     */
    public ActivationThread(IClient iClient, JButton activityButton, Icon flash, Icon end, Icon roll) {

        this.iClient = iClient;
        this.button = activityButton;
        this.startIcon = this.button.getIcon();
        this.flashIcon = flash;
        this.finishIcon = end;
        this.rollOverIcon = roll;
    }

    /**
     * Clears the button's rollover icon, then alternates the button's icon between
     * {@link #flashIcon} and {@link #startIcon} twice (each state held for 550ms, so roughly 2.2
     * seconds total), and finally sets the rollover icon to {@link #rollOverIcon} and the icon to
     * {@link #finishIcon}.
     * <p>
     * Note: this mutates Swing components directly from a background thread rather than via
     * {@code SwingUtilities.invokeLater}, which is not strictly EDT-safe but is how this class has
     * always worked. Any exception during the sleep/icon updates (including
     * {@link InterruptedException}) is silently swallowed.
     */
    public synchronized void run() {
        button.setRolloverIcon(null);
        for (int count = 0; count < 2; count++) {
            try {
                this.button.setIcon(flashIcon);
                Thread.sleep(550);
                this.button.setIcon(startIcon);
                Thread.sleep(550);
            } catch (Exception ex) {

            }
        }
        this.button.setRolloverIcon(rollOverIcon);
        this.button.setIcon(finishIcon);
    }

}
