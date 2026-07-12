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

/**
 * A {@link ListCellRenderer} used to draw each row of the online/user list (see {@code CUserListPanel}/
 * {@code CUserListModel}). For each visible {@link CUser}, this renderer builds up a label that reflects: user
 * level (via a leading marker character and, for higher levels, a different marker), invisibility (an "(I)"
 * marker, shown to viewers with sufficient privilege to see invisible users), mute state relative to the viewing
 * player (public/private/faction ignore lists), no-play/admin-exclude status, bold/color/logged-in status, and a
 * status icon (logged out/reserve/active/fighting) drawn from images cached in the client config. This renderer is
 * intentionally reused across all list cells (the standard {@link ListCellRenderer} pattern) rather than one
 * instance per row.
 */
public class UserListCellRenderer extends javax.swing.JLabel implements ListCellRenderer<CUserListModel> {
    private static final MMLogger LOGGER = MMLogger.create(UserListCellRenderer.class);

    /**
     * Serialization version identifier for this {@link javax.swing.JLabel}.
     */
    @Serial
    private static final long serialVersionUID = 4400213401819469963L;
    /** The client used to read config/display preferences and to resolve the viewing player's own data. */
    private final IClient client;
    /** The list model this renderer belongs to; used to look up the {@link CUser} for a given row index. */
    private final CUserListModel Owner;
    /** Whether the user list currently being rendered represents logged-in users (vs. a logged-out/offline list). */
    private boolean LoggedIn = false;
    /** Cached copy of the "USER_LIST_BOLD" config preference (bold names for active/reserve users). */
    private boolean TextBold;
    /** Cached copy of the "USER_LIST_COLOR" config preference (color names by the user's chosen color). */
    private boolean TextColor;
    /** Cached copy of the "USER_LIST_IMAGE" config preference (show status icons). */
    private boolean TextImage;
    /** Cached status icon shown for logged-out users. */
    private ImageIcon LogoutImage;
    /** Cached status icon shown for users in reserve status. */
    private ImageIcon ReserveImage;
    /** Cached status icon shown for active users. */
    private ImageIcon ActiveImage;
    /** Cached status icon shown for users currently in a game/battle. */
    private ImageIcon FightImage;

    /**
     * Creates a renderer bound to the given list model, caching the current display preferences and status icons
     * from the client config at construction time. Note these cached values are not automatically kept in sync
     * with later config changes; see {@link #refreshParams()}.
     *
     * @param towner the list model this renderer will render cells for
     */
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

    /**
     * Sets whether this renderer should treat rows as belonging to the logged-in user list (affects bolding,
     * status icons, and tooltip verbosity in {@link #getListCellRendererComponent}).
     */
    public void setLoggedIn(boolean loggedIn) {
        LoggedIn = loggedIn;
    }

    /**
     * Re-reads the display preference flags and status icons from the client config. Must be called explicitly
     * after a relevant config change (e.g. the user toggles "bold names" in options) since the constructor only
     * caches these values once.
     */
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
     * <p>
     * MekWars-specific behavior: looks up the {@link CUser} for {@code index} via the owning {@link CUserListModel}
     * (ignoring the {@code value} parameter), then builds up this label's text/color/font/icon/tooltip based on:
     * <ul>
     *   <li>User level tiers, each with a distinct leading marker: {@code < 30} no marker, {@code [30,100)} "^",
     *   {@code [100,200)} "*", {@code >= 200} "@" - followed by an "(I) " marker if the user is invisible (and thus
     *   only visible to viewers with sufficient privilege to begin with).</li>
     *   <li>Mute status relative to the viewing player: checks the public, private, and (if same house) faction
     *   ignore lists and appends "[muted+++]"-style suffixes (one "+" per extra list matched beyond the first) -
     *   this lookup re-tokenizes the ignore-list config strings on every single row render, which is noted in the
     *   source as "sickeningly inefficient" and a candidate for rewrite. Also note the ignore-list config keys read
     *   here ({@code IGNORE_PUBLIC}, {@code IGNORE_PRIVATE}, {@code IGNORE_HOUSE}) use underscores, while
     *   {@link GUIClientConfig}'s defaults define them without underscores ({@code IGNOREPUBLIC}, etc.) - this
     *   mismatch means the underscored lookups here likely never find the persisted values.</li>
     *   <li>No-play/admin-exclude status: appends "[np]" if the viewing player has this user on their admin- or
     *   player-exclude list.</li>
     *   <li>Logged-in vs. logged-out rendering: logged-out users are always shown in plain (non-bold) font with no
     *   status icon; logged-in users are bold/plain per the {@code TextBold} preference and get a status icon
     *   (logout/reserve/active/fighting) per the {@code TextImage} preference.</li>
     * </ul>
     * Note: if the model has no user at {@code index} (e.g. a stale/out-of-range index), this method returns
     * {@code null} rather than a component - which is unusual for a {@link ListCellRenderer} (callers such as
     * {@link JList}'s UI delegate generally expect a non-null component) and could cause a
     * {@link NullPointerException} downstream if triggered.
     *
     * @param list         The JList we're painting.
     * @param value        The value returned by list.getModel().getElementAt(index).
     * @param index        The cells index.
     * @param isSelected   True if the specified cell was selected.
     * @param cellHasFocus True if the specified cell has the focus.
     *
     * @return A component whose paint() method will render the specified value; {@code null} if there is no
     *       corresponding {@link CUser} at {@code index}.
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
            setText(String.format("^%s%s", invisFlag, user.getName()));
        }

        if (userLevel >= 100 && userLevel < 200) {
            setText(String.format("*%s%s", invisFlag, user.getName()));
        }

        if (userLevel >= 200) {
            setText(String.format("@%s%s", invisFlag, user.getName()));
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
            setText(String.format("%s [muted%s, np]", getText(), muteUps));
        } else if (isMuted > 0) {
            setText(String.format("%s [muted%s]", getText(), muteUps));
        } else if (isOnNoPlay) {
            setText(String.format("%s [np]", getText()));
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
