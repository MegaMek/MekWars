package mekwars.common.gui.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringTokenizer;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.panels.CUserListPanel;

public class UserListPopupListener extends MouseAdapter implements ActionListener {
    private final static MMLogger LOGGER = MMLogger.create(UserListPopupListener.class);

    private final CUserListPanel cUserListPanel;

    public UserListPopupListener(CUserListPanel cUserListPanel) {
        this.cUserListPanel = cUserListPanel;
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            int row = cUserListPanel.getcUserListModelJList().locationToIndex(event.getPoint());

            if (row > -1 && row < cUserListPanel.getcUserListModelJList().getModel().getSize()) {
                //don't show mail/money/mute/noplay for player himself
                CUser user = cUserListPanel.getcUserListModel().getUser(row);
                String input = String.format("%smail %s, ", IClient.GUI_PREFIX, user.getName());
                input = input + cUserListPanel.getClient().getMainFrame().getMainPanel().getCommPanel().getInput();
                cUserListPanel.getClient().getMainFrame().getMainPanel().getCommPanel().setInput(input);
                cUserListPanel.getClient().getMainFrame().getMainPanel().getCommPanel().focusInputField();
            }

        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        maybeShowPopup(mouseEvent);
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        maybeShowPopup(mouseEvent);
    }

