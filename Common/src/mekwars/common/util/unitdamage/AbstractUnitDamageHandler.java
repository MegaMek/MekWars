package mekwars.common.util.unitdamage;

import megamek.common.units.Entity;

/**
 * Strategy/template base class for encoding and decoding a unit's battle damage as a compact,
 * delimited "damage string" used by MekWars to persist and transmit post-battle
 * repair/salvage state.
 * <p>
 * MekWars does not serialize the full {@link Entity} state for repair bookkeeping; instead each
 * unit type produces (via {@link #buildDamageString}) and consumes (via {@link #applyDamageString})
 * a compact text representation covering armor, internal structure, critical hit/repair
 * status, and (optionally) remaining ammunition. This lets the repair bay UI and the
 * server-side campaign persistence layer store/replay damage without needing a full unit
 * snapshot.
 * <p>
 * Concrete subclasses implement the unit-type-specific encoding rules (the location indices,
 * whether rear/turret armor exists, whether crit slots even apply, etc.). Instances are
 * obtained through {@link UnitDamageHandlerFactory#getHandler(Entity)}, which inspects the
 * runtime type of the {@link Entity} and picks the matching handler:
 * <ul>
 * <li>{@link MekDamageHandler} - BattleMeks (fully implemented, most complex)</li>
 * <li>{@link VehicleDamageHandler} - Combat vehicles/tanks (fully implemented)</li>
 * <li>{@link AeroDamageHandler} - Aerospace units (stub, not yet implemented)</li>
 * <li>{@link BattleArmorDamageHandler} - Battle armor (stub, not yet implemented)</li>
 * <li>{@link InfantryDamageHandler} - Conventional infantry (stub, not yet implemented)</li>
 * <li>{@link ProtoDamageHandler} - ProtoMeks (stub, not yet implemented)</li>
 * <li>{@link GenericDamageHandler} - Fallback for any unrecognized entity type</li>
 * </ul>
 *
 * @see UnitDamageHandlerFactory
 */
public abstract class AbstractUnitDamageHandler {

    /**
     * Default no-arg constructor. Subclasses are stateless; a new handler instance is created
     * per call to {@link UnitDamageHandlerFactory#getHandler(Entity)}.
     */
    public AbstractUnitDamageHandler() {
    }

    /**
     * Encodes the current damage/repair state of {@code unit} (armor, internal structure,
     * critical slot status, and optionally ammo) into a compact delimited string suitable for
     * storage or transmission, and later replay via {@link #applyDamageString}.
     *
     * @param unit     the entity whose current damage state should be captured
     * @param sendAmmo whether remaining ammunition counts should also be included in the
     *                 resulting string
     * @return an implementation-defined delimited damage string; the exact format is
     *         unit-type-specific and only guaranteed to round-trip through the same
     *         handler's {@link #applyDamageString}
     */
    public abstract String buildDamageString(Entity unit, boolean sendAmmo);

    /**
     * Parses a damage string previously produced by {@link #buildDamageString} and applies the
     * encoded armor, internal structure, critical slot, and ammo state back onto {@code unit}.
     *
     * @param unit        the entity to mutate in place
     * @param report      the delimited damage string to parse, as produced by
     *                    {@link #buildDamageString}
     * @param isRepairing {@code true} when this application represents a repair-bay action
     *                    (e.g. re-flagging equipment as under repair rather than fully
     *                    clearing outstanding repair-queue bookkeeping); {@code false} when
     *                    simply restoring/recording damage
     */
    public abstract void applyDamageString(Entity unit, String report, boolean isRepairing);
}
