/*
 * MekWars - Copyright (C) 2018
 * 
 * Original author - Bob Eldred (spork@mekwars.org)  
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

package hpgnet;

import java.util.Vector;

public class HPGPurgeThread extends Thread {

	HPGNet tracker;
	
	
	/**
	 * Waits.  Because that's what threads do
	 * @param time
	 */
	private void extendedWait(long time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
			tracker.addToLog(e);
		}
	}
	
	/**
	 * The thread in charge of deleting expired tracker entries
	 * @param tracker
	 */
	public HPGPurgeThread(HPGNet tracker) {
		this.tracker = tracker;
	}
	
	/**
	 * Start running the purge thread
	 */
	public synchronized void run() {

		/*
		 * Pause the thread for 10 minutes initially - this is the default period between
		 * updates from servers and will allow for servers to get reconnected just in case
		 */
		this.extendedWait(600000);
		
		if (tracker.isBusy())
			this.extendedWait(90000);
		
		/*
		 * Begin blocking
		 */
		tracker.getPurgingThreads().add(this);
		
		Vector<HPGSubscriber> toDelete = new Vector<HPGSubscriber>();
		
		for(HPGSubscriber sub : tracker.getSubscribers()) {
			sub.calculateThreatLevel();
			if(sub.getThreatLevel() == HPGSubscriber.THREAT_LEVEL_PURGE) {
				toDelete.add(sub);
			}
		}
		
		if (toDelete.size() > 0) {
			for(HPGSubscriber sub : toDelete) {
				tracker.delete(sub);
			}
		}
		
		/*
		 * Unblock and sleep for 12 hours
		 */
		tracker.getPurgingThreads().remove(this);
		
		int hours = Integer.parseInt(tracker.getConfig().getProperty("purgefrequency", "12"));
		int waittime = hours * 60 * 60 * 1000;
		extendedWait(waittime);
	}
}
