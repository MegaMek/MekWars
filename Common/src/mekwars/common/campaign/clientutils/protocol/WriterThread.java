package mekwars.common.campaign.clientutils.protocol;

/**
 * Write the messages in the queue to the socket's output stream
 */
class WriterThread extends Thread {
    private boolean keepGoing = true;
    private java.util.Vector<String> outgoingMessages;
    private java.io.PrintStream _out;

    WriterThread(java.io.PrintStream out) {
        super("ConnectionHandler$WriterThread");
        _out = out;
        outgoingMessages = new java.util.Vector<String>(1, 1);
    }

    @Override
    public synchronized void run() {
        //        MMClient.mwClientLog.clientErrLog("ConnectionHandlerLocal$WriterThread: running");
        try {
            while (keepGoing) {
                flushOutputQueue();
                // wait until there are more messages
                wait(1000);
            }
            common.util.MWLogger.errLog("WriterThread: stopping gracefully.");
        } catch (InterruptedException e) {
            common.util.MWLogger.errLog("ConnectionHandlerLocal$WriterThread.run(): Interrupted!");
        }
    }

    void flushOutputQueue() {
        while (outgoingMessages.size() > 0) {
            String message = (String) outgoingMessages.elementAt(0);
            outgoingMessages.removeElementAt(0);
            mekwars.common.campaign.clientutils.protocol.ConnectionHandlerLocal.DEBUG("> " + message);
            _out.print(message + "\r\n");
        }
        _out.flush();
    }


    synchronized void queueMessage(String s) {
        outgoingMessages.addElement(s);
        // notify the writer thread that there is at least one new message
        notify();
    }

    void pleaseStop() {
        keepGoing = false;
    }

}
