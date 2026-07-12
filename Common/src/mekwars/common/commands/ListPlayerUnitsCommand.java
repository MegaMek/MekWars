/*
 * Copyright (C) 2004 - Nathan Morris (urgru@users.sourceforge.net)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */


package mekwars.common.commands;

import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Command that sends a player's unit list.
 * <p>
 * Client-side handler for the {@code ListPlayerUnitsCommand} protocol message. The server sends this in response
 * to a request that needs the user to pick one specific unit out of a given player's hangar (e.g. an admin
 * transferring a unit, or viewing another player's unit). Executing it pops up a modal unit-picker dialog and,
 * once the player chooses a unit and confirms, sends a follow-up chat/campaign command back to the server
 * encoding that choice.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class ListPlayerUnitsCommand extends Command {

    /**
     *
     */
    public ListPlayerUnitsCommand(IClient client) {
        super(client);
    }

    /**
     * Parses, in order: the original command name that triggered this unit list (e.g. {@code admin_transfer} or
     * {@code view_player_unit}), the target username whose hangar is being listed, a {@code '#'}-delimited list of
     * unit entries, and an optional trailing "receiving player" name (present only for commands that move a unit
     * to a second player).
     * <p>
     * The unit entries are split on {@code '#'}, each re-prefixed with {@code '#'}, and loaded into a
     * {@link TreeSet} (which both de-duplicates and alphabetizes them) to populate a non-editable
     * {@link JComboBox}. That combo box is shown in a modal {@link JOptionPane} with OK/Cancel options.
     * <p>
     * If the dialog is cancelled, or no item is selected, nothing is sent back to the server. On a confirmed
     * selection, the selected combo entry's text before its first space is taken as the unit identifier (i.e. any
     * descriptive text after a unit ID is discarded), and a reply is sent via {@link IClient#sendChat(String)}
     * using the {@code IClient.CAMPAIGN_PREFIX + "c "} chat-command convention:
     * <ul>
     *     <li>if a receiving player is present and {@code commandName} is {@code "admin_transfer"}, the reply
     *     encodes {@code commandName#username#receivingPlayer<unit>};</li>
     *     <li>if a receiving player is present and {@code commandName} is {@code "view_player_unit"}, the reply
     *     encodes {@code commandName#username<unit>#receivingPlayer} (note the different token order versus the
     *     transfer case);</li>
     *     <li>if there is no receiving player at all, the reply encodes {@code commandName#username<unit>},
     *     regardless of what {@code commandName} is.</li>
     * </ul>
     * Note: if a receiving player is present but {@code commandName} is something other than the two names
     * checked above, no reply is sent at all — the dialog result is silently dropped.
     *
     * @param input the raw, delimited protocol line for this command; see {@link Command#decode(String)}
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer command = decode(input);
        String commandName = command.nextToken();
        String username = command.nextToken();
        String unitList = command.nextToken();
        String receivingPlayer = null;

        if (command.hasMoreTokens()) {
            receivingPlayer = command.nextToken();
        }

        StringTokenizer units = new StringTokenizer(unitList, "#");

        TreeSet<String> list = new TreeSet<>();

        while (units.hasMoreElements()) {
            list.add(String.format("#%s", units.nextToken()));
        }

        JComboBox<String> combo = new JComboBox<>();
        list.forEach(combo::addItem);

        combo.setEditable(false);
        JOptionPane jop = new JOptionPane(combo, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(client.getMainFrame(), "Select a unit.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);
        String unit = (String) combo.getSelectedItem();

        if (unit != null) {
            unit = unit.substring(0, unit.indexOf(" "));
            int value = (Integer) jop.getValue();

            if (value == JOptionPane.CANCEL_OPTION) {
                return;
            }

            if (receivingPlayer != null) {
                if (commandName.equalsIgnoreCase("admin_transfer")) {
                    client.sendChat(String.format("%sc %s#%s#%s%s", IClient.CAMPAIGN_PREFIX, commandName, username, receivingPlayer, unit));
                } else if (commandName.equalsIgnoreCase("view_player_unit")) {
                    client.sendChat(String.format("%sc %s#%s%s#%s", IClient.CAMPAIGN_PREFIX, commandName, username, unit, receivingPlayer));
                }
            } else {
                client.sendChat(String.format("%sc %s#%s%s", IClient.CAMPAIGN_PREFIX, commandName, username, unit));
            }
        }
    }

    /**
     * Unused on the client side; this command has no reply-argument parsing behavior.
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * Unused on the client side; this command is never parsed as server-bound arguments.
     */
    @Override
    public void parseArguments(String s) {

    }
}
