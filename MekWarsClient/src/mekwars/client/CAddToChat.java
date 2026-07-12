package mekwars.client;

class CAddToChat implements Runnable {
    private final MWClient mwClient;
    String input = "";
    int channel = -1;
    String tabName = "";

    public CAddToChat(MWClient mwClient, String tinput, int tchannel, String ttabName) {
        this.mwClient = mwClient;
        input = tinput;
        channel = tchannel;
        tabName = ttabName;
    }

    @Override
    public void run() {
        (mwClient.MainFrame.getMainPanel().getCommPanel()).setChat(input, channel,
              tabName);
    }
    //@salient discord bot chat capture
    //if i wanted to capture multiple channels
    //i'd have to do it here?
}
