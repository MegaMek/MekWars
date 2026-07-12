package mekwars.common.util.unitdamage;

import megamek.common.units.Entity;

/**
 * {@link AbstractUnitDamageHandler} strategy for ProtoMeks (as recognized by
 * {@code entity instanceof }{@link megamek.common.units.ProtoMek}).
 * <p>
 * ProtoMeks have their own armor/internal-structure location layout and critical hit table
 * distinct from full-size Meks, so this cannot simply delegate to {@link MekDamageHandler}.
 * That unit-type-specific accounting has not been implemented yet: both methods are currently
 * no-ops/placeholders. {@link #buildDamageString} always returns the sentinel empty-sections
 * string {@code "%%-%%-%%"}, and {@link #applyDamageString} does nothing, so applying a report
 * to a ProtoMek currently has no effect on its state.
 *
 * @see UnitDamageHandlerFactory#getHandler(Entity)
 * @see MekDamageHandler
 */
public class ProtoDamageHandler extends AbstractUnitDamageHandler {

    /**
     * Not yet implemented for ProtoMek units.
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
     * Not yet implemented for ProtoMek units. Does nothing; {@code unit} is left unmodified
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
