/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
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

package mekwars.common.gui.panels;

import megamek.client.generator.RandomGenderGenerator;
import megamek.common.units.CrewType;
import megamek.common.units.Infantry;
import mekwars.common.House;
import mekwars.common.Unit;
import mekwars.common.UnitFactory;
import mekwars.common.campaign.CCampaign;
import mekwars.common.campaign.CPlayer;
import mekwars.common.gui.MMNetHyperLinkListener;
import mekwars.common.gui.MWUnitDisplay;
import mekwars.common.gui.MyHTMLEditorKit;
import mekwars.common.util.MWLogger;
import mekwars.common.util.SpringLayoutHelper;
import mekwars.common.util.UnitUtils;

/**
 * SHouse Status Panel
 */

public class CHSPanel extends javax.swing.JPanel {

    /**
     *
     */
    private static final long serialVersionUID = -6985292870326367798L;
    MWClient mwclient;
    CPlayer thePlayer;
    CCampaign theCampaign;
    javax.swing.JEditorPane mainPane = new javax.swing.JEditorPane();
    javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane();
    MyHTMLEditorKit kit = new MyHTMLEditorKit();
    java.awt.GridBagConstraints gridBagConstraints;

    private javax.swing.JPanel pnlBtns = new javax.swing.JPanel();
    private javax.swing.JPanel hsButtonSpringPanel = new javax.swing.JPanel();

    private javax.swing.JButton buyNewButton = new javax.swing.JButton();
    private javax.swing.JButton buyUsedButton = new javax.swing.JButton();
    private javax.swing.JLabel lblInfo = new javax.swing.JLabel();
    private CHSPanel.BuyPopupListener myPopup = null;

    // Needed to internally store SHouse Status
    private String HouseName;
    // hastable of Hashtables
    private java.util.TreeMap<String, String> componentsInfo;
    private java.util.TreeMap<String, java.util.TreeMap<String, String>> factoriesInfo;
    private java.util.TreeMap<String, java.util.Vector<HSMek>> unitsInfo;

    public CHSPanel(client.MWClient client) {

        setLayout(new java.awt.GridBagLayout());
        mwclient = client;
        theCampaign = mwclient.getCampaign();
        thePlayer = theCampaign.getPlayer();
        myPopup = new CHSPanel.BuyPopupListener();

        mainPane.setEditorKit(kit);
        mainPane.setEditable(false);
        mainPane.addHyperlinkListener(new MMNetHyperLinkListener(mwclient, this));
        scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setViewportBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        scrollPane.setViewportView(mainPane);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        this.add(scrollPane, gridBagConstraints);

        // set up the button row
        pnlBtns.setLayout(new javax.swing.BoxLayout(pnlBtns, javax.swing.BoxLayout.Y_AXIS));
        hsButtonSpringPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());

