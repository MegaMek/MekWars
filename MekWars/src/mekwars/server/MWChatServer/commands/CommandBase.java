/*
 * MekWars - Copyright (C) 2005 
 * 
 * Original author - Torren (torren@users.sourceforge.net)
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
 * Derived from NFCChat, a GPL chat client/server. 
 * Original code can be found @ http://nfcchat.sourceforge.net
 * Our thanks to the original authors.
 */ 
/**
 * 
 * @author Torren (Jason Tighe) 11.5.05 
 * 
 */
package server.MWChatServer.commands;

public abstract class CommandBase implements ICommandProcessorRemote {
    protected String _help;
    protected int _access;
    protected String _usage;

    public void setHelp(String help) {
        _help = help;
    }

    public String getHelp() {
        return _help;
    }

    public void setAccessRequired(int access) {
        _access = access;
    }

    public int accessRequired() {
        return _access;
    }

    public void setUsage(String usage) {
        _usage = usage;
    }

    public String getUsage(String myName) {
        return _usage;
    }
}
