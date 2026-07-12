package mekwars.common.gui;

import java.awt.Point;

/**
 * Holds the display/configuration state for the MekWars in-game star map (rendered by {@code InnerStellarMap}):
 * zoom/scale, scroll offset, dot sizing bounds, per-layer visibility thresholds and toggles, background color, and
 * the currently-selected planet. This is a plain data holder (no persistence logic of its own) - callers are
 * responsible for reading/writing it to/from the client config.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public final class InnerStellarMapConfig {
    /**
     * Smallest allowed rendered diameter (in pixels) for a planet dot when scaling by zoom.
     * <p>
     * Note: despite the field-level Javadoc originally attached here ("Whether to scale planet dots on zoom or
     * not"), this field actually holds the minimum dot size, not a boolean toggle; there does not appear to be a
     * separate on/off flag for dot scaling in this class.
     */
    private int minDotSize = 2;
    /** Largest allowed rendered diameter (in pixels) for a planet dot when scaling by zoom. */
    private int maxDotSize = 25;
    /**
     * The scaling maximum dimension
     */
    private int reverseScaleMax = 100;
    /**
     * The scaling minimum dimension
     */
    private int reverseScaleMin = 2;
    /**
     * Threshold to not show influence anymore. 0 means show always
     */
    private double showInfluenceThreshold = 0.0;
    /**
     * Threshold to not show unit factories anymore. 0 means show always
     */
    private double showUnitFactoriesThreshold = 0.0;
    /**
     * Threshold to not show planet names. 0 means show always
     */
    private double showPlanetNamesThreshold = 0.0;
    /**
     * brightness correction for colors. This is no gamma correction! Gamma correction brightens medium-level colors
     * more than extreme ones. 0 means no brightening.
     */
    private double colorAdjustment = 0.5;
    /**
     * The maps background color
     */
    private String backgroundColor = "#000000";
    /**
     * Various per-layer display toggles for the map (e.g. planet names, faction control coloring, factories,
     * warehouses, ranges, recent changes). Note: the array has 8 elements but only 6 names are listed in this
     * comment, so the mapping of index to meaning for all 8 slots is not fully documented here; consult the
     * {@code InnerStellarMap} rendering code for the authoritative index-to-purpose mapping.
     */
    private boolean[] display = new boolean[] { true, false, true, true, true, true, true, true };
    /**
     * The actual scale factor. 1.0 for default, higher means bigger.
     */
    private double scale = 1.0;
    /**
     * The scrolling offset
     */
    private Point offset = new Point();
    /**
     * The current selected Planet-id
     */
    private int planetID;

    /**
     * @return the influence threshold below which faction-influence overlays are hidden ({@code 0} = always show).
     */
    public double getShowInfluenceThreshold() {
        return showInfluenceThreshold;
    }

    /** Sets the influence display threshold. */
    void setShowInfluenceThreshold(double showInfluenceThreshold) {
        this.showInfluenceThreshold = showInfluenceThreshold;
    }

    /**
     * @return the influence threshold below which unit-factory markers are hidden ({@code 0} = always show).
     */
    public double getShowUnitFactoriesThreshold() {
        return showUnitFactoriesThreshold;
    }

    /** Sets the unit-factory display threshold. */
    void setShowUnitFactoriesThreshold(double showUnitFactoriesThreshold) {
        this.showUnitFactoriesThreshold = showUnitFactoriesThreshold;
    }

    /**
     * @return the zoom threshold below which planet names are hidden ({@code 0} = always show).
     */
    public double getShowPlanetNamesThreshold() {
        return showPlanetNamesThreshold;
    }

    /** Sets the planet-name display threshold. */
    void setShowPlanetNamesThreshold(double showPlanetNamesThreshold) {
        this.showPlanetNamesThreshold = showPlanetNamesThreshold;
    }

    /**
     * @return the current color brightness adjustment (not gamma correction; {@code 0} = no brightening).
     */
    public double getColorAdjustment() {
        return colorAdjustment;
    }

    /** Sets the color brightness adjustment. */
    void setColorAdjustment(double colorAdjustment) {
        this.colorAdjustment = colorAdjustment;
    }

    /**
     * @return the map's background color as a hex RGB string (e.g. {@code "#000000"}).
     */
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /** Sets the map's background color (hex RGB string). */
    void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * @return the array of per-layer display toggles backing this config (returns the live array, not a copy).
     */
    public boolean[] getDisplay() {
        return display;
    }

    /** Replaces the array of per-layer display toggles. */
    void setDisplay(boolean[] display) {
        this.display = display;
    }

    /**
     * @return the current zoom/scale factor ({@code 1.0} = default).
     */
    public double getScale() {
        return scale;
    }

    /** Sets the zoom/scale factor. */
    void setScale(double scale) {
        this.scale = scale;
    }

    /**
     * @return the current scroll offset of the map view (returns the live {@link Point}, not a copy).
     */
    public Point getOffset() {
        return offset;
    }

    /** Sets the map's scroll offset. */
    void setOffset(Point offset) {
        this.offset = offset;
    }

    /**
     * @return the ID of the currently selected planet.
     */
    public int getPlanetID() {
        return planetID;
    }

    /** Sets the ID of the currently selected planet. */
    void setPlanetID(int planetID) {
        this.planetID = planetID;
    }

    /**
     * @return the lower bound of the scaling range used when computing reverse-scaled dot sizes.
     */
    public int getReverseScaleMin() {
        return reverseScaleMin;
    }

    /** Sets the lower bound of the reverse-scaling range. */
    void setReverseScaleMin(int reverseScaleMin) {
        this.reverseScaleMin = reverseScaleMin;
    }

    /**
     * @return the upper bound of the scaling range used when computing reverse-scaled dot sizes.
     */
    public int getReverseScaleMax() {
        return reverseScaleMax;
    }

    /** Sets the upper bound of the reverse-scaling range. */
    void setReverseScaleMax(int reverseScaleMax) {
        this.reverseScaleMax = reverseScaleMax;
    }

    /**
     * @return the minimum rendered planet dot diameter, in pixels.
     */
    public int getMinDotSize() {
        return minDotSize;
    }

    /** Sets the minimum rendered planet dot diameter. */
    void setMinDotSize(int minDotSize) {
        this.minDotSize = minDotSize;
    }

    /**
     * @return the maximum rendered planet dot diameter, in pixels.
     */
    public int getMaxDotSize() {
        return maxDotSize;
    }

    /** Sets the maximum rendered planet dot diameter. */
    void setMaxDotSize(int maxDotSize) {
        this.maxDotSize = maxDotSize;
    }
}
