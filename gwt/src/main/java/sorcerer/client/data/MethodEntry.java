package sorcerer.client.data;

import com.google.gwt.core.client.JavaScriptObject;
import sorcerer.client.js.JsArray;

/**
 * @author Kohsuke Kawaguchi
 */
public final class MethodEntry extends JavaScriptObject {
    protected MethodEntry() {}
    
    public native int owner() /*-{
        return this[0];
    }-*/;

    public native String name() /*-{
        return this[1];
    }-*/;

    /**
     * Parameters. Each item is either FQCN or an index to the type table.
     */
    public native JsArray params() /*-{
        return this[2];
    }-*/;

    public native String css() /*-{
        return this[3];
    }-*/;
}

