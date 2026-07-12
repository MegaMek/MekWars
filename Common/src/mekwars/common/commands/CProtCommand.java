/*
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

import mekwars.common.campaign.clientutils.protocol.CConnector;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Abstract base class for protocol commands that implement {@link IProtCommand} — a separate, lighter-weight
 * command family from {@link Command}/{@link ICommand}, apparently used for lower-level protocol-connector
 * commands (as opposed to the higher-level chat/campaign commands handled by {@code Command} subclasses). Each
 * concrete subclass is expected to represent one named protocol command, identified by {@link #name}, and is
 * matched against incoming input via {@link #check(String)} after stripping a shared {@link #prefix}.
 * <p>
 * The default {@link #execute(String)} implementation is a no-op that always reports success; subclasses are
 * expected to override it to actually do something.
 */

public abstract class CProtCommand implements IProtCommand {
    /** The bare command name/alias this instance responds to (without {@link #prefix}). Defaults to empty. */
    String name = "";
    /** The prefix expected before the command name on the wire, e.g. {@code IClient.PROTOCOL_PREFIX}. */
    String prefix;
    /** The token delimiter used within this command's arguments, e.g. {@code IClient.PROTOCOL_DELIMITER}. */
    String delimiter;
    /** The client this command operates against. */
    IClient client;
    /** The connector used to talk to the client/server; obtained from {@link IClient#getConnector()}. */
    CConnector Connector;

    /**
     * Creates the command bound to the given client, initializing {@link #prefix} and {@link #delimiter} from the
     * client's protocol-level constants and caching the client's {@link CConnector}.
     *
     * @param client the client this command will operate against
     */
    public CProtCommand(IClient client) {
        this.client = client;
        Connector = client.getConnector();
        prefix = IClient.PROTOCOL_PREFIX;
        delimiter = IClient.PROTOCOL_DELIMITER;
    }

    public IClient getClient() {
        return client;
    }

    public void setClient(IClient client) {
        this.client = client;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public CConnector getConnector() {
        return Connector;
    }

    public void setConnector(CConnector connector) {
        Connector = connector;
    }

    /**
     * Determines whether {@code tokenName} identifies this command. If {@code tokenName} begins with
     * {@link #prefix}, the prefix is stripped before comparing; the remainder must then exactly equal
     * {@link #name} (case-sensitive).
     *
     * @param tokenName the candidate command token, with or without the protocol prefix
     *
     * @return {@code true} if this command's {@link #name} matches the (prefix-stripped) token
     */
    public boolean check(String tokenName) {
        if (tokenName.startsWith(prefix)) {
            tokenName = tokenName.substring(prefix.length());
        }

        return (name.equals(tokenName));
    }

    /**
     * Default no-op execution: subclasses should override this to perform the command's actual behavior. As
     * written here, it always reports success without doing anything.
     *
     * @param input the raw input line for this command
     *
     * @return always {@code true} in this base implementation
     */
    // execute command
    public boolean execute(String input) {
        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Hook for echoing the command's input back into the GUI (e.g. showing what was typed/received). Default
     * implementation intentionally does nothing; subclasses may override.
     *
     * @param input the raw input line to echo
     */
    // echo command in GUI
    protected void echo(String input) {}

    /**
     * Strips a leading protocol {@link #prefix} and, if still present after that, a leading {@link #name} from
     * {@code input}, trimming whitespace after each removal. Used by subclasses to reduce a full protocol line
     * down to just its argument portion.
     *
     * @param input the raw input line, potentially prefixed with {@link #prefix} and/or {@link #name}
     *
     * @return {@code input} with the leading prefix and command name removed (if present) and surrounding
     *       whitespace trimmed after each removal
     */
    // remove prefix and name/alias from input
    protected String decompose(String input) {
        if (input.startsWith(prefix)) {
            input = input.substring(prefix.length()).trim();
        }

        if (input.startsWith(name)) {
            input = input.substring(name.length()).trim();
        }

        return input;
    }

}
