package mekwars.client.gui;

class ActivationThread extends Thread {

    javax.swing.Icon flashIcon = null;
    javax.swing.Icon startIcon = null;
    javax.swing.Icon finishIcon = null;
    javax.swing.Icon rollOverIcon = null;

    client.MWClient mwclient = null;
    javax.swing.JButton button = null;

    public ActivationThread(
          client.MWClient mwclient, javax.swing.JButton activityButton, javax.swing.Icon flash, javax.swing.Icon end,
          javax.swing.Icon roll) {
        ;
        this.mwclient = mwclient;
        this.button = activityButton;
        this.startIcon = this.button.getIcon();
        this.flashIcon = flash;
        this.finishIcon = end;
        this.rollOverIcon = roll;
    }

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
