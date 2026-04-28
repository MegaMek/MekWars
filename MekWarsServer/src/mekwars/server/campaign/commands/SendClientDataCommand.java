/*
 * MekWars - Copyright (C) 2007
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
package mekwars.server.campaign.commands;

import common.util.MWLogger;

public class SendClientDataCommand implements Command {

    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        try {

            StringBuilder userData = new StringBuilder("USERDATA:");
            StringBuilder verifyData = new StringBuilder("File Info for " + Username + ": ");
            userData.append(Username);
            userData.append(";");

            int count = 0;

            while (command.hasMoreElements()) {
                if (count == 0) {
                    // what file we're running
                    verifyData.append(command.nextToken());
                } else if (count == 1) {
                    // MD5
                    verifyData.append("  MD5: " + command.nextToken());
                } else if (count == 2) {
                    // MMNet MD5
                    verifyData.append("  MegaMek MD5: " + command.nextToken());
                } else {
                    userData.append(command.nextToken() + ";");
                }
                count++;
            }
            MWLogger.ipLog(userData.toString());
            MWLogger.ipLog(verifyData.toString());
        } catch (Exception ex) {
            //do nothing
        }
    }//end process

    public int getExecutionLevel() {return 0;}

    public void setExecutionLevel(int i) {}

    public String getSyntax() {return syntax;}

}//end AttackFromReserveCommand
