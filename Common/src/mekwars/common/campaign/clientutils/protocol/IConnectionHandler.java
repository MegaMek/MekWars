/*
 * Copyright (C) 2000 Lyrisoft Solutions, Inc. - Used by permission.
 * Copyright (C) 2005 - Torren (torren@users.sourceforge.net)
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

/*
 * Derived from NFCChat, a GPL chat client/server.
 * Original code can be found @ http://nfcchat.sourceforge.net
 * Our thanks to the original authors.
 */
package mekwars.common.campaign.clientutils.protocol;

/**
 * Interface that ConnectionHandlers must implement.
 * <p>
 * A connection handler owns the low-level socket I/O for one client-to-server connection: it accepts outgoing
 * messages (queued or immediate) and reports incoming/closed-connection notifications to a registered
 * {@link IConnectionListener}. {@link CConnector} depends on this interface (rather than directly on a socket) so
 * the transport can be swapped/mocked; {@link ConnectionHandlerLocal} is the concrete implementation used for real
 * TCP socket connections.
 */
public interface IConnectionHandler {
    /**
     * Queue a message headed outbound. The message is handed to a writer thread/queue rather than written to the
     * socket synchronously, so this call returns without waiting for the data to actually be sent.
     */
    void queueMessage(String message);

    /**
     * Send a message immediately, bypassing any outbound queue, and flush the underlying stream so the bytes are
     * written to the socket right away.
     */
    void sendImmediately(String message);

    /**
     * Shutdown this connection listener.  The notification parameter indicates whether the client (ConnectionListener)
     * should be notified of the shutdown.  Basically, notify should only be false if the client itself called us.
     *
     * @param notify to notify the ConnectionListener
     */
    void shutdown(boolean notify);

    /**
     * Set the connection listener for this connection handler
     */
    void setListener(IConnectionListener listener);
}
