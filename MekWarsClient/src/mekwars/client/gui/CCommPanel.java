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

package mekwars.client.gui;

import java.awt.Rectangle;
import javax.swing.text.DefaultCaret;

import common.util.MWLogger;
import common.util.StringUtils;

/**
 * This is a tabbed multi-channet Communications Panel using Swing to manage the display.
 */
/*
 * Communications Panel
 */

public class CCommPanel extends javax.swing.JPanel
      implements javax.swing.event.ChangeListener, java.awt.event.ComponentListener, java.awt.event.MouseListener {

    /**
     *
     */
    private static final long serialVersionUID = 8754254920729491806L;

    /**
     * Caret that does not auto-scroll if nothing is selected.
     */
    static class ScrollCaret extends javax.swing.text.DefaultCaret {
        /**
         *
         */
        private static final long serialVersionUID = 7038682935535985621L;
        public boolean showCaret = true;

        /**
         * @see DefaultCaret#adjustVisibility(Rectangle)
         */
        @Override
        protected void adjustVisibility(java.awt.Rectangle nloc) {
            if (showCaret) {super.adjustVisibility(nloc);}
        }
    }

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
     * Maximum buffer of each channel. Each channel will be capped to have no more than this amount of characters.
     */
    public static final int MAXBUFFER = 100000;
    /**
     * Amount of characters removed when buffer overflow. If a channels maximum character count is reached, cap that
     * many characters.
     *
     * @see CCommPanel#MAXBUFFER
     */
    public static final int CAPBUFFERAMOUNT = 60000;

    client.MWClient mwclient;

    javax.swing.JTabbedPane CommTPane = new javax.swing.JTabbedPane(javax.swing.SwingConstants.BOTTOM);
    MyHTMLEditorKit kit = new MyHTMLEditorKit();
    javax.swing.JPanel MChannelPanel = new javax.swing.JPanel();
    mekwars.client.gui.CCommPanel.CSelectTabAction MChannelSelect = null;
    javax.swing.JEditorPane MChannelEPane = new javax.swing.JEditorPane("text/html", "");
    javax.swing.JScrollPane MChannelSPane = new javax.swing.JScrollPane();
    javax.swing.JPanel HMailPanel = new javax.swing.JPanel();
    mekwars.client.gui.CCommPanel.CSelectTabAction HMailSelect = null;
    javax.swing.JEditorPane HMailEPane = new javax.swing.JEditorPane("text/html", "");
    javax.swing.JScrollPane HMailSPane = new javax.swing.JScrollPane();
    javax.swing.JPanel PMailPanel = new javax.swing.JPanel();
    mekwars.client.gui.CCommPanel.CSelectTabAction PMailSelect = null;
    javax.swing.JEditorPane PMailEPane = new javax.swing.JEditorPane("text/html", "");
    javax.swing.JScrollPane PMailSPane = new javax.swing.JScrollPane();
    javax.swing.JPanel PLogPanel = new javax.swing.JPanel();
    mekwars.client.gui.CCommPanel.CSelectTabAction PLogSelect = null;
    javax.swing.JEditorPane PLogEPane = new javax.swing.JEditorPane("text/html", "");
    javax.swing.JScrollPane PLogSPane = new javax.swing.JScrollPane();
    javax.swing.JPanel SLogPanel = new javax.swing.JPanel();
    mekwars.client.gui.CCommPanel.CSelectTabAction SLogSelect = null;
    javax.swing.JEditorPane SLogEPane = new javax.swing.JEditorPane("text/html", "");
    javax.swing.JScrollPane SLogSPane = new javax.swing.JScrollPane();
    javax.swing.JPanel MiscChannelPanel = new javax.swing.JPanel();
    mekwars.client.gui.CCommPanel.CSelectTabAction MiscChannelSelect = null;
    javax.swing.JEditorPane MiscChannelEPane = new javax.swing.JEditorPane("text/html", "");
    javax.swing.JScrollPane MiscChannelSPane = new javax.swing.JScrollPane();
    javax.swing.JPanel ModMailPanel = new javax.swing.JPanel();
    mekwars.client.gui.CCommPanel.CSelectTabAction ModMailSelect = null;
    javax.swing.JEditorPane ModMailEPane = new javax.swing.JEditorPane("text/html", "");
    javax.swing.JScrollPane ModMailSPane = new javax.swing.JScrollPane();
    javax.swing.JPanel ErrorLogPanel = new javax.swing.JPanel();
    mekwars.client.gui.CCommPanel.CSelectTabAction ErrorLogSelect = null;
    javax.swing.JEditorPane ErrorLogEPane = new javax.swing.JEditorPane("text/html", "");
    javax.swing.JScrollPane ErrorLogSPane = new javax.swing.JScrollPane();
    javax.swing.JPanel RPGChannelPanel = new javax.swing.JPanel();
    mekwars.client.gui.CCommPanel.CSelectTabAction RPGChannelSelect = null;
    javax.swing.JEditorPane RPGChannelEPane = new javax.swing.JEditorPane("text/html", "");
    javax.swing.JScrollPane RPGChannelSPane = new javax.swing.JScrollPane();
    mekwars.client.gui.CCommPanel.CChatField chatField;
    java.awt.Color TabForeground;
    java.awt.Color TabBackground;
    boolean autoTextUpdate;
    mekwars.client.gui.CCommPanel.CTabForwardAction ForwardCommTab;
    mekwars.client.gui.CCommPanel.CTabBackwardAction BackwardCommTab;

    public CCommPanel(client.MWClient client) {
        int index;
        int mnemo;
        String tabText = "";
        mwclient = client;

        /*
         * Instead of making 6 HyperLinkListeners, make one here and use it for
         * all tabs.
         *
         * @urgru 7.17.05
         */
        MMNetHyperLinkListener chatHLL = new MMNetHyperLinkListener(mwclient);

        CommTPane.addMouseListener(this);

        TabForeground = StringUtils.html2Color(mwclient.getConfigParam("SYSMESSAGECOLOR"));
        TabBackground = new javax.swing.JList().getSelectionBackground().darker();
        autoTextUpdate = mwclient.getConfig().isParam("AUTOSCROLL");
        setLayout(new java.awt.BorderLayout());
        addComponentListener(this);
        MChannelEPane.setEditable(false);
        MChannelEPane.setCaret(new mekwars.client.gui.CCommPanel.ScrollCaret());
        MChannelEPane.addHyperlinkListener(chatHLL);
        MChannelEPane.setEditorKit(kit);
        MChannelEPane.addMouseListener(this);
        MChannelEPane.setName(Integer.toString(CHANNEL_MAIN));
        MChannelSPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        MChannelSPane.setViewportView(MChannelEPane);
        MChannelPanel.setLayout(new java.awt.BorderLayout());
        MChannelPanel.add(MChannelSPane, java.awt.BorderLayout.CENTER);
        String mnemonicText;
        tabText = mwclient.getConfig().getParam("MAINCHANNELTABNAME");
        mnemonicText = mwclient.getConfig().getParam("MAINCHANNELMNEMONIC");

        CommTPane.addTab(tabText,
              null,
              MChannelPanel,
              "Interfaction Communication Channel (Alt + " + mnemonicText + ")");
        index = CommTPane.indexOfComponent(MChannelPanel);
        mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
        if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        MChannelSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(MChannelPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()),
              "MChannelSelect");
        getActionMap().put("MChannelSelect", MChannelSelect);

        HMailEPane.setEditable(false);
        HMailEPane.setCaret(new mekwars.client.gui.CCommPanel.ScrollCaret());
        HMailEPane.addHyperlinkListener(chatHLL);
        HMailEPane.setEditorKit(kit);
        HMailEPane.addMouseListener(this);
        HMailEPane.setName(Integer.toString(CHANNEL_HMAIL));
        HMailSPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        HMailSPane.setViewportBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        HMailSPane.setViewportView(HMailEPane);
        HMailPanel.setLayout(new java.awt.BorderLayout());
        HMailPanel.add(HMailSPane, java.awt.BorderLayout.CENTER);

        if (mwclient.getConfig().isParam("HOUSEMAILVISIBLE")) {

            tabText = mwclient.getConfig().getParam("HOUSEMAILTABNAME");
            mnemonicText = mwclient.getConfig().getParam("HOUSEMAILMNEMONIC");

            CommTPane.addTab(tabText,
                  null,
                  HMailPanel,
                  "House Communication Channel (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(HMailPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            HMailSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(HMailPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " +
                                                                                             mnemonicText.toUpperCase()),
                  "HMailSelect");
            getActionMap().put("HMailSelect", HMailSelect);
        }

        /* RPG-Channel */
        RPGChannelEPane.setEditable(false);
        RPGChannelEPane.setCaret(new mekwars.client.gui.CCommPanel.ScrollCaret());
        RPGChannelEPane.addHyperlinkListener(chatHLL);
        RPGChannelEPane.setEditorKit(kit);
        RPGChannelEPane.addMouseListener(this);
        RPGChannelEPane.setName(Integer.toString(CHANNEL_RPG));

        RPGChannelSPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        RPGChannelSPane.setViewportBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        RPGChannelSPane.setViewportView(RPGChannelEPane);
        RPGChannelPanel.setLayout(new java.awt.BorderLayout());
        RPGChannelPanel.add(RPGChannelSPane, java.awt.BorderLayout.CENTER);

        RPGChannelEPane.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent e) {
                /*
                 * if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                 * removeHttpLinksFromEditorPane(CHANNEL_MISC);
                 */
            }
        });

        if (mwclient.getConfig().isParam("RPGVISIBLE")) {

            tabText = mwclient.getConfig().getParam("RPGTABNAME");
            mnemonicText = mwclient.getConfig().getParam("RPGMNEMONIC");

            CommTPane.addTab(tabText, null, RPGChannelPanel, "RP (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(RPGChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            RPGChannelSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(RPGChannelPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " +
                                                                                             mnemonicText.toUpperCase()),
                  "RPGChannelSelect");
            getActionMap().put("RPGChannelSelect", RPGChannelSelect);
        }

        if (!mwclient.getConfig().isParam("USEMULTIPLEPM")) {
            PMailEPane.setEditable(false);
            PMailEPane.setCaret(new mekwars.client.gui.CCommPanel.ScrollCaret());
            PMailEPane.addHyperlinkListener(chatHLL);
            PMailEPane.setEditorKit(kit);
            PMailEPane.addMouseListener(this);
            PMailEPane.setName(Integer.toString(CHANNEL_PMAIL));
            PMailSPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            PMailSPane.setViewportBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
            PMailSPane.setViewportView(PMailEPane);
            PMailPanel.setLayout(new java.awt.BorderLayout());
            PMailPanel.add(PMailSPane, java.awt.BorderLayout.CENTER);


            if (mwclient.getConfig().isParam("PRIVATEMAILVISIBLE")) {
                tabText = mwclient.getConfig().getParam("PRIVATEMAILTABNAME");
                mnemonicText = mwclient.getConfig().getParam("PRIVATEMAILMNEMONIC");

                CommTPane.addTab(tabText, null, PMailPanel, "Private Mail (Alt + " + mnemonicText.toUpperCase() + ")");
                index = CommTPane.indexOfComponent(PMailPanel);
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
                if (mnemo == -1) {
                    mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
                }
                CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
                PMailSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(PMailPanel);
                getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " +
                                                                                                 mnemonicText.toUpperCase()),
                      "PMailSelect");
                getActionMap().put("PMailSelect", PMailSelect);

            }
        }

        PLogEPane.setEditable(false);
        PLogEPane.setCaret(new mekwars.client.gui.CCommPanel.ScrollCaret());
        PLogEPane.addHyperlinkListener(chatHLL);
        PLogEPane.setEditorKit(kit);
        PLogEPane.addMouseListener(this);
        PLogEPane.setName(Integer.toString(CHANNEL_PLOG));
        PLogSPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        PLogSPane.setViewportBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        PLogSPane.setViewportView(PLogEPane);
        PLogPanel.setLayout(new java.awt.BorderLayout());
        PLogPanel.add(PLogSPane, java.awt.BorderLayout.CENTER);


        if (mwclient.getConfig().isParam("PERSONALLOGVISIBLE")) {

            tabText = mwclient.getConfig().getParam("PERSONALLOGTABNAME");
            mnemonicText = mwclient.getConfig().getParam("PERSONALLOGMNEMONIC");

            CommTPane.addTab(tabText, null, PLogPanel, "Logged Messages (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(PLogPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            PLogSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(PLogPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " +
                                                                                             mnemonicText.toUpperCase()),
                  "PLogSelect (Alt + " + mnemonicText.toUpperCase() + ")");
            getActionMap().put("PLogSelect", PLogSelect);
        }
        SLogEPane.setEditable(false);
        SLogEPane.setCaret(new mekwars.client.gui.CCommPanel.ScrollCaret());
        SLogEPane.addHyperlinkListener(chatHLL);
        SLogEPane.setEditorKit(kit);
        SLogEPane.addMouseListener(this);
        SLogEPane.setName(Integer.toString(CHANNEL_SLOG));
        SLogSPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        SLogSPane.setViewportBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        SLogSPane.setViewportView(SLogEPane);
        SLogPanel.setLayout(new java.awt.BorderLayout());
        SLogPanel.add(SLogSPane, java.awt.BorderLayout.CENTER);

        if (mwclient.getConfig().isParam("SYSTEMLOGVISIBLE")) {

            tabText = mwclient.getConfig().getParam("SYSTEMLOGTABNAME");
            mnemonicText = mwclient.getConfig().getParam("SYSTEMLOGMNEMONIC");

            CommTPane.addTab(tabText, null, SLogPanel, "System Messages (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(SLogPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            SLogSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(SLogPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " +
                                                                                             mnemonicText.toUpperCase()),
                  "SLogSelect");
            getActionMap().put("SLogSelect", SLogSelect);
        }

        /* Misc-Channel */
        MiscChannelEPane.setEditable(false);
        MiscChannelEPane.setCaret(new mekwars.client.gui.CCommPanel.ScrollCaret());
        MiscChannelEPane.addHyperlinkListener(chatHLL);
        MiscChannelEPane.setEditorKit(kit);
        MiscChannelEPane.addMouseListener(this);
        MiscChannelEPane.setName(Integer.toString(CHANNEL_MISC));
        MiscChannelSPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        MiscChannelSPane.setViewportBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        MiscChannelSPane.setViewportView(MiscChannelEPane);
        MiscChannelPanel.setLayout(new java.awt.BorderLayout());
        MiscChannelPanel.add(MiscChannelSPane, java.awt.BorderLayout.CENTER);

        if (mwclient.getConfig().isParam("MISCELLANEOUSVISIBLE")) {

            tabText = mwclient.getConfig().getParam("MISCELLANEOUSTABNAME");
            mnemonicText = mwclient.getConfig().getParam("MISCELLANEOUSMNEMONIC");

            CommTPane.addTab(tabText,
                  null,
                  MiscChannelPanel,
                  "Miscellaneous Stuff (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(MiscChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            MiscChannelSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(MiscChannelPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " +
                                                                                             mnemonicText.toUpperCase()),
                  "MiscChannelSelect");
            getActionMap().put("MiscChannelSelect", MiscChannelSelect);
        }

        ModMailEPane.setEditable(false);
        ModMailEPane.setCaret(new mekwars.client.gui.CCommPanel.ScrollCaret());
        ModMailEPane.addHyperlinkListener(chatHLL);
        ModMailEPane.setEditorKit(kit);
        ModMailEPane.addMouseListener(this);
        ModMailEPane.setName(Integer.toString(CHANNEL_MOD));
        ModMailSPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ModMailSPane.setViewportBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        ModMailSPane.setViewportView(ModMailEPane);
        ModMailPanel.setLayout(new java.awt.BorderLayout());
        ModMailPanel.add(ModMailSPane, java.awt.BorderLayout.CENTER);

        ErrorLogEPane.setEditable(false);
        ErrorLogEPane.setCaret(new mekwars.client.gui.CCommPanel.ScrollCaret());
        ErrorLogEPane.addHyperlinkListener(chatHLL);
        ErrorLogEPane.setEditorKit(kit);
        ErrorLogEPane.addMouseListener(this);
        ErrorLogEPane.setName(Integer.toString(CHANNEL_ERROR));
        ErrorLogSPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ErrorLogSPane.setViewportBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        ErrorLogSPane.setViewportView(ErrorLogEPane);
        ErrorLogPanel.setLayout(new java.awt.BorderLayout());
        ErrorLogPanel.add(ErrorLogSPane, java.awt.BorderLayout.CENTER);

        /*
         * CHANNEL_MAIN = CommTPane.indexOfComponent(MChannelPanel);
         * CHANNEL_NEWS = CommTPane.indexOfComponent(NChannelPanel);
         * CHANNEL_HMAIL = CommTPane.indexOfComponent(HMailPanel); CHANNEL_PMAIL =
         * CommTPane.indexOfComponent(PMailPanel); CHANNEL_PLOG =
         * CommTPane.indexOfComponent(PLogPanel); CHANNEL_SLOG =
         * CommTPane.indexOfComponent(SLogPanel); CHANNEL_MISC =
         * CommTPane.indexOfComponent(MiscChannelPanel);
         */
        /*
         * CommTPane.setSelectedIndex(CHANNEL_SLOG);
         * CommTPane.setSelectedIndex(CHANNEL_PLOG);
         * CommTPane.setSelectedIndex(CHANNEL_PMAIL);
         * CommTPane.setSelectedIndex(CHANNEL_HMAIL);
         */
        CommTPane.setSelectedIndex(CHANNEL_MAIN);
        CommTPane.addChangeListener(this);
        add(CommTPane, java.awt.BorderLayout.CENTER);
        chatField = new mekwars.client.gui.CCommPanel.CChatField(mwclient);
        chatField.setMaximumSize(new java.awt.Dimension(10000, 100));
        chatField.setMinimumSize(new java.awt.Dimension(550, 20));
        chatField.addMouseListener(this);
        // this is a little messy, can be fixed later...
        chatField.setReceiver(new mekwars.client.gui.CCommPanel.IInputReceiver() {
            public boolean processInput(String input) {
                return (sendChat(input));
            }
        });
        add(chatField, java.awt.BorderLayout.SOUTH);
        ForwardCommTab = new mekwars.client.gui.CCommPanel.CTabForwardAction();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt Z"), "TabForward");
        getActionMap().put("TabForward", ForwardCommTab);
        BackwardCommTab = new mekwars.client.gui.CCommPanel.CTabBackwardAction();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("shift alt Z"), "TabBackward");
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

        // map the coammands.
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("F1"), "HitF1");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("F2"), "HitF2");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("F3"), "HitF3");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("F4"), "HitF4");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("F5"), "HitF5");
        getActionMap().put("HitF1", new mekwars.client.gui.CCommPanel.CF1KeyAction());
        getActionMap().put("HitF2", new mekwars.client.gui.CCommPanel.CF2KeyAction());
        getActionMap().put("HitF3", new mekwars.client.gui.CCommPanel.CF3KeyAction());
        getActionMap().put("HitF4", new mekwars.client.gui.CCommPanel.CF4KeyAction());
        getActionMap().put("HitF5", new mekwars.client.gui.CCommPanel.CF5KeyAction());
    }// end CommPanel()

    public void createModTab() {
        // tabText = Client.getConfig().getParam("HOUSEMAILTABNAME");
        String tabText = "Mod Mail";
        CommTPane.addTab(tabText, null, ModMailPanel, "Mod Communication Channel (Alt + O)");
        int index = CommTPane.indexOfComponent(ModMailPanel);
        int mnemo = CommTPane.getTitleAt(index).indexOf("O");
        if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf("o");}

        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        ModMailSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(ModMailPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt O"), "ModMailSelect");
        getActionMap().put("ModMailSelect", ModMailSelect);

    }

    public void createErrorTab() {
        String tabText = "Error Log";
        // ErrorLogEPane.addMouseListener(this);

        CommTPane.addTab(tabText, null, ErrorLogPanel, "Error Log Channel (Alt + R)");
        int index = CommTPane.indexOfComponent(ErrorLogPanel);
        int mnemo = CommTPane.getTitleAt(index).indexOf("R");
        if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf("r");}

        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        ErrorLogSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(ErrorLogPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt R"), "ErrorLogSelect");
        getActionMap().put("ErrorLogSelect", ErrorLogSelect);
    }

    public javax.swing.JPanel findMailTab(String tabName) {
        javax.swing.JPanel panel = null;

        for (int pos = 0; pos < CommTPane.getTabCount(); pos++) {
            // System.err.println("Tab: "+CommTPane.getTitleAt(pos));
            if (CommTPane.getComponent(pos) instanceof javax.swing.JPanel) {
                panel = (javax.swing.JPanel) CommTPane.getComponent(pos);
                // System.err.println("Panel Name: "+panel.getName());
                // System.err.println("Find: "+tabName);
                if (panel != null && panel.getName() != null && panel.getName().equals("Mail Tab " + tabName)) {
                    return panel;
                }
            }
        }
        return null;
    }

    public int countMailTabs() {
        javax.swing.JPanel panel = null;
        int count = 0;

        for (int pos = 0; pos < CommTPane.getTabCount(); pos++) {
            // System.err.println("Tab: "+CommTPane.getTitleAt(pos));
            if (CommTPane.getComponent(pos) instanceof javax.swing.JPanel) {
                panel = (javax.swing.JPanel) CommTPane.getComponent(pos);
                // System.err.println("Panel Name: "+panel.getName());
                // System.err.println("Find: "+tabName);
                if (panel != null && panel.getName() != null && panel.getName().startsWith("Mail Tab ")) {count++;}
            }
        }
        return count;
    }

    public int getNextMailTabNumber() {
        javax.swing.JPanel panel = null;
        int count = 1;
        int maxTabs = mwclient.getConfig().getIntParam("MAXPMTABS");
        boolean found = false;

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

    public void createMailTab(String tabName) {

        javax.swing.JEditorPane newETab = new javax.swing.JEditorPane("text/html", "");
        javax.swing.JScrollPane newSTab = new javax.swing.JScrollPane();
        javax.swing.JPanel newPanel = new javax.swing.JPanel();
        MMNetHyperLinkListener chatHLL = new MMNetHyperLinkListener(mwclient);
        String mnoemonic = Integer.toString(getNextMailTabNumber());

        newETab.setEditable(false);
        newETab.setCaret(new mekwars.client.gui.CCommPanel.ScrollCaret());
        newETab.addHyperlinkListener(chatHLL);
        newETab.setEditorKit(kit);
        newSTab.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        newSTab.setViewportBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        newSTab.setViewportView(newETab);
        newPanel.setLayout(new java.awt.BorderLayout());
        newPanel.add(newSTab, java.awt.BorderLayout.CENTER);

        newPanel.setName("Mail Tab " + tabName);
        newETab.addMouseListener(this);

        CommTPane.addTab(mnoemonic + ". " + tabName,
              null,
              newPanel,
              "Mail From " + tabName + " (Alt + " + mnoemonic + ")");

        int index = CommTPane.indexOfComponent(newPanel);
        int mnemo = CommTPane.getTitleAt(index).indexOf(mnoemonic.toUpperCase());
        if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnoemonic.toLowerCase());}

        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        mekwars.client.gui.CCommPanel.CSelectTabAction LogSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(
              newPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " + mnoemonic.toUpperCase()),
              "Mail From " + tabName);
        getActionMap().put("Mail From " + tabName, LogSelect);
    }

    // something that the user has typed
    public boolean sendChat(String s) {
        if (!s.startsWith(client.MWClient.GUI_PREFIX) &&
                  CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(HMailPanel)) {
            s = client.MWClient.GUI_PREFIX + "c hm#" + s;
        }
        if (!s.startsWith(client.MWClient.GUI_PREFIX) &&
                  CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(ModMailPanel)) {
            s = client.MWClient.GUI_PREFIX + "c mm#" + s;
        }
        if (!s.startsWith(client.MWClient.GUI_PREFIX) &&
                  CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(RPGChannelPanel)) {
            s = client.MWClient.GUI_PREFIX + "c ic#" + s;
        }

        if (!s.startsWith(client.MWClient.GUI_PREFIX) &&
                  !mwclient.getConfig().isParam("USEMULTIPLEPM") &&
                  CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(PMailPanel)) {
            String receiver = mwclient.getLastQuery();
            if (receiver != null && !receiver.equals("")) {
                s = client.MWClient.GUI_PREFIX + "mail " + receiver + "," + s;
            } else {
                mwclient.showInfoWindow("No receiver set.");
                chatField.setText(s);
                return false;
            }
        }
        if (!s.startsWith(
              client.MWClient.GUI_PREFIX) &&
                  mwclient.getConfig().isParam("USEMULTIPLEPM") &&
                  ((javax.swing.JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex())).getName() != null &&
                  ((javax.swing.JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex())).getName()
                        .startsWith("Mail Tab ")) {
            javax.swing.JPanel panel = ((javax.swing.JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex()));
            String mailTab = "Mail Tab ";
            String receiver = panel.getName().substring(mailTab.length()).trim();
            if (receiver != null && !receiver.equals("")) {
                s = client.MWClient.GUI_PREFIX + "mail " + receiver + "," + s;
            } else {
                mwclient.showInfoWindow("No receiver set.");
                chatField.setText(s);
                return false;
            }
        }

        if (s.startsWith(client.MWClient.GUI_PREFIX + "me") || s.startsWith(client.MWClient.GUI_PREFIX + "c me")) {
            if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(RPGChannelPanel)) {
                s += "|ic";
            } else if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(ModMailPanel)) {
                s += "|mm";
            } else if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(HMailPanel)) {
                s += "|hm";
            } else if (!mwclient.getConfig().isParam("USEMULTIPLEPM") &&
                             CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(PMailPanel)) {
                s += "|mail";
                String receiver = mwclient.getLastQuery();
                if (receiver != null && !receiver.equals("")) {s += "|" + receiver;} else {
                    mwclient.showInfoWindow("No receiver set.");
                    chatField.setText(s);
                    return false;
                }
            } else if (mwclient.getConfig().isParam("USEMULTIPLEPM") &&
                             ((javax.swing.JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex())).getName() !=
                                   null &&
                             ((javax.swing.JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex())).getName()
                                   .startsWith("Mail Tab ")) {
                s += "|mail";
                javax.swing.JPanel panel = ((javax.swing.JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex()));
                String mailTab = "Mail Tab ";
                String receiver = panel.getName().substring(mailTab.length()).trim();
                if (receiver != null && !receiver.equals("")) {s += "|" + receiver;} else {
                    mwclient.showInfoWindow("No receiver set.");
                    chatField.setText(s);
                    return false;
                }
            }

        }
        mwclient.processGUIInput(s);
        return true;
    }

    public void setInput(String input) {
        chatField.setText(input);
    }

    public String getInput() {
        return chatField.getText();
    }

    public void setChat(String s, int channel) {
        setChat(s, channel, null);
    }

    // this method needs some tuning (carret and scrollbar issues)
    public void setChat(String s, int channel, String mailTab) {

        int tabChannel = channel;
        if (channel == CHANNEL_HMAIL) {
            if (!mwclient.getConfig().isParam("HOUSEMAILVISIBLE")) {
                printCommLog(s, channel);
                return;
            }
            // else
            tabChannel = CommTPane.indexOfComponent(HMailPanel);
        } else if (channel == CHANNEL_PMAIL) {

            if (!mwclient.getConfig().isParam("PRIVATEMAILVISIBLE")) {
                printCommLog(s, channel);
                return;
            }

            if (mwclient.getConfig().isParam("USEMULTIPLEPM")) {
                if (mailTab == null) {return;}
                // else
                tabChannel = CommTPane.indexOfComponent(findMailTab(mailTab));
            } else {
                tabChannel = CommTPane.indexOfComponent(PMailPanel);
            }
        } else if (channel == CHANNEL_PLOG) {
            if (!mwclient.getConfig().isParam("PERSONALLOGVISIBLE")) {
                printCommLog(s, channel);
                return;
            }
            // else
            tabChannel = CommTPane.indexOfComponent(PLogPanel);
        } else if (channel == CHANNEL_SLOG) {
            if (!mwclient.getConfig().isParam("SYSTEMLOGVISIBLE")) {
                printCommLog(s, channel);
                return;
            }
            // else
            tabChannel = CommTPane.indexOfComponent(SLogPanel);
        } else if (channel == CHANNEL_MISC) {
            if (!mwclient.getConfig().isParam("MISCELLANEOUSVISIBLE")) {
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
            if (!mwclient.getConfig().isParam("RPGVISIBLE")) {
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
                CommTPane.setTitleAt(tabChannel, "*" + title);
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
            ((mekwars.client.gui.CCommPanel.ScrollCaret) editorpane.getCaret()).showCaret = false;
        } else {((mekwars.client.gui.CCommPanel.ScrollCaret) editorpane.getCaret()).showCaret = true;}

        try {
            if (s.endsWith("<br>")) {
                s = s.substring(0, s.length() - 4);
            }
            ((javax.swing.text.html.HTMLEditorKit) editorpane.getEditorKit()).read(new java.io.StringReader(s),
                  editorpane.getDocument(),
                  editorpane.getDocument().getLength());
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
        if (oldSize > MAXBUFFER) { // used to bo only when scrolling
            try {
                // remove enough to get us back in our "nice" zone
                editorpane.getDocument().remove(0, oldSize - CAPBUFFERAMOUNT);
            } catch (javax.swing.text.BadLocationException ex) {/* Ignore */
            }
        }
        if (scroll) {editorpane.setCaretPosition(editorpane.getDocument().getLength());}
    }

    /*
     * remove in a given window all http-link tags public void
     * removeHttpLinksFromEditorPane(int channel) { JEditorPane ePane =
     * getEditorPane(channel); ePane.setText(ePane.getText().replaceAll("(<a[^>]*>)|(</a>)",
     * ""));
     *  }
     */

    public javax.swing.JEditorPane getEditorPane(int channel) {
        return getEditorPane(channel, null);
    }

    public javax.swing.JEditorPane getEditorPane(int channel, String mailTab) {

        if (channel == CHANNEL_MAIN) {
            return MChannelEPane;
        }
        if (channel == CHANNEL_HMAIL) {
            return HMailEPane;
        }

        if (channel == CHANNEL_PMAIL) {

            if (mwclient.getConfig().isParam("USEMULTIPLEPM")) {

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

    public javax.swing.JScrollPane getScrollPane(int channel) {
        return getScrollPane(channel, null);
    }

    public javax.swing.JScrollPane getScrollPane(int channel, String tabName) {
        if (channel == CHANNEL_MAIN) {
            return MChannelSPane;
        } else if (channel == CHANNEL_HMAIL) {
            return HMailSPane;
        }

        // special accomidation for PM's b/c of multimail tabs
        else if (channel == CHANNEL_PMAIL) {
            if (mwclient.getConfig().isParam("USEMULTIPLEPM")) {
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

    // change listener
    // component listener
    public void componentHidden(java.awt.event.ComponentEvent e) {
    }

    public void componentMoved(java.awt.event.ComponentEvent e) {
    }

    public void componentShown(java.awt.event.ComponentEvent e) {
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
            ps.print(s + "<br>");
            ps.close();
            fos.close();
        } catch (Exception e) {
            MWLogger.errLog(e);
        }
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

    // component listener actions
    private class CTabForwardAction extends javax.swing.AbstractAction {

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
            index++;
            if (index == count) {
                index = 0;
            }
            while (!CommTPane.isEnabledAt(index)) {
                index++;
                if (index == count) {
                    index = 0;
                }
            }
            CommTPane.setSelectedIndex(index);
        }
    }

    private class CTabBackwardAction extends javax.swing.AbstractAction {

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
            index--;
            if (index == -1) {
                index = count - 1;
            }
            while (!CommTPane.isEnabledAt(index)) {
                index--;
                if (index == -1) {
                    index = count - 1;
                }
            }
            CommTPane.setSelectedIndex(index);
        }
    }

    private class CSelectTabAction extends javax.swing.AbstractAction {

        private static final long serialVersionUID = -3297024782997974093L;
        private java.awt.Component Tab = null;

        public CSelectTabAction(java.awt.Component tab) {
            Tab = tab;
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (CommTPane.isEnabledAt(CommTPane.indexOfComponent(Tab))) {CommTPane.setSelectedComponent(Tab);}
        }
    }

    private class CF1KeyAction extends javax.swing.AbstractAction {
        private static final long serialVersionUID = -9057337706669800548L;

        public void actionPerformed(java.awt.event.ActionEvent e) {
            processFunctionKeyCommand(mwclient.getConfigParam("F1BIND"));
        }
    }// end CF1Action

    private class CF2KeyAction extends javax.swing.AbstractAction {
        private static final long serialVersionUID = 2756143179187978156L;

        public void actionPerformed(java.awt.event.ActionEvent e) {
            processFunctionKeyCommand(mwclient.getConfigParam("F2BIND"));
        }
    }// end CF1Action

    private class CF3KeyAction extends javax.swing.AbstractAction {
        private static final long serialVersionUID = -4939591054092680536L;

        public void actionPerformed(java.awt.event.ActionEvent e) {
            processFunctionKeyCommand(mwclient.getConfigParam("F3BIND"));
        }
    }// end CF1Action

    private class CF4KeyAction extends javax.swing.AbstractAction {
        private static final long serialVersionUID = -6563867639119041768L;

        public void actionPerformed(java.awt.event.ActionEvent e) {
            processFunctionKeyCommand(mwclient.getConfigParam("F4BIND"));
        }
    }// end CF1Action

    private class CF5KeyAction extends javax.swing.AbstractAction {
        private static final long serialVersionUID = 6214466835711778622L;

        public void actionPerformed(java.awt.event.ActionEvent e) {
            processFunctionKeyCommand(mwclient.getConfigParam("F5BIND"));
        }
    }// end CF1Action

    // chat field class
    public class CChatField extends javax.swing.JTextField
          implements java.awt.event.ActionListener, java.awt.event.KeyListener {

        private static final long serialVersionUID = -9140296134799032871L;

        client.MWClient Client;
        int ChatHistoryNumber = 0;
        int UserNumber = 0;

        java.util.ArrayList<String> ChatHistory = new java.util.ArrayList<String>();
        java.util.ArrayList<String> Users = new java.util.ArrayList<String>();

        mekwars.client.gui.CCommPanel.IInputReceiver myReceiver;
        String textandnick = "";

        public CChatField(client.MWClient client) {
            Client = client;
            this.addActionListener(this);
            this.addKeyListener(this);
            this.setFocusTraversalKeysEnabled(false);
            this.setBackground(StringUtils.html2Color(mwclient.getConfigParam("BACKGROUNDCOLOR")));
            this.setForeground(StringUtils.html2Color(mwclient.getConfigParam("CHATFONTCOLOR")));
            this.setCaretColor(StringUtils.html2Color(mwclient.getConfigParam("CHATFONTCOLOR")));
        }

        public void setReceiver(mekwars.client.gui.CCommPanel.IInputReceiver receiver) {
            this.myReceiver = receiver;
        }

        public String parseOutUserName(String text) {

            // there are spaces in the text so get the last word
            if (text.trim().indexOf(" ") != -1) {text = text.substring(0, text.trim().lastIndexOf(" ")).trim();}

            // The name is the first word.
            else {text = "";}

            return text;
        }

        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            ChatHistory.add(getText());
            ChatHistoryNumber = 0;
            if (this.myReceiver != null) {
                if (myReceiver.processInput(getText())) {
                    setText("");
                }
            }
        }

        public void keyPressed(java.awt.event.KeyEvent e) {

            if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                UserNumber = 0;
                if (ChatHistoryNumber < ChatHistory.size()) {
                    ChatHistoryNumber++;
                    setText(ChatHistory.get(ChatHistory.size() - ChatHistoryNumber));
                }
            } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) {
                UserNumber = 0;
                if (ChatHistoryNumber > 0) {
                    ChatHistoryNumber--;
                    if (ChatHistoryNumber == 0) {
                        setText("");
                    } else {
                        setText(ChatHistory.get(ChatHistory.size() - ChatHistoryNumber));
                    }
                }
            } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_TAB) {
                String messageText = "";
                if (UserNumber == 0) {
                    Users = Client.getPartialUser(getText());
                    // No users where found keep the UserNumber at 0 and
                    // wait for the next key press.
                    if (Users == null || Users.isEmpty()) {return;}
                    textandnick = parseOutUserName(getText());
                    // Get rid of any leading spaces if the name is the first
                    // thing typed.
                    messageText = textandnick.trim() + " " + Users.get(UserNumber++).trim();
                } else if (UserNumber < Users.size()) {
                    messageText = textandnick.trim() + " " + Users.get(UserNumber++).trim();
                } else {
                    UserNumber = 0;
                    messageText = textandnick.trim() + " " + Users.get(UserNumber++).trim();
                }
                setText(messageText.trim());
            } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {

                if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(ErrorLogPanel)) {
                    CommTPane.remove(CommTPane.getSelectedComponent());
                } else if (mwclient.getConfig().isParam("USEMULTIPLEPM") &&
                                 CommTPane.getSelectedComponent() instanceof javax.swing.JPanel) {
                    javax.swing.JPanel panel = (javax.swing.JPanel) CommTPane.getSelectedComponent();
                    if (panel.getName() != null && panel.getName().startsWith("Mail Tab ")) {
                        CommTPane.remove(panel);
                        panel = null;
                    }
                }
            } else {UserNumber = 0;}

        }

        public void keyReleased(java.awt.event.KeyEvent e) {
            // filler
        }

        public void keyTyped(java.awt.event.KeyEvent e) {
            // filler
        }
    }

    // receiver interface
    public static interface IInputReceiver {
        public boolean processInput(String input);
    }

    // receiver interface
    public void mouseClicked(java.awt.event.MouseEvent e) {

        // only close from right clicks
        if (e.getButton() != java.awt.event.MouseEvent.BUTTON3) {
            return;
        }

        if (e.getSource() instanceof javax.swing.JEditorPane) {
            javax.swing.JPopupMenu clipboard = new javax.swing.JPopupMenu();

            javax.swing.JEditorPane pane = (javax.swing.JEditorPane) e.getSource();

            pane.requestFocusInWindow();
            // Information
            javax.swing.JMenuItem copy = new javax.swing.JMenuItem("Copy");
            if (pane.getSelectionStart() == pane.getSelectionEnd()) {copy.setActionCommand("");} else {
                copy.setActionCommand(pane.getSelectedText());
            }
            copy.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent ae) {
                    java.awt.datatransfer.StringSelection ss = new java.awt.datatransfer.StringSelection(ae.getActionCommand());
                    java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit()
                                                                      .getSystemClipboard();
                    clipboard.setContents(ss, ss);
                }
            });

            clipboard.add(copy);

            clipboard.addSeparator();

            copy = new javax.swing.JMenuItem("Select All");
            copy.setActionCommand(pane.getName());
            copy.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent ae) {
                    try {
                        int index = Integer.parseInt(ae.getActionCommand());
                        javax.swing.JEditorPane pane = getEditorPane(index);
                        pane.selectAll();
                    } catch (Exception ex) {
                        MWLogger.errLog(ex);
                    }
                }
            });

            clipboard.add(copy);
            clipboard.show(e.getComponent(), e.getX(), e.getY());
            return;
        }

        if (e.getSource() instanceof mekwars.client.gui.CCommPanel.CChatField) {
            javax.swing.JPopupMenu clipboard = new javax.swing.JPopupMenu();
            javax.swing.JMenuItem copy = new javax.swing.JMenuItem("Cut");

            copy.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent ae) {
                    java.awt.datatransfer.StringSelection ss = new java.awt.datatransfer.StringSelection(chatField.getSelectedText());
                    java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit()
                                                                      .getSystemClipboard();
                    clipboard.setContents(ss, ss);
                    try {
                        String newText = chatField.getText(0, chatField.getSelectionStart());
                        newText += chatField.getText(chatField.getSelectionEnd(),
                              chatField.getText().length() - chatField.getSelectionEnd());
                        chatField.setText(newText);
                    } catch (Exception ex) {
                        MWLogger.errLog(ex);
                    }
                }
            });

            clipboard.add(copy);

            copy = new javax.swing.JMenuItem("Copy");
            copy.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent ae) {
                    java.awt.datatransfer.StringSelection ss = new java.awt.datatransfer.StringSelection(chatField.getSelectedText());
                    java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit()
                                                                      .getSystemClipboard();
                    clipboard.setContents(ss, ss);
                }
            });

            clipboard.add(copy);

            copy = new javax.swing.JMenuItem("Paste");
            copy.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent ae) {
                    java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit()
                                                                      .getSystemClipboard();
                    String clipping = "";
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
            copy.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent ae) {
                    try {
                        String newText = chatField.getText(0, chatField.getSelectionStart());
                        newText += chatField.getText(chatField.getSelectionEnd(),
                              chatField.getText().length() - chatField.getSelectionEnd());
                        chatField.setText(newText);
                    } catch (Exception ex) {
                        MWLogger.errLog(ex);
                    }
                }
            });

            clipboard.add(copy);

            clipboard.addSeparator();

            copy = new javax.swing.JMenuItem("Select All");
            copy.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent ae) {
                    try {
                        chatField.selectAll();
                    } catch (Exception ex) {
                        MWLogger.errLog(ex);
                    }
                }
            });

            clipboard.add(copy);

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
            info.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    CommTPane.remove(CommTPane.getSelectedComponent());
                }
            });
            popup.add(info);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }

        // offer to close mail tabs
        if (mwclient.getConfig().isParam("USEMULTIPLEPM") &&
                  CommTPane.getSelectedComponent() instanceof javax.swing.JPanel) {
            javax.swing.JPanel panel = (javax.swing.JPanel) CommTPane.getSelectedComponent();
            if (panel != null && panel.getName().startsWith("Mail Tab ")) {
                javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
                javax.swing.JMenuItem info = new javax.swing.JMenuItem("Close");
                info.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        CommTPane.remove(CommTPane.getSelectedComponent());
                    }
                });
                popup.add(info);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
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
        String tabText = "";

        String mnemonicText;
        tabText = mwclient.getConfig().getParam("MAINCHANNELTABNAME");
        mnemonicText = mwclient.getConfig().getParam("MAINCHANNELMNEMONIC");

        CommTPane.addTab(tabText,
              null,
              MChannelPanel,
              "Interfaction Communication Channel (Alt + " + mnemonicText + ")");
        index = CommTPane.indexOfComponent(MChannelPanel);
        mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
        if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()),
              "MChannelSelect");
        getActionMap().put("MChannelSelect", MChannelSelect);

        if (mwclient.getConfig().isParam("HOUSEMAILVISIBLE")) {

            tabText = mwclient.getConfig().getParam("HOUSEMAILTABNAME");
            mnemonicText = mwclient.getConfig().getParam("HOUSEMAILMNEMONIC");

            CommTPane.addTab(tabText,
                  null,
                  HMailPanel,
                  "House Communication Channel (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(HMailPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            if (HMailSelect == null) {HMailSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(HMailPanel);}
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " +
                                                                                             mnemonicText.toUpperCase()),
                  "HMailSelect");
            getActionMap().put("HMailSelect", HMailSelect);
        }

        if (!mwclient.getConfig().isParam("USEMULTIPLEPM")) {

            if (mwclient.getConfig().isParam("PRIVATEMAILVISIBLE")) {
                tabText = mwclient.getConfig().getParam("PRIVATEMAILTABNAME");
                mnemonicText = mwclient.getConfig().getParam("PRIVATEMAILMNEMONIC");

                CommTPane.addTab(tabText, null, PMailPanel, "Private Mail (Alt + " + mnemonicText.toUpperCase() + ")");
                index = CommTPane.indexOfComponent(PMailPanel);
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
                if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
                CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
                if (PMailSelect == null) {PMailSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(PMailPanel);}
                getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " +
                                                                                                 mnemonicText.toUpperCase()),
                      "PMailSelect");
                getActionMap().put("PMailSelect", PMailSelect);

            }
        }

        if (mwclient.getConfig().isParam("PERSONALLOGVISIBLE")) {

            tabText = mwclient.getConfig().getParam("PERSONALLOGTABNAME");
            mnemonicText = mwclient.getConfig().getParam("PERSONALLOGMNEMONIC");

            CommTPane.addTab(tabText, null, PLogPanel, "Logged Messages (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(PLogPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            if (PLogSelect == null) {PLogSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(PLogPanel);}
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " +
                                                                                             mnemonicText.toUpperCase()),
                  "PLogSelect (Alt + " + mnemonicText.toUpperCase() + ")");
            getActionMap().put("PLogSelect", PLogSelect);
        }

        if (mwclient.getConfig().isParam("SYSTEMLOGVISIBLE")) {

            tabText = mwclient.getConfig().getParam("SYSTEMLOGTABNAME");
            mnemonicText = mwclient.getConfig().getParam("SYSTEMLOGMNEMONIC");

            CommTPane.addTab(tabText, null, SLogPanel, "System Messages (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(SLogPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            if (SLogSelect == null) {SLogSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(SLogPanel);}
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " +
                                                                                             mnemonicText.toUpperCase()),
                  "SLogSelect");
            getActionMap().put("SLogSelect", SLogSelect);
        }

        /* Misc-Channel */
        if (mwclient.getConfig().isParam("MISCELLANEOUSVISIBLE")) {

            tabText = mwclient.getConfig().getParam("MISCELLANEOUSTABNAME");
            mnemonicText = mwclient.getConfig().getParam("MISCELLANEOUSMNEMONIC");

            CommTPane.addTab(tabText,
                  null,
                  MiscChannelPanel,
                  "Miscellaneous Stuff (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(MiscChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            if (MiscChannelSelect == null) {
                MiscChannelSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(MiscChannelPanel);
            }
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " +
                                                                                             mnemonicText.toUpperCase()),
                  "MiscChannelSelect");
            getActionMap().put("MiscChannelSelect", MiscChannelSelect);
        }
        /* RPG-Channel */
        if (mwclient.getConfig().isParam("RPGVISIBLE")) {

            tabText = mwclient.getConfig().getParam("RPGTABNAME");
            mnemonicText = mwclient.getConfig().getParam("RPGMNEMONIC");

            CommTPane.addTab(tabText, null, RPGChannelPanel, "RP (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(RPGChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1) {mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());}
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            RPGChannelSelect = new mekwars.client.gui.CCommPanel.CSelectTabAction(RPGChannelPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " +
                                                                                             mnemonicText.toUpperCase()),
                  "RPGChannelSelect");
            getActionMap().put("RPGChannelSelect", RPGChannelSelect);
        }

    }

    private void processFunctionKeyCommand(String command) {
        java.util.StringTokenizer commands = new java.util.StringTokenizer(command, ";");

        while (commands.hasMoreTokens()) {
            mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "c " + commands.nextToken());
        }
    }
}
