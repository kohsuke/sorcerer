package org.jvnet.sorcerer;

import org.jvnet.sorcerer.ParsedType.Match;

import javax.lang.model.element.ExecutableElement;
import java.util.Collection;

/**
 * {@link Bookmark} that shows a list of {@link ExecutableElement}s.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class MethodListBookmark extends ElementListBookmark<ExecutableElement> {
    protected MethodListBookmark(Collection<Match> r, LinkResolver resolver) {
        super(convertToMethodList(r),resolver);
    }

    private static ExecutableElement[] convertToMethodList(Collection<Match> r) {
        ExecutableElement[] methods = new ExecutableElement[r.size()];
        int i=0;
        for (Match m : r)
            methods[i++] = m.method;
        return methods;
    }

    @Override
    protected String getDisplayName(ExecutableElement m) {
        return m.getEnclosingElement().toString();
    }
}
