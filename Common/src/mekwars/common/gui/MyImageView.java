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

/**
 * Custom Swing {@link View} implementation that renders {@code <IMG>} elements inside a {@link JEditorPane}'s
 * HTML document (used by MekWars' chat/info panes, e.g. planet descriptions and other HTML-formatted text that
 * embeds images). It is registered with a matching {@code ViewFactory}/{@code MyHTMLEditorKit} elsewhere in this
 * package so that {@code img} tags produce instances of this class instead of Swing's default {@code ImageView}.
 * <p>
 * This is derived from (and closely mirrors) the JDK's package-private {@code javax.swing.text.html.ImageView},
 * reimplemented here as a public class so MekWars' HTML editor kit can use it directly. Responsibilities include:
 * loading the image referenced by the element's {@code SRC} attribute (from a URL or from the local filesystem),
 * honoring explicit {@code WIDTH}/{@code HEIGHT}/{@code BORDER}/{@code ALIGN}/{@code HSPACE}/{@code VSPACE}
 * attributes or falling back to the image's natural size/defaults, showing a "pending" or "broken image" icon
 * while the image loads or if it fails to load, repainting incrementally as image data streams in (see
 * {@link #imageUpdate}), and supporting basic mouse-driven selection/resize interaction when embedded in an
 * editable document (click to select; drag the bottom-right corner of a selected image to resize it, optionally
 * holding shift to preserve aspect ratio).
 *
 * @see #paint(Graphics, Shape)
 * @see #imageUpdate(Image, int, int, int, int, int)
 */
public class MyImageView extends View implements ImageObserver, MouseListener, MouseMotionListener {
    /** {@code ALIGN} attribute value: align the image's top with the tallest element on the line. */
    public static final String TOP = "top";

