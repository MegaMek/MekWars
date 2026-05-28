/*
 * Copyright (C) 2004 Helge Richter (McWizard)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekwars.common.gui.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.Serial;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.LineBorder;

import megamek.client.generator.RandomGenderGenerator;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.logging.MMLogger;
import mekwars.common.House;
import mekwars.common.Unit;
import mekwars.common.UnitFactory;
import mekwars.common.campaign.CCampaign;
import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.HSMek;
import mekwars.common.gui.MWUnitDisplay;
import mekwars.common.gui.MyHTMLEditorKit;
import mekwars.common.gui.listeners.BuyPopupListener;
import mekwars.common.gui.listeners.MMNetHyperLinkListener;
import mekwars.common.util.SpringLayoutHelper;
import mekwars.common.util.UnitUtils;

/**
 * SHouse Status Panel
 */

public class CHSPanel extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(CHSPanel.class);

    @Serial
    private static final long serialVersionUID = -6985292870326367798L;
    private final JPanel hsButtonSpringPanel;
    private final JLabel lblInfo = new JLabel();
    private final TreeMap<String, String> componentsInfo;
    private final TreeMap<String, TreeMap<String, String>> factoriesInfo;
    private final TreeMap<String, Vector<HSMek>> unitsInfo;
    private final BuyPopupListener myPopup;
    private final IClient client;
    private final CPlayer thePlayer;
    private final JEditorPane mainPane = new JEditorPane();
    private String HouseName;

    public CHSPanel(IClient client) {
        setLayout(new GridBagLayout());
        this.client = client;
        CCampaign theCampaign = this.client.getCampaign();
        thePlayer = theCampaign.getPlayer();
        myPopup = new BuyPopupListener(this);

        MyHTMLEditorKit kit = new MyHTMLEditorKit();
        mainPane.setEditorKit(kit);
        mainPane.setEditable(false);
        mainPane.addHyperlinkListener(new MMNetHyperLinkListener(this.client, this));
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setViewportBorder(new LineBorder(new java.awt.Color(0, 0, 0)));
        scrollPane.setViewportView(mainPane);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        this.add(scrollPane, gridBagConstraints);

        // set up the button row
        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.Y_AXIS));
        hsButtonSpringPanel = new JPanel(new SpringLayout());

        // button to buy new units
        JButton buyNewButton = new JButton();
        buyNewButton.setText("Buy New");
        buyNewButton.addActionListener(this::buyNewButtonActionPerformed);
        buyNewButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) {
                buyNewUnitMouseEvent(evt);
            }
        });
        hsButtonSpringPanel.add(buyNewButton);

        // button to buy used units
        JButton buyUsedButton = new JButton();
        buyUsedButton.setText("Buy Used");
        buyUsedButton.addActionListener(this::buyUsedButtonActionPerformed);
        buyUsedButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) {
                buyUsedUnitMouseEvent(evt);
            }
        });
        hsButtonSpringPanel.add(buyUsedButton);

        SpringLayoutHelper.setupSpringGrid(hsButtonSpringPanel, 1, 3);

        // set up lblInfo, which is used when rolling over factories
        lblInfo.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        lblInfo.setSize(0, 0);

        pnlButtons.add(lblInfo);
        pnlButtons.add(hsButtonSpringPanel);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        this.add(pnlButtons, gridBagConstraints);

        // make information holders
        componentsInfo = new java.util.TreeMap<>();
        factoriesInfo = new java.util.TreeMap<>();
        unitsInfo = new java.util.TreeMap<>();
    }

    // BUY MENU METHODS AND LISTENERS
    private void buyNewButtonActionPerformed(ActionEvent event) {
    }// do nothing on action

    // make popup on press or release of New button
    private void buyNewUnitMouseEvent(MouseEvent event) {
        JPopupMenu buy = createBuyNewPopupMenu();
        buy.show(event.getComponent(), event.getX(), event.getY());
    }

    private void buyUsedButtonActionPerformed(ActionEvent event) {
    }// do nothing

    private void buyUsedUnitMouseEvent(MouseEvent event) {
        JPopupMenu buy = createBuyUsedPopupMenu();
        buy.show(event.getComponent(), event.getX(), event.getY());
    }

    private JPopupMenu createBuyNewPopupMenu() {
        JMenu tmenu;
        JPopupMenu buy = new JPopupMenu();
        JMenuItem menuItem;

        tmenu = new JMenu("Mek");
        buy.add(tmenu);
        menuItem = new JMenuItem("Light Mek");
        menuItem.setActionCommand(STR."BUY|LIGHT|\{Unit.MEK}");
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        menuItem = new JMenuItem("Medium Mek");
        menuItem.setActionCommand(STR."BUY|MEDIUM|\{Unit.MEK}");
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        menuItem = new JMenuItem("Heavy Mek");
        menuItem.setActionCommand(STR."BUY|HEAVY|\{Unit.MEK}");
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        menuItem = new JMenuItem("Assault Mek");
        menuItem.setActionCommand(STR."BUY|ASSAULT|\{Unit.MEK}");
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        if (Boolean.parseBoolean(client.getServerConfigs("UseVehicle"))) {
            tmenu = new JMenu("Vehicle");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light Vehicle");
            menuItem.setActionCommand(STR."BUY|LIGHT|\{Unit.VEHICLE}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Vehicle");
            menuItem.setActionCommand(STR."BUY|MEDIUM|\{Unit.VEHICLE}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Vehicle");
            menuItem.setActionCommand(STR."BUY|HEAVY|\{Unit.VEHICLE}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Vehicle");
            menuItem.setActionCommand(STR."BUY|ASSAULT|\{Unit.VEHICLE}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseInfantry"))) {
            tmenu = new JMenu("Infantry");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light Infantry");
            menuItem.setActionCommand(STR."BUY|LIGHT|\{Unit.INFANTRY}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Infantry");
            menuItem.setActionCommand(STR."BUY|MEDIUM|\{Unit.INFANTRY}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Infantry");
            menuItem.setActionCommand(STR."BUY|HEAVY|\{Unit.INFANTRY}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Infantry");
            menuItem.setActionCommand(STR."BUY|ASSAULT|\{Unit.INFANTRY}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseProtoMek"))) {
            tmenu = new JMenu("ProtoMek");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light ProtoMek");
            menuItem.setActionCommand(STR."BUY|LIGHT|\{Unit.PROTOMEK}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium ProtoMek");
            menuItem.setActionCommand(STR."BUY|MEDIUM|\{Unit.PROTOMEK}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy ProtoMek");
            menuItem.setActionCommand(STR."BUY|HEAVY|\{Unit.PROTOMEK}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault ProtoMek");
            menuItem.setActionCommand(STR."BUY|ASSAULT|\{Unit.PROTOMEK}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseBattleArmor"))) {
            tmenu = new JMenu("Battle Armor");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light Battle Armor");
            menuItem.setActionCommand(STR."BUY|LIGHT|\{Unit.BATTLEARMOR}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Battle Armor");
            menuItem.setActionCommand(STR."BUY|MEDIUM|\{Unit.BATTLEARMOR}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Battle Armor");
            menuItem.setActionCommand(STR."BUY|HEAVY|\{Unit.BATTLEARMOR}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Battle Armor");
            menuItem.setActionCommand(STR."BUY|ASSAULT|\{Unit.BATTLEARMOR}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseAero"))) {
            tmenu = new JMenu("Aero");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light Aero");
            menuItem.setActionCommand(STR."BUY|LIGHT|\{Unit.AERO}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Aero");
            menuItem.setActionCommand(STR."BUY|MEDIUM|\{Unit.AERO}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Aero");
            menuItem.setActionCommand(STR."BUY|HEAVY|\{Unit.AERO}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Aero");
            menuItem.setActionCommand(STR."BUY|ASSAULT|\{Unit.AERO}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("AllowPersonalPilotQueues"))) {
            tmenu = new JMenu("Pilots");
            buy.add(tmenu);
            JMenu smenu = new JMenu("Mek");
            menuItem = new JMenuItem("Light Pilot");
            menuItem.setActionCommand(STR."BUYP|\{Unit.MEK}|\{Unit.LIGHT}");
            menuItem.addActionListener(myPopup);
            smenu.add(menuItem);

            menuItem = new JMenuItem("Medium Pilot");
            menuItem.setActionCommand(STR."BUYP|\{Unit.MEK}|\{Unit.MEDIUM}");
            menuItem.addActionListener(myPopup);
            smenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Pilot");
            menuItem.setActionCommand(STR."BUYP|\{Unit.MEK}|\{Unit.HEAVY}");
            menuItem.addActionListener(myPopup);
            smenu.add(menuItem);

            menuItem = new JMenuItem("Assault Pilot");
            menuItem.setActionCommand(STR."BUYP|\{Unit.MEK}|\{Unit.ASSAULT}");
            menuItem.addActionListener(myPopup);
            smenu.add(menuItem);
            tmenu.add(smenu);

            if (Boolean.parseBoolean(client.getServerConfigs("UseProtoMek"))) {
                smenu = new JMenu("Proto");
                menuItem = new JMenuItem("Light Pilot");
                menuItem.setActionCommand(STR."BUYP|\{Unit.PROTOMEK}|\{Unit.LIGHT}");
                menuItem.addActionListener(myPopup);
                smenu.add(menuItem);

                menuItem = new JMenuItem("Medium Pilot");
                menuItem.setActionCommand(STR."BUYP|\{Unit.PROTOMEK}|\{Unit.MEDIUM}");
                menuItem.addActionListener(myPopup);
                smenu.add(menuItem);

                menuItem = new JMenuItem("Heavy Pilot");
                menuItem.setActionCommand(STR."BUYP|\{Unit.PROTOMEK}|\{Unit.HEAVY}");
                menuItem.addActionListener(myPopup);
                smenu.add(menuItem);

                menuItem = new JMenuItem("Assault Pilot");
                menuItem.setActionCommand(STR."BUYP|\{Unit.PROTOMEK}|\{Unit.ASSAULT}");
                menuItem.addActionListener(myPopup);
                smenu.add(menuItem);
                tmenu.add(smenu);
            }
        }

        return buy;
    }

    private JPopupMenu createBuyUsedPopupMenu() {
        JMenu tmenu;
        JPopupMenu buy = new JPopupMenu();
        JMenuItem menuItem;

        tmenu = new JMenu("Mek");
        buy.add(tmenu);
        menuItem = new JMenuItem("Light Mek");
        menuItem.setActionCommand(STR."BUYU|LIGHT|\{Unit.MEK}");
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        menuItem = new JMenuItem("Medium Mek");
        menuItem.setActionCommand(STR."BUYU|MEDIUM|\{Unit.MEK}");
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        menuItem = new JMenuItem("Heavy Mek");
        menuItem.setActionCommand(STR."BUYU|HEAVY|\{Unit.MEK}");
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        menuItem = new JMenuItem("Assault Mek");
        menuItem.setActionCommand(STR."BUYU|ASSAULT|\{Unit.MEK}");
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        if (Boolean.parseBoolean(client.getServerConfigs("UseVehicle"))) {
            tmenu = new JMenu("Vehicle");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light Vehicle");
            menuItem.setActionCommand(STR."BUYU|LIGHT|\{Unit.VEHICLE}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Vehicle");
            menuItem.setActionCommand(STR."BUYU|MEDIUM|\{Unit.VEHICLE}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Vehicle");
            menuItem.setActionCommand(STR."BUYU|HEAVY|\{Unit.VEHICLE}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Vehicle");
            menuItem.setActionCommand(STR."BUYU|ASSAULT|\{Unit.VEHICLE}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }
        if (Boolean.parseBoolean(client.getServerConfigs("UseInfantry"))) {
            tmenu = new JMenu("Infantry");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light Infantry");
            menuItem.setActionCommand(STR."BUYU|LIGHT|\{Unit.INFANTRY}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Infantry");
            menuItem.setActionCommand(STR."BUYU|MEDIUM|\{Unit.INFANTRY}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Infantry");
            menuItem.setActionCommand(STR."BUYU|HEAVY|\{Unit.INFANTRY}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Infantry");
            menuItem.setActionCommand(STR."BUYU|ASSAULT|\{Unit.INFANTRY}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseProtoMek"))) {
            tmenu = new JMenu("ProtoMek");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light ProtoMek");
            menuItem.setActionCommand(STR."BUYU|LIGHT|\{Unit.PROTOMEK}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Infantry");
            menuItem.setActionCommand(STR."BUYU|MEDIUM|\{Unit.PROTOMEK}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Infantry");
            menuItem.setActionCommand(STR."BUYU|HEAVY|\{Unit.PROTOMEK}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Infantry");
            menuItem.setActionCommand(STR."BUYU|ASSAULT|\{Unit.PROTOMEK}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseBattleArmor"))) {
            tmenu = new JMenu("Battle Armor");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light Battle Armor");
            menuItem.setActionCommand(STR."BUYU|LIGHT|\{Unit.BATTLEARMOR}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Battle Armor");
            menuItem.setActionCommand(STR."BUYU|MEDIUM|\{Unit.BATTLEARMOR}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Battle Armor");
            menuItem.setActionCommand(STR."BUYU|HEAVY|\{Unit.BATTLEARMOR}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Battle Armor");
            menuItem.setActionCommand(STR."BUYU|ASSAULT|\{Unit.BATTLEARMOR}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseAero"))) {
            tmenu = new JMenu("Aero");
            buy.add(tmenu);

            menuItem = new JMenuItem("Light Aero");
            menuItem.setActionCommand(STR."BUYU|LIGHT|\{Unit.AERO}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Aero");
            menuItem.setActionCommand(STR."BUYU|MEDIUM|\{Unit.AERO}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Aero");
            menuItem.setActionCommand(STR."BUYU|HEAVY|\{Unit.AERO}");
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Aero");
            menuItem.setActionCommand(STR."BUYU|ASSAULT|\{Unit.AERO}");
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
     * Add a unit to the units' hash. Called from FactionStatusScreenUpdateCommand.java when the client receives
     * FactionStatusScreenUpdateCommand|AU|data command.
     */
    public void addFactionUnit(String unitData) {

        StringTokenizer tokenizer = new StringTokenizer(unitData, "$");

        String weight = tokenizer.nextToken();
        String type = tokenizer.nextToken();

        HSMek currHSUnit = new HSMek(tokenizer);// reads rest of
        // tokens

        // if there isn't a vector for this type + weight combo already, create
        // one
        Vector<HSMek> weightAndTypeVec = unitsInfo.computeIfAbsent(STR."\{weight}$\{type}",
              _ -> new Vector<>(1, 1));

        // add the unit to the vector
        weightAndTypeVec.add(currHSUnit);
    }

    /**
     * Remove a unit from the units' hash+vector sets. Called from FactionStatusScreenUpdateCommand.java when client
     * receives FactionStatusScreenUpdateCommand|RU| command.
     */
    public void removeFactionUnit(String unitData) {

        StringTokenizer tokenizer = new StringTokenizer(unitData, "$");

        String weight = tokenizer.nextToken();
        String type = tokenizer.nextToken();
        int unitID = Integer.parseInt(tokenizer.nextToken());

        Vector<HSMek> weightAndTypeVec = unitsInfo.get(STR."\{weight}$\{type}");

        // if weight and type are null, there is no way to remove the unit.
        if (weightAndTypeVec == null) {
            return;
        }

        // iterate through all units of this weight &type. remove matching id.
        Iterator<HSMek> i = weightAndTypeVec.iterator();
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

        StringTokenizer tokenizer = new StringTokenizer(componentData, "$");

        String weight = tokenizer.nextToken();
        String type = tokenizer.nextToken();

        String currentPP = tokenizer.nextToken();
        String prodUnits = tokenizer.nextToken();

        // remove old value, if any, and insert new value
        componentsInfo.remove(STR."\{weight}$\{type}");
        componentsInfo.put(STR."\{weight}$\{type}", STR."\{currentPP}$\{prodUnits}");
    }

    /**
     * Add a factory. Called from FactionStatusScreenUpdateCommand.java when FactionStatusScreenUpdateCommand|AF|
     * command received.
     */
    public void addFactionFactory(String factoryData) {

        StringTokenizer tokenizer = new StringTokenizer(factoryData, "$");

        // read factory data
        int weight = Integer.parseInt(tokenizer.nextToken());
        int type = Integer.parseInt(tokenizer.nextToken());

        String founder = tokenizer.nextToken();
        String planet = tokenizer.nextToken();
        String factoryName = tokenizer.nextToken();

        int timeToRefresh = Integer.parseInt(tokenizer.nextToken());
        int accessLevel = Integer.parseInt(tokenizer.nextToken());

        String factoryID = tokenizer.nextToken();

        /*
         * Check for multiproduction and add to all appropriate factory
         * categories. Overly complex, and makes me want to punch the person who
         * RFE'ed Multics in the face
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
        if (productionCapabilities == UnitFactory.BUILD_ALL) {
            return true;
        }

        int test = productionCapabilities;

        if ((test - UnitFactory.BUILD_AERO) >= 0) {
            test -= UnitFactory.BUILD_AERO;
            if (type_id == Unit.AERO) {
                return true;
            }
        }

        if ((test - UnitFactory.BUILD_BATTLEARMOR) >= 0) {
            test -= UnitFactory.BUILD_BATTLEARMOR;
            if (type_id == Unit.BATTLEARMOR) {
                return true;
            }
        }

        if ((test - UnitFactory.BUILD_PROTOMEKS) >= 0) {
            test -= UnitFactory.BUILD_PROTOMEKS;
            if (type_id == Unit.PROTOMEK) {
                return true;
            }
        }

        if ((test - UnitFactory.BUILD_INFANTRY) >= 0) {
            test -= UnitFactory.BUILD_INFANTRY;
            if (type_id == Unit.INFANTRY) {
                return true;
            }
        }

        if ((test - UnitFactory.BUILD_VEHICLES) >= 0) {
            test -= UnitFactory.BUILD_VEHICLES;
            if (type_id == Unit.VEHICLE) {
                return true;
            }
        }

        if ((test - UnitFactory.BUILD_MEK) >= 0) {
            return type_id == Unit.MEK;
        }

        return false;
    }

    /**
     * Private method called only from addFactionFactory. Abstracts out some repetitive code that checks for factory
     * vectors and creates missing listings.
     */
    private void addFactoryHelper(int weight, int type, int timeToRefresh, String founder, String planet,
          String factoryName, int accessLevel, String factoryID) {

        // if there isn't a vector for this type + weight combo already, create
        // one
        TreeMap<String, String> weightAndTypeMap = factoriesInfo.computeIfAbsent(STR."\{weight}$\{type}",
              _ -> new TreeMap<>());

        /*
         * Add the factory to the map. Note that we use a map so the factories
         * appear in alpha order, by world.
         */
        weightAndTypeMap.put(STR."\{planet}$\{factoryName}",
              STR."\{founder}$\{planet}$\{factoryName}$\{timeToRefresh}$\{accessLevel}$\{factoryID}");
    }

    /**
     * Remove a factory from house status. Used when client receives a FactionStatusScreenUpdateCommand|RF| command.
     * Usually after a world changes hands.
     * <p>
     * Format: FactionStatusScreenUpdateCommand|RF|weight$metatype$planet$name|
     */
    public void removeFactionFactory(String factoryData) {

        StringTokenizer tokenizer = new StringTokenizer(factoryData, "$");

        int weight = Integer.parseInt(tokenizer.nextToken());
        int type = Integer.parseInt(tokenizer.nextToken());

        String planet = tokenizer.nextToken();
        String factoryName = tokenizer.nextToken();

        /*
         * Check for multiproduction and remove from all appropriate factory
         * categories. Overly complex, and makes me want to punch the person who
         * RFE'ed Multics in the face :-(
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
     * Helper that abstracts out some repetitive checks from removeFactionFactory.
     */
    private void removeFactoryHelper(int weight, int type, String planet, String factoryName) {

        TreeMap<String, String> weightAndTypeMap = factoriesInfo.get(STR."\{weight}$\{type}");

        // if weight and type map is null, there is no way to remove the
        // factory.
        if (weightAndTypeMap == null) {
            return;
        }

        // iterate through all facs of this weight&type. remove matching names.
        weightAndTypeMap.keySet().removeIf(currName -> currName.equals(STR."\{planet}$\{factoryName}"));
    }

    /**
     * Change a factory's information. Used to update refresh times. Format:
     * FactionStatusScreenUpdateCommand|CF|weight$metatype$name$planet$timetorefresh|
     */
    public void changeFactionFactory(String factoryData) {

        StringTokenizer tokenizer = new StringTokenizer(factoryData, "$");

        int weight = Integer.parseInt(tokenizer.nextToken());
        int type = Integer.parseInt(tokenizer.nextToken());

        String planet = tokenizer.nextToken();
        String factoryName = tokenizer.nextToken();

        int timeToRefresh = Integer.parseInt(tokenizer.nextToken());

        int accessLevel = Integer.parseInt(tokenizer.nextToken());

        String factoryID = tokenizer.nextToken();

        /*
         * Check for multiproduction and update in all appropriate factory
         * categories. Overly complex, and makes me want to punch the person who
         * RFE'ed Multics in the face :-(
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
     * Helper that abstracts out some repetitive checks from checkFactionFactory.
     */
    private void changeFactoryHelper(int weight, int type, String planet, String factoryName, int timeToRefresh,
          int accessLevel, String factoryID) {

        TreeMap<String, String> weightAndTypeMap = factoriesInfo.get(STR."\{weight}$\{type}");

        // if weight and type map is null, there is no way to change the
        // factory.
        if (weightAndTypeMap == null) {
            LOGGER.debug("Error updating factory: null treemap at weight & type.");
            return;
        }

        // no factory with matching name on planet. return.
        String oldFactoryInfo = weightAndTypeMap.get(STR."\{planet}$\{factoryName}");
        if (oldFactoryInfo == null) {
            LOGGER.debug("Error updating factory: null oldFactory.");
            return;
        }

        // get the founder, which wasn't transferred.
        StringTokenizer tokenizer = new StringTokenizer(oldFactoryInfo, "$");
        String founder = tokenizer.nextToken();

        // overwrite the old entry
        weightAndTypeMap.put(STR."\{planet}$\{factoryName}",
              STR."\{founder}$\{planet}$\{factoryName}$\{timeToRefresh}$\{accessLevel}$\{factoryID}");
    }

    public void updateDisplay() {

        // Returns the Private Status for Members only
        StringBuilder result =
              new StringBuilder(STR."<BODY  TEXT=\"\{client.getConfigParam("CHAT_FONT_COLOR")}\" BGCOLOR=\"\{client.getConfigParam(
                    "BACKGROUND_COLOR")}\">");
        boolean usingAdvanceRepairs = client.isUsingAdvanceRepairs();
        int playerAccessLevel = client.getPlayer().getSubFactionAccess();
        result.append(STR."<TABLE Border=\"1\"><TR><TH>\{HouseName}</TH><TH>\{client.getServerConfigs(
              "LightFactoryTypeTitle")}</TH><TH>\{client.getServerConfigs("MediumFactoryTypeTitle")}</TH><TH>\{client.getServerConfigs(
              "HeavyFactoryTypeTitle")}</TH><TH>\{client.getServerConfigs("AssaultFactoryTypeTitle")}</TH></TR>");
        int factoryGifCounter;

        for (int type_id = 0; type_id < Unit.TOTAL_TYPES; type_id++) {

            // hide unit types that aren't in use on the server
            String useIt = STR."Use\{Unit.getTypeClassDesc(type_id)}";

            if (!Boolean.parseBoolean(client.getServerConfigs(useIt))) {
                continue;
            }

            if (!hasFactories(type_id)) {
                continue;
            }

            String factoryTitle = client.getServerConfigs(STR."\{Unit.getTypeClassDesc(type_id)}FactoryClassTitle");
            result.append("<TR><TD VALIGN=MIDDLE><b>").append(factoryTitle).append("</b></TD>");

            for (int weight = 0; weight < 4; weight++) {

                String buyNew = STR."CanBuyNew\{Unit.getWeightClassDesc(weight)}\{Unit.getTypeClassDesc(type_id)}";

                String Comps = componentsInfo.get(STR."\{weight}$\{type_id}");
                StringTokenizer ST = new StringTokenizer(Comps, "$");
                int comps = Integer.parseInt(ST.nextToken());
                if ((comps > 0) || (factoriesInfo.get(STR."\{weight}$\{type_id}") != null)) {

                    result.append("<TD>" + "<img src=\"data/images/miniticks.gif\">:").append(comps);
                    result.append("<img src=\"data/images/units.gif\">:").append(ST.nextToken()).append("<br>");

                    // Needed because of the binary coding.

                    TreeMap<String, String> factories = factoriesInfo.get(STR."\{weight}$\{type_id}");
                    if ((factories != null) && Boolean.parseBoolean(thePlayer.getSubFaction().getConfig(buyNew))) {

                        boolean hasOpen = false;
                        int minrefresh = Integer.MAX_VALUE;

                        factoryGifCounter = 0;
                        for (String Fac : factories.values()) {

                            ST = new StringTokenizer(Fac, "$");
                            String founder = ST.nextToken();
                            String planet = ST.nextToken();
                            String factoryName = ST.nextToken();
                            int refreshTime = Integer.parseInt(ST.nextToken());
                            int accessLevel = Integer.parseInt(ST.nextToken());
                            ST.nextToken();

                            String openImage = STR."data/images/open\{founder}.gif";
                            String closeImage = STR."data/images/closed\{founder}.gif";

                            if (!new File(openImage).exists()) {
                                openImage = "data/images/open.gif";
                            }

                            if (!new File(closeImage).exists()) {
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
                                House foundH = client.getData().getHouseByName(founder);
                                int cbillCost = CUnit.getPriceForUnit(client,
                                      weight,
                                      type_id,
                                      foundH) + client.getPlayer().getHangarPurchasePenalty(type_id, weight);
                                int fluCost = CUnit.getInfluenceForUnit(client,
                                      weight,
                                      type_id,
                                      foundH);
                                int ppCost = CUnit.getPPForUnit(client, weight, type_id, foundH);

                                if (!client.getPlayer().getMyHouse().getName().equalsIgnoreCase(foundH.getName())) {
                                    cbillCost = Math.round(cbillCost *
                                                                 Float.parseFloat(client.getServerConfigs(
                                                                       "NonOriginalCBillMultiplier"))) +
                                                      client.getPlayer().getHangarPurchasePenalty(type_id, weight);
                                    fluCost = Math.round(fluCost *
                                                               Float.parseFloat(client.getServerConfigs(
                                                                     "NonOriginalInfluenceMultiplier")));
                                    ppCost = Math.round(ppCost *
                                                              Float.parseFloat(client.getServerConfigs(
                                                                    "NonOriginalComponentMultiplier")));
                                }

                                String costString = STR."(Cost: \{client.moneyOrFluMessage(true,
                                      true,
                                      cbillCost,
                                      false)}, \{client.moneyOrFluMessage(false,
                                      true,
                                      fluCost,
                                      false)}, \{ppCost} Components)";

                                result.append(STR."<a href=\"MEKWARS/c request#\{weight}#\{type_id}#\{planet}#\{factoryName}\"><img border=\"0\" alt=\"Click to buy a \{founder} \{Unit.getTypeClassDesc(
                                      type_id)} from \{factoryName} on \{planet}. \{costString}\" src=\"\{openImage}\"></a>");
                                hasOpen = true;

                            } else {
                                result.append(STR."<a href=\"MEKWARS/c request#\{weight}#\{type_id}#\{planet}#\{factoryName}\"<img border=\"0\" alt=\"\{factoryName} on \{planet} built by \{founder} (Refresh Time: \{refreshTime})\" src=\"\{closeImage}\"></a>");
                                if (refreshTime < minrefresh) {
                                    minrefresh = refreshTime;
                                }
                            }
                        }

                        if (!hasOpen) {
                            result.append("<img src=\"data/images/clock.gif\">:").append(minrefresh);
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
        for (int type = 0; type < Unit.TOTAL_TYPES; type++) {

            // is not using units of the type, skip the listings
            String useIt = STR."Use\{Unit.getTypeClassDesc(type)}";
            if (!Boolean.parseBoolean(client.getServerConfigs(useIt))) {
                continue;
            }

            // if the house has any units at all, add a Bays: title.
            boolean hasUnits = false;
            for (int weight = 0; weight < 4; weight++) {
                if (unitsInfo.get(STR."\{weight}$\{type}") != null) {
                    hasUnits = true;
                }
            }
            if (hasUnits) {
                String factoryTitle = client.getServerConfigs(STR."\{Unit.getTypeClassDesc(type)}FactoryClassTitle");
                result.append(STR."<b>\{factoryTitle} Bays</b><br>");
            }

            // fill out bays
            for (int weight = 0; weight < 4; weight++) {

                String buyUsed = STR."CanBuyUsed\{Unit.getWeightClassDesc(weight)}\{Unit.getTypeClassDesc(type)}";
                if (!Boolean.parseBoolean(thePlayer.getSubFaction().getConfig(buyUsed))) {
                    continue;
                }

                if ((unitsInfo.get(STR."\{weight}$\{type}") != null) &&
                          (!unitsInfo.get(STR."\{weight}$\{type}").isEmpty())) {
                    House foundH = client.getData().getHouseByName(client.getPlayer().getMyHouse().getName());
                    int cBillCost = Math.round(CUnit.getPriceForUnit(client, weight, type, foundH) *
                                                     foundH.getUsedMekBayMultiplier()) +
                                          client.getPlayer().getHangarPurchasePenalty(type, weight);
                    int fluCost = Math.round(CUnit.getInfluenceForUnit(client, weight, type, foundH) *
                                                   foundH.getUsedMekBayMultiplier());
                    result.append(STR."<a href=\"MEKWARS/c requestdonated#\{weight}#\{type}\"><img border=\"0\" alt=\"Request one of the Units from this bay (Cost: \{client.moneyOrFluMessage(
                          true,
                          true,
                          cBillCost,
                          false)}, \{client.moneyOrFluMessage(false,
                          true,
                          fluCost,
                          false)})\" src=\"data/images/cart.gif\"></a> \{Unit.getWeightClassDesc(weight)}: ");
                    Vector<HSMek> v = unitsInfo.get(STR."\{weight}$\{type}");
                    HSMek[] entities = new HSMek[v.size()];

                    for (int i = 0; i < v.size(); i++) {
                        entities[i] = v.elementAt(i);
                    }

                    // alpha sort
                    Arrays.sort(entities, (obj1, obj2) -> {
                        // if the names are the same, check for damage
                        if (obj1.getName().compareTo(obj2.getName()) == 0) {

                            Integer gunneryA = obj1.getEntity().getCrew().getGunnery();
                            Integer gunneryB = obj2.getEntity().getCrew().getGunnery();

                            int compare = gunneryA.compareTo(gunneryB);

                            if (compare != 0) {
                                return compare;
                            }

                            Integer pilotingA = obj1.getEntity().getCrew().getPiloting();
                            Integer pilotingB = obj2.getEntity().getCrew().getPiloting();

                            compare = pilotingA.compareTo(pilotingB);

                            if (compare != 0) {
                                return compare;
                            }

                            String damA = obj1.getBattleDamage();
                            String damB = obj2.getBattleDamage();

                            return damA.compareTo(damB);
                        }
                        // else
                        return obj1.getName().compareTo(obj2.getName());
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
                            unitString.append(STR."<a href=\"MEKINFO\{m.getMekFile()}#\{m.getBV()}#\{m.getEntity()
                                                                                                           .getCrew()
                                                                                                           .getGunnery()}#\{m.getEntity()
                                                                                                                                  .getCrew()
                                                                                                                                  .getPiloting()}#\{m.getBattleDamage()}\">\{m.getName()} (\{m.getEntity()
                                                                                                                                                                                                   .getCrew()
                                                                                                                                                                                                   .getGunnery()}/\{m.getEntity()
                                                                                                                                                                                                                          .getCrew()
                                                                                                                                                                                                                          .getPiloting()})");
                        } else {
                            if ((m.getEntity() instanceof Infantry) &&
                                      ((Infantry) m.getEntity()).canMakeAntiMekAttacks()) {
                                unitString.append(STR."<a href=\"MEKINFO\{m.getMekFile()}#\{m.getBV()}#\{m.getEntity()
                                                                                                               .getCrew()
                                                                                                               .getGunnery()}#\{m.getEntity()
                                                                                                                                      .getCrew()
                                                                                                                                      .getPiloting()}#\{m.getBattleDamage()}\">\{m.getName()} (\{m.getEntity()
                                                                                                                                                                                                       .getCrew()
                                                                                                                                                                                                       .getGunnery()}/\{m.getEntity()
                                                                                                                                                                                                                              .getCrew()
                                                                                                                                                                                                                              .getPiloting()})");
                            } else {
                                unitString.append(STR."<a href=\"MEKINFO\{m.getMekFile()}#\{m.getBV()}#\{m.getEntity()
                                                                                                               .getCrew()
                                                                                                               .getGunnery()}#\{m.getEntity()
                                                                                                                                      .getCrew()
                                                                                                                                      .getPiloting()}#\{m.getBattleDamage()}\">\{m.getName()} (\{m.getEntity()
                                                                                                                                                                                                       .getCrew()
                                                                                                                                                                                                       .getGunnery()})");
                            }
                        }

                        // front load the dupe indicator, to reduce confusion
                        // with mono-skill units
                        if (num > 1) {
                            unitString.insert(0, STR."\{num} x ");
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
                        result.append(unitString);
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
            if (factoriesInfo.get(STR."\{weight}$\{type}") != null) {
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
        } else if (s.isEmpty()) {
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
        Entity unitEntity;
        CUnit embeddedUnit = new CUnit();
        embeddedUnit.setUnitFilename(mekFile);
        embeddedUnit.createEntity();
        unitEntity = embeddedUnit.getEntity();

        javax.swing.JFrame InfoWindow = new javax.swing.JFrame();
        MWUnitDisplay unitDetailInfo = new MWUnitDisplay(null, client);
        unitEntity.loadAllWeapons();
        unitEntity.setCrew(new Crew(CrewType.SINGLE,
              "",
              1,
              gunnery,
              gunnery,
              gunnery,
              piloting,
              RandomGenderGenerator.generate(),
              true,
              null));

        if (battleDamage.trim().length() > 1) {
            UnitUtils.applyBattleDamage(unitEntity, battleDamage, false);
        }

        InfoWindow.getContentPane().add(unitDetailInfo);
        InfoWindow.setSize(300, 400);
        InfoWindow.setResizable(false);
        InfoWindow.setTitle(unitEntity.getModel());
        InfoWindow.setLocationRelativeTo(client.getMainFrame());
        InfoWindow.setVisible(true);
        unitDetailInfo.displayEntity(unitEntity);
    }

}
