/*
 * MekWars - Copyright (C) 2006
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

package server.campaign.commands.admin;

import java.util.Hashtable;
import java.util.StringTokenizer;

import common.util.UnitUtils;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.commands.Command;

/**
 * @author Jason Tighe
 */
public class AdminGetUnitComponentsCommand implements Command {

    int accessLevel = IAuthenticator.ADMIN;
    String syntax = "TargetPlayer#Option[BreakDownUnit,DisplayParts,AddParts]#Unit[ID,FileName]";

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }

    public void process(StringTokenizer command, String Username) {

        // access level check
        int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.cm.toUser(
                    "AM:Insufficient access level for command. Level: "
                            + userLevel + ". Required: " + accessLevel + ".",
                    Username, true);
            return;
        }

        int year = CampaignMain.cm.getIntegerConfig("CampaignYear");

        try {
            String targetName = command.nextToken();
            String option = command.nextToken();
            StringBuffer result = new StringBuffer();

            SPlayer target = CampaignMain.cm.getPlayer(targetName);

            if (target == null) {
                CampaignMain.cm.toUser(
                        "Target player could not be found. Try again.",
                        Username, true);
                return;
            }

            if (option.equalsIgnoreCase("DisplayParts")) {
                result.append("You have the current parts stockpiled<br>");
                result.append(target.getUnitParts().tableizeComponents(year));

                CampaignMain.cm.toUser(result.toString(), Username);
            } else if (option.equalsIgnoreCase("addParts")) {
                // non null target, so attempt the scrap
                int unitID = Integer.parseInt(command.nextToken());
                SUnit m = target.getUnit(unitID);

                target.getUnitParts().add(getUnitComponents(m.getEntity()));

                CampaignMain.cm.toUser(
                        "All useable parts from " + m.getModelName()
                                + " where added to " + target.getName()
                                + "'s parts stockpile", Username);
                CampaignMain.cm.toUser(
                        "All useable parts from " + m.getModelName()
                                + " where added to your parts stockpile",
                        target.getName());
            } else {
                // non null target, so attempt the scrap
                int unitID = Integer.parseInt(command.nextToken());
                SUnit m = target.getUnit(unitID);
                Hashtable<String, Integer> components = new Hashtable<String, Integer>();

                // break out if the player doesn't have a unit with that id
                if (m == null) {
                    CampaignMain.cm.toUser(
                            "Target player doesn't have a unit with ID# "
                                    + unitID + ".", Username, true);
                    return;
                }
                Entity ent = m.getEntity();

                components.putAll(getUnitComponents(ent));

                result.append("Component list for #" + unitID + " "
                        + m.getModelName() + "<br>");
                result.append("<table><tr><th>Component</th><th># of Crits</th></tr>");

                for (String key : components.keySet()) {
                    result.append("<tr><td>");
                    result.append(key);
                    result.append("</td><td>");
                    result.append(components.get(key));
                    result.append("</td></tr>");
                }
                result.append("</table>");

                CampaignMain.cm.toUser(result.toString(), Username);

            }
        } catch (Exception ex) {
            CampaignMain.cm
                    .toUser("Invalid Syntax: /admingetunitcomponents TargetPlayer#Option[BreakDownUnit,DisplayParts,AddParts]#Unit[ID,FileName]",
                            Username);
        }
    }

    public Hashtable<String, Integer> getUnitComponents(Entity ent) {
        Hashtable<String, Integer> components = new Hashtable<String, Integer>();

        int IS = 0;
        int armor = 0;
        int rear = 0;
        String part = "";

        for (int location = 0; location < ent.locations(); location++) {
            IS += Math.max(0, ent.getInternal(location));
            armor += Math.max(0, ent.getArmor(location));
            rear += Math.max(0, ent.getArmor(location, true));
            for (int slot = 0; slot < ent.getNumberOfCriticals(location); slot++) {
                CriticalSlot crit = ent.getCritical(location, slot);
                if (crit == null) {
                    continue;
                }

                if (UnitUtils.isActuator(crit)) {
                    part = "Actuator";
                } else if (crit.getType() == CriticalSlot.TYPE_SYSTEM) {
                    part = ((Mech) ent).getSystemName(crit.getIndex());
                } else {
                    part = crit.getMount().getType().getInternalName();
                }
                if (components.containsKey(part)) {
                    components.put(part, components.get(part) + 1);
                } else {
                    components.put(part, 1);
                }
            }
            components.put(
                    "Armor: "
                            + EquipmentType.getArmorTypeName(ent
                                    .getArmorType(location)), armor + rear);
        }

        components.put(
                "IS: "
                        + EquipmentType.getStructureTypeName(ent
                                .getStructureType()), IS);

        return components;

    }
}// end AdminGetUnitComponentsCommand