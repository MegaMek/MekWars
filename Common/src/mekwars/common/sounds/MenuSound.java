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

/**
 * Swing {@link javax.swing.event.MenuListener} that plays a UI sound effect whenever the menu it's attached to is
 * opened, if the user has sound enabled in their client configuration.
 * <p>
 * Attach an instance of this to a {@code JMenu} to get a "menu opened" sound; see {@link MenuPopupSound} for the
 * equivalent behavior on popup menus.
 */
public class MenuSound implements javax.swing.event.MenuListener {

    IClient client;

    /**
     * @param client the client whose configuration (sound enabled flag, sound file choice) and audio playback are
     *               used
     */
    public MenuSound(IClient client) {
        this.client = client;
    }

    /** Plays the configured "menu opened" sound if {@code ENABLEMENUSOUND} is set in the client config. */
    public void menuSelected(javax.swing.event.MenuEvent arg0) {
        if (client.getConfig().isParam("ENABLEMENUSOUND")) {
            client.doPlaySound(client.getConfigParam("SOUNDONMENU"));
        }
    }

    /** No sound is played on menu close. */
    public void menuDeselected(javax.swing.event.MenuEvent arg0) {
    }

    /** No sound is played when menu selection is cancelled. */
    public void menuCanceled(javax.swing.event.MenuEvent arg0) {
    }
}
