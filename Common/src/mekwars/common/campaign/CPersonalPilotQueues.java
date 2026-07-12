/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekwars.common.campaign;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringTokenizer;

import megamek.logging.MMLogger;
import mekwars.common.Unit;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.campaign.pilot.skills.PilotSkill;
import mekwars.common.util.TokenReader;

/**
 * @author Torren (Jason Tighe)
 *       <p>
 *       client-side holder of Personal Pilot Queue information. The queue is a collection of pilots, managed by a
 *       player, which may be moved between eligible units (restricted by type and weightclass). This client-side
 *       representation is necessary in order to draw menus and controls in the CHQPanel.
 *       <p>
 *       Conceptually this is a per-{@link CPlayer} "reserve bench" of {@link Pilot}s that aren't currently piloting
 *       any {@link CUnit}: pilots wait here (organized by unit type and weight class) until the player assigns them
 *       to a unit, or they're pulled in automatically to replace a casualty. It never holds a reference to the
 *       owning player; it is simply a field the player/HQ data model owns and keeps in sync with the server via
 *       {@link #fromString}, {@link #addPilot}, and {@link #removePilot}.
 */

public class CPersonalPilotQueues {
    private final static MMLogger LOGGER = MMLogger.create(CPersonalPilotQueues.class);

    /*
     * Don't need to synchronize on the client side. Two threads won't WRITE to
     * these, although multiple threads may read.
     */
    /** Waiting BattleMek pilots, indexed by weight class ({@link Unit#LIGHT}..{@link Unit#ASSAULT}). */
    private final ArrayList<LinkedList<Pilot>> mekPilots = new ArrayList<>();
    /** Waiting ProtoMek pilots, indexed by weight class. */
    private final ArrayList<LinkedList<Pilot>> protoPilots = new ArrayList<>();
    /** Waiting Aerospace pilots, indexed by weight class. */
    private final ArrayList<LinkedList<Pilot>> aeroPilots = new ArrayList<>();

    // CONSTRUCTOR

    /**
     * Simple param-free constructor that creates pilot-holding LinkedLists, in multiple weight classes (L -> A).
     * All three type-specific ArrayLists (mek/proto/aero) end up with one empty {@link LinkedList} per weight class
     * from {@link Unit#LIGHT} through {@link Unit#ASSAULT}, so queue lookups never need a null-check.
     */
    public CPersonalPilotQueues() {

        for (int i = Unit.LIGHT; i <= Unit.ASSAULT; i++) {// for (0 - 3)
            mekPilots.add(i, new LinkedList<>());
            protoPilots.add(i, new LinkedList<>());
            aeroPilots.add(i, new LinkedList<>());
        }

    }

    // METHODS

    /**
     * Method to add a pilot to the client side queue. This discrete update saves bandwidth by allowing a single pilot
     * (instead of the whole queue, as was done in the past) to be sent down when a game ends w/ a dispossessed pilot, a
     * new pilot is hired, etc.
     * <p>
     * Format: PL|AP2PPQ|Unit Type|Unit Weight Class|Pilot Data
     * <p>
     * Appends the parsed pilot to the tail (last position) of the queue matching the given unit type and weight
     * class. Any exception while parsing/adding (malformed tokens, bad indices) is caught and logged rather than
     * propagated, so a malformed server message will silently fail to add the pilot.
     *
     * @param ST tokenizer positioned at the unit-type token of a single addPilot payload.
     */
    public void addPilot(StringTokenizer ST) {
        try {
            int pilotType = TokenReader.readInt(ST);
            int pilotClass = TokenReader.readInt(ST);
            Pilot pilot = getPilotFromString(TokenReader.readString(ST));

            this.getUnitTypeQueue(pilotType).get(pilotClass).addLast(pilot);
        } catch (Exception ex) {
            LOGGER.error(ex, "Error while adding pilot to PPQ");
        }

    }

    /**
     * Private method that reads Pilot data from a "#"-delimited PPQ sub-string. Eliminates duplicative code in
     * {@link #fromString}'s multiple loops through the full data, and is also used by {@link #addPilot}.
     * <p>
     * Reads name, experience, gunnery, and piloting skill, then a skill count followed by that many
     * {@link PilotSkill} entries. Certain named skills carry extra payload beyond the base PilotSkill fields and are
     * special-cased here: "Weapon Specialist" reads an extra weapon-name token, "Trait" reads an extra
     * current-faction token, and "Edge" reads four extra booleans (TAC/KO/head-hit/explosion re-roll flags). Finally
     * reads a kill count.
     *
     * @param pilotData the "#"-delimited fields describing a single pilot.
     * @return the fully reconstructed {@link Pilot}.
     */
    private Pilot getPilotFromString(String pilotData) {
        StringTokenizer subTokenizer = new StringTokenizer(pilotData, "#");
        String pilotName = TokenReader.readString(subTokenizer);
        int exp = TokenReader.readInt(subTokenizer);
        int gunnery = TokenReader.readInt(subTokenizer);
        int piloting = TokenReader.readInt(subTokenizer);// will always be 5

        // set up the pilot
        Pilot pilot = new Pilot(pilotName, gunnery, piloting);
        pilot.setExperience(exp);

        // read skills, if any
        int skillAmount = TokenReader.readInt(subTokenizer);
        for (int i = 0; i < skillAmount; i++) {
            PilotSkill skill = new PilotSkill(TokenReader.readInt(subTokenizer),
                  TokenReader.readString(subTokenizer),
                  TokenReader.readInt(subTokenizer),
                  TokenReader.readString(subTokenizer));

            if (skill.getName().equals("Weapon Specialist")) {
                pilot.setWeapon(TokenReader.readString(subTokenizer));
            }

            if (skill.getName().equals("Trait")) {
                pilot.setCurrentFaction(TokenReader.readString(subTokenizer));
            }

            if (skill.getName().equals("Edge")) {
                pilot.setTac(TokenReader.readBoolean(subTokenizer));
                pilot.setKO(TokenReader.readBoolean(subTokenizer));
                pilot.setHeadHit(TokenReader.readBoolean(subTokenizer));
                pilot.setExplosion(TokenReader.readBoolean(subTokenizer));
            }

            pilot.getSkills().add(skill);
        }

        pilot.setKills(TokenReader.readInt(subTokenizer));

        // all done.
        return pilot;
    }

