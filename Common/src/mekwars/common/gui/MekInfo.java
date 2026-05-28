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
import java.io.Serial;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.ui.tileset.MekTileset;
import megamek.client.ui.util.RotateFilter;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;
import mekwars.common.Unit;
import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.IClientConfig;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.UnitUtils;

/**
 *
 * @author Steve Hawkins
 */

public class MekInfo extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(MekInfo.class);

    @Serial
    private static final long serialVersionUID = 4308503800966118202L;
    protected static MekTileset mekTileset;
    private final JLabel lblName;
    private IClient client = null;
    private IClientConfig Config = null;
    private ImageIcon previewIcon = null;
    private CUnit cm = null;
    private CArmy army = null;
    private JLabel lblImage = new JLabel();
    private int cellWidth = 86;

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

        lblImage = new JLabel() {
            @Serial
            private static final long serialVersionUID = -114192798426952281L;

            @Override
            public void paint(Graphics graphics) {

                // First, draw the background image - tiled
                if (Config.isParam("UNIT_HEX")) {
                    ImageIcon image = new ImageIcon((new ImageIcon(
                          "data/images/hexes/boring/beige_plains_0.gif")).getImage()
                                                          .getScaledInstance(cellWidth,
                                                                getHeight(),
                                                                Image.SCALE_DEFAULT));
                    graphics.drawImage(image.getImage(),
                          (getWidth() - image.getIconWidth()) / 2,
                          (getHeight() - image.getIconHeight()) / 2,
                          null,
                          null);
                }

                // Now let the regular paint code do it's work
                Icon icon = getIcon();
                icon.paintIcon(this,
                      graphics,
                      (getWidth() - icon.getIconWidth()) / 2,
                      (getHeight() - icon.getIconHeight()) / 2);

                if (MekInfo.this.client != null && MekInfo.this.client.getConfig().isUsingStatusIcons() && cm != null) {
                    int height = 0;
                    boolean dynamic = MekInfo.this.client.getConfig().isParam("LEFT_COLUMN_DYNAMIC");
                    ImageIcon imageIcon;
                    Entity entity = cm.getEntity();

                    if (lblImage.isVisible() && (entity instanceof Mek || entity instanceof Tank)) {
                        boolean useAdvanceRepairs = MekInfo.this.client.isUsingAdvanceRepairs();

                        // Pilot Block
                        if (Config.isParam("LEFT_PILOT_EJECT")) {
                            if (cm.hasVacantPilot()) {
                                imageIcon = new ImageIcon("data/images/status/nopilot.gif");

                                graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else if (cm.getPilot().getHits() > 0) {
                                imageIcon = new ImageIcon("data/images/status/wound.gif");
                                graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else if (entity instanceof Mek && ((Mek) entity).isAutoEject()) {
                                if (!dynamic) {
                                    imageIcon = new ImageIcon("data/images/status/eject.gif");
                                    graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                }
                            } else {
                                imageIcon = new ImageIcon("data/images/status/noeject.gif");
                                graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();

                            }
                        }

                        // Repairing status
                        if (Config.isParam("LEFT_REPAIR")) {
                            if (useAdvanceRepairs) {
                                if (UnitUtils.isRepairing(cm.getEntity())) {
                                    imageIcon = new ImageIcon("data/images/status/repairing.gif");
                                    graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                } else if (MekInfo.this.client.getRMT() != null &&
                                                 MekInfo.this.client.getRMT().hasQueuedOrders(cm.getId())) {
                                    imageIcon = new ImageIcon("data/images/status/pending.gif");
                                    graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                }
                            } else {
                                if (cm.getStatus() == Unit.STATUS_UNMAINTAINED) {
                                    imageIcon = new ImageIcon("data/images/status/unmaint.gif");
                                    graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                } else {
                                    if (!dynamic) {
                                        imageIcon = new ImageIcon("data/images/status/maint.gif");
                                        graphics.drawImage(imageIcon.getImage(),
                                              0,
                                              height,
                                              imageIcon.getImageObserver());
                                        height += imageIcon.getIconHeight();
                                    }
                                }
                            }
                        }

                        // Engine Damage
                        if (Config.isParam("LEFT_ENGINE")) {
                            // Engine Block
                            if (UnitUtils.getNumberOfDamagedEngineCrits(entity) >= 1) {
                                imageIcon = new ImageIcon("data/images/status/engine.gif");
                                graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    imageIcon = new ImageIcon("data/images/status/blank.gif");
                                    graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                }
                            }
                        }

                        // Equipiment/Crit Damage
                        if (Config.isParam("LEFT_EQUIPMENT")) {
                            if (UnitUtils.hasCriticalDamage(entity)) {
                                imageIcon = new ImageIcon("data/images/status/critical.gif");
                                graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    imageIcon = new ImageIcon("data/images/status/blank.gif");
                                    graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                }
                            }
                        }

                        // Armor/IS Damage
                        if (Config.isParam("LEFT_ARMOR")) {
                            if (UnitUtils.hasISDamage(entity)) {
                                imageIcon = new ImageIcon("data/images/status/structure.gif");
                                graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else if (UnitUtils.hasArmorDamage(entity)) {
                                imageIcon = new ImageIcon("data/images/status/armor.gif");
                                graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    imageIcon = new ImageIcon("data/images/status/blank.gif");
                                    graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                }
                            }
                        }

                        // ammo block
                        if (Config.isParam("LEFT_AMMO")) {
                            if (UnitUtils.isAmmoless(entity)) {
                                if (!dynamic) {
                                    imageIcon = new ImageIcon("data/images/status/blank.gif");
                                    graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                }
                            } else if (UnitUtils.hasEmptyAmmo(entity)) {
                                imageIcon = new ImageIcon("data/images/status/empty.gif");
                                graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else if (UnitUtils.hasLowAmmo(entity)) {
                                imageIcon = new ImageIcon("data/images/status/low.gif");
                                graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    imageIcon = new ImageIcon("data/images/status/blank.gif");
                                    graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                }
                            }
                        }

                        // commander block
                        if (Config.isParam("LEFT_COMMANDER") && army != null) {
                            if (army.isCommander(cm.getId())) {
                                imageIcon = new ImageIcon("data/images/status/comm.gif");
                                graphics.drawImage(imageIcon.getImage(), 0, height, imageIcon.getImageObserver());
                            } else {
                                if (!dynamic) {
                                    imageIcon = new ImageIcon("data/images/status/blank.gif");
                                    graphics.drawImage(imageIcon.getImage(),
                                          cellWidth - imageIcon.getIconWidth(),
                                          height,
                                          imageIcon.getImageObserver());
                                }
                            }
                        }

                        height = 0;
                        // Screw it. I can't find the width any other way.
                        // consecutive paints will fix the issue.
                        cellWidth = Math.min(cellWidth, getWidth());
                        dynamic = Config.isParam("RIGHT_COLUMN_DYNAMIC");
                        // Pilot Block
                        if (Config.isParam("RIGHT_PILOT_EJECT")) {
                            if (cm.hasVacantPilot()) {
                                imageIcon = new ImageIcon("data/images/status/nopilot.gif");

                                graphics.drawImage(imageIcon.getImage(),
                                      cellWidth - imageIcon.getIconWidth(),
                                      height,
                                      imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else if (cm.getPilot().getHits() > 0) {
                                imageIcon = new ImageIcon("data/images/status/wound.gif");
                                graphics.drawImage(imageIcon.getImage(),
                                      cellWidth - imageIcon.getIconWidth(),
                                      height,
                                      imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else if (entity instanceof Mek && ((Mek) entity).isAutoEject()) {
                                if (!dynamic) {
                                    imageIcon = new ImageIcon("data/images/status/eject.gif");
                                    graphics.drawImage(imageIcon.getImage(),
                                          cellWidth - imageIcon.getIconWidth(),
                                          height,
                                          imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                }
                            } else {
                                imageIcon = new ImageIcon("data/images/status/noeject.gif");
                                graphics.drawImage(imageIcon.getImage(),
                                      cellWidth - imageIcon.getIconWidth(),
                                      height,
                                      imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();

                            }
                        }

                        // Repairing status
                        if (Config.isParam("RIGHT_REPAIR")) {
                            if (useAdvanceRepairs) {
                                if (UnitUtils.isRepairing(cm.getEntity())) {
                                    imageIcon = new ImageIcon("data/images/status/repairing.gif");
                                    graphics.drawImage(imageIcon.getImage(),
                                          cellWidth - imageIcon.getIconWidth(),
                                          height,
                                          imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                } else if (MekInfo.this.client.getRMT() != null &&
                                                 MekInfo.this.client.getRMT().hasQueuedOrders(cm.getId())) {
                                    imageIcon = new ImageIcon("data/images/status/pending.gif");
                                    graphics.drawImage(imageIcon.getImage(),
                                          cellWidth - imageIcon.getIconWidth(),
                                          height,
                                          imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                }
                            } else {
                                if (cm.getStatus() == Unit.STATUS_UNMAINTAINED) {
                                    imageIcon = new ImageIcon("data/images/status/unmaint.gif");
                                    graphics.drawImage(imageIcon.getImage(),
                                          cellWidth - imageIcon.getIconWidth(),
                                          height,
                                          imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                } else {
                                    if (!dynamic) {
                                        imageIcon = new ImageIcon("data/images/status/maint.gif");
                                        graphics.drawImage(imageIcon.getImage(),
                                              cellWidth - imageIcon.getIconWidth(),
                                              height,
                                              imageIcon.getImageObserver());
                                        height += imageIcon.getIconHeight();
                                    }
                                }
                            }
                        }

                        // Engine Damage
                        if (Config.isParam("RIGHT_ENGINE")) {
                            // Engine Block
                            if (UnitUtils.getNumberOfDamagedEngineCrits(entity) >= 1) {
                                imageIcon = new ImageIcon("data/images/status/engine.gif");
                                graphics.drawImage(imageIcon.getImage(),
                                      cellWidth - imageIcon.getIconWidth(),
                                      height,
                                      imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    imageIcon = new ImageIcon("data/images/status/blank.gif");
                                    graphics.drawImage(imageIcon.getImage(),
                                          cellWidth - imageIcon.getIconWidth(),
                                          height,
                                          imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                }
                            }
                        }

                        // Equipiment/Crit Damage
                        if (Config.isParam("RIGHT_EQUIPMENT")) {
                            if (UnitUtils.hasCriticalDamage(entity)) {
                                imageIcon = new ImageIcon("data/images/status/critical.gif");
                                graphics.drawImage(imageIcon.getImage(),
                                      cellWidth - imageIcon.getIconWidth(),
                                      height,
                                      imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    imageIcon = new ImageIcon("data/images/status/blank.gif");
                                    graphics.drawImage(imageIcon.getImage(),
                                          cellWidth - imageIcon.getIconWidth(),
                                          height,
                                          imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                }
                            }
                        }

                        // Armor/IS Damage
                        if (Config.isParam("RIGHT_ARMOR")) {
                            if (UnitUtils.hasISDamage(entity)) {
                                imageIcon = new ImageIcon("data/images/status/structure.gif");
                                graphics.drawImage(imageIcon.getImage(),
                                      cellWidth - imageIcon.getIconWidth(),
                                      height,
                                      imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else if (UnitUtils.hasArmorDamage(entity)) {
                                imageIcon = new ImageIcon("data/images/status/armor.gif");
                                graphics.drawImage(imageIcon.getImage(),
                                      cellWidth - imageIcon.getIconWidth(),
                                      height,
                                      imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    imageIcon = new ImageIcon("data/images/status/blank.gif");
                                    graphics.drawImage(imageIcon.getImage(),
                                          cellWidth - imageIcon.getIconWidth(),
                                          height,
                                          imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                }
                            }
                        }

                        // ammo block
                        if (Config.isParam("RIGHT_AMMO")) {
                            if (UnitUtils.isAmmoless(entity)) {
                                if (!dynamic) {
                                    imageIcon = new ImageIcon("data/images/status/blank.gif");
                                    graphics.drawImage(imageIcon.getImage(),
                                          cellWidth - imageIcon.getIconWidth(),
                                          height,
                                          imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                }
                            } else if (UnitUtils.hasEmptyAmmo(entity)) {
                                imageIcon = new ImageIcon("data/images/status/empty.gif");
                                graphics.drawImage(imageIcon.getImage(),
                                      cellWidth - imageIcon.getIconWidth(),
                                      height,
                                      imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else if (UnitUtils.hasLowAmmo(entity)) {
                                imageIcon = new ImageIcon("data/images/status/low.gif");
                                graphics.drawImage(imageIcon.getImage(),
                                      cellWidth - imageIcon.getIconWidth(),
                                      height,
                                      imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            } else {
                                if (!dynamic) {
                                    imageIcon = new ImageIcon("data/images/status/blank.gif");
                                    graphics.drawImage(imageIcon.getImage(),
                                          cellWidth - imageIcon.getIconWidth(),
                                          height,
                                          imageIcon.getImageObserver());
                                    height += imageIcon.getIconHeight();
                                }
                            }
                        }
                    }

                    // commander block
                    if (Config.isParam("RIGHT_COMMANDER") && army != null) {
                        if (army.isCommander(cm.getId())) {
                            imageIcon = new ImageIcon("data/images/status/comm.gif");
                            graphics.drawImage(imageIcon.getImage(),
                                  cellWidth - imageIcon.getIconWidth(),
                                  height,
                                  imageIcon.getImageObserver());
                            height += imageIcon.getIconHeight();
                        } else {
                            if (!dynamic) {
                                imageIcon = new ImageIcon("data/images/status/blank.gif");
                                graphics.drawImage(imageIcon.getImage(),
                                      cellWidth - imageIcon.getIconWidth(),
                                      height,
                                      imageIcon.getImageObserver());
                                height += imageIcon.getIconHeight();
                            }
                        }
                    }

                }

                // super.paint(graphics);
            }
        };// end new JLabel(LBL Image)

        lblName = new JLabel();
        setLayout(new GridBagLayout());

        lblImage.setHorizontalAlignment(SwingConstants.CENTER);
        java.awt.GridBagConstraints gridBagConstraints = new GridBagConstraints();
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
     * Creates new MekInfo for use in previews. Is passed a ficticious config which contains preview camo.
     * <p>
     * Used to generate images in HQ, BM, etc.
     */
    public MekInfo(ImageIcon preview) {

        // set the preview icon
        this.previewIcon = preview;
        Config = null;

        java.awt.GridBagConstraints gridBagConstraints;

        if (client != null) {
            Config = client.getConfig();
        }

        lblImage = new JLabel() {
            @Serial
            private static final long serialVersionUID = 639618470390199477L;

            @Override
            public void paint(Graphics graphics) {
                // first draw the background image - tiled
                ImageIcon image = new ImageIcon((new ImageIcon(
                      "data/images/hexes/boring/beige_plains_0.gif")).getImage()
                                                      .getScaledInstance(80,
                                                            68,
                                                            Image.SCALE_DEFAULT));
                graphics.drawImage(image.getImage(),
                      (getWidth() - image.getIconWidth()) / 2,
                      (getHeight() - image.getIconHeight()) / 2,
                      null,
                      null);

                // Now let the regular paint code do it's work
                Icon icon = getIcon();
                icon.paintIcon(this,
                      graphics,
                      (getWidth() - icon.getIconWidth()) / 2,
                      (getHeight() - icon.getIconHeight()) / 2);
                // super.paint(graphics);
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

    public static Image getImageFor(Entity entity, Component component) {
        if (mekTileset == null) {
            mekTileset = new MekTileset(new File("data/images/units/"));
            try {
                mekTileset.loadFromFile("mechset.txt");
            } catch (IOException ex) {
                LOGGER.error(ex, "Unable to read data/images/units/mechset.txt");
            }
        }// end if(null tileset)
        //@Salient - from what i can tell from the megamek code, passing in the component does nothing.
        return mekTileset.imageFor(entity, -1);
    }

    public void setText(String text) {
        lblName.setText(text);
    }

    public Image getEmbeddedImage() {
        return ((ImageIcon) lblImage.getIcon()).getImage();
    }

    public void setPreviewIcon(ImageIcon preview) {
        previewIcon = preview;
    }

    public void setUnit(Entity entity) {
        Image unit;
        Image camo = null;
        ImageIcon camoIcon;
        this.cm = null;

        unit = getImageFor(entity, lblImage).getScaledInstance(84, 72, Image.SCALE_DEFAULT);

        // look for a config image to load. if no config exists,
        // try to load the preview icon.
        if (Config != null) {
            camoIcon = Config.getImage("CAMO");
        } else {
            camoIcon = previewIcon;
        }

        if (camoIcon != null) {
            camo = camoIcon.getImage();
        }

        MekInfo.EntityImage entityImage = new EntityImage(unit,
              0xFFFFFF,
              camo,
              this);
        setImage(entityImage.loadPreviewImage());

    }

    public void setImage(Image img) {
        lblImage.setIcon(new ImageIcon(img.getScaledInstance(cellWidth, 74, Image.SCALE_DEFAULT)));
    }

    public void setUnit(CUnit cm, CArmy army) {
        if (cm == null) {
            return;
        }

        this.cm = cm;
        this.army = army;
        Image unit = null;
        Image camo = null;
        ImageIcon camoIcon;
        Entity entity = cm.getEntity();

        try // @ salient, this should fix the gui problem.
        {
            unit = getImageFor(entity, lblImage).getScaledInstance(84, 72, Image.SCALE_DEFAULT);
        } catch (Exception ex) {
            LOGGER.error(ex, "Error setting unit. {}", ex.getLocalizedMessage());
            try {
                File pathToFile = new File("./data/images/ImageMissing.png");
                unit = ImageIO.read(pathToFile);
                unit = unit.getScaledInstance(84, 72, Image.SCALE_DEFAULT);
            } catch (IOException ex2) {
                LOGGER.debug(ex2, "incorrect image filename in mechset.txt for {} {}",
                      cm.getModelName(),
                      CUnit.getTypeClassDesc(cm.getType()));
            }
        }

        // look for a config image to load. if no config exists,
        // try to load the preview icon.
        if (Config != null) {
            camoIcon = Config.getImage("CAMO");
        } else {
            camoIcon = previewIcon;
        }

        if (camoIcon != null) {
            camo = camoIcon.getImage();
        }

        MekInfo.EntityImage entityImage = new EntityImage(unit,
              0xFFFFFF,
              camo,
              this);
        setImage(entityImage.loadPreviewImage());

    }

    public void setImageVisible(boolean flag) {
        lblImage.setVisible(flag);
    }

    /**
     * A class to handle the image permutations for an entity (Code from megamek.common.TilesetManager class)
     */
    private static class EntityImage {
        private final int tint;
        private final Image camo;
        private final Image[] facings = new Image[6];
        private final Image[] wreckFacings = new Image[6];
        private final Component comp;
        private final int IMG_WIDTH = 84;
        private final int IMG_HEIGHT = 72;
        private final int IMG_SIZE = IMG_WIDTH * IMG_HEIGHT;
        private Image base;
        private Image wreck;
        private Image icon;

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
                ImageProducer rotSource = new FilteredImageSource(base.getSource(),
                      new RotateFilter((Math.PI / 3) * (6 - i)));
                facings[i] = comp.createImage(rotSource);
            }

            if (wreck != null) {
                wreck = applyColor(wreck);
                for (int i = 0; i < 6; i++) {
                    ImageProducer rotSource = new FilteredImageSource(wreck.getSource(),
                          new RotateFilter((Math.PI / 3) * (6 - i)));
                    wreckFacings[i] = comp.createImage(rotSource);
                }
            }
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
                LOGGER.error(e, "EntityImage.applyColor(): Failed to grab pixels for mek image. {}", e.getMessage());
                return image;
            }
            if ((pgMech.getStatus() & ImageObserver.ABORT) != 0) {
                LOGGER.debug("EntityImage.applyColor(): Failed to grab pixels for mek image. ImageObserver aborted.");
                return image;
            }

            if (useCamo) {
                PixelGrabber pgCamo = new PixelGrabber(camo, 0, 0, IMG_WIDTH, IMG_HEIGHT, pCamo, 0, IMG_WIDTH);
                try {
                    pgCamo.grabPixels();
                } catch (InterruptedException e) {
                    LOGGER.error(e,
                          "EntityImage.applyColor(): Failed to grab pixels for camo image. {}",
                          e.getMessage());
                    return image;
                }
                if ((pgCamo.getStatus() & ImageObserver.ABORT) != 0) {
                    LOGGER.debug("EntityImage.applyColor(): Failed to grab pixels for mek image. ImageObserver " +
                                       "aborted.");
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
    }

}
