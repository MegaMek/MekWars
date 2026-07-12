package mekwars.common.gui.dialogs.customUnits;

import java.io.Serial;
import java.util.Vector;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import mekwars.common.gui.dialogs.CustomUnitDialog;

/**
 * When a Protomech selects ammo, you need to adjust the shots on the unit for the weight of the selected munition.
 *
 * <p>ProtoMek ammo bins have a fixed physical volume/weight rather than a fixed shot count, so switching
 * to a munition type with a different shots-per-ton value (e.g. a heavier special munition) must rescale
 * the number of shots the bin actually holds. This subclass layers that rescaling on top of the standard
 * {@link MunitionChoicePanel} ammo-selection behavior.
 */
public class ProtoMekMunitionChoicePanel extends MunitionChoicePanel {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 3984045407240841489L;
    /** Shots remaining in the bin at the time this panel was constructed, before any new choice is applied. */
    private final float m_origShotsLeft;
    /** Ammo type that was loaded in the bin at the time this panel was constructed. */
    private final megamek.common.equipment.AmmoType m_origAmmo;

    /**
     * Captures the mount's original ammo type and shot count so {@link #applyChoice()} can later
     * compute the correct rescaled shot count for whatever munition the player selects.
     */
    public ProtoMekMunitionChoicePanel(CustomUnitDialog customUnitDialog, Mounted<?> m, Vector<AmmoType> vTypes,
          int location) {
        super(customUnitDialog, m, vTypes, location);
        m_origAmmo = (megamek.common.equipment.AmmoType) m.getType();
        m_origShotsLeft = m.getUsableShotsLeft();
    }

    /**
     * All ammo must be applied in ratios to the starting load.
     *
     * <p>After delegating to {@link MunitionChoicePanel#applyChoice()} (which sends the server command
     * and, if dumping, zeroes the local shot count), this rescales the mount's local shot count by the
     * ratio of the original ammo's shots-per-ton to the newly selected ammo's shots-per-ton, applied
     * against the original shots-left. Note some special munitions are twice as heavy as standard ammo
     * and so carry half the shots (rounded down/nearest via {@code Math.round}). If the dump checkbox
     * is selected, the shot count is forced back to 0 after the rescale (a defensive re-assertion, since
     * the superclass's dump handling should already have left the pre-rescale value at 0).
     */
    @Override
    public void applyChoice() {
        super.applyChoice();

        // Calculate the number of shots for the new ammo.
        // N.B. Some special ammos are twice as heavy as normal
        // so they have half the number of shots (rounded down).
        setShotsLeft(Math.round((getShotsLeft() * m_origShotsLeft) / m_origAmmo.getShots()));
        if (chDump.isSelected()) {
            setShotsLeft(0);
        }
    }
}
