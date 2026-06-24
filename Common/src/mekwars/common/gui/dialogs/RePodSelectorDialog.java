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
package mekwars.common.gui.dialogs;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.Serial;
import java.util.Arrays;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.ui.dialogs.UnitFailureDialog;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.dialogs.unitSelectorDialogs.ConfigurableMekViewPanel;
import megamek.common.comparators.MekSummaryComparator;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.CMainFrame;
import mekwars.common.gui.MekInfo;
import mekwars.common.util.SpringLayoutHelper;
import mekwars.common.util.UnitUtils;

/*
 * Allows a user to sort through a list of MechSummaries and select one
 */

public class RePodSelectorDialog extends JFrame
      implements ActionListener, KeyListener, ListSelectionListener, Runnable, WindowListener, ItemListener {
    private final static MMLogger LOGGER = MMLogger.create(RePodSelectorDialog.class);

    @Serial
    private static final long serialVersionUID = -6467246609231845514L;

    // how long after a key is typed does a new search begin
    private final static int KEY_TIMEOUT = 1000;
    private static final String SPACES = "                        ";
    // frame which owns the dialog
    private final CMainFrame cMainFrame;
    private final UnitLoadingDialog unitLoadingDialog;
    private final JButton bRePod = new JButton("RePod");
    private final JButton bCancel = new JButton("Close");
    private final JButton bRandom = new JButton("Random");
    private final JTextPane mechViewLeft;
    private final JTextPane mechViewRight;
    private final IClient client;
    private final TreeMap<String, String> chassisList = new TreeMap<>();
    private final String unitId;
    private final DefaultListModel<String> defaultModel;
    private final JList<String> mekList;
    private MekSummary[] meksCurrent;
    private StringBuilder m_sbSearch = new StringBuilder();
    private long m_nLastSearch = 0;
    private JPanel pPreview = new JPanel();
    private boolean global = false;

    public RePodSelectorDialog(CMainFrame cMainFrame, UnitLoadingDialog uld, IClient client,
          String chassisList, String unitId) {
        super("RePod Selector");

        // save params
        this.cMainFrame = cMainFrame;
        unitLoadingDialog = uld;
        this.client = client;
        StringTokenizer stringTokenizer = new StringTokenizer(chassisList, "#");

        while (stringTokenizer.hasMoreElements()) {
            String tempString = stringTokenizer.nextToken();
            if (tempString.equals("GLOBAL")) {
                global = true;
            } else if (tempString.contains(".")) {
                String chassisMods = stringTokenizer.nextToken();
                this.chassisList.put(tempString, chassisMods);
            }
        }

        this.unitId = unitId;

        // construct 2 text boxes
        mechViewLeft = new JTextPane(); //(22, 29);
        mechViewRight = new JTextPane(); //(22, 34);

        mechViewLeft.setContentType("text/html");
        mechViewRight.setContentType("text/html");

        // construct a model and list
        defaultModel = new DefaultListModel<>();
        mekList = new JList<>(defaultModel);
        ListSelectionModel listSelectionModel = mekList.getSelectionModel();
        mekList.setVisibleRowCount(22);// give the list same number of rows as
        // the text boxes
        listSelectionModel.addListSelectionListener(this);

        // place the list and text boxes in scroll panes
        JScrollPane listScrollPane = new JScrollPane(mekList);
        JScrollPane leftScrollPane = new JScrollPane(mechViewLeft);
        JScrollPane rightScrollPane = new JScrollPane(mechViewRight);

        // set list/scroll options
        listScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        listScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        leftScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        rightScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mekList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // set fonts
        mechViewLeft.setFont(new Font("Monospaced", Font.PLAIN, 11));
        mechViewRight.setFont(new Font("Monospaced", Font.PLAIN, 11));
        mekList.setFont(new Font("Monospaced", Font.PLAIN, 11));

        // set up the upper panel (combo boxes, preview image)
        pPreview = new MekInfo(client);
        pPreview.setVisible(false);
        pPreview.setMinimumSize(new Dimension(86, 74));
        pPreview.setMaximumSize(new Dimension(86, 74));
        JPanel pUpper = new JPanel();
        pUpper.setLayout(new FlowLayout(FlowLayout.CENTER));
        JPanel pParams = new JPanel();
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
        buttonHolder.add(bRePod);
        buttonHolder.add(bRandom);
        buttonHolder.add(bCancel);

        // set a default button
        getRootPane().setDefaultButton(bCancel);

        // set up the overall SpringLayout
        JPanel springHolder = new JPanel(new SpringLayout());
        springHolder.add(pUpper);
        springHolder.add(textBoxSpring);
        springHolder.add(buttonHolder);
        SpringLayoutHelper.setupSpringGrid(springHolder, 3, 1);
        getContentPane().add(springHolder);

        clearMechPreview();
        setSize(785, 560);
        setResizable(false);

        // set button usability based on server settings
        bRandom.setEnabled(Boolean.parseBoolean(client.getServerConfigs("RandomRePodAllowed")));
        bRePod.setEnabled(!Boolean.parseBoolean(client.getServerConfigs("RandomRePodOnly")));

        // add all the listeners
        mekList.addListSelectionListener(this);
        mekList.addKeyListener(this);
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
        previewMek(null);

    }

    void previewMek(Entity entity) {
        Entity currEntity = entity;
        boolean populateTextFields = true;

        // null entity, so load a default unit.
        if (entity == null) {
            currEntity = UnitUtils.createOMG();// new
            populateTextFields = false;
        }

        ConfigurableMekViewPanel mekViewPanel = null;

        try {
            mekViewPanel = new ConfigurableMekViewPanel(currEntity);
        } catch (Exception e) {
            // the error unit didn't load right. this is bad news.
            populateTextFields = false;
        }

        mechViewLeft.setEditable(false);
        mechViewRight.setEditable(false);

        if (populateTextFields) {
            mechViewLeft.setText(mekViewPanel.getMechReadoutBasic());
            mechViewRight.setText(mekViewPanel.getMechReadoutLoadout());
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
            // shouldn't ever get here ...
        }
    }

    public void run() {

        // Loading meks can take a while, so it will have its own thread.
        // This prevents the UI from freezing and allows the
        // "Please wait..." dialog to behave properly on various Java VMs.

        filterMeks();
        sortMeks();
        unitLoadingDialog.setVisible(false);

        final Map<String, String> hFailedFiles = MekSummaryCache.getInstance().getFailedFiles();

        if ((hFailedFiles != null) && (!hFailedFiles.isEmpty())) {
            new UnitFailureDialog(cMainFrame, hFailedFiles); // self-showing
            // dialog
        }

        try {
            mekList.setSelectedIndex(0);
        } catch (Exception e) {
            mekList.setSelectedIndex(-1);
        }

        pPreview.setVisible(true);
        setVisible(true);
        mekList.requestFocus();
    }

    private void filterMeks() {
        Vector<MekSummary> vMeks = new Vector<>(1, 1);
        MekSummary[] meks = MekSummaryCache.getInstance().getAllMeks();

        // break out if there are no units to filter
        if (meks == null) {
            LOGGER.error("No units to filter!");
            return;
        }

        int x = 0;

        try {
            for (; x < meks.length; x++) {
                String model = UnitUtils.getMekSummaryFileName(meks[x]);

                if ((chassisList.get(model) != null) && !vMeks.contains(meks[x])) {
                    vMeks.addElement(meks[x]);
                }// end if(chassis)
            }// end for(all meks)
        } catch (Exception ex) {
            LOGGER.error(ex, STR."meks size: \{meks.length} x: \{x}");
        }

        meksCurrent = new MekSummary[vMeks.size()];
        vMeks.copyInto(meksCurrent);
        sortMeks();
    }

    private void sortMeks() {
        Arrays.sort(meksCurrent, new MekSummaryComparator(MekSummaryComparator.T_CHASSIS));
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

    private String formatMek(MekSummary mekSummary) {
        String result = STR."\{makeLength(mekSummary.getModel(), 12)} \{makeLength(mekSummary.getChassis(),
              10)} \{makeLength(STR."\{mekSummary.getTons()}",
              3)} \{makeLength(STR."\{mekSummary.getBV()}", 5)}";

        String chassisMods = chassisList.get(UnitUtils.getMekSummaryFileName(mekSummary));

        java.util.StringTokenizer mods = new java.util.StringTokenizer(chassisMods, "$");
        result += STR." \{makeLength(mods.nextToken() + client.moneyOrFluMessage(true, true, -1), 5)}";
        result += STR." \{makeLength(STR."\{mods.nextToken()}cp", 7)}";
        result += STR." \{makeLength(mods.nextToken() + client.moneyOrFluMessage(false, true, -1), 5)}";

        return result;
    }

    /**
     * for compliance with ListSelectionListener
     */
    public void valueChanged(ListSelectionEvent event) {

        int selected = mekList.getSelectedIndex();

        if (selected == -1) {
            clearMechPreview();
            return;
        }

        // else
        MekSummary mekSummary = meksCurrent[selected];

        try {
            Entity entity = new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName()).getEntity();
            previewMek(entity);
        } catch (EntityLoadingException ex) {
            LOGGER.error(ex,
                  STR."Unable to load mech: \{mekSummary.getSourceFile()}: \{mekSummary.getEntryName()}: \{ex.getMessage()}");
            clearMechPreview();
        }
    }

    public void itemStateChanged(ItemEvent itemEvent) {
        Object currSelection = mekList.getSelectedValue();

        sortMeks();
        filterMeks();

        // try to reselect the previous choice. if the choice cant be found,
        // the list automatically reverts to -1 (no selection)
        mekList.setSelectedValue(currSelection, true);
    }

    private String makeLength(String string, int nLength) {
        if (string.length() == nLength) {
            return string;
        } else if (string.length() > nLength) {
            return STR."\{string.substring(0, nLength - 2)}..";
        } else {
            return string + SPACES.substring(0, nLength - string.length());
        }
    }

    public void keyTyped(KeyEvent keyEvent) {
    }

    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
            ActionEvent event = new ActionEvent(bCancel, ActionEvent.ACTION_PERFORMED, "");
            actionPerformed(event);
        }

        long curTime = System.currentTimeMillis();

        if ((curTime - m_nLastSearch) > KEY_TIMEOUT) {
            m_sbSearch = new StringBuilder();
        }

        m_nLastSearch = curTime;
        m_sbSearch.append(keyEvent.getKeyChar());
        searchFor(m_sbSearch.toString().toLowerCase());
    }

    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == bCancel) {
            dispose();
        }

        if (actionEvent.getSource() == bRePod) {
            try {
                MekSummary mekSummary = meksCurrent[mekList.getSelectedIndex()];

                String unitFile = UnitUtils.getMekSummaryFileName(mekSummary);

                if (global) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c repod#\{unitId}#GLOBAL#\{unitFile}");
                } else {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c repod#\{unitId}#\{unitFile}");
                }

                Thread.sleep(125);
                dispose();
            } catch (Exception ex) {
                LOGGER.error(ex, "Error in RePod Action Performed: {}", ex.getLocalizedMessage());
            }
        }

        if (actionEvent.getSource() == bRandom) {
            try {
                if (global) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c repod#\{unitId}#GLOBAL#RANDOM");
                } else {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c repod#\{unitId}#RANDOM");
                }
                Thread.sleep(125);
                dispose();
            } catch (Exception ex) {
                LOGGER.error(ex, "Error in RePod Action Performed (Random): {}", ex.getLocalizedMessage());
            }
        }
    }

    private void searchFor(String search) {
        for (int i = 0; i < meksCurrent.length; i++) {
            if (meksCurrent[i].getName().toLowerCase().startsWith(search)) {
                mekList.setSelectedIndex(i);
                mekList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    public void keyReleased(KeyEvent keyEvent) {
        // no action on release
    }

    public void windowOpened(WindowEvent windowEvent) {
    }

    public void windowClosing(WindowEvent windowEvent) {
        dispose();
    }

    public void windowClosed(WindowEvent windowEvent) {
    }

    public void windowIconified(WindowEvent windowEvent) {
    }

    public void windowDeiconified(WindowEvent windowEvent) {
    }

    // WindowListener
    public void windowActivated(WindowEvent windowEvent) {
    }

    public void windowDeactivated(WindowEvent windowEvent) {
    }
}
