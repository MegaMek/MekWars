package mekwars.common.gui;

import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

public class HTMLFactoryX extends HTMLEditorKit.HTMLFactory implements ViewFactory {

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
