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

import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.models.CUserListModel;
import mekwars.common.util.MWLogger;

public class UserListCellRenderer extends javax.swing.JLabel implements ListCellRenderer<CUserListModel> {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 4400213401819469963L;
    IClient client;
    CUserListModel Owner;
    boolean LoggedIn = false;
    boolean TextBold;
    boolean TextColor;
    boolean TextImage;
    ImageIcon LogoutImage;
    ImageIcon ReserveImage;
    ImageIcon ActiveImage;
    ImageIcon FightImage;

    public UserListCellRenderer(CUserListModel towner) {
        Owner = towner;
        client = towner.client;
        TextBold = client.getConfig().isParam("USERLISTBOLD");
        TextColor = client.getConfig().isParam("USERLISTCOLOR");
        TextImage = client.getConfig().isParam("USERLISTIMAGE");
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
        TextBold = client.getConfig().isParam("USERLISTBOLD");
        TextColor = client.getConfig().isParam("USERLISTCOLOR");
        TextImage = client.getConfig().isParam("USERLISTIMAGE");
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
        int userlevel;
        int status;

        CUser user = Owner.getUser(index);
        if (user == null) {
            return null;
        }

        userlevel = user.getUserLevel();
        String invisFlag = " ";

        //if you can see them, and they are invis, then your level is >= to theres
        if (user.isInvisible()) {
            invisFlag = "(I) ";
        }

        if (userlevel < 30) {
            setText(user.getName());
        }

        if (userlevel >= 30 && userlevel < 100) {
            setText(STR."^\{invisFlag}\{user.getName()}");
        }

        if (userlevel >= 100 && userlevel < 200) {
            setText(STR."*\{invisFlag}\{user.getName()}");
        }

        if (userlevel >= 200) {
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

        if (userlevel < 100) {
            String ignoreList = client.getConfig().getParam("IGNOREPUBLIC");
            StringTokenizer it = new StringTokenizer(ignoreList, ",");
            while (it.hasMoreTokens()) {
                String currString = it.nextToken().trim();
                if (currString.equalsIgnoreCase(searchString)) {
                    isMuted++;
                }
            }

            //search PrivateMessageCommand mute as well
            ignoreList = client.getConfig().getParam("IGNOREPRIVATE");
            it = new StringTokenizer(ignoreList, ",");
            while (it.hasMoreTokens()) {
                String currString = it.nextToken().trim();
                if (currString.equalsIgnoreCase(searchString)) {
                    isMuted++;
                }
            }


            //and the faction ...
            if (user.getHouse().equals(client.getPlayer().getHouse())) {
                ignoreList = client.getConfig().getParam("IGNOREHOUSE");
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
                        MWLogger.errLog(ex);
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
                            MWLogger.errLog(ex);
                        }
                    }
                    if (status == IClient.STATUS_ACTIVE) {
                        try {
                            setIcon(ActiveImage);
                        } catch (Exception ex) {
                            MWLogger.errLog(ex);
                        }
                    }
                    if (status == IClient.STATUS_FIGHTING) {
                        try {
                            setIcon(FightImage);
                        } catch (Exception ex) {
                            MWLogger.errLog(ex);
                        }
                    }
                } else {
                    setIcon(null);
                }
            }
            setIconTextGap(7);
            setToolTipText(user.getInfo(client.getConfig().isParam("NOIMGINCHAT")));
        } else {

            //logged-out users don't see bold names OR icons
            setFont(getFont().deriveFont(java.awt.Font.PLAIN));
            setIcon(null);

            setToolTipText(user.getShortInfo());
        }
        return this;
    }
}
