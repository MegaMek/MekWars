package mekwars.common.gui.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.util.StringTokenizer;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.panels.CHSPanel;

public class BuyPopupListener extends MouseAdapter implements ActionListener {
    private final CHSPanel chsPanel;

    public BuyPopupListener(CHSPanel chsPanel) {
        this.chsPanel = chsPanel;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        String actionCommand = actionEvent.getActionCommand();
        StringTokenizer stringTokenizer = new StringTokenizer(actionCommand, "|");
        String command = stringTokenizer.nextToken();

        if (command.equalsIgnoreCase("BUY")) {
            chsPanel.getClient()
                  .sendChat(String.format("%sc request#%s#%s", IClient.CAMPAIGN_PREFIX, stringTokenizer.nextToken(), stringTokenizer.nextToken()));
        } else if (command.equalsIgnoreCase("BUYU")) {
            chsPanel.getClient()
                  .sendChat(String.format("%sc requestdonated#%s#%s", IClient.CAMPAIGN_PREFIX, stringTokenizer.nextToken(), stringTokenizer.nextToken()));
        } else if (command.equalsIgnoreCase("BUYP")) {
            chsPanel.getClient()
                  .sendChat(String.format("%sc buypilotsfromhouse#%s#%s", IClient.CAMPAIGN_PREFIX, stringTokenizer.nextToken(), stringTokenizer.nextToken()));
        }
    }
}
