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

package mekwars.server.campaign.commands.admin;

import java.util.Hashtable;
import java.util.StringTokenizer;

import megamek.codeUtilities.MathUtility;
import megamek.common.CriticalSlot;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekwars.common.util.UnitUtils;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SPlayer;
import mekwars.server.campaign.SUnit;
import mekwars.server.campaign.commands.Command;

/**
 * @author Jason Tighe
 */
public class AdminGetUnitComponentsCommand implements Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "TargetPlayer#Option[BreakDownUnit,DisplayParts,AddParts]#Unit[ID,FileName]";

    public void process(StringTokenizer command, String Username) {

        // access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser(
                  STR."AM:Insufficient access level for command. Level: \{userLevel}. Required: \{accessLevel}.",
                  Username, true);
            return;
        }

        int year = CampaignMain.campaignMain.getIntegerConfig("CampaignYear");

        try {
            String targetName = command.nextToken();
            String option = command.nextToken();
            StringBuilder result = new StringBuilder();

            SPlayer target = CampaignMain.campaignMain.getPlayer(targetName);

            if (target == null) {
                CampaignMain.campaignMain.toUser(
                      "Target player could not be found. Try again.",
                      Username, true);
                return;
            }

            if (option.equalsIgnoreCase("DisplayParts")) {
                result.append("You have the current parts stockpiled<br>");
                result.append(target.getUnitParts().tableComponents(year));

                CampaignMain.campaignMain.toUser(result.toString(), Username);
            } else if (option.equalsIgnoreCase("addParts")) {
                // non-null target, so attempt the scrap
                int unitID = MathUtility.parseInt(command.nextToken(), -1);

                SUnit unit = target.getUnit(unitID);

                if (unit != null) {
                    Entity ent = unit.getEntity();

                    if (ent != null) {
                        target.getUnitParts().add(getUnitComponents(ent));
                    }

                    CampaignMain.campaignMain.toUser(
                          STR."All useable parts from \{unit.getModelName()} where added to \{target.getName()}'s parts stockpile",
                          Username);
                    CampaignMain.campaignMain.toUser(
                          STR."All useable parts from \{unit.getModelName()} where added to your parts stockpile",
                          target.getName());
                }
            } else {
                // non-null target, so attempt the scrap
                int unitID = MathUtility.parseInt(command.nextToken(), -1);
                SUnit unit = target.getUnit(unitID);

                // break out if the player doesn't have a unit with that id
                if (unit == null) {
                    CampaignMain.campaignMain.toUser(
                          STR."Target player doesn't have a unit with ID# \{unitID}.", Username, true);
                    return;
                }
                Entity ent = unit.getEntity();
                Hashtable<String, Integer> components = new Hashtable<>(getUnitComponents(ent));

                result.append("Component list for #")
                      .append(unitID)
                      .append(" ")
                      .append(unit.getModelName())
                      .append("<br>");
                result.append("<table><tr><th>Component</th><th># of Crits</th></tr>");

                for (String key : components.keySet()) {
                    result.append("<tr><td>");
                    result.append(key);
                    result.append("</td><td>");
                    result.append(components.get(key));
                    result.append("</td></tr>");
                }
                result.append("</table>");

                CampaignMain.campaignMain.toUser(result.toString(), Username);

            }
        } catch (Exception ex) {
            CampaignMain.campaignMain
                  .toUser(
                        "Invalid Syntax: /admingetunitcomponents TargetPlayer#Option[BreakDownUnit,DisplayParts,AddParts]#Unit[ID,FileName]",
                        Username);
        }
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int executionLevel) {
        accessLevel = executionLevel;
    }

    public String getSyntax() {
        return syntax;
    }

    public Hashtable<String, Integer> getUnitComponents(Entity ent) {
        Hashtable<String, Integer> components = new Hashtable<>();

        int IS = 0;
        int armor = 0;
        int rear = 0;
        String part;

        for (int location = 0; location < ent.locations(); location++) {
            IS += Math.max(0, ent.getInternal(location));
            armor += Math.max(0, ent.getArmor(location));
            rear += Math.max(0, ent.getArmor(location, true));

            for (int slot = 0; slot < ent.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot crit = ent.getCritical(location, slot);
                if (crit == null) {
                    continue;
                }

                if (UnitUtils.isActuator(crit)) {
                    part = "Actuator";
                } else if (crit.getType() == CriticalSlot.TYPE_SYSTEM) {
                    part = ((Mek) ent).getSystemName(crit.getIndex());
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
                  STR."Armor: \{EquipmentType.getArmorTypeName(ent.getArmorType(location))}", armor + rear);
        }

        components.put(
              STR."IS: \{EquipmentType.getStructureTypeName(ent.getStructureType())}", IS);

        return components;

    }
}// end AdminGetUnitComponentsCommand
