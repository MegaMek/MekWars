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

package server.campaign;


import java.io.FileWriter;
import java.io.IOException;

import common.util.MWLogger;
import server.campaign.util.Statistics;

public class TickThread extends Thread {
	
	server.campaign.CampaignMain myCampaign;
	private long until;
	private int Duration = 900000;
	private int tickid = 0;
	
	public TickThread(server.campaign.CampaignMain main, int Duration) {
	    super("Tick Thread");
		this.Duration = Duration;
		myCampaign = main;
	}
	
	public int getTickID() {
		return tickid;
	}
	
	public void extendedWait(int time) {
		until = System.currentTimeMillis() + time;
		try {
			this.wait(time);
		} catch (Exception ex) {
			MWLogger.errLog(ex);
		}
		
	}
	
	public long getRemainingSleepTime() {
		return Math.max(0, until - System.currentTimeMillis());
	}
	
	@Override
	public synchronized void run() {
		try {
			while (true) {
				
				this.extendedWait(Duration);  //15 mins by default
				
				tickid++;
				MWLogger.tickLog("Tick (" + tickid + ") Started");
				
				try {
					myCampaign.tick(true,tickid);
				} catch (Exception ex) {
					MWLogger.errLog(ex);
					myCampaign.doSendToAllOnlinePlayers("Tick skipped. Errors occured", true);
				}
				
				if (true) {
					try {
						myCampaign.toFile();
					} catch (Exception ex) {
						myCampaign.doSendToAllOnlinePlayers("Warning! AutoSave failed!", true);
					}
				}
				
				if (this.tickid % 8 == 0) {
					this.myCampaign.addToNewsFeed("Faction Rankings", "Server News", Statistics.getReadableHouseRanking(false));
					if(CampaignMain.cm.getBooleanConfig("DiscordEnable")) {
						CampaignMain.cm.postToDiscord(Statistics.getReadableHouseRanking(false));
					}
					
					try {
						FileWriter out = new FileWriter(myCampaign.getConfig("HouseRankPath"), true); // opened in APPEND mode; will be controlled by config setting
						out.write(Statistics.getReadableHouseRanking(false)); // dump actual SHouse Ranking data to a permanent file
						out.write("\n");
						out.close();
					} catch (IOException e) {
						MWLogger.errLog(e);
					}
				}
				
				MWLogger.tickLog("Tick (" + tickid + ") Finished");
				myCampaign.doSendToAllOnlinePlayers("CC|NT|" + this.Duration + "|" + true,false);
			}
		}
		catch (Exception ex) {
			MWLogger.errLog(ex);
		}
	}
}