        // button to buy new units
        buyNewButton.setText("Buy New");
        buyNewButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buyNewButtonActionPerformed(evt);
            }
        });
        buyNewButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                buyNewUnitMouseEvent(evt);
            }
        });
        hsButtonSpringPanel.add(buyNewButton);

        // button to buy used units
        buyUsedButton.setText("Buy Used");
        buyUsedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buyUsedButtonActionPerformed(evt);
            }
        });
        buyUsedButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                buyUsedUnitMouseEvent(evt);
            }
        });
        hsButtonSpringPanel.add(buyUsedButton);

        SpringLayoutHelper.setupSpringGrid(hsButtonSpringPanel, 1, 3);

        // set up lblInfo, which is used when rolling over factories
        lblInfo.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        lblInfo.setSize(0, 0);

        pnlBtns.add(lblInfo);
        pnlBtns.add(hsButtonSpringPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        // gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        this.add(pnlBtns, gridBagConstraints);

        // make information holders
        componentsInfo = new java.util.TreeMap<String, String>();
        factoriesInfo = new java.util.TreeMap<String, java.util.TreeMap<String, String>>();
        unitsInfo = new java.util.TreeMap<String, java.util.Vector<HSMek>>();
    }

    // BUY MENU METHODS AND LISTENERS
    private void buyNewButtonActionPerformed(java.awt.event.ActionEvent e) {
    }// do nothing on action

    // make popup on press or release of New button
    private void buyNewUnitMouseEvent(java.awt.event.MouseEvent e) {
        javax.swing.JPopupMenu buy = createBuyNewPopupMenu();
        buy.show(e.getComponent(), e.getX(), e.getY());
    }

    private void buyUsedButtonActionPerformed(java.awt.event.ActionEvent e) {
    }// do nothing

    private void buyUsedUnitMouseEvent(java.awt.event.MouseEvent e) {
        javax.swing.JPopupMenu buy = createBuyUsedPopupMenu();
        buy.show(e.getComponent(), e.getX(), e.getY());
    }

    private javax.swing.JPopupMenu createBuyNewPopupMenu() {
        javax.swing.JMenu tmenu;
        javax.swing.JPopupMenu buy = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem menuItem = null;

        tmenu = new javax.swing.JMenu("Mek");
        buy.add(tmenu);
        menuItem = new javax.swing.JMenuItem("Light Mek");
        menuItem.setActionCommand("BUY|LIGHT|" + Unit.MEK);
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);
        menuItem = new javax.swing.JMenuItem("Medium Mek");
        menuItem.setActionCommand("BUY|MEDIUM|" + Unit.MEK);
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);
        menuItem = new javax.swing.JMenuItem("Heavy Mek");
        menuItem.setActionCommand("BUY|HEAVY|" + Unit.MEK);
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);
        menuItem = new javax.swing.JMenuItem("Assault Mek");
        menuItem.setActionCommand("BUY|ASSAULT|" + Unit.MEK);
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UseVehicle"))) {
            tmenu = new javax.swing.JMenu("Vehicle");
            buy.add(tmenu);
            menuItem = new javax.swing.JMenuItem("Light Vehicle");
            menuItem.setActionCommand("BUY|LIGHT|" + Unit.VEHICLE);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Medium Vehicle");
            menuItem.setActionCommand("BUY|MEDIUM|" + Unit.VEHICLE);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Heavy Vehicle");
            menuItem.setActionCommand("BUY|HEAVY|" + Unit.VEHICLE);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Assault Vehicle");
            menuItem.setActionCommand("BUY|ASSAULT|" + Unit.VEHICLE);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UseInfantry"))) {
            tmenu = new javax.swing.JMenu("Infantry");
            buy.add(tmenu);
            menuItem = new javax.swing.JMenuItem("Light Infantry");
            menuItem.setActionCommand("BUY|LIGHT|" + Unit.INFANTRY);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Medium Infantry");
            menuItem.setActionCommand("BUY|MEDIUM|" + Unit.INFANTRY);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Heavy Infantry");
            menuItem.setActionCommand("BUY|HEAVY|" + Unit.INFANTRY);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Assault Infantry");
            menuItem.setActionCommand("BUY|ASSAULT|" + Unit.INFANTRY);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UseProtoMek"))) {
            tmenu = new javax.swing.JMenu("ProtoMek");
            buy.add(tmenu);
            menuItem = new javax.swing.JMenuItem("Light ProtoMek");
            menuItem.setActionCommand("BUY|LIGHT|" + Unit.PROTOMEK);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Medium ProtoMek");
            menuItem.setActionCommand("BUY|MEDIUM|" + Unit.PROTOMEK);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Heavy ProtoMek");
            menuItem.setActionCommand("BUY|HEAVY|" + Unit.PROTOMEK);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Assault ProtoMek");
            menuItem.setActionCommand("BUY|ASSAULT|" + Unit.PROTOMEK);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UseBattleArmor"))) {
            tmenu = new javax.swing.JMenu("Battle Armor");
            buy.add(tmenu);
            menuItem = new javax.swing.JMenuItem("Light Battle Armor");
            menuItem.setActionCommand("BUY|LIGHT|" + Unit.BATTLEARMOR);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Medium Battle Armor");
            menuItem.setActionCommand("BUY|MEDIUM|" + Unit.BATTLEARMOR);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Heavy Battle Armor");
            menuItem.setActionCommand("BUY|HEAVY|" + Unit.BATTLEARMOR);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Assault Battle Armor");
            menuItem.setActionCommand("BUY|ASSAULT|" + Unit.BATTLEARMOR);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UseAero"))) {
            tmenu = new javax.swing.JMenu("Aero");
            buy.add(tmenu);
            menuItem = new javax.swing.JMenuItem("Light Aero");
            menuItem.setActionCommand("BUY|LIGHT|" + Unit.AERO);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Medium Aero");
            menuItem.setActionCommand("BUY|MEDIUM|" + Unit.AERO);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Heavy Aero");
            menuItem.setActionCommand("BUY|HEAVY|" + Unit.AERO);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Assault Aero");
            menuItem.setActionCommand("BUY|ASSAULT|" + Unit.AERO);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(mwclient.getserverConfigs("AllowPersonalPilotQueues"))) {
            tmenu = new javax.swing.JMenu("Pilots");
            buy.add(tmenu);
            javax.swing.JMenu smenu = new javax.swing.JMenu("Mek");
            menuItem = new javax.swing.JMenuItem("Light Pilot");
            menuItem.setActionCommand("BUYP|" + Unit.MEK + "|" + Unit.LIGHT);
            menuItem.addActionListener(myPopup);
            smenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Medium Pilot");
            menuItem.setActionCommand("BUYP|" + Unit.MEK + "|" + Unit.MEDIUM);
            menuItem.addActionListener(myPopup);
            smenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Heavy Pilot");
            menuItem.setActionCommand("BUYP|" + Unit.MEK + "|" + Unit.HEAVY);
            menuItem.addActionListener(myPopup);
            smenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Assault Pilot");
            menuItem.setActionCommand("BUYP|" + Unit.MEK + "|" + Unit.ASSAULT);
            menuItem.addActionListener(myPopup);
            smenu.add(menuItem);
            tmenu.add(smenu);

            if (Boolean.parseBoolean(mwclient.getserverConfigs("UseProtoMek"))) {
                smenu = new javax.swing.JMenu("Proto");
                menuItem = new javax.swing.JMenuItem("Light Pilot");
                menuItem.setActionCommand("BUYP|" + Unit.PROTOMEK + "|" + Unit.LIGHT);
                menuItem.addActionListener(myPopup);
                smenu.add(menuItem);
                menuItem = new javax.swing.JMenuItem("Medium Pilot");
                menuItem.setActionCommand("BUYP|" + Unit.PROTOMEK + "|" + Unit.MEDIUM);
                menuItem.addActionListener(myPopup);
                smenu.add(menuItem);
                menuItem = new javax.swing.JMenuItem("Heavy Pilot");
                menuItem.setActionCommand("BUYP|" + Unit.PROTOMEK + "|" + Unit.HEAVY);
                menuItem.addActionListener(myPopup);
                smenu.add(menuItem);
                menuItem = new javax.swing.JMenuItem("Assault Pilot");
                menuItem.setActionCommand("BUYP|" + Unit.PROTOMEK + "|" + Unit.ASSAULT);
                menuItem.addActionListener(myPopup);
                smenu.add(menuItem);
                tmenu.add(smenu);
            }
        }

        return buy;
    }

    private javax.swing.JPopupMenu createBuyUsedPopupMenu() {
        javax.swing.JMenu tmenu;
        javax.swing.JPopupMenu buy = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem menuItem = null;

        tmenu = new javax.swing.JMenu("Mek");
        buy.add(tmenu);
        menuItem = new javax.swing.JMenuItem("Light Mek");
        menuItem.setActionCommand("BUYU|LIGHT|" + Unit.MEK);
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);
        menuItem = new javax.swing.JMenuItem("Medium Mek");
        menuItem.setActionCommand("BUYU|MEDIUM|" + Unit.MEK);
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);
        menuItem = new javax.swing.JMenuItem("Heavy Mek");
        menuItem.setActionCommand("BUYU|HEAVY|" + Unit.MEK);
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);
        menuItem = new javax.swing.JMenuItem("Assault Mek");
        menuItem.setActionCommand("BUYU|ASSAULT|" + Unit.MEK);
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UseVehicle"))) {
            tmenu = new javax.swing.JMenu("Vehicle");
            buy.add(tmenu);
            menuItem = new javax.swing.JMenuItem("Light Vehicle");
            menuItem.setActionCommand("BUYU|LIGHT|" + Unit.VEHICLE);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Medium Vehicle");
            menuItem.setActionCommand("BUYU|MEDIUM|" + Unit.VEHICLE);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Heavy Vehicle");
            menuItem.setActionCommand("BUYU|HEAVY|" + Unit.VEHICLE);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Assault Vehicle");
            menuItem.setActionCommand("BUYU|ASSAULT|" + Unit.VEHICLE);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }
        if (Boolean.parseBoolean(mwclient.getserverConfigs("UseInfantry"))) {
            tmenu = new javax.swing.JMenu("Infantry");
            buy.add(tmenu);
            menuItem = new javax.swing.JMenuItem("Light Infantry");
            menuItem.setActionCommand("BUYU|LIGHT|" + Unit.INFANTRY);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Medium Infantry");
            menuItem.setActionCommand("BUYU|MEDIUM|" + Unit.INFANTRY);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Heavy Infantry");
            menuItem.setActionCommand("BUYU|HEAVY|" + Unit.INFANTRY);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Assault Infantry");
            menuItem.setActionCommand("BUYU|ASSAULT|" + Unit.INFANTRY);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UseProtoMek"))) {
            tmenu = new javax.swing.JMenu("ProtoMek");
            buy.add(tmenu);
            menuItem = new javax.swing.JMenuItem("Light ProtoMek");
            menuItem.setActionCommand("BUYU|LIGHT|" + Unit.PROTOMEK);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Medium Infantry");
            menuItem.setActionCommand("BUYU|MEDIUM|" + Unit.PROTOMEK);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Heavy Infantry");
            menuItem.setActionCommand("BUYU|HEAVY|" + Unit.PROTOMEK);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Assault Infantry");
            menuItem.setActionCommand("BUYU|ASSAULT|" + Unit.PROTOMEK);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UseBattleArmor"))) {
            tmenu = new javax.swing.JMenu("Battle Armor");
            buy.add(tmenu);
            menuItem = new javax.swing.JMenuItem("Light Battle Armor");
            menuItem.setActionCommand("BUYU|LIGHT|" + Unit.BATTLEARMOR);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Medium Battle Armor");
            menuItem.setActionCommand("BUYU|MEDIUM|" + Unit.BATTLEARMOR);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Heavy Battle Armor");
            menuItem.setActionCommand("BUYU|HEAVY|" + Unit.BATTLEARMOR);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Assault Battle Armor");
            menuItem.setActionCommand("BUYU|ASSAULT|" + Unit.BATTLEARMOR);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UseAero"))) {
            tmenu = new javax.swing.JMenu("Aero");
            buy.add(tmenu);
            menuItem = new javax.swing.JMenuItem("Light Aero");
            menuItem.setActionCommand("BUYU|LIGHT|" + Unit.AERO);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Medium Aero");
            menuItem.setActionCommand("BUYU|MEDIUM|" + Unit.AERO);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Heavy Aero");
            menuItem.setActionCommand("BUYU|HEAVY|" + Unit.AERO);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
            menuItem = new javax.swing.JMenuItem("Assault Aero");
            menuItem.setActionCommand("BUYU|ASSAULT|" + Unit.AERO);
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        return buy;
    }

    /**
     * Set faction name. Called in response to FactionStatusScreenUpdateCommand|FN| command.
     */
    public void setFactionName(String name) {
        HouseName = name;
    }

    /**
     * Clear all faction data.
     */
    public void clearHouseStatusData() {
        componentsInfo.clear();
        factoriesInfo.clear();
        unitsInfo.clear();
    }

    /**
     * Add a unit to the units' hash. Called from FactionStatusScreenUpdateCommand.java when client receives
     * FactionStatusScreenUpdateCommand|AU|data command.
     */
    public void addFactionUnit(String unitData) {

        java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(unitData, "$");

        String weight = tokenizer.nextToken();
        String type = tokenizer.nextToken();

        HSMek currHSUnit = new HSMek(mwclient, tokenizer);// reads rest of
        // tokens

        // if there isn't a vector for this type + weight combo already, create
        // one
        java.util.Vector<mekwars.client.gui.HSMek> weightAndTypeVec = unitsInfo.get(weight + "$" + type);
        if (weightAndTypeVec == null) {
            weightAndTypeVec = new java.util.Vector<mekwars.client.gui.HSMek>(1, 1);
            unitsInfo.put(weight + "$" + type, weightAndTypeVec);
        }

        // add the unit to the vector
        weightAndTypeVec.add(currHSUnit);
    }

    /**
     * Remove a unit from the units' hash+vector sets. Called from FactionStatusScreenUpdateCommand.java when client
     * receives FactionStatusScreenUpdateCommand|RU| command.
     */
    public void removeFactionUnit(String unitData) {

        java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(unitData, "$");

        String weight = tokenizer.nextToken();
        String type = tokenizer.nextToken();
        int unitID = Integer.valueOf(tokenizer.nextToken());

        java.util.Vector<mekwars.client.gui.HSMek> weightAndTypeVec = unitsInfo.get(weight + "$" + type);

        // if weight and type are null, there is no way to remove the unit.
        if (weightAndTypeVec == null) {
            return;
        }

        // iterate through all units of this wieght&type. remove matching id.
        java.util.Iterator<mekwars.client.gui.HSMek> i = weightAndTypeVec.iterator();
        while (i.hasNext()) {
            HSMek currHSMek = i.next();
            if (currHSMek.getUnitID() == unitID) {
                i.remove();
                return;
            }
        }
    }

    /**
     * Change the component display for a given weight & type combo. Called from FactionStatusScreenUpdateCommand.java
     * when client receives FactionStatusScreenUpdateCommand|CC| command. Because components are so simple, change is
     * always used and there are no adds/removes.
     */
    public void changeFactionComponents(String componentData) {

        java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(componentData, "$");

        String weight = tokenizer.nextToken();
        String type = tokenizer.nextToken();

        String currentPP = tokenizer.nextToken();
        String prodUnits = tokenizer.nextToken();

        // remove old value, if any, and insert new value
        componentsInfo.remove(weight + "$" + type);
        componentsInfo.put(weight + "$" + type, currentPP + "$" + prodUnits);
    }

    /**
     * Add a factory. Called from FactionStatusScreenUpdateCommand.java when FactionStatusScreenUpdateCommand|AF|
     * command received.
     */
    public void addFactionFactory(String factoryData) {

        java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(factoryData, "$");

        // read factory data
        int weight = Integer.valueOf(tokenizer.nextToken());
        int type = Integer.valueOf(tokenizer.nextToken());

        String founder = tokenizer.nextToken();
        String planet = tokenizer.nextToken();
        String factoryName = tokenizer.nextToken();

        int timeToRefresh = Integer.valueOf(tokenizer.nextToken());
        int accessLevel = Integer.parseInt(tokenizer.nextToken());

        String factoryID = tokenizer.nextToken();

        /*
         * Check for multiproduction and add to all appropriate factory
         * categories. Overly complex, and makes me want to punch the person who
         * RFE'ed multifacs in the face
         *
         * :-(
         */
        if (canProduce(Unit.MEK, type)) {
            addFactoryHelper(weight, Unit.MEK, timeToRefresh, founder, planet, factoryName, accessLevel, factoryID);
        }
        if (canProduce(Unit.VEHICLE, type)) {
            addFactoryHelper(weight, Unit.VEHICLE, timeToRefresh, founder, planet, factoryName, accessLevel, factoryID);
        }
        if (canProduce(Unit.INFANTRY, type)) {
            addFactoryHelper(weight,
                  Unit.INFANTRY,
                  timeToRefresh,
                  founder,
                  planet,
                  factoryName,
                  accessLevel,
                  factoryID);
        }
        if (canProduce(Unit.PROTOMEK, type)) {
            addFactoryHelper(weight,
                  Unit.PROTOMEK,
                  timeToRefresh,
                  founder,
                  planet,
                  factoryName,
                  accessLevel,
                  factoryID);
        }
        if (canProduce(Unit.BATTLEARMOR, type)) {
            addFactoryHelper(weight,
                  Unit.BATTLEARMOR,
                  timeToRefresh,
                  founder,
                  planet,
                  factoryName,
                  accessLevel,
                  factoryID);
        }
        if (canProduce(Unit.AERO, type)) {
            addFactoryHelper(weight, Unit.AERO, timeToRefresh, founder, planet, factoryName, accessLevel, factoryID);
        }
    }

    /**
     * Helper used to determine which unit types a multi-fac can produce.
     */
    private boolean canProduce(int type_id, int productionCapabilities) {

        // Exception 0 = everything;
        if (productionCapabilities == UnitFactory.BUILDALL) {
            return true;
        }

        int test = productionCapabilities;
        if ((test - UnitFactory.BUILDAERO) >= 0) {
            test -= UnitFactory.BUILDAERO;
            if (type_id == Unit.AERO) {
                return true;
            }
        }

        if ((test - UnitFactory.BUILDBATTLEARMOR) >= 0) {
            test -= UnitFactory.BUILDBATTLEARMOR;
            if (type_id == Unit.BATTLEARMOR) {
                return true;
            }
        }

        if ((test - UnitFactory.BUILDPROTOMECHS) >= 0) {
            test -= UnitFactory.BUILDPROTOMECHS;
            if (type_id == Unit.PROTOMEK) {
                return true;
            }
        }

        if ((test - UnitFactory.BUILDINFANTRY) >= 0) {
            test -= UnitFactory.BUILDINFANTRY;
            if (type_id == Unit.INFANTRY) {
                return true;
            }
        }

        if ((test - UnitFactory.BUILDVEHICLES) >= 0) {
            test -= UnitFactory.BUILDVEHICLES;
            if (type_id == Unit.VEHICLE) {
                return true;
            }
        }

        if ((test - UnitFactory.BUILDMEK) >= 0) {
            if (type_id == Unit.MEK) {
                return true;
            }
        }

        return false;
    }

    /**
     * Private method called only from addFactionFactory. Abstracts out some repetetive code that checks for factory
     * vectors and creates missing listings.
     */
    private void addFactoryHelper(int weight, int type, int timeToRefresh, String founder, String planet,
          String factoryName, int accessLevel, String factoryID) {

        // if there isn't a vector for this type + weight combo already, create
        // one
        java.util.TreeMap<String, String> weightAndTypeMap = factoriesInfo.get(weight + "$" + type);
        if (weightAndTypeMap == null) {
            weightAndTypeMap = new java.util.TreeMap<String, String>();
            factoriesInfo.put(weight + "$" + type, weightAndTypeMap);
        }

        /*
         * Add the factory to the map. Note that we use a map so the factories
         * appear in alpha order, by world.
         */
        weightAndTypeMap.put(planet + "$" + factoryName,
              founder + "$" + planet + "$" + factoryName + "$" + timeToRefresh + "$" + accessLevel + "$" + factoryID);
    }

    /**
     * Remove a factory from house status. Used when client receives a FactionStatusScreenUpdateCommand|RF| command.
     * Usually after a world changes hands.
     * <p>
     * Format: FactionStatusScreenUpdateCommand|RF|weight$metatype$planet$name|
     */
    public void removeFactionFactory(String factoryData) {

        java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(factoryData, "$");

        int weight = Integer.valueOf(tokenizer.nextToken());
        int type = Integer.valueOf(tokenizer.nextToken());

        String planet = tokenizer.nextToken();
        String factoryName = tokenizer.nextToken();

        /*
         * Check for multiproduction and remove from all appropriate factory
         * categories. Overly complex, and makes me want to punch the person who
         * RFE'ed multifacs in the face :-(
         */
        if (canProduce(Unit.MEK, type)) {
            removeFactoryHelper(weight, Unit.MEK, planet, factoryName);
        }
        if (canProduce(Unit.VEHICLE, type)) {
            removeFactoryHelper(weight, Unit.VEHICLE, planet, factoryName);
        }
        if (canProduce(Unit.INFANTRY, type)) {
            removeFactoryHelper(weight, Unit.INFANTRY, planet, factoryName);
        }
        if (canProduce(Unit.PROTOMEK, type)) {
            removeFactoryHelper(weight, Unit.PROTOMEK, planet, factoryName);
        }
        if (canProduce(Unit.BATTLEARMOR, type)) {
            removeFactoryHelper(weight, Unit.BATTLEARMOR, planet, factoryName);
        }
        if (canProduce(Unit.AERO, type)) {
            removeFactoryHelper(weight, Unit.AERO, planet, factoryName);
        }
    }

    /**
     * Helper that abstracts out some repetetive checks from removeFactionFactory.
     */
    private void removeFactoryHelper(int weight, int type, String planet, String factoryName) {

        java.util.TreeMap<String, String> weightAndTypeMap = factoriesInfo.get(weight + "$" + type);

        // if weight and type map is null, there is no way to remove the
        // factory.
        if (weightAndTypeMap == null) {
            return;
        }

        // iterate through all facs of this wieght&type. remove matching names.
        java.util.Iterator<String> i = weightAndTypeMap.keySet().iterator();
        while (i.hasNext()) {
            String currName = i.next();
            if (currName.equals(planet + "$" + factoryName)) {
                i.remove();
            }
        }
    }

    /**
     * Change a factory's information. Used to update refresh times. Format:
     * FactionStatusScreenUpdateCommand|CF|weight$metatype$name$planet$timetorefresh|
     */
    public void changeFactionFactory(String factoryData) {

        java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(factoryData, "$");

        int weight = Integer.valueOf(tokenizer.nextToken());
        int type = Integer.valueOf(tokenizer.nextToken());

        String planet = tokenizer.nextToken();
        String factoryName = tokenizer.nextToken();

        int timeToRefresh = Integer.valueOf(tokenizer.nextToken());

        int accessLevel = Integer.parseInt(tokenizer.nextToken());

        String factoryID = tokenizer.nextToken();

        /*
         * Check for multiproduction and update in all appropriate factory
         * categories. Overly complex, and makes me want to punch the person who
         * RFE'ed multifacs in the face :-(
         */
        if (canProduce(Unit.MEK, type)) {
            changeFactoryHelper(weight, Unit.MEK, planet, factoryName, timeToRefresh, accessLevel, factoryID);
        }
        if (canProduce(Unit.VEHICLE, type)) {
            changeFactoryHelper(weight, Unit.VEHICLE, planet, factoryName, timeToRefresh, accessLevel, factoryID);
        }
        if (canProduce(Unit.INFANTRY, type)) {
            changeFactoryHelper(weight, Unit.INFANTRY, planet, factoryName, timeToRefresh, accessLevel, factoryID);
        }
        if (canProduce(Unit.PROTOMEK, type)) {
            changeFactoryHelper(weight, Unit.PROTOMEK, planet, factoryName, timeToRefresh, accessLevel, factoryID);
        }
        if (canProduce(Unit.BATTLEARMOR, type)) {
            changeFactoryHelper(weight, Unit.BATTLEARMOR, planet, factoryName, timeToRefresh, accessLevel, factoryID);
        }
        if (canProduce(Unit.AERO, type)) {
            changeFactoryHelper(weight, Unit.AERO, planet, factoryName, timeToRefresh, accessLevel, factoryID);
        }
    }

    /**
     * Helper that abstracts out some repetetive checks from checkFactionFactory.
     */
    private void changeFactoryHelper(int weight, int type, String planet, String factoryName, int timeToRefresh,
          int accessLevel, String factoryID) {

        java.util.TreeMap<String, String> weightAndTypeMap = factoriesInfo.get(weight + "$" + type);

        // if weight and type map is null, there is no way to change the
        // factory.
        if (weightAndTypeMap == null) {
            MWLogger.errLog("Error updating factory: null treemap at weight & type.");
            return;
        }

        // no factory with matching name on planet. return.
        String oldFactoryInfo = weightAndTypeMap.get(planet + "$" + factoryName);
        if (oldFactoryInfo == null) {
            MWLogger.errLog("Error updating factory: null oldFactory.");
            return;
        }

        // get the founder, which wasn't transferred.
        java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(oldFactoryInfo, "$");
        String founder = tokenizer.nextToken();

        // overwrite the old entry
        weightAndTypeMap.put(planet + "$" + factoryName,
              founder + "$" + planet + "$" + factoryName + "$" + timeToRefresh + "$" + accessLevel + "$" + factoryID);
    }

    public void updateDisplay() {

        // Returns the Private Status for Members only
        StringBuilder result = new StringBuilder("<BODY  TEXT=\"" +
                                                       mwclient.getConfigParam("CHATFONTCOLOR") +
                                                       "\" BGCOLOR=\"" +
                                                       mwclient.getConfigParam("BACKGROUNDCOLOR") +
                                                       "\">");
        boolean usingAdvanceRepairs = mwclient.isUsingAdvanceRepairs();
        int playerAccessLevel = mwclient.getPlayer().getSubFactionAccess();
        result.append("<TABLE Border=\"1\"><TR><TH>" +
                            HouseName +
                            "</TH><TH>" +
                            mwclient.getserverConfigs("LightFactoryTypeTitle") +
                            "</TH><TH>" +
                            mwclient.getserverConfigs("MediumFactoryTypeTitle") +
                            "</TH><TH>" +
                            mwclient.getserverConfigs("HeavyFactoryTypeTitle") +
                            "</TH><TH>" +
                            mwclient.getserverConfigs("AssaultFactoryTypeTitle") +
                            "</TH></TR>");
        int factoryGifCounter;
        for (int type_id = 0; type_id < Unit.TOTALTYPES; type_id++) {

            // hide unit types that aren't in use on the server
            String useIt = "Use" + Unit.getTypeClassDesc(type_id);

            if (!Boolean.parseBoolean(mwclient.getserverConfigs(useIt))) {
                continue;
            }
            if (!hasFactories(type_id)) {
                continue;
            }

            String factoryTitle = mwclient.getserverConfigs(Unit.getTypeClassDesc(type_id) + "FactoryClassTitle");
            result.append("<TR><TD VALIGN=MIDDLE><b>" + factoryTitle + "</b></TD>");

            for (int weight = 0; weight < 4; weight++) {

                String buyNew = "CanBuyNew" + Unit.getWeightClassDesc(weight) + Unit.getTypeClassDesc(type_id);

                String Comps = componentsInfo.get(weight + "$" + type_id);
                java.util.StringTokenizer ST = new java.util.StringTokenizer(Comps, "$");
                int comps = Integer.parseInt(ST.nextToken());
                if ((comps > 0) || (factoriesInfo.get(weight + "$" + type_id) != null)) {

                    result.append("<TD>" + "<img src=\"data/images/miniticks.gif\">:" + comps);
                    result.append("<img src=\"data/images/units.gif\">:" + ST.nextToken() + "<br>");

                    // Needed because of the binary coding.
                    int typetocheck = type_id;

                    java.util.TreeMap<String, String> facs = factoriesInfo.get(weight + "$" + typetocheck);
                    if ((facs != null) && Boolean.parseBoolean(thePlayer.getSubFaction().getConfig(buyNew))) {

                        boolean hasOpen = false;
                        int minrefresh = Integer.MAX_VALUE;

                        factoryGifCounter = 0;
                        for (String Fac : facs.values()) {

                            ST = new java.util.StringTokenizer(Fac, "$");
                            String founder = ST.nextToken();
                            String planet = ST.nextToken();
                            String factoryName = ST.nextToken();
                            int refreshTime = Integer.parseInt(ST.nextToken());
                            int accessLevel = Integer.parseInt(ST.nextToken());
                            String factoryID = ST.nextToken();

                            String openImage = "data/images/open" + founder + ".gif";
                            String closeImage = "data/images/closed" + founder + ".gif";

                            if (!new java.io.File(openImage).exists()) {
                                openImage = "data/images/open.gif";
                            }
                            if (!new java.io.File(closeImage).exists()) {
                                closeImage = "data/images/closed.gif";
                            }

                            if (accessLevel > playerAccessLevel) {
                                hasOpen = true;
                                continue;
                            }
                            factoryGifCounter++;
                            if (factoryGifCounter == 11) {
                                result.append("<br>");
                                factoryGifCounter = 1;
                            }
                            if (refreshTime == 0) {

                                House foundH = mwclient.getData().getHouseByName(founder);
                                int cbillCost = client.campaign.CUnit.getPriceForUnit(mwclient,
                                      weight,
                                      type_id,
                                      foundH) + mwclient.getPlayer().getHangarPurchasePenalty(type_id, weight);
                                int fluCost = client.campaign.CUnit.getInfluenceForUnit(mwclient,
                                      weight,
                                      type_id,
                                      foundH);
                                int ppCost = client.campaign.CUnit.getPPForUnit(mwclient, weight, type_id, foundH);

                                if (!mwclient.getPlayer().getMyHouse().getName().equalsIgnoreCase(foundH.getName())) {
                                    cbillCost = Math.round(cbillCost *
                                                                 Float.parseFloat(mwclient.getserverConfigs(
                                                                       "NonOriginalCBillMultiplier"))) +
                                                      mwclient.getPlayer().getHangarPurchasePenalty(type_id, weight);
                                    fluCost = Math.round(fluCost *
                                                               Float.parseFloat(mwclient.getserverConfigs(
                                                                     "NonOriginalInfluenceMultiplier")));
                                    ppCost = Math.round(ppCost *
                                                              Float.parseFloat(mwclient.getserverConfigs(
                                                                    "NonOriginalComponentMultiplier")));
                                }

                                String costString = "(Cost: " +
                                                          mwclient.moneyOrFluMessage(true, true, cbillCost, false) +
                                                          ", " +
                                                          mwclient.moneyOrFluMessage(false, true, fluCost, false) +
                                                          ", " +
                                                          ppCost +
                                                          " Components)";

                                result.append("<a href=\"MEKWARS/c request#" +
                                                    weight +
                                                    "#" +
                                                    type_id +
                                                    "#" +
                                                    planet +
                                                    "#" +
                                                    factoryName +
                                                    "\"><img border=\"0\" alt=\"Click to buy a " +
                                                    founder +
                                                    " " +
                                                    Unit.getTypeClassDesc(type_id) +
                                                    " from " +
                                                    factoryName +
                                                    " on " +
                                                    planet +
                                                    ". " +
                                                    costString +
                                                    "\" src=\"" +
                                                    openImage +
                                                    "\"></a>");
                                hasOpen = true;

                            } else {
                                result.append("<a href=\"MEKWARS/c request#" +
                                                    weight +
                                                    "#" +
                                                    type_id +
                                                    "#" +
                                                    planet +
                                                    "#" +
                                                    factoryName +
                                                    "\"<img border=\"0\" alt=\"" +
                                                    factoryName +
                                                    " on " +
                                                    planet +
                                                    " built by " +
                                                    founder +
                                                    " (Refresh Time: " +
                                                    refreshTime +
                                                    ")\" src=\"" +
                                                    closeImage +
                                                    "\"></a>");
                                if (refreshTime < minrefresh) {
                                    minrefresh = refreshTime;
                                }
                            }
                        }

                        if (!hasOpen) {
                            result.append("<img src=\"data/images/clock.gif\">:" + minrefresh);
                        }
                    } else {
                        result.append("<img src=\"data/images/absent.gif\">");
                    }
                } else {
                    result.append("<TD> </TD>");
                }

                result.append("</TD>");
            }
            result.append("</TR>");
        }
        result.append("</TABLE>");

        // Bays
        for (int type = 0; type < Unit.TOTALTYPES; type++) {

            // is not using units of the type, skip the listings
            String useIt = "Use" + Unit.getTypeClassDesc(type);
            if (!Boolean.parseBoolean(mwclient.getserverConfigs(useIt))) {
                continue;
            }

            // if the house has any units at all, add a Bays: title.
            boolean hasUnits = false;
            for (int weight = 0; weight < 4; weight++) {
                if (unitsInfo.get(weight + "$" + type) != null) {
                    hasUnits = true;
                }
            }
            if (hasUnits) {
                String factoryTitle = mwclient.getserverConfigs(Unit.getTypeClassDesc(type) + "FactoryClassTitle");
                result.append("<b>" + factoryTitle + " Bays</b><br>");
            }

            // fill out bays
            for (int weight = 0; weight < 4; weight++) {

                String buyUsed = "CanBuyUsed" + Unit.getWeightClassDesc(weight) + Unit.getTypeClassDesc(type);
                if (!Boolean.parseBoolean(thePlayer.getSubFaction().getConfig(buyUsed))) {
                    continue;
                }

                if ((unitsInfo.get(weight + "$" + type) != null) && (unitsInfo.get(weight + "$" + type).size() > 0)) {
                    House foundH = mwclient.getData().getHouseByName(mwclient.getPlayer().getMyHouse().getName());
                    int cbillCost = Math.round(client.campaign.CUnit.getPriceForUnit(mwclient, weight, type, foundH) *
                                                     foundH.getUsedMekBayMultiplier()) +
                                          mwclient.getPlayer().getHangarPurchasePenalty(type, weight);
                    int fluCost = Math.round(client.campaign.CUnit.getInfluenceForUnit(mwclient, weight, type, foundH) *
                                                   foundH.getUsedMekBayMultiplier());
                    result.append("<a href=\"MEKWARS/c requestdonated#" +
                                        weight +
                                        "#" +
                                        type +
                                        "\"><img border=\"0\" alt=\"Request one of the Units from this bay (Cost: " +
                                        mwclient.moneyOrFluMessage(true, true, cbillCost, false) +
                                        ", " +
                                        mwclient.moneyOrFluMessage(false, true, fluCost, false) +
                                        ")\" src=\"data/images/cart.gif\"></a> " +
                                        Unit.getWeightClassDesc(weight) +
                                        ": ");
                    java.util.Vector<mekwars.client.gui.HSMek> v = unitsInfo.get(weight + "$" + type);
                    HSMek[] entities = new HSMek[v.size()];
                    for (int i = 0; i < v.size(); i++) {
                        entities[i] = v.elementAt(i);
                    }

                    // alpha sort
                    java.util.Arrays.sort(entities, new java.util.Comparator<mekwars.client.gui.HSMek>() {
                        public int compare(HSMek obj1, HSMek obj2) {

                            HSMek a = obj1;
                            HSMek b = obj2;

                            // if the names are the same, check for damage
                            if (a.getName().compareTo(b.getName()) == 0) {

                                Integer gunneryA = a.getEntity().getCrew().getGunnery();
                                Integer gunneryB = b.getEntity().getCrew().getGunnery();

                                int compare = gunneryA.compareTo(gunneryB);

                                if (compare != 0) {
                                    return compare;
                                }

                                Integer pilotingA = a.getEntity().getCrew().getPiloting();
                                Integer pilotingB = b.getEntity().getCrew().getPiloting();

                                compare = pilotingA.compareTo(pilotingB);

                                if (compare != 0) {
                                    return compare;
                                }

                                String damA = a.getBattleDamage();
                                String damB = b.getBattleDamage();

                                return damA.compareTo(damB);
                            }
                            // else
                            return a.getName().compareTo(b.getName());
                        }
                    });

                    // group identical units together and add to result
                    for (int j = 0; j < entities.length; j++) {

                        StringBuilder unitString = new StringBuilder();

                        HSMek m = entities[j];
                        int num = 1;

                        while ((j < (entities.length - 1)) &&
                                     m.getName().equalsIgnoreCase(entities[j + 1].getName()) &&
                                     (m.getBattleDamage().equalsIgnoreCase(entities[j + 1].getBattleDamage())) &&
                                     (m.getEntity().getCrew().getPiloting() ==
                                            entities[j + 1].getEntity().getCrew().getPiloting()) &&
                                     (m.getEntity().getCrew().getGunnery() ==
                                            entities[j + 1].getEntity().getCrew().getGunnery())) {
                            j++;
                            num++;
                        }

                        // change color depending on level of damage, if using
                        // AS
                        if (usingAdvanceRepairs) {
                            Entity e = m.getEntity();
                            UnitUtils.applyBattleDamage(e, m.getBattleDamage(), true);
                            if (!UnitUtils.canStartUp(e)) {
                                unitString.append("<font color=\"BLUE\">");
                            } else if (UnitUtils.hasCriticalDamage(e)) {
                                unitString.append("<font color=\"red\">");
                            } else if (UnitUtils.hasArmorDamage(e)) {
                                unitString.append("<font color=\"yellow\">");
                            } else {
                                unitString.append("<font>");
                            }
                        }

                        if (m.getType().equalsIgnoreCase("mek") || m.getType().equalsIgnoreCase("vehicle")) {
                            unitString.append("<a href=\"MEKINFO" +
                                                    m.getMekFile() +
                                                    "#" +
                                                    m.getBV() +
                                                    "#" +
                                                    m.getEntity().getCrew().getGunnery() +
                                                    "#" +
                                                    m.getEntity().getCrew().getPiloting() +
                                                    "#" +
                                                    m.getBattleDamage() +
                                                    "\">" +
                                                    m.getName() +
                                                    " (" +
                                                    m.getEntity().getCrew().getGunnery() +
                                                    "/" +
                                                    m.getEntity().getCrew().getPiloting() +
                                                    ")");
                        } else {
                            if ((m.getEntity() instanceof Infantry) &&
                                      ((Infantry) m.getEntity()).canMakeAntiMekAttacks()) {
                                unitString.append("<a href=\"MEKINFO" +
                                                        m.getMekFile() +
                                                        "#" +
                                                        m.getBV() +
                                                        "#" +
                                                        m.getEntity().getCrew().getGunnery() +
                                                        "#" +
                                                        m.getEntity().getCrew().getPiloting() +
                                                        "#" +
                                                        m.getBattleDamage() +
                                                        "\">" +
                                                        m.getName() +
                                                        " (" +
                                                        m.getEntity().getCrew().getGunnery() +
                                                        "/" +
                                                        m.getEntity().getCrew().getPiloting() +
                                                        ")");
                            } else {
                                unitString.append("<a href=\"MEKINFO" +
                                                        m.getMekFile() +
                                                        "#" +
                                                        m.getBV() +
                                                        "#" +
                                                        m.getEntity().getCrew().getGunnery() +
                                                        "#" +
                                                        m.getEntity().getCrew().getPiloting() +
                                                        "#" +
                                                        m.getBattleDamage() +
                                                        "\">" +
                                                        m.getName() +
                                                        " (" +
                                                        m.getEntity().getCrew().getGunnery() +
                                                        ")");
                            }
                        }

                        // frontload the dupe indicator, to reduce confusion
                        // with mono-skill units
                        if (num > 1) {
                            unitString.insert(0, num + " x ");
                        }

                        unitString.append("</a>");
                        if (usingAdvanceRepairs) {
                            unitString.append("</font>");
                        }
                        if (j < (entities.length - 1)) {
                            unitString.append(", ");
                        } else {
                            unitString.append("<br>");
                        }

                        // add this unit's string to the overall result
                        result.append(unitString.toString());
                    }
                }
            }
        }

        result.append("</BODY>");
        mainPane.setText("");
        mainPane.setText(result.toString());
        mainPane.repaint();
    }

    private boolean hasFactories(int type) {

        for (int weight = 0; weight <= Unit.ASSAULT; weight++) {
            if (factoriesInfo.get(weight + "$" + type) != null) {
                return true;
            }
        }
        return false;
    }

    public void setInfoText(String s) {
        lblInfo.setText(s);
        if (s == null) {
            hsButtonSpringPanel.setVisible(true);
            lblInfo.setVisible(false);
        } else if (s.equals("")) {
            hsButtonSpringPanel.setVisible(true);
            lblInfo.setVisible(false);
        } else {

            // only set the info label's dimension once. has to be done here to
            // ensure
            // that the label and panel dimensions match exactly.
            if ((lblInfo.getWidth() == 0) && (lblInfo.getHeight() == 0)) {
                java.awt.Dimension newDim = new java.awt.Dimension();
                newDim.setSize(lblInfo.getPreferredSize().getWidth(), hsButtonSpringPanel.getSize().getHeight());
                lblInfo.setMinimumSize(newDim);
            }

            hsButtonSpringPanel.setVisible(false);
            lblInfo.setVisible(true);
        }
    }

    public void showInfoWindow(String mekFile, int bv, int gunnery, int piloting, String battleDamage) {
        Entity unitEntity = null;
        client.campaign.CUnit embeddedUnit = new client.campaign.CUnit();
        embeddedUnit.setUnitFilename(mekFile);
        embeddedUnit.createEntity();
        unitEntity = embeddedUnit.getEntity();

        javax.swing.JFrame InfoWindow = new javax.swing.JFrame();
        UnitDisplay unitDetailInfo = new MWUnitDisplay(null, mwclient);
        unitEntity.loadAllWeapons();
        unitEntity.setCrew(new megamek.common.Crew(CrewType.SINGLE,
              "",
              1,
              gunnery,
              gunnery,
              gunnery,
              piloting,
              RandomGenderGenerator.generate(),
              null));
        if (battleDamage.trim().length() > 1) {
            UnitUtils.applyBattleDamage(unitEntity, battleDamage, false);
        }
        InfoWindow.getContentPane().add(unitDetailInfo);
        InfoWindow.setSize(300, 400);
        InfoWindow.setResizable(false);
        InfoWindow.setTitle(unitEntity.getModel());
        InfoWindow.setLocationRelativeTo(mwclient.getMainFrame());
        InfoWindow.setVisible(true);
        unitDetailInfo.displayEntity(unitEntity);
    }

    class BuyPopupListener extends java.awt.event.MouseAdapter implements java.awt.event.ActionListener {

        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            String s = actionEvent.getActionCommand();
            java.util.StringTokenizer st = new java.util.StringTokenizer(s, "|");
            String command = st.nextToken();

            if (command.equalsIgnoreCase("BUY")) {
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX +
                                        "c request#" +
                                        st.nextToken() +
                                        "#" +
                                        st.nextToken());
                // (Client.getMainFrame().getMainPanel().getCommPanel()).
                // removeHttpLinksFromEditorPane(CCommPanel.CHANNEL_MISC);
            } else if (command.equalsIgnoreCase("BUYU")) {
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX +
                                        "c requestdonated#" +
                                        st.nextToken() +
                                        "#" +
                                        st.nextToken());
                // (Client.getMainFrame().getMainPanel().getCommPanel()).
                // removeHttpLinksFromEditorPane(CCommPanel.CHANNEL_MISC);
            } else if (command.equalsIgnoreCase("BUYP")) {
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX +
                                        "c buypilotsfromhouse#" +
                                        st.nextToken() +
                                        "#" +
                                        st.nextToken());
                // (Client.getMainFrame().getMainPanel().getCommPanel()).
                // removeHttpLinksFromEditorPane(CCommPanel.CHANNEL_MISC);
            }
        }
    }

}
