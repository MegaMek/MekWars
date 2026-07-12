package mekwars.common.util.unitdamage;

import megamek.common.units.Entity;

/**
 * Fallback {@link AbstractUnitDamageHandler} strategy used by
 * {@link UnitDamageHandlerFactory#getHandler(Entity)} when the supplied {@link Entity} doesn't
 * match any of the recognized MegaMek unit types (Mek, Tank, Aero, BattleArmor, ProtoMek,
 * Infantry). The factory logs an error before returning this handler, so its use generally
 * indicates either an unsupported/unexpected {@code Entity} subclass or a new unit type added
 * to MegaMek that this package has not yet been updated to handle.
 * <p>
 * Both methods are unconditional no-ops: {@link #buildDamageString} always returns the sentinel
 * empty-sections string {@code "%%-%%-%%"}, and {@link #applyDamageString} does nothing.
 *
 * @see UnitDamageHandlerFactory#getHandler(Entity)
 */
public class GenericDamageHandler extends AbstractUnitDamageHandler {

    /**
     * Always returns the sentinel empty damage string; this handler does not know how to
     * inspect {@code unit}'s damage state.
     *
     * @param unit     ignored
     * @param sendAmmo ignored
     * @return the sentinel empty damage string {@code "%%-%%-%%"}
     */
    @Override
    public String buildDamageString(Entity unit, boolean sendAmmo) {
        // Nothing really to do here.  We've not implemented whatever
        // unit type this is attached to
        return "%%-%%-%%";
    }

    /**
     * Does nothing; {@code unit} is left unmodified regardless of the contents of
     * {@code report}. Per the inline comment, units routed to this handler are assumed not to
     * take damage in a way this package tracks.
     *
     * @param unit        ignored
     * @param report      ignored
     * @param isRepairing ignored
     */
    @Override
    public void applyDamageString(Entity unit, String report, boolean isRepairing) {
        // Nothing to do here - this type doesn't take damage
        return;
    }

}
