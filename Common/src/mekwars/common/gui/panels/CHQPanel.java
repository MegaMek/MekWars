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
 * Headquarters Panel
 */

public class CHQPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = -5137503055464771160L;
    private final IClient client;
    protected MekTableMouseAdapter mouseAdapter;
    private MekTableModel MekTable;
    private CPlayer Player;

    // graphical components
    private GridBagConstraints gridBagConstraints;

    private JPanel pnlMeks;
    private JScrollPane spMeks;
    private JTable tblMeks;
    private JPanel pnlMeksButtons;
    private JButton btnAddLance;
    private JButton btnRemoveAllArmies;
    private JButton setCamoButton;
    private JButton newbieResetUnitsButton;
    private JButton repairAllUnitsButton;
    private JButton reloadAllUnitsButton;

    //@Salient (mwosux@gmail.com) added for SolFreeBuild option
    private JButton solFreeBuildButton;
    private boolean useAdvanceRepairs = false;
    private boolean useUnitLocking = false;

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

    public void refresh() {
        useAdvanceRepairs = client.isUsingAdvanceRepairs();
        MekTable.refreshModel();
        tblMeks.setPreferredSize(new Dimension(tblMeks.getWidth(), tblMeks.getRowHeight() * (MekTable.getRowCount())));
        tblMeks.revalidate();
        client.getPlayer().sortArmies();
    }

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

    private void btnAddLanceActionPerformed(ActionEvent evt) {
        client.sendChat(String.format("%sc cra#%s", IClient.CAMPAIGN_PREFIX, client.getConfigParam("DEFAULTARMYNAME")));
    }

    // try to remove all armies
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

    private void newbieResetUnitsButtonActionPerformed(ActionEvent evt) {
        client.sendChat(String.format("%sc request#resetunits", IClient.CAMPAIGN_PREFIX));
    }

    private void repairAllUnitsButtonActionPerformed(ActionEvent evt) {
        if (!client.getPlayer().getHangar().isEmpty()) {
            new BulkRepairDialog(client,
                  client.getPlayer().getHangar().firstElement().getId(),
                  BulkRepairDialog.TYPE_BULK,
                  BulkRepairDialog.UNIT_TYPE_ALL);
        }
    }

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

    private void setCamoButtonActionPerformed(ActionEvent evt) {
        CamoSelectionDialog camoDialog = new CamoSelectionDialog(client.getMainFrame(), client);
        camoDialog.setVisible(true);
    }

    //@Salient (mwosux@gmail.com) added for SolFreeBuild option
    private void solFreeBuildButtonActionPerformed(ActionEvent evt) {
        SolFreeBuildDialog solDialog = new SolFreeBuildDialog(client);
        solDialog.setVisible(true);
    }

    public boolean isUseUnitLocking() {
        return useUnitLocking;
    }

    public void setUseUnitLocking(boolean useUnitLocking) {
        this.useUnitLocking = useUnitLocking;
    }

    ;

    public boolean useAdvanceRepairs() {
        return useAdvanceRepairs;
    }

    ;

    public MekTableModel getMekTable() {
        return MekTable;
    }

    public IClient getClient() {
        return client;
    }

    public JTable getTableMeks() {
        return tblMeks;
    }

    /**
     * Public calls that reinitialize the HQ panel. Hacky and evil, but lets camo and # columns in HQ display get
     * updated on the fly.
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

    public CPlayer getPlayer() {
        return Player;
    }

    public void setPlayer(CPlayer player) {
        Player = player;
    }
}
