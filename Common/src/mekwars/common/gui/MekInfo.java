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

/*
 * MekInfo.java
 *
 * Created on June 14, 2002, 9:02 PM
 */

package mekwars.common.gui;


import java.io.Serial;

import megamek.client.ui.tileset.MekTileset;
import megamek.client.ui.util.RotateFilter;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import mekwars.common.Unit;
import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.IClientConfig;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.MWLogger;
import mekwars.common.util.UnitUtils;

/**
 *
 * @author Steve Hawkins
 */

public class MekInfo extends javax.swing.JPanel {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 4308503800966118202L;
    protected static MekTileset mt;
    private final javax.swing.JLabel lblName;
    IClient client = null;
    IClientConfig Config = null;
    javax.swing.ImageIcon previewIcon = null;
    CUnit cm = null;
    CArmy army = null;
    private javax.swing.JLabel lblImage = new javax.swing.JLabel();
    private int cellWidth = 86;

    /*
     * public void setBackground(Color color){ super.setBackground(color); try{
     * imagePanel.setBackground(color); }catch(Exception ex){} }
     */

    /**
     * Creates new general-purpose MekInfo.
     * <p>
     * Used to generate images in HQ, BM, etx.
     */
    public MekInfo(IClient client) {
        this.client = client;

        if (this.client != null) {
            Config = this.client.getConfig();
        }

        lblImage = new javax.swing.JLabel() {

            /**
             *
             */
            @Serial
            private static final long serialVersionUID = -114192798426952281L;

            @Override
            public void paint(java.awt.Graphics g) {

                // First draw the background image - tiled
                if (Config.isParam("UNITHEX")) {
                    javax.swing.ImageIcon image = new javax.swing.ImageIcon((new javax.swing.ImageIcon(
                          "data/images/hexes/boring/beige_plains_0.gif")).getImage()
                                                                                  .getScaledInstance(cellWidth,
                                                                                        getHeight(),
                                                                                        java.awt.Image.SCALE_DEFAULT));
                    g.drawImage(image.getImage(),
                          (getWidth() - image.getIconWidth()) / 2,
                          (getHeight() - image.getIconHeight()) / 2,
                          null,
                          null);
                }

                // Now let the regular paint code do it's work
                javax.swing.Icon icon = getIcon();
                icon.paintIcon(this,
                      g,
                      (getWidth() - icon.getIconWidth()) / 2,
                      (getHeight() - icon.getIconHeight()) / 2);

                if (MekInfo.this.client != null && MekInfo.this.client.getConfig().isUsingStatusIcons() && cm != null) {

                    int height = 0;
                    boolean dynamic = MekInfo.this.client.getConfig().isParam("LEFTCOLUMNDYNAMIC");
                    javax.swing.ImageIcon ic;
                    Entity m = cm.getEntity();

                    if (lblImage.isVisible() && (m instanceof Mek || m instanceof Tank)) {

                        boolean useAdvanceRepairs = MekInfo.this.client.isUsingAdvanceRepairs();

                        // Pilot Block
                        if (Config.isParam("LEFTPILOTEJECT")) {
                            if (cm.hasVacantPilot()) {
                                ic = new javax.swing.ImageIcon("data/images/status/nopilot.gif");

                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                                // pilotImage.setIcon(new
                                // ImageIcon("data/images/status/nopilot.gif"));
                            } else if (cm.getPilot().getHits() > 0) {
                                ic = new javax.swing.ImageIcon("data/images/status/wound.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else if (m instanceof Mek && ((Mek) m).isAutoEject()) {
                                if (!dynamic) {
                                    ic = new javax.swing.ImageIcon("data/images/status/eject.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            } else {
                                ic = new javax.swing.ImageIcon("data/images/status/noeject.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();

                            }
                        }

                        // Repairing status
                        if (Config.isParam("LEFTREPAIR")) {
                            if (useAdvanceRepairs) {
                                if (UnitUtils.isRepairing(cm.getEntity())) {
                                    ic = new javax.swing.ImageIcon("data/images/status/repairing.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                } else if (MekInfo.this.client.getRMT() != null &&
                                                 MekInfo.this.client.getRMT().hasQueuedOrders(cm.getId())) {
                                    ic = new javax.swing.ImageIcon("data/images/status/pending.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            } else {
                                if (cm.getStatus() == Unit.STATUS_UNMAINTAINED) {
                                    ic = new javax.swing.ImageIcon("data/images/status/unmaint.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                } else {
                                    if (!dynamic) {
                                        ic = new javax.swing.ImageIcon("data/images/status/maint.gif");
                                        g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                        height += ic.getIconHeight();
                                    }
                                }
                            }
                        }

                        // Engine Damage
                        if (Config.isParam("LEFTENGINE")) {
                            // Engine Block
                            if (UnitUtils.getNumberOfDamagedEngineCrits(m) >= 1) {
                                ic = new javax.swing.ImageIcon("data/images/status/engine.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new javax.swing.ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        // Equipiment/Crit Damage
                        if (Config.isParam("LEFTEQUIPMENT")) {
                            if (UnitUtils.hasCriticalDamage(m)) {
                                ic = new javax.swing.ImageIcon("data/images/status/critical.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new javax.swing.ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        // Armor/IS Damage
                        if (Config.isParam("LEFTARMOR")) {
                            if (UnitUtils.hasISDamage(m)) {
                                ic = new javax.swing.ImageIcon("data/images/status/structure.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else if (UnitUtils.hasArmorDamage(m)) {
                                ic = new javax.swing.ImageIcon("data/images/status/armor.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new javax.swing.ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        // ammo block
                        if (Config.isParam("LEFTAMMO")) {
                            if (UnitUtils.isAmmoless(m)) {
                                if (!dynamic) {
                                    ic = new javax.swing.ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            } else if (UnitUtils.hasEmptyAmmo(m)) {
                                ic = new javax.swing.ImageIcon("data/images/status/empty.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else if (UnitUtils.hasLowAmmo(m)) {
                                ic = new javax.swing.ImageIcon("data/images/status/low.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new javax.swing.ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        // commander block
                        if (Config.isParam("LEFTCOMMANDER") && army != null) {
                            if (army.isCommander(cm.getId())) {
                                ic = new javax.swing.ImageIcon("data/images/status/comm.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new javax.swing.ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(),
                                          cellWidth - ic.getIconWidth(),
                                          height,
                                          ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        height = 0;
                        // Screw it I can't find the width any other way.
                        // consecutive paints will fix the issue.
                        cellWidth = Math.min(cellWidth, getWidth());
                        dynamic = Config.isParam("RIGHTCOLUMNDYNAMIC");
                        // Pilot Block
                        if (Config.isParam("RIGHTPILOTEJECT")) {
                            if (cm.hasVacantPilot()) {
                                ic = new javax.swing.ImageIcon("data/images/status/nopilot.gif");

                                g.drawImage(ic.getImage(),
                                      cellWidth - ic.getIconWidth(),
                                      height,
                                      ic.getImageObserver());
                                height += ic.getIconHeight();
                                // pilotImage.setIcon(new
                                // ImageIcon("data/images/status/nopilot.gif"));
                            } else if (cm.getPilot().getHits() > 0) {
                                ic = new javax.swing.ImageIcon("data/images/status/wound.gif");
                                g.drawImage(ic.getImage(),
                                      cellWidth - ic.getIconWidth(),
                                      height,
                                      ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else if (m instanceof Mek && ((Mek) m).isAutoEject()) {
                                if (!dynamic) {
                                    ic = new javax.swing.ImageIcon("data/images/status/eject.gif");
                                    g.drawImage(ic.getImage(),
                                          cellWidth - ic.getIconWidth(),
                                          height,
                                          ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            } else {
                                ic = new javax.swing.ImageIcon("data/images/status/noeject.gif");
                                g.drawImage(ic.getImage(),
                                      cellWidth - ic.getIconWidth(),
                                      height,
                                      ic.getImageObserver());
                                height += ic.getIconHeight();

                            }
                        }

                        // Repairing status
                        if (Config.isParam("RIGHTREPAIR")) {
                            if (useAdvanceRepairs) {
                                if (UnitUtils.isRepairing(cm.getEntity())) {
                                    ic = new javax.swing.ImageIcon("data/images/status/repairing.gif");
                                    g.drawImage(ic.getImage(),
                                          cellWidth - ic.getIconWidth(),
                                          height,
                                          ic.getImageObserver());
                                    height += ic.getIconHeight();
                                } else if (MekInfo.this.client.getRMT() != null &&
                                                 MekInfo.this.client.getRMT().hasQueuedOrders(cm.getId())) {
                                    ic = new javax.swing.ImageIcon("data/images/status/pending.gif");
                                    g.drawImage(ic.getImage(),
                                          cellWidth - ic.getIconWidth(),
                                          height,
                                          ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            } else {
                                if (cm.getStatus() == Unit.STATUS_UNMAINTAINED) {
                                    ic = new javax.swing.ImageIcon("data/images/status/unmaint.gif");
                                    g.drawImage(ic.getImage(),
                                          cellWidth - ic.getIconWidth(),
                                          height,
                                          ic.getImageObserver());
                                    height += ic.getIconHeight();
                                } else {
                                    if (!dynamic) {
                                        ic = new javax.swing.ImageIcon("data/images/status/maint.gif");
                                        g.drawImage(ic.getImage(),
                                              cellWidth - ic.getIconWidth(),
                                              height,
                                              ic.getImageObserver());
                                        height += ic.getIconHeight();
                                    }
                                }
                            }
                        }

                        // Engine Damage
                        if (Config.isParam("RIGHTENGINE")) {
                            // Engine Block
                            if (UnitUtils.getNumberOfDamagedEngineCrits(m) >= 1) {
                                ic = new javax.swing.ImageIcon("data/images/status/engine.gif");
                                g.drawImage(ic.getImage(),
                                      cellWidth - ic.getIconWidth(),
                                      height,
                                      ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new javax.swing.ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(),
                                          cellWidth - ic.getIconWidth(),
                                          height,
                                          ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        // Equipiment/Crit Damage
                        if (Config.isParam("RIGHTEQUIPMENT")) {
                            if (UnitUtils.hasCriticalDamage(m)) {
                                ic = new javax.swing.ImageIcon("data/images/status/critical.gif");
                                g.drawImage(ic.getImage(),
                                      cellWidth - ic.getIconWidth(),
                                      height,
                                      ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new javax.swing.ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(),
                                          cellWidth - ic.getIconWidth(),
                                          height,
                                          ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        // Armor/IS Damage
                        if (Config.isParam("RIGHTARMOR")) {
                            if (UnitUtils.hasISDamage(m)) {
                                ic = new javax.swing.ImageIcon("data/images/status/structure.gif");
                                g.drawImage(ic.getImage(),
                                      cellWidth - ic.getIconWidth(),
                                      height,
                                      ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else if (UnitUtils.hasArmorDamage(m)) {
                                ic = new javax.swing.ImageIcon("data/images/status/armor.gif");
                                g.drawImage(ic.getImage(),
                                      cellWidth - ic.getIconWidth(),
                                      height,
                                      ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new javax.swing.ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(),
                                          cellWidth - ic.getIconWidth(),
                                          height,
                                          ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        // ammo block
                        if (Config.isParam("RIGHTAMMO")) {
                            if (UnitUtils.isAmmoless(m)) {
                                if (!dynamic) {
                                    ic = new javax.swing.ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(),
                                          cellWidth - ic.getIconWidth(),
                                          height,
                                          ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            } else if (UnitUtils.hasEmptyAmmo(m)) {
                                ic = new javax.swing.ImageIcon("data/images/status/empty.gif");
                                g.drawImage(ic.getImage(),
                                      cellWidth - ic.getIconWidth(),
                                      height,
                                      ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else if (UnitUtils.hasLowAmmo(m)) {
                                ic = new javax.swing.ImageIcon("data/images/status/low.gif");
                                g.drawImage(ic.getImage(),
                                      cellWidth - ic.getIconWidth(),
                                      height,
                                      ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new javax.swing.ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(),
                                          cellWidth - ic.getIconWidth(),
                                          height,
                                          ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }
                    }

                    // commander block
                    if (Config.isParam("RIGHTCOMMANDER") && army != null) {
                        if (army.isCommander(cm.getId())) {
                            ic = new javax.swing.ImageIcon("data/images/status/comm.gif");
                            g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                            height += ic.getIconHeight();
                        } else {
                            if (!dynamic) {
                                ic = new javax.swing.ImageIcon("data/images/status/blank.gif");
                                g.drawImage(ic.getImage(),
                                      cellWidth - ic.getIconWidth(),
                                      height,
                                      ic.getImageObserver());
                                height += ic.getIconHeight();
                            }
                        }
                    }

                    // setLeftStatusIcons(g, icon);
                    // setRightStatusIcons(g);
                }

                // super.paint(g);
            }
        };// end new JLabel(LBL Image)

        lblName = new javax.swing.JLabel();
        setLayout(new java.awt.GridBagLayout());

        lblImage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        add(lblImage, gridBagConstraints);

        lblName.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        add(lblName, gridBagConstraints);
    }

    /**
     * Creates new MekInfo for use in previews. Is passed a ficticious config which contains preview camo.
     * <p>
     * Used to generate images in HQ, BM, etc.
     */
    public MekInfo(javax.swing.ImageIcon preview) {

        // set the preview icon
        this.previewIcon = preview;
        Config = null;

        java.awt.GridBagConstraints gridBagConstraints;
        if (client != null) {
            Config = client.getConfig();
        }

        lblImage = new javax.swing.JLabel() {

            /**
             *
             */
            @Serial
            private static final long serialVersionUID = 639618470390199477L;

            @Override
            public void paint(java.awt.Graphics g) {
                // first draw the background image - tiled
                javax.swing.ImageIcon image = new javax.swing.ImageIcon((new javax.swing.ImageIcon(
                      "data/images/hexes/boring/beige_plains_0.gif")).getImage()
                                                                              .getScaledInstance(80,
                                                                                    68,
                                                                                    java.awt.Image.SCALE_DEFAULT));
                g.drawImage(image.getImage(),
                      (getWidth() - image.getIconWidth()) / 2,
                      (getHeight() - image.getIconHeight()) / 2,
                      null,
                      null);

                // Now let the regular paint code do it's work
                javax.swing.Icon icon = getIcon();
                icon.paintIcon(this,
                      g,
                      (getWidth() - icon.getIconWidth()) / 2,
                      (getHeight() - icon.getIconHeight()) / 2);
                // super.paint(g);
            }
        };// end new JLabel(LBL Image)

        lblName = new javax.swing.JLabel();
        setLayout(new java.awt.GridBagLayout());
        lblImage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(lblImage, gridBagConstraints);

        lblName.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(lblName, gridBagConstraints);
    }

    public void setText(String s) {
        lblName.setText(s);
    }

    public java.awt.Image getEmbeddedImage() {
        return ((javax.swing.ImageIcon) lblImage.getIcon()).getImage();
    }

    public void setPreviewIcon(javax.swing.ImageIcon preview) {
        previewIcon = preview;
    }

    public void setUnit(Entity m) {
        java.awt.Image unit;
        java.awt.Image camo = null;
        javax.swing.ImageIcon camoicon;
        this.cm = null;

        unit = getImageFor(m, lblImage).getScaledInstance(84, 72, java.awt.Image.SCALE_DEFAULT);

        // look for a config image to load. if no config exists,
        // try to load the preview icon.
        if (Config != null) {camoicon = Config.getImage("CAMO");} else {camoicon = previewIcon;}

        if (camoicon != null) {camo = camoicon.getImage();}

        mekwars.common.gui.MekInfo.EntityImage ei = new EntityImage(unit,
              0xFFFFFF,
              camo,
              this);
        setImage(ei.loadPreviewImage());

    }

    public static java.awt.Image getImageFor(Entity m, java.awt.Component component) {

        if (mt == null) {
            mt = new MekTileset(new java.io.File("data/images/units/"));
            try {
                mt.loadFromFile("mechset.txt");
            } catch (java.io.IOException ex) {
                MWLogger.errLog("Unable to read data/images/units/mechset.txt");
            }
        }// end if(null tileset)
        //@Salient - from what i can tell from the megamek code, passing in the component does nothing.
        return mt.imageFor(m, -1);
    }

    public void setImage(java.awt.Image img) {
        lblImage.setIcon(new javax.swing.ImageIcon(img.getScaledInstance(cellWidth, 74, java.awt.Image.SCALE_DEFAULT)));
    }

    public void setUnit(CUnit cm, CArmy army) {

        if (cm == null) {return;}

        this.cm = cm;
        this.army = army;
        java.awt.Image unit = null;
        java.awt.Image camo = null;
        javax.swing.ImageIcon camoicon;
        Entity m = cm.getEntity();

        try // @ salient, this should fix the gui problem.
        {
            unit = getImageFor(m, lblImage).getScaledInstance(84, 72, java.awt.Image.SCALE_DEFAULT);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            try {
                java.io.File pathToFile = new java.io.File("./data/images/ImageMissing.png");
                unit = javax.imageio.ImageIO.read(pathToFile);
                unit = unit.getScaledInstance(84, 72, java.awt.Image.SCALE_DEFAULT);
                MWLogger.errLog("incorrect image filename in mechset.txt for " +
                                      cm.getModelName() +
                                      " " +
                                      CUnit.getTypeClassDesc(cm.getType()));
            } catch (java.io.IOException ex2) {
                MWLogger.errLog("incorrect image filename in mechset.txt for " +
                                      cm.getModelName() +
                                      " " +
                                      CUnit.getTypeClassDesc(cm.getType()));
                MWLogger.errLog(ex2);
            }
        }

        // look for a config image to load. if no config exists,
        // try to load the preview icon.
        if (Config != null) {camoicon = Config.getImage("CAMO");} else {camoicon = previewIcon;}

        if (camoicon != null) {camo = camoicon.getImage();}

        mekwars.common.gui.MekInfo.EntityImage ei = new EntityImage(unit,
              0xFFFFFF,
              camo,
              this);
        setImage(ei.loadPreviewImage());

    }

    public void setImageVisible(boolean flag) {

        lblImage.setVisible(flag);
    }

    /**
     * A class to handle the image permutations for an entity (Code from megamek.common.TilesetManager class)
     */
    private static class EntityImage {
        private final int tint;
        private final java.awt.Image camo;
        private final java.awt.Image[] facings = new java.awt.Image[6];
        private final java.awt.Image[] wreckFacings = new java.awt.Image[6];
        private final java.awt.Component comp;
        private final int IMG_WIDTH = 84;
        private final int IMG_HEIGHT = 72;
        private final int IMG_SIZE = IMG_WIDTH * IMG_HEIGHT;
        private java.awt.Image base;
        private java.awt.Image wreck;
        private java.awt.Image icon;

        public EntityImage(java.awt.Image base, int tint, java.awt.Image camo, java.awt.Component comp) {
            this(base, null, tint, camo, comp);
        }

        public EntityImage(java.awt.Image base, java.awt.Image wreck, int tint, java.awt.Image camo,
              java.awt.Component comp) {
            this.base = base;
            this.tint = tint;
            this.camo = camo;
            this.comp = comp;
            this.wreck = wreck;
        }

        public void loadFacings() {
            base = applyColor(base);

            icon = base.getScaledInstance(56, 48, java.awt.Image.SCALE_SMOOTH);
            for (int i = 0; i < 6; i++) {
                java.awt.image.ImageProducer rotSource = new java.awt.image.FilteredImageSource(base.getSource(),
                      new RotateFilter((Math.PI / 3) * (6 - i)));
                facings[i] = comp.createImage(rotSource);
            }

            if (wreck != null) {
                wreck = applyColor(wreck);
                for (int i = 0; i < 6; i++) {
                    java.awt.image.ImageProducer rotSource = new java.awt.image.FilteredImageSource(wreck.getSource(),
                          new RotateFilter((Math.PI / 3) * (6 - i)));
                    wreckFacings[i] = comp.createImage(rotSource);
                }
            }
        }

        private java.awt.Image applyColor(java.awt.Image image) {
            java.awt.Image iMech;
            boolean useCamo = (camo != null);

            iMech = image;

            int[] pMech = new int[IMG_SIZE];
            int[] pCamo = new int[IMG_SIZE];
            java.awt.image.PixelGrabber pgMech = new java.awt.image.PixelGrabber(iMech,
                  0,
                  0,
                  IMG_WIDTH,
                  IMG_HEIGHT,
                  pMech,
                  0,
                  IMG_WIDTH);

            try {
                pgMech.grabPixels();
            } catch (InterruptedException e) {
                MWLogger.errLog("EntityImage.applyColor(): Failed to grab pixels for mech image." + e.getMessage());
                return image;
            }
            if ((pgMech.getStatus() & java.awt.image.ImageObserver.ABORT) != 0) {
                MWLogger.errLog("EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted.");
                return image;
            }

            if (useCamo) {
                java.awt.image.PixelGrabber pgCamo = new java.awt.image.PixelGrabber(camo,
                      0,
                      0,
                      IMG_WIDTH,
                      IMG_HEIGHT,
                      pCamo,
                      0,
                      IMG_WIDTH);
                try {
                    pgCamo.grabPixels();
                } catch (InterruptedException e) {
                    MWLogger.errLog("EntityImage.applyColor(): Failed to grab pixels for camo image." + e.getMessage());
                    return image;
                }
                if ((pgCamo.getStatus() & java.awt.image.ImageObserver.ABORT) != 0) {
                    MWLogger.errLog(
                          "EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted.");
                    return image;
                }
            }

            for (int i = 0; i < IMG_SIZE; i++) {
                int pixel = pMech[i];
                int alpha = (pixel >> 24) & 0xff;

                if (alpha != 0) {
                    int pixel1 = useCamo ? pCamo[i] : tint;
                    float red1 = ((float) ((pixel1 >> 16) & 0xff)) / 255;
                    float green1 = ((float) ((pixel1 >> 8) & 0xff)) / 255;
                    float blue1 = ((float) ((pixel1) & 0xff)) / 255;

                    float black = ((pMech[i]) & 0xff);

                    int red2 = Math.round(red1 * black);
                    int green2 = Math.round(green1 * black);
                    int blue2 = Math.round(blue1 * black);

                    pMech[i] = (alpha << 24) | (red2 << 16) | (green2 << 8) | blue2;
                }
            }

            image = comp.createImage(new java.awt.image.MemoryImageSource(IMG_WIDTH, IMG_HEIGHT, pMech, 0, IMG_WIDTH));
            return image;
        }

        public java.awt.Image loadPreviewImage() {
            base = applyColor(base);
            return base;
        }

        public java.awt.Image getFacing(int facing) {
            return facings[facing];
        }

        public java.awt.Image getWreckFacing(int facing) {
            return wreckFacings[facing];
        }

        public java.awt.Image getBase() {
            return base;
        }

        public java.awt.Image getIcon() {
            return icon;
        }
    }

}
