package sorcerer.client.data.pkg;

import com.google.gwt.core.client.JavaScriptObject;
import sorcerer.client.js.JsArray;

/**
 * A node in the package tree structure.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Package extends JavaScriptObject {
    protected Package() {}

    public native String shortName() /*-{ return this.name; }-*/;

    /**
     * List of child packages.
     */
    public native JsArray<Package> children() /*-{ return this.children || []; }-*/;
    public native boolean isLeaf() /*-{ return this.leaf || false; }-*/;

//
// inferred attributes that are computed later
//
    public native String fullName() /*-{ return this.fullName; }-*/;
    private native void fullName(String n) /*-{ this.fullName = n; }-*/;
    public String fullDisplayName() {
        String n = fullName();
        if (n.length()==0)  return "(unnamed package)";
        return n;
    }

    private native void intermediate(Package child) /*-{ this.intermediate=child; }-*/;

    /*package*/ void init(String prefix) {
        fullName(prefix+shortName());
        if (!isLeaf() && children().length()==1)
            intermediate(children().get(0));

        for (Package c : children().iterable())
            c.init(fullName()+ (fullName().length()>0 ? "." : ""));
    }
}
