/*
 * MekWars - Copyright (C) 2004
 *
 * Original Author - Nathan Morris (urgru@users.sourceforge.net)
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
package mekwars.client.commands;

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
     * @param client
     */
    public ListPlayerUnitsCommand(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer command = decode(input);
        String commandName = command.nextToken();
        String username = command.nextToken();
        String unitList = command.nextToken();
        String receivingPlayer = null;

        if (command.hasMoreTokens()) {receivingPlayer = command.nextToken();}

        java.util.StringTokenizer units = new java.util.StringTokenizer(unitList, "#");

        java.util.TreeSet<String> list = new java.util.TreeSet<>();

        while (units.hasMoreElements()) {
            list.add("#" + units.nextToken());
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

            if (value == javax.swing.JOptionPane.CANCEL_OPTION) {
                return;
            }

            if (receivingPlayer != null) {
                if (commandName.equalsIgnoreCase("admintransfer")) {
                    client.sendChat(IClient.CAMPAIGN_PREFIX +
                                          "c " +
                                          commandName +
                                          "#" +
                                          username +
                                          "#" +
                                          receivingPlayer +
                                          unit);
                } else if (commandName.equalsIgnoreCase("viewplayerunit")) {
                    client.sendChat(IClient.CAMPAIGN_PREFIX +
                                          "c " +
                                          commandName +
                                          "#" +
                                          username +
                                          unit +
                                          "#" +
                                          receivingPlayer);
                }
            } else {
                client.sendChat(IClient.CAMPAIGN_PREFIX + "c " + commandName + "#" + username + unit);
            }
        }
    }
}
