package org.jvnet.sorcerer;

import java.util.ArrayList;

/**
 * Used to organize {@link Tag}s in a tree. Representing a list of {@link Tag}s that
 * are open at the current point of the source code.
 *
 * @author Kohsuke Kawaguchi
 */
final class TagStack extends ArrayList<TagStack.Adder> {
    public Adder peek() {
        if(isEmpty())   return null;
        return get(size()-1);
    }
    public Adder pop() {
        return remove(size()-1);
    }
    public void push(Tag t) {
        add(new Adder(t));
    }

    static final class Adder {
        final Tag parent;
        Tag head;
        boolean first = true;

        public Adder(Tag head) {
            this.head = this.parent = head;
        }

        public long endPos() {
            return parent.ep;
        }

        void add(Tag t) {
            if(first) {
                assert head.firstChild==null;
                head.firstChild=t;
                first=false;
            } else {
                assert head.nextSibling==null;
                head.nextSibling=t;
            }
            head=t;
        }
    }
}
