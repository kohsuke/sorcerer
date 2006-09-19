package org.jvnet.sorcerer;

import javax.lang.model.element.Element;
import java.io.PrintWriter;

/**
 * {@link Bookmark} that displays a link to a list of {@link Element}s.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class ElementListBookmark<T extends Element> extends Bookmark {
    private final T[] elements;
    private final LinkResolver resolver;

    protected ElementListBookmark(T[] r, LinkResolver resolver) {
        this.resolver = resolver;
        this.elements = r;
    }

    private String href(T t) {
        String r = resolver.href(t);
        if(r!=null) return "href='"+r+"'";
        else        return "";
    }

    protected void writeTo(PrintWriter w) {
        w.print("<span class='bookmark'>");
        if(elements.length==1)
            w.printf("<a %1s alt='%2s'>", href(elements[0]), getCaption());
        w.print(getMark());
        if(elements.length==1)
            w.print("</a>");
        w.print("<div class='popup overridden'>");
        w.print(getCaption());
        w.print("<ol>");
        for (T m : elements) {
            w.printf("<li>&#x2192;<a %1s>%2s</a>",
                href(m), getDisplayName(m));
        }
        w.print("</ol>");
        w.print("</div></span>");
    }

    /**
     * Gets the name to be displayed as a link.
     */
    protected abstract String getDisplayName(T e);

    /**
     * Gets the mark symbol to be displayed.
     * This must take 2 ASCII char space in width.
     */
    protected abstract String getMark();

    /**
     * Gets the caption of this bookmark.
     */
    protected abstract String getCaption();
}
