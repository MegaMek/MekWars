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
 * MechInfo.java
 *
 * Created on June 14, 2002, 9:02 PM
 */

package client.gui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import client.GUIClientConfig;
import client.MWClient;
import client.campaign.CArmy;
import client.campaign.CUnit;
import common.Unit;
import common.util.MWLogger;
import common.util.UnitUtils;
import megamek.client.ui.swing.tileset.MechTileset;
import megamek.client.ui.swing.util.RotateFilter;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Tank;

/**
 * 
 * @author Steve Hawkins
 */

public class MechInfo extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 4308503800966118202L;
    protected static MechTileset mt;
    private JLabel lblName = new JLabel();
    private JLabel lblImage = new JLabel();
    private int cellWidth = 86;

    MWClient mwclient = null;
    GUIClientConfig Config = null;
    ImageIcon previewIcon = null;
    CUnit cm = null;
    CArmy army = null;

    /*
     * public void setBackground(Color color){ super.setBackground(color); try{
     * imagePanel.setBackground(color); }catch(Exception ex){} }
     */

    /**
     * Creates new general-purpose MechInfo.
     * 
     * Used to generate images in HQ, BM, etx.
     */
    public MechInfo(MWClient client) {
        mwclient = client;

        if (mwclient != null)
            Config = mwclient.getConfig();

        lblImage = new JLabel() {

            /**
             * 
             */
            private static final long serialVersionUID = -114192798426952281L;

            @Override
            public void paint(Graphics g) {

                // First draw the background image - tiled
                if (Config.isParam("UNITHEX")) {
                    ImageIcon image = new ImageIcon((new ImageIcon("data/images/hexes/boring/beige_plains_0.gif")).getImage().getScaledInstance(cellWidth, getHeight(), Image.SCALE_DEFAULT));
                    g.drawImage(image.getImage(), (getWidth() - image.getIconWidth()) / 2, (getHeight() - image.getIconHeight()) / 2, null, null);
                }

                // Now let the regular paint code do it's work
                Icon icon = getIcon();
                icon.paintIcon(this, g, (getWidth() - icon.getIconWidth()) / 2, (getHeight() - icon.getIconHeight()) / 2);

                if (mwclient != null && mwclient.getConfig().isUsingStatusIcons() && cm != null) {

                    int height = 0;
                    boolean dynamic = mwclient.getConfig().isParam("LEFTCOLUMNDYNAMIC");
                    ImageIcon ic = null;
                    Entity m = cm.getEntity();

                    if (lblImage.isVisible() && (m instanceof Mech || m instanceof Tank)) {

                        boolean useAdvanceRepairs = mwclient.isUsingAdvanceRepairs();

                        // Pilot Block
                        if (Config.isParam("LEFTPILOTEJECT")) {
                            if (cm.hasVacantPilot()) {
                                ic = new ImageIcon("data/images/status/nopilot.gif");

                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                                // pilotImage.setIcon(new
                                // ImageIcon("data/images/status/nopilot.gif"));
                            } else if (cm.getPilot().getHits() > 0) {
                                ic = new ImageIcon("data/images/status/wound.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else if (m instanceof Mech && ((Mech) m).isAutoEject()) {
                                if (!dynamic) {
                                    ic = new ImageIcon("data/images/status/eject.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            } else {
                                ic = new ImageIcon("data/images/status/noeject.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();

                            }
                        }

                        // Repairing status
                        if (Config.isParam("LEFTREPAIR")) {
                            if (useAdvanceRepairs) {
                                if (UnitUtils.isRepairing(cm.getEntity())) {
                                    ic = new ImageIcon("data/images/status/repairing.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                } else if (mwclient.getRMT() != null && mwclient.getRMT().hasQueuedOrders(cm.getId())) {
                                    ic = new ImageIcon("data/images/status/pending.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            } else {
                                if (cm.getStatus() == Unit.STATUS_UNMAINTAINED) {
                                    ic = new ImageIcon("data/images/status/unmaint.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                } else {
                                    if (!dynamic) {
                                        ic = new ImageIcon("data/images/status/maint.gif");
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
                                ic = new ImageIcon("data/images/status/engine.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        // Equipiment/Crit Damage
                        if (Config.isParam("LEFTEQUIPMENT")) {
                            if (UnitUtils.hasCriticalDamage(m)) {
                                ic = new ImageIcon("data/images/status/critical.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        // Armor/IS Damage
                        if (Config.isParam("LEFTARMOR")) {
                            if (UnitUtils.hasISDamage(m)) {
                                ic = new ImageIcon("data/images/status/structure.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else if (UnitUtils.hasArmorDamage(m)) {
                                ic = new ImageIcon("data/images/status/armor.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        // ammo block
                        if (Config.isParam("LEFTAMMO")) {
                            if (UnitUtils.isAmmoless(m)) {
                                if (!dynamic) {
                                    ic = new ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            } else if (UnitUtils.hasEmptyAmmo(m)) {
                                ic = new ImageIcon("data/images/status/empty.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else if (UnitUtils.hasLowAmmo(m)) {
                                ic = new ImageIcon("data/images/status/low.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        // commander block
                        if (Config.isParam("LEFTCOMMANDER") && army != null) {
                            if (army.isCommander(cm.getId())) {
                                ic = new ImageIcon("data/images/status/comm.gif");
                                g.drawImage(ic.getImage(), 0, height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
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
                                ic = new ImageIcon("data/images/status/nopilot.gif");

                                g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                height += ic.getIconHeight();
                                // pilotImage.setIcon(new
                                // ImageIcon("data/images/status/nopilot.gif"));
                            } else if (cm.getPilot().getHits() > 0) {
                                ic = new ImageIcon("data/images/status/wound.gif");
                                g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else if (m instanceof Mech && ((Mech) m).isAutoEject()) {
                                if (!dynamic) {
                                    ic = new ImageIcon("data/images/status/eject.gif");
                                    g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            } else {
                                ic = new ImageIcon("data/images/status/noeject.gif");
                                g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                height += ic.getIconHeight();

                            }
                        }

                        // Repairing status
                        if (Config.isParam("RIGHTREPAIR")) {
                            if (useAdvanceRepairs) {
                                if (UnitUtils.isRepairing(cm.getEntity())) {
                                    ic = new ImageIcon("data/images/status/repairing.gif");
                                    g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                } else if (mwclient.getRMT() != null && mwclient.getRMT().hasQueuedOrders(cm.getId())) {
                                    ic = new ImageIcon("data/images/status/pending.gif");
                                    g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            } else {
                                if (cm.getStatus() == Unit.STATUS_UNMAINTAINED) {
                                    ic = new ImageIcon("data/images/status/unmaint.gif");
                                    g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                } else {
                                    if (!dynamic) {
                                        ic = new ImageIcon("data/images/status/maint.gif");
                                        g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                        height += ic.getIconHeight();
                                    }
                                }
                            }
                        }

                        // Engine Damage
                        if (Config.isParam("RIGHTENGINE")) {
                            // Engine Block
                            if (UnitUtils.getNumberOfDamagedEngineCrits(m) >= 1) {
                                ic = new ImageIcon("data/images/status/engine.gif");
                                g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        // Equipiment/Crit Damage
                        if (Config.isParam("RIGHTEQUIPMENT")) {
                            if (UnitUtils.hasCriticalDamage(m)) {
                                ic = new ImageIcon("data/images/status/critical.gif");
                                g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        // Armor/IS Damage
                        if (Config.isParam("RIGHTARMOR")) {
                            if (UnitUtils.hasISDamage(m)) {
                                ic = new ImageIcon("data/images/status/structure.gif");
                                g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else if (UnitUtils.hasArmorDamage(m)) {
                                ic = new ImageIcon("data/images/status/armor.gif");
                                g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }

                        // ammo block
                        if (Config.isParam("RIGHTAMMO")) {
                            if (UnitUtils.isAmmoless(m)) {
                                if (!dynamic) {
                                    ic = new ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            } else if (UnitUtils.hasEmptyAmmo(m)) {
                                ic = new ImageIcon("data/images/status/empty.gif");
                                g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else if (UnitUtils.hasLowAmmo(m)) {
                                ic = new ImageIcon("data/images/status/low.gif");
                                g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                height += ic.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    ic = new ImageIcon("data/images/status/blank.gif");
                                    g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                                    height += ic.getIconHeight();
                                }
                            }
                        }
                    }

                    // commander block
                    if (Config.isParam("RIGHTCOMMANDER") && army != null) {
                        if (army.isCommander(cm.getId())) {
                            ic = new ImageIcon("data/images/status/comm.gif");
                            g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
                            height += ic.getIconHeight();
                        } else {
                            if (!dynamic) {
                                ic = new ImageIcon("data/images/status/blank.gif");
                                g.drawImage(ic.getImage(), cellWidth - ic.getIconWidth(), height, ic.getImageObserver());
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

        lblName = new JLabel();
        setLayout(new GridBagLayout());

        lblImage.setHorizontalAlignment(SwingConstants.CENTER);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        add(lblImage, gridBagConstraints);

        lblName.setHorizontalAlignment(SwingConstants.CENTER);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        add(lblName, gridBagConstraints);
    }

    /**
     * Creates new MechInfo for use in previews. Is passed a ficticious config
     * which contains preview camo.
     * 
     * Used to generate images in HQ, BM, etc.
     */
    public MechInfo(ImageIcon preview) {

        // set the preview icon
        this.previewIcon = preview;
        Config = null;

        GridBagConstraints gridBagConstraints;
        if (mwclient != null) {
            Config = mwclient.getConfig();
        }

        lblImage = new JLabel() {

            /**
             * 
             */
            private static final long serialVersionUID = 639618470390199477L;

            @Override
            public void paint(Graphics g) {
                // first draw the background image - tiled
                ImageIcon image = new ImageIcon((new ImageIcon("data/images/hexes/boring/beige_plains_0.gif")).getImage().getScaledInstance(80, 68, Image.SCALE_DEFAULT));
                g.drawImage(image.getImage(), (getWidth() - image.getIconWidth()) / 2, (getHeight() - image.getIconHeight()) / 2, null, null);

                // Now let the regular paint code do it's work
                Icon icon = getIcon();
                icon.paintIcon(this, g, (getWidth() - icon.getIconWidth()) / 2, (getHeight() - icon.getIconHeight()) / 2);
                // super.paint(g);
            }
        };// end new JLabel(LBL Image)

        lblName = new JLabel();
        setLayout(new GridBagLayout());
        lblImage.setHorizontalAlignment(SwingConstants.CENTER);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(lblImage, gridBagConstraints);

        lblName.setHorizontalAlignment(SwingConstants.CENTER);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(lblName, gridBagConstraints);
    }

    public void setText(String s) {
        lblName.setText(s);
    }

    public void setImage(Image img) {
        lblImage.setIcon(new ImageIcon(img.getScaledInstance(cellWidth, 74, Image.SCALE_DEFAULT)));
    }

    public Image getEmbeddedImage() {
        return ((ImageIcon) lblImage.getIcon()).getImage();
    }

    public static Image getImageFor(Entity m, Component c) {

        if (mt == null) {
            mt = new MechTileset(new File("data/images/units/"));
            try {
                mt.loadFromFile("mechset.txt");
            } catch (IOException ex) {
                MWLogger.errLog("Unable to read data/images/units/mechset.txt");
            }
        }// end if(null tileset)
        //@Salient - from what i can tell from the megamek code, passing in the component does nothing.
        return mt.imageFor(m, -1); 
    }

    public void setPreviewIcon(ImageIcon preview) {
        previewIcon = preview;
    }

    public void setUnit(Entity m) {
        Image unit = null;
        Image camo = null;
        ImageIcon camoicon = null;
        this.cm = null;

        unit = getImageFor(m, lblImage).getScaledInstance(84, 72, Image.SCALE_DEFAULT);

        // look for a config image to load. if no config exists,
        // try to load the preview icon.
        if (Config != null)
            camoicon = Config.getImage("CAMO");
        else
            camoicon = previewIcon;

        if (camoicon != null)
            camo = camoicon.getImage();

        EntityImage ei = new EntityImage(unit, 0xFFFFFF, camo, this);
        setImage(ei.loadPreviewImage());

    }

    public void setUnit(CUnit cm, CArmy army) {

        if (cm == null)
            return;

        this.cm = cm;
        this.army = army;
        Image unit = null;
        Image camo = null;
        ImageIcon camoicon = null;
        Entity m = cm.getEntity();

        try // @ salient, this should fix the gui problem.
        {
        	unit = getImageFor(m, lblImage).getScaledInstance(84, 72, Image.SCALE_DEFAULT);     	
        }
        catch (Exception ex)
        {
        	MWLogger.errLog(ex);   	
        	try 
        	{
        	    File pathToFile = new File("./data/images/ImageMissing.png");
        	    unit = ImageIO.read(pathToFile);
        	    unit = unit.getScaledInstance(84, 72, Image.SCALE_DEFAULT);
        	    MWLogger.errLog("incorrect image filename in mechset.txt for " + cm.getModelName() + " " + CUnit.getTypeClassDesc(cm.getType()));
        	} 
        	catch (IOException ex2) 
        	{
        	    MWLogger.errLog("incorrect image filename in mechset.txt for " + cm.getModelName() + " " + CUnit.getTypeClassDesc(cm.getType()));
        	    MWLogger.errLog(ex2);
        	}
        }

        // look for a config image to load. if no config exists,
        // try to load the preview icon.
        if (Config != null)
            camoicon = Config.getImage("CAMO");
        else
            camoicon = previewIcon;

        if (camoicon != null)
            camo = camoicon.getImage();

        EntityImage ei = new EntityImage(unit, 0xFFFFFF, camo, this);
        setImage(ei.loadPreviewImage());

    }

    public void setImageVisible(boolean flag) {

        lblImage.setVisible(flag);
    }

    /**
     * A class to handle the image permutations for an entity (Code from
     * megamek.common.TilesetManager class)
     */
    private class EntityImage {
        private Image base;
        private Image wreck;
        private Image icon;
        private int tint;
        private Image camo;
        private Image[] facings = new Image[6];
        private Image[] wreckFacings = new Image[6];
        private Component comp;

        private final int IMG_WIDTH = 84;
        private final int IMG_HEIGHT = 72;
        private final int IMG_SIZE = IMG_WIDTH * IMG_HEIGHT;

        public EntityImage(Image base, int tint, Image camo, Component comp) {
            this(base, null, tint, camo, comp);
        }

        public EntityImage(Image base, Image wreck, int tint, Image camo, Component comp) {
            this.base = base;
            this.tint = tint;
            this.camo = camo;
            this.comp = comp;
            this.wreck = wreck;
        }

        public void loadFacings() {
            base = applyColor(base);

            icon = base.getScaledInstance(56, 48, Image.SCALE_SMOOTH);
            for (int i = 0; i < 6; i++) {
                ImageProducer rotSource = new FilteredImageSource(base.getSource(), new RotateFilter((Math.PI / 3) * (6 - i)));
                facings[i] = comp.createImage(rotSource);
            }

            if (wreck != null) {
                wreck = applyColor(wreck);
                for (int i = 0; i < 6; i++) {
                    ImageProducer rotSource = new FilteredImageSource(wreck.getSource(), new RotateFilter((Math.PI / 3) * (6 - i)));
                    wreckFacings[i] = comp.createImage(rotSource);
                }
            }
        }

        public Image loadPreviewImage() {
            base = applyColor(base);
            return base;
        }

        public Image getFacing(int facing) {
            return facings[facing];
        }

        public Image getWreckFacing(int facing) {
            return wreckFacings[facing];
        }

        public Image getBase() {
            return base;
        }

        public Image getIcon() {
            return icon;
        }

        private Image applyColor(Image image) {
            Image iMech;
            boolean useCamo = (camo != null);

            iMech = image;

            int[] pMech = new int[IMG_SIZE];
            int[] pCamo = new int[IMG_SIZE];
            PixelGrabber pgMech = new PixelGrabber(iMech, 0, 0, IMG_WIDTH, IMG_HEIGHT, pMech, 0, IMG_WIDTH);

            try {
                pgMech.grabPixels();
            } catch (InterruptedException e) {
                MWLogger.errLog("EntityImage.applyColor(): Failed to grab pixels for mech image." + e.getMessage());
                return image;
            }
            if ((pgMech.getStatus() & ImageObserver.ABORT) != 0) {
                MWLogger.errLog("EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted.");
                return image;
            }

            if (useCamo) {
                PixelGrabber pgCamo = new PixelGrabber(camo, 0, 0, IMG_WIDTH, IMG_HEIGHT, pCamo, 0, IMG_WIDTH);
                try {
                    pgCamo.grabPixels();
                } catch (InterruptedException e) {
                    MWLogger.errLog("EntityImage.applyColor(): Failed to grab pixels for camo image." + e.getMessage());
                    return image;
                }
                if ((pgCamo.getStatus() & ImageObserver.ABORT) != 0) {
                    MWLogger.errLog("EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted.");
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

            image = comp.createImage(new MemoryImageSource(IMG_WIDTH, IMG_HEIGHT, pMech, 0, IMG_WIDTH));
            return image;
        }
    }

}
