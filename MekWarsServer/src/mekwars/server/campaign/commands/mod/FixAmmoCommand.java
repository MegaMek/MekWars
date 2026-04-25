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

package mekwars.server.campaign.commands.mod;

import common.util.StringUtils;
import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Mounted;

/**
 * @author Helge Richter
 */
public class FixAmmoCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name#Unit ID";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }

        String targetName;
        server.campaign.SPlayer target = null;
        int unitID;

        try {
            targetName = command.nextToken();
            unitID = Integer.parseInt(command.nextToken());
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("AM:Invalid Syntax: /FixAmmo Player#UnitID", Username);
            return;
        }

        target = server.campaign.CampaignMain.cm.getPlayer(targetName);

        if (target == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Target player could not be found. Try again.", Username, true);
            return;
        }

        //non null target, so attempt the scrap
        server.campaign.SUnit m = target.getUnit(unitID);

        //break out if the player doesn't have a unit with that id
        if (m == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Target player doesn't have a unit with ID# " + unitID + ".",
                  Username,
                  true);
            return;
        }

        fixAmmo(m);
        //tell the player the unit has been fixed
        server.campaign.CampaignMain.cm.toUser(targetName + "'s " + m.getModelName() + "'s ammo has been fixed.",
              Username,
              true);
        server.campaign.CampaignMain.cm.toUser(Username +
                                                     " has fixed the ammo on your " +
                                                     m.getModelName() +
                                                     " (ID#" +
                                                     m.getId() +
                                                     ")", targetName, true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username +
                    " fixed the ammo on " +
                    StringUtils.aOrAn(m.getModelName(), true) +
                    " belonging to " +
                    targetName);
        target.setSave();
        target.checkAndUpdateArmies(m);
        server.campaign.CampaignMain.cm.toUser("PL|UU|" + m.getId() + "|" + m.toString(true), targetName, false);
    }

    /**
     * If the units ammo is different from the baseline entity the ammo is fixed.
     *
     * @param unit
     */
    private void fixAmmo(server.campaign.SUnit unit) {
        Entity baseLine = null;
        Entity en = unit.getEntity();

        baseLine = server.campaign.SUnit.loadMech(unit.getUnitFilename());

        java.util.ArrayList<Mounted> baseLineAmmo = baseLine.getAmmo();
        java.util.ArrayList<Mounted> currentAmmo = en.getAmmo();

        if (baseLineAmmo.size() < 1) {unit.getEntity().getAmmo().clear();}

        for (int pos = 0; pos < baseLineAmmo.size(); pos++) {
            AmmoType baseAmmo = (AmmoType) baseLineAmmo.get(pos).getType();
            AmmoType ammo = (AmmoType) currentAmmo.get(pos).getType();

            if (!ammo.getInternalName().startsWith(baseAmmo.getInternalName())) {
                unit.getEntity().getAmmo().set(pos, baseLineAmmo.get(pos));
            }
        }
    }

}//end ModFullRepairCommand
