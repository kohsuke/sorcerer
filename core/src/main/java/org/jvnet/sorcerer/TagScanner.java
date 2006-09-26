package org.jvnet.sorcerer;

import java.util.List;

/**
 * Iterator-like forward scanner.
 */
final class TagScanner {
    private int idx=0;

    private final List<Tag> tags;

    public TagScanner(List<Tag> tags) {
        this.tags = tags;
    }

    public Tag peek() {
        if(idx==tags.size()) return null;
        return tags.get(idx);
    }
    public Tag pop() {
        return tags.get(idx++);
    }
}
