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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serial;
import java.io.StringReader;
import java.util.StringTokenizer;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;

import jakarta.annotation.Nonnull;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.MyHTMLEditorKit;
import mekwars.common.gui.listeners.MMNetHyperLinkListener;
import mekwars.common.util.StringUtils;

/**
 * This is a tabbed multi-channel Communications Panel using Swing to manage the display.
 */
/*
 * Communications Panel
 */

public class CCommPanel extends JPanel implements ChangeListener, ComponentListener, MouseListener {
    public static final int CHANNEL_MAIN = 0;
    public static final int CHANNEL_HOUSE_MAIL = 1;
    public static final int CHANNEL_PRIVATE_MAIL = 2;
    public static final int CHANNEL_PERSONAL_LOG = 3;
    public static final int CHANNEL_SYSTEM_LOG = 4;
    public static final int CHANNEL_MISC = 5;
    public static final int CHANNEL_RPG = 6;
    public static final int CHANNEL_MOD = 7;
    public static final int CHANNEL_ERROR = 8;
    /**
     * Maximum buffer of each channel. Each channel will be capped to have no more than this number of characters.
     */
    public static final int MAX_BUFFER = 100000;
    /**
     * Number of characters removed when buffer overflow. If a channels maximum character count is reached, cap that
     * many characters.
     *
     * @see CCommPanel#MAX_BUFFER
     */
    public static final int CAP_BUFFER_AMOUNT = 60000;
    private static final MMLogger LOGGER = MMLogger.create(CCommPanel.class);
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 8754254920729491806L;
    IClient client;
    JTabbedPane CommTPane = new JTabbedPane(SwingConstants.BOTTOM);
    MyHTMLEditorKit kit = new MyHTMLEditorKit();
    JPanel MChannelPanel = new JPanel();
    CCommPanel.CSelectTabAction MChannelSelect = null;
    JEditorPane MChannelEPane = new JEditorPane("text/html", "");
    JScrollPane MChannelSPane = new JScrollPane();
    JPanel HMailPanel = new JPanel();
    CCommPanel.CSelectTabAction HMailSelect = null;
    JEditorPane HMailEPane = new JEditorPane("text/html", "");
    JScrollPane HMailSPane = new JScrollPane();
    JPanel PMailPanel = new JPanel();
    CCommPanel.CSelectTabAction PMailSelect = null;
    JEditorPane PMailEPane = new JEditorPane("text/html", "");
    JScrollPane PMailSPane = new JScrollPane();
    JPanel PLogPanel = new JPanel();
    CCommPanel.CSelectTabAction PLogSelect = null;
    JEditorPane PLogEPane = new JEditorPane("text/html", "");
    JScrollPane PLogSPane = new JScrollPane();
    JPanel SLogPanel = new JPanel();
    CCommPanel.CSelectTabAction SLogSelect = null;
    JEditorPane SLogEPane = new JEditorPane("text/html", "");
    JScrollPane SLogSPane = new JScrollPane();
    JPanel MiscChannelPanel = new JPanel();
    CCommPanel.CSelectTabAction MiscChannelSelect = null;
    JEditorPane MiscChannelEPane = new JEditorPane("text/html", "");
    JScrollPane MiscChannelSPane = new JScrollPane();
    JPanel ModMailPanel = new JPanel();
    CCommPanel.CSelectTabAction ModMailSelect = null;
    JEditorPane ModMailEPane = new JEditorPane("text/html", "");
    JScrollPane ModMailSPane = new JScrollPane();
    JPanel ErrorLogPanel = new JPanel();
    CCommPanel.CSelectTabAction ErrorLogSelect = null;
    JEditorPane ErrorLogEPane = new JEditorPane("text/html", "");
    JScrollPane ErrorLogSPane = new JScrollPane();
    JPanel RPGChannelPanel = new JPanel();
    CCommPanel.CSelectTabAction RPGChannelSelect = null;
    JEditorPane RPGChannelEPane = new JEditorPane("text/html", "");
    JScrollPane RPGChannelSPane = new JScrollPane();
    CChatField chatField;
    Color TabForeground;
    Color TabBackground;
    CCommPanel.CTabForwardAction ForwardCommTab;
    CCommPanel.CTabBackwardAction BackwardCommTab;
    private boolean autoTextUpdate;

