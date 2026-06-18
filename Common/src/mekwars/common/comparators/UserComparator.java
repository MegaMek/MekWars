package mekwars.common.comparators;

import java.util.Comparator;

import mekwars.common.campaign.CUser;
import mekwars.common.gui.panels.CUserListPanel;

public class UserComparator implements Comparator<CUser> {

    int Mode;
    int Order;

    public UserComparator() {
        Mode = CUserListPanel.SORT_MODE_NAME;
        Order = CUserListPanel.SORT_ORDER_ASCENDING;
    }

    public int compare(CUser o1, CUser o2) {
        CUser user1 = null;
        CUser user2 = null;
        int result = 0;

        if (Order == CUserListPanel.SORT_ORDER_DESCENDING) {
            user1 = o2;
            user2 = o1;
        } else {
            user1 = o1;
            user2 = o2;
        }

        if (Mode == CUserListPanel.SORT_MODE_NAME) {
            return (user1.getName().compareToIgnoreCase(user2.getName()));
        }

        if (Mode == CUserListPanel.SORT_MODE_HOUSE) {
            result = user1.getHouse().compareToIgnoreCase(user2.getHouse());
        }

        if (Mode == CUserListPanel.SORT_MODE_COUNTRY) {
            result = user1.getCountry().compareToIgnoreCase(user2.getCountry());
        }
        // orders are switched for the following, meaning, bigger value is earlier on list
        if (Mode == CUserListPanel.SORT_MODE_EXP) {
            result = Integer.compare(user2.getExp(), user1.getExp());
        }
        if (Mode == CUserListPanel.SORT_MODE_RATING) {
            result = Float.compare(user2.getRating(), user1.getRating());
        }
        if (Mode == CUserListPanel.SORT_MODE_STATUS) {
            result = Integer.compare(user2.getStatus(), user1.getStatus());
        }
        if (Mode == CUserListPanel.SORT_MODE_USER_LEVEL) {
            result = Integer.compare(user2.getUserLevel(), user1.getUserLevel());
        }
        // if other modes gave equal result or no mode known, sort by name
        if (result == 0) {
            if (Order == CUserListPanel.SORT_ORDER_DESCENDING) {
                return (user2.getName().compareToIgnoreCase(user1.getName()));
            }
            //else
            return (user1.getName().compareToIgnoreCase(user2.getName()));
        }
        //else
        return result;
    }

    public boolean equals(Object o1, Object o2) {
        return (((CUser) o1).getName().equals(((CUser) o2).getName()));
    }

    public int getMode() {return Mode;}

    public void setMode(int mode) {
        if (mode == CUserListPanel.SORT_MODE_NAME || mode == CUserListPanel.SORT_MODE_HOUSE ||
                  mode == CUserListPanel.SORT_MODE_EXP || mode == CUserListPanel.SORT_MODE_RATING ||
                  mode == CUserListPanel.SORT_MODE_STATUS || mode == CUserListPanel.SORT_MODE_USER_LEVEL ||
                  mode == CUserListPanel.SORT_MODE_COUNTRY) {Mode = mode;}
    }

    public int getOrder() {
        return Order;
    }

    public void setOrder(int order) {
        if (order == CUserListPanel.SORT_ORDER_ASCENDING || order == CUserListPanel.SORT_ORDER_DESCENDING) {
            Order = order;
        }
    }
}
