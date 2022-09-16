package updaters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;

import updaters.utils.IOUtil;

public class ReplaceFileDiff extends FileDiff {
    public ReplaceFileDiff(FileInfo fileInfo, Version version) {
        localFileOffset_ = fileInfo.getLocalOffset();
        remoteFileOffset_ = fileInfo.getRemoteOffset();
        version_ = version;

        System.err.println("Building replace file diff for local " + localFileOffset_ + ", remote " + remoteFileOffset_);
    }

    @Override
    public void apply(AutoUpdater updater, Repository repository) {
        String file = localFileOffset_;

        if (file.indexOf("/") > -1) {
            file = ".." + file.substring(file.lastIndexOf("/"));
        }

        updater.setProgressNote("Updating " + file);
        try {
            // get a stream to the remote file
            InputStream in = version_.getBinaryFile(remoteFileOffset_);

            // if the local file points to a jar, parse jar name from entry name
            StringTokenizer toker = new StringTokenizer(localFileOffset_, "!");

            // Get the token for the file name and absolute path
            String fileToken = toker.nextToken();

            // Create the file target (where the file will actually be after the update script
            // is run...which is run after this process)
            String fileCopyTarget = updater.getLocalDir() + File.separator + fileToken;// IOUtil.fixPath(fileToken);

            // Create a destination path that will be the temporary place until the script is run
            String destFileName = updater.getLocalDir() + File.separator + AutoUpdater.UPDATE_TMP_DIR + File.separator + fileToken;// IOUtil.fixPath(fileToken);

            // We've got the proper paths set up, but if we have a flag
            // showing that the tmp directory already has the up to date
            // version, then we can just update the move script and get
            // out since there is no need to download.
            // UpdateFixScript.instance().addMoveCommand(destFileName,fileCopyTarget);
            if (tempFileUpToDate_) {
                return;
            }

            // first check if temp update dir exists and if not, create it
            File tmpDirFile = new File(updater.getLocalDir() + File.separator + AutoUpdater.UPDATE_TMP_DIR);
            if (!tmpDirFile.exists()) {
                tmpDirFile.mkdirs();
            }

            // remove directory of same name if it exists
            File destFile = new File(destFileName);
            if (destFile.isDirectory()) {
                destFile.delete();
            }

            // create parent dir if necessary
            File destFileParent = new File(destFile.getParent());
            if (!destFileParent.exists()) {
                destFileParent.mkdirs();
            }

            // Also, make sure parent in the target exists, so it's there for copying purposes
            File targetParent = new File(new File(fileCopyTarget).getParent());
            if (!targetParent.exists()) {
                targetParent.mkdirs();
            }

            // if the local file is embedded in a jar
            /*if( toker.hasMoreTokens() )
            {
                destFile.createNewFile();

                //handle case when jar doesn't already exist
                String offsetWithinJar = toker.nextToken();

                //build a temp directory to put the file in
                File tempDir = File.createTempFile("autoupdate",".tmp");
                tempDir.delete();
                File tempFile = new File(tempDir.getAbsolutePath() +
                                         File.separator + offsetWithinJar);

                //create the path to the new temporary file
                new File(tempFile.getParent()).mkdirs();

                OutputStream out = new FileOutputStream(tempFile);
                IOUtil.copy(in,out);
                out.close();

                //e.g. jar uf cosm.jar -C autoupdate32.tmp com\navtools\cosm\AdminApp.class
                jar_.run(new String[]{"uf", destFileName, "-C",
                                      tempDir.getAbsolutePath(),
                                      offsetWithinJar});

                tempFile.delete();
                IOUtil.removeDirectoriesBetween(new File(tempDir.getParent()),
                                                new File(tempFile.getParent()));
            }
            else
            {*/
            try {
                OutputStream out = new FileOutputStream(destFileName);
                IOUtil.copy(in, out);
                out.close();
            } catch (FileNotFoundException e) {
                // FileNotFoundException is assumed to be caused by
                // the file being in use. We must put the file under an
                // alternate file name and build a script, to be run after
                // autoupdater exits, that moves the files back to the
                // correct file names.
                System.err.println(destFileName + " in use.  Writing to alternate file name.  Must run updatefix script after autoupdate process completes.");

                // fiu == file in use
                String newDestFileName = destFileName + ".fiu";
                OutputStream out = new FileOutputStream(newDestFileName);
                IOUtil.copy(in, out);
                out.close();

                // Due to changes in the update process, this command will be
                // executed for every file in order to create a script
                // UpdateFixScript.instance().addMoveCommand(newDestFileName,
                // destFileName);
            }
            // }

            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setTempFileUpToDate(boolean isLatest) {
        tempFileUpToDate_ = isLatest;
    }

    private boolean tempFileUpToDate_ = false;
    public String localFileOffset_;
    public String remoteFileOffset_;
    public Version version_;
    // public Main jar_ = new Main(System.out, System.out, "jar");
}
