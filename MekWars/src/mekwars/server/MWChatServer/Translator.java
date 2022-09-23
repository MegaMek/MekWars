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
package server.MWChatServer;

import java.util.Properties;

/**
 * A wrapper around the com.lyrisoft.util.i18n.Translator object that
 * provides static methods that can be called from anywhere, and an
 * init() method that should be called once, early on.
 */
public class Translator {
    private static server.MWChatServer.translator.Translator _translator;

    /**
     * Initialize the "real", underlying Translator object with the
     * given properties file.
     */
    public static void init(Properties p) {
        if (_translator == null) {
            _translator = new server.MWChatServer.translator.Translator(p);
        } else {
            System.err.println("Warning:  Translator was initialized more than once");
        }
    }

    public static String getMessage(String key) {
        return _translator.getMessage(key);
    }

    public static String getMessage(String key, String arg1) {
        return _translator.getMessage(key, arg1);
    }

    public static String getMessage(String key, String arg1, String arg2) {
        return _translator.getMessage(key, arg1, arg2);
    }

    public static String getMessage(String key, String arg1, String arg2, String arg3) {
        return _translator.getMessage(key, arg1, arg2, arg3);
    }

    public static String getMessage(String key, String[] args) {
        return _translator.getMessage(key, args);
    }
}

