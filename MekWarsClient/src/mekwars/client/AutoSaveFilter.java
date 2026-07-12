package mekwars.client;

/*
 * INNER CLASSES
 */
class AutoSaveFilter implements java.io.FilenameFilter {
    @Override
    public boolean accept(java.io.File dir, String name) {
        return (name.startsWith("autosave"));
    }
}
