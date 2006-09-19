package org.jvnet.sorcerer;

import java.util.HashSet;

/**
 * Set of {@link Bookmark}s with additional logic to avoid
 * putting more than one bookmark of the same kind.
 *
 * @author Kohsuke Kawaguchi
 */
class BookmarkSet extends HashSet<Bookmark> {

    public final int lineNumber;

    public BookmarkSet(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean add(Bookmark e) {
        // bookmark set is expected to be fairly small, so this dumb algorithm should be fine.
        for (Bookmark b : this) {
            if(b.getClass()==e.getClass())
                return false;   // ignore
        }
        return super.add(e);
    }
}
