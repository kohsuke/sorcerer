package sorcerer.client.data;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * @author Kohsuke Kawaguchi
 */
public final class LocalVariableEntry extends JavaScriptObject {
    protected LocalVariableEntry() {}

    public native String name() /*-{
        return this[0];
    }-*/;

    public native String id() /*-{
        return this[1];
    }-*/;
}


