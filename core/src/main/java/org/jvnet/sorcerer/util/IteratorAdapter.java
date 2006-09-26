package org.jvnet.sorcerer.util;

import java.util.Iterator;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class IteratorAdapter<T,U> implements Iterator<T> {
    private final Iterator<? extends U> core;

    protected IteratorAdapter(Iterator<? extends U> core) {
        this.core = core;
    }

    public boolean hasNext() {
        return core.hasNext();
    }

    public T next() {
        return filter(core.next());
    }

    protected abstract T filter(U u);

    public void remove() {
        core.remove();
    }
}
