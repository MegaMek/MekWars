/*
 * MekWars - Copyright (C) 2005 
 * 
 * Original author - nmorris (urgru@users.sourceforge.net)
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
 * OperationManager stores all of a server's running Operations. A
 * single manager instance is created in CampaignMain on startup.
 * 
 * The Manager keeps track of Long and Short operations in seperate
 * lists. On ticks, the Manager iterates thorugh long Ops and stores
 * necessary persistance variables. Short ops are NOT saved.
 * 
 * Additionally, the Manager maintains a list of outstanding/unreported
 * games (disconnects). Although the manager doesn't time these directly,
 * there is a lookback to this list in the threads which track disconnect
 * times and triger resolves.
 * 
 * Upon creation, the Manager attempts to read the /data/operations/ sub-
 * directories (long, short, modifiers), via an OperationLoader. Once maps
 * of operations and map of modifying operations have been created, they are
 * used as for lookup and given to Validators and Resolvers as params, along
 * with specific instances of ShortOperations and LongOperations.
 */

package server.campaign.operations;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;

import common.campaign.operations.ModifyingOperation;
import common.campaign.operations.Operation;
import common.util.MWLogger;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.operations.newopmanager.AbstractOperationManager;
import server.campaign.operations.newopmanager.I_OperationManager;


public class OperationManager extends AbstractOperationManager implements I_OperationManager {
	

	
	/**
	 * Construction of the Manager is keystone event for
	 * Operations system. Construction triggers attempts to
	 * load Ops and ModOps.
	 */
	public OperationManager() {
		
		//construct utils
		opLoader = new OperationLoader();
		opWriter = new OperationWriter();
		shortResolver  = new ShortResolver();
		//longResolver   = new LongResolver();
		shortValidator = new ShortValidator(this);
		//longValidator  = new LongValidator(this);
		
		//construct local maps
		ops  = new TreeMap<String, Operation>();
		mods = new TreeMap<String, ModifyingOperation>();
		
		//maps for running/pending ops
		runningOperations = new TreeMap<Integer, ShortOperation>();
		activeLongOps = new TreeMap<Integer, LongOperation>();	
		
		//disconnection handling maps
		disconnectionThreads = new TreeMap<String, OpsDisconnectionThread>();
		scrapThreads = new TreeMap<String, OpsScrapThread>();
		disconnectionTimestamps = new TreeMap<String, Long>();
		disconnectionDurations = new TreeMap<String, Long>();
		
		loadOperations();
		
	}//end constructor

	//METHODS
	/**
	 * Method which checks to see whether a player who logged out
	 * (quit client, crashed, etc) was involved in a game. If so,
	 * a DisconnectionThread is started and a logout timestamp is
	 * set.
	 * 
	 * If both players are disconnected, kill game.
	 */
	public void doDisconnectCheckOnPlayer(String name) {
		
		//see if the player is real
		SPlayer p = CampaignMain.cm.getPlayer(name);
		if (p == null)
			return;
		
		//see if the player is in a game
		ShortOperation so = this.getShortOpForPlayer(p);
		if (so == null)
			return;
		
		//only have a possible resolution if game was in progress
		if (so.getStatus() != ShortOperation.STATUS_INPROGRESS)
			return;
		
		//if the operation has more than 2 players, return
		if (so.getAllPlayerNames().size() > 2)
			return;
		
		//determine opposing player's name
		String otherName = "";
		for (String currName : so.getAllPlayerNames()) {
			if (!currName.equalsIgnoreCase(p.getName()))
				otherName = currName;
		}
		
		//check to see if the player has disconnected recently
		Long timeOffline = disconnectionDurations.get(p.getName().toLowerCase());
		if (timeOffline == null)
			timeOffline = (long)0;
		
		//if the other player is also disconnected, simply terminate the game
		OpsDisconnectionThread otherThread = disconnectionThreads.get(otherName.toLowerCase());
		if (otherThread != null && !otherThread.playerHasReturned()) {
			this.terminateOperation(so, I_OperationManager.TERM_NO_REMAINING_PLAYERS, null);
			return;
		}
		
		//set up the new DisconnectionThread and save the exit time
		long logoutTime = System.currentTimeMillis();
		OpsDisconnectionThread discoThread = new OpsDisconnectionThread(so.getShortID(), otherName, p.getName(), timeOffline);
		
        OpsDisconnectionThread oldDiscoThread = disconnectionThreads.put(p.getName().toLowerCase(), discoThread);
        
        /* check for an old thread in the queue. 
         * if so set the player has returned var, if it hasn't already been sets.
         * Issues with old threads getting kicked out of the TreeMap 
         * but not being stopped.
         *  --Torren 
         */
        if ( oldDiscoThread != null && !oldDiscoThread.playerHasReturned() )
            oldDiscoThread.playerReturned(false,timeOffline);
        
		disconnectionTimestamps.put(p.getName().toLowerCase(), logoutTime);
		
		//start the clock!
		discoThread.start();
	}
	
