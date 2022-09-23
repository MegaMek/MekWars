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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;


/**
	 * Thread which processes information transmitted to the
	 * tracker by MekWarsServers. Servers send simple strings which note the
	 * type of information to follow with a header, and use %'s as delimits.
	 * 
	 * Types are as follows:
	 * 1) ServerStart - SS% header. Sent when a server boots. Contains vital
	 *                  information like server name and link to homepage.
	 * 2) UpdateInfo  - Overwrites select information from SS%, including the
	 *                  description and link. Server name may not be changed.
	 *                  Uses UI% as lead.
	 * 3) PhoneHome   - Most common command. Sent by server on a schedule set
	 *                  by a server operator (eg - every 10 minutes). Contains
	 *                  information re: number of players currently online, the
	 *                  number of running/recently completed games, and more.
	 *                  Uses PH% as lead.
	 */
	public class HPGProcessingThread extends Thread {
		
		//VARIABLES
		Socket sock;
		HPGNet tracker;
		
		//CONSTRUCTOR
		public HPGProcessingThread(Socket s, HPGNet t) {
			sock = s;
			tracker = t;
			tracker.addToLog("- new processing thread");
		}
		
		@Override
		public synchronized void run() {
			
			String line = "";
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				line = reader.readLine();
				reader.close();
				sock.close();
				tracker.addToLog("- line read, socket closed.");
			} catch (Exception e) {
				tracker.addToLog("- line read failed. terminating this processingthread.");
				return;//kill this processing attempt if cant read line
			}
			
			/*
			 * Line was read and stored, and socket is closed. Ensure
			 * that writing to the record now would not interfere with a
			 * purge or page generation by pausing until neither operation
			 * is in progress, then process the line.
			 */
			while (tracker.isGeneratingHTML() || tracker.isPurging()) {
				try {
					tracker.addToLog("- processing blocked. wait 5 seconds.");
					this.wait(5000);//5 seconds
				} catch (Exception e) {
					//do nothing on interrupt
				}
			}
			
			/*
			 * Now that we're going to start processing the line, add
			 * to the vector in order to block other operations. Note that
			 * multiple processing threads can be run simultanously as they
			 * will be writing to different files.
			 */
			//add this thread to the processing vector
			tracker.getProcessingThreads().add(this);
			tracker.addToLog("- being processing line. block other threads (" + tracker.getProcessingThreads().size() + " record(s) being processed)");
			tracker.addToLog(line);
			
			//break out if there's no actual text in line
			if (line == null || line.trim().equals("")) {
				tracker.addToLog("- empty line. stop processing. unblock.");
				tracker.getProcessingThreads().remove(this);
				return;
			}
			
			//Create tokenizer and draw header
			StringTokenizer st = new StringTokenizer(line, "%");
			String lead = st.nextToken();
			
			/*
			 * If the header is SS%, this is a server starting up. Delete
			 * any old info file and replace with these values.
			 * 
			 * This indicates a legacy system.  Not all the functionality will 
			 * be available
			 */
			if (lead.equals("SS")) {
				String name = st.nextToken();
				HPGSubscriber sub = tracker.getSubscriber(name);
				
				boolean isNew = false;
				
				if(sub == null) {
					sub = new HPGSubscriber();
					sub.setTracker(tracker);
					isNew = true;
				}
				
				String url = st.nextToken();
				String version = st.nextToken();
				String desc = st.nextToken();
				
				sub.setName(name);
				sub.setUrl(url);
				sub.setMWVersion(version);
				sub.setDescription(desc);
				sub.setLegacy(true);
				sub.update(0, 0, 0);

				if(isNew) {
					tracker.registerNewSubscriber(sub);
				}
				
				sub.setLastUpdated(new Date());
				sub.calculateThreatLevel();
				sub.generateHTMLString();
				tracker.save(sub);
				
			}//end "SS"
			
			/*
			 * If the header begins with UI%, update link and
			 * description but leave server name in-tact.
			 * 
			 * Also for legacy servers
			 */
			else if (lead.equals("UI")) {
				String name = st.nextToken();
				String url = st.nextToken();
				String version = st.nextToken();
				String desc = st.nextToken();
				
				tracker.addToLog("Updating " + name);

				HPGSubscriber sub = tracker.getSubscriber(name);
				
				boolean isNew = false;
				
				if(sub == null) {
					sub = new HPGSubscriber();
					sub.setTracker(tracker);
					isNew = true;
				}
				
				sub.setName(name);
				sub.setUrl(url);
				sub.setMWVersion(version);
				sub.setDescription(desc);
				sub.setLegacy(true);
				
				if(isNew) {
					sub.update(0, 0, 0);
					tracker.registerNewSubscriber(sub);
				}
				
				sub.setLastUpdated(new Date());
				tracker.save(sub);
			}//end "UI" 
			
			/*
			 * Phone Home - update the players, etc
			 * 
			 * This is for legacy systems
			 */
			else if (lead.equals("PH")) {
				String name = st.nextToken();
				tracker.addToLog("PH% from " + name);
				String numPlayers = st.nextToken();
				String numGames = st.nextToken();
				String completedGames = st.nextToken();
				
				HPGSubscriber sub = tracker.getSubscriber(name);
				
				boolean isNew = false;
				
				if(sub == null) {
					sub = new HPGSubscriber();
					sub.setTracker(tracker);
					isNew = true;
				}
				
				sub.setName(name);
				sub.setLegacy(true);
				//sub.setCompletedGames(Integer.parseInt(completedGames));
				
				if(isNew) {
					sub.update(0, 0, 0);
					tracker.registerNewSubscriber(sub);
				} else {
					sub.update(Integer.parseInt(numPlayers), Integer.parseInt(numGames), Integer.parseInt(completedGames));
				}
				
				sub.setLastUpdated(new Date());
				tracker.save(sub);
			}//end "PH" 
			
			// Things have been updated, let's generate some HTML.  I don't see a need to 
			// wait on a schedule
			tracker.generateHTML();
			
			//processing is finished. remove from vec then return.
			tracker.getProcessingThreads().remove(this);
			return;
			
		}//end run()
	}//end ProcessingThread