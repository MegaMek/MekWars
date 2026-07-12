package mekwars.common.gui.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.util.StringTokenizer;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.panels.CHSPanel;

/**
 * Action listener for buy-related buttons/menu items shown in a {@link CHSPanel} (the campaign
 * hangar/shop-style panel where players acquire units, donated units, or house pilots). Each UI
 * element's action command is a pipe ({@code |}) delimited string whose first token selects which
 * campaign-chat purchase command to forward to the server, followed by the arguments for that command
 * (typically a unit/house identifier and a target identifier).
 *
 * <p>Extends {@link MouseAdapter} but does not override any of its mouse-event methods — only the
 * {@link ActionListener} behavior is actually used by this class.
 */
public class BuyPopupListener extends MouseAdapter implements ActionListener {
    private final CHSPanel chsPanel;

    /**
     * @param chsPanel the panel this listener is attached to; used only to obtain the {@code IClient} for sending chat commands
     */
    public BuyPopupListener(CHSPanel chsPanel) {
        this.chsPanel = chsPanel;
    }

    /**
     * Dispatches on the first {@code |}-delimited token of the action command, forwarding the remaining
     * two tokens as arguments to the corresponding server-bound campaign-chat command:
     * <ul>
     *     <li>{@code BUY} → {@code request#<arg1>#<arg2>} (standard unit purchase request)</li>
     *     <li>{@code BUYU} → {@code requestdonated#<arg1>#<arg2>} (request a donated/free unit)</li>
     *     <li>{@code BUYP} → {@code buypilotsfromhouse#<arg1>#<arg2>} (buy pilots from a house/faction)</li>
     * </ul>
     * Any other command is silently ignored. Note this assumes exactly two more tokens are present after
     * the command; if fewer are supplied, {@link StringTokenizer#nextToken()} throws
     * {@link java.util.NoSuchElementException}.
     */
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
