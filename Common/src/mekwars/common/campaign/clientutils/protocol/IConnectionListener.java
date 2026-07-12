/*
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
 * Interface to implement when you want to get raw messages from the socket connection.
 * <p>
 * Registered with an {@link IConnectionHandler} via {@link IConnectionHandler#setListener(IConnectionListener)}; the
 * handler calls back into this interface as data arrives from (or the connection to) the remote peer changes state.
 * {@link CConnector} is the client-side implementation, translating these low-level notifications into calls on the
 * {@link IClient} it wraps.
 */
public interface IConnectionListener {
    /**
     * Notification that a new line was read from the socket.
     *
     * @param message the raw line of text received, not yet interpreted as a protocol command
     */
    void incomingMessage(String message);

    /**
     * Notification that the socket got closed (either by the remote peer, a network error, or a local shutdown
     * request).
     */
    void socketClosed();
}
