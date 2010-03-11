package sorcerer.client.js;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Java wrapper for JavaScript hash.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JsHash<V> extends JavaScriptObject {
    protected JsHash() {}

    public static native <V> JsHash<V> create() /*-{
        return {};
    }-*/;

    public native void put(int key, V value) /*-{
        this[key] = value;
    }-*/;

    public native V get(int key) /*-{
        return this[key];
    }-*/;

    public native V get(String key) /*-{
        return this[key];
    }-*/;

    public native void put(String key, V value) /*-{
        this[key] = value;
    }-*/;
}
