/*
 * MekWars - Copyright (C) 2004, 2005
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

/*
 * MechSelectorJDialog.java - Copyright (C) 2002,2004 Josh Yockey
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/*
 * Thanks to the MegaMek Crew for the Code base
 * Modified by Torren (Jason Tighe)
 * From Megamek.client.MechSelectorDialgo.java
 */

package mekwars.common.gui.dialogs;

import java.io.Serial;
import java.util.Vector;

import megamek.client.ui.dialogs.UnitFailureDialog;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.dialogs.unitSelectorDialogs.ConfigurableMekViewPanel;
import megamek.common.comparators.MekSummaryComparator;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.CMainFrame;
import mekwars.common.gui.MekInfo;
import mekwars.common.util.MWLogger;
import mekwars.common.util.SpringLayoutHelper;
import mekwars.common.util.UnitUtils;

/*
 * Allows a user to sort through a list of MechSummaries and select one
 */

public class RePodSelectorDialog extends javax.swing.JFrame
      implements java.awt.event.ActionListener, java.awt.event.KeyListener, javax.swing.event.ListSelectionListener,
                 Runnable, java.awt.event.WindowListener, java.awt.event.ItemListener {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -6467246609231845514L;

    // how long after a key is typed does a new search begin
    private final static int KEY_TIMEOUT = 1000;
    private static final String SPACES = "                        ";
    // frame which owns the dialog
    private final CMainFrame cMainFrame;
    private final UnitLoadingDialog unitLoadingDialog;
    private final javax.swing.JButton bRePod = new javax.swing.JButton("RePod");
    private final javax.swing.JButton bCancel = new javax.swing.JButton("Close");
    private final javax.swing.JButton bRandom = new javax.swing.JButton("Random");
    private final javax.swing.JTextPane mechViewLeft;
    private final javax.swing.JTextPane mechViewRight;
    private final IClient client;
    private final java.util.TreeMap<String, String> chassisList = new java.util.TreeMap<>();
    private final String unitId;
    javax.swing.DefaultListModel<String> defaultModel;
    javax.swing.ListSelectionModel listSelectionModel;
    javax.swing.JList<String> mechList;
    javax.swing.JScrollPane listScrollPane = null;
    javax.swing.JScrollPane leftScrollPane = null;
    javax.swing.JScrollPane rightScrollPane = null;
    private MekSummary[] meksCurrent;
    private StringBuilder m_sbSearch = new StringBuilder();
    private long m_nLastSearch = 0;
    private javax.swing.JPanel pPreview = new javax.swing.JPanel();
    private boolean global = false;

    public RePodSelectorDialog(CMainFrame cMainFrame, UnitLoadingDialog uld, IClient client,
          String chassisList, String unitId) {
        super("RePod Selector");

        // save params
        this.cMainFrame = cMainFrame;
        unitLoadingDialog = uld;
        this.client = client;
        java.util.StringTokenizer ST = new java.util.StringTokenizer(chassisList, "#");

        while (ST.hasMoreElements()) {
            String tempstr = ST.nextToken();
            if (tempstr.equals("GLOBAL")) {
                global = true;
            } else if (tempstr.contains(".")) {
                String chassieMods = ST.nextToken();
                // MWLogger.errLog("Chassie: "+tempstr+" mods: "+chassieMods+" ChassieList: "+chassisList);
                this.chassisList.put(tempstr, chassieMods);
            }
        }

        this.unitId = unitId;

        // construct 2 text boxes
        mechViewLeft = new javax.swing.JTextPane(); //(22, 29);
        mechViewRight = new javax.swing.JTextPane(); //(22, 34);

        mechViewLeft.setContentType("text/html");
        //        mechViewLeft.set

        mechViewRight.setContentType("text/html");

        // construct a model and list
        defaultModel = new javax.swing.DefaultListModel<>();
        mechList = new javax.swing.JList<>(defaultModel);
        listSelectionModel = mechList.getSelectionModel();
        mechList.setVisibleRowCount(22);// give the list same number of rows as
        // the text boxes
        listSelectionModel.addListSelectionListener(this);

        // place the list and text boxes in scroll panes
        listScrollPane = new javax.swing.JScrollPane(mechList);
        leftScrollPane = new javax.swing.JScrollPane(mechViewLeft);
        rightScrollPane = new javax.swing.JScrollPane(mechViewRight);

        // set list/scroll options
        listScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        listScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        leftScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        rightScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mechList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        // set fonts
        mechViewLeft.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 11));
        mechViewRight.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 11));
        mechList.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 11));

        // set up the upper panel (combo boxes, preview image)
        pPreview = new MekInfo(client);
        pPreview.setVisible(false);
        pPreview.setMinimumSize(new java.awt.Dimension(86, 74));
        pPreview.setMaximumSize(new java.awt.Dimension(86, 74));
        javax.swing.JPanel pUpper = new javax.swing.JPanel();
        pUpper.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        javax.swing.JPanel pParams = new javax.swing.JPanel();
        pUpper.add(pParams);
        pUpper.add(pPreview);

        // panel w/ 1x3 SpringLayout for the mechView bits
        javax.swing.JPanel textBoxSpring = new javax.swing.JPanel(new javax.swing.SpringLayout());
        textBoxSpring.add(listScrollPane);
        textBoxSpring.add(leftScrollPane);
        textBoxSpring.add(rightScrollPane);
        SpringLayoutHelper.setupSpringGrid(textBoxSpring, 1, 3);

        // set up a formatting holder for the cancel button
        javax.swing.JPanel buttonHolder = new javax.swing.JPanel();
        buttonHolder.add(bRePod);
        buttonHolder.add(bRandom);
        buttonHolder.add(bCancel);

        // set a default button
        getRootPane().setDefaultButton(bCancel);

        // set up the overall SpringLayout
        javax.swing.JPanel springHolder = new javax.swing.JPanel(new javax.swing.SpringLayout());
        springHolder.add(pUpper);
        // springHolder.add(flowHolder);
        springHolder.add(textBoxSpring);
        springHolder.add(buttonHolder);
        SpringLayoutHelper.setupSpringGrid(springHolder, 3, 1);
        getContentPane().add(springHolder);

        clearMechPreview();
        setSize(785, 560);
        setResizable(false);

        // set button usability based on server settings
        bRandom.setEnabled(Boolean.parseBoolean(client.getServerConfigs("RandomRepodAllowed")));
        bRePod.setEnabled(!Boolean.parseBoolean(client.getServerConfigs("RandomRepodOnly")));

        // add all the listeners
        mechList.addListSelectionListener(this);
        mechList.addKeyListener(this);
        bRePod.addActionListener(this);
        bCancel.addActionListener(this);
        bRandom.addActionListener(this);
        addWindowListener(this);
    }

    void clearMechPreview() {
        mechViewLeft.setEditable(false);
        mechViewRight.setEditable(false);
        mechViewLeft.setText("");
        mechViewRight.setText("");

        // Remove preview image.
        previewMech(null);

    }

    void previewMech(Entity entity) {

        Entity currEntity = entity;
        boolean populateTextFields = true;

        // null entity, so load a default unit.
        if (entity == null) {
            try {
                // MechSummary ms =
                // MechSummaryCache.getInstance().getMech("Error OMG-UR-FD");
                currEntity = UnitUtils.createOMG();// new
                // MechFileParser(ms.getSourceFile(),
                // ms.getEntryName()).getEntity();
                populateTextFields = false;
            } catch (Exception e) {
                // this would be very very bad ...
            }
        }

        ConfigurableMekViewPanel mechView = null;
        try {
            mechView = new ConfigurableMekViewPanel(currEntity);
        } catch (Exception e) {
            // error unit didn't load right. this is bad news.
            populateTextFields = false;
        }

        mechViewLeft.setEditable(false);
        mechViewRight.setEditable(false);
        if (populateTextFields) {
            mechViewLeft.setText(mechView.getMechReadoutBasic());
            mechViewRight.setText(mechView.getMechReadoutLoadout());
        } else {
            mechViewLeft.setText("No unit selected");
            mechViewRight.setText("No unit selected");
        }
        mechViewLeft.setCaretPosition(0);
        mechViewRight.setCaretPosition(0);

        // Preview image of the unit...
        try {
            ((MekInfo) pPreview).setUnit(currEntity);
            ((MekInfo) pPreview).setImageVisible(true);
            pPreview.paint(pPreview.getGraphics());
        } catch (Exception ex) {
            // shouldnt ever get here ...
        }
    }

    public void run() {

        // Loading mechs can take a while, so it will have its own thread.
        // This prevents the UI from freezing, and allows the
        // "Please wait..." dialog to behave properly on various Java VMs.

        filterMeks();
        sortMechs();
        unitLoadingDialog.setVisible(false);

        final java.util.Map<String, String> hFailedFiles = MekSummaryCache.getInstance().getFailedFiles();
        if ((hFailedFiles != null) && (!hFailedFiles.isEmpty())) {
            new UnitFailureDialog(cMainFrame, hFailedFiles); // self-showing
            // dialog
        }

        try {
            mechList.setSelectedIndex(0);
        } catch (Exception e) {
            mechList.setSelectedIndex(-1);
        }

        pPreview.setVisible(true);
        setVisible(true);
        mechList.requestFocus();
    }

    private void filterMeks() {
        Vector<MekSummary> vMeks = new Vector<>(1, 1);
        MekSummary[] meks = MekSummaryCache.getInstance().getAllMeks();

        // break out if there are no units to filter
        if (meks == null) {
            System.err.println("No units to filter!");
            return;
        }

        int x = 0;
        try {
            for (; x < meks.length; x++) {

                String model = UnitUtils.getMekSummaryFileName(meks[x]);

                if ((chassisList.get(model) != null) && !vMeks.contains(meks[x])) {
                    vMeks.addElement(meks[x]);
                }// end if(chassie)
            }// end for(all meks)
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            System.err.println(STR."meks size: \{meks.length} x: \{x}");
        }
        meksCurrent = new MekSummary[vMeks.size()];
        vMeks.copyInto(meksCurrent);
        sortMechs();
    }

    private void sortMechs() {
        java.util.Arrays.sort(meksCurrent, new MekSummaryComparator(0));
        defaultModel.clear();

        for (int x = 0; x < meksCurrent.length; x++) {
            defaultModel.add(x, formatMek(meksCurrent[x]));
        }
        repaint();
    }

    @Override
    public void setVisible(boolean show) {
        setLocationRelativeTo(null);
        super.setVisible(show);
        pack();
    }

    private String formatMek(MekSummary ms) {
        String result = makeLength(ms.getModel(), 12) +
                              " " +
                              makeLength(ms.getChassis(), 10) +
                              " " +
                              makeLength("" + ms.getTons(), 3) +
                              " " +
                              makeLength("" + ms.getBV(), 5);

        String chassieMods = chassisList.get(UnitUtils.getMekSummaryFileName(ms));
        // MWLogger.errLog("Name: "+ms.getName()+" Mods: "+chassieMods);

        java.util.StringTokenizer mods = new java.util.StringTokenizer(chassieMods, "$");
        result += " " + makeLength(mods.nextToken() + client.moneyOrFluMessage(true, true, -1), 5);
        result += " " + makeLength(mods.nextToken() + "cp", 7);
        result += " " + makeLength(mods.nextToken() + client.moneyOrFluMessage(false, true, -1), 5);

        return result;
        // ms.getYear();
    }

    /**
     * for compliance with ListSelectionListener
     */
    public void valueChanged(javax.swing.event.ListSelectionEvent event) {

        int selected = mechList.getSelectedIndex();
        if (selected == -1) {
            clearMechPreview();
            return;
        }
        // else
        MekSummary ms = meksCurrent[selected];
        try {
            Entity entity = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            previewMech(entity);
        } catch (EntityLoadingException ex) {
            System.out.println(STR."Unable to load mech: \{ms.getSourceFile()}: \{ms.getEntryName()}: \{ex.getMessage()}");
            MWLogger.errLog(ex);
            clearMechPreview();
        }
    }

    public void itemStateChanged(java.awt.event.ItemEvent ie) {

        Object currSelection = mechList.getSelectedValue();

        sortMechs();
        filterMeks();

        // try to reselect the previous choice. if the choice cant be found,
        // the list automatically reverts to -1 (no selection)
        mechList.setSelectedValue(currSelection, true);
    }

    private String makeLength(String s, int nLength) {
        if (s.length() == nLength) {
            return s;
        } else if (s.length() > nLength) {
            return s.substring(0, nLength - 2) + "..";
        } else {
            return s + SPACES.substring(0, nLength - s.length());
        }
    }

    public void keyTyped(java.awt.event.KeyEvent ke) {
    }

    public void keyPressed(java.awt.event.KeyEvent ke) {
        if (ke.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            java.awt.event.ActionEvent event = new java.awt.event.ActionEvent(bCancel,
                  java.awt.event.ActionEvent.ACTION_PERFORMED,
                  "");
            actionPerformed(event);
        }
        long curTime = System.currentTimeMillis();
        if ((curTime - m_nLastSearch) > KEY_TIMEOUT) {
            m_sbSearch = new StringBuilder();
        }
        m_nLastSearch = curTime;
        m_sbSearch.append(ke.getKeyChar());
        searchFor(m_sbSearch.toString().toLowerCase());
    }

    public void actionPerformed(java.awt.event.ActionEvent ae) {
        if (ae.getSource() == bCancel) {
            dispose();
        }
        if (ae.getSource() == bRePod) {
            try {
                MekSummary ms = meksCurrent[mechList.getSelectedIndex()];

                String unitFile = UnitUtils.getMekSummaryFileName(ms);
                if (global) {
                    client.sendChat(IClient.CAMPAIGN_PREFIX + "c repod#" + unitId + "#GLOBAL#" + unitFile);
                } else {
                    client.sendChat(IClient.CAMPAIGN_PREFIX + "c repod#" + unitId + "#" + unitFile);
                }
                Thread.sleep(125);
                dispose();
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
        }
        if (ae.getSource() == bRandom) {
            try {
                if (global) {
                    client.sendChat(IClient.CAMPAIGN_PREFIX + "c repod#" + unitId + "#GLOBAL#RANDOM");
                } else {
                    client.sendChat(IClient.CAMPAIGN_PREFIX + "c repod#" + unitId + "#RANDOM");
                }
                Thread.sleep(125);
                dispose();
            } catch (Exception ex) {
                MWLogger.errLog(ex);
                // MMClient.mwClientLog.clientErrLog("Problem with actionPerformed in RepodDialog");
            }
        }
    }

    private void searchFor(String search) {
        for (int i = 0; i < meksCurrent.length; i++) {
            if (meksCurrent[i].getName().toLowerCase().startsWith(search)) {
                mechList.setSelectedIndex(i);
                mechList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    public void keyReleased(java.awt.event.KeyEvent ke) {
        // no action on release
    }

    public void windowOpened(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        dispose();
    }

    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowIconified(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {
    }

    // WindowListener
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {
    }
}