    private void maybeShowPopup(MouseEvent mouseEvent) {
        JMenuItem item;
        JPopupMenu popup;
        int row = -1;
        String userName = "";

        popup = new JPopupMenu();
        if (mouseEvent.isPopupTrigger()) {

            row = cUserListPanel.getcUserListModelJList().locationToIndex(mouseEvent.getPoint());
            if (row > -1 && row < cUserListPanel.getcUserListModelJList().getModel().getSize()) {
                //don't show mail/money/mute/noplay for player himself
                CUser user = cUserListPanel.getcUserListModel().getUser(row);
                userName = user.getName();

                /*
                 * MOD MENU @Torren 4.7.05
                 *
                 * Most of the mod menu moved into a separate
                 * admin package, since its a waste if bytes to
                 * have all players downloading things that only
                 * a handful has access to.
                 */
                if (cUserListPanel.getClient().isMod()) {
                    URLClassLoader loader = null;
                    try {
                        File loadJar = new File("./MekWarsAdmin.jar");
                        if (!loadJar.exists()) {
                            LOGGER.debug("StaffUserlistPopupMenu creation skipped. No MekWarsAdmin.jar present.");
                        } else {
                            loader = new URLClassLoader(new URL[] { loadJar.toURI().toURL() });
                            Class<?> loadedClass = loader.loadClass("admin.StaffUserlistPopupMenu");
                            Object newInstance = loadedClass.getDeclaredConstructor().newInstance();
                            loadedClass.getDeclaredMethod("createMenu", new Class[] { IClient.class, CUser.class })
                                  .invoke(newInstance, cUserListPanel.getClient(), user);
                            popup.add((JMenu) newInstance);
                        }
                    } catch (Exception ex) {
                        LOGGER.error(ex, "StaffUserlistPopupMenu creation FAILED!");
                    } finally {
                        try {
                            loader.close();
                        } catch (java.io.IOException e1) {
                            LOGGER.error(e1, "URLClassLoader close FAILED!");
                        }
                    }

                    popup.addSeparator();
                }

                IClient panelClient = cUserListPanel.getClient();

                if (!userName.equalsIgnoreCase(panelClient.getPlayer().getName())) {
                    item = new JMenuItem(String.format("<HTML>Mail %s</b></HTML>", userName));
                    item.setActionCommand(String.format("MA|%s", userName));
                    item.addActionListener(this);
                    popup.add(item);

                    if (cUserListPanel.isLoggedIn() && user.getStatus() != IClient.STATUS_LOGGED_OUT) {
                        JMenu sendMen = new JMenu("Send");

                        item = new JMenuItem(String.format("Send %s", panelClient.moneyOrFluMessage(true,
                              false,
                              -2)));
                        item.setActionCommand(String.format("MO|%s", userName));
                        item.addActionListener(this);
                        sendMen.add(item);

                        item = new JMenuItem(String.format("Send %s", panelClient.getServerConfigs("RPLongName")));
                        item.setActionCommand(String.format("MR|%s", userName));
                        item.addActionListener(this);
                        sendMen.add(item);

                        item = new JMenuItem(String.format("Send %s", panelClient.getServerConfigs("FluLongName"))); //@salient
                        item.setActionCommand(String.format("MI|%s", userName));
                        item.addActionListener(this);
                        sendMen.add(item);

                        item = new JMenuItem("Send Unit");
                        item.setActionCommand(String.format("TU|%s", userName));
                        item.addActionListener(this);
                        sendMen.add(item);

                        if (MathUtility.parseBoolean(panelClient.getServerConfigs("AllowPersonalPilotQueues"), false)) {
                            item = new JMenuItem("Send Pilot");
                            item.setActionCommand(String.format("TP|%s", userName));
                            item.addActionListener(this);
                            sendMen.add(item);
                        }

                        if (MathUtility.parseBoolean(panelClient.getServerConfigs("UseDirectSell"), false)) {
                            item = new JMenuItem("Direct Sell Unit");
                            item.setActionCommand(String.format("DSU|%s", userName));
                            item.addActionListener(this);
                            sendMen.add(item);
                        }

                        popup.add(sendMen);
                    }

                    JMenu blockMen = new JMenu("Block");

                    /*
                     * Mute/Unmute the player. Detect the name string in the ignore
                     * list and then display it as appropriate. for Main.
                     */

                    boolean matched = false;

                    String ignoreList = panelClient.getConfig().getParam("IGNORE_PUBLIC");
                    StringTokenizer stringTokenizer = new StringTokenizer(ignoreList, ",");

                    while (stringTokenizer.hasMoreTokens() && !matched) {
                        String currString = stringTokenizer.nextToken().trim();

                        if (currString.equalsIgnoreCase(userName)) {
                            matched = true;
                        }
                    }

                    if (!matched) {
                        item = new JMenuItem("Mute (Main)");
                        item.setActionCommand(String.format("MU|%s|PUBLIC", userName));
                        item.addActionListener(this);
                        blockMen.add(item);
                    } else {
                        item = new JMenuItem("Unmute (Main)");
                        item.setActionCommand(String.format("UMU|%s|PUBLIC", userName));
                        item.addActionListener(this);
                        blockMen.add(item);
                    }

                    /*
                     * Mute/Unmute the player via PrivateMessageCommand
                     */
                    ignoreList = panelClient.getConfig().getParam("IGNORE_PRIVATE");
                    stringTokenizer = new StringTokenizer(ignoreList, ",");
                    matched = false;
                    while (stringTokenizer.hasMoreTokens() && !matched) {
                        String currString = stringTokenizer.nextToken().trim();

                        if (currString.equalsIgnoreCase(userName)) {
                            matched = true;
                        }
                    }

                    if (!matched) {
                        item = new JMenuItem("Mute (Private)");
                        item.setActionCommand(String.format("MU|%s|PRIVATE", userName));
                        item.addActionListener(this);
                        blockMen.add(item);
                    } else {
                        item = new JMenuItem("Unmute (Private)");
                        item.setActionCommand(String.format("UMU|%s|PRIVATE", userName));
                        item.addActionListener(this);
                        blockMen.add(item);
                    }

                    //if in the same faction, also show faction mute
                    if (user.getHouse().equals(panelClient.getPlayer().getHouse())) {
                        ignoreList = panelClient.getConfig().getParam("IGNORE_HOUSE");
                        stringTokenizer = new StringTokenizer(ignoreList, ",");
                        matched = false;

                        while (stringTokenizer.hasMoreTokens() && !matched) {
                            String currString = stringTokenizer.nextToken().trim();

                            if (currString.equalsIgnoreCase(userName)) {
                                matched = true;
                            }
                        }

                        if (!matched) {
                            item = new JMenuItem("Mute (House)");
                            item.setActionCommand(String.format("MU|%s|HOUSE", userName));
                            item.addActionListener(this);
                            blockMen.add(item);
                        } else {
                            item = new JMenuItem("Unmute (House)");
                            item.setActionCommand(String.format("UMU|%s|HOUSE", userName));
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
                    if (MathUtility.parseInt(panelClient.getServerConfigs("NoPlayListSize"), 0) >= 1) {
                        if (cUserListPanel.isLoggedIn() && user.getStatus() != IClient.STATUS_LOGGED_OUT) {
                            boolean isOnNoPlay = false;

                            if (panelClient.getPlayer().getAdminExcludes().contains(userName.toLowerCase())) {
                                isOnNoPlay = true;
                            } else if (panelClient.getPlayer().getPlayerExcludes().contains(userName.toLowerCase())) {
                                isOnNoPlay = true;
                            }

                            if (isOnNoPlay) {
                                item = new JMenuItem("Remove from No-Play");
                                item.setActionCommand(String.format("RNP|%s", userName));
                            } else {
                                item = new JMenuItem("Add to No-Play");
                                item.setActionCommand(String.format("ANP|%s", userName));
                            }

                            item.addActionListener(this);
                            blockMen.add(item);
                        } else {//show blank
                            item = new JMenuItem("No-Play");
                            item.setEnabled(false);
                            blockMen.add(item);
                        }
                        popup.addSeparator();
                    }//end if (should draw no-play menu items)
                    popup.add(blockMen);


                }//end if (clicked player isn't THE player)
                //Toggle ascending/decending order
                if (cUserListPanel.getcUserListModel().getSortOrder() == CUserListPanel.SORT_ORDER_DESCENDING) {
                    item = new JMenuItem("Ascending Order");
                    item.setActionCommand("SO|A");
                } else {
                    item = new JMenuItem("Descending Order");
                    item.setActionCommand("SO|D");
                }
                item.addActionListener(this);
                popup.add(item);

                //Sort Sub-Menu
                JMenu sortSub = new JMenu("Sort By");
                popup.add(sortSub);

                item = new JMenuItem("Name");
                item.setActionCommand("SM|N");
                item.addActionListener(this);
                sortSub.add(item);

                if (cUserListPanel.isLoggedIn()) {
                    item = new JMenuItem("Faction");
                    item.setActionCommand("SM|H");
                    item.addActionListener(this);
                    sortSub.add(item);

                    item = new JMenuItem("Experience");
                    item.setActionCommand("SM|E");
                    item.addActionListener(this);
                    sortSub.add(item);

                    if (!MathUtility.parseBoolean(panelClient.getServerConfigs("HideELO"), false)) {
                        item = new JMenuItem("Rating");
                        item.setActionCommand("SM|R");
                        item.addActionListener(this);
                        sortSub.add(item);
                    }

                    item = new JMenuItem("Status");
                    item.setActionCommand("SM|S");
                    item.addActionListener(this);
                    sortSub.add(item);
                }
                item = new JMenuItem("User Level");
                item.setActionCommand("SM|L");
                item.addActionListener(this);
                sortSub.add(item);

                item = new JMenuItem("Country");
                item.setActionCommand("SM|C");
                item.addActionListener(this);
                sortSub.add(item);

                popup.addSeparator();

                JMenu settingSub = new JMenu("List Settings");
                popup.add(settingSub);

                //activity button
                item = new JCheckBoxMenuItem("Activity Button");

                item.setSelected(panelClient.getConfig().isParam("USER_LIST_ACTIVITY_BUTTON"));

                item.setActionCommand(String.format("ULA|%s", !item.isSelected()));
                item.addActionListener(this);
                settingSub.add(item);

                //bold names
                item = new JCheckBoxMenuItem("Bold Names");
                item.setSelected(panelClient.getConfig().isParam("USER_LIST_BOLD"));
                item.setActionCommand(String.format("ULB|%s", !item.isSelected()));
                item.addActionListener(this);
                settingSub.add(item);

                //color
                item = new JCheckBoxMenuItem("Colored Names");
                item.setSelected(panelClient.getConfig().isParam("USER_LIST_COLOR"));
                item.setActionCommand(String.format("ULC|%s", !item.isSelected()));
                item.addActionListener(this);
                settingSub.add(item);

                //deds
                item = new JCheckBoxMenuItem("Dedicated Hosts");
                item.setSelected(cUserListPanel.isDedicated());
                item.setActionCommand("TD");
                item.addActionListener(this);
                settingSub.add(item);

                //player count
                item = new JCheckBoxMenuItem("Player Count");
                item.setSelected(panelClient.getConfig().isParam("USER_LIST_COUNT"));
                item.setActionCommand(String.format("ULN|%s", !item.isSelected()));
                item.addActionListener(this);
                settingSub.add(item);

                //images
                item = new JCheckBoxMenuItem("Status Images");
                item.setSelected(panelClient.getConfig().isParam("USER_LIST_IMAGE"));
                item.setActionCommand(String.format("ULI|%s", !item.isSelected()));
                item.addActionListener(this);
                settingSub.add(item);

                popup.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            }
        }
    }

    public void actionPerformed(ActionEvent actionEvent) {
        String actionCommand = actionEvent.getActionCommand();
        StringTokenizer stringTokenizer = new StringTokenizer(actionCommand, "|");
        String command = stringTokenizer.nextToken();
        String userName = "";

        //send mail
        if (command.equals("MA") && stringTokenizer.hasMoreElements()) {
            userName = stringTokenizer.nextToken();
            cUserListPanel.getClient().getMainFrame().jMenuFileMail_actionPerformed(userName);
            return;
        }
        //send Money
        if (command.equals("MO") && stringTokenizer.hasMoreElements()) {
            userName = stringTokenizer.nextToken();
            cUserListPanel.getClient().getMainFrame().jMenuCommanderTransferMoney_actionPerformed(userName);
            return;
        }

        if (command.equals("MR") && stringTokenizer.hasMoreElements()) {
            userName = stringTokenizer.nextToken();
            cUserListPanel.getClient().getMainFrame().jMenuCommanderTransferRewardPoints_actionPerformed(userName);
            return;
        }

        //@Salient
        if (command.equals("MI") && stringTokenizer.hasMoreElements()) {
            userName = stringTokenizer.nextToken();
            cUserListPanel.getClient().getMainFrame().jMenuCommanderTransferInfluence_actionPerformed(userName);
            return;
        }

        if (command.equals("TU") && stringTokenizer.hasMoreElements()) {
            userName = stringTokenizer.nextToken();
            cUserListPanel.getClient().getMainFrame().jMenuCommanderTransferUnit_actionPerformed(userName, -1);
            return;
        }

        if (command.equals("TP") && stringTokenizer.hasMoreElements()) {
            userName = stringTokenizer.nextToken();
            cUserListPanel.getClient().getMainFrame().jMenuCommanderTransferPilot_actionPerformed(userName);
            return;
        }

        if (command.equals("DSU") && stringTokenizer.hasMoreElements()) {
            userName = stringTokenizer.nextToken();
            cUserListPanel.getClient().getMainFrame().jMenuCommanderDirectSell_actionPerformed(userName, null);
            return;
        }

        if (command.equals("MU") && stringTokenizer.hasMoreElements()) {
            userName = stringTokenizer.nextToken();
            String mode = stringTokenizer.nextToken();
            String searchString = userName;
            String ignoreList = cUserListPanel.getClient().getConfig().getParam(String.format("IGNORE%s", mode));
            StringBuilder newList = new StringBuilder();
            StringTokenizer tokenizer = new StringTokenizer(ignoreList, ",");
            boolean matched = false;

            while (tokenizer.hasMoreTokens() && !matched) {
                //rebuild the list to make sure ,'actionCommand are ok.
                String currString = tokenizer.nextToken();
                newList.append(currString).append(",");

                if (currString.equals(searchString)) {
                    matched = true;
                }
            }

            if (!matched) {
                newList.append(searchString).append(",");
            }

            cUserListPanel.getClient().getConfig().setParam(String.format("IGNORE%s", mode), newList.toString());
            cUserListPanel.getClient().setIgnorePublic();
            cUserListPanel.getClient().setIgnoreHouse();
            cUserListPanel.getClient().setIgnorePrivate();
            cUserListPanel.getClient().getConfig().saveConfig();
            String toUser = String.format("CH|CLIENT: You muted %s (%s).", searchString, mode);
            cUserListPanel.getClient().doParseDataInput(toUser);
            cUserListPanel.getcUserListModelJList().repaint();
        }//end mute

        if (command.equals("UMU") && stringTokenizer.hasMoreElements()) {
            userName = stringTokenizer.nextToken();
            String mode = stringTokenizer.nextToken();
            String searchString = userName;
            String ignoreList = cUserListPanel.getClient().getConfig().getParam(String.format("IGNORE%s", mode));
            StringBuilder newList = new StringBuilder();
            StringTokenizer tokenizer = new StringTokenizer(ignoreList, ",");

            while (tokenizer.hasMoreTokens()) {
                String currString = tokenizer.nextToken();

                if (!currString.equals(searchString)) {
                    newList.append(currString).append(",");
                }
                //else do nothing ...

            }//end while(more ignore tokens)

            cUserListPanel.getClient().getConfig().setParam(String.format("IGNORE%s", mode), newList.toString());
            cUserListPanel.getClient().setIgnorePublic();
            cUserListPanel.getClient().setIgnoreHouse();
            cUserListPanel.getClient().setIgnorePrivate();
            cUserListPanel.getClient().getConfig().saveConfig();

            String toUser = String.format("CH|CLIENT: You unmuted %s (%s).", searchString, mode);
            cUserListPanel.getClient().doParseDataInput(toUser);
            cUserListPanel.getcUserListModelJList().repaint();
        }//end unmute

        if (command.equals("RNP") && stringTokenizer.hasMoreElements()) {
            userName = stringTokenizer.nextToken();
            cUserListPanel.getClient().sendChat(String.format("%sc noplay#remove#%s", IClient.CAMPAIGN_PREFIX, userName));
        }

        if (command.equals("ANP") && stringTokenizer.hasMoreElements()) {
            userName = stringTokenizer.nextToken();
            cUserListPanel.getClient().sendChat(String.format("%sc noplay#add#%s", IClient.CAMPAIGN_PREFIX, userName));
        }

        //change sort mode
        if (command.equals("SM") && stringTokenizer.hasMoreElements()) {
            command = stringTokenizer.nextToken();
            switch (command) {
                case "N" -> cUserListPanel.getcUserListModel().setSortMode(CUserListPanel.SORT_MODE_NAME);
                case "H" -> cUserListPanel.getcUserListModel().setSortMode(CUserListPanel.SORT_MODE_HOUSE);
                case "E" -> cUserListPanel.getcUserListModel().setSortMode(CUserListPanel.SORT_MODE_EXP);
                case "R" -> {
                    if (!MathUtility.parseBoolean(cUserListPanel.getClient().getServerConfigs("HideELO"), false)) {
                        cUserListPanel.getcUserListModel().setSortMode(CUserListPanel.SORT_MODE_RATING);
                    } else {
                        cUserListPanel.getcUserListModel().setSortMode(CUserListPanel.SORT_MODE_NAME);
                    }
                }
                case "S" -> cUserListPanel.getcUserListModel().setSortMode(CUserListPanel.SORT_MODE_STATUS);
                case "L" -> cUserListPanel.getcUserListModel().setSortMode(CUserListPanel.SORT_MODE_USER_LEVEL);
                case "C" -> cUserListPanel.getcUserListModel().setSortMode(CUserListPanel.SORT_MODE_COUNTRY);
            }

            //save block
            switch (command) {
                case "H" -> cUserListPanel.getClient().getConfig().setParam("SORT_MODE", "HOUSE");
                case "E" -> cUserListPanel.getClient().getConfig().setParam("SORT_MODE", "EXP");
                case "R" -> cUserListPanel.getClient().getConfig().setParam("SORT_MODE", "RATING");
                case "S" -> cUserListPanel.getClient().getConfig().setParam("SORT_MODE", "STATUS");
                case "L" -> cUserListPanel.getClient().getConfig().setParam("SORT_MODE", "USER_LEVEL");
                case "C" -> cUserListPanel.getClient().getConfig().setParam("SORT_MODE", "COUNTRY");
                default -> cUserListPanel.getClient().getConfig().setParam("SORT_MODE", "NAME");
            }

            cUserListPanel.getClient().getConfig().saveConfig();

            return;
        }
        //change sort order
        if (command.equals("SO") && stringTokenizer.hasMoreElements()) {
            command = stringTokenizer.nextToken();

            if (command.equals("A")) {
                cUserListPanel.getcUserListModel().setSortOrder(CUserListPanel.SORT_ORDER_ASCENDING);
            }

            if (command.equals("D")) {
                cUserListPanel.getcUserListModel().setSortOrder(CUserListPanel.SORT_ORDER_DESCENDING);
            }

            //save block
            if (command.equals("D")) {
                cUserListPanel.getClient().getConfig().setParam("SORT_ORDER", "DESCENDING");
            } else {
                cUserListPanel.getClient().getConfig().setParam("SORT_ORDER", "ASCENDING");
            }

            cUserListPanel.getClient().getConfig().saveConfig();

            return;
        }

        //settings
        if (command.equals("TD")) {
            cUserListPanel.setDedicated(!cUserListPanel.isDedicated());

            if (cUserListPanel.isDedicated()) {
                cUserListPanel.getClient().getConfig().setParam("USER_LIST_DEDICATEDS", "YES");
            } else {
                cUserListPanel.getClient().getConfig().setParam("USER_LIST_DEDICATEDS", "NO");
            }

            cUserListPanel.getcUserListModel().setDedicated(cUserListPanel.isDedicated());
            cUserListPanel.refresh();
            cUserListPanel.getClient().getConfig().saveConfig();
        } else if (command.equals("ULC") && stringTokenizer.hasMoreElements()) {
            command = stringTokenizer.nextToken();
            cUserListPanel.getClient().getConfig().setParam("USER_LIST_COLOR", command);
            cUserListPanel.getcUserListModel().getRenderer().refreshParams();
            cUserListPanel.getcUserListModelJList().repaint();
            cUserListPanel.getClient().getConfig().saveConfig();
        } else if (command.equals("ULI") && stringTokenizer.hasMoreElements()) {
            command = stringTokenizer.nextToken();
            cUserListPanel.getClient().getConfig().setParam("USER_LIST_IMAGE", command);
            cUserListPanel.getcUserListModel().getRenderer().refreshParams();
            cUserListPanel.getcUserListModelJList().repaint();
            cUserListPanel.getClient().getConfig().saveConfig();
        } else if (command.equals("ULB") && stringTokenizer.hasMoreElements()) {
            command = stringTokenizer.nextToken();
            cUserListPanel.getClient().getConfig().setParam("USER_LIST_BOLD", command);
            cUserListPanel.getcUserListModel().getRenderer().refreshParams();
            cUserListPanel.getcUserListModelJList().repaint();
            cUserListPanel.getClient().getConfig().saveConfig();
        } else if (command.equals("ULN") && stringTokenizer.hasMoreElements()) {
            command = stringTokenizer.nextToken();
            cUserListPanel.getClient().getConfig().setParam("USER_LIST_COUNT", command);
            cUserListPanel.getCountLabel().setVisible(Boolean.parseBoolean(command));
            cUserListPanel.repaint();
            cUserListPanel.getClient().getConfig().saveConfig();
        } else if (command.equals("ULA") && stringTokenizer.hasMoreElements()) {
            command = stringTokenizer.nextToken();
            cUserListPanel.getClient().getConfig().setParam("USER_LIST_ACTIVITY_BUTTON", command);
            cUserListPanel.getActivateButton().setVisible(Boolean.parseBoolean(command));
            cUserListPanel.repaint();
            cUserListPanel.getClient().getConfig().saveConfig();
        }

        /*
         * Mod commands used to be here. Moved into
         * admin.ModeratorPopupMenu, 6/26/05, @urgru
         */
    }
}
