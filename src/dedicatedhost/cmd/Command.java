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

package dedicatedhost.cmd;

import java.util.StringTokenizer;

import dedicatedhost.MWDedHost;

/**
 * Base class that all commands have to derrive from.
 *
 * HOWTO ADD A NEW COMMAND
 * -----------------------
 * - Subclass Command. Put your class in package dedicatedhost.cmd
 * - Name your class like the command in the chat input (e.g. "HS" for faction status)
 * - Provide an constructor with one MMClient argument.
 * - implement execute to do the things you want to be done.
 * - Remember that your command get only constructed once and execute will
 *   be called on the same object over and over again...
 *
 * No registration is needed. Your class will be found ;-)
 *  
 * @author Imi (immanuel.scholz@gmx.de)
 */
abstract public class Command {
	
	/**
	 * The Client's world
	 */
	protected final MWDedHost mwclient;

	/**
	 * Construct this Command. Remember, that your deriverate must have an
	 * Constructor taking exact one client as parameter too.
	 */
	public Command(MWDedHost client) {
		this.mwclient = client;
	}
	
	/**
	 * Executes the command on the given input data.
	 */
	public abstract void execute(String input);
	
	/**
	 * Helper to decode the inputstring for execute
	 * @param input The input string given to execute
	 * @return An StringTokenizer iterating over the tokens of input except the first one.
	 * @see Command#execute(String)
	 */
	protected StringTokenizer decode(String input) {
        //checkWaiting();
		StringTokenizer st = new StringTokenizer(input, MWDedHost.COMMAND_DELIMITER);
		st.nextToken();
		return st;
	}
}
