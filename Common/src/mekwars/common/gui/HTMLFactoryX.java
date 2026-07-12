package mekwars.common.gui;

import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

/**
 * A {@link ViewFactory} used by {@link MyHTMLEditorKit} to build the Swing {@link View} tree for HTML rendered in
 * MekWars client components (chat panes, tooltips, dialogs, etc.). It behaves exactly like the standard
 * {@link HTMLEditorKit.HTMLFactory}, except that it substitutes {@link MyImageView} for the view of any
 * {@code <img>} tag, which allows MekWars to customize how images are rendered/loaded inside HTML content (see
 * {@link MyImageView} for the actual customization).
 */
public class HTMLFactoryX extends HTMLEditorKit.HTMLFactory implements ViewFactory {

    /**
     * Creates the {@link View} for a given HTML {@link Element}. All tags are delegated to the superclass's default
     * behavior except {@code <img>}, which is rendered using {@link MyImageView} instead of the stock Swing image
     * view.
     *
     * @param elem the HTML element to build a view for
     *
     * @return a {@link MyImageView} for image elements, otherwise whatever view the standard HTML factory would
     *       produce
     */
    @Override
    public View create(Element elem) {
        Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
        if (o instanceof HTML.Tag kind) {
            if (kind == HTML.Tag.IMG) {
                return new MyImageView(elem);
            }
        }
        return super.create(elem);
    }
}
