package mekwars.common.gui.dialogs.customUnits;

import java.awt.Dimension;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.dialogs.CustomUnitDialog;

/*
 * In truth, this could be broken down into a method which returned a
 * JComboBox and the ammo-dumping CBox could be handled elsewhere; however,
 * the Panel extension is carried over from the original MegaMek code-path
 * and works well enough for our purposes. @urgru 7/30/05
 */
/**
 * Sub-panel embedded in the {@link CustomUnitDialog} for a single ammo-carrying mount, letting the
 * player pick which munition/ammo type to load, optionally dump the remaining ammo, and (where
 * applicable) hot-load the bin. One instance is created per ammo bin on the unit being customized.
 * The combo box lists every compatible {@link AmmoType} for the mount, each entry showing the
 * current shots remaining, the shots that would be refilled, and either a c-bill/FLU cost (normal
 * play) or a parts-crit count (crit-based economy), depending on dialog configuration.
 */
public class MunitionChoicePanel extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(MunitionChoicePanel.class);

    /**
     *
     */
    @java.io.Serial
    private static final long serialVersionUID = -5861067242226106955L;
    /** Parent dialog; used to look up the entity, dump/crit settings, and to send server commands. */
    private final CustomUnitDialog customUnitDialog;
    /** Ammo types available for this mount, in the same order as the {@link #m_choice} combo box entries. */
    private final Vector<AmmoType> m_vTypes;
    /** Combo box of formatted ammo choice descriptions; its selected index indexes into {@link #m_vTypes}. */
    private final JComboBox<String> m_choice;
    /** The ammo bin/mount this panel controls. */
    private final Mounted<?> m_mounted;
    /** Body location index (see {@code Entity.LOC_*}) of the mount. */
    private final int location;
    /** Client used to price ammo, look up parts-crit counts, and send the resulting command to the server. */
    private final IClient client;

    /** Checkbox to dump all remaining ammo in this bin (only added to the panel if dumping is permitted). */
    protected JCheckBox chDump = new JCheckBox();
    /** Checkbox to toggle hot-loading for this bin; disabled/non-functional unless the ammo type/entity/ruleset support it. */
    protected JCheckBox chHotLoad = new JCheckBox();

    /**
     * Builds the panel: populates the ammo-type combo box (with cost/shots-remaining/refill info per
     * entry), and conditionally adds the dump and hot-load checkboxes.
     *
     * @param customUnitDialog owning dialog, used to read settings (dump allowed, crit-based economy, TacOps options) and the entity
     * @param mounted the ammo bin/mount this panel edits
     * @param vTypes the compatible ammo types offered to the player, aligned by index with the combo box
     * @param location body location index of {@code mounted}
     */
    public MunitionChoicePanel(CustomUnitDialog customUnitDialog, Mounted<?> mounted, Vector<AmmoType> vTypes,
          int location) {
        this.customUnitDialog = customUnitDialog;
        this.client = customUnitDialog.getClient();
        boolean canDump = customUnitDialog.canDump();

        // save params
        m_vTypes = vTypes;
        m_mounted = mounted;
        this.location = location;

        // setup panel
        AmmoType curType = (AmmoType) mounted.getType();
        m_choice = new JComboBox<>();
        Enumeration<AmmoType> elements = m_vTypes.elements();

        for (int x = 0; elements.hasMoreElements(); x++) {
            AmmoType ammoType = elements.nextElement();
            m_choice.setMaximumSize(new Dimension(5, 5));
            int cost;
            int shotsLeft = mounted.getUsableShotsLeft();

            // "Shots left" is only meaningful relative to the ammo type currently loaded in the mount;
            // for every other candidate type in the list it is treated as empty (0).
            if (!curType.getInternalName().equalsIgnoreCase(ammoType.getInternalName())) {
                shotsLeft = 0;
            }

            double ammoCost = 0;

            try {
                ammoCost = client.getAmmoCost(ammoType.getInternalName());
            } catch (Exception ex) {
                LOGGER.error(ex, "error finding cost for: {}", ammoType.getName());
            }
            // LOC_NONE means the mount is not tied to a specific body location (e.g. a bay/pool-style
            // ammo slot); in that case there's no "refill to full" quantity to compute, so the entry
            // is priced as a flat single unit of ammo rather than a per-shot refill cost.
            if (mounted.getLocation() == megamek.common.units.Entity.LOC_NONE) {
                if (customUnitDialog.isUsingCrits()) {
                    m_choice.addItem(String.format("%s (%s/1/%s)", ammoType.getName(), shotsLeft, client.getPlayer()
                                                                                        .getPartsCache()
                                                                                        .getPartsCritCount(ammoType.getInternalName())));
                } else {
                    m_choice.addItem(String.format("%s (%s/1) %s", ammoType.getName(), shotsLeft, client.moneyOrFluMessage(true,
                          true,
                          (int) ammoCost)));
                }
            } else {
                int refillShots = ammoType.getShots();
                if (mounted.getUsableShotsLeft() == 0) {
                    // Capital Weapon
                    // Capital-scale weapons can report 0 usable shots even when the bin isn't truly
                    // empty at that ammo type's normal shot count, so fall back to the bin's original
                    // (as-built) shot count to compute how many shots a refill would need to add.
                    refillShots = mounted.getOriginalShots();
                }
                if (!curType.getInternalName().equalsIgnoreCase(ammoType.getInternalName())) {
                    shotsLeft = 0;
                }

                // No reason to continue if there are not shots to refill.
                if (shotsLeft == refillShots) {
                    cost = 0;
                } else {
                    refillShots -= shotsLeft;
                    cost = (int) Math.ceil(ammoCost * refillShots);
                }

                if (customUnitDialog.isUsingCrits()) {
                    m_choice.addItem(String.format("%s (%s/%s/%s)", ammoType.getName(), shotsLeft, refillShots, client.getPlayer()
                                                                                                     .getPartsCache()
                                                                                                     .getPartsCritCount(
                                                                                                           ammoType.getInternalName())));
                } else {
                    m_choice.addItem(String.format("%s (%s/%s) %s", ammoType.getName(), shotsLeft, refillShots, client.moneyOrFluMessage(
                          true,
                          true,
                          cost)));
                }

            }
            if (ammoType.getInternalName().equalsIgnoreCase(curType.getInternalName())) {
                m_choice.setSelectedIndex(x);
            }
        }

        add(m_choice);

        // set up the dump check box, if dumping is allowed
        if (canDump) {
            if (mounted.getUsableShotsLeft() == 0) {
                chDump.setSelected(true);
            }
            chDump.setText("Dump");
            add(chDump);
        }
        // Hot-load is only offered for TacOps games, on Meks/Tanks, and only when the currently
        // loaded ammo type actually supports hot-loading (F_HOTLOAD flag). Otherwise the checkbox is
        // still added to the panel (for consistent layout) but is disabled and inert.
        if (customUnitDialog.getMMClient().getGame().getOptions().booleanOption("tacops_hotload") &&
                  ((customUnitDialog.getEntity() instanceof Mek) ||
                         (customUnitDialog.getEntity() instanceof Tank)) &&
                  mounted.getType().hasFlag(AmmoType.F_HOTLOAD)) {
            chHotLoad.setSelected(mounted.isHotLoaded());
            chHotLoad.setText("Hot-Load");
            add(chHotLoad);
        } else {
            chHotLoad.setEnabled(false);
            chHotLoad.setText("Hot-Load");
            add(chHotLoad);
        }

    }

    /*
     * Yes, this to load the ammo Save Weapon Type from at.getAmmoType() call
     * weapon type save weapon position with at.getMunitionType() call ammo
     * type Load at.getMunitionsFor(ammoType) returns vector
     * ammo_vector.elementAt(MunitionType);
     */

    /**
     * Sends the selected ammo choice (and dump/hot-load state) to the server as a single campaign-chat
     * command. If nothing is selected in the combo box ({@code n < 0}, e.g. it was never populated),
     * this silently does nothing. If the dump checkbox is selected, the shots on the local mount object
     * are immediately zeroed out (client-side only; the actual authoritative shot count comes back from
     * the server) and the reported shot total sent to the server is forced to 0. If the mount has no
     * fixed body location ({@code LOC_NONE}), the reported total is forced to 1 regardless of the
     * selected ammo type's normal shot count, matching the single-unit pricing used when building the
     * combo box. The command sent is
     * {@code <prefix>c setunitammo#<externalId>#<location>#<ammoTypeId>#<internalName>#<totalShots>#<hotloaded>}.
     */
    public void applyChoice() {
        int n = m_choice.getSelectedIndex();

        if (n < 0) {
            return;
        }

        AmmoType ammoType = m_vTypes.elementAt(n);

        int totalShots = ammoType.getShots();

        boolean hotloaded = false;

        if (chHotLoad != null) {
            hotloaded = chHotLoad.isSelected();
        }

        if (chDump.isSelected()) {
            m_mounted.setShotsLeft(0);
            totalShots = 0;
        } else if (m_mounted.getLocation() == Entity.LOC_NONE) {
            totalShots = 1;
        }

        // Note: the local mount's shot count is NOT updated here for the normal (non-dump) case; the
        // line below is left commented out, so applying a new ammo choice relies entirely on the
        // server round-trip to update the actual shots-left value.
        // m_mounted.setShotsLeft(totalShots);
        client.sendChat(
              String.format("%sc setunitammo#%s#%s#%s#%s#%s#%s", IClient.CAMPAIGN_PREFIX, customUnitDialog.getEntity()
                                                                   .getExternalId(), location, ammoType.getAmmoType(), ammoType.getInternalName(), totalShots, hotloaded));
    }

    /**
     * Enables/disables only the ammo-type combo box; note this does not affect {@link #chDump} or
     * {@link #chHotLoad}, which manage their own enabled state independently.
     */
    @Override
    public void setEnabled(boolean enabled) {
        m_choice.setEnabled(enabled);
    }

    /**
     * Get the number of shots in the mount.
     *
     * @return the <code>int</code> number of shots in the mount.
     */
    /* package */int getShotsLeft() {
        return m_mounted.getUsableShotsLeft();
    }

    /**
     * Set the number of shots in the mount.
     *
     * <p>Package-private: exposed only so that {@link ProtoMekMunitionChoicePanel} can rescale the
     * shot count after applying its base ammo choice.
     *
     * @param shots the <code>int</code> number of shots for the mount.
     */
    /* package */void setShotsLeft(int shots) {
        m_mounted.setShotsLeft(shots);
    }
}
