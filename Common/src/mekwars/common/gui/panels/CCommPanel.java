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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentListener;
import java.awt.event.MouseListener;
import java.io.Serial;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeListener;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.MyHTMLEditorKit;
import mekwars.common.gui.listeners.MMNetHyperLinkListener;
import mekwars.common.util.MWLogger;
import mekwars.common.util.StringUtils;
import org.jspecify.annotations.NonNull;

/**
 * This is a tabbed multi-channel Communications Panel using Swing to manage the display.
 */
/*
 * Communications Panel
 */

public class CCommPanel extends JPanel implements ChangeListener, ComponentListener, MouseListener {

    public static final int CHANNEL_MAIN = 0;
    public static final int CHANNEL_HMAIL = 1;
    public static final int CHANNEL_PMAIL = 2;
    public static final int CHANNEL_PLOG = 3;
    public static final int CHANNEL_SLOG = 4;
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
    boolean autoTextUpdate;
    CCommPanel.CTabForwardAction ForwardCommTab;
    CCommPanel.CTabBackwardAction BackwardCommTab;

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

        TabForeground = StringUtils.html2Color(this.client.getConfigParam("SYSMESSAGECOLOR"));
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
        tabText = this.client.getConfig().getParam("MAINCHANNELTABNAME");
        mnemonicText = this.client.getConfig().getParam("MAINCHANNELMNEMONIC");

        CommTPane.addTab(tabText,
              null,
              MChannelPanel,
              STR."Interfaction Communication Channel (Alt + \{mnemonicText})");
        index = CommTPane.indexOfComponent(MChannelPanel);
        mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

        if (mnemo == -1) {
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
        }

        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        MChannelSelect = new CCommPanel.CSelectTabAction(MChannelPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(STR."alt \{mnemonicText.toUpperCase()}"),
              "MChannelSelect");
        getActionMap().put("MChannelSelect", MChannelSelect);

        HMailEPane.setEditable(false);
        HMailEPane.setCaret(new ScrollCaret());
        HMailEPane.addHyperlinkListener(chatHLL);
        HMailEPane.setEditorKit(kit);
        HMailEPane.addMouseListener(this);
        HMailEPane.setName(Integer.toString(CHANNEL_HMAIL));
        HMailSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        HMailSPane.setViewportBorder(new LineBorder(new java.awt.Color(0, 0, 0)));
        HMailSPane.setViewportView(HMailEPane);
        HMailPanel.setLayout(new BorderLayout());
        HMailPanel.add(HMailSPane, BorderLayout.CENTER);

