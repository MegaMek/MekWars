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
package mekwars.common.campaign.clientutils;

import megamek.common.event.GameCFREvent;

/**
 * Narrow interface exposing the parts of a game-hosting client (one that is running a MegaMek {@code Server}
 * embedded in the MekWars client/dedicated-host process) that other components need: status changes, permission
 * checks, and the current username. Implemented by the main client classes (e.g. {@code MWClient},
 * {@code MWDedHost}), both of which also extend {@link GameHost}, which supplies default implementations of most of
 * these methods.
 */
public interface IGameHost {

    /**
     * Changes the host's published status (see the {@code STATUS_*} constants on
     * {@link mekwars.common.campaign.clientutils.protocol.IClient}) and propagates the change, e.g. to the server.
     *
     * @param newStatus the new status code
     */
    void changeStatus(int newStatus);

    /**
     * @return true if the current user's level qualifies them as a server administrator.
     */
    boolean isAdmin();

    /**
     * @return true if the current user's level qualifies them as at least a moderator (admins also satisfy this).
     */
    boolean isMod();

    /**
     * @return the username of the account currently running this host.
     */
    String getUsername();

    /**
     * Called by the MegaMek game engine ({@link megamek.common.event.GameListener}) when the server needs feedback
     * from a client during play (a Client Feedback Request), e.g. to pick a target or confirm an action.
     *
     * @param arg0 the feedback-request event describing what input is needed
     */
    void gameClientFeedbackRequest(GameCFREvent arg0);
}
