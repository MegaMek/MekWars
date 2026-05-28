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
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.AbstractListModel;

import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.comparators.UserComparator;
import mekwars.common.gui.UserListCellRenderer;

public class CUserListModel extends AbstractListModel<CUser> {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 9141928592065940657L;
    private final SortedSet<CUser> Users;  //users' set
    private final UserListCellRenderer Renderer;  //list cells renderer
    private final IClient client;  //client owning this model
    private boolean Dedicated; //dedicated hosts visible

    public CUserListModel(IClient client) {
        this.client = client;
        Dedicated = this.client.getConfig().isParam("USER_LIST_DEDICATEDS");
        Users = Collections.synchronizedSortedSet(new TreeSet<>(new UserComparator()));
        Renderer = new UserListCellRenderer(this);
    }

    public IClient getClient() {
        return client;
    }

    public synchronized void remove(CUser user) {
        Users.remove(user);
    }

    public synchronized void addAll(Collection<CUser> users) {
        Users.addAll(users);
    }

    public synchronized int getSize() {
        return Users.size();
    }

    public synchronized CUser getElementAt(int index) {
        if (index < Users.size()) {
            return new ArrayList<>(Users).get(index);
        }

        return null;
    }

    public void setDedicated(boolean dedicated) {
        Dedicated = dedicated;
    }

    public int getSortMode() {
        return ((UserComparator) Users.comparator()).getMode();
    }

    public void setSortMode(int sortMode) {
        ((UserComparator) Users.comparator()).setMode(sortMode);
        refreshModel();
    }

    public synchronized void refreshModel() {

        fireIntervalRemoved(this, 0, Users.size());
        clear();
        int myLevel = client.getUserLevel();

        /*
         * Sync on client.getUsers() to prevent ConcurrentModError
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

    public int getSortOrder() {
        return ((UserComparator) Users.comparator()).getOrder();
    }

    public void setSortOrder(int sortOrder) {
        ((UserComparator) Users.comparator()).setOrder(sortOrder);
        refreshModel();
    }

    public synchronized CUser getUser(int index) {
        if (index < Users.size()) {
            return ((CUser) Users.toArray()[index]);
        }

        return null;
    }

    public synchronized CUser getUser(String name) {
        for (CUser user : Users) {
            if (user.getName().equals(name)) {
                return user;
            }
        }

        return new CUser();
    }

    public UserListCellRenderer getRenderer() {
        return Renderer;
    }
}
