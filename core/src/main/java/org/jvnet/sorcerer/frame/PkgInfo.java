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
class PkgInfo<T extends PkgInfo<T>> implements Comparable<T>, JsonWriter.Writable {
    final String name;
    final Set<T> children = new TreeSet<T>();
    /**
     * False if this class doesn't have any classes in it (excluding descendants.)
     */
    // TODO: move this to a subclass
    boolean hasClasses;


    public PkgInfo(String name) {
        this.name = name;
    }

    public int compareTo(T that) {
        return this.name.compareTo(that.name);
    }

    /**
     * Adds a new package of the given name to this tree.
     */
    public T add(String name,Factory<T> factory) {
        if(name.length()==0) {
            hasClasses =true;
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
                return c.add(rest,factory);

        T c = factory.create(head);
        children.add(c);
        return c.add(rest,factory);
    }

    public void write(JsonWriter js) {
        js.property("name",name);
        if(hasClasses)
            js.property("hasClasses",true);
        if(!children.isEmpty())
            js.property("children",children);
    }

    public interface Factory<T extends PkgInfo> {
        T create(String name);
    }
}