    public CCommPanel(IClient client) {
        int index;
        int mnemo;
        String tabText;
        this.client = client;

        /*
         * Instead of making 6 HyperLinkListeners, make one here and use it for
         * all tabs.
         *
         * @urgru 7.17.05
         */
        MMNetHyperLinkListener chatHLL = new MMNetHyperLinkListener(this.client);

        CommTPane.addMouseListener(this);

        TabForeground = StringUtils.html2Color(this.client.getConfigParam("SYS_MESSAGE_COLOR"));
        TabBackground = new JList<>().getSelectionBackground().darker();
        autoTextUpdate = this.client.getConfig().isParam("AUTOSCROLL");
        setLayout(new BorderLayout());
        addComponentListener(this);
        MChannelEPane.setEditable(false);
        MChannelEPane.setCaret(new ScrollCaret());
        MChannelEPane.addHyperlinkListener(chatHLL);
        MChannelEPane.setEditorKit(kit);
        MChannelEPane.addMouseListener(this);
        MChannelEPane.setName(Integer.toString(CHANNEL_MAIN));
        MChannelSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        MChannelSPane.setViewportView(MChannelEPane);
        MChannelPanel.setLayout(new BorderLayout());
        MChannelPanel.add(MChannelSPane, BorderLayout.CENTER);
        String mnemonicText;
        tabText = this.client.getConfig().getParam("MAIN_CHANNEL_TAB_NAME");
        mnemonicText = this.client.getConfig().getParam("MAIN_CHANNEL_MNEMONIC");

        CommTPane.addTab(tabText,
              null,
              MChannelPanel,
              String.format("Interfaction Communication Channel (Alt + %s)", mnemonicText));
        index = CommTPane.indexOfComponent(MChannelPanel);
        mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

        if (mnemo == -1) {
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
        }

        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        MChannelSelect = new CCommPanel.CSelectTabAction(MChannelPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(String.format("alt %s", mnemonicText.toUpperCase())),
              "MChannelSelect");
        getActionMap().put("MChannelSelect", MChannelSelect);

        HMailEPane.setEditable(false);
        HMailEPane.setCaret(new ScrollCaret());
        HMailEPane.addHyperlinkListener(chatHLL);
        HMailEPane.setEditorKit(kit);
        HMailEPane.addMouseListener(this);
        HMailEPane.setName(Integer.toString(CHANNEL_HOUSE_MAIL));
        HMailSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        HMailSPane.setViewportBorder(new LineBorder(new java.awt.Color(0, 0, 0)));
        HMailSPane.setViewportView(HMailEPane);
        HMailPanel.setLayout(new BorderLayout());
        HMailPanel.add(HMailSPane, BorderLayout.CENTER);

        if (this.client.getConfig().isParam("HOUSE_MAIL_VISIBLE")) {

            tabText = this.client.getConfig().getParam("HOUSE_MAIL_TAB_NAME");
            mnemonicText = this.client.getConfig().getParam("HOUSE_MAIL_MNEMONIC");

            CommTPane.addTab(tabText,
                  null,
                  HMailPanel,
                  String.format("House Communication Channel (Alt + %s)", mnemonicText.toUpperCase()));
            index = CommTPane.indexOfComponent(HMailPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            HMailSelect = new CCommPanel.CSelectTabAction(HMailPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(String.format("alt %s", mnemonicText.toUpperCase())),
                  "HMailSelect");
            getActionMap().put("HMailSelect", HMailSelect);
        }

        /* RPG-Channel */
        RPGChannelEPane.setEditable(false);
        RPGChannelEPane.setCaret(new ScrollCaret());
        RPGChannelEPane.addHyperlinkListener(chatHLL);
        RPGChannelEPane.setEditorKit(kit);
        RPGChannelEPane.addMouseListener(this);
        RPGChannelEPane.setName(Integer.toString(CHANNEL_RPG));

        RPGChannelSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        RPGChannelSPane.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
        RPGChannelSPane.setViewportView(RPGChannelEPane);
        RPGChannelPanel.setLayout(new BorderLayout());
        RPGChannelPanel.add(RPGChannelSPane, BorderLayout.CENTER);

        if (this.client.getConfig().isParam("RPG_VISIBLE")) {
            tabText = this.client.getConfig().getParam("RPG_TAB_NAME");
            mnemonicText = this.client.getConfig().getParam("RPG_MNEMONIC");

            CommTPane.addTab(tabText, null, RPGChannelPanel, String.format("RP (Alt + %s)", mnemonicText.toUpperCase()));
            index = CommTPane.indexOfComponent(RPGChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            RPGChannelSelect = new CCommPanel.CSelectTabAction(RPGChannelPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(String.format("alt %s", mnemonicText.toUpperCase())),
                  "RPGChannelSelect");
            getActionMap().put("RPGChannelSelect", RPGChannelSelect);
        }

        if (!this.client.getConfig().isParam("USE_MULTIPLE_PM")) {
            PMailEPane.setEditable(false);
            PMailEPane.setCaret(new ScrollCaret());
            PMailEPane.addHyperlinkListener(chatHLL);
            PMailEPane.setEditorKit(kit);
            PMailEPane.addMouseListener(this);
            PMailEPane.setName(Integer.toString(CHANNEL_PRIVATE_MAIL));
            PMailSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            PMailSPane.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
            PMailSPane.setViewportView(PMailEPane);
            PMailPanel.setLayout(new BorderLayout());
            PMailPanel.add(PMailSPane, BorderLayout.CENTER);


            if (this.client.getConfig().isParam("PRIVATE_MAIL_VISIBLE")) {
                tabText = this.client.getConfig().getParam("PRIVATE_MAIL_TAB_NAME");
                mnemonicText = this.client.getConfig().getParam("PRIVATE_MAIL_MNEMONIC");

                CommTPane.addTab(tabText, null, PMailPanel, String.format("Private Mail (Alt + %s)", mnemonicText.toUpperCase()));
                index = CommTPane.indexOfComponent(PMailPanel);
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

                if (mnemo == -1) {
                    mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
                }

                CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
                PMailSelect = new CCommPanel.CSelectTabAction(PMailPanel);
                getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(String.format("alt %s", mnemonicText.toUpperCase())),
                      "PMailSelect");
                getActionMap().put("PMailSelect", PMailSelect);
            }
        }

        PLogEPane.setEditable(false);
        PLogEPane.setCaret(new ScrollCaret());
        PLogEPane.addHyperlinkListener(chatHLL);
        PLogEPane.setEditorKit(kit);
        PLogEPane.addMouseListener(this);
        PLogEPane.setName(Integer.toString(CHANNEL_PERSONAL_LOG));
        PLogSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        PLogSPane.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
        PLogSPane.setViewportView(PLogEPane);
        PLogPanel.setLayout(new BorderLayout());
        PLogPanel.add(PLogSPane, BorderLayout.CENTER);

        if (this.client.getConfig().isParam("PERSONAL_LOG_VISIBLE")) {
            tabText = this.client.getConfig().getParam("PERSONAL_LOG_TAB_NAME");
            mnemonicText = this.client.getConfig().getParam("PERSONAL_LOG_MNEMONIC");

            CommTPane.addTab(tabText, null, PLogPanel, String.format("Logged Messages (Alt + %s)", mnemonicText.toUpperCase()));
            index = CommTPane.indexOfComponent(PLogPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            PLogSelect = new CCommPanel.CSelectTabAction(PLogPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(String.format("alt %s", mnemonicText.toUpperCase())),
                  String.format("PLogSelect (Alt + %s)", mnemonicText.toUpperCase()));
            getActionMap().put("PLogSelect", PLogSelect);
        }

        SLogEPane.setEditable(false);
        SLogEPane.setCaret(new ScrollCaret());
        SLogEPane.addHyperlinkListener(chatHLL);
        SLogEPane.setEditorKit(kit);
        SLogEPane.addMouseListener(this);
        SLogEPane.setName(Integer.toString(CHANNEL_SYSTEM_LOG));
        SLogSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        SLogSPane.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
        SLogSPane.setViewportView(SLogEPane);
        SLogPanel.setLayout(new BorderLayout());
        SLogPanel.add(SLogSPane, BorderLayout.CENTER);

        if (this.client.getConfig().isParam("SYSTEM_LOG_VISIBLE")) {
            tabText = this.client.getConfig().getParam("SYSTEM_LOG_TAB_NAME");
            mnemonicText = this.client.getConfig().getParam("SYSTEM_LOG_MNEMONIC");

            CommTPane.addTab(tabText, null, SLogPanel, String.format("System Messages (Alt + %s)", mnemonicText.toUpperCase()));
            index = CommTPane.indexOfComponent(SLogPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            SLogSelect = new CCommPanel.CSelectTabAction(SLogPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(String.format("alt %s", mnemonicText.toUpperCase())),
                  "SLogSelect");
            getActionMap().put("SLogSelect", SLogSelect);
        }

        /* Misc-Channel */
        MiscChannelEPane.setEditable(false);
        MiscChannelEPane.setCaret(new ScrollCaret());
        MiscChannelEPane.addHyperlinkListener(chatHLL);
        MiscChannelEPane.setEditorKit(kit);
        MiscChannelEPane.addMouseListener(this);
        MiscChannelEPane.setName(Integer.toString(CHANNEL_MISC));
        MiscChannelSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        MiscChannelSPane.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
        MiscChannelSPane.setViewportView(MiscChannelEPane);
        MiscChannelPanel.setLayout(new BorderLayout());
        MiscChannelPanel.add(MiscChannelSPane, BorderLayout.CENTER);

        if (this.client.getConfig().isParam("MISCELLANEOUS_VISIBLE")) {
            tabText = this.client.getConfig().getParam("MISCELLANEOUS_TAB_NAME");
            mnemonicText = this.client.getConfig().getParam("MISCELLANEOUS_MNEMONIC");

            CommTPane.addTab(tabText,
                  null,
                  MiscChannelPanel,
                  String.format("Miscellaneous Stuff (Alt + %s)", mnemonicText.toUpperCase()));
            index = CommTPane.indexOfComponent(MiscChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            MiscChannelSelect = new CCommPanel.CSelectTabAction(MiscChannelPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(String.format("alt %s", mnemonicText.toUpperCase())),
                  "MiscChannelSelect");
            getActionMap().put("MiscChannelSelect", MiscChannelSelect);
        }

        ModMailEPane.setEditable(false);
        ModMailEPane.setCaret(new ScrollCaret());
        ModMailEPane.addHyperlinkListener(chatHLL);
        ModMailEPane.setEditorKit(kit);
        ModMailEPane.addMouseListener(this);
        ModMailEPane.setName(Integer.toString(CHANNEL_MOD));
        ModMailSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ModMailSPane.setViewportBorder(new LineBorder(new java.awt.Color(0, 0, 0)));
        ModMailSPane.setViewportView(ModMailEPane);
        ModMailPanel.setLayout(new BorderLayout());
        ModMailPanel.add(ModMailSPane, BorderLayout.CENTER);

        ErrorLogEPane.setEditable(false);
        ErrorLogEPane.setCaret(new ScrollCaret());
        ErrorLogEPane.addHyperlinkListener(chatHLL);
        ErrorLogEPane.setEditorKit(kit);
        ErrorLogEPane.addMouseListener(this);
        ErrorLogEPane.setName(Integer.toString(CHANNEL_ERROR));
        ErrorLogSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ErrorLogSPane.setViewportBorder(new LineBorder(new java.awt.Color(0, 0, 0)));
        ErrorLogSPane.setViewportView(ErrorLogEPane);
        ErrorLogPanel.setLayout(new BorderLayout());
        ErrorLogPanel.add(ErrorLogSPane, BorderLayout.CENTER);

        CommTPane.setSelectedIndex(CHANNEL_MAIN);
        CommTPane.addChangeListener(this);
        add(CommTPane, BorderLayout.CENTER);
        chatField = new CChatField(this, this.client);
        chatField.setMaximumSize(new Dimension(10000, 100));
        chatField.setMinimumSize(new Dimension(550, 20));
        chatField.addMouseListener(this);
        // this is a little messy, can be fixed later...
        chatField.setReceiver(this::sendChat);

        add(chatField, java.awt.BorderLayout.SOUTH);
        ForwardCommTab = new CCommPanel.CTabForwardAction();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt Z"), "TabForward");
        getActionMap().put("TabForward", ForwardCommTab);
        BackwardCommTab = new CCommPanel.CTabBackwardAction();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("shift alt Z"), "TabBackward");
        getActionMap().put("TabBackward", BackwardCommTab);

        /*
         * Add Function Keys to the maps. Putting them here is fairly hacky; however, the tabbing controls (more
         * forward and back with x/z) are already here, so one more universal listen in CommPanel shouldn't
         * kill anyone.
         *
         * See the CFXKeyAction() private classes for actual functionality.
         * @nmorris 12/15/04
         */

        // map the commands.
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F1"), "HitF1");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F2"), "HitF2");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F3"), "HitF3");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F4"), "HitF4");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F5"), "HitF5");
        getActionMap().put("HitF1", new CCommPanel.CF1KeyAction());
        getActionMap().put("HitF2", new CCommPanel.CF2KeyAction());
        getActionMap().put("HitF3", new CCommPanel.CF3KeyAction());
        getActionMap().put("HitF4", new CCommPanel.CF4KeyAction());
        getActionMap().put("HitF5", new CCommPanel.CF5KeyAction());
    }// end CommPanel()

    public boolean getAutoTextUpdate() {
        return autoTextUpdate;
    }

    public void setAutoTextUpdate(boolean newValue) {
        autoTextUpdate = newValue;
    }

    // something that the user has typed
    public boolean sendChat(String s) {
        if (!s.startsWith(IClient.GUI_PREFIX) &&
                  CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(HMailPanel)) {
            s = String.format("%sc hm#%s", IClient.GUI_PREFIX, s);
        }
        if (!s.startsWith(IClient.GUI_PREFIX) &&
                  CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(ModMailPanel)) {
            s = String.format("%sc mm#%s", IClient.GUI_PREFIX, s);
        }
        if (!s.startsWith(IClient.GUI_PREFIX) &&
                  CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(RPGChannelPanel)) {
            s = String.format("%sc ic#%s", IClient.GUI_PREFIX, s);
        }

        if (!s.startsWith(IClient.GUI_PREFIX) &&
                  !client.getConfig().isParam("USE_MULTIPLE_PM") &&
                  CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(PMailPanel)) {
            String receiver = client.getLastQuery();
            if (receiver != null && !receiver.isEmpty()) {
                s = String.format("%smail %s,%s", IClient.GUI_PREFIX, receiver, s);
            } else {
                client.showInfoWindow("No receiver set.");
                chatField.setText(s);
                return false;
            }
        }
        if (!s.startsWith(
              IClient.GUI_PREFIX) &&
                  client.getConfig().isParam("USE_MULTIPLE_PM") &&
                  CommTPane.getComponent(CommTPane.getSelectedIndex()).getName() != null &&
                  CommTPane.getComponent(CommTPane.getSelectedIndex()).getName()
                        .startsWith("Mail Tab ")) {
            JPanel panel = ((JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex()));
            String mailTab = "Mail Tab ";
            String receiver = panel.getName().substring(mailTab.length()).trim();
            if (!receiver.isEmpty()) {
                s = String.format("%smail %s,%s", IClient.GUI_PREFIX, receiver, s);
            } else {
                client.showInfoWindow("No receiver set.");
                chatField.setText(s);
                return false;
            }
        }

        if (s.startsWith(String.format("%sme", IClient.GUI_PREFIX)) || s.startsWith(String.format("%sc me", IClient.GUI_PREFIX))) {
            if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(RPGChannelPanel)) {
                s += "|ic";
            } else if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(ModMailPanel)) {
                s += "|mm";
            } else if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(HMailPanel)) {
                s += "|hm";
            } else if (!client.getConfig().isParam("USE_MULTIPLE_PM") &&
                             CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(PMailPanel)) {
                s += "|mail";
                String receiver = client.getLastQuery();
                if (receiver != null && !receiver.isEmpty()) {
                    s += String.format("|%s", receiver);
                } else {
                    client.showInfoWindow("No receiver set.");
                    chatField.setText(s);
                    return false;
                }
            } else if (client.getConfig().isParam("USE_MULTIPLE_PM") &&
                             CommTPane.getComponent(CommTPane.getSelectedIndex()).getName() != null &&
                             CommTPane.getComponent(CommTPane.getSelectedIndex()).getName().startsWith("Mail Tab ")) {
                s += "|mail";
                JPanel panel = ((JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex()));
                String mailTab = "Mail Tab ";
                String receiver = panel.getName().substring(mailTab.length()).trim();
                if (!receiver.isEmpty()) {s += String.format("|%s", receiver);} else {
                    client.showInfoWindow("No receiver set.");
                    chatField.setText(s);
                    return false;
                }
            }

        }
        client.processGUIInput(s);
        return true;
    }

    public int countMailTabs() {
        javax.swing.JPanel panel;
        int count = 0;

        for (int pos = 0; pos < CommTPane.getTabCount(); pos++) {
            if (CommTPane.getComponent(pos) instanceof JPanel) {
                panel = (JPanel) CommTPane.getComponent(pos);
                if (panel != null && panel.getName() != null && panel.getName().startsWith("Mail Tab ")) {
                    count++;
                }
            }
        }
        return count;
    }

    public void createMailTab(String tabName) {
        JEditorPane newETab = new JEditorPane("text/html", "");
        JScrollPane newSTab = new JScrollPane();
        javax.swing.JPanel newPanel = new javax.swing.JPanel();
        MMNetHyperLinkListener chatHLL = new MMNetHyperLinkListener(client);
        String mnemonic = Integer.toString(getNextMailTabNumber());

        newETab.setEditable(false);
        newETab.setCaret(new ScrollCaret());
        newETab.addHyperlinkListener(chatHLL);
        newETab.setEditorKit(kit);
        newSTab.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        newSTab.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
        newSTab.setViewportView(newETab);
        newPanel.setLayout(new BorderLayout());
        newPanel.add(newSTab, BorderLayout.CENTER);

        newPanel.setName(String.format("Mail Tab %s", tabName));
        newETab.addMouseListener(this);

        CommTPane.addTab(String.format("%s. %s", mnemonic, tabName),
              null,
              newPanel,
              String.format("Mail From %s (Alt + %s)", tabName, mnemonic));

        int index = CommTPane.indexOfComponent(newPanel);
        int mnemo = CommTPane.getTitleAt(index).indexOf(mnemonic.toUpperCase());

        if (mnemo == -1) {
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonic.toLowerCase());
        }

        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        CCommPanel.CSelectTabAction LogSelect = new CCommPanel.CSelectTabAction(
              newPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(String.format("alt %s", mnemonic.toUpperCase())),
              String.format("Mail From %s", tabName));
        getActionMap().put(String.format("Mail From %s", tabName), LogSelect);
    }

    public int getNextMailTabNumber() {
        JPanel panel;
        int count;
        int maxTabs = client.getConfig().getIntParam("MAX_PM_TABS");
        boolean found;

        for (count = 1; count <= maxTabs; count++) {
            found = false;
            for (int pos = 0; pos < CommTPane.getTabCount(); pos++) {
                if (CommTPane.getComponent(pos) instanceof JPanel) {
                    panel = (JPanel) CommTPane.getComponent(pos);
                    if (panel != null && panel.getName() != null && panel.getName().startsWith("Mail Tab ")) {
                        String tabText = CommTPane.getTitleAt(pos).trim();

                        if (tabText.startsWith("*")) {
                            tabText = tabText.substring(1);
                        }

                        if (MathUtility.parseInt(tabText.substring(0, tabText.indexOf(".")), -1) == count) {
                            found = true;
                            break;// no reason to continue the search.
                        }
                    }
                }
            }

            if (!found) {
                break;
            }
        }
        return count;

    }

    public String getInput() {
        return chatField.getText();
    }

    public void setInput(String input) {
        chatField.setText(input);
    }

    public void setChat(String s, int channel) {
        setChat(s, channel, null);
    }

    // this method needs some tuning (carrot and scrollbar issues)
    public void setChat(String s, int channel, String mailTab) {
        int tabChannel = channel;
        if (channel == CHANNEL_HOUSE_MAIL) {
            if (!client.getConfig().isParam("HOUSE_MAIL_VISIBLE")) {
                printCommLog(s, channel);
                return;
            }
            // else
            tabChannel = CommTPane.indexOfComponent(HMailPanel);
        } else if (channel == CHANNEL_PRIVATE_MAIL) {

            if (!client.getConfig().isParam("PRIVATE_MAIL_VISIBLE")) {
                printCommLog(s, channel);
                return;
            }

            if (client.getConfig().isParam("USE_MULTIPLE_PM")) {
                if (mailTab == null) {
                    return;
                }
                // else
                tabChannel = CommTPane.indexOfComponent(findMailTab(mailTab));
            } else {
                tabChannel = CommTPane.indexOfComponent(PMailPanel);
            }
        } else if (channel == CHANNEL_PERSONAL_LOG) {
            if (!client.getConfig().isParam("PERSONAL_LOG_VISIBLE")) {
                printCommLog(s, channel);
                return;
            }
            // else
            tabChannel = CommTPane.indexOfComponent(PLogPanel);
        } else if (channel == CHANNEL_SYSTEM_LOG) {
            if (!client.getConfig().isParam("SYSTEM_LOG_VISIBLE")) {
                printCommLog(s, channel);
                return;
            }
            // else
            tabChannel = CommTPane.indexOfComponent(SLogPanel);
        } else if (channel == CHANNEL_MISC) {
            if (!client.getConfig().isParam("MISCELLANEOUS_VISIBLE")) {
                printCommLog(s, channel);
                return;
            }
            // else
            tabChannel = CommTPane.indexOfComponent(MiscChannelPanel);
        } else if (channel == CHANNEL_MOD) {
            if (CommTPane.indexOfComponent(ModMailPanel) == -1) {
                createModTab();
            }
            tabChannel = CommTPane.indexOfComponent(ModMailPanel);
        } else if (channel == CHANNEL_ERROR) {
            if (CommTPane.indexOfComponent(ErrorLogPanel) == -1) {
                createErrorTab();
            }
            tabChannel = CommTPane.indexOfComponent(ErrorLogPanel);
        } else if (channel == CHANNEL_RPG) {
            if (!client.getConfig().isParam("RPG_VISIBLE")) {
                printCommLog(s, channel);
                return;
            }
            // else
            tabChannel = CommTPane.indexOfComponent(RPGChannelPanel);
        }

        JEditorPane editorPane = getEditorPane(channel, mailTab);
        JScrollBar scrollbar = null;
        if (editorPane == null) {
            return;
        }

        if (getScrollPane(channel, mailTab) != null) {
            scrollbar = getScrollPane(channel, mailTab).getVerticalScrollBar();
        }

        int oldSize = editorPane.getDocument().getLength();
        boolean scroll = true;
        if (CommTPane.getSelectedIndex() != -1 && CommTPane.getSelectedIndex() != tabChannel) {
            String title = CommTPane.getTitleAt(tabChannel);
            if (!title.startsWith("*")) {
                int mnemo = CommTPane.getDisplayedMnemonicIndexAt(tabChannel);
                mnemo++;
                CommTPane.setTitleAt(tabChannel, String.format("*%s", title));
                CommTPane.setDisplayedMnemonicIndexAt(tabChannel, mnemo);
                if (CommTPane.getComponentAt(tabChannel) != PLogPanel &&
                          CommTPane.getComponentAt(tabChannel) != SLogPanel) {
                    CommTPane.setForegroundAt(tabChannel, TabForeground); // invert

                }
            }
        }
        // if scrollbar was moved up, add text, but keep scrollbar as it is,
        // otherwise scrollbar is almost at the bottom, add text in normal way
        if (scrollbar != null &&
                  !autoTextUpdate &&
                  (scrollbar.getValue() + scrollbar.getVisibleAmount()) < scrollbar.getMaximum() * 0.97) {
            scroll = false;
            ((ScrollCaret) editorPane.getCaret()).showCaret = false;
        } else {
            ((ScrollCaret) editorPane.getCaret()).showCaret = true;
        }

        try {
            if (s.endsWith("<br>")) {
                s = s.substring(0, s.length() - 4);
            }

            editorPane.getEditorKit().read(new StringReader(s),
                  editorPane.getDocument(),
                  editorPane.getDocument().getLength());
        } catch (Exception ex) {
            LOGGER.error(ex, "Issue with Editor Kit {}", ex.getLocalizedMessage());
        }
        if (oldSize > MAX_BUFFER) { // used to bo only when scrolling
            try {
                // remove enough to get us back in our "nice" zone
                editorPane.getDocument().remove(0, oldSize - CAP_BUFFER_AMOUNT);
            } catch (BadLocationException ignored) {
                LOGGER.debug("Bad Location Exception... ignored");
            }
        }
        if (scroll) {
            editorPane.setCaretPosition(editorPane.getDocument().getLength());
        }
    }

    public void printCommLog(String s, int channel) {
        String filePath = "";

        if (channel == CHANNEL_MAIN) {
            filePath = "./logs/Main.html";
        } else if (channel == CHANNEL_HOUSE_MAIL) {
            filePath = "./logs/HouseMail.html";
        } else if (channel == CHANNEL_PRIVATE_MAIL) {
            filePath = "./logs/PrivateMail.html";
        } else if (channel == CHANNEL_PERSONAL_LOG) {
            filePath = "./logs/PersonalLog.html";
        } else if (channel == CHANNEL_SYSTEM_LOG) {
            filePath = "./logs/SystemLog.html";
        } else if (channel == CHANNEL_MISC) {
            filePath = "./logs/Misc.html";
        } else if (channel == CHANNEL_MOD) {
            filePath = "./logs/Mod.html";
        } else if (channel == CHANNEL_RPG) {
            filePath = "./logs/RPG.html";
        }

        try {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath, true);
            java.io.PrintWriter ps = new java.io.PrintWriter(fos);
            ps.print(String.format("%s<br>", s));
            ps.close();
            fos.close();
        } catch (Exception e) {
            LOGGER.error(e, "File Related Error {}", e.getLocalizedMessage());
        }
    }

    public JPanel findMailTab(String tabName) {
        JPanel panel;

        for (int pos = 0; pos < CommTPane.getTabCount(); pos++) {
            if (CommTPane.getComponent(pos) instanceof JPanel) {
                panel = (JPanel) CommTPane.getComponent(pos);
                if (panel != null && panel.getName() != null && panel.getName().equals(String.format("Mail Tab %s", tabName))) {
                    return panel;
                }
            }
        }
        return null;
    }

    public void createModTab() {
        String tabText = "Mod Mail";
        CommTPane.addTab(tabText, null, ModMailPanel, "Mod Communication Channel (Alt + O)");
        int index = CommTPane.indexOfComponent(ModMailPanel);
        int mnemo = CommTPane.getTitleAt(index).indexOf("O");

        if (mnemo == -1) {
            mnemo = CommTPane.getTitleAt(index).indexOf("o");
        }

        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        ModMailSelect = new CCommPanel.CSelectTabAction(ModMailPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt O"), "ModMailSelect");
        getActionMap().put("ModMailSelect", ModMailSelect);

    }

    public void createErrorTab() {
        String tabText = "Error Log";

        CommTPane.addTab(tabText, null, ErrorLogPanel, "Error Log Channel (Alt + R)");
        int index = CommTPane.indexOfComponent(ErrorLogPanel);
        int mnemo = CommTPane.getTitleAt(index).indexOf("R");
        if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf("r");}

        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        ErrorLogSelect = new CCommPanel.CSelectTabAction(ErrorLogPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt R"), "ErrorLogSelect");
        getActionMap().put("ErrorLogSelect", ErrorLogSelect);
    }

    public JEditorPane getEditorPane(int channel, String mailTab) {

        if (channel == CHANNEL_MAIN) {
            return MChannelEPane;
        }
        if (channel == CHANNEL_HOUSE_MAIL) {
            return HMailEPane;
        }

        if (channel == CHANNEL_PRIVATE_MAIL) {

            if (client.getConfig().isParam("USE_MULTIPLE_PM")) {

                JPanel panel = findMailTab(mailTab);
                if (panel == null) {
                    return null;
                }

                JScrollPane sPane = (JScrollPane) panel.getComponent(0);
                if (sPane == null) {
                    return null;
                }

                return (JEditorPane) sPane.getViewport().getView();
            }
            return PMailEPane;
        }

        if (channel == CHANNEL_PERSONAL_LOG) {
            return PLogEPane;
        }
        if (channel == CHANNEL_SYSTEM_LOG) {
            return SLogEPane;
        }
        if (channel == CHANNEL_MISC) {
            return MiscChannelEPane;
        }
        if (channel == CHANNEL_MOD) {
            return ModMailEPane;
        }
        if (channel == CHANNEL_ERROR) {
            return ErrorLogEPane;
        }
        if (channel == CHANNEL_RPG) {
            return RPGChannelEPane;
        }

        return null;
    }

    public JScrollPane getScrollPane(int channel, String tabName) {
        if (channel == CHANNEL_MAIN) {
            return MChannelSPane;
        } else if (channel == CHANNEL_HOUSE_MAIL) {
            return HMailSPane;
        }

        // special accommodation for PM's b/c of multi mail tabs
        else if (channel == CHANNEL_PRIVATE_MAIL) {
            if (client.getConfig().isParam("USE_MULTIPLE_PM")) {
                JPanel panel = findMailTab(tabName);
                if (panel == null) {
                    return null;
                }
                return (JScrollPane) panel.getComponent(0);
            }
            return PMailSPane;
        } else if (channel == CHANNEL_PERSONAL_LOG) {
            return PLogSPane;
        } else if (channel == CHANNEL_SYSTEM_LOG) {
            return SLogSPane;
        } else if (channel == CHANNEL_MISC) {
            return MiscChannelSPane;
        } else if (channel == CHANNEL_MOD) {
            return ModMailSPane;
        } else if (channel == CHANNEL_ERROR) {
            return ErrorLogSPane;
        } else if (channel == CHANNEL_RPG) {
            return RPGChannelSPane;
        }

        return null;
    }

    public JTextField getInputField() {
        return chatField;
    }

    public void focusInputField() {
        chatField.requestFocusInWindow(); // pass focus to input field
    }

    public int getTabIndex(JPanel panel) {
        return CommTPane.indexOfComponent(panel);
    }

    // change listener
    public void stateChanged(ChangeEvent changeEvent) {
        int index = CommTPane.getSelectedIndex();
        if (changeEvent.getSource() == CommTPane) // watch CommTPane changes
        {
            if (index == -1) {
                return;
            }

            String title = CommTPane.getTitleAt(CommTPane.getSelectedIndex()); // get
            if (title.startsWith("*")) // if selected tab is marked with "*",
            // remove mark and change color
            {
                int mnemo = CommTPane.getDisplayedMnemonicIndexAt(index);
                mnemo--;
                CommTPane.setTitleAt(index, title.substring(1));
                CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
                CommTPane.setForegroundAt(index, null);
                CommTPane.setBackgroundAt(index, null);
            }
            chatField.requestFocusInWindow(); // pass focus to input field
        }
    }

    public void componentResized(ComponentEvent componentEvent) {
        for (int i = 0; i < CommTPane.getTabCount(); i++) {

            if (getScrollPane(i) == null || getEditorPane(i) == null) {
                continue;
            }

            JScrollPane scrollPane = getScrollPane(i);
            JEditorPane editorPane = getEditorPane(i);
            JScrollBar scrollbar = scrollPane.getVerticalScrollBar();
            editorPane.setCaretPosition(editorPane.getDocument().getLength());
            scrollbar.setValue(scrollbar.getMaximum() - scrollbar.getVisibleAmount());
        }
    }

    public JScrollPane getScrollPane(int channel) {
        return getScrollPane(channel, null);
    }

    public JEditorPane getEditorPane(int channel) {
        return getEditorPane(channel, null);
    }

    public void componentMoved(ComponentEvent componentEvent) {
    }

    public void componentShown(ComponentEvent componentEvent) {
    }

    // change listener
    // component listener
    public void componentHidden(ComponentEvent componentEvent) {
    }

    /**
     * A method which selects the FIRST tab, whatever it may be.
     */
    public void selectFirstTab() {
        try {
            CommTPane.setSelectedIndex(0);
        } catch (Exception ignored) {
            LOGGER.debug("No upper level tab.. moving on.");
        }
    }

    // receiver interface
    public void mouseClicked(MouseEvent mouseEvent) {

        // only close from right clicks
        if (mouseEvent.getButton() != MouseEvent.BUTTON3) {
            return;
        }

        if (mouseEvent.getSource() instanceof JEditorPane pane) {
            JPopupMenu clipboard = new JPopupMenu();

            pane.requestFocusInWindow();
            // Information
            JMenuItem copy = new JMenuItem("Copy");

            if (pane.getSelectionStart() == pane.getSelectionEnd()) {copy.setActionCommand("");} else {
                copy.setActionCommand(pane.getSelectedText());
            }

            copy.addActionListener(actionEvent -> {
                StringSelection stringSelection = new StringSelection(actionEvent.getActionCommand());
                Clipboard clipboard1 = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard1.setContents(stringSelection, stringSelection);
            });

            clipboard.add(copy);

            clipboard.addSeparator();

            copy = new JMenuItem("Select All");
            copy.setActionCommand(pane.getName());
            copy.addActionListener(actionEvent -> {
                int index = MathUtility.parseInt(actionEvent.getActionCommand(), 0);
                JEditorPane pane1 = getEditorPane(index);
                pane1.selectAll();
            });

            clipboard.add(copy);
            clipboard.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            return;
        }

        if (mouseEvent.getSource() instanceof CChatField) {
            JPopupMenu clipboard = getClipboard();

            clipboard.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            return;
        }

        // only close from click on CommTPane
        if (!mouseEvent.getComponent().equals(CommTPane)) {
            return;
        }

        // offer to close error tab
        if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(ErrorLogPanel)) {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem info = new JMenuItem("Close");
            info.addActionListener(actionEvent -> CommTPane.remove(CommTPane.getSelectedComponent()));
            popup.add(info);
            popup.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        }

        // offer to close mail tabs
        if (client.getConfig().isParam("USE_MULTIPLE_PM") &&
                  CommTPane.getSelectedComponent() instanceof JPanel panel) {
            if (panel.getName().startsWith("Mail Tab ")) {
                JPopupMenu popup = new JPopupMenu();
                JMenuItem info = new JMenuItem("Close");
                info.addActionListener(actionEvent -> CommTPane.remove(CommTPane.getSelectedComponent()));
                popup.add(info);
                popup.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            }
        }
    }

    private @Nonnull JPopupMenu getClipboard() {
        JPopupMenu clipboard = new JPopupMenu();
        JMenuItem copy = new JMenuItem("Cut");

        copy.addActionListener(actionEvent -> {
            StringSelection stringSelection = new StringSelection(chatField.getSelectedText());
            Clipboard clipboard2 = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard2.setContents(stringSelection, stringSelection);
            try {
                String newText = chatField.getText(0, chatField.getSelectionStart());
                newText += chatField.getText(chatField.getSelectionEnd(),
                      chatField.getText().length() - chatField.getSelectionEnd());
                chatField.setText(newText);
            } catch (Exception ex) {
                LOGGER.error(ex, "Untracked Exception - getClipboard(): {}", ex.getLocalizedMessage());
            }
        });

        clipboard.add(copy);

        copy = new JMenuItem("Copy");
        copy.addActionListener(actionEvent -> {
            StringSelection stringSelection = new StringSelection(chatField.getSelectedText());
            Clipboard clipboard3 = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard3.setContents(stringSelection, stringSelection);
        });

        clipboard.add(copy);

        copy = new JMenuItem("Paste");
        copy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String clipping;
                Transferable data = clipboard.getContents(this);

                try {
                    clipping = (String) data.getTransferData(DataFlavor.stringFlavor);
                } catch (Exception ex) {
                    clipping = data.toString();
                    LOGGER.error(ex, "Data: {}", clipping);
                }

                chatField.setText(chatField.getText() + clipping);
            }
        });

        clipboard.add(copy);

        copy = new JMenuItem("Delete");
        copy.addActionListener(actionEvent -> {
            try {
                String newText = chatField.getText(0, chatField.getSelectionStart());
                newText += chatField.getText(chatField.getSelectionEnd(),
                      chatField.getText().length() - chatField.getSelectionEnd());
                chatField.setText(newText);
            } catch (Exception ex) {
                LOGGER.error(ex, "Delete event listener: {}", ex.getLocalizedMessage());
            }
        });

        clipboard.add(copy);

        clipboard.addSeparator();

        copy = new JMenuItem("Select All");
        copy.addActionListener(actionEvent -> chatField.selectAll());

        clipboard.add(copy);
        return clipboard;
    }

    public void mousePressed(MouseEvent mouseEvent) {
    }

    public void mouseReleased(MouseEvent mouseEvent) {
    }

    public void mouseEntered(MouseEvent mouseEvent) {
    }

    public void mouseExited(MouseEvent mouseEvent) {
    }

    public void reload() {
        int index;
        int mnemo;
        String tabText;

        String mnemonicText;
        tabText = client.getConfig().getParam("MAIN_CHANNEL_TAB_NAME");
        mnemonicText = client.getConfig().getParam("MAIN_CHANNEL_MNEMONIC");

        CommTPane.addTab(tabText,
              null,
              MChannelPanel,
              String.format("Interfaction Communication Channel (Alt + %s)", mnemonicText));
        index = CommTPane.indexOfComponent(MChannelPanel);
        mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

        if (mnemo == -1) {
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
        }

        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(String.format("alt %s", mnemonicText.toUpperCase())),
              "MChannelSelect");
        getActionMap().put("MChannelSelect", MChannelSelect);

        if (client.getConfig().isParam("HOUSE_MAIL_VISIBLE")) {
            tabText = client.getConfig().getParam("HOUSE_MAIL_TAB_NAME");
            mnemonicText = client.getConfig().getParam("HOUSE_MAIL_MNEMONIC");

            CommTPane.addTab(tabText,
                  null,
                  HMailPanel,
                  String.format("House Communication Channel (Alt + %s)", mnemonicText.toUpperCase()));
            index = CommTPane.indexOfComponent(HMailPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);

            if (HMailSelect == null) {
                HMailSelect = new CCommPanel.CSelectTabAction(HMailPanel);
            }

            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(String.format("alt %s", mnemonicText.toUpperCase())),
                  "HMailSelect");
            getActionMap().put("HMailSelect", HMailSelect);
        }

        if (!client.getConfig().isParam("USE_MULTIPLE_PM")) {

            if (client.getConfig().isParam("PRIVATE_MAIL_VISIBLE")) {
                tabText = client.getConfig().getParam("PRIVATE_MAIL_TAB_NAME");
                mnemonicText = client.getConfig().getParam("PRIVATE_MAIL_MNEMONIC");

                CommTPane.addTab(tabText, null, PMailPanel, String.format("Private Mail (Alt + %s)", mnemonicText.toUpperCase()));
                index = CommTPane.indexOfComponent(PMailPanel);
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

                if (mnemo == -1) {
                    mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
                }

                CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
                if (PMailSelect == null) {
                    PMailSelect = new CCommPanel.CSelectTabAction(PMailPanel);
                }

                getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(String.format("alt %s", mnemonicText.toUpperCase())),
                      "PMailSelect");
                getActionMap().put("PMailSelect", PMailSelect);

            }
        }

        if (client.getConfig().isParam("PERSONAL_LOG_VISIBLE")) {

            tabText = client.getConfig().getParam("PERSONAL_LOG_TAB_NAME");
            mnemonicText = client.getConfig().getParam("PERSONAL_LOG_MNEMONIC");

            CommTPane.addTab(tabText, null, PLogPanel, String.format("Logged Messages (Alt + %s)", mnemonicText.toUpperCase()));
            index = CommTPane.indexOfComponent(PLogPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);

            if (PLogSelect == null) {
                PLogSelect = new CCommPanel.CSelectTabAction(PLogPanel);
            }

            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(String.format("alt %s", mnemonicText.toUpperCase())),
                  String.format("PLogSelect (Alt + %s)", mnemonicText.toUpperCase()));
            getActionMap().put("PLogSelect", PLogSelect);
        }

        if (client.getConfig().isParam("SYSTEM_LOG_VISIBLE")) {
            tabText = client.getConfig().getParam("SYSTEM_LOG_TAB_NAME");
            mnemonicText = client.getConfig().getParam("SYSTEM_LOG_MNEMONIC");

            CommTPane.addTab(tabText, null, SLogPanel, String.format("System Messages (Alt + %s)", mnemonicText.toUpperCase()));
            index = CommTPane.indexOfComponent(SLogPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);

            if (SLogSelect == null) {
                SLogSelect = new CCommPanel.CSelectTabAction(SLogPanel);
            }

            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(String.format("alt %s", mnemonicText.toUpperCase())),
                  "SLogSelect");
            getActionMap().put("SLogSelect", SLogSelect);
        }

        /* Misc-Channel */
        if (client.getConfig().isParam("MISCELLANEOUS_VISIBLE")) {

            tabText = client.getConfig().getParam("MISCELLANEOUS_TAB_NAME");
            mnemonicText = client.getConfig().getParam("MISCELLANEOUS_MNEMONIC");

            CommTPane.addTab(tabText,
                  null,
                  MiscChannelPanel,
                  String.format("Miscellaneous Stuff (Alt + %s)", mnemonicText.toUpperCase()));
            index = CommTPane.indexOfComponent(MiscChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);

            if (MiscChannelSelect == null) {
                MiscChannelSelect = new CCommPanel.CSelectTabAction(MiscChannelPanel);
            }

            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(String.format("alt %s", mnemonicText.toUpperCase())),
                  "MiscChannelSelect");
            getActionMap().put("MiscChannelSelect", MiscChannelSelect);
        }
        /* RPG-Channel */
        if (client.getConfig().isParam("RPG_VISIBLE")) {

            tabText = client.getConfig().getParam("RPG_TAB_NAME");
            mnemonicText = client.getConfig().getParam("RPG_MNEMONIC");

            CommTPane.addTab(tabText, null, RPGChannelPanel, String.format("RP (Alt + %s)", mnemonicText.toUpperCase()));
            index = CommTPane.indexOfComponent(RPGChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            RPGChannelSelect = new CCommPanel.CSelectTabAction(RPGChannelPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(String.format("alt %s", mnemonicText.toUpperCase())),
                  "RPGChannelSelect");
            getActionMap().put("RPGChannelSelect", RPGChannelSelect);
        }
    }

    private void processFunctionKeyCommand(String command) {
        StringTokenizer commands = new StringTokenizer(command, ";");

        while (commands.hasMoreTokens()) {
            client.sendChat(String.format("%sc %s", IClient.CAMPAIGN_PREFIX, commands.nextToken()));
        }
    }

    // component listener actions
    private class CTabForwardAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = -7910457998205249026L;

        public CTabForwardAction() {
            // empty constructor
        }

        public void actionPerformed(ActionEvent actionEvent) {
            int count = CommTPane.getTabCount();

            if (count < 2) {
                return;
            }

            int index = CommTPane.getSelectedIndex();

            do {
                index++;
                if (index == count) {
                    index = 0;
                }
            } while (!CommTPane.isEnabledAt(index));

            CommTPane.setSelectedIndex(index);
        }
    }

    private class CTabBackwardAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = -880460003608846342L;

        public CTabBackwardAction() {
            // empty constructor
        }

        public void actionPerformed(ActionEvent actionEvent) {
            int count = CommTPane.getTabCount();

            if (count < 2) {
                return;
            }

            int index = CommTPane.getSelectedIndex();

            do {
                index--;
                if (index == -1) {
                    index = count - 1;
                }
            } while (!CommTPane.isEnabledAt(index));

            CommTPane.setSelectedIndex(index);
        }
    }

    private class CSelectTabAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = -3297024782997974093L;
        private final Component Tab;

        public CSelectTabAction(Component tab) {
            Tab = tab;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            if (CommTPane.isEnabledAt(CommTPane.indexOfComponent(Tab))) {
                CommTPane.setSelectedComponent(Tab);
            }
        }
    }

    private class CF1KeyAction extends AbstractAction {
        @Serial
        private static final long serialVersionUID = -9057337706669800548L;

        public void actionPerformed(ActionEvent actionEvent) {
            processFunctionKeyCommand(client.getConfigParam("F1BIND"));
        }
    }// end CF1Action

    private class CF2KeyAction extends AbstractAction {
        @Serial
        private static final long serialVersionUID = 2756143179187978156L;

        public void actionPerformed(ActionEvent actionEvent) {
            processFunctionKeyCommand(client.getConfigParam("F2BIND"));
        }
    }// end CF1Action

    private class CF3KeyAction extends AbstractAction {
        @Serial
        private static final long serialVersionUID = -4939591054092680536L;

        public void actionPerformed(ActionEvent actionEvent) {
            processFunctionKeyCommand(client.getConfigParam("F3BIND"));
        }
    }// end CF1Action

    private class CF4KeyAction extends AbstractAction {
        @Serial
        private static final long serialVersionUID = -6563867639119041768L;

        public void actionPerformed(ActionEvent actionEvent) {
            processFunctionKeyCommand(client.getConfigParam("F4BIND"));
        }
    }// end CF1Action

    private class CF5KeyAction extends AbstractAction {
        @Serial
        private static final long serialVersionUID = 6214466835711778622L;

        public void actionPerformed(ActionEvent actionEvent) {
            processFunctionKeyCommand(client.getConfigParam("F5BIND"));
        }
    }// end CF1Action

}
