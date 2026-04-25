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

package mekwars.client.cmd;

import common.campaign.Buildings;

/**
 * @@author Torren (Jason Tighe)
 *       <p>
 *       Used for Randomn Building Placement on RMG's
 *
 */

public class RBP extends Command {

    /**
     * @see Command#Command(MMClient)
     */
    public RBP(client.MWClient mwclient) {
        super(mwclient);
    }

    /**
     * @see client.cmd.Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);
        Buildings building = new Buildings();

        building.fromString(st);

        mwclient.setBuildingTemplate(building);
    }
}
