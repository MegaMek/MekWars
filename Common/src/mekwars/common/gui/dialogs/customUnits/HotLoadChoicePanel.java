package mekwars.common.gui.dialogs.customUnits;

import java.io.Serial;

import megamek.common.equipment.Mounted;
import mekwars.common.gui.dialogs.CustomUnitDialog;

/**
 * A small sub-panel embedded in the {@link CustomUnitDialog} that lets a player toggle "hot-loading"
 * for a single ammo-carrying weapon mount (the TacOps rule where an ammo bin is rigged to boost the
 * weapon's damage at the risk of a more severe ammo explosion on a critical hit). One instance of this
 * panel is created per hot-load-capable mount on the unit being customized; it renders as a single
 * checkbox labeled with the weapon's name and its location abbreviation (e.g. "Hot-Load LRM 20 (RT)").
 */
public class HotLoadChoicePanel extends javax.swing.JPanel {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -4801226845131401403L;
    /** Parent dialog; used to look up the entity being customized and to send the change to the server. */
    private final CustomUnitDialog customUnitDialog;
    /** The specific weapon/ammo mount this panel controls the hot-load flag for. */
    private final Mounted<?> m_mounted;
    /** Body location index (see {@code Entity.LOC_*}) of the mount, used only for the checkbox label. */
    private final int location;
    /** Checkbox reflecting/controlling whether {@link #m_mounted} is hot-loaded. */
    protected javax.swing.JCheckBox chHotLoad = new javax.swing.JCheckBox();

    /**
     * Builds the panel and initializes the checkbox from the mount's current hot-load state.
     *
     * @param customUnitDialog owning dialog, used to read the entity and to send server commands
     * @param m the weapon/ammo mount this panel toggles hot-loading for
     * @param location body location index of {@code m}, used for the checkbox label
     */
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

    /**
     * Sends the hot-load toggle to the server, but only if the checkbox state actually differs from the
     * mount's current hot-loaded state (i.e. the user changed it). Nothing is sent otherwise, so calling
     * this repeatedly without a state change is a no-op. The server-bound message is a campaign-chat
     * command of the form {@code <prefix>c setunithotload#<externalId>#<location>#<selected>}.
     */
    public void applyChoice() {
        if (m_mounted.isHotLoaded() != chHotLoad.isSelected()) {
            customUnitDialog.getClient().sendChat(
                  String.format("%sc setunithotload#%s#%s#%s", mekwars.common.campaign.clientutils.protocol.IClient.CAMPAIGN_PREFIX, customUnitDialog.getEntity()
                                                                                                                       .getExternalId(), location, chHotLoad.isSelected()));
        }
    }

    /**
     * Enables/disables the underlying checkbox (e.g. while the dialog is read-only or waiting on the server).
     */
    @Override
    public void setEnabled(boolean enabled) {
        chHotLoad.setEnabled(enabled);
    }
}
