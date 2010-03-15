package sorcerer.client.sourceview;

import com.google.gwt.dom.client.Element;

/**
 * @author Kohsuke Kawaguchi
 */
public class Rect {
    public int x1,y1,x2,y2;

    public Rect() {
    }

    /**
     * Creates a bounding box of the given element.
     */
    public Rect(Element e) {
        x1 = e.getAbsoluteLeft();
        x2 = x1+e.getOffsetWidth();
        y1 = e.getAbsoluteTop();
        y2 = y1+e.getOffsetHeight();
    }

    public void fatten(int margin) {
        x1 -= margin;
        y1 -= margin;
        x2 += margin;
        y2 += margin;
    }

    /**
     * Does this rectangle include the specified point?
     */
    public boolean contains(int x, int y) {
        return x1<=x && x<x2 && y1<=y && y<y2;
    }
}
