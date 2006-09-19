package org.jvnet.sorcerer;

import org.jvnet.sorcerer.ParsedType.Match;

import java.util.Collection;

/**
 * {@link Bookmark} to display list of overridden methods in super types.
 *
 * @author Kohsuke Kawaguchi
 */
final class OverriddenMethodsBookmark extends MethodListBookmark {
    OverriddenMethodsBookmark(Collection<Match> r, LinkResolver resolver) {
        super(r, resolver);
    }

    protected String getCaption() {
        return "Jump to super method";
    }

    protected String getMark() {
        return "&#x25B2;";
    }
}
