/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Copyright (C) 2004 Helge Richter (McWizard)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */


package mekwars.common.commands;

import java.io.Serial;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.interfaces.IServer;

/**
 * Abstract base class for every "chat-protocol" command handler in the {@code mekwars.common.commands} package
 * (roughly three dozen subclasses, including {@link PrivateMessageCommand}, {@link BuildTableCommand},
 * {@link OperationCommand}, {@link PlayerCommand} and {@link CH}). This is the higher-level, application command
 * layer that rides on top of ordinary chat lines exchanged between the MekWars client and server, as opposed to the
 * lower-level login/handshake layer represented by {@link IProtCommand}.
 * <p>
 * <b>Wire format and dispatch key ("prefix").</b> Every protocol line handled by this family is a sequence of
 * tokens separated by {@link #DELIM} ("|"), which is the same character as {@code IClient.COMMAND_DELIMITER}. The
 * very first token on the line is the dispatch key (referred to throughout this class as the "prefix"), a short
 * code such as {@code "CH"} or {@code "PM"} that identifies which concrete {@code Command} subclass understands the
 * rest of the line.
 * <p>
 * <b>Client-side flow.</b> {@code MWClient} tokenizes each incoming server line on the command delimiter, treats
 * the first token as a lookup/class name, obtains (creating and caching it on first use) the matching
 * {@code Command} instance, and then invokes {@link #execute(String)} with the <em>entire original line, prefix
 * included</em>. Consequently every {@code execute(String)} override is expected to call {@link #decode(String)}
 * itself to strip and discard that leading token before parsing the remaining arguments. In this package, in
 * practice, {@code execute(String)} is where all the real client-side work happens (updating the GUI, playing
 * sounds, writing files, etc.).
 * <p>
 * <b>Server-side flow.</b> {@code MWServ} keeps a {@link Table} of {@link ServerCommand}s keyed by
 * {@link #getPrefix()}; for an inbound client line it looks up the command for the leading token, then calls
 * {@link #setUsername(String)} followed by {@link #parseArguments(String)} with the remainder of the line (prefix
 * already stripped). In every concrete subclass documented in this pass, {@code parseArguments(String)} is left as
 * an empty no-op stub, which indicates these particular command classes are effectively client-inbound only (i.e.
 * they model messages the <em>server sends and the client executes</em>, not messages the client sends for the
 * server to act on).
 * <p>
 * <b>Request/reply flow.</b> A secondary, mostly-unused-by-this-package pathway supports commands that are sent as
 * a request and later matched against a coded reply: {@link #parseReply(String)} (implemented here, not overridden
 * by subclasses) checks the reply's leading token against {@link #getPrefix()}, detects an {@code "E"} error
 * marker, and delegates the remaining text to the abstract {@link #parseReplyArgs(String)} for the subclass to
 * interpret. {@link #timeout()} and the {@link #TIMEOUT}/{@link #UNKNOWN}/{@link #MALFORMED} constants belong to
 * this same flow. This machinery is only fully wired up when a subclass is constructed via
 * {@link #Command(String)} (which populates human-readable {@link #ErrorMessages}); subclasses constructed via
 * {@link #Command(IClient)} (all five reviewed in this pass) never populate {@code ErrorMessages}, so
 * {@link #getErrorMessage()} will return an empty string for them even if {@link #hasError()} is true.
 * <p>
 * <b>Quirks worth knowing.</b>
 * <ul>
 * <li>This class extends {@link Thread} but no subclass in this package overrides {@code run()}, and nothing in
 * the codebase calls {@code start()} on a {@code Command} instance — {@code execute}/{@code parseArguments} are
 * always invoked as ordinary synchronous method calls. The {@code Thread} superclass appears to be vestigial.</li>
 * <li>{@link #getPrefix()} returns {@code null} for any command built with {@link #Command(IClient)}, since only
 * {@link #Command(String)} sets {@code myPrefix}.</li>
 * <li>The {@link Table} dispatch registry (used by the server as {@code Command.Table myCommands}) is declared and
 * read, but no code populating it via {@code put(...)} was found in this codebase — server-side dispatch through
 * this table currently appears unwired/dead in practice.</li>
 * </ul>
 *
 * @author Steve Hawkins
 */

public abstract class Command extends Thread implements ClientCommand, ServerCommand {

    /** The field delimiter used on the wire between tokens of a command line; identical in value to
     *  {@code IClient.COMMAND_DELIMITER} ("|"). */
    public static final String DELIM = "|";