	/**
	 * Method which checks to see whether a reconnecting player should
	 * have a DisconnectionThread halted and ShortOp SPlayer/Sarmy
	 * references refreshed.
	 */
	public void doReconnectCheckOnPlayer(String name) {
		
		//see if the player is real
		SPlayer p = CampaignMain.cm.getPlayer(name);
		if (p == null)
			return;
		
		//check to see if the player has a pending disco
		OpsDisconnectionThread discoT = disconnectionThreads.get(p.getName().toLowerCase());
		if (discoT == null){
            
            //Matches with more then 2 players do not get disconnection threads
            //So check the player for those kinda ops. 
            if ( CampaignMain.cm.getOpsManager().getShortOpForPlayer(p) != null ){
                ShortOperation so = CampaignMain.cm.getOpsManager().getShortOpForPlayer(p);
                if (so.getStatus() == ShortOperation.STATUS_REPORTING || so.getStatus() == ShortOperation.STATUS_FINISHED)
                    return;
                
                //resend appropriate data to the player and update SO's references.
                so.sendReconnectInfoToPlayer(p);
            }
            return;
        }
		
		//determine time offline
		long currTime = System.currentTimeMillis();
		long exitStamp = disconnectionTimestamps.get(p.getName().toLowerCase());
		long discoDuration = currTime - exitStamp;
		
		//prevent the thread from triggering a report
		discoT.playerReturned(true, discoDuration);
		
		//load the short operation in question. check for nullness and finished status
		ShortOperation so = this.runningOperations.get(discoT.getShortID());
		if (so == null)
			return;
		if (so.getStatus() == ShortOperation.STATUS_REPORTING || so.getStatus() == ShortOperation.STATUS_FINISHED)
			return;
		
		//adjust the duration by the grace period (expressed in seconds in config)
		long gracePeriod = Long.parseLong(CampaignMain.cm.getConfig("DisconnectionGracePeriod")) * 1000;
		discoDuration -= gracePeriod;
		if (discoDuration < 0)
			discoDuration = 0;
		
		//put the duration in  the tree
		disconnectionDurations.put(p.getName().toLowerCase(), discoDuration);
		
		//resend appropriate data to the player and update SO's references.
		so.sendReconnectInfoToPlayer(p);
	}
	
	/**
	 * Method which clears all references to a player in the
	 * disconenctionTimestamp and disconnectionDuration Trees.
	 * 
	 * ShortResolver should call for all players when a game
	 * finishes, and this class should call for all players
	 * whenver terminating a game.
	 */
	public void clearAllDisconnectionTracks(ShortOperation so) {
		for (String currN : so.getAllPlayerNames()) {
			String lowerName = currN.toLowerCase();
			OpsDisconnectionThread discoThread = disconnectionThreads.get(lowerName);
			if (discoThread != null) {
				discoThread.playerReturned(false, 0);
				disconnectionThreads.remove(lowerName);
			}
			disconnectionDurations.remove(lowerName);
			disconnectionTimestamps.remove(lowerName);
		}//end foreach (Player in game)
	}//end clearAllDisconnectionTracks
	
	/**
	 * Method which fetches an Operation (paramater
	 * collection) from the ops TreeMap.
	 */
	public Operation getOperation(String name) {
		return ops.get(name);
	}
	
	public TreeMap<String, Operation> getOperations(){
		return ops;
	}
	
	private ModifyingOperation getModifyingOperation(String name) {
		return mods.get(name);
	}
	
	/**
	 * Method which returns a complete set of running
	 * operations. Used by /c ops (nea /c operations)
	 */
	public TreeMap<Integer, ShortOperation> getRunningOps() {
		return runningOperations;
	}
	
	/**
	 * Method which passes information from AttackCommand
	 * to the ShortValidator and returns any failures.
	 * 
	 * If the attacker passes the check, the Validator will
	 * automatically check active armies from the aa's OLH
	 * for matches. Only if a defender is available will the
	 * ShortValidator construct a ShortOperation.
	 * 
	 * The ShortOperation's chickenThreads contain all prechecked
	 * SArmies and SPlayers, and the DefendCommand will let these
	 * SPlayers/SArmies join automatically.
	 * 
	 * In short - validateShortAttack takes care of 90% of the
	 * work necessary to get an Attack running. The analagous
	 * defender call need only be used for SPlayers who activated
	 * after validateShortAttack was called ...
	 */
	public String validateShortAttack(SPlayer ap, SArmy aa, Operation o, SPlanet target, int longID, boolean joiningAttack) {
		ArrayList<Integer> failures = this.shortValidator.validateShortAttacker(ap, aa, o, target, longID, joiningAttack);
		if (failures.size() > 0)
			return this.shortValidator.failuresToString(failures);

		return null;
	}
	
