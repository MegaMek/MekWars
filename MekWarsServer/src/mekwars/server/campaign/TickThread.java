/*
 * Copyright (C) 2004 MekWars
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekwars.server.campaign;

import java.io.FileWriter;
import java.io.IOException;

import megamek.logging.MMLogger;
import mekwars.server.campaign.util.Statistics;

public class TickThread extends Thread {
    private final static MMLogger LOGGER = MMLogger.create(TickThread.class);
    private final int duration;
    CampaignMain myCampaign;
    private long until;
    private int tickID = 0;

    public TickThread(CampaignMain main, int Duration) {
        super("Tick Thread");
        this.duration = Duration;
        myCampaign = main;
    }

    public int getTickID() {
        return tickID;
    }

    public long getRemainingSleepTime() {
        return Math.max(0, until - System.currentTimeMillis());
    }

    @Override
    public synchronized void run() {
        try {
            while (true) {
                this.extendedWait(duration);  //15 mins by default

                tickID++;
                LOGGER.info(STR."Tick (\{tickID}) Started");

                try {
                    myCampaign.tick(true, tickID);
                } catch (Exception ex) {
                    LOGGER.error(ex, "Error during Tick on Campaign: {}", ex.getLocalizedMessage());
                    myCampaign.doSendToAllOnlinePlayers("Tick skipped. Errors occurred", true);
                }

                try {
                    myCampaign.toFile();
                } catch (Exception ex) {
                    myCampaign.doSendToAllOnlinePlayers("Warning! AutoSave failed!", true);
                }

                if (this.tickID % 8 == 0) {
                    this.myCampaign.addToNewsFeed("Faction Rankings",
                          "Server News",
                          Statistics.getReadableHouseRanking(false));
                    if (CampaignMain.campaignMain.getBooleanConfig("DiscordEnable")) {
                        CampaignMain.campaignMain.postToDiscord(Statistics.getReadableHouseRanking(false));
                    }

                    try {
                        FileWriter out = new FileWriter(myCampaign.getConfig("HouseRankPath"),
                              true); // opened in APPEND mode; will be controlled by config setting
                        out.write(Statistics.getReadableHouseRanking(false)); // dump actual SHouse Ranking data to a permanent file
                        out.write("\n");
                        out.close();
                    } catch (IOException e) {
                        LOGGER.error(e, "Unable to write to HouseRankFile. {}", e.getLocalizedMessage());
                    }
                }

                LOGGER.info(STR."Tick (\{tickID}) Finished");
                myCampaign.doSendToAllOnlinePlayers(STR."CC|NT|\{this.duration}|true", false);
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "Error during run: {}", ex.getLocalizedMessage());
        }
    }

    public void extendedWait(int time) {
        until = System.currentTimeMillis() + time;

        try {
            this.wait(time);
        } catch (Exception ex) {
            LOGGER.error(ex, "Error during extended wait: {}", ex.getLocalizedMessage());
        }

    }
}
