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

package client.gui.dialog;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowListener;
import java.util.Arrays;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import client.MWClient;
import client.gui.CMainFrame;
import client.gui.MechInfo;
import common.util.MWLogger;
import common.util.SpringLayoutHelper;
import common.util.UnitUtils;
import megamek.client.ui.swing.UnitFailureDialog;
import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MechSummaryComparator;
import megamek.common.MechView;
import megamek.common.loaders.EntityLoadingException;

/*
 * Allows a user to sort through a list of MechSummaries and select one
 */

public class RepodSelectorDialog extends JFrame implements ActionListener, KeyListener, ListSelectionListener, Runnable, WindowListener, ItemListener {

    /**
     *
     */
    private static final long serialVersionUID = -6467246609231845514L;

    // how long after a key is typed does a new search begin
    private final static int KEY_TIMEOUT = 1000;

    // frame which owns the dialog
    private CMainFrame clientgui;

    private MechSummary[] mechsCurrent;
    private UnitLoadingDialog unitLoadingDialog;

    private StringBuilder m_sbSearch = new StringBuilder();
    private long m_nLastSearch = 0;

    private JPanel pParams = new JPanel();

    DefaultListModel<String> defaultModel = null;
    ListSelectionModel listSelectionModel = null;
    JList<String> mechList = null;
    JScrollPane listScrollPane = null;
    JScrollPane leftScrollPane = null;
    JScrollPane rightScrollPane = null;

    private JButton bRepod = new JButton("Repod");
    private JButton bCancel = new JButton("Close");
    private JButton bRandom = new JButton("Random");
    private JTextPane mechViewLeft = null;
    private JTextPane mechViewRight = null;

    private JPanel pUpper = new JPanel();
    private JPanel pPreview = new JPanel();

    private MWClient mwclient = null;

    private TreeMap<String, String> chassieList = new TreeMap<String, String>();

    private String unitId = "";

    private boolean global = false;

    public RepodSelectorDialog(CMainFrame cl, UnitLoadingDialog uld, MWClient mwclient, String chassieList, String unitId) {
        super("Repod Selector");

        // save params
        clientgui = cl;
        unitLoadingDialog = uld;
        this.mwclient = mwclient;
        StringTokenizer ST = new StringTokenizer(chassieList, "#");

        while (ST.hasMoreElements()) {
            String tempstr = ST.nextToken();
            if (tempstr.equals("GLOBAL")) {
                global = true;
            } else if (tempstr.contains(".")) {
                String chassieMods = ST.nextToken();
                // MWLogger.errLog("Chassie: "+tempstr+" mods: "+chassieMods+" ChassieList: "+chassieList);
                this.chassieList.put(tempstr, chassieMods);
            }
        }

        this.unitId = unitId;

        // construct 2 text boxes
        mechViewLeft = new JTextPane(); //(22, 29);
        mechViewRight = new JTextPane(); //(22, 34);

        mechViewLeft.setContentType("text/html");
//        mechViewLeft.set
        
        mechViewRight.setContentType("text/html");
        
        // construct a model and list
        defaultModel = new DefaultListModel<String>();
        mechList = new JList<String>(defaultModel);
        listSelectionModel = mechList.getSelectionModel();
        mechList.setVisibleRowCount(22);// give the list same number of rows as
        // the text boxes
        listSelectionModel.addListSelectionListener(this);

        // place the list and text boxes in scroll panes
        listScrollPane = new JScrollPane(mechList);
        leftScrollPane = new JScrollPane(mechViewLeft);
        rightScrollPane = new JScrollPane(mechViewRight);

        // set list/scroll options
        listScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        listScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        leftScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        rightScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mechList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // set fonts
        mechViewLeft.setFont(new Font("Monospaced", Font.PLAIN, 11));
        mechViewRight.setFont(new Font("Monospaced", Font.PLAIN, 11));
        mechList.setFont(new Font("Monospaced", Font.PLAIN, 11));

        // set up the upper panel (combo boxes, preview image)
        pPreview = new MechInfo(mwclient);
        pPreview.setVisible(false);
        pPreview.setMinimumSize(new Dimension(86, 74));
        pPreview.setMaximumSize(new Dimension(86, 74));
        pUpper.setLayout(new FlowLayout(FlowLayout.CENTER));
        pUpper.add(pParams);
        pUpper.add(pPreview);

        // panel w/ 1x3 SpringLayout for the mechView bits
        JPanel textBoxSpring = new JPanel(new SpringLayout());
        textBoxSpring.add(listScrollPane);
        textBoxSpring.add(leftScrollPane);
        textBoxSpring.add(rightScrollPane);
        SpringLayoutHelper.setupSpringGrid(textBoxSpring, 1, 3);

        // set up a formatting holder for the cancel button
        JPanel buttonHolder = new JPanel();
        buttonHolder.add(bRepod);
        buttonHolder.add(bRandom);
        buttonHolder.add(bCancel);

        // set a default button
        getRootPane().setDefaultButton(bCancel);

        // set up the overall SpringLayout
        JPanel springHolder = new JPanel(new SpringLayout());
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
        bRandom.setEnabled(Boolean.parseBoolean(mwclient.getserverConfigs("RandomRepodAllowed")));
        bRepod.setEnabled(!Boolean.parseBoolean(mwclient.getserverConfigs("RandomRepodOnly")));

        // add all the listeners
        mechList.addListSelectionListener(this);
        mechList.addKeyListener(this);
        bRepod.addActionListener(this);
        bCancel.addActionListener(this);
        bRandom.addActionListener(this);
        addWindowListener(this);
    }

