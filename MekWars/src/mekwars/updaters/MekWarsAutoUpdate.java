/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - Torren (torren@users.sourceforge.net)
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
 *
 * @author Torren (Jason Tighe) 02.16.06
 *
 */

package updaters;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

public class MekWarsAutoUpdate {

    // Main-Method
    /**
     * main call. Clean and simple
     */
    private static final String CONFIG_FILE = "./data/mwconfig.txt";
    private static final String logFileName = "./logs/mekwarsautoupdate.log";
    private static final String VERSION = "4.0";
    private Properties config = null;
    private SplashWindow splash = null;

    public static void main(String[] args) {

        if (logFileName != null) {
            try {
                PrintStream ps = new PrintStream(new BufferedOutputStream(
                        new FileOutputStream(logFileName), 64));
                System.setOut(ps);
                System.setErr(ps);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } // End log-to-file

        System.err.println("Running MekWarsAutoUpdate: " + VERSION);
        System.err.flush();

        new MekWarsAutoUpdate(args);
        System.exit(0);
    }

    public MekWarsAutoUpdate(String[] args) {

        loadConfigs();

        if (args.length < 1) {

            Vector<String> selections = new Vector<String>(2,1);

            selections.add("Update");
            selections.add("Create Manifest");

            JComboBox combo = new JComboBox(selections);
            combo.setEditable(false);
            JOptionPane jop = new JOptionPane(combo,
                    JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

            JDialog dlg = jop.createDialog(null, "Select Update Option");
            combo.grabFocus();
            combo.getEditor().selectAll();

            dlg.setVisible(true);

            int selection = combo.getSelectedIndex();
            int value = ((Integer) jop.getValue()).intValue();

            if (value == JOptionPane.CANCEL_OPTION) {
                System.exit(0);
            }

            if (selection == 0) {
                args = new String[] { "PLAYER" };
            } else {
                VersionManifest.createListFile();
                JOptionPane.showMessageDialog(null, "Manifest Created");
                System.exit(0);
            }
        }

        /*
         * System.err.print("args: "); for ( int count = 0; count < args.length;
         * count++ ) System.err.print(args[count]+" "); System.err.println();
         */

        if (args[0].equals("PLAYER")) {
            splash = new SplashWindow();
        }

        try {
            System.out.println("Starting Update");
            System.out.flush();
            if (splash != null) {
                splash.setStatus(splash.STATUS_FETCHINGDATA);
            }
            String url = config.getProperty("UPDATEURL");
            if (url == null || url.trim().length() < 8 || url.equals("-1")) {
                System.out
                        .println("Unable to find UPDATEURL in the .\\mwconfig.txt");
                System.out.flush();
                System.exit(0);
            }

            AutoUpdater.update(".", url, splash);
            System.out.println("Update Finished");
            System.out.flush();
            System.out.println("Starting Updatefix");
            System.out.flush();
            copyTempFiles("./update-tmp");

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (splash != null) {
            splash.dispose();
        }
        try {
            System.out.println("Restarting");
            System.out.flush();
            Runtime runTime = Runtime.getRuntime();
            String memory = config.getProperty("DEDMEMORY");
            if ( !new File("MekWarsDed.jar").exists() && !args[0].equals("PLAYER")) {
	            String[] call = { "java", "-Xmx" + memory + "m", "-jar", "MekWarsDed.jar" };
	            runTime.exec(call);
            }else {
	            String[] call = { "java", "-jar", "MekWarsClient.jar" };
	            runTime.exec(call);
            }
            System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    public void copyTempFiles(String path) {
        JCopy copier = new JCopy();
        File folder = new File(path);

        File[] files = folder.listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                copyTempFiles(file.toString());
            } else {
                try {
                    // replace ./update-temp with ./
                    String newFile = "." + file.toString().substring(12);
                    copier.copyFile(file, new File(newFile));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    private void loadConfigs() {
        config = new Properties();
        // load the saved mwconfig.txt file
        try {
            File configfile = new File(CONFIG_FILE);
            FileInputStream fis = new FileInputStream(configfile);
            config.load(fis);
            fis.close();
        } catch (Exception ex) {
            System.err.println("Unable to load config file: " + CONFIG_FILE);
            System.err.flush();
            ex.printStackTrace();
            return;
        }
    }

}

class JCopy {

    JCopy() {
    }

    public void copyFile(File in, File out) throws Exception {

        System.err
                .println("Copying " + in.toString() + " to " + out.toString());
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        try {
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            fis.close();
            fos.close();
            in.delete();
        }
    }
}