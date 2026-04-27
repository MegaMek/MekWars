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

import common.campaign.clientutils.IClientConfig;
import common.util.MWLogger;
import common.util.StringUtils;

//import com.sun.jndi.toolkit.url.Uri;


//admin import. har!
//import admin.ModeratorPopupMenu;

/**
 * User List panel
 */

public class CUserListPanel extends javax.swing.JPanel implements java.awt.event.ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = 6676029823454849117L;
    public static int SORTMODE_NAME = 0;
    public static int SORTMODE_HOUSE = 1;
    public static int SORTMODE_EXP = 2;
    public static int SORTMODE_RATING = 3;
    public static int SORTMODE_STATUS = 4;
    public static int SORTMODE_USERLEVEL = 5;
    public static int SORTMODE_COUNTRY = 6;

    public static int SORTORDER_ASCENDING = 1;
    public static int SORTORDER_DESCENDING = 2;

    client.MWClient mwclient;
    IClientConfig Config;
    boolean LoggedIn = false;
    boolean Dedicateds;
    javax.swing.JScrollPane UserListSP;
    javax.swing.JList UserList;
    mekwars.client.gui.CUserListPanel.CUserListModel Users;

    //additional info
    javax.swing.JPanel countPanel = new javax.swing.JPanel();
    javax.swing.JLabel CountLabel = new javax.swing.JLabel();
    javax.swing.JLabel LinksLabel = new javax.swing.JLabel();
    javax.swing.JButton ActivityButton = new javax.swing.JButton();
    //@ Salient new buttons for discord/facebook/www
    javax.swing.JButton LinkButton = new javax.swing.JButton();
    javax.swing.JButton LinkButton1 = new javax.swing.JButton();
    javax.swing.JButton LinkButton2 = new javax.swing.JButton();
    javax.swing.JButton LinkButton3 = new javax.swing.JButton();
    javax.swing.JPanel linksPanel = new javax.swing.JPanel();
    javax.swing.JPanel bottomPanel = new javax.swing.JPanel();

    mekwars.client.gui.CUserListPanel.UserListPopupListener UserListPopup = new mekwars.client.gui.CUserListPanel.UserListPopupListener();

    private javax.swing.Icon link1Icon = null;
    private javax.swing.Icon link2Icon = null;
    private javax.swing.Icon link3Icon = null;
    private javax.swing.Icon activateIcon = null;
    private javax.swing.Icon deactivateIcon = null;
    private javax.swing.Icon mouseActivateIcon = null;
    private javax.swing.Icon mouseDeactivateIcon = null;
    private javax.swing.Icon activateFlashIcon = null;
    private javax.swing.Icon deactivateFlashIcon = null;

    public CUserListPanel(client.MWClient client) {
        mwclient = client;
        Dedicateds = mwclient.getConfig().isParam("USERLISTDEDICATEDS");
        setLayout(new java.awt.BorderLayout());
        setPreferredSize(new java.awt.Dimension(180, 480));
        setMinimumSize(new java.awt.Dimension(120, 100));
        setMaximumSize(new java.awt.Dimension(180, 2000));

        Users = new mekwars.client.gui.CUserListPanel.CUserListModel(mwclient);
        UserList = new javax.swing.JList(Users);
        UserList.setAlignmentX(0.0F);
        UserList.addMouseListener(UserListPopup);
        UserList.setCellRenderer(Users.getRenderer());
        UserListSP = new javax.swing.JScrollPane(UserList);
        UserListSP.setPreferredSize(new java.awt.Dimension(180, 380));
        UserListSP.setMinimumSize(new java.awt.Dimension(180, 100));
        UserListSP.setMaximumSize(new java.awt.Dimension(180, 2000));
        UserListSP.setBorder(new javax.swing.border.LineBorder(java.awt.Color.black));
        UserListSP.setViewportView(UserList);
        add(UserListSP, java.awt.BorderLayout.CENTER);

        UserList.setBackground(StringUtils.html2Color(mwclient.getConfigParam("BACKGROUNDCOLOR")));
        //set up the countlabel
        CountLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        CountLabel.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
        CountLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 2, 2, 2));
        CountLabel.setText("Player Count: " + UserList.getModel().getSize());
        if (mwclient.getConfig().isParam("USERLISTCOUNT")) {CountLabel.setVisible(true);} else {
            CountLabel.setVisible(false);
        }

        if (new java.io.File("./data/images/activatebutton.png").exists()) {
            activateIcon = new javax.swing.ImageIcon("./data/images/activatebutton.png");
        } else if (new java.io.File("./data/images/activatebutton.jpg").exists()) {
            activateIcon = new javax.swing.ImageIcon("./data/images/activatebutton.jpg");
        } else {activateIcon = null;}

        if (new java.io.File("./data/images/deactivatebutton.png").exists()) {
            deactivateIcon = new javax.swing.ImageIcon("./data/images/deactivatebutton.png");
        } else if (new java.io.File("./data/images/deactivatebutton.jpg").exists()) {
            deactivateIcon = new javax.swing.ImageIcon("./data/images/deactivatebutton.jpg");
        } else {deactivateIcon = null;}

        if (new java.io.File("./data/images/activatebuttonmouse.png").exists()) {
            mouseActivateIcon = new javax.swing.ImageIcon("./data/images/activatebuttonmouse.png");
        } else if (new java.io.File("./data/images/activatebuttonmouse.jpg").exists()) {
            mouseActivateIcon = new javax.swing.ImageIcon("./data/images/activatebuttonmouse.jpg");
        } else {mouseActivateIcon = null;}

        if (new java.io.File("./data/images/deactivatebuttonmouse.png").exists()) {
            mouseDeactivateIcon = new javax.swing.ImageIcon("./data/images/deactivatebuttonmouse.png");
        } else if (new java.io.File("./data/images/deactivatebuttonmouse.jpg").exists()) {
            mouseDeactivateIcon = new javax.swing.ImageIcon("./data/images/deactivatebuttonmouse.jpg");
        } else {mouseDeactivateIcon = null;}

        if (new java.io.File("./data/images/activateflashbutton.png").exists()) {
            activateFlashIcon = new javax.swing.ImageIcon("./data/images/activateflashbutton.png");
        } else if (new java.io.File("./data/images/activateflashbutton.jpg").exists()) {
            activateFlashIcon = new javax.swing.ImageIcon("./data/images/activateflashbutton.jpg");
        } else {activateFlashIcon = null;}

        if (new java.io.File("./data/images/deactivateflashbutton.png").exists()) {
            deactivateFlashIcon = new javax.swing.ImageIcon("./data/images/deactivateflashbutton.png");
        } else if (new java.io.File("./data/images/deactivateflashbutton.jpg").exists()) {
            deactivateFlashIcon = new javax.swing.ImageIcon("./data/images/deactivateflashbutton.jpg");
        } else {deactivateFlashIcon = null;}

        //set up activity button
        //setActivateButtonText("Waiting ...");
        ActivityButton.setEnabled(false);
        ActivityButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        ActivityButton.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
        ActivityButton.addActionListener(this);
        ActivityButton.setRolloverEnabled(true);
        ActivityButton.setRolloverIcon(mouseActivateIcon);
        ActivityButton.setIcon(activateIcon);

        resetActivityButton();

        if (mwclient.getConfig().isParam("USERLISTACTIVITYBTN")) {ActivityButton.setVisible(true);} else {
            ActivityButton.setVisible(false);
        }

        //add the button and label to CountPanel
        countPanel.setLayout(new javax.swing.BoxLayout(countPanel, javax.swing.BoxLayout.Y_AXIS));
        countPanel.add(ActivityButton);
        countPanel.add(CountLabel);

        //@ Salient - toggle for link area
        if (mwclient.getserverConfigs("Enable_Link_Area").equalsIgnoreCase("false")) {
            countPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 2, 3, 2));
            add(countPanel, java.awt.BorderLayout.SOUTH);
        } else {createLinkArea();}

        //restore the previous sort mode
        String mode = mwclient.getConfig().getParam("SORTMODE");
        if (mode.equals("HOUSE")) {
            ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(SORTMODE_HOUSE);
        } else if (mode.equals("EXP")) {
            ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(SORTMODE_EXP);
        } else if (mode.equals("RATING")) {
            if (!Boolean.parseBoolean(mwclient.getserverConfigs("HideELO"))) {
                ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(SORTMODE_RATING);
            } else {
                ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(SORTMODE_NAME);
            }
        } else if (mode.equals("STATUS")) {
            ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(SORTMODE_STATUS);
        } else if (mode.equals("USERLEVEL")) {
            ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(SORTMODE_USERLEVEL);
        } else if (mode.equals("COUNTRY")) {
            ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(SORTMODE_COUNTRY);
        } else {((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(SORTMODE_NAME);}

        //restore the previous sort order
        String order = mwclient.getConfig().getParam("SORTORDER");
        if (order.equals("DESCENDING")) {
            ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortOrder(SORTORDER_DESCENDING);
        } else {
            ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortOrder(SORTORDER_ASCENDING);
        }

    }

    public void resetActivityButton() {
        if (activateIcon != null) {
            ActivityButton.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            java.awt.Insets noInsets = new java.awt.Insets(0, 0, 0, 0);
            ActivityButton.setMargin(noInsets);
            ActivityButton.setBorder(javax.swing.BorderFactory.createEmptyBorder());
            ActivityButton.setContentAreaFilled(false);
            ActivityButton.setLayout(null);
            ActivityButton.setBorderPainted(false);
        }
    }

    private void createLinkArea() {
        LinksLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        LinksLabel.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
        LinksLabel.setText(mwclient.getserverConfigs("Link_Area_Label").trim());

        link1Icon = new javax.swing.ImageIcon(mwclient.getserverConfigs("Link1_Icon").trim());
        LinkButton1.setEnabled(true);
        LinkButton1.setIcon(link1Icon);
        LinkButton1.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        LinkButton1.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
        LinkButton1.setPreferredSize(new java.awt.Dimension(30, 30));
        LinkButton1.setMinimumSize(new java.awt.Dimension(30, 30));
        LinkButton1.setMaximumSize(new java.awt.Dimension(30, 30));

        LinkButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                java.awt.Desktop desktop = java.awt.Desktop.isDesktopSupported() ? java.awt.Desktop.getDesktop() : null;
                if (desktop != null && desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    try {
                        desktop.browse(new java.net.URL(mwclient.getserverConfigs("Link1_URL")).toURI());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        if (mwclient.getserverConfigs("Enable_Link1_Button").equalsIgnoreCase("true")) {
            LinkButton1.setVisible(true);
        } else {LinkButton1.setVisible(false);}

        //button2
        link2Icon = new javax.swing.ImageIcon(mwclient.getserverConfigs("Link2_Icon").trim());
        LinkButton2.setEnabled(true);
        LinkButton2.setIcon(link2Icon);
        LinkButton2.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        LinkButton2.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
        LinkButton2.setPreferredSize(new java.awt.Dimension(30, 30));
        LinkButton2.setMinimumSize(new java.awt.Dimension(30, 30));
        LinkButton2.setMaximumSize(new java.awt.Dimension(30, 30));

        LinkButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                java.awt.Desktop desktop = java.awt.Desktop.isDesktopSupported() ? java.awt.Desktop.getDesktop() : null;
                if (desktop != null && desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    try {
                        desktop.browse(new java.net.URL(mwclient.getserverConfigs("Link2_URL")).toURI());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        if (mwclient.getserverConfigs("Enable_Link2_Button").equalsIgnoreCase("true")) {
            LinkButton2.setVisible(true);
        } else {LinkButton2.setVisible(false);}

        //button3
        link3Icon = new javax.swing.ImageIcon(mwclient.getserverConfigs("Link3_Icon").trim());
        LinkButton3.setEnabled(true);
        LinkButton3.setIcon(link3Icon);
        LinkButton3.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        LinkButton3.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
        LinkButton3.setPreferredSize(new java.awt.Dimension(30, 30));
        LinkButton3.setMinimumSize(new java.awt.Dimension(30, 30));
        LinkButton3.setMaximumSize(new java.awt.Dimension(30, 30));

        LinkButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                java.awt.Desktop desktop = java.awt.Desktop.isDesktopSupported() ? java.awt.Desktop.getDesktop() : null;
                if (desktop != null && desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    try {
                        desktop.browse(new java.net.URL(mwclient.getserverConfigs("Link3_URL")).toURI());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        if (mwclient.getserverConfigs("Enable_Link3_Button").equalsIgnoreCase("true")) {
            LinkButton3.setVisible(true);
        } else {LinkButton3.setVisible(false);}


        linksPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.TRAILING));
        linksPanel.setBackground(java.awt.Color.LIGHT_GRAY);
        linksPanel.setPreferredSize(new java.awt.Dimension(175, 34));
        linksPanel.setMinimumSize(new java.awt.Dimension(175, 34));
        linksPanel.setMaximumSize(new java.awt.Dimension(175, 34));
        linksPanel.add(LinksLabel);
        linksPanel.add(LinkButton1);
        linksPanel.add(LinkButton2);
        linksPanel.add(LinkButton3);

        bottomPanel.setLayout(new java.awt.BorderLayout());
        bottomPanel.add(countPanel, java.awt.BorderLayout.CENTER);
        bottomPanel.add(linksPanel, java.awt.BorderLayout.SOUTH);

        add(bottomPanel, java.awt.BorderLayout.SOUTH);
    }

    public mekwars.client.gui.CUserListPanel.CUserListModel getUsers() {return Users;}

    public javax.swing.JList getUserList() {return UserList;}

    public synchronized void refresh() {
        try {
            ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).refreshModel();
        } catch (Exception ex) {MWLogger.errLog(ex);}
        CountLabel.setText("Player Count: " + UserList.getModel().getSize());
    }

    public void setLoggedIn(boolean tloggedin) {
        LoggedIn = tloggedin;
        if (LoggedIn) {
            ActivityButton.setEnabled(true);//update button for status
            //setActivityButton(true);
            setActivateButtonText("Activate");
        } else {//logged out
            ActivityButton.setEnabled(true);//update button for status
            setActivateButtonText("Login");
        }
    }

    public void setActivateButtonText(String s) {

        if (ActivityButton.getIcon() == null) {
            ActivityButton.setText(s);
            //ActivityButton.setBorder(BorderFactory.createEtchedBorder());
        } else {
            ActivityButton.setText("");
            //ActivityButton.setBorder(BorderFactory.createEmptyBorder());
        }
    }

    public void setActivityButton(Boolean activate) {

        if (activate) {
            setActivateButtonText("Activate");
            if (mwclient.getConfig().isParam("ENABLEDEACTIVATESOUND")) {
                mwclient.doPlaySound(mwclient.getConfigParam("SOUNDONDEACTIVATE"));
            }
            mekwars.client.gui.ActivationThread animator = new mekwars.client.gui.ActivationThread(mwclient,
                  ActivityButton,
                  deactivateFlashIcon,
                  activateIcon,
                  mouseActivateIcon);
            animator.start();

        } else {
            setActivateButtonText("Deactivate");
            if (mwclient.getConfig().isParam("ENABLEACTIVATESOUND")) {
                mwclient.doPlaySound(mwclient.getConfigParam("SOUNDONACTIVATE"));
            }
            mekwars.client.gui.ActivationThread animator = new mekwars.client.gui.ActivationThread(mwclient,
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
            if (mwclient.getMyStatus() == client.MWClient.STATUS_RESERVE)//is reserve
            {
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX +
                                        "c activate#" +
                                        client.MWClient.CLIENT_VERSION);
            } else if (mwclient.getMyStatus() == client.MWClient.STATUS_ACTIVE)//is active
            {mwclient.sendChat("/c deactivate");} else if (mwclient.getMyStatus() ==
                                                                 client.MWClient.STATUS_LOGGEDOUT)//is logged out
            {mwclient.sendChat("/c login");}
        }
    }

    public static class CUserListModel extends javax.swing.AbstractListModel {
        /**
         *
         */
        private static final long serialVersionUID = 9141928592065940657L;
        java.util.SortedSet<client.CUser> Users;  //users set
        mekwars.client.gui.CUserListPanel.CUserListModel.UserListCellRenderer Renderer;  //list cells renderer
        client.MWClient mwclient;  //client owning this model
        boolean Dedicateds; //dedicated hosts visible


        public CUserListModel(client.MWClient client) {
            mwclient = client;
            Dedicateds = mwclient.getConfig().isParam("USERLISTDEDICATEDS");
            Users = java.util.Collections.synchronizedSortedSet(new java.util.TreeSet<client.CUser>(new mekwars.client.gui.CUserListPanel.CUserListModel.UserComparator()));
            Renderer = new mekwars.client.gui.CUserListPanel.CUserListModel.UserListCellRenderer(this);
        }

        public synchronized void remove(client.CUser user) {Users.remove(user);}

        public synchronized void addAll(java.util.Collection<client.CUser> c) {Users.addAll(c);}

        public synchronized int getSize() {return Users.size();}

        public synchronized Object getElementAt(int index) {
            if (index < Users.size()) {return (((client.CUser) Users.toArray()[index]).getName());}
            //else
            return null;
        }

        public void setDedicateds(boolean dedicateds) {Dedicateds = dedicateds;}

        public int getSortMode() {return ((mekwars.client.gui.CUserListPanel.CUserListModel.UserComparator) Users.comparator()).getMode();}

        public void setSortMode(int tsortmode) {
            ((mekwars.client.gui.CUserListPanel.CUserListModel.UserComparator) Users.comparator()).setMode(tsortmode);
            refreshModel();
        }

        public synchronized void refreshModel() {

            fireIntervalRemoved(this, 0, Users.size());
            clear();
            int myLevel = mwclient.getUserLevel();

            /*
             * Synch on client.getUsers() to prevent ConcurrentModError
             * while rebuilding the CUserListPanel.
             */
            java.util.Collection<client.CUser> users = mwclient.getUsers();
            synchronized (users) {
                for (client.CUser currU : users) {
                    if (currU.isInvis() && myLevel < currU.getUserlevel()) {continue;}
                    if (currU.getName().startsWith("[Dedicated]") && !Dedicateds) {continue;}
                    add(currU);
                }
            }

            fireIntervalAdded(this, 0, Users.size());
        }

        public synchronized void clear() {Users.clear();}

        public void add(client.CUser user) {Users.add(user);}

        public int getSortOrder() {return ((mekwars.client.gui.CUserListPanel.CUserListModel.UserComparator) Users.comparator()).getOrder();}

        public void setSortOrder(int tsortorder) {
            ((mekwars.client.gui.CUserListPanel.CUserListModel.UserComparator) Users.comparator()).setOrder(tsortorder);
            refreshModel();
        }

        public synchronized client.CUser getUser(int index) {
            if (index < Users.size()) {return ((client.CUser) Users.toArray()[index]);}
            //else
            return null;
        }

        public synchronized client.CUser getUser(String name) {
            for (java.util.Iterator<client.CUser> i = Users.iterator(); i.hasNext(); ) {
                client.CUser user = i.next();
                if (user.getName().equals(name)) {return user;}
            }
            return new client.CUser();
        }

        public mekwars.client.gui.CUserListPanel.CUserListModel.UserListCellRenderer getRenderer() {return Renderer;}

        static class UserListCellRenderer extends javax.swing.JLabel implements javax.swing.ListCellRenderer {

            /**
             *
             */
            private static final long serialVersionUID = 4400213401819469963L;
            client.MWClient ulMwclient;
            mekwars.client.gui.CUserListPanel.CUserListModel Owner;
            boolean LoggedIn = false;
            boolean TextBold = true;
            boolean TextColor = true;
            boolean TextImage = true;
            javax.swing.ImageIcon LogoutImage;
            javax.swing.ImageIcon ReserveImage;
            javax.swing.ImageIcon ActiveImage;
            javax.swing.ImageIcon FightImage;

            public UserListCellRenderer(mekwars.client.gui.CUserListPanel.CUserListModel towner) {
                Owner = towner;
                ulMwclient = towner.mwclient;
                TextBold = ulMwclient.getConfig().isParam("USERLISTBOLD");
                TextColor = ulMwclient.getConfig().isParam("USERLISTCOLOR");
                TextImage = ulMwclient.getConfig().isParam("USERLISTIMAGE");
                LogoutImage = ulMwclient.getConfig().getImage("LOGOUT");
                ReserveImage = ulMwclient.getConfig().getImage("RESERVE");
                ActiveImage = ulMwclient.getConfig().getImage("ACTIVE");
                FightImage = ulMwclient.getConfig().getImage("FIGHT");
                setOpaque(true);
            }

            public void setLoggedIn(boolean tloggedin) {LoggedIn = tloggedin;}

            public void refreshParams() {
                TextBold = ulMwclient.getConfig().isParam("USERLISTBOLD");
                TextColor = ulMwclient.getConfig().isParam("USERLISTCOLOR");
                TextImage = ulMwclient.getConfig().isParam("USERLISTIMAGE");
                LogoutImage = ulMwclient.getConfig().getImage("LOGOUT");
                ReserveImage = ulMwclient.getConfig().getImage("RESERVE");
                ActiveImage = ulMwclient.getConfig().getImage("ACTIVE");
                FightImage = ulMwclient.getConfig().getImage("FIGHT");
            }

            //value to display, cell index, is selected, cell has focus?
            public java.awt.Component getListCellRendererComponent(javax.swing.JList list, Object value, int index,
                  boolean selected, boolean focus) {
                //have to make this renderer faster
                int userlevel = 0;
                int status;

                client.CUser user = Owner.getUser(index);
                if (user == null) {return null;}

                userlevel = user.getUserlevel();
                String invisFlag = " ";

                //if you can see them, and they are invis, then your level is >= to theres
                if (user.isInvis()) {invisFlag = "(I) ";}

                if (userlevel < 30) {setText(user.getName());}
                if (userlevel >= 30 && userlevel < 100) {setText("^" + invisFlag + user.getName());}
                if (userlevel >= 100 && userlevel < 200) {setText("*" + invisFlag + user.getName());}
                if (userlevel >= 200) {setText("@" + invisFlag + user.getName());}

                //check users No-Play status
                boolean isOnNoPlay = false;
                if (ulMwclient.getPlayer().getAdminExcludes().contains(user.getName().toLowerCase())) {
                    isOnNoPlay = true;
                } else if (ulMwclient.getPlayer().getPlayerExcludes().contains(user.getName().toLowerCase())) {
                    isOnNoPlay = true;
                }

                //append mute/unmuted status. this is sickeningly inefficient when the whole
                //list is being processed and should be rewritten eventually.
                String searchString = user.getName().trim();
                int isMuted = 0;

                if (userlevel < 100) {
                    String ignoreList = ulMwclient.getConfig().getParam("IGNOREPUBLIC");
                    java.util.StringTokenizer it = new java.util.StringTokenizer(ignoreList, ",");
                    while (it.hasMoreTokens()) {
                        String currString = it.nextToken().trim();
                        if (currString.equalsIgnoreCase(searchString)) {isMuted++;}
                    }

                    //search PrivateMessageCommand mute as well
                    ignoreList = ulMwclient.getConfig().getParam("IGNOREPRIVATE");
                    it = new java.util.StringTokenizer(ignoreList, ",");
                    while (it.hasMoreTokens()) {
                        String currString = it.nextToken().trim();
                        if (currString.equalsIgnoreCase(searchString)) {isMuted++;}
                    }


                    //and the faction ...
                    if (user.getHouse().equals(ulMwclient.getPlayer().getHouse())) {
                        ignoreList = ulMwclient.getConfig().getParam("IGNOREHOUSE");
                        it = new java.util.StringTokenizer(ignoreList, ",");
                        while (it.hasMoreTokens()) {
                            String currString = it.nextToken();
                            if (currString.equalsIgnoreCase(searchString)) {isMuted++;}
                        }
                    }
                }
                String muteUps = "";
                for (int i = 1; i < isMuted; i++) {muteUps += "+";}

                if (isMuted > 0 && isOnNoPlay) {setText(getText() + " [muted" + muteUps + ", np]");} else if (isMuted >
                                                                                                                    0) {
                    setText(getText() + " [muted" + muteUps + "]");
                } else if (isOnNoPlay) {
                    setText(getText() + " [np]");
                }


                if (selected) {
                    setForeground(list.getSelectionForeground());
                    setBackground(list.getSelectionBackground());
                } else {
                    setBackground(list.getBackground());
                    if (TextColor && LoggedIn) {setForeground(user.getRGBColor());} else {
                        setForeground(java.awt.Color.black);
                    }
                }

                if (LoggedIn) {
                    status = user.getStatus();
                    if (status == client.MWClient.STATUS_LOGGEDOUT) {

                        //logged out users are never bold
                        setFont(getFont().deriveFont(java.awt.Font.PLAIN));
                        if (TextImage) {try {setIcon(LogoutImage);} catch (Exception ex) {MWLogger.errLog(ex);}}
                    } else {
                        if (TextBold) {setFont(getFont().deriveFont(java.awt.Font.BOLD));} else {
                            setFont(getFont().deriveFont(java.awt.Font.PLAIN));
                        }

                        if (TextImage) {
                            if (status == client.MWClient.STATUS_RESERVE) {
                                try {setIcon(ReserveImage);} catch (Exception ex) {MWLogger.errLog(ex);}
                            }
                            if (status == client.MWClient.STATUS_ACTIVE) {
                                try {setIcon(ActiveImage);} catch (Exception ex) {MWLogger.errLog(ex);}
                            }
                            if (status == client.MWClient.STATUS_FIGHTING) {
                                try {setIcon(FightImage);} catch (Exception ex) {MWLogger.errLog(ex);}
                            }
                        } else {
                            setIcon(null);
                        }
                    }
                    setIconTextGap(7);
                    setToolTipText(user.getInfo(ulMwclient.getConfig().isParam("NOIMGINCHAT")));
                } else {

                    //logged out users don't see bold names OR icons
                    setFont(getFont().deriveFont(java.awt.Font.PLAIN));
                    setIcon(null);

                    setToolTipText(user.getShortInfo());
                }
                return this;
            }
        }

        public class UserComparator implements java.util.Comparator<client.CUser> {

            int Mode;
            int Order;

            public UserComparator() {
                Mode = SORTMODE_NAME;
                Order = SORTORDER_ASCENDING;
            }

            public int compare(client.CUser o1, client.CUser o2) {
                client.CUser user1 = null;
                client.CUser user2 = null;
                int result = 0;

                if (Order == SORTORDER_DESCENDING) {
                    user1 = o2;
                    user2 = o1;
                } else {
                    user1 = o1;
                    user2 = o2;
                }

                if (Mode == SORTMODE_NAME) {return (user1.getName().compareToIgnoreCase(user2.getName()));}
                if (Mode == SORTMODE_HOUSE) {result = user1.getHouse().compareToIgnoreCase(user2.getHouse());}
                if (Mode == SORTMODE_COUNTRY) {result = user1.getCountry().compareToIgnoreCase(user2.getCountry());}
                // orders are switched for the following, meaning, bigger value is earlier on list
                if (Mode == SORTMODE_EXP) {result = Integer.valueOf(user2.getExp()).compareTo(user1.getExp());}
                if (Mode == SORTMODE_RATING) {result = Float.valueOf(user2.getRating()).compareTo(user1.getRating());}
                if (Mode == SORTMODE_STATUS) {result = Integer.valueOf(user2.getStatus()).compareTo(user1.getStatus());}
                if (Mode == SORTMODE_USERLEVEL) {
                    result = Integer.valueOf(user2.getUserlevel()).compareTo(user1.getUserlevel());
                }
                // if other modes gave equal result or no mode known, sort by name
                if (result == 0) {
                    if (Order == SORTORDER_DESCENDING) {return (user2.getName().compareToIgnoreCase(user1.getName()));}
                    //else
                    return (user1.getName().compareToIgnoreCase(user2.getName()));
                }
                //else
                return result;
            }

            public boolean equals(Object o1, Object o2) {
                return (((client.CUser) o1).getName().equals(((client.CUser) o2).getName()));
            }

            public int getMode() {return Mode;}

            public void setMode(int tmode) {
                if (tmode == SORTMODE_NAME || tmode == SORTMODE_HOUSE ||
                          tmode == SORTMODE_EXP || tmode == SORTMODE_RATING ||
                          tmode == SORTMODE_STATUS || tmode == SORTMODE_USERLEVEL ||
                          tmode == SORTMODE_COUNTRY) {Mode = tmode;}
            }

            public int getOrder() {return Order;}

            public void setOrder(int torder) {
                if (torder == SORTORDER_ASCENDING || torder == SORTORDER_DESCENDING) {Order = torder;}
            }
        }

    }

    class UserListPopupListener extends java.awt.event.MouseAdapter implements java.awt.event.ActionListener {

        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
            if (e.getClickCount() == 2) {

                int row = UserList.locationToIndex(e.getPoint());
                if (row > -1 && row < UserList.getModel().getSize()) {
                    //don't show mail/money/mute/noplay for player himself
                    client.CUser user = ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).getUser(
                          row);
                    String input = client.MWClient.GUI_PREFIX + "mail " + user.getName() + ", ";
                    input = input + mwclient.getMainFrame().getMainPanel().getCommPanel().getInput();
                    mwclient.getMainFrame().getMainPanel().getCommPanel().setInput(input);
                    mwclient.getMainFrame().getMainPanel().getCommPanel().focusInputField();
                }

            }
        }

        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {maybeShowPopup(e);}

        @Override
        public void mouseReleased(java.awt.event.MouseEvent e) {maybeShowPopup(e);}

        private void maybeShowPopup(java.awt.event.MouseEvent e) {
            javax.swing.JMenuItem item;
            javax.swing.JPopupMenu popup;
            int row = -1;
            String userName = "";

            popup = new javax.swing.JPopupMenu();
            if (e.isPopupTrigger()) {

                row = UserList.locationToIndex(e.getPoint());
                if (row > -1 && row < UserList.getModel().getSize()) {
                    //don't show mail/money/mute/noplay for player himself
                    client.CUser user = ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).getUser(
                          row);
                    userName = user.getName();

                    /*
                     * MOD MENU @Torren 4.7.05
                     *
                     * Most of the mod menu moved into a seperate
                     * admin package, since its a waste if bytes to
                     * have all players downloading things that only
                     * a handful have access to.
                     */
                    if (mwclient.isMod()) {
                        java.net.URLClassLoader loader = null;
                        try {
                            java.io.File loadJar = new java.io.File("./MekWarsAdmin.jar");
                            if (!loadJar.exists()) {
                                MWLogger.errLog("StaffUserlistPopupMenu creation skipped. No MekWarsAdmin.jar present.");
                            } else {
                                loader = new java.net.URLClassLoader(new java.net.URL[] { loadJar.toURI().toURL() });
                                Class<?> c = loader.loadClass("admin.StaffUserlistPopupMenu");
                                Object o = c.newInstance();
                                c.getDeclaredMethod("createMenu",
                                      new Class[] { client.MWClient.class, client.CUser.class }).invoke(o,
                                      new Object[] { mwclient, user });
                                popup.add((javax.swing.JMenu) o);
                            }
                        } catch (Exception ex) {
                            MWLogger.errLog("StaffUserlistPopupMenu creation FAILED!");
                            MWLogger.errLog(ex);
                        } finally {
                            try {
                                loader.close();
                            } catch (java.io.IOException e1) {
                                MWLogger.errLog(e1);
                            }
                        }

                        popup.addSeparator();
                    }


                    if (!userName.equalsIgnoreCase(mwclient.getPlayer().getName())) {
                        item = new javax.swing.JMenuItem("<HTML>Mail " + userName + "</b></HTML>");
                        item.setActionCommand("MA|" + userName);
                        item.addActionListener(this);
                        popup.add(item);

                        //popup.addSeparator();

                        if (LoggedIn && user.getStatus() != client.MWClient.STATUS_LOGGEDOUT) {

                            javax.swing.JMenu sendMen = new javax.swing.JMenu("Send");

                            item = new javax.swing.JMenuItem("Send " + mwclient.moneyOrFluMessage(true, false, -2));
                            item.setActionCommand("MO|" + userName);
                            item.addActionListener(this);
                            sendMen.add(item);

                            item = new javax.swing.JMenuItem("Send " + mwclient.getserverConfigs("RPLongName"));
                            item.setActionCommand("MR|" + userName);
                            item.addActionListener(this);
                            sendMen.add(item);

                            item = new javax.swing.JMenuItem("Send " +
                                                                   mwclient.getserverConfigs("FluLongName")); //@salient
                            item.setActionCommand("MI|" + userName);
                            item.addActionListener(this);
                            sendMen.add(item);

                            item = new javax.swing.JMenuItem("Send Unit");
                            item.setActionCommand("TU|" + userName);
                            item.addActionListener(this);
                            sendMen.add(item);

                            if (Boolean.parseBoolean(mwclient.getserverConfigs("AllowPersonalPilotQueues"))) {
                                item = new javax.swing.JMenuItem("Send Pilot");
                                item.setActionCommand("TP|" + userName);
                                item.addActionListener(this);
                                sendMen.add(item);
                            }

                            if (Boolean.parseBoolean(mwclient.getserverConfigs("UseDirectSell"))) {
                                item = new javax.swing.JMenuItem("Direct Sell Unit");
                                item.setActionCommand("DSU|" + userName);
                                item.addActionListener(this);
                                sendMen.add(item);
                            }

                            popup.add(sendMen);
                        }

                        javax.swing.JMenu blockMen = new javax.swing.JMenu("Block");

                        /*
                         * Mute/Unmute the player. Detect name string in the ignore
                         * list and then display as appropriate. for Main.
                         */

                        String searchString = userName;
                        boolean matched = false;

                        String ignoreList = mwclient.getConfig().getParam("IGNOREPUBLIC");
                        java.util.StringTokenizer st = new java.util.StringTokenizer(ignoreList, ",");
                        while (st.hasMoreTokens() && !matched) {
                            String currString = st.nextToken().trim();
                            if (currString.equalsIgnoreCase(searchString)) {matched = true;}
                        }

                        if (!matched) {
                            item = new javax.swing.JMenuItem("Mute (Main)");
                            item.setActionCommand("MU|" + userName + "|PUBLIC");
                            item.addActionListener(this);
                            blockMen.add(item);
                        } else {
                            item = new javax.swing.JMenuItem("Unmute (Main)");
                            item.setActionCommand("UMU|" + userName + "|PUBLIC");
                            item.addActionListener(this);
                            blockMen.add(item);
                        }

                        /*
                         * Mute/Unmute the player via PrivateMessageCommand
                         */
                        ignoreList = mwclient.getConfig().getParam("IGNOREPRIVATE");
                        st = new java.util.StringTokenizer(ignoreList, ",");
                        matched = false;
                        while (st.hasMoreTokens() && !matched) {
                            String currString = st.nextToken().trim();
                            if (currString.equalsIgnoreCase(searchString)) {matched = true;}
                        }

                        if (!matched) {
                            item = new javax.swing.JMenuItem("Mute (Private)");
                            item.setActionCommand("MU|" + userName + "|PRIVATE");
                            item.addActionListener(this);
                            blockMen.add(item);
                        } else {
                            item = new javax.swing.JMenuItem("Unmute (Private)");
                            item.setActionCommand("UMU|" + userName + "|PRIVATE");
                            item.addActionListener(this);
                            blockMen.add(item);
                        }

                        //if in the same faction, also show faction mute
                        if (user.getHouse().equals(mwclient.getPlayer().getHouse())) {

                            ignoreList = mwclient.getConfig().getParam("IGNOREHOUSE");
                            st = new java.util.StringTokenizer(ignoreList, ",");
                            matched = false;
                            while (st.hasMoreTokens() && !matched) {
                                String currString = st.nextToken().trim();
                                if (currString.equalsIgnoreCase(searchString)) {matched = true;}
                            }

                            if (!matched) {
                                item = new javax.swing.JMenuItem("Mute (House)");
                                item.setActionCommand("MU|" + userName + "|HOUSE");
                                item.addActionListener(this);
                                blockMen.add(item);
                            } else {
                                item = new javax.swing.JMenuItem("Unmute (House)");
                                item.setActionCommand("UMU|" + userName + "|HOUSE");
                                item.addActionListener(this);
                                blockMen.add(item);
                            }
                        }

                        /*
                         * Add or remove the player to/from no-play list.
                         *
                         * Only show this option if list size >= 1. If the list
                         * is disabled, its just a confusing extraneous option.
                         */
                        if (Integer.parseInt(mwclient.getserverConfigs("NoPlayListSize")) >= 1) {

                            if (LoggedIn && user.getStatus() != client.MWClient.STATUS_LOGGEDOUT) {
                                boolean isOnNoPlay = false;
                                if (mwclient.getPlayer().getAdminExcludes().contains(userName.toLowerCase())) {
                                    isOnNoPlay = true;
                                } else if (mwclient.getPlayer().getPlayerExcludes().contains(userName.toLowerCase())) {
                                    isOnNoPlay = true;
                                }

                                if (isOnNoPlay) {
                                    item = new javax.swing.JMenuItem("Remove from No-Play");
                                    item.setActionCommand("RNP|" + userName);
                                    item.addActionListener(this);
                                    blockMen.add(item);
                                } else {
                                    item = new javax.swing.JMenuItem("Add to No-Play");
                                    item.setActionCommand("ANP|" + userName);
                                    item.addActionListener(this);
                                    blockMen.add(item);
                                }
                            } else {//show blank
                                item = new javax.swing.JMenuItem("No-Play");
                                item.setEnabled(false);
                                blockMen.add(item);
                            }
                            popup.addSeparator();
                        }//end if(should draw no-play menu items)
                        popup.add(blockMen);


                    }//end if(clicked player isn't THE player)
                    //Toggle ascending/decending order
                    if (((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).getSortOrder() ==
                              SORTORDER_DESCENDING) {
                        item = new javax.swing.JMenuItem("Ascending Order");
                        item.setActionCommand("SO|A");
                        item.addActionListener(this);
                        popup.add(item);
                    } else {
                        item = new javax.swing.JMenuItem("Descending Order");
                        item.setActionCommand("SO|D");
                        item.addActionListener(this);
                        popup.add(item);
                    }

                    //Sort Sub-Menu
                    javax.swing.JMenu sortSub = new javax.swing.JMenu("Sort By");
                    popup.add(sortSub);

                    item = new javax.swing.JMenuItem("Name");
                    item.setActionCommand("SM|N");
                    item.addActionListener(this);
                    sortSub.add(item);
                    if (LoggedIn) {
                        item = new javax.swing.JMenuItem("Faction");
                        item.setActionCommand("SM|H");
                        item.addActionListener(this);
                        sortSub.add(item);
                        item = new javax.swing.JMenuItem("Experience");
                        item.setActionCommand("SM|E");
                        item.addActionListener(this);
                        sortSub.add(item);
                        if (!Boolean.parseBoolean(mwclient.getserverConfigs("HideELO"))) {
                            item = new javax.swing.JMenuItem("Rating");
                            item.setActionCommand("SM|R");
                            item.addActionListener(this);
                            sortSub.add(item);
                        }
                        item = new javax.swing.JMenuItem("Status");
                        item.setActionCommand("SM|S");
                        item.addActionListener(this);
                        sortSub.add(item);
                    }
                    item = new javax.swing.JMenuItem("Userlevel");
                    item.setActionCommand("SM|L");
                    item.addActionListener(this);
                    sortSub.add(item);
                    item = new javax.swing.JMenuItem("Country");
                    item.setActionCommand("SM|C");
                    item.addActionListener(this);
                    sortSub.add(item);

                    popup.addSeparator();

                    javax.swing.JMenu settingSub = new javax.swing.JMenu("List Settings");
                    popup.add(settingSub);

                    //activity button
                    item = new javax.swing.JCheckBoxMenuItem("Activity Button");
                    if (mwclient.getConfig().isParam("USERLISTACTIVITYBTN")) {item.setSelected(true);} else {
                        item.setSelected(false);
                    }
                    item.setActionCommand("ULA|" + !item.isSelected());
                    item.addActionListener(this);
                    settingSub.add(item);

                    //bold names
                    item = new javax.swing.JCheckBoxMenuItem("Bold Names");
                    if (mwclient.getConfig().isParam("USERLISTBOLD")) {item.setSelected(true);} else {
                        item.setSelected(false);
                    }
                    item.setActionCommand("ULB|" + !item.isSelected());
                    item.addActionListener(this);
                    settingSub.add(item);

                    //color
                    item = new javax.swing.JCheckBoxMenuItem("Colored Names");
                    if (mwclient.getConfig().isParam("USERLISTCOLOR")) {item.setSelected(true);} else {
                        item.setSelected(false);
                    }
                    item.setActionCommand("ULC|" + !item.isSelected());
                    item.addActionListener(this);
                    settingSub.add(item);

                    //deds
                    item = new javax.swing.JCheckBoxMenuItem("Dedicated Hosts");
                    if (Dedicateds) {item.setSelected(true);} else {item.setSelected(false);}
                    item.setActionCommand("TD");
                    item.addActionListener(this);
                    settingSub.add(item);

                    //player count
                    item = new javax.swing.JCheckBoxMenuItem("Player Count");
                    if (mwclient.getConfig().isParam("USERLISTCOUNT")) {item.setSelected(true);} else {
                        item.setSelected(false);
                    }
                    item.setActionCommand("ULN|" + !item.isSelected());
                    item.addActionListener(this);
                    settingSub.add(item);

                    //images
                    item = new javax.swing.JCheckBoxMenuItem("Status Images");
                    if (mwclient.getConfig().isParam("USERLISTIMAGE")) {item.setSelected(true);} else {
                        item.setSelected(false);
                    }
                    item.setActionCommand("ULI|" + !item.isSelected());
                    item.addActionListener(this);
                    settingSub.add(item);

                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }

        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            String s = actionEvent.getActionCommand();
            java.util.StringTokenizer st = new java.util.StringTokenizer(s, "|");
            String command = st.nextToken();
            String userName = "";

            //send mail
            if (command.equals("MA") && st.hasMoreElements()) {
                userName = st.nextToken();
                if (true) {mwclient.getMainFrame().jMenuFileMail_actionPerformed(userName);}
                return;
            }
            //send Money
            if (command.equals("MO") && st.hasMoreElements()) {
                userName = st.nextToken();
                if (true) {mwclient.getMainFrame().jMenuCommanderTransferMoney_actionPerformed(userName);}
                return;
            }

            if (command.equals("MR") && st.hasMoreElements()) {
                userName = st.nextToken();
                mwclient.getMainFrame().jMenuCommanderTransferRewardPoints_actionPerformed(userName);
                return;
            }

            //@Salient
            if (command.equals("MI") && st.hasMoreElements()) {
                userName = st.nextToken();
                mwclient.getMainFrame().jMenuCommanderTransferInfluence_actionPerformed(userName);
                return;
            }

            if (command.equals("TU") && st.hasMoreElements()) {
                userName = st.nextToken();
                if (true) {mwclient.getMainFrame().jMenuCommanderTransferUnit_actionPerformed(userName, -1);}
                return;
            }

            if (command.equals("TP") && st.hasMoreElements()) {
                userName = st.nextToken();
                if (true) {mwclient.getMainFrame().jMenuCommanderTransferPilot_actionPerformed(userName);}
                return;
            }

            if (command.equals("DSU") && st.hasMoreElements()) {
                userName = st.nextToken();
                if (true) {mwclient.getMainFrame().jMenuCommanderDirectSell_actionPerformed(userName, null);}
                return;
            }

            if (command.equals("MU") && st.hasMoreElements()) {
                userName = st.nextToken();
                String mode = st.nextToken();
                if (true) {

                    String searchString = userName;
                    String ignoreList = mwclient.getConfig().getParam("IGNORE" + mode);
                    String newList = "";
                    java.util.StringTokenizer it = new java.util.StringTokenizer(ignoreList, ",");
                    boolean matched = false;
                    while (it.hasMoreTokens() && !matched) {
                        //rebuild the list to make sure ,'s are ok.
                        String currString = it.nextToken();
                        newList += currString + ",";
                        if (currString.equals(searchString)) {matched = true;}
                    }
                    if (!matched) {newList += searchString + ",";}
                    mwclient.getConfig().setParam("IGNORE" + mode, newList);
                    mwclient.setIgnorePublic();
                    mwclient.setIgnoreHouse();
                    mwclient.setIgnorePrivate();
                    mwclient.getConfig().saveConfig();
                    String toUser = "CH|CLIENT: You muted " + searchString + " (" + mode + ").";
                    mwclient.doParseDataInput(toUser);
                    UserList.repaint();
                }
            }//end mute

            if (command.equals("UMU") && st.hasMoreElements()) {
                userName = st.nextToken();
                String mode = st.nextToken();
                if (true) {

                    String searchString = userName;
                    String ignoreList = mwclient.getConfig().getParam("IGNORE" + mode);
                    String newList = "";
                    java.util.StringTokenizer it = new java.util.StringTokenizer(ignoreList, ",");
                    while (it.hasMoreTokens()) {
                        String currString = it.nextToken();
                        if (!currString.equals(searchString)) {newList += currString + ",";}
                        //else do nothing ...

                    }//end while(more ignore tokens)
                    mwclient.getConfig().setParam("IGNORE" + mode, newList);
                    mwclient.setIgnorePublic();
                    mwclient.setIgnoreHouse();
                    mwclient.setIgnorePrivate();
                    mwclient.getConfig().saveConfig();
                    String toUser = "CH|CLIENT: You unmuted " + searchString + " (" + mode + ").";
                    mwclient.doParseDataInput(toUser);
                    UserList.repaint();
                }
            }//end unmute

            if (command.equals("RNP") && st.hasMoreElements()) {

                userName = st.nextToken();
                if (true) {

                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "c noplay#remove#" + userName);
                }
            }

            if (command.equals("ANP") && st.hasMoreElements()) {
                userName = st.nextToken();
                if (true) {

                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "c noplay#add#" + userName);
                }
            }

            //change sort mode
            if (command.equals("SM") && st.hasMoreElements()) {
                command = st.nextToken();
                if (command.equals("N")) {
                    ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(SORTMODE_NAME);
                } else if (command.equals("H")) {
                    ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(SORTMODE_HOUSE);
                } else if (command.equals("E")) {
                    ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(SORTMODE_EXP);
                } else if (command.equals("R")) {
                    if (!Boolean.parseBoolean(mwclient.getserverConfigs("HideELO"))) {
                        ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(
                              SORTMODE_RATING);
                    } else {
                        ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(
                              SORTMODE_NAME);
                    }
                } else if (command.equals("S")) {
                    ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(SORTMODE_STATUS);
                } else if (command.equals("L")) {
                    ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(
                          SORTMODE_USERLEVEL);
                } else if (command.equals("C")) {
                    ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortMode(
                          SORTMODE_COUNTRY);
                }

                //saveblock
                if (command.equals("H")) {mwclient.getConfig().setParam("SORTMODE", "HOUSE");} else if (command.equals(
                      "E")) {mwclient.getConfig().setParam("SORTMODE", "EXP");} else if (command.equals("R")) {
                    mwclient.getConfig().setParam("SORTMODE", "RATING");
                } else if (command.equals("S")) {
                    mwclient.getConfig().setParam("SORTMODE", "STATUS");
                } else if (command.equals("L")) {
                    mwclient.getConfig().setParam("SORTMODE", "USERLEVEL");
                } else if (command.equals("C")) {mwclient.getConfig().setParam("SORTMODE", "COUNTRY");} else {
                    mwclient.getConfig().setParam("SORTMODE", "NAME");
                }
                mwclient.getConfig().saveConfig();

                return;
            }
            //change sort order
            if (command.equals("SO") && st.hasMoreElements()) {
                command = st.nextToken();
                if (command.equals("A")) {
                    ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortOrder(
                          SORTORDER_ASCENDING);
                }
                if (command.equals("D")) {
                    ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setSortOrder(
                          SORTORDER_DESCENDING);
                }

                //saveblock
                if (command.equals("D")) {mwclient.getConfig().setParam("SORTORDER", "DESCENDING");} else {
                    mwclient.getConfig().setParam("SORTORDER", "ASCENDING");
                }
                mwclient.getConfig().saveConfig();

                return;
            }

            //settingss
            if (command.equals("TD")) {
                Dedicateds = !Dedicateds;
                if (Dedicateds) {mwclient.getConfig().setParam("USERLISTDEDICATEDS", "YES");} else {
                    mwclient.getConfig().setParam("USERLISTDEDICATEDS", "NO");
                }
                ((mekwars.client.gui.CUserListPanel.CUserListModel) UserList.getModel()).setDedicateds(Dedicateds);
                refresh();
                mwclient.getConfig().saveConfig();
            } else if (command.equals("ULC") && st.hasMoreElements()) {
                command = st.nextToken();
                mwclient.getConfig().setParam("USERLISTCOLOR", command);
                Users.getRenderer().refreshParams();
                UserList.repaint();
                mwclient.getConfig().saveConfig();
            } else if (command.equals("ULI") && st.hasMoreElements()) {
                command = st.nextToken();
                mwclient.getConfig().setParam("USERLISTIMAGE", command);
                Users.getRenderer().refreshParams();
                UserList.repaint();
                mwclient.getConfig().saveConfig();
            } else if (command.equals("ULB") && st.hasMoreElements()) {
                command = st.nextToken();
                mwclient.getConfig().setParam("USERLISTBOLD", command);
                Users.getRenderer().refreshParams();
                UserList.repaint();
                mwclient.getConfig().saveConfig();
            } else if (command.equals("ULN") && st.hasMoreElements()) {
                command = st.nextToken();
                mwclient.getConfig().setParam("USERLISTCOUNT", command);
                CountLabel.setVisible(Boolean.parseBoolean(command));
                repaint();
                mwclient.getConfig().saveConfig();
            } else if (command.equals("ULA") && st.hasMoreElements()) {
                command = st.nextToken();
                mwclient.getConfig().setParam("USERLISTACTIVITYBTN", command);
                ActivityButton.setVisible(Boolean.parseBoolean(command));
                repaint();
                mwclient.getConfig().saveConfig();
            }

            /*
             * Mod commands used to be here. Moved into
             * admin.ModeratorPopupMenu, 6/26/05, @urgru
             */
        }
    }
}

