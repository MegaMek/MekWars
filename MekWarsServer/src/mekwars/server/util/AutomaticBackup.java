/*
 * MekWars - Copyright (C) 2004
 *
 * Original author - Nathan Morris (urgru@users.sourceforge.net)
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

/**
 * Automatice Backup class to allow the Server Operators to keep backups of data files such as faction.dat planets.dat
 * and the playerfiles.
 *
 * @author Torren (Jason Tighe) 8.4.05
 *
 */

package mekwars.server.util;

import common.util.MWLogger;

public class AutomaticBackup extends Thread {


    String dateTime = "";
    String factionZipFileName = "";
    String planetZipFileName = "";
    String playerZipFileName = "";
    String dataZipFileName = "";
    String dateTimeFormat = "yyyy.MM.dd.HH.mm";
    java.io.FileOutputStream out;
    java.util.zip.ZipOutputStream zipFile;
    long time;

    public AutomaticBackup(long time) {
        super("Automatic Backup");
        this.time = time;
    }


    public void run() {

        if (this.time < 1) {return;}

        long backupHours = Long.parseLong(server.campaign.CampaignMain.cm.getConfig("AutomaticBackupHours")) *
                                 3600000; //hour in ms
        long lastBackup = Long.parseLong(server.campaign.CampaignMain.cm.getConfig("LastAutomatedBackup"));

        if (lastBackup > time - backupHours) {return;}

        MWLogger.mainLog("Archiving Started at " + time);
        server.campaign.CampaignMain.cm.setArchiving(true);

        java.text.SimpleDateFormat sDF = new java.text.SimpleDateFormat(dateTimeFormat);
        java.util.Date date = new java.util.Date(time);

        java.io.File folder = new java.io.File("./campaign/backup");

        if (!folder.exists()) {folder.mkdir();}

        dateTime = sDF.format(date);

        factionZipFileName = "./campaign/backup/factions" + dateTime + ".zip";
        planetZipFileName = "./campaign/backup/planets" + dateTime + ".zip";
        playerZipFileName = "./campaign/backup/players" + dateTime + ".zip";
        dataZipFileName = "./campaign/backup/data" + dateTime + ".zip";

        try {
            out = new java.io.FileOutputStream(factionZipFileName);
            zipFile = new java.util.zip.ZipOutputStream(out);
            zipBackupFactions();
            zipFile.close();
        } catch (Exception ex) {
            MWLogger.errLog("Unable to create factions zip file");
            MWLogger.errLog(ex);
        }

        try {
            out = new java.io.FileOutputStream(planetZipFileName);
            zipFile = new java.util.zip.ZipOutputStream(out);
            zipBackupPlanets();
            zipFile.close();
        } catch (Exception ex) {
            MWLogger.errLog("Unable to create planets zip file");
            MWLogger.errLog(ex);
        }
        try {
            out = new java.io.FileOutputStream(playerZipFileName);
            zipFile = new java.util.zip.ZipOutputStream(out);
            zipBackupPlayers();
            zipFile.close();
        } catch (Exception ex) {
            MWLogger.errLog("Unable to create player zip file");
            MWLogger.errLog(ex);
        }

        try {
            out = new java.io.FileOutputStream(dataZipFileName);
            zipFile = new java.util.zip.ZipOutputStream(out);
            zipBackupData();
            zipFile.close();
        } catch (Exception ex) {
            MWLogger.errLog("Unable to create data zip file");
            MWLogger.errLog(ex);
        }
        server.campaign.CampaignMain.cm.getConfig().setProperty("LastAutomatedBackup", Long.toString(time));
        server.campaign.CampaignMain.dso.createConfig();
        server.campaign.CampaignMain.cm.setArchiving(false);
        MWLogger.mainLog("Archiving Ended.");
    }

    /**
     * @author Torren (Jason Tighe)
     *       <p>
     *       Backup the filename into a nice zip file.
     *
     */

    public void zipBackupFactions() {
        java.io.File folder = new java.io.File("./campaign/factions");

        java.io.File[] files = folder.listFiles();

        for (int i = 0; i < files.length; i++) {
            try {
                java.io.FileInputStream in = new java.io.FileInputStream(files[i]);
                java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(files[i].getName());

                zipFile.putNextEntry(entry);
                int c;
                while ((c = in.read()) != -1) {zipFile.write(c);}
                zipFile.closeEntry();
                in.close();
            } catch (java.io.FileNotFoundException fnfe) {
                MWLogger.errLog("Unable to backup faction file: " + files[i].getName());
            } catch (Exception ex) {
                MWLogger.errLog("Unable to backup faction files");
                MWLogger.errLog(ex);
            }
        }

    }

    public void zipBackupPlanets() {
        java.io.File folder = new java.io.File("./campaign/planets");

        java.io.File[] files = folder.listFiles();

        try {
            for (int i = 0; i < files.length; i++) {
                java.io.FileInputStream in = new java.io.FileInputStream(files[i]);
                java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(files[i].getName());

                zipFile.putNextEntry(entry);
                int c;
                while ((c = in.read()) != -1) {zipFile.write(c);}
                zipFile.closeEntry();
                in.close();
            }
        } catch (Exception ex) {
            MWLogger.errLog("Unable to backup planet files");
            MWLogger.errLog(ex);
        }

    }

    public void zipBackupPlayers() {
        java.io.File folder = new java.io.File("./campaign/players");

        java.io.File[] files = folder.listFiles();

        try {
            for (int i = 0; i < files.length; i++) {
                java.io.FileInputStream in = new java.io.FileInputStream(files[i]);
                java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(files[i].getName());

                zipFile.putNextEntry(entry);
                int c;
                while ((c = in.read()) != -1) {zipFile.write(c);}
                zipFile.closeEntry();
                in.close();
            }
        } catch (Exception ex) {
            MWLogger.errLog("Unable to backup player files");
            MWLogger.errLog(ex);
        }

    }

    public void zipBackupData() {
        zipBackupData("./data");
    }

    public void zipBackupData(String path) {
        java.io.File folder = new java.io.File(path);

        java.io.File[] files = folder.listFiles();
        java.util.zip.ZipEntry entry;

        try {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    entry = new java.util.zip.ZipEntry(files[i].getPath() + "/");
                    zipFile.putNextEntry(entry);
                    zipBackupData(files[i].getPath());
                    continue;
                }

                java.io.FileInputStream in = new java.io.FileInputStream(files[i]);
                entry = new java.util.zip.ZipEntry(path + "/" + files[i].getName());

                zipFile.putNextEntry(entry);
                int c;
                while ((c = in.read()) != -1) {zipFile.write(c);}
                zipFile.closeEntry();
                in.close();
            }
        } catch (Exception ex) {
            MWLogger.errLog("Unable to backup server data files: " + path);
            MWLogger.errLog(ex);
        }

    }
}
