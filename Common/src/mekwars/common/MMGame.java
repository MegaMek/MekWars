/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

package mekwars.common;

//A Class Holding a Game Object might need some more information
//@Author Helge Richter (McWizard@gmx.de)
//@Version 0.1

import java.io.Serial;
import java.io.Serializable;
import java.util.StringTokenizer;
import java.util.TreeSet;


public class MMGame implements Serializable {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -7500735952739732172L;
    //VARIABLES
    int port;
    int maxPlayers;
    String ip;
    String version;
    String comment = "";
    String hostName;
    String Status = "Open";
    TreeSet<String> currentPlayers = new TreeSet<>();

    //CONSTRUCTORS
    public MMGame(String s) {
        StringTokenizer ST = new StringTokenizer(s, "~");
        hostName = ST.nextToken();
        ip = ST.nextToken();
        port = (Integer.parseInt(ST.nextToken()));
        maxPlayers = (Integer.parseInt(ST.nextToken()));
        version = ST.nextToken();

        if (ST.hasMoreTokens()) {comment = ST.nextToken();}

        while (ST.hasMoreTokens()) {currentPlayers.add(ST.nextToken());}
    }

    //This constructor is used only by clients when opening a new host.
    public MMGame(String name, String ip, int port, int maxPlayers, String version, String comment) {

        this.hostName = name;
        this.ip = ip;
        this.port = port;
        this.maxPlayers = maxPlayers;
        this.version = version;
        this.comment = comment;
        if (comment.trim().isEmpty()) {
            comment = " ";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MMGame) {
            MMGame game;
            try {
                game = (MMGame) o;
            } catch (ClassCastException e) {
                return false;
            }

            return game.getHostName().equalsIgnoreCase(this.getHostName());
        }

        return false;
    }

    //METHODS
    @Override
    public String toString() {

        StringBuilder result = new StringBuilder();
        result.append(hostName)
              .append("~")
              .append(ip)
              .append("~")
              .append(port)
              .append("~")
              .append(maxPlayers)
              .append("~")
              .append(version)
              .append("~");

        //don't send empty comment
        if (comment == null || comment.isEmpty()) {
            result.append(" ~");
        } else {
            result.append(comment).append("~");
        }

        for (String currName : currentPlayers) {
            result.append(currName).append("~");
        }

        return result.toString();

    }

    public String getHostName() {
        return hostName;
    }

    public String getIp() {
        return ip;
    }

    //getters & setters
    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String Status) {
        this.Status = Status;
    }

    public TreeSet<String> getCurrentPlayers() {
        return currentPlayers;
    }

    /*
     * setCurrent is currently unused, but would be used by an "UpdatePlayers"
     * command of some kind that clears and refills the entire player list.
     */
    public void setCurrentPlayer(TreeSet<String> v) {
        currentPlayers = v;
    }

    //getters only
    public String getComment() {
        return comment;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getPort() {
        return port;
    }

    public String getVersion() {
        return version;
    }

}
