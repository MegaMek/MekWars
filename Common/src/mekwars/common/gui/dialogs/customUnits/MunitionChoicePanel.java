package mekwars.common.gui.dialogs.customUnits;

import java.util.Vector;
import javax.swing.JComboBox;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.dialogs.CustomUnitDialog;

/*
 * In truth, this could be broken down into a method which returned a
 * JComboBox and the ammo-dumping CBox could be handled elsewhere; however,
 * the Panel extension is carried over from the original MegaMek code-path
 * and works well enough for our purposes. @urgru 7/30/05
 */
public class MunitionChoicePanel extends javax.swing.JPanel {
    /**
     *
     */
    @java.io.Serial
    private static final long serialVersionUID = -5861067242226106955L;
    private final mekwars.common.gui.dialogs.CustomUnitDialog customUnitDialog;
    private final java.util.Vector<megamek.common.equipment.AmmoType> m_vTypes;
    private final JComboBox<String> m_choice;
    private final megamek.common.equipment.Mounted<?> m_mounted;
    private final int location;
    private final IClient client;

    protected javax.swing.JCheckBox chDump = new javax.swing.JCheckBox();
    protected javax.swing.JCheckBox chHotLoad = new javax.swing.JCheckBox();

    public MunitionChoicePanel(CustomUnitDialog customUnitDialog, Mounted<?> m, Vector<AmmoType> vTypes, int location) {
        this.customUnitDialog = customUnitDialog;
        this.client = customUnitDialog.getClient();
        boolean canDump = customUnitDialog.canDump();

        // save params
        m_vTypes = vTypes;
        m_mounted = m;
        this.location = location;

        // setup panel
        megamek.common.equipment.AmmoType curType = (megamek.common.equipment.AmmoType) m.getType();
        m_choice = new javax.swing.JComboBox<>();
        java.util.Enumeration<megamek.common.equipment.AmmoType> e = m_vTypes.elements();

        for (int x = 0; e.hasMoreElements(); x++) {
            megamek.common.equipment.AmmoType at = e.nextElement();
            m_choice.setMaximumSize(new java.awt.Dimension(5, 5));
            int cost;
            int shotsLeft = m.getUsableShotsLeft();
            if (!curType.getInternalName().equalsIgnoreCase(at.getInternalName())) {
                shotsLeft = 0;
            }

            double ammoCost = 0;
            try {
                ammoCost = client.getAmmoCost(at.getInternalName());
            } catch (Exception ex) {
                mekwars.common.util.MWLogger.errLog("error finding cost for: " + at.getName());
                mekwars.common.util.MWLogger.errLog(ex);
            }
            if (m.getLocation() == megamek.common.units.Entity.LOC_NONE) {
                if (customUnitDialog.isUsingCrits()) {
                    m_choice.addItem(at.getName() +
                                           " (" +
                                           shotsLeft +
                                           "/1/" +
                                           client.getPlayer()
                                                 .getPartsCache()
                                                 .getPartsCritCount(at.getInternalName()) +
                                           ")");
                } else {
                    m_choice.addItem(at.getName() +
                                           " (" +
                                           shotsLeft +
                                           "/1) " +
                                           client.moneyOrFluMessage(true, true, (int) ammoCost));
                }
            } else {
                int refillShots = at.getShots();
                if (m.getUsableShotsLeft() == 0) {
                    // Capital Weapon
                    refillShots = m.getOriginalShots();
                }
                if (!curType.getInternalName().equalsIgnoreCase(at.getInternalName())) {
                    shotsLeft = 0;
                }

                // No reason to continue if there are not shots to refill.
                if (shotsLeft == refillShots) {
                    cost = 0;
                } else {
                    refillShots -= shotsLeft;
                    cost = (int) Math.ceil(ammoCost * refillShots);
                }

                // MWLogger.errLog("Cost: "+cost+" string: "+client.moneyOrFluMessage(true,true,cost));
                if (customUnitDialog.isUsingCrits()) {
                    m_choice.addItem(at.getName() +
                                           " (" +
                                           shotsLeft +
                                           "/" +
                                           refillShots +
                                           "/" +
                                           client.getPlayer()
                                                 .getPartsCache()
                                                 .getPartsCritCount(at.getInternalName()) +
                                           ")");
                } else {
                    m_choice.addItem(at.getName() +
                                           " (" +
                                           shotsLeft +
                                           "/" +
                                           refillShots +
                                           ") " +
                                           client.moneyOrFluMessage(true, true, cost));
                }

            }
            if (at.getInternalName().equalsIgnoreCase(curType.getInternalName())) {
                m_choice.setSelectedIndex(x);
            }
        }

        add(m_choice);

        // set up the dump checkbox, if dumping is allowed
        if (canDump) {
            if (m.getUsableShotsLeft() == 0) {
                chDump.setSelected(true);
            }
            chDump.setText("Dump");
            add(chDump);
        }
        if (customUnitDialog.getMMClient().getGame().getOptions().booleanOption("tacops_hotload") &&
                  ((customUnitDialog.getEntity() instanceof megamek.common.units.Mek) ||
                         (customUnitDialog.getEntity() instanceof megamek.common.units.Tank)) &&
                  m.getType().hasFlag(megamek.common.equipment.AmmoType.F_HOTLOAD)) {
            chHotLoad.setSelected(m.isHotLoaded());
            chHotLoad.setText("Hot-Load");
            add(chHotLoad);
        } else {
            chHotLoad.setEnabled(false);
            chHotLoad.setText("Hot-Load");
            add(chHotLoad);
        }

    }

    /*
     * Yes this to load the ammo Save Weapon Type from at.getAmmoType() call
     * weapon type save weapon position with at.getMunitionType() call ammo
     * type Load at.getMunitionsFor(ammoType) returns vector
     * ammo_vector.elementAt(MunitionType);
     */

    public void applyChoice() {
        int n = m_choice.getSelectedIndex();

        if (n < 0) {
            return;
        }
        megamek.common.equipment.AmmoType at = m_vTypes.elementAt(n);
        // m_mounted.changeAmmoType(at);

        int totalShots = at.getShots();

        boolean hotloaded = false;

        if (chHotLoad != null) {
            hotloaded = chHotLoad.isSelected();
        }

        if (chDump.isSelected()) {
            m_mounted.setShotsLeft(0);
            totalShots = 0;
        } else if (m_mounted.getLocation() == megamek.common.units.Entity.LOC_NONE) {
            totalShots = 1;
        }

        // m_mounted.setShotsLeft(totalShots);
        client.sendChat(
              STR."\{mekwars.common.campaign.clientutils.protocol.IClient.CAMPAIGN_PREFIX}c setunitammo#\{customUnitDialog.getEntity()
                                                                                                                .getExternalId()}#\{location}#\{at.getAmmoType()}#\{at.getInternalName()}#\{totalShots}#\{hotloaded}");
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
