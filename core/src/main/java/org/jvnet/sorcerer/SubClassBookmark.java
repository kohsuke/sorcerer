package org.jvnet.sorcerer;

import javax.lang.model.element.TypeElement;
import java.util.Collection;

/**
 * @author Kohsuke Kawaguchi
 */
public class SubClassBookmark extends ElementListBookmark<TypeElement> {
    public SubClassBookmark(Collection<ParsedType> list, LinkResolver resolver) {
        super(convert(list), resolver);
    }

    private static TypeElement[] convert(Collection<ParsedType> list) {
        TypeElement[] r = new TypeElement[list.size()];
        int i=0;
        for (ParsedType pt : list) {
            r[i++] = pt.element;
        }
        return r;
    }

    @Override
    protected String getDisplayName(TypeElement e) {
        return e.getQualifiedName().toString();
    }

    @Override
    protected String getMark() {
        return "&#x25BC;";
    }

    @Override
    protected String getCaption() {
        return "Jump to subtypes";
    }
}
