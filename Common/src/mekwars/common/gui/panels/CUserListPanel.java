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
 * User List panel
 */

public class CUserListPanel extends JPanel implements ActionListener {
    private static final MMLogger LOGGER = MMLogger.create(CUserListPanel.class);

    @Serial
    private static final long serialVersionUID = 6676029823454849117L;
    public static int SORT_MODE_NAME = 0;
    public static int SORT_MODE_HOUSE = 1;
    public static int SORT_MODE_EXP = 2;
    public static int SORT_MODE_RATING = 3;
    public static int SORT_MODE_STATUS = 4;
    public static int SORT_MODE_USER_LEVEL = 5;
    public static int SORT_MODE_COUNTRY = 6;

    public static int SORT_ORDER_ASCENDING = 1;
    public static int SORT_ORDER_DESCENDING = 2;
    private final Icon activateIcon;
    private final Icon deactivateIcon;
    private final Icon mouseActivateIcon;
    private final Icon mouseDeactivateIcon;
    private final Icon activateFlashIcon;
    private final Icon deactivateFlashIcon;
    private final JPanel countPanel = new JPanel();
    private final JLabel countLabel = new JLabel();
    private final JLabel linksLabel = new JLabel();
    private final JButton activityButton = new JButton();
    private final JButton linkButton1 = new JButton();
    private final JButton linkButton2 = new JButton();
    private final JButton linkButton3 = new JButton();
    private final JPanel linksPanel = new JPanel();
    private final JPanel bottomPanel = new JPanel();
    private final IClient client;
    private final JList<CUserListModel> cUserListModelJList;
    private final CUserListModel cUserListModel;
    private boolean isLoggedIn = false;
    private boolean isDedicated = false;

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

        linkButton1.addActionListener(_ -> {
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

        linkButton2.addActionListener(_ -> {
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

        linkButton3.addActionListener(_ -> {
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

    public CUserListModel getcUserListModel() {
        return cUserListModel;
    }

    public JButton getActivateButton() {
        return activityButton;
    }

    public JLabel getCountLabel() {
        return countLabel;
    }

    public boolean isDedicated() {
        return isDedicated;
    }

    public void setDedicated(boolean isDedicated) {
        this.isDedicated = isDedicated;
    }

    public IClient getClient() {
        return client;
    }

    public JList<CUserListModel> getcUserListModelJList() {
        return cUserListModelJList;
    }

    public synchronized void refresh() {
        try {
            getcUserListModel().refreshModel();
        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to refresh the model. {}", ex.getLocalizedMessage());
        }
        countLabel.setText(String.format("Player Count: %s", cUserListModelJList.getModel().getSize()));
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

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

    public void setActivateButtonText(String s) {
        if (activityButton.getIcon() == null) {
            activityButton.setText(s);
        } else {
            activityButton.setText("");
        }
    }

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

    public void setActivityButtonEnabled(boolean activityButtonEnabled) {
        activityButton.setEnabled(activityButtonEnabled);
    }

    /**
     * ActionPerformed method, to comply with ActionListener.
     * <p>
     * If activityButton is pressed, look at client's current login/activity status and act accordingly.
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

