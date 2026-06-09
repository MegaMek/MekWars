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

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.ImageObserver;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Dictionary;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import jakarta.annotation.Nullable;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;

public class MyImageView extends View implements ImageObserver, MouseListener, MouseMotionListener {
    public static final String TOP = "top";

    public static final String TEXT_TOP = "text_top";
    public static final String MIDDLE = "middle";
    public static final String ABS_MIDDLE = "abs_middle";
    public static final String CENTER = "center";
    public static final String BOTTOM = "bottom";
    private static final MMLogger LOGGER = MMLogger.create(MyImageView.class);
    private static final String IMAGE_CACHE_PROPERTY = "imageCache";
    private static final String PENDING_IMAGE_SRC = "icons/image-delayed.gif";
    private static final String MISSING_IMAGE_SRC = "icons/image-failed.gif";
    private static final int DEFAULT_WIDTH = 32;
    private static final int DEFAULT_HEIGHT = 32;
    private static final int DEFAULT_BORDER = 2;
    /**
     * Static properties for incremental drawing. Swiped from Component.java
     *
     * @see #imageUpdate
     */
    private static boolean sIsInc = true;
    private static int sIncRate = 100;
    private static Icon sPendingImageIcon;
    private static Icon sMissingImageIcon;
    private final AttributeSet attr;
    private Element fElement;
    private Image fImage;
    private int fHeight, fWidth;
    private Container fContainer;
    private Rectangle fBounds;
    private Component fComponent;
    private Point fGrowBase;        // base of drag while growing image
    /**
     * Set to true, while the receiver is locked, to indicate the reciever is loading the image. This is used in
     * imageUpdate.
     */
    private boolean loading;


    // --- Painting --------------------------------------------------------

    /**
     * Creates a new view that represents an IMG element.
     *
     * @param elem the element to create a view for
     */
    public MyImageView(Element elem) {
        super(elem);
        initialize(elem);
        StyleSheet sheet = getStyleSheet();
        attr = sheet.getViewAttributes(this);
    }

    private void initialize(Element elem) {
        synchronized (this) {
            loading = true;
            fWidth = fHeight = 0;
        }

        int width = 0;
        int height = 0;
        boolean customWidth = false;
        boolean customHeight = false;
        try {
            fElement = elem;

            if (isURL()) {
                URI src = getSourceURL();
                if (src != null) {
                    Dictionary<URL, Image> cache = (Dictionary<URL, Image>) getDocument().getProperty(
                          IMAGE_CACHE_PROPERTY);
                    if (cache != null) {
                        fImage = cache.get(src);
                    } else {
                        try {
                            fImage = Toolkit.getDefaultToolkit().getImage(src.toURL());
                        } catch (IllegalArgumentException | MalformedURLException ex) {
                            LOGGER.error(ex, "Illegal argument or malformed URL: {}", ex.getLocalizedMessage());
                            return;
                        }
                    }
                }
            } else {
                String src = (String) fElement.getAttributes().getAttribute(HTML.Attribute.SRC);
                src = processSrcPath(src);
                fImage = Toolkit.getDefaultToolkit().createImage(src);

                try {
                    waitForImage();
                } catch (InterruptedException ignored) {
                    fImage = null;
                }

            }

            // Get height/width from params or image or defaults:
            height = getIntAttr(HTML.Attribute.HEIGHT, -1);
            customHeight = (height > 0);

            if (!customHeight && fImage != null) {
                height = fImage.getHeight(this);
            }

            if (height <= 0) {
                height = DEFAULT_HEIGHT;
            }

            width = getIntAttr(HTML.Attribute.WIDTH, -1);
            customWidth = (width > 0);

            if (!customWidth && fImage != null) {
                width = fImage.getWidth(this);
            }

            if (width <= 0) {
                width = DEFAULT_WIDTH;
            }

            // Make sure the image starts loading:
            if (fImage != null) {
                if (customWidth && customHeight) {
                    Toolkit.getDefaultToolkit().prepareImage(fImage, height, width, this);
                } else {
                    Toolkit.getDefaultToolkit().prepareImage(fImage, -1, -1, this);
                }
            }
        } finally {
            synchronized (this) {
                loading = false;

                if (customWidth || fWidth == 0) {
                    fWidth = width;
                }

                if (customHeight || fHeight == 0) {
                    fHeight = height;
                }
            }
        }
    }

    protected StyleSheet getStyleSheet() {
        HTMLDocument doc = (HTMLDocument) getDocument();
        return doc.getStyleSheet();
    }

