package org.jvnet.sorcerer;

import org.jvnet.sorcerer.util.JsonWriter;

import java.util.ArrayList;
import java.util.Collections;

/**
 * List of package names.
 *
 * @author Kohsuke Kawaguchi
 */
public class PackageSet extends ArrayList<String> implements JsonWriter.Writable {
    public class DefinedPkgInfo extends PkgInfo<DefinedPkgInfo> {
        public DefinedPkgInfo(String name) {
            super(name);
        }

        protected DefinedPkgInfo create(String name) {
            return new DefinedPkgInfo(name);
        }

        /**
         * True if this package has a class (meaning it's a leaf).
         */
        boolean leaf;

        public void write(JsonWriter js) {
            super.write(js);
            if(leaf)
                js.property("leaf",true);
        }
    }

    public void sort() {
        Collections.sort(this);
    }

    /**
     * Organizes a tree from the list of packages.
     */
    public DefinedPkgInfo buildTree() {
        // build package tree info
        DefinedPkgInfo root = new DefinedPkgInfo("");

        for (String n : this) {
            root.add(n).leaf =true;
        }

        return root;
    }

    public void write(JsonWriter w) {
        buildTree().write(w);
    }
}
