/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Original author Helge Richter (McWizard)
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

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class UG extends Command {

    /**
     * @param client
     */
    public UG(client.MWClient mwclient) {
        super(mwclient);
    }

    /**
     * @see client.cmd.Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);
        //UG = User Gone (UG|<MMClientInfo.toString>|[Gone]) Gone is used when the client didn't just change his name
        //Create a new MMClienrInfo-Object from the String
        client.CUser mmci = new client.CUser((String) st.nextElement());

        //Check the Users and remove the User
        client.CUser user = mwclient.getUser(mmci.getName());
        //delete every instance of that user from the list
        while (mwclient.getUsers().remove(user)) {
            user = mwclient.getUser(mmci.getName());
        }

        mwclient.refreshGUI(client.MWClient.REFRESH_USERLIST);

        if (mwclient.isDedicated()) {
            return;
        }

        if ((mmci.isInvis() && mmci.getUserlevel() > mwclient.getUserLevel())
                  || mmci.getName().startsWith("[Dedicated]")) {
            return;
        }

        //Since there are more Elements, it'll be a Gone, so the user has left the room.
        if (st.hasMoreTokens()) {
            //Print the User-gone Info using the Info-Color (Maroon)
            String toSend = "<font color=\"maroon\">>> Exit " + mmci.getName() + "</font>";

            if (mwclient.getConfig().isParam("TIMESTAMP")) {toSend = mwclient.getShortTime() + toSend;}

            if (mwclient.getConfig().isParam("SHOWENTERANDEXIT") && !mmci.getName().equalsIgnoreCase("Nobody")) {
                mwclient.addToChat(toSend);
            }

            //Play the sound
            mwclient.doPlaySound(mwclient.getConfigParam("SOUNDONEXIT"));
        }
    }
}
