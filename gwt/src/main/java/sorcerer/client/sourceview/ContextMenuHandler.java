package sorcerer.client.sourceview;

import com.google.gwt.dom.client.Element;
import com.smartgwt.client.widgets.menu.IMenuButton;

import static com.smartgwt.client.types.Positioning.ABSOLUTE;
import static com.smartgwt.client.types.Visibility.HIDDEN;
import static com.smartgwt.client.types.Visibility.VISIBLE;

/**
 * @author Kohsuke Kawaguchi
 */
public class ContextMenuHandler {
    private final IMenuButton button;

    public ContextMenuHandler(IMenuButton button) {
        this.button = button;
//        button.setPosition(ABSOLUTE);
    }

    private void onMouseOver(Element e) {
//        button.setLeft(e.getAbsoluteRight());
//        button.setTop(e.getAbsoluteTop());
//        button.setVisibility(VISIBLE);
    }

    private void onMouseOut(Element e) {
//        button.setVisibility(HIDDEN);
    }

    public native void installFor(Element e) /*-{
        var self = this;
//        e.addEventListener("mouseover",function(e) {
//            self.@sorcerer.client.sourceview.ContextMenuHandler::onMouseOver(Lcom/google/gwt/dom/client/Element;)(this);
//        },false);
//        e.addEventListener("mouseout",function(e) {
//            self.@sorcerer.client.sourceview.ContextMenuHandler::onMouseOut(Lcom/google/gwt/dom/client/Element;)(this);
//        },false);
    }-*/;
}
