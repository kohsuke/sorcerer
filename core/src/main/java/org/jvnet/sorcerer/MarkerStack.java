package org.jvnet.sorcerer;

import java.util.ArrayList;

/**
 * LIFO stack of {@link Marker}.
 */
final class MarkerStack extends ArrayList<Marker> {
    public Marker peek() {
        if(isEmpty())   return null;
        return get(size()-1);
    }
    public Marker pop() {
        return remove(size()-1);
    }
    public void push(Marker m) {
        add(m);
    }
}
