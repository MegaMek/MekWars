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

package mekwars.common.gui.dialogs;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serial;
import java.util.Arrays;
import java.util.TreeMap;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.common.icons.Camouflage;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.GUIClientConfig;
import mekwars.common.gui.MekInfo;
import mekwars.common.util.UnitUtils;

/*
 *
 * @author urgru
 *
 * inner class which sets up a camo selection dialog.
 */
public class CamoSelectionDialog extends JDialog implements ListSelectionListener, ActionListener {
    private final static MMLogger LOGGER = MMLogger.create(CamoSelectionDialog.class);

    @Serial
    private static final long serialVersionUID = 491308053668750747L;
    private final String originalCamo;
    private final IClient client;
    private final String okayCommand = "Okay";
    private TreeMap<String, Object> camos;
    private JList<String> camoList;
    private MekInfo newCamo;
    private Entity newEntity;

    // CONSTRUCTOR
    public CamoSelectionDialog(JFrame parent, IClient client) {

        // init superclass
        super(parent, "Select Camo Pattern", true);

        // save the client
        this.client = client;

        // save the original camo
        originalCamo = client.getConfigParam("UNIT_CAMO");

        // set up entities
        Entity oldEntity;

        try {
            // show a random unit from the player's hangar
            CUnit toShow = null;
            int hangarSize = client.getPlayer().getHangar().size();

            if (hangarSize > 0) {
                java.util.Random r = new java.util.Random();
                toShow = client.getPlayer().getHangar().get(r.nextInt(hangarSize));
            }

            if (toShow != null) {
                oldEntity = toShow.getEntity();
                newEntity = toShow.getEntity();
            } else {
                oldEntity = UnitUtils.createOMG();// new
                newEntity = UnitUtils.createOMG();// new
            }
        } catch (Exception e) {
            LOGGER.error(e, "Unable to get Old or New Entity: {}", e.getLocalizedMessage());
            dispose();
            return;
        }

        // set up the buttons
        JButton okayButton = new JButton("OK");
        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

        // Create a list model and add NO CAMO
        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement(Camouflage.NO_CAMOUFLAGE);

        // Get camo file names.
        camos = new TreeMap<>();
        File camoDirectory = new File("./data/images/camo");
        String[] camoNames = camoDirectory.list();

        if (camoNames != null) {
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
        }

        // create the "old camo" icon.
        JPanel oldPanel = new JPanel();
        oldPanel.setLayout(new BoxLayout(oldPanel, BoxLayout.Y_AXIS));

        JLabel oldHeader = new JLabel("Old Camo", SwingConstants.CENTER);
        oldHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        oldPanel.add(oldHeader);

        String oldCamoName = client.getConfig().getParam("UNIT_CAMO");
        Image oldCamoImage = Toolkit.getDefaultToolkit().getImage(String.format("./data/images/camo/%s", oldCamoName));
        oldCamoImage.getScaledInstance(84, 72, Image.SCALE_FAST);
        camos.remove(oldCamoName);// remove the old
        ImageIcon oldCamoIcon = new ImageIcon(oldCamoImage);
        camos.put(oldCamoName, oldCamoIcon);

        MekInfo oldCamo = new MekInfo(oldCamoIcon);
        oldCamo.setUnit(oldEntity);
        oldCamo.setMinimumSize(new Dimension(84, 72));
        oldPanel.add(oldCamo);

        // Create the "new camo" icon.
        JPanel newPanel = new JPanel();
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        JLabel newHeader = new JLabel("New Camo", SwingConstants.CENTER);
        newHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        newPanel.add(newHeader);

        newCamo = new MekInfo(new ImageIcon());
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
        camoList = new JList<>(listModel);
        camoList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        camoList.setLayoutOrientation(JList.VERTICAL);
        camoList.setVisibleRowCount(-1);
        // holds the JList
        JScrollPane scrollPane = new JScrollPane(camoList);
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
        JPanel listAndIconFlow = new JPanel();
        listAndIconFlow.add(scrollPane);
        listAndIconFlow.add(iconPanel);

        JPanel buttonFlow = new JPanel();
        buttonFlow.add(okayButton);
        buttonFlow.add(cancelButton);

        JPanel generalLayout = new JPanel();
        generalLayout.setLayout(new BoxLayout(generalLayout, BoxLayout.Y_AXIS));
        generalLayout.add(listAndIconFlow);
        generalLayout.add(buttonFlow);
        getContentPane().add(generalLayout);
        pack();
        setResizable(false);

        // center the dialog.
        setLocationRelativeTo(null);

    }

    /**
     * OK or CANCEL buttons pressed. Handle any changes and then close the dialog.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        // accepted the change, so push it back to the ConfigDialog
        String currCamo = camoList.getSelectedValue();
        if (command.equals(okayCommand) && !currCamo.equals(originalCamo)) {
            // set and save the config
            client.getConfig().setParam("UNIT_CAMO", currCamo);
            client.getConfig().saveConfig();
            client.setConfig();

            // then reload images and update the GUI
            client.getConfig().loadImage(GUIClientConfig.CAMO_PATH + currCamo, "CAMO", 84, 72);
            client.getMainFrame().getMainPanel().selectFirstTab();
            client.getMainFrame().getMainPanel().getHQPanel().reinitialize();
        }

        // dispose of the dialog
        dispose();

    }// end actionPerformed

    /**
     * Update the "new camo" icon whenever a list item is selected.
     *
     * @param event - ItemEvent from the list.
     */
    public void valueChanged(ListSelectionEvent event) {

        // only care about the final selection, not sliders.
        if (!event.getValueIsAdjusting()) {

            // If "NO CAMO" is selected, clear the image.
            if (camoList.getSelectedIndex() == 0) {
                newCamo.setPreviewIcon(null);
                newCamo.setUnit(newEntity);
            } else {
                // if the image hasnt been loaded and scaled
                // yet, do the load and cache it for future use.
                String currSelection = camoList.getSelectedValue();
                ImageIcon currCamoIcon;

                if (camos.get(currSelection).equals("filler")) {
                    Image currCamo = Toolkit.getDefaultToolkit().getImage(String.format("./data/images/camo/%s", currSelection));
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
