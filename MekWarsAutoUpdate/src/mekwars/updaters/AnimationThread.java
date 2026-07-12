/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original File: GraphicGimicks.java
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package mekwars.updaters;

class AnimationThread extends Thread {

    // vars
    private final mekwars.updaters.SplashWindow splash;
    private int cycle;
    private int progress;
    private String progressText = "";

    public AnimationThread(mekwars.updaters.SplashWindow s) {
        splash = s;
        progress = 0;
        cycle = 0;
    }

    protected void setLabelText(String s) {

        this.progressText = "<HTML><CENTER><b>" + s + "</b></CENTER></HTML>";
    }

    @Override
    public synchronized void run() {

        // shouldAnimate is essentially a perpetual true, but someone
        // suggested this as a potential remedy to the infamous "splash
        // crash," so what the hell ... lets try it! @urgru 11.21.05
        while (splash.shouldAnimate()) {

            try {

                // update the current task every 150 ms (every cycle), add
                // a bullet to progress meter every .6 seconds (every 4th cycle)
                wait(150);
                cycle++;
                if (cycle == 4) {
                    cycle = 0;
                }

                int currStatus = splash.getStatus();
                if (currStatus == splash.STATUS_DATA_ERROR ||
                          currStatus == splash.STATUS_INPUT_WAIT ||
                          currStatus == splash.STATUS_CONNECT_FAILED) {
                    // do not advanced the progress meter. roll back the cycle.
                    cycle--;
                } else if (cycle == 0) {
                    progress++;
                    if (progress == 10) {
                        progress = 0;
                    }
                }

                updateProgress();

                if (!splash.shouldAnimate()) {return;}

            } catch (Exception e) {
                splash.getImageLabel().setText("Error in animation thread!");
                return;
            }

        }// end while

    }// end run()

    private void updateProgress() {
        splash.getImageLabel().setText(progressText);
        // splash.getProgressBar().setValue(progress);
    }
}// end CheckAttackThread
