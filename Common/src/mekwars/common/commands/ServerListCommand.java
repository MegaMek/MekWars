/*
 * Copyright (C) 2006 nmorris (urgru@users.sourceforge.net)
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

import mekwars.common.MMGame;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Server list commands. All commands relating to servers in the battle tab are processed though ServerListCommand
 * subcommands.
 * <p>
 * This is a client-side handler for the {@code ServerListCommand} protocol message: the master/lobby server uses
 * it to keep the client's "battle tab" list of known MegaMek game servers ({@link MMGame} entries, keyed by
 * hostname) in sync — announcing new servers, removing closed ones, updating a server's status text, and tracking
 * which players are currently in which server. Every successfully handled sub-command triggers a refresh of the
 * battle table GUI.
 */
public class ServerListCommand extends Command {

    //VARIABLES
    // - none

    //CONSTRUCTOR
    public ServerListCommand(IClient client) {
        super(client);
    }

    //METHODS
    //@see client.cmd.Command#execute(java.lang.String)
    /**
     * Dispatches on a sub-command code to update the client's known-server map ({@link IClient#getServers()}):
     * <ul>
     *     <li>{@code NG} - a new server opened; the remaining token is fed straight into the {@link MMGame}
     *     string constructor and the resulting game is added, keyed by its host name.</li>
     *     <li>{@code CG} - a server closed; removes the entry for the given hostname.</li>
     *     <li>{@code SHS} - set host status; looks up the server by hostname and, if found, updates its status
     *     text from the next token. Silently does nothing if the hostname isn't known.</li>
     *     <li>{@code JG} - a player joined a game; looks up the server by hostname and adds the player name to
     *     its current-players list. Note: unlike {@code SHS}, this does NOT null-check the lookup result, so an
     *     unknown hostname here will throw a {@link NullPointerException}.</li>
     *     <li>{@code LG} - a player left a game; same lookup-then-mutate pattern (and same missing null-check)
     *     as {@code JG}, but removes the player name instead.</li>
     * </ul>
     * If there is no token after the sub-command code, the method returns without doing anything (and without
     * refreshing the GUI). Any unrecognized sub-command code falls through without action, but the GUI is still
     * refreshed afterward.
     *
     * @param input the raw, delimited protocol line for this command; see {@link Command#decode(String)}
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        String cmd = stringTokenizer.nextToken();//ServerListCommand

        if (!stringTokenizer.hasMoreTokens()) {
            return;
        } else if (cmd.equals("NG")) { // new server opened, ServerListCommand|NG|<MMGame.toString>
            MMGame newGame = new MMGame(stringTokenizer.nextToken());
            client.getServers().put(newGame.getHostName(), newGame);
        } else if (cmd.equals("CG")) { // server closed, ServerListCommand|CG|Hostname
            client.getServers().remove(stringTokenizer.nextToken());
        } else if (cmd.equals("SHS")) { // set host status, ServerListCommand|SHS|Hostname
            MMGame toUpdate = client.getServers().get(stringTokenizer.nextToken());
            if (toUpdate != null) {
                toUpdate.setStatus(stringTokenizer.nextToken());
            }
        } else if (cmd.equals("JG")) { //player joined a game, GL|JG|Hostname|PlayerName
            MMGame toUpdate = client.getServers().get(stringTokenizer.nextToken());
            toUpdate.getCurrentPlayers().add(stringTokenizer.nextToken());
        } else if (cmd.equals("LG")) { //player left a game, GL|LG|Hostname|PlayerName
            MMGame toUpdate = client.getServers().get(stringTokenizer.nextToken());
            toUpdate.getCurrentPlayers().remove(stringTokenizer.nextToken());
        }

        //refresh affected portion of the GUI
        client.refreshGUI(IClient.REFRESH_BATTLE_TABLE);
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
