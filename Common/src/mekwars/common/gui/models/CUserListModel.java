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

package mekwars.common.gui.models;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.AbstractListModel;

import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.comparators.UserComparator;
import mekwars.common.gui.UserListCellRenderer;

/**
 * List model backing the connected-users JList (the online-player roster panel). Wraps a
 * {@link TreeSet} of {@link CUser} ordered by a configurable {@link UserComparator} (sort mode +
 * ascending/descending order), and supports hiding "invisible" higher-privilege users from
 * lower-level viewers as well as optionally hiding server "[Dedicated]" bot/host accounts.
 * <p>
 * Unlike a typical incremental list model, {@link #refreshModel()} always tears down and rebuilds
 * the entire visible set from the client's current user collection rather than diffing
 * additions/removals, so the backing JList is fully refreshed (and may lose scroll position) on
 * every call.
 */
public class CUserListModel extends AbstractListModel<CUser> {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 9141928592065940657L;
    /** Backing set of currently-visible users, kept ordered by the active {@link UserComparator}
     *  and made thread-safe via {@link Collections#synchronizedSortedSet}. */
    private final SortedSet<CUser> Users;  //users' set
    /** Cell renderer used to draw each row of the JList (icons/colors/text per user state). */
    private final UserListCellRenderer Renderer;  //list cells renderer
    /** Client owning this model; supplies the full user roster and the viewer's own access level. */
    private final IClient client;  //client owning this model
    /** Whether "[Dedicated]"-prefixed dedicated-host/bot accounts should be included in the list. */
    private boolean Dedicated; //dedicated hosts visible

    /**
     * @param client the client whose user roster this model mirrors; also used to read the
     *               "USER_LIST_DEDICATEDS" config flag for the initial {@link #Dedicated} state
     */
    public CUserListModel(IClient client) {
        this.client = client;
        Dedicated = this.client.getConfig().isParam("USER_LIST_DEDICATEDS");
        Users = Collections.synchronizedSortedSet(new TreeSet<>(new UserComparator()));
        Renderer = new UserListCellRenderer(this);
    }

    public IClient getClient() {
        return client;
    }

    /** Removes a single user from the visible set (no list-changed event is fired here). */
    public synchronized void remove(CUser user) {
        Users.remove(user);
    }

    /** Adds a batch of users to the visible set (no list-changed event is fired here). */
    public synchronized void addAll(Collection<CUser> users) {
        Users.addAll(users);
    }

    public synchronized int getSize() {
        return Users.size();
    }

    /**
     * Returns the user at the given position in sorted order. Note each call copies the whole
     * {@link #Users} set into a fresh {@link ArrayList} to obtain positional (index-based) access,
     * since {@link SortedSet} itself has no {@code get(index)} — this is O(n) per call rather than
     * O(1).
     *
     * @param index position in sorted order
     * @return the user at that position, or {@code null} if {@code index} is out of range
     */
    public synchronized CUser getElementAt(int index) {
        if (index < Users.size()) {
            return new ArrayList<>(Users).get(index);
        }

        return null;
    }

    /** Sets whether dedicated-host accounts should appear; takes effect on the next
     *  {@link #refreshModel()}. */
    public void setDedicated(boolean dedicated) {
        Dedicated = dedicated;
    }

    /** @return the current sort mode from the active {@link UserComparator}, or {@code 0} if the
     *  set's comparator isn't a {@link UserComparator}. */
    public int getSortMode() {
        Comparator<?> comparator = Users.comparator();

        if (!(comparator instanceof UserComparator userComparator)) {
            return 0;
        }

        return userComparator.getMode();
    }

    /**
     * Changes the sort mode on the active {@link UserComparator} (e.g. sort by name vs. rank) and
     * immediately rebuilds the visible list via {@link #refreshModel()}. No-op if the set's
     * comparator isn't a {@link UserComparator}.
     */
    public void setSortMode(int sortMode) {
        Comparator<?> comparator = Users.comparator();

        if (!(comparator instanceof UserComparator userComparator)) {
            return;
        }

        userComparator.setMode(sortMode);
        refreshModel();
    }

    /**
     * Rebuilds the entire visible user set from {@code client.getUsers()} and fires the
     * corresponding {@code ListDataEvent}s. A user is excluded when either:
     * <ul>
     *   <li>they are marked invisible and the viewing client's user level is lower than theirs, or</li>
     *   <li>their name starts with {@code "[Dedicated]"} and {@link #Dedicated} is {@code false}</li>
     * </ul>
     * {@code fireIntervalRemoved} is called (with the pre-clear size) before {@link #clear()}, and
     * {@code fireIntervalAdded} after repopulating, so listeners see a full remove-then-add cycle
     * rather than incremental diffs.
     */
    public synchronized void refreshModel() {

        fireIntervalRemoved(this, 0, Users.size());
        clear();
        int myLevel = client.getUserLevel();

        /*
         * Sync on client.getcUserListModel() to prevent ConcurrentModError
         * while rebuilding the CUserListPanel.
         */
        Collection<CUser> users = client.getUsers();
        synchronized (users) {
            for (CUser currU : users) {
                if (currU.isInvisible() && myLevel < currU.getUserLevel()) {
                    continue;
                }

                if (currU.getName().startsWith("[Dedicated]") && !Dedicated) {
                    continue;
                }

                add(currU);
            }
        }

        fireIntervalAdded(this, 0, Users.size());
    }

    public synchronized void clear() {
        Users.clear();
    }

    public void add(CUser user) {
        Users.add(user);
    }

    /** @return the current ascending/descending sort order from the active {@link UserComparator},
     *  or {@code 0} if the set's comparator isn't a {@link UserComparator}. */
    public int getSortOrder() {
        Comparator<?> comparator = Users.comparator();

        if (!(comparator instanceof UserComparator userComparator)) {
            return 0;
        }

        return userComparator.getOrder();
    }

    /**
     * Changes the sort order (ascending/descending) on the active {@link UserComparator} and
     * immediately rebuilds the visible list via {@link #refreshModel()}. No-op if the set's
     * comparator isn't a {@link UserComparator}.
     */
    public void setSortOrder(int sortOrder) {
        Comparator<?> comparator = Users.comparator();

        if (!(comparator instanceof UserComparator userComparator)) {
            return;
        }

        userComparator.setOrder(sortOrder);
        refreshModel();
    }

    /**
     * Returns the user at the given position in sorted order via {@code Users.toArray()}. Like
     * {@link #getElementAt(int)}, this copies the set on every call.
     *
     * @return the user at that position, or {@code null} if {@code index} is out of range
     */
    public synchronized CUser getUser(int index) {
        if (index < Users.size()) {
            return ((CUser) Users.toArray()[index]);
        }

        return null;
    }

    /**
     * Linear search for a user by exact name match.
     * <p>
     * Note the not-found behavior differs from {@link #getUser(int)}: this overload returns a new,
     * blank {@code CUser()} sentinel instead of {@code null} when no match is found, so callers
     * cannot rely on a single "not found" convention across both overloads.
     *
     * @param name exact user name to search for
     * @return the matching user, or a new empty {@link CUser} if none match
     */
    public synchronized CUser getUser(String name) {
        for (CUser user : Users) {
            if (user.getName().equals(name)) {
                return user;
            }
        }

        return new CUser();
    }

    /** @return the shared {@link UserListCellRenderer} for this model. */
    public UserListCellRenderer getRenderer() {
        return Renderer;
    }
}
