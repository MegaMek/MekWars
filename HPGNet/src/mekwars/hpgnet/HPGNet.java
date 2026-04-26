/*
 * MekWars - Copyright (C) 2018
 *
 * Original author - Bob Eldred (spork@mekwars.org)
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

package mekwars.hpgnet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Vector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * Starts a server which listens for information from running MekWars Servers and periodically generates informational
 * webpages.  This server will also listen for and facilitate messages between MekWars Servers.  Eventually, a
 * replacement for the tracker.
 * <p>
 * Final b/c threads start in constructor.
 */

public final class HPGNet {
    Properties config;

    /**
     * @return the config
     */
    public Properties getConfig() {
        return config;
    }

    /**
     * @param config the config to set
     */
    public void setConfig(Properties config) {
        this.config = config;
    }

    private String filepath;
    private TreeSet<HPGSubscriber> subscribers;
    private int port;

    private Vector<HPGProcessingThread> processingThreads = new Vector<>();
    private Vector<HPGPurgeThread> purgingThreads = new Vector<>();

    boolean generatingHTML = false;
    boolean processing = false;
    boolean purging = false;


    public boolean isBusy() {
        return (isGeneratingHTML() | isPurging() | isProcessing());
    }

    /**
     * @param processing the processing to set
     */
    public void setProcessing(boolean processing) {
        this.processing = processing;
    }

    /**
     * @return the purgingThreads
     */
    public Vector<HPGPurgeThread> getPurgingThreads() {
        return purgingThreads;
    }

    /**
     * @param purgingThreads the purgingThreads to set
     */
    public void setPurgingThreads(Vector<HPGPurgeThread> purgingThreads) {
        this.purgingThreads = purgingThreads;
    }

    /**
     * @return the writingHTML
     */
    public boolean isGeneratingHTML() {
        return generatingHTML;
    }

    /**
     * @param writingHTML the writingHTML to set
     */
    public void setGeneratingHTML(boolean writingHTML) {
        this.generatingHTML = writingHTML;
    }

    /**
     * @return the filepath
     */
    public String getFilepath() {
        return filepath;
    }

    /**
     * @param filepath the filepath to set
     */
    private void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    /**
     * @return the subscribers
     */
    public TreeSet<HPGSubscriber> getSubscribers() {
        return subscribers;
    }

    /**
     * @param subscribers the subscribers to set
     */
    private void setSubscribers(TreeSet<HPGSubscriber> subscribers) {
        this.subscribers = subscribers;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Instantiate a new HPGNet server
     */
    public HPGNet() {
        config = new Properties();
        InputStream in;
        String fileName = "hpgnet.properties";

        try {
            in = new FileInputStream(fileName);
            config.load(in);
            in.close();
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        addToLog("config: " + config.toString());
        setPort(Integer.parseInt(config.getProperty("port", "13731")));
        setFilepath(config.getProperty("filepath", "./subscribers/"));

        setSubscribers(new TreeSet<HPGSubscriber>());

        loadAllFromDisk();

        HPGListenerThread listenerThread = new HPGListenerThread(this);
        listenerThread.setName("ListenerThread");
        listenerThread.start();

        HPGPurgeThread purgeThread = new HPGPurgeThread(this);
        purgeThread.setName("Purge Thread");
        purgeThread.start();
    }

    /**
     * Start the server.  Makes the jar runnable
     *
     */
    public static void main(String[] args) {

        try {
            new HPGNet();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    /**
     * Saves the subscriber to disk so it can be loaded next time HPGNet starts
     *
     * @param sub the Subscriber to save
     */
    public void save(HPGSubscriber sub) {
        String filename;
        String identifier = getSubscriberID(sub);

        filename = getFilepath() + identifier + ".dat";

        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        builder.setPrettyPrinting();
        Gson gson = builder.create();

        try {
            FileWriter file = new FileWriter(filename);
            file.write(gson.toJson(sub));
            file.flush();
            file.close();
        } catch (IOException e) {

        }

    }

    /**
     * Deletes an HPGSubscriber from the TreeSet and from disk
     *
     * @param sub the HPGSubscriber to delete
     */
    public void delete(HPGSubscriber sub) {
        getSubscribers().remove(sub);
        String filename = getFilepath() + getSubscriberID(sub) + ".dat";

        File file = new File(filename);
        if (file.exists() && file.isFile()) {
            file.delete();
        }
    }

    /**
     * Reads an HPGSubscriber from disk and into the TreeSet
     *
     * @return the HPGSubscribcleaR er
     */
    private HPGSubscriber load(String filename) {
        HPGSubscriber sub = null;

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(HPGSubscriber.class, new HPGSubscriberDeserializer());
            builder.excludeFieldsWithoutExposeAnnotation();
            builder.setPrettyPrinting();
            Gson gson = builder.create();

            sub = gson.fromJson(bufferedReader, HPGSubscriber.class);
            bufferedReader.close();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (sub != null) {
            sub.setTracker(this);
            sub.calculateThreatLevel();
            sub.generateHTMLString();
            return sub;
        }

        return null;
    }

    /**
     * Write a String to the log
     *
     * @param s the String to log
     */
    public void addToLog(String s) {
        //Turn on if testing.
        String fileName = "log.txt";
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileName, true);
            PrintStream printStream = new PrintStream(fileOutputStream);
            printStream.println(s);//1st line is server name
            printStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            System.out.println("Error writing to log file!");
        }
    }

    /**
     * Write an Exception to the log
     *
     * @param e the Exception to log
     */
    public void addToLog(Exception e) {
        String fileName = "log.txt";

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileName, true);
            PrintStream printStream = new PrintStream(fileOutputStream);
            e.printStackTrace(printStream);
        } catch (Exception ex) {
            System.out.println("Error writing to log file!");
        }
    }

