/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - Jason Tighe (torren@users.sourceforge.net)
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

package mekwars.server.campaign.commands;

import common.util.MWLogger;


/**
 * Moving the SignOff command from MWServ into the normal command structure.
 * <p>
 * Syntax  /c SignOff
 */
public class SignOffCommand implements Command {

    public int getExecutionLevel() {return 0;}

    public void setExecutionLevel(int i) {}

    String syntax = "";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        MWLogger.errLog(Username + " has sent signoff command");
        server.campaign.CampaignMain.cm.getServer().clientLogout(Username);
    }
}
