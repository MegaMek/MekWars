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
 * The (sub-)House Status panel shown in the MekWars client's main window. It renders, as a single scrollable block
 * of server-styled HTML, the player's faction's production status: for each unit weight class and unit-type
 * category the server has enabled (Mek, Vehicle, Infantry, ProtoMek, Battle Armor, Aero), it shows the number of
 * available production components, clickable open/closed factory icons (with refresh-timer countdowns) that let the
 * player request a freshly-built unit, and a list of "Bays" containing already-built/donated units sitting in the
 * hangar that can be requested instead. A "Buy New" / "Buy Used" button row at the bottom opens popup menus so the
 * player can directly request a new-production or used/salvage unit (and, if the server allows it, queue a personal
 * pilot) without needing to click through the factory/bay HTML links.
 * <p>
 * The panel does not talk to the server directly to fetch this data; instead it is populated incrementally by
 * {@code FactionStatusScreenUpdateCommand} pushes from the server, which call {@link #addFactionUnit(String)},
 * {@link #removeFactionUnit(String)}, {@link #addFactionFactory(String)}, {@link #removeFactionFactory(String)},
 * {@link #changeFactionFactory(String)} and {@link #changeFactionComponents(String)} to keep the in-memory maps
 * up to date, after which {@link #updateDisplay()} is expected to be called (by the owner of this panel) to
 * re-render the HTML from the current state of those maps.
 */
public class CHSPanel extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(CHSPanel.class);

    @Serial
    private static final long serialVersionUID = -6985292870326367798L;
    /** Panel holding the "Buy New" / "Buy Used" buttons, laid out via {@link SpringLayoutHelper}; hidden while {@link #lblInfo} is showing rollover text (see {@link #setInfoText(String)}). */
    private final JPanel hsButtonSpringPanel;
    /**
     * Label used in place of the buy-button row to show contextual rollover/status text (e.g. hovering over a
     * factory icon). Its minimum size is captured lazily from {@link #hsButtonSpringPanel}'s size the first time
     * it's shown, so the label and button row occupy the same footprint when swapped via {@link #setInfoText(String)}.
     */
    private final JLabel lblInfo = new JLabel();
    /**
     * Component ("mini-tick") counts per weight/type combo. Keyed by {@code "<weight>$<type>"} (both as their
     * integer string forms), value is {@code "<currentComponents>$<producibleUnits>"}. Populated/replaced wholesale
     * by {@link #changeFactionComponents(String)}.
     */
    private final TreeMap<String, String> componentsInfo;
    /**
     * Known factories, keyed by {@code "<weight>$<type>"}, each mapping to a nested {@link TreeMap} keyed by
     * {@code "<planet>$<factoryName>"} (sorted alphabetically by world) whose value encodes
     * {@code "<founder>$<planet>$<factoryName>$<timeToRefresh>$<accessLevel>$<factoryID>"}. Maintained by
     * {@link #addFactionFactory(String)}, {@link #removeFactionFactory(String)} and {@link #changeFactionFactory(String)}.
     */
    private final TreeMap<String, TreeMap<String, String>> factoriesInfo;
    /**
     * Units currently sitting in the hangar/donation bays, keyed by {@code "<weight>$<type>"} to a {@link Vector} of
     * {@link HSMek} wrappers. Maintained by {@link #addFactionUnit(String)} and {@link #removeFactionUnit(String)}.
     */
    private final TreeMap<String, Vector<HSMek>> unitsInfo;
    /** Single shared listener for every "Buy New"/"Buy Used" popup menu item; parses the action command and issues the actual purchase/queue request. */
    private final BuyPopupListener myPopup;
    /** The client/campaign connection supplying server config values and the active player/campaign data. */
    private final IClient client;
    /** The local player, cached once at construction time from {@code client.getCampaign().getPlayer()}. */
    private final CPlayer thePlayer;
    /** Read-only HTML pane that renders the status table/bays built by {@link #updateDisplay()}. */
    private final JEditorPane mainPane = new JEditorPane();
    /** Display name of the (sub-)faction this panel is showing status for; set via {@link #setFactionName(String)} and used as the status table's header. */
    private String HouseName;

    /**
     * Builds the panel layout: a scrollable {@link #mainPane} on top (via {@link GridBagLayout}) and a bottom row
     * containing the "Buy New"/"Buy Used" buttons (or, alternately, {@link #lblInfo} when rollover text is being
     * shown). Also initializes the empty {@link #componentsInfo}/{@link #factoriesInfo}/{@link #unitsInfo} maps that
     * later get populated by the {@code addFaction*}/{@code changeFaction*} methods as server updates arrive.
     *
     * @param client the client/campaign connection this panel reads player/config data from
     */
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
    /**
     * Intentionally empty {@link ActionListener} for the "Buy New" button. The button also has a
     * {@link MouseAdapter#mousePressed(MouseEvent)} handler ({@link #buyNewUnitMouseEvent(MouseEvent)}) that opens
     * the popup menu on mouse-down; this action-performed handler exists only to satisfy the method-reference
     * listener registration and does nothing. Quirk: because popup opening lives in {@code mousePressed} rather
     * than {@code actionPerformed}, activating the button via keyboard (Enter/Space while focused) fires this
     * empty handler and will not open the buy menu — the button is effectively mouse-only.
     */
    private void buyNewButtonActionPerformed(ActionEvent event) {
    }// do nothing on action

    /** Opens the "Buy New" popup menu at the mouse-press location on the Buy New button. */
    private void buyNewUnitMouseEvent(MouseEvent event) {
        JPopupMenu buy = createBuyNewPopupMenu();
        buy.show(event.getComponent(), event.getX(), event.getY());
    }

    /**
     * Intentionally empty {@link ActionListener} for the "Buy Used" button; see
     * {@link #buyNewButtonActionPerformed(ActionEvent)} for why this is a deliberate no-op and the resulting
     * keyboard-activation quirk.
     */
    private void buyUsedButtonActionPerformed(ActionEvent event) {
    }// do nothing

    /** Opens the "Buy Used" popup menu at the mouse-press location on the Buy Used button. */
    private void buyUsedUnitMouseEvent(MouseEvent event) {
        JPopupMenu buy = createBuyUsedPopupMenu();
        buy.show(event.getComponent(), event.getX(), event.getY());
    }

    /**
     * Builds the "Buy New" popup menu: one submenu per unit-type category the server has enabled (Mek always
     * present; Vehicle/Infantry/ProtoMek/Battle Armor/Aero gated by the corresponding {@code Use<Type>} server
     * config flag), each containing four weight-class items (Light/Medium/Heavy/Assault) whose action command is
     * {@code "BUY|<WEIGHT>|<unitTypeConstant>"}. If the server allows personal pilot queues
     * ({@code AllowPersonalPilotQueues}), an additional "Pilots" submenu is added with per-unit-type,
     * per-weight-class items using action command {@code "BUYP|<unitTypeConstant>|<weightConstant>"} (note the
     * argument order is swapped relative to the unit-buying commands). Every item shares the single
     * {@link #myPopup} listener, which is responsible for interpreting the action command and issuing the request.
     *
     * @return a freshly-built popup menu reflecting the server's currently enabled unit types
     */
    private JPopupMenu createBuyNewPopupMenu() {
        JMenu tmenu;
        JPopupMenu buy = new JPopupMenu();
        JMenuItem menuItem;

        tmenu = new JMenu("Mek");
        buy.add(tmenu);
        menuItem = new JMenuItem("Light Mek");
        menuItem.setActionCommand(String.format("BUY|LIGHT|%s", Unit.MEK));
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        menuItem = new JMenuItem("Medium Mek");
        menuItem.setActionCommand(String.format("BUY|MEDIUM|%s", Unit.MEK));
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        menuItem = new JMenuItem("Heavy Mek");
        menuItem.setActionCommand(String.format("BUY|HEAVY|%s", Unit.MEK));
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        menuItem = new JMenuItem("Assault Mek");
        menuItem.setActionCommand(String.format("BUY|ASSAULT|%s", Unit.MEK));
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        if (Boolean.parseBoolean(client.getServerConfigs("UseVehicle"))) {
            tmenu = new JMenu("Vehicle");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light Vehicle");
            menuItem.setActionCommand(String.format("BUY|LIGHT|%s", Unit.VEHICLE));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Vehicle");
            menuItem.setActionCommand(String.format("BUY|MEDIUM|%s", Unit.VEHICLE));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Vehicle");
            menuItem.setActionCommand(String.format("BUY|HEAVY|%s", Unit.VEHICLE));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Vehicle");
            menuItem.setActionCommand(String.format("BUY|ASSAULT|%s", Unit.VEHICLE));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseInfantry"))) {
            tmenu = new JMenu("Infantry");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light Infantry");
            menuItem.setActionCommand(String.format("BUY|LIGHT|%s", Unit.INFANTRY));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Infantry");
            menuItem.setActionCommand(String.format("BUY|MEDIUM|%s", Unit.INFANTRY));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Infantry");
            menuItem.setActionCommand(String.format("BUY|HEAVY|%s", Unit.INFANTRY));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Infantry");
            menuItem.setActionCommand(String.format("BUY|ASSAULT|%s", Unit.INFANTRY));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseProtoMek"))) {
            tmenu = new JMenu("ProtoMek");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light ProtoMek");
            menuItem.setActionCommand(String.format("BUY|LIGHT|%s", Unit.PROTOMEK));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium ProtoMek");
            menuItem.setActionCommand(String.format("BUY|MEDIUM|%s", Unit.PROTOMEK));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy ProtoMek");
            menuItem.setActionCommand(String.format("BUY|HEAVY|%s", Unit.PROTOMEK));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault ProtoMek");
            menuItem.setActionCommand(String.format("BUY|ASSAULT|%s", Unit.PROTOMEK));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseBattleArmor"))) {
            tmenu = new JMenu("Battle Armor");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light Battle Armor");
            menuItem.setActionCommand(String.format("BUY|LIGHT|%s", Unit.BATTLEARMOR));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Battle Armor");
            menuItem.setActionCommand(String.format("BUY|MEDIUM|%s", Unit.BATTLEARMOR));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Battle Armor");
            menuItem.setActionCommand(String.format("BUY|HEAVY|%s", Unit.BATTLEARMOR));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Battle Armor");
            menuItem.setActionCommand(String.format("BUY|ASSAULT|%s", Unit.BATTLEARMOR));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseAero"))) {
            tmenu = new JMenu("Aero");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light Aero");
            menuItem.setActionCommand(String.format("BUY|LIGHT|%s", Unit.AERO));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Aero");
            menuItem.setActionCommand(String.format("BUY|MEDIUM|%s", Unit.AERO));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Aero");
            menuItem.setActionCommand(String.format("BUY|HEAVY|%s", Unit.AERO));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Aero");
            menuItem.setActionCommand(String.format("BUY|ASSAULT|%s", Unit.AERO));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("AllowPersonalPilotQueues"))) {
            tmenu = new JMenu("Pilots");
            buy.add(tmenu);
            JMenu smenu = new JMenu("Mek");
            menuItem = new JMenuItem("Light Pilot");
            menuItem.setActionCommand(String.format("BUYP|%s|%s", Unit.MEK, Unit.LIGHT));
            menuItem.addActionListener(myPopup);
            smenu.add(menuItem);

            menuItem = new JMenuItem("Medium Pilot");
            menuItem.setActionCommand(String.format("BUYP|%s|%s", Unit.MEK, Unit.MEDIUM));
            menuItem.addActionListener(myPopup);
            smenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Pilot");
            menuItem.setActionCommand(String.format("BUYP|%s|%s", Unit.MEK, Unit.HEAVY));
            menuItem.addActionListener(myPopup);
            smenu.add(menuItem);

            menuItem = new JMenuItem("Assault Pilot");
            menuItem.setActionCommand(String.format("BUYP|%s|%s", Unit.MEK, Unit.ASSAULT));
            menuItem.addActionListener(myPopup);
            smenu.add(menuItem);
            tmenu.add(smenu);

            if (Boolean.parseBoolean(client.getServerConfigs("UseProtoMek"))) {
                smenu = new JMenu("Proto");
                menuItem = new JMenuItem("Light Pilot");
                menuItem.setActionCommand(String.format("BUYP|%s|%s", Unit.PROTOMEK, Unit.LIGHT));
                menuItem.addActionListener(myPopup);
                smenu.add(menuItem);

                menuItem = new JMenuItem("Medium Pilot");
                menuItem.setActionCommand(String.format("BUYP|%s|%s", Unit.PROTOMEK, Unit.MEDIUM));
                menuItem.addActionListener(myPopup);
                smenu.add(menuItem);

                menuItem = new JMenuItem("Heavy Pilot");
                menuItem.setActionCommand(String.format("BUYP|%s|%s", Unit.PROTOMEK, Unit.HEAVY));
                menuItem.addActionListener(myPopup);
                smenu.add(menuItem);

                menuItem = new JMenuItem("Assault Pilot");
                menuItem.setActionCommand(String.format("BUYP|%s|%s", Unit.PROTOMEK, Unit.ASSAULT));
                menuItem.addActionListener(myPopup);
                smenu.add(menuItem);
                tmenu.add(smenu);
            }
        }

        return buy;
    }

    /**
     * Builds the "Buy Used" popup menu: same per-unit-type/weight-class structure as
     * {@link #createBuyNewPopupMenu()}, gated by the same server config flags, but action commands use the
     * {@code "BUYU|<WEIGHT>|<unitTypeConstant>"} prefix instead of {@code "BUY|..."} and there is no pilot-queue
     * submenu (used units come with their own existing crew).
     *
     * @return a freshly-built popup menu reflecting the server's currently enabled unit types
     */
    private JPopupMenu createBuyUsedPopupMenu() {
        JMenu tmenu;
        JPopupMenu buy = new JPopupMenu();
        JMenuItem menuItem;

        tmenu = new JMenu("Mek");
        buy.add(tmenu);
        menuItem = new JMenuItem("Light Mek");
        menuItem.setActionCommand(String.format("BUYU|LIGHT|%s", Unit.MEK));
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        menuItem = new JMenuItem("Medium Mek");
        menuItem.setActionCommand(String.format("BUYU|MEDIUM|%s", Unit.MEK));
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        menuItem = new JMenuItem("Heavy Mek");
        menuItem.setActionCommand(String.format("BUYU|HEAVY|%s", Unit.MEK));
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        menuItem = new JMenuItem("Assault Mek");
        menuItem.setActionCommand(String.format("BUYU|ASSAULT|%s", Unit.MEK));
        menuItem.addActionListener(myPopup);
        tmenu.add(menuItem);

        if (Boolean.parseBoolean(client.getServerConfigs("UseVehicle"))) {
            tmenu = new JMenu("Vehicle");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light Vehicle");
            menuItem.setActionCommand(String.format("BUYU|LIGHT|%s", Unit.VEHICLE));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Vehicle");
            menuItem.setActionCommand(String.format("BUYU|MEDIUM|%s", Unit.VEHICLE));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Vehicle");
            menuItem.setActionCommand(String.format("BUYU|HEAVY|%s", Unit.VEHICLE));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Vehicle");
            menuItem.setActionCommand(String.format("BUYU|ASSAULT|%s", Unit.VEHICLE));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }
        if (Boolean.parseBoolean(client.getServerConfigs("UseInfantry"))) {
            tmenu = new JMenu("Infantry");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light Infantry");
            menuItem.setActionCommand(String.format("BUYU|LIGHT|%s", Unit.INFANTRY));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Infantry");
            menuItem.setActionCommand(String.format("BUYU|MEDIUM|%s", Unit.INFANTRY));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Infantry");
            menuItem.setActionCommand(String.format("BUYU|HEAVY|%s", Unit.INFANTRY));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Infantry");
            menuItem.setActionCommand(String.format("BUYU|ASSAULT|%s", Unit.INFANTRY));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseProtoMek"))) {
            tmenu = new JMenu("ProtoMek");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light ProtoMek");
            menuItem.setActionCommand(String.format("BUYU|LIGHT|%s", Unit.PROTOMEK));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Infantry");
            menuItem.setActionCommand(String.format("BUYU|MEDIUM|%s", Unit.PROTOMEK));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Infantry");
            menuItem.setActionCommand(String.format("BUYU|HEAVY|%s", Unit.PROTOMEK));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Infantry");
            menuItem.setActionCommand(String.format("BUYU|ASSAULT|%s", Unit.PROTOMEK));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseBattleArmor"))) {
            tmenu = new JMenu("Battle Armor");
            buy.add(tmenu);
            menuItem = new JMenuItem("Light Battle Armor");
            menuItem.setActionCommand(String.format("BUYU|LIGHT|%s", Unit.BATTLEARMOR));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Battle Armor");
            menuItem.setActionCommand(String.format("BUYU|MEDIUM|%s", Unit.BATTLEARMOR));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Battle Armor");
            menuItem.setActionCommand(String.format("BUYU|HEAVY|%s", Unit.BATTLEARMOR));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Battle Armor");
            menuItem.setActionCommand(String.format("BUYU|ASSAULT|%s", Unit.BATTLEARMOR));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("UseAero"))) {
            tmenu = new JMenu("Aero");
            buy.add(tmenu);

            menuItem = new JMenuItem("Light Aero");
            menuItem.setActionCommand(String.format("BUYU|LIGHT|%s", Unit.AERO));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Medium Aero");
            menuItem.setActionCommand(String.format("BUYU|MEDIUM|%s", Unit.AERO));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Heavy Aero");
            menuItem.setActionCommand(String.format("BUYU|HEAVY|%s", Unit.AERO));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);

            menuItem = new JMenuItem("Assault Aero");
            menuItem.setActionCommand(String.format("BUYU|ASSAULT|%s", Unit.AERO));
            menuItem.addActionListener(myPopup);
            tmenu.add(menuItem);
        }

        return buy;
    }

    /** @return the client/campaign connection this panel is bound to. */
    public IClient getClient() {
        return client;
    }

    /**
     * Set the faction name. Called in response to FactionStatusScreenUpdateCommand|FN| command.
     *
     * @param name the display name to show as the status table's header
     */
    public void setFactionName(String name) {
        HouseName = name;
    }

    /**
     * Clear all faction data. Empties {@link #componentsInfo}, {@link #factoriesInfo} and {@link #unitsInfo};
     * typically called before repopulating this panel for a different (sub-)faction or on reconnect. Does not
     * itself trigger a re-render — callers must invoke {@link #updateDisplay()} afterward to reflect the cleared
     * state in the HTML view.
     */
    public void clearHouseStatusData() {
        componentsInfo.clear();
        factoriesInfo.clear();
        unitsInfo.clear();
    }

    /**
     * Add a unit to the units' hash. Called from FactionStatusScreenUpdateCommand.java when the client receives
     * FactionStatusScreenUpdateCommand|AU|data command.
     *
     * @param unitData {@code "<weight>$<type>$..."} where the remaining {@code $}-delimited tokens are consumed by
     *                 the {@link HSMek#HSMek(StringTokenizer)} constructor
     */
    public void addFactionUnit(String unitData) {

        StringTokenizer tokenizer = new StringTokenizer(unitData, "$");

        String weight = tokenizer.nextToken();
        String type = tokenizer.nextToken();

        HSMek currHSUnit = new HSMek(tokenizer);// reads rest of
        // tokens

        // if there isn't a vector for this type + weight combo already, create
        // one
        Vector<HSMek> weightAndTypeVec = unitsInfo.computeIfAbsent(String.format("%s$%s", weight, type),
              ignored -> new Vector<>(1, 1));

        // add the unit to the vector
        weightAndTypeVec.add(currHSUnit);
    }

    /**
     * Remove a unit from the units' hash+vector sets. Called from FactionStatusScreenUpdateCommand.java when client
     * receives FactionStatusScreenUpdateCommand|RU| command. If no bucket exists for the given weight/type, or no
     * unit with the given id is found in it, this is a silent no-op.
     *
     * @param unitData {@code "<weight>$<type>$<unitID>"}
     */
    public void removeFactionUnit(String unitData) {

        StringTokenizer tokenizer = new StringTokenizer(unitData, "$");

        String weight = tokenizer.nextToken();
        String type = tokenizer.nextToken();
        int unitID = Integer.parseInt(tokenizer.nextToken());

        Vector<HSMek> weightAndTypeVec = unitsInfo.get(String.format("%s$%s", weight, type));

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
     *
     * @param componentData {@code "<weight>$<type>$<currentPP>$<producibleUnits>"}
     */
    public void changeFactionComponents(String componentData) {

        StringTokenizer tokenizer = new StringTokenizer(componentData, "$");

        String weight = tokenizer.nextToken();
        String type = tokenizer.nextToken();

        String currentPP = tokenizer.nextToken();
        String prodUnits = tokenizer.nextToken();

        // remove old value, if any, and insert new value
        componentsInfo.remove(String.format("%s$%s", weight, type));
        componentsInfo.put(String.format("%s$%s", weight, type), String.format("%s$%s", currentPP, prodUnits));
    }

    /**
     * Add a factory. Called from FactionStatusScreenUpdateCommand.java when FactionStatusScreenUpdateCommand|AF|
     * command received. A single factory can be a "multi-production" facility capable of building several unit-type
     * categories at once (encoded as a bitmask in {@code type}, see {@link #canProduce(int, int)}); this method adds
     * the same factory listing under every unit-type bucket it can build for, via {@link #addFactoryHelper}.
     *
     * @param factoryData {@code "<weight>$<typeBitmask>$<founder>$<planet>$<factoryName>$<timeToRefresh>$<accessLevel>$<factoryID>"}
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
     * Helper used to determine which unit types a multi-fac can produce. {@code productionCapabilities} is a
     * bitmask built from the {@code UnitFactory.BUILD_*} flag constants; this decodes it by repeatedly subtracting
     * the largest-remaining flag value that still fits, checking at each step whether the subtracted flag
     * corresponds to {@code type_id}. {@code UnitFactory.BUILD_ALL} (0) is special-cased up front to mean "can
     * build everything".
     *
     * @param type_id                the {@code Unit.*} type constant being tested
     * @param productionCapabilities the factory's raw production-capability bitmask
     * @return {@code true} if the factory can build unit type {@code type_id}
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
     *
     * @param weight        weight class index (0..3)
     * @param type          the specific {@code Unit.*} type this listing is being recorded under
     * @param timeToRefresh turns/time remaining until the factory produces its next unit (0 = ready now)
     * @param founder       the house/faction that originally built the factory
     * @param planet        the world the factory is located on
     * @param factoryName   the factory's display name
     * @param accessLevel   minimum sub-faction access level required to see/use this factory
     * @param factoryID     server-side identifier for this factory, echoed back in purchase requests
     */
    private void addFactoryHelper(int weight, int type, int timeToRefresh, String founder, String planet,
          String factoryName, int accessLevel, String factoryID) {

        // if there isn't a vector for this type + weight combo already, create
        // one
        TreeMap<String, String> weightAndTypeMap = factoriesInfo.computeIfAbsent(String.format("%s$%s", weight, type),
              ignored -> new TreeMap<>());

        /*
         * Add the factory to the map. Note that we use a map so the factories
         * appear in alpha order, by world.
         */
        weightAndTypeMap.put(String.format("%s$%s", planet, factoryName),
              String.format("%s$%s$%s$%s$%s$%s", founder, planet, factoryName, timeToRefresh, accessLevel, factoryID));
    }

    /**
     * Remove a factory from house status. Used when client receives a FactionStatusScreenUpdateCommand|RF| command.
     * Usually after a world changes hands.
     * <p>
     * Format: FactionStatusScreenUpdateCommand|RF|weight$metatype$planet$name|
     *
     * @param factoryData {@code "<weight>$<typeBitmask>$<planet>$<factoryName>"}
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
     *
     * @param weight      weight class index (0..3)
     * @param type        the specific {@code Unit.*} type bucket to remove the factory from
     * @param planet      the world the factory is located on
     * @param factoryName the factory's display name
     */
    private void removeFactoryHelper(int weight, int type, String planet, String factoryName) {

        TreeMap<String, String> weightAndTypeMap = factoriesInfo.get(String.format("%s$%s", weight, type));

        // if weight and type map is null, there is no way to remove the
        // factory.
        if (weightAndTypeMap == null) {
            return;
        }

        // iterate through all facs of this weight&type. remove matching names.
        weightAndTypeMap.keySet().removeIf(currName -> currName.equals(String.format("%s$%s", planet, factoryName)));
    }

    /**
     * Change a factory's information. Used to update refresh times. Format:
     * FactionStatusScreenUpdateCommand|CF|weight$metatype$name$planet$timetorefresh|
     *
     * @param factoryData {@code "<weight>$<typeBitmask>$<planet>$<factoryName>$<timeToRefresh>$<accessLevel>$<factoryID>"}
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
     * Helper that abstracts out some repetitive checks from checkFactionFactory. Looks up the existing entry
     * (logging a debug message and bailing out if the weight/type bucket or the specific factory can't be found)
     * purely to recover its {@code founder} value, which isn't part of the incoming change data, then overwrites
     * the entry with the new refresh time/access level/factory id.
     *
     * @param weight        weight class index (0..3)
     * @param type          the specific {@code Unit.*} type bucket to update
     * @param planet        the world the factory is located on
     * @param factoryName   the factory's display name
     * @param timeToRefresh new turns/time remaining until next production
     * @param accessLevel   new minimum sub-faction access level required to use this factory
     * @param factoryID     new server-side identifier for this factory
     */
    private void changeFactoryHelper(int weight, int type, String planet, String factoryName, int timeToRefresh,
          int accessLevel, String factoryID) {

        TreeMap<String, String> weightAndTypeMap = factoriesInfo.get(String.format("%s$%s", weight, type));

        // if weight and type map is null, there is no way to change the
        // factory.
        if (weightAndTypeMap == null) {
            LOGGER.debug("Error updating factory: null treemap at weight & type.");
            return;
        }

        // no factory with matching name on planet. return.
        String oldFactoryInfo = weightAndTypeMap.get(String.format("%s$%s", planet, factoryName));
        if (oldFactoryInfo == null) {
            LOGGER.debug("Error updating factory: null oldFactory.");
            return;
        }

        // get the founder, which wasn't transferred.
        StringTokenizer tokenizer = new StringTokenizer(oldFactoryInfo, "$");
        String founder = tokenizer.nextToken();

        // overwrite the old entry
        weightAndTypeMap.put(String.format("%s$%s", planet, factoryName),
              String.format("%s$%s$%s$%s$%s$%s", founder, planet, factoryName, timeToRefresh, accessLevel, factoryID));
    }

    /**
     * Rebuilds and pushes the full HTML content of {@link #mainPane} from the current contents of
     * {@link #componentsInfo}, {@link #factoriesInfo} and {@link #unitsInfo}. This is the panel's main "render"
     * step and should be called after any of the {@code addFaction*}/{@code removeFaction*}/{@code changeFaction*}
     * methods (or {@link #clearHouseStatusData()}) mutate that state.
     * <p>
     * Structure of the generated HTML:
     * <ol>
     * <li>A header table with one column per weight class (Light/Medium/Heavy/Assault) and one row per enabled
     * unit-type category that has at least one known factory ({@link #hasFactories(int)}); unit types the server
     * doesn't use ({@code Use<Type>} config flag) are skipped entirely.</li>
     * <li>Each cell shows the component ("mini-tick") count and producible-unit count for that weight/type, then,
     * if the player's sub-faction is allowed to buy new units of that weight/type, either: a countdown icon if every
     * factory of that weight/type is still refreshing, or one clickable open/closed factory icon per factory
     * (open = ready to buy now, linking to a {@code MEKWARS/c request#...} command with computed C-bill/Influence/
     * component costs; closed = still refreshing, showing its refresh time). Factories the player's access level
     * doesn't permit are skipped (their existence still counts toward "has open factories" bookkeeping).
     * Non-owning-house purchases get their costs scaled by the {@code NonOriginal*Multiplier} server configs.</li>
     * <li>A second pass renders "Bays" sections per unit-type/weight listing already-built units available for
     * donation request (if the player's sub-faction is allowed to buy used units of that type/weight). Units are
     * sorted alphabetically by name, with ties broken by gunnery, then piloting, then battle-damage string; runs of
     * otherwise-identical units are collapsed into a single "N x Name (gunnery/piloting)" entry. Each entry links to
     * a {@code MEKINFO...} URL (handled elsewhere to open {@link #showInfoWindow}) and, when Advanced Repairs is in
     * use, is colored based on damage severity (blue = destroyed/can't start up, red = critical damage, yellow =
     * armor damage only).</li>
     * </ol>
     * The resulting HTML string entirely replaces {@code mainPane}'s previous content (cleared to {@code ""} first)
     * rather than being incrementally patched, which is simple but means the whole status screen re-renders on
     * every single incoming update.
     */
    public void updateDisplay() {

        // Returns the Private Status for Members only
        StringBuilder result =
              new StringBuilder(String.format("<BODY  TEXT=\"%s\" BGCOLOR=\"%s\">", client.getConfigParam("CHAT_FONT_COLOR"), client.getConfigParam(
                    "BACKGROUND_COLOR")));
        boolean usingAdvanceRepairs = client.isUsingAdvanceRepairs();
        int playerAccessLevel = client.getPlayer().getSubFactionAccess();
        result.append(String.format("<TABLE Border=\"1\"><TR><TH>%s</TH><TH>%s</TH><TH>%s</TH><TH>%s</TH><TH>%s</TH></TR>", HouseName, client.getServerConfigs(
              "LightFactoryTypeTitle"), client.getServerConfigs("MediumFactoryTypeTitle"), client.getServerConfigs(
              "HeavyFactoryTypeTitle"), client.getServerConfigs("AssaultFactoryTypeTitle")));
        int factoryGifCounter;

        for (int type_id = 0; type_id < Unit.TOTAL_TYPES; type_id++) {

            // hide unit types that aren't in use on the server
            String useIt = String.format("Use%s", Unit.getTypeClassDesc(type_id));

            if (!Boolean.parseBoolean(client.getServerConfigs(useIt))) {
                continue;
            }

            if (!hasFactories(type_id)) {
                continue;
            }

            String factoryTitle = client.getServerConfigs(String.format("%sFactoryClassTitle", Unit.getTypeClassDesc(type_id)));
            result.append("<TR><TD VALIGN=MIDDLE><b>").append(factoryTitle).append("</b></TD>");

            for (int weight = 0; weight < 4; weight++) {

                String buyNew = String.format("CanBuyNew%s%s", Unit.getWeightClassDesc(weight), Unit.getTypeClassDesc(type_id));

                String Comps = componentsInfo.get(String.format("%s$%s", weight, type_id));
                StringTokenizer ST = new StringTokenizer(Comps, "$");
                int comps = Integer.parseInt(ST.nextToken());
                if ((comps > 0) || (factoriesInfo.get(String.format("%s$%s", weight, type_id)) != null)) {

                    result.append("<TD>" + "<img src=\"data/images/miniticks.gif\">:").append(comps);
                    result.append("<img src=\"data/images/units.gif\">:").append(ST.nextToken()).append("<br>");

                    // Needed because of the binary coding.

                    TreeMap<String, String> factories = factoriesInfo.get(String.format("%s$%s", weight, type_id));
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

                            String openImage = String.format("data/images/open%s.gif", founder);
                            String closeImage = String.format("data/images/closed%s.gif", founder);

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

                                String costString = String.format("(Cost: %s, %s, %s Components)", client.moneyOrFluMessage(true,
                                      true,
                                      cbillCost,
                                      false), client.moneyOrFluMessage(false,
                                      true,
                                      fluCost,
                                      false), ppCost);

                                StringBuilder openLink = new StringBuilder();
                                openLink.append("<a href=\"MEKWARS/c request#").append(weight)
                                      .append('#').append(type_id)
                                      .append('#').append(planet)
                                      .append('#').append(factoryName)
                                      .append("\"><img border=\"0\" alt=\"Click to buy a ").append(founder)
                                      .append(' ').append(Unit.getTypeClassDesc(type_id))
                                      .append(" from ").append(factoryName)
                                      .append(" on ").append(planet)
                                      .append(". ").append(costString)
                                      .append("\" src=\"").append(openImage)
                                      .append("\"></a>");
                                result.append(openLink);
                                hasOpen = true;

                            } else {
                                StringBuilder closedLink = new StringBuilder();
                                closedLink.append("<a href=\"MEKWARS/c request#").append(weight)
                                      .append('#').append(type_id)
                                      .append('#').append(planet)
                                      .append('#').append(factoryName)
                                      .append("\"<img border=\"0\" alt=\"").append(factoryName)
                                      .append(" on ").append(planet)
                                      .append(" built by ").append(founder)
                                      .append(" (Refresh Time: ").append(refreshTime)
                                      .append(")\" src=\"").append(closeImage)
                                      .append("\"></a>");
                                result.append(closedLink);
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
            String useIt = String.format("Use%s", Unit.getTypeClassDesc(type));
            if (!Boolean.parseBoolean(client.getServerConfigs(useIt))) {
                continue;
            }

            // if the house has any units at all, add a Bays: title.
            boolean hasUnits = false;
            for (int weight = 0; weight < 4; weight++) {
                if (unitsInfo.get(String.format("%s$%s", weight, type)) != null) {
                    hasUnits = true;
                }
            }
            if (hasUnits) {
                String factoryTitle = client.getServerConfigs(String.format("%sFactoryClassTitle", Unit.getTypeClassDesc(type)));
                result.append(String.format("<b>%s Bays</b><br>", factoryTitle));
            }

            // fill out bays
            for (int weight = 0; weight < 4; weight++) {

                String buyUsed = String.format("CanBuyUsed%s%s", Unit.getWeightClassDesc(weight), Unit.getTypeClassDesc(type));
                if (!Boolean.parseBoolean(thePlayer.getSubFaction().getConfig(buyUsed))) {
                    continue;
                }

                if ((unitsInfo.get(String.format("%s$%s", weight, type)) != null) &&
                          (!unitsInfo.get(String.format("%s$%s", weight, type)).isEmpty())) {
                    House foundH = client.getData().getHouseByName(client.getPlayer().getMyHouse().getName());
                    int cBillCost = Math.round(CUnit.getPriceForUnit(client, weight, type, foundH) *
                                                     foundH.getUsedMekBayMultiplier()) +
                                          client.getPlayer().getHangarPurchasePenalty(type, weight);
                    int fluCost = Math.round(CUnit.getInfluenceForUnit(client, weight, type, foundH) *
                                                   foundH.getUsedMekBayMultiplier());
                    StringBuilder donatedLink = new StringBuilder();
                    donatedLink.append("<a href=\"MEKWARS/c requestdonated#").append(weight)
                          .append('#').append(type)
                          .append("\"><img border=\"0\" alt=\"Request one of the Units from this bay (Cost: ")
                          .append(client.moneyOrFluMessage(true, true, cBillCost, false))
                          .append(", ")
                          .append(client.moneyOrFluMessage(false, true, fluCost, false))
                          .append(")\" src=\"data/images/cart.gif\"></a> ")
                          .append(Unit.getWeightClassDesc(weight))
                          .append(": ");
                    result.append(donatedLink);
                    Vector<HSMek> v = unitsInfo.get(String.format("%s$%s", weight, type));
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
                            unitString.append(String.format("<a href=\"MEKINFO%s#%s#%s#%s#%s\">%s (%s/%s)", m.getMekFile(), m.getBV(), m.getEntity()
                                                                                                           .getCrew()
                                                                                                           .getGunnery(), m.getEntity()
                                                                                                                                  .getCrew()
                                                                                                                                  .getPiloting(), m.getBattleDamage(), m.getName(), m.getEntity()
                                                                                                                                                                                                   .getCrew()
                                                                                                                                                                                                   .getGunnery(), m.getEntity()
                                                                                                                                                                                                                          .getCrew()
                                                                                                                                                                                                                          .getPiloting()));
                        } else {
                            if ((m.getEntity() instanceof Infantry) &&
                                      ((Infantry) m.getEntity()).canMakeAntiMekAttacks()) {
                                unitString.append(String.format("<a href=\"MEKINFO%s#%s#%s#%s#%s\">%s (%s/%s)", m.getMekFile(), m.getBV(), m.getEntity()
                                                                                                               .getCrew()
                                                                                                               .getGunnery(), m.getEntity()
                                                                                                                                      .getCrew()
                                                                                                                                      .getPiloting(), m.getBattleDamage(), m.getName(), m.getEntity()
                                                                                                                                                                                                       .getCrew()
                                                                                                                                                                                                       .getGunnery(), m.getEntity()
                                                                                                                                                                                                                              .getCrew()
                                                                                                                                                                                                                              .getPiloting()));
                            } else {
                                unitString.append(String.format("<a href=\"MEKINFO%s#%s#%s#%s#%s\">%s (%s)", m.getMekFile(), m.getBV(), m.getEntity()
                                                                                                               .getCrew()
                                                                                                               .getGunnery(), m.getEntity()
                                                                                                                                      .getCrew()
                                                                                                                                      .getPiloting(), m.getBattleDamage(), m.getName(), m.getEntity()
                                                                                                                                                                                                       .getCrew()
                                                                                                                                                                                                       .getGunnery()));
                            }
                        }

                        // front load the dupe indicator, to reduce confusion
                        // with mono-skill units
                        if (num > 1) {
                            unitString.insert(0, String.format("%s x ", num));
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

    /**
     * @param type the {@code Unit.*} type to check
     * @return {@code true} if at least one weight class (0..{@link Unit#ASSAULT}) has any known factory entry for
     *         this unit type, used by {@link #updateDisplay()} to decide whether to render that type's status row
     *         at all.
     */
    private boolean hasFactories(int type) {

        for (int weight = 0; weight <= Unit.ASSAULT; weight++) {
            if (factoriesInfo.get(String.format("%s$%s", weight, type)) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Shows or hides the contextual rollover-info label ({@link #lblInfo}) in place of the Buy New/Buy Used button
     * row ({@link #hsButtonSpringPanel}); presumably wired up elsewhere (e.g. a hyperlink hover listener over the
     * status HTML) to surface details about whatever the mouse is currently over.
     * <p>
     * Passing {@code null} or an empty string hides the label and restores the button row. Passing non-empty text
     * shows the label with that text and hides the buttons; the label's minimum size is captured from the button
     * row's current size the very first time this happens (when {@link #lblInfo} still has zero width/height), so
     * that swapping between the two doesn't change the panel's overall footprint. If the button row hasn't been
     * laid out yet (size still zero) on that first call, the captured minimum size will itself be zero, and later
     * calls will never re-capture it since the check only looks at {@link #lblInfo}'s own size, not the button
     * row's.
     *
     * @param s the text to show in the info label, or {@code null}/empty to show the buy buttons instead
     */
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

    /**
     * Opens a standalone preview window (a plain {@link javax.swing.JFrame}, not a modal dialog) showing a unit's
     * loadout/stats via {@link MWUnitDisplay}, used when the user clicks one of the {@code MEKINFO...} hyperlinks
     * embedded in the status HTML built by {@link #updateDisplay()}. Builds a throwaway single-person {@link Crew}
     * with the given gunnery/piloting skills (the same gunnery value is used for all three weapon-skill slots) purely
     * to drive the stat display, and applies the supplied battle-damage string if non-trivial. The {@code bv}
     * parameter is accepted but not used by this method.
     *
     * @param mekFile      unit definition file identifying which unit to load/display
     * @param bv           battle value of the unit (currently unused here)
     * @param gunnery      gunnery skill to assign the preview crew (applied to all weapon-skill slots)
     * @param piloting     piloting skill to assign the preview crew
     * @param battleDamage encoded battle-damage string (see {@link UnitUtils#applyBattleDamage}); ignored if it
     *                     trims down to length &lt;= 1
     */
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
