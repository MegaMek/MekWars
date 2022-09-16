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

import common.util.MWLogger;

/**
 * @author urgru A barebones timing thread which calls slices in CampaignMain.
 * 
 * Created in CM as follows: SThread = new SliceThread(this,
 * Integer.parseInt(getConfig("SliceTime"))); SThread.start();//it slices, it
 * dices, it chops!
 */

public class SliceThread extends Thread {
    server.campaign.CampaignMain myCampaign;
    long until;
    int Duration;
    int sliceid = 0;
    int lastHouseId = 0;

    public SliceThread(server.campaign.CampaignMain main, int Duration) {
        super("slicethread");
        this.Duration = Duration; // set length when thread is spun
        myCampaign = main;
    }

    public int getSliceID() {
        return sliceid;
    }

    public void extendedWait(int time) {
        until = System.currentTimeMillis() + time;
        try {
            this.wait(time);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }// end ExtendedWait(time)

    public long getRemainingSleepTime() {
        return Math.max(0, until - System.currentTimeMillis());
    }

    @Override
    public synchronized void run() {
        try {
            int sleepTime = Duration;
            long startTime = 0;
            while (true) {
                this.extendedWait(sleepTime);
                startTime = System.currentTimeMillis();
                sliceid++;
                try {
                    myCampaign.slice(getSliceID());

                    if (CampaignMain.cm.getBooleanConfig("ProcessHouseTicksAtSlice")) {
                        long endTime = startTime + Duration/2;
                        while ( endTime > System.currentTimeMillis()) {
                            if ( lastHouseId > CampaignMain.cm.getData().getAllHouses().size() ) {
                                lastHouseId = 0;
                            }
                            SHouse house = CampaignMain.cm.getHouseById(lastHouseId);
                            if ( house != null && house.getAllOnlinePlayers().size() > 0 ) {
                                CampaignMain.cm.getHouseById(lastHouseId).tick(true, sliceid );
                                lastHouseId++;
                            }else {
                                lastHouseId++;
                            }
                        }
                    }
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                    myCampaign.doSendToAllOnlinePlayers("Slice skipped. Errors occured", true);
                }
                sleepTime = (int)(Duration - (System.currentTimeMillis() - startTime));
                sleepTime = Math.max(100, sleepTime);
                
            }
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }
}