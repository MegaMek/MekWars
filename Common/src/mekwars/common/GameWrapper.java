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

public class GameWrapper implements GameInterface {
    private static final MMLogger LOGGER = MMLogger.create(GameWrapper.class);
    private final Game game;

    public GameWrapper(Game game) {
        this.game = game;
    }

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

    public boolean hasWinner() {
        return game.getVictoryTeam() != Player.TEAM_NONE;
    }

    public Enumeration<Entity> getDevastatedEntities() {
        return game.getDevastatedEntities();
    }

    public Enumeration<Entity> getGraveyardEntities() {
        return game.getGraveyardEntities();
    }

    public Iterator<Entity> getEntities() {
        return game.getEntitiesVector().iterator();
    }

    public Enumeration<Entity> getRetreatedEntities() {
        return game.getRetreatedEntities();
    }


}
