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

            File logFile = new File("./logs/" + logName + ".log");

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
