/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Original author Imi (immanuel.scholz@gmx.de)
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

import java.util.StringTokenizer;

import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Base class that all commands have to derive from.
 * <p>
 * HOWTO ADD A NEW COMMAND ----------------------- - Subclass Command. Put your class in package client.cmd - Name your
 * class like the command in the chat input (e.g. "FactionStatusScreenUpdateCommand" for faction status) - Provide an
 * constructor with one MMClient argument. - implement execute to do the things you want to be done. - Remember that
 * your command get only constructed once and execute will be called on the same object over and over again...
 * <p>
 * No registration is needed. Your class will be found ;-)
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
abstract public class Command {

    /**
     * The Client's world
     */
    protected final IClient client;

    /**
     * Construct this Command. Remember that your derivative must have an Constructor taking exact one client as
     * parameter too.
     */
    public Command(IClient client) {
        this.client = client;
    }

    /**
     * Executes the command on the given input data.
     */
    public abstract void execute(String input);

    /**
     * Helper to decode the input string for execute
     *
     * @param input The input string given to execute
     *
     * @return An StringTokenizer iterating over the tokens of input except the first one.
     *
     * @see Command#execute(String)
     */
    protected StringTokenizer decode(String input) {
        StringTokenizer st = new StringTokenizer(input, IClient.COMMAND_DELIMITER);
        st.nextToken();
        return st;
    }
}
