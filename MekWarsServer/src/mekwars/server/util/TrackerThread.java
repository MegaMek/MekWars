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

package mekwars.server.util;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

import megamek.logging.MMLogger;
import mekwars.server.MWServ;
import mekwars.server.campaign.CampaignMain;
import org.jspecify.annotations.NonNull;

/**
 *
 * @author urgru
 * @author Torren
 * @version 2.0
 *       <p>
 *       A thread which communicates with the tracker.
 *       <p>
 *       ChangeLog: Updated to use CampaignConfig instead of ServerConfig, so we don't have to reboot to affect change.
 *       *
 */
public class TrackerThread extends Thread {
    private final static MMLogger LOGGER = MMLogger.create(TrackerThread.class);

    //VARIABLE
    MWServ serv;
    private UUID trackerID = null;

    public TrackerThread(MWServ mwServ) {
        super("Tracker Thread");
        serv = mwServ;

        if (trackerID == null) {
            // Tracker ID isn't loaded yet
            String savedUUIDString = serv.getCampaign().getConfig("TrackerUUID");
            try {
                trackerID = UUID.fromString(savedUUIDString);
            } catch (Exception e) {
                // The server does not yet set the tracker ID. Let mwServ set it so this never happens again
                trackerID = UUID.randomUUID();
                serv.getCampaign().getConfig().setProperty("TrackerUUID", String.valueOf(trackerID));
                serv.saveConfigs();
            }
        }

        LOGGER.info("Created TrackerThread");
    }

    //METHODS

    @Override
    public synchronized void run() {

        LOGGER.info("TrackerThread running.");

        //core info
        String name = serv.getCampaign().getConfig("ServerName");
        String link = serv.getCampaign().getConfig("TrackerLink");
        String desc = serv.getCampaign().getConfig("TrackerDesc");

        //ip of tracker
        //String trackerAddress = serv.getConfigParam("TRACKERADDRESS");
        String trackerAddress = serv.getCampaign().getConfig("TrackerAddress");
        LOGGER.info(STR."\{name} \{link} \{desc} \{trackerAddress}");

        /*
         * Immediately send core info to tracker.
         */
        Socket sock = null;

        try {
            LOGGER.info("TrackerThread attempting to send ServerStart information.");
            sock = new Socket(trackerAddress, 13731);//fixed port
            PrintWriter printWriter = new PrintWriter(sock.getOutputStream());

            printWriter.println(STR."SS%\{name}%\{link}%\{MWServ.SERVER_VERSION}%\{desc}");
            printWriter.flush();
            printWriter.close();
            LOGGER.info("TrackerThread sent server start information.");
        } catch (Exception e) {
            LOGGER.error(e, "Could not contact tracker. Shutting down TrackerThread.");
            return;
        } finally {
            try {
                if (sock != null && !sock.isClosed()) {
                    sock.close();
                }
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }

        /*
         * socket is closed server side after this
         * line is read from a buffered stream.
         */

        /*
         * now that the first run is done, start a forever-long
         * phone home loop which updates tracker with player
         * counts, game counts, etc. every 10 minutes.
         */
        try {
            while (true) {

                //10-minute wait between updates

                try {
                    Thread.sleep(600000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }

                //set up substrings
                //name - already saved from ServerStart
                int playersOnline = serv.userCount(false);
                String toSend = getToSend(name, playersOnline);

                //set up a socket to the tracker and send this record
                try {
                    LOGGER.info("TrackerThread attempting to send PhoneHome information.");
                    sock = new Socket(trackerAddress, 13731);//fixed port
                    PrintWriter printWriter = new PrintWriter(sock.getOutputStream());

                    printWriter.println(toSend);
                    printWriter.flush();
                    printWriter.close();
                    LOGGER.info("TrackerThread sent PH% information.");
                } catch (Exception e) {
                    LOGGER.error(e, "Could not contact tracker.");
                }
            }//end (forever)
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }

    private @NonNull String getToSend(String name, int playersOnline) {
        int gamesInProgress = 0;
        int gamesCompleted = 0;

        //get campaign main. if not null, get game info.
        CampaignMain campaign = serv.getCampaign();
        if (campaign != null) {
            gamesInProgress = campaign.getOpsManager().getRunningOps().size();
            gamesCompleted = campaign.getGamesCompleted();
            campaign.setGamesCompleted(0);
        }

        //string to send to tracker
        return STR."PH%\{name}%\{playersOnline}%\{gamesInProgress}%\{gamesCompleted}";
    }
}
