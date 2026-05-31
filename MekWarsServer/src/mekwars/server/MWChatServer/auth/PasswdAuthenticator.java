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

import megamek.logging.MMLogger;
import mekwars.server.MWChatServer.MWChatClient;
import mekwars.server.MWChatServer.MWChatServer;
import mekwars.server.util.MWPasswd;
import mekwars.server.util.MWPasswdRecord;

/**
 * Authenitcator that reads from a password file.<p>
 * <p>
 * The file is made up of colon-delimited fields:
 * <userId>:<access_level>:<password>
 * <p>
 * If a user if found in the password file, his password is checked. If a user is not found in the password file, the
 * access level IAuthenticator.USER is returned.
 */
public class PasswdAuthenticator extends NullAuthenticator {
    private final static MMLogger LOGGER = MMLogger.create(PasswdAuthenticator.class);

    public PasswdAuthenticator(MWChatServer server, boolean allowGuests, boolean storeGuests) {
        super(server, allowGuests, storeGuests);
    }

    @Override
    public Auth authenticate(MWChatClient client, String password) throws Exception {
        String userId = client.getUserId();

        try {
            MWPasswdRecord record = MWPasswd.getRecord(userId, password);

            if (record == null) {
                LOGGER.debug("record is null for: {}", userId);
                if (_allowGuests) {
                    Auth auth = super.authenticate(client, password);

                    if (_storeGuests) {
                        MWPasswd.writeRecord(auth.getUserId(), AccessRole.GUEST, password);
                    }

                    return auth;
                }
                //else
                throw new Exception(userId);
            }

            //else
            return new Auth(userId, record.access);

        } catch (java.io.IOException e) {
            MWLogger.errLog(e);
            throw new Exception(userId);
        }
    }

    /** checks all stored users besides just those currently logged on */
    @Override
    public String getUserId(String target) {
        return MWPasswd.getUserId(target);
    }

}
