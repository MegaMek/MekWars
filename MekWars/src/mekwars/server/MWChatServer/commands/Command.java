/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet),
 * which based its code on classes from NFCChat, a GPL chat client/server.
 * Original NFC code can be found @ http://nfcchat.sourceforge.net
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
package server.MWChatServer.commands;

import common.util.MWLogger;
import server.ServerWrapper;
import server.MWChatServer.MWChatClient;
import server.MWChatServer.MWChatServer;

/**
 *
 * @author  Administrator
 */
public class Command extends CommandBase implements ICommands {
  
  /** @return true if this message should be distributed to other clients
   */
  public boolean process(MWChatClient client, String[] args) {
    try {
		((ServerWrapper)client.getServer()).processCommand(client.getUserId(), common.comm.TransportCodec.unescape(args[1]));
    } catch (Exception e) {
      MWLogger.errLog(e);
      MWLogger.errLog("Not supposed to happen");
    }
    return false;
  }
  
  public void processDistributed(String client, String origin, String[] args, MWChatServer server) {
  }  
}
