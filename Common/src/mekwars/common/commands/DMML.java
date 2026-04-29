/*
 * Copyright (C) 2006 - jtighe (torren@users.sourceforge.net)
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


package mekwars.common.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.MWLogger;

/**
 * @author Torren Ded MegaMekLog
 */
public class DMML extends Command {

    /**
     * @see Command#Command(IClient)
     */
    public DMML(IClient mwclient) {
        super(mwclient);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer st = decode(input);
        if (st.hasMoreElements()) {
            String logName = st.nextToken();

            File logFile = new File(STR."./logs/\{logName}.log");

            try {
                logFile.createNewFile();
                FileOutputStream fileOutputStream = new FileOutputStream(logFile, true);
                PrintStream printStream = new PrintStream(fileOutputStream);

                while (st.hasMoreElements()) {
                    printStream.append(st.nextToken());
                    printStream.append(" ");
                }
                printStream.append('\n');
                printStream.flush();
                printStream.close();
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
        }
    }

    /**
     * @param s
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * @param s
     */
    @Override
    public void parseArguments(String s) {

    }
}
