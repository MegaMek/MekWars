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

package server.campaign.pilot.skills;

import common.MegaMekPilotOption;
import common.campaign.pilot.Pilot;
import megamek.common.Entity;

/**
 * @author Helge Richter
 */
public class ClanPilotTrainingSkill extends SPilotSkill {

    public ClanPilotTrainingSkill() {
        // TODO: replace with ReflectionProvider
    }

    public ClanPilotTrainingSkill(int id) {
        super(id, "Clan Pilot Training", "CPT");
        setDescription("Pilot has a +1 penalty for physical attacks, because clans do not train for dishonourable combat.");
    }

    @Override
    public void modifyPilot(Pilot p) {
        // super.addToPilot(p);
        p.addMegamekOption(new MegaMekPilotOption("clan_pilot_training", true));
        p.setBvMod(p.getBVMod() + 0.01);
    }

    @Override
    public int getChance(int unitType, Pilot p) {
        return 0;
    }

    @Override
    public int getBVMod(Entity unit) {

        return 0;
    }

}