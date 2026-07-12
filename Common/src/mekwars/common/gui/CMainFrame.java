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

package mekwars.common.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serial;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.*;

import jakarta.annotation.Nonnull;
import megamek.SuiteConstants;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.codeUtilities.MathUtility;
import megamek.common.equipment.AmmoType;
import megamek.logging.MMLogger;
import mekwars.common.House;
import mekwars.common.Unit;
import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.CCampaign;
import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.gui.dialogs.*;
import mekwars.common.gui.dialogs.buildtableviewer.BuildTableViewer;
import mekwars.common.gui.panels.CMainPanel;
import mekwars.common.sounds.MenuPopupSound;
import mekwars.common.sounds.MenuSound;
import mekwars.common.threads.ClientThread;
import mekwars.common.util.StringUtils;

//import client.gui.dialog.TableViewerDialog;

/**
 * The main application window of the MekWars client (a {@link JFrame}).
 * <p>
 * This class owns the top-level menu bar and every menu item's action handler
 * (methods named like {@code jMenuXyz_actionPerformed}), and hosts the tabbed
 * UI in its content pane via {@link CMainPanel} (see {@code gui.panels}). It
 * is the primary bridge between the Swing UI and the {@link IClient}
 * interface: nearly every action handler in this class either calls a method
 * directly on {@link #client} or builds a campaign command string (prefixed
 * with {@link IClient#CAMPAIGN_PREFIX}) and sends it via
 * {@link IClient#sendChat(String)} for the server to process.
 * <p>
 * Responsibilities of this class include:
 * <ul>
 * <li>Building the entire {@link JMenuBar} (File, Campaign, Attack/Game, Host,
 * Options, Leadership, Help, Emoji, and the dynamically-loaded Mod/Admin/Operations
 * menus) in {@link #createMenu()}.</li>
 * <li>Showing/hiding and enabling/disabling menu items based on the player's
 * current connection/login status and access level, in {@link #enableMenu()}.</li>
 * <li>Handling menu item clicks, most of which pop up a small input dialog
 * (e.g. {@link PlayerNameDialog}, {@link HouseNameDialog}) and then forward a
 * campaign command to the server.</li>
 * <li>Dynamically loading optional admin/moderator/operations-editor menus from
 * external jars ({@code MekWarsAdmin.jar}, {@code MekWarsOpEditor.jar}) via
 * reflection, when present on disk, so that server operators can distribute
 * privileged tooling without shipping it to every client.</li>
 * <li>Window lifecycle: confirming exit while hosting a game, and toggling
 * MegaMek key-press handling based on window focus.</li>
 * </ul>
 * Most Swing component fields below are {@link JMenu}/{@link JMenuItem}
 * instances whose purpose is self-explanatory from their name; only
 * non-obvious state/caches/listeners are individually documented.
 */
public class CMainFrame extends JFrame {
    private static final MMLogger LOGGER = MMLogger.create(CMainFrame.class);

    @Serial
    private static final long serialVersionUID = -1198882220815512476L;

    /** Plays a sound whenever a top-level menu is opened; wired up in {@link #addMenuListener}. */
    private final MenuSound sound;

    /** Plays a (typically different) sound whenever a submenu/popup menu is opened; wired up in {@link #addMenuItemListener}. */
    private final MenuPopupSound popupSound;

    /** The client-side facade to the campaign server/session that this window is displaying; set once in the constructor. */
    public IClient client;
    JPanel contentPane;
    JMenuBar jMenuBar1 = new JMenuBar();
    // FILE Menu
    JMenu jMenuFile = new JMenu();
    JMenuItem jMenuFileConnect = new JMenuItem();
    JMenuItem jMenuFileDisconnect = new JMenuItem();
    JMenuItem jMenuFileRegister = new JMenuItem();
    JMenuItem jMenuFileMail = new JMenuItem();
    JMenuItem jMenuFileLastOnline = new JMenuItem();
    JMenuItem jMenuFileExit = new JMenuItem();
    JMenuItem jMenuFileConfig = new JMenuItem();

    // CAMPAIGN Menu
    JMenu jMenuCampaign = new JMenu();
    JMenu jMenuCampaignSubStatus = new JMenu();// submenu in Campaign
    JMenu jMenuCampaignSubTechs = new JMenu();
    JMenu jMenuCampaignSubBays = new JMenu();
    JMenu jMenuCampaignSubTransfer = new JMenu();
    JMenu jMenuCampaignSubAttack = new JMenu();
    JMenu jMenuCampaignSubMerc = new JMenu();
    JMenu jMenuCampaignSubOther = new JMenu();
    JMenu jMenuCampaignPersonnelTechSubMenu = new JMenu("Techs");
    JMenu jMenuCampaignPersonnelPilotsSubMenu = new JMenu("Pilots");
    JMenuItem jMenuCampaignMyStatus = new JMenuItem();
    JMenuItem jMenuCampaignLogin = new JMenuItem();
    JMenuItem jMenuCampaignActivate = new JMenuItem();
    JMenuItem jMenuCampaignDeactivate = new JMenuItem();
    JMenuItem jMenuCampaignLogout = new JMenuItem();
    JMenuItem jMenuCampaignPlayers = new JMenuItem();
    JMenuItem jMenuCampaignISStatus = new JMenuItem();
    JMenuItem jMenuCampaignHouses = new JMenuItem();
    JMenuItem jMenuCampaignFactionStatus = new JMenuItem();
    JMenuItem jMenuCampaignCheckAttack = new JMenuItem();
    JMenuItem jMenuCampaignRange = new JMenuItem();
    JMenuItem jMenuFindContestedPlanets = new JMenuItem(); //BarukKhazad 20151129
    JMenuItem jMenuCampaignTransferUnit = new JMenuItem();
    JMenuItem jMenuCampaignTransferMoney = new JMenuItem();
    JMenuItem jMenuCampaignTransferPilot = new JMenuItem();
    JMenuItem jMenuCampaignLogo = new JMenuItem();
    JMenuItem jMenuCampaignPersonalPilotQueue = new JMenuItem();
    JMenuItem jMenuCampaignDonatePersonalPilot = new JMenuItem();
    JMenuItem jMenuCampaignDirectSell = new JMenuItem();
    JMenuItem jMenuCampaignDefect = new JMenuItem();
    JMenuItem jMenuCampaignSelfPromote = new JMenuItem(); //@salient
    JMenuItem jMenuCampaignReportStatusMC = new JMenuItem(); //@salient
    JMenuItem jMenuCampaignRewardPoints = new JMenuItem();
    JMenuItem jMenuCampaignInfluencePoints = new JMenuItem();
    JMenuItem jMenuCampaignPartsCache = new JMenuItem();
    JMenuItem jMenuSubCampaignFireTechs = new JMenuItem();
    JMenuItem jMenuSubCampaignHireTechs = new JMenuItem();
    JMenuItem jMenuSubCampaignSellBays = new JMenuItem();
    JMenuItem jMenuSubCampaignBuyBays = new JMenuItem();
    JMenuItem jMenuCampaignBuyPilots = new JMenuItem();
    JMenuItem jMenuMercStatus = new JMenuItem();
    JMenuItem jMenuMercUnemployed = new JMenuItem();
    JMenuItem jMenuMercContracted = new JMenuItem();
    JMenuItem jMenuMercOfferContract = new JMenuItem();

    /*
     * ATTACK/GAME Menu is a class unto itself and needs constant update calls.
     */
    /** The dynamically-populated Attack/Game menu; a separate component class since its contents change with game state. */
    AttackMenu jMenuAttackMenu;
    // HOST Menu
    JMenu jMenuHost = new JMenu();
    JMenuItem jMenuCSHostAndJoin = new JMenuItem();
    JMenuItem jMenuCSHostDedicated = new JMenuItem();
    JMenuItem jMenuCSHostLoad = new JMenuItem();
    JMenuItem jMenuCSHostLoadAndJoin = new JMenuItem();
    JMenuItem jMenuCSHostStop = new JMenuItem();

    // OPTIONS menu components
    JMenu jMenuOptions = new JMenu();
    JCheckBoxMenuItem jMenuOptionsAutoScroll = new JCheckBoxMenuItem();
    JCheckBoxMenuItem jMenuOptionsMute = new JCheckBoxMenuItem();
    JMenuItem jMenuOptionsReloadAllData = new JMenuItem();

    // Leadership Menu
    JMenu jMenuLeaderShip = new JMenu();
    JMenuItem jMenuLeaderPromote = new JMenuItem();
    JMenuItem jMenuLeaderDemote = new JMenuItem();
    JMenuItem jMenuLeaderFluff = new JMenuItem();
    JMenuItem jMenuLeaderMute = new JMenuItem();
    JMenuItem jMenuLeaderFactionColor = new JMenuItem();
    JMenuItem jMenuLeaderPlayerColor = new JMenuItem();
    JMenuItem jMenuLeaderPurchaseFactory = new JMenuItem();
    JMenuItem jMenuLeaderResearchTech = new JMenuItem();
    JMenuItem jMenuLeaderResearchUnit = new JMenuItem();
    JMenuItem jMenuLeaderSetComponentConversion = new JMenuItem();
    JMenuItem jMenuLeaderViewFactionPartsCache = new JMenuItem();

    // HELP Menu
    JMenu jMenuHelp = new JMenu();
    JMenuItem jMenuHelpAbout = new JMenuItem();
    JMenuItem jMenuHelpMemory = new JMenuItem();
    JMenuItem jMenuHelpHelp = new JMenuItem();
    JMenuItem jMenuHelpViewUnit = new JMenuItem();
    JMenuItem jMenuHelpViewBuildTables = new JMenuItem();
    JMenuItem jMenuHelpViewTraits = new JMenuItem();
    JMenuItem jMenuHelpPilotSkills = new JMenuItem();
    JMenuItem jMenuHelpOpViewer = new JMenuItem();

    // Emoji Menu
    JMenu jMenuEmoji = new JMenu();
    JMenuItem jMenuEmojiFlip = new JMenuItem();
    JMenuItem jMenuEmojiShrug = new JMenuItem();
    JMenuItem jMenuEmojiFingers = new JMenuItem();
    JMenuItem jMenuEmojiKiss = new JMenuItem();
    JMenuItem jMenuEmojiSmile = new JMenuItem();
    JMenuItem jMenuEmojiDeal = new JMenuItem();

    // These are simple holders for when real menus
    // is generated/returned from the admin plugin.
    JMenu jMenuMod = new JMenu();
    JMenu jMenuAdmin = new JMenu();
    JMenu jMenuOperations = new JMenu();

    /** The tabbed content panel (chat, HQ, map, etc.) that fills this frame's content pane; see {@code gui.panels.CMainPanel}. */
    CMainPanel MainPanel;

    /** Cached reference to the client's campaign data, captured once at construction time. */
    CCampaign theCampaign;

    /** Cached reference to the locally-controlled player, captured once at construction time. */
    CPlayer thePlayer;

    /** Whether the server is using the "Advance Repairs" ruleset (bays/techs by type rather than a flat tech count); drives visibility of several menu items. */
    boolean useAdvanceRepairs;

    /** Whether the server allows personal pilot queues (per-player pilot hiring/firing/transfer); drives visibility of several menu items. */
    boolean usePersonalPilotQueues;

    /** The player's current access level, refreshed each time {@link #enableMenu()} runs; used to gate visibility of leader/operations menu items. */
    private int userLevel = 0;

    /** Set once the optional admin/mod/operations menus have been (attempted to be) built, so {@link #enableMenu()} does not repeat the reflective jar-loading work on every call. */
    private boolean hasAdminMenus = false;