	/**
	 * Method which clears a defender to participate in a game. Should
	 * only be called from DefendCommand, and even then only when an
	 * Army or Player has not been pre-cleared to participate in a game.
	 * 
	 * See validateShortAttack() for a detailed explaination of how/why
	 * this works.
	 */
	public String validateShortDefense(SPlayer dp, SArmy da, Operation o,SPlanet target) {
		ArrayList<Integer> failures = this.shortValidator.validateShortDefender(dp, da, o,target);
		if (failures.size() > 0)
			return this.shortValidator.failuresToString(failures);
		return null;
	}
	
	/** 
	 * Conduit method. Takes ShortOperation, Operation, and a report
	 * String from CampaignMain and sends them to the ShortResolver.
	 */
	public void resolveShortAttack(Operation o, ShortOperation so, String report) {
		synchronized (this.shortResolver) {
			this.shortResolver.resolveShortAttack(o, so, report);
		}
	}
	
	/**
	 * Conduit method. Takes ShortOperation, Operation, a winner and a
	 * loser from a DisconnectionThread and sends them to ShortResolver
	 */
	public void resolveShortAttack(Operation o, ShortOperation so, String winnerName, String loserName) {
		synchronized (this.shortResolver) {
			this.shortResolver.resolveShortAttack(o, so, winnerName, loserName);
		}
	}
	
	/**
	 * Method which adds a new short operation to this.runningOperations.
	 * 
	 * SPlayer ap and Operation o are sent for convenience. They could be
	 * derived from the ShortOperation with ease, but they're already in
	 * the validator, so ...
	 */
	public void addShortOperation(ShortOperation so, SPlayer ap, Operation o) {
		
		//nullcheck, just in case.
		if (so == null) {
			MWLogger.errLog("Error: Tried to add a null ShortOperation to the Manager");
			return;
		}
		
		//add to the list. Validator got a free shortID from the
		//manager right before adding this.
		this.runningOperations.put(so.getShortID(), so);
		
		//if there are costs, charge the attacker
		int money = o.getIntValue("AttackerCostMoney");
		int flu = o.getIntValue("AttackerCostInfluence");
		int rp = o.getIntValue("AttackerCostReward");
		
		String toSend = "You are on your way to " + so.getTargetWorld().getName() + " (" + o.getName();
		if (money > 0) {
			ap.addMoney(-money);
			toSend += ", " + CampaignMain.cm.moneyOrFluMessage(true, true, -money, true);
		}
		if (flu > 0) {
			ap.addInfluence(-flu);
			toSend += ", " + CampaignMain.cm.moneyOrFluMessage(false, true, -flu, true);
		}
		if (rp > 0) {
			ap.addReward(-rp);
			toSend += ", -" + rp + " " + CampaignMain.cm.getConfig("RPShortName");
		}
		toSend += ").";
		
		//tell the attacker that his attack has begun
		CampaignMain.cm.toUser(toSend,ap.getName(),true);
	}
	
