/*
 * Copyright (c) 2000 Lyrisoft Solutions, Inc. - Used by permission
 * Copyright (C) 2002-2003 Helge Richter, MMNET
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

package mekwars.server.util;

/*
 * Modified 2/26/2003 by Jonathan Ellis
 * - rereading file into memory is nice b/c it allows users to be manually added externally while the server is running,
 * but it prohibitively slows *  past a few thousand users. changed to only read once.  Now that PasswdAuthenticator
 * will auto-add users, the old feature really isn't necessary.
 * - removed some unnecessary synchronization.  Remember the Hashtable synchronizes automatically.
 */

import java.io.IOException;

import megamek.logging.MMLogger;
import mekwars.server.MWChatServer.auth.AccessRole;
import mekwars.server.MWChatServer.commands.ICommands;
import mekwars.server.MWChatServer.translator.jcrypt;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SPlayer;

/**
 * Represents a unix-style passwd file with three colon-delimited fields:
 * <li>userId (String)
 * <li>access level (int)
 * <li>crypted password (String)
 */

public class MWPasswd implements ICommands {
    private final static MMLogger LOGGER = MMLogger.create(MWPasswd.class);

    static {
        reloadFile();
    }

    public static String getUserId(String target) {
        SPlayer player = CampaignMain.campaignMain.getPlayer(target);

        if (player == null) {
            return null;
        }

        return player.getName();
    }

    /**
     * Gets a PasswdRecord.
     *
     * @param userId   the user Id
     * @param password the password in plaintext
     *
     * @return the PasswdRecord or null if the user was not found
     *
     */
    public static MWPasswdRecord getRecord(String userId, String password) throws Exception {
        MWPasswdRecord mwPasswdRecord;
        mwPasswdRecord = getRecord(userId.toLowerCase());

        if (mwPasswdRecord == null) {
            return null;
        }

        if (password == null) {
            password = "";
        }

        if (password.length() < 2) {
            LOGGER.info("Access denied: {}", userId);
            throw new Exception(userId);
        }

        try {
            String salt = mwPasswdRecord.passwd.substring(0, 2);
            if (jcrypt.crypt(salt, password).equals(mwPasswdRecord.passwd)) {
                return mwPasswdRecord;
            }
        } catch (Exception ex) {
            return null;
        }

        //else
        LOGGER.debug("Access denied: {}", userId);
        throw new Exception(ACCESS_DENIED);
    }

    public static MWPasswdRecord getRecord(String userId) {
        SPlayer player = CampaignMain.campaignMain.getPlayer(userId);

        if (player == null) {
            return null;
        }

        if (player.getPassword() == null) {
            return null;
        }
        //else
        return player.getPassword();
    }

    /**
     * Write a PasswdRecord to the passwd file. If an line already existed for the user specified, it gets overwritten,
     * otherwise, it is appended.
     *
     * @param mwPasswdRecord the record to write
     */
    public static void writeRecord(MWPasswdRecord mwPasswdRecord, String userId) {
        SPlayer player = CampaignMain.campaignMain.getPlayer(userId);

        if (player == null) {
            return;
        }

        player.setPassword(mwPasswdRecord);
    }

    public static void removeRecord(String userid) {
        SPlayer player = CampaignMain.campaignMain.getPlayer(userid);

        if (player == null) {
            return;
        }

        if (player.getPassword() != null) {
            player.setPassword(null);
        }

    }

    /**
     * Save the in-memory Hashtable of PasswdRecords out to disk.
     */
    public synchronized static void save() throws IOException {
    }

    public static void reloadFile() {
    }

    static void main(String[] args) {
        try {
            writeRecord(args[0], Integer.parseInt(args[1]), args[2]);
        } catch (java.io.IOException e) {
            LOGGER.error(e, "An I/O error occurred: {}", e.getMessage());
        } catch (Exception e) {
            showUsageAndExit();
        }
    }

    /**
     * Write a new entry to the passwd file.  If an entry already exists for the given userId, it gets overwritten,
     * otherwise, it is appended.  The password specified here gets encrypted.
     *
     * @param userId the user Id
     * @param access the access level
     * @param passwd the plaintext password that will get encrypted
     */
    public static void writeRecord(String userId, AccessRole access, String passwd) throws IOException {
        SPlayer player = CampaignMain.campaignMain.getPlayer(userId);

        if (player == null) {
            LOGGER.info("writeRecord::Player is null");
            return;
        }

        MWPasswdRecord mwPasswdRecord = new MWPasswdRecord(userId, access, passwd, System.currentTimeMillis(), "");
        String salt = String.valueOf(System.currentTimeMillis());
        int len = salt.length();
        salt = salt.substring(len - 2, len);
        mwPasswdRecord.passwd = jcrypt.crypt(salt, passwd);
        player.setPassword(mwPasswdRecord);
    }

    private static void showUsageAndExit() {
        LOGGER.debug("Passwd Program.  Adds new line to the passwd file, encrypting the password.");
        LOGGER.debug("usage: java com.lyrisoft.chat.server.remote.auth.Passwd " +
                           "[user id] [access level] [password] [time of last use]");
        System.exit(1);
    }
}
