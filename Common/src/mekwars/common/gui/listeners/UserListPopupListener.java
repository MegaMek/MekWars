package mekwars.common.gui.listeners;

import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.models.CUserListModel;
import mekwars.common.gui.panels.CUserListPanel;
import mekwars.common.util.MWLogger;

class UserListPopupListener extends java.awt.event.MouseAdapter implements java.awt.event.ActionListener {

    private final CUserListPanel cUserListPanel;

    public UserListPopupListener(CUserListPanel cUserListPanel) {this.cUserListPanel = cUserListPanel;}

    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {
        if (e.getClickCount() == 2) {

            int row = cUserListPanel.UserList.locationToIndex(e.getPoint());
            if (row > -1 && row < cUserListPanel.UserList.getModel().getSize()) {
                //don't show mail/money/mute/noplay for player himself
                CUser user = ((CUserListModel) cUserListPanel.UserList.getModel()).getUser(
                      row);
                String input = STR."\{IClient.GUI_PREFIX}mail \{user.getName()}, ";
                input = input + cUserListPanel.client.getMainFrame().getMainPanel().getCommPanel().getInput();
                cUserListPanel.client.getMainFrame().getMainPanel().getCommPanel().setInput(input);
                cUserListPanel.client.getMainFrame().getMainPanel().getCommPanel().focusInputField();
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

            row = cUserListPanel.UserList.locationToIndex(e.getPoint());
            if (row > -1 && row < cUserListPanel.UserList.getModel().getSize()) {
                //don't show mail/money/mute/noplay for player himself
                CUser user = ((CUserListModel) cUserListPanel.UserList.getModel()).getUser(
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
                if (cUserListPanel.client.isMod()) {
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
                                  new Object[] { cUserListPanel.client, user });
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


                if (!userName.equalsIgnoreCase(cUserListPanel.client.getPlayer().getName())) {
                    item = new javax.swing.JMenuItem("<HTML>Mail " + userName + "</b></HTML>");
                    item.setActionCommand("MA|" + userName);
                    item.addActionListener(this);
                    popup.add(item);

                    //popup.addSeparator();

                    if (cUserListPanel.LoggedIn &&
                              user.getStatus() != cUserListPanel.client.MWClient.STATUS_LOGGEDOUT) {

                        javax.swing.JMenu sendMen = new javax.swing.JMenu("Send");

                        item = new javax.swing.JMenuItem("Send " +
                                                               cUserListPanel.client.moneyOrFluMessage(true,
                                                                     false,
                                                                     -2));
                        item.setActionCommand("MO|" + userName);
                        item.addActionListener(this);
                        sendMen.add(item);

                        item = new javax.swing.JMenuItem("Send " +
                                                               cUserListPanel.client.getserverConfigs("RPLongName"));
                        item.setActionCommand("MR|" + userName);
                        item.addActionListener(this);
                        sendMen.add(item);

                        item = new javax.swing.JMenuItem("Send " +
                                                               cUserListPanel.client.getserverConfigs("FluLongName")); //@salient
                        item.setActionCommand("MI|" + userName);
                        item.addActionListener(this);
                        sendMen.add(item);

                        item = new javax.swing.JMenuItem("Send Unit");
                        item.setActionCommand("TU|" + userName);
                        item.addActionListener(this);
                        sendMen.add(item);

                        if (Boolean.parseBoolean(cUserListPanel.client.getserverConfigs("AllowPersonalPilotQueues"))) {
                            item = new javax.swing.JMenuItem("Send Pilot");
                            item.setActionCommand("TP|" + userName);
                            item.addActionListener(this);
                            sendMen.add(item);
                        }

                        if (Boolean.parseBoolean(cUserListPanel.client.getserverConfigs("UseDirectSell"))) {
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

                    String ignoreList = cUserListPanel.client.getConfig().getParam("IGNOREPUBLIC");
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
                    ignoreList = cUserListPanel.client.getConfig().getParam("IGNOREPRIVATE");
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
                    if (user.getHouse().equals(cUserListPanel.client.getPlayer().getHouse())) {

                        ignoreList = cUserListPanel.client.getConfig().getParam("IGNOREHOUSE");
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
                    if (Integer.parseInt(cUserListPanel.client.getserverConfigs("NoPlayListSize")) >= 1) {

                        if (cUserListPanel.LoggedIn &&
                                  user.getStatus() != cUserListPanel.client.MWClient.STATUS_LOGGEDOUT) {
                            boolean isOnNoPlay = false;
                            if (cUserListPanel.client.getPlayer().getAdminExcludes().contains(userName.toLowerCase())) {
                                isOnNoPlay = true;
                            } else if (cUserListPanel.client.getPlayer()
                                             .getPlayerExcludes()
                                             .contains(userName.toLowerCase())) {
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
                if (((CUserListModel) cUserListPanel.UserList.getModel()).getSortOrder() ==
                          CUserListPanel.SORT_ORDER_DESCENDING) {
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
                if (cUserListPanel.LoggedIn) {
                    item = new javax.swing.JMenuItem("Faction");
                    item.setActionCommand("SM|H");
                    item.addActionListener(this);
                    sortSub.add(item);
                    item = new javax.swing.JMenuItem("Experience");
                    item.setActionCommand("SM|E");
                    item.addActionListener(this);
                    sortSub.add(item);
                    if (!Boolean.parseBoolean(cUserListPanel.client.getserverConfigs("HideELO"))) {
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
                if (cUserListPanel.client.getConfig().isParam("USERLISTACTIVITYBTN")) {item.setSelected(true);} else {
                    item.setSelected(false);
                }
                item.setActionCommand("ULA|" + !item.isSelected());
                item.addActionListener(this);
                settingSub.add(item);

                //bold names
                item = new javax.swing.JCheckBoxMenuItem("Bold Names");
                if (cUserListPanel.client.getConfig().isParam("USERLISTBOLD")) {item.setSelected(true);} else {
                    item.setSelected(false);
                }
                item.setActionCommand("ULB|" + !item.isSelected());
                item.addActionListener(this);
                settingSub.add(item);

                //color
                item = new javax.swing.JCheckBoxMenuItem("Colored Names");
                if (cUserListPanel.client.getConfig().isParam("USERLISTCOLOR")) {item.setSelected(true);} else {
                    item.setSelected(false);
                }
                item.setActionCommand("ULC|" + !item.isSelected());
                item.addActionListener(this);
                settingSub.add(item);

                //deds
                item = new javax.swing.JCheckBoxMenuItem("Dedicated Hosts");
                if (cUserListPanel.Dedicated) {item.setSelected(true);} else {item.setSelected(false);}
                item.setActionCommand("TD");
                item.addActionListener(this);
                settingSub.add(item);

                //player count
                item = new javax.swing.JCheckBoxMenuItem("Player Count");
                if (cUserListPanel.client.getConfig().isParam("USERLISTCOUNT")) {item.setSelected(true);} else {
                    item.setSelected(false);
                }
                item.setActionCommand("ULN|" + !item.isSelected());
                item.addActionListener(this);
                settingSub.add(item);

                //images
                item = new javax.swing.JCheckBoxMenuItem("Status Images");
                if (cUserListPanel.client.getConfig().isParam("USERLISTIMAGE")) {item.setSelected(true);} else {
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
            cUserListPanel.client.getMainFrame().jMenuFileMail_actionPerformed(userName);
            return;
        }
        //send Money
        if (command.equals("MO") && st.hasMoreElements()) {
            userName = st.nextToken();
            cUserListPanel.client.getMainFrame().jMenuCommanderTransferMoney_actionPerformed(userName);
            return;
        }

        if (command.equals("MR") && st.hasMoreElements()) {
            userName = st.nextToken();
            cUserListPanel.client.getMainFrame().jMenuCommanderTransferRewardPoints_actionPerformed(userName);
            return;
        }

        //@Salient
        if (command.equals("MI") && st.hasMoreElements()) {
            userName = st.nextToken();
            cUserListPanel.client.getMainFrame().jMenuCommanderTransferInfluence_actionPerformed(userName);
            return;
        }

        if (command.equals("TU") && st.hasMoreElements()) {
            userName = st.nextToken();
            if (true) {cUserListPanel.client.getMainFrame().jMenuCommanderTransferUnit_actionPerformed(userName, -1);}
            return;
        }

        if (command.equals("TP") && st.hasMoreElements()) {
            userName = st.nextToken();
            if (true) {cUserListPanel.client.getMainFrame().jMenuCommanderTransferPilot_actionPerformed(userName);}
            return;
        }

        if (command.equals("DSU") && st.hasMoreElements()) {
            userName = st.nextToken();
            if (true) {cUserListPanel.client.getMainFrame().jMenuCommanderDirectSell_actionPerformed(userName, null);}
            return;
        }

        if (command.equals("MU") && st.hasMoreElements()) {
            userName = st.nextToken();
            String mode = st.nextToken();
            if (true) {

                String searchString = userName;
                String ignoreList = cUserListPanel.client.getConfig().getParam("IGNORE" + mode);
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
                cUserListPanel.client.getConfig().setParam("IGNORE" + mode, newList);
                cUserListPanel.client.setIgnorePublic();
                cUserListPanel.client.setIgnoreHouse();
                cUserListPanel.client.setIgnorePrivate();
                cUserListPanel.client.getConfig().saveConfig();
                String toUser = "CH|CLIENT: You muted " + searchString + " (" + mode + ").";
                cUserListPanel.client.doParseDataInput(toUser);
                cUserListPanel.UserList.repaint();
            }
        }//end mute

        if (command.equals("UMU") && st.hasMoreElements()) {
            userName = st.nextToken();
            String mode = st.nextToken();
            if (true) {

                String searchString = userName;
                String ignoreList = cUserListPanel.client.getConfig().getParam("IGNORE" + mode);
                String newList = "";
                java.util.StringTokenizer it = new java.util.StringTokenizer(ignoreList, ",");
                while (it.hasMoreTokens()) {
                    String currString = it.nextToken();
                    if (!currString.equals(searchString)) {newList += currString + ",";}
                    //else do nothing ...

                }//end while(more ignore tokens)
                cUserListPanel.client.getConfig().setParam("IGNORE" + mode, newList);
                cUserListPanel.client.setIgnorePublic();
                cUserListPanel.client.setIgnoreHouse();
                cUserListPanel.client.setIgnorePrivate();
                cUserListPanel.client.getConfig().saveConfig();
                String toUser = "CH|CLIENT: You unmuted " + searchString + " (" + mode + ").";
                cUserListPanel.client.doParseDataInput(toUser);
                cUserListPanel.UserList.repaint();
            }
        }//end unmute

        if (command.equals("RNP") && st.hasMoreElements()) {

            userName = st.nextToken();
            if (true) {

                cUserListPanel.client.sendChat(cUserListPanel.client.MWClient.CAMPAIGN_PREFIX +
                                                     "c noplay#remove#" +
                                                     userName);
            }
        }

        if (command.equals("ANP") && st.hasMoreElements()) {
            userName = st.nextToken();
            if (true) {

                cUserListPanel.client.sendChat(cUserListPanel.client.MWClient.CAMPAIGN_PREFIX +
                                                     "c noplay#add#" +
                                                     userName);
            }
        }

        //change sort mode
        if (command.equals("SM") && st.hasMoreElements()) {
            command = st.nextToken();
            if (command.equals("N")) {
                ((CUserListModel) cUserListPanel.UserList.getModel()).setSortMode(CUserListPanel.SORT_MODE_NAME);
            } else if (command.equals("H")) {
                ((CUserListModel) cUserListPanel.UserList.getModel()).setSortMode(CUserListPanel.SORT_MODE_HOUSE);
            } else if (command.equals("E")) {
                ((CUserListModel) cUserListPanel.UserList.getModel()).setSortMode(CUserListPanel.SORT_MODE_EXP);
            } else if (command.equals("R")) {
                if (!Boolean.parseBoolean(cUserListPanel.client.getserverConfigs("HideELO"))) {
                    ((CUserListModel) cUserListPanel.UserList.getModel()).setSortMode(
                          CUserListPanel.SORT_MODE_RATING);
                } else {
                    ((CUserListModel) cUserListPanel.UserList.getModel()).setSortMode(
                          CUserListPanel.SORT_MODE_NAME);
                }
            } else if (command.equals("S")) {
                ((CUserListModel) cUserListPanel.UserList.getModel()).setSortMode(
                      CUserListPanel.SORT_MODE_STATUS);
            } else if (command.equals("L")) {
                ((CUserListModel) cUserListPanel.UserList.getModel()).setSortMode(
                      CUserListPanel.SORT_MODE_USER_LEVEL);
            } else if (command.equals("C")) {
                ((CUserListModel) cUserListPanel.UserList.getModel()).setSortMode(
                      CUserListPanel.SORT_MODE_COUNTRY);
            }

            //saveblock
            if (command.equals("H")) {
                cUserListPanel.client.getConfig().setParam("SORTMODE", "HOUSE");
            } else if (command.equals(
                  "E")) {cUserListPanel.client.getConfig().setParam("SORTMODE", "EXP");} else if (command.equals("R")) {
                cUserListPanel.client.getConfig().setParam("SORTMODE", "RATING");
            } else if (command.equals("S")) {
                cUserListPanel.client.getConfig().setParam("SORTMODE", "STATUS");
            } else if (command.equals("L")) {
                cUserListPanel.client.getConfig().setParam("SORTMODE", "USERLEVEL");
            } else if (command.equals("C")) {cUserListPanel.client.getConfig().setParam("SORTMODE", "COUNTRY");} else {
                cUserListPanel.client.getConfig().setParam("SORTMODE", "NAME");
            }
            cUserListPanel.client.getConfig().saveConfig();

            return;
        }
        //change sort order
        if (command.equals("SO") && st.hasMoreElements()) {
            command = st.nextToken();
            if (command.equals("A")) {
                ((CUserListModel) cUserListPanel.UserList.getModel()).setSortOrder(
                      CUserListPanel.SORT_ORDER_ASCENDING);
            }
            if (command.equals("D")) {
                ((CUserListModel) cUserListPanel.UserList.getModel()).setSortOrder(
                      CUserListPanel.SORT_ORDER_DESCENDING);
            }

            //saveblock
            if (command.equals("D")) {cUserListPanel.client.getConfig().setParam("SORTORDER", "DESCENDING");} else {
                cUserListPanel.client.getConfig().setParam("SORTORDER", "ASCENDING");
            }
            cUserListPanel.client.getConfig().saveConfig();

            return;
        }

        //settingss
        if (command.equals("TD")) {
            cUserListPanel.Dedicated = !cUserListPanel.Dedicated;
            if (cUserListPanel.Dedicated) {
                cUserListPanel.client.getConfig().setParam("USERLISTDEDICATEDS", "YES");
            } else {
                cUserListPanel.client.getConfig().setParam("USERLISTDEDICATEDS", "NO");
            }
            ((CUserListModel) cUserListPanel.UserList.getModel()).setDedicated(cUserListPanel.Dedicated);
            cUserListPanel.refresh();
            cUserListPanel.client.getConfig().saveConfig();
        } else if (command.equals("ULC") && st.hasMoreElements()) {
            command = st.nextToken();
            cUserListPanel.client.getConfig().setParam("USERLISTCOLOR", command);
            cUserListPanel.Users.getRenderer().refreshParams();
            cUserListPanel.UserList.repaint();
            cUserListPanel.client.getConfig().saveConfig();
        } else if (command.equals("ULI") && st.hasMoreElements()) {
            command = st.nextToken();
            cUserListPanel.client.getConfig().setParam("USERLISTIMAGE", command);
            cUserListPanel.Users.getRenderer().refreshParams();
            cUserListPanel.UserList.repaint();
            cUserListPanel.client.getConfig().saveConfig();
        } else if (command.equals("ULB") && st.hasMoreElements()) {
            command = st.nextToken();
            cUserListPanel.client.getConfig().setParam("USERLISTBOLD", command);
            cUserListPanel.Users.getRenderer().refreshParams();
            cUserListPanel.UserList.repaint();
            cUserListPanel.client.getConfig().saveConfig();
        } else if (command.equals("ULN") && st.hasMoreElements()) {
            command = st.nextToken();
            cUserListPanel.client.getConfig().setParam("USERLISTCOUNT", command);
            cUserListPanel.CountLabel.setVisible(Boolean.parseBoolean(command));
            cUserListPanel.repaint();
            cUserListPanel.client.getConfig().saveConfig();
        } else if (command.equals("ULA") && st.hasMoreElements()) {
            command = st.nextToken();
            cUserListPanel.client.getConfig().setParam("USERLISTACTIVITYBTN", command);
            cUserListPanel.ActivityButton.setVisible(Boolean.parseBoolean(command));
            cUserListPanel.repaint();
            cUserListPanel.client.getConfig().saveConfig();
        }

        /*
         * Mod commands used to be here. Moved into
         * admin.ModeratorPopupMenu, 6/26/05, @urgru
         */
    }
}
