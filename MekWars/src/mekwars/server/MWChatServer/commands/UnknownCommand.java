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

import server.MWChatServer.MWChatClient;
import server.MWChatServer.MWChatServer;
import server.MWChatServer.Translator;


public class UnknownCommand extends CommandBase {
    public boolean process(MWChatClient client, String[] args) {
        client.generalError(Translator.getMessage("unknown_command"));
        return false;
    }

    public void processDistributed(String client, String origin, String[] args, MWChatServer server) { 
    }
}
