package mekwars.common.gui.dialogs.customUnits;

import java.io.Serial;
import java.util.Vector;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import mekwars.common.gui.dialogs.CustomUnitDialog;

/**
 * When a Protomech selects ammo, you need to adjust the shots on the unit for the weight of the selected munition.
 */
public class ProtoMekMunitionChoicePanel extends MunitionChoicePanel {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 3984045407240841489L;
    private final float m_origShotsLeft;
    private final megamek.common.equipment.AmmoType m_origAmmo;

    public ProtoMekMunitionChoicePanel(CustomUnitDialog customUnitDialog, Mounted<?> m, Vector<AmmoType> vTypes,
          int location) {
        super(customUnitDialog, m, vTypes, location);
        m_origAmmo = (megamek.common.equipment.AmmoType) m.getType();
        m_origShotsLeft = m.getUsableShotsLeft();
    }

    /**
     * All ammo must be applied in ratios to the starting load.
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
