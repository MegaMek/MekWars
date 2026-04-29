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
package mekwars.common.campaign.clientutils.protocol.commands;

import mekwars.common.campaign.clientutils.protocol.CConnector;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Abstract class for protocol Commands
 */

public abstract class CProtCommand implements IProtCommand {
    String name = "";
    String prefix;
    String delimiter;
    IClient client;
    CConnector Connector;

    public CProtCommand(IClient client) {
        this.client = client;
        Connector = client.getConnector();
        prefix = IClient.PROTOCOL_PREFIX;
        delimiter = IClient.PROTOCOL_DELIMITER;
    }

    public IClient getClient() {
        return client;
    }

    public void setClient(IClient c) {
        client = c;
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

    public boolean check(String tname) {
        if (tname.startsWith(prefix)) {
            tname = tname.substring(prefix.length());
        }

        return (name.equals(tname));
    }

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

    // echo command in GUI
    protected void echo(String input) {}

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
