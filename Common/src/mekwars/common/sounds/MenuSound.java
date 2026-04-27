/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package mekwars.common.sounds;

import mekwars.common.campaign.clientutils.protocol.IClient;

public class MenuSound implements javax.swing.event.MenuListener {

    IClient client;

    public MenuSound(IClient client) {
        this.client = client;
    }

    public void menuSelected(javax.swing.event.MenuEvent arg0) {
        if (client.getConfig().isParam("ENABLEMENUSOUND")) {
            client.doPlaySound(client.getConfigParam("SOUNDONMENU"));
        }
    }

    public void menuDeselected(javax.swing.event.MenuEvent arg0) {
    }

    public void menuCanceled(javax.swing.event.MenuEvent arg0) {
    }
}
