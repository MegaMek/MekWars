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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import common.util.MWLogger;

/**
 * HPGListenerThread listens for connections from MekWars servers, makes
 * the connections and hands them off to HPGProcessingThreads to deal with
 * 
 * @author Spork
 * @author Torren
 * @author urgru
 *
 */
public class HPGListenerThread extends Thread {
	//VARIABLES
			HPGNet hpgnet;
			
			//CONSTRUCTOR
			public HPGListenerThread(HPGNet t) {
				hpgnet = t;
			}
			
			@Override
			public synchronized void run() {
				
				hpgnet.addToLog("listener thread running!");
				
				/*
				 * Attempt to open a socket, then wait for
				 * incoming calls. Sys-exit if the server
				 * socket cannot be initialized.
				 * 
				 * As calls come in, spin them into separate
				 * processing threads which handle communications
				 * over discrete sockets.
				 */
				ServerSocket server = null;
				int listenPort = hpgnet.getPort();
				
				try {
					hpgnet.addToLog("attempt to open a serversocket on port " + listenPort);
					server = new ServerSocket(listenPort,0,InetAddress.getLocalHost());
					while(true){//always listen for calls
						Socket sock = server.accept();
						hpgnet.addToLog("lthread: connection accepted from " + sock.getInetAddress() + ", being processing.");
						new HPGProcessingThread(sock, hpgnet).start();
					}
				} catch (IOException e) {
					hpgnet.addToLog("Server socket creation failed. Exiting.");
					System.exit(0);
				} finally {
					try {
						server.close();
					} catch (IOException e) {
						MWLogger.errLog(e);
					}
				}
				
			}//end run()    
}
