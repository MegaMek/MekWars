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
 * Created on 21.04.2004
 *
 */
package server.campaign.pilot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.StringTokenizer;

import common.campaign.pilot.Pilot;
import common.campaign.pilot.skills.PilotSkill;
import common.util.MWLogger;
import common.util.TokenReader;
import megamek.common.Infantry;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.pilot.skills.AstechSkill;
import server.campaign.pilot.skills.EdgeSkill;
import server.campaign.pilot.skills.SPilotSkill;
import server.campaign.pilot.skills.TraitSkill;
import server.campaign.pilot.skills.WeaponSpecialistSkill;
import server.campaign.util.SerializedMessage;

/**
 * @author Helge Richter
 *
 */
public class SPilot extends Pilot {
    /**
     *
     */
    private int originalID;
    private int pickedUpID;
    private boolean death = false;

    public SPilot(String name, int gunnery, int piloting) {
        super(name, gunnery, piloting);
    }

    public SPilot() {
        // TODO: remove when possible
    }

    /**
     * Used to check and level up pilot. This should only be called from within
     * ShortResolver.getSalvageStrings()
     *
     * @param the
     *            unit to check
     * @param owner
     *            of unit. used to send updates.
     * @return a string detailing outcome.
     */
    public String checkForPilotSkillImprovement(SUnit unit, SPlayer owner) {

        if ( CampaignMain.cm.getBooleanConfig("PlayersCanBuyPilotUpgrades") ){
            return "";
        }

        SHouse house = owner.getMyHouse();
        /*
         * This is a pretty radical departure from the old Tasks-style levelling
         * chart. People will flip their crap when they see they no longer have
         * 1/5 units.
         */
        int bestGunnery = house.getIntegerConfig("BestGunnerySkill");
        int bestPiloting = house.getIntegerConfig("BestPilotingSkill");
        int bestTotal = house.getIntegerConfig("BestTotalPilot");

        int startingSkillAmount = 10;// 4/6 and 5/5 are worst possible green
        // units

        int currentSkillAmount = getGunnery() + getPiloting();
        SPilotSkill skillToAdd = null;

        /*
         * - If the differential is positive, we will always level gunnery. - If
         * the differential is negative, we will always level piloting. - If the
         * differential is 0, we'll level gunnery 70% of the time and piloting
         * 30% of the time.
         */
        int differential = getGunnery() - getPiloting();

        if (unit.getEntity() instanceof Infantry) {
            // Give AntiMek and Non Anti Mek infantry an equal chance of
            // leveling Gunnery and Piloting.
            differential = 0;
        }
        /*
         * Adjust the differential for NaturalAptitudes. Push the differential
         * towards the skill we want to get.
         *
         * Note that this "push" creates 2/4 gunners and 4/3 pilots.
         */
        if (getSkills().has(PilotSkill.NaturalAptitudeGunnerySkillID)) {
            differential++;
        }
        if (getSkills().has(PilotSkill.NaturalAptitudePilotingSkillID)) {
            differential--;
        }

        // save BV for before and after comparison
        int oldBV = unit.getBVForMatch();

        /*
         * Check to see whether or not the pilot is elite. This is used to
         * determine whether or not to make a level-up roll, and in the
         * construction of a return string.
         */
        boolean pilotIsElite = false;
        if (getGunnery() <= bestGunnery && getPiloting() <= bestPiloting) {
            pilotIsElite = true;
        }
        if (getGunnery() + getPiloting() <= bestTotal) {
            pilotIsElite = true;
        }

        /*
         * Determine how high a player must roll in order to achieve a level up.
         * This is somewhat configurable, but not exactly the highly ganular
         * control some operators would like to see.
         *
         * Should be revisited at a later date.
         */
        int baseRollToLevel = house.getIntegerConfig("BaseRollToLevel");
        int rollMultiplier = house.getIntegerConfig("MultiplierPerPreviousLevel");

        /*
         * Determine the difference between the starting and current skills.
         * Elite pilots should roll for retirement against the same 1dX they
         * rolled their final level up, which means we need to subtract 1 from
         * their multiplier.
         *
         * If the multiplier somehow drops below 0, set it back to 1.
         */
        int multiplier = startingSkillAmount - currentSkillAmount;
        if (pilotIsElite) {
            multiplier--;
        }
        if (multiplier < 1) {
            multiplier = 1;
        }

        // set up the size die to roll
        int dieSize = 0;
        if (rollMultiplier <= 0) {
            dieSize = baseRollToLevel;
        } else {
            dieSize = baseRollToLevel * rollMultiplier * multiplier;
        }

        /*
         * Check the level up mode. If the pilot isn't elite, we check for level
         * ups. If the pilto is elite, and there's a chance to retire
         * automatically on this server, we check for a level DOWN (back to
         * base).
         *
         * If the levels are fixed, bump the pilot only if the XP is higher than
         * the die size. If the level ups are random, roll the die and change
         * the level if the pilot's XP is greater than the roll.
         */
        boolean shouldLevelUp = false;
        boolean shouldLevelDown = false;
        boolean useRandomLevels = house.getBooleanConfig("UseRandomPilotLevelups");
        if (!pilotIsElite) {

            if (!useRandomLevels && getExperience() >= dieSize) {
                shouldLevelUp = true;
            } else if (getExperience() >= CampaignMain.cm.getRandomNumber(dieSize)) {
                shouldLevelUp = true;
            }

        } else if (pilotIsElite && house.getBooleanConfig("RandomRetirementOfElites")) {

            if (!useRandomLevels && getExperience() >= dieSize) {
                shouldLevelDown = true;
            } else if (getExperience() >= CampaignMain.cm.getRandomNumber(dieSize)) {
                shouldLevelDown = true;
            }

        }

        /*
         * Check to see if he gains any kind of PilotSkill. This is a flat %
         * chance for skill gain, and does not vary as piloting and gunnery
         * change. A pilot may not gain a skill at the same time that he changes
         * levels.
         *
         * It may be possible for a null skill tobe returned (eg - a total
         * chance of skill acquisition of 0 for a certain unit type, like BA or
         * vehs). Ignore the null here, but don't add the skill later or clear
         * XP.
         */
        boolean pilotsCanGainSkills = house.getBooleanConfig("PilotSkills");
        if (!shouldLevelUp && !shouldLevelDown && pilotsCanGainSkills) {

            int chanceToGainSkill = house.getIntegerConfig("SkillLevelChance");
            if (unit.getPilot().getSkills().has(PilotSkill.GiftedID)) {
                chanceToGainSkill += house.getIntegerConfig("GiftedPercent");
            }

            int dieRoll = CampaignMain.cm.getRandomNumber(100) + 1;
            if (dieRoll < chanceToGainSkill) {
                skillToAdd = SPilotSkills.getRandomSkill(this, unit.getType());
            }
        }

        /*
         * If the pilot gains a skill or levels up, clear his experience.
         */
        if (shouldLevelUp || shouldLevelDown || skillToAdd != null) {
            setExperience(0);
        }

        /*
         * Do the actual levelling up, if necessary.
         */
        if (shouldLevelUp) {

            int random = CampaignMain.cm.getRandomNumber(10);
            boolean levelGunnery = false;
            boolean levelPiloting = false;

            if(CampaignMain.cm.getBooleanConfig("AllowAsymmetricPilotLevels")) {
            	// Have the differential modify it.  The further away from x/x+1 they are, the more likely
            	// to level up in a manner that pushes it toward x/x+1, but can still get quite varied levelups
            	
            	// at 4/5, differential = -1
            	// at 3/5, differential = -2
            	// at 4/4, differential = 0
            	// at 4/3, differential = 1
            	
            	if((random - differential) < 5)
            		levelGunnery = true;
            	else
            		levelPiloting = true;
            } else {
            	if (differential > 0)
            		levelGunnery = true;
            	else if (differential < 0)
            		levelPiloting = true;
            	else if (random < 3 // 0-2, 30% chance for piloting on push
            			|| (unit.getEntity() instanceof Infantry && random < 5)) // differential
            		levelPiloting = true; // 50/50 for Infantry
            	else
            		levelGunnery = true;
            }

            /*
             * The natural aptitude skills sometimes make it possible to get an
             * otherwise banned level up. Check to make sure that the
             * piloting/gunnery level rolled isn't going to break the caps.
             *
             * If there is an underage, check the opposite skill and switch the
             * upgrade if possible. If a flip isn't allowed, don't return and
             * send the elite message.
             */
            if (levelGunnery && getGunnery() <= bestGunnery) {
                levelGunnery = false;
                if (getPiloting() > bestPiloting) {
                    levelPiloting = true;
                } else {
                    pilotIsElite = true;
                }
            }
            if (levelPiloting && getPiloting() <= bestPiloting) {
                levelPiloting = false;
                if (getGunnery() > bestGunnery) {
                    levelGunnery = true;
                } else {
                    pilotIsElite = true;
                }
            }

            // do the actual level ups
            if (levelGunnery) {
                setGunnery(getGunnery() - 1);
            } else if (levelPiloting) {

                if (unit.getEntity() instanceof Infantry) {
                    if (((Infantry) unit.getEntity()).canMakeAntiMekAttacks()){
                        setPiloting(getPiloting() - 1);
                    }
                    else{
                        // do not change piloting for Non-AntiMek Infantry
                        levelPiloting = false;
                    }
                } else{
                    setPiloting(getPiloting() - 1);
                }
            }

            // only send returns if the pilot was actually changed (might have
            // been untouched because of caps)
            if (levelGunnery || levelPiloting) {
                unit.setPilot(this);// refresh pilot! HACKY! CHANGE!
                return " and advanced a level. " + getName() + " is now " + getGunnery() + "/" + getPiloting() + " [Old BV: " + oldBV + "/New BV: " + unit.getBVForMatch() + "]";
            }
        }

        /*
         * Do actual leveling down, if necessary.
         */
        else if (shouldLevelDown) {

            // return pilot to faction base
            setGunnery(owner.getMyHouse().getBaseGunner(unit.getType()));
            setPiloting(owner.getMyHouse().getBasePilot(unit.getType()));

            // Store the old name for use in return, and change to successor
            // name
            String oldName = getName();

            // Age the pilot. Odds of someone getting beyond a 10th generation
            // eliete are so slim that we need not worry.
            if (oldName.endsWith("Jr.")) {
                setName(oldName.substring(0, oldName.lastIndexOf("Jr.")) + "III");
            } else if (oldName.endsWith("III")) {
                setName(oldName.substring(0, oldName.lastIndexOf("III")) + "IV");
            } else if (oldName.endsWith("IV")) {
                setName(oldName.substring(0, oldName.lastIndexOf("IV")) + "V");
            } else if (oldName.endsWith("V")) {
                setName(oldName + "I");
            } else if (oldName.endsWith("VI")) {
                setName(oldName + "I");
            } else if (oldName.endsWith("VII")) {
                setName(oldName + "I");
            } else if (oldName.endsWith("VIII")) {
                setName(oldName.substring(0, oldName.lastIndexOf("VIII")) + "IX");
            } else if (oldName.endsWith("IX")) {
                setName(oldName.substring(0, oldName.lastIndexOf("IX")) + "X");
            } else {
                setName(oldName + " Jr.");
            }
            
            // New pilots are getting old injuries
            super.setHits(0);

            unit.setPilot(this);// refresh pilot! HACKY! CHANGE!
            return ". " + oldName + " grew weary of war and retired from active duty. The unit was passed on to " + getName() + " [" + getGunnery() + "/" + getPiloting() + ", Old BV: " + oldBV + "/New BV: " + unit.getBVForMatch() + "]";
        }

        if (skillToAdd != null) {

            // special accomidation for WS and Trait
            if (skillToAdd instanceof WeaponSpecialistSkill) {
                ((WeaponSpecialistSkill) skillToAdd).assignWeapon(unit.getEntity(), this);
            } else if (skillToAdd instanceof TraitSkill) {
                ((TraitSkill) skillToAdd).assignTrait(this);
            } else if (skillToAdd instanceof EdgeSkill) {
                if (getSkills().has(PilotSkill.EdgeSkillID)) {
                    skillToAdd = (SPilotSkill) getSkills().getPilotSkill(PilotSkill.EdgeSkillID);
                    ((EdgeSkill) skillToAdd).setLevel(skillToAdd.getLevel() + 1);
                }
            }else if (skillToAdd instanceof AstechSkill) {
                if (getSkills().has(PilotSkill.AstechSkillID)) {
                    skillToAdd = (SPilotSkill) getSkills().getPilotSkill(PilotSkill.AstechSkillID);
                    ((AstechSkill) skillToAdd).setLevel(skillToAdd.getLevel() + 1);
                }
            }

            skillToAdd.addToPilot(this);
            skillToAdd.modifyPilot(this);
            unit.setPilot(this);// refresh pilot! HACKY! CHANGE!

            int newBV = unit.getBVForMatch();

            if (skillToAdd instanceof AstechSkill && !CampaignMain.cm.isUsingAdvanceRepair()) {
                CampaignMain.cm.toUser("PL|SF|" + owner.getFreeBays(), owner.getName(), false);
            }

            String toSend = ". " + getName() + " gained the " + skillToAdd.getName() + " skill";
            if (newBV != oldBV) {
                toSend += " [Old BV: " + oldBV + "/New BV: " + newBV + "]";
            }

            return toSend;
        }

        /*
         * NOTE: ShortResolver looks for "is elite" in SkillImprovement returns
         * to differntiate between those units which level up and those which
         * simply cannot level. If this message is changed at all, make sure to
         * update the ShortResovler as well.
         */
        if (pilotIsElite) {
            return " but could not level up because " + getName() + " is elite";
        }
        // else
        return "";
    }// end checkForPilotSkillImprovement

