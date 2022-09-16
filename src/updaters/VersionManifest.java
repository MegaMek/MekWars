package updaters;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JFileChooser;

import common.util.MWLogger;
import updaters.utils.IOUtil;

public class VersionManifest {
    public VersionManifest(BufferedReader manifestStream) {
        String line = null;
        try {
            while ((line = manifestStream.readLine()) != null) {
                // System.err.println("Found line \"" + line + "\" in
                // manifest");

                // if line is not a cleanup line
                if (line.indexOf("<CLEANUP>") == -1) {
                    fileList_.add(new FileInfo(line));
                } else {
                    StringTokenizer toker = new StringTokenizer(line, separator);
                    // first token should be <CLEANUP>, throw away
                    toker.nextToken();
                    // second token is name of directory to clean up, keep it.
                    dirsToCleanUp_.add(toker.nextToken());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<FileInfo> getDiffInfos(AutoUpdater updater) {
        System.err.println("Getting diff infos");

        List<FileInfo> retval = new ArrayList<FileInfo>();
        long tempCRC = 0;
        for (FileInfo file : fileList_) {

            try {
                /*
                 * Get the local update-tmp CRC as well. If this matches, but
                 * the real local CRC does not, we'll set a flag so that we
                 * don't have to actually download the file...but it will still
                 * be added to the script.
                 */

                // Use the current FileInfo class to create a temp FileInfo
                // class representing the update-tmp version.
                StringBuilder tempManifest = new StringBuilder("./");
                tempManifest.append(AutoUpdater.UPDATE_TMP_DIR);
                tempManifest.append(File.separator);
                tempManifest.append(IOUtil.removeLeadingDotSlash(file.getLocalOffset()));
                tempManifest.append(VersionManifest.separator);
                tempManifest.append(file.getRemoteOffset());
                tempManifest.append(VersionManifest.separator);
                tempManifest.append(file.getCRC32());
                tempCRC = 0;

                try {
                    FileInfo tempFile = new FileInfo(tempManifest.toString());
                    InputStream tempIs = updater.getLocalFileStream(tempFile);
                    tempCRC = IOUtil.getCRC32(tempIs);
                    tempIs.close();
                } catch (FileNotFoundException fnfe) {
                    // File does not exist in tmp directory, proceed
                    // as usual.
                    System.err.println("File " + file.getLocalOffset() + " is not in the temp directory.");
                }

                InputStream is = updater.getLocalFileStream(file);
                long localCRC = IOUtil.getCRC32(is);
                is.close();

                if (localCRC != file.getCRC32()) {
                    System.err.println("Noting difference between remote file " + file.getRemoteOffset() + " and local file " + file.getLocalOffset());

                    // If there is not a difference between the remote and
                    // local temporary file, set flag so we don't download.
                    if (tempCRC == file.getCRC32()) {
                        file.setTempFileUpToDate(true);
                        System.err.println("The file " + file.getLocalOffset() + " is different, but there is an up to date " + " file in the tmp directory.");
                    } else {
                        file.setTempFileUpToDate(false);
                    }
                    retval.add(file);
                } else {
                    System.err.println("Remote file " + file.getRemoteOffset() + " and local file " + file.getLocalOffset() + " appear to be the same.");
                }
            }
            // File does not exist locally; retrieve it
            catch (FileNotFoundException e) {
                System.err.println("File " + file.getLocalOffset() + " doesn't exist locally.  Retrieving.");

                // check the temp CRC to see if we need to flag for
                // no download
                if (tempCRC == file.getCRC32()) {
                    file.setTempFileUpToDate(true);
                    System.err.println("File " + file.getLocalOffset() + " doesn't exist locally, but it is up " + "to date in the temp directory, no need to download.");
                } else {
                    file.setTempFileUpToDate(false);
                }
                retval.add(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return retval;
    }

    /**
     * Returns a list of all of the files that this manifest specifies should be
     * on the client.
     */
    public List<String> getClientFileStructure() {
        System.err.println("Getting client file structure");

        List<String> retval = new ArrayList<String>();
        for (FileInfo file : fileList_) {
            retval.add(file.getLocalOffset());
        }

        return retval;
    }

    /**
     * Returns a list of directories that this manifest specifies must be
     * cleaned up; i.e. all files in one of these directories that is not also
     * in the manifest should be deleted.
     */
    public List<String>/* String */getDirectoriesToCleanUp() {
        return dirsToCleanUp_;
    }

    protected static void createListFile() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setCurrentDirectory(new File("./"));
        fc.setApproveButtonText("Create List");
        fc.setApproveButtonToolTipText("<html>Select the folder where the file list<br>and manifest will be crated.</html>");
        int selection = fc.showOpenDialog(null);

        if (selection != JFileChooser.APPROVE_OPTION)
            return;
        /*
         * FileDialog fDialog = new FileDialog(new JDialog(),"File
         * Path",FileDialog.LOAD); fDialog.setVisible(true);
         * 
         * String rootDir = fDialog.getDirectory();
         */
        String rootDir = fc.getSelectedFile().getPath() + File.separatorChar;
        String outFileName = rootDir + "files.txt";

        // Remove old manifest files
        if (new File(outFileName).exists())
            new File(outFileName).delete();
        if (new File(rootDir + "Manifest.txt").exists())
            new File(rootDir + "Manifest.txt").delete();
        // if ( new File(rootDir+"Manifest.txt.jar").exists() )
        // new File(rootDir+"Manifest.txt.jar").delete();

        // want to leave that last \ or / on
        // rootDir = rootDir.substring(0,rootDir.length()-1);

        try {
            FileOutputStream out = new FileOutputStream(outFileName);
            PrintStream ps = new PrintStream(out);

            File[] listFiles = new File(rootDir).listFiles();

            for (File file : listFiles)
                VersionManifest.printOutFiles(ps, file, rootDir);
            ps.flush();
            out.flush();
            ps.close();
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        createManifestFile(outFileName);
    }

    private static void printOutFiles(PrintStream ps, File file, String root) {

        try {
            if (file.isDirectory()) {
                // Dont bother sending the logs folder.
                if (file.getAbsolutePath().endsWith("logs") 
                        || file.getAbsolutePath().endsWith("servers") 
                        || file.getAbsolutePath().endsWith("campaign") 
                        || file.getAbsolutePath().endsWith("mmconf"))
                    return;

                File[] listFiles = file.listFiles();
                for (File newFile : listFiles)
                    VersionManifest.printOutFiles(ps, newFile, root);
            } else {

                String path = file.getAbsolutePath();

                // Dont want any windows Thumbs.db files
                // are mwconfig files as those could
                // screw over the end users.
                if (path.endsWith("Thumbs.db") || path.endsWith("mwconfig.txt") || path.endsWith("mwconfig.txt.bak") || path.endsWith("files.txt") || path.endsWith("Manifest.txt") || path.endsWith("units.cache"))
                    // || path.endsWith("Manifest.txt.jar"))
                    return;

                int index = path.indexOf(root);

                if (index > -1)
                    path = "./" + path.substring(index + root.length());
                ps.println(path);
                ps.flush();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        BufferedReader manifestList = new BufferedReader(new FileReader(args[0]));
        String line;
        while ((line = manifestList.readLine()) != null) {
            long crc32 = IOUtil.getCRC32(new BufferedInputStream(new FileInputStream(line)));
            System.out.println(line + separator + line + separator + crc32);
        }
        manifestList.close();
    }

    private static void createManifestFile(String outPutFileName) {
    	BufferedReader manifestList = null;
    	try {
            manifestList = new BufferedReader(new FileReader(outPutFileName));

            String manifestFileName = outPutFileName.substring(0, outPutFileName.indexOf("files.txt")) + "Manifest.txt";
            FileOutputStream out = new FileOutputStream(manifestFileName);
            PrintStream ps = new PrintStream(out);

            String line, newLine = "";
            while ((line = manifestList.readLine()) != null) {
                long crc32 = IOUtil.getCRC32(new BufferedInputStream(new FileInputStream(line)));
                newLine = IOUtil.replaceString(line, "\\", "/");
                ps.println(newLine + separator + newLine + separator + crc32);
            }
            // Basic folders that should be cleaned up regularly.
            ps.println("<CLEANUP>*./data/mechfiles");
            ps.println("<CLEANUP>*./data/images/units");
            ps.println("<CLEANUP>*./data/images/camo");
            ps.println("<CLEANUP>*./data/images/units/wrecks");
            ps.println("<CLEANUP>*./data/images/misc");
            ps.println("<CLEANUP>*./data/images/");
            ps.println("<CLEANUP>*./data/images/widgets");
            ps.println("<CLEANUP>*./lib");
            ps.println("<CLEANUP>*./data/boards/");
            ps.flush();
            out.flush();
            ps.close();
            out.close();
            /*
             * Runtime runtime = Runtime.getRuntime(); String[] call = { "jar",
             * "-cf", "Manifest.txt.jar", "Manifest.txt" }; Process event =
             * runtime.exec(call); ProcessLogger outLogger = new
             * ProcessLogger(event.getInputStream()); ProcessLogger errLogger =
             * new ProcessLogger(event.getErrorStream()); outLogger.start();
             * errLogger.start(); event.waitFor();
             */
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
        	try {
				manifestList.close();
			} catch (IOException e) {
				MWLogger.errLog(e);
			}
        }
    }

    protected List<FileInfo> fileList_ = new ArrayList<FileInfo>();

    protected List<String> dirsToCleanUp_ = new ArrayList<String>();

    public static String separator = "*";
}