    /** {@code ALIGN} attribute value: align the image's top with the top of the surrounding text. */
    public static final String TEXT_TOP = "text_top";
    /** {@code ALIGN} attribute value: align the image's vertical middle with the baseline. */
    public static final String MIDDLE = "middle";
    /** {@code ALIGN} attribute value: align the image's vertical middle with the middle of the surrounding text. */
    public static final String ABS_MIDDLE = "abs_middle";
    /** {@code ALIGN} attribute value: same vertical alignment handling as {@link #MIDDLE} in {@link #getVerticalAlignment()}. */
    public static final String CENTER = "center";
    /** {@code ALIGN} attribute value: align the image's bottom with the baseline (the default). */
    public static final String BOTTOM = "bottom";
    private static final MMLogger LOGGER = MMLogger.create(MyImageView.class);
    /** Document property key under which a {@code Dictionary<URL, Image>} pre-populated image cache may be stored. */
    private static final String IMAGE_CACHE_PROPERTY = "imageCache";
    /** Classpath-relative resource path for the "image is still loading" placeholder icon. */
    private static final String PENDING_IMAGE_SRC = "icons/image-delayed.gif";
    /** Classpath-relative resource path for the "image failed to load" placeholder icon. */
    private static final String MISSING_IMAGE_SRC = "icons/image-failed.gif";
    private static final int DEFAULT_WIDTH = 32;
    private static final int DEFAULT_HEIGHT = 32;
    /** Default border thickness (in pixels) drawn around images that are links, when no explicit BORDER is set. */
    private static final int DEFAULT_BORDER = 2;
    /**
     * Static properties for incremental drawing. Swiped from Component.java
     *
     * @see #imageUpdate
     */
    private static boolean sIsInc = true;
    /** Minimum delay (ms) between incremental repaints while an image is still streaming in, when {@link #sIsInc} is true. */
    private static int sIncRate = 100;
    /** Shared, lazily-loaded icon shown in place of an image that has not finished loading yet. */
    private static Icon sPendingImageIcon;
    /** Shared, lazily-loaded icon shown in place of an image that failed to load. */
    private static Icon sMissingImageIcon;
    /** This view's resolved attribute set (element attributes merged with the document's style sheet). */
    private final AttributeSet attr;
    /** The document element (the {@code <IMG>} tag) this view renders. */
    private Element fElement;
    /** The loaded (or still-loading) image to render; {@code null} if loading has not started or has failed. */
    private Image fImage;
    /** Current rendered height/width of the image, in pixels (may reflect a custom attribute, the image's natural
     *  size, or {@link #DEFAULT_WIDTH}/{@link #DEFAULT_HEIGHT}). */
    private int fHeight, fWidth;
    /** The AWT container (usually the {@link JEditorPane}) this view is ultimately rendered within. */
    private Container fContainer;
    /** The last allocated on-screen bounds for this view, cached so {@link #repaint(long)} knows what to invalidate. */
    private Rectangle fBounds;
    /**
     * An on-screen component associated with this view, used for grow-box drag coordinates.
     * <p>
     * NOTE (apparent bug): nowhere in this class is {@code fComponent} ever assigned a non-null value — it is only
     * read (in {@link #mousePressed} and {@link #mouseDragged}) and nulled out (in {@link #setParent}). As written,
     * it is always {@code null}, so {@link #mousePressed}/{@link #mouseDragged} will throw a
     * {@link NullPointerException} as soon as they dereference it, meaning the grow-box resize interaction is
     * effectively non-functional unless something outside this class sets {@code fComponent} via reflection or a
     * subclass.
     */
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
     * <p>
     * Delegates to {@link #initialize(Element)} to kick off image loading and size resolution, then resolves this
     * view's attribute set from the document's style sheet.
     *
     * @param elem the element to create a view for
     */
    public MyImageView(Element elem) {
        super(elem);
        initialize(elem);
        StyleSheet sheet = getStyleSheet();
        attr = sheet.getViewAttributes(this);
    }

    /**
     * (Re-)loads the image for {@code elem} and computes this view's rendered {@link #fWidth}/{@link #fHeight}.
     * Called both from the constructor and from {@link #changedUpdate} when the element's attributes change.
     * <p>
     * If the element's {@code SRC} looks like a URL (see {@link #isURL()}), the image is fetched via an optional
     * per-document image cache ({@link #IMAGE_CACHE_PROPERTY}) or {@link Toolkit#getImage(URL)}; if the source
     * cannot be resolved to a valid {@link URL} (an {@link IllegalArgumentException} or
     * {@link MalformedURLException}), this method returns early with {@code fImage} left {@code null} — the
     * {@code finally} block below still runs and falls back to the default width/height in that case. Otherwise
     * (a local/relative path) the image is loaded synchronously via {@link Toolkit#createImage(String)} and
     * {@link #waitForImage()} is used to block until it either finishes loading or errors, since local images
     * aren't already cached/streamed the way URL-backed ones can be.
     * <p>
     * Size resolution: explicit {@code HEIGHT}/{@code WIDTH} attributes win; otherwise the loaded image's natural
     * size is used; otherwise {@link #DEFAULT_HEIGHT}/{@link #DEFAULT_WIDTH}. Finally, {@link Toolkit#prepareImage}
     * is called to ensure asynchronous loading/decoding proceeds (feeding {@link #imageUpdate} callbacks) even
     * though a rough size is already known.
     *
     * @param elem the {@code <IMG>} element to (re)initialize this view from
     */
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

    /** @return the style sheet of the enclosing {@link HTMLDocument}, used to resolve this view's attributes. */
    protected StyleSheet getStyleSheet() {
        HTMLDocument doc = (HTMLDocument) getDocument();
        return doc.getStyleSheet();
    }

    /**
     * Determines if path is in the form of a URL
     * <p>
     * NOTE (potential bug): this reads the element's {@code SRC} attribute and calls {@code src.toLowerCase()}
     * without a null check; if an {@code <IMG>} element somehow has no {@code SRC} attribute, this throws a
     * {@link NullPointerException} rather than treating it as "not a URL".
     *
     * @return {@code true} if the {@code SRC} attribute starts with {@code "file"} or {@code "http"} (case
     *       insensitive)
     */
    private boolean isURL() {
        String src = (String) fElement.getAttributes().getAttribute(HTML.Attribute.SRC);
        return src.toLowerCase().startsWith("file") || src.toLowerCase().startsWith("http");
    }

    /**
     * Return a URL for the image source, or null if it could not be determined.
     * <p>
     * Resolves the element's {@code SRC} attribute against the document's base URL ({@link HTMLDocument#getBase()})
     * by naive string concatenation of the base URL and the parsed {@code SRC} {@link URI}.
     *
     * @return the resolved absolute source {@link URI}, or {@code null} if {@code SRC} is missing or malformed
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
     * <p>
     * NOTE (apparent dead code): both branches currently return {@code src} unchanged — whether or not
     * {@code imageFile.isAbsolute()} is true, the method is a no-op passthrough. Despite the doc comment's
     * description, no absolute-path lookup or path-joining actually happens here.
     *
     * @param src the raw {@code SRC} attribute value (relative or absolute path)
     *
     * @return {@code src}, unchanged
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
     * <p>
     * Busy-polls {@link Toolkit#checkImage} every 10ms until the image reports an error/abort (in which case this
     * throws {@link InterruptedException} to signal failure to the caller) or has at least one full frame decoded
     * ({@code ALLBITS}/{@code FRAMEBITS}), at which point it returns normally.
     *
     * @throws InterruptedException if the image fails to load (used here as a "loading failed" signal, not
     *       necessarily actual thread interruption) or if {@link Thread#sleep} is interrupted
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

    /**
     * Look up an integer-valued attribute. <b>Not</b> recursive.
     *
     * @param name         the HTML attribute to look up (checked only on this element, not inherited from parents)
     * @param defaultValue value to use if the attribute is undefined, has no value, or fails to parse as an integer
     *
     * @return the attribute's non-negative integer value, or {@code defaultValue}
     */
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

    /**
     * Returns the text editor's highlight color.
     *
     * @return the selection color of the {@link JTextComponent} this view is rendered in (assumes {@link #fContainer}
     *       is a {@link JTextComponent}; will throw {@link ClassCastException} otherwise)
     */
    protected Color getHighlightColor() {
        JTextComponent textComp = (JTextComponent) fContainer;
        return textComp.getSelectionColor();
    }

    // This can come on any thread. If we are in the process of reloading
    // the image and determining our state (loading == true) we don't fire
    // preference changed, or repaint, we just reset the fWidth/fHeight as
    // necessary and return. This is ok as we know when loading finishes
    // it will pick up the new height/width, if necessary.
    /**
     * {@link ImageObserver} callback invoked (possibly on any thread, e.g. the image-fetcher thread) as the image
     * referenced by this view loads. Ignores callbacks for images other than the currently tracked {@link #fImage}.
     * On an error/abort, drops the image (renders as "missing" thereafter) and repaints. Otherwise, if the
     * {@code HEIGHT}/{@code WIDTH} flags are set (and the element doesn't pin an explicit HTML attribute for that
     * dimension), updates {@link #fWidth}/{@link #fHeight} from the new size and, once loading has fully finished
     * (i.e. {@link #loading} is false — while still loading, the resize is deferred to when {@link #initialize}
     * completes), notifies the parent view via {@link #preferenceChanged} so the layout can adjust. Finally
     * schedules a repaint, either immediately (image fully/partially rendered: {@code FRAMEBITS}/{@code ALLBITS})
     * or throttled (partial data: {@code SOMEBITS}, subject to {@link #sIncRate} if {@link #sIsInc} is enabled).
     * <p>
     * NOTE (apparent bug): the bit used to gate the {@link #fWidth} update is set from the {@code HEIGHT} observer
     * flag, and the bit gating the {@link #fHeight} update is set from the {@code WIDTH} flag — i.e. the two are
     * swapped relative to what the variable names suggest. In practice this is usually harmless because real
     * image-loading callbacks tend to report both flags together with the current width and height, but a callback
     * reporting only one of {@code HEIGHT}/{@code WIDTH} would update the wrong dimension.
     *
     * @return {@code true} if more updates are needed (image not yet fully loaded), {@code false} once loading is
     *       complete, aborted, or this callback is for a stale/foreign image
     *
     * @see ImageObserver#imageUpdate(Image, int, int, int, int, int)
     */
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
            default -> throw new IllegalArgumentException(String.format("Invalid axis: %s", axis));
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

    /**
     * @param imageObserver observer to pass through to {@link Image#getHeight}/{@link Image#getWidth} (dimensions
     *                      may not be known yet without one)
     *
     * @return {@code true} if {@link #fImage} is non-null and has known, positive width and height (i.e. at least
     *       one frame of pixel data is available to draw)
     */
    boolean hasPixels(ImageObserver imageObserver) {
        return fImage != null && fImage.getHeight(imageObserver) > 0 && fImage.getWidth(imageObserver) > 0;
    }

    /**
     * Lazily loads the shared static "pending" and "missing" image placeholder icons (see {@link #makeIcon}) the
     * first time they're needed; subsequent calls are no-ops once both are cached.
     */
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

    /** @return {@code true} if this view is inside an editable {@link JEditorPane} (enables exclusive selection/resize). */
    protected boolean isEditable() {
        return fContainer instanceof JEditorPane && ((JEditorPane) fContainer).isEditable();
    }

    /**
     * Loads a small GIF icon bundled as a classpath resource next to this class (used for the pending/missing image
     * placeholders). Reads the resource fully into a byte array first (rather than handing a URL/stream directly to
     * {@link ImageIcon}) because, per the inline comment, some browsers/sandboxes treat {@code Class.getResource}
     * as a security risk since it can be used to load additional classes, whereas
     * {@code Class.getResourceAsStream} only exposes raw bytes.
     *
     * @param gifFile classpath-relative path to the GIF resource
     *
     * @return the loaded icon, or {@code null} if the resource is missing or empty
     *
     * @throws IOException if reading the resource stream fails
     */
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

    /**
     * My attributes may have changed.
     * <p>
     * Re-runs {@link #initialize(Element)} to reload the image/re-resolve size from the (possibly changed) element
     * attributes, then compares the old and new width/height/vertical-alignment to decide whether to notify the
     * parent view via {@link View#preferenceChanged} that a re-layout is needed.
     */
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

    /**
     * Returns the image's vertical alignment, derived from the {@code ALIGN} HTML attribute (case-insensitive):
     * {@code top}/{@code text_top} align to 0.0 (top of the line), {@code center}/{@code middle}/{@code abs_middle}
     * align to 0.5, and anything else (including no {@code ALIGN} attribute) defaults to 1.0 (bottom/baseline).
     *
     * @return a value in {@code [0.0, 1.0]} as used by {@link #getAlignment(int)}
     */
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

    /** Returns the size of the border to use: the explicit {@code BORDER} attribute if set, else {@link #DEFAULT_BORDER} for links or 0 otherwise. */
    int getBorder() {
        return getIntAttr(HTML.Attribute.BORDER, isLink() ? DEFAULT_BORDER : 0);
    }

    /**
     * Returns the amount of extra space to add along an axis, from the {@code HSPACE} ({@link #X_AXIS}) or
     * {@code VSPACE} ({@link #Y_AXIS}) HTML attribute, defaulting to 0.
     */
    int getSpace(int axis) {
        return getIntAttr(axis == X_AXIS ? HTML.Attribute.HSPACE : HTML.Attribute.VSPACE, 0);
    }

    /**
     * Is this image within a link?
     * <p>
     * Checked by looking for an enclosing {@code <A>} tag attribute set with a defined {@code HREF}. Deliberately
     * not cached (per the inline comment) since the surrounding markup can change in an editor.
     *
     * @return {@code true} if this image is wrapped in an {@code <a href="...">} anchor
     */
    boolean isLink() {
        //! It would be nice to cache this but in an editor it can change
        // See if I have an HREF attribute courtesy of the enclosing A tag:
        AttributeSet anchorAttr = (AttributeSet) fElement.getAttributes().getAttribute(HTML.Tag.A);

        if (anchorAttr != null) {
            return anchorAttr.isDefined(HTML.Attribute.HREF);
        }

        return false;
    }

    /**
     * Resize image if initial click was in grow-box:
     * <p>
     * If a grow-drag was started (see {@link #mousePressed}, which sets {@link #fGrowBase}), computes a new
     * width/height from how far the mouse has moved from the grow-box's screen-space anchor point, optionally
     * constraining the new size to the image's original aspect ratio when Shift is held, then applies it via
     * {@link #resize(int, int)}. No-op if no grow-drag is in progress.
     *
     * @see MouseMotionListener#mouseDragged(MouseEvent)
     */
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
     * <p>
     * No-ops if the requested size matches the current size. Otherwise updates {@link #fWidth}/{@link #fHeight}
     * immediately and writes new {@code WIDTH}/{@code HEIGHT} character attributes onto the underlying document
     * element, which will in turn trigger {@link #changedUpdate} to fire a layout preference change.
     *
     * @param width  new width in pixels
     * @param height new height in pixels
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

    /** No-op; mouse-move (hover, as opposed to drag) events are not currently handled by this view. */
    public void mouseMoved(MouseEvent mouseEvent) {
    }

    // --- constants and static stuff --------------------------------

    /** On double-click, open image properties dialog. Currently unimplemented (see the {@code $ IMPLEMENT} marker below). */
    public void mouseClicked(MouseEvent mouseEvent) {
        //$ IMPLEMENT
    }

    /**
     * Select or grow image when clicked.
     * <p>
     * If the click lands within 7 pixels of the bottom-right corner while the image is exclusively selected (see
     * {@link #getSelectionState()}), begins a grow-box resize drag by recording the screen-space anchor point in
     * {@link #fGrowBase} (consumed by {@link #mouseDragged}). Otherwise, clears any grow drag and instead adjusts
     * the enclosing text component's selection/caret to cover this image element, extending the existing selection
     * if Shift is held.
     * <p>
     * NOTE: see the caveat on {@link #fComponent} — {@code fComponent.getSize()} below will throw a
     * {@link NullPointerException} given the field is never populated elsewhere in this class.
     *
     * @see MouseListener#mousePressed(MouseEvent)
     */
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

    /** Ends any in-progress grow-box resize drag. Per the inline TODO, a resize is not posted as an undoable edit. */
    public void mouseReleased(MouseEvent mouseEvent) {
        fGrowBase = null;
        //! Should post some command to make the action undo-able
    }

    /** No-op; entering the image's bounds requires no state change. */
    public void mouseEntered(MouseEvent mouseEvent) {
    }

    /** No-op; leaving the image's bounds requires no state change. */
    public void mouseExited(MouseEvent mouseEvent) {
    }
}
