package mekwars.common.gui.filters;

/*
 * Original source: http://docs.rinet.ru/J21/ch25.htm#AnAlphaImageFilter Original author: Michael Morrison
 */

/**
 * An {@link java.awt.image.RGBImageFilter} that scales down the alpha (opacity) channel of every pixel in an
 * image, used to render images semi-transparently (e.g. disabled/greyed-out icons) in Swing UI.
 */
public class AlphaFilter extends java.awt.image.RGBImageFilter {
    /** Target opacity out of 255; each pixel's existing alpha is scaled proportionally to this maximum. */
    int alphaLevel;

    /**
     * @param alpha the maximum opacity (0-255) pixels should be scaled down to
     */
    public AlphaFilter(int alpha) {
        alphaLevel = alpha;
        canFilterIndexColorModel = true;
    }

    /** Scales the alpha channel of a single ARGB pixel by {@link #alphaLevel}/255, leaving RGB untouched. */
    @Override
    public int filterRGB(int x, int y, int rgb) {
        // Adjust the alpha value
        int alpha = (rgb >> 24) & 0xff;
        alpha = (alpha * alphaLevel) / 255;

        // Return the result
        return ((rgb & 0x00ffffff) | (alpha << 24));
    }
}
