/**
 * $Id: AutoUpdater.java,v 1.2 2006/09/04 23:51:15 urgru Exp $
 * 
 * Integerated autoupdater into MekWarsAutoupdate --Torren 
 * 
 * $Log: AutoUpdater.java,v $
 * Revision 1.2  2006/09/04 23:51:15  urgru
 * + Many more refactors, mostly small, involving loops, generics, and other
 *   warnings being thrown in Eclipse 3.2 that weren't caught in 3.1.
 *
 * Revision 1.1  2006/05/22 19:42:35  torren
 * + Added new MekWarsAutoUpdate.jar run in stand alone this can be used to update
 * 	the Client or create a manifest.
 *
 * Revision 1.5  2002/12/04 22:49:24  cactushack76
 *
 *
 * Changed to download new files to temp directory and create a script
 * to copy them over.
 *
 * Changed so that even if a file is not up to date, if the up to date file
 * is in the temporary directory, it won't download it again, but will update
 * the script that will move the files.
 *
 * Revision 1.4  2001/07/17 04:48:33  wurp
 * added capability for autoupdater to update files that it is currently using
 * (e.g. java.exe) by creating an updatefix.bat file to be run after the update is
 * complete.  Currently only works for DOS/WIndows platforms, but will be extended...
 *
 * Revision 1.3  2001/07/04 23:08:00  uid51722
 * Changed to use * instead of ~ for manifest file delimiter
 * Added capability of adding "<CLEANUP>*dir name" lines to the manifest.
 * This will cause all files under the "dir name" directory tree on the client which are
 * not also in the manifest to be deleted.
 *
 * Revision 1.2  2001/07/04 20:18:09  uid51722
 * Adding lots of new debug logging
 * Added test to try to use log4j_cfg.prop; log at level INFO to update.log otherwise
 *
 * Revision 1.1.1.1  2001/06/26 03:29:50  wurp
 * no message
 *
 * Revision 1.8  2001/06/23 16:23:59  wurp
 * More log4j migration
 * Fixed sr; replacement of $1 type variables was broken.
 *
 * Revision 1.7  2001/06/20 03:36:05  wurp
 * added log4j.
 * debugging character creation issues.
 *
 * Revision 1.6  2001/04/27 06:37:46  wurp
 * Barest beginnings of party/group/guild/organization support.
 * Progress on real character creation (sending stuff over the wire).
 *
 */

package updaters;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import updaters.utils.IOUtil;
import updaters.utils.Terminator;
import updaters.utils.VoidFunction;
public class AutoUpdater
{
    // Temporary directory to store the files that will be updated.
    public static final String UPDATE_TMP_DIR = "update-tmp";
    protected SplashWindow splash = null;
    
    public AutoUpdater(String localDir, SplashWindow splash)
    {
        localDir_ = new File(localDir);
        this.splash = splash;
        
    }

    public void updateToLatestVersion(Repository repository)
        throws IOException
    {
        Version latest = repository.getLatestVersion();
        updateToSpecificVersion(repository,latest);
    }

    public void updateToSpecificVersion(Repository repository,Version version)
        throws IOException
    {
        //set up progress monitor for getting manifest
        int stepsCompleted = 1;

        
        setProgressNote("Updating to version " + version.getName());

        setProgressNote("Retrieving remote manifest");
        VersionManifest manifest = version.getManifest();

        List<FileInfo> fileDiffInfos = manifest.getDiffInfos(this);

        //set up progress monitor for getting the rest of the files
        setMaximumSteps(fileDiffInfos.size());
        setProgress(stepsCompleted);

        //for each file that differs, update it
        for (FileInfo nextFileInfo : fileDiffInfos)
        {
            FileDiff diff = repository.getDiff(nextFileInfo,version);
            diff.setTempFileUpToDate(nextFileInfo.getTempFileUpToDate());
            diff.apply(this,repository);
            setProgress(++stepsCompleted);
        }

        
        cleanUpLocalFiles(manifest);
    }

    public void cleanUpLocalFiles(VersionManifest manifest)
    {
    	//get the list of directories to be cleaned up
    	List<String> dirsToCleanUp = manifest.getDirectoriesToCleanUp();
    
        System.err.println("Begining Clean up.");
    	//get the list of files that the manifest says should
    	//be on the client
    	List<File> expectedFileStructure = getExpectedClientFileStructure(manifest);
    
    	//for each directory to be cleaned up
    	for(String dirName : dirsToCleanUp)
    	{
    
            setProgressNote("Cleaning up file directory");
    	    //get the list of all files in the directories to be cleaned up
    	    List<File> localFiles =	IOUtil.getTree(new File(localDir_,dirName));
    
    	    //for each file
    	    for(File localFile : localFiles)
    	    {
        		//if the file is not part of the manifest's list, delete it.
        		if( !expectedFileStructure.contains(localFile) &&
        		    !localFile.isDirectory() )
        		{
        		    System.err.println("Deleting " + localFile);
        		    localFile.deleteOnExit();
        		}
    	    }
    	}
    }

