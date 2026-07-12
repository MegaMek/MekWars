package mekwars.common.util.unitdamage;

import megamek.common.units.Entity;

/**
 * {@link AbstractUnitDamageHandler} strategy for Aerospace units (Aero and its subtypes:
 * conventional fighters, aerospace fighters, small craft, dropships, etc., as recognized by
 * {@code entity instanceof }{@link megamek.common.units.Aero}).
 * <p>
 * Aerospace-specific accounting (structural integrity, fuel, thruster/heat-sink criticals,
 * armor facings that differ from Mek/vehicle locations, etc.) has not been implemented yet.
 * Both methods are currently no-ops/placeholders: {@link #buildDamageString} always returns the
 * sentinel empty-sections string {@code "%%-%%-%%"} (no external armor, no internal structure,
 * no crits, no ammo section), and {@link #applyDamageString} does nothing at all, so applying a
 * report to an Aero unit currently has no effect on its state.
 *
 * @see UnitDamageHandlerFactory#getHandler(Entity)
 * @see MekDamageHandler
 */
public class AeroDamageHandler extends AbstractUnitDamageHandler {
    /**
     * Not yet implemented for Aerospace units.
     *
     * @param unit     ignored
     * @param sendAmmo ignored
     * @return the sentinel empty damage string {@code "%%-%%-%%"}
     */
    @Override
    public String buildDamageString(Entity unit, boolean sendAmmo) {
        // Damage to this unit type not yet implemented
        return "%%-%%-%%";
    }

    /**
     * Not yet implemented for Aerospace units. Does nothing; {@code unit} is left unmodified
     * regardless of the contents of {@code report}.
     *
     * @param unit        ignored
     * @param report      ignored
     * @param isRepairing ignored
     */
    @Override
    public void applyDamageString(Entity unit, String report, boolean isRepairing) {
        // Damage to this unit type not yet implemented
        return;
    }
}
