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

import mekwars.common.MMGame;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Server list commands. All commands relating to servers in the battles tab are processed though ServerListCommand
 * subcommands.
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
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);
        String cmd = st.nextToken();//ServerListCommand

        if (!st.hasMoreTokens()) {
            return;
        } else if (cmd.equals("NG")) { // new server opened, ServerListCommand|NG|<MMGame.toString>
            MMGame newGame = new MMGame(st.nextToken());
            client.getServers().put(newGame.getHostName(), newGame);
        } else if (cmd.equals("CG")) { // server closed, ServerListCommand|CG|Hostname
            client.getServers().remove(st.nextToken());
        } else if (cmd.equals("SHS")) { // set host status, ServerListCommand|SHS|Hostname
            MMGame toUpdate = client.getServers().get(st.nextToken());
            if (toUpdate != null) {
                toUpdate.setStatus(st.nextToken());
            }
        } else if (cmd.equals("JG")) { //player joined a game, GL|JG|Hostname|Playername
            MMGame toUpdate = client.getServers().get(st.nextToken());
            toUpdate.getCurrentPlayers().add(st.nextToken());
        } else if (cmd.equals("LG")) { //player left a game, GL|LG|Hostname|Playername
            MMGame toUpdate = client.getServers().get(st.nextToken());
            toUpdate.getCurrentPlayers().remove(st.nextToken());
        }

        //refresh affected portion of the GUI
        client.refreshGUI(IClient.REFRESH_BATTLE_TABLE);
    }

    /**
     * @param s
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * @param s
     */
    @Override
    public void parseArguments(String s) {

    }
}