    /**
     * Builds a list of file objects using the correct localDir from the
     * list of expected file offsets in the manifest.
     */
    public List<File> getExpectedClientFileStructure(VersionManifest manifest)
    {
    	//get the list of file offsets that the manifest says should
    	//be on the client
    	List<String> expectedOffsetStructure =manifest.getClientFileStructure();
    
    	//convert offset structure to a bunch of File objects
    	List<File> expectedFileStructure = new ArrayList<File>(expectedOffsetStructure.size());
    
    	for (String offset : expectedOffsetStructure)
    	{
    	    expectedFileStructure.add(new File(localDir_, offset));
    	}
    
    	return expectedFileStructure;
    }

    public InputStream getLocalFileStream(FileInfo fileInfo) throws IOException {
    	
        //file is embedded in a jar file, must kludge
        //java doesn't close inputstreams into jar files, so we
        //must make a copy and open a stream into that
        String[] urlParts = IOUtil.parseJarURL(fileInfo.getLocalOffset());
        if( urlParts.length > 1 ) {
            if( urlParts.length > 2 ) {
                throw new IOException("Do not understand jars within jars");
            }

            //copy the jar to a temp file
            File originalJarFile = new File(getLocalDir() + File.separator + urlParts[0]);
            String originalJarFileName = originalJarFile.getAbsolutePath();
            File tempJarFile;
            if( !copyMap_.containsKey(originalJarFileName) ) {
            	tempJarFile = File.createTempFile("copy",".jar");
            	String tempJarFileName = tempJarFile.getAbsolutePath();

            	IOUtil.copy(originalJarFileName, tempJarFileName);
            	copyMap_.put(originalJarFileName, tempJarFile);

            	final File finalTempJarFile = tempJarFile;
            	Terminator.instance().runOnExit(new VoidFunction() {
            		private static final long serialVersionUID = 1L;
            		public Object execute() {
            			finalTempJarFile.delete();
            			return null;
            		}
            	});
            } else {
            	tempJarFile = copyMap_.get(originalJarFileName);
            }

            String completeURL = tempJarFile.toURI().toURL().toString() + "!" + urlParts[1];

            completeURL = IOUtil.fixJarURL(completeURL);
            System.err.println("Reading from jarfile: " + completeURL);
            return new BufferedInputStream(new URL(completeURL).openStream());
        }
        
        //else
        System.err.println("Reading from file " + urlParts[0]);
        return new FileInputStream(getLocalDir() + File.separator +urlParts[0]);
        
    }

    public String getLocalDir()
    {
        return localDir_.getAbsolutePath();
    }

    public static void main(String[] args) throws IOException
    {
        String repositoryName = "http://www.navtools.com/cosm-client";
        String localDir = ".";


        if( args.length > 2 )
        {
            usage();
            System.exit(0);
        }
        else if( args.length > 0 )
        {
            //if args.length == 1 or 2
            localDir = args[0];

            if( args.length == 2 )
            {
                repositoryName = args[1];
            }
        }

	//create instance of UpdateFixScript singleton so any old one is
	//overwritten
	//UpdateFixScript.instance();
        update(localDir, repositoryName,null);
        Terminator.instance().exit(0);
    }

    public static void usage()
    {
        System.out.println("AutoUpdater: Update a local application from a " +
                           "remote repository.");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("java com.navtools.autoupdate.AutoUpdater [local directory=.] [repository=www.navtools.com/cosm-client]");
    }

    public static void update(String localDir, String repositoryName, SplashWindow splash) throws IOException {
        AutoUpdater updater = new AutoUpdater(localDir,splash);
        //updater.setProgressMonitor(new ProgressMonitor(new JWindow(),"Updating from remote repository","",0,1));
        Repository repository = new Repository(repositoryName);
        updater.updateToLatestVersion(repository);
    }


    public void setMaximumSteps(int max) {
        if( splash != null )
        {
            splash.getProgressBar().setMaximum(max);
        }
    }

    public void setProgressNote(String note) {
        if ( splash != null )
            this.splash.getAnimator().setLabelText(note);
    }

    public void setProgress(int stepsCompleted)
    {
        if( splash != null )
        {
            
            splash.getProgressBar().setValue(stepsCompleted);
        }
    }

    protected File localDir_;
    protected Hashtable<String,File> copyMap_ = new Hashtable<String,File>();
}

