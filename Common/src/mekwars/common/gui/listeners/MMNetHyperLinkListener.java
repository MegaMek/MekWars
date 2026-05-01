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

package mekwars.common.gui.listeners;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.StringTokenizer;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import mekwars.common.Planet;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.InnerStellarMap;
import mekwars.common.gui.panels.CHSPanel;
import mekwars.common.util.MWLogger;

public class MMNetHyperLinkListener implements HyperlinkListener {

    protected boolean isHovering = false;
    protected String Tooltip = null;
    protected CHSPanel HSPanel = null;
    IClient client;

    /*
     * Construct, which takes only IClient,
     * is used for chat tabs.
     *
     * Construct which takes CHSPanel is
     * used exclusively within that panel.
     */
    public MMNetHyperLinkListener(IClient client) {
        this.client = client;
    }

    public MMNetHyperLinkListener(IClient client, CHSPanel panel) {
        this.client = client;
        HSPanel = panel;
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

    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            isHovering = true;
            try {
                Tooltip = (String) event.getSourceElement()
                                         .getAttributes()
                                         .getAttribute(HTML.getAttributeKey("alt"));
                MWLogger.infoLog(Tooltip);
                if (HSPanel != null) {
                    HSPanel.setInfoText(Tooltip);
                }
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
        } else if (event.getEventType() == HyperlinkEvent.EventType.EXITED) {
            isHovering = false;
            if (HSPanel != null) {
                HSPanel.setInfoText("");
            }
            Tooltip = null;
        }


        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            JEditorPane pane = (JEditorPane) event.getSource();
            if (event instanceof HTMLFrameHyperlinkEvent evt) {
                HTMLDocument doc = (HTMLDocument) pane.getDocument();
                doc.processHTMLFrameHyperlinkEvent(evt);
            } else {
                try {
                    if (event.getDescription().startsWith("MEKWARS")) {
                        String command = event.getDescription();
                        command = command.substring(7);
                        client.sendChat(command);
                    } else if (event.getDescription().startsWith("MEKMAIL")) {
                        String command = event.getDescription();
                        command = command.substring(7);
                        StringTokenizer commandStr = new StringTokenizer(command, "*");
                        command = STR."\{commandStr.nextToken()}, \{commandStr.nextToken()}";
                        MWLogger.errLog(STR."Command \{command}");
                        client.sendChat(STR."/mail \{command}");
                    } else if (event.getDescription().startsWith("MEKINFO")) {
                        String command = event.getDescription();
                        command = command.substring(7);
                        StringTokenizer ST = new StringTokenizer(command, "#");
                        String filename = ST.nextToken().replace("%22", "\"");

                        int BV = Integer.parseInt(ST.nextToken());
                        int gunnery = Integer.parseInt(ST.nextToken());
                        int piloting = Integer.parseInt(ST.nextToken());
                        String battleDamage = "";
                        if (ST.hasMoreTokens()) {battleDamage = ST.nextToken();}
                        client.getMainFrame()
                              .getMainPanel()
                              .getHSPanel()
                              .showInfoWindow(filename, BV, gunnery, piloting, battleDamage);
                    } else if (event.getDescription().startsWith("MWUSERP")) {
                        client.rewardPointsDialog();
                    } else if (event.getDescription().startsWith("MWREG")) {
                        client.getMainFrame().jMenuFileRegister_actionPerformed();
                    } else if (event.getDescription().startsWith("JUMPTOPLANET")) {
                        String command = event.getDescription();
                        command = command.substring(12);
                        StringTokenizer ST = new StringTokenizer(command, "#");
                        String planetName = ST.nextToken();

                        //fetch the map
                        InnerStellarMap map = client.getMainFrame().getMainPanel().getMapPanel().getMap();

                        //get the planet
                        Planet currPlanet = client.getData().getPlanetByName(planetName);

                        if (currPlanet != null) {
                            map.setSelectedPlanet(currPlanet);
                            map.activate(currPlanet, true);
                            map.saveMapSelection(currPlanet);

                            /*
                             * If the map is visible and we're supposed to jump to it, get the
                             * main panel and call .selectMapTab(). selectMap will check where
                             * the map is (top or bottom) and send the correct tab to the front.
                             */
                            if (client.getConfig().isParam("MAPTABONCLICK") &&
                                      client.getConfig().isParam("MAPTABVISIBLE")) {
                                client.getMainFrame().getMainPanel().selectMapTab();
                            }
                        }
                    } else if (event.getDescription().startsWith("MWDEFECTDLG")) {
                        String command = event.getDescription();
                        command = command.substring(11);//strip command code

                        //open a warning dialog
                        Object[] options = { " Defect ", " Cancel " };
                        int confirmed = javax.swing.JOptionPane.showOptionDialog(client.getMainFrame(),
                              "Are you SURE you want to defect?",
                              "Defection Confirmation",
                              javax.swing.JOptionPane.DEFAULT_OPTION,
                              javax.swing.JOptionPane.WARNING_MESSAGE,
                              null,
                              options,
                              options[1]);

                        //if confirmed, send CONFIRM command
                        if (confirmed == javax.swing.JOptionPane.OK_OPTION) {client.sendChat(command);}
                    } else if (event.getDescription().startsWith("MWSOLDEFECT")) {
                        client.getMainFrame().jMenuCommanderDefect_actionPerformed();
                    } else if (event.getDescription().startsWith("REMOVEQUEUEDWORKORDER")) {
                        String command = event.getDescription();
                        StringTokenizer ST = new StringTokenizer(command, "|");
                        ST.nextToken();//strip command code
                        int tech = Integer.parseInt(ST.nextToken());
                        String position = ST.nextToken();

                        client.getRMT().removeWorkOrder(tech, position);

                    } else if (event.getDescription().startsWith("REMOVESALVAGEQUEUEDWORKORDER")) {
                        String command = event.getDescription();
                        StringTokenizer ST = new StringTokenizer(command, "|");
                        ST.nextToken();//strip command code
                        int tech = Integer.parseInt(ST.nextToken());
                        String position = ST.nextToken();

                        client.getSMT().removeWorkOrder(tech, position);

                    } else {
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            try {
                                Desktop.getDesktop().browse(event.getURL().toURI());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            } catch (URISyntaxException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                } catch (Throwable t) {
                    MWLogger.errLog((Exception) t);
                }
            }
        }
    }
}
