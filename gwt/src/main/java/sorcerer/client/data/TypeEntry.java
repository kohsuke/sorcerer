package sorcerer.client.data;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * @author Kohsuke Kawaguchi
 */
public final class TypeEntry extends JavaScriptObject {
    protected TypeEntry() {}
    
    public native String binaryName() /*-{
        return this[0];
    }-*/;

    public native String css() /*-{
        return this[1];
    }-*/;
}
