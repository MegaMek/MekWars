package mekwars.common.comparators;

import mekwars.common.campaign.CUser;
import mekwars.common.gui.panels.CUserListPanel;

public class UserComparator implements java.util.Comparator<CUser> {

    int Mode;
    int Order;

    public UserComparator() {
        Mode = CUserListPanel.SORT_MODE_NAME;
        Order = CUserListPanel.SORT_ORDER_ASCENDING;
    }

    public int compare(CUser o1, CUser o2) {
        client.CUser user1 = null;
        client.CUser user2 = null;
        int result = 0;

        if (Order == CUserListPanel.SORT_ORDER_DESCENDING) {
            user1 = o2;
            user2 = o1;
        } else {
            user1 = o1;
            user2 = o2;
        }

        if (Mode == CUserListPanel.SORT_MODE_NAME) {return (user1.getName().compareToIgnoreCase(user2.getName()));}
        if (Mode == CUserListPanel.SORT_MODE_HOUSE) {
            result = user1.getHouse().compareToIgnoreCase(user2.getHouse());
        }
        if (Mode == CUserListPanel.SORT_MODE_COUNTRY) {
            result = user1.getCountry().compareToIgnoreCase(user2.getCountry());
        }
        // orders are switched for the following, meaning, bigger value is earlier on list
        if (Mode == CUserListPanel.SORT_MODE_EXP) {
            result = Integer.valueOf(user2.getExp()).compareTo(user1.getExp());
        }
        if (Mode == CUserListPanel.SORT_MODE_RATING) {
            result = Float.valueOf(user2.getRating()).compareTo(user1.getRating());
        }
        if (Mode == CUserListPanel.SORT_MODE_STATUS) {
            result = Integer.valueOf(user2.getStatus()).compareTo(user1.getStatus());
        }
        if (Mode == CUserListPanel.SORT_MODE_USER_LEVEL) {
            result = Integer.valueOf(user2.getUserlevel()).compareTo(user1.getUserlevel());
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
        return (((client.CUser) o1).getName().equals(((client.CUser) o2).getName()));
    }

    public int getMode() {return Mode;}

    public void setMode(int tmode) {
        if (tmode == CUserListPanel.SORT_MODE_NAME || tmode == CUserListPanel.SORT_MODE_HOUSE ||
                  tmode == CUserListPanel.SORT_MODE_EXP || tmode == CUserListPanel.SORT_MODE_RATING ||
                  tmode == CUserListPanel.SORT_MODE_STATUS || tmode == CUserListPanel.SORT_MODE_USER_LEVEL ||
                  tmode == CUserListPanel.SORT_MODE_COUNTRY) {Mode = tmode;}
    }

    public int getOrder() {return Order;}

    public void setOrder(int torder) {
        if (torder == CUserListPanel.SORT_ORDER_ASCENDING || torder == CUserListPanel.SORT_ORDER_DESCENDING) {
            Order = torder;
        }
    }
}
