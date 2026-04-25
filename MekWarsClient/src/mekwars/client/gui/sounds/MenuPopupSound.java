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
package mekwars.client.gui.sounds;

public class MenuPopupSound implements javax.swing.event.MenuListener {

    client.MWClient mwclient = null;

    public MenuPopupSound(client.MWClient mwclient) {
        this.mwclient = mwclient;
    }

    public void menuCanceled(javax.swing.event.MenuEvent arg0) {
    }

    public void menuDeselected(javax.swing.event.MenuEvent arg0) {
    }

    public void menuSelected(javax.swing.event.MenuEvent arg0) {

        if (mwclient.getConfig().isParam("ENABLEMENUPOPUPSOUND")) {
            mwclient.doPlaySound(mwclient.getConfigParam("SOUNDONMENUPOPUP"));
        }
    }

}
