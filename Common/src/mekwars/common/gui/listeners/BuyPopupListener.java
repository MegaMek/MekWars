package mekwars.common.gui.listeners;

import mekwars.common.gui.panels.CHSPanel;

public class BuyPopupListener extends java.awt.event.MouseAdapter implements java.awt.event.ActionListener {
    private final CHSPanel chsPanel;

    public BuyPopupListener(CHSPanel chsPanel) {this.chsPanel = chsPanel;}

    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        String s = actionEvent.getActionCommand();
        java.util.StringTokenizer st = new java.util.StringTokenizer(s, "|");
        String command = st.nextToken();

        if (command.equalsIgnoreCase("BUY")) {
            chsPanel.client.sendChat(chsPanel.client.MWClient.CAMPAIGN_PREFIX +
                                           "c request#" +
                                           st.nextToken() +
                                           "#" +
                                           st.nextToken());
            // (client.getMainFrame().getMainPanel().getCommPanel()).
            // removeHttpLinksFromEditorPane(CCommPanel.CHANNEL_MISC);
        } else if (command.equalsIgnoreCase("BUYU")) {
            chsPanel.client.sendChat(chsPanel.client.MWClient.CAMPAIGN_PREFIX +
                                           "c requestdonated#" +
                                           st.nextToken() +
                                           "#" +
                                           st.nextToken());
            // (client.getMainFrame().getMainPanel().getCommPanel()).
            // removeHttpLinksFromEditorPane(CCommPanel.CHANNEL_MISC);
        } else if (command.equalsIgnoreCase("BUYP")) {
            chsPanel.client.sendChat(chsPanel.client.MWClient.CAMPAIGN_PREFIX +
                                           "c buypilotsfromhouse#" +
                                           st.nextToken() +
                                           "#" +
                                           st.nextToken());
            // (client.getMainFrame().getMainPanel().getCommPanel()).
            // removeHttpLinksFromEditorPane(CCommPanel.CHANNEL_MISC);
        }
    }
}
