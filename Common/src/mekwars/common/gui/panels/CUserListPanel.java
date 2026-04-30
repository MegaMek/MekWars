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

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.UserListPopupListener;
import mekwars.common.gui.models.CUserListModel;
import mekwars.common.threads.ActivationThread;
import mekwars.common.util.MWLogger;
import mekwars.common.util.StringUtils;

/**
 * User List panel
 */

public class CUserListPanel extends JPanel implements ActionListener {

    /**
     *
     */
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
    IClient client;
    boolean LoggedIn = false;
    boolean Dedicated;
    JScrollPane UserListSP;
    JList<CUserListModel> UserList;
    CUserListModel Users;
    //additional info
    JPanel countPanel = new JPanel();
    JLabel CountLabel = new JLabel();
    JLabel LinksLabel = new JLabel();
    JButton ActivityButton = new JButton();
    JButton LinkButton1 = new JButton();
    JButton LinkButton2 = new JButton();
    JButton LinkButton3 = new JButton();
    JPanel linksPanel = new JPanel();
    JPanel bottomPanel = new JPanel();
    UserListPopupListener UserListPopup = new UserListPopupListener(this);

    public CUserListPanel(IClient client) {
        this.client = client;
        Dedicated = this.client.getConfig().isParam("USERLISTDEDICATEDS");
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(180, 480));
        setMinimumSize(new Dimension(120, 100));
        setMaximumSize(new Dimension(180, 2000));

        Users = new CUserListModel(this.client);
        UserList = new JList<>();
        UserList.add(Users.getRenderer());
        UserList.setAlignmentX(0.0F);
        UserList.addMouseListener(UserListPopup);
        UserList.setCellRenderer(Users.getRenderer());
        UserListSP = new JScrollPane(UserList);
        UserListSP.setPreferredSize(new Dimension(180, 380));
        UserListSP.setMinimumSize(new Dimension(180, 100));
        UserListSP.setMaximumSize(new Dimension(180, 2000));
        UserListSP.setBorder(new LineBorder(Color.black));
        UserListSP.setViewportView(UserList);
        add(UserListSP, BorderLayout.CENTER);

        UserList.setBackground(StringUtils.html2Color(this.client.getConfigParam("BACKGROUNDCOLOR")));
        //set up the count label
        CountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        CountLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        CountLabel.setBorder(BorderFactory.createEmptyBorder(3, 2, 2, 2));
        CountLabel.setText(STR."Player Count: \{UserList.getModel().getSize()}");
        CountLabel.setVisible(this.client.getConfig().isParam("USERLISTCOUNT"));

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
        //setActivateButtonText("Waiting ...");
        ActivityButton.setEnabled(false);
        ActivityButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        ActivityButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        ActivityButton.addActionListener(this);
        ActivityButton.setRolloverEnabled(true);
        ActivityButton.setRolloverIcon(mouseActivateIcon);
        ActivityButton.setIcon(activateIcon);

        resetActivityButton();

        ActivityButton.setVisible(this.client.getConfig().isParam("USERLISTACTIVITYBTN"));

        //add the button and label to CountPanel
        countPanel.setLayout(new BoxLayout(countPanel, BoxLayout.Y_AXIS));
        countPanel.add(ActivityButton);
        countPanel.add(CountLabel);

        //@ Salient - toggle for link area
        if (this.client.getServerConfigs("Enable_Link_Area").equalsIgnoreCase("false")) {
            countPanel.setBorder(BorderFactory.createEmptyBorder(4, 2, 3, 2));
            add(countPanel, BorderLayout.SOUTH);
        } else {
            createLinkArea();
        }

        //restore the previous sort mode
        String mode = this.client.getConfig().getParam("SORTMODE");
        switch (mode) {
            case "HOUSE" -> ((CUserListModel) UserList.getModel()).setSortMode(SORT_MODE_HOUSE);
            case "EXP" -> ((CUserListModel) UserList.getModel()).setSortMode(SORT_MODE_EXP);
            case "RATING" -> {
                if (!Boolean.parseBoolean(this.client.getServerConfigs("HideELO"))) {
                    ((CUserListModel) UserList.getModel()).setSortMode(SORT_MODE_RATING);
                } else {
                    ((CUserListModel) UserList.getModel()).setSortMode(SORT_MODE_NAME);
                }
            }
            case "STATUS" -> ((CUserListModel) UserList.getModel()).setSortMode(SORT_MODE_STATUS);
            case "USERLEVEL" -> ((CUserListModel) UserList.getModel()).setSortMode(SORT_MODE_USER_LEVEL);
            case "COUNTRY" -> ((CUserListModel) UserList.getModel()).setSortMode(SORT_MODE_COUNTRY);
            default -> ((CUserListModel) UserList.getModel()).setSortMode(SORT_MODE_NAME);
        }

