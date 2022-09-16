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

/*
 * Created:       03/25/05
 * Last refactor: 01/12/06
 */
package client.campaign;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringTokenizer;

import common.Unit;
import common.campaign.pilot.Pilot;
import common.campaign.pilot.skills.PilotSkill;
import common.util.MWLogger;
import common.util.TokenReader;

/**
 * @author Torren (Jason Tighe)
 * 
 * Client-side holder of Personal Pilot Queue information. The queue is a
 * collection of pilots, managed by a player, which may be moved between
 * eligible units (restricted by type and weightclass). This client-side
 * representation is necessary in order to draw menus and controls in the
 * CHQPanel.
 */

public class CPersonalPilotQueues {

    /*
     * Don't need to synchronize on the client side. Two threads won't WRITE to
     * these, although multiple threads may read.
     */
    private ArrayList<LinkedList<Pilot>> mekPilots = new ArrayList<LinkedList<Pilot>>();
    private ArrayList<LinkedList<Pilot>> protoPilots = new ArrayList<LinkedList<Pilot>>();
    private ArrayList<LinkedList<Pilot>> aeroPilots = new ArrayList<LinkedList<Pilot>>();

    // CONSTRUCTOR
    /**
     * Simple param-free constructor that creates pilot-holding LinkedLists, in
     * multiple weight classes (L -> A).
     */
    public CPersonalPilotQueues() {

        for (int i = Unit.LIGHT; i <= Unit.ASSAULT; i++) {// for (0 - 3)
            mekPilots.add(i, new LinkedList<Pilot>());
            protoPilots.add(i, new LinkedList<Pilot>());
            aeroPilots.add(i, new LinkedList<Pilot>());
        }

    }

    // METHODS
    /**
     * Rather than if/else'ing meks and protos throughout the other methods of
     * the class, use a private get method which returns mek or proto as needed
     * and then work on the arraylist without regard to type.
     */
    private ArrayList<LinkedList<Pilot>> getUnitTypeQueue(int typeToGet) {

        if (typeToGet == Unit.PROTOMEK)
            return protoPilots;
        //else if
        if (typeToGet == Unit.AERO)
            return aeroPilots;
        // else
        return mekPilots;
    }

    /**
     * Private method which reads SPilot data from a PPQ string. Eliminates
     * dulpicative code in formString's multiple loops thorugh the full data.
     */
    private Pilot getPilotFromString(String pilotData) {

        StringTokenizer subTokenizer = new StringTokenizer(pilotData, "#");
        String pilotname = TokenReader.readString(subTokenizer);
        int exp = TokenReader.readInt(subTokenizer);
        int gunnery = TokenReader.readInt(subTokenizer);
        int piloting = TokenReader.readInt(subTokenizer);// will always be 5

        // set up the pilot
        Pilot pilot = new Pilot(pilotname, gunnery, piloting);
        pilot.setExperience(exp);

        // read skills, if any
        int skillAmount = TokenReader.readInt(subTokenizer);
        for (int i = 0; i < skillAmount; i++) {
            PilotSkill skill = new PilotSkill(TokenReader.readInt(subTokenizer), TokenReader.readString(subTokenizer), TokenReader.readInt(subTokenizer), TokenReader.readString(subTokenizer));

            if (skill.getName().equals("Weapon Specialist"))// WS skill has an
                                                            // extra var
                pilot.setWeapon(TokenReader.readString(subTokenizer));

            if (skill.getName().equals("Trait"))// Trait skill has an extra var
                pilot.setCurrentFaction(TokenReader.readString(subTokenizer));

            if (skill.getName().equals("Edge")) {
                pilot.setTac(TokenReader.readBoolean(subTokenizer));
                pilot.setKO(TokenReader.readBoolean(subTokenizer));
                pilot.setHeadHit(TokenReader.readBoolean(subTokenizer));
                pilot.setExplosion(TokenReader.readBoolean(subTokenizer));
            }

            pilot.getSkills().add(skill);
        }

        // read the kills, if any

        pilot.setKills(TokenReader.readInt(subTokenizer));

        // all done. whoopdie doo.
        return pilot;
    }