    /** Error code (index into {@link #ErrorMessages}) recorded by {@link #timeout()} when a request received no
     *  reply in time. */
    public static final int TIMEOUT = 0;
    /** Error code (index into {@link #ErrorMessages}) for a reply whose prefix did not match any known command. */
    public static final int UNKNOWN = 1;
    /** Error code (index into {@link #ErrorMessages}) for a reply that could not be parsed
     *  (see {@link #parseReply(String)}). */
    public static final int MALFORMED = 2;
    /** Highest valid error code value; kept equal to {@link #MALFORMED} since that is currently the last one
     *  defined. */
    public static final int LAST_ERROR = 2;

    /** Human-readable messages indexed by error code ({@link #TIMEOUT}, {@link #UNKNOWN}, {@link #MALFORMED}).
     *  Only populated when the command is constructed via {@link #Command(String)}; commands constructed via
     *  {@link #Command(IClient)} leave this empty. */
    public Vector<String> ErrorMessages = new Vector<>(1, 1);

    /** Client-side handle used by subclasses to reach GUI state, chat output, configuration, etc. Set by the
     *  {@link #Command(IClient)} constructor or later via {@link #setClient(IClient)}. */
    protected IClient client = null;
    /** Server-side handle used to send messages back to the connected client (see {@link #clientSend(String)}).
     *  Only meaningful for server-side instances; set via {@link #setServer(IServer)}. */
    protected IServer myServer = null;
    /** Username of the client this (server-side) command instance is currently acting on behalf of. */
    protected String username;
    /** Current error/timeout state: {@code -1} means "no error"; otherwise one of {@link #TIMEOUT},
     *  {@link #UNKNOWN}, {@link #MALFORMED} (or another subclass-defined code within {@link #ErrorMessages}'s
     *  bounds). */
    protected int error_code = -1;
    /** The dispatch key ("prefix") this command answers to, e.g. {@code "CH"}. Only set when constructed via
     *  {@link #Command(String)}; remains {@code null} for instances built with {@link #Command(IClient)}. */
    protected String myPrefix;

    /**
     * Constructs a client-side instance of this command bound to the given client. Note that this is the
     * constructor used by every subclass in this package that only ever runs client-side; it does not set
     * {@link #myPrefix}, so {@link #getPrefix()} will return {@code null} unless a subclass sets it separately.
     * Remember that your derivative must have a Constructor taking exactly one client as a
     * parameter too.
     */
    public Command(IClient client) {
        this.client = client;
    }

    /**
     * Constructs a command bound to a given dispatch prefix and initializes the standard
     * {@link #ErrorMessages} (for {@link #TIMEOUT}, {@link #UNKNOWN} and {@link #MALFORMED} in that order, matching
     * the numeric error codes). Used by the request/reply flavor of command that expects a coded reply from the
     * other side (see {@link #parseReply(String)}).
     *
     * @param prefix the dispatch key that identifies this command on the wire; stored in {@link #myPrefix} and
     *               returned by {@link #getPrefix()}.
     */
    public Command(String prefix) {
        this.myPrefix = prefix;
        this.ErrorMessages.add("This operation has timed out");
        this.ErrorMessages.add("The packet received was unknown");
        this.ErrorMessages.add("The packet received was malformed");
    }

    /**
     * Binds this (server-side) command instance to the server so it can later send messages back to a client via
     * {@link #clientSend(String)}.
     */
    public void setServer(IServer server) {
        this.myServer = server;
    }

    /**
     * Records the username of the client this (server-side) command instance is currently operating on behalf of.
     */
    public void setUsername(String name) {
        this.username = name;
    }

    /**
     * Sends raw text back to the client currently bound via {@link #setUsername(String)}, using the server handle
     * set by {@link #setServer(IServer)}.
     *
     * @param txt the raw text to send
     */
    public void clientSend(String txt) {
        this.myServer.clientSend(txt, this.username);
    }

    /**
     * Helper to decode the input string for executing. Splits {@code input} on
     * {@code IClient.COMMAND_DELIMITER} and discards the first token, which is expected to be this command's
     * dispatch prefix (e.g. {@code "CH"}) rather than actual argument data — callers of {@link #execute(String)}
     * pass the whole raw line, prefix included, so subclasses must call this method to skip past it.
     *
     * @param input The input string given to execute
     *
     * @return An StringTokenizer iterating over the tokens of input except the first one.
     *
     * @see Command#execute(String)
     */
    protected StringTokenizer decode(String input) {
        StringTokenizer st = new StringTokenizer(input, IClient.COMMAND_DELIMITER);
        st.nextToken();
        return st;
    }

    /**
     * Executes the command on the given input data. {@code input} is the entire raw protocol line as received,
     * including the leading dispatch prefix token; implementations are expected to call {@link #decode(String)}
     * (or otherwise skip the first token) before interpreting the remaining arguments. This is the primary
     * entry point invoked by the client for each incoming server line whose prefix matches this command.
     *
     * @param input the full raw line, prefix included
     */
    public abstract void execute(String input);

