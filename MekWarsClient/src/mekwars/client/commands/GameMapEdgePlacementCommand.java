/*
 * MekWars - Copyright (C) 2004
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

import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * @author Torren (Jason Tighe)
 *       <p>
 *       Used for Game Map Edge Placement Normally used when you have buildings being placed on the map and want the
 *       defender to start near them.
 *
 */

public class GameMapEdgePlacementCommand extends Command {

    /**
     * @see Command#Command(IClient)
     */
    public GameMapEdgePlacementCommand(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);
        int edge = Integer.parseInt(st.nextToken());
        client.setPlayerStartingEdge(edge);
    }
}
