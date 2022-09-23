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
 * A utility which checks to see whether armies are valid for
 * particular op types @ launch/join.
 */
package server.campaign.operations;

//IMPORTS
import java.util.ArrayList;
import java.util.Iterator;

import common.campaign.operations.Operation;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.operations.newopmanager.I_OperationManager;
//TODO: remove "unused" once longs are running
public class LongValidator {
	
	//IVARS
	//backreference to manager which owns this object
	private I_OperationManager manager;
	
	//public static int MODE_ATTACKER = 0;
	//public static int MODE_DEFENDER = 1;
	//long presumes attack!
	
	//these failures are immediate (reserved, through 100)
	public static final int LFAILS_UNAUTHORIZED = 0;
	public static final int LFAILS_ALREADYTARGETTED = 1;
	public static final int LFAILS_HOUSEMAXED = 2;
	
	//these failures accumulate
	public static final int LFAILS_RANGE 			= 101;
	public static final int LFAILS_HOUSEMONEY 		= 102;
	public static final int LFAILS_HOUSEACTIONS	= 103;
	
	public static final int LFAILS_PLAYERMONEY 	= 104;
	public static final int LFAILS_PLAYERREWARD 	= 105;
	public static final int LFAILS_PLAYERFLU 		= 106;
	public static final int LFAILS_PLAYEREXP 		= 107;
	
	//CONSTRUCTORS
	public LongValidator(OperationManager m) {
		manager = m;
	}

	//METHODS
	
	/**
	 * Method which checks a players army vs. an operation
	 * type to see if the army can be used to attack/defend.
	 * 
	 * Returns a vector of failure codes.
	 */
	public ArrayList<Integer> validateLongOp(SPlayer p, SHouse ah, SPlanet pl, Operation o) {
		
		/*
		 * Failure list. Elsewhere in code you'd probably see a Vector for
		 * this sort of thing; however, there's no need to have a synchronized
		 * structure here. Replacing vectors, where possible, with ArrayLists
		 * will be an ongoing performance improvement thrust ...
		 */
		ArrayList<Integer> failList = new ArrayList<Integer>();
		
		
		//first, make sure the player has the authority to start long ops.
		//NOTE: Commented out until ops are ready, at which point hooks
		//      into SHouse/SPlayer will be written.
		//if (!h.canStartHouseOperations(p)) {
		//	failList.add(new Integer(LFAILS_UNAUTHORIZED));
		//	return failList;
		//}
		
		//next, make sure that this will be the only op on planet
		//NOTE: Commented out until ops are ready, at which point hooks
		//      into SHouse/SPlayer will be written.
		if (manager.hasLongOnPlanet(ah, pl)) {
			failList.add(LFAILS_ALREADYTARGETTED);
			return failList;
		}
		
		//next, check to make sure that the faction hasn't hit its op cap
		//NOTE: Commented out until ops are ready, at which point hooks
		//      into SHouse/SPlayer will be written.
		//if (ah.getLongOpCount() >= ah.getOpCeiling()) {
		//	failList.add(new Integer(LFAILS_HOUSEMAXED));
		//	return failList;
		//}
		
		/*
		 * Passed the instant-failure portion of the validation. Now,
		 * load necessary paramaters from the operation and check vs.
		 * faction/player launch requirements (money, etc).
		 */
		int requiredMoney = o.getIntValue("LHouseLaunchMoney");
		//int requiredActions = o.getIntValue("LHouseLaunchActions");
		
		int reqPlayerMoney = o.getIntValue("LPlayerLaunchMoney");
		int reqPlayerRP = o.getIntValue("LPlayerLaunchReward");
		int reqPlayerFlu = o.getIntValue("LPlayerLaunchFlu");
		int reqPlayerXP = o.getIntValue("LPlayerLaunchExp");

		if (requiredMoney > ah.getMoney())
			failList.add(LFAILS_HOUSEMONEY);
		//if (requiredActions > ah.getActions())
			//failList.add(new Integer(LFAILS_HOUSEACTIONS));
		
		if (reqPlayerMoney > p.getMoney())
			failList.add(LFAILS_PLAYERMONEY);
		if (reqPlayerFlu > p.getInfluence())
			failList.add(LFAILS_PLAYERFLU);
		if (reqPlayerRP > p.getReward())
			failList.add(LFAILS_PLAYERREWARD);
		if (reqPlayerXP > p.getExperience())
			failList.add(LFAILS_PLAYEREXP);
		
		return failList;
	}
	
	/**
	 * Method which converts Integer failure list into
	 * a human readible failure string.
	 */
	public String failuresToString(ArrayList<Integer> failList) {
	
		String s = "Launch failed ";
		if (failList == null)
			return s += " for reasons unknown ...";
		
		if (failList.size() == 1)
			return s += " because " + this.decodeFailure((Integer)failList.get(0)) + ".";
		
		s += "because:<br>";
		Iterator<Integer> i = failList.iterator();
		while (i.hasNext()) {
			s += "- " + this.decodeFailure(i.next());
			if (i.hasNext())
				s += "<br>";
		}
		
		return "";
	}
	
	/**
	 * Method (private) which converts a given failure
	 * code into a string. Helper for this.failuresToString
	 */
	private String decodeFailure(Integer code) {
		
		//make an int
		int decoded = code.intValue();
		
		//run a switch statement
		switch (decoded) {
			case LFAILS_ALREADYTARGETTED: 	return "the target planet is already under attack";
			case LFAILS_HOUSEACTIONS:		return "your faction has too few actions remaining";
			case LFAILS_HOUSEMAXED:			return "your faction has too many running operations";
			case LFAILS_HOUSEMONEY:			return "your faction has insufficient funds";
			case LFAILS_PLAYEREXP:			return "you do not have enough experience";
			case LFAILS_PLAYERFLU:			return "you do not have enough influence";
			case LFAILS_PLAYERMONEY:		return "your funds are insufficient";
			case LFAILS_PLAYERREWARD:		return "you do not have enough " + CampaignMain.cm.getConfig("RPShortName");
			case LFAILS_RANGE:				return "the target planet is out of range";
			case LFAILS_UNAUTHORIZED:		return "you are not able to initiate assaults";
		}
		
		//switch failed
		return "for reasons unknown ... (report error to admin/operator!)";
	}
	
}//end OperationsManager class