	/**
	 * Method which terminates an operation. This returns all
	 * player cost, stops all chicken threads, returns all
	 * players to ACTIVE status if the so was in RUNNING mode.
	 * 
	 * It is not possible to terminate reporting or finished
	 * ShortOperations.
	 * 
	 * It is also not possible to auto-terminate any Operation
	 * which is running without the voluntary fail code (ie -
	 * if the termination is not initiated by a user via the
	 * TerminateCommand or ModTerminateCommand).
	 * 
	 * @param so         - short operation to terminate
	 * @param termCode   - code to terminate with. determines
	 *                     which message is sent to players.
	 * @param terminator - player who is trying to terminate
	 *                     the operation. not used in all cases.
	 *                     
	 * @param ignoreStatus - use true to terminate reporting and
	 *                       finished games. should only be passed
	 *                       from /c hardterminate.
	 */
	public void terminateOperation(ShortOperation so, int termCode, SPlayer terminator, boolean ignoreStatus) {
		
		if (so == null) {
			MWLogger.errLog("Attempted to terminate null ShortOperation");
			return;
		}
		
		if (!ignoreStatus) {
			//do not cancel games which are in the process of reporting
			if (so.getStatus() == ShortOperation.STATUS_REPORTING)
				return;
			
			//do not cancel games which have finalized their reports
			if (so.getStatus() == ShortOperation.STATUS_FINISHED)
				return;
			
			//do not cancel running games involuntarily, unless *EVERY* player involved is gone
			if (so.getStatus() == ShortOperation.STATUS_INPROGRESS && (termCode != TERM_TERMCOMMAND && termCode != TERM_NO_REMAINING_PLAYERS))
				return;
		}

		//assemble message header
		String message = "Attack #" + so.getShortID() + " was cancelled";
		switch (termCode) {
		
			case TERM_TERMCOMMAND:
				message += " by " + terminator.getName();
				break;
				
			case TERM_NOPOSSIBLEDEFENDERS:
				message += " because no potential defenders remained";
				break;
				
			case TERM_NOATTACKERS:
				message += " because no attacking players remained";
				break;
				
			case TERM_REPORTINGERROR:
				message += " because there was a reporting error";
				break;
				
			case TERM_NO_REMAINING_PLAYERS:
				message += " because all involved players disconnected";
				break;
		}
		
		//finish the strings and send them to their users.
		Operation o = this.getOperation(so.getName());
		
		int attmoney = o.getIntValue("AttackerCostMoney");
		int attflu = o.getIntValue("AttackerCostInfluence");
		int attrp = o.getIntValue("AttackerCostReward");
		
		int defmoney = o.getIntValue("DefenderCostMoney");
		int defflu = o.getIntValue("DefenderCostInfluence");
		int defrp = o.getIntValue("DefenderCostReward");
		
		for (String currName : so.getAttackers().keySet()) {
			SPlayer currP = CampaignMain.cm.getPlayer(currName);
			boolean didReturn = false;
			String toPlayer = message;
			if (attmoney > 0) {
				didReturn = true;
				currP.addMoney(attmoney);
				toPlayer += " (+" + CampaignMain.cm.moneyOrFluMessage(true, true, attmoney);
			}
			if (attflu > 0) {
				currP.addInfluence(attflu);
				if (!didReturn)
					toPlayer += " (+" + CampaignMain.cm.moneyOrFluMessage(false, true, attflu);
				else 
					toPlayer += ", +" + CampaignMain.cm.moneyOrFluMessage(false, true, attflu);
                didReturn = true;
			}
			if (attrp > 0) {
				currP.addReward(attrp);
				if (!didReturn) 
					toPlayer += " (+" + attrp + " " + CampaignMain.cm.getConfig("RPShortName");
				else 
					toPlayer += ", +" + attrp + " " + CampaignMain.cm.getConfig("RPShortName");
					didReturn = true;
			}
			if (didReturn)
				toPlayer += ").";
			else
				toPlayer += ".";
			
			CampaignMain.cm.toUser(toPlayer,currName,true);
			CampaignMain.cm.toUser("PL|STN|"+-1, toPlayer,false);
		}
		
		for (String currName : so.getDefenders().keySet()) {
			SPlayer currP = CampaignMain.cm.getPlayer(currName);
			boolean didReturn = false;
			String toPlayer = message;
			if (defmoney > 0) {
				didReturn = true;
				currP.addMoney(defmoney);
				toPlayer += "(+" + CampaignMain.cm.moneyOrFluMessage(true, true, defmoney);
			}
			if (defflu > 0) {
				currP.addInfluence(defflu);
				if (!didReturn)
					toPlayer += "(+" + CampaignMain.cm.moneyOrFluMessage(false, true, defflu);
				else {
					toPlayer += ", +" + CampaignMain.cm.moneyOrFluMessage(false, true, defflu);
					didReturn = true;
				}
			}
			if (defrp > 0) {
				currP.addReward(defrp);
				if (!didReturn) {
					toPlayer += "(+" + defrp + " " + CampaignMain.cm.getConfig("RPShortName");
					didReturn = true;
				}
				else {
					toPlayer += ", +" + defrp + " " + CampaignMain.cm.getConfig("RPShortName");
					didReturn = true;
				}
			}
			if (didReturn)
				toPlayer += ").";
			else
				toPlayer += ".";
			
            if ( o.getBooleanValue("AttackerUnitsTakenBeforeFightStarts") ){
                try{
                    SHouse faction = CampaignMain.cm.getHouseForPlayer(so.getDefenders().firstKey());
                    for(SUnit unit : so.preCapturedUnits )
                        faction.addUnit(unit, true);
                }catch(Exception ex){
                    MWLogger.errLog(ex);
                }
                    
            }
            
			CampaignMain.cm.toUser(toPlayer,currName,true);
			CampaignMain.cm.toUser("PL|STN|"+-1, toPlayer,false);
		}
		
		/*
		 * All armies should be unlocked.
		 */
		TreeMap<String,Integer> allArmies = so.getAllPlayersAndArmies();
		for (String currN : allArmies.keySet()) {
            try{
    			CampaignMain.cm.getPlayer(currN).lockArmy(-1);
            }catch(Exception ex){
                MWLogger.errLog(currN+" had a null army while terminating. Continuing to next player.");
                continue;
            }
		}
		
		/*
		 * Players from in-progress games ought be returned to their previous status. AFR
		 * players should go reserve, standard game players should be made Active.
		 */
		if (so.getStatus() == ShortOperation.STATUS_INPROGRESS) {
			
			for (String currN : so.getAllPlayerNames()) {
                
				/*
				 * If the player has a disconnection thread outstanding, stop it (with a false
				 * playerReturned) and continue the loop. The various timestamps and the thread
				 * itself will be cleared later by clearAllDiscoThreads(). We don't want to put
				 * an offline player in active status.
				 */
				OpsDisconnectionThread dt = disconnectionThreads.get(currN.toLowerCase());
				if (dt != null && !dt.playerHasReturned()) {
					dt.playerReturned(false, 0);//duplicative, but ensures the thread won't penalize in the millis before clear().
					continue;
				}
				
				SPlayer currP = CampaignMain.cm.getPlayer(currN);
				CampaignMain.cm.getIThread().removeImmunity(currP);//ensure player is not in immunity tree.
				
				//If AFR, return to reserve. Else, standard switch to activated.
				if (so.isFromReserve())
					currP.setFighting(false, true);
				else
					currP.setFighting(false);
    			
				//all players should see a cancellation
				CampaignMain.cm.sendPlayerStatusUpdate(currP,true);

			}
		}
		
		/*
		 * Disconnect information related to the game should be
		 * cleared, and all chicken threads should be stopped.
		 */
		this.clearAllDisconnectionTracks(so);
		so.terminateChickenThreads();
		
		//finally, completely nuke the short op
		this.runningOperations.remove(so.getShortID());
		so = null;//faster gc.
		
	}//end terminateOperation
	
