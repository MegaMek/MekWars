/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - Helge Richter
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

package mekwars.server.campaign.commands;

import java.util.StringTokenizer;

import mekwars.server.MWChatServer.auth.AccessRole;

public interface Command {

    public void process(StringTokenizer command, String Username);

    //for userlevel configurability
    public AccessRole getExecutionLevel();

    public void setExecutionLevel(AccessRole accessRole);

    public String getSyntax();
}
