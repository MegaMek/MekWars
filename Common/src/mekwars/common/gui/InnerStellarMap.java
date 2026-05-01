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

package mekwars.common.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import com.thoughtworks.xstream.io.xml.DomDriver;
import mekwars.common.House;
import mekwars.common.Influences;
import mekwars.common.Planet;
import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.dialogs.PlanetSearchDialog;
import mekwars.common.gui.panels.CMapPanel;
import mekwars.common.util.MMNetXStream;
import mekwars.common.util.MWLogger;
import mekwars.common.util.Position;
import mekwars.common.util.StringUtils;

/**
 * Draws the main map component.
 *
 * @author Imi
 */

public class InnerStellarMap extends JComponent
      implements MouseListener, MouseMotionListener, MouseWheelListener, ActionListener {

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
            java.io.File dir = new java.io.File(client.getCacheDir());
            if (!dir.exists()) {
                dir.mkdirs();
            }
            conf = (InnerStellarMapConfig) xml.fromXML(new FileReader(STR."\{client.getCacheDir()}/mapconf.xml"));
            if (conf.getDisplay().length != displayStr.length) {
                throw new RuntimeException("not my file");
            }
        } catch (Exception e) {
            MWLogger.errLog(e);
            MWLogger.infoLog("could not read map config file. Will use defaults");
            conf = new InnerStellarMapConfig();
        }

        try {
            parseOverlayFile();
        } catch (Exception e) {
            MWLogger.errLog(e);
            MWLogger.infoLog("could not read map overlay file.");
        }

        for (int i = 0; i < displayStr.length; ++i) {
            display[i] = new JCheckBoxMenuItem(displayStr[i], conf.getDisplay()[i]);
            display[i].addActionListener(this);
        }

        // read in map filter settings
        StringTokenizer tokenizer = new StringTokenizer(this.client.getConfigParam("MAPFILTER1"), "$");
        int currFilter = FILTER_ALL;
        while (tokenizer.hasMoreElements() || currFilter < filterStr.length) {
            String nextToken = tokenizer.nextToken();

            if (currFilter == FILTER_SEP) {
                currFilter++;
                continue;
            }

            if (nextToken != null) {
                boolean filterState = Boolean.parseBoolean(nextToken);
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
                } catch (InterruptedException e) {
                }
                blinkPhase = !blinkPhase;

                if (!changesSinceLastRefresh.isEmpty()) {
                    mapPanel.repaint();
                }
            }
        }).start();

        // restore previous zoom level
        double storedValue = Double.parseDouble(client.getConfigParam("MAPZOOMLEVEL"));

        if (storedValue != 0) {
            conf.setScale(storedValue);
        }

        // restore previous offset
        int storedXOffset = Integer.parseInt(client.getConfigParam("MAPXOFFSET"));
        int storedYOffset = Integer.parseInt(client.getConfigParam("MAPYOFFSET"));
        conf.setOffset(new Point(storedXOffset, storedYOffset));

        // restore previously selected planet
        String storedPlanetName = client.getConfigParam("SELECTEDPLANET");
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
        String color = client.getConfigParam("MAPOVERLAYCOLOR");
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
            new MMNetXStream().toXML(conf, new FileWriter(STR."\{client.getCacheDir()}/mapconf.xml"));
        } catch (IOException e1) {
            MWLogger.errLog(e1);
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
            conf.getOffset().setLocation(-planet.getPosition().x * conf.getScale(), planet.getPosition().y *
                                                                                          conf.getScale());
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
        client.getConfig().setParam("SELECTEDPLANET", planet.getName());
        client.getConfig().setParam("MAPZOOMLEVEL", STR."\{conf.getScale()}");
        client.getConfig().setParam("MAPYOFFSET", STR."\{(int) conf.getOffset().getY()}");
        client.getConfig().setParam("MAPXOFFSET", STR."\{(int) conf.getOffset().getX()}");

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
            info.addActionListener(_ -> {
                JEditorPane label;
                if (Boolean.parseBoolean(client.getServerConfigs("UseStaticMaps"))) {
                    House house = client.getData().getHouseByName(mapPanel.getPPanel().getPlanet().getOriginalOwner());
                    String color = client.getServerConfigs("DisputedPlanetColor");
                    String name = "None";

                    if (house != null) {
                        color = house.getHouseColor();
                        name = house.getName();
                    }

                    label = new JEditorPane("text/html",
                          STR."<html>\{mapPanel.getPPanel()
                                             .getPlanet()
                                             .getAdvanceDescription(client.getUser(client.getUsername())
                                                                          .getUserLevel())}<b>Original Owner:</b><br><font color=\{color}>\{name}</font></html>");
                    label.setEditable(false);
                    label.setCaretPosition(0);
                    label.setPreferredSize(new java.awt.Dimension(500, 400));
                    JOptionPane.showMessageDialog(
                          InnerStellarMap.this,
                          new JScrollPane(label),
                          STR."Information for \{mapPanel.getPPanel().getPlanet().getName()}",
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
                          STR."<html>\{mapPanel.getPPanel()
                                             .getPlanet()
                                             .getLongDescription(true)}<b>Original Owner:</b><br><font color=\{color}>\{name}</font></html>");
                    // client.getData().getHouseByName("hi").getName();
                    label.setEditable(false);
                    label.setCaretPosition(0);
                    label.setPreferredSize(new java.awt.Dimension(500, 400));
                    JOptionPane.showMessageDialog(
                          InnerStellarMap.this,
                          new JScrollPane(label),
                          STR."Information for \{mapPanel.getPPanel().getPlanet().getName()}",
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
            javax.swing.JMenuItem search = new javax.swing.JMenuItem("Find Planet");
            search.addActionListener(_ -> createPlanetSearchDialog());
            popup.add(search);

            // CENTER Menu.
            javax.swing.JMenu centerM = new javax.swing.JMenu("Center Map");
            javax.swing.JMenuItem item = new javax.swing.JMenuItem("On Selected Planet");
            if (planet != null) {// only add if there is a planet to center on
                item.addActionListener(_ -> {
                    conf.getOffset().setLocation(-planet.getPosition().x * conf.getScale(),
                          planet.getPosition().y * conf.getScale());
                    mapPanel.repaint();
                });
                centerM.add(item);
            }

            item = new javax.swing.JMenuItem("On Natural Center");
            item.addActionListener(_ -> {
                conf.setOffset(new Point());
                conf.setScale(1);
                mapPanel.getSlider().setValue((int) Math.round(50 / conf.getScale()));
                mapPanel.repaint();
            });
            centerM.add(item);
            popup.add(centerM);

            // DISPLAY options
            javax.swing.JMenu displayMenu = new javax.swing.JMenu("Display");
            popup.add(displayMenu);
            for (int i = 0; i < displayStr.length; ++i) {
                displayMenu.add(display[i]);
            }

            // FILTER options
            javax.swing.JMenu filterMenu = new javax.swing.JMenu("Filter");
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
                item = new javax.swing.JMenuItem("Purchase Factory");
                item.addActionListener(_ -> client.getMainFrame()
                                                  .jMenuLeaderPurchaseFactory_actionPerformed(planet == null ?
                                                                                                    null :
                                                                                                    planet.getName()));
                popup.add(item);
            }

            popup.addSeparator();

            // REFRESH - one button
            item = new javax.swing.JMenuItem("Refresh");
            item.addActionListener(_ -> {
                changesSinceLastRefresh.clear();
                client.refreshData();
                mapPanel.repaint();
            });
            popup.add(item);

            if (client.isMod()) {

                try {

                    java.io.File loadJar = new java.io.File("./MekWarsAdmin.jar");
                    if (!loadJar.exists()) {
                        MWLogger.errLog("AdminMapPopupMenu creation skipped. No MekWarsAdmin.jar present.");
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
                    MWLogger.errLog("AdminMapPopupMenu creation FAILED!");
                    MWLogger.errLog(ex);
                }
            }

            popup.show(this, e.getX() + 10, e.getY() + 10);
        }

        // normal left click
        else if (e.getButton() == MouseEvent.BUTTON1) {

            if (e.getClickCount() >= 2) {

                javax.swing.JEditorPane label;
                if (Boolean.parseBoolean(client.getServerConfigs("UseStaticMaps"))) {
                    House h = client.getData().getHouseByName(mapPanel.getPPanel().getPlanet().getOriginalOwner());
                    String color = client.getServerConfigs("DisputedPlanetColor");
                    String name = "None";

                    if (h != null) {
                        color = h.getHouseColor();
                        name = h.getName();
                    }

                    label = new JEditorPane("text/html",
                          STR."<html>\{mapPanel.getPPanel()
                                             .getPlanet()
                                             .getAdvanceDescription(client.getUser(client.getUsername())
                                                                          .getUserLevel())}<b>Original Owner:</b><br><font color=\{color}>\{name}</font></html>");
                    label.setEditable(false);
                    label.setCaretPosition(0);
                    label.setPreferredSize(new Dimension(500, 400));
                    JOptionPane.showMessageDialog(
                          InnerStellarMap.this,
                          new JScrollPane(label),
                          STR."Information for \{mapPanel.getPPanel().getPlanet().getName()}",
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
                          "<html>" +
                                mapPanel.getPPanel().getPlanet().getLongDescription(true) +
                                "<b>Original Owner:</b><br><font color=" +
                                color +
                                ">" +
                                name +
                                "</font>" +
                                "</html>");
                    // client.getData().getHouseByName("hi").getName();
                    label.setEditable(false);
                    label.setCaretPosition(0);
                    label.setPreferredSize(new java.awt.Dimension(500, 400));
                    javax.swing.JOptionPane.showMessageDialog(
                          mekwars.common.gui.InnerStellarMap.this,
                          new javax.swing.JScrollPane(label),
                          "Information for " + mapPanel.getPPanel().getPlanet().getName(),
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

    public void mousePressed(java.awt.event.MouseEvent e) {
        mouseMod = e.getButton();
        if (e.getButton() != java.awt.event.MouseEvent.BUTTON1) {
            return;
        }
        selectedPlanet = nearestNeighbour(scr2mapX(e.getX()), scr2mapY(e.getY()));
        activate(selectedPlanet);
    }

    public void mouseReleased(java.awt.event.MouseEvent e) {
        mouseMod = 0;
    }

    public void mouseEntered(java.awt.event.MouseEvent e) {
        // mp.requestFocus();
        lastMousePos = new java.awt.Point(e.getX(), e.getY());
    }

    public void mouseExited(java.awt.event.MouseEvent e) {
        lastMousePos = null;
    }

    /**
     * Actually does the drawing of the map.
     */
    @Override
    public void paint(java.awt.Graphics g) {
        java.util.Collection<Planet> planets = mapPanel.getData().getAllPlanets();
        // background
        g.setColor(StringUtils.html2Color(conf.getBackgroundColor()));
        g.fillRect(0, 0, getWidth(), getHeight());
        int size = (int) Math.round(Math.max(5, Math.log(conf.getScale()) * 15 + 5));
        size = Math.clamp(size, conf.getMinDotSize(), conf.getMaxDotSize());

        if (conf.getDisplay()[DISPLAY_OVERLAY] && overlayLines != null) {
            for (java.util.ArrayList<Position> points : overlayLines) {
                Position last = null;
                for (Position p : points) {
                    if (last != null) {
                        g.setColor(StringUtils.html2Color(p.getColor()));
                        g.drawLine(map2scrX(last.x), map2scrY(last.y), map2scrX(p.x), map2scrY(p.y));
                    }
                    last = p;
                }
            }
        } else {
            try {
                int x = client.getConfig().getIntParam("MAPIMAGEX");
                int y = client.getConfig().getIntParam("MAPIMAGEY");
                int height = (int) (client.getConfig().getIntParam("MAPIMAGEHEIGHT") * conf.getScale());
                int width = (int) (client.getConfig().getIntParam("MAPIMAGEWIDTH") * conf.getScale());
                javax.swing.ImageIcon ic;

                boolean useJPGImage = new java.io.File("data/images/mekwarsmap.jpg").exists();
                if (useJPGImage) {
                    ic = new javax.swing.ImageIcon("data/images/mekwarsmap.jpg");
                } else {
                    ic = new javax.swing.ImageIcon("data/images/mekwarsmap.gif");
                }

                g.drawImage(ic.getImage(), map2scrX(x), map2scrY(y), width, height, ic.getImageObserver());

            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }

        }

        // For each through the collection of Planets
        for (Planet p : planets) {

            /*
             * check the visibility of the planet. TODO: Checking on every pait is slow. We should cache the visibility data in a map of some kind and clear it on refresh or viewing option change.
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

            if (Boolean.parseBoolean(client.getConfigParam("DARKERMAP"))) {
                try {
                    white = StringUtils.html2Color(houseColor);
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                    MWLogger.errLog(STR."Bad House for planet: \{p.getName()}");
                }
            } else {
                try {
                    white = adjustColor(StringUtils.html2Color(houseColor));
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                    MWLogger.errLog(STR."Bad House for planet: \{p.getName()}");
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
                g.setColor(java.awt.Color.WHITE);
                g.fillArc(x - 2, y - 2, size + 4, size + 4, 0, 360);
            }

            // planet dot
            int dotSize = size;
            boolean blink = false;
            if (conf.getDisplay()[DISPLAY_LAST_CHANGED] &&
                      blinkPhase &&
                      changesSinceLastRefresh.containsKey(p.getId())) {
                g.setColor(java.awt.Color.WHITE);
                dotSize++;
                blink = true;
            } else {
                g.setColor(white);
            }
            if (size < 3) {
                g.fillRect(x, y, dotSize, dotSize);
            } else {
                g.fillArc(x, y, dotSize, dotSize, 0, 360);
            }
            // names
            if (!blink) {
                g.setColor(white);
            }
            if (conf.getDisplay()[DISPLAY_NAMES] &&
                      (conf.getShowPlanetNamesThreshold() == 0 ||
                             conf.getScale() > conf.getShowPlanetNamesThreshold())) {
                g.drawString(p.getName(), x + size, y);
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
                        g.setColor(factionColor);
                        g.fillRect(x - 10, y + size + pos, 10, flu);
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
                    javax.swing.ImageIcon starIcon = iconCache.get("data/images/star.gif");
                    starIcon.paintIcon(this, g, x + size + pos, y + size / 2);
                }
            }

            // warehouses
            if (conf.getDisplay()[DISPLAY_WAREHOUSES] && p.getBaysProvided() > 0) {
                g.setColor(java.awt.Color.WHITE);
                g.drawString(Integer.toString(p.getBaysProvided()), x - 8, y);
            }

            if (p.isHomeWorld()) {
                javax.swing.ImageIcon homeicon = iconCache.get("data/images/homeworld.gif");
                homeicon.paintIcon(this, g, x, y);
            }
        }

        try {

            /*
             * Draw Range Circles. This is not so simple as it was with Tasks.
             */

            if (conf.getDisplay()[DISPLAY_RANGES]) {

                // determine which ops the player is eligible for
                java.util.TreeSet<String> legalOps = new java.util.TreeSet<>();
                for (CArmy currA : client.getPlayer().getArmies()) {
                    legalOps.addAll(currA.getLegalOperations());
                }

                // loop and draw
                for (String typeName : legalOps) {

                    String[] vals = client.getAllOps().get(typeName);
                    double range = Double.parseDouble(vals[0]);

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

                        java.awt.Color c = StringUtils.html2Color(vals[1]);

                        g.setColor(c);
                        int rSize = (int) Math.round(2 * range * conf.getScale());
                        g.drawArc(x - rSize / 2, y - rSize / 2, rSize, rSize, 0, 360);
                    }

                }
            }// end if(should display)

        } catch (Exception ex) {
            MWLogger.errLog(ex);
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
    public java.awt.Color adjustColor(java.awt.Color c) {
        return new java.awt.Color(adj(c.getRed()), adj(c.getGreen()), adj(c.getBlue()));
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

    public void mouseDragged(java.awt.event.MouseEvent e) {
        if (mouseMod != java.awt.event.MouseEvent.BUTTON3) {
            return;
        }
        if (lastMousePos != null) {
            conf.getOffset().x -= lastMousePos.x - e.getX();
            conf.getOffset().y -= lastMousePos.y - e.getY();
        }
        mouseMoved(e);
        mapPanel.repaint();
    }

    public void mouseMoved(java.awt.event.MouseEvent e) {

        if (lastMousePos == null) {
            lastMousePos = new java.awt.Point(e.getX(), e.getY());
        } else {
            lastMousePos.x = e.getX();
            lastMousePos.y = e.getY();
        }

        if (conf.getDisplay()[DISPLAY_TOOLTIPS]) {

            Planet planet = nearestNeighbour(scr2mapX(e.getX()), scr2mapY(e.getY()));
            StringBuilder result = new StringBuilder(STR."<html><center><b><u>\{planet.getName()}</b></u></center>");
            result.append("<TABLE CELLPADDING=1 CELLSPACING=1>");

            for (House h : planet.getInfluence().getHouses()) {
                String color = client.getServerConfigs("DisputedPlanetColor");
                String name = "None";
                int id = -1;

                if (h != null) {
                    color = h.getHouseColor();
                    name = h.getName();
                    id = h.getId();
                }

                result.append(STR."<TR><TD><font color=\{color}>\{name}</font></TD><TD>\{Math.floor((double) (100 *
                                                                                                                    planet.getInfluence()
                                                                                                                          .getInfluence(
                                                                                                                                id)) /
                                                                                                          planet.getConquestPoints())}%</TD></TR>");
            }
            result.append("</TABLE></html>");

            setToolTipText(result.toString());
        } else {
            setToolTipText(null);
        }
    }

    /** Handle the key pressed event from the text field. */
    public void keyPressed(java.awt.event.KeyEvent e) {
        int keyCode = e.getKeyCode();

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

    public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
        mapPanel.getSlider().setValue(mapPanel.getSlider().getValue() + e.getWheelRotation() * 3);
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
    public void setSelectedPlanet(Planet p) {
        selectedPlanet = p;
    }

    /**
     * The event listener for all the display options...
     *
     * @see ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(java.awt.event.ActionEvent e) {

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

        client.getConfig().setParam("MAPFILTER", filterString.toString());
        client.getConfig().saveConfig();
    }

    /**
     * Solves events of data fetches by adding the changes to the current change set.
     */
    public void dataFetched(java.util.Map<Integer, Influences> changes) {
        for (int id : changes.keySet()) {
            try {
                Influences addinf = changes.get(id);
                Influences oldinf = changesSinceLastRefresh.get(id);
                if (oldinf != null) {
                    addinf.add(oldinf);
                }
                changesSinceLastRefresh.put(id, addinf);
                if (mapPanel != null) {
                    mapPanel.repaint();
                }
            } catch (Exception ex) {
                MWLogger.errLog("Error with Planet: " + client.getData().getPlanet(id).getName());
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