	/**
	 * Method which passes Terminates w/o an ignore
	 * boolean into the full termination method.
	 */
	public void terminateOperation(ShortOperation so, int termCode, SPlayer terminator) {
		this.terminateOperation(so, termCode, terminator, false);
	}
	
	//public void terminateOperation(LongOperation lo) {
	//	//ADD CONTENTS!
	//}
	
	/**
	 * Method which returns the ID of a specific long
	 * operation. It is assumed that hasLongOnPlanet
	 * or hasSpecificLongOnPlanet will precede this in
	 * order to ensure a useful ID value.
	 */
	public int getLongID(SHouse h, SPlanet p) {
	
		Iterator<LongOperation> i = activeLongOps.values().iterator();
		while (i.hasNext()) {
			LongOperation currL = i.next();
			SHouse attacker = currL.getAttackingHouse();
			SPlanet planet = currL.getTargetWorld();
			if (attacker.equals(h) && planet.equals(p))
				return currL.getID();
		}
		return -1;
	}
	
	/**
	 * Method which checks whether there is a long
	 * operation, from any faction, running on a planet.
	 */
	private boolean isLongOnPlanet(SPlanet p) {
		Iterator<LongOperation> i = activeLongOps.values().iterator();
		while (i.hasNext()) {
			LongOperation currL = i.next();
			SPlanet planet = currL.getTargetWorld();
			if (planet.equals(p))
				return true;
		}
		return false;
	}
	
	/**
	 * Method which checks to see if a Faction has
	 * a long operation running on a given world.
	 */
	public boolean hasLongOnPlanet(SHouse h, SPlanet p) {
			
		Iterator<LongOperation> i = activeLongOps.values().iterator();
		while (i.hasNext()) {
			LongOperation currL = i.next();
			SHouse attacker = currL.getAttackingHouse();
			SPlanet planet = currL.getTargetWorld();
			if (attacker.equals(h) && planet.equals(p))
				return true;
		}
		return false;
	}
	