    /**
     * Parses a reply to a previously issued request (part of the request/reply flavor of this command family; see
     * class-level docs). Detects errors and pops the prefix: tokenizes {@code s} on the literal
     * {@code "|"} delimiter, verifies the first token matches {@link #getPrefix()} (case-insensitively) — setting
     * {@link #error_code} to {@link #MALFORMED} and returning early if the reply is empty or the prefix does not
     * match — then checks whether the next token is the literal error marker {@code "E"}, in which case the token
     * after that is parsed as the numeric {@link #error_code}. Finally, regardless of whether an error was found,
     * delegates the remainder of the string (everything after the first {@link #DELIM} character) to
     * {@link #parseReplyArgs(String)}.
     *
     * @param s the raw reply line, prefix included
     */
    //Detect Errors And Pop the Prefix
    public void parseReply(String s) {
        StringTokenizer st = new StringTokenizer(s, "|");
        if (!st.hasMoreTokens()) {
            this.error_code = Command.MALFORMED;
            return;
        }
        if (!st.nextToken().equalsIgnoreCase(this.getPrefix())) {
            this.error_code = Command.MALFORMED;
            return;
        }
        //is error
        if (st.hasMoreTokens() && st.nextToken().equalsIgnoreCase("E")) {
            if (st.hasMoreTokens()) {
                this.error_code = Integer.parseInt(st.nextToken());
            }
        }
        this.parseReplyArgs(s.substring(s.indexOf(Command.DELIM) + 1));
    }

    /**
     * @return this command's dispatch prefix, or {@code null} if it was constructed via {@link #Command(IClient)}
     *         (which never sets {@link #myPrefix}).
     */
    public String getPrefix() {
        return this.myPrefix;
    }

    /**
     * @return {@code true} if an error/timeout/malformed condition has been recorded via {@link #timeout()} or
     *         {@link #parseReply(String)} since construction or the last {@link #reset()}.
     */
    public boolean hasError() {
        return (this.error_code != -1);
    }

    /**
     * Builds a human-readable description of the current error, combining this command's {@code toString()} with
     * the message text at index {@link #error_code} in {@link #ErrorMessages}.
     *
     * @return the formatted error message, or an empty string if there is no error, {@link #error_code} is out of
     *         range, or {@link #ErrorMessages} is empty (which is always the case for commands constructed via
     *         {@link #Command(IClient)}, since only {@link #Command(String)} populates it).
     */
    public String getErrorMessage() {
        if (this.hasError() && this.error_code >= 0 && this.error_code < this.ErrorMessages.size()) {
            return String.format("%s %s", this, this.ErrorMessages.elementAt(this.error_code));
        }
        return "";
    }

    /**
     * Clears the current error state (see {@link #hasError()}) and blanks the recorded {@link #username}. Does
     * not touch {@link #ErrorMessages} or {@link #myPrefix}.
     */
    public void reset() {
        this.error_code = -1;
        this.username = "";
    }

    /**
     * Parses the argument portion (prefix already stripped) of a reply to a request this command previously sent,
     * as delegated to by {@link #parseReply(String)}.
     *
     * @param s the reply text following the first {@link #DELIM} character
     */
    public abstract void parseReplyArgs(String s);

    /**
     * Marks this command as having timed out (no reply received in time) by setting {@link #error_code} to
     * {@link #TIMEOUT}.
     */
    public void timeout() {
        this.error_code = Command.TIMEOUT;
    }

    /**
     * Sends this command as a request. Default no-op implementation; part of the request/reply flavor of this
     * command family (see class-level docs) for subclasses that need to actually transmit a request.
     *
     * @param blocking whether the send should block until complete
     */
    public void send(boolean blocking) {

    }

    /**
     * Rebinds this command instance to a different client handle.
     */
    public void setClient(IClient client) {
        this.client = client;
    }

    /**
     * Registry of {@link ICommand}s keyed by their dispatch prefix ({@link ICommand#getPrefix()}). Used
     * server-side ({@code MWServ}) as the table consulted to find the {@link ServerCommand} matching an inbound
     * line's leading token. As of this documentation pass, no code populating this table via {@link #put(ICommand)}
     * was found elsewhere in the codebase — the table is declared and read but appears otherwise unwired.
     */
    public static class Table extends Hashtable<String, ICommand> {
        @Serial
        private static final long serialVersionUID = -4187092531938444178L;

        /**
         * Stores {@code command} keyed by its own {@link ICommand#getPrefix()}, rather than requiring the caller
         * to supply a separate key.
         */
        public void put(ICommand command) {
            super.put(command.getPrefix(), command);
        }
    }
}
