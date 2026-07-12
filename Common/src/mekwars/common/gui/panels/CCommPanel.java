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
 * The Communications panel is the bottom/chat area of the MekWars client's main game window. It hosts a
 * {@link JTabbedPane} ({@link #CommTPane}) in which each tab is a separate chat "channel" (main faction chat, house
 * mail, private mail, a personal message log, a system message log, a miscellaneous channel, an in-character RPG
 * channel, moderator mail and an error log), plus any number of dynamically created private-mail sub-tabs when the
 * server is configured for multiple PM tabs. Each channel tab renders incoming, server-formatted HTML text in a
 * read-only {@link JEditorPane}; the user reads chat/game messages there and types replies or slash-style commands
 * into the single {@link CChatField} docked at the bottom of the panel ({@link #chatField}).
 * <p>
 * Which channels are actually shown, their tab labels, and their keyboard mnemonics are all driven by server/client
 * configuration parameters (e.g. {@code HOUSE_MAIL_VISIBLE}, {@code MAIN_CHANNEL_TAB_NAME}) rather than being
 * hard-coded, so most of the constructor is repetitive "create tab, wire listeners, register Alt+mnemonic
 * shortcut" boilerplate performed once per optional channel. Alt+Z / Shift+Alt+Z cycle tabs forward/back, and F1-F5
 * fire user-configurable chat macros (see {@link #processFunctionKeyCommand(String)}).
 */
public class CCommPanel extends JPanel implements ChangeListener, ComponentListener, MouseListener {
    /** Tab index/id for the main (global faction) chat channel. */
    public static final int CHANNEL_MAIN = 0;
    /** Tab index/id for the house (sub-faction) mail channel. */
    public static final int CHANNEL_HOUSE_MAIL = 1;
    /** Tab index/id for the private mail channel (single-tab mode) or the PM channel family (multi-tab mode). */
    public static final int CHANNEL_PRIVATE_MAIL = 2;
    /** Tab index/id for the player's personal log channel (messages logged for the player only). */
    public static final int CHANNEL_PERSONAL_LOG = 3;
    /** Tab index/id for the system log channel (server/system announcements). */
    public static final int CHANNEL_SYSTEM_LOG = 4;
    /** Tab index/id for the miscellaneous channel. */
    public static final int CHANNEL_MISC = 5;
    /** Tab index/id for the in-character roleplay (RPG) channel. */
    public static final int CHANNEL_RPG = 6;
    /** Tab index/id for the moderator mail channel; the tab is created lazily on first use, see {@link #createModTab()}. */
    public static final int CHANNEL_MOD = 7;
    /** Tab index/id for the error log channel; the tab is created lazily on first use, see {@link #createErrorTab()}. */
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
    @Serial
    private static final long serialVersionUID = 8754254920729491806L;
    /** The client/campaign connection this panel is bound to; used to read config and dispatch outgoing chat. */
    IClient client;
    /** The tabbed pane holding one component per visible channel; tabs sit at the bottom of the panel. */
    JTabbedPane CommTPane = new JTabbedPane(SwingConstants.BOTTOM);
    /** Shared HTML editor kit used by every channel's {@link JEditorPane} so chat markup renders consistently. */
    MyHTMLEditorKit kit = new MyHTMLEditorKit();
    /** Container for the main channel's scroll pane. */
    JPanel MChannelPanel = new JPanel();
    /** Action bound to the main channel's Alt+mnemonic shortcut, selecting {@link #MChannelPanel} when fired. */
    CCommPanel.CSelectTabAction MChannelSelect = null;
    /** Read-only HTML display for the main channel's chat text. */
    JEditorPane MChannelEPane = new JEditorPane("text/html", "");
    /** Scroll pane wrapping {@link #MChannelEPane}. */
    JScrollPane MChannelSPane = new JScrollPane();
    /** Container for the house mail channel's scroll pane; only added as a tab when {@code HOUSE_MAIL_VISIBLE}. */
    JPanel HMailPanel = new JPanel();
    /** Action bound to the house mail channel's Alt+mnemonic shortcut. */
    CCommPanel.CSelectTabAction HMailSelect = null;
    /** Read-only HTML display for the house mail channel's text. */
    JEditorPane HMailEPane = new JEditorPane("text/html", "");
    /** Scroll pane wrapping {@link #HMailEPane}. */
    JScrollPane HMailSPane = new JScrollPane();
    /**
     * Container for the single-tab private mail channel; only populated/added when the server is NOT using
     * multiple PM tabs ({@code USE_MULTIPLE_PM} is false). When multiple PM tabs are enabled, private mail instead
     * lives in dynamically created "Mail Tab &lt;name&gt;" panels (see {@link #createMailTab(String)}).
     */
    JPanel PMailPanel = new JPanel();
    /** Action bound to the (single) private mail channel's Alt+mnemonic shortcut. */
    CCommPanel.CSelectTabAction PMailSelect = null;
    /** Read-only HTML display for the single-tab private mail channel's text. */
    JEditorPane PMailEPane = new JEditorPane("text/html", "");
    /** Scroll pane wrapping {@link #PMailEPane}. */
    JScrollPane PMailSPane = new JScrollPane();
    /** Container for the player's personal log channel's scroll pane; only added as a tab when {@code PERSONAL_LOG_VISIBLE}. */
    JPanel PLogPanel = new JPanel();
    /** Action bound to the personal log channel's Alt+mnemonic shortcut. */
    CCommPanel.CSelectTabAction PLogSelect = null;
    /** Read-only HTML display for the personal log channel's text. */
    JEditorPane PLogEPane = new JEditorPane("text/html", "");
    /** Scroll pane wrapping {@link #PLogEPane}. */
    JScrollPane PLogSPane = new JScrollPane();
    /** Container for the system log channel's scroll pane; only added as a tab when {@code SYSTEM_LOG_VISIBLE}. */
    JPanel SLogPanel = new JPanel();
    /** Action bound to the system log channel's Alt+mnemonic shortcut. */
    CCommPanel.CSelectTabAction SLogSelect = null;
    /** Read-only HTML display for the system log channel's text. */
    JEditorPane SLogEPane = new JEditorPane("text/html", "");
    /** Scroll pane wrapping {@link #SLogEPane}. */
    JScrollPane SLogSPane = new JScrollPane();
    /** Container for the miscellaneous channel's scroll pane; only added as a tab when {@code MISCELLANEOUS_VISIBLE}. */
    JPanel MiscChannelPanel = new JPanel();
    /** Action bound to the miscellaneous channel's Alt+mnemonic shortcut. */
    CCommPanel.CSelectTabAction MiscChannelSelect = null;
    /** Read-only HTML display for the miscellaneous channel's text. */
    JEditorPane MiscChannelEPane = new JEditorPane("text/html", "");
    /** Scroll pane wrapping {@link #MiscChannelEPane}. */
    JScrollPane MiscChannelSPane = new JScrollPane();
    /** Container for the moderator mail channel; not added as a tab until {@link #createModTab()} runs (lazy, on first mod message). */
    JPanel ModMailPanel = new JPanel();
    /** Action bound to the moderator mail channel's Alt+mnemonic shortcut; created in {@link #createModTab()}. */
    CCommPanel.CSelectTabAction ModMailSelect = null;
    /** Read-only HTML display for the moderator mail channel's text. */
    JEditorPane ModMailEPane = new JEditorPane("text/html", "");
    /** Scroll pane wrapping {@link #ModMailEPane}. */
    JScrollPane ModMailSPane = new JScrollPane();
    /** Container for the error log channel; not added as a tab until {@link #createErrorTab()} runs (lazy, on first logged error). */
    JPanel ErrorLogPanel = new JPanel();
    /** Action bound to the error log channel's Alt+mnemonic shortcut; created in {@link #createErrorTab()}. */
    CCommPanel.CSelectTabAction ErrorLogSelect = null;
    /** Read-only HTML display for the error log channel's text. */
    JEditorPane ErrorLogEPane = new JEditorPane("text/html", "");
    /** Scroll pane wrapping {@link #ErrorLogEPane}. */
    JScrollPane ErrorLogSPane = new JScrollPane();
    /** Container for the in-character RPG channel's scroll pane; only added as a tab when {@code RPG_VISIBLE}. */
    JPanel RPGChannelPanel = new JPanel();
    /** Action bound to the RPG channel's Alt+mnemonic shortcut. */
    CCommPanel.CSelectTabAction RPGChannelSelect = null;
    /** Read-only HTML display for the RPG channel's text. */
    JEditorPane RPGChannelEPane = new JEditorPane("text/html", "");
    /** Scroll pane wrapping {@link #RPGChannelEPane}. */
    JScrollPane RPGChannelSPane = new JScrollPane();
    /** The single chat input field docked at the bottom of the panel; see {@link #sendChat(String)} for what happens to submitted text. */
    CChatField chatField;
    /** Foreground color applied to a tab's title text while it has unread ("*"-flagged) activity. */
    Color TabForeground;
    /** Unused background color captured at construction time for tabs with unread activity (declared but never assigned to a tab). */
    Color TabBackground;
    /** Action wired to Alt+Z that advances {@link #CommTPane} to the next enabled tab. */
    CCommPanel.CTabForwardAction ForwardCommTab;
    /** Action wired to Shift+Alt+Z that moves {@link #CommTPane} to the previous enabled tab. */
    CCommPanel.CTabBackwardAction BackwardCommTab;
    /**
     * Whether incoming text should auto-scroll each channel to the bottom. Mirrors the {@code AUTOSCROLL} config
     * parameter at construction time, but can be toggled at runtime via {@link #setAutoTextUpdate(boolean)}; read by
     * {@link #setChat(String, int, String)} to decide whether to preserve the user's current scroll position.
     */
    private boolean autoTextUpdate;

    /**
     * Builds the full communications panel: constructs every channel's editor pane/scroll pane/tab, wires the
     * shared hyperlink listener, registers each visible channel's Alt+mnemonic keyboard shortcut, adds the chat
     * input field, and binds tab-cycling (Alt+Z / Shift+Alt+Z) and function-key macro (F1-F5) shortcuts. Which
     * optional channels (house mail, RPG, private mail, personal log, system log, misc) are actually created as
     * tabs depends on the corresponding {@code *_VISIBLE} config flags; moderator mail and the error log are never
     * added here and instead appear later, lazily, via {@link #createModTab()}/{@link #createErrorTab()}.
     *
     * @param client the client/campaign connection supplying config values and chat routing
     */
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

    /** @return whether channels are currently configured to auto-scroll to the bottom on new text. */
    public boolean getAutoTextUpdate() {
        return autoTextUpdate;
    }

    /** Enables or disables auto-scroll-to-bottom behavior for all channels at runtime. */
    public void setAutoTextUpdate(boolean newValue) {
        autoTextUpdate = newValue;
    }

    /**
     * Receiver callback invoked by {@link CChatField} whenever the user submits text from the chat input field
     * (wired via {@code chatField.setReceiver(this::sendChat)} in the constructor). Depending on which tab is
     * currently selected, this rewrites the raw text {@code s} into the appropriate slash-command form expected by
     * the server before forwarding it to {@link IClient#processGUIInput(String)}:
     * <ul>
     * <li>House mail tab -&gt; prefixed as a house-mail command ({@code c hm#...}).</li>
     * <li>Moderator mail tab -&gt; prefixed as a mod-mail command ({@code c mm#...}).</li>
     * <li>RPG tab -&gt; prefixed as an in-character command ({@code c ic#...}).</li>
     * <li>Single private-mail tab -&gt; rewritten into a {@code mail <receiver>,<text>} command, using the last
     * queried player as the implicit receiver; if there is no last-query receiver, the send is aborted, an info
     * window is shown, and the original text is restored into the input field.</li>
     * <li>A dynamic "Mail Tab &lt;name&gt;" tab (multi-PM mode) -&gt; same as above, but the receiver is parsed out
     * of the tab's component name rather than the last query.</li>
     * <li>Text starting with the "/me" or "/c me" emote prefixes gets an extra {@code |ic}, {@code |mm}, {@code |hm}
     * or {@code |mail[|receiver]} suffix appended depending on the active tab, again failing back to showing an
     * info window if a private-mail receiver can't be determined.</li>
     * </ul>
     * Text that already starts with {@link IClient#GUI_PREFIX} is left untouched by the channel-specific rewriting
     * (it's assumed to already be a fully-formed client command). Whatever the transformed text ends up being, it is
     * unconditionally sent via {@link IClient#processGUIInput(String)} at the end of the method.
     *
     * @param s the raw text typed by the user
     * @return {@code true} if the (possibly rewritten) text was forwarded to the client; {@code false} if sending
     *         was aborted because a private-mail receiver could not be resolved
     */
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

    /**
     * @return the number of dynamically created private-mail tabs currently open, identified by component name
     *         prefix {@code "Mail Tab "} (only relevant when {@code USE_MULTIPLE_PM} is enabled).
     */
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

    /**
     * Creates and adds a new private-mail tab for conversing with a specific player, used when the server is
     * configured for multiple PM tabs ({@code USE_MULTIPLE_PM}). The new tab's underlying panel is named
     * {@code "Mail Tab <tabName>"} so it can later be located by {@link #findMailTab(String)} and so
     * {@link #sendChat(String)}/{@link #mouseClicked(MouseEvent)} can recognize it as a mail tab. The tab is given
     * the next free numeric mnemonic slot (see {@link #getNextMailTabNumber()}) as both its Alt+N shortcut and a
     * "N. name" label prefix.
     *
     * @param tabName the display name (typically the other player's name) for this conversation tab
     */
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
        // Note: variable name "LogSelect" is a copy/paste leftover from the log-tab setup code above; it actually
        // selects this new mail tab, not a log tab.
        CCommPanel.CSelectTabAction LogSelect = new CCommPanel.CSelectTabAction(
              newPanel);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(String.format("alt %s", mnemonic.toUpperCase())),
              String.format("Mail From %s", tabName));
        getActionMap().put(String.format("Mail From %s", tabName), LogSelect);
    }

    /**
     * Scans existing "Mail Tab " panels to find the lowest-numbered free slot (1..{@code MAX_PM_TABS}) for a new
     * private-mail tab, so tab numbers/mnemonics can be reused after a tab is closed. If every slot up to
     * {@code MAX_PM_TABS} is occupied, returns {@code MAX_PM_TABS + 1} (i.e. one past the configured maximum,
     * since the loop simply exits without finding a gap).
     *
     * @return the lowest available mail-tab number
     */
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

    /** @return the text currently sitting in the chat input field (not yet submitted). */
    public String getInput() {
        return chatField.getText();
    }

    /** Overwrites the chat input field's text, e.g. to restore a message after a failed {@link #sendChat(String)}. */
    public void setInput(String input) {
        chatField.setText(input);
    }

    /** Convenience overload of {@link #setChat(String, int, String)} for channels that don't need a mail-tab name. */
    public void setChat(String s, int channel) {
        setChat(s, channel, null);
    }

    /**
     * Appends server-supplied HTML text {@code s} to the given channel's display and, if that channel is a
     * currently-hidden tab (per its {@code *_VISIBLE} config flag), instead logs the text to disk via
     * {@link #printCommLog(String, int)} and returns without touching the UI. Moderator mail and error log tabs are
     * created on demand here if they don't exist yet. Side effects on the target tab/editor pane:
     * <ul>
     * <li>If the target tab isn't the currently selected tab, its title gets a leading {@code "*"} unread marker
     * and its foreground color is switched to {@link #TabForeground} (skipped for the personal/system log tabs).</li>
     * <li>If auto-scroll is disabled and the channel's scrollbar isn't already near the bottom (within ~97% of
     * max), the new text is appended without moving the scroll position or caret, so the user doesn't lose their
     * place; otherwise the view auto-scrolls to the newly appended text.</li>
     * <li>A trailing {@code <br>} on {@code s}, if present, is stripped before insertion (the HTML editor kit adds
     * its own line break structure).</li>
     * <li>If the channel's document exceeds {@link #MAX_BUFFER} characters, the oldest characters are trimmed back
     * down to {@link #CAP_BUFFER_AMOUNT} to bound memory/render cost.</li>
     * </ul>
     * Comment left by a previous maintainer: "this method needs some tuning (carat and scrollbar issues)".
     *
     * @param s       the HTML-formatted text to display/log
     * @param channel one of the {@code CHANNEL_*} constants identifying the destination channel
     * @param mailTab for {@link #CHANNEL_PRIVATE_MAIL} in multi-PM mode, the name of the specific mail tab to
     *                target; ignored for all other channels. If {@code null} while multi-PM mode is active, the
     *                message is silently dropped.
     */
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

    /**
     * Appends {@code s} (with a trailing {@code <br>}) to a per-channel HTML log file under {@code ./logs/}, used as
     * a fallback when {@link #setChat(String, int, String)} is called for a channel whose tab is currently hidden
     * (so the message isn't lost, just not shown live). Each channel maps to its own fixed file name (e.g.
     * {@code Main.html}, {@code HouseMail.html}); channels with no matching branch fall through with an empty
     * {@code filePath}, which will cause the {@link java.io.FileOutputStream} constructor to throw and the failure
     * to be logged rather than crash the caller. The file is opened in append mode and closed immediately after
     * each write.
     *
     * @param s       the HTML text to log
     * @param channel one of the {@code CHANNEL_*} constants selecting the destination log file
     */
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

    /**
     * Looks up the dynamically-created private-mail tab panel whose name exactly matches {@code "Mail Tab <tabName>"}.
     *
     * @param tabName the conversation name the tab was created with (see {@link #createMailTab(String)})
     * @return the matching tab panel, or {@code null} if no such tab is currently open
     */
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

    /**
     * Lazily adds the "Mod Mail" tab (Alt+O) to {@link #CommTPane} the first time a moderator-mail message needs to
     * be displayed. Unlike most other channel setup in the constructor, this tab has no visibility config flag; it
     * simply doesn't exist until the first {@code CHANNEL_MOD} message arrives via {@link #setChat(String, int, String)}.
     */
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

    /**
     * Lazily adds the "Error Log" tab (Alt+R) to {@link #CommTPane} the first time an error needs to be displayed
     * to the user; like {@link #createModTab()}, this tab has no visibility config flag and simply doesn't exist
     * until first needed.
     */
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

    /**
     * Resolves the read-only HTML display for a given channel, dereferencing into a dynamic mail tab when
     * necessary.
     *
     * @param channel one of the {@code CHANNEL_*} constants
     * @param mailTab for {@link #CHANNEL_PRIVATE_MAIL} under {@code USE_MULTIPLE_PM}, the name of the specific mail
     *                tab to resolve; ignored otherwise
     * @return the matching {@link JEditorPane}, or {@code null} if the channel is unrecognized or (for multi-PM
     *         mode) no matching mail tab/scroll pane could be found
     */
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

    /**
     * Resolves the scroll pane wrapping a given channel's editor pane, dereferencing into a dynamic mail tab when
     * necessary. Used by {@link #setChat(String, int, String)} to inspect/preserve scrollbar position.
     *
     * @param channel one of the {@code CHANNEL_*} constants
     * @param tabName for {@link #CHANNEL_PRIVATE_MAIL} under {@code USE_MULTIPLE_PM}, the name of the specific mail
     *                tab to resolve; ignored otherwise
     * @return the matching {@link JScrollPane}, or {@code null} if unresolved
     */
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

    /** @return the chat input field, as a plain {@link JTextField} reference. */
    public JTextField getInputField() {
        return chatField;
    }

    /** Moves keyboard focus to the chat input field. */
    public void focusInputField() {
        chatField.requestFocusInWindow(); // pass focus to input field
    }

    /** @return the index of the given tab panel within {@link #CommTPane}, or -1 if it isn't a tab. */
    public int getTabIndex(JPanel panel) {
        return CommTPane.indexOfComponent(panel);
    }

    /**
     * {@link ChangeListener} callback fired whenever {@link #CommTPane}'s selected tab changes. When the user
     * switches to a tab whose title carries the unread {@code "*"} marker (set by
     * {@link #setChat(String, int, String)}), the marker is stripped, the mnemonic index is shifted back by one to
     * compensate for the removed leading character, and the tab's foreground/background colors are reset to
     * defaults ({@code null}). Focus is always returned to the chat input field afterward so the user can keep
     * typing without clicking back into it.
     */
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

    /**
     * {@link ComponentListener} callback fired when this panel is resized; forces every channel's scrollbar down
     * to the bottom so text doesn't appear to "jump" oddly after a resize.
     * <p>
     * <b>Quirk/potential bug:</b> the loop variable {@code i} (0..{@link JTabbedPane#getTabCount()}-1) is the raw,
     * positional tab index inside {@link #CommTPane}, but it is passed straight into {@link #getScrollPane(int)}/
     * {@link #getEditorPane(int)}, whose {@code channel} parameter is meant to be one of the {@code CHANNEL_*}
     * constants. This only lines up correctly when every optional channel happens to be visible and tabs were added
     * in exactly {@code CHANNEL_*} numeric order; if any optional channel (house mail, private mail, personal log,
     * etc.) is hidden via its {@code *_VISIBLE} config flag, the tab actually occupying a given position will not
     * match the channel constant of that same value, so this method can silently scroll/caret-jump the wrong
     * channel's pane (or, if {@code getScrollPane}/{@code getEditorPane} return null for that mismatched constant,
     * simply skip a tab that should have been scrolled).
     */
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

    /** Single-channel overload of {@link #getScrollPane(int, String)} for channels that aren't multi-PM tabs. */
    public JScrollPane getScrollPane(int channel) {
        return getScrollPane(channel, null);
    }

    /** Single-channel overload of {@link #getEditorPane(int, String)} for channels that aren't multi-PM tabs. */
    public JEditorPane getEditorPane(int channel) {
        return getEditorPane(channel, null);
    }

    /** No-op {@link ComponentListener} callback; this panel doesn't react to being moved. */
    public void componentMoved(ComponentEvent componentEvent) {
    }

    /** No-op {@link ComponentListener} callback; this panel doesn't react to being shown. */
    public void componentShown(ComponentEvent componentEvent) {
    }

    /** No-op {@link ComponentListener} callback; this panel doesn't react to being hidden. */
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

    /**
     * {@link MouseListener} callback shared by every channel's editor pane, the chat input field, and
     * {@link #CommTPane} itself (all register {@code this} as their mouse listener). Only right-clicks
     * ({@link MouseEvent#BUTTON3}) are handled; left/middle clicks are ignored. Behavior depends on the click
     * source:
     * <ul>
     * <li>Right-click on a channel's {@link JEditorPane}: shows a "Copy" / "Select All" context menu. "Copy" copies
     * the current text selection (or an empty string if nothing is selected) to the system clipboard. "Select All"
     * re-resolves the target editor pane by parsing the channel id out of the pane's {@code name} (set in the
     * constructor via {@code setName(Integer.toString(CHANNEL_*))}) and selects all of its text.</li>
     * <li>Right-click on the {@link #chatField}: shows the cut/copy/paste/delete/select-all context menu built by
     * {@link #getClipboard()}.</li>
     * <li>Right-click directly on {@link #CommTPane} (the tab strip) while the error-log tab or (in multi-PM mode)
     * a dynamic mail tab is selected: shows a single "Close" menu item that removes that tab from the pane.</li>
     * </ul>
     * The leading {@code // receiver interface} comment above this method is a section marker left by a previous
     * maintainer grouping the {@link MouseListener} implementation methods together, not a reference to an actual
     * "receiver interface" type.
     */
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

    /**
     * Builds a small Cut/Copy/Paste/Delete/Select All context menu for {@link #chatField}, shown by
     * {@link #mouseClicked(MouseEvent)} on right-click. "Cut" and "Delete" both manually splice the field's text
     * around the current selection (there's no {@code cut()}/selection-replace API used here); "Cut" additionally
     * copies the removed text to the system clipboard first. Quirk: "Paste" always appends the clipboard contents
     * to the end of the field's text rather than inserting at the caret position or replacing the current
     * selection, so pasting in the middle of existing text will not behave as a user would typically expect from a
     * text field.
     *
     * @return a freshly-built popup menu wired to {@link #chatField}
     */
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

    /** No-op {@link MouseListener} callback; only {@link #mouseClicked(MouseEvent)} is used. */
    public void mousePressed(MouseEvent mouseEvent) {
    }

    /** No-op {@link MouseListener} callback; only {@link #mouseClicked(MouseEvent)} is used. */
    public void mouseReleased(MouseEvent mouseEvent) {
    }

    /** No-op {@link MouseListener} callback; only {@link #mouseClicked(MouseEvent)} is used. */
    public void mouseEntered(MouseEvent mouseEvent) {
    }

    /** No-op {@link MouseListener} callback; only {@link #mouseClicked(MouseEvent)} is used. */
    public void mouseExited(MouseEvent mouseEvent) {
    }

    /**
     * Re-runs the tab-creation logic for every {@code *_VISIBLE}-gated channel (main, house mail, private mail,
     * personal log, system log, misc, RPG), presumably to refresh tab labels/mnemonics/visibility after the
     * client's configuration has changed at runtime. Each {@code *Select} action field is only (re)created when it
     * is still {@code null} (e.g. {@link #HMailSelect}, {@link #PMailSelect}, {@link #PLogSelect},
     * {@link #SLogSelect}, {@link #MiscChannelSelect}) &mdash; except {@link #MChannelSelect} (implicitly reused
     * without a null check further down) and {@link #RPGChannelSelect}, which are unconditionally reassigned every
     * call.
     * <p>
     * <b>Quirk/potential bug:</b> this method calls {@link JTabbedPane#addTab} again for panels that may already be
     * showing as tabs (added originally in the constructor), rather than removing and rebuilding the tab set first.
     * Because a {@link java.awt.Component} can only belong to one parent/position at a time, Swing will silently
     * detach an already-added panel from its old tab slot and re-insert it, which has the practical effect of
     * re-ordering the visible tabs (moving each reloaded channel to the end) every time {@code reload()} runs,
     * rather than leaving tab order untouched.
     */
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

    /**
     * Splits {@code command} on {@code ';'} and sends each piece to the server as a campaign-prefixed chat command
     * ({@code c <token>}). Used by the F1-F5 macro key actions below to fire off one or more pre-configured
     * commands (e.g. common RP or admin actions) bound to a function key via the {@code F1BIND}..{@code F5BIND}
     * config parameters.
     *
     * @param command the semicolon-separated list of command strings to send
     */
    private void processFunctionKeyCommand(String command) {
        StringTokenizer commands = new StringTokenizer(command, ";");

        while (commands.hasMoreTokens()) {
            client.sendChat(String.format("%sc %s", IClient.CAMPAIGN_PREFIX, commands.nextToken()));
        }
    }

    // component listener actions
    /** Action bound to Alt+Z: advances {@link #CommTPane} to the next enabled tab, wrapping around at the end. */
    private class CTabForwardAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = -7910457998205249026L;

        public CTabForwardAction() {
            // empty constructor
        }

        /** Selects the next enabled tab after the current one, wrapping to index 0 if needed. No-op if fewer than 2 tabs exist. */
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

    /** Action bound to Shift+Alt+Z: moves {@link #CommTPane} to the previous enabled tab, wrapping around at the start. */
    private class CTabBackwardAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = -880460003608846342L;

        public CTabBackwardAction() {
            // empty constructor
        }

        /** Selects the enabled tab before the current one, wrapping to the last tab if needed. No-op if fewer than 2 tabs exist. */
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

    /**
     * A reusable {@link Action} that, when fired, switches {@link #CommTPane} directly to a specific tab
     * ({@link #Tab}) bound at construction time. One instance is created per channel tab and registered against
     * that channel's Alt+mnemonic keyboard shortcut.
     */
    private class CSelectTabAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = -3297024782997974093L;
        /** The specific tab component this action selects when invoked. */
        private final Component Tab;

        /** @param tab the tab component to select whenever this action fires */
        public CSelectTabAction(Component tab) {
            Tab = tab;
        }

        /** Selects {@link #Tab}, but only if it's currently an enabled tab (no-op otherwise, e.g. if since removed). */
        public void actionPerformed(ActionEvent actionEvent) {
            if (CommTPane.isEnabledAt(CommTPane.indexOfComponent(Tab))) {
                CommTPane.setSelectedComponent(Tab);
            }
        }
    }

    /** Action bound to F1: sends the user-configured {@code F1BIND} macro command(s). */
    private class CF1KeyAction extends AbstractAction {
        @Serial
        private static final long serialVersionUID = -9057337706669800548L;

        public void actionPerformed(ActionEvent actionEvent) {
            processFunctionKeyCommand(client.getConfigParam("F1BIND"));
        }
    }// end CF1Action

    /** Action bound to F2: sends the user-configured {@code F2BIND} macro command(s). */
    private class CF2KeyAction extends AbstractAction {
        @Serial
        private static final long serialVersionUID = 2756143179187978156L;

        public void actionPerformed(ActionEvent actionEvent) {
            processFunctionKeyCommand(client.getConfigParam("F2BIND"));
        }
    }// end CF1Action

    /** Action bound to F3: sends the user-configured {@code F3BIND} macro command(s). */
    private class CF3KeyAction extends AbstractAction {
        @Serial
        private static final long serialVersionUID = -4939591054092680536L;

        public void actionPerformed(ActionEvent actionEvent) {
            processFunctionKeyCommand(client.getConfigParam("F3BIND"));
        }
    }// end CF1Action

    /** Action bound to F4: sends the user-configured {@code F4BIND} macro command(s). */
    private class CF4KeyAction extends AbstractAction {
        @Serial
        private static final long serialVersionUID = -6563867639119041768L;

        public void actionPerformed(ActionEvent actionEvent) {
            processFunctionKeyCommand(client.getConfigParam("F4BIND"));
        }
    }// end CF1Action

    /** Action bound to F5: sends the user-configured {@code F5BIND} macro command(s). */
    private class CF5KeyAction extends AbstractAction {
        @Serial
        private static final long serialVersionUID = 6214466835711778622L;

        public void actionPerformed(ActionEvent actionEvent) {
            processFunctionKeyCommand(client.getConfigParam("F5BIND"));
        }
    }// end CF1Action

}
