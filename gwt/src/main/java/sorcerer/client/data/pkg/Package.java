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
    public native JsArray<Package> children() /*-{ return this.children; }-*/;
    public native boolean isLeaf() /*-{ return this.leaf; }-*/;
}