    public void run() {

        // Loading mechs can take a while, so it will have its own thread.
        // This prevents the UI from freezing, and allows the
        // "Please wait..." dialog to behave properly on various Java VMs.

        filterMechs();
        sortMechs();
        unitLoadingDialog.setVisible(false);

        final Map<String, String> hFailedFiles = MechSummaryCache.getInstance().getFailedFiles();
        if ((hFailedFiles != null) && (hFailedFiles.size() > 0)) {
            new UnitFailureDialog(clientgui, hFailedFiles); // self-showing
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

    private void filterMechs() {

        Vector<MechSummary> vMechs = new Vector<MechSummary>(1, 1);
        MechSummary[] mechs = MechSummaryCache.getInstance().getAllMechs();

        // break out if there are no units to filter
        if (mechs == null) {
            System.err.println("No units to filter!");
            return;
        }

        int x = 0;
        try {
            for (; x < mechs.length; x++) {

                // String chassis = mechs[x].getChassis();
                String model = UnitUtils.getMechSummaryFileName(mechs[x]);

                if ((chassieList.get(model) != null) && !vMechs.contains(mechs[x].getModel())) {
                    vMechs.addElement(mechs[x]);
                }// end if(chassie)
            }// end for(all mechs)
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            System.err.println("mechs size: " + mechs.length + " x: " + x);
        }
        mechsCurrent = new MechSummary[vMechs.size()];
        vMechs.copyInto(mechsCurrent);
        sortMechs();
    }

    private void sortMechs() {
        Arrays.sort(mechsCurrent, new MechSummaryComparator(0));
        defaultModel.clear();

        for (int x = 0; x < mechsCurrent.length; x++) {
            defaultModel.add(x, formatMech(mechsCurrent[x]));
        }
        repaint();
    }

    private void searchFor(String search) {
        for (int i = 0; i < mechsCurrent.length; i++) {
            if (mechsCurrent[i].getName().toLowerCase().startsWith(search)) {
                mechList.setSelectedIndex(i);
                mechList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    @Override
    public void setVisible(boolean show) {
        setLocationRelativeTo(null);
        super.setVisible(show);
        pack();
    }

    private String formatMech(MechSummary ms) {
        // return makeLength(ms.getModel(), 10) + " " +
        // makeLength(ms.getChassis(), 20) + " " +
        // makeLength("" + ms.getTons(), 3) + " " +
        // makeLength("" + ms.getBV(),5)+""+
        // ms.getYear();

        String result = makeLength(ms.getModel(), 12) + " " + makeLength(ms.getChassis(), 10) + " " + makeLength("" + ms.getTons(), 3) + " " + makeLength("" + ms.getBV(), 5);

        String chassieMods = chassieList.get(UnitUtils.getMechSummaryFileName(ms));
        // MWLogger.errLog("Name: "+ms.getName()+" Mods: "+chassieMods);

        StringTokenizer mods = new StringTokenizer(chassieMods, "$");
        result += " " + makeLength(mods.nextToken() + mwclient.moneyOrFluMessage(true, true, -1), 5);
        result += " " + makeLength(mods.nextToken() + "cp", 7);
        result += " " + makeLength(mods.nextToken() + mwclient.moneyOrFluMessage(false, true, -1), 5);

        return result;
        // ms.getYear();
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == bCancel) {
            dispose();
        }
        if (ae.getSource() == bRepod) {
            try {
                MechSummary ms = mechsCurrent[mechList.getSelectedIndex()];

                String unitFile = UnitUtils.getMechSummaryFileName(ms);
                if (global) {
                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c repod#" + unitId + "#GLOBAL#" + unitFile);
                } else {
                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c repod#" + unitId + "#" + unitFile);
                }
                Thread.sleep(125);
                dispose();
            } catch (Exception ex) {
                MWLogger.errLog(ex);
                // MMClient.mwClientLog.clientErrLog("Problem with actionPerformed in RepodDialog");
            }
        }
        if (ae.getSource() == bRandom) {
            try {
                if (global) {
                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c repod#" + unitId + "#GLOBAL#RANDOM");
                } else {
                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c repod#" + unitId + "#RANDOM");
                }
                Thread.sleep(125);
                dispose();
            } catch (Exception ex) {
                MWLogger.errLog(ex);
                // MMClient.mwClientLog.clientErrLog("Problem with actionPerformed in RepodDialog");
            }
        }
    }

    /**
     * for compliance with ListSelectionListener
     */
    public void valueChanged(ListSelectionEvent event) {

        int selected = mechList.getSelectedIndex();
        if (selected == -1) {
            clearMechPreview();
            return;
        }
        // else
        MechSummary ms = mechsCurrent[selected];
        try {
            Entity entity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            previewMech(entity);
        } catch (EntityLoadingException ex) {
            System.out.println("Unable to load mech: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage());
            MWLogger.errLog(ex);
            clearMechPreview();
            return;
        }
    }

    public void itemStateChanged(ItemEvent ie) {

        Object currSelection = mechList.getSelectedValue();

        sortMechs();
        filterMechs();

        // try to reselect the previous choice. if the choice cant be found,
        // the list automatically reverts to -1 (no selection)
        mechList.setSelectedValue(currSelection, true);
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

        MechView mechView = null;
        try {
            mechView = new MechView(currEntity, true);
        } catch (Exception e) {
            // error unit didn't load right. this is bad news.
            populateTextFields = false;
        }

        mechViewLeft.setEditable(false);
        mechViewRight.setEditable(false);
        if (populateTextFields && (mechView != null)) {
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
            ((MechInfo) pPreview).setUnit(currEntity);
            ((MechInfo) pPreview).setImageVisible(true);
            pPreview.paint(pPreview.getGraphics());
        } catch (Exception ex) {
            // shouldnt ever get here ...
        }
    }

    private static final String SPACES = "                        ";

    private String makeLength(String s, int nLength) {
        if (s.length() == nLength) {
            return s;
        } else if (s.length() > nLength) {
            return s.substring(0, nLength - 2) + "..";
        } else {
            return s + SPACES.substring(0, nLength - s.length());
        }
    }

    public void keyReleased(java.awt.event.KeyEvent ke) {
        // no action on release
    }

    public void keyPressed(java.awt.event.KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
            ActionEvent event = new ActionEvent(bCancel, ActionEvent.ACTION_PERFORMED, "");
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

    public void keyTyped(java.awt.event.KeyEvent ke) {
    }

    // WindowListener
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        dispose();
    }

    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowIconified(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowOpened(java.awt.event.WindowEvent windowEvent) {
    }
}
