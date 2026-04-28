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

package mekwars.client.gui;

import javax.swing.text.BadLocationException;
import javax.swing.text.View;

import common.util.MWLogger;

public class MyImageView extends javax.swing.text.View
      implements java.awt.image.ImageObserver, java.awt.event.MouseListener, java.awt.event.MouseMotionListener {

    // --- Attribute Values ------------------------------------------

    public static final String
          TOP = "top",
          TEXTTOP = "texttop",
          MIDDLE = "middle",
          ABSMIDDLE = "absmiddle",
          CENTER = "center",
          BOTTOM = "bottom";


    // --- Construction ----------------------------------------------
    //$ move this someplace public
    static final String IMAGE_CACHE_PROPERTY = "imageCache";
    private static final String
          PENDING_IMAGE_SRC = "icons/image-delayed.gif",  // both stolen from HotJava
          MISSING_IMAGE_SRC = "icons/image-failed.gif";
    private static final boolean DEBUG = false;
    // Height/width to use before we know the real size:
    private static final int
          DEFAULT_WIDTH = 32,
          DEFAULT_HEIGHT = 32,
    // Default value of BORDER param:      //? possibly move into stylesheet?
    DEFAULT_BORDER = 2;
    /**
     * Static properties for incremental drawing. Swiped from Component.java
     *
     * @see #imageUpdate
     */
    private static boolean sIsInc = true;
    private static int sIncRate = 100;
    private static javax.swing.Icon sPendingImageIcon,
          sMissingImageIcon;
    private javax.swing.text.AttributeSet attr;
    private javax.swing.text.Element fElement;
    private java.awt.Image fImage;
    private int fHeight, fWidth;
    private java.awt.Container fContainer;
    private java.awt.Rectangle fBounds;
    private java.awt.Component fComponent;
    private java.awt.Point fGrowBase;        // base of drag while growing image
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
    public MyImageView(javax.swing.text.Element elem) {
        super(elem);
        initialize(elem);
        javax.swing.text.html.StyleSheet sheet = getStyleSheet();
        attr = sheet.getViewAttributes(this);
    }

    @SuppressWarnings("unchecked")
    private void initialize(javax.swing.text.Element elem) {
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

            // Request image from document's cache:
            //AttributeSet attr = elem.getAttributes();
            if (isURL()) {
                java.net.URL src = getSourceURL();
                if (src != null) {
                    java.util.Dictionary<java.net.URL, java.awt.Image> cache = (java.util.Dictionary<java.net.URL, java.awt.Image>) getDocument().getProperty(
                          IMAGE_CACHE_PROPERTY);
                    if (cache != null) {fImage = (java.awt.Image) cache.get(src);} else {
                        fImage = java.awt.Toolkit.getDefaultToolkit().getImage(src);
                    }
                }
            } else {

                /******** Code to load from relative path *************/
                String src =
                      (String) fElement.getAttributes().getAttribute
                                                             (javax.swing.text.html.HTML.Attribute.SRC);
                //MMClient.mwClientLog.clientOutputLog("before src: " + src);
                src = processSrcPath(src);
                //MMClient.mwClientLog.clientOutputLog("after src: " + src);
                fImage = java.awt.Toolkit.getDefaultToolkit().createImage(src);
                try {waitForImage();} catch (InterruptedException e) {fImage = null;}
                /******************************************************/

            }

            // Get height/width from params or image or defaults:
            height = getIntAttr(javax.swing.text.html.HTML.Attribute.HEIGHT, -1);
            customHeight = (height > 0);
            if (!customHeight && fImage != null) {height = fImage.getHeight(this);}
            if (height <= 0) {height = DEFAULT_HEIGHT;}

            width = getIntAttr(javax.swing.text.html.HTML.Attribute.WIDTH, -1);
            customWidth = (width > 0);
            if (!customWidth && fImage != null) {width = fImage.getWidth(this);}
            if (width <= 0) {width = DEFAULT_WIDTH;}

            // Make sure the image starts loading:
            if (fImage != null) {
                if (customWidth && customHeight) {
                    java.awt.Toolkit.getDefaultToolkit().prepareImage(fImage, height,
                          width, this);
                } else {
                    java.awt.Toolkit.getDefaultToolkit().prepareImage(fImage, -1, -1,
                          this);
                }
            }

            /********************************************************
             // Rob took this out. Changed scope of src.
             if( DEBUG ) {
             if( fImage != null )
             MMClient.mwClientLog.clientOutputLog("ImageInfo: new on "+src+
             " ("+fWidth+"x"+fHeight+")");
             else
             MMClient.mwClientLog.clientOutputLog("ImageInfo: couldn't get image at "+
             src);
             if(isLink())
             MMClient.mwClientLog.clientOutputLog("           It's a link! Border = "+
             getBorder());
             //((AbstractDocument.AbstractElement)elem).dump(System.out,4);
             }
             ********************************************************/
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

    protected javax.swing.text.html.StyleSheet getStyleSheet() {
        javax.swing.text.html.HTMLDocument doc = (javax.swing.text.html.HTMLDocument) getDocument();
        return doc.getStyleSheet();
    }

    /** Determines if path is in the form of a URL */
    private boolean isURL() {
        String src =
              (String) fElement.getAttributes().getAttribute(javax.swing.text.html.HTML.Attribute.SRC);
        return src.toLowerCase().startsWith("file") ||
                     src.toLowerCase().startsWith("http");
    }

    /**
     * Return a URL for the image source, or null if it could not be determined.
     */
    private java.net.URL getSourceURL() {
        String src = (String) fElement.getAttributes().getAttribute(javax.swing.text.html.HTML.Attribute.SRC);
        if (src == null) {return null;}

        java.net.URL reference = ((javax.swing.text.html.HTMLDocument) getDocument()).getBase();
        try {
            java.net.URL u = new java.net.URL(reference, src);
            return u;
        } catch (java.net.MalformedURLException e) {
            return null;
        }
    }

    // --- Progressive display ---------------------------------------------

    /**
     * Checks to see if the absolute path is availabe thru an application global static variable or thru a system
     * variable. If so, appends the relative path to the absolute path and returns the String.
     */
    private String processSrcPath(String src) {
        String val = src;

        java.io.File imageFile = new java.io.File(src);
        if (imageFile.isAbsolute()) {return src;}
		/* Won't work
		 //try to get application images path...
		  if (PicTest.ApplicationImagePath != null) {
		  String imagePath = PicTest.ApplicationImagePath;
		  val = (new File(imagePath, imageFile.getPath())).toString();
		  }
		  //try to get system images path...
		   else {
		   String imagePath = System.getProperty("system.image.path.key");
		   if (imagePath != null) {
		   val = (new File(imagePath, imageFile.getPath())).toString();
		   }
		   }
		   */
        //MMClient.mwClientLog.clientOutputLog("src before: " + src + ", src after: " + val);
        return val;
    }
    /*
     */

    /**
     * Added this guy to make sure an image is loaded - ie no broken images. So far its used only for images loaded off
     * the disk (non-URL). It seems to work marvelously. By the way, it does the same thing as MediaTracker, but you
     * dont need to know the component its being rendered on. Rob
     */
    private void waitForImage() throws InterruptedException {
        int w = 0;
        int h = 0;
        try {
            w = fImage.getWidth(this);
            h = fImage.getHeight(this);
        } catch (Exception ex) {
            return;
        }
        while (true) {
            int flags = java.awt.Toolkit.getDefaultToolkit().checkImage(fImage, w, h, this);

            if (((flags & ERROR) != 0) || ((flags & ABORT) != 0)) {throw new InterruptedException();} else if ((flags &
                                                                                                                      (ALLBITS |
                                                                                                                             FRAMEBITS)) !=
                                                                                                                     0) {
                return;
            }
            Thread.sleep(10);
            //MMClient.mwClientLog.clientOutputLog("rise and shine...");
        }
    }

    /** Look up an integer-valued attribute. <b>Not</b> recursive. */
    private int getIntAttr(javax.swing.text.html.HTML.Attribute name, int deflt) {
        javax.swing.text.AttributeSet attr = fElement.getAttributes();
        if (attr.isDefined(name)) {        // does not check parents!
            int i;
            String val = (String) attr.getAttribute(name);
            if (val == null) {i = deflt;} else {
                try {
                    i = Math.max(0, Integer.parseInt(val));
                } catch (NumberFormatException x) {
                    i = deflt;
                }
            }
            return i;
        }

        //else
        return deflt;
    }

    // --- Layout ----------------------------------------------------------

    /** Returns the text editor's highlight color. */
    protected java.awt.Color getHighlightColor() {
        javax.swing.text.JTextComponent textComp = (javax.swing.text.JTextComponent) fContainer;
        return textComp.getSelectionColor();
    }

    // This can come on any thread. If we are in the process of reloading
    // the image and determining our state (loading == true) we don't fire
    // preference changed, or repaint, we just reset the fWidth/fHeight as
    // necessary and return. This is ok as we know when loading finishes
    // it will pick up the new height/width, if necessary.
    public boolean imageUpdate(java.awt.Image img, int flags, int x, int y,
          int width, int height) {
        if (fImage == null || fImage != img) {return false;}

        // Bail out if there was an error:
        if ((flags & (ABORT | ERROR)) != 0) {
            fImage = null;
            repaint(0);
            return false;
        }

        // Resize image if necessary:
        short changed = 0;
        if ((flags & java.awt.image.ImageObserver.HEIGHT) != 0) {
            if (!getElement().getAttributes().isDefined(javax.swing.text.html.HTML.Attribute.HEIGHT)) {
                changed |= 1;
            }
        }
        if ((flags & java.awt.image.ImageObserver.WIDTH) != 0) {
            if (!getElement().getAttributes().isDefined(javax.swing.text.html.HTML.Attribute.WIDTH)) {
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
                // No need to resize or repaint, still in the process of
                // loading.
                return true;
            }
        }
        if (changed != 0) {
            // May need to resize myself, asynchronously:
            if (DEBUG) {MWLogger.infoLog("ImageView: resized to " + fWidth + "x" + fHeight);}

            javax.swing.text.Document doc = getDocument();
            try {
                if (doc instanceof javax.swing.text.AbstractDocument) {
                    ((javax.swing.text.AbstractDocument) doc).readLock();
                }
                preferenceChanged(this, true, true);
            } finally {
                if (doc instanceof javax.swing.text.AbstractDocument) {
                    ((javax.swing.text.AbstractDocument) doc).readUnlock();
                }
            }

            return true;
        }

        // Repaint when done or when new pixels arrive:
        if ((flags & (FRAMEBITS | ALLBITS)) != 0) {repaint(0);} else if ((flags & SOMEBITS) != 0) {
            if (sIsInc) {repaint(sIncRate);}
        }

        return ((flags & ALLBITS) == 0);
    }

    /**
     * Request that this view be repainted. Assumes the view is still at its last-drawn location.
     */
    protected void repaint(long delay) {
        if (fContainer != null && fBounds != null) {
            fContainer.repaint(delay,
                  fBounds.x, fBounds.y, fBounds.width, fBounds.height);
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
        //if(DEBUG)MMClient.mwClientLog.clientOutputLog("ImageView: getPreferredSpan");
        int extra = 2 * (getBorder() + getSpace(axis));
        switch (axis) {
            case javax.swing.text.View.X_AXIS:
                return fWidth + extra;
            case javax.swing.text.View.Y_AXIS:
                return fHeight + extra;
            default:
                throw new IllegalArgumentException("Invalid axis: " + axis);
        }
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
        switch (axis) {
            case javax.swing.text.View.Y_AXIS:
                return getVerticalAlignment();
            default:
                return super.getAlignment(axis);
        }
    }

    /**
     * Paints the image.
     *
     * @param g the rendering surface to use
     * @param a the allocated region to render into
     *
     * @see View#paint
     */
    @Override
    public void paint(java.awt.Graphics g, java.awt.Shape a) {
        java.awt.Color oldColor = g.getColor();
        fBounds = a.getBounds();
        int border = getBorder();
        int x = fBounds.x + border + getSpace(X_AXIS);
        int y = fBounds.y + border + getSpace(Y_AXIS);
        int width = fWidth;
        int height = fHeight;
        int sel = getSelectionState();

        // Make sure my Component is in the right place:
		/*
		 if( fComponent == null ) {
		 fComponent = new Component() { };
		 fComponent.addMouseListener(this);
		 fComponent.addMouseMotionListener(this);
		 fComponent.setCursor(Cursor.getDefaultCursor());	// use arrow cursor
		 fContainer.add(fComponent);
		 }
		 fComponent.setBounds(x,y,width,height);
		 */
        // If no pixels yet, draw gray outline and icon:
        if (!hasPixels(this)) {
            g.setColor(java.awt.Color.lightGray);
            g.drawRect(x, y, width - 1, height - 1);
            g.setColor(oldColor);
            loadIcons();
            javax.swing.Icon icon = fImage == null ? sMissingImageIcon : sPendingImageIcon;
            if (icon != null) {icon.paintIcon(getContainer(), g, x, y);}
        }

        // Draw image:
        if (fImage != null) {
            g.drawImage(fImage, x, y, width, height, this);
            // Use the following instead of g.drawImage when
            // BufferedImageGraphics2D.setXORMode is fixed (4158822).

            //  Use Xor mode when selected/highlighted.
            //! Could darken image instead, but it would be more expensive.
			/*
			 if( sel > 0 )
			 g.setXORMode(Color.white);
			 g.drawImage(fImage,x, y,
			 width,height,this);
			 if( sel > 0 )
			 g.setPaintMode();
			 */
        }

        // If selected exactly, we need a black border & grow-box:
        java.awt.Color bc = getBorderColor();
        if (sel == 2) {
            // Make sure there's room for a border:
            int delta = 2 - border;
            if (delta > 0) {
                x += delta;
                y += delta;
                width -= delta << 1;
                height -= delta << 1;
                border = 2;
            }
            bc = null;
            g.setColor(java.awt.Color.black);
            // Draw grow box:
            g.fillRect(x + width - 5, y + height - 5, 5, 5);
        }

        // Draw border:
        if (border > 0) {
            if (bc != null) {g.setColor(bc);}
            // Draw a thick rectangle:
            for (int i = 1; i <= border; i++) {g.drawRect(x - i, y - i, width - 1 + i + i, height - 1 + i + i);}
            g.setColor(oldColor);
        }
    }

    // --- Mouse event handling --------------------------------------------

    /** Returns the size of the border to use. */
    int getBorder() {
        return getIntAttr(javax.swing.text.html.HTML.Attribute.BORDER, isLink() ? DEFAULT_BORDER : 0);
    }

    /** Returns the amount of extra space to add along an axis. */
    int getSpace(int axis) {
        return getIntAttr(axis == X_AXIS ?
                                javax.swing.text.html.HTML.Attribute.HSPACE :
                                javax.swing.text.html.HTML.Attribute.VSPACE,
              0);
    }

    /**
     * Determines whether the image is selected, and if it's the only thing selected.
     *
     * @return 0 if not selected, 1 if selected, 2 if exclusively selected. "Exclusive" selection is only returned when
     *       editable.
     */
    protected int getSelectionState() {
        int p0 = fElement.getStartOffset();
        int p1 = fElement.getEndOffset();
        if (fContainer instanceof javax.swing.text.JTextComponent) {
            javax.swing.text.JTextComponent textComp = (javax.swing.text.JTextComponent) fContainer;
            int start = textComp.getSelectionStart();
            int end = textComp.getSelectionEnd();
            if (start <= p0 && end >= p1) {
                if (start == p0 && end == p1 && isEditable()) {return 2;}
                //else
                return 1;
            }
        }
        return 0;
    }

    boolean hasPixels(java.awt.image.ImageObserver obs) {
        return fImage != null && fImage.getHeight(obs) > 0
                     && fImage.getWidth(obs) > 0;
    }

    private void loadIcons() {
        try {
            if (sPendingImageIcon == null) {sPendingImageIcon = makeIcon(PENDING_IMAGE_SRC);}
            if (sMissingImageIcon == null) {sMissingImageIcon = makeIcon(MISSING_IMAGE_SRC);}
        } catch (Exception x) {
            MWLogger.errLog("ImageView: Couldn't load image icons");
        }
    }

    /** Returns the border's color, or null if this is not a link. */
    java.awt.Color getBorderColor() {
        javax.swing.text.StyledDocument doc = (javax.swing.text.StyledDocument) getDocument();
        return doc.getForeground(getAttributes());
    }

    /** Is this image within a link? */
    boolean isLink() {
        //! It would be nice to cache this but in an editor it can change
        // See if I have an HREF attribute courtesy of the enclosing A tag:
        javax.swing.text.AttributeSet anchorAttr = (javax.swing.text.AttributeSet)
                                                         fElement.getAttributes()
                                                               .getAttribute(javax.swing.text.html.HTML.Tag.A);
        if (anchorAttr != null) {
            return anchorAttr.isDefined(javax.swing.text.html.HTML.Attribute.HREF);
        }
        return false;
    }

    // --- Static icon accessors -------------------------------------------

    protected boolean isEditable() {
        return fContainer instanceof javax.swing.JEditorPane
                     && ((javax.swing.JEditorPane) fContainer).isEditable();
    }

    private javax.swing.Icon makeIcon(final String gifFile) throws java.io.IOException {
        /* Copy resource into a byte array.  This is
         * necessary because several browsers consider
         * Class.getResource a security risk because it
         * can be used to load additional classes.
         * Class.getResourceAsStream just returns raw
         * bytes, which we can convert to an image.
         */
        java.io.InputStream resource = mekwars.client.gui.MyImageView.class.getResourceAsStream(gifFile);

        if (resource == null) {
            //MMClient.mwClientLog.clientErrLog(MyImageView.class.getName() + "/" +gifFile + " not found.");
            return null;
        }
        java.io.BufferedInputStream in =
              new java.io.BufferedInputStream(resource);
        java.io.ByteArrayOutputStream out =
              new java.io.ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        in.close();
        out.flush();

        buffer = out.toByteArray();
        if (buffer.length == 0) {
            MWLogger.errLog("warning: " + gifFile +
                                  " is zero-length");
            return null;
        }
        return new javax.swing.ImageIcon(buffer);
    }

    /**
     * Establishes the parent view for this view. Seize this moment to cache the AWT Container I'm in.
     */
    @Override
    public void setParent(javax.swing.text.View parent) {
        super.setParent(parent);
        fContainer = parent != null ? getContainer() : null;
        if (parent == null && fComponent != null) {
            fComponent.getParent().remove(fComponent);
            fComponent = null;
        }
    }

    // --- member variables ------------------------------------------------

    /**
     * Provides a mapping from the document model coordinate space to the coordinate space of the view mapped to it.
     *
     * @param pos the position to convert
     * @param a   the allocated region to render into
     *
     * @return the bounding box of the given position
     *
     * @throws BadLocationException if the given position does not represent a valid location in the associated
     *                              document
     * @see View#modelToView
     */
    @Override
    public java.awt.Shape modelToView(int pos, java.awt.Shape a, javax.swing.text.Position.Bias b)
          throws javax.swing.text.BadLocationException {
        int p0 = getStartOffset();
        int p1 = getEndOffset();
        if ((pos >= p0) && (pos <= p1)) {
            java.awt.Rectangle r = a.getBounds();
            if (pos == p1) {
                r.x += r.width;
            }
            r.width = 0;
            return r;
        }
        return null;
    }

    /**
     * Provides a mapping from the view coordinate space to the logical coordinate space of the model.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param a the allocated region to render into
     *
     * @return the location within the model that best represents the given point of view
     *
     * @see View#viewToModel
     */
    @Override
    public int viewToModel(float x, float y, java.awt.Shape a, javax.swing.text.Position.Bias[] bias) {
        java.awt.Rectangle alloc = (java.awt.Rectangle) a;
        if (x < alloc.x + alloc.width) {
            bias[0] = javax.swing.text.Position.Bias.Forward;
            return getStartOffset();
        }
        bias[0] = javax.swing.text.Position.Bias.Backward;
        return getEndOffset();
    }

    /** My attributes may have changed. */
    @Override
    public void changedUpdate(javax.swing.event.DocumentEvent e, java.awt.Shape a, javax.swing.text.ViewFactory f) {
        if (DEBUG) {MWLogger.infoLog("ImageView: changedUpdate begin...");}
        super.changedUpdate(e, a, f);
        float align = getVerticalAlignment();

        int height = fHeight;
        int width = fWidth;

        initialize(getElement());

        boolean hChanged = fHeight != height;
        boolean wChanged = fWidth != width;
        if (hChanged || wChanged || getVerticalAlignment() != align) {
            if (DEBUG) {MWLogger.infoLog("ImageView: calling preferenceChanged");}
            getParent().preferenceChanged(this, hChanged, wChanged);
        }
        if (DEBUG) {MWLogger.infoLog("ImageView: changedUpdate end; valign=" + getVerticalAlignment());}
    }

    /**
     * Fetches the attributes to use when rendering.  This is implemented to multiplex the attributes specified in the
     * model with a StyleSheet.
     */
    @Override
    public javax.swing.text.AttributeSet getAttributes() {
        return attr;
    }

    /**
     * Set the size of the view. (Ignored.)
     *
     * @param width  the width
     * @param height the height
     */
    @Override
    public void setSize(float width, float height) {
        // Ignore this -- image size is determined by the tag attrs and
        // the image itself, not the surrounding layout!
    }

    /** Returns the image's vertical alignment. */
    float getVerticalAlignment() {
        String align = (String) fElement.getAttributes().getAttribute(javax.swing.text.html.HTML.Attribute.ALIGN);
        if (align != null) {
            align = align.toLowerCase();
            if (align.equals(TOP) || align.equals(TEXTTOP)) {
                return 0.0f;
            } else if (align.equals(mekwars.client.gui.MyImageView.CENTER) || align.equals(MIDDLE)
                             || align.equals(ABSMIDDLE)) {return 0.5f;}
        }
        return 1.0f;        // default alignment is bottom
    }

    /** Resize image if initial click was in grow-box: */
    public void mouseDragged(java.awt.event.MouseEvent e) {
        if (fGrowBase != null) {
            java.awt.Point loc = fComponent.getLocationOnScreen();
            int width = Math.max(2, loc.x + e.getX() - fGrowBase.x);
            int height = Math.max(2, loc.y + e.getY() - fGrowBase.y);

            if (e.isShiftDown() && fImage != null) {
                // Make sure size is proportional to actual image size:
                float imgWidth = fImage.getWidth(this);
                float imgHeight = fImage.getHeight(this);
                if (imgWidth > 0 && imgHeight > 0) {
                    float prop = imgHeight / imgWidth;
                    float pwidth = height / prop;
                    float pheight = width * prop;
                    if (pwidth > width) {width = (int) pwidth;} else {height = (int) pheight;}
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
        if (width == fWidth && height == fHeight) {return;}

        fWidth = width;
        fHeight = height;

        // Replace attributes in document:
        javax.swing.text.MutableAttributeSet attr = new javax.swing.text.SimpleAttributeSet();
        attr.addAttribute(javax.swing.text.html.HTML.Attribute.WIDTH, Integer.toString(width));
        attr.addAttribute(javax.swing.text.html.HTML.Attribute.HEIGHT, Integer.toString(height));
        ((javax.swing.text.StyledDocument) getDocument()).setCharacterAttributes(
              fElement.getStartOffset(),
              fElement.getEndOffset(),
              attr, false);
    }
    //private boolean   fGrowProportionally;	// should grow be proportional?

    public void mouseMoved(java.awt.event.MouseEvent e) {
    }

    // --- constants and static stuff --------------------------------

    /** On double-click, open image properties dialog. */
    public void mouseClicked(java.awt.event.MouseEvent e) {
        if (e.getClickCount() == 2) {
            //$ IMPLEMENT
        }
    }

    /** Select or grow image when clicked. */
    public void mousePressed(java.awt.event.MouseEvent e) {
        java.awt.Dimension size = fComponent.getSize();
        if (e.getX() >= size.width - 7 && e.getY() >= size.height - 7
                  && getSelectionState() == 2) {
            // Click in selected grow-box:
            if (DEBUG) {MWLogger.infoLog("ImageView: grow!!! Size=" + fWidth + "x" + fHeight);}
            java.awt.Point loc = fComponent.getLocationOnScreen();
            fGrowBase = new java.awt.Point(loc.x + e.getX() - fWidth,
                  loc.y + e.getY() - fHeight);
            //fGrowProportionally = e.isShiftDown();
        } else {
            // Else select image:
            fGrowBase = null;
            javax.swing.text.JTextComponent comp = (javax.swing.text.JTextComponent) fContainer;
            int start = fElement.getStartOffset();
            int end = fElement.getEndOffset();
            int mark = comp.getCaret().getMark();
            int dot = comp.getCaret().getDot();
            if (e.isShiftDown()) {
                // extend selection if shift key down:
                if (mark <= start) {comp.moveCaretPosition(end);} else {comp.moveCaretPosition(start);}
            } else {
                // just select image, without shift:
                if (mark != start) {comp.setCaretPosition(start);}
                if (dot != end) {comp.moveCaretPosition(end);}
            }
        }
    }

    public void mouseReleased(java.awt.event.MouseEvent e) {
        fGrowBase = null;
        //! Should post some command to make the action undo-able
    }

    public void mouseEntered(java.awt.event.MouseEvent e) {
    }

    public void mouseExited(java.awt.event.MouseEvent e) {
    }

}
