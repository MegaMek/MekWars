package mekwars.common.gui.models;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.AbstractListModel;

import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.comparators.UserComparator;
import mekwars.common.gui.UserListCellRenderer;

public class CUserListModel extends AbstractListModel {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 9141928592065940657L;
    SortedSet<CUser> Users;  //users set
    UserListCellRenderer Renderer;  //list cells renderer
    IClient client;  //client owning this model
    boolean Dedicated; //dedicated hosts visible


    public CUserListModel(IClient client) {
        this.client = client;
        Dedicated = this.client.getConfig().isParam("USERLISTDEDICATEDS");
        Users = Collections.synchronizedSortedSet(new TreeSet<>(new UserComparator()));
        Renderer = new UserListCellRenderer(this);
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

    public synchronized Object getElementAt(int index) {
        if (index < Users.size()) {
            return (((CUser) Users.toArray()[index]).getName());
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
