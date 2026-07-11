package mekwars.common.gui.dialogs.customUnits;

import megamek.common.equipment.Mounted;
import mekwars.common.gui.dialogs.CustomUnitDialog;

public class MachineGunChoicePanel extends javax.swing.JPanel {

    /**
     *
     */
    @java.io.Serial
    private static final long serialVersionUID = -2207765894385312209L;
    private final CustomUnitDialog customUnitDialog;
    private final Mounted<?> m_mounted;
    private final int location;
    private final int slot;
    protected javax.swing.JCheckBox chBurst = new javax.swing.JCheckBox();

    public MachineGunChoicePanel(CustomUnitDialog customUnitDialog, Mounted<?> mounted, int location, int slot) {
        this.customUnitDialog = customUnitDialog;

        // store params
        m_mounted = mounted;
        this.location = location;
        this.slot = slot;

        // restore previous setting
        chBurst.setSelected(m_mounted.isRapidFire());

        // setup
        chBurst.setText(String.format("Rapid Fire MG (%s)", customUnitDialog.getEntity().getLocationAbbr(location)));
        add(chBurst);
    }

    /*
     * Yes this to load the ammo Save Weapon Type from at.getAmmoType() call
     * weapon type save weapon position with at.getMunitionType() call ammo
     * type Load at.getMunitionsFor(ammoType) returns vector
     * ammo_vector.elementAt(MunitionType);
     */

    public void applyChoice() {
        if (m_mounted.isRapidFire() != chBurst.isSelected()) {
            customUnitDialog.getClient().sendChat(
                  String.format("%sc setunitburst#%s#%s#%s#%s", mekwars.common.campaign.clientutils.protocol.IClient.CAMPAIGN_PREFIX, customUnitDialog.getEntity()
                                                                                                                     .getExternalId(), location, slot, chBurst.isSelected()));
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        chBurst.setEnabled(enabled);
    }
}
