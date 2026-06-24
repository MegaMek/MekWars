/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Copyright (C) 2004 Helge Richter (McWizard)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */


package mekwars.common.commands;

import java.io.Serial;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.interfaces.IServer;

/**
 *
 * @author Steve Hawkins
 */

public abstract class Command extends Thread implements ClientCommand, ServerCommand {

    public static final String DELIM = "|";

    public static final int TIMEOUT = 0;
    public static final int UNKNOWN = 1;
    public static final int MALFORMED = 2;
    public static final int LAST_ERROR = 2;

    public Vector<String> ErrorMessages = new Vector<>(1, 1);

    protected IClient client = null;
    protected IServer myServer = null;
    protected String username;
    protected int error_code = -1;
    protected String myPrefix;

    /**
     * Construct this Command. Remember that your derivative must have a Constructor taking exactly one client as a
     * parameter too.
     */
    public Command(IClient client) {
        this.client = client;
    }

    public Command(String prefix) {
        this.myPrefix = prefix;
        this.ErrorMessages.add("This operation has timed out");
        this.ErrorMessages.add("The packet received was unknown");
        this.ErrorMessages.add("The packet received was malformed");
    }

    public void setServer(IServer server) {
        this.myServer = server;
    }

    public void setUsername(String name) {
        this.username = name;
    }

    public void clientSend(String txt) {
        this.myServer.clientSend(txt, this.username);
    }

    /**
     * Helper to decode the input string for executing
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

    /**
     * Executes the command on the given input data.
     */
    public abstract void execute(String input);

    //Detect Errors And Pop the Prefix
    public void parseReply(String s) {
        StringTokenizer st = new StringTokenizer(s, "|");
        if (!st.hasMoreTokens()) {
            this.error_code = Command.MALFORMED;
            return;
        }
        if (!st.nextToken().equalsIgnoreCase(this.getPrefix())) {
            this.error_code = Command.MALFORMED;
            return;
        }
        //is error
        if (st.hasMoreTokens() && st.nextToken().equalsIgnoreCase("E")) {
            if (st.hasMoreTokens()) {
                this.error_code = Integer.parseInt(st.nextToken());
            }
        }
        this.parseReplyArgs(s.substring(s.indexOf(Command.DELIM) + 1));
    }

    public String getPrefix() {
        return this.myPrefix;
    }

    public boolean hasError() {
        return (this.error_code != -1);
    }

    public String getErrorMessage() {
        if (this.hasError() && this.error_code >= 0 && this.error_code < this.ErrorMessages.size()) {
            return STR."\{this} \{this.ErrorMessages.elementAt(this.error_code)}";
        }
        return "";
    }

    public void reset() {
        this.error_code = -1;
        this.username = "";
    }

    public abstract void parseReplyArgs(String s);

    public void timeout() {
        this.error_code = Command.TIMEOUT;
    }

    public void send(boolean blocking) {

    }

    public void setClient(IClient client) {
        this.client = client;
    }

    public static class Table extends Hashtable<String, ICommand> {
        @Serial
        private static final long serialVersionUID = -4187092531938444178L;

        public void put(ICommand command) {
            super.put(command.getPrefix(), command);
        }
    }
}