        //restore the previous sort order
        String order = this.client.getConfig().getParam("SORTORDER");
        if (order.equals("DESCENDING")) {
            ((CUserListModel) UserList.getModel()).setSortOrder(SORT_ORDER_DESCENDING);
        } else {
            ((CUserListModel) UserList.getModel()).setSortOrder(SORT_ORDER_ASCENDING);
        }

    }

    public void resetActivityButton() {
        if (activateIcon != null) {
            ActivityButton.setUI(new BasicButtonUI());
            java.awt.Insets noInsets = new Insets(0, 0, 0, 0);
            ActivityButton.setMargin(noInsets);
            ActivityButton.setBorder(BorderFactory.createEmptyBorder());
            ActivityButton.setContentAreaFilled(false);
            ActivityButton.setLayout(null);
            ActivityButton.setBorderPainted(false);
        }
    }

    private void createLinkArea() {
        LinksLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        LinksLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        LinksLabel.setText(client.getServerConfigs("Link_Area_Label").trim());

        Icon link1Icon = new ImageIcon(client.getServerConfigs("Link1_Icon").trim());
        LinkButton1.setEnabled(true);
        LinkButton1.setIcon(link1Icon);
        LinkButton1.setAlignmentX(Component.CENTER_ALIGNMENT);
        LinkButton1.setAlignmentY(Component.CENTER_ALIGNMENT);
        LinkButton1.setPreferredSize(new Dimension(30, 30));
        LinkButton1.setMinimumSize(new Dimension(30, 30));
        LinkButton1.setMaximumSize(new Dimension(30, 30));

        LinkButton1.addActionListener(_ -> {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI(client.getServerConfigs("Link1_URL")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        LinkButton1.setVisible(client.getServerConfigs("Enable_Link1_Button").equalsIgnoreCase("true"));

        //button2
        Icon link2Icon = new ImageIcon(client.getServerConfigs("Link2_Icon").trim());
        LinkButton2.setEnabled(true);
        LinkButton2.setIcon(link2Icon);
        LinkButton2.setAlignmentX(Component.CENTER_ALIGNMENT);
        LinkButton2.setAlignmentY(Component.CENTER_ALIGNMENT);
        LinkButton2.setPreferredSize(new Dimension(30, 30));
        LinkButton2.setMinimumSize(new Dimension(30, 30));
        LinkButton2.setMaximumSize(new Dimension(30, 30));

        LinkButton2.addActionListener(_ -> {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI(client.getServerConfigs("Link2_URL")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        if (client.getServerConfigs("Enable_Link2_Button").equalsIgnoreCase("true")) {
            LinkButton2.setVisible(true);
        } else {
            LinkButton2.setVisible(false);
        }

        //button3
        Icon link3Icon = new ImageIcon(client.getServerConfigs("Link3_Icon").trim());
        LinkButton3.setEnabled(true);
        LinkButton3.setIcon(link3Icon);
        LinkButton3.setAlignmentX(Component.CENTER_ALIGNMENT);
        LinkButton3.setAlignmentY(Component.CENTER_ALIGNMENT);
        LinkButton3.setPreferredSize(new Dimension(30, 30));
        LinkButton3.setMinimumSize(new Dimension(30, 30));
        LinkButton3.setMaximumSize(new Dimension(30, 30));

        LinkButton3.addActionListener(_ -> {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI(client.getServerConfigs("Link3_URL")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        LinkButton3.setVisible(client.getServerConfigs("Enable_Link3_Button").equalsIgnoreCase("true"));


        linksPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        linksPanel.setBackground(Color.LIGHT_GRAY);
        linksPanel.setPreferredSize(new Dimension(175, 34));
        linksPanel.setMinimumSize(new Dimension(175, 34));
        linksPanel.setMaximumSize(new Dimension(175, 34));
        linksPanel.add(LinksLabel);
        linksPanel.add(LinkButton1);
        linksPanel.add(LinkButton2);
        linksPanel.add(LinkButton3);

        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(countPanel, BorderLayout.CENTER);
        bottomPanel.add(linksPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public CUserListModel getUsers() {
        return Users;
    }

    public JList<CUserListModel> getUserList() {
        return UserList;
    }

    public synchronized void refresh() {
        try {
            ((CUserListModel) UserList.getModel()).refreshModel();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
        CountLabel.setText(STR."Player Count: \{UserList.getModel().getSize()}");
    }

    public void setLoggedIn(boolean loggedIn) {
        LoggedIn = loggedIn;

        if (LoggedIn) {
            ActivityButton.setEnabled(true);//update button for status
            setActivateButtonText("Activate");
        } else {//logged out
            ActivityButton.setEnabled(true);//update button for status
            setActivateButtonText("Login");
        }
    }

    public void setActivateButtonText(String s) {
        if (ActivityButton.getIcon() == null) {
            ActivityButton.setText(s);
        } else {
            ActivityButton.setText("");
        }
    }

    public void setActivityButton(Boolean activate) {
        if (activate) {
            setActivateButtonText("Activate");
            if (client.getConfig().isParam("ENABLEDEACTIVATESOUND")) {
                client.doPlaySound(client.getConfigParam("SOUNDONDEACTIVATE"));
            }
            ActivationThread animator = new ActivationThread(client,
                  ActivityButton,
                  deactivateFlashIcon,
                  activateIcon,
                  mouseActivateIcon);
            animator.start();

        } else {
            setActivateButtonText("Deactivate");
            if (client.getConfig().isParam("ENABLEACTIVATESOUND")) {
                client.doPlaySound(client.getConfigParam("SOUNDONACTIVATE"));
            }
            ActivationThread animator = new ActivationThread(client,
                  ActivityButton,
                  activateFlashIcon,
                  deactivateIcon,
                  mouseDeactivateIcon);
            animator.start();
        }
    }

    public void setActivityButtonEnabled(boolean b) {
        ActivityButton.setEnabled(b);
    }

    /**
     * ActionPerformed method, to comply with ActionListener.
     * <p>
     * If ActivityButton is pressed, look at Client's current login/activity status and act accordingly.
     */
    public void actionPerformed(java.awt.event.ActionEvent e) {
        if (e.getSource() == ActivityButton) {
            if (client.getMyStatus() == IClient.STATUS_RESERVE)//is reserve
            {
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c activate#\{IClient.CLIENT_VERSION}");
            } else if (client.getMyStatus() == IClient.STATUS_ACTIVE) {
                client.sendChat("/c deactivate");
            } else if (client.getMyStatus() == IClient.STATUS_LOGGED_OUT)//is logged out
            {
                client.sendChat("/c login");
            }
        }
    }

}

