package org.jvnet.sorcerer.frame;

import org.jvnet.sorcerer.util.JsonWriter;

import java.util.Set;
import java.util.TreeSet;

/**
 * Tree structure of packages.
 *
 * This model treats each dot-separated token as a node, so "org.acme.foo" package
 * would be placed 3 level deep.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class PkgInfo<T extends PkgInfo<T>> implements Comparable<T>, JsonWriter.Writable {
    final String name;
    final Set<T> children = new TreeSet<T>();


    public PkgInfo(String name) {
        this.name = name;
    }

    public int compareTo(T that) {
        return this.name.compareTo(that.name);
    }

    /**
     * Adds a new package of the given name to this tree.
     */
    public T add(String name) {
        if(name.length()==0) {
            return (T)this;
        }

        String head,rest;
        int idx = name.indexOf('.');
        if(idx>=0) {
            head = name.substring(0,idx);
            rest = name.substring(idx+1);
        } else {
            head = name;
            rest = "";
        }

        for (T c : children)
            if(c.name.equals(head))
                return c.add(rest);

        T c = create(head);
        children.add(c);
        return c.add(rest);
    }

    public void write(JsonWriter js) {
        js.property("name",name);
        if(!children.isEmpty())
            js.property("children",children);
    }

    protected abstract T create(String name);
}
