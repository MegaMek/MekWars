package mekwars.client;

import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;

class TimeOutThread extends Thread {
    private static final MMLogger LOGGER = MMLogger.create(TimeOutThread.class);

    private final MWClient mwClient;
    MWClient mwclient;

    public TimeOutThread(MWClient mwClient, MWClient client) {
        this.mwClient = mwClient;
        mwclient = client;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(mwClient.TimeOut * 100);
            } catch (Exception ex) {
                LOGGER.error(ex, "");
            }
            if (mwClient.Status != IClient.STATUS_DISCONNECTED) {
                long timeout = (System.currentTimeMillis() / 1000)
                                     - mwClient.LastPing;
                if (timeout > mwClient.TimeOut) {
                    mwClient.systemMessage("Ping timeout (" + timeout + " s)");
                    mwClient.Connector.closeConnection();
                }
            } else {
                mwClient.LastPing = System.currentTimeMillis() / 1000;
            }
        }
    }
}