	/**
	 * Method which checks to see if a faction has a long
	 * operation of a specific type running on a given
	 * world. This is similar to hasLongOnPlanet, but has
	 * different uses.
	 */
	public boolean hasSpecificLongOnPlanet(SHouse h, SPlanet p, Operation o) {
		
		Iterator<LongOperation> i = activeLongOps.values().iterator();
		while (i.hasNext()) {
			LongOperation currL = i.next();
			SHouse attacker = currL.getAttackingHouse();
			SPlanet planet = currL.getTargetWorld();
			//String opName = currL.getName();
			if (attacker.equals(h) && planet.equals(p) && currL.getName().equals(o.getName()))
				return true;
		}
		return false;
	}
	
	/**
	 * Method which checks to see if a Faction has
	 * a short operation running on a given world.
	 */
	private boolean hasShortOnPlanet(SHouse h, SPlanet p) {
			
		Iterator<ShortOperation> i = runningOperations.values().iterator();
		while (i.hasNext()) {
			ShortOperation currS = i.next();
			SPlayer anAttacker = CampaignMain.cm.getPlayer(currS.getAttackers().firstKey());
			SHouse attacker = anAttacker.getHouseFightingFor();
			SPlanet planet = currS.getTargetWorld();
			if (attacker.equals(h) && planet.equals(p))
				return true;
		}
		return false;
	}
	
	/**
	 * Method which returns a chicken thread, if one
	 * is present, for the given player/operation.
	 * 
	 * Can be done directly from the ShortOperation since
	 * the ID is already known, but this is shorter than
	 * loading the ShortOperation if only the Thread is
	 * needed.
	 */
	private OpsChickenThread getPlayerThreadForAttack(String pname, int opID) {
		TreeMap<String, OpsChickenThread> threads = this.getRunningOps().get(opID).getChickenThreads();
		return threads.get(pname.toLowerCase());
	}
	
	/**
	 * Method which removes a player from every attack's
	 * possibleDefender lists and turns off all chicken
	 * threads which are still running.
	 * 
	 * If removing this player leaves no possible defenders,
	 * the attack should be terminated. This will usually happen
	 * when a player deactivates or joins another game.
	 */
	public void removePlayerFromAllPossibleDefenderLists(String playerName, boolean penalize) {
		
		//remove the defender from all possible ops. if no players
		//remain, save in a toTerminate list. Removing immediatley
		//would cause a ConcurrentModification exception.
		ArrayList<ShortOperation> toTerminate = new ArrayList<ShortOperation>();
		for (ShortOperation so : this.getRunningOps().values()) {
        	so.removePossibleDefender(playerName, penalize);
        	if (so.getChickenThreads().size() == 0)
        		toTerminate.add(so);
		}
		
		//terminate any operations which may no longer be defended.
		for (ShortOperation so : toTerminate)
			this.terminateOperation(so, TERM_NOPOSSIBLEDEFENDERS, null);
		
	}
	
	/**
	 * Method which removes a player from the attacker lists of all
	 * operations, except the given operation. If removing the
	 * player from an attacker lists means there are no attackers left,
	 * cancel the game and return all influence, etc.
	 */
	public void removePlayerFromAllAttackerLists(SPlayer p, ShortOperation so, boolean verbose) {
		
		ArrayList<ShortOperation> toTerminate = new ArrayList<ShortOperation>();
		for (ShortOperation currO : this.getRunningOps().values()) {
			
			//only remove people from WAITING games
			if (currO.getStatus() != ShortOperation.STATUS_WAITING)
				continue;
			
			//move to next loop if the player isn't part of the game
			if(!currO.hasPlayer(p))
				continue;
			
			/*
			 * If the short op is null, the player is simply
			 * leaving. He's logged out or deactivated. If the
			 * game is in waiting status, we remove the player.
			 */
			if (so == null) {
				currO.removeAttacker(p); 
				if (verbose) {
					for (String currN : currO.getAllPlayerNames())
						CampaignMain.cm.toUser(p.getName() + " left Attack #" + currO.getShortID() + " (deactivated or logged out).",currN, true);
				}
				if (currO.getAttackers().size() == 0)
					toTerminate.add(currO);
			} 
			
			/*
			 * If leaving to join another game, tell the other
			 * player(s) which Game was joined instead.
			 */
			else if (currO != so) {//can assume so != null
				
				currO.removeAttacker(p); 
				if (verbose) {
					for (String currN : currO.getAllPlayerNames())
						CampaignMain.cm.toUser(p.getName() + " left the game for Attack #" + so.getShortID() + ".",currN, true);
				}
				if (currO.getAttackers().size() == 0)
					toTerminate.add(currO);
			}
				
		}//end for(each running operation)
		
		//terminate any empty games
		for (ShortOperation ott : toTerminate)
			this.terminateOperation(ott, TERM_NOATTACKERS, null);
		
	}//end removePlayerFromAllAttackerLists
	
