package mekwars.common.gui.dialogs.customUnits;

import megamek.common.equipment.Mounted;
import mekwars.common.gui.dialogs.CustomUnitDialog;

/**
 * A small sub-panel embedded in the {@link CustomUnitDialog} that lets a player toggle "rapid fire"
 * (burst-fire) mode for a single machine gun mount, per TacOps rules. One instance is created per
 * machine gun on the unit being customized; it renders as a single checkbox labeled with the mount's
 * location abbreviation (e.g. "Rapid Fire MG (RA)").
 */
public class MachineGunChoicePanel extends javax.swing.JPanel {

    /**
     *
     */
    @java.io.Serial
    private static final long serialVersionUID = -2207765894385312209L;
    /** Parent dialog; used to look up the entity being customized and to send the change to the server. */
    private final CustomUnitDialog customUnitDialog;
    /** The specific machine gun mount this panel controls the rapid-fire flag for. */
    private final Mounted<?> m_mounted;
    /** Body location index (see {@code Entity.LOC_*}) of the mount, used for the checkbox label and the server command. */
    private final int location;
    /** Critical-slot index of the mount within {@link #location}, needed because a location may hold multiple machine guns. */
    private final int slot;
    /** Checkbox reflecting/controlling whether {@link #m_mounted} is set to rapid fire. */
    protected javax.swing.JCheckBox chBurst = new javax.swing.JCheckBox();

    /**
     * Builds the panel and initializes the checkbox from the mount's current rapid-fire state.
     *
     * @param customUnitDialog owning dialog, used to read the entity and to send server commands
     * @param mounted the machine gun mount this panel toggles rapid fire for
     * @param location body location index of {@code mounted}
     * @param slot critical-slot index of {@code mounted} within {@code location}
     */
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

    /**
     * Sends the rapid-fire toggle to the server, but only if the checkbox state actually differs from the
     * mount's current rapid-fire state (i.e. the user changed it). Nothing is sent otherwise. The
     * server-bound message is a campaign-chat command of the form
     * {@code <prefix>c setunitburst#<externalId>#<location>#<slot>#<selected>}.
     */
    public void applyChoice() {
        if (m_mounted.isRapidFire() != chBurst.isSelected()) {
            customUnitDialog.getClient().sendChat(
                  String.format("%sc setunitburst#%s#%s#%s#%s", mekwars.common.campaign.clientutils.protocol.IClient.CAMPAIGN_PREFIX, customUnitDialog.getEntity()
                                                                                                                     .getExternalId(), location, slot, chBurst.isSelected()));
        }
    }

    /**
     * Enables/disables the underlying checkbox (e.g. while the dialog is read-only or waiting on the server).
     */
    @Override
    public void setEnabled(boolean enabled) {
        chBurst.setEnabled(enabled);
    }
}
