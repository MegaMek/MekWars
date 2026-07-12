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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serial;
import java.net.URI;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;

import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.listeners.UserListPopupListener;
import mekwars.common.gui.models.CUserListModel;
import mekwars.common.threads.ActivationThread;
import mekwars.common.util.StringUtils;

/**
 * Backs the connected-users list shown in the lower half of {@link CMainPanel}'s side column (below
 * {@link CPlayerPanel}). Displays every player currently connected to the server — sourced from
 * {@link CUserListModel}, rendered in a single {@link JList} — sorted/ordered according to whichever mode the
 * player last chose (persisted via {@code SORT_MODE} / {@code SORT_ORDER} config params and restored in the
 * constructor). Right-clicking an entry opens {@link UserListPopupListener}'s context menu (e.g. to challenge,
 * message, or view info about that player).
 * <p>
 * Below the list sits a player-count label and an "activity" toggle button that lets the player switch between
 * Active/Reserve/Login states (delegating to {@link #actionPerformed} which sends the appropriate {@code /c}
 * chat command). If the server enables a configurable "link area" ({@code Enable_Link_Area}), up to three
 * clickable link buttons (opening URLs in the system browser) are shown at the very bottom instead of the count
 * panel alone.
 */

public class CUserListPanel extends JPanel implements ActionListener {
    private static final MMLogger LOGGER = MMLogger.create(CUserListPanel.class);

    @Serial
    private static final long serialVersionUID = 6676029823454849117L;
    /** Sort key: alphabetical by player name. */
    public static int SORT_MODE_NAME = 0;
    /** Sort key: grouped by house/faction. */
    public static int SORT_MODE_HOUSE = 1;
    /** Sort key: by experience level. */
    public static int SORT_MODE_EXP = 2;
    /** Sort key: by player rating/ELO (only used when the server hasn't hidden ratings via {@code HideELO}). */
    public static int SORT_MODE_RATING = 3;
    /** Sort key: by online status (active/reserve/etc.). */
    public static int SORT_MODE_STATUS = 4;
    /** Sort key: by user permission level. */
    public static int SORT_MODE_USER_LEVEL = 5;
    /** Sort key: by player's country. */
    public static int SORT_MODE_COUNTRY = 6;

    /** Sort order: ascending. */
    public static int SORT_ORDER_ASCENDING = 1;
    /** Sort order: descending. */
    public static int SORT_ORDER_DESCENDING = 2;
    /** Icon for the activity button when the player is inactive/reserve (invites them to activate). Loaded from {@code ./data/images/activatebutton.(png|jpg)}, or {@code null} if neither file exists. */
    private final Icon activateIcon;
    /** Icon for the activity button when the player is active (invites them to deactivate). Loaded from {@code ./data/images/deactivatebutton.(png|jpg)}, or {@code null} if neither file exists. */
    private final Icon deactivateIcon;
    /** Rollover (mouse-over) variant of {@link #activateIcon}. */
    private final Icon mouseActivateIcon;
    /** Rollover (mouse-over) variant of {@link #deactivateIcon}. */
    private final Icon mouseDeactivateIcon;
    /** Transient "flash" icon shown briefly by {@link ActivationThread} while transitioning into the activate state. */
    private final Icon activateFlashIcon;
    /** Transient "flash" icon shown briefly by {@link ActivationThread} while transitioning into the deactivate state. */
    private final Icon deactivateFlashIcon;
    /** Container for {@link #activityButton} and {@link #countLabel}, stacked vertically. */
    private final JPanel countPanel = new JPanel();
    /** Displays the current connected-player count (visible only when {@code USER_LIST_COUNT} is set). */
    private final JLabel countLabel = new JLabel();
    /** Label for the optional server-configured link area (its text comes from {@code Link_Area_Label}). */
    private final JLabel linksLabel = new JLabel();
    /** Toggle button used to switch the player's active/reserve/login status; see {@link #actionPerformed}. */
    private final JButton activityButton = new JButton();
    /** First of up to three optional server-configured external link buttons (see {@link #createLinkArea()}). */
    private final JButton linkButton1 = new JButton();
    /** Second optional server-configured external link button. */
    private final JButton linkButton2 = new JButton();
    /** Third optional server-configured external link button. */
    private final JButton linkButton3 = new JButton();
    /** Holds the (up to three) link buttons plus {@link #linksLabel}, shown only when {@code Enable_Link_Area} is true. */
    private final JPanel linksPanel = new JPanel();
    /** Bottom strip combining {@link #countPanel} and {@link #linksPanel}; only built/used when the link area is enabled. */
    private final JPanel bottomPanel = new JPanel();
    /** Client session, used for config lookups and to send activity/status chat commands. */
    private final IClient client;
    /** The visible list widget rendering {@link #cUserListModel}'s entries. */
    private final JList<CUserListModel> cUserListModelJList;
    /** Backing model listing every connected player and providing the cell renderer/sort state. */
    private final CUserListModel cUserListModel;
    /** Whether the local player is currently logged in; toggles visibility of player-related UI elsewhere. */
    private boolean isLoggedIn = false;
    /** Whether this client is running as a dedicated (headless/server-hosting) instance. */
    private boolean isDedicated = false;

