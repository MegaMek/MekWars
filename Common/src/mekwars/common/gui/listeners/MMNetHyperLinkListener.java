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
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.Planet;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.InnerStellarMap;
import mekwars.common.gui.panels.CHSPanel;

public class MMNetHyperLinkListener implements HyperlinkListener {
    private static final MMLogger LOGGER = MMLogger.create(MMNetHyperLinkListener.class);
    private final IClient client;
    protected boolean isHovering = false;
    protected String tooltip = null;
    protected CHSPanel hsPanel = null;

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
        hsPanel = panel;
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
        return tooltip;
    }

    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            isHovering = true;

            try {
                tooltip = (String) event.getSourceElement()
                                         .getAttributes()
                                         .getAttribute(HTML.getAttributeKey("alt"));
                LOGGER.info(tooltip);
                if (hsPanel != null) {
                    hsPanel.setInfoText(tooltip);
                }
            } catch (Exception ex) {
                LOGGER.error(ex, "Hyperlink Error: {}", ex.getLocalizedMessage());
            }
        } else if (event.getEventType() == HyperlinkEvent.EventType.EXITED) {
            isHovering = false;

            if (hsPanel != null) {
                hsPanel.setInfoText("");
            }

            tooltip = null;
        }


        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            JEditorPane pane = (JEditorPane) event.getSource();
            if (event instanceof HTMLFrameHyperlinkEvent htmlFrameHyperlinkEvent) {
                HTMLDocument doc = (HTMLDocument) pane.getDocument();
                doc.processHTMLFrameHyperlinkEvent(htmlFrameHyperlinkEvent);
            } else {
                if (event.getDescription().startsWith("MEKWARS")) {
                    String command = event.getDescription();
                    command = command.substring(7);
                    client.sendChat(command);
                } else if (event.getDescription().startsWith("MEK_MAIL")) {
                    String command = event.getDescription();
                    command = command.substring(7);
                    StringTokenizer commandStr = new StringTokenizer(command, "*");
                    command = String.format("%s, %s", commandStr.nextToken(), commandStr.nextToken());
                    LOGGER.debug(String.format("Command %s", command));
                    client.sendChat(String.format("/mail %s", command));
                } else if (event.getDescription().startsWith("MEK_INFO")) {
                    String command = event.getDescription();
                    command = command.substring(7);
                    StringTokenizer stringTokenizer = new StringTokenizer(command, "#");
                    String filename = stringTokenizer.nextToken().replace("%22", "\"");

                    int BV = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                    int gunnery = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                    int piloting = MathUtility.parseInt(stringTokenizer.nextToken(), 0);

                    String battleDamage = "";
                    if (stringTokenizer.hasMoreTokens()) {
                        battleDamage = stringTokenizer.nextToken();
                    }

                    client.getMainFrame()
                          .getMainPanel()
                          .getHSPanel()
                          .showInfoWindow(filename, BV, gunnery, piloting, battleDamage);
                } else if (event.getDescription().startsWith("MW_USER_P")) {
                    client.rewardPointsDialog();
                } else if (event.getDescription().startsWith("MW_REG")) {
                    client.getMainFrame().jMenuFileRegister_actionPerformed();
                } else if (event.getDescription().startsWith("JUMP_TO_PLANET")) {
                    String command = event.getDescription();
                    command = command.substring(12);
                    StringTokenizer stringTokenizer = new StringTokenizer(command, "#");
                    String planetName = stringTokenizer.nextToken();

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
                        if (client.getConfig().isParam("MAP_TAB_ON_CLICK") &&
                                  client.getConfig().isParam("MAP_TAB_VISIBLE")) {
                            client.getMainFrame().getMainPanel().selectMapTab();
                        }
                    }
                } else if (event.getDescription().startsWith("MW_DEFECT_DIALOG")) {
                    String command = event.getDescription();
                    command = command.substring(11);//strip command code

                    //open a warning dialog
                    Object[] options = { " Defect ", " Cancel " };
                    int confirmed = JOptionPane.showOptionDialog(client.getMainFrame(),
                          "Are you SURE you want to defect?",
                          "Defection Confirmation",
                          JOptionPane.DEFAULT_OPTION,
                          JOptionPane.WARNING_MESSAGE,
                          null,
                          options,
                          options[1]);

                    //if confirmed, send CONFIRM command
                    if (confirmed == JOptionPane.OK_OPTION) {
                        client.sendChat(command);
                    }

                } else if (event.getDescription().startsWith("MW_SOL_DEFECT")) {
                    client.getMainFrame().jMenuCommanderDefect_actionPerformed();
                } else if (event.getDescription().startsWith("REMOVE_QUEUED_WORK_ORDER")) {
                    String command = event.getDescription();
                    StringTokenizer stringTokenizer = new StringTokenizer(command, "|");
                    stringTokenizer.nextToken();//strip command code
                    int tech = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                    String position = stringTokenizer.nextToken();

                    client.getRMT().removeWorkOrder(tech, position);
                } else if (event.getDescription().startsWith("REMOVE_SALVAGE_QUEUED_WORK_ORDER")) {
                    String command = event.getDescription();
                    StringTokenizer stringTokenizer = new StringTokenizer(command, "|");
                    stringTokenizer.nextToken();//strip command code
                    int tech = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                    String position = stringTokenizer.nextToken();

                    client.getSMT().removeWorkOrder(tech, position);
                } else {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        try {
                            Desktop.getDesktop().browse(event.getURL().toURI());
                        } catch (IOException | URISyntaxException e) {
                            LOGGER.error(e, "Unable to browse to URL: {}", event.getURL().toExternalForm());
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }
}
