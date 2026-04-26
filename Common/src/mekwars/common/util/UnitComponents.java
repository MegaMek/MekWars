/*
 * MekWars - Copyright (C) 2006
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

package mekwars.common.util;

import java.util.Comparator;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.TechConstants;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;

public class UnitComponents {

    Hashtable<String, Integer> components = new Hashtable<>();

    public String tableizeComponents(int year) {
        return tableizeComponents(components, year);
    }

    public static Comparator<? super Object> stringComparator() {
        return (Comparator<Object>) (o1, o2) -> {
            String s1 = ((String) o1).toLowerCase();
            String s2 = ((String) o2).toLowerCase();
            return s1.compareTo(s2);
        };
    }

    public String tableizeComponents(Hashtable<String, Integer> parts, int year) {

        StringBuilder result = new StringBuilder();

        Comparator<? super Object> stringCompare = UnitComponents.stringComparator();

        Hashtable<String, String> keys = new Hashtable<>();

        for (String key : parts.keySet()) {
            keys.put(UnitComponents.getName(key), key);
        }

        Vector<String> equipment = new Vector<>(keys.keySet());
        equipment.sort(stringCompare);

        result.append(
              "<table><tr><th>Component</th><th># of Crits</th><th>Tech</th><th>Component</th><th># of Crits</th><th>Tech</th></tr>");
        result.append("<tr><td>");
        for (int pos = 0; pos < equipment.size(); pos++) {
            String key = equipment.get(pos);
            result.append(key);
            result.append("</td><td>");
            result.append(parts.get(keys.get(key)));
            result.append("</td><td>");
            result.append(UnitComponents.getTech(keys.get(key), year));
            if ((pos % 2) == 1) {
                result.append("</td></tr>");
                result.append("<tr><td>");
            } else {
                result.append("</td><td>");
            }

        }
        result.append("</table>");

        return result.toString();
    }

    public String toString(String token) {
        StringBuilder result = new StringBuilder();

        if (components.isEmpty()) {
            return token + " " + token;
        }
        for (String key : components.keySet()) {

            if (components.get(key) < 1) {
                continue;
            }
            result.append(key);
            result.append(token);
            result.append(components.get(key));
            result.append(token);
        }
        return result.toString();

    }

    public void fromString(String data, String token) {

        StringTokenizer st = new StringTokenizer(data, token);

        try {
            components.clear();
            while (st.hasMoreTokens()) {
                String key = st.nextToken();
                if (!st.hasMoreElements()) {
                    return;
                }
                int value = Integer.parseInt(st.nextToken());
                components.put(key, value);
            }
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }

    }

    public void fromString(StringTokenizer st) {

        try {
            components.clear();
            while (st.hasMoreTokens()) {
                components.put(st.nextToken(), Integer.parseInt(st.nextToken()));
            }
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }

    }

    public void add(String part, int amount) {

        if (amount < 1) {
            return;
        }

        if (components.containsKey(part)) {
            components.put(part, Math.max(0, components.get(part) + amount));
        } else {
            components.put(part, amount);
        }

    }

    public void add(Hashtable<String, Integer> parts) {

        for (String part : parts.keySet()) {

            if (components.containsKey(part)) {
                components.put(part, components.get(part) + parts.get(part));
            } else {
                components.put(part, parts.get(part));
            }
        }
    }

    public String canRepoUnit(Entity mainUnit, Entity repoUnit) {

        Hashtable<String, Integer> mainUnitParts = new Hashtable<>();
        Hashtable<String, Integer> repoUnitParts = new Hashtable<>();

        int IS = 0;
        int armor = 0;
        int rear = 0;
        String part;

        for (int location = 0; location < mainUnit.locations(); location++) {
            IS += Math.max(0, mainUnit.getInternal(location));
            armor += Math.max(0, mainUnit.getArmor(location));
            rear += Math.max(0, mainUnit.getArmor(location, true));
            for (int slot = 0; slot < mainUnit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot crit = mainUnit.getCritical(location, slot);
                if ((crit == null) || crit.isDamaged()) {
                    continue;
                }

                part = UnitUtils.getCritName(mainUnit, slot, location, false);

                if (part.equalsIgnoreCase("Ammo Bin")) {
                    Mounted<?> mount = crit.getMount();
                    String ammoName = mount.getType().getInternalName();

                    if (mainUnitParts.containsKey(ammoName)) {
                        mainUnitParts.put(ammoName, mainUnitParts.get(ammoName) + mount.getUsableShotsLeft());
                    } else {
                        mainUnitParts.put(ammoName, mount.getUsableShotsLeft());
                    }
                    if (mainUnitParts.containsKey(part)) {
                        mainUnitParts.put(part, mainUnitParts.get(part) + 1);
                    } else {
                        mainUnitParts.put(part, 1);
                    }
                } else {
                    if (mainUnitParts.containsKey(part)) {
                        mainUnitParts.put(part, mainUnitParts.get(part) + 1);
                    } else {
                        mainUnitParts.put(part, 1);
                    }
                }

            }
        }

        part = UnitUtils.getCritName(mainUnit, UnitUtils.LOC_CENTER_TORSO, 0, true);

        mainUnitParts.put(part, armor + rear);

        part = UnitUtils.getCritName(mainUnit, UnitUtils.LOC_INTERNAL_ARMOR, 0, true);
        mainUnitParts.put(part, IS);


        IS = 0;
        armor = 0;
        rear = 0;

        for (int location = 0; location < repoUnit.locations(); location++) {
            IS += Math.max(0, repoUnit.getInternal(location));
            armor += Math.max(0, repoUnit.getArmor(location));
            rear += Math.max(0, repoUnit.getArmor(location, true));
            for (int slot = 0; slot < repoUnit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot crit = repoUnit.getCritical(location, slot);
                if (crit == null) {
                    continue;
                }

                part = UnitUtils.getCritName(repoUnit, slot, location, false);

                if (part.equalsIgnoreCase("Ammo Bin")) {
                    Mounted<?> mount = crit.getMount();
                    String ammoName = mount.getType().getInternalName();

                    if (repoUnitParts.containsKey(ammoName)) {
                        repoUnitParts.put(ammoName, repoUnitParts.get(ammoName) + mount.getUsableShotsLeft());
                    } else {
                        repoUnitParts.put(ammoName, mount.getUsableShotsLeft());
                    }
                    if (repoUnitParts.containsKey(part)) {
                        repoUnitParts.put(part, repoUnitParts.get(part) + 1);
                    } else {
                        repoUnitParts.put(part, 1);
                    }
                } else {
                    if (repoUnitParts.containsKey(part)) {
                        repoUnitParts.put(part, repoUnitParts.get(part) + 1);
                    } else {
                        repoUnitParts.put(part, 1);
                    }
                }
            }
        }

        part = UnitUtils.getCritName(repoUnit, UnitUtils.LOC_FRONT_ARMOR, 0, true);

        repoUnitParts.put(part, armor + rear);

        part = UnitUtils.getCritName(repoUnit, UnitUtils.LOC_INTERNAL_ARMOR, 0, true);

        repoUnitParts.put(part, IS);

        StringBuilder result = new StringBuilder("<table><tr>");
        int count = 0;
        boolean missingCrits = false;
        for (String key : repoUnitParts.keySet()) {

            if (!components.containsKey(key) && !mainUnitParts.containsKey(key)) {
                missingCrits = true;
                result.append("<td>");
                result.append(repoUnitParts.get(key)).append(" of ").append(UnitComponents.getName(key));
                result.append("</td>");
                if ((count++ % 4) == 3) {
                    result.append("</tr><tr>");
                }
            } else {

                int partAmount = 0;

                if (components.containsKey(key)) {
                    partAmount += components.get(key);
                }
                if (mainUnitParts.containsKey(key)) {
                    partAmount += mainUnitParts.get(key);
                }

                if (partAmount < repoUnitParts.get(key)) {
                    missingCrits = true;
                    result.append("<td>");
                    result.append(repoUnitParts.get(key) - partAmount)
                          .append(" of ")
                          .append(UnitComponents.getName(key));
                    result.append("</td>");
                    if ((count++ % 4) == 3) {
                        result.append("</tr><tr>");
                    }
                }
            }
        }

        result.append("</table>");

        //Have all the parts return nothing.
        if (!missingCrits) {
            return "";
        }

        return result.toString();
    }

    public boolean repoUnit(Entity mainUnit, Entity repoUnit) {

        Hashtable<String, Integer> repoUnitParts = new Hashtable<>();

        int IS = 0;
        int armor = 0;
        int rear = 0;
        String part;

        for (int location = 0; location < mainUnit.locations(); location++) {
            IS += Math.max(0, mainUnit.getInternal(location));
            armor += Math.max(0, mainUnit.getArmor(location));
            rear += Math.max(0, mainUnit.getArmor(location, true));
            for (int slot = 0; slot < mainUnit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot crit = mainUnit.getCritical(location, slot);
                if ((crit == null) || crit.isDamaged()) {
                    continue;
                }

                part = UnitUtils.getCritName(mainUnit, slot, location, false);

                if (part.contains("Ammo")) {
                    Mounted<?> mount = crit.getMount();

                    this.add(part, mount.getUsableShotsLeft());
                    this.add("Ammo Bin", 1);
                } else {
                    this.add(part, 1);
                }
            }
        }

        part = UnitUtils.getCritName(mainUnit, UnitUtils.LOC_FRONT_ARMOR, 0, true);

        this.add(part, armor + rear);

        part = UnitUtils.getCritName(mainUnit, UnitUtils.LOC_INTERNAL_ARMOR, 0, true);
        this.add(part, IS);

        IS = 0;
        armor = 0;
        rear = 0;

        for (int location = 0; location < mainUnit.locations(); location++) {
            IS += Math.max(0, repoUnit.getInternal(location));
            armor += Math.max(0, repoUnit.getArmor(location));
            rear += Math.max(0, repoUnit.getArmor(location, true));
            for (int slot = 0; slot < repoUnit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot crit = repoUnit.getCritical(location, slot);
                if (crit == null) {
                    continue;
                }
                part = UnitUtils.getCritName(repoUnit, slot, location, false);

                if (part.contains("Ammo")) {
                    Mounted<?> mount = crit.getMount();

                    if (repoUnitParts.containsKey(part)) {
                        repoUnitParts.put(part, repoUnitParts.get(part) + mount.getUsableShotsLeft());
                    } else {
                        repoUnitParts.put(part, mount.getUsableShotsLeft());
                    }
                    if (repoUnitParts.containsKey("Ammo Bin")) {
                        repoUnitParts.put("Ammo Bin", repoUnitParts.get("Ammo Bin") + 1);
                    } else {
                        repoUnitParts.put("Ammo Bin", 1);
                    }
                } else {
                    if (repoUnitParts.containsKey(part)) {
                        repoUnitParts.put(part, repoUnitParts.get(part) + 1);
                    } else {
                        repoUnitParts.put(part, 1);
                    }
                }
            }
        }

        part = UnitUtils.getCritName(repoUnit, UnitUtils.LOC_FRONT_ARMOR, 0, true);

        repoUnitParts.put(part, armor + rear);

        part = UnitUtils.getCritName(repoUnit, UnitUtils.LOC_INTERNAL_ARMOR, 0, true);

        repoUnitParts.put(part, IS);

        for (String key : repoUnitParts.keySet()) {
            remove(key, repoUnitParts.get(key));
        }

        return true;
    }

    public int getPartsCritCount(String key) {

        if (components.get(key) == null) {
            return 0;
        }
        return components.get(key);
    }

    public boolean hasEnoughCrits(String crit, int amount) {

        if (components.get(crit) == null) {
            return false;
        }

        return components.get(crit) >= amount;
    }

    public void remove(String key, int amount) {

        if (components.get(key) == null) {
            return;
        }

        int parts = components.get(key);

        parts -= Math.abs(amount);

        if (parts <= 0) {
            components.remove(key);
        } else {
            components.put(key, parts);
        }

    }

    public static String getTech(String crit, int year) {
        EquipmentType eq = EquipmentType.get(crit);


        //Armor, IS, Engines, Actuators, Cockpit, Sensors anything that doesn't
        //make a normal object in MM
        if (eq == null) {

            return "All";
        } else {

            if (UnitUtils.isClanEQ(eq, year)) {
                return "Clan";
            }
            if ((eq.getTechLevel(year) == TechConstants.T_ALL) ||
                      (eq.getTechLevel(year) <= TechConstants.T_TW_ALL)) {
                return "All";
            }

            return "IS";

        }

    }

    public static String getName(String crit) {
        EquipmentType eq = EquipmentType.get(crit);
        //Armor,IS,Engines,Actuators,Cockpit,Sensors anything that doesn't
        //make a normal object in MM
        if (eq == null) {
            return crit;
        }

        return eq.getName();
    }

    public void clear() {
        components.clear();
    }
}
