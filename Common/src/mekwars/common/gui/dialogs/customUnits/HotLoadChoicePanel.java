package mekwars.common.gui.dialogs.customUnits;

import java.io.Serial;

import megamek.common.equipment.Mounted;
import mekwars.common.gui.dialogs.CustomUnitDialog;

public class HotLoadChoicePanel extends javax.swing.JPanel {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -4801226845131401403L;
    private final CustomUnitDialog customUnitDialog;
    private final Mounted<?> m_mounted;
    private final int location;
    protected javax.swing.JCheckBox chHotLoad = new javax.swing.JCheckBox();

    public HotLoadChoicePanel(CustomUnitDialog customUnitDialog, Mounted<?> m, int location) {
        this.customUnitDialog = customUnitDialog;

        // store params
        m_mounted = m;
        this.location = location;

        // mount hodler int
        int loc;
        loc = m.getLocation();

        // restore previous setting
        chHotLoad.setSelected(m_mounted.isHotLoaded());

        // setup
        chHotLoad.setText(String.format("Hot-Load %s (%s)", m_mounted.getName(), customUnitDialog.getEntity().getLocationAbbr(loc)));
        add(chHotLoad);
    }

    /*
     * Yes this to load the ammo Save Weapon Type from at.getAmmoType() call
     * weapon type save weapon position with at.getMunitionType() call ammo
     * type Load at.getMunitionsFor(ammoType) returns vector
     * ammo_vector.elementAt(MunitionType);
     */

    public void applyChoice() {
        if (m_mounted.isHotLoaded() != chHotLoad.isSelected()) {
            customUnitDialog.getClient().sendChat(
                  String.format("%sc setunithotload#%s#%s#%s", mekwars.common.campaign.clientutils.protocol.IClient.CAMPAIGN_PREFIX, customUnitDialog.getEntity()
                                                                                                                       .getExternalId(), location, chHotLoad.isSelected()));
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        chHotLoad.setEnabled(enabled);
    }
}
