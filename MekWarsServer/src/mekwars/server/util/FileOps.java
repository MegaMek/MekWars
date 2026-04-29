package mekwars.server.util;

class FileOps {
    private java.io.File file;
    private java.io.FileInputStream fis;
    private java.io.BufferedReader br;

    public FileOps(String file) {
        this();
        this.file = new java.io.File(file);
    }

    public FileOps() {

    }

    public int openFile() {
        try {
            fis = new java.io.FileInputStream(file);
            br = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
            return 0;
        } catch (java.io.FileNotFoundException ex) {
            return 1;
        }
    }

    public int closeFile() {
        try {
            fis.close();
            br.close();
            return 0;
        } catch (java.io.IOException ex) {
            return 1;
        }
    }

    public String getTextFromFile() {

        try {
            return br.readLine();
        } catch (java.io.IOException ex) {
            return "";
        }
    }

    public boolean eofFile() {
        try {
            return !br.ready();
        } catch (java.io.IOException ex) {
            return true;
        }
    }
}