        if (this.client.getConfig().isParam("HOUSEMAILVISIBLE")) {

            tabText = this.client.getConfig().getParam("HOUSEMAILTABNAME");
            mnemonicText = this.client.getConfig().getParam("HOUSEMAILMNEMONIC");

            CommTPane.addTab(tabText,
                  null,
                  HMailPanel,
                  STR."House Communication Channel (Alt + \{mnemonicText.toUpperCase()})");
            index = CommTPane.indexOfComponent(HMailPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            HMailSelect = new CCommPanel.CSelectTabAction(HMailPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(STR."alt \{mnemonicText.toUpperCase()}"),
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

        if (this.client.getConfig().isParam("RPGVISIBLE")) {
            tabText = this.client.getConfig().getParam("RPGTABNAME");
            mnemonicText = this.client.getConfig().getParam("RPGMNEMONIC");

            CommTPane.addTab(tabText, null, RPGChannelPanel, STR."RP (Alt + \{mnemonicText.toUpperCase()})");
            index = CommTPane.indexOfComponent(RPGChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            RPGChannelSelect = new CCommPanel.CSelectTabAction(RPGChannelPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(STR."alt \{mnemonicText.toUpperCase()}"),
                  "RPGChannelSelect");
            getActionMap().put("RPGChannelSelect", RPGChannelSelect);
        }

        if (!this.client.getConfig().isParam("USEMULTIPLEPM")) {
            PMailEPane.setEditable(false);
            PMailEPane.setCaret(new ScrollCaret());
            PMailEPane.addHyperlinkListener(chatHLL);
            PMailEPane.setEditorKit(kit);
            PMailEPane.addMouseListener(this);
            PMailEPane.setName(Integer.toString(CHANNEL_PMAIL));
            PMailSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            PMailSPane.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
            PMailSPane.setViewportView(PMailEPane);
            PMailPanel.setLayout(new BorderLayout());
            PMailPanel.add(PMailSPane, BorderLayout.CENTER);


            if (this.client.getConfig().isParam("PRIVATEMAILVISIBLE")) {
                tabText = this.client.getConfig().getParam("PRIVATEMAILTABNAME");
                mnemonicText = this.client.getConfig().getParam("PRIVATEMAILMNEMONIC");

                CommTPane.addTab(tabText, null, PMailPanel, STR."Private Mail (Alt + \{mnemonicText.toUpperCase()})");
                index = CommTPane.indexOfComponent(PMailPanel);
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

                if (mnemo == -1) {
                    mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
                }

                CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
                PMailSelect = new CCommPanel.CSelectTabAction(PMailPanel);
                getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(STR."alt \{mnemonicText.toUpperCase()}"),
                      "PMailSelect");
                getActionMap().put("PMailSelect", PMailSelect);
            }
        }

        PLogEPane.setEditable(false);
        PLogEPane.setCaret(new ScrollCaret());
        PLogEPane.addHyperlinkListener(chatHLL);
        PLogEPane.setEditorKit(kit);
        PLogEPane.addMouseListener(this);
        PLogEPane.setName(Integer.toString(CHANNEL_PLOG));
        PLogSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        PLogSPane.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
        PLogSPane.setViewportView(PLogEPane);
        PLogPanel.setLayout(new BorderLayout());
        PLogPanel.add(PLogSPane, BorderLayout.CENTER);

        if (this.client.getConfig().isParam("PERSONALLOGVISIBLE")) {
            tabText = this.client.getConfig().getParam("PERSONALLOGTABNAME");
            mnemonicText = this.client.getConfig().getParam("PERSONALLOGMNEMONIC");

            CommTPane.addTab(tabText, null, PLogPanel, STR."Logged Messages (Alt + \{mnemonicText.toUpperCase()})");
            index = CommTPane.indexOfComponent(PLogPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            PLogSelect = new CCommPanel.CSelectTabAction(PLogPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(STR."alt \{mnemonicText.toUpperCase()}"),
                  STR."PLogSelect (Alt + \{mnemonicText.toUpperCase()})");
            getActionMap().put("PLogSelect", PLogSelect);
        }

        SLogEPane.setEditable(false);
        SLogEPane.setCaret(new ScrollCaret());
        SLogEPane.addHyperlinkListener(chatHLL);
        SLogEPane.setEditorKit(kit);
        SLogEPane.addMouseListener(this);
        SLogEPane.setName(Integer.toString(CHANNEL_SLOG));
        SLogSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        SLogSPane.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
        SLogSPane.setViewportView(SLogEPane);
        SLogPanel.setLayout(new BorderLayout());
        SLogPanel.add(SLogSPane, BorderLayout.CENTER);

        if (this.client.getConfig().isParam("SYSTEMLOGVISIBLE")) {
            tabText = this.client.getConfig().getParam("SYSTEMLOGTABNAME");
            mnemonicText = this.client.getConfig().getParam("SYSTEMLOGMNEMONIC");

            CommTPane.addTab(tabText, null, SLogPanel, STR."System Messages (Alt + \{mnemonicText.toUpperCase()})");
            index = CommTPane.indexOfComponent(SLogPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            SLogSelect = new CCommPanel.CSelectTabAction(SLogPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(STR."alt \{mnemonicText.toUpperCase()}"),
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

        if (this.client.getConfig().isParam("MISCELLANEOUSVISIBLE")) {
            tabText = this.client.getConfig().getParam("MISCELLANEOUSTABNAME");
            mnemonicText = this.client.getConfig().getParam("MISCELLANEOUSMNEMONIC");

            CommTPane.addTab(tabText,
                  null,
                  MiscChannelPanel,
                  STR."Miscellaneous Stuff (Alt + \{mnemonicText.toUpperCase()})");
            index = CommTPane.indexOfComponent(MiscChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            MiscChannelSelect = new CCommPanel.CSelectTabAction(MiscChannelPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(STR."alt \{mnemonicText.toUpperCase()}"),
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
         * Add Function Keys to the maps. Putting them here is fairly hacky;
         * however, the tabbing controls (more forwward and back with x/z) are
         * already here, so one more universal listen in CommPanel shouldn't
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

    // something that the user has typed
    public boolean sendChat(String s) {
        if (!s.startsWith(IClient.GUI_PREFIX) &&
                  CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(HMailPanel)) {
            s = STR."\{IClient.GUI_PREFIX}c hm#\{s}";
        }
        if (!s.startsWith(IClient.GUI_PREFIX) &&
                  CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(ModMailPanel)) {
            s = STR."\{IClient.GUI_PREFIX}c mm#\{s}";
        }
        if (!s.startsWith(IClient.GUI_PREFIX) &&
                  CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(RPGChannelPanel)) {
            s = STR."\{IClient.GUI_PREFIX}c ic#\{s}";
        }

        if (!s.startsWith(IClient.GUI_PREFIX) &&
                  !client.getConfig().isParam("USEMULTIPLEPM") &&
                  CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(PMailPanel)) {
            String receiver = client.getLastQuery();
            if (receiver != null && !receiver.isEmpty()) {
                s = STR."\{IClient.GUI_PREFIX}mail \{receiver},\{s}";
            } else {
                client.showInfoWindow("No receiver set.");
                chatField.setText(s);
                return false;
            }
        }
        if (!s.startsWith(
              IClient.GUI_PREFIX) &&
                  client.getConfig().isParam("USEMULTIPLEPM") &&
                  CommTPane.getComponent(CommTPane.getSelectedIndex()).getName() != null &&
                  CommTPane.getComponent(CommTPane.getSelectedIndex()).getName()
                        .startsWith("Mail Tab ")) {
            JPanel panel = ((JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex()));
            String mailTab = "Mail Tab ";
            String receiver = panel.getName().substring(mailTab.length()).trim();
            if (!receiver.isEmpty()) {
                s = STR."\{IClient.GUI_PREFIX}mail \{receiver},\{s}";
            } else {
                client.showInfoWindow("No receiver set.");
                chatField.setText(s);
                return false;
            }
        }

        if (s.startsWith(STR."\{IClient.GUI_PREFIX}me") || s.startsWith(STR."\{IClient.GUI_PREFIX}c me")) {
            if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(RPGChannelPanel)) {
                s += "|ic";
            } else if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(ModMailPanel)) {
                s += "|mm";
            } else if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(HMailPanel)) {
                s += "|hm";
            } else if (!client.getConfig().isParam("USEMULTIPLEPM") &&
                             CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(PMailPanel)) {
                s += "|mail";
                String receiver = client.getLastQuery();
                if (receiver != null && !receiver.isEmpty()) {
                    s += STR."|\{receiver}";
                } else {
                    client.showInfoWindow("No receiver set.");
                    chatField.setText(s);
                    return false;
                }
            } else if (client.getConfig().isParam("USEMULTIPLEPM") &&
                             CommTPane.getComponent(CommTPane.getSelectedIndex()).getName() != null &&
                             CommTPane.getComponent(CommTPane.getSelectedIndex()).getName().startsWith("Mail Tab ")) {
                s += "|mail";
                javax.swing.JPanel panel = ((javax.swing.JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex()));
                String mailTab = "Mail Tab ";
                String receiver = panel.getName().substring(mailTab.length()).trim();
                if (!receiver.isEmpty()) {s += STR."|\{receiver}";} else {
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
            if (CommTPane.getComponent(pos) instanceof javax.swing.JPanel) {
                panel = (javax.swing.JPanel) CommTPane.getComponent(pos);
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
        String mnoemonic = Integer.toString(getNextMailTabNumber());

        newETab.setEditable(false);
        newETab.setCaret(new ScrollCaret());
        newETab.addHyperlinkListener(chatHLL);
        newETab.setEditorKit(kit);
        newSTab.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        newSTab.setViewportBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        newSTab.setViewportView(newETab);
        newPanel.setLayout(new java.awt.BorderLayout());
        newPanel.add(newSTab, java.awt.BorderLayout.CENTER);

        newPanel.setName(STR."Mail Tab \{tabName}");
        newETab.addMouseListener(this);

        CommTPane.addTab(STR."\{mnoemonic}. \{tabName}",
              null,
              newPanel,
              STR."Mail From \{tabName} (Alt + \{mnoemonic})");

        int index = CommTPane.indexOfComponent(newPanel);
        int mnemo = CommTPane.getTitleAt(index).indexOf(mnoemonic.toUpperCase());
        if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnoemonic.toLowerCase());}

        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        CCommPanel.CSelectTabAction LogSelect = new CCommPanel.CSelectTabAction(
              newPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(STR."alt \{mnoemonic.toUpperCase()}"),
              STR."Mail From \{tabName}");
        getActionMap().put(STR."Mail From \{tabName}", LogSelect);
    }

    public int getNextMailTabNumber() {
        javax.swing.JPanel panel;
        int count;
        int maxTabs = client.getConfig().getIntParam("MAXPMTABS");
        boolean found;

        for (count = 1; count <= maxTabs; count++) {
            found = false;
            for (int pos = 0; pos < CommTPane.getTabCount(); pos++) {
                if (CommTPane.getComponent(pos) instanceof javax.swing.JPanel) {
                    panel = (javax.swing.JPanel) CommTPane.getComponent(pos);
                    if (panel != null && panel.getName() != null && panel.getName().startsWith("Mail Tab ")) {
                        String tabText = CommTPane.getTitleAt(pos).trim();
                        if (tabText.startsWith("*")) {tabText = tabText.substring(1);}
                        if (Integer.parseInt(tabText.substring(0, tabText.indexOf("."))) == count) {
                            found = true;
                            break;// no reason to continue the search.
                        }
                    }
                }
            }
            if (!found) {break;}
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

    // this method needs some tuning (carret and scrollbar issues)
    public void setChat(String s, int channel, String mailTab) {

        int tabChannel = channel;
        if (channel == CHANNEL_HMAIL) {
            if (!client.getConfig().isParam("HOUSEMAILVISIBLE")) {
                printCommLog(s, channel);
                return;
            }
            // else
            tabChannel = CommTPane.indexOfComponent(HMailPanel);
        } else if (channel == CHANNEL_PMAIL) {

            if (!client.getConfig().isParam("PRIVATEMAILVISIBLE")) {
                printCommLog(s, channel);
                return;
            }

            if (client.getConfig().isParam("USEMULTIPLEPM")) {
                if (mailTab == null) {return;}
                // else
                tabChannel = CommTPane.indexOfComponent(findMailTab(mailTab));
            } else {
                tabChannel = CommTPane.indexOfComponent(PMailPanel);
            }
        } else if (channel == CHANNEL_PLOG) {
            if (!client.getConfig().isParam("PERSONALLOGVISIBLE")) {
                printCommLog(s, channel);
                return;
            }
            // else
            tabChannel = CommTPane.indexOfComponent(PLogPanel);
        } else if (channel == CHANNEL_SLOG) {
            if (!client.getConfig().isParam("SYSTEMLOGVISIBLE")) {
                printCommLog(s, channel);
                return;
            }
            // else
            tabChannel = CommTPane.indexOfComponent(SLogPanel);
        } else if (channel == CHANNEL_MISC) {
            if (!client.getConfig().isParam("MISCELLANEOUSVISIBLE")) {
                printCommLog(s, channel);
                return;
            }
            // else
            tabChannel = CommTPane.indexOfComponent(MiscChannelPanel);
        } else if (channel == CHANNEL_MOD) {
            if (CommTPane.indexOfComponent(ModMailPanel) == -1) {createModTab();}
            tabChannel = CommTPane.indexOfComponent(ModMailPanel);
        } else if (channel == CHANNEL_ERROR) {
            if (CommTPane.indexOfComponent(ErrorLogPanel) == -1) {createErrorTab();}
            tabChannel = CommTPane.indexOfComponent(ErrorLogPanel);
        } else if (channel == CHANNEL_RPG) {
            if (!client.getConfig().isParam("RPGVISIBLE")) {
                printCommLog(s, channel);
                return;
            }
            // else
            tabChannel = CommTPane.indexOfComponent(RPGChannelPanel);
        }

        javax.swing.JEditorPane editorpane = getEditorPane(channel, mailTab);
        javax.swing.JScrollBar scrollbar = null;
        if (editorpane == null) {
            return;
        }

        if (getScrollPane(channel, mailTab) != null) {
            scrollbar = getScrollPane(channel, mailTab).getVerticalScrollBar();
        }

        int oldSize = editorpane.getDocument().getLength();
        boolean scroll = true;
        if (CommTPane.getSelectedIndex() != -1 && CommTPane.getSelectedIndex() != tabChannel) {
            String title = CommTPane.getTitleAt(tabChannel);
            if (!title.startsWith("*")) {
                int mnemo = CommTPane.getDisplayedMnemonicIndexAt(tabChannel);
                mnemo++;
                CommTPane.setTitleAt(tabChannel, STR."*\{title}");
                CommTPane.setDisplayedMnemonicIndexAt(tabChannel, mnemo);
                if (CommTPane.getComponentAt(tabChannel) != PLogPanel &&
                          CommTPane.getComponentAt(tabChannel) != SLogPanel) {
                    CommTPane.setForegroundAt(tabChannel, TabForeground); // invert
                    // tab
                    // text
                    // CommTPane.setBackgroundAt(tabChannel, TabBackground);
                }
            }
        }
        // if scrollbar was moved up, add text, but keep scrollbar as it is,
        // otherwise scrollbar is almost at the bottom, add text in normal way
        if (scrollbar != null &&
                  !autoTextUpdate &&
                  (scrollbar.getValue() + scrollbar.getVisibleAmount()) < scrollbar.getMaximum() * 0.97) {
            scroll = false;
            ((ScrollCaret) editorpane.getCaret()).showCaret = false;
        } else {((ScrollCaret) editorpane.getCaret()).showCaret = true;}

        try {
            if (s.endsWith("<br>")) {
                s = s.substring(0, s.length() - 4);
            }
            editorpane.getEditorKit().read(new java.io.StringReader(s),
                  editorpane.getDocument(),
                  editorpane.getDocument().getLength());
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
        if (oldSize > MAX_BUFFER) { // used to bo only when scrolling
            try {
                // remove enough to get us back in our "nice" zone
                editorpane.getDocument().remove(0, oldSize - CAP_BUFFER_AMOUNT);
            } catch (javax.swing.text.BadLocationException ex) {/* Ignore */
            }
        }
        if (scroll) {editorpane.setCaretPosition(editorpane.getDocument().getLength());}
    }

    public void printCommLog(String s, int channel) {
        String filePath = "";

        if (channel == CHANNEL_MAIN) {
            filePath = "./logs/Main.html";
        } else if (channel == CHANNEL_HMAIL) {
            filePath = "./logs/HouseMail.html";
        } else if (channel == CHANNEL_PMAIL) {
            filePath = "./logs/PrivateMail.html";
        } else if (channel == CHANNEL_PLOG) {
            filePath = "./logs/PersonalLog.html";
        } else if (channel == CHANNEL_SLOG) {
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
            ps.print(STR."\{s}<br>");
            ps.close();
            fos.close();
        } catch (Exception e) {
            MWLogger.errLog(e);
        }
    }

    public javax.swing.JPanel findMailTab(String tabName) {
        javax.swing.JPanel panel;

        for (int pos = 0; pos < CommTPane.getTabCount(); pos++) {
            if (CommTPane.getComponent(pos) instanceof javax.swing.JPanel) {
                panel = (javax.swing.JPanel) CommTPane.getComponent(pos);
                if (panel != null && panel.getName() != null && panel.getName().equals(STR."Mail Tab \{tabName}")) {
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
        if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf("o");}

        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        ModMailSelect = new CCommPanel.CSelectTabAction(ModMailPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt O"), "ModMailSelect");
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
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt R"), "ErrorLogSelect");
        getActionMap().put("ErrorLogSelect", ErrorLogSelect);
    }

    public javax.swing.JEditorPane getEditorPane(int channel, String mailTab) {

        if (channel == CHANNEL_MAIN) {
            return MChannelEPane;
        }
        if (channel == CHANNEL_HMAIL) {
            return HMailEPane;
        }

        if (channel == CHANNEL_PMAIL) {

            if (client.getConfig().isParam("USEMULTIPLEPM")) {

                javax.swing.JPanel panel = findMailTab(mailTab);
                if (panel == null) {return null;}

                javax.swing.JScrollPane sPane = (javax.swing.JScrollPane) panel.getComponent(0);
                if (sPane == null) {return null;}

                return (javax.swing.JEditorPane) sPane.getViewport().getView();
            }
            return PMailEPane;
        }

        if (channel == CHANNEL_PLOG) {
            return PLogEPane;
        }
        if (channel == CHANNEL_SLOG) {
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

    public javax.swing.JScrollPane getScrollPane(int channel, String tabName) {
        if (channel == CHANNEL_MAIN) {
            return MChannelSPane;
        } else if (channel == CHANNEL_HMAIL) {
            return HMailSPane;
        }

        // special accomidation for PM's b/c of multimail tabs
        else if (channel == CHANNEL_PMAIL) {
            if (client.getConfig().isParam("USEMULTIPLEPM")) {
                javax.swing.JPanel panel = findMailTab(tabName);
                if (panel == null) {return null;}
                return (javax.swing.JScrollPane) panel.getComponent(0);
            }
            return PMailSPane;
        } else if (channel == CHANNEL_PLOG) {
            return PLogSPane;
        } else if (channel == CHANNEL_SLOG) {
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

    public javax.swing.JTextField getInputField() {
        return chatField;
    }

    public void focusInputField() {
        chatField.requestFocusInWindow(); // pass focus to input field
    }

    public int getTabIndex(javax.swing.JPanel panel) {
        return CommTPane.indexOfComponent(panel);
    }

    // change listener
    public void stateChanged(javax.swing.event.ChangeEvent e) {
        // JScrollBar scrollbar = MChannelSPane.getVerticalScrollBar();
        int index = CommTPane.getSelectedIndex();
        if (e.getSource() == CommTPane) // watch CommTPane changes
        {
            if (index == -1) {
                return;
            }
            String title = CommTPane.getTitleAt(CommTPane.getSelectedIndex()); // get
            // selected
            // tab
            // title
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

    public void componentResized(java.awt.event.ComponentEvent e) {
        for (int i = 0; i < CommTPane.getTabCount(); i++) {

            if (getScrollPane(i) == null || getEditorPane(i) == null) {continue;}

            javax.swing.JScrollPane scrollpane = getScrollPane(i);
            javax.swing.JEditorPane editorpane = getEditorPane(i);
            javax.swing.JScrollBar scrollbar = scrollpane.getVerticalScrollBar();
            editorpane.setCaretPosition(editorpane.getDocument().getLength()); // we
            // document
            scrollbar.setValue(scrollbar.getMaximum() - scrollbar.getVisibleAmount()); // and
            // scrollbar
            // at
            // bottom
        }
    }

    public javax.swing.JScrollPane getScrollPane(int channel) {
        return getScrollPane(channel, null);
    }

    public javax.swing.JEditorPane getEditorPane(int channel) {
        return getEditorPane(channel, null);
    }

    public void componentMoved(java.awt.event.ComponentEvent e) {
    }

    public void componentShown(java.awt.event.ComponentEvent e) {
    }

    // change listener
    // component listener
    public void componentHidden(java.awt.event.ComponentEvent e) {
    }

    /**
     * A method which selects the FIRST tab, whatever it may be.
     */
    public void selectFirstTab() {
        try {
            CommTPane.setSelectedIndex(0);
        } catch (Exception e) {
            // do nothing. just means no upper-level tabs.
        }
    }

    // receiver interface
    public void mouseClicked(java.awt.event.MouseEvent e) {

        // only close from right clicks
        if (e.getButton() != java.awt.event.MouseEvent.BUTTON3) {
            return;
        }

        if (e.getSource() instanceof JEditorPane pane) {
            javax.swing.JPopupMenu clipboard = new javax.swing.JPopupMenu();

            pane.requestFocusInWindow();
            // Information
            javax.swing.JMenuItem copy = new javax.swing.JMenuItem("Copy");
            if (pane.getSelectionStart() == pane.getSelectionEnd()) {copy.setActionCommand("");} else {
                copy.setActionCommand(pane.getSelectedText());
            }
            copy.addActionListener(ae -> {
                java.awt.datatransfer.StringSelection ss = new java.awt.datatransfer.StringSelection(ae.getActionCommand());
                java.awt.datatransfer.Clipboard clipboard1 = java.awt.Toolkit.getDefaultToolkit()
                                                                   .getSystemClipboard();
                clipboard1.setContents(ss, ss);
            });

            clipboard.add(copy);

            clipboard.addSeparator();

            copy = new javax.swing.JMenuItem("Select All");
            copy.setActionCommand(pane.getName());
            copy.addActionListener(ae -> {
                try {
                    int index = Integer.parseInt(ae.getActionCommand());
                    JEditorPane pane1 = getEditorPane(index);
                    pane1.selectAll();
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
            });

            clipboard.add(copy);
            clipboard.show(e.getComponent(), e.getX(), e.getY());
            return;
        }

        if (e.getSource() instanceof CChatField) {
            JPopupMenu clipboard = getClipboard();

            clipboard.show(e.getComponent(), e.getX(), e.getY());
            return;
        }

        // only close from click on CommTPane
        if (!e.getComponent().equals(CommTPane)) {
            return;
        }

        // offer to close error tab
        if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(ErrorLogPanel)) {
            javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
            javax.swing.JMenuItem info = new javax.swing.JMenuItem("Close");
            info.addActionListener(_ -> CommTPane.remove(CommTPane.getSelectedComponent()));
            popup.add(info);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }

        // offer to close mail tabs
        if (client.getConfig().isParam("USEMULTIPLEPM") &&
                  CommTPane.getSelectedComponent() instanceof JPanel panel) {
            if (panel.getName().startsWith("Mail Tab ")) {
                javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
                javax.swing.JMenuItem info = new javax.swing.JMenuItem("Close");
                info.addActionListener(_ -> CommTPane.remove(CommTPane.getSelectedComponent()));
                popup.add(info);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private @NonNull JPopupMenu getClipboard() {
        JPopupMenu clipboard = new JPopupMenu();
        javax.swing.JMenuItem copy = new javax.swing.JMenuItem("Cut");

        copy.addActionListener(_ -> {
            java.awt.datatransfer.StringSelection ss = new java.awt.datatransfer.StringSelection(chatField.getSelectedText());
            java.awt.datatransfer.Clipboard clipboard2 = java.awt.Toolkit.getDefaultToolkit()
                                                               .getSystemClipboard();
            clipboard2.setContents(ss, ss);
            try {
                String newText = chatField.getText(0, chatField.getSelectionStart());
                newText += chatField.getText(chatField.getSelectionEnd(),
                      chatField.getText().length() - chatField.getSelectionEnd());
                chatField.setText(newText);
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
        });

        clipboard.add(copy);

        copy = new javax.swing.JMenuItem("Copy");
        copy.addActionListener(_ -> {
            java.awt.datatransfer.StringSelection ss = new java.awt.datatransfer.StringSelection(chatField.getSelectedText());
            java.awt.datatransfer.Clipboard clipboard3 = java.awt.Toolkit.getDefaultToolkit()
                                                               .getSystemClipboard();
            clipboard3.setContents(ss, ss);
        });

        clipboard.add(copy);

        copy = new javax.swing.JMenuItem("Paste");
        copy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent ae) {
                java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit()
                                                                  .getSystemClipboard();
                String clipping;
                java.awt.datatransfer.Transferable data = clipboard.getContents(this);

                try {
                    clipping = (String) data.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
                } catch (Exception ex) {
                    clipping = data.toString();
                    MWLogger.errLog(ex);
                }
                chatField.setText(chatField.getText() + clipping);
            }
        });

        clipboard.add(copy);

        copy = new javax.swing.JMenuItem("Delete");
        copy.addActionListener(_ -> {
            try {
                String newText = chatField.getText(0, chatField.getSelectionStart());
                newText += chatField.getText(chatField.getSelectionEnd(),
                      chatField.getText().length() - chatField.getSelectionEnd());
                chatField.setText(newText);
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
        });

        clipboard.add(copy);

        clipboard.addSeparator();

        copy = new javax.swing.JMenuItem("Select All");
        copy.addActionListener(_ -> {
            try {
                chatField.selectAll();
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
        });

        clipboard.add(copy);
        return clipboard;
    }

    public void mousePressed(java.awt.event.MouseEvent arg0) {
    }

    public void mouseReleased(java.awt.event.MouseEvent arg0) {
    }

    public void mouseEntered(java.awt.event.MouseEvent arg0) {
    }

    public void mouseExited(java.awt.event.MouseEvent arg0) {
    }

    public void reload() {

        int index;
        int mnemo;
        String tabText;

        String mnemonicText;
        tabText = client.getConfig().getParam("MAINCHANNELTABNAME");
        mnemonicText = client.getConfig().getParam("MAINCHANNELMNEMONIC");

        CommTPane.addTab(tabText,
              null,
              MChannelPanel,
              STR."Interfaction Communication Channel (Alt + \{mnemonicText})");
        index = CommTPane.indexOfComponent(MChannelPanel);
        mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
        if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(STR."alt \{mnemonicText.toUpperCase()}"),
              "MChannelSelect");
        getActionMap().put("MChannelSelect", MChannelSelect);

        if (client.getConfig().isParam("HOUSEMAILVISIBLE")) {

            tabText = client.getConfig().getParam("HOUSEMAILTABNAME");
            mnemonicText = client.getConfig().getParam("HOUSEMAILMNEMONIC");

            CommTPane.addTab(tabText,
                  null,
                  HMailPanel,
                  STR."House Communication Channel (Alt + \{mnemonicText.toUpperCase()})");
            index = CommTPane.indexOfComponent(HMailPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            if (HMailSelect == null) {HMailSelect = new CCommPanel.CSelectTabAction(HMailPanel);}
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(STR."alt \{mnemonicText.toUpperCase()}"),
                  "HMailSelect");
            getActionMap().put("HMailSelect", HMailSelect);
        }

        if (!client.getConfig().isParam("USEMULTIPLEPM")) {

            if (client.getConfig().isParam("PRIVATEMAILVISIBLE")) {
                tabText = client.getConfig().getParam("PRIVATEMAILTABNAME");
                mnemonicText = client.getConfig().getParam("PRIVATEMAILMNEMONIC");

                CommTPane.addTab(tabText, null, PMailPanel, STR."Private Mail (Alt + \{mnemonicText.toUpperCase()})");
                index = CommTPane.indexOfComponent(PMailPanel);
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
                if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
                CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
                if (PMailSelect == null) {PMailSelect = new CCommPanel.CSelectTabAction(PMailPanel);}
                getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(STR."alt \{mnemonicText.toUpperCase()}"),
                      "PMailSelect");
                getActionMap().put("PMailSelect", PMailSelect);

            }
        }

        if (client.getConfig().isParam("PERSONALLOGVISIBLE")) {

            tabText = client.getConfig().getParam("PERSONALLOGTABNAME");
            mnemonicText = client.getConfig().getParam("PERSONALLOGMNEMONIC");

            CommTPane.addTab(tabText, null, PLogPanel, STR."Logged Messages (Alt + \{mnemonicText.toUpperCase()})");
            index = CommTPane.indexOfComponent(PLogPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            if (PLogSelect == null) {PLogSelect = new CCommPanel.CSelectTabAction(PLogPanel);}
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(STR."alt \{mnemonicText.toUpperCase()}"),
                  STR."PLogSelect (Alt + \{mnemonicText.toUpperCase()})");
            getActionMap().put("PLogSelect", PLogSelect);
        }

        if (client.getConfig().isParam("SYSTEMLOGVISIBLE")) {

            tabText = client.getConfig().getParam("SYSTEMLOGTABNAME");
            mnemonicText = client.getConfig().getParam("SYSTEMLOGMNEMONIC");

            CommTPane.addTab(tabText, null, SLogPanel, STR."System Messages (Alt + \{mnemonicText.toUpperCase()})");
            index = CommTPane.indexOfComponent(SLogPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            if (SLogSelect == null) {SLogSelect = new CCommPanel.CSelectTabAction(SLogPanel);}
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(STR."alt \{mnemonicText.toUpperCase()}"),
                  "SLogSelect");
            getActionMap().put("SLogSelect", SLogSelect);
        }

        /* Misc-Channel */
        if (client.getConfig().isParam("MISCELLANEOUSVISIBLE")) {

            tabText = client.getConfig().getParam("MISCELLANEOUSTABNAME");
            mnemonicText = client.getConfig().getParam("MISCELLANEOUSMNEMONIC");

            CommTPane.addTab(tabText,
                  null,
                  MiscChannelPanel,
                  STR."Miscellaneous Stuff (Alt + \{mnemonicText.toUpperCase()})");
            index = CommTPane.indexOfComponent(MiscChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            if (MiscChannelSelect == null) {
                MiscChannelSelect = new CCommPanel.CSelectTabAction(MiscChannelPanel);
            }
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(STR."alt \{mnemonicText.toUpperCase()}"),
                  "MiscChannelSelect");
            getActionMap().put("MiscChannelSelect", MiscChannelSelect);
        }
        /* RPG-Channel */
        if (client.getConfig().isParam("RPGVISIBLE")) {

            tabText = client.getConfig().getParam("RPGTABNAME");
            mnemonicText = client.getConfig().getParam("RPGMNEMONIC");

            CommTPane.addTab(tabText, null, RPGChannelPanel, STR."RP (Alt + \{mnemonicText.toUpperCase()})");
            index = CommTPane.indexOfComponent(RPGChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            RPGChannelSelect = new CCommPanel.CSelectTabAction(RPGChannelPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(STR."alt \{mnemonicText.toUpperCase()}"),
                  "RPGChannelSelect");
            getActionMap().put("RPGChannelSelect", RPGChannelSelect);
        }

    }

    private void processFunctionKeyCommand(String command) {
        java.util.StringTokenizer commands = new java.util.StringTokenizer(command, ";");

        while (commands.hasMoreTokens()) {
            client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c \{commands.nextToken()}");
        }
    }

    // component listener actions
    private class CTabForwardAction extends javax.swing.AbstractAction {

        @Serial
        private static final long serialVersionUID = -7910457998205249026L;

        public CTabForwardAction() {
            // empty constructor
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
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

    private class CTabBackwardAction extends javax.swing.AbstractAction {

        @Serial
        private static final long serialVersionUID = -880460003608846342L;

        public CTabBackwardAction() {
            // empty constructor
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
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

    private class CSelectTabAction extends javax.swing.AbstractAction {

        @Serial
        private static final long serialVersionUID = -3297024782997974093L;
        private final java.awt.Component Tab;

        public CSelectTabAction(java.awt.Component tab) {
            Tab = tab;
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (CommTPane.isEnabledAt(CommTPane.indexOfComponent(Tab))) {CommTPane.setSelectedComponent(Tab);}
        }
    }

    private class CF1KeyAction extends javax.swing.AbstractAction {
        @Serial
        private static final long serialVersionUID = -9057337706669800548L;

        public void actionPerformed(java.awt.event.ActionEvent e) {
            processFunctionKeyCommand(client.getConfigParam("F1BIND"));
        }
    }// end CF1Action

    private class CF2KeyAction extends javax.swing.AbstractAction {
        @Serial
        private static final long serialVersionUID = 2756143179187978156L;

        public void actionPerformed(java.awt.event.ActionEvent e) {
            processFunctionKeyCommand(client.getConfigParam("F2BIND"));
        }
    }// end CF1Action

    private class CF3KeyAction extends javax.swing.AbstractAction {
        @Serial
        private static final long serialVersionUID = -4939591054092680536L;

        public void actionPerformed(java.awt.event.ActionEvent e) {
            processFunctionKeyCommand(client.getConfigParam("F3BIND"));
        }
    }// end CF1Action

    private class CF4KeyAction extends javax.swing.AbstractAction {
        @Serial
        private static final long serialVersionUID = -6563867639119041768L;

        public void actionPerformed(java.awt.event.ActionEvent e) {
            processFunctionKeyCommand(client.getConfigParam("F4BIND"));
        }
    }// end CF1Action

    private class CF5KeyAction extends javax.swing.AbstractAction {
        @Serial
        private static final long serialVersionUID = 6214466835711778622L;

        public void actionPerformed(java.awt.event.ActionEvent e) {
            processFunctionKeyCommand(client.getConfigParam("F5BIND"));
        }
    }// end CF1Action

}
