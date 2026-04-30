package mekwars.common.threads;

import javax.swing.Icon;
import javax.swing.JButton;

import mekwars.common.campaign.clientutils.protocol.IClient;

public class ActivationThread extends Thread {

    Icon flashIcon = null;
    Icon startIcon = null;
    Icon finishIcon = null;
    Icon rollOverIcon = null;

    IClient iClient = null;
    JButton button = null;

    public ActivationThread(IClient iClient, JButton activityButton, Icon flash, Icon end, Icon roll) {

        this.iClient = iClient;
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
