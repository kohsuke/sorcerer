package sorcerer.client.js;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * JavaScript array.
 *
 * @author Kohsuke Kawaguchi
 */
public class JsArray<T> extends JavaScriptObject {

    protected JsArray() {
    }
    
    public static <T> JsArray<T> create() {
        return (JsArray<T>)createArray();
    }

    public final native T get(int index) /*-{
        return this[index];
    }-*/;

    public final native String join(String separator) /*-{
        return this.join(separator);
    }-*/;

    public final native int length() /*-{
        return this.length;
    }-*/;

    public final native void push(T value) /*-{
        this.push(value);
    }-*/;

    public final native T pop() /*-{
        return this.pop();
        }-*/;


    public final native void set(int index, T value) /*-{
        this[index] = value;
    }-*/;

    public final native void setLength(int newLength) /*-{
        this.length = newLength;
    }-*/;

    public final native T shift() /*-{
        return this.shift();
    }-*/;

    public final native void unshift(T value) /*-{
        this.unshift(value);
    }-*/;
}

