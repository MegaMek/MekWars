/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - jtighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */


package mekwars.client.cmd;

import common.util.MWLogger;

/**
 * @author Torren Ded MegaMekLog
 */
public class DMML extends Command {

    /**
     * @see Command#Command(MMClient)
     */
    public DMML(client.MWClient mwclient) {
        super(mwclient);
    }

    /**
     * @see client.cmd.Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);
        if (st.hasMoreElements()) {
            String logName = st.nextToken();

            java.io.File logFile = new java.io.File("./logs/" + logName + ".log");

            try {
                logFile.createNewFile();
                java.io.FileOutputStream fos = new java.io.FileOutputStream(logFile, true);
                java.io.PrintStream p = new java.io.PrintStream(fos);

                while (st.hasMoreElements()) {
                    p.append(st.nextToken());
                    p.append(" ");
                }
                p.append('\n');
                p.flush();
                p.close();
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
        }
    }
}