    /**
     * Rather than if/else'ing meks and ProtoMeks throughout the other methods of the class, use a private get method
     * which returns mek or proto as needed and then work on the arraylist without regard to type.
     * <p>
     * Note: any unit type other than {@link Unit#PROTOMEK} or {@link Unit#AERO} falls through to the mek queue —
     * this includes vehicles, infantry, and battle armor, which do not have their own dedicated queues in this class.
     *
     * @param typeToGet one of the {@link Unit} type constants.
     * @return the per-weight-class queue list appropriate for that unit type.
     */
    private ArrayList<LinkedList<Pilot>> getUnitTypeQueue(int typeToGet) {
        if (typeToGet == Unit.PROTOMEK) {
            return protoPilots;
        }

        if (typeToGet == Unit.AERO) {
            return aeroPilots;
        }

        return mekPilots;
    }

    /**
     * Method that removes a specific pilot from the PPQ. This discrete update saves bandwidth by eliminating the need
     * to send the entire hangar to the player when a pilot is removed.
     * <p>
     * Format: PL|RPPPQ|Unit Type|Unit Weight|Position
     * <p>
     * Removes the pilot at the given list index (not by identity/name) from the matching type/weight-class queue.
     * Any exception (e.g. index out of bounds if client/server queues have drifted out of sync) is caught and
     * logged rather than propagated.
     *
     * @param stringTokenizer tokenizer positioned at the unit-type token of a single removePilot payload.
     */
    public void removePilot(StringTokenizer stringTokenizer) {

        try {
            int pilotType = TokenReader.readInt(stringTokenizer);
            int pilotClass = TokenReader.readInt(stringTokenizer);
            int pilotPosition = TokenReader.readInt(stringTokenizer);

            this.getUnitTypeQueue(pilotType).get(pilotClass).remove(pilotPosition);
        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to remove pilot form queue");
        }
    }

    /**
     * Method that returns a particular class/size queue. Used throughout the client code to fetch a queue, which is
     * then iterated to draw menus, dialog boxes, etc.
     * <p>
     * Because these queues are always created in the constructor, they will never be null, even if a LIGHT_ONLY option
     * for vehicles or infantry is enabled.
     *
     * @param unitType one of the {@link Unit} type constants (non-ProtoMek/Aero types map to the mek queue).
     * @param weightClass one of the {@link Unit} weight-class constants ({@link Unit#LIGHT}..{@link Unit#ASSAULT}).
     * @return the live queue of waiting pilots for that type/weight-class combination.
     */
    public LinkedList<Pilot> getPilotQueue(int unitType, int weightClass) {
        return this.getUnitTypeQueue(unitType).get(weightClass);
    }

    /**
     * Convert a server-generated String into useful data-actual pilots, in proper type and class-based LinkedLists.
     * <p>
     * NOTE: String send by the server is generated in SPPQueues.java and delimited with $'s (main) and #'s
     * (subtokens).
     * <p>
     * Performs a full replace of all queue contents: first clears every mek/proto/aero weight-class list, then
     * reads, in order, four weight-class-worth of mek pilot lists (light through assault), then four for ProtoMeks,
     * then four for Aeros — each list preceded by a token giving its size. This is a wholesale resync (as opposed to
     * {@link #addPilot}/{@link #removePilot}'s discrete updates), typically used on initial connect/login.
     *
     * @param stringFromServer the full "$"-delimited PPQ payload for this player.
     */
    public void fromString(String stringFromServer) {
        StringTokenizer mainTokenizer = new StringTokenizer(stringFromServer, "$");

        // first, clear all existing pilots from the linked lists
        for (LinkedList<Pilot> currList : mekPilots) {
            currList.clear();
        }

        for (LinkedList<Pilot> currList : protoPilots) {
            currList.clear();
        }

        for (LinkedList<Pilot> currList : aeroPilots) {
            currList.clear();
        }

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