	/**
	 * Method which removes a player from the defender lists of all
	 * operations, except the given operation. Used to remove player
	 * from a set of multiplayer games when another game is changed
	 * from "waiting" to "inprogress" status.
	 */
	public void removePlayerFromAllDefenderLists(SPlayer p, ShortOperation so, boolean verbose) {
		
		for (ShortOperation currO : this.getRunningOps().values()) {
			
			//only remove people from WAITING games
			if (currO.getStatus() != ShortOperation.STATUS_WAITING)
				continue;
			
			//move to next loop if the player isn't part of the game
			if(!currO.hasPlayer(p))
				continue;
				
			/*
			 * If the short op is null, the player is simply
			 * leaving. He's logged out or deactivated. If the
			 * game is in waiting status, we remove the player.
			 */
			if (so == null && currO.getStatus() == ShortOperation.STATUS_WAITING) {
				
				currO.removeDefender(p); 
				if (verbose) {
					for (String currN : currO.getAllPlayerNames())
						CampaignMain.cm.toUser(p.getName() + " left Attack #" + currO.getShortID() + " (deactivated or logged out).",currN, true);
				}
			}
			
			/*
			 * If leaving to join another game, tell the other player(s) which
			 * Game was joined instead. Unlike attacks, its perfectly fine for a
			 * defender list to have no entries, so we don't check and terminate.
			 */
			else if (so != null && currO != so) {
				
				currO.removeDefender(p); 
				if (verbose) {
					for (String currN : currO.getAllPlayerNames())
						CampaignMain.cm.toUser(p.getName() + " left the game for Attack #" + so.getShortID() + ".",currN, true);
				}
			}
			
		}//end foreach(runningop)
	}//end removePlayerFromAllDefenderLists
	
	/**
	 * Method which is called from CampaignMain on ticks. Cleans
	 * up resolved operations (short and long) and prepares a
	 * display string. This also releases the op numbers for 
	 * future use.
	 */
	public String tick() {
		
		TreeMap<Long,ShortOperation> timeSort = new TreeMap<Long,ShortOperation>();
		String toReturn = "<b><i>Recently Completed Games:</i><br>";
		
		Iterator<ShortOperation> i = this.runningOperations.values().iterator();
		while (i.hasNext()) {
			ShortOperation currO = i.next();
			if (currO.getStatus() == ShortOperation.STATUS_FINISHED) {
				timeSort.put(currO.getCompletionTime(),currO);
				if (currO.decrementShowsToClear())//decrement, and remove if warranted
					i.remove();
			}
		}//end while (more short ops)
		
		//clean up scrap threads
		Iterator<OpsScrapThread> i2 = this.scrapThreads.values().iterator();
		while (i2.hasNext()) {
			if (i2.next().isFinished())
				i2.remove();
		}
		
		//write info, in time order.
		boolean completeInfo = CampaignMain.cm.getBooleanConfig("ShowCompleteGameInfoOnTick");
		if (timeSort.size() == 0)
			return "";

		for (ShortOperation currO : timeSort.values())
			toReturn += currO.getInfo(completeInfo,false) + "<br>";
		

		toReturn += "</b>";
		timeSort.clear();//make sure ops are purged.
		return toReturn;
	}//end .tick()
	
	/**
	 * Method which checks whether or not a player is
	 * registered as an attacker. Used by AttackCommand
	 * to ensure that a player does not multilaunch.
	 */
	public int playerIsAnAttacker(SPlayer p) {
		for (ShortOperation currO : this.getRunningOps().values()) {
			if (currO.getAttackers().get(p.getName().toLowerCase()) != null && currO.getStatus() != ShortOperation.STATUS_FINISHED)
				return currO.getShortID();
		}
		return -1;
	}
	
	/**
	 * Method which checks whether or not a player is
	 * registered as a defender.
	 */
	public int playerIsADefender(SPlayer p) {
		for (ShortOperation currO : this.getRunningOps().values()) {
			if (currO.getDefenders().get(p.getName().toLowerCase()) != null && currO.getStatus() != ShortOperation.STATUS_FINISHED)
				return currO.getShortID();
		}
		return -1;
	}
	
	/**
	 * Method which returns the ShortOperation
	 * in which a player is participating.
	 */
	public ShortOperation getShortOpForPlayer(SPlayer p) {
		for (ShortOperation currSO : this.getRunningOps().values()) {
			if (currSO.hasPlayer(p) && currSO.getStatus() != ShortOperation.STATUS_FINISHED)
				return currSO;
		}
		return null;
	}
	