    /**
     * Builds the main window: caches campaign/player references from the client,
     * constructs the tabbed {@link CMainPanel}, reads a handful of server config
     * flags that gate menu visibility, builds the menu bar (via {@link #createMenu()}),
     * and installs a window listener that intercepts the close button so a
     * confirmation is required while this client is hosting a game.
     * <p>
     * Also wires MegaMek's key-press handling to this window's focus state:
     * whenever this campaign-client frame GAINS focus, every embedded MegaMek
     * client's controller is told to ignore key presses (since the user is
     * interacting with this frame, not a MegaMek game window), and whenever
     * this frame LOSES focus (presumably to a MegaMek game window), those
     * controllers are told to stop ignoring key presses again. This avoids
     * this frame's own key handling (e.g. text fields) leaking into any
     * concurrently open MegaMek game windows.
     *
     * @param myC the client facade this window will display and act upon
     */
    public CMainFrame(IClient myC) {
        client = myC;
        theCampaign = client.getCampaign();
        thePlayer = client.getPlayer();
        MainPanel = new CMainPanel(client, this);

        useAdvanceRepairs = client.isUsingAdvanceRepairs();
        usePersonalPilotQueues = Boolean.parseBoolean(client.getServerConfigs("AllowPersonalPilotQueues"));
        sound = new MenuSound(client);
        popupSound = new MenuPopupSound(client);

        /*
         * ATTACK/GAME Menu is a class unto itself and needs constant update
         * calls. Have to build it here so its client isn't null and it isn't
         * being handed to createMenu() as a null itself.
         */

        jMenuAttackMenu = new AttackMenu(client, -1, "-1");

        enableEvents(java.awt.AWTEvent.WINDOW_EVENT_MASK);
        setResizable(true);
        setSize(new java.awt.Dimension(640, 480));
        setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        setTitle(String.format("%s (MekWars client %s)", client.getConfigParam("CAMPAIGN_SERVER_NAME"), IClient.CLIENT_VERSION));
        contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new java.awt.BorderLayout());
        // NOTE: redundant re-assignment — useAdvanceRepairs/usePersonalPilotQueues were already
        // set to the same values a few lines above; harmless, but likely leftover from a refactor.
        useAdvanceRepairs = client.isUsingAdvanceRepairs();
        usePersonalPilotQueues = Boolean.parseBoolean(client.getServerConfigs("AllowPersonalPilotQueues"));
        try {
            // factored out to reduce bloat
            createMenu();
        } catch (Exception e) {
            LOGGER.error(e, "Unable to create the menu. {}", e.getLocalizedMessage());
        }
        setJMenuBar(jMenuBar1);
        enableMenu();
        repaint();
        contentPane.add(MainPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            // Intercepts the OS close button (X). Since setDefaultCloseOperation is
            // DO_NOTHING_ON_CLOSE, this is the ONLY path that can close the window;
            // if this client is currently hosting a game, the user must confirm first.
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                if (client.isServerRunning()) {
                    int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                          "Are you sure you want to exit?",
                          "You are hosting a game!",
                          JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        client.goodbye();
                        System.exit(0);
                    }
                } else {
                    client.goodbye();
                    System.exit(0);
                }
            }


            // This frame gained focus: any embedded MegaMek clients should ignore
            // key presses since input is going to this frame instead.
            @Override
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
                for (ClientThread mmClient : client.getMMClients()) {
                    mmClient.getMegaMekController().setIgnoreKeyPresses(true);
                }
            }


            // This frame lost focus (presumably to a MegaMek game window): let
            // embedded MegaMek clients resume handling key presses.
            @Override
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
                for (ClientThread mmClient : client.getMMClients()) {
                    mmClient.getMegaMekController().setIgnoreKeyPresses(false);
                }
            }
        });
    }

    /**
     * Builds a non-editable combo box listing pilots, formatted for display in a
     * pilot-selection dialog (e.g. transfer/donate personal pilot).
     * Each entry shows the pilot's name and skill numbers: for a {@link Unit#MEK}
     * this is "Name (Gunnery/Piloting)[skills]", otherwise "Name (Gunnery)[skills]".
     *
     * @param pilots   an array of {@link Pilot} objects to list (untyped so callers
     *                 can pass a {@code Vector.toArray()} result directly)
     * @param unitType one of the {@link Unit} type constants; only {@link Unit#MEK}
     *                 changes the formatting (adds the piloting skill)
     * @return a populated, non-editable combo box ready to embed in a {@link JOptionPane}
     */
    private static @org.jspecify.annotations.NonNull JComboBox<String> getStringJComboBox(Object[] pilots,
          int unitType) {
        JComboBox<String> combo = new JComboBox<>();

        for (Object pilot : pilots) {
            Pilot mm = (Pilot) pilot;
            if (unitType == Unit.MEK) {
                combo.addItem(mm.getName() +
                                    " (" +
                                    mm.getGunnery() +
                                    "/" +
                                    mm.getPiloting() +
                                    ")[" +
                                    mm.getSkillString(true) +
                                    "]");
            } else {
                combo.addItem(mm.getName() + " (" + mm.getGunnery() + ")[" + mm.getSkillString(true) + "]");
            }
        }

        combo.setEditable(false);
        return combo;
    }

    /** @return the tabbed content panel hosted by this frame. */
    public CMainPanel getMainPanel() {
        return MainPanel;
    }

    /**
     * Refreshes which menu items are visible/enabled based on the player's
     * current connection status ({@link IClient#getMyStatus()}) and access
     * level, and lazily builds the optional Mod/Admin/Operations menus the
     * first time this player is detected as a mod or admin.
     * <p>
     * The Mod and Admin menus are loaded reflectively from an external
     * {@code ./MekWarsAdmin.jar} (classes {@code admin.ModeratorMenu} and
     * {@code admin.AdminMenu}), and the Operations menu is built inline if
     * {@code ./MekWarsOpEditor.jar} is present on disk. This indirection lets
     * server operators distribute privileged tooling only to trusted clients
     * without bundling it into the base MekWars client. If neither jar is
     * present, the corresponding menu(s) are simply skipped (with a log
     * message for the admin jar) and never appear.
     * <p>
     * This method also reloads server commands ({@link IClient#loadServerCommands()})
     * and repaints the frame every time it runs, so it is relatively heavyweight
     * and is only expected to be called on status/permission changes rather than
     * on every UI tick.
     */
    // Initializing Components
    public void enableMenu() {
        boolean disconnected = false;
        boolean loggedout = false;
        boolean loggedin = false;
        boolean reserve = false;
        boolean active = false;
        boolean admin = false;
        boolean mod = false;

        userLevel = client.getUserLevel();
        client.loadServerCommands();

        if (client.getMyStatus() == IClient.STATUS_DISCONNECTED) {
            disconnected = true;
        }
        if (client.getMyStatus() == IClient.STATUS_LOGGED_OUT) {
            loggedout = true;
        }
        if (client.getMyStatus() == IClient.STATUS_RESERVE) {
            loggedin = true;
            reserve = true;
        }
        if (client.getMyStatus() == IClient.STATUS_ACTIVE) {
            loggedin = true;
            active = true;
        }
        if (client.getMyStatus() == IClient.STATUS_FIGHTING) {
            loggedin = true;
            // fighting = true;
        }
        if (client.isAdmin()) {
            admin = true;
        }
        if (client.isMod()) {
            mod = true;
        }

        if ((mod || admin) && !hasAdminMenus) {

            File loadJar = new File("./MekWarsAdmin.jar");

            // don't print an entire trace if the jar is missing.
            if (!loadJar.exists()) {
                LOGGER.error("Player/Server menu creation skipped. No MekWarsAdmin.jar present.");
            } else {
                // assume mod
                try {
                    java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] {
                          loadJar.toURI().toURL()
                    });

                    Class<?> clazz = loader.loadClass("admin.ModeratorMenu");
                    Object object = clazz.getDeclaredConstructor().newInstance();
                    clazz.getDeclaredMethod("createMenu", new Class[] { IClient.class }).invoke(object, client);
                    jMenuBar1.remove(jMenuMod);
                    jMenuMod = (JMenu) object;
                    jMenuBar1.add(jMenuMod);
                    loader.close();
                } catch (Exception ex) {
                    LOGGER.error(ex, "ModeratorMenu creation FAILED!");
                }
                try {
                    java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] {
                          loadJar.toURI().toURL() });
                    Class<?> clazz = loader.loadClass("admin.AdminMenu");
                    Object object = clazz.getDeclaredConstructor().newInstance();
                    clazz.getDeclaredMethod("createMenu", new Class[] { IClient.class }).invoke(object, client);
                    jMenuBar1.remove(jMenuAdmin);
                    jMenuAdmin = (JMenu) object;
                    jMenuBar1.add(jMenuAdmin);
                    loader.close();
                } catch (Exception ex) {
                    LOGGER.error(ex, "AdminMenu creation FAILED!");
                }
            }// end else(Admin.jar exists)

            if (new java.io.File("./MekWarsOpEditor.jar").exists()) {
                jMenuBar1.remove(jMenuOperations);
                jMenuOperations.setText("Operations");
                JMenuItem item = getJMenuItem();
                jMenuOperations.add(item);
                JMenuItem jMenuRetrieveOperationFile = new JMenuItem();
                JMenuItem jMenuSetOperationFile = new JMenuItem();
                JMenuItem jMenuSetNewOperationFile = new JMenuItem();
                JMenuItem jMenuSendAllOperationFiles = new JMenuItem();
                JMenuItem jMenuUpdateOperations = new JMenuItem();

                jMenuRetrieveOperationFile.setText("Retrieve Operation File");
                jMenuRetrieveOperationFile.addActionListener(this::jMenuRetrieveOperationFile_actionPerformed);

                jMenuSetOperationFile.setText("Set Operation File");
                jMenuSetOperationFile.addActionListener(this::jMenuSetOperationFile_actionPerformed);

                jMenuSetNewOperationFile.setText("Set New Operation File");
                jMenuSetNewOperationFile.addActionListener(this::jMenuSetNewOperationFile_actionPerformed);

                jMenuSendAllOperationFiles.setText("Send All Local Op Files");
                jMenuSendAllOperationFiles.addActionListener(this::jMenuSendAllOperationFiles_actionPerformed);

                jMenuUpdateOperations.setText("Update Operations");
                jMenuUpdateOperations.addActionListener(this::jMenuUpdateOperations_actionPerformed);

                int userLevel = client.getUserLevel();
                if (userLevel >= client.getData().getAccessLevel("RetrieveOperation")) {
                    jMenuOperations.add(jMenuRetrieveOperationFile);
                }
                if (userLevel >= client.getData().getAccessLevel("SetOperation")) {
                    jMenuOperations.add(jMenuSetOperationFile);
                    jMenuOperations.add(jMenuSetNewOperationFile);
                    jMenuOperations.add(jMenuSendAllOperationFiles);
                }
                if (userLevel >= client.getData().getAccessLevel("UpdateOperations")) {
                    jMenuOperations.add(jMenuUpdateOperations);
                }

                jMenuBar1.add(jMenuOperations);
            }
            hasAdminMenus = true;
        }// end if(is admin or mod)

        jMenuAdmin.setVisible(admin);
        jMenuMod.setVisible(mod);
        jMenuOperations.setVisible(mod);

        jMenuCampaign.setVisible(!disconnected);
        jMenuCampaignMyStatus.setVisible(loggedin);
        jMenuCampaignSubAttack.setVisible(loggedin);
        jMenuCampaignSubMerc.setVisible(loggedin);
        jMenuCampaignSubTransfer.setVisible(loggedin);
        jMenuCampaignSubStatus.setVisible(loggedin);
        jMenuCampaignSubTechs.setVisible(loggedin);
        jMenuCampaignSubBays.setVisible(useAdvanceRepairs && loggedin);
        jMenuCampaignSubOther.setVisible(loggedin);
        // jMenuTask.setVisible(loggedin);
        jMenuHost.setVisible(!disconnected);

        jMenuLeaderShip.setVisible(client.isLeader());

        jMenuLeaderDemote.setVisible(userLevel >= client.getData().getAccessLevel("DemotePlayer"));
        jMenuLeaderFluff.setVisible(userLevel >= client.getData().getAccessLevel("FactionLeaderFluff"));
        jMenuLeaderMute.setVisible(userLevel >= client.getData().getAccessLevel("FactionLeaderMute"));
        jMenuLeaderPromote.setVisible(userLevel >= client.getData().getAccessLevel("PromotePlayer"));
        jMenuLeaderFactionColor.setVisible(userLevel >= client.getData().getAccessLevel("ChangeHouseColor"));
        jMenuLeaderPlayerColor.setVisible(userLevel >= client.getData().getAccessLevel("AdminSetHousePlayerColor"));
        jMenuLeaderPurchaseFactory.setVisible(userLevel >= client.getData().getAccessLevel("PurchaseFactory"));
        jMenuLeaderResearchTech.setVisible(userLevel >= client.getData().getAccessLevel("ResearchTechLevel"));
        jMenuLeaderResearchUnit.setVisible(userLevel >= client.getData().getAccessLevel("ResearchUnit"));
        jMenuLeaderSetComponentConversion.setVisible(userLevel >=
                                                           client.getData().getAccessLevel("SetComponentConversion"));
        jMenuLeaderViewFactionPartsCache.setVisible(userLevel >=
                                                          client.getData().getAccessLevel("ViewFactionPartsCache"));

        jMenuFileConnect.setVisible(disconnected);
        jMenuFileDisconnect.setVisible(!disconnected);
        jMenuFileRegister.setVisible(!disconnected);
        jMenuFileMail.setVisible(!disconnected);
        jMenuFileLastOnline.setVisible(!disconnected);
        jMenuFileConfig.setVisible(true);

        jMenuCampaignLogin.setVisible(loggedout);
        jMenuCampaignActivate.setVisible(reserve);
        jMenuCampaignDeactivate.setVisible(active);
        jMenuCampaignLogout.setVisible(loggedin);
        jMenuCampaignPersonalPilotQueue.setVisible(usePersonalPilotQueues);
        jMenuCampaignTransferPilot.setVisible(usePersonalPilotQueues);
        jMenuCampaignDonatePersonalPilot.setVisible(usePersonalPilotQueues);
        jMenuCampaignDirectSell.setVisible(Boolean.parseBoolean(client.getServerConfigs("UseDirectSell")));

        addMenuListener(jMenuBar1.getComponents());
        this.repaint();
    }

    /**
     * Builds the "Op Editor" menu item that launches the external Operations
     * Editor application (from {@code ./MekWarsOpEditor.jar}) via reflection,
     * invoking {@code OperationsEditor.MainOperations.main(Object)} on a freshly
     * loaded class. Used only within the Operations menu construction in
     * {@link #enableMenu()} when that jar is present.
     *
     * @return a ready-to-add "Op Editor" menu item with its action listener attached
     */
    private @Nonnull JMenuItem getJMenuItem() {
        JMenuItem item = new JMenuItem("Op Editor");
        item.addActionListener(actionEvent -> {
            try {
                URLClassLoader loader = new URLClassLoader(new URL[] {
                      new File("./MekWarsOpEditor.jar").toURI().toURL() });
                Class<?> clazz = loader.loadClass("OperationsEditor.MainOperations");
                Object object = clazz.getDeclaredConstructor().newInstance();
                clazz.getDeclaredMethod("main", new Class[] { Object.class }).invoke(object, client);
                loader.close();
            } catch (Exception ex) {
                LOGGER.error(ex, "Unable to create class loader {}", ex.getLocalizedMessage());
            }
        });
        return item;
    }

    /**
     * Constructs every static menu (File, Campaign and its submenus, Host,
     * Options, Leadership, Help, Emoji) and assembles them into {@link #jMenuBar1}.
     * This is invoked once from the constructor. Each menu item's text, mnemonic,
     * and {@link ActionListener} (usually delegating to a
     * {@code jMenuXyz_actionPerformed} method on this class) are set up here, and
     * a handful of items/submenus are only added when relevant server config
     * flags are enabled (e.g. advance repairs bays, personal pilot queues, emoji,
     * mini-campaigns, direct sell). The dynamic Attack/Game menu
     * ({@link #jMenuAttackMenu}) and the optional Mod/Admin/Operations menus are
     * NOT built here; the latter are built lazily in {@link #enableMenu()}.
     *
     * @throws Exception declared broadly on the original signature; in practice
     *                    no checked exception is thrown by the body, and the
     *                    constructor already wraps calls to this method in a
     *                    try/catch that merely logs any failure.
     */
    protected void createMenu() throws Exception {

        jMenuFile.setText("File");
        jMenuFile.setMnemonic('F');

        jMenuFileConnect.setText("Connect");
        jMenuFileConnect.setMnemonic('o');
        jMenuFileConnect.addActionListener(actionEvent -> jMenuFileConnect_actionPerformed());

        jMenuFileDisconnect.setText("Disconnect");
        jMenuFileDisconnect.setMnemonic('D');
        jMenuFileDisconnect.addActionListener(actionEvent -> client.getConnector().closeConnection());

        jMenuFileRegister.setText("Register Nickname");
        jMenuFileRegister.setMnemonic('R');
        jMenuFileRegister.addActionListener(actionEvent -> jMenuFileRegister_actionPerformed());

        jMenuFileMail.setText("Mail User");
        jMenuFileMail.setMnemonic('M');
        jMenuFileMail.addActionListener(actionEvent -> jMenuFileMail_actionPerformed(null));

        jMenuFileLastOnline.setText("Last Online");
        jMenuFileLastOnline.setMnemonic('L');
        jMenuFileLastOnline.addActionListener(actionEvent -> jMenuFileLastOnline_actionPerformed());

        jMenuFileConfig.setText("Configuration");
        jMenuFileConfig.setMnemonic('C');
        jMenuFileConfig.addActionListener(actionEvent -> new ConfigurationDialog(client));

        jMenuFileExit.setText("Exit");
        jMenuFileExit.setMnemonic('X');
        jMenuFileExit.addActionListener(actionEvent -> jMenuFileExit_actionPerformed());

        jMenuCampaign.setText("Campaign");
        jMenuCampaign.setMnemonic('C');

        jMenuCampaignLogin.setText("Log in!");
        jMenuCampaignLogin.setMnemonic('I');
        jMenuCampaignLogin.addActionListener(actionEvent -> client.sendChat(String.format("%sc login", IClient.CAMPAIGN_PREFIX)));

        jMenuCampaignActivate.setText("Activate");
        jMenuCampaignActivate.addActionListener(actionEvent -> client.sendChat(String.format("%sc activate#%s", IClient.CAMPAIGN_PREFIX, IClient.CLIENT_VERSION)));

        jMenuCampaignDeactivate.setText("Deactivate");
        jMenuCampaignDeactivate.addActionListener(actionEvent -> client.sendChat(String.format("%sc deactivate", IClient.CAMPAIGN_PREFIX)));

        jMenuCampaignLogout.setText("Log Out");
        jMenuCampaignLogout.addActionListener(actionEvent -> client.sendChat(String.format("%sc logout", IClient.CAMPAIGN_PREFIX)));

        jMenuCampaignPlayers.setText("Players Status");
        jMenuCampaignPlayers.setMnemonic('P');
        jMenuCampaignPlayers.addActionListener(actionEvent -> client.sendChat(String.format("%sc players", IClient.CAMPAIGN_PREFIX)));

        jMenuCampaignISStatus.setText("Planetary Control");
        jMenuCampaignISStatus.setMnemonic('C');
        jMenuCampaignISStatus.addActionListener(actionEvent -> jMenuCampaignISStatus_actionPerformed());

        jMenuCampaignFactionStatus.setText("Faction Status");
        jMenuCampaignFactionStatus.setMnemonic('F');
        jMenuCampaignFactionStatus.addActionListener(actionEvent -> jMenuCampaignFactionStatus_actionPerformed());

        jMenuCampaignHouses.setText("Factions List");
        jMenuCampaignHouses.setMnemonic('L');
        jMenuCampaignHouses.addActionListener(actionEvent -> client.sendChat(String.format("%sc housestatus", IClient.CAMPAIGN_PREFIX)));

        if (useAdvanceRepairs) {
            jMenuCampaignSubBays.setText("Bays");
            jMenuCampaignSubBays.setMnemonic('B');
        }

        jMenuCampaignSubTechs.setText("Personnel");
        jMenuCampaignSubTechs.setMnemonic('E');

        jMenuCampaignSubTransfer.setText("Transfer");
        jMenuCampaignSubTransfer.setMnemonic('T');

        jMenuCampaignSubAttack.setText("Front Line");
        jMenuCampaignSubAttack.setMnemonic('F');

        jMenuCampaignSubStatus.setText("Status");
        jMenuCampaignSubStatus.setMnemonic('U');

        jMenuCampaignSubOther.setText("Other");
        jMenuCampaignSubOther.setMnemonic('O');

        jMenuCampaignMyStatus.setText("My Status");
        jMenuCampaignMyStatus.setMnemonic('M');
        jMenuCampaignMyStatus.addActionListener(actionEvent -> client.sendChat(String.format("%sc mystatus", IClient.CAMPAIGN_PREFIX)));

        jMenuCampaignCheckAttack.setText("Attack Options");
        jMenuCampaignCheckAttack.setMnemonic('A');
        jMenuCampaignCheckAttack.addActionListener(actionEvent -> jMenuCommanderCheckAttack_actionPerformed(-1));

        jMenuCampaignRange.setText("Range Calculator");
        jMenuCampaignRange.setMnemonic('R');
        jMenuCampaignRange.addActionListener(actionEvent -> jMenuCommanderRange_actionPerformed());

        jMenuFindContestedPlanets.setText("Find Contested Planets");
        jMenuFindContestedPlanets.setMnemonic('Z');
        jMenuFindContestedPlanets.addActionListener(actionEvent -> jMenuFindContestedPlanets_actionPerformed());  //BarukKhazad 20151129 - end 1

        jMenuCampaignTransferUnit.setText("Transfer Unit");
        jMenuCampaignTransferUnit.setMnemonic('U');
        jMenuCampaignTransferUnit.addActionListener(actionEvent -> jMenuCommanderTransferUnit_actionPerformed(null, -1));

        jMenuCampaignTransferMoney.setText(String.format("Transfer %s", client.moneyOrFluMessage(true, true, -2)));
        jMenuCampaignTransferMoney.setMnemonic('C');
        jMenuCampaignTransferMoney.addActionListener(actionEvent -> jMenuCommanderTransferMoney_actionPerformed(null));

        jMenuCampaignLogo.setText("Set Logo");
        jMenuCampaignLogo.setMnemonic('L');
        jMenuCampaignLogo.addActionListener(actionEvent -> jMenuCommanderLogo_actionPerformed());

        jMenuCampaignPersonalPilotQueue.setText("View Pilot Queue");
        jMenuCampaignPersonalPilotQueue.setMnemonic('Q');
        jMenuCampaignPersonalPilotQueue.addActionListener(actionEvent -> jMenuCommanderPersonalPilotQueue_actionPerformed());

        jMenuCampaignDonatePersonalPilot.setText("Fire Pilot");
        jMenuCampaignDonatePersonalPilot.setMnemonic('o');
        jMenuCampaignDonatePersonalPilot.addActionListener(actionEvent -> jMenuCommanderDonatePersonalPilot_actionPerformed());

        jMenuCampaignDirectSell.setText("Direct Sell Unit");
        jMenuCampaignDirectSell.setMnemonic('S');
        jMenuCampaignDirectSell.addActionListener(actionEvent -> jMenuCommanderDirectSell_actionPerformed(null, null));

        jMenuCampaignTransferPilot.setText("Transfer Pilot");
        jMenuCampaignTransferPilot.setMnemonic('T');
        jMenuCampaignTransferPilot.addActionListener(actionEvent -> jMenuCommanderTransferPilot_actionPerformed(null));

        jMenuCampaignDefect.setText("Defect");
        jMenuCampaignDefect.setMnemonic('D');
        jMenuCampaignDefect.addActionListener(actionEvent -> jMenuCommanderDefect_actionPerformed());

        jMenuCampaignSelfPromote.setText("Self Promote"); //@salient
        jMenuCampaignSelfPromote.addActionListener(actionEvent -> jMenuCommanderSelfPromote_actionPerformed());

        jMenuCampaignReportStatusMC.setText("Check MiniCampaign Status"); //@salient for mini campaign
        jMenuCampaignReportStatusMC.addActionListener(actionEvent -> jMenuCommanderReportStatusMC_actionPerformed());

        jMenuCampaignRewardPoints.setText(String.format("Use %s", client.getServerConfigs("RPLongName")));
        jMenuCampaignRewardPoints.setMnemonic('P');
        jMenuCampaignRewardPoints.addActionListener(actionEvent -> client.rewardPointsDialog());

        //@Salient
        jMenuCampaignInfluencePoints.setText(String.format("Use %s", client.getServerConfigs("FluLongName")));
        jMenuCampaignInfluencePoints.addActionListener(actionEvent -> client.influencePointsDialog());

        jMenuCampaignPartsCache.setText("View Parts");
        jMenuCampaignPartsCache.setMnemonic('V');
        jMenuCampaignPartsCache.addActionListener(actionEvent -> jMenuCampaignPartsCache_actionPerformed());

        if (useAdvanceRepairs) {
            jMenuSubCampaignBuyBays.setText("Lease Bays");
            jMenuSubCampaignBuyBays.setMnemonic('L');
            jMenuSubCampaignBuyBays.addActionListener(actionEvent -> jMenuCommanderBuyBays_actionPerformed());

            jMenuSubCampaignSellBays.setText("Return Bays");
            jMenuSubCampaignSellBays.setMnemonic('R');
            jMenuSubCampaignSellBays.addActionListener(actionEvent -> jMenuCommanderSellBays_actionPerformed());
        }

        if (usePersonalPilotQueues) {
            jMenuCampaignBuyPilots.setText("Hire Pilots");
            jMenuCampaignBuyPilots.setMnemonic('P');
            jMenuCampaignBuyPilots.addActionListener(actionEvent -> jMenuCampaignSubOtherBuyPilots_actionPerformed());
        }

        jMenuSubCampaignHireTechs.setText("Hire Techs");
        jMenuSubCampaignHireTechs.setMnemonic('H');
        jMenuSubCampaignHireTechs.addActionListener(actionEvent -> jMenuCommanderHireTechs_actionPerformed());

        jMenuSubCampaignFireTechs.setText("Fire Techs");
        jMenuSubCampaignFireTechs.setMnemonic('F');
        jMenuSubCampaignFireTechs.addActionListener(actionEvent -> jMenuCommanderFireTechs_actionPerformed());

        jMenuCampaignSubMerc.setText("Mercenaries");
        jMenuCampaignSubMerc.setMnemonic('r');

        jMenuMercOfferContract.setText("Offer a Mercenary Contract");
        jMenuMercOfferContract.setMnemonic('O');
        jMenuMercOfferContract.addActionListener(actionEvent -> jMenuMercOfferContract_actionPerformed());

        jMenuMercStatus.setText("Mercenary Status");
        jMenuMercStatus.setMnemonic('M');
        jMenuMercStatus.addActionListener(actionEvent -> jMenuMercStatus_actionPerformed());

        jMenuMercUnemployed.setText("Unemployed Mercs");
        jMenuMercUnemployed.setMnemonic('U');
        jMenuMercUnemployed.addActionListener(actionEvent -> client.sendChat(String.format("%sc unemployedmercs", IClient.CAMPAIGN_PREFIX)));

        jMenuMercContracted.setText("Contracted Mercs");
        jMenuMercContracted.setMnemonic('C');
        jMenuMercContracted.addActionListener(actionEvent -> client.sendChat(String.format("%sc housecontracts", IClient.CAMPAIGN_PREFIX)));

        jMenuHost.setText("Host");
        jMenuHost.setMnemonic('S');

        jMenuCSHostAndJoin.setText("Start Hosting (and Join)");
        jMenuCSHostAndJoin.setMnemonic('H');
        jMenuCSHostAndJoin.addActionListener(actionEvent -> {
            startHost();
            client.startHost(false, true, false);
        });

        jMenuCSHostDedicated.setText("Start Dedicated Host");
        jMenuCSHostDedicated.setMnemonic('D');
        jMenuCSHostDedicated.setEnabled(false);
        jMenuCSHostDedicated.setVisible(false);
        jMenuCSHostDedicated.addActionListener(actionEvent -> {
            startHost();
            client.startHost(true, false, false);
        });

        jMenuCSHostLoad.setText("Start Dedicated Host (Load Savegame)");
        jMenuCSHostLoad.setMnemonic('L');
        jMenuCSHostLoad.setEnabled(false);
        jMenuCSHostLoad.setVisible(false);
        jMenuCSHostLoad.addActionListener(actionEvent -> {
            startHost();
            client.startHost(true, false, true);
        });

        jMenuCSHostLoadAndJoin.setText("Start Hosting (Load Savegame and Join)");
        jMenuCSHostLoadAndJoin.setMnemonic('S');
        jMenuCSHostLoadAndJoin.addActionListener(actionEvent -> {
            startHost();
            client.startHost(false, false, true);
        });

        jMenuCSHostStop.setText("Stop Hosting");
        jMenuCSHostStop.setMnemonic('S');
        jMenuCSHostStop.setEnabled(false);
        jMenuCSHostStop.setVisible(false);
        jMenuCSHostStop.addActionListener(actionEvent -> {
            stopHost();
            client.stopHost();
        });

        jMenuOptions.setText("Options");
        jMenuOptions.setMnemonic('I');

        jMenuOptionsAutoScroll.setText("Auto Scroll");
        jMenuOptionsAutoScroll.setMnemonic('A');
        jMenuOptionsAutoScroll.setState(MainPanel.getCommPanel().getAutoTextUpdate());
        jMenuOptionsAutoScroll.addActionListener(actionEvent -> {
            boolean newValue = !MainPanel.getCommPanel().getAutoTextUpdate();
            MainPanel.getCommPanel().setAutoTextUpdate(newValue);
            jMenuOptionsAutoScroll.setState(newValue);

            client.getConfig().setParam("AUTOSCROLL", Boolean.toString(newValue));
            client.getConfig().saveConfig();
        });

        jMenuOptionsMute.setText("Mute");
        jMenuOptionsMute.setMnemonic('M');
        jMenuOptionsMute.addActionListener(actionEvent -> client.setSoundMuted(jMenuOptionsMute.getState()));

        jMenuOptionsReloadAllData.setText("Reload Data");
        jMenuOptionsReloadAllData.setMnemonic('D');
        jMenuOptionsReloadAllData.addActionListener(actionEvent -> client.reloadData());

        jMenuLeaderShip.setText("Leadership");

        jMenuLeaderPromote.setText("Promote Player");
        jMenuLeaderPromote.addActionListener(actionEvent -> jMenuLeaderPromote_actionPerformed());

        jMenuLeaderDemote.setText("Demote Player");
        jMenuLeaderDemote.addActionListener(actionEvent -> jMenuLeaderDemote_actionPerformed());

        jMenuLeaderFluff.setText("Fluff Player");
        jMenuLeaderFluff.addActionListener(actionEvent -> jMenuLeaderFluff_actionPerformed());

        jMenuLeaderMute.setText("Mute Player");
        jMenuLeaderMute.addActionListener(actionEvent -> jMenuLeaderMute_actionPerformed());

        jMenuLeaderFactionColor.setText("Faction Color");
        jMenuLeaderFactionColor.addActionListener(actionEvent -> jMenuLeaderFactionColor_actionPerformed());

        jMenuLeaderPlayerColor.setText("Player Color");
        jMenuLeaderPlayerColor.addActionListener(actionEvent -> jMenuLeaderPlayerColor_actionPerformed());

        jMenuLeaderResearchUnit.setText("Research Unit");
        jMenuLeaderResearchUnit.addActionListener(actionEvent -> jMenuLeaderResearchUnit_actionPerformed());

        jMenuLeaderResearchTech.setText("Research Tech");
        jMenuLeaderResearchTech.addActionListener(actionEvent -> {
            int option = JOptionPane.showConfirmDialog(client.getMainFrame(),
                  "Do you wish to research tech?",
                  "Research?",
                  JOptionPane.YES_NO_OPTION);

            if (option == JOptionPane.NO_OPTION) {
                return;
            }

            client.sendChat(String.format("%sc researchtechlevel", IClient.CAMPAIGN_PREFIX));
        });

        jMenuLeaderPurchaseFactory.setText("Purchase Factory");
        jMenuLeaderPurchaseFactory.addActionListener(actionEvent -> jMenuLeaderPurchaseFactory_actionPerformed(null));

        jMenuLeaderSetComponentConversion.setText("Set Component Conversion");
        jMenuLeaderSetComponentConversion.addActionListener(actionEvent -> jMenuLeaderSetComponentConversion_actionPerformed());

        jMenuLeaderViewFactionPartsCache.setText("View Faction Cache");
        jMenuLeaderViewFactionPartsCache.addActionListener(actionEvent -> client.sendChat(String.format("%sc viewfactionpartscache", IClient.CAMPAIGN_PREFIX)));

        jMenuHelp.setText("Help");
        jMenuHelp.setMnemonic('E');

        jMenuHelpAbout.setText("About");
        jMenuHelpAbout.setMnemonic('A');
        jMenuHelpAbout.addActionListener(actionEvent -> jMenuHelpAbout_actionPerformed());

        jMenuHelpMemory.setText("Memory");
        jMenuHelpMemory.setMnemonic('M');
        jMenuHelpMemory.addActionListener(actionEvent -> jMenuHelpMemory_actionPerformed());

        jMenuHelpHelp.setText("Online Help");
        jMenuHelpHelp.setMnemonic('H');
        jMenuHelpHelp.addActionListener(actionEvent -> jMenuHelpHelp_actionPerformed());

        jMenuHelpViewUnit.setText("Unit Viewer");
        jMenuHelpViewUnit.setMnemonic('U');
        jMenuHelpViewUnit.addActionListener(actionEvent -> jMenuHelpViewUnit_actionPerformed());

        jMenuHelpViewBuildTables.setText("Build Table Viewer");
        jMenuHelpViewBuildTables.setMnemonic('B');
        jMenuHelpViewBuildTables.addActionListener(actionEvent -> jMenuHelpViewBuildTables_actionPerformed());

        jMenuHelpViewTraits.setText("View Faction Traits");
        jMenuHelpViewTraits.addActionListener(actionEvent -> new TraitDialog(client, true));

        jMenuHelpPilotSkills.setText("Pilot Skill Descriptions");
        jMenuHelpPilotSkills.setMnemonic('P');
        jMenuHelpPilotSkills.addActionListener(actionEvent -> jMenuHelpPilotSkills_actionPerformed());

        jMenuHelpOpViewer.setText("Operation Viewer");
        jMenuHelpOpViewer.addActionListener(actionEvent -> client.sendChat(String.format("%s getops md5", IClient.CAMPAIGN_PREFIX)));

        /*
         * Display Report "MekWars Bug" and "Report MegaMek Bug" links in the Help Menu. Create the actual menu
         * options, with browsers calls, here to add them to the menu in the formatting blocks that follow. These are
         * hardcoded. Server ops can add their own links with the links.txt detailed above. @urgru 12.5.04
         */
        JMenuItem jMenuMekWarsBug = new JMenuItem("Report Bug/RFE (MekWars)");
        JMenuItem jMenuMegaMekBug = new JMenuItem("Report Bug/REF (MegaMek)");
        ActionListener mekWarsListener = actionEvent -> {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/MegaMek/MekWars"));
                } catch (IOException e) {
                    LOGGER.error(e, "Unable to load the browser. Moving on");
                } catch (URISyntaxException e) {
                    LOGGER.error(e, "Curious how we got here as it's a manual entry.");
                }
            }
        };
        ActionListener megaMekListener = actionEvent -> {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/MegaMek/megamek"));
                } catch (IOException e) {
                    LOGGER.error(e, "Unable to load the browser. Moving On.");
                } catch (URISyntaxException e) {
                    LOGGER.error(e, "Curious how we got here with a manual entry.");
                }
            }
        };

        jMenuMekWarsBug.addActionListener(mekWarsListener);
        jMenuMegaMekBug.addActionListener(megaMekListener);

        //@sal emojis

        jMenuEmoji.setText("Emojis");

        if (Boolean.parseBoolean(client.getServerConfigs("AllowEmoji"))) {
            jMenuEmojiFlip.setText("(╯°□°)╯︵ ┻━┻");
            jMenuEmojiFlip.addActionListener(actionEvent -> client.sendChat(String.format("%sec#fl", IClient.CAMPAIGN_PREFIX)));

            jMenuEmojiShrug.setText("¯\\_(ツ)_/¯");
            jMenuEmojiShrug.addActionListener(actionEvent -> client.sendChat(String.format("%sec#sh", IClient.CAMPAIGN_PREFIX)));

            jMenuEmojiFingers.setText("t(-.-t)");
            jMenuEmojiFingers.addActionListener(actionEvent -> client.sendChat(String.format("%sec#fi", IClient.CAMPAIGN_PREFIX)));

            jMenuEmojiKiss.setText("( ˘ ³˘)♥");
            jMenuEmojiKiss.addActionListener(actionEvent -> client.sendChat(String.format("%sec#ki", IClient.CAMPAIGN_PREFIX)));

            jMenuEmojiSmile.setText("◉‿◉");
            jMenuEmojiSmile.addActionListener(actionEvent -> client.sendChat(String.format("%sec#sm", IClient.CAMPAIGN_PREFIX)));

            jMenuEmojiDeal.setText("•_•) ( •_•)>⌐■-■ (⌐■_■)");
            jMenuEmojiDeal.addActionListener(actionEvent -> client.sendChat(String.format("%sec#de", IClient.CAMPAIGN_PREFIX)));
        }

        /*
         * FORMATTING BLOCK
         */
        jMenuFile.add(jMenuFileConnect);
        jMenuFile.add(jMenuFileRegister);
        jMenuFile.add(jMenuFileMail);
        jMenuFile.add(jMenuFileLastOnline);
        jMenuFile.add(jMenuFileConfig);
        jMenuFile.addSeparator();
        jMenuFile.add(jMenuFileDisconnect);
        jMenuFile.add(jMenuFileExit);

        /*
         * Put together the campaign menu. Start by assembling the sub-menus, then add the manus and line items all
         * together ...
         */

        // front-line submenu
        jMenuCampaignSubAttack.add(jMenuCampaignCheckAttack);
        jMenuCampaignSubAttack.add(jMenuCampaignRange);
        jMenuCampaignSubAttack.add(jMenuFindContestedPlanets); //BarukKhazad 20151129

        // send submenu
        jMenuCampaignSubTransfer.add(jMenuCampaignTransferMoney);
        jMenuCampaignSubTransfer.add(jMenuCampaignTransferUnit);
        jMenuCampaignSubTransfer.add(jMenuCampaignTransferPilot);

        // techs sub menu
        jMenuCampaignPersonnelTechSubMenu.add(jMenuSubCampaignHireTechs);
        jMenuCampaignPersonnelTechSubMenu.add(jMenuSubCampaignFireTechs);
        jMenuCampaignSubTechs.add(jMenuCampaignPersonnelTechSubMenu);

        // Pilots sub menus.
        if (usePersonalPilotQueues) {
            jMenuCampaignPersonnelPilotsSubMenu.add(jMenuCampaignPersonalPilotQueue);
            jMenuCampaignPersonnelPilotsSubMenu.add(jMenuCampaignBuyPilots);
            jMenuCampaignPersonnelPilotsSubMenu.add(jMenuCampaignDonatePersonalPilot);
            jMenuCampaignSubTechs.add(jMenuCampaignPersonnelPilotsSubMenu);
        }

        // Bay sub menu
        jMenuCampaignSubBays.add(jMenuSubCampaignBuyBays);
        jMenuCampaignSubBays.add(jMenuSubCampaignSellBays);

        // status sub menu
        jMenuCampaignSubStatus.add(jMenuCampaignPlayers);
        jMenuCampaignSubStatus.add(jMenuCampaignFactionStatus);
        jMenuCampaignSubStatus.add(jMenuCampaignISStatus);
        jMenuCampaignSubStatus.add(jMenuCampaignHouses);

        // mercs sub menu
        jMenuCampaignSubMerc.add(jMenuMercStatus);
        jMenuCampaignSubMerc.add(jMenuMercUnemployed);
        jMenuCampaignSubMerc.add(jMenuMercContracted);
        jMenuCampaignSubMerc.add(jMenuMercOfferContract);

        // other sub menu
        jMenuCampaignSubOther.add(jMenuCampaignLogo);
        jMenuCampaignSubOther.add(jMenuCampaignDefect);

        if (Boolean.parseBoolean(client.getServerConfigs("Self_Promote_Subfaction"))) {
            jMenuCampaignSubOther.add(jMenuCampaignSelfPromote);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("Enable_MiniCampaign"))) {
            jMenuCampaignSubOther.add(jMenuCampaignReportStatusMC);
        }

        jMenuCampaignSubOther.add(jMenuCampaignRewardPoints);
        jMenuCampaignSubOther.add(jMenuCampaignInfluencePoints);

        if (Boolean.parseBoolean(client.getServerConfigs("UsePartsBlackMarket"))) {
            jMenuCampaignSubOther.add(jMenuCampaignPartsCache);
        }

        jMenuCampaignSubOther.add(jMenuCampaignDirectSell);

        // assemble the actual campaign menu...
        jMenuCampaign.add(jMenuCampaignMyStatus);
        jMenuCampaign.add(jMenuCampaignSubAttack);
        jMenuCampaign.addSeparator();
        jMenuCampaign.add(jMenuCampaignSubMerc);
        jMenuCampaign.add(jMenuCampaignSubTransfer);
        jMenuCampaign.add(jMenuCampaignSubStatus);
        jMenuCampaign.add(jMenuCampaignSubBays);
        jMenuCampaign.add(jMenuCampaignSubTechs);
        jMenuCampaign.add(jMenuCampaignSubOther);
        jMenuCampaign.addSeparator();
        jMenuCampaign.add(jMenuCampaignLogin);
        jMenuCampaign.add(jMenuCampaignActivate);
        jMenuCampaign.add(jMenuCampaignDeactivate);
        jMenuCampaign.add(jMenuCampaignLogout);

        /*
         * Games menu is assembled in a Menu Factory
         */

        // assemble host menu
        jMenuHost.add(jMenuCSHostAndJoin);
        jMenuHost.add(jMenuCSHostLoadAndJoin);
        jMenuHost.add(jMenuCSHostDedicated);
        jMenuHost.add(jMenuCSHostLoad);
        jMenuHost.add(jMenuCSHostStop);

        jMenuOptions.add(jMenuOptionsAutoScroll);
        jMenuOptions.add(jMenuOptionsMute);
        jMenuOptions.addSeparator();
        jMenuOptions.add(jMenuOptionsReloadAllData);

        jMenuLeaderShip.add(jMenuLeaderFluff);
        jMenuLeaderShip.add(jMenuLeaderMute);
        jMenuLeaderShip.add(jMenuLeaderPromote);
        jMenuLeaderShip.add(jMenuLeaderDemote);
        jMenuLeaderShip.add(jMenuLeaderFactionColor);
        jMenuLeaderShip.add(jMenuLeaderPlayerColor);
        jMenuLeaderShip.add(jMenuLeaderPurchaseFactory);
        jMenuLeaderShip.add(jMenuLeaderResearchTech);
        jMenuLeaderShip.add(jMenuLeaderResearchUnit);
        jMenuLeaderShip.add(jMenuLeaderSetComponentConversion);
        jMenuLeaderShip.add(jMenuLeaderViewFactionPartsCache);

        jMenuHelp.add(jMenuHelpAbout);
        jMenuHelp.add(jMenuHelpMemory);
        jMenuHelp.addSeparator();
        jMenuHelp.add(jMenuHelpViewUnit);
        jMenuHelp.add(jMenuHelpViewBuildTables);

        /*
         * Only add the trait viewer if the server allows traits. We'll use BattleMek traits as a proxy for ALL
         * trait types when deciding whether to show.
         */

        if (MathUtility.parseInt(client.getServerConfigs("chanceForTNforMek"), 0) > 0) {
            jMenuHelp.add(jMenuHelpViewTraits);
        }

        jMenuHelp.add(jMenuHelpHelp);
        jMenuHelp.add(jMenuHelpPilotSkills);
        jMenuHelp.add(jMenuHelpOpViewer);
        jMenuHelp.addSeparator();
        jMenuHelp.add(jMenuMekWarsBug);
        jMenuHelp.add(jMenuMegaMekBug);

        //@salient emoji
        jMenuEmoji.add(jMenuEmojiFlip);
        jMenuEmoji.add(jMenuEmojiShrug);
        jMenuEmoji.add(jMenuEmojiFingers);
        jMenuEmoji.add(jMenuEmojiKiss);
        jMenuEmoji.add(jMenuEmojiSmile);
        jMenuEmoji.add(jMenuEmojiDeal);

        /*
         * Admin menu setup used to be here. @urgru
         */

        /*
         * Mod menu setup used to be here. @urgru
         */

        jMenuBar1.add(jMenuFile);
        jMenuBar1.add(jMenuCampaign);
        jMenuBar1.add(jMenuAttackMenu);
        jMenuBar1.add(jMenuHost);
        jMenuBar1.add(jMenuOptions);
        jMenuBar1.add(jMenuLeaderShip);
        jMenuBar1.add(jMenuHelp);

        if (Boolean.parseBoolean(client.getServerConfigs("AllowEmoji"))) {
            jMenuBar1.add(jMenuEmoji);
        }

        jMenuBar1.add(jMenuOperations);
    }

    /**
     * Handles File &gt; Connect: asks the client to connect to the configured
     * server, then, if the connection succeeded (status no longer the literal
     * string {@code "Not connected"}), announces this client's version to the
     * server so it can be recorded/validated server-side.
     */
    public void jMenuFileConnect_actionPerformed() {
        client.connectToServer();
        // Set Version upon reconnection.
        if (!client.getStatus().equals("Not connected")) {
            client.sendChat(
                  String.format("%sc setclientversion#%s#%s", IClient.CAMPAIGN_PREFIX, client.getUsername()
                                                                            .trim(), IClient.CLIENT_VERSION));
        }
    }

    /** Handles File &gt; Register Nickname: opens the {@link RegisterNameDialog} to register the player's chosen nickname. */
    public void jMenuFileRegister_actionPerformed() {
        new RegisterNameDialog(client);
    }

    /**
     * Handles File &gt; Mail User: prompts for a recipient nickname (unless one
     * is supplied) and a message body, then sends it via
     * {@link IClient#processGUIInput(String)} as a GUI-prefixed {@code mail} command.
     *
     * @param Nickname the recipient's name, or {@code null}/blank to prompt the user for one
     */
    public void jMenuFileMail_actionPerformed(String Nickname) {
        String message;
        if (Nickname == null) {
            Nickname = JOptionPane.showInputDialog(getContentPane(),
                  "Nickname",
                  "Send mail to whom?",
                  JOptionPane.PLAIN_MESSAGE);
            if (Nickname == null) {
                return;
            }
        }
        message = JOptionPane.showInputDialog(getContentPane(),
              "message",
              String.format("Send mail to %s", Nickname),
              JOptionPane.PLAIN_MESSAGE);
        if (message == null) {
            return;
        }
        client.processGUIInput(String.format("%smail %s,%s", IClient.GUI_PREFIX, Nickname, message));
    }

    /** Handles File &gt; Last Online: prompts for a player name and asks the server when they were last online. */
    public void jMenuFileLastOnline_actionPerformed() {
        String Nickname;
        Nickname = JOptionPane.showInputDialog(getContentPane(), "Player name?");
        if (Nickname == null) {
            return;
        }
        client.sendChat(String.format("%sc lastonline#%s", IClient.CAMPAIGN_PREFIX, Nickname));
    }

    /** Handles File &gt; Exit: tells the client to disconnect/clean up, then immediately terminates the JVM. */
    public void jMenuFileExit_actionPerformed() {
        client.goodbye();
        System.exit(0);
    }

    /**
     * Handles Campaign &gt; Status &gt; Planetary Control: prompts for a faction
     * name and, if given a non-empty faction, an optional second ("secondary")
     * faction, then asks the server for the interstellar/planetary control
     * status filtered accordingly. If the first faction prompt is left blank,
     * an unfiltered {@code isstatus} request is sent instead.
     */
    public void jMenuCampaignISStatus_actionPerformed() {

        String House;
        String House2;

        HouseNameDialog factionDialog = new HouseNameDialog(client, "Faction", true, false);
        factionDialog.setVisible(true);
        House = factionDialog.getHouseName();
        factionDialog.dispose();

        if (House == null) {
            return;
        }

        if (!House.isEmpty()) {
            factionDialog = new HouseNameDialog(client, "Secondary Faction", true, false);
            factionDialog.setVisible(true);
            House2 = factionDialog.getHouseName();
            factionDialog.dispose();
            client.sendChat(String.format("%sc isstatus#%s#%s", IClient.CAMPAIGN_PREFIX, House, House2));
        } else {
            client.sendChat(String.format("%sc isstatus", IClient.CAMPAIGN_PREFIX));
        }
    }

    /** Handles Campaign &gt; Status &gt; Faction Status: prompts for a faction name and requests its status from the server. */
    public void jMenuCampaignFactionStatus_actionPerformed() {

        String House;

        HouseNameDialog factionDialog = new HouseNameDialog(client, "Faction", true, false);
        factionDialog.setVisible(true);
        House = factionDialog.getHouseName();
        factionDialog.dispose();

        if (House == null) {
            return;
        }

        client.sendChat(String.format("%sc faction#%s", IClient.CAMPAIGN_PREFIX, House));
    }

    /** Handles Campaign &gt; Mercenaries &gt; Mercenary Status: prompts for a mercenary player name (via a mercs-only picker) and requests their status. */
    public void jMenuMercStatus_actionPerformed() {
        PlayerNameDialog playerDialog = new PlayerNameDialog(client,
              "Which Merc do you want info on?",
              PlayerNameDialog.MERCS_ONLY);
        playerDialog.setVisible(true);
        String Merc = playerDialog.getPlayerName();
        playerDialog.dispose();

        if (Merc == null) {
            return;
        }

        client.sendChat(String.format("%sc mstatus#%s", IClient.CAMPAIGN_PREFIX, Merc));
    }

    /**
     * Handles Campaign &gt; Front Line &gt; Attack Options: requests attack
     * option checks for a specific lance/army id, or for all of the player's
     * armies when {@code lid} is {@code -1} (the value used by the top-level
     * menu item; a specific id is passed when invoked contextually, e.g. from
     * a table's right-click menu).
     *
     * @param lid the army/lance id to check, or {@code -1} for all armies
     */
    public void jMenuCommanderCheckAttack_actionPerformed(int lid) {
        if (lid == -1) {
            client.sendChat(String.format("%sc ca", IClient.CAMPAIGN_PREFIX));
        } else {
            client.sendChat(String.format("%sc ca#%s", IClient.CAMPAIGN_PREFIX, lid));
        }
    }

    /**
     * Handles Campaign &gt; Front Line &gt; Range Calculator: prompts for a
     * maximum range (light years) and a target faction, then asks the server
     * to list planets/targets within that range of the given faction.
     */
    public void jMenuCommanderRange_actionPerformed() {
        String range;
        String faction;

        range = JOptionPane.showInputDialog(getContentPane(), "Max distance in Light years?");

        if ((range == null) || (range.isEmpty())) {
            return;
        }

        HouseNameDialog factionDialog = new HouseNameDialog(client, "Faction", false, false);
        factionDialog.setVisible(true);
        faction = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((faction == null) || (faction.isEmpty())) {
            return;
        }

        client.sendChat(String.format("%sc range#%s#%s", IClient.CAMPAIGN_PREFIX, range, faction));
    }

    /**
     * Handles Campaign &gt; Front Line &gt; Find Contested Planets: prompts for
     * a minimum attacker-planet percentage threshold and a target faction, then
     * requests a list of contested planets between the player's own faction
     * (from {@link #thePlayer}) and the chosen target faction. Refuses (with a
     * chat message) if the target faction is the same as the player's own.
     */
    public void jMenuFindContestedPlanets_actionPerformed() { //BarukKhazad 20151129 - start 2
        String h1 = thePlayer.getHouse();
        String h2;
        String Perc = "20";
        Perc = (String) JOptionPane.showInputDialog(getContentPane(),
              "Minimum Attacker Planet Percentage? (1 to 100)",
              "",
              JOptionPane.PLAIN_MESSAGE,
              null,
              null,
              Perc);

        if ((Perc == null) || (Perc.isEmpty())) {
            return;
        }

        HouseNameDialog factionDialog = new HouseNameDialog(client, "Target Faction", false, false);
        factionDialog.setVisible(true);
        h2 = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((h2 == null) || (h2.isEmpty())) {
            return;
        }

        if (Objects.equals(h1, h2)) {
            client.addToChat("That is your faction. Target an enemy faction.");
            return;
        }

        client.sendChat(String.format("%sfindcp %s#%s#%s", IClient.CAMPAIGN_PREFIX, h1, h2, Perc));
    }  //BarukKhazad 20151129 - end 2

    /**
     * Handles Campaign &gt; Transfer &gt; Transfer Money (also invoked with a
     * pre-supplied recipient from context menus): prompts for a recipient
     * (unless one is supplied) restricted to same-faction players, then an
     * amount, and sends a {@code transfermoney} campaign command.
     *
     * @param name recipient player name, or {@code null}/blank to prompt via {@link PlayerNameDialog}
     */
    public void jMenuCommanderTransferMoney_actionPerformed(String name) {

        String targetPlayer;
        String Amount;

        if ((name == null) || name.trim().isEmpty()) {
            PlayerNameDialog pnd = new PlayerNameDialog(client, "Transfer Recipient", PlayerNameDialog.FACTION_ONLY);
            pnd.setVisible(true);
            targetPlayer = pnd.getPlayerName();
            pnd.dispose();
        } else {
            targetPlayer = name;
        }

        if (targetPlayer == null) {
            return;
        }

        Amount = JOptionPane.showInputDialog(getContentPane(),
              "Amount",
              String.format("Send %s to %s", client.moneyOrFluMessage(true, true, -2), targetPlayer),
              JOptionPane.PLAIN_MESSAGE);

        if (Amount == null) {
            return;
        }

        client.sendChat(String.format("%sc transfermoney#%s#%s", IClient.CAMPAIGN_PREFIX, targetPlayer, Amount));
    }

    /**
     * Transfers Reward Points to another same-faction player: prompts for a
     * recipient (unless supplied) and an amount, then sends a
     * {@code transferrewardpoints} campaign command. Not wired to a static menu
     * item text label directly (the label uses the server-configured "RP" name).
     *
     * @param name recipient player name, or {@code null}/blank to prompt via {@link PlayerNameDialog}
     */
    public void jMenuCommanderTransferRewardPoints_actionPerformed(String name) {
        String targetPlayer;
        String Amount;

        if ((name == null) || name.trim().isEmpty()) {
            PlayerNameDialog pnd = new PlayerNameDialog(client, "Transfer Recipient", PlayerNameDialog.FACTION_ONLY);
            pnd.setVisible(true);
            targetPlayer = pnd.getPlayerName();
            pnd.dispose();
        } else {
            targetPlayer = name;
        }

        if (targetPlayer == null) {
            return;
        }

        Amount = JOptionPane.showInputDialog(getContentPane(),
              "Amount",
              String.format("Send %s to %s", client.getServerConfigs("RPShortName"), targetPlayer),
              JOptionPane.PLAIN_MESSAGE);

        if (Amount == null) {
            return;
        }

        client.sendChat(String.format("%sc transferrewardpoints#%s#%s", IClient.CAMPAIGN_PREFIX, targetPlayer, Amount));
    }

    /**
     * Transfers Influence Points to another same-faction player: prompts for a
     * recipient (unless supplied) and an amount, then sends a
     * {@code transferinfluence} campaign command.
     *
     * @param name recipient player name, or {@code null}/blank to prompt via {@link PlayerNameDialog}
     */
    //@Salient
    public void jMenuCommanderTransferInfluence_actionPerformed(String name) {
        String targetPlayer;
        String Amount;

        if ((name == null) || name.trim().isEmpty()) {
            PlayerNameDialog pnd = new PlayerNameDialog(client, "Transfer Recipient", PlayerNameDialog.FACTION_ONLY);
            pnd.setVisible(true);
            targetPlayer = pnd.getPlayerName();
            pnd.dispose();
        } else {
            targetPlayer = name;
        }

        if (targetPlayer == null) {
            return;
        }

        Amount = JOptionPane.showInputDialog(getContentPane(),
              "Amount",
              String.format("Send %s to %s", client.getServerConfigs("FluShortName"), targetPlayer),
              JOptionPane.PLAIN_MESSAGE);

        if (Amount == null) {
            return;
        }

        client.sendChat(String.format("%sc transferinfluence#%s#%s", IClient.CAMPAIGN_PREFIX, targetPlayer, Amount));
    }

    /**
     * Handles Campaign &gt; Transfer &gt; Transfer Unit: prompts for a recipient
     * (unless supplied) and a unit to transfer (via {@link UnitSelectionDialog},
     * unless a unit id is supplied), then sends a {@code transferunit} campaign
     * command.
     *
     * @param name recipient player name, or {@code null}/blank to prompt
     * @param mid  the unit id to transfer, or {@code -1} to prompt for one
     */
    public void jMenuCommanderTransferUnit_actionPerformed(String name, int mid) {

        String targetPlayer;

        if ((name == null) || name.trim().isEmpty()) {
            PlayerNameDialog pnd = new PlayerNameDialog(client, "Transfer Recipient", PlayerNameDialog.FACTION_ONLY);
            pnd.setVisible(true);
            targetPlayer = pnd.getPlayerName();
            pnd.dispose();
        } else {
            targetPlayer = name;
        }

        if (targetPlayer == null) {
            return;
        }

        if (mid == -1) {
            UnitSelectionDialog usd = new UnitSelectionDialog(client, "Transfer Unit", "Select unit to transfer:");
            usd.setVisible(true);
            mid = Integer.parseInt(usd.getUnitID());
            usd.dispose();
        }

        if (mid == -1) {
            return;
        }

        client.sendChat(String.format("%sc transferunit#%s#%s", IClient.CAMPAIGN_PREFIX, targetPlayer, mid));
    }

    /**
     * Opens the {@link SellUnitDialog} to list a single unit for sale on the
     * black market. Not wired to a static menu item; invoked contextually
     * (e.g. from a unit table's right-click menu) with a specific unit id.
     *
     * @param mid the id of the unit (owned by {@link #thePlayer}) to sell
     */
    public void jMenuCommanderAddToBM_actionPerformed(int mid) {

        Vector<CUnit> toSell = new Vector<>(1, 1);
        toSell.add(client.getPlayer().getUnit(mid));

        SellUnitDialog sud = new SellUnitDialog(this, client, toSell);
        sud.setVisible(true);
    }

    /**
     * Disbands/removes an army (lance): prompts for an army id (unless
     * supplied) and sends a {@code rma} (remove army) campaign command.
     *
     * @param lid the army id to remove, or {@code -1} to prompt for one
     */
    public void jMenuCommanderRemoveLance_actionPerformed(int lid) {
        String LanceID;
        if (lid == -1) {
            LanceID = JOptionPane.showInputDialog(getContentPane(), "Army ID?");
            if (LanceID == null) {
                return;
            }
            lid = Integer.parseInt(LanceID);
        }
        client.sendChat(String.format("%sc rma#%s", IClient.CAMPAIGN_PREFIX, lid));
    }

    /**
     * Renames a pilot: prompts for a new name and sends a {@code namepilot}
     * campaign command.
     * <p>
     * Only called from HQ, via MechTableMouseAdapter. Will always have valid
     * unit id.
     *
     * @param uid the id of the unit whose pilot is being renamed
     */
    public void jMenuCommanderNamePilot_actionPerformed(int uid) {
        String newName = JOptionPane.showInputDialog(getContentPane(), "Pilot's Name?");
        if (newName == null) {
            return;
        }
        client.sendChat(String.format("%sc namepilot#%s#%s", IClient.CAMPAIGN_PREFIX, uid, newName));
    }

    /**
     * Renames an army (lance): prompts for a new name, pre-filled with the
     * army's current name. Entering a blank name sends the literal string
     * {@code "clear"} as the new name (server-side convention for clearing a
     * custom army name back to its default).
     * <p>
     * Only called from HQ, via MechTableMouseAdapter. Will always have valid
     * army id.
     *
     * @param aid the army id to rename; if it does not resolve to an army owned
     *            by the current player, this method silently does nothing
     */
    public void jMenuCommanderNameArmy_actionPerformed(int aid) {
        CArmy selectedArmy = client.getPlayer().getArmy(aid);
        if (selectedArmy == null) {
            return;
        }

        String newName = JOptionPane.showInputDialog(getContentPane(),
              "New army name? [Leave blank to clear]",
              selectedArmy.getName());
        if (newName == null) {
            return;
        }

        if (newName.trim().isEmpty()) {
            newName = "clear";
        }

        client.sendChat(String.format("%sc namearmy#%s#%s", IClient.CAMPAIGN_PREFIX, aid, newName));
    }

    /**
     * Locks an army so that only its owning player can command it (server-side
     * "playerlockarmy" toggle). No confirmation dialog is shown.
     *
     * @param aid the army id to lock
     */
    public void jMenuCommanderPlayerLockArmy_actionPerformed(int aid) {
        client.sendChat(String.format("%sc playerlockarmy#%s", IClient.CAMPAIGN_PREFIX, aid));
    }

    /**
     * Unlocks a previously player-locked army (server-side "playerunlockarmy" toggle).
     * No confirmation dialog is shown.
     *
     * @param aid the army id to unlock
     */
    public void jMenuCommanderPlayerUnlockArmy_actionPerformed(int aid) {
        client.sendChat(String.format("%sc playerunlockarmy#%s", IClient.CAMPAIGN_PREFIX, aid));
    }

    /**
     * Toggles whether an army is disabled/available for play (server-side
     * "togglearmydisabled" flag flip). No confirmation dialog is shown.
     *
     * @param aid the army id to toggle
     */
    public void jMenuCommanderDisableArmy_actionPerformed(int aid) {
        // Toggle armyDisabled
        client.sendChat(String.format("%sc togglearmydisabled#%s", IClient.CAMPAIGN_PREFIX, aid));
    }

    /**
     * Sets an army's lower unit-count limiter: opponents fielding fewer units
     * than this limit will not be matched against this army. Prompts with an
     * explanatory example and the current lower limit pre-filled.
     * <p>
     * Only called from HQ, via MechTableMouseAdapter. Will always have valid
     * army id.
     *
     * @param aid the army id to configure; if it does not resolve to an army
     *            owned by the current player, this method silently does nothing
     */
    public void jMenuCommanderSetLowerUnitLimit_actionPerformed(int aid) {

        CArmy selectedArmy = client.getPlayer().getArmy(aid);
        if (selectedArmy == null) {
            return;
        }

        int newLimit;

        String example = "Example: An Army of 8 units with a Lower Limit of<br>" +
                               "4 will not be able to fight an Army with only 3 units.<br>" +
                               "This can be useful if you want to avoid fighting a<br>" +
                               "small number of super heavy/levelled units.";

        String limit = JOptionPane.showInputDialog(getContentPane(),
              String.format("<HTML>Lower Limit? [-1 to disable the limit]<i><br><br>%s<br></i></HTML>", example),
              Integer.toString(selectedArmy.getLowerLimiter()),
              JOptionPane.PLAIN_MESSAGE);

        if (limit == null) {
            return;
        }

        newLimit = Integer.parseInt(limit);
        client.sendChat(String.format("%sc all#%s#%s", IClient.CAMPAIGN_PREFIX, aid, newLimit));
    }

    /**
     * Sets an army's upper unit-count limiter: opponents fielding more units
     * than this limit will not be matched against this army. Prompts with an
     * explanatory example and a "current value" pre-filled in the dialog.
     * <p>
     * NOTE (pre-existing quirk): the dialog's pre-filled default is populated
     * from {@code selectedArmy.getLowerLimiter()} rather than an upper-limit
     * getter, so the value shown as the "current" upper limit is actually the
     * army's lower limit. This looks like a copy/paste bug from
     * {@link #jMenuCommanderSetLowerUnitLimit_actionPerformed(int)}; the value
     * the user types in is still sent as the upper limit ("aul" command), so
     * only the displayed default is affected, not the value ultimately applied.
     * <p>
     * Only called from HQ, via MechTableMouseAdapter. Will always have valid
     * army id.
     *
     * @param aid the army id to configure; if it does not resolve to an army
     *            owned by the current player, this method silently does nothing
     */
    public void jMenuCommanderSetUpperUnitLimit_actionPerformed(int aid) {

        CArmy selectedArmy = client.getPlayer().getArmy(aid);
        if (selectedArmy == null) {
            return;
        }

        int newLimit;

        // generate an example string.
        String example = "Example: An Army of 4 units with an Upper Limit of 5<br>" +
                               "will not be able to fight against Armies with more than<br>" +
                               "9 units. This can be useful if you don't want to play<br>" +
                               "against swarms";

        String limit = JOptionPane.showInputDialog(getContentPane(),
              String.format("<HTML>Upper Limit? [-1 to disable the limit]<i><br><br>%s<br></i></HTML>", example),
              // NOTE: pre-fills with the LOWER limiter's value; see class-level note above (likely a copy/paste bug).
              Integer.toString(selectedArmy.getLowerLimiter()),
              JOptionPane.PLAIN_MESSAGE);

        if (limit == null) {
            return;
        }

        newLimit = Integer.parseInt(limit);
        client.sendChat(String.format("%sc aul#%s#%s", IClient.CAMPAIGN_PREFIX, aid, newLimit));
    }

    /**
     * Sets an army's "opposing force size to face" preference: the force size
     * this army expects/prefers to be matched against when requesting a match.
     * Prompts with the current value (from {@link CArmy#getOpForceSize()})
     * pre-filled, and sends an {@code aofs} campaign command. Note the input is
     * taken as a raw string and forwarded without numeric validation here.
     * <p>
     * Only called from HQ, via MechTableMouseAdapter. Will always have valid
     * army id.
     *
     * @param aid the army id to configure; if it does not resolve to an army
     *            owned by the current player, this method silently does nothing
     */
    public void jMenuCommanderSetForceSizeToFace_actionPerformed(int aid) {

        CArmy selectedArmy = client.getPlayer().getArmy(aid);
        if (selectedArmy == null) {
            return;
        }

        // generate an example string.
        String example = "This is the force size you expect to face when you request a match";

        String force = JOptionPane.showInputDialog(getContentPane(),
              String.format("<HTML>Force Size To Face\t? [-1 to disable the limit]<i><br><br>%s<br></i></HTML>", example),
              Float.toString(selectedArmy.getOpForceSize()),
              JOptionPane.PLAIN_MESSAGE);

        if (force == null) {
            return;
        }

        client.sendChat(String.format("%sc aofs#%s#%s", IClient.CAMPAIGN_PREFIX, aid, force));

    }

    /**
     * Handles Campaign &gt; Other &gt; Set Logo: prompts for a logo image URL
     * (pre-filled with the player's current logo) and sends a
     * {@code setmylogo} campaign command.
     */
    public void jMenuCommanderLogo_actionPerformed() {
        String LogoURL;
        LogoURL = JOptionPane.showInputDialog(getContentPane(),
              "URL? (i.e. https://www.mysite.com/mypic.jpg)",
              client.getPlayer().getMyLogo());
        if (LogoURL == null) {
            return;
        }
        client.sendChat(String.format("%sc setmylogo#%s", IClient.CAMPAIGN_PREFIX, LogoURL));
    }

    /** Handles Campaign &gt; Personnel &gt; Pilots &gt; View Pilot Queue: requests display of the player's personal pilot queue. */
    public void jMenuCommanderPersonalPilotQueue_actionPerformed() {
        client.sendChat(String.format("%sc displayplayerpersonalpilotqueue", IClient.CAMPAIGN_PREFIX));
    }

    /**
     * Handles Campaign &gt; Transfer &gt; Transfer Pilot: prompts for a
     * recipient (unless supplied), then walks the user through selecting a
     * unit type, weight class, and a specific pilot from the player's personal
     * pilot queue for that type/weight (via a combo box built by
     * {@link #getStringJComboBox}), and finally sends a {@code transferpilot}
     * campaign command identifying the pilot by its position in that queue.
     * Shows an error dialog and aborts if no pilots exist for the chosen
     * type/weight combination.
     *
     * @param name recipient player name, or {@code null}/blank to prompt via {@link PlayerNameDialog}
     */
    public void jMenuCommanderTransferPilot_actionPerformed(String name) {

        // get player
        String targetPlayer;

        if ((name == null) || name.trim().isEmpty()) {
            PlayerNameDialog pnd = new PlayerNameDialog(client, "Transfer Recipient", PlayerNameDialog.FACTION_ONLY);
            pnd.setVisible(true);
            targetPlayer = pnd.getPlayerName();
            pnd.dispose();
        } else {
            targetPlayer = name;
        }

        if (targetPlayer == null) {
            return;
        }

        // arrays for message box usage
        Object[] pWeightClass = { "Light", "Medium", "Heavy", "Assault" };
        Object[] pUnitType = { "Mek", "Proto" };

        int weightClass;
        int unitType;

        // determine the unit type to use
        String pUnitTypeString = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select a pilot unit type",
              "Unit Type Selection",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              pUnitType,
              pUnitType[0]);

        if ((pUnitTypeString == null) || (pUnitTypeString.isEmpty())) {
            return;
        }

        if (pUnitTypeString.equals("Mek")) {
            unitType = Unit.MEK;
        } else {
            unitType = Unit.PROTOMEK;
        }

        // determine the weight class to use
        String pWeightClassString = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select a pilot unit size",
              "Weight Class Selection",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              pWeightClass,
              pWeightClass[0]);

        if ((pWeightClassString == null) || (pWeightClassString.isEmpty())) {
            return;
        }

        weightClass = Unit.getWeightIDForName(pWeightClassString);

        if (client.getPlayer().getPersonalPilotQueue().getPilotQueue(unitType, weightClass).isEmpty()) {
            JOptionPane.showMessageDialog(null,
                  String.format("You do not have any pilots for %s %s", StringUtils.aOrAn(pWeightClassString,
                        true), pUnitTypeString),
                  "No Pilots!",
                  JOptionPane.PLAIN_MESSAGE);
            return;
        }

        Object[] pilots = client.getPlayer().getPersonalPilotQueue().getPilotQueue(unitType, weightClass).toArray();

        JComboBox<String> combo = getStringJComboBox(pilots, unitType);
        JOptionPane jop = new JOptionPane(combo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(client.getMainFrame(), "Select a pilot.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        int position = combo.getSelectedIndex();

        int value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        client.sendChat(
              IClient.CAMPAIGN_PREFIX +
                    "c transferpilot#" +
                    targetPlayer +
                    "#" +
                    unitType +
                    "#" +
                    weightClass +
                    "#" +
                    position);
    }

    /**
     * Handles Campaign &gt; Personnel &gt; Pilots &gt; Fire Pilot: walks the user
     * through selecting a unit type, weight class, and a specific pilot from
     * the player's personal pilot queue (mirroring
     * {@link #jMenuCommanderTransferPilot_actionPerformed}, but with no
     * recipient), then sends a {@code donatepilot} campaign command
     * (server-side, "donating"/firing a personal pilot back to the general pool).
     */
    public void jMenuCommanderDonatePersonalPilot_actionPerformed() {
        boolean allowProto = Boolean.parseBoolean(client.getServerConfigs("UseProtoMek"));

        Object[] pWeightClass = { "Light", "Medium", "Heavy", "Assault" };
        Object[] pUnitType;

        if (allowProto) {
            pUnitType = new Object[] { "Mek", "Proto" };
        } else {
            pUnitType = new Object[] { "Mek" };
        }

        int weightClass;
        int unitType;

        // determine the unit type to use
        String pUnitTypeString = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select a pilot unit type",
              "Unit Type Selection",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              pUnitType,
              pUnitType[0]);

        if ((pUnitTypeString == null) || (pUnitTypeString.isEmpty())) {
            return;
        }

        if (pUnitTypeString.equals("Mek")) {
            unitType = Unit.MEK;
        } else {
            unitType = Unit.PROTOMEK;
        }

        // determine the weight class to use
        String pWeightClassString = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select a pilot unit size",
              "Weight Class Selection",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              pWeightClass,
              pWeightClass[0]);

        if ((pWeightClassString == null) || (pWeightClassString.isEmpty())) {
            return;
        }

        weightClass = Unit.getWeightIDForName(pWeightClassString);

        if (client.getPlayer().getPersonalPilotQueue().getPilotQueue(unitType, weightClass).isEmpty()) {
            JOptionPane.showMessageDialog(null,
                  String.format("You do not have any pilots for %s %s.", StringUtils.aOrAn(pWeightClassString,
                        true), pUnitTypeString),
                  "No Pilots!",
                  JOptionPane.PLAIN_MESSAGE);
            return;
        }

        Object[] pilots = client.getPlayer().getPersonalPilotQueue().getPilotQueue(unitType, weightClass).toArray();

        JComboBox<String> combo = getStringJComboBox(pilots, unitType);
        JOptionPane jop = new JOptionPane(combo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(client.getMainFrame(), "Select a pilot.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        int position = combo.getSelectedIndex();

        int value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        client.sendChat(String.format("%sc donatepilot#%s#%s#%s", IClient.CAMPAIGN_PREFIX, unitType, weightClass, position));
    }

    /**
     * Handles Campaign &gt; Other &gt; Direct Sell Unit: prompts for a buyer
     * (unless supplied) and a unit to sell (via {@link UnitSelectionDialog},
     * unless a unit id is supplied), computes and displays the applicable
     * service fee (looked up from server config as
     * {@code SellDirect<Weight><Type>Price}), prompts for an asking price, and
     * sends a {@code directsellunit} campaign command.
     *
     * @param name buyer player name, or {@code null}/blank to prompt (any player allowed)
     * @param id   the unit id to sell, or {@code null}/blank to prompt for one
     */
    public void jMenuCommanderDirectSell_actionPerformed(String name, String id) {

        // get player
        String buyer;
        String unitID;
        String price;

        if ((name == null) || name.trim().isEmpty()) {
            PlayerNameDialog pnd = new PlayerNameDialog(client, "Buyer", PlayerNameDialog.ANY_PLAYER);
            pnd.setVisible(true);
            buyer = pnd.getPlayerName();
            pnd.dispose();
        } else {
            buyer = name;
        }

        if (buyer == null) {
            return;
        }

        if ((id == null) || id.trim().isEmpty()) {
            UnitSelectionDialog usd = new UnitSelectionDialog(client, "Unit", "Select a unit to sell");
            usd.setVisible(true);
            unitID = usd.getUnitID();
            usd.dispose();
        } else {
            unitID = id;
        }

        CUnit unit = client.getPlayer().getUnit(MathUtility.parseInt(unitID, -1));

        String serviceFee = String.format("SellDirect%s%sPrice", Unit.getWeightClassDesc(unit.getWeightClass()), Unit.getTypeClassDesc(unit.getType()));
        price = JOptionPane.showInputDialog(getContentPane(),
              String.format("""
How much do you wish to offer? (%s)
\rPlease note a service charge of %s will be added.""", client.moneyOrFluMessage(true, true, -2), client.moneyOrFluMessage(true,
                    true,
                    MathUtility.parseInt(client.getServerConfigs(serviceFee), 0))));

        if ((price == null) || (price.isEmpty())) {
            return;
        }

        client.sendChat(
              String.format("%sc directsellunit#%s#%s#%s#%s", IClient.CAMPAIGN_PREFIX, buyer, client.getPlayer()
                                                                               .getName(), unitID, price));
    }

    /**
     * Handles Campaign &gt; Mercenaries &gt; Offer a Mercenary Contract: prompts
     * for a mercenary player, an amount, a contract type (Exp/Land/Units/Components/Delay,
     * picked from a fixed combo box), and a duration, then sends an
     * {@code offercontract} campaign command.
     */
    public void jMenuMercOfferContract_actionPerformed() {
        String Amount;
        String Duration;
        PlayerNameDialog playerDialog = new PlayerNameDialog(client,
              "Which Merc do you want to offer a contract?",
              PlayerNameDialog.MERCS_ONLY);
        playerDialog.setVisible(true);
        String Merc = playerDialog.getPlayerName();
        playerDialog.dispose();

        if (Merc == null) {
            return;
        }

        Amount = JOptionPane.showInputDialog(getContentPane(),
              String.format("How much do you wish to offer? (%s)", client.moneyOrFluMessage(true, true, -2)));
        if (Amount == null) {
            return;
        }

        Vector<String> techTypes = new Vector<>(5, 1);
        techTypes.add("Exp");
        techTypes.add("Land");
        techTypes.add("Units");
        techTypes.add("Components");
        techTypes.add("Delay");
        JComboBox<String> combo = new JComboBox<>(techTypes);
        combo.setEditable(false);
        JOptionPane jop = new JOptionPane(combo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(this, "Select contract type.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        String Type = (String) combo.getSelectedItem();

        int value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        Duration = JOptionPane.showInputDialog(getContentPane(), "Duration of the contract?");
        if (Duration == null) {
            return;
        }

        client.sendChat(
              String.format("%sc offercontract#%s#%s#%s#%s", IClient.CAMPAIGN_PREFIX, Merc, Amount, Duration, Type));
    }

    /**
     * Handles Campaign &gt; Other &gt; Defect: if the server allows single-player
     * factions, prompts for a brand-new faction name and short name and sends
     * a {@code defect#<name>#newfaction#<shortName>} command (creating and
     * defecting to a new one-player faction). Otherwise, prompts for an
     * existing faction (via {@link HouseNameDialog}, excluding new-faction
     * creation) and sends a plain {@code defect#<house>} command.
     */
    public void jMenuCommanderDefect_actionPerformed() {
        // String Confirmation;
        String House;

        if (Boolean.parseBoolean(client.getServerConfigs("AllowSinglePlayerFactions"))) {
            House = JOptionPane.showInputDialog(getContentPane(),
                  "Name of your new Faction?",
                  "New Faction Name?",
                  JOptionPane.QUESTION_MESSAGE);
            if (House == null) {
                return;
            }
            String shortName = JOptionPane.showInputDialog(getContentPane(),
                  String.format("%s's short name?", House),
                  "Short Name?",
                  JOptionPane.QUESTION_MESSAGE);
            if (shortName == null) {
                return;
            }
            client.sendChat(String.format("%sc defect#%s#newfaction#%s", IClient.CAMPAIGN_PREFIX, House, shortName));

            return;
        }

        HouseNameDialog factionDialog = new HouseNameDialog(client, "Defect to faction:", false, true);
        factionDialog.setVisible(true);
        House = factionDialog.getHouseName();
        factionDialog.dispose();

        if (House == null) {
            return;
        }

        // send unconfirmed defection command
        client.sendChat(String.format("%sc defect#%s", IClient.CAMPAIGN_PREFIX, House));
    }

    /**
     * Handles Campaign &gt; Other &gt; Self Promote (shown only when the server
     * config {@code Self_Promote_Subfaction} is enabled): prompts for a
     * sub-faction name and sends a {@code selfpromote} campaign command.
     */
    public void jMenuCommanderSelfPromote_actionPerformed() {

        SubFactionNameDialog subFactionDialog = new SubFactionNameDialog(client,
              "SubFaction",
              client.getPlayer().getHouse());
        subFactionDialog.setVisible(true);
        String subFactionName = subFactionDialog.getSubFactionName();
        subFactionDialog.dispose();

        if ((subFactionName == null) || (subFactionName.isEmpty())) {
            return;
        }

        client.sendChat(String.format("%sc selfpromote#%s", IClient.CAMPAIGN_PREFIX, subFactionName));
    }

    /** Handles Campaign &gt; Other &gt; Check MiniCampaign Status (shown only when {@code Enable_MiniCampaign} is set): requests mini-campaign status. */
    public void jMenuCommanderReportStatusMC_actionPerformed() {
        client.sendChat(String.format("%sc reportstatusmc#", IClient.CAMPAIGN_PREFIX));
    }

    /**
     * Handles Campaign &gt; Personnel &gt; Techs &gt; Fire Techs: prompts for a
     * count of techs to fire, validating against the player's current tech
     * count when not using advance repairs. Under advance repairs, additionally
     * prompts for a tech quality tier (Green/Regular/Vet/Elite) and sends
     * {@code firetechs#<count>#<tier>}; otherwise sends {@code firetechs#<count>}.
     */
    public void jMenuCommanderFireTechs_actionPerformed() {

        String techsToFire = JOptionPane.showInputDialog(getContentPane(),
              "How many techs do you want to fire?");
        if ((techsToFire == null) || (techsToFire.trim().isEmpty())) {
            return;
        }

        int techs = Integer.parseInt(techsToFire);
        if (!useAdvanceRepairs && (thePlayer.getTechs() <= 0)) {
            client.addToChat("<b>You have no hired techs to fire.<b>");
            return;
        }

        if (!useAdvanceRepairs && ((techs < 1) || (techs > thePlayer.getTechs()))) {
            client.addToChat(String.format("<b>Try picking a number between 1 and %s<b>", thePlayer.getTechs()));
            return;
        }

        if (useAdvanceRepairs) {

            java.util.Vector<String> techTypes = new java.util.Vector<>(4, 1);
            techTypes.add("Green");
            techTypes.add("Regular");
            techTypes.add("Vet");
            techTypes.add("Elite");
            JComboBox<String> combo = new JComboBox<>(techTypes);
            combo.setEditable(true);
            JOptionPane jop = new JOptionPane(combo,
                  JOptionPane.QUESTION_MESSAGE,
                  JOptionPane.OK_CANCEL_OPTION);

            JDialog dlg = jop.createDialog(client.getMainFrame(), "Select tech to fire.");
            combo.grabFocus();
            combo.getEditor().selectAll();

            dlg.setVisible(true);

            int techType = combo.getSelectedIndex();

            if (techType < 0) {
                return;
            }

            int value = (Integer) jop.getValue();

            if (value == JOptionPane.CANCEL_OPTION) {
                return;
            }

            client.sendChat(String.format("%sc firetechs#%s#%s", IClient.CAMPAIGN_PREFIX, techs, techType));
        } else {
            client.sendChat(String.format("%sc firetechs#%s", IClient.CAMPAIGN_PREFIX, techs));
        }
    }

    /**
     * Handles Campaign &gt; Personnel &gt; Techs &gt; Hire Techs: prompts for a
     * count of techs to hire (showing the green-tech unit cost in the prompt
     * when advance repairs is on and regular techs are not hireable). When
     * advance repairs AND regular-tech hiring are both enabled, additionally
     * prompts to choose Green vs. Regular tech quality (with costs shown) and
     * sends {@code hiretechs#<count>#<tier>}; otherwise sends
     * {@code hiretechs#<count>}.
     */
    public void jMenuCommanderHireTechs_actionPerformed() {
        boolean allowRegTechs = Boolean.parseBoolean(client.getServerConfigs("AllowRegTechsToBeHired"));

        String techsToHire;

        if (useAdvanceRepairs && !allowRegTechs) {
            techsToHire = JOptionPane.showInputDialog(getContentPane(),
                  String.format("How many green techs do you want to hire?(%s%s)", Integer.parseInt(client.getServerConfigs(
                        "GreenTechHireCost")), client.moneyOrFluMessage(true, true, -2)));
        } else {
            techsToHire = JOptionPane.showInputDialog(getContentPane(),
                  "How many techs do you want to hire?");
        }

        if ((techsToHire == null) || (techsToHire.isEmpty())) {
            return;
        }

        int techs = Integer.parseInt(techsToHire);
        if (techs < 1) {
            client.addToChat("Try picking a number greater then 0");
            return;
        }

        if (useAdvanceRepairs && allowRegTechs) {

            java.util.Vector<String> techTypes = new java.util.Vector<>(2, 1);
            techTypes.add(String.format("Green %s%s", MathUtility.parseInt(client.getServerConfigs("GreenTechHireCost"),
                  0), client.moneyOrFluMessage(true, true, -2)));
            techTypes.add(String.format("Regular %s%s", MathUtility.parseInt(client.getServerConfigs("RegTechHireCost"),
                  0), client.moneyOrFluMessage(true, true, -2)));
            JComboBox<String> combo = new JComboBox<>(techTypes);
            combo.setEditable(false);
            JOptionPane jop = new JOptionPane(combo,
                  JOptionPane.QUESTION_MESSAGE,
                  JOptionPane.OK_CANCEL_OPTION);

            JDialog dlg = jop.createDialog(client.getMainFrame(), "Select tech to hire.");
            combo.grabFocus();
            combo.getEditor().selectAll();

            dlg.setVisible(true);

            int techType = combo.getSelectedIndex();

            if (techType < 0) {
                return;
            }

            int value = (Integer) jop.getValue();

            if (value == JOptionPane.CANCEL_OPTION) {
                return;
            }

            client.sendChat(String.format("%sc hiretechs#%s#%s", IClient.CAMPAIGN_PREFIX, techs, techType));
        } else {
            client.sendChat(String.format("%sc hiretechs#%s", IClient.CAMPAIGN_PREFIX, techs));
        }
    }

    /**
     * Handles Campaign &gt; Personnel &gt; Pilots &gt; Hire Pilots (shown only
     * when personal pilot queues are enabled): walks the user through
     * selecting a unit type (Mek, and Proto/Aero if the server allows them), a
     * weight class, and a quantity to hire, then sends a
     * {@code buypilotsfromhouse} campaign command.
     */
    public void jMenuCampaignSubOtherBuyPilots_actionPerformed() {
        boolean allowProto = MathUtility.parseBoolean(client.getServerConfigs("UseProtoMek"), false);
        boolean allowAero = MathUtility.parseBoolean(client.getServerConfigs("UseAero"), false);
        int unitType;
        int unitClass;

        Object[] pWeightClass = { "Light", "Medium", "Heavy", "Assault" };
        Object[] pUnitType;

        if (allowProto && allowAero) {
            pUnitType = new Object[] { "Mek", "Proto", "Aero" };
        } else if (allowProto) {
            pUnitType = new Object[] { "Mek", "Proto" };
        } else if (allowAero) {
            pUnitType = new Object[] { "Mek", "Aero" };
        } else {
            pUnitType = new Object[] { "Mek" };
        }

        // determine the unit type to use
        String pUnitTypeString = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select unit type",
              "Unit Type Selection",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              pUnitType,
              pUnitType[0]);

        if ((pUnitTypeString == null) || (pUnitTypeString.isEmpty())) {
            return;
        }

        unitType = Unit.getTypeIDForName(pUnitTypeString);

        // determine the weight class to use
        String pWeightClassString = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select unit size",
              "Weight Class Selection",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              pWeightClass,
              pWeightClass[0]);

        if ((pWeightClassString == null) || (pWeightClassString.isEmpty())) {
            return;
        }

        unitClass = Unit.getWeightIDForName(pWeightClassString);

        String numberOfPilots = JOptionPane.showInputDialog(getContentPane(),
              "How many pilots do you want to hire?",
              1);

        if ((numberOfPilots == null) || (numberOfPilots.isEmpty())) {
            return;
        }

        client.sendChat(String.format("%sc buypilotsfromhouse#%s#%s#%s", IClient.CAMPAIGN_PREFIX, unitType, unitClass, numberOfPilots));
    }

    /**
     * Handles Campaign &gt; Bays &gt; Return Bays: prompts for a count of bays
     * to return, validated against the player's current free bay count, then
     * sends a {@code sellbays} campaign command.
     */
    public void jMenuCommanderSellBays_actionPerformed() {
        String baysToFire = JOptionPane.showInputDialog(getContentPane(),
              "How many bays do you want to return??");

        if ((baysToFire == null) || (baysToFire.isEmpty())) {
            return;
        }

        int bays = Integer.parseInt(baysToFire);
        if (thePlayer.getFreeBays() <= 0) {
            client.addToChat("<b>You have no free bays to return.<b>");
            return;
        }
        if ((bays < 1) || (bays > thePlayer.getFreeBays())) {
            client.addToChat(String.format("<b>Try picking a number between 1 and %s<b>", thePlayer.getFreeBays()));
            return;
        }
        client.sendChat(String.format("%sc sellbays#%s", IClient.CAMPAIGN_PREFIX, bays));
    }

    /**
     * Handles Campaign &gt; Other &gt; View Parts (shown only when
     * {@code UsePartsBlackMarket} is set): builds an HTML table of the
     * player's black-market parts cache contents and routes it to the misc
     * chat output via {@link IClient#doParseDataInput(String)}.
     */
    public void jMenuCampaignPartsCache_actionPerformed() {
        CPlayer p = client.getPlayer();
        StringBuilder result = new StringBuilder();
        int year = Integer.parseInt(client.getServerConfigs("CampaignYear"));

        result.append(p.getPartsCache().tableComponents(year));
        client.doParseDataInput(String.format("SM|%s", result));
    }

    /**
     * Handles Campaign &gt; Bays &gt; Lease Bays: prompts for a count of bays to
     * lease (showing the per-bay cost), validates the count is positive, and
     * sends a {@code buybays} campaign command.
     */
    public void jMenuCommanderBuyBays_actionPerformed() {
        String baysToHire = JOptionPane.showInputDialog(getContentPane(),
              String.format("How many bays do you want to lease?(%s%s)", MathUtility.parseInt(client.getServerConfigs(
                    "CostToBuyNewBay"), 0), client.moneyOrFluMessage(
                    true,
                    true,
                    -2)));

        if ((baysToHire == null) || (baysToHire.isEmpty())) {
            return;
        }

        int bays = Integer.parseInt(baysToHire);
        if (bays < 1) {
            client.addToChat("Try picking a number greater then 0");
            return;
        }
        client.sendChat(String.format("%sc buybays#%s", IClient.CAMPAIGN_PREFIX, bays));
    }

    /**
     * Handles Help &gt; Build Table Viewer: if the player's access level is high
     * enough (either the admin or the regular "request build table" level),
     * opens the client-side {@link BuildTableViewer} GUI directly; otherwise
     * falls back to requesting the build table list via a server command.
     */
    public void jMenuHelpViewBuildTables_actionPerformed() {
        /*
         * Show the client side GUI if the requisite file is available.
         * Otherwise, make use of server commands.
         */
        // User the new BuildTableViewer
        if ((userLevel >= client.getData().getAccessLevel("AdminRequestBuildTable")) ||
                  (userLevel >= client.getData().getAccessLevel("RequestBuildTable"))) {
            BuildTableViewer btv = new BuildTableViewer(this, client);
            btv.run();
        } else {
            client.sendChat(String.format("%sc buildtablelist", IClient.CAMPAIGN_PREFIX));
        }

    }

    /**
     * Handles Help &gt; Unit Viewer: shows a {@link UnitLoadingDialog} while
     * MegaMek unit data loads, then opens a {@link NewUnitViewerDialog} in
     * plain "view units" mode, running the loader on a background thread so
     * the EDT is not blocked while unit data is read from disk.
     */
    public void jMenuHelpViewUnit_actionPerformed() {

        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(client.getMainFrame());
        NewUnitViewerDialog unitSelector = new NewUnitViewerDialog(this,
              unitLoadingDialog,
              client,
              NewUnitViewerDialog.UNIT_VIEWER);
        new Thread(unitSelector).start();
    }

    /**
     * Handles Leadership &gt; Promote Player: prompts for a target player
     * (scoped to the current faction unless the requester is a mod/admin) and
     * a destination sub-faction, then sends a {@code promoteplayer} campaign command.
     */
    public void jMenuLeaderPromote_actionPerformed() {
        String targetPlayer;

        int menuType = client.isMod() ? PlayerNameDialog.ANY_PLAYER : PlayerNameDialog.FACTION_ONLY;

        PlayerNameDialog pnd = new PlayerNameDialog(client, "Promote", menuType);
        pnd.setVisible(true);
        targetPlayer = pnd.getPlayerName();
        pnd.dispose();

        if (targetPlayer == null) {
            return;
        }

        SubFactionNameDialog subFactionDialog = new SubFactionNameDialog(client,
              "SubFaction",
              client.getUser(targetPlayer).getHouse());
        subFactionDialog.setVisible(true);
        String subFactionName = subFactionDialog.getSubFactionName();
        subFactionDialog.dispose();

        if ((subFactionName == null) || (subFactionName.isEmpty())) {
            return;
        }

        client.sendChat(String.format("%sc promoteplayer#%s#%s", IClient.CAMPAIGN_PREFIX, targetPlayer, subFactionName));

    }

    /**
     * Handles Leadership &gt; Demote Player: prompts for a target player and a
     * destination sub-faction ("None" removes them entirely), then sends a
     * {@code demoteplayer} campaign command.
     */
    public void jMenuLeaderDemote_actionPerformed() {
        String targetPlayer;

        int menuType = client.isMod() ? PlayerNameDialog.ANY_PLAYER : PlayerNameDialog.FACTION_ONLY;

        PlayerNameDialog pnd = new PlayerNameDialog(client, "Demote Player", menuType);
        pnd.setVisible(true);
        targetPlayer = pnd.getPlayerName();
        pnd.dispose();

        if (targetPlayer == null) {
            return;
        }

        SubFactionNameDialog subFactionDialog = new SubFactionNameDialog(client,
              "Use None to remove completely",
              client.getUser(targetPlayer).getHouse());
        subFactionDialog.setVisible(true);
        String subFactionName = subFactionDialog.getSubFactionName();
        subFactionDialog.dispose();

        if ((subFactionName == null) || (subFactionName.isEmpty())) {
            return;
        }

        client.sendChat(String.format("%sc demoteplayer#%s#%s", IClient.CAMPAIGN_PREFIX, targetPlayer, subFactionName));

    }

    /**
     * Handles Leadership &gt; Fluff Player: prompts for a same-faction target
     * player, shows their current fluff text pre-filled for editing, and sends
     * a {@code FactionLeaderFluff} campaign command with the new text (unless
     * the dialog is cancelled).
     */
    public void jMenuLeaderFluff_actionPerformed() {
        String targetPlayer;

        int menuType = PlayerNameDialog.FACTION_ONLY;

        PlayerNameDialog pnd = new PlayerNameDialog(client, "Fluff Player", menuType);
        pnd.setVisible(true);
        targetPlayer = pnd.getPlayerName();
        pnd.dispose();

        if (targetPlayer == null) {
            return;
        }

        CUser user = (CUser) client.getUser(targetPlayer);

        String newFluff = JOptionPane.showInputDialog(this,
              "Fluff? (Leave blank to remove)",
              user.getFluff());

        if (newFluff != null) {
            client.sendChat(String.format("%sc FactionLeaderFluff#%s#%s", IClient.CAMPAIGN_PREFIX, targetPlayer, newFluff));
        }
    }

    /**
     * Handles Leadership &gt; Mute Player: prompts for a same-faction target
     * player and sends a {@code FactionLeaderMute} campaign command
     * (server-side, presumably toggles that player's chat mute state).
     */
    public void jMenuLeaderMute_actionPerformed() {
        String targetPlayer;

        int menuType = PlayerNameDialog.FACTION_ONLY;

        PlayerNameDialog pnd = new PlayerNameDialog(client, "Mute Player", menuType);
        pnd.setVisible(true);
        targetPlayer = pnd.getPlayerName();
        pnd.dispose();

        if (targetPlayer == null) {
            return;
        }

        client.sendChat(String.format("%sc FactionLeaderMute#%s", IClient.CAMPAIGN_PREFIX, targetPlayer));
    }

    /**
     * Handles Leadership &gt; Faction Color: prompts for a new color value and
     * sends a {@code ChangeHouseColor} campaign command for the player's own house.
     */
    public void jMenuLeaderFactionColor_actionPerformed() {
        String newColor = JOptionPane.showInputDialog(this,
              "Faction Color?",
              "Faction Color?",
              JOptionPane.QUESTION_MESSAGE);

        if (newColor != null) {
            client.sendChat(
                  String.format("%sc ChangeHouseColor#%s#%s", IClient.CAMPAIGN_PREFIX, client.getPlayer().getHouse(), newColor));
        }
    }

    /**
     * Handles Leadership &gt; Player Color: prompts for a new color value and
     * sends an {@code AdminSetHousePlayerColor} campaign command for the
     * player's own house.
     */
    public void jMenuLeaderPlayerColor_actionPerformed() {
        String newColor = JOptionPane.showInputDialog(this,
              "Player Color?",
              "Player Color?",
              JOptionPane.QUESTION_MESSAGE);

        if (newColor != null) {
            client.sendChat(
                  String.format("%sc AdminSetHousePlayerColor#%s#%s", IClient.CAMPAIGN_PREFIX, client.getPlayer()
                                                                                    .getHouse(), newColor));
        }
    }

    /**
     * Handles Leadership &gt; Research Unit: shows a {@link UnitLoadingDialog}
     * while MegaMek unit data loads, then opens a {@link NewUnitViewerDialog}
     * (named "Unit Selector") in plain "view units" mode on a background
     * thread. Note this handler does not appear to differ functionally from
     * {@link #jMenuHelpViewUnit_actionPerformed()} beyond the thread's name;
     * unit research itself is presumably driven from within that dialog.
     */
    public void jMenuLeaderResearchUnit_actionPerformed() {
        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(client.getMainFrame());
        NewUnitViewerDialog unitSelector = new NewUnitViewerDialog(this,
              unitLoadingDialog,
              client,
              NewUnitViewerDialog.UNIT_VIEWER);
        unitSelector.setName("Unit Selector");
        new Thread(unitSelector).start();
    }

    /** Handles Leadership &gt; Set Component Conversion: opens the {@link ComponentConverterDialog}. */
    public void jMenuLeaderSetComponentConversion_actionPerformed() {
        new ComponentConverterDialog(client);
    }

    /**
     * Handles Leadership &gt; Purchase Factory: prompts for a factory name, a
     * unit type, and a weight class (each via a combo box in a
     * {@link JOptionPane}), then, unless a target planet is already supplied,
     * prompts for a planet (defaulting to the player's own house and a set of
     * placeholder option values), and finally sends a {@code purchaseFactory}
     * campaign command.
     *
     * @param planet the target planet name, or {@code null} to prompt for one via {@link PlanetNameDialog}
     */
    public void jMenuLeaderPurchaseFactory_actionPerformed(String planet) {
        String[] units = { Unit.getTypeClassDesc(Unit.MEK), Unit.getTypeClassDesc(Unit.VEHICLE),
                           Unit.getTypeClassDesc(Unit.INFANTRY), Unit.getTypeClassDesc(Unit.PROTOMEK),
                           Unit.getTypeClassDesc(Unit.BATTLEARMOR) };
        String[] weight = { Unit.getWeightClassDesc(Unit.LIGHT), Unit.getWeightClassDesc(Unit.MEDIUM),
                            Unit.getWeightClassDesc(Unit.HEAVY), Unit.getWeightClassDesc(Unit.ASSAULT) };

        String factoryName = JOptionPane.showInputDialog(this,
              "Factory Name?",
              "Factory Name?",
              JOptionPane.QUESTION_MESSAGE);

        if (factoryName == null) {
            return;
        }

        JComboBox<String> combo = new JComboBox<>(units);
        combo.setEditable(false);
        JOptionPane jop = new JOptionPane(combo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(client.getMainFrame(), "Unit Type");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        int unitType = combo.getSelectedIndex();

        if (unitType < 0) {
            return;
        }

        int value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        combo = new JComboBox<>(weight);
        combo.setEditable(false);
        jop = new JOptionPane(combo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);

        dlg = jop.createDialog(client.getMainFrame(), "Unit Weight");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        int unitWeight = combo.getSelectedIndex();

        if (unitWeight < 0) {
            return;
        }

        value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        if (planet == null) {
            String[] opString = { "20000", " ", " ", " ", " ", "100", "100", "0", Integer.toString(Integer.MAX_VALUE),
                                  client.getPlayer().getHouse(), "", "" };
            PlanetNameDialog planetDialog = new PlanetNameDialog(client, "Choose a planet", opString);
            planetDialog.setVisible(true);
            planet = planetDialog.getPlanetName();
            planetDialog.dispose();

            if (planet == null) {
                return;
            }
        }

        client.sendChat(
              String.format("%sc purchaseFactory#%s#%s#%s#%s", IClient.CAMPAIGN_PREFIX, factoryName, unitType, unitWeight, planet));
    }

    /**
     * Handles Help &gt; About: builds and shows a small modal dialog listing
     * the MekWars client and MegaMek versions, license info, and a link to
     * the project's GitHub page, centered over this frame.
     */
    // Show data about the mek wars client and server
    public void jMenuHelpAbout_actionPerformed() {

        // make the dialog
        JDialog dlg = new JDialog(this, "MekWars client Info");

        // set up the contents
        JPanel child = new JPanel();
        child.setLayout(new BoxLayout(child, BoxLayout.Y_AXIS));

        // set the text up.
        JLabel mekwars = new JLabel(String.format("MekWars client Version: %s", IClient.CLIENT_VERSION));
        JLabel version = new JLabel(String.format("MegaMek Version: %s", SuiteConstants.VERSION));
        JLabel license1 = new JLabel("MekWars client software is under GPL. See");
        JLabel license2 = new JLabel("LICENSE in the root for details.");
        JLabel license3 = new JLabel("Project Info and Server Packages:");
        JLabel license4 = new JLabel("       https://github.com/megamek/MekWars       ");
        JLabel data1 = new JLabel("       Datasets are prepared by server operators.       ");
        JLabel data2 = new JLabel("       Contact a server administrator for information       ");
        JLabel data3 = new JLabel("       regarding data use and redistribution.       ");

        // center everything
        mekwars.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        version.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        license1.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        license2.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        license3.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        license4.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        data1.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        data2.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        data3.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        // add to child panel
        child.add(new JLabel("\n"));
        child.add(mekwars);
        child.add(version);
        child.add(new JLabel("\n"));
        child.add(license1);
        child.add(license2);
        child.add(new JLabel("\n"));
        child.add(license3);
        child.add(license4);
        child.add(new JLabel("\n"));
        child.add(data1);
        child.add(data2);
        child.add(data3);
        child.add(new JLabel("\n"));

        // then add child panel to the content pane.
        dlg.getContentPane().add(child);

        // set the location of the dialog
        java.awt.Dimension dlgSize = dlg.getPreferredSize();
        java.awt.Dimension frmSize = getSize();
        java.awt.Point loc = getLocation();
        dlg.setLocation(((frmSize.width - dlgSize.width) / 2) + loc.x, ((frmSize.height - dlgSize.height) / 2) + loc.y);
        dlg.setModal(true);
        dlg.setResizable(false);
        dlg.pack();
        dlg.setVisible(true);
    }

    /**
     * Handles Help &gt; Memory: builds and shows a small non-modal dialog
     * reporting the JVM's current free/allocated/max memory (via
     * {@link Runtime}), formatted in kilobytes.
     */
    // Show data about the mek wars client memory usage
    public void jMenuHelpMemory_actionPerformed() {

        // make the dialog
        JDialog dlg = new JDialog(this, "MekWars Memory Usage");

        // set up the contents
        JPanel child = new JPanel();
        child.setLayout(new BoxLayout(child, BoxLayout.Y_AXIS));

        Runtime runtime = Runtime.getRuntime();

        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        java.text.DecimalFormat myFormatter = new java.text.DecimalFormat("#,### kb");
        // set the text up.
        JLabel freeMem = new JLabel(String.format("Free Memory:          %s", myFormatter.format(freeMemory / 1024)));
        JLabel allocatedMem = new JLabel(String.format("Allocated Memory:  %s", myFormatter.format(allocatedMemory / 1024)));
        JLabel maxMem = new JLabel(String.format("Max Memory:           %s", myFormatter.format(maxMemory / 1024)));
        JLabel totalFreeMem = new JLabel(String.format("Total Free Memory: %s", myFormatter.format((freeMemory +
                                                                                             (maxMemory -
                                                                                                    allocatedMemory)) /
                                                                                            1024)));

        // center everything
        freeMem.setAlignmentX(Component.LEFT_ALIGNMENT);
        allocatedMem.setAlignmentX(Component.LEFT_ALIGNMENT);
        maxMem.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalFreeMem.setAlignmentX(Component.LEFT_ALIGNMENT);

        // add to child panel
        child.add(new JLabel("\n"));
        child.add(freeMem);
        child.add(allocatedMem);
        child.add(maxMem);
        child.add(totalFreeMem);
        child.add(new JLabel("\n"));

        // then add child panel to the content pane.
        dlg.getContentPane().add(child);

        // set the location of the dialog
        dlg.setLocationRelativeTo(this);
        dlg.setModal(false);
        dlg.setResizable(false);
        dlg.pack();
        dlg.setVisible(true);

    }

    /**
     * Handles Help &gt; Online Help: builds a large HTML reference table
     * covering unit purchase/re-podding/tech-hire/bay costs, per-weight-class
     * experience requirements, black-market experience thresholds, defection
     * experience requirements, and the meaning of every unit status icon shown
     * elsewhere in the UI, all derived live from current server config values
     * so it reflects this particular server's rules. Also lists any
     * server-wide or house-specific banned ammunition types. The final HTML is
     * routed to the misc chat output via {@link IClient#doParseDataInput(String)}.
     */
    public void jMenuHelpHelp_actionPerformed() {
        CPlayer player = client.getPlayer();
        boolean trueCost = Boolean.parseBoolean(client.getServerConfigs("UseCalculatedCosts"));
        StringBuilder result = new StringBuilder();
        result.append("<font color=\"black\">");
        result.append("MEKWARS ONLINE HELP<br>");

        result.append("<table><tr><th>Name</th><th>")
              .append(client.moneyOrFluMessage(true, false, -2))
              .append("</th><th>")
              .append(client.moneyOrFluMessage(false, false, -2))
              .append("</th><th>Components</th>");

        if (useAdvanceRepairs) {
            result.append("<th>Bays</th></tr>");
        } else {
            result.append("<th>Techs</th></tr>");
        }

        int typeamount = Unit.MAX_BUILD;
        for (int type = 0; type < typeamount; type++) {
            String useIt = String.format("Use%s", Unit.getTypeClassDesc(type));

            if (!Boolean.parseBoolean(client.getServerConfigs(useIt))) {
                continue;
            }

            for (int weight = 0; weight < 4; weight++) {

                // No reason to cycle through med-assault infantry if you are
                // only using light
                if (Boolean.parseBoolean(client.getServerConfigs("UseOnlyLightInfantry")) &&
                          (type == Unit.INFANTRY) &&
                          (weight != Unit.LIGHT)) {
                    break;
                }

                // only using one vee size means only lights are used. so no
                // reason to keep cycling if we are past the lights.
                if (Boolean.parseBoolean(client.getServerConfigs("UseOnlyOneVehicleSize")) &&
                          (type == Unit.VEHICLE) &&
                          (weight != Unit.LIGHT)) {
                    break;
                }

                result.append("<tr><td>")
                      .append(Unit.getWeightClassDesc(weight))
                      .append(" ")
                      .append(Unit.getTypeClassDesc(type))
                      .append("</td><td>");
                if (trueCost) {
                    result.append("See Unit Viewer");
                } else {
                    result.append(CUnit.getPriceForUnit(client, weight, type, player.getMyHouse()));
                }
                result.append("</td><td>").append(CUnit.getInfluenceForUnit(client,
                      weight,
                      type,
                      player.getMyHouse())).append("</td>");
                result.append("<td>")
                      .append(CUnit.getPPForUnit(client, weight, type, player.getMyHouse()))
                      .append("</td><td>");
                if (type == Unit.PROTOMEK) {
                    result.append(client.getServerConfigs("TechsToProtoPointRatio")).append(" per 5");
                } else {
                    result.append(player.getHangarSpaceRequired(type, weight, 0, ""));
                }
                result.append("</td></tr>");

            }
        }
        result.append("</table>");

        result.append("<br><b><i>Re-Podding facts:</b></i>");
        result.append("<table><tr><th>Class</th><th>")
              .append(client.moneyOrFluMessage(true, false, -2))
              .append("</th><th>")
              .append(client.moneyOrFluMessage(false, false, -2))
              .append("</th><th>Components</th></tr>");
        typeamount = 1;
        for (int type = 0; type < typeamount; type++) {
            for (int weight = 0; weight < 4; weight++) {
                String rePodFlu = String.format("RePodFlu%s", Unit.getWeightClassDesc(weight));
                String rePodCost = String.format("RePodCost%s", Unit.getWeightClassDesc(weight));
                String rePodComponents = String.format("RePodComp%s", Unit.getWeightClassDesc(weight));

                int rePodCostInt = MathUtility.parseInt(client.getServerConfigs(rePodCost), 0);
                int rePodComponentsInt = MathUtility.parseInt(client.getServerConfigs(rePodComponents), 0);

                if (!Boolean.parseBoolean(client.getServerConfigs("DoesRePodCost"))) {
                    rePodCostInt = 0;
                }

                if (!Boolean.parseBoolean(client.getServerConfigs("RePodUsesComp"))) {
                    rePodComponentsInt = 0;
                }

                result.append("<tr><td>")
                      .append(Unit.getWeightClassDesc(weight))
                      .append("</td><td>")
                      .append(rePodCostInt)
                      .append("</td><td>")
                      .append(client.getServerConfigs(rePodFlu))
                      .append("</td><td>")
                      .append(rePodComponentsInt)
                      .append("</td></tr>");
            }
        }

        result.append("</table><br>");
        if (useAdvanceRepairs) {
            result.append("<br><b><i>Tech/BayCosts:</b></i>");
            result.append("<table><tr><th>Type</th><th>")
                  .append(client.moneyOrFluMessage(true, false, -2))
                  .append("</th>");
            result.append("<tr><td>Bay Cost</td><td>")
                  .append(Integer.parseInt(client.getServerConfigs("CostToBuyNewBay")))
                  .append("</td></tr>");
            result.append("<tr><td>Bay Sale</td><td>")
                  .append(Integer.parseInt(client.getServerConfigs("BaySellBackPrice")))
                  .append("</td></tr>");
            result.append("<tr><td>Green Tech</td><td>")
                  .append(Integer.parseInt(client.getServerConfigs("GreenTechHireCost")))
                  .append("</td></tr>");

            if (Boolean.parseBoolean(client.getServerConfigs("AllowRegTechsToBeHired"))) {
                result.append("<tr><td>Reg Tech</td><td>")
                      .append(Integer.parseInt(client.getServerConfigs("RegTechHireCost")))
                      .append("</td></tr>");
            }
            result.append("</table>");
        }
        result.append("<table><tr><th>Size of Unit</th><th>EXP needed</th></tr>");
        result.append("<tr><td>Light</td><td>")
              .append(client.getServerConfigs("MinEXPforLight"))
              .append("</td></tr>"); //@salient
        result.append("<tr><td>Medium</td><td>")
              .append(client.getServerConfigs("MinEXPforMedium"))
              .append("</td></tr>");
        result.append("<tr><td>Heavy</td><td>").append(client.getServerConfigs("MinEXPforHeavy")).append("</td></tr>");
        result.append("<tr><td>Assault</td><td>")
              .append(client.getServerConfigs("MinEXPforAssault"))
              .append("</td></tr>");
        result.append("</table>");
        result.append("EXP needed to Buy/Sell on the Black Market: ")
              .append(client.getServerConfigs("MinEXPforBMBuying"))
              .append("/")
              .append(client.getServerConfigs("MinEXPforBMSelling"))
              .append("<br>");
        result.append("EXP needed to defect from ")
              .append(client.getServerConfigs("NewbieHouseName"))
              .append(" to a Faction: ")
              .append(client.getServerConfigs("MinEXPforDefecting"))
              .append("<br>");

        result.append("</table><br>");

        result.append("<b><i>Unit Status Icons</B></i><br>");
        result.append("<table><tr><th>Icon</th><th>Description</th></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/ammo.gif\"></td><td>Unit has all ammo bins loaded</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/low.gif\"></td><td>Unit has 1 or more ammo bins that are low on ammo</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/empty.gif\"></td><td>Unit has 1 or more ammo bins that are empty</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/armor.gif\"></td><td>Unit has armor damage</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/structure.gif\"></td><td>Unit has IS damage</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/critical.gif\"></td><td>Unit has critical damaged</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/engine.gif\"></td><td>Unit is engined</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/nopilot.gif\"></td><td>Unit has no pilot</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/pilot.gif\"></td><td>Unit has a pilot</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/wound.gif\"></td><td>Unit has a wounded pilot</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/eject.gif\"></td><td>Unit has auto ejection enabled</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/noeject.gif\"></td><td>Unit has auto ejection disabled</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/pending.gif\"></td><td>Unit has pending repairs</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/repairing.gif\"></td><td>Unit is currently under repair</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/unmaint.gif\"></td><td>Unit is unmaintained/damaged</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/maint.gif\"></td><td>Unit is fully maintained</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/search.gif\"></td><td>Unit is equipped with a " +
                    "search lite</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/searchon.gif\"></td><td>Unit is equipped with a search lite and " +
                    "defaults to on</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/nosearch.gif\"></td><td>Unit is not equipped with a " +
                    "search lite</td></tr>");
        result.append("</table><br>");

        result.append("</table><br>");

        if (!client.getData().getServerBannedAmmo().isEmpty()) {
            result.append("<b><i>Server Banned ammo</b></i><br>");
            for (AmmoType.Munitions bannedAmmo : client.getData().getServerBannedAmmo()) {
                result.append(client.getData().getMunitionsByNumber().get(bannedAmmo)).append("<br>");
            }
        }

        House faction = client.getData().getHouseByName(client.getPlayer().getHouse());
        if (!faction.getBannedAmmo().isEmpty()) {
            result.append("<b><i>House Banned Ammo</b></i><br>");
            for (AmmoType.Munitions bannedAmmo : faction.getBannedAmmo()) {
                result.append(client.getData().getMunitionsByNumber().get(bannedAmmo)).append("<br>");
            }
        }

        /*
         * use process incoming, instead of adding directly to misc, so that
         * output is directly to main if misc in main is enabled or players has
         * the misc. tab off.
         */
        client.doParseDataInput(String.format("SM|%s", result));
    }

    /**
     * Looks up the server-configured XP cost for gaining a given pilot skill
     * on a given unit type (config key {@code chancefor<skillFullName>for<unitType>})
     * and, if it is enabled (cost &gt; 0), formats a short "{unitType} xp cost: {cost}"
     * fragment for use in the pilot skills help blurb; used by
     * {@link #pilotSkillBlurbLine}.
     *
     * @param a the skill's full name, used to build the config key
     * @param b the unit type name (e.g. "Mek", "Vehicle"), used to build the config key
     * @return a short descriptive fragment, or an empty string if the config value is not positive
     */
    private String pilotSkillBVBlurbLine(String a, String b) {//BK added
        // builds help menu's pilot skill bv blurb, wants a and b to build server config lookup and get the value
        int i = Integer.parseInt(client.getServerConfigs(String.format("chancefor%sfor%s", a, b)));

        if (i > 0) {
            return String.format(" %s xp cost: %s", b, i);
        } else {
            return "";
        }
    }

    /**
     * Builds one HTML table row for the pilot skills help table (see
     * {@link #jMenuHelpPilotSkills_actionPerformed()}), combining the skill's
     * name/abbreviation/description with a per-unit-type XP cost breakdown
     * assembled from repeated calls to {@link #pilotSkillBVBlurbLine} (Mek,
     * Vehicle, Infantry, ProtoMek, BattleArmor, Aero). If none of those calls
     * produced a cost fragment, an empty/near-empty row (just the leading
     * space) is returned so the skill is effectively omitted from the table.
     *
     * @param skill     the skill's short display name for the "Name" column
     * @param fullName  the skill's full name, used to build server config lookup keys
     * @param shortName the skill's abbreviation, used for the "Abbreviation" column
     * @return a complete {@code <tr>...</tr>} row of HTML, or a near-empty string if no unit type has a configured cost for this skill
     */
    private String pilotSkillBlurbLine(String skill, String fullName, String shortName) {//BK added
        // builds help menu's pilot skill blurb line, wants skill and fullName and shortName as skill, full name and skill shortname and
        // description, e.g. "AsTech" and "AT" and "does this..."
        String s = " ";
        //find if there is any chance for this skill, and if yes, create entry
        s += pilotSkillBVBlurbLine(fullName, "Mek") +
                   pilotSkillBVBlurbLine(fullName, "Vehicle") +
                   pilotSkillBVBlurbLine(fullName, "Infantry") +
                   pilotSkillBVBlurbLine(fullName, "ProtoMek") +
                   pilotSkillBVBlurbLine(fullName, "BattleArmor") +
                   pilotSkillBVBlurbLine(fullName, "Aero");//not sure where to get skill reiterable list for unit types
        if (s.length() > 1) {
            s = String.format("<tr><td>%s</td><td>%s</td><td>%s<br>%s</td></tr>", skill, fullName, shortName, s);
        }
        return s;
    }

    /**
     * Handles Help &gt; Pilot Skill Descriptions: builds an HTML table
     * describing every special pilot skill in the game (AsTech, Edge,
     * Maneuvering Ace, etc.), including per-unit-type XP costs pulled live
     * from server config via {@link #pilotSkillBlurbLine}, and routes the
     * result to the misc chat output via {@link IClient#doParseDataInput(String)}.
     * The skill descriptions themselves are hardcoded text (not server-configurable).
     */
    public void jMenuHelpPilotSkills_actionPerformed() {
        //BK; would prefer to have this Help Menu list built using a reiteration of the pilot skills by pulling the
        // info from those classes step one was adding pilot xp costs to the help menu, step two will be adding bv
        // costs, drawn via the skill
        String result = "";
        result += "<font color=\"black\">";
        result += "<b><i>MekWars/MegaMek Pilot Skills</b></i><br>";
        result += "<table><tr><th>Name</th>" + "<th>Abbreviation</th>" + "<th>Description</th></tr>";
        if (useAdvanceRepairs) {
            result += pilotSkillBlurbLine("AsTech", "AT", "Pilot acts as a tech with repairs only costing parts");
        } else {
            result += pilotSkillBlurbLine("AsTech", "AT", "Reduces the number of techs needed to repair a unit by 1");
        }
        result += pilotSkillBlurbLine("MD Buffered VDNI", "BVDNI", "Allows pilots to take more damage.");
        result += pilotSkillBlurbLine("Dodge Maneuver",
              "DM",
              "Enables the unit to make a dodge maneuver instead of a physical attack.<br>This maneuver adds +2 to the BTH to physical attacks against the unit.");
        result += pilotSkillBlurbLine("Edge", "ED", "Allows Pilot to reroll 1 roll(per level) per game.");
        result += pilotSkillBlurbLine("Enhanced Interface",
              "EI",
              "Neural interface to the clan enhanced imaging system<br>-1 To PSR<br>+2 when targeting with TC instead of +3<br>Can Target without TC at +6<br>Reduces all forest and Smoke mods to 1<br>Pilot receives 1 point of damage every time Units IS is hit,<br>If you fail a roll of 7+<br>BA's recieve 1 extra point of damage every time they are hit.");
        result += pilotSkillBlurbLine("Gifted",
              "GT",
              new StringBuilder("Pilots receive an extra ")
                    .append(client.getServerConfigs("GiftedPercent"))
                    .append("% chance to gain a skill when they fail<br>to level Piloting or Gunnery after a win.")
                    .toString());
        result += pilotSkillBlurbLine("Gunnery Ballistic",
              "GB",
              "NOTE: This is an unofficial rule. Pilot gets a -1 to-hit bonus on all<br>ballistic weapons (MGs, all ACs, Gauss rifles).");
        result += pilotSkillBlurbLine("Gunnery Laser",
              "GL",
              "NOTE: This is an unofficial rule. Pilot gets a -1 to-hit bonus on all<br>energy-based weapons (Laser, PPC, and Flamer).");
        result += pilotSkillBlurbLine("Gunnery Missile",
              "GM",
              "NOTE: This is an unofficial rule. Pilot gets a -1 to-hit bonus on all<br>missile weapons (LRM, MRM, SRM).");
        result += pilotSkillBlurbLine("Iron Man",
              "IM",
              "NOTE: This is an unofficial rule. A pilot with this skill receives only<br>1 pilot hit from ammunition explosions.");
        result += pilotSkillBlurbLine("Maneuvering Ace",
              "MA",
              "Enables the unit to move laterally like a Quad. Units also receive a -1<br>BTH to rolls against skidding.");
        result += pilotSkillBlurbLine("Melee Specialist",
              "MS",
              "Enables the unit to do 1 additional point of damage with physical attacks<br>and subtracts one from the attacker movement modifier (to a minimum of zero).");
        result += pilotSkillBlurbLine("MedTech",
              "MT",
              "A pilot with the MedTech skill will heal 1 extra point per tick.");
        result += pilotSkillBlurbLine("Natural Aptitude: Gunnery",
              "NAG",
              "The pilot checks leveling for gunnery at one level higher then current i.e.<br>5 instead of 4 for a 4/5 pilot.");
        result += pilotSkillBlurbLine("Natural Aptitude: Piloting",
              "NAP",
              "The pilot checks leveling for piloting at one level higher then current i.e.<br>6 instead of 5 for a 4/5 pilot.");
        result += pilotSkillBlurbLine("Pain Resistance",
              "PR",
              "When making consciousness rolls, 1 is added to all rolls. Also, damage received<BR>from ammo explosions is reduced to 1.");
        result += pilotSkillBlurbLine("Pain Shunt",
              "PS",
              "When making consciousness rolls, 1 is added to all rolls. Also, damage received<BR>from ammo explosions is reduced to 1.");
        result += pilotSkillBlurbLine("Quick Study",
              "QS",
              "Pilots with the Quick Study skill gain a 5% bonus to all XP earned.");
        result += pilotSkillBlurbLine("Survivalist",
              "SV",
              "If a pilot has this skill they will have a +20% of returning home if ejected and<br>left on the field.");
        result += pilotSkillBlurbLine("Tactical Genius",
              "TG",
              "A pilot who has a Tactical Genius may reroll their initiative once per turn.<br>The second roll must be accepted.");
        result += pilotSkillBlurbLine("Trait", "TN", "Pilot traits for use with modifying the gaining of other skills" +
                                                           ".");
        result += pilotSkillBlurbLine("VDNI MD Skill", "VDNI", "Allows Pilot to Take more Damage.");
        result += pilotSkillBlurbLine("Weapon Specialist",
              "WS",
              "A pilot who specializes in a particular weapon receives a -2 to hit modifier<br>on all attacks with that weapon.");
        result += pilotSkillBlurbLine("Clan Pilot Training",
              "CPT",
              "Pilot has a +1 penalty for physical attacks,<br>because clans do not train for dishonourable combat.");
        result += "</table>";

        /*
         * use process incoming, instead of adding directly to misc, so that
         * output is directly to main if misc in main is enabled or player's
         * misc. tab is disabled.
         */
        client.doParseDataInput(String.format("SM|%s", result));

    }

    /**
     * Shows a picker dialog for a list of MUL (MegaMek Unit List) file names
     * received from the server, and, once the user selects one and confirms,
     * sends a {@code retrievemul} campaign command for that file.
     *
     * @param data a {@code '#'}-delimited list of MUL file names, as sent by the server
     */
    public void showMulFileList(String data) {

        java.util.StringTokenizer mulList = new java.util.StringTokenizer(data, "#");

        java.util.Vector<String> list = new java.util.Vector<>(1, 1);

        while (mulList.hasMoreElements()) {
            list.add(mulList.nextToken());
        }

        JComboBox<String> combo = new JComboBox<>(list);
        combo.setEditable(false);
        JOptionPane jop = new JOptionPane(combo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(client.getMainFrame(), "Select mul file.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        if (combo.getSelectedIndex() < 0) {
            return;
        }

        int value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }
        String selectedMul = list.elementAt(combo.getSelectedIndex());

        client.sendChat(String.format("%sc retrievemul#%s", IClient.CAMPAIGN_PREFIX, selectedMul));
    }

    /*
     * Admin methods used to be here. These have been extracted into a separate
     * .jar file. @urgru
     */

    /*
     * Moderator-specific methods used to be here. These have been extracted
     * into a separate .jar file. @urgru
     */

    /** Delegates to {@link CMainPanel#refreshBattleTable()} to refresh the battle/game list display. */
    public void refreshBattleTable() {
        MainPanel.refreshBattleTable();
    }

    /**
     * Synchronizes the Options &gt; Mute checkbox item's checked state with an
     * externally-driven mute setting (e.g. loaded from config at startup), without
     * re-triggering the mute logic itself.
     *
     * @param b {@code true} if sound should show as muted
     */
    public void setSoundMuted(boolean b) {
        jMenuOptionsMute.setState(b);
    }

    /**
     * Refreshes the dynamic Attack/Game menu's contents. Guards against the
     * menu not existing yet: during login, this can be invoked before
     * {@link #jMenuAttackMenu} has been constructed, so a null check with a
     * warning log avoids a {@link NullPointerException} in that window.
     */
    public void updateAttackMenu() {

        // the login call of UOE occurs before the menu is
        // created. return to stop NPE's.
        if (jMenuAttackMenu == null) {
            LOGGER.warn("Attack Menu is Null!");
            return;
        }

        jMenuAttackMenu.updateMenuItems(true);
    }

    /**
     * Updates the Host menu's visual/enabled state to reflect that this client
     * is now hosting a game: turns the menu label red, disables the "start"
     * options, and swaps visibility so only "Stop Hosting" is shown. Does not
     * itself start the host process; called alongside {@link IClient#startHost}.
     */
    public void startHost() {
        jMenuHost.setForeground(java.awt.Color.red);
        jMenuCSHostAndJoin.setEnabled(false);
        jMenuCSHostDedicated.setEnabled(false);
        jMenuCSHostStop.setEnabled(true);
        jMenuCSHostAndJoin.setVisible(false);
        jMenuCSHostDedicated.setVisible(false);
        jMenuCSHostLoad.setVisible(false);
        jMenuCSHostLoadAndJoin.setVisible(false);
        jMenuCSHostStop.setVisible(true);
    }

    /**
     * Reverts the Host menu's visual/enabled state after this client stops
     * hosting a game: restores the label color, re-enables the "start"
     * options, and swaps visibility so the various "start hosting" options
     * are shown again instead of "Stop Hosting". Called alongside {@link IClient#stopHost}.
     */
    public void stopHost() {
        jMenuHost.setForeground(java.awt.Color.black);
        jMenuCSHostAndJoin.setEnabled(true);
        jMenuCSHostDedicated.setEnabled(true);
        jMenuCSHostStop.setEnabled(false);
        jMenuCSHostAndJoin.setVisible(true);
        jMenuCSHostDedicated.setVisible(true);
        jMenuCSHostLoad.setVisible(true);
        jMenuCSHostLoadAndJoin.setVisible(true);
        jMenuCSHostStop.setVisible(false);
    }

    /**
     * Reacts to a player status change (e.g. going active, reserve, fighting,
     * or logging out): if the "status in tray icon" config option is set,
     * swaps the window's title-bar/taskbar icon to reflect the new status
     * (falling back to a generic tray icon image otherwise), then forwards the
     * status change to {@link CMainPanel#changeStatus} and refreshes the menu
     * bar via {@link #enableMenu()} to update visibility for the new status.
     *
     * @param status     the new {@code IClient.STATUS_*} value
     * @param lastStatus the previous {@code IClient.STATUS_*} value
     */
    public void changeStatus(int status, int lastStatus) {

        if (client.getConfig().isParam("STATUS_INT_RAY_ICON")) {
            if (status == IClient.STATUS_RESERVE) {
                try {
                    setIconImage(client.getConfig().getImage("RESERVE").getImage());
                } catch (Exception ex) {
                    LOGGER.error(ex, "Reserve Icon {}", ex.getLocalizedMessage());
                }
            } else if (status == IClient.STATUS_ACTIVE) {
                try {
                    setIconImage(client.getConfig().getImage("ACTIVE").getImage());
                } catch (Exception ex) {
                    LOGGER.error(ex, "Active Icon {}", ex.getLocalizedMessage());
                }
            } else if (status == IClient.STATUS_FIGHTING) {
                try {
                    setIconImage(client.getConfig().getImage("FIGHT").getImage());
                } catch (Exception ex) {
                    LOGGER.error(ex, "Fight Icon {}", ex.getLocalizedMessage());
                }
            } else if ((status == IClient.STATUS_LOGGED_OUT) || (status == IClient.STATUS_DISCONNECTED)) {
                try {
                    setIconImage(client.getConfig().getImage("LOGOUT").getImage());
                } catch (Exception ex) {
                    LOGGER.error(ex, "Log Out Icon {}", ex.getLocalizedMessage());
                }
            }
        }

        // if not showing status, show the operator's custom icon
        else {
            try {
                setIconImage(client.getConfig().getImage("TRAY").getImage());
            } catch (Exception ex) {
                LOGGER.error(ex, "Tray Icon {}", ex.getLocalizedMessage());
            }
        }

        MainPanel.changeStatus(status, lastStatus);
        enableMenu();
        repaint();
    }

    /**
     * Creates a new army by importing units from a MUL (MegaMek Unit List)
     * file: prompts for the owning player (any player is eligible; an empty
     * string is used if the dialog is cancelled), lets the user pick which MUL
     * file from a supplied list, prompts for an army name, and sends a
     * {@code createarmyfrommul} campaign command.
     *
     * @param data a {@code '#'}-delimited list of MUL file names, as sent by the server
     */
    public void createArmyFromMul(String data) {
        PlayerNameDialog playerDialog = new PlayerNameDialog(client, "Choose a Player.", PlayerNameDialog.ANY_PLAYER);
        playerDialog.setVisible(true);
        String player = playerDialog.getPlayerName();
        playerDialog.dispose();

        if (player == null) {
            player = "";
        }

        LOGGER.debug("String Tokenizer called");
        StringTokenizer mulList = new StringTokenizer(data, "#");

        Vector<String> list = new Vector<>(1, 1);

        // System.err.println("adding mul's to vector");
        while (mulList.hasMoreElements()) {
            list.add(mulList.nextToken());
        }

        // System.err.println("creating combo box.");

        JComboBox<String> combo = new JComboBox<>(list);
        combo.setEditable(false);
        JOptionPane jop = new JOptionPane(combo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(client.getMainFrame(), "Select mul file.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        if (combo.getSelectedIndex() < 0) {
            return;
        }

        int value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }
        String selectedMul = list.elementAt(combo.getSelectedIndex());

        String fluff = JOptionPane.showInputDialog(getContentPane(), "Army Name.");
        if ((fluff == null) || (fluff.isEmpty())) {
            return;
        }

        client.sendChat(String.format("%screatearmyfrommul %s#%s#%s", IClient.CAMPAIGN_PREFIX, selectedMul, fluff, player));
    }

    /**
     * Handles Operations &gt; Send All Local Op Files (available only in the
     * dynamically-built Operations menu, when {@code ./MekWarsOpEditor.jar} is
     * present and the player has sufficient access): after confirmation,
     * reads every {@code .txt} file under {@code ./data/operations/short/},
     * escapes any {@code '#'} characters in each line (replaced with the
     * literal text {@code "(pound)"} to avoid colliding with the
     * {@code '#'}-delimited campaign command protocol), and sends a
     * {@code setoperation} campaign command per file to upload it to the server.
     *
     * @param actionEvent unused; present only to match the {@link ActionListener} signature
     */
    public void jMenuSendAllOperationFiles_actionPerformed(ActionEvent actionEvent) {

        int result = JOptionPane.showConfirmDialog(null,
              "Upload All local OpFiles?",
              "Upload Ops",
              JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.NO_OPTION) {
            return;
        }

        File opFiles = new File("./data/operations/short/");

        if (!opFiles.exists()) {
            return;
        }

        StringBuilder opData = new StringBuilder();

        File[] opFilesList = opFiles.listFiles();
        if (opFilesList != null) {
            for (File opFile : opFilesList) {
                try {
                    if (!opFile.getName().endsWith(".txt")) {
                        continue;
                    }

                    FileInputStream fis = new FileInputStream(opFile);
                    BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
                    opData.append(opFile.getName(), 0, opFile.getName().lastIndexOf(".txt")).append("#");

                    while (dis.ready()) {
                        opData.append(dis.readLine().replace("#", "(pound)")).append("#");
                    }

                    dis.close();
                    fis.close();

                } catch (Exception ex) {
                    LOGGER.error(ex, "Unable to read file: {}", opFile);
                    return;
                }
                client.sendChat(String.format("%sc setoperation#short#%s", IClient.CAMPAIGN_PREFIX, opData));
                opData.setLength(0);
                opData.trimToSize();
            }
        }
    }

    /**
     * Handles Operations &gt; Set New Operation File: prompts for an operation
     * name, reads the matching local file from
     * {@code ./data/operations/short/<name>.txt} (silently doing nothing if it
     * does not exist), escapes {@code '#'} characters, and sends a
     * {@code setoperation} campaign command to upload it to the server.
     *
     * @param e unused; present only to match the {@link ActionListener} signature
     */
    public void jMenuSetNewOperationFile_actionPerformed(ActionEvent e) {

        String opName = JOptionPane.showInputDialog(client.getMainFrame().getContentPane(), "New Op Name?");

        if ((opName == null) || (opName.trim().isEmpty())) {
            return;
        }

        File opFile = new File(String.format("./data/operations/short/%s.txt", opName));

        if (!opFile.exists()) {
            return;
        }

        StringBuilder opData = new StringBuilder();

        try {
            FileInputStream fis = new FileInputStream(opFile);
            BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
            opData.append(opName).append("#");

            while (dis.ready()) {
                opData.append(dis.readLine().replace("#", "(pound)")).append("#");
            }

            dis.close();
            fis.close();

        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to read file: {}", opFile);
            return;
        }

        client.sendChat(String.format("%sc setoperation#short#%s", IClient.CAMPAIGN_PREFIX, opData));
    }

    /**
     * Handles Operations &gt; Update Operations: locks the campaign, triggers a
     * server-side operations update, then unlocks the campaign again, via
     * three sequential campaign commands. No user prompt is shown. Note the
     * three commands are all fired immediately back-to-back with no
     * acknowledgement wait between them, relying on the server to process
     * them strictly in order.
     *
     * @param e unused; present only to match the {@link ActionListener} signature
     */
    public void jMenuUpdateOperations_actionPerformed(ActionEvent e) {
        client.sendChat(String.format("%sc adminlockcampaign", IClient.CAMPAIGN_PREFIX));
        client.sendChat(String.format("%sc updateoperations", IClient.CAMPAIGN_PREFIX));
        client.sendChat(String.format("%sc adminunlockcampaign", IClient.CAMPAIGN_PREFIX));

    }

    /**
     * Handles Operations &gt; Retrieve Operation File: lets the user pick an
     * operation from {@link IClient#getAllOps()} and sends a
     * {@code RETRIEVEOPERATION} campaign command to fetch its definition from
     * the server.
     *
     * @param e unused; present only to match the {@link ActionListener} signature
     */
    public void jMenuRetrieveOperationFile_actionPerformed(ActionEvent e) {
        JComboBox<String> opCombo = new JComboBox<>(client.getAllOps()
                                                          .keySet()
                                                          .toArray(new String[0]));
        opCombo.setEditable(false);

        JOptionPane jop = new JOptionPane(opCombo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);
        JDialog dlg = jop.createDialog(this, "Select Op.");
        opCombo.grabFocus();
        opCombo.getEditor().selectAll();

        dlg.setVisible(true);

        if ((Integer) jop.getValue() == JOptionPane.CANCEL_OPTION) {
            return;
        }

        String opName = (String) opCombo.getSelectedItem();

        client.sendChat(String.format("%sc RETRIEVEOPERATION#short#%s", IClient.CAMPAIGN_PREFIX, opName));
    }

    /**
     * Handles Operations &gt; Set Operation File: lets the user pick a known
     * operation name (from {@link IClient#getAllOps()}), reads the matching
     * local file from {@code ./data/operations/short/<name>.txt} (silently
     * doing nothing if it does not exist), escapes {@code '#'} characters, and
     * sends a {@code setoperation} campaign command to upload it to the server.
     *
     * @param e unused; present only to match the {@link ActionListener} signature
     */
    public void jMenuSetOperationFile_actionPerformed(ActionEvent e) {

        JComboBox<String> opCombo = new JComboBox<>();
        client.getAllOps().keySet().forEach(opCombo::addItem);
        opCombo.setEditable(false);

        JOptionPane jop = new JOptionPane(opCombo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);
        JDialog dlg = jop.createDialog(this, "Select Op.");
        opCombo.grabFocus();
        opCombo.getEditor().selectAll();

        dlg.setVisible(true);

        if ((Integer) jop.getValue() == JOptionPane.CANCEL_OPTION) {
            return;
        }

        String opName = (String) opCombo.getSelectedItem();

        File opFile = new File(String.format("./data/operations/short/%s.txt", opName));

        if (!opFile.exists()) {
            return;
        }

        StringBuilder opData = new StringBuilder();

        try {
            FileInputStream fis = new FileInputStream(opFile);
            BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
            opData.append(opName).append("#");

            while (dis.ready()) {
                opData.append(dis.readLine().replace("#", "(pound)")).append("#");
            }

            dis.close();
            fis.close();

        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to read Set Operation File {}", opFile);
            return;
        }

        client.sendChat(String.format("%sc setoperation#short#%s", IClient.CAMPAIGN_PREFIX, opData));
    }

    /**
     * Attaches {@link #sound} as a {@code MenuListener} to every top-level
     * {@link JMenu} in the given array of menu bar components (removing any
     * existing registration first to avoid duplicate listeners on repeated
     * calls to {@link #enableMenu()}), then recurses into each menu's submenus
     * via {@link #addMenuItemListener}.
     *
     * @param components the top-level components of {@link #jMenuBar1} (a mix of {@link JMenu} and other component types)
     */
    private void addMenuListener(Object[] components) {
        for (Object menu : components) {
            if (menu instanceof JMenu jmenu) {
                jmenu.removeMenuListener(sound);
                jmenu.addMenuListener(sound);
                addMenuItemListener(jmenu);
            }
        }
    }

    /**
     * Recursively attaches {@link #popupSound} as a {@code MenuListener} to
     * every nested submenu ({@link JMenu} items) within the given menu,
     * removing any existing registration first to avoid duplicates on repeated
     * calls. Ordinary {@link JMenuItem}s (which aren't themselves menus) are
     * left untouched since they cannot open a popup.
     *
     * @param menu the menu whose item tree should be walked
     */
    private void addMenuItemListener(JMenu menu) {
        for (int pos = 0; pos < menu.getItemCount(); pos++) {
            JMenuItem item = menu.getItem(pos);
            if (item instanceof JMenu) {
                ((JMenu) item).removeMenuListener(popupSound);
                ((JMenu) item).addMenuListener(popupSound);
                addMenuItemListener((JMenu) item);
            }
        }

    }
}

