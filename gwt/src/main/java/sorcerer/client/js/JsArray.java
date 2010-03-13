package sorcerer.client.js;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.Iterator;

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

    public final Iterable<T> iterable() {
        return new Iterable<T>() {
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    int index = 0;
                    public boolean hasNext() {
                        return index<length();
                    }

                    public T next() {
                        return get(index++);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
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