    /** Determines if path is in the form of a URL */
    private boolean isURL() {
        String src = (String) fElement.getAttributes().getAttribute(HTML.Attribute.SRC);
        return src.toLowerCase().startsWith("file") || src.toLowerCase().startsWith("http");
    }

    /**
     * Return a URL for the image source, or null if it could not be determined.
     */
    private @Nullable URI getSourceURL() {
        String src = (String) fElement.getAttributes().getAttribute(HTML.Attribute.SRC);

        if (src == null) {
            return null;
        }

        try {
            URL base = ((HTMLDocument) getDocument()).getBase();
            URI path = new URI(src);

            return new URI(base.toString() + path);
        } catch (URISyntaxException e) {
            LOGGER.error(e, "Malformed or URI Exception: error: {}", e.getLocalizedMessage());
            return null;
        }
    }

    // --- Progressive display ---------------------------------------------

    /**
     * Checks to see if the absolute path is availabe thru an application global static variable or thru a system
     * variable. If so, appends the relative path to the absolute path and returns the String.
     */
    private String processSrcPath(String src) {
        File imageFile = new File(src);

        if (imageFile.isAbsolute()) {
            return src;
        }

        return src;
    }

    /**
     * Added this guy to make sure an image is loaded - ie no broken images. So far its used only for images loaded off
     * the disk (non-URL). It seems to work marvelously. By the way, it does the same thing as MediaTracker, but you
     * dont need to know the component its being rendered on. Rob
     */
    private void waitForImage() throws InterruptedException {
        int w;
        int h;

        w = fImage.getWidth(this);
        h = fImage.getHeight(this);

        while (true) {
            int flags = Toolkit.getDefaultToolkit().checkImage(fImage, w, h, this);

            if (((flags & ERROR) != 0) || ((flags & ABORT) != 0)) {
                throw new InterruptedException();
            } else if ((flags & (ALLBITS | FRAMEBITS)) != 0) {
                return;
            }

            Thread.sleep(10);
        }
    }

    /** Look up an integer-valued attribute. <b>Not</b> recursive. */
    private int getIntAttr(HTML.Attribute name, int defaultValue) {
        AttributeSet attr = fElement.getAttributes();

        if (attr.isDefined(name)) {        // does not check parents!
            int i;

            String val = (String) attr.getAttribute(name);
            if (val == null) {
                i = defaultValue;
            } else {
                i = Math.max(0, MathUtility.parseInt(val, defaultValue));
            }

            return i;
        }

        //else
        return defaultValue;
    }

    // --- Layout ----------------------------------------------------------

    /** Returns the text editor's highlight color. */
    protected Color getHighlightColor() {
        JTextComponent textComp = (JTextComponent) fContainer;
        return textComp.getSelectionColor();
    }

    // This can come on any thread. If we are in the process of reloading
    // the image and determining our state (loading == true) we don't fire
    // preference changed, or repaint, we just reset the fWidth/fHeight as
    // necessary and return. This is ok as we know when loading finishes
    // it will pick up the new height/width, if necessary.
    public boolean imageUpdate(Image img, int flags, int x, int y, int width, int height) {
        if (fImage == null || fImage != img) {
            return false;
        }

        // Bail out if there was an error:
        if ((flags & (ABORT | ERROR)) != 0) {
            fImage = null;
            repaint(0);
            return false;
        }

        // Resize image if necessary:
        short changed = 0;
        if ((flags & ImageObserver.HEIGHT) != 0) {
            if (!getElement().getAttributes().isDefined(HTML.Attribute.HEIGHT)) {
                changed |= 1;
            }
        }

        if ((flags & ImageObserver.WIDTH) != 0) {
            if (!getElement().getAttributes().isDefined(HTML.Attribute.WIDTH)) {
                changed |= 2;
            }
        }

        synchronized (this) {
            if ((changed & 1) == 1) {
                fWidth = width;
            }

            if ((changed & 2) == 2) {
                fHeight = height;
            }

            if (loading) {
                // No need to resize or repaint, still in the process of loading.
                return true;
            }
        }
        if (changed != 0) {
            // May need to resize myself, asynchronously:
            LOGGER.debug("ImageView: resized to {}x{}", fWidth, fHeight);

            Document doc = getDocument();
            try {
                if (doc instanceof AbstractDocument abstractDocument) {
                    abstractDocument.readLock();
                }

                preferenceChanged(this, true, true);
            } finally {
                if (doc instanceof AbstractDocument abstractDocument) {
                    abstractDocument.readUnlock();
                }
            }

            return true;
        }

        // Repaint when done or when new pixels arrive:
        if ((flags & (FRAMEBITS | ALLBITS)) != 0) {
            repaint(0);
        } else if ((flags & SOMEBITS) != 0) {
            if (sIsInc) {
                repaint(sIncRate);
            }
        }

        return ((flags & ALLBITS) == 0);
    }

