/*
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

import java.awt.Component;
import java.awt.Font;
import java.io.Serial;
import java.util.StringTokenizer;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import megamek.logging.MMLogger;
import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.models.CUserListModel;

public class UserListCellRenderer extends javax.swing.JLabel implements ListCellRenderer<CUserListModel> {
    private static final MMLogger LOGGER = MMLogger.create(UserListCellRenderer.class);

    @Serial
    private static final long serialVersionUID = 4400213401819469963L;
    private final IClient client;
    private final CUserListModel Owner;
    private boolean LoggedIn = false;
    private boolean TextBold;
    private boolean TextColor;
    private boolean TextImage;
    private ImageIcon LogoutImage;
    private ImageIcon ReserveImage;
    private ImageIcon ActiveImage;
    private ImageIcon FightImage;

    public UserListCellRenderer(CUserListModel towner) {
        Owner = towner;
        client = towner.getClient();
        TextBold = client.getConfig().isParam("USER_LIST_BOLD");
        TextColor = client.getConfig().isParam("USER_LIST_COLOR");
        TextImage = client.getConfig().isParam("USER_LIST_IMAGE");
        LogoutImage = client.getConfig().getImage("LOGOUT");
        ReserveImage = client.getConfig().getImage("RESERVE");
        ActiveImage = client.getConfig().getImage("ACTIVE");
        FightImage = client.getConfig().getImage("FIGHT");
        setOpaque(true);
    }

    public void setLoggedIn(boolean loggedIn) {
        LoggedIn = loggedIn;
    }

    public void refreshParams() {
        TextBold = client.getConfig().isParam("USER_LIST_BOLD");
        TextColor = client.getConfig().isParam("USER_LIST_COLOR");
        TextImage = client.getConfig().isParam("USER_LIST_IMAGE");
        LogoutImage = client.getConfig().getImage("LOGOUT");
        ReserveImage = client.getConfig().getImage("RESERVE");
        ActiveImage = client.getConfig().getImage("ACTIVE");
        FightImage = client.getConfig().getImage("FIGHT");
    }

    /**
     * Return a component that has been configured to display the specified value. That component's <code>paint</code>
     * method is then called to "render" the cell.  If it is necessary to compute the dimensions of a list because the
     * list cells do not have a fixed size, this method is called to generate a component on which
     * <code>getPreferredSize</code> can be invoked.
     *
     * @param list         The JList we're painting.
     * @param value        The value returned by list.getModel().getElementAt(index).
     * @param index        The cells index.
     * @param isSelected   True if the specified cell was selected.
     * @param cellHasFocus True if the specified cell has the focus.
     *
     * @return A component whose paint() method will render the specified value.
     *
     * @see JList
     * @see ListSelectionModel
     * @see ListModel
     */
    @Override
    public Component getListCellRendererComponent(JList<? extends CUserListModel> list, CUserListModel value, int index,
          boolean isSelected, boolean cellHasFocus) {
        //have to make this renderer faster
        int userLevel;
        int status;

        CUser user = Owner.getUser(index);

        if (user == null) {
            return null;
        }

        userLevel = user.getUserLevel();
        String invisFlag = " ";

        //if you can see them, and they are invis, then your level is >= to theres
        if (user.isInvisible()) {
            invisFlag = "(I) ";
        }

        if (userLevel < 30) {
            setText(user.getName());
        }

        if (userLevel >= 30 && userLevel < 100) {
            setText(STR."^\{invisFlag}\{user.getName()}");
        }

        if (userLevel >= 100 && userLevel < 200) {
            setText(STR."*\{invisFlag}\{user.getName()}");
        }

        if (userLevel >= 200) {
            setText(STR."@\{invisFlag}\{user.getName()}");
        }

        //check users No-Play status
        boolean isOnNoPlay = false;

        if (client.getPlayer().getAdminExcludes().contains(user.getName().toLowerCase())) {
            isOnNoPlay = true;
        } else if (client.getPlayer().getPlayerExcludes().contains(user.getName().toLowerCase())) {
            isOnNoPlay = true;
        }

        //append mute/unmuted status. this is sickeningly inefficient when the whole
        //list is being processed and should be rewritten eventually.
        String searchString = user.getName().trim();
        int isMuted = 0;

        if (userLevel < 100) {
            String ignoreList = client.getConfig().getParam("IGNORE_PUBLIC");
            StringTokenizer it = new StringTokenizer(ignoreList, ",");

            while (it.hasMoreTokens()) {
                String currString = it.nextToken().trim();
                if (currString.equalsIgnoreCase(searchString)) {
                    isMuted++;
                }
            }

            //search PrivateMessageCommand mute as well
            ignoreList = client.getConfig().getParam("IGNORE_PRIVATE");
            it = new StringTokenizer(ignoreList, ",");
            while (it.hasMoreTokens()) {
                String currString = it.nextToken().trim();
                if (currString.equalsIgnoreCase(searchString)) {
                    isMuted++;
                }
            }


            //and the faction ...
            if (user.getHouse().equals(client.getPlayer().getHouse())) {
                ignoreList = client.getConfig().getParam("IGNORE_HOUSE");
                it = new StringTokenizer(ignoreList, ",");
                while (it.hasMoreTokens()) {
                    String currString = it.nextToken();
                    if (currString.equalsIgnoreCase(searchString)) {
                        isMuted++;
                    }
                }
            }
        }
        StringBuilder muteUps = new StringBuilder();
        muteUps.repeat("+", Math.max(0, isMuted - 1));

        if (isMuted > 0 && isOnNoPlay) {
            setText(STR."\{getText()} [muted\{muteUps}, np]");
        } else if (isMuted > 0) {
            setText(STR."\{getText()} [muted\{muteUps}]");
        } else if (isOnNoPlay) {
            setText(STR."\{getText()} [np]");
        }


        if (isSelected) {
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
            if (status == IClient.STATUS_LOGGED_OUT) {

                //logged-out users are never bold
                setFont(getFont().deriveFont(java.awt.Font.PLAIN));
                if (TextImage) {
                    try {
                        setIcon(LogoutImage);
                    } catch (Exception ex) {
                        LOGGER.error(ex, "Unable to set Logged Out icon: {}", ex.getLocalizedMessage());
                    }
                }
            } else {
                if (TextBold) {
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setFont(getFont().deriveFont(Font.PLAIN));
                }

                if (TextImage) {
                    if (status == IClient.STATUS_RESERVE) {
                        try {
                            setIcon(ReserveImage);
                        } catch (Exception ex) {
                            LOGGER.error(ex, "Unable to set Reserve Icon: {}", ex.getLocalizedMessage());
                        }
                    }
                    if (status == IClient.STATUS_ACTIVE) {
                        try {
                            setIcon(ActiveImage);
                        } catch (Exception ex) {
                            LOGGER.error(ex, "Unable to set Active Icon: {}", ex.getLocalizedMessage());
                        }
                    }
                    if (status == IClient.STATUS_FIGHTING) {
                        try {
                            setIcon(FightImage);
                        } catch (Exception ex) {
                            LOGGER.error(ex, "Unable to set Fighting Icon: {}", ex.getLocalizedMessage());
                        }
                    }
                } else {
                    setIcon(null);
                }
            }
            setIconTextGap(7);
            setToolTipText(user.getInfo(client.getConfig().isParam("NO_IMG_IN_CHAT")));
        } else {

            //logged-out users don't see bold names OR icons
            setFont(getFont().deriveFont(java.awt.Font.PLAIN));
            setIcon(null);

            setToolTipText(user.getShortInfo());
        }
        return this;
    }
}
