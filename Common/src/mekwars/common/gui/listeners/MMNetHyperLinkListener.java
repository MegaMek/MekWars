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

/**
 * Shared {@link HyperlinkListener} attached to HTML-rendering panes throughout the MekWars client (chat
 * tabs, the hangar/shop panel's info pane, etc.). Beyond showing tooltips for hovered links, its main
 * job is interpreting a set of MekWars-specific pseudo-protocol commands embedded as {@code href}
 * values (e.g. {@code MEKWARS...}, {@code MEK_MAIL...}, {@code JUMP_TO_PLANET...}) so that clicking
 * specially-crafted links in generated HTML can trigger client actions (sending chat commands, opening
 * dialogs, jumping the star map, etc.) rather than navigating like an ordinary hyperlink. Links that
 * don't match any recognized pseudo-command fall back to being opened in the system's default browser.
 */
public class MMNetHyperLinkListener implements HyperlinkListener {
    private static final MMLogger LOGGER = MMLogger.create(MMNetHyperLinkListener.class);
    private final IClient client;
    /** True while the mouse is currently hovering over a hyperlink; toggled by {@link #hyperlinkUpdate}. */
    protected boolean isHovering = false;
    /** Text of the last hovered link's HTML {@code alt} attribute, or {@code null} when not hovering. */
    protected String tooltip = null;
    /** When set (via the two-arg constructor), hover text is also pushed into this panel's info line. */
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

    /**
     * @param client used to send chat/command strings triggered by clicked pseudo-links
     * @param panel a {@link CHSPanel} whose info/status line should be updated with hover tooltip text
     */
    public MMNetHyperLinkListener(IClient client, CHSPanel panel) {
        this.client = client;
        hsPanel = panel;
    }

    /**
     * @return true if the mouse is currently positioned over a hyperlink in the observed pane
     */
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

    /**
     * Central handler for all hyperlink events from the observed pane(s):
     * <ul>
     *     <li>{@code ENTERED} — marks {@link #isHovering} true, reads the hovered element's HTML
     *         {@code alt} attribute into {@link #tooltip} (any failure, e.g. a missing attribute, is
     *         caught and merely logged), and mirrors it into {@link #hsPanel}'s info line if set.</li>
     *     <li>{@code EXITED} — clears {@link #isHovering}, {@link #tooltip}, and {@link #hsPanel}'s info line.</li>
     *     <li>{@code ACTIVATED} (click) — for links inside an HTML {@code <frame>}, delegates to
     *         {@link HTMLDocument#processHTMLFrameHyperlinkEvent} per normal Swing multi-frame handling.
     *         Otherwise the link's description (its {@code href} value) is matched by prefix against a
     *         fixed set of MekWars pseudo-commands (see below); anything not matching is treated as an
     *         ordinary URL and opened in the system's default browser via {@link Desktop#browse}.</li>
     * </ul>
     *
     * <p><b>Recognized {@code ACTIVATED} prefixes</b> (in match order): {@code MEKWARS} sends the
     * remainder of the description directly as a chat/command string; {@code MEK_MAIL} builds and sends
     * a {@code /mail <recipient>, <message>} command; {@code MEK_INFO} opens a unit info window with a
     * BV/gunnery/piloting/(optional) battle-damage breakdown; {@code MW_USER_P} opens the reward-points
     * dialog; {@code MW_REG} opens the registration menu action; {@code JUMP_TO_PLANET} re-centers and
     * selects a named planet on the star map (and optionally switches to the map tab); {@code
     * MW_DEFECT_DIALOG} shows a yes/cancel confirmation dialog and, if confirmed, sends the embedded
     * command; {@code MW_SOL_DEFECT} triggers the commander-defect menu action; {@code
     * REMOVE_QUEUED_WORK_ORDER}/{@code REMOVE_SALVAGE_QUEUED_WORK_ORDER} remove a queued repair/salvage
     * work order for a given tech and position.
     *
     * <p><b>Bug:</b> several of the branches below strip their pseudo-command prefix with a hard-coded
     * {@code substring(N)} where {@code N} does not actually match the prefix's length, leaving a
     * leftover fragment of the prefix glued onto the real payload. Concretely: {@code "MEK_MAIL"} (8
     * chars) uses {@code substring(7)}, leaving a leading {@code "L"}; {@code "MEK_INFO"} (8 chars) also
     * uses {@code substring(7)}, leaving a leading {@code "O"}; {@code "JUMP_TO_PLANET"} (14 chars) uses
     * {@code substring(12)}, leaving a leading {@code "ET"}; and {@code "MW_DEFECT_DIALOG"} (16 chars)
     * uses {@code substring(11)}, leaving a leading {@code "IALOG"}. In each case the corrupted leading
     * characters end up prepended to the first token parsed out afterwards (mail recipient, info-window
     * filename, planet name, or defect command), which is likely to break that feature in practice. Only
     * the plain {@code "MEKWARS"} branch (7-char prefix, {@code substring(7)}) strips correctly; the two
     * {@code REMOVE_*_WORK_ORDER} branches sidestep the issue entirely by discarding the whole first
     * {@code "|"}-delimited token instead of using a raw {@code substring} offset. This is documented as
     * observed behavior only — it is not fixed here.
     */
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
                    // BUG: "MEK_MAIL" is 8 characters but only 7 are stripped here, leaving a leading "L".
                    command = command.substring(7);
                    StringTokenizer commandStr = new StringTokenizer(command, "*");
                    command = String.format("%s, %s", commandStr.nextToken(), commandStr.nextToken());
                    LOGGER.debug(String.format("Command %s", command));
                    client.sendChat(String.format("/mail %s", command));
                } else if (event.getDescription().startsWith("MEK_INFO")) {
                    String command = event.getDescription();
                    // BUG: "MEK_INFO" is 8 characters but only 7 are stripped here, leaving a leading "O".
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
                    // BUG: "JUMP_TO_PLANET" is 14 characters but only 12 are stripped here, leaving a leading "ET".
                    command = command.substring(12);
                    StringTokenizer stringTokenizer = new StringTokenizer(command, "#");
                    String planetName = stringTokenizer.nextToken();

                    //fetch the map
                    InnerStellarMap map = client.getMainFrame().getMainPanel().getMapPanel().getMap();

                    //get the planet
                    Planet currPlanet = client.getData().getPlanetByName(planetName);

                    // Selecting/activating/jumping only happens if the (possibly corrupted, see above) name
                    // resolves to a known planet; if it doesn't, the click silently does nothing.
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
                    // BUG: "MW_DEFECT_DIALOG" is 16 characters but only 11 are stripped here, leaving a leading "IALOG".
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
                    // Unlike the substring-based branches above, this correctly discards the whole
                    // leading "|"-delimited command-code token rather than guessing a character offset.
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
                    // Fallback: anything not matching a recognized MekWars pseudo-command is treated as
                    // an ordinary external URL and opened in the OS default browser, if supported.
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        try {
                            Desktop.getDesktop().browse(event.getURL().toURI());
                        } catch (IOException | URISyntaxException e) {
                            // Logged here AND rethrown as an unchecked exception, so the failure is
                            // reported twice: once via the logger, and again wherever this propagates to
                            // (there is no surrounding catch, so it will surface via the AWT event
                            // dispatch thread's default uncaught-exception handling).
                            LOGGER.error(e, "Unable to browse to URL: {}", event.getURL().toExternalForm());
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }
}
