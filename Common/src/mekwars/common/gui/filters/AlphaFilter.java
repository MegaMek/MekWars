package mekwars.common.gui.filters;

/*
 * Original source: http://docs.rinet.ru/J21/ch25.htm#AnAlphaImageFilter Original author: Michael Morrison
 */
public class AlphaFilter extends java.awt.image.RGBImageFilter {
    int alphaLevel;

    public AlphaFilter(int alpha) {
        alphaLevel = alpha;
        canFilterIndexColorModel = true;
    }

    @Override
    public int filterRGB(int x, int y, int rgb) {
        // Adjust the alpha value
        int alpha = (rgb >> 24) & 0xff;
        alpha = (alpha * alphaLevel) / 255;

        // Return the result
        return ((rgb & 0x00ffffff) | (alpha << 24));
    }
}
