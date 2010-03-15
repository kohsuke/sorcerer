package sorcerer.client.js;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;

import java.util.Iterator;

/**
 * @author Kohsuke Kawaguchi
 */
public class Iterables {
    public static <T extends Node> Iterable<T> $A(final NodeList<T> nl) {
        return new Iterable<T>() {
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    int i=0;
                    public boolean hasNext() {
                        return i<nl.getLength();
                    }

                    public T next() {
                        return nl.getItem(i++);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}
