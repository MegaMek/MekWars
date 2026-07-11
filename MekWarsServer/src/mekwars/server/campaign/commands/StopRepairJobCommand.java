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
 * Created on 10.05.2005
 *
 */
package mekwars.server.campaign.commands;
import megamek.logging.MMLogger;
import mekwars.server.campaign.CampaignMain;

/**
 * @author Torren (Jason Tighe) this parses out what the User wants reparied on thier unit and sends that data to the
 *       repair thread
 */
public class StopRepairJobCommand implements Command {
    private static final MMLogger LOGGER = MMLogger.create(StopRepairJobCommand.class);

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        try {

            int unitID = Integer.parseInt(command.nextToken());
            int location = Integer.parseInt(command.nextToken());
            int slot = Integer.parseInt(command.nextToken());
            boolean armor = Boolean.parseBoolean(command.nextToken());

            server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(Username);
            server.campaign.SUnit unit = player.getUnit(unitID);

            if (!CampaignMain.campaignMain.getRTT().isBeingRepaired(unitID, location, slot, armor)) {
                CampaignMain.campaignMain.toUser("FSM|There is no repair order for this section at the present.",
                      Username,
                      false);
                return;
            }

            if (CampaignMain.campaignMain.getRTT().getState() == java.lang.Thread.State.TERMINATED) {
                CampaignMain.campaignMain.toUser(
                      "FSM|Sorry your repair order could not be processed - the repair thread terminated. Staff was notified.",
                      Username,
                      false);
                LOGGER.error("NOTE: Repair Thread terminated! Use the restartrepairthread command to restart the thread. If all else fails reboot!");
                return;
            }

            CampaignMain.campaignMain.getRTT().stopRepair(unitID, location, slot, armor);

            CampaignMain.campaignMain.toUser("PL|UU|" + unitID + "|" + unit.toString(true), Username, false);

        } catch (Exception ex) {
            LOGGER.error("AM:Unable to Process Repair Unit Command!");
            LOGGER.error(ex, "");
        }

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}


}//end RepairUnitCommand
