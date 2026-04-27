package mekwars.client;

class CRefreshGUI implements Runnable {
    private final MWClient mwClient;
    protected int mode = -1;

    public CRefreshGUI(MWClient mwClient, int tmode) {
        this.mwClient = mwClient;
        mode = tmode;
    }

    @Override
    public void run() {
        if (mwClient.MainFrame == null) {
            return;
        }// return if main frame not yet drawn (still fetching data)
        try {
            switch (mode) {
                case MWClient.REFRESH_USERLIST:
                    mwClient.MainFrame.getMainPanel().getUserListPanel().refresh();
                    break;
                case REFRESH_PLAYERPANEL:
                    mwClient.MainFrame.getMainPanel().getPlayerPanel().refresh();
                    break;
                case REFRESH_BATTLETABLE:
                    mwClient.MainFrame.refreshBattleTable();
                    break;
                case REFRESH_HQPANEL:
                    mwClient.MainFrame.getMainPanel().getHQPanel().refresh();
                    break;
                case MWClient.REFRESH_STATUS:
                    mwClient.MainFrame.changeStatus(mwClient.Status, mwClient.LastStatus);
                    break;
                case REFRESH_BMPANEL:
                    mwClient.MainFrame.getMainPanel().getBMPanel().refresh();
                    break;
            }
        } catch (Exception ex) {
            // do nothing
        }
    }
}
