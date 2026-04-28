package mekwars.common.campaign.clientutils.protocol;

import java.io.PrintStream;
import java.util.Vector;

import mekwars.common.util.MWLogger;

/**
 * Write the messages in the queue to the socket's output stream
 */
public class WriterThread extends Thread {
    private final Vector<String> outgoingMessages;
    private final PrintStream _out;
    private boolean keepGoing = true;

    WriterThread(PrintStream out) {
        super("ConnectionHandler$WriterThread");
        _out = out;
        outgoingMessages = new Vector<>(1, 1);
    }

    @Override
    public synchronized void run() {
        try {
            while (keepGoing) {
                flushOutputQueue();
                // wait until there are more messages
                wait(1000);
            }
            MWLogger.errLog("WriterThread: stopping gracefully.");
        } catch (InterruptedException e) {
            MWLogger.errLog("ConnectionHandlerLocal$WriterThread.run(): Interrupted!");
        }
    }

    void flushOutputQueue() {
        while (!outgoingMessages.isEmpty()) {
            String message = outgoingMessages.elementAt(0);
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
