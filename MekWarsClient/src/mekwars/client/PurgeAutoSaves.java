package mekwars.client;

import mekwars.common.util.MWLogger;

class PurgeAutoSaves implements Runnable {

    public PurgeAutoSaves() {
        super();
    }

    @Override
    public void run() {
        long twoHours = 2 * 60 * 60 * 1000;
        try {
            while (true) {
                java.io.File saveFiles = new java.io.File("./savegames");
                if (!saveFiles.exists()) {
                    return;
                }
                java.io.FilenameFilter filter = new AutoSaveFilter();
                java.io.File[] fileList = saveFiles.listFiles(filter);
                for (java.io.File savedFile : fileList) {
                    long lastTime = savedFile.lastModified();
                    if (savedFile.exists()
                              && savedFile.isFile()
                              && (lastTime < (System.currentTimeMillis() - twoHours))) {
                        try {
                            MWLogger.infoLog("Purging File: "
                                                   + savedFile.getName()
                                                   + " Time: "
                                                   + lastTime
                                                   + " purge Time: "
                                                   + (System.currentTimeMillis() - twoHours));
                            savedFile.delete();
                        } catch (Exception ex) {
                            MWLogger.errLog("Error trying to delete these files!");
                            MWLogger.errLog(ex);
                        }
                    }
                }
                Thread.sleep(twoHours);
            }
        } catch (Exception ex) {
            return;
        }
    }
}// end PurgeAutoSaves