    /**
     * Method to add a pilot to the client side queue. This discrete update
     * saves bandwidth by allowing a single pilot (instead of the whole queue,
     * as was done in the past) to be sent down when a game ends w/ a
     * dispossessed pilot, a new pilot is hired, etc.
     * 
     * Format: PL|AP2PPQ|Unit Type|Unit Weight Class|Pilot Data
     */
    public void addPilot(StringTokenizer ST) {
        try {
            int pilotType = TokenReader.readInt(ST);
            int pilotClass = TokenReader.readInt(ST);
            Pilot pilot = getPilotFromString(TokenReader.readString(ST));

            this.getUnitTypeQueue(pilotType).get(pilotClass).addLast(pilot);
        } catch (Exception ex) {
            MWLogger.errLog("Error while adding pilot to PPQ");
            MWLogger.errLog(ex);
        }

    }

    /**
     * Method that removes a specific pilot from the PPQ. This discrete update
     * saves bandwidth by eliminating the need to send the entire hangar to the
     * player when a pilot is removed.
     * 
     * Format: PL|RPPPQ|Unit Type|Unit Weight|Position
     */
    public void removePilot(StringTokenizer ST) {

        try {
            int pilotType = TokenReader.readInt(ST);
            int pilotClass = TokenReader.readInt(ST);
            int pilotPosition = TokenReader.readInt(ST);

            this.getUnitTypeQueue(pilotType).get(pilotClass).remove(pilotPosition);
        } catch (Exception ex) {
            MWLogger.errLog("Unable to remove pilot form queue");
            MWLogger.errLog(ex);
        }
    }

    /**
     * Method that returns a particular class/size queue. Used throughout the
     * client code to fecth queue, which are then iterated in order to draw
     * menus, dialog boxes, etc.
     * 
     * Because these queues are always created in the constructor, they will
     * never be null, even if a LIGHTONLY option for vehs or infantry is
     * enabled.
     */
    public LinkedList<Pilot> getPilotQueue(int unitType, int weightClass) {
        return this.getUnitTypeQueue(unitType).get(weightClass);
    }

    /**
     * Convert a server-generated String into usedful data - actual pilots, in
     * proper type and class-based LinkedLists.
     * 
     * NOTE: String send by the server is generated in SPPQueues.java, and
     * delimited with $'s (main) and #'s (subtokens).
     */
    public void fromString(String stringFromServer) {

        StringTokenizer mainTokenizer = new StringTokenizer(stringFromServer, "$");

        // first, clear all existing pilots from the linked lists
        for (LinkedList<Pilot> currList : mekPilots)
            currList.clear();
        for (LinkedList<Pilot> currList : protoPilots)
            currList.clear();
        for (LinkedList<Pilot> currList : aeroPilots)
            currList.clear();

        // loop once to read in meks (light -> assault lists)
        for (int weightClass = Unit.LIGHT; weightClass <= Unit.ASSAULT; weightClass++) {

            int listSize = TokenReader.readInt(mainTokenizer);
            for (int count = 0; count < listSize; count++) {
                Pilot toAdd = this.getPilotFromString(TokenReader.readString(mainTokenizer));
                this.getUnitTypeQueue(Unit.MEK).get(weightClass).addLast(toAdd);
            }
        }

        // loop a second time to read in protomeks (light -> assault lists)
        for (int weightClass = Unit.LIGHT; weightClass <= Unit.ASSAULT; weightClass++) {

            int listSize = TokenReader.readInt(mainTokenizer);
            for (int count = 0; count < listSize; count++) {
                Pilot toAdd = this.getPilotFromString(TokenReader.readString(mainTokenizer));
                this.getUnitTypeQueue(Unit.PROTOMEK).get(weightClass).addLast(toAdd);
            }
        }

        // loop a third time to read in Aeros (light -> assault lists)
        for (int weightClass = Unit.LIGHT; weightClass <= Unit.ASSAULT; weightClass++) {

            int listSize = TokenReader.readInt(mainTokenizer);
            for (int count = 0; count < listSize; count++) {
                Pilot toAdd = this.getPilotFromString(TokenReader.readString(mainTokenizer));
                this.getUnitTypeQueue(Unit.AERO).get(weightClass).addLast(toAdd);
            }
        }
    }

}// end CPPQ
