/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

/*
 * ClientCommand.java
 *
 * Created on June 3, 2002, 7:18 AM
 */

package mekwars.common.comm;

import mekwars.common.interfaces.IClient;

/**
 *
 * @author Administrator
 */
public interface ClientCommand extends ICommand {

    void parseReply(String s);

    void timeout();

    void send(boolean blocking);

    void setClient(IClient mwClient);
}
