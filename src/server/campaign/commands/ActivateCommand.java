/*
 * MekWars - Copyright (C) 2004
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

package server.campaign.commands;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import common.Unit;
import common.campaign.operations.Operation;
import common.util.MWLogger;
import common.util.UnitUtils;
import megamek.common.Mech;
import server.MWServ;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.operations.newopmanager.I_OperationManager;
import server.campaign.util.scheduler.MWScheduler;
import server.util.SPlayerToJSON;
import server.util.StringUtil;

public class ActivateCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

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

        if (accessLevel != 0) {
            int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".", Username, true);
                return;
            }
        }

        SPlayer p = CampaignMain.cm.getPlayer(Username);
        if (Boolean.parseBoolean(CampaignMain.cm.getConfig("CampaignLock")) == true) {
            CampaignMain.cm.toUser("AM:The campaign is currently locked. Player activation is disabled until the campaign is unlocked. NOTE: Running games will resolve normally.", Username, true);
            return;
        }

        if (command.hasMoreTokens()) {
            p.setPlayerClientVersion(command.nextToken());
        }
        
        // Put it into a try block. One user was causing FormatExceptions
        try {
            if (!(MWServ.SERVER_VERSION).substring(0, server.MWServ.SERVER_VERSION.lastIndexOf(".")).equals(p.getPlayerClientVersion().substring(0, p.getPlayerClientVersion().lastIndexOf(".")))) {
                // server.MWLogger.modLog(Username + " failed to activate. Was using version " + p.getPlayerClientVersion()+" Server Version: "+
                // MWServ.SERVER_VERSION);
                CampaignMain.cm.doSendModMail("NOTE", Username + " failed to activate. Was using version " + p.getPlayerClientVersion() + " Server Version: " + MWServ.SERVER_VERSION);
                CampaignMain.cm.toUser("AM:You may not go active with an incompatible client version! Please switch to version " + MWServ.SERVER_VERSION + "!", Username, true);
                return;
            }
        } catch (Exception ex) {
            MWLogger.errLog("Error activating player. User reported client verson: " + p.getPlayerClientVersion() + " --- Stack Trace Follows.");
            // MWLogger.errLog(ex);
            CampaignMain.cm.toUser("AM:Your clients version was not reported to the server. <a href=\"MEKWARS/c setclientversion#" + Username + "#" + MWServ.SERVER_VERSION + "\">Click here to update the server.</a> then try to activate again.", Username);
            return;
        }

        int currentStatus = p.getDutyStatus();
        if (currentStatus == SPlayer.STATUS_FIGHTING) {
            CampaignMain.cm.toUser("AM:You are already fighting!", Username, true);
            return;
        }

        if (currentStatus == SPlayer.STATUS_ACTIVE) {
            CampaignMain.cm.toUser("AM:You are already on active duty!", Username, true);
            return;
        }
        // for those sleepy mods who activate and attack while invis
        if (p.isInvisible()) {
            CampaignMain.cm.toUser("AM:You are invisible... tsk tsk.", Username, true);
            return;
        }

        // this should never come up, but better safe than sorry
        if (currentStatus == SPlayer.STATUS_LOGGEDOUT) {
            CampaignMain.cm.toUser("AM:You are logged out and may not activate.", Username, true);
            return;
        }

        boolean MULOnlyOps = CampaignMain.cm.getOpsManager().hasMULOnlyOps();
        // if player has no armies, break out
        if (p.getArmies().size() == 0 && !MULOnlyOps) {
            CampaignMain.cm.toUser("AM:You must have armies constructed in order to activate.", Username, true);
            return;
        }

        // if player has no enabled armies, break out
        int enabledArmies = 0;
        for (SArmy a : p.getArmies()) {
            if (!a.isDisabled()) {
                enabledArmies++;
                // All we need is one enabled army
                break;
            }
        }

        if (enabledArmies == 0 && !MULOnlyOps) {
            CampaignMain.cm.toUser("AM:You must have at least 1 enabled army to activate.", Username, true);
            return;
        }

        // check for empty armies, pilotless units
        for (SArmy currA : p.getArmies()) {

            if (currA.getAmountOfUnits() == 0 && !MULOnlyOps) {
                CampaignMain.cm.toUser("AM:You may not activate with empty armies.", Username, true);
                return;
            }

            if (currA.hasPilotWithTooManySkills()) {
                CampaignMain.cm.toUser("AM:Army #" + currA.getID() + " has a pilot with too many pilot skills, you may not go active.", Username, true);
                return;
            }

            for (Unit currU : currA.getUnits()) {

                if (currU.hasVacantPilot()) {
                    CampaignMain.cm.toUser("AM:You may not activate with pilotless units!", Username, true);
                    return;
                }
            }

        }

        if (!CampaignMain.cm.getBooleanConfig("allowGoingActiveWithoutUnitCommanders") && hasCommanderlessUnits(p.getArmies())) {
            CampaignMain.cm.toUser("AM:You may not activate with armies lacking unit commanders!", Username, true);
            return;
        }

        // AR-only activation checks
        if (CampaignMain.cm.isUsingAdvanceRepair()) {

            if (armiesContainEnginedUnit(p.getArmies())) {
                CampaignMain.cm.toUser("AM:You may not activate with a cored or engine-disabled unit in an army.", Username, true);
                return;
            }

            if (armiesContainLeggedUnit(p.getArmies())) {
                CampaignMain.cm.toUser("AM:You may not activate with a legless unit in an army.", Username, true);
                return;
            }

            if (p.hasRepairingUnits()) {
                CampaignMain.cm.toUser("AM:You may not activate while units in your armies are undergoing repairs.", Username, true);
                return;
            }

            if (CampaignMain.cm.getBooleanConfig("UseSimpleRepair") && armiesDamagedUnits(p.getArmies())) {
                CampaignMain.cm.toUser("AM:You may not activate while units in your armies have damage.", Username, true);
                return;
            }

            if (!CampaignMain.cm.getBooleanConfig("AllowUnitsToActivateWithPartialBins") && armiesPartialAmmoBinUnits(p.getArmies())) {
                CampaignMain.cm.toUser("AM:You may not activate while units in your armies have partial ammo bins.", Username, true);
                return;
            }
            
            if ( (!CampaignMain.cm.getBooleanConfig("AllowActivationWithDamagedUnits")) && this.armiesDamagedUnits(p.getArmies())) {
            	CampaignMain.cm.toUser("AM:You may not activate while units in your armies have damage.", Username, true);
            	return;
            }
        }

        /*
         * We can now be sure that the player sending the command is not
         * active. Check him for unmaintained units and bar him from the
         * front if an unmaint is in an army.
         * 
         * Might be cleaner to put this in Army as hasUnmaintainedUnits()
         * and call as needed, but this should be the only place this check
         * is currently done.
         */
        for (SArmy currA : p.getArmies()) {
            for (Unit currUnit : currA.getUnits()) {
                if (currUnit.getStatus() == Unit.STATUS_UNMAINTAINED) {
                    CampaignMain.cm.toUser("AM:You may not send armies containing unmaintained units to the front lines!", Username, true);
                    return;
                }
            }
        }

        if (p.getFreeBays() < 0 && CampaignMain.cm.getIntegerConfig("MaxNegativeBaysForActivation") > -1 && ((p.getFreeBays() + CampaignMain.cm.getIntegerConfig("MaxNegativeBaysForActivation")) < 0)) {
            if (CampaignMain.cm.isUsingAdvanceRepair()) {
                CampaignMain.cm.toUser("AM:You may not activate with more than " + CampaignMain.cm.getIntegerConfig("MaxNegativeBaysForActivation") + " negative bays!", Username, true);
            } else {
                CampaignMain.cm.toUser("AM:You may not activate with more than " + CampaignMain.cm.getIntegerConfig("MaxNegativeBaysForActivation") + " negative techs!", Username, true);
            }
            return;
        }

        int armyID = hasIllegalOpArmies(p, p.getArmies());

        if (armyID > -1) {
            CampaignMain.cm.toUser("AM:Army #" + armyID + " is currently unable to launch or defend any ops!  You may not go active.", Username, true);
            return;
        }
        
        armyID = hasNoAttackOptions(p, p.getArmies());
        
        if (armyID > -1) {
        	CampaignMain.cm.toUser("AM:Army #" + armyID + " is currently unable to launch any attacks!  You may not go active.", Username, true);
        	return;
        }

        // Check if the SOs have disabled activation while over unit limits
        if (CampaignMain.cm.getBooleanConfig("DisableActivationIfOverHangarLimits") && p.isOverAnyUnitLimits()) {
        	CampaignMain.cm.toUser("AM: You have exceeded one or more hangar limits.  Activation is disabled until you get under those limits.", Username, true);
        	return;
        }
        
        // @salient
        // Check if the SOs have disabled going active with unused mektokens
        if (CampaignMain.cm.getBooleanConfig("FreeBuild_LimitGoActive") && p.hasUnusedMekTokens()) {
        	CampaignMain.cm.toUser("AM: To go active you must first finish creating your free meks.", Username, true);
        	return;
        }
        
        // @salient
        // Check mini campaign settings
        // seperated out locked unit check so it can be used without mini campaigns.
        if(CampaignMain.cm.getBooleanConfig("LockUnits"))
        {
        	if(hasLockedUnitsInArmies(p.getArmies()))
        	{
        		p.toSelf("AM: To go active you must first remove locked units from your armies.");
        		return;
        	}        	
        }
        
        if(CampaignMain.cm.getBooleanConfig("Enable_MiniCampaign"))
        {
        	//handles all checks, resets currency, and msgs to player
	        if(p.canActivateForMiniCampaign() == false)
	        {
	        	return;
	        }
        }
        
        if(CampaignMain.cm.getBooleanConfig("Enable_BotPlayerInfo"))
        {
        	//update json file for this player (used with discord bot)
        	SPlayerToJSON.writeToFile(p);
        }
        
        if(CampaignMain.cm.getBooleanConfig("Activate_Subfaction_Only"))
        {
        	if(StringUtil.isNullOrEmpty(p.getSubFactionName()))
        	{
        		p.toSelf("AM: You must first join a subfaction before you can activate!");
        		return;
        	}
        }
        
        
        for (SArmy army : p.getArmies()) {
            CampaignMain.cm.getOpsManager().checkOperations(army, false);
        }

        // p.resetWeightedArmyNumber();
        p.getWeightedArmyNumber();

        // Start the Activity Jobs in Quartz to generate flu and components
        MWScheduler.getInstance().activateUser(p.getName(), p.getWeightedArmyNumber(), p.getHouseFightingFor().getName());
        
        p.setActive(true);

        CampaignMain.cm.toUser("AM:[!] You're on your way to the front lines.", Username, true);
        CampaignMain.cm.sendPlayerStatusUpdate(p,!Boolean.parseBoolean(CampaignMain.cm.getConfig("HideActiveStatus")));
        
        

        // set up a thread which will do an auto /c ca once the minactivetime expires
        int threadLenth = CampaignMain.cm.getIntegerConfig("MinActiveTime") * 1000;
        CheckAttackThread caThread = new CheckAttackThread(p, threadLenth);
        caThread.start();

    }// end process()

    /**
     * Method which returns a boolean indicating whether any unit in a set of armies has 3 engine crits.
     * 
     * Separate method, instead of inline in process(), so this can be moved into SPlayer easily if other code needs to do engine checks.
     */
    private boolean armiesContainEnginedUnit(Vector<SArmy> armies) {

        // loop though all units in all armies
        for (SArmy army : armies) {
            Iterator<Unit> units = army.getUnits().iterator();
            while (units.hasNext()) {
                SUnit unit = (SUnit) units.next();
                if (UnitUtils.isCored(unit.getEntity())) {
                    return true;
                }
            }
        }

        // no 3-hit units found. return false.
        return false;
    }

    /**
     * Method which returns a boolean indicating whether any unit in a set of armies is missing a leg.
     * 
     * Separate method, instead of inline in process(), so this can be moved into SPlayer easily if other code needs to do leglessness (sp?) checks.
     */
    private boolean armiesContainLeggedUnit(Vector<SArmy> armies) {

        // loop though all units in all armies
        for (SArmy army : armies) {
            Iterator<Unit> units = army.getUnits().iterator();
            while (units.hasNext()) {

                // check RL/LL on a standard mek
                SUnit unit = (SUnit) units.next();
                if (unit.getType() == Unit.MEK) {
                    if (unit.getEntity().getInternal(Mech.LOC_LLEG) <= 0) {
                        return true;
                    }
                    if (unit.getEntity().getInternal(Mech.LOC_RLEG) <= 0) {
                        return true;
                    }
                }

                // check all 4 legs on a quad
                else if (unit.getType() == Unit.QUAD) {
                    if (unit.getEntity().getInternal(Mech.LOC_LLEG) <= 0) {
                        return true;
                    }
                    if (unit.getEntity().getInternal(Mech.LOC_RLEG) <= 0) {
                        return true;
                    }
                    if (unit.getEntity().getInternal(Mech.LOC_LARM) <= 0) {
                        return true;
                    }
                    if (unit.getEntity().getInternal(Mech.LOC_RARM) <= 0) {
                        return true;
                    }
                }
            }
        }

        // no 3-hit units found. return false.
        return false;
    }

    /**
     * Method which returns a boolean indicating whether any unit in a set of armies has any damage.
     * 
     * Separate method, instead of inline in process(), so this can be moved into SPlayer easily if other code needs to do damaged unit checks.
     */
    private boolean armiesDamagedUnits(Vector<SArmy> armies) {

        // loop though all units in all armies
        for (SArmy army : armies) {
            Iterator<Unit> units = army.getUnits().iterator();
            while (units.hasNext()) {

                // check RL/LL on a standard mek
                SUnit unit = (SUnit) units.next();
                if (UnitUtils.hasArmorDamage(unit.getEntity())) {
                    return true;
                }
                if (UnitUtils.hasCriticalDamage(unit.getEntity())) {
                    return true;
                }
            }
        }

        // no 3-hit units found. return false.
        return false;
    }

    /**
     * This checks all the units in the army to see if they have partial ammo bins.
     * 
     */
    private boolean armiesPartialAmmoBinUnits(Vector<SArmy> armies) {

        // loop though all units in all armies
        for (SArmy army : armies) {
            Iterator<Unit> units = army.getUnits().iterator();
            while (units.hasNext()) {

                SUnit unit = (SUnit) units.next();
                if (!UnitUtils.hasAllAmmo(unit.getEntity())) {
                    return true;
                }
            }
        }

        // no units with partial ammobins found. return false.
        return false;
    }
    
    /**
     * @author Salient
     * see if any units in armies are locked, if so return true.
     * 
     */
    private boolean hasLockedUnitsInArmies(Vector<SArmy> armies) 
    {
        for (SArmy army : armies) 
        {
            Iterator<Unit> units = army.getUnits().iterator();
            while (units.hasNext()) 
            {
                SUnit unit = (SUnit) units.next();
                if (unit.isLocked()) 
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCommanderlessUnits(Vector<SArmy> armies) {
        // start Baruk Khazad!  20151108a 
    	Boolean result = false;  
        for (SArmy army : armies) {
            if (army.getCommanders().size() == 0 && !army.isDisabled()) {    
                result = true;   
            }
        }
        return result;  
        // end Baruk Khazad!  20151108a 
    }

    private int hasIllegalOpArmies(SPlayer player, Vector<SArmy> armies) {

        for (SArmy army : armies) {

            boolean canAttack = false;
            boolean canDefend = false;

            // check for legal attacks
            if (army.getLegalOperations().size() > 0) {
                canAttack = true;
            }

            // check for legal defense
            I_OperationManager manager = CampaignMain.cm.getOpsManager();
            for (Operation op : manager.getOperations().values()) {
                if (manager.validateShortDefense(player, army, op, null) == null) {
                    canDefend = true;
                    break;
                }
            }

            if (!canAttack && !canDefend && !army.isDisabled()) {
                return army.getID();
            }
        }

        return -1;
    }

    private int hasNoAttackOptions(SPlayer player, Vector<SArmy> armies) {

        for (SArmy army : armies) {

            boolean canAttack = false;
            boolean requireAttackCapable = CampaignMain.cm.getBooleanConfig("RequireAttackCapableArmiesForActivation");

            // check for legal attacks
            if (army.getLegalOperations().size() > 0) {
                canAttack = true;
            }

            if(!canAttack && !army.isDisabled() && requireAttackCapable) {
            	return army.getID();
            }
        }

        return -1;
    }
    
}// end activatecommand class

/**
 * @author urgru
 * 
 *         private thread. simple minactivetime wait which runs a checkattack for the activating player.
 */
class CheckAttackThread extends Thread {

    // vars
    SPlayer p;
    long duration;

    public CheckAttackThread(SPlayer p, int duration) {
        super("ActivationThread-" + p.getName());
        this.duration = duration; // set length when thread is spun
        this.p = p;
    }

    @Override
    public synchronized void run() {
        try {
            wait(duration + 250);// buffer by a quarter second

            // make sure the player is still active (hasnt logged out,
            // been forcedeactivated, attacked or joined a game).
            if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE) {
                CheckAttackCommand ca = new CheckAttackCommand();
                CampaignMain.cm.toUser("<br>You have arrived on the front lines!", p.getName(), true);
                ca.process(new StringTokenizer(""), p.getName());
            }
            p.leechCount = 0;
            // ran once. kill the thread by returning.
            return;
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }// end run()
}// end CheckAttackThread