    public String toFileFormat(String delimiter, boolean toPlayer) {
        SerializedMessage result = new SerializedMessage(delimiter);
        result.append(getName());
        result.append(getExperience());
        result.append(getGunnery());
        result.append(getPiloting());
        result.append(getSkills().size());
        for (PilotSkill skill : getSkills().getPilotSkills()) {
            SPilotSkill sk = (SPilotSkill) skill;
            result.append(sk.getId());
            if (toPlayer) {
                result.append(sk.getName());
            }
            result.append(sk.getLevel());
            if (toPlayer) {
                result.append(sk.getAbbreviation());
            }
            if (sk instanceof WeaponSpecialistSkill) {
                result.append(getWeapon());
            }
            if (sk instanceof TraitSkill) {
                result.append(getTraitName());
            }
            if (sk instanceof EdgeSkill) {
                result.append(((EdgeSkill) sk).getTac());
                result.append(((EdgeSkill) sk).getKO());
                result.append(((EdgeSkill) sk).getHeadHit());
                result.append(((EdgeSkill) sk).getExplosion());
            }

        }
        result.append(getKills());
        if (!toPlayer) {
            if (getCurrentFaction().trim().length() > 0) {
                result.append(getCurrentFaction());
            } else {
                result.append(CampaignMain.cm.getConfig("NewbieHouseName"));
            }
            result.append(getPilotId());
        }
        if (CampaignMain.cm.getBooleanConfig("AllowPilotDamageToTransfer")) {
            if (toPlayer) {

                // recalculate the number of hits a pilot has for CBT standards
                // If you have less then AllowPilotDamageToTransfer but greater
                // then 0
                // then set to 1
                int hits = 0;
                if (getHits() != 0) {
                    hits = Math.max(1, getHits() / CampaignMain.cm.getIntegerConfig("AmountOfDamagePerPilotHit"));
                }
                result.append(hits);
            } else {
                result.append(getHits());
            }
        } else {
            result.append(0);
        }

        if (!toPlayer) {
            result.append(0);// unused var
        }

        return result.toString();
    }

