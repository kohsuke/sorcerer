package org.jvnet.sorcerer;

import org.jvnet.sorcerer.ParsedType.Match;

import java.util.Collection;

/**
 * {@link Bookmark} to display list of overriding methods in descendant types.
 *
 * @author Kohsuke Kawaguchi
 */
class OverridingMethodsBookmark extends MethodListBookmark {
    public OverridingMethodsBookmark(Collection<Match> r, LinkResolver resolver) {
        super(r, resolver);
    }

    protected String getCaption() {
        return "Jump to overriding methods";
    }

    protected String getMark() {
        return "&#x25BC;";
    }
}
