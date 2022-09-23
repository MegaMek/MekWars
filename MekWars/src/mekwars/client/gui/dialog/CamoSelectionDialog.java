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

package client.gui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import client.GUIClientConfig;
import client.MWClient;
import client.campaign.CUnit;
import client.gui.MechInfo;
import common.util.MWLogger;
import common.util.UnitUtils;
import megamek.common.Entity;
import megamek.common.Player;
import megamek.common.icons.Camouflage;

/*
 * 
 * @author urgru
 * 
 * inner class which sets up a camo selection dialog.
 */
public class CamoSelectionDialog extends JDialog implements ListSelectionListener, ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 491308053668750747L;
    // IVARS
    private TreeMap<String, Object> camos;
    private JList<String> camoList;
    private DefaultListModel<String> listModel;
    private MechInfo oldCamo;// mechinfo is a JPanel extension
    private MechInfo newCamo;// mechinfo is a JPanel extension
    private JScrollPane scrollPane;// holds the JList
    private Entity oldEntity;
    private Entity newEntity;
    private String originalCamo = "";
    private MWClient mwclient;
    private final JButton okayButton = new JButton("OK");
    private final JButton cancelButton = new JButton("Cancel");
    private final String okayCommand = "Okay";

    // CONSTRUCTOR
    // public CamoSelectionDialog(JFrame parent, MMClient client, ConfigPage
    // loopback) {
    public CamoSelectionDialog(JFrame parent, MWClient mwclient) {

        // init superclass
        super(parent, "Select Camo Pattern", true);

        // save the client
        this.mwclient = mwclient;

        // save the original camo
        originalCamo = mwclient.getConfigParam("UNITCAMO");

        // set up entities
        try {

            // show a random unit from the player's hangar
            CUnit toShow = null;
            int hangarSize = mwclient.getPlayer().getHangar().size();
            if (hangarSize > 0) {
                Random r = new Random();
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
        listModel = new DefaultListModel<String>();
        listModel.addElement(Camouflage.NO_CAMOUFLAGE);

        // Get camo file names.
        camos = new TreeMap<String, Object>();
        File camoDirectory = new File("./data/images/camo");
        String[] camoNames = camoDirectory.list();

        // alpha-sort the camo names
        Arrays.sort(camoNames);

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
        JPanel oldPanel = new JPanel();
        oldPanel.setLayout(new BoxLayout(oldPanel, BoxLayout.Y_AXIS));

        JLabel oldHeader = new JLabel("Old Camo", SwingConstants.CENTER);
        oldHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        oldPanel.add(oldHeader);

        String oldCamoName = mwclient.getConfig().getParam("UNITCAMO");
        Image oldCamoImage = Toolkit.getDefaultToolkit().getImage("./data/images/camo/" + oldCamoName);
        oldCamoImage.getScaledInstance(84, 72, Image.SCALE_FAST);
        camos.remove(oldCamoName);// remove the old
        ImageIcon oldCamoIcon = new ImageIcon(oldCamoImage);
        camos.put(oldCamoName, oldCamoIcon);

        oldCamo = new MechInfo(oldCamoIcon);
        oldCamo.setUnit(oldEntity);
        oldCamo.setMinimumSize(new Dimension(84, 72));
        oldPanel.add(oldCamo);

        // Create the "new camo" icon.
        JPanel newPanel = new JPanel();
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        JLabel newHeader = new JLabel("New Camo", SwingConstants.CENTER);
        newHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        newPanel.add(newHeader);

        newCamo = new MechInfo(new ImageIcon());
        newCamo.setUnit(newEntity);
        newCamo.setMinimumSize(new Dimension(84, 72));
        newPanel.add(newCamo);

        // create a panel to hold the icons.
        JPanel iconPanel = new JPanel();
        iconPanel.setLayout(new BoxLayout(iconPanel, BoxLayout.Y_AXIS));
        iconPanel.add(oldPanel);
        iconPanel.add(new JLabel("\n "));// spacer
        iconPanel.add(new JLabel("\n "));// spacer
        iconPanel.add(newPanel);

        // create the actual list and put it in a scroll pane
        camoList = new JList<String>(listModel);
        camoList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        camoList.setLayoutOrientation(JList.VERTICAL);
        camoList.setVisibleRowCount(-1);
        scrollPane = new JScrollPane(camoList);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(280, 425));

        // update the "new camo" icon when an item is selected.
        ListSelectionModel listSelectionModel = camoList.getSelectionModel();
        listSelectionModel.addListSelectionListener(this);

        // now, select the 1st camo on the list. this
        // will trigger image placement for the new-camo-icon
        camoList.setSelectedValue(oldCamoName, true);

        // set a default button
        getRootPane().setDefaultButton(okayButton);

        // Perform the initial layout.
        JPanel listandIconFlow = new JPanel();
        listandIconFlow.add(scrollPane);
        listandIconFlow.add(iconPanel);
        JPanel buttonFlow = new JPanel();
        buttonFlow.add(okayButton);
        buttonFlow.add(cancelButton);
        JPanel generalLayout = new JPanel();
        generalLayout.setLayout(new BoxLayout(generalLayout, BoxLayout.Y_AXIS));
        generalLayout.add(listandIconFlow);
        generalLayout.add(buttonFlow);
        getContentPane().add(generalLayout);
        pack();
        setResizable(false);

        // center the dialog.
        setLocationRelativeTo(null);

    }

    /**
     * OK or CANCEL buttons pressed. Handle any changes and then close the
     * dialouge.
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        // accepted the change, so push it back to the ConfigDialog
        String currCamo = (String) camoList.getSelectedValue();
        if (command.equals(okayCommand) && !currCamo.equals(originalCamo)) {

            // set and save the config
            mwclient.getConfig().setParam("UNITCAMO", currCamo);
            mwclient.getConfig().saveConfig();
            mwclient.setConfig();

            // then reload images and update the GUI
            mwclient.getConfig().loadImage(GUIClientConfig.CAMO_PATH + currCamo, "CAMO", 84, 72);
            mwclient.getMainFrame().getMainPanel().selectFirstTab();
            mwclient.getMainFrame().getMainPanel().getHQPanel().reinitialize();
        }

        // dispose of the dialog
        dispose();

    }// end actionPerformed

    /**
     * Update the "new camo" icon whenever a list item is selected.
     * 
     * @param event
     *            - ItemEvent from the list.
     */
    public void valueChanged(ListSelectionEvent event) {

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
                ImageIcon currCamoIcon = null;
                if (camos.get(currSelection).equals("filler")) {
                    Image currCamo = Toolkit.getDefaultToolkit().getImage("./data/images/camo/" + currSelection);
                    currCamo.getScaledInstance(84, 72, Image.SCALE_FAST);
                    camos.remove(currSelection);// remove the old
                    currCamoIcon = new ImageIcon(currCamo);
                    camos.put(currSelection, currCamoIcon);
                } else {
                    currCamoIcon = (ImageIcon) camos.get(currSelection);
                }

                // set the new icon.
                newCamo.setPreviewIcon(currCamoIcon);
                newCamo.setUnit(newEntity);

            }// end else

            newCamo.repaint();
        }
    }// end valueChanged()
}// end CamoSelectionDialog