    public void fromFileFormat(String s, String delimiter) {

        TraitSkill traitSkill = null;

        try {
            StringTokenizer ST = new StringTokenizer(s, delimiter);

            if (ST.countTokens() < 1) {
                setName("NULL");
                return;
            }
            setName(TokenReader.readString(ST));
            setExperience(TokenReader.readInt(ST));
            setGunnery(TokenReader.readInt(ST));
            setPiloting(TokenReader.readInt(ST));
            int skills = TokenReader.readInt(ST);
            for (int i = 0; i < skills; i++) {
                SPilotSkill skill = SPilotSkills.getPilotSkill(TokenReader.readInt(ST));
                int level = TokenReader.readInt(ST);
                if (skill instanceof AstechSkill) {
                    skill = new AstechSkill(PilotSkill.AstechSkillID);
                }

                if (skill instanceof WeaponSpecialistSkill) {
                    setWeapon(TokenReader.readString(ST));
                }
                if (skill instanceof TraitSkill) {
                    String traitName = TokenReader.readString(ST);

                    if (traitName.equalsIgnoreCase("none")) {
                        traitSkill = (TraitSkill) skill;
                    } else {
                        setTraitName(traitName);
                    }
                }

                if (skill instanceof EdgeSkill) {
                    skill = new EdgeSkill(PilotSkill.EdgeSkillID);
                    ((EdgeSkill) skill).setTac(TokenReader.readBoolean(ST));
                    ((EdgeSkill) skill).setKO(TokenReader.readBoolean(ST));
                    ((EdgeSkill) skill).setHeadHit(TokenReader.readBoolean(ST));
                    ((EdgeSkill) skill).setExplosion(TokenReader.readBoolean(ST));
                }

                skill.setLevel(level);
                skill.addToPilot(this);
                skill.modifyPilot(this);
            }

            setKills(TokenReader.readInt(ST));

            setCurrentFaction(TokenReader.readString(ST));

            setPilotId(TokenReader.readInt(ST));

            setHits(TokenReader.readInt(ST));

            TokenReader.readString(ST);

            /*
             * some times a pilot doesn't get assigned a skill this code fixes
             * that however the Trait skill needs a house name other wise they
             * default to common.
             */
            if (traitSkill != null) {
                traitSkill.assignTrait(this);
            }

            if (getPilotId() == -1) {
                setPilotId(CampaignMain.cm.getAndUpdateCurrentPilotID());
            }
        } catch (Exception ex) {
            MWLogger.errLog("Error loading Pilot " + getName());
            MWLogger.errLog(ex);
        }
    }