    /**
     * Builds the user list widget and its surrounding chrome: loads the activity-button icon set from disk (if
     * present under {@code ./data/images/}), sets up the count label and activity toggle button, optionally builds
     * the server-configured link area (see {@link #createLinkArea()}), and restores the previously-used sort mode
     * and order from client config ({@code SORT_MODE} / {@code SORT_ORDER}).
     *
     * @param client the active client session
     */
    public CUserListPanel(IClient client) {
        this.client = client;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(180, 480));
        setMinimumSize(new Dimension(120, 100));
        setMaximumSize(new Dimension(180, 2000));

        cUserListModel = new CUserListModel(this.client);
        cUserListModelJList = new JList<>();
        cUserListModelJList.add(cUserListModel.getRenderer());
        cUserListModelJList.setAlignmentX(0.0F);

        UserListPopupListener userListPopup = new UserListPopupListener(this);
        cUserListModelJList.addMouseListener(userListPopup);
        cUserListModelJList.setCellRenderer(cUserListModel.getRenderer());

        JScrollPane userListSP = new JScrollPane(cUserListModelJList);
        userListSP.setPreferredSize(new Dimension(180, 380));
        userListSP.setMinimumSize(new Dimension(180, 100));
        userListSP.setMaximumSize(new Dimension(180, 2000));
        userListSP.setBorder(new LineBorder(Color.black));
        userListSP.setViewportView(cUserListModelJList);
        add(userListSP, BorderLayout.CENTER);

        cUserListModelJList.setBackground(StringUtils.html2Color(this.client.getConfigParam("BACKGROUND_COLOR")));
        //set up the count label
        countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        countLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        countLabel.setBorder(BorderFactory.createEmptyBorder(3, 2, 2, 2));
        countLabel.setText(String.format("Player Count: %s", cUserListModelJList.getModel().getSize()));
        countLabel.setVisible(this.client.getConfig().isParam("USER_LIST_COUNT"));

        if (new File("./data/images/activatebutton.png").exists()) {
            activateIcon = new ImageIcon("./data/images/activatebutton.png");
        } else if (new File("./data/images/activatebutton.jpg").exists()) {
            activateIcon = new ImageIcon("./data/images/activatebutton.jpg");
        } else {
            activateIcon = null;
        }

        if (new File("./data/images/deactivatebutton.png").exists()) {
            deactivateIcon = new ImageIcon("./data/images/deactivatebutton.png");
        } else if (new File("./data/images/deactivatebutton.jpg").exists()) {
            deactivateIcon = new ImageIcon("./data/images/deactivatebutton.jpg");
        } else {
            deactivateIcon = null;
        }

        if (new File("./data/images/activatebuttonmouse.png").exists()) {
            mouseActivateIcon = new ImageIcon("./data/images/activatebuttonmouse.png");
        } else if (new File("./data/images/activatebuttonmouse.jpg").exists()) {
            mouseActivateIcon = new ImageIcon("./data/images/activatebuttonmouse.jpg");
        } else {
            mouseActivateIcon = null;
        }

        if (new File("./data/images/deactivatebuttonmouse.png").exists()) {
            mouseDeactivateIcon = new ImageIcon("./data/images/deactivatebuttonmouse.png");
        } else if (new File("./data/images/deactivatebuttonmouse.jpg").exists()) {
            mouseDeactivateIcon = new ImageIcon("./data/images/deactivatebuttonmouse.jpg");
        } else {
            mouseDeactivateIcon = null;
        }

        if (new File("./data/images/activateflashbutton.png").exists()) {
            activateFlashIcon = new ImageIcon("./data/images/activateflashbutton.png");
        } else if (new File("./data/images/activateflashbutton.jpg").exists()) {
            activateFlashIcon = new ImageIcon("./data/images/activateflashbutton.jpg");
        } else {
            activateFlashIcon = null;
        }

