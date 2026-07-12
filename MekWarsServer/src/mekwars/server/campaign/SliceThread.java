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

import megamek.logging.MMLogger;

/**
 * @author urgru A barebones timing thread which calls slices in CampaignMain.
 *       <p>
 *       Created in CM as follows: SThread = new SliceThread(this, Integer.parseInt(getConfig("SliceTime")));
 *       SThread.start();//it slices, it dices, it chops!
 */

public class SliceThread extends Thread {
    private final static MMLogger LOGGER = MMLogger.create(SliceThread.class);

    CampaignMain myCampaign;
    long until;
    int duration;
    int sliceID = 0;
    int lastHouseId = 0;

    public SliceThread(CampaignMain main, int Duration) {
        super("sliceThread");
        this.duration = Duration; // set length when thread is spun
        myCampaign = main;
    }

    public long getRemainingSleepTime() {
        return Math.max(0, until - System.currentTimeMillis());
    }

    @Override
    public synchronized void run() {
        try {
            int sleepTime = duration;
            long startTime;

            while (true) {
                this.extendedWait(sleepTime);
                startTime = System.currentTimeMillis();
                sliceID++;
                try {
                    myCampaign.slice(getSliceID());

                    if (CampaignMain.campaignMain.getBooleanConfig("ProcessHouseTicksAtSlice")) {
                        long endTime = startTime + duration / 2;
                        while (endTime > System.currentTimeMillis()) {
                            if (lastHouseId > CampaignMain.campaignMain.getData().getAllHouses().size()) {
                                lastHouseId = 0;
                            }
                            SHouse house = CampaignMain.campaignMain.getHouseById(lastHouseId);

                            if (house != null && !house.getAllOnlinePlayers().isEmpty()) {
                                CampaignMain.campaignMain.getHouseById(lastHouseId).tick(true, sliceID);
                            }

                            lastHouseId++;
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex, "Process Ticks at Slice: {}", ex.getLocalizedMessage());
                    myCampaign.doSendToAllOnlinePlayers("Slice skipped. Errors occurred", true);
                }
                sleepTime = (int) (duration - (System.currentTimeMillis() - startTime));
                sleepTime = Math.max(100, sleepTime);

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
            LOGGER.error(ex, "Extended Wait: {}", ex.getLocalizedMessage());
        }
    }// end ExtendedWait(time)

    public int getSliceID() {
        return sliceID;
    }
}
