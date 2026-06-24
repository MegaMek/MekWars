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
public class MunitionChoicePanel extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(MunitionChoicePanel.class);

    /**
     *
     */
    @java.io.Serial
    private static final long serialVersionUID = -5861067242226106955L;
    private final CustomUnitDialog customUnitDialog;
    private final Vector<AmmoType> m_vTypes;
    private final JComboBox<String> m_choice;
    private final Mounted<?> m_mounted;
    private final int location;
    private final IClient client;

    protected JCheckBox chDump = new JCheckBox();
    protected JCheckBox chHotLoad = new JCheckBox();

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

            if (!curType.getInternalName().equalsIgnoreCase(ammoType.getInternalName())) {
                shotsLeft = 0;
            }

            double ammoCost = 0;

            try {
                ammoCost = client.getAmmoCost(ammoType.getInternalName());
            } catch (Exception ex) {
                LOGGER.error(ex, "error finding cost for: {}", ammoType.getName());
            }
            if (mounted.getLocation() == megamek.common.units.Entity.LOC_NONE) {
                if (customUnitDialog.isUsingCrits()) {
                    m_choice.addItem(STR."\{ammoType.getName()} (\{shotsLeft}/1/\{client.getPlayer()
                                                                                        .getPartsCache()
                                                                                        .getPartsCritCount(ammoType.getInternalName())})");
                } else {
                    m_choice.addItem(STR."\{ammoType.getName()} (\{shotsLeft}/1) \{client.moneyOrFluMessage(true,
                          true,
                          (int) ammoCost)}");
                }
            } else {
                int refillShots = ammoType.getShots();
                if (mounted.getUsableShotsLeft() == 0) {
                    // Capital Weapon
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
                    m_choice.addItem(STR."\{ammoType.getName()} (\{shotsLeft}/\{refillShots}/\{client.getPlayer()
                                                                                                     .getPartsCache()
                                                                                                     .getPartsCritCount(
                                                                                                           ammoType.getInternalName())})");
                } else {
                    m_choice.addItem(STR."\{ammoType.getName()} (\{shotsLeft}/\{refillShots}) \{client.moneyOrFluMessage(
                          true,
                          true,
                          cost)}");
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

        // m_mounted.setShotsLeft(totalShots);
        client.sendChat(
              STR."\{IClient.CAMPAIGN_PREFIX}c setunitammo#\{customUnitDialog.getEntity()
                                                                   .getExternalId()}#\{location}#\{ammoType.getAmmoType()}#\{ammoType.getInternalName()}#\{totalShots}#\{hotloaded}");
    }

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
     * @param shots the <code>int</code> number of shots for the mount.
     */
    /* package */void setShotsLeft(int shots) {
        m_mounted.setShotsLeft(shots);
    }
}
