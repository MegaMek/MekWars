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
 * Command.java
 *
 * Created on June 2, 2002, 12:23 AM
 */

package mekwars.common.comm;

import java.io.Serial;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import mekwars.common.interfaces.IClient;
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

    public Vector<String> ErrorMessages = new Vector<String>(1, 1);

    protected IClient mwClient = null;
    protected IServer myServer = null;
    protected String username;
    protected int error_code = -1;
    protected String myPrefix;

    public Command(String prefix) {
        this.myPrefix = prefix;
        this.ErrorMessages.add("This operation has timed out");
        this.ErrorMessages.add("The packet received was unknown");
        this.ErrorMessages.add("The packet received was malformed");
    }

    public String getPrefix() {
        return this.myPrefix;
    }

    public void timeout() {
        this.error_code = Command.TIMEOUT;
    }

    public void reset() {
        this.error_code = -1;
        this.username = "";
    }

    public boolean hasError() {
        return (this.error_code != -1);
    }

    public String getErrorMessage() {
        if (this.hasError() && this.error_code >= 0 && this.error_code < this.ErrorMessages.size()) {
            return this.toString() + " " + this.ErrorMessages.elementAt(this.error_code);
        }
        return "";
    }

    public void send(boolean blocking) {
        //Not implemented
        //this.myClient.sendCommand(this);
    }

    public void setClient(IClient client) {
        mwClient = client;
    }

    public void setServer(IServer server) {
        this.myServer = server;
    }

    public void setUsername(String name) {
        this.username = name;
    }

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

    public abstract void parseReplyArgs(String s);

    public void clientSend(String txt) {
        this.myServer.clientSend(txt, this.username);
    }

    public static class Table extends Hashtable<String, ICommand> {
        /**
         *
         */
        @Serial
        private static final long serialVersionUID = -4187092531938444178L;

        public void put(ICommand command) {
            super.put(command.getPrefix(), command);
        }
        //the get method could return an UNKNOWN command object...
    }
}
