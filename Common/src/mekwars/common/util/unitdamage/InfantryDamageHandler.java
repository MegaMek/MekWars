package mekwars.common.util.unitdamage;

import megamek.common.units.Entity;

/**
 * {@link AbstractUnitDamageHandler} strategy for conventional Infantry platoons (as recognized
 * by {@code entity instanceof }{@link megamek.common.units.Infantry}, which also matches
 * {@link megamek.common.battleArmor.BattleArmor} - see the factory's type-check ordering in
 * {@link UnitDamageHandlerFactory#getHandler(Entity)}, which checks {@code BattleArmor} first).
 * <p>
 * Conventional infantry damage is tracked as trooper/squad casualties rather than the
 * armor/internal-structure-per-location and critical-slot model used by Meks and vehicles, so it
 * cannot reuse {@link MekDamageHandler}'s or {@link VehicleDamageHandler}'s encoding scheme.
 * That casualty accounting has not been implemented yet: both methods are currently
 * no-ops/placeholders. {@link #buildDamageString} always returns the sentinel empty-sections
 * string {@code "%%-%%-%%"}, and {@link #applyDamageString} does nothing, so applying a report
 * to an infantry unit currently has no effect on its state.
 *
 * @see UnitDamageHandlerFactory#getHandler(Entity)
 * @see MekDamageHandler
 */
public class InfantryDamageHandler extends AbstractUnitDamageHandler {
    /**
     * Not yet implemented for Infantry units.
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
     * Not yet implemented for Infantry units. Does nothing; {@code unit} is left unmodified
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
