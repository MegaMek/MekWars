/*
 * MekWars - Copyright (C) 2006
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

package mekwars.server.campaign;


import java.util.ArrayList;
import java.util.TreeMap;

import megamek.logging.MMLogger;
import mekwars.server.campaign.util.OpponentListHelper;

/**
 * @author urgru
 *       <p>
 *       Private thread. Simple loop that keeps track of player's immunity. All methods that modify the immunePlayers
 *       TreeMap lock the map in order to prevent concurrent modification errors.
 */
public final class ImmunityThread extends Thread {//no extension

    //VARIABLES
    private final static MMLogger LOGGER = MMLogger.create(ImmunityThread.class);
    private final TreeMap<String, Long> immunePlayers;

    //CONSTRUCTOR
    public ImmunityThread() {
        super("Immunity Thread");
        this.immunePlayers = new TreeMap<>();
    }

    //METHODS

    /**
     * Method that adds a newly immune player to the Thread. The player is informed of his immunity. If he's a newbie,
     * he is also told if/how to reset his units.
     *
     * @param sPlayer - player who is now immune.
     */
    public void addImmunePlayer(SPlayer sPlayer) {

        //get name and immunity duration
        String lowerCaseName = sPlayer.getName().toLowerCase();
        int immunitySeconds = CampaignMain.campaignMain.getIntegerConfig("ImmunityTime");

        //if immunity is disabled, just ignore and return.
        if (immunitySeconds < 1) {
            return;
        }

        //inform player. also, if newbie, tell him about potential unit resets
        CampaignMain.campaignMain.toUser(STR."You are immune to attack for \{immunitySeconds} seconds. [<a href=\"MEKWARS/c deactivate\">Deactivate</a>]",
              sPlayer.getName(),
              true);
        if (sPlayer.getMyHouse().isNewbieHouse()) {
            int numResets = CampaignMain.campaignMain.getIntegerConfig("NumResetsWhileImmune");
            if (numResets > 0) {
                //set the resets
                NewbieHouse pHouse = (NewbieHouse) sPlayer.getMyHouse();
                pHouse.addResetPlayer(sPlayer, numResets);

                //and inform the lucky player
                String toSend = "You may reset your units ";

                if (numResets == 1) {
                    toSend += " once";
                } else {
                    toSend += STR."\{numResets} times";
                }

                CampaignMain.campaignMain.toUser(STR."\{toSend} while immune by selecting \"Reset Units\" in the HQ. You may only reset while in reserve.",
                      sPlayer.getName(),
                      true);

            }
        }

        //put name and end-time (in millis) in immune players hash. SYNCED!
        synchronized (immunePlayers) {
            this.immunePlayers.put(lowerCaseName, System.currentTimeMillis() + (immunitySeconds * 1000L));
        }

    }

    /**
     * Check to see if a player is immune.
     */
    public boolean isImmune(SPlayer sPlayer) {
        return immunePlayers.containsKey(sPlayer.getName().toLowerCase());
    }

    /**
     * Remove a player from the Immunity list. This is called when logging in or activating, but should NEVER be called
     * when deactivating. If deactivated players were removed using this call, SOL would not be able to reset units.
     */
    public void removeImmunity(SPlayer sPlayer) {
        synchronized (immunePlayers) {
            immunePlayers.remove(sPlayer.getName().toLowerCase());
        }
        if (sPlayer.getMyHouse().isNewbieHouse()) {
            NewbieHouse pHouse = (NewbieHouse) sPlayer.getMyHouse();
            pHouse.removeResetPlayer(sPlayer);
        }
    }

    /**
     * Override Thread's run() method to do what we want - periodically wake and poll the immune players hash, bumping
     * anyone whose time has expired back to standard status. Runs forever.
     */
    @Override
    public synchronized void run() {
        while (true) {
            //poll very 5 seconds
            this.extendedWait(5000);

            synchronized (immunePlayers) {

                //keep track of players to be removed
                ArrayList<String> toRemove = new ArrayList<>();

                //loop through all players
                for (String currName : immunePlayers.keySet()) {
                    //if the player's immunity has not expired, continue
                    long startTime = System.currentTimeMillis();
                    if (startTime < immunePlayers.get(currName)) {
                        continue;
                    }

                    //immunity expired. add to a removal list
                    toRemove.add(currName);
                }

                //remove and inform expired players
                for (String currName : toRemove) {
                    //load the player
                    SPlayer player = CampaignMain.campaignMain.getPlayer(currName);

                    if (player == null) {
                        synchronized (immunePlayers) {
                            immunePlayers.remove(currName);
                        }

                        continue;
                    }

                    /*
                     * make sure the player is still active (hasnt logged out, deactivated (voluntary
                     * of otherwise) and subsequently re-activated, attacked or joined a game.
                     */
                    if (player.getDutyStatus() == SPlayer.STATUS_ACTIVE) {
                        //tell the player
                        CampaignMain.campaignMain.toUser("[!] Your post-game immunity expired!",
                              player.getName(),
                              true);

                        //alert other players
                        OpponentListHelper opponentListHelper = new OpponentListHelper(player,
                              OpponentListHelper.MODE_ADD);
                        opponentListHelper.sendInfoToOpponents(
                              " finished an R&R cycle and returned to the front. You may attack it with ");

                        //newbie player handling. if he has resets left, tell him time has expired. always remove from the list.
                        if (player.getMyHouse().isNewbieHouse()) {
                            NewbieHouse currH = (NewbieHouse) player.getMyHouse();
                            if (currH.getResetsRemaining(player) > 0) {
                                CampaignMain.campaignMain.toUser("[!] Your post-game reset time expired!",
                                      player.getName(),
                                      true);
                            }

                            currH.removeResetPlayer(player);
                        }
                    }

                    synchronized (immunePlayers) {
                        immunePlayers.remove(currName);
                    }
                }

            }//end sync lock on immunePlayers

        }
    }// end run()

    /**
     * Method that waits the Immunity thread and prints any interruptions to the error log.
     */
    public void extendedWait(int time) {
        try {
            this.wait(time);
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }

}// end ImmunityThread
