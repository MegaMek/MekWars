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
 * A small self-contained {@link JPanel} widget that renders a single unit's (Mek/Tank/etc.) tileset image plus a
 * name label underneath it, used throughout the MekWars client (unit lists, HQ, Battle Manager "BM", previews,
 * etc.) wherever a compact visual representation of a unit is needed.
 * <p>
 * The unit sprite itself is produced by {@link MekTileset} (MegaMek's tileset lookup, cached per-class in
 * {@link #mekTileset}) and then camo/tint colorized by the nested {@link EntityImage} helper. On top of the base
 * sprite, when {@link #cm} (a live campaign unit) is set and the client is configured to use status icons, small
 * overlay icons are painted in a left and/or right column indicating pilot status (ejected/wounded/vacant),
 * repair/maintenance state, engine damage, critical/equipment damage, armor/internal structure damage, ammo state,
 * and unit-commander status. Which icons appear (and whether "blank" placeholder icons are drawn to keep the
 * column a fixed height, or the column instead grows/shrinks "dynamically") is entirely driven by boolean
 * {@code LEFT_*}/{@code RIGHT_*}/{@code *_DYNAMIC} parameters read from the user's {@link IClientConfig}.
 * <p>
 * This class has no direct mouse/keyboard interaction of its own; it is purely a rendering widget that other
 * panels place into their layout and update by calling {@link #setUnit(Entity, Component)}-style setters (see
 * {@link #setUnit(CUnit, CArmy)} and {@link #setUnit(Entity)}) whenever the underlying unit changes.
 *
 * @author Steve Hawkins
 */

public class MekInfo extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(MekInfo.class);

    @Serial
    private static final long serialVersionUID = 4308503800966118202L;
    /** Shared, lazily-initialized tileset used to look up unit sprite images; loaded once per classloader. */
    protected static MekTileset mekTileset;
    /** Label displaying the unit's name/text below the image (set via {@link #setText(String)}). */
    private final JLabel lblName;
    /** The client this panel is bound to; may be {@code null} for standalone/preview usage. */
    private IClient client = null;
    /** The client's configuration, used to decide which status icons/hex background to render. */
    private IClientConfig Config = null;
    /** Fallback camo image used when there is no {@link #client}/{@link #Config} to pull the real camo from. */
    private ImageIcon previewIcon = null;
    /** The campaign unit currently displayed, or {@code null} if only a raw {@link Entity} preview is shown. */
    private CUnit cm = null;
    /** The army the currently displayed unit belongs to, used to check commander status. */
    private CArmy army = null;
    /** The label whose icon is the rendered/colorized unit sprite (with status-icon overlays painted on top). */
    private JLabel lblImage = new JLabel();
    /** Width in pixels used for laying out/centering the background hex tile and status-icon columns. */
    private int cellWidth = 86;

    /**
     * Creates new general-purpose MekInfo.
     * <p>
     * Used to generate images in HQ, BM, etx.
     * <p>
     * Builds {@link #lblImage} as an anonymous {@link JLabel} subclass whose {@link JLabel#paint(Graphics)} is
     * overridden to layer: (1) an optional tiled hex-terrain background (if {@code UNIT_HEX} is configured), (2)
     * the unit's icon, and (3) — when status icons are enabled and a live campaign unit ({@link #cm}) is set — two
     * columns of small status overlay icons (left and right) reporting pilot, repair, engine, equipment, armor, and
     * ammo condition plus commander status. See the paint method body for the full icon precedence rules; it is
     * intentionally verbose/repetitive rather than table-driven.
     *
     * @param client the client providing config and (later) the live unit/army being displayed; may be {@code null}
     */
    public MekInfo(IClient client) {
        this.client = client;

        if (this.client != null) {
            Config = this.client.getConfig();
        }

        lblImage = new JLabel() {
            @Serial
            private static final long serialVersionUID = -114192798426952281L;

            /**
             * Paints the background hex tile (optional), the unit icon, and — if applicable — the left/right
             * status-icon columns. Each status category (pilot, repair, engine, equipment, armor, ammo, commander)
             * is checked independently per {@code LEFT_*}/{@code RIGHT_*} config flags; within a category, at most
             * one icon is drawn (the first matching condition wins), and if none of the "interesting" conditions
             * apply, a blank placeholder icon is drawn instead unless the corresponding {@code *_DYNAMIC} config
             * flag is set (in which case the column simply doesn't grow for that slot). The right column's
             * {@code height} counter is reset to 0 and reuses the same variable name as the left column's, so the
             * two columns are independently stacked top-down at x=0 (left) and x=cellWidth-iconWidth (right).
             */
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
     * <p>
     * NOTE (apparent quirk): {@code this.client} is not set by this constructor (there is no {@code client}
     * parameter), so the {@code if (client != null)} check below always evaluates against the field's default
     * value ({@code null}) and never actually assigns {@link #Config} here — {@link #Config} stays {@code null}
     * unless set later some other way. In practice this constructor path always renders using {@link #previewIcon}
     * rather than a config-provided camo.
     *
     * @param preview the preview camo/unit icon to display
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

            /** Paints a fixed-size tiled hex background followed by the unit icon, centered in the label. */
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

    /**
     * Looks up the base (uncolorized) sprite image for an entity from the shared MegaMek tileset, lazily loading
     * the tileset from {@code data/images/units/mechset.txt} on first use.
     * <p>
     * The {@code component} parameter is accepted for API compatibility but, per the existing code comment, appears
     * to have no actual effect on the result ({@link MekTileset#imageFor} is called with facing {@code -1}
     * regardless).
     *
     * @param entity    the unit to find a sprite for
     * @param component unused by the current {@link MekTileset} implementation; kept for signature compatibility
     *
     * @return the base sprite image for the entity, or a tileset-defined default if no exact match exists
     */
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

    /** Sets the text shown in the name label under the unit image. */
    public void setText(String text) {
        lblName.setText(text);
    }

    /** @return the {@link Image} currently backing the unit icon label. */
    public Image getEmbeddedImage() {
        return ((ImageIcon) lblImage.getIcon()).getImage();
    }

    /** Sets the fallback camo icon used when no {@link #Config}-provided camo is available. */
    public void setPreviewIcon(ImageIcon preview) {
        previewIcon = preview;
    }

    /**
     * Displays the given entity's sprite (colorized with the configured or preview camo), clearing any previously
     * bound campaign unit ({@link #cm} is reset to {@code null}) so no status icons will be drawn on top.
     *
     * @param entity the raw MegaMek entity to render (not tied to a campaign unit)
     */
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

    /** Scales the given image to the current cell size and sets it as the unit icon label's image. */
    public void setImage(Image img) {
        lblImage.setIcon(new ImageIcon(img.getScaledInstance(cellWidth, 74, Image.SCALE_DEFAULT)));
    }

    /**
     * Binds this panel to a live campaign unit (and its army, for commander-icon lookups) and renders its sprite,
     * colorized with the configured or preview camo. Once bound, the anonymous {@code lblImage} paint override will
     * also draw the left/right status-icon columns (pilot/repair/engine/equipment/armor/ammo/commander) for this
     * unit, since {@link #cm} is no longer {@code null}. Does nothing if {@code cm} is {@code null} — in particular
     * it does NOT clear a previously bound unit/army in that case.
     * <p>
     * If sprite lookup fails, falls back to loading {@code ./data/images/ImageMissing.png} as a placeholder; if
     * that also fails, {@code unit} is left {@code null} and rendering downstream will likely throw/skip.
     *
     * @param cm   the campaign unit to display; if {@code null}, this method is a no-op
     * @param army the army the unit belongs to, used to check commander status for the status icons
     */
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

    /** Shows or hides the unit image label (e.g. to hide the sprite while keeping the name label visible). */
    public void setImageVisible(boolean flag) {
        lblImage.setVisible(flag);
    }

    /**
     * A class to handle the image permutations for an entity (Code from megamek.common.TilesetManager class)
     * <p>
     * Wraps a base unit sprite plus an optional camo pattern and tint color, and knows how to recolor the sprite
     * (via pixel-level manipulation, see {@link #applyColor}) and pre-render it at all six hex facings (see
     * {@link #loadFacings}), or just recolor it once for a static preview (see {@link #loadPreviewImage}, used by
     * {@link MekInfo}).
     */
    private static class EntityImage {
        /** Solid tint color (as 0xRRGGBB) applied when no camo image is available. */
        private final int tint;
        /** Camo pattern image to blend with the sprite's shading, or {@code null} to use the solid {@link #tint}. */
        private final Image camo;
        /** The base sprite pre-rotated to each of the six hex facings (0-5), populated by {@link #loadFacings}. */
        private final Image[] facings = new Image[6];
        /** The wreck sprite pre-rotated to each of the six hex facings, populated by {@link #loadFacings}. */
        private final Image[] wreckFacings = new Image[6];
        /** The AWT component used as the image-producer's context (needed by {@link Component#createImage}). */
        private final Component comp;
        private final int IMG_WIDTH = 84;
        private final int IMG_HEIGHT = 72;
        private final int IMG_SIZE = IMG_WIDTH * IMG_HEIGHT;
        /** The (initially uncolorized) base unit sprite; replaced in-place with the colorized version. */
        private Image base;
        /** The (initially uncolorized) destroyed/wreck sprite, or {@code null} if none was supplied. */
        private Image wreck;
        /** A small (56x48) colorized preview icon derived from {@link #base}, set by {@link #loadFacings}. */
        private Image icon;

        /** Convenience constructor for an entity with no separate wreck sprite. */
        public EntityImage(Image base, int tint, Image camo, Component comp) {
            this(base, null, tint, camo, comp);
        }

        /**
         * @param base  the base (uncolorized) unit sprite
         * @param wreck the base (uncolorized) wreck sprite, or {@code null} if this entity type has none
         * @param tint  solid fallback color (0xRRGGBB) used when {@code camo} is {@code null}
         * @param camo  camo pattern image to recolor the sprite with, or {@code null} for a solid tint
         * @param comp  component used to create derived images via {@link Component#createImage}
         */
        public EntityImage(Image base, Image wreck, int tint, Image camo, Component comp) {
            this.base = base;
            this.tint = tint;
            this.camo = camo;
            this.comp = comp;
            this.wreck = wreck;
        }

        /**
         * Colorizes {@link #base} (and {@link #wreck}, if present) and pre-renders both at all six hex facings by
         * rotating the colorized image in 60-degree increments, plus derives a small preview {@link #icon}. Used
         * when the full set of facings is needed (e.g. rendering a unit on the hex map), unlike
         * {@link #loadPreviewImage} which only needs a single static image.
         */
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

        /**
         * Recolors {@code image} pixel-by-pixel: for every non-transparent pixel, the sprite's original brightness
         * (taken from its blue channel, per the base sprite's grayscale-shading convention — see {@code black}
         * below) is used to scale either the camo pattern's color at that pixel (if {@link #camo} is set) or the
         * solid {@link #tint} color, preserving shading/shadow detail while replacing the hue. Falls back to
         * returning the original {@code image} unchanged if pixel grabbing fails or is aborted.
         *
         * @param image the uncolorized sprite (or wreck sprite) to recolor
         *
         * @return a new recolored image, or the original {@code image} if recoloring could not be performed
         */
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

        /**
         * Recolors {@link #base} in place and returns it, without generating rotated facings or a preview icon.
         * Cheaper than {@link #loadFacings} for callers (like {@link MekInfo}) that only need a single static
         * image rather than all six hex-facing orientations.
         *
         * @return the colorized base sprite
         */
        public Image loadPreviewImage() {
            base = applyColor(base);
            return base;
        }

        /** @return the pre-rendered sprite rotated for the given hex facing (0-5); requires {@link #loadFacings} to have run. */
        public Image getFacing(int facing) {
            return facings[facing];
        }

        /** @return the pre-rendered wreck sprite rotated for the given hex facing (0-5); requires {@link #loadFacings} to have run. */
        public Image getWreckFacing(int facing) {
            return wreckFacings[facing];
        }

        /** @return the colorized base sprite (facing-independent). */
        public Image getBase() {
            return base;
        }

        /** @return the small colorized preview icon derived in {@link #loadFacings}. */
        public Image getIcon() {
            return icon;
        }
    }

}
