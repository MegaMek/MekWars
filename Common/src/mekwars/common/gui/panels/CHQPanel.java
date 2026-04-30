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

import java.awt.GridBagConstraints;
import java.io.Serial;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

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

public class CHQPanel extends javax.swing.JPanel {

    @Serial
    private static final long serialVersionUID = -5137503055464771160L;
    public MekTableModel MekTable;
    protected MekTableMouseAdapter mouseAdapter;
    IClient client;
    client.campaign.CPlayer Player;

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

    public CHQPanel(IClient client) {
        this.client = client;
        Player = this.client.getPlayer();
        MekTable = new MekTableModel(this);
        mouseAdapter = new MechTableMouseAdapter(this);
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

    private void init() {
        pnlMeks = new javax.swing.JPanel();
        spMeks = new javax.swing.JScrollPane();
        tblMeks = new javax.swing.JTable();
        pnlMeksButtons = new javax.swing.JPanel();
        btnAddLance = new javax.swing.JButton();
        btnRemoveAllArmies = new javax.swing.JButton();
        setCamoButton = new javax.swing.JButton();
        newbieResetUnitsButton = new javax.swing.JButton();
        repairAllUnitsButton = new javax.swing.JButton();
        reloadAllUnitsButton = new javax.swing.JButton();
        //@Salient (mwosux@gmail.com) added for SolFreeBuild option
        solFreeBuildButton = new javax.swing.JButton();
        // pnlMekIcon = new MechInfo(client);
        // btnShowMek = new JButton();

        setLayout(new java.awt.GridBagLayout());

        createMeksPanel();
        // tpMain.addTab("Meks", null, pnlMeks, "Command Your Meks");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(pnlMeks, gridBagConstraints);
        useAdvanceRepairs = client.isUsingAdvanceRepairs();
        boolean useUnitLocking = Boolean.parseBoolean(client.getserverConfigs("LockUnits"));
    }

    public void refresh() {
        useAdvanceRepairs = client.isUsingAdvanceRepairs();
        MekTable.refreshModel();
        tblMeks.setPreferredSize(new java.awt.Dimension(tblMeks.getWidth(),
              tblMeks.getRowHeight() * (MekTable.getRowCount())));
        tblMeks.revalidate();
        client.getPlayer().sortArmies();
    }

    private void createMeksPanel() {
        // pnlMeks.setLayout(new BoxLayout(pnlMeks, BoxLayout.Y_AXIS));
        pnlMeks.setLayout(new java.awt.GridBagLayout());
        spMeks.setPreferredSize(new java.awt.Dimension(300, 400));
        tblMeks.setBackground(new java.awt.Color(255, 255, 255));
        tblMeks.setForeground(new java.awt.Color(0, 0, 0));
        tblMeks.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        tblMeks.setDoubleBuffered(true);
        tblMeks.setMaximumSize(new java.awt.Dimension(2147483647, 10000));
        tblMeks.setPreferredScrollableViewportSize(new java.awt.Dimension(300, 400));
        tblMeks.setPreferredSize(new java.awt.Dimension(300, 400));
        tblMeks.setRowHeight(100);
        tblMeks.setRowSelectionAllowed(false);
        tblMeks.setColumnSelectionAllowed(false);
        tblMeks.setCellSelectionEnabled(true);
        tblMeks.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        tblMeks.setModel(MekTable);
        tblMeks.getColumnModel().getColumn(0).setPreferredWidth(120);
        /*
         * tblMeks.getColumnModel().getColumn(1).setPreferredWidth(90); tblMeks.getColumnModel().getColumn(2).setPreferredWidth(90); tblMeks.getColumnModel().getColumn(3).setPreferredWidth(90); tblMeks.getColumnModel().getColumn(4).setPreferredWidth(90); tblMeks.getColumnModel().getColumn(5).setPreferredWidth(30);
         */
        tblMeks.getTableHeader().setReorderingAllowed(false);
        for (int i = 0; i < tblMeks.getColumnCount(); i++) {
            tblMeks.getColumnModel().getColumn(i).setCellRenderer(MekTable.getRenderer());
        }
        tblMeks.addMouseListener(mouseAdapter);
        tblMeks.addMouseMotionListener(mouseAdapter);

        spMeks.setViewportView(tblMeks);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        // gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        pnlMeks.add(spMeks, gridBagConstraints);

        // set up the row of buttons under the table
        pnlMeksButtons.setLayout(new javax.swing.BoxLayout(pnlMeksButtons, javax.swing.BoxLayout.Y_AXIS));
        makeButtons();// makes pnlMeksButtons

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        // gridBagConstraints.ipadx = 30;
        gridBagConstraints.weightx = 1.0;
        // gridBagConstraints.weighty = 0.0;
        // gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        pnlMeks.add(pnlMeksButtons, gridBagConstraints);
    }

    public void makeButtons() {

        javax.swing.JPanel hqButtonSpring = new javax.swing.JPanel(new javax.swing.SpringLayout());
        pnlMeksButtons.removeAll();

        btnAddLance.setText("Create New Army");
        btnAddLance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddLanceActionPerformed(evt);
            }
        });
        hqButtonSpring.add(btnAddLance);

        btnRemoveAllArmies.setText("Remove All Armies");
        btnRemoveAllArmies.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveAllArmiesActionPerformed(evt);
            }
        });
        hqButtonSpring.add(btnRemoveAllArmies);

        /*
         * Inefficient block of code used to check and see if a "Reset Units" button should be displayed. Hacky and ugly, but by no means unduly burdensome for the Client.
         */
        int numButtons = 3;
        client.campaign.CPlayer player = client.getPlayer();
        if (player != null) {
            if (player.getMyHouse().getName().equalsIgnoreCase(client.getserverConfigs("NewbieHouseName"))) {
                newbieResetUnitsButton.setText("Reset Units");
                newbieResetUnitsButton.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        newbieResetUnitsButtonActionPerformed(evt);
                    }
                });
                hqButtonSpring.add(newbieResetUnitsButton);
                numButtons++;
            }
        }

        if (useAdvanceRepairs) {
            repairAllUnitsButton.setText("Repair All Units");
            repairAllUnitsButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    repairAllUnitsButtonActionPerformed(evt);
                }
            });
            hqButtonSpring.add(repairAllUnitsButton);
            numButtons++;

            reloadAllUnitsButton.setText("Reload All Units");
            reloadAllUnitsButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    reloadAllUnitsButtonActionPerformed(evt);
                }
            });
            hqButtonSpring.add(reloadAllUnitsButton);
            numButtons++;
        }

        setCamoButton.setText("Change Camo");
        setCamoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setCamoButtonActionPerformed(evt);
            }
        });
        hqButtonSpring.add(setCamoButton);

        //@Salient add sol free build button
        if (player != null) {
            if (player.getMyHouse().getName().equalsIgnoreCase(client.getserverConfigs("NewbieHouseName"))
                      && client.getserverConfigs("Sol_FreeBuild").equalsIgnoreCase("true")) {
                solFreeBuildButton.setText("Create Unit");
                solFreeBuildButton.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        solFreeBuildButtonActionPerformed(evt);
                    }
                });
                hqButtonSpring.add(solFreeBuildButton);
                numButtons++;
            }
            //also spawn button if post defection option is set
            if (!player.getMyHouse().getName().equalsIgnoreCase(client.getserverConfigs("NewbieHouseName"))
                      && client.getserverConfigs("FreeBuild_PostDefection").equalsIgnoreCase("true")) {
                solFreeBuildButton.setText("Create Unit");
                solFreeBuildButton.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        solFreeBuildButtonActionPerformed(evt);
                    }
                });
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

    private void btnAddLanceActionPerformed(java.awt.event.ActionEvent evt) {
        client.sendChat(client.MWClient.CAMPAIGN_PREFIX + "c cra#" + client.getConfigParam("DEFAULTARMYNAME"));
    }

    // try to remove all armies
    private void btnRemoveAllArmiesActionPerformed(java.awt.event.ActionEvent evt) {
        //no armies... don't bother   		//Baruk Khazad! 20151204 - start block 1
        if (client.getPlayer().getArmies().size() == 0) {return;}
        //get confirm
        int result = javax.swing.JOptionPane.showConfirmDialog(client.getMainFrame(),
              "Are you sure you want to remove all of your armies?",
              "Remove all armies?",
              javax.swing.JOptionPane.YES_NO_OPTION);
        if (result == javax.swing.JOptionPane.NO_OPTION) {
            return;        //Baruk Khazad! 20151204 - end block 1
        }
        // only remove all if he's logged in, not fighting/active/logout/discon
        if (client.getMyStatus() != client.MWClient.STATUS_RESERVE) {
            return;
        }

        for (client.campaign.CArmy currA : client.getPlayer().getArmies()) {
            if (!currA.isPlayerLocked()) {
                client.sendChat(client.MWClient.CAMPAIGN_PREFIX + "c removearmy#" + currA.getID());
            }
        }
    }// end btnRemoveAllArmiesActionPerformed

    private void newbieResetUnitsButtonActionPerformed(java.awt.event.ActionEvent evt) {
        client.sendChat(client.MWClient.CAMPAIGN_PREFIX + "c request#resetunits");
    }

    ;

    private void repairAllUnitsButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (client.getPlayer().getHangar().size() > 0) {
            new BulkRepairDialog(client,
                  client.getPlayer().getHangar().firstElement().getId(),
                  BulkRepairDialog.TYPE_BULK,
                  BulkRepairDialog.UNIT_TYPE_ALL);
        }
    }

    ;

    private void reloadAllUnitsButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (client.getPlayer().getHangar().size() > 0) {
            int result = javax.swing.JOptionPane.showConfirmDialog(client.getMainFrame(),
                  "Are you sure you want to reload all the ammo on all your units?",
                  "Reload all units?",
                  javax.swing.JOptionPane.YES_NO_OPTION);

            if (result == javax.swing.JOptionPane.YES_OPTION) {
                for (client.campaign.CUnit unit : client.getPlayer().getHangar()) {
                    if (!UnitUtils.hasAllAmmo(unit.getEntity())) {
                        client.sendChat(client.MWClient.CAMPAIGN_PREFIX + "c RELOADALLAMMO#" + unit.getId());
                    }
                }

                refresh();
            }
        }
    }

    private void setCamoButtonActionPerformed(java.awt.event.ActionEvent evt) {
        CamoSelectionDialog camoDialog = new CamoSelectionDialog(client.getMainFrame(), client);
        camoDialog.setVisible(true);
    }

    //@Salient (mwosux@gmail.com) added for SolFreeBuild option
    private void solFreeBuildButtonActionPerformed(java.awt.event.ActionEvent evt) {
        SolFreeBuildDialog solDialog = new SolFreeBuildDialog(client);
        solDialog.setVisible(true);
    }

    /**
     * Public call which reinitializes the HQ panel. Hacky and evil, but lets camo and # columns in HQ display get
     * updated on the fly.
     */
    public void reinitialize() {

        // client.getMainFrame().getMainPanel().selectFirstTab();
        // client.getMainFrame().getMainPanel().getCommPanel().selectFirstTab();

        // remove all the old components.
        removeAll();
        // this.setVisible(false);

        Player = client.getPlayer();
        MekTable = new MekTableModel(this);
        mouseAdapter = new MechTableMouseAdapter(this);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        // gridBagConstraints.insets = new Insets(0, 0, 0, 10);

        init();
        refresh();
        // this.setVisible(true);
    }

    /*
     * Original source: http://docs.rinet.ru/J21/ch25.htm#AnAlphaImageFilter Original author: Michael Morrison
     */
    private static class AlphaFilter extends java.awt.image.RGBImageFilter {
        int alphaLevel;

        public AlphaFilter(int alpha) {
            alphaLevel = alpha;
            canFilterIndexColorModel = true;
        }

        @Override
        public int filterRGB(int x, int y, int rgb) {
            // Adjust the alpha value
            int alpha = (rgb >> 24) & 0xff;
            alpha = (alpha * alphaLevel) / 255;

            // Return the result
            return ((rgb & 0x00ffffff) | (alpha << 24));
        }
    }

}
