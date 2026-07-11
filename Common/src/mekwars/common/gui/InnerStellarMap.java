/*
 * Copyright (C) 2004 Helge Richter (McWizard)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekwars.common.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serial;
import java.io.StreamTokenizer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import com.thoughtworks.xstream.io.xml.DomDriver;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.House;
import mekwars.common.Influences;
import mekwars.common.Planet;
import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.dialogs.PlanetSearchDialog;
import mekwars.common.gui.panels.CMapPanel;
import mekwars.common.util.MMNetXStream;
import mekwars.common.util.Position;
import mekwars.common.util.StringUtils;

/**
 * Draws the main map component.
 *
 * @author Imi
 */

public class InnerStellarMap extends JComponent
      implements MouseListener, MouseMotionListener, MouseWheelListener, ActionListener {

    private static final MMLogger LOGGER = MMLogger.create(InnerStellarMap.class);

    @Serial
    private static final long serialVersionUID = 8655078955521790260L;
    private static final String[] displayStr = { "Planet Names", "Planet Control", "Factories", "Warehouses",
                                                 "Attack Ranges", "Recent Changes", "Overlay", "Tooltips" };
    private static final int DISPLAY_NAMES = 0;
    private static final int DISPLAY_INFLUENCE = 1;
    private static final int DISPLAY_UNITS = 2;
    private static final int DISPLAY_WAREHOUSES = 3;
    private static final int DISPLAY_RANGES = 4;
    private static final int DISPLAY_LAST_CHANGED = 5;
    private static final int DISPLAY_OVERLAY = 6;
    private static final int DISPLAY_TOOLTIPS = 7;
    private static final String[] filterStr = { "All", "", "Factories", "Facilities", "Faction", "Disputed",
                                                "Contested" };
    private static final int FILTER_ALL = 0;
    private static final int FILTER_SEP = 1;
    private static final int FILTER_FACTORIES = 2;
    private static final int FILTER_FACILITIES = 3;
    private static final int FILTER_FACTION = 4;
    private static final int FILTER_DISPUTED = 5;
    private static final int FILTER_CONTESTED = 6;
    private final CMapPanel mapPanel;
    /**
     * The main client to access
     */
    private final IClient client;
    private final IconProvider iconCache = new IconProvider();
    /**
     * Various display options
     *
     * @see InnerStellarMapConfig
     */
    private final JCheckBoxMenuItem[] display = new JCheckBoxMenuItem[displayStr.length];
    private final JCheckBoxMenuItem[] filter = new JCheckBoxMenuItem[filterStr.length];
    /**
     * A data structure to hold all planets marked as "changed" since last update. - see
     * common.CampaignData.decodeMutablePlanets()
     */
    private final Map<Integer, Influences> changesSinceLastRefresh;
    /**
     * The current configuration & filtration options.
     */
    InnerStellarMapConfig conf = new InnerStellarMapConfig();
    ArrayList<ArrayList<Position>> overlayLines = new ArrayList<>();
    /**
     * Map filtering options ; ALL, _FILLER_, Factories, Facilities, Disputed, Contested
     */
    boolean[] filterSettings = new boolean[] { true, false, true, true, true, true, true };
    Point lastMousePos = null;
    int mouseMod = 0;
    private Planet selectedPlanet = null;
    /**
     * Used to indicate the blinking of a planet. If true, the planets are drawn white.
     */
    private boolean blinkPhase = false;

    /**
     * Constructs the ISMap.
     *
     * @param panel - The panel it belongs to.
     */
    public InnerStellarMap(CMapPanel panel, IClient client, CMainFrame mainFrame) {
        this.client = client;
        setBackground(java.awt.Color.BLACK);
        mapPanel = panel;
        setOpaque(true);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);

        MMNetXStream xml = new MMNetXStream(new DomDriver());

        try {
            File dir = new File(client.getCacheDir());
            if (!dir.exists() && dir.mkdirs()) {
                LOGGER.info("Made cache dir...");
            }

            conf = (InnerStellarMapConfig) xml.fromXML(new FileReader(String.format("%s/mapconf.xml", client.getCacheDir())));

            if (conf.getDisplay().length != displayStr.length) {
                conf = new InnerStellarMapConfig();
            }
        } catch (Exception e) {
            LOGGER.debug(e, "Unable to load map config file.. {}", e.getLocalizedMessage());
            conf = new InnerStellarMapConfig();
        }

        try {
            parseOverlayFile();
        } catch (Exception e) {
            LOGGER.error(e, "Could not read map overlay file: {}", e.getLocalizedMessage());
        }

        for (int i = 0; i < displayStr.length; ++i) {
            display[i] = new JCheckBoxMenuItem(displayStr[i], conf.getDisplay()[i]);
            display[i].addActionListener(this);
        }

        // read in map filter settings
        StringTokenizer tokenizer = new StringTokenizer(this.client.getConfigParam("MAP_FILTER_1"), "$");
        int currFilter = FILTER_ALL;
        while (tokenizer.hasMoreElements() || currFilter < filterStr.length) {
            String nextToken = tokenizer.nextToken();

            if (currFilter == FILTER_SEP) {
                currFilter++;
                continue;
            }

            if (nextToken != null) {
                boolean filterState = MathUtility.parseBoolean(nextToken, false);
                filterSettings[currFilter] = filterState;
                filter[currFilter] = new JCheckBoxMenuItem(filterStr[currFilter], filterState);
                filter[currFilter].addActionListener(this);
            }

            // null. default to true and add.
            else {
                filterSettings[currFilter] = true;
                filter[currFilter] = new JCheckBoxMenuItem(filterStr[currFilter], true);
                filter[currFilter].addActionListener(this);
            }

            currFilter++;
        }

        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                processTick();
            }
        });

        changesSinceLastRefresh = client.getChangesSinceLastRefresh();
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    // Thread interruption...
                }

                blinkPhase = !blinkPhase;

                if (!changesSinceLastRefresh.isEmpty()) {
                    mapPanel.repaint();
                }
            }
        }).start();

        // restore previous zoom level
        double storedValue = Double.parseDouble(client.getConfigParam("MAP_ZOOM_LEVEL"));

        if (storedValue != 0) {
            conf.setScale(storedValue);
        }

        // restore previous offset
        int storedXOffset = Integer.parseInt(client.getConfigParam("MAP_X_OFFSET"));
        int storedYOffset = Integer.parseInt(client.getConfigParam("MAP_Y_OFFSET"));
        conf.setOffset(new Point(storedXOffset, storedYOffset));

        // restore previously selected planet
        String storedPlanetName = client.getConfigParam("SELECTED_PLANET");
        if (storedPlanetName != null && !storedPlanetName.trim().isEmpty()) {
            // planet setting exists. lets see if the planet does ...
            Planet currPlan = client.getData().getPlanetByName(storedPlanetName);
            if (currPlan != null) {
                this.activate(currPlan, false);
            }
        }

    }

    private void parseOverlayFile() throws Exception {
        File file = new File("data/mapoverlay.txt");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        StreamTokenizer streamTokenizer = new StreamTokenizer(bufferedReader);
        streamTokenizer.eolIsSignificant(true);
        streamTokenizer.commentChar('#');
        ArrayList<Position> line = new ArrayList<>();
        Position position;
        String color = client.getConfigParam("MAP_OVERLAY_COLOR");

        while (streamTokenizer.nextToken() != StreamTokenizer.TT_EOF) {
            if (streamTokenizer.ttype == StreamTokenizer.TT_WORD &&
                      streamTokenizer.sval.equals("LINE") &&
                      !line.isEmpty()) {
                overlayLines.add(line);
                line = new ArrayList<>();
            } else if (streamTokenizer.ttype == StreamTokenizer.TT_WORD && streamTokenizer.sval.startsWith("COLOR")) {
                color = streamTokenizer.sval.substring("COLOR".length());
            } else if (streamTokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                double x = streamTokenizer.nval;

                if (streamTokenizer.nextToken() == StreamTokenizer.TT_NUMBER) {
                    position = new Position(x, streamTokenizer.nval);
                    position.setColor(color);
                    line.add(position);
                }

                while (streamTokenizer.ttype != StreamTokenizer.TT_EOF &&
                             streamTokenizer.ttype != StreamTokenizer.TT_EOL) {
                    streamTokenizer.nextToken();
                }
            }
        }

        if (!line.isEmpty()) {
            overlayLines.add(line);
        }
    }

    /**
     * At each tick, save the config file... (I just needed a time to do this)
     */
    public void processTick() {
        try {
            new MMNetXStream().toXML(conf, new FileWriter(String.format("%s/mapconf.xml", client.getCacheDir())));
        } catch (IOException e1) {
            LOGGER.error(e1, "Error loading map conf... {}", e1.getLocalizedMessage());
        }
    }

    /**
     * Activate and Center
     */
    public void activate(Planet planet, boolean center) {

        if (planet == null) {
            return;
        }

        // activate normally
        this.activate(planet);

        // then center on the world
        if (center) {
            conf.getOffset()
                  .setLocation(-planet.getPosition().x * conf.getScale(), planet.getPosition().y * conf.getScale());
        }

    }// end activate(planet,center)

    /**
     * Activate a specific planet
     *
     * @param planet This planet becomes the selected one.
     */
    public void activate(Planet planet) {

        if (planet == null) {
            return;
        }

        if (mapPanel.getPPanel() != null && mapPanel.getPPanel().getPlanet() != planet) {

            mapPanel.getPPanel().update(planet);
            conf.setPlanetID(planet.getId());
            mapPanel.repaint();

            saveMapSelection(planet);
        }
    }

    /**
     * Method which saves current map properties. Called when a new planet is selected. The settings are restored when a
     * client is loaded, preserving map selections between user sessions.
     */
    public void saveMapSelection(Planet planet) {

        // save the config
        client.getConfig().setParam("SELECTED_PLANET", planet.getName());
        client.getConfig().setParam("MAP_ZOOM_LEVEL", String.format("%s", conf.getScale()));
        client.getConfig().setParam("MAP_Y_OFFSET", String.format("%s", (int) conf.getOffset().getY()));
        client.getConfig().setParam("MAP_X_OFFSET", String.format("%s", (int) conf.getOffset().getX()));

        client.getConfig().saveConfig();
        client.setConfig();

    }

    public InnerStellarMapConfig getConf() {
        return conf;
    }

    /**
     * Method which returns the panel underlying the map. This is used by the AdminMapPopupMenu class in admin package.
     */
    public CMapPanel getMapPanel() {
        return mapPanel;
    }

    /**
     * If right button clicked, open the popup menu
     *
     * @see MouseListener#mouseClicked(MouseEvent)
     */
    public void mouseClicked(MouseEvent e) {

        if (e.getButton() == MouseEvent.BUTTON3) {

            // popup menu
            final Planet planet = nearestNeighbour(scr2mapX(e.getX()), scr2mapY(e.getY()));

            if (planet != null) {
                activate(planet);
            }

            JPopupMenu popup = new JPopupMenu();

            // Information
            JMenuItem info = new JMenuItem("Information");
            info.addActionListener(actionEvent -> {
                JEditorPane label;
                if (MathUtility.parseBoolean(client.getServerConfigs("UseStaticMaps"), false)) {
                    House house = client.getData().getHouseByName(mapPanel.getPPanel().getPlanet().getOriginalOwner());
                    String color = client.getServerConfigs("DisputedPlanetColor");
                    String name = "None";

                    if (house != null) {
                        color = house.getHouseColor();
                        name = house.getName();
                    }

                    label = new JEditorPane("text/html",
                          String.format("<html>%s<b>Original Owner:</b><br><font color=%s>%s</font></html>", mapPanel.getPPanel()
                                             .getPlanet()
                                             .getAdvanceDescription(client.getUser(client.getUsername())
                                                                          .getUserLevel()), color, name));
                    label.setEditable(false);
                    label.setCaretPosition(0);
                    label.setPreferredSize(new java.awt.Dimension(500, 400));
                    JOptionPane.showMessageDialog(
                          InnerStellarMap.this,
                          new JScrollPane(label),
                          String.format("Information for %s", mapPanel.getPPanel().getPlanet().getName()),
                          JOptionPane.INFORMATION_MESSAGE);
                } else {
                    House houseByName = client.getData()
                                              .getHouseByName(mapPanel.getPPanel().getPlanet().getOriginalOwner());
                    String color = client.getServerConfigs("DisputedPlanetColor");
                    String name = "None";

                    if (houseByName != null) {
                        color = houseByName.getHouseColor();
                        name = houseByName.getName();
                    }

                    label = new JEditorPane("text/html",
                          String.format("<html>%s<b>Original Owner:</b><br><font color=%s>%s</font></html>", mapPanel.getPPanel()
                                             .getPlanet()
                                             .getLongDescription(true), color, name));
                    // client.getData().getHouseByName("hi").getName();
                    label.setEditable(false);
                    label.setCaretPosition(0);
                    label.setPreferredSize(new java.awt.Dimension(500, 400));
                    JOptionPane.showMessageDialog(
                          InnerStellarMap.this,
                          new JScrollPane(label),
                          String.format("Information for %s", mapPanel.getPPanel().getPlanet().getName()),
                          javax.swing.JOptionPane.INFORMATION_MESSAGE);
                }
            });
            popup.add(info);

            /*
             * If a planet is selected and the player is active, build an attack menu and include it in the popup.
             */
            if (planet != null) {
                AttackMenu aMenu = new AttackMenu(client, -1, planet.getName());
                aMenu.updateMenuItems(false);
                popup.add(aMenu);
            }

            // Separator!
            popup.addSeparator();

            // Search, using planet dialog.
            JMenuItem search = new JMenuItem("Find Planet");
            search.addActionListener(actionEvent -> createPlanetSearchDialog());
            popup.add(search);

            // CENTER Menu.
            JMenu centerM = new JMenu("Center Map");
            JMenuItem item = new JMenuItem("On Selected Planet");

            if (planet != null) {// only add if there is a planet to center on
                item.addActionListener(actionEvent -> {
                    conf.getOffset().setLocation(-planet.getPosition().x * conf.getScale(),
                          planet.getPosition().y * conf.getScale());
                    mapPanel.repaint();
                });

                centerM.add(item);
            }

            item = new JMenuItem("On Natural Center");
            item.addActionListener(actionEvent -> {
                conf.setOffset(new Point());
                conf.setScale(1);
                mapPanel.getSlider().setValue((int) Math.round(50 / conf.getScale()));
                mapPanel.repaint();
            });
            centerM.add(item);
            popup.add(centerM);

            // DISPLAY options
            JMenu displayMenu = new JMenu("Display");
            popup.add(displayMenu);

            for (int i = 0; i < displayStr.length; ++i) {
                displayMenu.add(display[i]);
            }

            // FILTER options
            JMenu filterMenu = new JMenu("Filter");
            popup.add(filterMenu);

            for (int i = 0; i < filterStr.length; i++) {

                if (i == FILTER_SEP) {
                    filterMenu.addSeparator();
                    continue;
                }

                filterMenu.add(filter[i]);
            }

            if (client.isLeader() &&
                      client.getUserLevel() >= client.getData().getAccessLevel("PurchaseFactory")) {
                item = new JMenuItem("Purchase Factory");
                item.addActionListener(actionEvent -> client.getMainFrame()
                                                  .jMenuLeaderPurchaseFactory_actionPerformed(planet == null ?
                                                                                                    null :
                                                                                                    planet.getName()));
                popup.add(item);
            }

            popup.addSeparator();

            // REFRESH - one button
            item = new JMenuItem("Refresh");
            item.addActionListener(actionEvent -> {
                changesSinceLastRefresh.clear();
                client.refreshData();
                mapPanel.repaint();
            });
            popup.add(item);

            if (client.isMod()) {

                try {

                    File loadJar = new File("./MekWarsAdmin.jar");
                    if (!loadJar.exists()) {
                        LOGGER.debug("AdminMapPopupMenu creation skipped. No MekWarsAdmin.jar present.");
                    } else {
                        URLClassLoader loader = new URLClassLoader(new URL[] { loadJar.toURI().toURL() });
                        Class<?> clazz = loader.loadClass("admin.AdminMapPopupMenu");
                        Object object = clazz.getDeclaredConstructor().newInstance();
                        clazz.getDeclaredMethod("createMenu",
                                    new Class[] { IClient.class, mekwars.common.gui.InnerStellarMap.class,
                                                  Integer.class, Integer.class, Planet.class })
                              .invoke(object,
                                    client, this, (int) scr2mapX(e.getX()), (int) scr2mapY(e.getY()),
                                    mapPanel.getPPanel().getPlanet());
                        popup.add((javax.swing.JMenu) object);
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex, "AdminMapPopupMenu creation FAILED!");
                }
            }

            popup.show(this, e.getX() + 10, e.getY() + 10);
        } else if (e.getButton() == MouseEvent.BUTTON1) {
            if (e.getClickCount() >= 2) {
                JEditorPane label;
                if (Boolean.parseBoolean(client.getServerConfigs("UseStaticMaps"))) {
                    House h = client.getData().getHouseByName(mapPanel.getPPanel().getPlanet().getOriginalOwner());
                    String color = client.getServerConfigs("DisputedPlanetColor");
                    String name = "None";

                    if (h != null) {
                        color = h.getHouseColor();
                        name = h.getName();
                    }

                    label = new JEditorPane("text/html",
                          String.format("<html>%s<b>Original Owner:</b><br><font color=%s>%s</font></html>", mapPanel.getPPanel()
                                             .getPlanet()
                                             .getAdvanceDescription(client.getUser(client.getUsername())
                                                                          .getUserLevel()), color, name));
                    label.setEditable(false);
                    label.setCaretPosition(0);
                    label.setPreferredSize(new Dimension(500, 400));
                    JOptionPane.showMessageDialog(
                          InnerStellarMap.this,
                          new JScrollPane(label),
                          String.format("Information for %s", mapPanel.getPPanel().getPlanet().getName()),
                          JOptionPane.INFORMATION_MESSAGE);
                } else {
                    House houseByName = client.getData()
                                              .getHouseByName(mapPanel.getPPanel().getPlanet().getOriginalOwner());
                    String color = client.getServerConfigs("DisputedPlanetColor");
                    String name = "None";

                    if (houseByName != null) {
                        color = houseByName.getHouseColor();
                        name = houseByName.getName();
                    }

                    label = new javax.swing.JEditorPane("text/html",
                          String.format("<html>%s<b>Original Owner:</b><br><font color=%s>%s</font></html>", mapPanel.getPPanel()
                                             .getPlanet()
                                             .getLongDescription(true), color, name));
                    // client.getData().getHouseByName("hi").getName();
                    label.setEditable(false);
                    label.setCaretPosition(0);
                    label.setPreferredSize(new java.awt.Dimension(500, 400));
                    javax.swing.JOptionPane.showMessageDialog(
                          mekwars.common.gui.InnerStellarMap.this,
                          new javax.swing.JScrollPane(label),
                          String.format("Information for %s", mapPanel.getPPanel().getPlanet().getName()),
                          javax.swing.JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    /**
     * Calculate the nearest neighbour for the given point If anyone has a better algorithm than this stupid kind of
     * shit, please, feel free to exchange my brute force thing... An good idea would be an voronoi diagram and the
     * sweep algorithm from Steven Fortune.
     */
    private Planet nearestNeighbour(double x, double y) {
        Iterator<Planet> it = mapPanel.getData().getAllPlanets().iterator();
        double minDiff = Double.MAX_VALUE;
        double diff;
        Planet minPlanet = null;

        while (it.hasNext()) {
            Planet p = it.next();
            diff = p.getPosition().distanceSq(x, y);
            if (diff < minDiff && planetIsVisible(p)) {
                minDiff = diff;
                minPlanet = p;
            }
        }

        return minPlanet;
    }

    /**
     * Computes the map-coordinate from the screen coordinate system
     */
    private double scr2mapX(int x) {
        return Math.round((x - (double) getWidth() / 2 - conf.getOffset().x) / conf.getScale());
    }

    private double scr2mapY(int y) {
        return Math.round(((double) getHeight() / 2 - (y - conf.getOffset().y)) / conf.getScale());
    }

    /*
     * Called from action listeners, or stand alone from a button on a non-map panel.
     */
    public void createPlanetSearchDialog() {
        PlanetSearchDialog searchDialog = new PlanetSearchDialog(this, client);
        searchDialog.setVisible(true);
    }

    /**
     * Utility method that checks the visibility of a given planet.
     *
     */
    private boolean planetIsVisible(Planet planet) {

        if (null == planet) {
            return false;
        }

        // first, make sure its not "All"
        if (filterSettings[FILTER_ALL]) {
            return true;
        }

        /*
         * Not showing all, so do check all relevant server options and determine whether this particular world should be visible @ this time.
         */
        if (filterSettings[FILTER_FACTORIES] && planet.getFactoryCount() > 0) {
            return true;
        }

        if (filterSettings[FILTER_FACILITIES] && planet.getBaysProvided() > 0) {
            return true;
        }

        if (filterSettings[FILTER_DISPUTED]) {

            Integer houseID = planet.getInfluence().getOwner();

            // no owner means disputed
            if (houseID == null) {
                return true;
            }

            // there's a high ID, but it doesn't own enough of the world to be undisputed
            if (planet.getInfluence().getInfluence(houseID) < client.getMinPlanetOwnerShip(planet)) {
                return true;
            }
        }

        if (filterSettings[FILTER_CONTESTED] && planet.getInfluence().getHouses().size() > 1) {
            return true;
        }

        return filterSettings[FILTER_FACTION] &&
                     planet.getInfluence().getInfluence(client.getPlayer().getMyHouse().getId()) > 0;

        // no qualifiers. we shouldn't see the world.
    }

    public void mousePressed(MouseEvent mouseEvent) {
        mouseMod = mouseEvent.getButton();

        if (mouseEvent.getButton() != MouseEvent.BUTTON1) {
            return;
        }

        selectedPlanet = nearestNeighbour(scr2mapX(mouseEvent.getX()), scr2mapY(mouseEvent.getY()));
        activate(selectedPlanet);
    }

    public void mouseReleased(MouseEvent mouseEvent) {
        mouseMod = 0;
    }

    public void mouseEntered(MouseEvent mouseEvent) {
        // mp.requestFocus();
        lastMousePos = new Point(mouseEvent.getX(), mouseEvent.getY());
    }

    public void mouseExited(MouseEvent mouseEvent) {
        lastMousePos = null;
    }

    /**
     * Actually does the drawing of the map.
     */
    @Override
    public void paint(Graphics graphics) {
        Collection<Planet> planets = mapPanel.getData().getAllPlanets();
        // background
        graphics.setColor(StringUtils.html2Color(conf.getBackgroundColor()));
        graphics.fillRect(0, 0, getWidth(), getHeight());
        int size = (int) Math.round(Math.max(5, Math.log(conf.getScale()) * 15 + 5));
        size = Math.clamp(size, conf.getMinDotSize(), conf.getMaxDotSize());

        if (conf.getDisplay()[DISPLAY_OVERLAY] && overlayLines != null) {
            for (java.util.ArrayList<Position> points : overlayLines) {
                Position last = null;
                for (Position p : points) {
                    if (last != null) {
                        graphics.setColor(StringUtils.html2Color(p.getColor()));
                        graphics.drawLine(map2scrX(last.x), map2scrY(last.y), map2scrX(p.x), map2scrY(p.y));
                    }
                    last = p;
                }
            }
        } else {
            try {
                int x = client.getConfig().getIntParam("MAP_IMAGE_X");
                int y = client.getConfig().getIntParam("MAP_IMAGE_Y");
                int height = (int) (client.getConfig().getIntParam("MAP_IMAGE_HEIGHT") * conf.getScale());
                int width = (int) (client.getConfig().getIntParam("MAP_IMAGE_WIDTH") * conf.getScale());
                ImageIcon ic;

                boolean useJPGImage = new File("data/images/mekwarsmap.jpg").exists();

                if (useJPGImage) {
                    ic = new ImageIcon("data/images/mekwarsmap.jpg");
                } else {
                    ic = new ImageIcon("data/images/mekwarsmap.gif");
                }

                graphics.drawImage(ic.getImage(), map2scrX(x), map2scrY(y), width, height, ic.getImageObserver());
            } catch (Exception ex) {
                LOGGER.error(ex, "Unable to load image: {}", ex.getLocalizedMessage());
            }

        }

        // For each through the collection of Planets
        for (Planet p : planets) {

            /*
             * check the visibility of the planet.
             *
             * TODO: Checking on every part is slow. We should cache the visibility data in a map of some kind and
             *  clear it on refresh or viewing option change.
             */
            if (!planetIsVisible(p)) {
                continue;
            }

            // calculate the color of the faction owner
            Integer houseID = p.getInfluence().getOwner();
            String houseColor;

            if (houseID == null ||
                      houseID == -1 ||
                      p.getInfluence().getInfluence(houseID) < client.getMinPlanetOwnerShip(p)) {
                houseColor = client.getServerConfigs("DisputedPlanetColor");
            } else {
                houseColor = client.getData().getHouse(houseID).getHouseColor();
            }

            Color white = Color.WHITE;

            if (MathUtility.parseBoolean(client.getConfigParam("DARKER_MAP"), false)) {
                try {
                    white = StringUtils.html2Color(houseColor);
                } catch (Exception ex) {
                    LOGGER.error(ex, String.format("Bad House for planet: %s", p.getName()));
                }
            } else {
                try {
                    white = adjustColor(StringUtils.html2Color(houseColor));
                } catch (Exception ex) {
                    LOGGER.error(ex, String.format("Bad House for planet: %s", p.getName()));
                }
            }

            if ((white.getRed() == 0 && white.getBlue() == 0)) {
                white = java.awt.Color.white;
            }

            // calculate the current screen position
            int x = map2scrX(p.getPosition().x) - size / 2;
            int y = map2scrY(p.getPosition().y) - size / 2;

            if (mapPanel.getPPanel() != null &&
                      mapPanel.getPPanel().getPlanet() != null &&
                      mapPanel.getPPanel().getPlanet().equals(p)) {
                graphics.setColor(java.awt.Color.WHITE);
                graphics.fillArc(x - 2, y - 2, size + 4, size + 4, 0, 360);
            }

            // planet dot
            int dotSize = size;
            boolean blink = false;

            if (conf.getDisplay()[DISPLAY_LAST_CHANGED] &&
                      blinkPhase &&
                      changesSinceLastRefresh.containsKey(p.getId())) {
                graphics.setColor(java.awt.Color.WHITE);
                dotSize++;
                blink = true;
            } else {
                graphics.setColor(white);
            }

            if (size < 3) {
                graphics.fillRect(x, y, dotSize, dotSize);
            } else {
                graphics.fillArc(x, y, dotSize, dotSize, 0, 360);
            }
            // names
            if (!blink) {
                graphics.setColor(white);
            }
            if (conf.getDisplay()[DISPLAY_NAMES] &&
                      (conf.getShowPlanetNamesThreshold() == 0 ||
                             conf.getScale() > conf.getShowPlanetNamesThreshold())) {
                graphics.drawString(p.getName(), x + size, y);
            }

            // influence icon
            if (conf.getDisplay()[DISPLAY_INFLUENCE] &&
                      (conf.getShowInfluenceThreshold() == 0 || conf.getScale() > conf.getShowInfluenceThreshold())) {
                int pos = 0;

                for (House h : p.getInfluence().getHouses()) {
                    String color = client.getServerConfigs("DisputedPlanetColor");
                    int id = -1;

                    if (h != null) {
                        color = h.getHouseColor();
                        id = h.getId();
                    }

                    int flu = p.getInfluence().getInfluence(id) / 10;

                    if (flu != 10) {
                        Color factionColor = StringUtils.html2Color(color);
                        graphics.setColor(factionColor);
                        graphics.fillRect(x - 10, y + size + pos, 10, flu);
                        pos += flu;
                    }
                }
            }

            // unit factories
            if (conf.getDisplay()[DISPLAY_UNITS] &&
                      (conf.getShowUnitFactoriesThreshold() == 0 || conf.getScale() >
                                                                          conf.getShowUnitFactoriesThreshold())) {
                int pos = 0;

                if (p.getFactoryCount() > 0) {
                    ImageIcon starIcon = iconCache.get("data/images/star.gif");
                    starIcon.paintIcon(this, graphics, x + size + pos, y + size / 2);
                }
            }

            // warehouses
            if (conf.getDisplay()[DISPLAY_WAREHOUSES] && p.getBaysProvided() > 0) {
                graphics.setColor(java.awt.Color.WHITE);
                graphics.drawString(Integer.toString(p.getBaysProvided()), x - 8, y);
            }

            if (p.isHomeWorld()) {
                ImageIcon imageIcon = iconCache.get("data/images/homeworld.gif");
                imageIcon.paintIcon(this, graphics, x, y);
            }
        }

        try {

            /*
             * Draw Range Circles. This is not so simple as it was with Tasks.
             */

            if (conf.getDisplay()[DISPLAY_RANGES]) {

                // determine which ops the player is eligible for
                TreeSet<String> legalOps = new TreeSet<>();

                for (CArmy currA : client.getPlayer().getArmies()) {
                    legalOps.addAll(currA.getLegalOperations());
                }

                // loop and draw
                for (String typeName : legalOps) {
                    String[] vals = client.getAllOps().get(typeName);
                    double range = MathUtility.parseDouble(vals[0], 0.0);

                    // don't draw stupidly large ranges.
                    if (range < 2000) {
                        Planet p = mapPanel.getPPanel().getPlanet();

                        /*
                         * TODO: Replace the constant visibility checks with a map. See earlier comment.
                         */
                        if (!planetIsVisible(p)) {
                            continue;
                        }

                        int x = map2scrX(p.getPosition().x);
                        int y = map2scrY(p.getPosition().y);

                        Color html2Color = StringUtils.html2Color(vals[1]);

                        graphics.setColor(html2Color);
                        int rSize = (int) Math.round(2 * range * conf.getScale());
                        graphics.drawArc(x - rSize / 2, y - rSize / 2, rSize, rSize, 0, 360);
                    }

                }
            }// end if(should display)

        } catch (Exception ex) {
            LOGGER.error(ex, "Paint error: {}", ex.getLocalizedMessage());
        }

    }

    private int map2scrX(double x) {
        return (int) Math.round((double) getWidth() / 2 + x * conf.getScale()) + conf.getOffset().x;
    }

    private int map2scrY(double y) {
        return (int) Math.round((double) getHeight() / 2 - y * conf.getScale()) + conf.getOffset().y;
    }

    /**
     * Adjust the color according to the current colorAdjustment...
     */
    public Color adjustColor(Color color) {
        return new Color(adj(color.getRed()), adj(color.getGreen()), adj(color.getBlue()));
    }

    /**
     * What we NOT want, is to wash out the color tone by adding simple gray to the color. I preferre the code from
     * Color.brighter() which simple looks good. (But it had to be adjusted a bit) Imi
     */
    private int adj(int r) {
        if (conf.getColorAdjustment() == 0) {
            return r;
        }
        if (conf.getColorAdjustment() == 1) {
            return 255;
        }

        int i = (int) (1.0 / conf.getColorAdjustment());

        if (r > 0 && r < i) {
            r = i;
        }

        return Math.min((int) (r / (1 - conf.getColorAdjustment())), 255);
    }

    public void mouseDragged(MouseEvent mouseEvent) {
        if (mouseMod != java.awt.event.MouseEvent.BUTTON3) {
            return;
        }

        if (lastMousePos != null) {
            conf.getOffset().x -= lastMousePos.x - mouseEvent.getX();
            conf.getOffset().y -= lastMousePos.y - mouseEvent.getY();
        }

        mouseMoved(mouseEvent);
        mapPanel.repaint();
    }

    public void mouseMoved(MouseEvent mouseEvent) {

        if (lastMousePos == null) {
            lastMousePos = new Point(mouseEvent.getX(), mouseEvent.getY());
        } else {
            lastMousePos.x = mouseEvent.getX();
            lastMousePos.y = mouseEvent.getY();
        }

        if (conf.getDisplay()[DISPLAY_TOOLTIPS]) {

            Planet planet = nearestNeighbour(scr2mapX(mouseEvent.getX()), scr2mapY(mouseEvent.getY()));
            StringBuilder result = new StringBuilder(String.format("<html><center><b><u>%s</b></u></center>", planet.getName()));
            result.append("<TABLE CELLPADDING=1 CELLSPACING=1>");

            for (House house : planet.getInfluence().getHouses()) {
                String color = client.getServerConfigs("DisputedPlanetColor");
                String name = "None";
                int id = -1;

                if (house != null) {
                    color = house.getHouseColor();
                    name = house.getName();
                    id = house.getId();
                }

                result.append(String.format("<TR><TD><font color=%s>%s</font></TD><TD>%s%%</TD></TR>", color, name, Math.floor((double) (100 *
                                                                                                                    planet.getInfluence()
                                                                                                                          .getInfluence(
                                                                                                                                id)) /
                                                                                                          planet.getConquestPoints())));
            }
            result.append("</TABLE></html>");

            setToolTipText(result.toString());
        } else {
            setToolTipText(null);
        }
    }

    /** Handle the key pressed event from the text field. */
    public void keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();

        if (keyCode == 37)// left arrow
        {
            conf.getOffset().y -= (int) conf.getScale();
        } else if (keyCode == 38) // uparrow
        {
            conf.getOffset().x -= (int) conf.getScale();
        } else if (keyCode == 39)// right arrow
        {
            conf.getOffset().y += (int) conf.getScale();
        } else if (keyCode == 40)// down arrow
        {
            conf.getOffset().x += (int) conf.getScale();
        } else {
            return;
        }
        mapPanel.repaint();
    }

    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
        mapPanel.getSlider().setValue(mapPanel.getSlider().getValue() + mouseWheelEvent.getWheelRotation() * 3);
        if (selectedPlanet != null) {
            conf.getOffset().setLocation(-selectedPlanet.getPosition().x * conf.getScale(),
                  selectedPlanet.getPosition().y * conf.getScale());
            mapPanel.repaint();
        }

    }

    /**
     * @param scale The scale to set.
     */
    public void setScale(double scale) {
        conf.setScale(scale);
        if (selectedPlanet != null) {
            conf.getOffset().setLocation(-selectedPlanet.getPosition().x * conf.getScale(),
                  selectedPlanet.getPosition().y * conf.getScale());
        }
    }

    /**
     * @param off The conf.offset.x to set.
     */
    public void setXOff(int off) {
        conf.getOffset().x = off;
    }

    /**
     * @param off The conf.offset.y to set.
     */
    public void setYOff(int off) {
        conf.getOffset().y = off;
    }

    /**
     * Method to set the selected world. - Called by the HyperLinkListener
     */
    public void setSelectedPlanet(Planet planet) {
        selectedPlanet = planet;
    }

    /**
     * The event listener for all the display options...
     *
     * @see ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent actionEvent) {

        mapPanel.repaint();

        // save display settings
        for (int i = 0; i < displayStr.length; ++i) {
            conf.getDisplay()[i] = display[i].isSelected();
        }

        // save filter settings
        StringBuilder filterString = new StringBuilder();
        for (int i = 0; i < filterSettings.length; i++) {

            if (i == FILTER_SEP) {
                filterString.append("false$");
                continue;
            }

            filterSettings[i] = filter[i].isSelected();
            filterString.append(filterSettings[i]).append("$");
        }

        client.getConfig().setParam("MAP_FILTER", filterString.toString());
        client.getConfig().saveConfig();
    }

    /**
     * Solves events of data fetches by adding the changes to the current change set.
     */
    public void dataFetched(Map<Integer, Influences> changes) {
        for (int id : changes.keySet()) {
            try {
                Influences addInfluences = changes.get(id);
                Influences oldInfluences = changesSinceLastRefresh.get(id);

                if (oldInfluences != null) {
                    addInfluences.add(oldInfluences);
                }

                changesSinceLastRefresh.put(id, addInfluences);

                if (mapPanel != null) {
                    mapPanel.repaint();
                }
            } catch (Exception ex) {
                LOGGER.error(ex, "Error with Planet: {}", client.getData().getPlanet(id).getName());
            }
        }
    }

    /**
     * @return Returns the changesSinceLastRefresh.
     */
    public Map<Integer, Influences> getChangesSinceLastRefresh() {
        return changesSinceLastRefresh;
    }

    /**
     * A cache for image icons. key=filename(String), value=ImageIcon
     */
    private static class IconProvider extends TreeMap<String, ImageIcon> {
        /**
         *
         */
        @Serial
        private static final long serialVersionUID = 4594828039895948331L;

        public ImageIcon get(String key) {
            if (!containsKey(key)) {
                put(key, new ImageIcon(key));
            }
            return super.get(key);
        }
    }

}