    /*
     * Thanks to Helge from MegaMekNet for the code snippet.
     */

    /**
     * @return Returns the originalID.
     */
    public int getOriginalID() {
        return originalID;
    }

    /**
     * @param originalID
     *            The originalID to set.
     */
    public void setOriginalID(int originalID) {
        this.originalID = originalID;
    }

    /**
     * @return Returns the pickedUpID.
     */
    public int getPickedUpID() {
        return pickedUpID;
    }

    /**
     * @param pickedUpID
     *            The pickedUpID to set.
     */
    public void setPickedUpID(int pickedUpID) {
        this.pickedUpID = pickedUpID;
    }

    /**
     * sets Pilots living status
     *
     * @param death -
     *            whether or not the pilot is alive or dead
     */
    public void setDeath(boolean death) {
        this.death = death;
    }

    /**
     * @return Returns living status.
     */
    public boolean isDead() {
        return death;
    }

    public static SPilot getMekWarrior(int originalID, int pickedUpId) {
        SPilot mw = new SPilot();
        mw.setOriginalID(originalID);
        mw.setPickedUpID(pickedUpId);
        return mw;
    }

    /**
     * @author Torren (Jason Tighe)
     * @param pilot
     * @return String
     */
    public String getPilotCaptureMessageToOwner(SUnit unit) {
    	BufferedReader dis = null; 
        try {

            File folder = new File("./data/pilotmessages");

            if (!folder.exists()) {
                folder.mkdir();
            }

            String scrapFile = "/pilotcapturemessagestoowner.txt";
            // MWLogger.errLog(folder.getPath()+scrapFile);
            FileInputStream fis = new FileInputStream(folder.getPath() + scrapFile);
            dis = new BufferedReader(new InputStreamReader(fis));
            int messages = Integer.parseInt(dis.readLine());
            Random rand = new Random();
            int id = rand.nextInt(messages);
            String scrapMessage = "";
            while (dis.ready()) {
                scrapMessage = dis.readLine();
                if (id <= 0) {
                    break;
                }
                id--;
            }
            String scrapMessageWithPilot = scrapMessage.replaceAll("PILOT", getName());
            return scrapMessageWithPilot.replaceAll("UNIT", unit.getModelName());

        } catch (FileNotFoundException fnfn) {
            return getName() + " was captured by enemy forces after fleeing the " + unit.getModelName() + ".";
        } catch (Exception e) {
            MWLogger.errLog("A problem occured with your pilotcapturemessagestoowner File!");
            return getName() + " was captured by enemy forces after fleeing the " + unit.getModelName() + ".";
        } finally {
        	if (dis != null) {
        		try {
					dis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					MWLogger.errLog("Unable to close reader in GetPilotCaptureMessageToOwner");
					MWLogger.errLog(e.getMessage());
				}
        	}
        }

    }

