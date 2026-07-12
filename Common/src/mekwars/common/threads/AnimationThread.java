package mekwars.common.threads;

import mekwars.common.gui.SplashWindow;

/**
 * Drives the animated status text and progress bar on the client's {@link SplashWindow} while the
 * application is starting up (initializing, building the Swing GUI, downloading campaign data from
 * the server, and connecting to the server socket).
 * <p>
 * This thread has no interaction with the network reader/writer threads directly; it only polls
 * {@link SplashWindow}'s status flag (set elsewhere as startup progresses) on a fixed interval and
 * updates the splash screen's label text and progress bar to reflect it. It runs until
 * {@link SplashWindow#shouldAnimate()} returns false (e.g. once startup completes or the splash is
 * dismissed).
 */
public class AnimationThread extends Thread {

    //vars
    /** The splash screen window whose status/progress bar/label this thread updates. */
    private final SplashWindow splash;
    private final String initializing = "Initializing";
    private final String constructing = "Constructing GUI";
    private final String fetching = "Downloading Data";
    private final String connecting = "Connecting to Server";
    /** Counts 0-3 each animation tick (150ms); every 4th tick (i.e. every ~0.6s) the progress bar advances. */
    private int cycle;
    /** Progress bar value, 0-9, wrapping back to 0 after reaching 10 (a repeating "marquee" effect, not a real percentage). */
    private int progress;

    /**
     * @param s the splash window to animate; its status is polled and its label/progress bar are
     *          updated by {@link #run()}
     */
    public AnimationThread(SplashWindow s) {
        splash = s;
        progress = 0;
        cycle = 0;
    }

    /**
     * Loops every 150ms (via {@link Object#wait(long)} on {@code this}, since the method is
     * {@code synchronized}) while {@link SplashWindow#shouldAnimate()} is true, polling the splash
     * window's current status and:
     * <ul>
     *     <li>advancing the progress bar roughly every 4th tick (~0.6s), unless the status
     *     indicates an error/waiting/failed-connection state, in which case the cycle counter is
     *     rolled back so the progress bar doesn't advance;</li>
     *     <li>updating the splash label text to match the current status (initializing,
     *     downloading data, constructing GUI, connecting, or one of the error/wait variants).</li>
     * </ul>
     * Returns (ending the thread) either when {@code shouldAnimate()} becomes false or if any
     * exception is thrown, in which case the splash label is set to an error message before
     * returning. Note the {@code wait(150)} call has no corresponding {@code notify()} anywhere;
     * it purely relies on the wait timing out after 150ms every iteration to drive the animation
     * tick, so no other thread needs to (or does) signal this one.
     */
    @Override
    public synchronized void run() {

        //shouldAnimate is essentially a perpetual true, but someone
        //suggested this as a potential remedy to the infamous "splash
        //crash," so what the hell ... lets try it! @urgru 11.21.05
        while (splash.shouldAnimate()) {

            try {

                //update the current task every 150 ms (every cycle), add
                //a bullet to progress meter every .6 second (every 4th cycle)
                wait(150);
                cycle++;
                if (cycle == 4) {cycle = 0;}

                int currStatus = splash.getStatus();
                if (currStatus == splash.STATUS_DATA_ERROR ||
                          currStatus == splash.STATUS_INPUT_WAIT ||
                          currStatus == splash.STATUS_CONNECT_FAILED) {
                    //do not advance the progress meter. roll back the cycle.
                    cycle--;
                } else if (cycle == 0) {
                    progress++;
                    if (progress == 10) {progress = 0;}
                }

                if (currStatus == splash.STATUS_INITIALIZING) {
                    this.setLabelText(initializing);
                } else if (currStatus == splash.STATUS_FETCHING_DATA) {
                    this.setLabelText(fetching);
                } else if (currStatus == splash.STATUS_CONSTRUCTING_GUI) {
                    this.setLabelText(constructing);
                } else if (currStatus == splash.STATUS_CONNECTING) {
                    this.setLabelText(connecting);
                } else if (currStatus == splash.STATUS_INPUT_WAIT) {
                    splash.getImageLabel()
                          .setText("<HTML><CENTER><b>Connecting to Server<br>[Waiting for Input]</b></CENTER></HTML>");
                } else if (currStatus == splash.STATUS_DATA_ERROR) {
                    splash.getImageLabel()
                          .setText("<HTML><CENTER><b>Downloading Data<br>[Data Access Error]</b></CENTER></HTML>");
                } else if (currStatus == splash.STATUS_CONNECT_FAILED) {
                    splash.getImageLabel()
                          .setText("<HTML><CENTER><b>Connecting to Server<br>[Connection Failed]</b></CENTER></HTML>");
                }
                if (!splash.shouldAnimate()) {
                    return;
                }
            } catch (Exception e) {
                splash.getImageLabel().setText("Error in animation thread!");
                return;
            }

        }//end while

    }//end run()

    /**
     * Updates the splash window's label to bold, centered HTML text and refreshes its progress bar
     * to the current {@link #progress} value.
     *
     * @param s the plain-text status message to display
     */
    private void setLabelText(String s) {
        splash.getImageLabel().setText(String.format("<HTML><CENTER><b>%s</b></CENTER></HTML>", s));
        splash.getProgressBar().setValue(progress);
    }
}//end AnimationThread
