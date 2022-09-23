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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
// import org.mekwars.libpk.logging.PKLogManager;
import org.apache.logging.log4j.Marker;


public final class MWLogger {// final - no extension of the server logger

    private File logDir;
    // private static PKLogManager logmanager = null;
    private static MWLogger logger = null;
    
    private MWLogger() {
        
        // logmanager = PKLogManager.getInstance();
        // logDir = new File("logs");
        LogManager.getLogger().info("MWLogger Started");
    //     if (!logDir.exists()) {
    //         try {
    //             if (!logDir.mkdirs()) {
    //                 System.err.println("WARNING: logging directory cannot be created!");
    //                 System.err.println("WARNING: disabling log subsystem");
    //                 return;
    //             }
    //         } catch (Exception e) {
    //             e.printStackTrace();
    //         }
    //     } else if (!logDir.isDirectory()) {
    //         System.err.println("WARNING: logging directory is not a directory!");
    //         System.err.println("WARNING: disabling log subsystem");
    //         return;
    //     }

    //     if (!logDir.canWrite()) {
    //         System.err.println("WARNING: cannot write in logging directory!");
    //         System.err.println("WARNING: disabling log subsystem");
    //         return;
    //     }
    }
    
    public static MWLogger getInstance() {
        if (logger == null) {
            logger = new MWLogger();
        }
        return logger;
    }
    
    public static void errLog(String message) {
       LogManager.getLogger().error(message);
    }
    
    public static void mainLog(String message) {
        LogManager.getLogger().log(Level.ALL, message);
    }
    
    public static void modLog(String message) {
        LogManager.getLogger().log(Level.INFO, message);
    }
    
    public static void debugLog(String message) {
        LogManager.getLogger().debug(message);
    }
    
    public static void ipLog(String message) {
        LogManager.getLogger().info( message);
    }
    
    public static void cmdLog(String message) {
        LogManager.getLogger().error(message);
    }
    
    public static void errLog(Exception e) {
        LogManager.getLogger().error(e);
    }
    
    public static void mainLog(Exception e) {
        LogManager.getLogger().info(e);
    }
    
    public static void modLog(Exception e) {
        LogManager.getLogger().info(e);
    }
    
    public static void debugLog(Exception e) {
        LogManager.getLogger().info(e);
    }
    
    public static void ipLog(Exception e) {
        LogManager.getLogger().info(e);
    }
    
    public static void cmdLog(Exception e) {
        LogManager.getLogger().info(e);
    }
    
    public static void infoLog(String message) {
        LogManager.getLogger().info(message);
    }
    
    public static void infoLog(Exception e) {
        LogManager.getLogger().info(e);
    }

    public static void bmLog(String message) {
        LogManager.getLogger().info(message);
    }
    
    public static void bmLog(Exception e) {
        LogManager.getLogger().info(e);
    }
    public static void resultsLog(String message) {
        final Marker results = MarkerManager.getMarker("resutlsLog");
        LogManager.getLogger().info(results, message);
    }
    
    public static void resultsLog(Exception e) {
        LogManager.getLogger().info(e);
    }

    public static void gameLog(String message) {
        final Marker game = MarkerManager.getMarker("gameLog");
        LogManager.getLogger().info(game, message);
    }
    
    public static void gameLog(Exception e) {
        LogManager.getLogger().info(e);
    }
    
    public static void testLog(String message) {
        final Marker test = MarkerManager.getMarker("testLog");
        LogManager.getLogger().info(test, message);
    }
    
    public static void testLog(Exception e) {
        LogManager.getLogger().info(e);
    }
    
    public static void tickLog(String message) {
        final Marker tick = MarkerManager.getMarker("tickLog");
        LogManager.getLogger().info(tick, message);
    }
    
    public static void tickLog(Exception e) {
        LogManager.getLogger().info(e);
    }
    
    public static void warnLog(String message) {
        LogManager.getLogger().warn(message);
    }
    
    public static void warnLog(Exception e) {
        LogManager.getLogger().warn(e);
    }
    
    public static void pmLog(String message) {
        final Marker pm = MarkerManager.getMarker("pmLog");
        LogManager.getLogger().info(pm, message);
    }
    
    public static void pmLog(Exception e) {
        LogManager.getLogger().info(e);
    }
    
    public static void factionLog(String factionName, String message) {
        final Marker faction = MarkerManager.getMarker(factionName);
        LogManager.getLogger().info(faction, message);
    }
    
    public static void factionLog(String factionName, Exception e) {
        final Marker factiondebug = MarkerManager.getMarker(factionName);
        LogManager.getLogger().debug(factiondebug, e.getMessage());
    }
}
