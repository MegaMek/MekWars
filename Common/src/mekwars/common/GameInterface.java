package mekwars.common;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import megamek.common.units.Entity;

/**
 * Read-only contract for querying the outcome and unit state of a completed (or in-progress) MegaMek game/battle,
 * without exposing the full MegaMek {@code Game} API to MekWars callers.
 * <p>
 * {@link GameWrapper} is the sole production implementation, delegating each method to an underlying MegaMek
 * {@code Game} instance. Server- and client-side battle result processing code (e.g. victory/loss handling, unit
 * salvage/loss bookkeeping) depends on this interface rather than on MegaMek's {@code Game} class directly.
 *
 * @author Helge Richter
 */
public interface GameInterface {

    /**
     * @return the names of the players belonging to the winning team, or an empty list if there is no winner (see
     *       {@link #hasWinner()}).
     */
    List<String> getWinners();

    /**
     * @return {@code true} if the game has a victorious team (i.e. a team other than "no team" won).
     */
    boolean hasWinner();

    /**
     * @return the entities that were destroyed/devastated during the game.
     */
    Enumeration<Entity> getDevastatedEntities();

    /**
     * @return the entities that ended up in the "graveyard" (destroyed and removed from play).
     */
    Enumeration<Entity> getGraveyardEntities();

    /**
     * @return an iterator over all entities that took part in the game, regardless of outcome.
     */
    Iterator<Entity> getEntities();

    /**
     * @return the entities whose owning side retreated/withdrew from the game.
     */
    Enumeration<Entity> getRetreatedEntities();

}