    /**
     * @author Torren (Jason Tighe)
     * @param pilot
     * @return String
     */
    public String getPilotCaptureAndDefectedMessage(SUnit unit, SHouse house) {
    	BufferedReader dis = null;
        try {

            File folder = new File("./data/pilotmessages");

            if (!folder.exists()) {
                folder.mkdir();
            }

            String scrapFile = "/pilotcaptureanddefectedmessages.txt";
            // MWLogger.errLog(folder.getPath()+scrapFile);
            FileInputStream fis = new FileInputStream(folder.getPath() + scrapFile);
            dis = new BufferedReader(new InputStreamReader(fis));
            int messages = Integer.parseInt(dis.readLine());
            Random rand = new Random();
            int id = rand.nextInt(messages);
            String scrapMessage = "";
            while (dis.ready()) {
                scrapMessage = dis.readLine();
                if (id <= 0) {
                    break;
                }
                id--;
            }
            String scrapMessageWithPilot = scrapMessage.replaceAll("PILOT", getName());
            String scrapMessageWithHouse = scrapMessageWithPilot.replaceAll("HOUSE", house.getNameAsLink());
            return scrapMessageWithHouse.replaceAll("UNIT", unit.getModelName());

        } catch (FileNotFoundException fnfn) {
            return getName() + " was rescued from his unit by our infantry and has decided to join " + house.getColoredNameAsLink() + ".";
        } catch (Exception e) {
            MWLogger.errLog("A problem occured with your pilotcapturemessagesdefect File!");
            return getName() + " was rescued from his unit by our infantry and has decided to join " + house.getColoredNameAsLink() + ".";
        } finally {
        	if (dis != null) {
        		try {
					dis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					MWLogger.errLog("Unable to close reader in GetPilotCaptureAndDefectedMessage");
					MWLogger.errLog(e.getMessage());
				}
        	}
        }
    }

