/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet) Original author Helge Richter (McWizard)
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

package mekwars.common.gui.panels;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.Serial;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;

import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.adapters.MekTableMouseAdapter;
import mekwars.common.gui.dialogs.BulkRepairDialog;
import mekwars.common.gui.dialogs.CamoSelectionDialog;
import mekwars.common.gui.dialogs.SolFreeBuildDialog;
import mekwars.common.gui.models.MekTableModel;
import mekwars.common.util.SpringLayoutHelper;
import mekwars.common.util.UnitUtils;

/**
 * The "Headquarters" (HQ) tab of the MekWars client: shows the player's owned units/armies in a table
 * ({@link MekTableModel}), with a row of action buttons below it for managing the player's forces — creating a
 * new army, removing all armies, changing camo, repairing/reloading all units, and (conditionally, depending on
 * server config and the player's faction) resetting units for newbies or building a free unit. Most buttons send
 * a campaign chat command to the server rather than mutating local state directly; the server's response is what
 * ultimately updates the displayed data via {@link #refresh()}.
 */

public class CHQPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = -5137503055464771160L;
    /** Connection/session handle used to read player state and send campaign commands. */
    private final IClient client;
    /** Mouse handler for the units table (handles clicks/drags for unit selection, drag-and-drop into armies, etc). */
    protected MekTableMouseAdapter mouseAdapter;
    /** Table model backing {@link #tblMeks}; holds the player's units/armies. */
    private MekTableModel MekTable;
    /** The player whose HQ this panel displays. */
    private CPlayer Player;

    // graphical components
    /** Scratch/reused constraints object for GridBagLayout calls; reassigned repeatedly during layout. */
    private GridBagConstraints gridBagConstraints;

    private JPanel pnlMeks;
    private JScrollPane spMeks;
    private JTable tblMeks;
    private JPanel pnlMeksButtons;
    private JButton btnAddLance;
    private JButton btnRemoveAllArmies;
    private JButton setCamoButton;
    /** Only shown to players in the server's configured "newbie" house; resets their starting units. */
    private JButton newbieResetUnitsButton;
    private JButton repairAllUnitsButton;
    private JButton reloadAllUnitsButton;

    //@Salient (mwosux@gmail.com) added for SolFreeBuild option
    /** Only shown when a free-build option applies to the player; opens the free-unit-creation dialog. */
    private JButton solFreeBuildButton;
    /** Whether the server is using "advance repairs" mode; gates the repair/reload-all buttons. Kept in sync via {@link #refresh()}. */
    private boolean useAdvanceRepairs = false;
    /** Whether unit locking is in effect for this panel; toggled externally via {@link #setUseUnitLocking(boolean)}. */
    private boolean useUnitLocking = false;

    /**
     * Captures the current player and builds a fresh {@link MekTableModel} and {@link MekTableMouseAdapter}, then
     * lays out the panel via {@link #init()} and populates it via {@link #refresh()}.
     *
     * @param client the client providing player/campaign state
     */
    public CHQPanel(IClient client) {
        this.client = client;
        Player = this.client.getPlayer();
        MekTable = new MekTableModel(this);
        mouseAdapter = new MekTableMouseAdapter(this);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;

        init();
        refresh();
    }

    /**
     * Instantiates all child components and lays out the units panel via {@link #createMeksPanel()}, then
     * captures the server's current advance-repairs setting.
     */
    private void init() {
        pnlMeks = new JPanel();
        spMeks = new JScrollPane();
        tblMeks = new JTable();
        pnlMeksButtons = new JPanel();
        btnAddLance = new JButton();
        btnRemoveAllArmies = new JButton();
        setCamoButton = new JButton();
        newbieResetUnitsButton = new JButton();
        repairAllUnitsButton = new JButton();
        reloadAllUnitsButton = new JButton();
        //@Salient (mwosux@gmail.com) added for SolFreeBuild option
        solFreeBuildButton = new JButton();

        setLayout(new GridBagLayout());

        createMeksPanel();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(pnlMeks, gridBagConstraints);
        useAdvanceRepairs = client.isUsingAdvanceRepairs();
    }

    /**
     * Re-syncs the advance-repairs flag from the client, reloads the units table model, resizes the table to fit
     * its (possibly changed) row count, and re-sorts the player's armies for display.
     */
    public void refresh() {
        useAdvanceRepairs = client.isUsingAdvanceRepairs();
        MekTable.refreshModel();
        tblMeks.setPreferredSize(new Dimension(tblMeks.getWidth(), tblMeks.getRowHeight() * (MekTable.getRowCount())));
        tblMeks.revalidate();
        client.getPlayer().sortArmies();
    }

    /** Builds and lays out the scrollable units table and the button row beneath it, both inside {@link #pnlMeks}. */
    private void createMeksPanel() {
        pnlMeks.setLayout(new GridBagLayout());
        spMeks.setPreferredSize(new Dimension(300, 400));
        tblMeks.setBackground(new Color(255, 255, 255));
        tblMeks.setForeground(new Color(0, 0, 0));
        tblMeks.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tblMeks.setDoubleBuffered(true);
        tblMeks.setMaximumSize(new Dimension(2147483647, 10000));
        tblMeks.setPreferredScrollableViewportSize(new Dimension(300, 400));
        tblMeks.setPreferredSize(new Dimension(300, 400));
        tblMeks.setRowHeight(100);
        tblMeks.setRowSelectionAllowed(false);
        tblMeks.setColumnSelectionAllowed(false);
        tblMeks.setCellSelectionEnabled(true);
        tblMeks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tblMeks.setModel(MekTable);
        tblMeks.getColumnModel().getColumn(0).setPreferredWidth(120);
        tblMeks.getTableHeader().setReorderingAllowed(false);
        for (int i = 0; i < tblMeks.getColumnCount(); i++) {
            tblMeks.getColumnModel().getColumn(i).setCellRenderer(MekTable.getRenderer());
        }
        tblMeks.addMouseListener(mouseAdapter);
        tblMeks.addMouseMotionListener(mouseAdapter);

        spMeks.setViewportView(tblMeks);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlMeks.add(spMeks, gridBagConstraints);

        // set up the row of buttons under the table
        pnlMeksButtons.setLayout(new BoxLayout(pnlMeksButtons, BoxLayout.Y_AXIS));
        makeButtons();// makes pnlMeksButtons

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        pnlMeks.add(pnlMeksButtons, gridBagConstraints);
    }

    /**
     * (Re)builds the row of action buttons under the units table (clearing any previous ones first). Which
     * buttons appear is conditional: "Reset Units" only shows for players in the server's configured newbie
     * house; "Repair/Reload All Units" only show when advance-repairs mode is active; "Create Unit" (free build)
     * shows either for newbie-house players when {@code Sol_FreeBuild} is enabled, or for non-newbie players when
     * {@code FreeBuild_PostDefection} is enabled. "Create New Army", "Remove All Armies", and "Change Camo" are
     * always shown.
     */
    public void makeButtons() {

        JPanel hqButtonSpring = new JPanel(new SpringLayout());
        pnlMeksButtons.removeAll();

        btnAddLance.setText("Create New Army");
        btnAddLance.addActionListener(this::btnAddLanceActionPerformed);
        hqButtonSpring.add(btnAddLance);

        btnRemoveAllArmies.setText("Remove All Armies");
        btnRemoveAllArmies.addActionListener(this::btnRemoveAllArmiesActionPerformed);
        hqButtonSpring.add(btnRemoveAllArmies);

        /*
         * Inefficient block of code used to check and see if a "Reset Units" button should be displayed. Hacky and ugly, but by no means unduly burdensome for the client.
         */
        int numButtons = 3;
        CPlayer player = client.getPlayer();
        if (player != null) {
            if (player.getMyHouse().getName().equalsIgnoreCase(client.getServerConfigs("NewbieHouseName"))) {
                newbieResetUnitsButton.setText("Reset Units");
                newbieResetUnitsButton.addActionListener(this::newbieResetUnitsButtonActionPerformed);
                hqButtonSpring.add(newbieResetUnitsButton);
                numButtons++;
            }
        }

        if (useAdvanceRepairs) {
            repairAllUnitsButton.setText("Repair All Units");
            repairAllUnitsButton.addActionListener(this::repairAllUnitsButtonActionPerformed);
            hqButtonSpring.add(repairAllUnitsButton);
            numButtons++;

            reloadAllUnitsButton.setText("Reload All Units");
            reloadAllUnitsButton.addActionListener(this::reloadAllUnitsButtonActionPerformed);
            hqButtonSpring.add(reloadAllUnitsButton);
            numButtons++;
        }

        setCamoButton.setText("Change Camo");
        setCamoButton.addActionListener(this::setCamoButtonActionPerformed);
        hqButtonSpring.add(setCamoButton);

        //@Salient add sol free build button
        if (player != null) {
            if (player.getMyHouse().getName().equalsIgnoreCase(client.getServerConfigs("NewbieHouseName"))
                      && client.getServerConfigs("Sol_FreeBuild").equalsIgnoreCase("true")) {
                solFreeBuildButton.setText("Create Unit");
                solFreeBuildButton.addActionListener(this::solFreeBuildButtonActionPerformed);
                hqButtonSpring.add(solFreeBuildButton);
                numButtons++;
            }
            //also spawn button if post defection option is set
            if (!player.getMyHouse().getName().equalsIgnoreCase(client.getServerConfigs("NewbieHouseName"))
                      && client.getServerConfigs("FreeBuild_PostDefection").equalsIgnoreCase("true")) {
                solFreeBuildButton.setText("Create Unit");
                solFreeBuildButton.addActionListener(this::solFreeBuildButtonActionPerformed);
                hqButtonSpring.add(solFreeBuildButton);
                numButtons++;
            }
        }

        // do the spring layout on the buttons, then add them to the box
        SpringLayoutHelper.setupSpringGrid(hqButtonSpring, 1, numButtons);
        pnlMeksButtons.add(hqButtonSpring);
        pnlMeksButtons.validate();
        pnlMeksButtons.repaint();
    }
    // NOTE: makeButtons() is public and re-adds action listeners to the shared button fields (btnAddLance,
    // solFreeBuildButton, etc.) without removing previously-added ones first; calling it more than once on the
    // same CHQPanel instance (its only current caller is createMeksPanel(), invoked once per init()/reinitialize()
    // cycle) would stack duplicate listeners and fire handlers multiple times per click.

    /** Sends the "create army" campaign command using the configured default army name. */
    private void btnAddLanceActionPerformed(ActionEvent evt) {
        client.sendChat(String.format("%sc cra#%s", IClient.CAMPAIGN_PREFIX, client.getConfigParam("DEFAULTARMYNAME")));
    }

    /**
     * Confirms with the user, then sends a "remove army" campaign command for every one of the player's armies
     * that is not player-locked. No-ops if the player has no armies, the user declines the confirmation, or the
     * player's status is anything other than {@link IClient#STATUS_RESERVE} (i.e. not actively fighting).
     */
    private void btnRemoveAllArmiesActionPerformed(ActionEvent evt) {
        //no armies... don't bother   		//Baruk Khazad! 20151204 - start block 1
        if (client.getPlayer().getArmies().isEmpty()) {
            return;
        }
        //get confirm
        int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
              "Are you sure you want to remove all of your armies?",
              "Remove all armies?",
              JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.NO_OPTION) {
            return;        //Baruk Khazad! 20151204 - end block 1
        }
        // only remove all if he's logged in, not fighting/active/logout/discon
        if (client.getMyStatus() != IClient.STATUS_RESERVE) {
            return;
        }

        for (CArmy currA : client.getPlayer().getArmies()) {
            if (!currA.isPlayerLocked()) {
                client.sendChat(String.format("%sc removearmy#%s", IClient.CAMPAIGN_PREFIX, currA.getID()));
            }
        }
    }// end btnRemoveAllArmiesActionPerformed

    /** Sends the "resetunits" campaign request, used to reset a newbie-house player's starting units. */
    private void newbieResetUnitsButtonActionPerformed(ActionEvent evt) {
        client.sendChat(String.format("%sc request#resetunits", IClient.CAMPAIGN_PREFIX));
    }

    /**
     * Opens a bulk repair dialog for all units, seeded with the first unit in the player's hangar. Does nothing if
     * the hangar is empty.
     */
    private void repairAllUnitsButtonActionPerformed(ActionEvent evt) {
        if (!client.getPlayer().getHangar().isEmpty()) {
            new BulkRepairDialog(client,
                  client.getPlayer().getHangar().firstElement().getId(),
                  BulkRepairDialog.TYPE_BULK,
                  BulkRepairDialog.UNIT_TYPE_ALL);
        }
    }

    /**
     * After confirmation, sends a "reload all ammo" campaign command for every unit in the player's hangar that is
     * not already fully loaded, then refreshes the panel. Does nothing if the hangar is empty or the user declines.
     */
    private void reloadAllUnitsButtonActionPerformed(ActionEvent evt) {
        if (!client.getPlayer().getHangar().isEmpty()) {
            int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                  "Are you sure you want to reload all the ammo on all your units?",
                  "Reload all units?",
                  JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                for (CUnit unit : client.getPlayer().getHangar()) {
                    if (!UnitUtils.hasAllAmmo(unit.getEntity())) {
                        client.sendChat(String.format("%sc RELOADALLAMMO#%s", IClient.CAMPAIGN_PREFIX, unit.getId()));
                    }
                }

                refresh();
            }
        }
    }

    /** Opens the modal camo-selection dialog for the player's units. */
    private void setCamoButtonActionPerformed(ActionEvent evt) {
        CamoSelectionDialog camoDialog = new CamoSelectionDialog(client.getMainFrame(), client);
        camoDialog.setVisible(true);
    }

    //@Salient (mwosux@gmail.com) added for SolFreeBuild option
    /** Opens the modal free-unit-build dialog (shown only under the newbie/post-defection free-build conditions). */
    private void solFreeBuildButtonActionPerformed(ActionEvent evt) {
        SolFreeBuildDialog solDialog = new SolFreeBuildDialog(client);
        solDialog.setVisible(true);
    }

    /**
     * @return whether unit locking is currently enabled for this panel
     */
    public boolean isUseUnitLocking() {
        return useUnitLocking;
    }

    /**
     * @param useUnitLocking whether unit locking should be enabled for this panel
     */
    public void setUseUnitLocking(boolean useUnitLocking) {
        this.useUnitLocking = useUnitLocking;
    }

    // NOTE: stray empty statement (leftover no-op); harmless but has no effect.
    ;

    /**
     * @return whether the server is currently using "advance repairs" mode, as last synced by {@link #refresh()}
     */
    public boolean useAdvanceRepairs() {
        return useAdvanceRepairs;
    }

    // NOTE: stray empty statement (leftover no-op); harmless but has no effect.
    ;

    /**
     * @return the table model backing the units table
     */
    public MekTableModel getMekTable() {
        return MekTable;
    }

    /**
     * @return the client this panel is bound to
     */
    public IClient getClient() {
        return client;
    }

    /**
     * @return the Swing table component displaying the player's units
     */
    public JTable getTableMeks() {
        return tblMeks;
    }

    /**
     * Public calls that reinitialize the HQ panel. Hacky and evil, but lets camo and # columns in HQ display get
     * updated on the fly. Discards and rebuilds every child component (table, model, mouse adapter, buttons) from
     * scratch by calling {@link #removeAll()} followed by {@link #init()} and {@link #refresh()}.
     */
    public void reinitialize() {

        // remove all the old components.
        removeAll();

        Player = client.getPlayer();
        MekTable = new MekTableModel(this);
        mouseAdapter = new MekTableMouseAdapter(this);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;

        init();
        refresh();
    }

    /**
     * @return the player currently displayed by this HQ panel
     */
    public CPlayer getPlayer() {
        return Player;
    }

    /**
     * @param player the player to associate with this panel (does not itself trigger a refresh)
     */
    public void setPlayer(CPlayer player) {
        Player = player;
    }
}
