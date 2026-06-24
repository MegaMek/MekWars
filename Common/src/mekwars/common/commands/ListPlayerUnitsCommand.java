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
            list.add(STR."#\{units.nextToken()}");
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
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c \{commandName}#\{username}#\{receivingPlayer}\{unit}");
                } else if (commandName.equalsIgnoreCase("view_player_unit")) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c \{commandName}#\{username}\{unit}#\{receivingPlayer}");
                }
            } else {
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c \{commandName}#\{username}\{unit}");
            }
        }
    }

    /**
     *
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     *
     */
    @Override
    public void parseArguments(String s) {

    }
}
