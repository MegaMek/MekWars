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

package client.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLEditorKit;

import client.MWClient;
import common.util.MWLogger;
import common.util.StringUtils;

/**
 * This is a tabbed multi-channet Communications Panel using Swing to manage the
 * display.
 */
/*
 * Communications Panel
 */

public class CCommPanel extends JPanel implements ChangeListener, ComponentListener, MouseListener {

    /**
     * 
     */
    private static final long serialVersionUID = 8754254920729491806L;

    /**
     * Caret that does not auto-scroll if nothing is selected.
     */
    static class ScrollCaret extends DefaultCaret {
        /**
         * 
         */
        private static final long serialVersionUID = 7038682935535985621L;
        public boolean showCaret = true;

        /**
         * @see javax.swing.text.DefaultCaret#adjustVisibility(java.awt.Rectangle)
         */
        @Override
        protected void adjustVisibility(Rectangle nloc) {
            if (showCaret)
                super.adjustVisibility(nloc);
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
     * Maximum buffer of each channel. Each channel will be capped to have no
     * more than this amount of characters.
     */
    public static final int MAXBUFFER = 100000;
    /**
     * Amount of characters removed when buffer overflow. If a channels maximum
     * character count is reached, cap that many characters.
     * 
     * @see CCommPanel#MAXBUFFER
     */
    public static final int CAPBUFFERAMOUNT = 60000;

    MWClient mwclient;

    JTabbedPane CommTPane = new JTabbedPane(SwingConstants.BOTTOM);
    MyHTMLEditorKit kit = new MyHTMLEditorKit();
    JPanel MChannelPanel = new JPanel();
    CSelectTabAction MChannelSelect = null;
    JEditorPane MChannelEPane = new JEditorPane("text/html", "");
    JScrollPane MChannelSPane = new JScrollPane();
    JPanel HMailPanel = new JPanel();
    CSelectTabAction HMailSelect = null;
    JEditorPane HMailEPane = new JEditorPane("text/html", "");
    JScrollPane HMailSPane = new JScrollPane();
    JPanel PMailPanel = new JPanel();
    CSelectTabAction PMailSelect = null;
    JEditorPane PMailEPane = new JEditorPane("text/html", "");
    JScrollPane PMailSPane = new JScrollPane();
    JPanel PLogPanel = new JPanel();
    CSelectTabAction PLogSelect = null;
    JEditorPane PLogEPane = new JEditorPane("text/html", "");
    JScrollPane PLogSPane = new JScrollPane();
    JPanel SLogPanel = new JPanel();
    CSelectTabAction SLogSelect = null;
    JEditorPane SLogEPane = new JEditorPane("text/html", "");
    JScrollPane SLogSPane = new JScrollPane();
    JPanel MiscChannelPanel = new JPanel();
    CSelectTabAction MiscChannelSelect = null;
    JEditorPane MiscChannelEPane = new JEditorPane("text/html", "");
    JScrollPane MiscChannelSPane = new JScrollPane();
    JPanel ModMailPanel = new JPanel();
    CSelectTabAction ModMailSelect = null;
    JEditorPane ModMailEPane = new JEditorPane("text/html", "");
    JScrollPane ModMailSPane = new JScrollPane();
    JPanel ErrorLogPanel = new JPanel();
    CSelectTabAction ErrorLogSelect = null;
    JEditorPane ErrorLogEPane = new JEditorPane("text/html", "");
    JScrollPane ErrorLogSPane = new JScrollPane();
    JPanel RPGChannelPanel = new JPanel();
    CSelectTabAction RPGChannelSelect = null;
    JEditorPane RPGChannelEPane = new JEditorPane("text/html", "");
    JScrollPane RPGChannelSPane = new JScrollPane();
    CChatField chatField;
    Color TabForeground;
    Color TabBackground;
    boolean autoTextUpdate;
    CTabForwardAction ForwardCommTab;
    CTabBackwardAction BackwardCommTab;

    public CCommPanel(MWClient client) {
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
        TabBackground = new JList().getSelectionBackground().darker();
        autoTextUpdate = mwclient.getConfig().isParam("AUTOSCROLL");
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
        tabText = mwclient.getConfig().getParam("MAINCHANNELTABNAME");
        mnemonicText = mwclient.getConfig().getParam("MAINCHANNELMNEMONIC");

        CommTPane.addTab(tabText, null, MChannelPanel, "Interfaction Communication Channel (Alt + " + mnemonicText + ")");
        index = CommTPane.indexOfComponent(MChannelPanel);
        mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
        if (mnemo == -1)
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        MChannelSelect = new CSelectTabAction(MChannelPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()), "MChannelSelect");
        getActionMap().put("MChannelSelect", MChannelSelect);

        HMailEPane.setEditable(false);
        HMailEPane.setCaret(new ScrollCaret());
        HMailEPane.addHyperlinkListener(chatHLL);
        HMailEPane.setEditorKit(kit);
        HMailEPane.addMouseListener(this);
        HMailEPane.setName(Integer.toString(CHANNEL_HMAIL));
        HMailSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        HMailSPane.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
        HMailSPane.setViewportView(HMailEPane);
        HMailPanel.setLayout(new BorderLayout());
        HMailPanel.add(HMailSPane, BorderLayout.CENTER);
        
        if (mwclient.getConfig().isParam("HOUSEMAILVISIBLE")) {

            tabText = mwclient.getConfig().getParam("HOUSEMAILTABNAME");
            mnemonicText = mwclient.getConfig().getParam("HOUSEMAILMNEMONIC");

            CommTPane.addTab(tabText, null, HMailPanel, "House Communication Channel (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(HMailPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            HMailSelect = new CSelectTabAction(HMailPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()), "HMailSelect");
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

        RPGChannelEPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
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
            RPGChannelSelect = new CSelectTabAction(RPGChannelPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()), "RPGChannelSelect");
            getActionMap().put("RPGChannelSelect", RPGChannelSelect);
        }

        if (!mwclient.getConfig().isParam("USEMULTIPLEPM")) {
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
                PMailSelect = new CSelectTabAction(PMailPanel);
                getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()), "PMailSelect");
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
            PLogSelect = new CSelectTabAction(PLogPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()), "PLogSelect (Alt + " + mnemonicText.toUpperCase() + ")");
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
            SLogSelect = new CSelectTabAction(SLogPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()), "SLogSelect");
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

        if (mwclient.getConfig().isParam("MISCELLANEOUSVISIBLE")) {

            tabText = mwclient.getConfig().getParam("MISCELLANEOUSTABNAME");
            mnemonicText = mwclient.getConfig().getParam("MISCELLANEOUSMNEMONIC");

            CommTPane.addTab(tabText, null, MiscChannelPanel, "Miscellaneous Stuff (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(MiscChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());

            if (mnemo == -1) {
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            }

            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            MiscChannelSelect = new CSelectTabAction(MiscChannelPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()), "MiscChannelSelect");
            getActionMap().put("MiscChannelSelect", MiscChannelSelect);
        }

        ModMailEPane.setEditable(false);
        ModMailEPane.setCaret(new ScrollCaret());
        ModMailEPane.addHyperlinkListener(chatHLL);
        ModMailEPane.setEditorKit(kit);
        ModMailEPane.addMouseListener(this);
        ModMailEPane.setName(Integer.toString(CHANNEL_MOD));
        ModMailSPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ModMailSPane.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
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
        ErrorLogSPane.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
        ErrorLogSPane.setViewportView(ErrorLogEPane);
        ErrorLogPanel.setLayout(new BorderLayout());
        ErrorLogPanel.add(ErrorLogSPane, BorderLayout.CENTER);

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
        add(CommTPane, BorderLayout.CENTER);
        chatField = new CChatField(mwclient);
        chatField.setMaximumSize(new java.awt.Dimension(10000, 100));
        chatField.setMinimumSize(new java.awt.Dimension(550, 20));
        chatField.addMouseListener(this);
        // this is a little messy, can be fixed later...
        chatField.setReceiver(new IInputReceiver() {
            public boolean processInput(String input) {
                return (sendChat(input));
            }
        });
        add(chatField, BorderLayout.SOUTH);
        ForwardCommTab = new CTabForwardAction();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt Z"), "TabForward");
        getActionMap().put("TabForward", ForwardCommTab);
        BackwardCommTab = new CTabBackwardAction();
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

        // map the coammands.
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F1"), "HitF1");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F2"), "HitF2");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F3"), "HitF3");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F4"), "HitF4");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F5"), "HitF5");
        getActionMap().put("HitF1", new CF1KeyAction());
        getActionMap().put("HitF2", new CF2KeyAction());
        getActionMap().put("HitF3", new CF3KeyAction());
        getActionMap().put("HitF4", new CF4KeyAction());
        getActionMap().put("HitF5", new CF5KeyAction());
    }// end CommPanel()

    public void createModTab() {
        // tabText = Client.getConfig().getParam("HOUSEMAILTABNAME");
        String tabText = "Mod Mail";
        CommTPane.addTab(tabText, null, ModMailPanel, "Mod Communication Channel (Alt + O)");
        int index = CommTPane.indexOfComponent(ModMailPanel);
        int mnemo = CommTPane.getTitleAt(index).indexOf("O");
        if (mnemo == -1)
            mnemo = CommTPane.getTitleAt(index).indexOf("o");

        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        ModMailSelect = new CSelectTabAction(ModMailPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt O"), "ModMailSelect");
        getActionMap().put("ModMailSelect", ModMailSelect);

    }

    public void createErrorTab() {
        String tabText = "Error Log";
        // ErrorLogEPane.addMouseListener(this);

        CommTPane.addTab(tabText, null, ErrorLogPanel, "Error Log Channel (Alt + R)");
        int index = CommTPane.indexOfComponent(ErrorLogPanel);
        int mnemo = CommTPane.getTitleAt(index).indexOf("R");
        if (mnemo == -1)
            mnemo = CommTPane.getTitleAt(index).indexOf("r");

        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        ErrorLogSelect = new CSelectTabAction(ErrorLogPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt R"), "ErrorLogSelect");
        getActionMap().put("ErrorLogSelect", ErrorLogSelect);
    }

    public JPanel findMailTab(String tabName) {
        JPanel panel = null;

        for (int pos = 0; pos < CommTPane.getTabCount(); pos++) {
            // System.err.println("Tab: "+CommTPane.getTitleAt(pos));
            if (CommTPane.getComponent(pos) instanceof JPanel) {
                panel = (JPanel) CommTPane.getComponent(pos);
                // System.err.println("Panel Name: "+panel.getName());
                // System.err.println("Find: "+tabName);
                if (panel != null && panel.getName() != null && panel.getName().equals("Mail Tab " + tabName))
                    return panel;
            }
        }
        return null;
    }

    public int countMailTabs() {
        JPanel panel = null;
        int count = 0;

        for (int pos = 0; pos < CommTPane.getTabCount(); pos++) {
            // System.err.println("Tab: "+CommTPane.getTitleAt(pos));
            if (CommTPane.getComponent(pos) instanceof JPanel) {
                panel = (JPanel) CommTPane.getComponent(pos);
                // System.err.println("Panel Name: "+panel.getName());
                // System.err.println("Find: "+tabName);
                if (panel != null && panel.getName() != null && panel.getName().startsWith("Mail Tab "))
                    count++;
            }
        }
        return count;
    }

    public int getNextMailTabNumber() {
        JPanel panel = null;
        int count = 1;
        int maxTabs = mwclient.getConfig().getIntParam("MAXPMTABS");
        boolean found = false;

        for (count = 1; count <= maxTabs; count++) {
            found = false;
            for (int pos = 0; pos < CommTPane.getTabCount(); pos++) {
                if (CommTPane.getComponent(pos) instanceof JPanel) {
                    panel = (JPanel) CommTPane.getComponent(pos);
                    if (panel != null && panel.getName() != null && panel.getName().startsWith("Mail Tab ")) {
                        String tabText = CommTPane.getTitleAt(pos).trim();
                        if (tabText.startsWith("*"))
                            tabText = tabText.substring(1);
                        if (Integer.parseInt(tabText.substring(0, tabText.indexOf("."))) == count) {
                            found = true;
                            break;// no reason to continue the search.
                        }
                    }
                }
            }
            if (!found)
                break;
        }
        return count;

    }

    public void createMailTab(String tabName) {

        JEditorPane newETab = new JEditorPane("text/html", "");
        JScrollPane newSTab = new JScrollPane();
        JPanel newPanel = new JPanel();
        MMNetHyperLinkListener chatHLL = new MMNetHyperLinkListener(mwclient);
        String mnoemonic = Integer.toString(getNextMailTabNumber());

        newETab.setEditable(false);
        newETab.setCaret(new ScrollCaret());
        newETab.addHyperlinkListener(chatHLL);
        newETab.setEditorKit(kit);
        newSTab.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        newSTab.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
        newSTab.setViewportView(newETab);
        newPanel.setLayout(new BorderLayout());
        newPanel.add(newSTab, BorderLayout.CENTER);

        newPanel.setName("Mail Tab " + tabName);
        newETab.addMouseListener(this);

        CommTPane.addTab(mnoemonic + ". " + tabName, null, newPanel, "Mail From " + tabName + " (Alt + " + mnoemonic + ")");

        int index = CommTPane.indexOfComponent(newPanel);
        int mnemo = CommTPane.getTitleAt(index).indexOf(mnoemonic.toUpperCase());
        if (mnemo == -1)
            mnemo = CommTPane.getTitleAt(index).indexOf(mnoemonic.toLowerCase());

        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        CSelectTabAction LogSelect = new CSelectTabAction(newPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnoemonic.toUpperCase()), "Mail From " + tabName);
        getActionMap().put("Mail From " + tabName, LogSelect);
    }

    // something that the user has typed
    public boolean sendChat(String s) {
        if (!s.startsWith(MWClient.GUI_PREFIX) && CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(HMailPanel)) {
            s = MWClient.GUI_PREFIX + "c hm#" + s;
        }
        if (!s.startsWith(MWClient.GUI_PREFIX) && CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(ModMailPanel)) {
            s = MWClient.GUI_PREFIX + "c mm#" + s;
        }
        if (!s.startsWith(MWClient.GUI_PREFIX) && CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(RPGChannelPanel)) {
            s = MWClient.GUI_PREFIX + "c ic#" + s;
        }

        if (!s.startsWith(MWClient.GUI_PREFIX) && !mwclient.getConfig().isParam("USEMULTIPLEPM") && CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(PMailPanel)) {
            String receiver = mwclient.getLastQuery();
            if (receiver != null && !receiver.equals("")) {
                s = MWClient.GUI_PREFIX + "mail " + receiver + "," + s;
            } else {
                mwclient.showInfoWindow("No receiver set.");
                chatField.setText(s);
                return false;
            }
        }
        if (!s.startsWith(MWClient.GUI_PREFIX) && mwclient.getConfig().isParam("USEMULTIPLEPM") && ((JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex())).getName() != null && ((JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex())).getName().startsWith("Mail Tab ")) {
            JPanel panel = ((JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex()));
            String mailTab = "Mail Tab ";
            String receiver = panel.getName().substring(mailTab.length()).trim();
            if (receiver != null && !receiver.equals("")) {
                s = MWClient.GUI_PREFIX + "mail " + receiver + "," + s;
            } else {
                mwclient.showInfoWindow("No receiver set.");
                chatField.setText(s);
                return false;
            }
        }

        if (s.startsWith(MWClient.GUI_PREFIX + "me") || s.startsWith(MWClient.GUI_PREFIX + "c me")) {
            if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(RPGChannelPanel))
                s += "|ic";
            else if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(ModMailPanel))
                s += "|mm";
            else if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(HMailPanel))
                s += "|hm";
            else if (!mwclient.getConfig().isParam("USEMULTIPLEPM") && CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(PMailPanel)) {
                s += "|mail";
                String receiver = mwclient.getLastQuery();
                if (receiver != null && !receiver.equals(""))
                    s += "|" + receiver;
                else {
                    mwclient.showInfoWindow("No receiver set.");
                    chatField.setText(s);
                    return false;
                }
            } else if (mwclient.getConfig().isParam("USEMULTIPLEPM") && ((JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex())).getName() != null && ((JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex())).getName().startsWith("Mail Tab ")) {
                s += "|mail";
                JPanel panel = ((JPanel) CommTPane.getComponent(CommTPane.getSelectedIndex()));
                String mailTab = "Mail Tab ";
                String receiver = panel.getName().substring(mailTab.length()).trim();
                if (receiver != null && !receiver.equals(""))
                    s += "|" + receiver;
                else {
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
                if (mailTab == null)
                    return;
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
            if (CommTPane.indexOfComponent(ModMailPanel) == -1)
                createModTab();
            tabChannel = CommTPane.indexOfComponent(ModMailPanel);
        } else if (channel == CHANNEL_ERROR) {
            if (CommTPane.indexOfComponent(ErrorLogPanel) == -1)
                createErrorTab();
            tabChannel = CommTPane.indexOfComponent(ErrorLogPanel);
        } else if (channel == CHANNEL_RPG) {
            if (!mwclient.getConfig().isParam("RPGVISIBLE")) {
                printCommLog(s, channel);
                return;
            }
            // else
            tabChannel = CommTPane.indexOfComponent(RPGChannelPanel);
        }

        JEditorPane editorpane = getEditorPane(channel, mailTab);
        JScrollBar scrollbar = null;
        if (editorpane == null) {
            return;
        }

        if (getScrollPane(channel, mailTab) != null)
            scrollbar = getScrollPane(channel, mailTab).getVerticalScrollBar();

        int oldSize = editorpane.getDocument().getLength();
        boolean scroll = true;
        if (CommTPane.getSelectedIndex() != -1 && CommTPane.getSelectedIndex() != tabChannel) {
            String title = CommTPane.getTitleAt(tabChannel);
            if (!title.startsWith("*")) {
                int mnemo = CommTPane.getDisplayedMnemonicIndexAt(tabChannel);
                mnemo++;
                CommTPane.setTitleAt(tabChannel, "*" + title);
                CommTPane.setDisplayedMnemonicIndexAt(tabChannel, mnemo);
                if (CommTPane.getComponentAt(tabChannel) != PLogPanel && CommTPane.getComponentAt(tabChannel) != SLogPanel) {
                    CommTPane.setForegroundAt(tabChannel, TabForeground); // invert
                    // tab
                    // text
                    // CommTPane.setBackgroundAt(tabChannel, TabBackground);
                }
            }
        }
        // if scrollbar was moved up, add text, but keep scrollbar as it is,
        // otherwise scrollbar is almost at the bottom, add text in normal way
        if (scrollbar != null && !autoTextUpdate && (scrollbar.getValue() + scrollbar.getVisibleAmount()) < scrollbar.getMaximum() * 0.97) {
            scroll = false;
            ((ScrollCaret) editorpane.getCaret()).showCaret = false;
        } else
            ((ScrollCaret) editorpane.getCaret()).showCaret = true;

        try {
            if (s.endsWith("<br>")) {
                s = s.substring(0, s.length() - 4);
            }
            ((HTMLEditorKit) editorpane.getEditorKit()).read(new StringReader(s), editorpane.getDocument(), editorpane.getDocument().getLength());
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
        if (scroll)
            editorpane.setCaretPosition(editorpane.getDocument().getLength());
    }

    /*
     * remove in a given window all http-link tags public void
     * removeHttpLinksFromEditorPane(int channel) { JEditorPane ePane =
     * getEditorPane(channel); ePane.setText(ePane.getText().replaceAll("(<a[^>]*>)|(</a>)",
     * ""));
     *  }
     */

    public JEditorPane getEditorPane(int channel) {
        return getEditorPane(channel, null);
    }

    public JEditorPane getEditorPane(int channel, String mailTab) {

        if (channel == CHANNEL_MAIN) {
            return MChannelEPane;
        }
        if (channel == CHANNEL_HMAIL) {
            return HMailEPane;
        }

        if (channel == CHANNEL_PMAIL) {

            if (mwclient.getConfig().isParam("USEMULTIPLEPM")) {

                JPanel panel = findMailTab(mailTab);
                if (panel == null)
                    return null;

                JScrollPane sPane = (JScrollPane) panel.getComponent(0);
                if (sPane == null)
                    return null;

                return (JEditorPane) sPane.getViewport().getView();
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

    public JScrollPane getScrollPane(int channel) {
        return getScrollPane(channel, null);
    }

    public JScrollPane getScrollPane(int channel, String tabName) {
        if (channel == CHANNEL_MAIN) {
            return MChannelSPane;
        } else if (channel == CHANNEL_HMAIL) {
            return HMailSPane;
        }

        // special accomidation for PM's b/c of multimail tabs
        else if (channel == CHANNEL_PMAIL) {
            if (mwclient.getConfig().isParam("USEMULTIPLEPM")) {
                JPanel panel = findMailTab(tabName);
                if (panel == null)
                    return null;
                return (JScrollPane) panel.getComponent(0);
            }
            return PMailSPane;
        }

        else if (channel == CHANNEL_PLOG) {
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
    public void stateChanged(ChangeEvent e) {
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
    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        for (int i = 0; i < CommTPane.getTabCount(); i++) {

            if (getScrollPane(i) == null || getEditorPane(i) == null)
                continue;

            JScrollPane scrollpane = getScrollPane(i);
            JEditorPane editorpane = getEditorPane(i);
            JScrollBar scrollbar = scrollpane.getVerticalScrollBar();
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
            FileOutputStream fos = new FileOutputStream(filePath, true);
            PrintWriter ps = new PrintWriter(fos);
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
    private class CTabForwardAction extends AbstractAction {

        private static final long serialVersionUID = -7910457998205249026L;

        public CTabForwardAction() {
            // empty constructor
        }

        public void actionPerformed(ActionEvent e) {
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

    private class CTabBackwardAction extends AbstractAction {

        private static final long serialVersionUID = -880460003608846342L;

        public CTabBackwardAction() {
            // empty constructor
        }

        public void actionPerformed(ActionEvent e) {
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

    private class CSelectTabAction extends AbstractAction {

        private static final long serialVersionUID = -3297024782997974093L;
        private Component Tab = null;

        public CSelectTabAction(Component tab) {
            Tab = tab;
        }

        public void actionPerformed(ActionEvent e) {
            if (CommTPane.isEnabledAt(CommTPane.indexOfComponent(Tab)))
                CommTPane.setSelectedComponent(Tab);
        }
    }

    private class CF1KeyAction extends AbstractAction {
        private static final long serialVersionUID = -9057337706669800548L;

        public void actionPerformed(ActionEvent e) {
            processFunctionKeyCommand(mwclient.getConfigParam("F1BIND"));
        }
    }// end CF1Action

    private class CF2KeyAction extends AbstractAction {
        private static final long serialVersionUID = 2756143179187978156L;

        public void actionPerformed(ActionEvent e) {
            processFunctionKeyCommand(mwclient.getConfigParam("F2BIND"));
        }
    }// end CF1Action

    private class CF3KeyAction extends AbstractAction {
        private static final long serialVersionUID = -4939591054092680536L;

        public void actionPerformed(ActionEvent e) {
            processFunctionKeyCommand(mwclient.getConfigParam("F3BIND"));
        }
    }// end CF1Action

    private class CF4KeyAction extends AbstractAction {
        private static final long serialVersionUID = -6563867639119041768L;

        public void actionPerformed(ActionEvent e) {
            processFunctionKeyCommand(mwclient.getConfigParam("F4BIND"));
        }
    }// end CF1Action

    private class CF5KeyAction extends AbstractAction {
        private static final long serialVersionUID = 6214466835711778622L;

        public void actionPerformed(ActionEvent e) {
            processFunctionKeyCommand(mwclient.getConfigParam("F5BIND"));
        }
    }// end CF1Action

    // chat field class
    public class CChatField extends JTextField implements ActionListener, KeyListener {

        private static final long serialVersionUID = -9140296134799032871L;

        MWClient Client;
        int ChatHistoryNumber = 0;
        int UserNumber = 0;

        ArrayList<String> ChatHistory = new ArrayList<String>();
        ArrayList<String> Users = new ArrayList<String>();

        IInputReceiver myReceiver;
        String textandnick = "";

        public CChatField(MWClient client) {
            Client = client;
            this.addActionListener(this);
            this.addKeyListener(this);
            this.setFocusTraversalKeysEnabled(false);
            this.setBackground(StringUtils.html2Color(mwclient.getConfigParam("BACKGROUNDCOLOR")));
            this.setForeground(StringUtils.html2Color(mwclient.getConfigParam("CHATFONTCOLOR")));
            this.setCaretColor(StringUtils.html2Color(mwclient.getConfigParam("CHATFONTCOLOR")));
        }

        public void setReceiver(IInputReceiver receiver) {
            this.myReceiver = receiver;
        }

        public String parseOutUserName(String text) {

            // there are spaces in the text so get the last word
            if (text.trim().indexOf(" ") != -1)
                text = text.substring(0, text.trim().lastIndexOf(" ")).trim();

            // The name is the first word.
            else
                text = "";

            return text;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            ChatHistory.add(getText());
            ChatHistoryNumber = 0;
            if (this.myReceiver != null) {
                if (myReceiver.processInput(getText())) {
                    setText("");
                }
            }
        }

        public void keyPressed(KeyEvent e) {

            if (e.getKeyCode() == KeyEvent.VK_UP) {
                UserNumber = 0;
                if (ChatHistoryNumber < ChatHistory.size()) {
                    ChatHistoryNumber++;
                    setText(ChatHistory.get(ChatHistory.size() - ChatHistoryNumber));
                }
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                UserNumber = 0;
                if (ChatHistoryNumber > 0) {
                    ChatHistoryNumber--;
                    if (ChatHistoryNumber == 0) {
                        setText("");
                    } else {
                        setText(ChatHistory.get(ChatHistory.size() - ChatHistoryNumber));
                    }
                }
            } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                String messageText = "";
                if (UserNumber == 0) {
                    Users = Client.getPartialUser(getText());
                    // No users where found keep the UserNumber at 0 and
                    // wait for the next key press.
                    if (Users == null || Users.isEmpty())
                        return;
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
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {

                if (CommTPane.getSelectedIndex() == CommTPane.indexOfComponent(ErrorLogPanel)) {
                    CommTPane.remove(CommTPane.getSelectedComponent());
                } else if (mwclient.getConfig().isParam("USEMULTIPLEPM") && CommTPane.getSelectedComponent() instanceof JPanel) {
                    JPanel panel = (JPanel) CommTPane.getSelectedComponent();
                    if (panel.getName() != null && panel.getName().startsWith("Mail Tab ")) {
                        CommTPane.remove(panel);
                        panel = null;
                    }
                }
            } else
                UserNumber = 0;

        }

        public void keyReleased(KeyEvent e) {
            // filler
        }

        public void keyTyped(KeyEvent e) {
            // filler
        }
    }

    // receiver interface
    public static interface IInputReceiver {
        public boolean processInput(String input);
    }

    // receiver interface
    public void mouseClicked(MouseEvent e) {

        // only close from right clicks
        if (e.getButton() != MouseEvent.BUTTON3) {
            return;
        }

        if (e.getSource() instanceof JEditorPane) {
            JPopupMenu clipboard = new JPopupMenu();

            JEditorPane pane = (JEditorPane) e.getSource();

            pane.requestFocusInWindow();
            // Information
            JMenuItem copy = new JMenuItem("Copy");
            if (pane.getSelectionStart() == pane.getSelectionEnd())
                copy.setActionCommand("");
            else
                copy.setActionCommand(pane.getSelectedText());
            copy.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    StringSelection ss = new StringSelection(ae.getActionCommand());
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(ss, ss);
                }
            });

            clipboard.add(copy);

            clipboard.addSeparator();

            copy = new JMenuItem("Select All");
            copy.setActionCommand(pane.getName());
            copy.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        int index = Integer.parseInt(ae.getActionCommand());
                        JEditorPane pane = getEditorPane(index);
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

        if (e.getSource() instanceof CChatField) {
            JPopupMenu clipboard = new JPopupMenu();
            JMenuItem copy = new JMenuItem("Cut");

            copy.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    StringSelection ss = new StringSelection(chatField.getSelectedText());
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(ss, ss);
                    try {
                        String newText = chatField.getText(0, chatField.getSelectionStart());
                        newText += chatField.getText(chatField.getSelectionEnd(), chatField.getText().length() - chatField.getSelectionEnd());
                        chatField.setText(newText);
                    } catch (Exception ex) {
                        MWLogger.errLog(ex);
                    }
                }
            });

            clipboard.add(copy);

            copy = new JMenuItem("Copy");
            copy.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    StringSelection ss = new StringSelection(chatField.getSelectedText());
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(ss, ss);
                }
            });

            clipboard.add(copy);

            copy = new JMenuItem("Paste");
            copy.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    String clipping = "";
                    Transferable data = clipboard.getContents(this);

                    try {
                        clipping = (String) data.getTransferData(DataFlavor.stringFlavor);
                    } catch (Exception ex) {
                        clipping = data.toString();
                        MWLogger.errLog(ex);
                    }
                    chatField.setText(chatField.getText() + clipping);
                }
            });

            clipboard.add(copy);

            copy = new JMenuItem("Delete");
            copy.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        String newText = chatField.getText(0, chatField.getSelectionStart());
                        newText += chatField.getText(chatField.getSelectionEnd(), chatField.getText().length() - chatField.getSelectionEnd());
                        chatField.setText(newText);
                    } catch (Exception ex) {
                        MWLogger.errLog(ex);
                    }
                }
            });

            clipboard.add(copy);

            clipboard.addSeparator();

            copy = new JMenuItem("Select All");
            copy.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
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
            JPopupMenu popup = new JPopupMenu();
            JMenuItem info = new JMenuItem("Close");
            info.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    CommTPane.remove(CommTPane.getSelectedComponent());
                }
            });
            popup.add(info);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }

        // offer to close mail tabs
        if (mwclient.getConfig().isParam("USEMULTIPLEPM") && CommTPane.getSelectedComponent() instanceof JPanel) {
            JPanel panel = (JPanel) CommTPane.getSelectedComponent();
            if (panel != null && panel.getName().startsWith("Mail Tab ")) {
                JPopupMenu popup = new JPopupMenu();
                JMenuItem info = new JMenuItem("Close");
                info.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        CommTPane.remove(CommTPane.getSelectedComponent());
                    }
                });
                popup.add(info);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    public void mousePressed(MouseEvent arg0) {
    }

    public void mouseReleased(MouseEvent arg0) {
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }

    public void reload() {

        int index;
        int mnemo;
        String tabText = "";

        String mnemonicText;
        tabText = mwclient.getConfig().getParam("MAINCHANNELTABNAME");
        mnemonicText = mwclient.getConfig().getParam("MAINCHANNELMNEMONIC");

        CommTPane.addTab(tabText, null, MChannelPanel, "Interfaction Communication Channel (Alt + " + mnemonicText + ")");
        index = CommTPane.indexOfComponent(MChannelPanel);
        mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
        if (mnemo == -1)
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
        CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()), "MChannelSelect");
        getActionMap().put("MChannelSelect", MChannelSelect);

        if (mwclient.getConfig().isParam("HOUSEMAILVISIBLE")) {

            tabText = mwclient.getConfig().getParam("HOUSEMAILTABNAME");
            mnemonicText = mwclient.getConfig().getParam("HOUSEMAILMNEMONIC");

            CommTPane.addTab(tabText, null, HMailPanel, "House Communication Channel (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(HMailPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1)
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            if (HMailSelect == null)
                HMailSelect = new CSelectTabAction(HMailPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()), "HMailSelect");
            getActionMap().put("HMailSelect", HMailSelect);
        }

        if (!mwclient.getConfig().isParam("USEMULTIPLEPM")) {

            if (mwclient.getConfig().isParam("PRIVATEMAILVISIBLE")) {
                tabText = mwclient.getConfig().getParam("PRIVATEMAILTABNAME");
                mnemonicText = mwclient.getConfig().getParam("PRIVATEMAILMNEMONIC");

                CommTPane.addTab(tabText, null, PMailPanel, "Private Mail (Alt + " + mnemonicText.toUpperCase() + ")");
                index = CommTPane.indexOfComponent(PMailPanel);
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
                if (mnemo == -1)
                    mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
                CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
                if (PMailSelect == null)
                    PMailSelect = new CSelectTabAction(PMailPanel);
                getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()), "PMailSelect");
                getActionMap().put("PMailSelect", PMailSelect);

            }
        }

        if (mwclient.getConfig().isParam("PERSONALLOGVISIBLE")) {

            tabText = mwclient.getConfig().getParam("PERSONALLOGTABNAME");
            mnemonicText = mwclient.getConfig().getParam("PERSONALLOGMNEMONIC");

            CommTPane.addTab(tabText, null, PLogPanel, "Logged Messages (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(PLogPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1)
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            if (PLogSelect == null)
                PLogSelect = new CSelectTabAction(PLogPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()), "PLogSelect (Alt + " + mnemonicText.toUpperCase() + ")");
            getActionMap().put("PLogSelect", PLogSelect);
        }

        if (mwclient.getConfig().isParam("SYSTEMLOGVISIBLE")) {

            tabText = mwclient.getConfig().getParam("SYSTEMLOGTABNAME");
            mnemonicText = mwclient.getConfig().getParam("SYSTEMLOGMNEMONIC");

            CommTPane.addTab(tabText, null, SLogPanel, "System Messages (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(SLogPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1)
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            if (SLogSelect == null)
                SLogSelect = new CSelectTabAction(SLogPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()), "SLogSelect");
            getActionMap().put("SLogSelect", SLogSelect);
        }

        /* Misc-Channel */
        if (mwclient.getConfig().isParam("MISCELLANEOUSVISIBLE")) {

            tabText = mwclient.getConfig().getParam("MISCELLANEOUSTABNAME");
            mnemonicText = mwclient.getConfig().getParam("MISCELLANEOUSMNEMONIC");

            CommTPane.addTab(tabText, null, MiscChannelPanel, "Miscellaneous Stuff (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(MiscChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1)
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            if (MiscChannelSelect == null)
                MiscChannelSelect = new CSelectTabAction(MiscChannelPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()), "MiscChannelSelect");
            getActionMap().put("MiscChannelSelect", MiscChannelSelect);
        }
        /* RPG-Channel */
        if (mwclient.getConfig().isParam("RPGVISIBLE")) {

            tabText = mwclient.getConfig().getParam("RPGTABNAME");
            mnemonicText = mwclient.getConfig().getParam("RPGMNEMONIC");

            CommTPane.addTab(tabText, null, RPGChannelPanel, "RP (Alt + " + mnemonicText.toUpperCase() + ")");
            index = CommTPane.indexOfComponent(RPGChannelPanel);
            mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toUpperCase());
            if (mnemo == -1)
                mnemo = CommTPane.getTitleAt(index).indexOf(mnemonicText.toLowerCase());
            CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
            RPGChannelSelect = new CSelectTabAction(RPGChannelPanel);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt " + mnemonicText.toUpperCase()), "RPGChannelSelect");
            getActionMap().put("RPGChannelSelect", RPGChannelSelect);
        }

    }

    private void processFunctionKeyCommand(String command) {
        StringTokenizer commands = new StringTokenizer(command, ";");

        while (commands.hasMoreTokens())
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c " + commands.nextToken());
    }
}
