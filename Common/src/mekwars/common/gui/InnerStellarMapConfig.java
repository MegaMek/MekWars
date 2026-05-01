package mekwars.common.gui;

import java.awt.Point;

/**
 * All configuration behaviour of InterStellarMap are saved here.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public final class InnerStellarMapConfig {
    /**
     * Whether to scale planet dots on zoom or not
     */
    private int minDotSize = 2;
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
     * Various display options - Names, Control, Factories, Warehouses, Ranges, Changes
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

    public double getShowInfluenceThreshold() {
        return showInfluenceThreshold;
    }

    void setShowInfluenceThreshold(double showInfluenceThreshold) {
        this.showInfluenceThreshold = showInfluenceThreshold;
    }

    public double getShowUnitFactoriesThreshold() {
        return showUnitFactoriesThreshold;
    }

    void setShowUnitFactoriesThreshold(double showUnitFactoriesThreshold) {
        this.showUnitFactoriesThreshold = showUnitFactoriesThreshold;
    }

    public double getShowPlanetNamesThreshold() {
        return showPlanetNamesThreshold;
    }

    void setShowPlanetNamesThreshold(double showPlanetNamesThreshold) {
        this.showPlanetNamesThreshold = showPlanetNamesThreshold;
    }

    public double getColorAdjustment() {
        return colorAdjustment;
    }

    void setColorAdjustment(double colorAdjustment) {
        this.colorAdjustment = colorAdjustment;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public boolean[] getDisplay() {
        return display;
    }

    void setDisplay(boolean[] display) {
        this.display = display;
    }

    public double getScale() {
        return scale;
    }

    void setScale(double scale) {
        this.scale = scale;
    }

    public Point getOffset() {
        return offset;
    }

    void setOffset(Point offset) {
        this.offset = offset;
    }

    public int getPlanetID() {
        return planetID;
    }

    void setPlanetID(int planetID) {
        this.planetID = planetID;
    }

    public int getReverseScaleMin() {
        return reverseScaleMin;
    }

    void setReverseScaleMin(int reverseScaleMin) {
        this.reverseScaleMin = reverseScaleMin;
    }

    public int getReverseScaleMax() {
        return reverseScaleMax;
    }

    void setReverseScaleMax(int reverseScaleMax) {
        this.reverseScaleMax = reverseScaleMax;
    }

    public int getMinDotSize() {
        return minDotSize;
    }

    void setMinDotSize(int minDotSize) {
        this.minDotSize = minDotSize;
    }

    public int getMaxDotSize() {
        return maxDotSize;
    }

    void setMaxDotSize(int maxDotSize) {
        this.maxDotSize = maxDotSize;
    }
}