        if (new File("./data/images/deactivateflashbutton.png").exists()) {
            deactivateFlashIcon = new ImageIcon("./data/images/deactivateflashbutton.png");
        } else if (new File("./data/images/deactivateflashbutton.jpg").exists()) {
            deactivateFlashIcon = new ImageIcon("./data/images/deactivateflashbutton.jpg");
        } else {
            deactivateFlashIcon = null;
        }

        //set up activity button
        activityButton.setEnabled(false);
        activityButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        activityButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        activityButton.addActionListener(this);
        activityButton.setRolloverEnabled(true);
        activityButton.setRolloverIcon(mouseActivateIcon);
        activityButton.setIcon(activateIcon);

        resetActivityButton();

        activityButton.setVisible(this.client.getConfig().isParam("USER_LIST_ACTIVITY_BTN"));

        //add the button and label to CountPanel
        countPanel.setLayout(new BoxLayout(countPanel, BoxLayout.Y_AXIS));
        countPanel.add(activityButton);
        countPanel.add(countLabel);

        //@ Salient - toggle for link area
        if (this.client.getServerConfigs("Enable_Link_Area").equalsIgnoreCase("false")) {
            countPanel.setBorder(BorderFactory.createEmptyBorder(4, 2, 3, 2));
            add(countPanel, BorderLayout.SOUTH);
        } else {
            createLinkArea();
        }

        //restore the previous sort mode
        String mode = this.client.getConfig().getParam("SORT_MODE");
        switch (mode) {
            case "HOUSE" -> getcUserListModel().setSortMode(SORT_MODE_HOUSE);
            case "EXP" -> getcUserListModel().setSortMode(SORT_MODE_EXP);
            case "RATING" -> {
                if (!Boolean.parseBoolean(this.client.getServerConfigs("HideELO"))) {
                    getcUserListModel().setSortMode(SORT_MODE_RATING);
                } else {
                    getcUserListModel().setSortMode(SORT_MODE_NAME);
                }
            }
            case "STATUS" -> getcUserListModel().setSortMode(SORT_MODE_STATUS);
            case "USER_LEVEL" -> getcUserListModel().setSortMode(SORT_MODE_USER_LEVEL);
            case "COUNTRY" -> getcUserListModel().setSortMode(SORT_MODE_COUNTRY);
            default -> getcUserListModel().setSortMode(SORT_MODE_NAME);
        }

