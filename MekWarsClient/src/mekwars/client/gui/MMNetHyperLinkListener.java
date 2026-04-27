/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package mekwars.client.gui;

import mekwars.client.MWClient;
import mekwars.common.Planet;
import mekwars.common.gui.InnerStellarMap;
import mekwars.common.util.MWLogger;

class MMNetHyperLinkListener implements javax.swing.event.HyperlinkListener {

    protected boolean isHovering = false;
    protected String Tooltip = null;
    protected mekwars.common.gui.CHSPanel HSPanel = null;
    MWClient mwclient;

    /*
     * Construct which takes only MWClient
     * is used for chat tabs.
     *
     * Construct which takes CHSPanel is
     * used exclusively within that panel.
     */
    public MMNetHyperLinkListener(client.MWClient f) {
        mwclient = f;
    }

    public MMNetHyperLinkListener(client.MWClient f, mekwars.common.gui.CHSPanel p) {
        mwclient = f;
        HSPanel = p;
    }

    public boolean isHoveringOverHyperlink() {
        return isHovering;
    }

    /**
     * Gets the URL being hovered over.
     *
     * @return The URL value if mouse is currently hovering over a URL, or
     *       <code>null</code> if not currently hovering over a URL
     */
    public String getTooltip() {
        return Tooltip;
    }

    public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent e) {
        if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ENTERED) {
            isHovering = true;
            try {
                Tooltip = (String) e.getSourceElement()
                                         .getAttributes()
                                         .getAttribute(javax.swing.text.html.HTML.getAttributeKey("alt"));
                MWLogger.infoLog(Tooltip);
                if (HSPanel != null) {HSPanel.setInfoText(Tooltip);}
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
            //MWLogger.infoLog("hyperlinkUpdate fired");
            //MWLogger.infoLog("     entered->");
        } else if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.EXITED) {
            isHovering = false;
            if (HSPanel != null) {HSPanel.setInfoText("");}
            Tooltip = null;
            //MWLogger.infoLog("     <-exited");
        }


        if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
            javax.swing.JEditorPane pane = (javax.swing.JEditorPane) e.getSource();
            if (e instanceof javax.swing.text.html.HTMLFrameHyperlinkEvent) {
                javax.swing.text.html.HTMLFrameHyperlinkEvent evt = (javax.swing.text.html.HTMLFrameHyperlinkEvent) e;
                javax.swing.text.html.HTMLDocument doc = (javax.swing.text.html.HTMLDocument) pane.getDocument();
                doc.processHTMLFrameHyperlinkEvent(evt);
            } else {
                try {

                    if (e.getDescription().startsWith("MEKWARS")) {
                        String command = e.getDescription();
                        command = command.substring(7);
                        mwclient.sendChat(command);
                    } else if (e.getDescription().startsWith("MEKMAIL")) {
                        String command = e.getDescription();
                        command = command.substring(7);
                        java.util.StringTokenizer commandStr = new java.util.StringTokenizer(command, "*");
                        command = commandStr.nextToken() + ", " + commandStr.nextToken();
                        MWLogger.errLog("Command " + command);
                        mwclient.sendChat("/mail " + command);
                    } else if (e.getDescription().startsWith("MEKINFO")) {
                        String command = e.getDescription();
                        command = command.substring(7);
                        java.util.StringTokenizer ST = new java.util.StringTokenizer(command, "#");
                        String filename = ST.nextToken().replace("%22", "\"");
                        int BV = Integer.parseInt(ST.nextToken());
                        int gunnery = Integer.parseInt(ST.nextToken());
                        int piloting = Integer.parseInt(ST.nextToken());
                        String battleDamage = "";
                        if (ST.hasMoreTokens()) {battleDamage = ST.nextToken();}
                        mwclient.getMainFrame()
                              .getMainPanel()
                              .getHSPanel()
                              .showInfoWindow(filename, BV, gunnery, piloting, battleDamage);
                    } else if (e.getDescription().startsWith("MWUSERP")) {
                        String command = e.getDescription();
                        command = command.substring(7);
                        mwclient.rewardPointsDialog();
                    } else if (e.getDescription().startsWith("MWREG")) {
                        String command = e.getDescription();
                        command = command.substring(5);
                        mwclient.getMainFrame().jMenuFileRegister_actionPerformed();
                    } else if (e.getDescription().startsWith("JUMPTOPLANET")) {
                        String command = e.getDescription();
                        command = command.substring(12);
                        java.util.StringTokenizer ST = new java.util.StringTokenizer(command, "#");
                        String planetName = ST.nextToken();

                        //fetch the map
                        InnerStellarMap map = mwclient.getMainFrame().getMainPanel().getMapPanel().getMap();

                        //get the planet
                        Planet currPlanet = mwclient.getData().getPlanetByName(planetName);

                        if (currPlanet != null) {
                            map.setSelectedPlanet(currPlanet);
                            map.activate(currPlanet, true);
                            map.saveMapSelection(currPlanet);

                            /*
                             * If the map is visible and we're supposed to jump to it, get the
                             * main panel and call .selectMapTab(). selectMap will check where
                             * the map is (top or bottom) and send the correct tab to the front.
                             */
                            if (mwclient.getConfig().isParam("MAPTABONCLICK") &&
                                      mwclient.getConfig().isParam("MAPTABVISIBLE")) {
                                mwclient.getMainFrame().getMainPanel().selectMapTab();
                            }
                        }
                    } else if (e.getDescription().startsWith("MWDEFECTDLG")) {
                        String command = e.getDescription();
                        command = command.substring(11);//strip command code

                        //open a warning dialog
                        Object[] options = { " Defect ", " Cancel " };
                        int confirmed = javax.swing.JOptionPane.showOptionDialog(mwclient.getMainFrame(),
                              "Are you SURE you want to defect?",
                              "Defection Confirmation",
                              javax.swing.JOptionPane.DEFAULT_OPTION,
                              javax.swing.JOptionPane.WARNING_MESSAGE,
                              null,
                              options,
                              options[1]);

                        //if confirmed, send CONFIRM command
                        if (confirmed == javax.swing.JOptionPane.OK_OPTION) {mwclient.sendChat(command);}
                    } else if (e.getDescription().startsWith("MWSOLDEFECT")) {
                        mwclient.getMainFrame().jMenuCommanderDefect_actionPerformed();
                    } else if (e.getDescription().startsWith("REMOVEQUEUEDWORKORDER")) {
                        String command = e.getDescription();
                        java.util.StringTokenizer ST = new java.util.StringTokenizer(command, "|");
                        ST.nextToken();//strip command code
                        int tech = Integer.parseInt(ST.nextToken());
                        String position = ST.nextToken();

                        mwclient.getRMT().removeWorkOrder(tech, position);

                    } else if (e.getDescription().startsWith("REMOVESALVAGEQUEUEDWORKORDER")) {
                        String command = e.getDescription();
                        java.util.StringTokenizer ST = new java.util.StringTokenizer(command, "|");
                        ST.nextToken();//strip command code
                        int tech = Integer.parseInt(ST.nextToken());
                        String position = ST.nextToken();

                        mwclient.getSMT().removeWorkOrder(tech, position);

                    } else {
                        mekwars.common.gui.Browser.displayURL(e.getURL().toExternalForm());
                    }
                } catch (Throwable t) {
                    MWLogger.errLog((Exception) t);
                }
            }
        }
    }
}
