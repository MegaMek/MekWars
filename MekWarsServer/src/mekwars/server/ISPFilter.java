package mekwars.server;

class ISPFilter implements java.io.FilenameFilter {
    public boolean accept(java.io.File dir, String name) {
        return (name.endsWith(".prv"));
    }
}
