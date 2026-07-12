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
package mekwars.common;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Adapts a MegaMek {@link Game} instance to the narrower {@link GameInterface} contract used by MekWars.
 * <p>
 * This class holds a reference to the live/finished MegaMek game and translates queries about winners and unit
 * fate (devastated, graveyarded, retreated) into terms MekWars campaign code can consume, without leaking the full
 * MegaMek {@code Game} API surface to callers on the MekWars side.
 *
 * @author Helge Richter
 */
public class GameWrapper implements GameInterface {
    private static final MMLogger LOGGER = MMLogger.create(GameWrapper.class);
    /** The underlying MegaMek game this wrapper delegates to. */
    private final Game game;

    /**
     * @param game the MegaMek game to wrap.
     */
    public GameWrapper(Game game) {
        this.game = game;
    }

    /**
     * Determines the winning side's player names by comparing each player's team to
     * {@link Game#getVictoryTeam()}.
     *
     * @return trimmed names of all players on the victorious team, or an empty list if there is no winning team.
     */
    public List<String> getWinners() {
        ArrayList<String> result = new ArrayList<>();
        //TODO: Winners sometimes coming up empty. Let's see why

        List<Player> playersList = game.getPlayersList();

        LOGGER.debug("  :: game.getPlayers(): {}", playersList.toString());
        LOGGER.debug("  :: VictoryTeam: {}", game.getVictoryTeam());

        for (Player player : playersList) {
            LOGGER.debug("  :: ==> Player: {} :: Team: {}", player.getName().trim(), player.getTeam());

            if (player.getTeam() == game.getVictoryTeam()) {
                result.add(player.getName().trim());
            }
        }

        return result;
    }

    /**
     * @return {@code true} if the game's victory team is not {@link Player#TEAM_NONE}.
     */
    public boolean hasWinner() {
        return game.getVictoryTeam() != Player.TEAM_NONE;
    }

    /**
     * @return the entities MegaMek marked as devastated (destroyed beyond salvage) during the game.
     */
    public Enumeration<Entity> getDevastatedEntities() {
        return game.getDevastatedEntities();
    }

    /**
     * @return the entities MegaMek moved to the graveyard (destroyed/removed from play).
     */
    public Enumeration<Entity> getGraveyardEntities() {
        return game.getGraveyardEntities();
    }

    /**
     * @return an iterator over every entity that participated in the game.
     */
    public Iterator<Entity> getEntities() {
        return game.getEntitiesVector().iterator();
    }

    /**
     * @return the entities whose side retreated/withdrew from the game.
     */
    public Enumeration<Entity> getRetreatedEntities() {
        return game.getRetreatedEntities();
    }


}
