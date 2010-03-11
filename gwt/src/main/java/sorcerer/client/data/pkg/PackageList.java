package sorcerer.client.data.pkg;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Data representation of <tt>package-list.js</tt>
 *
 * @author Kohsuke Kawaguchi
 */
public final class PackageList extends JavaScriptObject {
    protected PackageList() {}

    public native String name() /*-{ return this.name; }-*/;
    public native Package packages() /*-{ return this.packages; }-*/;
    // TODO: linker support
}
