/*
 * Copyright (c) 2000 Lyrisoft Solutions, Inc.
 * Used by permission
 */
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
package common.campaign.clientutils.protocol;

/**
 * Interface that ConnectionHandlers must implement
 */
public interface IConnectionHandler {
    /**
     * Queue a message headed outbound.
     */
    public void queueMessage(String message);

    /**
     * Send a message immediately.
     */
    public void sendImmediately(String message);

    /**
     * Shutdown this connection listener.  The notify parameter
     * indicates whether or not the client (ConnectionListener) should
     * be notified of the shutdown.  Basically, notify should only be
     * false if the client itself called us.
     * @param notify to notify the ConnectionListener
     */
    public void shutdown(boolean notify);

    /**
     * Set the connection listener for this connection handler
     */
    public void setListener(IConnectionListener listener);
}
