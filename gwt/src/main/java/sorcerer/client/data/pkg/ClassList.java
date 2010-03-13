package sorcerer.client.data.pkg;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * @author Kohsuke Kawaguchi
 */
public final class ClassList extends JavaScriptObject {
    protected ClassList() {}
    /**
     * Short class name.
     */
    public native String name() /*-{ return this.name; }-*/;

    /**
     * Kind of type. interface, class, enum and annotation.
     */
    public native String kind() /*-{ return this.kind; }-*/;

    /**
     * Access modifier. Either public or protected.
     */
    public native String access() /*-{ return this.access; }-*/;

    /**
     * Source file script that this class is defined in.
     */
    public native String script() /*-{ return this.script; }-*/;
}
