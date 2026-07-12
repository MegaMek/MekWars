/*
 * Copyright (c) 2000 Lyrisoft Solutions, Inc. - Used by permission
 * Copyright (C) 2005 Torren (torren@users.sourceforge.net)
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

package mekwars.server.MWChatServer.auth;

import mekwars.server.MWChatServer.MWChatClient;
import mekwars.server.MWChatServer.MWChatServer;

/**
 * Authenitcator that authenticates everybody also serves as superclass for "real" authenticators
 */
public class NullAuthenticator implements IAuthenticator {
    protected MWChatServer _server;
    /** Boolean indicating whether users not in the database are allowed access. */
    protected boolean _allowGuests;
    /** Boolean indicating whether guests should be stored in the database. */
    protected boolean _storeGuests;

    public NullAuthenticator(MWChatServer server, boolean allowGuests, boolean storeGuests) {
        _server = server;
        _allowGuests = allowGuests;
        _storeGuests = storeGuests;
    }

    /** handles username conflicts by appending integers until it finds an unused one */
    public Auth authenticate(MWChatClient client, String password) throws Exception {
        // allow/store Guests ignored
        String newUserId = client.getUserId();
        return new Auth(newUserId, AccessRole.GUEST);
    }

    public String getUserId(String target) {
        MWChatClient c = _server.getClient(target);
        return c == null ? target : c.getUserId();
    }

}
