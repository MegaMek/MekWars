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
 * Automatice Backup class to allow the Server Operators to keep backups of
 * data files such as faction.dat planets.dat and the playerfiles.
 * 
 * @author Torren (Jason Tighe) 8.4.05 
 * 
 */

package server.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import common.util.MWLogger;
import server.campaign.CampaignMain;

public class AutomaticBackup extends Thread{


    String dateTime = "";
    String factionZipFileName = "";
    String planetZipFileName = "";
    String playerZipFileName = "";
    String dataZipFileName = "";
    String dateTimeFormat = "yyyy.MM.dd.HH.mm";
    FileOutputStream out;
    ZipOutputStream zipFile;
    long time;

    public AutomaticBackup(long time) {
        super("Automatic Backup");
        this.time = time;
    }
    

    public void run() {
    	
    	if ( this.time < 1)
    		return;
    	
        long backupHours = Long.parseLong(CampaignMain.cm.getConfig("AutomaticBackupHours")) * 3600000; //hour in ms
        long lastBackup = Long.parseLong(CampaignMain.cm.getConfig("LastAutomatedBackup"));
        
        if (lastBackup > time - backupHours )
            return;
        
        MWLogger.mainLog("Archiving Started at "+time);
        CampaignMain.cm.setArchiving(true);
        
        SimpleDateFormat sDF = new SimpleDateFormat(dateTimeFormat);
        Date date = new Date(time);

        File folder = new File("./campaign/backup");
        
        if ( !folder.exists() )
            folder.mkdir();
        
        dateTime = sDF.format(date);
        
        factionZipFileName = "./campaign/backup/factions"+dateTime+".zip";
        planetZipFileName = "./campaign/backup/planets"+dateTime+".zip";
        playerZipFileName = "./campaign/backup/players"+dateTime+".zip";
        dataZipFileName = "./campaign/backup/data"+dateTime+".zip";
        
    	try{
        	out = new FileOutputStream(factionZipFileName);
        	zipFile = new ZipOutputStream(out);
        	zipBackupFactions();
        	zipFile.close();
    	}
    	catch(Exception ex){
        	MWLogger.errLog("Unable to create factions zip file");
        	MWLogger.errLog(ex);
    	}

    	try{
        	out = new FileOutputStream(planetZipFileName);
        	zipFile = new ZipOutputStream(out);
        	zipBackupPlanets();
        	zipFile.close();
    	}
    	catch(Exception ex){
        	MWLogger.errLog("Unable to create planets zip file");
        	MWLogger.errLog(ex);
    	}
        try{
	        out = new FileOutputStream(playerZipFileName);
	        zipFile = new ZipOutputStream(out);
	        zipBackupPlayers();
	        zipFile.close();
        }
        catch(Exception ex){
            MWLogger.errLog("Unable to create player zip file");
            MWLogger.errLog(ex);
        }

        try{
            out = new FileOutputStream(dataZipFileName);
            zipFile = new ZipOutputStream(out);
            zipBackupData();
            zipFile.close();
        }
        catch(Exception ex){
            MWLogger.errLog("Unable to create data zip file");
            MWLogger.errLog(ex);
        }
		CampaignMain.cm.getConfig().setProperty("LastAutomatedBackup",Long.toString(time));
		CampaignMain.dso.createConfig();
        CampaignMain.cm.setArchiving(false);
        MWLogger.mainLog("Archiving Ended.");
    }
    
    /**
     * @author Torren (Jason Tighe)
     *
     * Backup the filename into a nice zip file.
     * 
     */
    
    public void zipBackupFactions(){
        File folder = new File("./campaign/factions");
        
        File[] files = folder.listFiles();

            for ( int i = 0; i < files.length; i++ ){
                try {
                    FileInputStream in = new FileInputStream(files[i]);
                    ZipEntry entry = new ZipEntry(files[i].getName());
                    
                    zipFile.putNextEntry(entry);
                    int c;
                    while ((c = in.read()) != -1)
                        zipFile.write(c);
                    zipFile.closeEntry();
                    in.close();
                }
                catch ( FileNotFoundException fnfe ){
                    MWLogger.errLog("Unable to backup faction file: "+files[i].getName());
                }
                catch (Exception ex){
                    MWLogger.errLog("Unable to backup faction files");
                    MWLogger.errLog(ex);
                }
            }
        
    }

    public void zipBackupPlanets(){
        File folder = new File("./campaign/planets");
        
        File[] files = folder.listFiles();

        try {
            for ( int i = 0; i < files.length; i++ ){
                FileInputStream in = new FileInputStream(files[i]);
                ZipEntry entry = new ZipEntry(files[i].getName());
                
                zipFile.putNextEntry(entry);
                int c;
                while ((c = in.read()) != -1)
                    zipFile.write(c);
                zipFile.closeEntry();
                in.close();
            }
        }
        catch (Exception ex){
            MWLogger.errLog("Unable to backup planet files");
            MWLogger.errLog(ex);
        }
        
    }
    
    public void zipBackupPlayers(){
        File folder = new File("./campaign/players");
        
        File[] files = folder.listFiles();

        try {
	        for ( int i = 0; i < files.length; i++ ){
	            FileInputStream in = new FileInputStream(files[i]);
				ZipEntry entry = new ZipEntry(files[i].getName());
				
				zipFile.putNextEntry(entry);
				int c;
				while ((c = in.read()) != -1)
				    zipFile.write(c);
	            zipFile.closeEntry();
	            in.close();
	        }
        }
        catch (Exception ex){
            MWLogger.errLog("Unable to backup player files");
            MWLogger.errLog(ex);
        }
        
    }

    public void zipBackupData(){
    	zipBackupData("./data");
    }

    public void zipBackupData(String path){
        File folder = new File(path);
        
        File[] files = folder.listFiles();
        ZipEntry entry;
        
        try {
            for ( int i = 0; i < files.length; i++ ){
                if ( files[i].isDirectory() ){
                   	entry = new ZipEntry(files[i].getPath()+"/");
                    zipFile.putNextEntry(entry);
                	zipBackupData(files[i].getPath());
                    continue;
                }
                
                FileInputStream in = new FileInputStream(files[i]);
               	entry = new ZipEntry(path+"/"+files[i].getName());

                zipFile.putNextEntry(entry);
                int c;
                while ((c = in.read()) != -1)
                    zipFile.write(c);
                zipFile.closeEntry();
                in.close();
            }
        }
        catch (Exception ex){
            MWLogger.errLog("Unable to backup server data files: "+path);
            MWLogger.errLog(ex);
        }
        
    }
}