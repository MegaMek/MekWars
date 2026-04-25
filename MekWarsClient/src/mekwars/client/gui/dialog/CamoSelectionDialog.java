/*
 * MekWars - Copyright (C) 2004
 *
 * original author - nmorris (urgru@users.sourceforge.net)
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

package mekwars.client.gui.dialog;

import common.util.MWLogger;
import common.util.UnitUtils;
import megamek.common.Entity;
import megamek.common.icons.Camouflage;

/*
 *
 * @author urgru
 *
 * inner class which sets up a camo selection dialog.
 */
public class CamoSelectionDialog extends javax.swing.JDialog
      implements javax.swing.event.ListSelectionListener, java.awt.event.ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = 491308053668750747L;
    // IVARS
    private java.util.TreeMap<String, Object> camos;
    private javax.swing.JList<String> camoList;
    private javax.swing.DefaultListModel<String> listModel;
    private client.gui.MechInfo oldCamo;// mechinfo is a JPanel extension
    private client.gui.MechInfo newCamo;// mechinfo is a JPanel extension
    private javax.swing.JScrollPane scrollPane;// holds the JList
    private Entity oldEntity;
    private Entity newEntity;
    private String originalCamo = "";
    private client.MWClient mwclient;
    private final javax.swing.JButton okayButton = new javax.swing.JButton("OK");
    private final javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
    private final String okayCommand = "Okay";

    // CONSTRUCTOR
    // public CamoSelectionDialog(JFrame parent, MMClient client, ConfigPage
    // loopback) {
    public CamoSelectionDialog(javax.swing.JFrame parent, client.MWClient mwclient) {

        // init superclass
        super(parent, "Select Camo Pattern", true);

        // save the client
        this.mwclient = mwclient;

        // save the original camo
        originalCamo = mwclient.getConfigParam("UNITCAMO");

        // set up entities
        try {

            // show a random unit from the player's hangar
            client.campaign.CUnit toShow = null;
            int hangarSize = mwclient.getPlayer().getHangar().size();
            if (hangarSize > 0) {
                java.util.Random r = new java.util.Random();
                toShow = mwclient.getPlayer().getHangar().get(r.nextInt(hangarSize));
            }

            if (toShow != null) {
                oldEntity = toShow.getEntity();
                newEntity = toShow.getEntity();
            } else {
                // MechSummary ms =
                // MechSummaryCache.getInstance().getMech("Error OMG-UR-FD");
                oldEntity = UnitUtils.createOMG();// new
                // MechFileParser(ms.getSourceFile(),
                // ms.getEntryName()).getEntity();
                newEntity = UnitUtils.createOMG();// new
                // MechFileParser(ms.getSourceFile(),
                // ms.getEntryName()).getEntity();
            }

        } catch (Exception e) {
            MWLogger.errLog(e);
            dispose();
            return;
        }

        // set up the buttons
        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);

        // Create a list model and add NO CAMO
        listModel = new javax.swing.DefaultListModel<String>();
        listModel.addElement(Camouflage.NO_CAMOUFLAGE);

        // Get camo file names.
        camos = new java.util.TreeMap<String, Object>();
        java.io.File camoDirectory = new java.io.File("./data/images/camo");
        String[] camoNames = camoDirectory.list();

        // alpha-sort the camo names
        java.util.Arrays.sort(camoNames);

        for (String currCamoName : camoNames) {
            // get the file extension
            String ext = "";
            int offset = currCamoName.lastIndexOf('.');
            if (offset > 0 && offset < currCamoName.length() - 1) {
                ext = currCamoName.substring(offset + 1).toLowerCase();
            }

            // if its an image, add it to the lists.
            if (ext.equals("png") || ext.equals("jpeg") || ext.equals("jpg") || ext.equals("gif")) {
                camos.put(currCamoName, "filler");
                listModel.addElement(currCamoName);
            }
        }// end for(all files in dir)

        // create the "old camo" icon.
        javax.swing.JPanel oldPanel = new javax.swing.JPanel();
        oldPanel.setLayout(new javax.swing.BoxLayout(oldPanel, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.JLabel oldHeader = new javax.swing.JLabel("Old Camo", javax.swing.SwingConstants.CENTER);
        oldHeader.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        oldPanel.add(oldHeader);

        String oldCamoName = mwclient.getConfig().getParam("UNITCAMO");
        java.awt.Image oldCamoImage = java.awt.Toolkit.getDefaultToolkit()
                                            .getImage("./data/images/camo/" + oldCamoName);
        oldCamoImage.getScaledInstance(84, 72, java.awt.Image.SCALE_FAST);
        camos.remove(oldCamoName);// remove the old
        javax.swing.ImageIcon oldCamoIcon = new javax.swing.ImageIcon(oldCamoImage);
        camos.put(oldCamoName, oldCamoIcon);

        oldCamo = new client.gui.MechInfo(oldCamoIcon);
        oldCamo.setUnit(oldEntity);
        oldCamo.setMinimumSize(new java.awt.Dimension(84, 72));
        oldPanel.add(oldCamo);

        // Create the "new camo" icon.
        javax.swing.JPanel newPanel = new javax.swing.JPanel();
        newPanel.setLayout(new javax.swing.BoxLayout(newPanel, javax.swing.BoxLayout.Y_AXIS));
        javax.swing.JLabel newHeader = new javax.swing.JLabel("New Camo", javax.swing.SwingConstants.CENTER);
        newHeader.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        newPanel.add(newHeader);

        newCamo = new client.gui.MechInfo(new javax.swing.ImageIcon());
        newCamo.setUnit(newEntity);
        newCamo.setMinimumSize(new java.awt.Dimension(84, 72));
        newPanel.add(newCamo);

        // create a panel to hold the icons.
        javax.swing.JPanel iconPanel = new javax.swing.JPanel();
        iconPanel.setLayout(new javax.swing.BoxLayout(iconPanel, javax.swing.BoxLayout.Y_AXIS));
        iconPanel.add(oldPanel);
        iconPanel.add(new javax.swing.JLabel("\n "));// spacer
        iconPanel.add(new javax.swing.JLabel("\n "));// spacer
        iconPanel.add(newPanel);

        // create the actual list and put it in a scroll pane
        camoList = new javax.swing.JList<String>(listModel);
        camoList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        camoList.setLayoutOrientation(javax.swing.JList.VERTICAL);
        camoList.setVisibleRowCount(-1);
        scrollPane = new javax.swing.JScrollPane(camoList);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new java.awt.Dimension(280, 425));

        // update the "new camo" icon when an item is selected.
        javax.swing.ListSelectionModel listSelectionModel = camoList.getSelectionModel();
        listSelectionModel.addListSelectionListener(this);

        // now, select the 1st camo on the list. this
        // will trigger image placement for the new-camo-icon
        camoList.setSelectedValue(oldCamoName, true);

        // set a default button
        getRootPane().setDefaultButton(okayButton);

        // Perform the initial layout.
        javax.swing.JPanel listandIconFlow = new javax.swing.JPanel();
        listandIconFlow.add(scrollPane);
        listandIconFlow.add(iconPanel);
        javax.swing.JPanel buttonFlow = new javax.swing.JPanel();
        buttonFlow.add(okayButton);
        buttonFlow.add(cancelButton);
        javax.swing.JPanel generalLayout = new javax.swing.JPanel();
        generalLayout.setLayout(new javax.swing.BoxLayout(generalLayout, javax.swing.BoxLayout.Y_AXIS));
        generalLayout.add(listandIconFlow);
        generalLayout.add(buttonFlow);
        getContentPane().add(generalLayout);
        pack();
        setResizable(false);

        // center the dialog.
        setLocationRelativeTo(null);

    }

    /**
     * OK or CANCEL buttons pressed. Handle any changes and then close the dialouge.
     */
    public void actionPerformed(java.awt.event.ActionEvent event) {

        String command = event.getActionCommand();

        // accepted the change, so push it back to the ConfigDialog
        String currCamo = (String) camoList.getSelectedValue();
        if (command.equals(okayCommand) && !currCamo.equals(originalCamo)) {

            // set and save the config
            mwclient.getConfig().setParam("UNITCAMO", currCamo);
            mwclient.getConfig().saveConfig();
            mwclient.setConfig();

            // then reload images and update the GUI
            mwclient.getConfig().loadImage(client.GUIClientConfig.CAMO_PATH + currCamo, "CAMO", 84, 72);
            mwclient.getMainFrame().getMainPanel().selectFirstTab();
            mwclient.getMainFrame().getMainPanel().getHQPanel().reinitialize();
        }

        // dispose of the dialog
        dispose();

    }// end actionPerformed

    /**
     * Update the "new camo" icon whenever a list item is selected.
     *
     * @param event - ItemEvent from the list.
     */
    public void valueChanged(javax.swing.event.ListSelectionEvent event) {

        // only care about the final selection, not sliders.
        if (event.getValueIsAdjusting() == false) {

            // If "NO CAMO" is selected, clear the image.
            if (camoList.getSelectedIndex() == 0) {
                newCamo.setPreviewIcon(null);
                newCamo.setUnit(newEntity);
            }

            // set the camo image.
            else {

                // if the image hasnt been loaded and scaled
                // yet, do the load and cache it for future use.
                String currSelection = (String) camoList.getSelectedValue();
                javax.swing.ImageIcon currCamoIcon = null;
                if (camos.get(currSelection).equals("filler")) {
                    java.awt.Image currCamo = java.awt.Toolkit.getDefaultToolkit()
                                                    .getImage("./data/images/camo/" + currSelection);
                    currCamo.getScaledInstance(84, 72, java.awt.Image.SCALE_FAST);
                    camos.remove(currSelection);// remove the old
                    currCamoIcon = new javax.swing.ImageIcon(currCamo);
                    camos.put(currSelection, currCamoIcon);
                } else {
                    currCamoIcon = (javax.swing.ImageIcon) camos.get(currSelection);
                }

                // set the new icon.
                newCamo.setPreviewIcon(currCamoIcon);
                newCamo.setUnit(newEntity);

            }// end else

            newCamo.repaint();
        }
    }// end valueChanged()
}// end CamoSelectionDialog
