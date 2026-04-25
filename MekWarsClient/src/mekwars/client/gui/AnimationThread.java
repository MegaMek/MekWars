package mekwars.client.gui;

class AnimationThread extends Thread {

    //vars
    private SplashWindow splash;
    private int cycle;
    private int progress;

    private final String initializing = "Initializing";
    private final String constructing = "Constructing GUI";
    private final String fetching = "Downloading Data";
    private final String connecting = "Connecting to Server";

    public AnimationThread(SplashWindow s) {
        splash = s;
        progress = 0;
        cycle = 0;
    }

    private void setLabelText(String s) {
        splash.getImageLabel().setText("<HTML><CENTER><b>" + s + "</b></CENTER></HTML>");
        splash.getProgressBar().setValue(progress);
    }

    @Override
    public synchronized void run() {

        //shouldAnimate is essentially a perpetual true, but someone
        //suggested this as a potential remedy to the infamous "splash
        //crash," so what the hell ... lets try it! @urgru 11.21.05
        while (splash.shouldAnimate()) {

            try {

                //update the current task every 150 ms (every cycle), add
                //a bullet to progress meter every .6 seconds (every 4th cycle)
                wait(150);
                cycle++;
                if (cycle == 4) {cycle = 0;}

                int currStatus = splash.getStatus();
                if (currStatus == splash.STATUS_DATAERROR ||
                          currStatus == splash.STATUS_INPUTWAIT ||
                          currStatus == splash.STATUS_CONNECTFAILED) {
                    //do not advanced the progress meter. roll back the cycle.
                    cycle--;
                } else if (cycle == 0) {
                    progress++;
                    if (progress == 10) {progress = 0;}
                }


                if (currStatus == splash.STATUS_INITIALIZING) {this.setLabelText(initializing);} else if (currStatus ==
                                                                                                                splash.STATUS_FETCHINGDATA) {
                    this.setLabelText(fetching);
                } else if (currStatus ==
                                 splash.STATUS_CONSTRUCTINGGUI) {
                    this.setLabelText(constructing);
                } else if (currStatus == splash.STATUS_CONNECTING) {
                    this.setLabelText(connecting);
                } else if (currStatus ==
                                 splash.STATUS_INPUTWAIT) {
                    splash.getImageLabel()
                          .setText("<HTML><CENTER><b>Connecting to Server<br>[Waiting for Input]</b></CENTER></HTML>");
                } else if (currStatus == splash.STATUS_DATAERROR) {
                    splash.getImageLabel()
                          .setText("<HTML><CENTER><b>Downloading Data<br>[Data Access Error]</b></CENTER></HTML>");
                } else if (currStatus == splash.STATUS_CONNECTFAILED) {
                    splash.getImageLabel()
                          .setText("<HTML><CENTER><b>Connecting to Server<br>[Connection Failed]</b></CENTER></HTML>");
                }

                if (!splash.shouldAnimate()) {return;}

            } catch (Exception e) {
                splash.getImageLabel().setText("Error in animation thread!");
                return;
            }

        }//end while

    }//end run()
}//end AnimationThread
