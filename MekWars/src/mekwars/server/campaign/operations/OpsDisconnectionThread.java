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

import common.CampaignData;
import common.campaign.operations.Operation;
import common.util.MWLogger;
import server.campaign.CampaignMain;
import server.util.StringUtil;

public class OpsDisconnectionThread extends Thread {
	
	//VARIABLES
	private boolean playerReturned;
	private int id;
	private String winnerName = "";
	private String loserName = "";
	private long waitReduction;
	
	//CONSTRUCTORS
	public OpsDisconnectionThread(int opID, String w, String l, long waitReduction) {
		playerReturned = false;
		
		if ( l == null || l.equalsIgnoreCase("null") ||
		        w == null || w.equalsIgnoreCase("null") ) {
		    throw new NullPointerException();
		}
		    
		this.winnerName = w;
		this.loserName = l;
		this.id = opID;
		this.waitReduction = waitReduction;
	}
	
	//METHODS
	@Override
	public synchronized void run() {
		
		long timeToReport = Long.parseLong(CampaignMain.cm.getConfig("DisconnectionTimeToReport"));
		
		//make the seconds miliseconds
		timeToReport *= 1000;
		
		//adjust for previous time off-server, w/ a 2 minute minimum
		timeToReport -= waitReduction;
		if (timeToReport < 120000)
			timeToReport = 120000;
		
		//inform the potential "winner" that the game will resolve
		String timeToReturn = StringUtil.readableTimeWithSeconds(timeToReport);
		CampaignMain.cm.toUser(CampaignMain.cm.getPlayer(loserName).getColoredName() + " disconnected. You will win by forfeit if he does not return within " + timeToReturn + ".",winnerName,true);
		
		//add the start to the log
		MWLogger.gameLog("Disco Thread/Start:" + id + "/" + loserName + ". " + winnerName + " wins in " + timeToReturn);
		
		try {
			this.wait(timeToReport);
		} catch (Exception ex) {
			MWLogger.errLog(ex);
		}
		
		//only report if player is still missing
		if (playerReturned)
			return;
			
		//check to see that the op hasnt been cancelled (is still in tree)
		ShortOperation so = CampaignMain.cm.getOpsManager().getRunningOps().get(id);
		if (so == null)
			return;
				
		Operation o = CampaignMain.cm.getOpsManager().getOperation(so.getName());
				
		//add to log and send to resolver
		MWLogger.gameLog("Autoreport: " + id + "/" + loserName + ". " + winnerName + " wins by forfeit");
		CampaignMain.cm.getOpsManager().resolveShortAttack(o, so, winnerName, loserName);
		
	}//end run()
	
	public void playerReturned(boolean tellOtherPlayer, long timeOffline) {
		if (tellOtherPlayer) {
			CampaignMain.cm.toUser(CampaignMain.cm.getPlayer(loserName).getColoredName() + " returned. He was offline for " + StringUtil.readableTimeWithSeconds(timeOffline) + ".",winnerName,true);
			MWLogger.gameLog("Disco Thread/Stop:" + id + "/" + loserName + ". Player returned.");
		} else {
			MWLogger.gameLog("Disco Thread/Stop:" + id + "/" + loserName + ". Player threads cleared.");
		}
		playerReturned = true;
	}
	
	public int getShortID() {
		return id;
	}
	
	/**
	 * Method is used by Operation Manager's
	 * doDisconnectCheckOnPlayer method in
	 * orderto determine whether or not to
	 * cancel game.
	 * 
	 * @return playerReturned
	 */
	public boolean playerHasReturned() {
		return playerReturned;
	}
	
}//end OpsChickenThread class