        //restore the previous sort order
        String order = this.client.getConfig().getParam("SORT_ORDER");
        if (order.equals("DESCENDING")) {
            getcUserListModel().setSortOrder(SORT_ORDER_DESCENDING);
        } else {
            getcUserListModel().setSortOrder(SORT_ORDER_ASCENDING);
        }

    }

    /**
     * Strips the activity button down to a plain, borderless, unfilled image button (no margin/insets), but only
     * if a custom activate icon image was actually found on disk; otherwise the button keeps the platform's
     * default look and just shows text (see {@link #setActivateButtonText(String)}).
     */
    public void resetActivityButton() {
        if (activateIcon != null) {
            activityButton.setUI(new BasicButtonUI());
            java.awt.Insets noInsets = new Insets(0, 0, 0, 0);
            activityButton.setMargin(noInsets);
            activityButton.setBorder(BorderFactory.createEmptyBorder());
            activityButton.setContentAreaFilled(false);
            activityButton.setLayout(null);
            activityButton.setBorderPainted(false);
        }
    }

    /**
     * Builds the optional server-configured "link area": up to three small icon buttons (each independently shown
     * via its own {@code Enable_LinkN_Button} flag) that open a server-configured URL ({@code LinkN_URL}) in the
     * system's default browser via {@link Desktop#browse}, plus a label ({@code Link_Area_Label}). Assembles
     * {@link #linksPanel} and {@link #bottomPanel} (count panel above, links below) and adds {@link #bottomPanel}
     * to this panel. Only called when the server enables {@code Enable_Link_Area}; otherwise {@link #countPanel}
     * is added directly instead. Browse failures (e.g. malformed URL, unsupported platform) are caught and logged
     * rather than shown to the user.
     */
    private void createLinkArea() {
        linksLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        linksLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        linksLabel.setText(client.getServerConfigs("Link_Area_Label").trim());

        Icon link1Icon = new ImageIcon(client.getServerConfigs("Link1_Icon").trim());
        linkButton1.setEnabled(true);
        linkButton1.setIcon(link1Icon);
        linkButton1.setAlignmentX(Component.CENTER_ALIGNMENT);
        linkButton1.setAlignmentY(Component.CENTER_ALIGNMENT);
        linkButton1.setPreferredSize(new Dimension(30, 30));
        linkButton1.setMinimumSize(new Dimension(30, 30));
        linkButton1.setMaximumSize(new Dimension(30, 30));

        linkButton1.addActionListener(actionEvent -> {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                String uri = client.getServerConfigs("Link1_URL");

                try {
                    desktop.browse(new URI(uri));
                } catch (Exception e) {
                    LOGGER.error(e, "Unable to browse to (Link1_URL) {} due to: {}", uri, e.getLocalizedMessage());
                }
            }
        });

        linkButton1.setVisible(client.getServerConfigs("Enable_Link1_Button").equalsIgnoreCase("true"));

        //button2
        Icon link2Icon = new ImageIcon(client.getServerConfigs("Link2_Icon").trim());
        linkButton2.setEnabled(true);
        linkButton2.setIcon(link2Icon);
        linkButton2.setAlignmentX(Component.CENTER_ALIGNMENT);
        linkButton2.setAlignmentY(Component.CENTER_ALIGNMENT);
        linkButton2.setPreferredSize(new Dimension(30, 30));
        linkButton2.setMinimumSize(new Dimension(30, 30));
        linkButton2.setMaximumSize(new Dimension(30, 30));

        linkButton2.addActionListener(actionEvent -> {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                String uri = client.getServerConfigs("Link2_URL");
                try {
                    desktop.browse(new URI(client.getServerConfigs("Link2_URL")));
                } catch (Exception e) {
                    LOGGER.error(e, "Unable to browse to (Link2_URL) {} due to {}", uri, e.getLocalizedMessage());
                }
            }
        });

        linkButton2.setVisible(client.getServerConfigs("Enable_Link2_Button").equalsIgnoreCase("true"));

        //button3
        Icon link3Icon = new ImageIcon(client.getServerConfigs("Link3_Icon").trim());
        linkButton3.setEnabled(true);
        linkButton3.setIcon(link3Icon);
        linkButton3.setAlignmentX(Component.CENTER_ALIGNMENT);
        linkButton3.setAlignmentY(Component.CENTER_ALIGNMENT);
        linkButton3.setPreferredSize(new Dimension(30, 30));
        linkButton3.setMinimumSize(new Dimension(30, 30));
        linkButton3.setMaximumSize(new Dimension(30, 30));

        linkButton3.addActionListener(actionEvent -> {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                String uri = client.getServerConfigs("Link3_URL");
                try {
                    desktop.browse(new URI(uri));
                } catch (Exception e) {
                    LOGGER.error(e, "Unable to go to (Link3_URL) {} due to {}", uri, e.getLocalizedMessage());
                }
            }
        });

        linkButton3.setVisible(client.getServerConfigs("Enable_Link3_Button").equalsIgnoreCase("true"));


        linksPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        linksPanel.setBackground(Color.LIGHT_GRAY);
        linksPanel.setPreferredSize(new Dimension(175, 34));
        linksPanel.setMinimumSize(new Dimension(175, 34));
        linksPanel.setMaximumSize(new Dimension(175, 34));
        linksPanel.add(linksLabel);
        linksPanel.add(linkButton1);
        linksPanel.add(linkButton2);
        linksPanel.add(linkButton3);

        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(countPanel, BorderLayout.CENTER);
        bottomPanel.add(linksPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    /** @return the backing model listing connected players. */
    public CUserListModel getcUserListModel() {
        return cUserListModel;
    }

    /** @return the activity toggle button (Login/Activate/Deactivate). */
    public JButton getActivateButton() {
        return activityButton;
    }

    /** @return the label showing the current connected-player count. */
    public JLabel getCountLabel() {
        return countLabel;
    }

    /** @return whether this client instance is running as a dedicated server host. */
    public boolean isDedicated() {
        return isDedicated;
    }

    /** Marks whether this client instance is running as a dedicated server host. */
    public void setDedicated(boolean isDedicated) {
        this.isDedicated = isDedicated;
    }

    /** @return the client session this panel was built with. */
    public IClient getClient() {
        return client;
    }

    /** @return the {@link JList} widget rendering the user list. */
    public JList<CUserListModel> getcUserListModelJList() {
        return cUserListModelJList;
    }

    /**
     * Reloads the underlying model (re-fetching/re-sorting the connected-player list) and updates the visible
     * player-count label to match. Synchronized to avoid concurrent refreshes racing on {@link #cUserListModel}
     * (this can be invoked both from network-message handling and from UI actions). Any exception during the
     * model refresh is caught and logged rather than propagated, so the count label update still runs afterward.
     */
    public synchronized void refresh() {
        try {
            getcUserListModel().refreshModel();
        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to refresh the model. {}", ex.getLocalizedMessage());
        }
        countLabel.setText(String.format("Player Count: %s", cUserListModelJList.getModel().getSize()));
    }

    /** @return whether the local player is currently logged in. */
    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    /**
     * Updates the logged-in flag and relabels the activity button accordingly: "Activate" once logged in (the
     * player starts in reserve/inactive status after logging in, so the button invites them to activate), or
     * "Login" once logged out. In both branches the button itself is (re-)enabled.
     */
    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;

        if (loggedIn) {
            activityButton.setEnabled(true);//update button for status
            setActivateButtonText("Activate");
        } else {//logged out
            activityButton.setEnabled(true);//update button for status
            setActivateButtonText("Login");
        }
    }

    /**
     * Sets the activity button's visible label, but only if no icon image is currently set on the button — when
     * an icon is present, the text is cleared instead so the icon alone represents the state (see
     * {@link #resetActivityButton()}/{@link #setActivityButton(Boolean)} for how icons get attached).
     *
     * @param s the label to show when the button is text-only (no icon)
     */
    public void setActivateButtonText(String s) {
        if (activityButton.getIcon() == null) {
            activityButton.setText(s);
        } else {
            activityButton.setText("");
        }
    }

    /**
     * Flips the activity button between its two active-session states and plays an optional flash animation via
     * {@link ActivationThread} plus an optional sound cue.
     * <p>
     * <b>Naming note:</b> {@code activate == true} means the button should now prompt the player to
     * <em>activate</em> — i.e. the player has just gone inactive/reserve, so this plays the "deactivate" sound
     * and flashes from the deactivate icon into the activate icon. Conversely {@code activate == false} means the
     * player just activated, so the button now prompts "Deactivate". The parameter therefore describes the
     * button's next prompt/target state, not the player's current status.
     *
     * @param activate {@code true} to show the "Activate" prompt (player just went inactive); {@code false} to
     *                 show the "Deactivate" prompt (player just went active)
     */
    public void setActivityButton(Boolean activate) {
        if (activate) {
            setActivateButtonText("Activate");

            if (client.getConfig().isParam("ENABLE_DEACTIVATE_SOUND")) {
                client.doPlaySound(client.getConfigParam("SOUND_ON_DEACTIVATE"));
            }

            ActivationThread animator = new ActivationThread(client,
                  activityButton,
                  deactivateFlashIcon,
                  activateIcon,
                  mouseActivateIcon);
            animator.start();

        } else {
            setActivateButtonText("Deactivate");

            if (client.getConfig().isParam("ENABLE_ACTIVATE_SOUND")) {
                client.doPlaySound(client.getConfigParam("SOUND_ON_ACTIVATE"));
            }

            ActivationThread animator = new ActivationThread(client,
                  activityButton,
                  activateFlashIcon,
                  deactivateIcon,
                  mouseDeactivateIcon);
            animator.start();
        }
    }

    /** Enables or disables the activity toggle button without changing its label/icon. */
    public void setActivityButtonEnabled(boolean activityButtonEnabled) {
        activityButton.setEnabled(activityButtonEnabled);
    }

    /**
     * ActionPerformed method, to comply with ActionListener.
     * <p>
     * If activityButton is pressed, look at client's current login/activity status and act accordingly: sends a
     * {@code /c activate#<clientVersion>} chat command when reserve (moving the player to active status), a
     * {@code /c deactivate} when active, or a {@code /c login} when logged out. Ignores the event entirely if it
     * didn't come from {@link #activityButton} (this class only ever registers itself as a listener on that one
     * button, so in practice the source check always passes).
     */
    public void actionPerformed(java.awt.event.ActionEvent e) {
        if (e.getSource() == activityButton) {
            if (client.getMyStatus() == IClient.STATUS_RESERVE)//is reserve
            {
                client.sendChat(String.format("%sc activate#%s", IClient.CAMPAIGN_PREFIX, IClient.CLIENT_VERSION));
            } else if (client.getMyStatus() == IClient.STATUS_ACTIVE) {
                client.sendChat("/c deactivate");
            } else if (client.getMyStatus() == IClient.STATUS_LOGGED_OUT) {
                client.sendChat("/c login");
            }
        }
    }
}