    /**
     * Request that this view be repainted. Assumes the view is still at its last-drawn location.
     */
    protected void repaint(long delay) {
        if (fContainer != null && fBounds != null) {
            fContainer.repaint(delay, fBounds.x, fBounds.y, fBounds.width, fBounds.height);
        }
    }

    /**
     * Determines the preferred span for this view along an axis.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     *
     * @returns the span the view would like to be rendered into. Typically the view is told to render into the
     *       span that is returned, although there is no guarantee. The parent may choose to resize or break the view.
     */
    @Override
    public float getPreferredSpan(int axis) {
        int extra = 2 * (getBorder() + getSpace(axis));
        return switch (axis) {
            case View.X_AXIS -> fWidth + extra;
            case View.Y_AXIS -> fHeight + extra;
            default -> throw new IllegalArgumentException(STR."Invalid axis: \{axis}");
        };
    }

    /**
     * Determines the desired alignment for this view along an axis.  This is implemented to give the alignment to the
     * bottom of the icon along the y axis, and the default along the x axis.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     *
     * @returns the desired alignment.  This should be a value between 0.0 and 1.0 where 0 indicates alignment at
     *       the origin and 1.0 indicates alignment to the full span away from the origin.  An alignment of 0.5 would be
     *       the center of the view.
     */
    @Override
    public float getAlignment(int axis) {
        if (axis == View.Y_AXIS) {
            return getVerticalAlignment();
        }

        return super.getAlignment(axis);
    }

    /**
     * Paints the image.
     *
     * @param graphics the rendering surface to use
     * @param shape    the allocated region to render into
     *
     * @see View#paint
     */
    @Override
    public void paint(Graphics graphics, Shape shape) {
        Color oldColor = graphics.getColor();
        fBounds = shape.getBounds();

        int border = getBorder();
        int x = fBounds.x + border + getSpace(X_AXIS);
        int y = fBounds.y + border + getSpace(Y_AXIS);
        int width = fWidth;
        int height = fHeight;
        int sel = getSelectionState();

        // If no pixels yet, draw gray outline and icon:
        if (!hasPixels(this)) {
            graphics.setColor(Color.lightGray);
            graphics.drawRect(x, y, width - 1, height - 1);
            graphics.setColor(oldColor);
            loadIcons();
            Icon icon = fImage == null ? sMissingImageIcon : sPendingImageIcon;

            if (icon != null) {
                icon.paintIcon(getContainer(), graphics, x, y);
            }
        }

        // Draw image:
        if (fImage != null) {
            graphics.drawImage(fImage, x, y, width, height, this);
        }

        // If selected exactly, we need shape black border & grow-box:
        Color borderColor = getBorderColor();
        if (sel == 2) {
            // Make sure there's room for shape border:
            int delta = 2 - border;
            if (delta > 0) {
                x += delta;
                y += delta;
                width -= delta << 1;
                height -= delta << 1;
                border = 2;
            }
            borderColor = null;
            graphics.setColor(Color.black);
            // Draw grow box:
            graphics.fillRect(x + width - 5, y + height - 5, 5, 5);
        }

        // Draw border:
        if (border > 0) {
            if (borderColor != null) {
                graphics.setColor(borderColor);
            }

            // Draw shape thick rectangle:
            for (int i = 1; i <= border; i++) {
                graphics.drawRect(x - i, y - i, width - 1 + i + i, height - 1 + i + i);
            }

            graphics.setColor(oldColor);
        }
    }

    // --- Mouse event handling --------------------------------------------

    /**
     * Determines whether the image is selected, and if it's the only thing selected.
     *
     * @return 0 if not selected, 1 if selected, 2 if exclusively selected. "Exclusive" selection is only returned when
     *       editable.
     */
    protected int getSelectionState() {
        int p0 = fElement.getStartOffset();
        int p1 = fElement.getEndOffset();

        if (fContainer instanceof JTextComponent textComp) {
            int start = textComp.getSelectionStart();
            int end = textComp.getSelectionEnd();
            if (start <= p0 && end >= p1) {
                if (start == p0 && end == p1 && isEditable()) {
                    return 2;
                }
                //else
                return 1;
            }
        }

        return 0;
    }

    boolean hasPixels(ImageObserver imageObserver) {
        return fImage != null && fImage.getHeight(imageObserver) > 0 && fImage.getWidth(imageObserver) > 0;
    }

