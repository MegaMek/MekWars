/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
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

import java.util.StringTokenizer;

import megamek.codeUtilities.MathUtility;
import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Client-side handler for the {@code PI} ("Player Info") protocol message, sent by the server to push
 * incremental updates about one or more users' campaign-related info (campaign data blob, status, fluff text,
 * sub-faction name, experience, or rating). Executing it dispatches on a sub-command code to update the matching
 * {@link CUser} entries in the client's user list, then refreshes the user-list GUI exactly once regardless of
 * how many users/fields were touched.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class PI extends Command {

    /**
     *
     */
    public PI(IClient client) {
        super(client);
    }

    /**
     * Reads a sub-command code and then, per code, one or more {@code username, value} pairs, looking up each
     * named user via {@link IClient#getUser(String)} and applying the update if the user is found (unknown
     * usernames are silently skipped):
     * <ul>
     *     <li>{@code PL} - "Player List": loops over <em>all remaining</em> username/campaign-data pairs in the
     *     payload, calling {@link CUser#setCampaignData(IClient, String)} for each — the only sub-command that
     *     processes more than one user per message.</li>
     *     <li>{@code DA} - "Data": sets a single user's campaign data; if that user happens to be the local
     *     player, also re-enables the main frame's menu (e.g. because campaign data becoming available unlocks
     *     menu actions).</li>
     *     <li>{@code ChangeStatusCommand} - sets a single user's numeric status, parsed with
     *     {@link MathUtility#parseInt(String, int)} defaulting to {@code 0} on failure.</li>
     *     <li>{@code FT} - "Fluff Text": sets a single user's fluff text, only if a value token is actually
     *     present.</li>
     *     <li>{@code SSN} - "Set Sub-faction Name": sets a single user's sub-faction name, only if a value token
     *     is present.</li>
     *     <li>{@code EX} - "Experience": sets a single user's experience, parsed as an int (default {@code 0}),
     *     only if a value token is present.</li>
     *     <li>{@code RA} - "Rating": sets a single user's rating, parsed as a float (default {@code 0.0f}), only
     *     if a value token is present.</li>
     * </ul>
     * An unrecognized sub-command code is silently ignored. In every case (including an unrecognized code), the
     * user-list GUI is refreshed once at the end of the method.
     *
     * @param input the raw, delimited protocol line for this command; see {@link Command#decode(String)}
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        String task = stringTokenizer.nextToken();
        CUser user;
        switch (task) {
            case "PL" -> {
                while (stringTokenizer.hasMoreTokens()) {
                    user = (CUser) client.getUser(stringTokenizer.nextToken());
                    if (user != null) {
                        user.setCampaignData(client, stringTokenizer.nextToken());
                    }
                }
            }
            case "DA" -> {
                user = (CUser) client.getUser(stringTokenizer.nextToken());
                if (user != null) {
                    user.setCampaignData(client, stringTokenizer.nextToken());
                    if (user.getName().equalsIgnoreCase(client.getPlayer().getName())) {
                        client.getMainFrame().enableMenu();
                    }
                }
            }
            case "ChangeStatusCommand" -> {
                user = (CUser) client.getUser(stringTokenizer.nextToken());
                if (user != null) {
                    user.setStatus(MathUtility.parseInt(stringTokenizer.nextToken(), 0));
                }
            }
            case "FT" -> {
                user = (CUser) client.getUser(stringTokenizer.nextToken());
                if (user != null && stringTokenizer.hasMoreTokens()) {
                    user.setFluff(stringTokenizer.nextToken());
                }
            }
            case "SSN" -> {
                user = (CUser) client.getUser(stringTokenizer.nextToken());
                if (user != null && stringTokenizer.hasMoreTokens()) {
                    user.setSubFactionName(stringTokenizer.nextToken());
                }
            }
            case "EX" -> {
                user = (CUser) client.getUser(stringTokenizer.nextToken());
                if (user != null && stringTokenizer.hasMoreTokens()) {
                    user.setExp(MathUtility.parseInt(stringTokenizer.nextToken(), 0));
                }
            }
            case "RA" -> {
                user = (CUser) client.getUser(stringTokenizer.nextToken());
                if (user != null && stringTokenizer.hasMoreTokens()) {
                    user.setRating(MathUtility.parseFloat(stringTokenizer.nextToken(), 0.0f));
                }
            }
        }

        client.refreshGUI(IClient.REFRESH_USERLIST);
    }

    /**
     * Unused on the client side; this command has no reply-argument parsing behavior.
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * Unused on the client side; this command is never parsed as server-bound arguments.
     */
    @Override
    public void parseArguments(String s) {

    }
}