    /**
     * @author Torren (Jason Tighe)
     * @param pilot
     * @return String
     */
    public String getPilotCaptureAndRemovedMessage(SUnit unit) {
    	BufferedReader dis = null;
        try {

            File folder = new File("./data/pilotmessages");

            if (!folder.exists()) {
                folder.mkdir();
            }

            String scrapFile = "/pilotcaptureandremovedmessages.txt";
            // MWLogger.errLog(folder.getPath()+scrapFile);
            FileInputStream fis = new FileInputStream(folder.getPath() + scrapFile);
            dis = new BufferedReader(new InputStreamReader(fis));
            int messages = Integer.parseInt(dis.readLine());
            Random rand = new Random();
            int id = rand.nextInt(messages);
            String scrapMessage = "";
            while (dis.ready()) {
                scrapMessage = dis.readLine();
                if (id <= 0) {
                    break;
                }
                id--;
            }
            String scrapMessageWithPilot = scrapMessage.replaceAll("PILOT", getName());
            return scrapMessageWithPilot.replaceAll("UNIT", unit.getModelName());

        } catch (FileNotFoundException fnfn) {
            return getName() + " captured by our infantry transferred to HQ for interrogation.";
        } catch (Exception e) {
            MWLogger.errLog("A problem occured with your pilotcapturemessagesdefect File!");
            return getName() + " captured by our infantry transferred to HQ for interrogation.";
        } finally {
        	if (dis != null) {
        		try {
					dis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					MWLogger.errLog("Unable to close reader in GetPilotCaptureAndRemoveMessage");
					MWLogger.errLog(e.getMessage());
				}
        	}
        }

    }