    private void loadIcons() {
        try {
            if (sPendingImageIcon == null) {
                sPendingImageIcon = makeIcon(PENDING_IMAGE_SRC);
            }
            if (sMissingImageIcon == null) {
                sMissingImageIcon = makeIcon(MISSING_IMAGE_SRC);
            }
        } catch (Exception x) {
            LOGGER.error(x, "ImageView: Couldn't load image icons");
        }
    }

    /** Returns the border's color, or null if this is not a link. */
    Color getBorderColor() {
        StyledDocument doc = (StyledDocument) getDocument();
        return doc.getForeground(getAttributes());
    }

    protected boolean isEditable() {
        return fContainer instanceof JEditorPane && ((JEditorPane) fContainer).isEditable();
    }

    private Icon makeIcon(final String gifFile) throws IOException {
        /*
         * Copy a resource into a byte array.  This is necessary because several browsers consider Class.getResource
         * a security risk because it can be used to load additional classes. Class.getResourceAsStream just returns raw
         * bytes, which we can convert to an image.
         */
        InputStream resource = MyImageView.class.getResourceAsStream(gifFile);

        if (resource == null) {
            return null;
        }

        BufferedInputStream in = new BufferedInputStream(resource);
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];

        int n;

        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }

        in.close();
        out.flush();

        buffer = out.toByteArray();
        if (buffer.length == 0) {
            LOGGER.debug("warning: {} is zero-length", gifFile);
            return null;
        }

        return new ImageIcon(buffer);
    }

    /**
     * Establishes the parent view for this view. Seize this moment to cache the AWT Container I'm in.
     */
    @Override
    public void setParent(View parent) {
        super.setParent(parent);
        fContainer = parent != null ? getContainer() : null;

        if (parent == null && fComponent != null) {
            fComponent.getParent().remove(fComponent);
            fComponent = null;
        }
    }

    // --- Static icon accessors -------------------------------------------

    /**
     * Provides shape mapping from the document model coordinate space to the coordinate space of the view mapped to
     * it.
     *
     * @param pos   the position to convert
     * @param shape the allocated region to render into
     *
     * @return the bounding box of the given position
     *
     * @see View#modelToView
     */
    @Override
    public Shape modelToView(int pos, Shape shape, Position.Bias bias) {
        int startOffset = getStartOffset();
        int endOffset = getEndOffset();

        if ((pos >= startOffset) && (pos <= endOffset)) {
            Rectangle rectangle = shape.getBounds();

            if (pos == endOffset) {
                rectangle.x += rectangle.width;
            }

            rectangle.width = 0;
            return rectangle;
        }

        return null;
    }

    /**
     * Provides shape mapping from the view coordinate space to the logical coordinate space of the model.
     *
     * @param x     the X coordinate
     * @param y     the Y coordinate
     * @param shape the allocated region to render into
     *
     * @return the location within the model that best represents the given point of view
     *
     * @see View#viewToModel
     */
    @Override
    public int viewToModel(float x, float y, Shape shape, Position.Bias[] bias) {
        Rectangle alloc = (Rectangle) shape;

        if (x < alloc.x + alloc.width) {
            bias[0] = Position.Bias.Forward;
            return getStartOffset();
        }

        bias[0] = Position.Bias.Backward;
        return getEndOffset();
    }

    /** My attributes may have changed. */
    @Override
    public void changedUpdate(DocumentEvent documentEvent, Shape shape, ViewFactory viewFactory) {
        LOGGER.debug("ImageView: changedUpdate begin...");
        super.changedUpdate(documentEvent, shape, viewFactory);
        float align = getVerticalAlignment();

        int height = fHeight;
        int width = fWidth;

        initialize(getElement());

        boolean hChanged = fHeight != height;
        boolean wChanged = fWidth != width;
        if (hChanged || wChanged || getVerticalAlignment() != align) {
            LOGGER.debug("ImageView: calling preferenceChanged");
            getParent().preferenceChanged(this, hChanged, wChanged);
        }

        LOGGER.debug("ImageView: changedUpdate end; valign={}", getVerticalAlignment());
    }

    // --- member variables ------------------------------------------------

    /**
     * Fetches the attributes to use when rendering.  This is implemented to multiplex the attributes specified in the
     * model with a StyleSheet.
     */
    @Override
    public AttributeSet getAttributes() {
        return attr;
    }

    /** Returns the image's vertical alignment. */
    float getVerticalAlignment() {
        String align = (String) fElement.getAttributes().getAttribute(HTML.Attribute.ALIGN);

        if (align != null) {
            align = align.toLowerCase();
            if (align.equals(TOP) || align.equals(TEXT_TOP)) {
                return 0.0f;
            } else if (align.equals(MyImageView.CENTER) || align.equals(MIDDLE) || align.equals(ABS_MIDDLE)) {
                return 0.5f;
            }
        }
        return 1.0f;        // default alignment is bottom
    }

    /** Returns the size of the border to use. */
    int getBorder() {
        return getIntAttr(HTML.Attribute.BORDER, isLink() ? DEFAULT_BORDER : 0);
    }

    /** Returns the amount of extra space to add along an axis. */
    int getSpace(int axis) {
        return getIntAttr(axis == X_AXIS ? HTML.Attribute.HSPACE : HTML.Attribute.VSPACE, 0);
    }

    /** Is this image within a link? */
    boolean isLink() {
        //! It would be nice to cache this but in an editor it can change
        // See if I have an HREF attribute courtesy of the enclosing A tag:
        AttributeSet anchorAttr = (AttributeSet) fElement.getAttributes().getAttribute(HTML.Tag.A);

        if (anchorAttr != null) {
            return anchorAttr.isDefined(HTML.Attribute.HREF);
        }

        return false;
    }

    /** Resize image if initial click was in grow-box: */
    public void mouseDragged(MouseEvent mouseEvent) {
        if (fGrowBase != null) {
            Point loc = fComponent.getLocationOnScreen();
            int width = Math.max(2, loc.x + mouseEvent.getX() - fGrowBase.x);
            int height = Math.max(2, loc.y + mouseEvent.getY() - fGrowBase.y);

            if (mouseEvent.isShiftDown() && fImage != null) {
                // Make sure size is proportional to actual image size:
                float imgWidth = fImage.getWidth(this);
                float imgHeight = fImage.getHeight(this);
                if (imgWidth > 0 && imgHeight > 0) {
                    float prop = imgHeight / imgWidth;
                    float propWidth = height / prop;
                    float propHeight = width * prop;

                    if (propWidth > width) {
                        width = (int) propWidth;
                    } else {
                        height = (int) propHeight;
                    }
                }
            }

            resize(width, height);
        }
    }

    /**
     * Change the size of this image. This alters the HEIGHT and WIDTH attributes of the Element and causes a
     * re-layout.
     */
    protected void resize(int width, int height) {
        if (width == fWidth && height == fHeight) {
            return;
        }

        fWidth = width;
        fHeight = height;

        // Replace attributes in document:
        MutableAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(HTML.Attribute.WIDTH, Integer.toString(width));
        attr.addAttribute(HTML.Attribute.HEIGHT, Integer.toString(height));
        ((StyledDocument) getDocument()).setCharacterAttributes(fElement.getStartOffset(),
              fElement.getEndOffset(),
              attr,
              false);
    }

    public void mouseMoved(MouseEvent mouseEvent) {
    }

    // --- constants and static stuff --------------------------------

    /** On double-click, open image properties dialog. */
    public void mouseClicked(MouseEvent mouseEvent) {
        //$ IMPLEMENT
    }

    /** Select or grow image when clicked. */
    public void mousePressed(MouseEvent mouseEvent) {
        Dimension size = fComponent.getSize();

        if (mouseEvent.getX() >= size.width - 7 && mouseEvent.getY() >= size.height - 7 && getSelectionState() == 2) {
            // Click in selected grow-box:
            LOGGER.debug("ImageView: grow!!! Size={}x{}", fWidth, fHeight);
            Point locationOnScreen = fComponent.getLocationOnScreen();
            fGrowBase = new Point(locationOnScreen.x + mouseEvent.getX() - fWidth,
                  locationOnScreen.y + mouseEvent.getY() - fHeight);
        } else {
            // Else select image:
            fGrowBase = null;
            JTextComponent comp = (JTextComponent) fContainer;

            int start = fElement.getStartOffset();
            int end = fElement.getEndOffset();
            int mark = comp.getCaret().getMark();
            int dot = comp.getCaret().getDot();
            if (mouseEvent.isShiftDown()) {
                // extend selection if shift key down:
                if (mark <= start) {
                    comp.moveCaretPosition(end);
                } else {
                    comp.moveCaretPosition(start);
                }
            } else {
                // just select image, without shift:
                if (mark != start) {
                    comp.setCaretPosition(start);
                }

                if (dot != end) {
                    comp.moveCaretPosition(end);
                }
            }
        }
    }

    public void mouseReleased(MouseEvent mouseEvent) {
        fGrowBase = null;
        //! Should post some command to make the action undo-able
    }

    public void mouseEntered(MouseEvent mouseEvent) {
    }

    public void mouseExited(MouseEvent mouseEvent) {
    }
}
