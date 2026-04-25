/*
 * Based on code by Lyrisoft Solutions, Inc.
 *
 */
/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - Torren (torren@users.sourceforge.net)
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


/*
 * Derived from NFCChat, a GPL chat client/server.
 * Original code can be found @ http://nfcchat.sourceforge.net
 * Our thanks to the original authors.
 */
/**
 *
 * @author Torren (Jason Tighe) 11.5.05
 *
 */
package common.campaign.clientutils.protocol;

import java.io.IOException;
import java.net.Socket;

import client.gui.SplashWindow;
import common.util.MWLogger;

/**
 *
 *
 */
public class CConnector implements IConnectionListener
{
    protected IClient Client;

    protected String _host = "";
    protected int _port = -1;
    protected boolean _connected = false;
    protected IConnectionHandler _connectionHandler;
    private SplashWindow splash;

    public CConnector(IClient client) {
        Client = client;
    }

    public CConnector(IClient client, String host, int port) {
        Client = client;
        _host = host;
        _port = port;
    }

    public boolean isConnected() {return _connected;}

    /**
     * This method is called by ConnectionHandlerLocal when a new message comes in
     * from the server.
     */
    public void incomingMessage(String message) {Client.processIncoming(message);}

    /**
     * This method is called by ConnectionHandlerLocal when the connect to the server is lost.
     * connectionLost() is called on the client to inform it that the connection is lost.
     */
    public void socketClosed() {
        _connected = false;
        Client.connectionLost();
    }

    /**
     * Construct and queue an outgoing message.
     */
    public void send(String message) {
    	if ( message.indexOf("CH%7c%2fc+sendclientdata%23") < 0
    	        && message.indexOf("CH%7c%2fc+sendtomisc%23") < 0
    	        && message.indexOf("/pong") < 0) {
            MWLogger.infoLog("SENT: " + message);
        }
      _connectionHandler.queueMessage(message);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    /**
     * Make a socket connection to the server (if we're not already connected).
     * Once connected, create a ConnectionHandlerLocal, that will handle I/O.
     * @see ConnectionHandlerLocal
     */

    public void connect(String host, int port) {
        _host = host;
        _port = port;
        connect();
    }

    public void connect() {

      try {
        if (_connected) {
            MWLogger.errLog("already connected...");
            return;
        }

        if (_host.equals("") || _port == -1)
        {
            MWLogger.errLog("no host or port set...");
            return;
        }

        IOException ioexception = null;

        MWLogger.errLog("Opening socket connection to " + _host + ":" + _port);
        Socket s = null;
        try {
          s = new Socket(_host, _port);
          MWLogger.errLog("CConnector: connected to " + _host + ":" + _port);
          //MWLogger.errLog("setting NO_DELAY = true");
          s.setTcpNoDelay(true);
          _connectionHandler = new ConnectionHandlerLocal(s);
          _connectionHandler.setListener(this);
          _connected = true;
          Client.connectionEstablished();
          return;
        }
        catch (IOException e) {ioexception = e;}
        MWLogger.errLog("giving up");
        if (ioexception != null) {throw ioexception;}
      }
      catch (IOException e) {

      	if (splash != null) {
            splash.setStatus(splash.STATUS_CONNECTFAILED);
        }

        MWLogger.errLog(e);
        /*Object[] options = {"Exit"};
        int selectedValue = JOptionPane.showOptionDialog(null,"Could not connect to " + _host + ":" + _port,"Connection error!",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null,options,options[0]);
        if (selectedValue == 0)
        	System.exit(0);//exit, if they so choose*/ //Bad to do to a ded. Deds should retry every 60 seconds. --Torren.

        return;
      }
    }

    public void closeConnection() {
      _connectionHandler.shutdown(true);
    }

    public void setSplashWindow(SplashWindow s) {
    	splash = s;
    }
}
