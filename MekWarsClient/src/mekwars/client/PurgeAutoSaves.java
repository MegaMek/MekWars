package mekwars.client;

import megamek.logging.MMLogger;

class PurgeAutoSaves implements Runnable {
    private static final MMLogger LOGGER = MMLogger.create(PurgeAutoSaves.class);

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
                            LOGGER.info("Purging File: "
                                                   + savedFile.getName()
                                                   + " Time: "
                                                   + lastTime
                                                   + " purge Time: "
                                                   + (System.currentTimeMillis() - twoHours));
                            savedFile.delete();
                        } catch (Exception ex) {
                            LOGGER.error("Error trying to delete these files!");
                            LOGGER.error(ex, "");
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