    /**
     * @return the processingThreads
     */
    public Vector<HPGProcessingThread> getProcessingThreads() {
        return processingThreads;
    }

    /**
     * @param processingThreads the processingThreads to set
     */
    public void setProcessingThreads(Vector<HPGProcessingThread> processingThreads) {
        this.processingThreads = processingThreads;
    }

    /**
     * Are there any active HPGProcessingThreads?
     *
     */
    public boolean isProcessing() {
        return !processingThreads.isEmpty();
    }

    /**
     * Are we actively purging old entries?
     *
     */
    public boolean isPurging() {
        return !purgingThreads.isEmpty();
    }

    /**
     * Add an HPGSubsriber to the TreeSet
     *
     */
    public void addSubscriber(HPGSubscriber sub) {
        getSubscribers().add(sub);
    }

    /**
     * Add a new HPGSubscriber to the TreeSet and save
     *
     */
    public void registerNewSubscriber(HPGSubscriber sub) {
        addSubscriber(sub);
        save(sub);
    }

    /**
     * Called at startup.  Read all the save files and load them into the TreeSet
     */
    public void loadAllFromDisk() {
        File dir = new File(getFilepath());
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }


        for (File current : files) {
            if (current.isFile()) {
                if (current.getName().endsWith(".dat")) {
                    HPGSubscriber sub = load(current.toString());
                    addSubscriber(sub);
                }
            }
        }

        generateHTML();
    }

    /**
     * Get an HPGSubscriber from the TreeSet
     *
     */
    public HPGSubscriber getSubscriber(String subscriberId) {
        for (HPGSubscriber sub : getSubscribers()) {
            if (sub.getName().equalsIgnoreCase(subscriberId)) {
                return sub;
            }
        }

        return null;
    }

    /**
     * Build the web page
     */
    public void generateHTML() {
        this.setGeneratingHTML(true);

        String fileName = "mwtracker.html"; // TODO: move this to properties
        String header = "";
        String tableheader = "";
        String tablefooter = "";
        String footer = "";

        try {
            header = new String(Files.readAllBytes(Paths.get("templates/header.txt")));
            tableheader = new String(Files.readAllBytes(Paths.get("templates/table_header.txt")));
            tablefooter = new String(Files.readAllBytes(Paths.get("templates/table_footer.txt")));
            footer = new String(Files.readAllBytes(Paths.get("templates/footer.txt")));
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();

        sb.append(header);
        sb.append(tableheader);

        for (HPGSubscriber sub : subscribers) {
            sb.append(sub.getTrackerEntry()).append("\n");
        }

        sb.append(tablefooter);
        SimpleDateFormat format = new SimpleDateFormat("MMM d, yyyy h:mm:ss a");

        sb.append(format.format(new Date()));

        sb.append(footer);

        try {
            FileWriter file = new FileWriter(fileName);
            file.write(sb.toString());
            file.flush();
            file.close();
        } catch (IOException e) {

        }

        this.setGeneratingHTML(false);
    }

    /**
     * Get the identifier from an HPGSubscriber.  On legacy systems, there is no UUID, so this will be the name.  On
     * newer ones, it will be the UUID.
     *
     */
    public String getSubscriberID(HPGSubscriber sub) {
        String identifier;
        if (sub.getUuid() == null) {
            identifier = sub.getName();
        } else {
            identifier = sub.getUuid();
        }
        return identifier;
    }
}
