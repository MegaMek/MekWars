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

package server.campaign.operations;

import java.util.ArrayList;
import java.util.Vector;

import common.Unit;
import common.UnitFactory;
import common.campaign.operations.Operation;
import common.util.MWLogger;
import common.util.StringUtils;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.SUnitFactory;

public class OpsChickenThread extends Thread {

    // VARIABLE
    private SPlayer pdefender;
    private Vector<SArmy> parmies;
    private int opID;
    private String opName;
    private String message;

    private boolean shouldContinue;
    private int waittime;
    //private int leechCount;

    // CONSTRUCTOS
    public OpsChickenThread(SPlayer pd, int id, String name, String mess) {
        pdefender = pd;
        parmies = new Vector<SArmy>(1, 1);
        shouldContinue = true;
        //leechCount = 0;
        message = mess;
        opID = id;
        opName = name;

        // determine the wait period
        Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);
        waittime = o.getIntValue("TimeToNondefensePenalty");
    }

    // METHODS
    public void addArmy(SArmy a) {
        parmies.add(a);
    }

    public Vector<SArmy> getArmies() {
        return parmies;
    }

    /**
     * Method which turns the thread off.
     * 
     * Note - the thread will not take note of the change until it next wakes.
     * At that point it will terminate. This means that a thread with a long
     * wait time can stick around for several minutes after it is supposedly
     * "stopped."
     */
    public synchronized void stopChicken() {
        MWLogger.gameLog("ChickenThread " + opID + "/" + pdefender.getName() + " turned off.");
        shouldContinue = false;
    }

    /**
     * Method which gives a player a leech penalty.
     */
    public synchronized void doPenalty() {

        // if the stop signal was sent while we were
        // waiting, return and end.
        if (!shouldContinue)
            return;

        // new players lose nothing, but get a warning.
        if (pdefender.getMyHouse().isNewbieHouse()) {
            String toPlayer = "You did not defend Attack #" + opID + ". You've not been punished because " + "you are in the training faction; however, if you leave an attack undefended in a normal " + "faction you may lose money, units, experience, influence, rewards, or some combination thereof.";
            CampaignMain.cm.toUser(toPlayer, pdefender.getName(), true);
            return;
        }

        if (CampaignMain.cm.getIThread().isImmune(pdefender)) {
            String toPlayer = "You did not defend Attack #" + opID + ". You've not been punished because " + "you are still immune; however, if your immunity wears off and you're still under attack you may " + "lose money, units, experience, influence, rewards, or some combination thereof.";
            CampaignMain.cm.toUser(toPlayer, pdefender.getName(), true);
            return;
        }

        // get the latest copy of the operation
        Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);

        // load the percent hits.
        double rpPercent = o.getDoubleValue("PercentRPChickenPenalty");
        double expPercent = o.getDoubleValue("PercentExpChickenPenalty");
        double fluPrecent = o.getDoubleValue("PercentInfluenceChickenPenalty");
        double moneyPercent = o.getDoubleValue("PercentCBillChickenPenalty");

        // load the flat hits
        int rpFlat = o.getIntValue("FlatRPChickenPenalty");
        int expFlat = o.getIntValue("FlatExpChickenPenalty");
        int fluFlat = o.getIntValue("FlatInfluenceChickenPenalty");
        int moneyFlat = o.getIntValue("FlatCBillChickenPenalty");

        // holders for totals
        int totalRPLoss = 0;
        int totalEXPLoss = 0;
        int totalFluLoss = 0;
        int totalMoneyLoss = 0;

        // add % hits to the totals
        if (rpPercent > 0)
            totalRPLoss = (int) (pdefender.getReward() * rpPercent);
        if (expPercent > 0)
            totalEXPLoss = (int) (pdefender.getExperience() * expPercent);
        if (fluPrecent > 0)
            totalFluLoss = (int) (pdefender.getInfluence() * fluPrecent);
        if (moneyPercent > 0)
            totalMoneyLoss = (int) (pdefender.getMoney() * moneyPercent);

        // add flats to the totals
        if (rpFlat > 0)
            totalRPLoss += rpFlat;
        if (rpFlat > 0)
            totalEXPLoss += expFlat;
        if (rpFlat > 0)
            totalFluLoss += fluFlat;
        if (rpFlat > 0)
            totalMoneyLoss += moneyFlat;

        // check totals against the player's amounts and
        // correct to max if the total is too high.
        totalRPLoss = Math.min(totalRPLoss, pdefender.getReward());
        totalEXPLoss = Math.min(totalEXPLoss, pdefender.getExperience());
        totalFluLoss = Math.min(totalFluLoss, pdefender.getInfluence());
        totalMoneyLoss = Math.min(totalMoneyLoss, pdefender.getMoney());

        // assemble the loss string and send it to the player.
        boolean hasLoss = false;
        String toPlayer = "Attack #" + opID + " wasn't defended in time!";

        StringBuilder winnerHSUpdates = new StringBuilder();
        StringBuilder loserHSUpdates = new StringBuilder();

        if (totalRPLoss > 0) {
            if (!hasLoss)
                toPlayer += " (-" + totalRPLoss + " " + CampaignMain.cm.getConfig("RPShortName");
            else
                toPlayer += ", -" + totalRPLoss + " " + CampaignMain.cm.getConfig("RPShortName");
            pdefender.addReward(-totalRPLoss);
            hasLoss = true;
        }
        if (totalEXPLoss > 0) {
            if (!hasLoss)
                toPlayer += " (-" + totalEXPLoss + " XP";
            else
                toPlayer += ", -" + totalEXPLoss + " XP";
            pdefender.addExperience(-totalEXPLoss, false);
            hasLoss = true;
        }
        if (totalFluLoss > 0) {
            if (!hasLoss)
                toPlayer += " (-" + CampaignMain.cm.moneyOrFluMessage(false, true, totalFluLoss);
            else
                toPlayer += ", -" + CampaignMain.cm.moneyOrFluMessage(false, true, totalFluLoss);
            pdefender.addInfluence(-totalFluLoss);
            hasLoss = true;
        }
        if (totalMoneyLoss > 0) {
            if (!hasLoss)
                toPlayer += " (-" + CampaignMain.cm.moneyOrFluMessage(true, true, totalMoneyLoss);
            else
                toPlayer += ", -" + CampaignMain.cm.moneyOrFluMessage(true, true, totalMoneyLoss);
            pdefender.addMoney(-totalMoneyLoss);
            hasLoss = true;
        }

        if (hasLoss)
            toPlayer += ")";

        CampaignMain.cm.toUser(toPlayer, pdefender.getName(), true);

        /*
         * Player has been aprised of his troubles ... now let the whole world
         * know that the house was spanked. And, just for fun, reveal the
         * player's name. Shame is sometimes a better hinderance than the actual
         * punishment ...
         */

        // get the actual ShortOperation. Catch any nulls.
        ShortOperation parentOp = CampaignMain.cm.getOpsManager().getRunningOps().get(opID);
        if (parentOp == null) {
            MWLogger.errLog("Tried to do a leech with a null ShortOperation!");
            return;
        }

        // load the penalties
        int conquestPenalty = o.getIntValue("ConquestPerLeech");
        int delayPenalty = o.getIntValue("DelayPerLeech");
        int prodPenalty = o.getIntValue("ProdPointsPerLeech");
        int unitPenalty = o.getIntValue("UnitsPerLeech");
        int rolloverPenalty = o.getIntValue("FailurePenalty");

        // get the attacking house (first player from attacker tree)
        String firstKey = parentOp.getAttackers().firstKey();
        SHouse attackH = CampaignMain.cm.getPlayer(firstKey).getHouseFightingFor();

        // get the defending house (actual defender in this thread)
        SHouse defendH = pdefender.getHouseFightingFor();

        // get the target world
        SPlanet target = parentOp.getTargetWorld();

        String toMain = pdefender.getColoredName() + " did not defend " + StringUtils.aOrAn(attackH.getName(), true, false) + attackH.getColoredNameAsLink() + " attack on " + target.getNameAsColoredLink() + "(#" + opID + ") in time. " + defendH.getColoredNameAsLink() + " lost ";

        // Drop % to 0 if either group is non-conquer
        if (!attackH.isConquerable() || !defendH.isConquerable())
            conquestPenalty = 0;

        // Drop % to 0 if the planet is non-conquerable
        if (!target.isConquerable())
            conquestPenalty = 0;

        // reset hasLoss for formatting. it's also
        // used to determine whether or not to use
        // the rollover penalty.
        hasLoss = false;

        if (conquestPenalty > 0) {

            int actualPenalty = target.doGainInfluence(attackH, defendH, conquestPenalty, false);

            if (actualPenalty > 0) {
                toMain += actualPenalty + "cp from the world";
                hasLoss = true;
            }
        }

        if (delayPenalty > 0) {

            // return if there are no factories on the world
            if (target.getFactoryCount() == 0)
                return;

            // return if the defender doesnt own the world
            SHouse owner = target.getOwner();
            if (owner == null || !target.getOwner().equals(defendH))
                return;

            // damage all of the factories
            for (UnitFactory UF : target.getUnitFactories()) {
                SUnitFactory currFac = (SUnitFactory) UF;
                loserHSUpdates.append(currFac.addRefresh(delayPenalty, false));
            }

            if (hasLoss)
                toMain += ", production for " + delayPenalty + " miniticks";
            else {
                toMain += " production for " + delayPenalty + " miniticks";
                hasLoss = true;
            }
        }

        /*
         * Note that unlike the old task system which looked for factories and
         * took from them, or took light units if no factories were on world,
         * Operations takes the BEST units available at the time.
         */
        if (unitPenalty > 0) {

            ArrayList<SUnit> capturedUnits = new ArrayList<SUnit>();

            // for (all unit types, mek preferred)
            int numCaptured = 0;
            for (int type = Unit.MEK; type < Unit.MAXBUILD; type++) {

                // for all weights (assault preferred)
                for (int weight = Unit.ASSAULT; weight >= Unit.LIGHT; weight--) {

                    boolean noUnits = false;
                    while (!noUnits && numCaptured < unitPenalty) {
                        SUnit captured = defendH.getEntity(weight, type);
                        if (captured == null)
                            noUnits = true;
                        else {
                            capturedUnits.add(captured);
                            winnerHSUpdates.append(attackH.addUnit(captured, false));
                            loserHSUpdates.append(defendH.getHSUnitRemovalString(captured));
                            numCaptured++;
                        }
                    }// end while(units remain in this Weight+Type pool)
                }// end for all weights
            }// end all types

            if (numCaptured > 0) {

                // get the units
                String unitString = "";
                for (SUnit currU : capturedUnits) {
                    unitString += currU.getModelName() + ", ";
                }

                // strip the last ", "
                unitString = unitString.substring(0, unitString.length() - 2);

                // add to the string
                if (hasLoss) {
                    // toMain += ", " + numCaptured;
                    if (numCaptured == 1)
                        toMain += ", a unit [" + unitString + "]";
                    else
                        toMain += ", " + numCaptured + " units [" + unitString + "]";
                } else {
                    hasLoss = true;
                    // toMain += " " + numCaptured;
                    if (numCaptured == 1)
                        toMain += " a unit [" + unitString + "]";
                    else
                        toMain += " " + numCaptured + " units [" + unitString + "]";
                }
            }// end string creation
        }// end if(unitPen > 0)

        /*
         * Note that unlike the old task system which looked for factories and
         * took from them, or took light components if no factories were on
         * world, Operations takes the BEST Mek comps available at the time,
         * generally Assault Mek Components.
         */
        if (prodPenalty > 0 || (!hasLoss && rolloverPenalty > 0)) {

            // use the prod penalty if its over 0, the rollover if all
            // the other penalties have failed and PP is also 0.
            if (prodPenalty <= 0)
                prodPenalty = rolloverPenalty;

            // look at all of the defending house's PP pools
            // until we find something to take.
            boolean foundComponents = false;
            for (int type = Unit.ASSAULT; type >= Unit.LIGHT && !foundComponents; type--) {
                if (defendH.getPP(type, Unit.MEK) > 0) {

                    // check to see if the house really
                    int actualPP = defendH.getPP(type, Unit.MEK);
                    if (actualPP < prodPenalty)
                        prodPenalty = actualPP;

                    // move the components
                    loserHSUpdates.append(defendH.addPP(type, Unit.MEK, -prodPenalty, false));
                    winnerHSUpdates.append(attackH.addPP(type, Unit.MEK, prodPenalty, false));

                    // set up the string
                    if (hasLoss)
                        toMain += ", " + prodPenalty + " " + Unit.getWeightClassDesc(type) + " Mek components";
                    else {
                        toMain += " " + prodPenalty + " " + Unit.getWeightClassDesc(type) + " Mek components";
                        hasLoss = true;
                    }

                    // set foundComponents, so we don't take lighter things
                    foundComponents = true;
                }
            }// end for(mech wieght classes)
        }// end prodPenalty

        // send the announcement to everyone, if there was actual loss.
        if (hasLoss)
            CampaignMain.cm.doSendToAllOnlinePlayers(toMain, true);

        // check to see if updates should be transmitted
        if (winnerHSUpdates.length() > 0)
            CampaignMain.cm.doSendToAllOnlinePlayers(attackH, "HS|" + winnerHSUpdates.toString(), false);
        if (loserHSUpdates.length() > 0)
            CampaignMain.cm.doSendToAllOnlinePlayers(defendH, "HS|" + loserHSUpdates.toString(), false);

        // and add the info to the log
        MWLogger.gameLog("Leech: " + this.opID + "/" + pdefender.getName() + "<br> Player saw: " + toPlayer + "<br> Main saw: " + toMain);
    }

    @Override
    public synchronized void run() {

        CampaignMain.cm.toUser(generateAttackLinks(), pdefender.getName(), true);

        // Send Attack Event so Client can play a sound
        CampaignMain.cm.toUser(generateAttackDialogCall(), pdefender.getName(), false);

        // message is extraneous now. null it.
        message = null;

        /*
         * Chicken loop. shouldContinue is set to false when a player makes an
         * external action which should prevent chickening:
         *  - joins this game - becomes invovled in another RUNNING game -
         * deactivates
         * 
         * Deactivating will pull the players chicken threads from the manager,
         * looping through to stop them. It will also punish the player for
         * leeching, calling the public doPenalty() in order to give a
         * punishment equal to that delivered for a timeout.
         */
        while (true) {

            // wait for the proscribed amount of time
            try {
                this.wait(waittime * 1000);// time given in seconds
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }

            // if the stop signal was sent while we were
            // waiting, return and end the run();
            if (!shouldContinue)
                return;

            if (pdefender.leechCount > CampaignMain.cm.getOpsManager().getOperation(opName).getIntValue("LeechesToDeactivate")) {
            	// Some other thing pushed this over the edge.  Don't do anything but return.
            	return;
            }
            
            // FFA ops start once the leech is done.
            if (CampaignMain.cm.getOpsManager().getOperation(opName).getBooleanValue("FreeForAllOperation")) {

                ShortOperation parentOp = CampaignMain.cm.getOpsManager().getRunningOps().get(opID);
                // get the latest copy of the operation
                Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);

                int minPlayers = 3;
                try {
                    minPlayers = o.getIntValue("MinNumberOfPlayers");
                } catch (Exception ex) {
                }

                if (parentOp.getDefenders().size() + parentOp.getAttackers().size() > minPlayers)
                    parentOp.changeStatus(ShortOperation.STATUS_INPROGRESS);
                else
                    CampaignMain.cm.getOpsManager().terminateOperation(parentOp, OperationManager.TERM_NOPOSSIBLEDEFENDERS, null);
                return;
            }
            // not stopped. add a leech.
            pdefender.leechCount++;

            /*
             * If we've reached the leach limit, deactivate the player. The
             * deactivation process will, itself, cause a leech penalty, so we
             * don't actually need to do one.
             * 
             * Otherwise, penalize the player normally.
             * 
             * SPlayer is reloaded as currP in case the person quit and
             * rejoined, creating a new SPlayer instance.
             */
            if (pdefender.leechCount > CampaignMain.cm.getOpsManager().getOperation(opName).getIntValue("LeechesToDeactivate")) {
                shouldContinue = false;
                SPlayer currP = CampaignMain.cm.getPlayer(pdefender.getName());
                currP.setActive(false);
                currP.leechCount = 0;
                CampaignMain.cm.sendPlayerStatusUpdate(currP, !Boolean.parseBoolean(CampaignMain.cm.getConfig("HideActiveStatus")));
                CampaignMain.cm.toUser("You've been deactivated!", currP.getName(), true);
                return;
            }

            this.doPenalty();

        }// end the never ending while loop.
    }// end run()

    public String generateAttackLinks() {
        /*
         * Setup. Inform the player of the outstanding attack and send him links
         * to join for defense.
         */
        Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);
        boolean hasAnArmy = false;// used to set up string for armies past the
                                    // first
        StringBuilder toSend = new StringBuilder(message + "<br>You may defend with: ");
        int numberOfTeams = Math.max(2, Math.min(8, o.getIntValue("NumberOfTeams")));

        for (SArmy currArmy : parmies) {
            int aID = currArmy.getID();
            int aBV = currArmy.getOperationsBV(null);
            int aUnits = currArmy.getAmountOfUnits();

            if (hasAnArmy)
                toSend.append(", ");

            // Team Operations that are faction specific get teams assigned in
            // the defendCommand
            if (o.getBooleanValue("TeamOperation") && !o.getBooleanValue("TeamsMustBeSameFaction") && !o.getBooleanValue("RandomTeamDetermination")) {

                for (int teamNumber = 1; teamNumber <= numberOfTeams; teamNumber++)
                    toSend.append("<a href=\"MEKWARS/c defend#" + opID + "#" + aID + "#" + teamNumber + "\"> Team #" + teamNumber + " Army #" + aID + "</a> (Units: " + aUnits + " / BV: " + aBV + ")");
            } else {
                toSend.append("<a href=\"MEKWARS/c defend#" + opID + "#" + aID + "#" + -1 + "\">Army #" + aID + "</a> (Units: " + aUnits + " / BV: " + aBV + ")");
            }

            hasAnArmy = true;
        }
        return toSend.toString();
    }

    public String generateAttackDialogCall() {
        String opString = "CC|AT|" + opID;
        StringBuilder defendingArmyList = new StringBuilder();
        Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);
        int numberOfTeams = -1;
        if (o.getBooleanValue("TeamOperation")) {
            numberOfTeams = Math.max(2, Math.min(8, o.getIntValue("NumberOfTeams")));
            if (o.getBooleanValue("RandomTeamDetermination"))
                numberOfTeams = -1;
        }
        for (SArmy currArmy : parmies) {

            int aID = currArmy.getID();
            defendingArmyList.append("|" + aID);
        }

        return opString + "|" + numberOfTeams + defendingArmyList.toString();
    }
}// end OpsChickenThread class
