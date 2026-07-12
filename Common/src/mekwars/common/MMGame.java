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


/**
 * Represents a single hosted MegaMek game as advertised on the MekWars "open games" list — i.e. a lightweight,
 * network-transmittable record describing where a MegaMek server is running and who is currently connected to it,
 * not the live game state itself (contrast with {@link GameWrapper}/{@link GameInterface}, which deal with an
 * actual in-progress/completed MegaMek {@code Game}).
 * <p>
 * Instances are commonly round-tripped as a single {@code "~"}-delimited string via {@link #MMGame(String)} and
 * {@link #toString()}, which is the wire format used to advertise/announce a hosted game between server and
 * clients.
 *
 * @author Helge Richter (McWizard@gmx.de)
 * @version 0.1
 */
public class MMGame implements Serializable {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -7500735952739732172L;
    //VARIABLES
    /** TCP port the MegaMek server is listening on. */
    int port;
    /** Maximum number of players allowed to join this game. */
    int maxPlayers;
    /** Host/IP address of the MegaMek server. */
    String ip;
    /** MegaMek version string of the hosted server, used for compatibility checks by joining clients. */
    String version;
    /** Free-text comment describing the game, shown in the games list; blank if none was supplied. */
    String comment = "";
    /** Display name of the host/game, also used as the equality/identity key (see {@link #equals(Object)}). */
    String hostName;
    /** Free-text status of the game (e.g. "Open"); note the non-standard capitalized field name. */
    String Status = "Open";
    /** Names of players currently known to have joined this game, kept in sorted order. */
    TreeSet<String> currentPlayers = new TreeSet<>();

    //CONSTRUCTORS
    /**
     * Parses a game announcement previously produced by {@link #toString()}: a {@code "~"}-delimited string of
     * host name, IP, port, max players, version, an optional comment, then zero or more current player names.
     *
     * @param s the {@code "~"}-delimited game descriptor to parse.
     *
     * @throws java.util.NoSuchElementException if {@code s} has fewer than the five mandatory tokens
     *                                           (host/ip/port/maxPlayers/version).
     * @throws NumberFormatException             if the port or max-players token is not a valid integer.
     */
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

    /**
     * This constructor is used only by clients when opening a new host: builds a game announcement directly from
     * its component fields rather than parsing a delimited string.
     * <p>
     * Note: if {@code comment} is blank, only the local parameter variable {@code comment} is reassigned to
     * {@code " "} — the {@link #comment} field was already set to the original (empty) value on the line above,
     * so this blank-comment normalization has no actual effect on the field.
     *
     * @param name       the host/game display name.
     * @param ip         the server's IP/host address.
     * @param port       the server's listening port.
     * @param maxPlayers the maximum number of players allowed.
     * @param version    the MegaMek version string of the server.
     * @param comment    a free-text description of the game.
     */
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

    /**
     * Two {@code MMGame}s are considered equal purely by case-insensitive host name match; no other field
     * (ip, port, players, etc.) is compared.
     */
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
    /**
     * Serializes this game announcement back into the {@code "~"}-delimited wire format consumed by
     * {@link #MMGame(String)}: host, ip, port, maxPlayers, version, comment (or a single space if blank so the
     * field isn't dropped by the tokenizer), followed by each current player name.
     */
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

    /** @return the host/game display name. */
    public String getHostName() {
        return hostName;
    }

    /** @return the server's IP/host address. */
    public String getIp() {
        return ip;
    }

    //getters & setters
    /** @param ip the server's IP/host address to set. */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** @return the game's free-text status (e.g. "Open"). */
    public String getStatus() {
        return Status;
    }

    /** @param Status the status to set. */
    public void setStatus(String Status) {
        this.Status = Status;
    }

    /** @return the sorted set of player names currently known to be in this game. */
    public TreeSet<String> getCurrentPlayers() {
        return currentPlayers;
    }

    /*
     * setCurrent is currently unused, but would be used by an "UpdatePlayers"
     * command of some kind that clears and refills the entire player list.
     */
    /** @param v the full replacement set of current player names. */
    public void setCurrentPlayer(TreeSet<String> v) {
        currentPlayers = v;
    }

    //getters only
    /** @return the free-text comment describing this game. */
    public String getComment() {
        return comment;
    }

    /** @return the maximum number of players allowed. */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /** @return the server's listening port. */
    public int getPort() {
        return port;
    }

    /** @return the MegaMek version string of the hosted server. */
    public String getVersion() {
        return version;
    }

}