    /**
     * @author Torren (Jason Tighe)
     * @param pilot
     * @return String
     */
    public String getPilotRescueMessage(SUnit unit) {
    	BufferedReader dis = null;
        try {

            File folder = new File("./data/pilotmessages");

            if (!folder.exists()) {
                folder.mkdir();
            }

            String scrapFile = "/pilotrescuemessages.txt";
            // MWLogger.errLog(folder.getPath()+scrapFile);
            FileInputStream fis = new FileInputStream(folder.getPath() + scrapFile);
            dis = new BufferedReader(new InputStreamReader(fis));
            int messages = Integer.parseInt(dis.readLine());
            Random rand = new Random();
            int id = rand.nextInt(messages);
            String scrapMessage = "";
            while (dis.ready()) {
                scrapMessage = dis.readLine();
                if (id <= 0) {
                    break;
                }
                id--;
            }
            String scrapMessageWithPilot = scrapMessage.replaceAll("PILOT", getName());
            return scrapMessageWithPilot.replaceAll("UNIT", unit.getModelName());

        } catch (FileNotFoundException fnfn) {
            return getName() + " hiked back to base.";
        } catch (Exception e) {
            MWLogger.errLog("A problem occured with your pilotcapturemessages File!");
            return getName() + " hiked back to base.";
        } finally {
        	if (dis != null) {
        		try {
					dis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					MWLogger.errLog("Unable to close reader in GetPilotRescueMessage");
					MWLogger.errLog(e.getMessage());
				}
        	}
        }

    }

    // STATIC METHODS
    /**
     * Get a random pilot name.
     *
     * @param r
     * @return
     */
    public static String getRandomPilotName(Random r) {
        String result = "John Doe";
        BufferedReader dis = null;
        try {
            File configFile = new File("./data/pilotnames/Pilotnames.txt");
            FileInputStream fis = new FileInputStream(configFile);
            dis = new BufferedReader(new InputStreamReader(fis));
            int names = Integer.parseInt(dis.readLine());
            int pilotid = r.nextInt(names);
            while (dis.ready()) {
                String line = dis.readLine();
                if (pilotid <= 0) {
                    return line;
                }

                // else
                pilotid--;
            }
        } catch (Exception e) {
            MWLogger.errLog("A problem occured with your Pilotnames File!");
        } finally {
        	if (dis != null) {
        		try {
					dis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					MWLogger.errLog("Unable to close reader in GetRandomPilotName");
					MWLogger.errLog(e.getMessage());
				}
        	}
        }
        return result;
    }

}