	/**
	 * Method which checks to see if a player has an active ChickenThread. Whenever a thread is
	 * stopped w/ stopChicken() it is also removed from the ShortOperation's tree, so we should
	 * only check for nulls and may ignore the thread's shouldContinue boolean.
	 * 
	 * Used by ShortValidator to determine whether or not a player is under attack and to stop
	 * counterattacks if they're not allowed by a given operation.
	 */
	public boolean playerHasActiveChickenThread(SPlayer p) {
		for (ShortOperation currSO : this.getRunningOps().values()) {
			if (currSO.getChickenThreads().containsKey(p.getName().toLowerCase()))
				return true;
		}
		return false;
	}
	
	/**
	 * Method which returns th scrap threads. Used by ShortResolvers
	 * to add threads and by ScrapCommand to determine whether scraps
	 * should stipper payments to player or by player.
	 */
	public TreeMap<String, OpsScrapThread> getScrapThreads() {
		return scrapThreads;
	}
	
	/**
	 * Method which determines the first available ID for
	 * a new ShortOperation.
	 */
	public int getFreeShortID() {
		int i = 0;
		while (true) {
			if (runningOperations.get(i) == null) return i;
			i++;
		}
	}
	
	/**
	 * Method which determines the first available ID
	 * for a new LongOperation
	 */
	public int getFreeLongID() {
		int i = 0;
		while (true) {
			if (activeLongOps.get(i) == null) return i;
			i++;
		}
	}
	
	/**
	 * Method which returns the ShortValidator. Should be used
	 * only by the OpponentListHelper's constructor. Hacky.
	 *
	 * SArmies legalOperations also need to be checked with a
	 * short validator, checkOperations() should be piped through
	 * this class and into the validator.
	 */
	public ShortValidator getShortValidator() {
		return shortValidator;
	}
	
	/**
	 * Conduit method.
	 */
	public void checkOperations(SArmy a, boolean display) {
		shortValidator.checkOperations(a, display, ops);
	}

	public void loadOperations(){
		/*
		 * Check for the operations directories.
		 * If they're missing create them. 
		 */
		File shortDir = new File("./data/operations/short/");
		File longDir = new File("./data/operations/long/");
		File modDir = new File("./data/operations/modifiers/");
		try {
			if (!shortDir.exists())
				shortDir.mkdirs();
			if (!longDir.exists())
				longDir.mkdir();
			if (!modDir.exists())
				modDir.mkdir();
		} catch (Exception e) {
			MWLogger.errLog("Error while creating operations directories.");
		}
		
		ops.clear();
		mods.clear();
		MULOnlyArmiesOpsLoad = false;
		
		/*
		 * read the shortoperation's subdir and do loads. since every
		 * long has a corresponding short, its possible to do loads
		 * via the short names only (loader handles this properly)
		 */
		String[] shortNames = shortDir.list();
		for (int i = 0; i < shortNames.length; i++) {
			Operation currOp = opLoader.loadOpValues(shortNames[i]);
			ops.put(currOp.getName(), currOp);
			if ( currOp.getBooleanValue("MULArmiesOnly") ) {
			    MULOnlyArmiesOpsLoad = true;
			}
		}
		
		/*
		 * read the mod operations subdir and do loads. add the mods to
		 * target ops' modmaps as the loads occur. Throw error if, for some
		 * reason, a given target cannot be found.
		 */
		String[] modNames = modDir.list();
		for (int i = 0; i < modNames.length; i++) {
			ModifyingOperation currMod = opLoader.loadModOpValues(modNames[i]);
			mods.put(currMod.getName(), currMod);
			
			/*
			 * mod loaded. now, try to put it into standard op's trees.
			 * targets are a string w/ ; as deliminter. trim to remove leading
			 * and trailing spaces.
			 */
			String targets = currMod.getValueAsString("LinkedOperations");
			StringTokenizer st = new StringTokenizer(targets, ";");
			while (st.hasMoreTokens()) {
				String currTarget = st.nextToken().trim();
				Operation currOp = ops.get(currTarget);
				if (currOp == null)
					MWLogger.errLog("Error assigning modop target. Mod: " + currMod.getName() + " Target: " + currTarget);
				else
					currOp.addModifyingOperation(currMod);
			}//end while(more targets)
		}//end modOp loading
		
		/*
		 * Now that all Ops are loaded, write out the crib sheet for clients.
		 */
		opWriter.writeOpList(ops);

	}
	
	/**
	 * Lets us know if MUL only ops have been loaded
	 * This effects the status of commands like activate
	 * attack and defend.
	 * @return
	 */
	public boolean hasMULOnlyOps() {
	    return MULOnlyArmiesOpsLoad;
	}
	
}//end OperationsManager class