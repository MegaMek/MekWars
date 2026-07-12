/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

import common.util.StringUtils;
import mekwars.server.campaign.CampaignMain;
import server.campaign.pilot.SPilot;


public class NamePilotCommand implements Command {

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

        int unitid = -1;
        String name = "";

        try {
            unitid = Integer.parseInt((String) command.nextElement());
            name = (String) command.nextElement();
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper syntx. Try: /c namepilot#unitid#newname",
                  Username,
                  true);
            return;
        }

        //check validity of name
        if (name.length() > 30) {name = name.substring(0, 30);}

        if (StringUtils.hasBadChars(name, true).trim().length() > 0) {
            CampaignMain.campaignMain.toUser(StringUtils.hasBadChars(name, true).trim(), Username);
            return;
        }

        //check player
        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);
        if (p == null) {
            CampaignMain.campaignMain.toUser("AM:Null player while naming pilot. Report to an admin.",
                  Username,
                  true);
            return;
        }

        //fetch unit
        server.campaign.SUnit u = p.getUnit(unitid);
        if (u == null) {
            CampaignMain.campaignMain.toUser("AM:Could not find a unit with ID#" + unitid + ".", Username, true);
            return;
        }

        //fetch pilot
        SPilot pilot = (SPilot) u.getPilot();
        if (pilot == null) {
            CampaignMain.campaignMain.toUser("AM:Unit #" + unitid + " has a null pilot. Report this to an admin.",
                  Username,
                  true);
            return;
        }

        //make sure pilot isn't Vacant (99/99)
        if (pilot.getName().toLowerCase().startsWith("vacant")) {
            CampaignMain.campaignMain.toUser("AM:There is no pilot in that unit! It is vacant!", Username, true);
            return;
        }

        //checks passed. change name,
        pilot.setName(name);
        CampaignMain.campaignMain.toUser("AM:The pilot of the " +
                                               u.getModelName() +
                                               " (#" +
                                               unitid +
                                               ") was renamed. New name: " +
                                               name +
                                               ".", Username, true);
        CampaignMain.campaignMain.toUser("PL|UU|" + u.getId() + "|" + u.toString(true), Username, false);

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}//end NamePilotCommand
