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

package common.util;

import java.io.File;

import org.mekwars.libpk.logging.PKLogManager;


public final class MWLogger {// final - no extension of the server logger

    private File logDir;
    private static PKLogManager logmanager = null;
    private static MWLogger logger = null;
    
    private MWLogger() {
        
        logmanager = PKLogManager.getInstance();
        logDir = new File("logs");
        if (!logDir.exists()) {
            try {
                if (!logDir.mkdirs()) {
                    System.err.println("WARNING: logging directory cannot be created!");
                    System.err.println("WARNING: disabling log subsystem");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (!logDir.isDirectory()) {
            System.err.println("WARNING: logging directory is not a directory!");
            System.err.println("WARNING: disabling log subsystem");
            return;
        }

        if (!logDir.canWrite()) {
            System.err.println("WARNING: cannot write in logging directory!");
            System.err.println("WARNING: disabling log subsystem");
            return;
        }
    }
    
    public static MWLogger getInstance() {
        if (logger == null) {
            logger = new MWLogger();
        }
        return logger;
    }
    
    public static void errLog(String message) {
        logmanager.log("errlog", message);
    }
    
    public static void mainLog(String message) {
        logmanager.log("mainlog", message);
    }
    
    public static void modLog(String message) {
        logmanager.log("modlog", message);
    }
    
    public static void debugLog(String message) {
        logmanager.log("debuglog", message);
    }
    
    public static void ipLog(String message) {
        logmanager.log("iplog", message);
    }
    
    public static void cmdLog(String message) {
        logmanager.log("errlog", message);
    }
    
    public static void errLog(Exception e) {
        logmanager.log("errlog", e);
    }
    
    public static void mainLog(Exception e) {
        logmanager.log("mainlog", e);
    }
    
    public static void modLog(Exception e) {
        logmanager.log("modlog", e);
    }
    
    public static void debugLog(Exception e) {
        logmanager.log("debuglog", e);
    }
    
    public static void ipLog(Exception e) {
        logmanager.log("iplog", e);
    }
    
    public static void cmdLog(Exception e) {
        logmanager.log("errlog", e);
    }
    
    public static void infoLog(String message) {
        logmanager.log("infolog", message);
    }
    
    public static void infoLog(Exception e) {
        logmanager.log("infolog", e);
    }

    public static void bmLog(String message) {
        logmanager.log("bmlog", message);
    }
    
    public static void bmLog(Exception e) {
        logmanager.log("bmlog", e);
    }
    public static void resultsLog(String message) {
        logmanager.log("resultslog", message);
    }
    
    public static void resultsLog(Exception e) {
        logmanager.log("resultslog", e);
    }

    public static void gameLog(String message) {
        logmanager.log("gamelog", message);
    }
    
    public static void gameLog(Exception e) {
        logmanager.log("gamelog", e);
    }
    
    public static void testLog(String message) {
        logmanager.log("testlog", message);
    }
    
    public static void testLog(Exception e) {
        logmanager.log("testlog", e);
    }
    
    public static void tickLog(String message) {
        logmanager.log("ticklog", message);
    }
    
    public static void tickLog(Exception e) {
        logmanager.log("ticklog", e);
    }
    
    public static void warnLog(String message) {
        logmanager.log("warnlog", message);
    }
    
    public static void warnLog(Exception e) {
        logmanager.log("warnlog", e);
    }
    
    public static void pmLog(String message) {
        logmanager.log("pmlog", message);
    }
    
    public static void pmLog(Exception e) {
        logmanager.log("pmlog", e);
    }
    
    public static void factionLog(String factionName, String message) {
        logmanager.log(factionName, message);
    }
    
    public static void factionLog(String factionName, Exception e) {
        logmanager.log(factionName, e);
    }
}
