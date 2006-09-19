package org.jvnet.sorcerer.util;

import java.io.PrintWriter;

/**
 * Writes text in the JSON format.
 *
 * TODO: indentation
 *
 * @author Kohsuke Kawaguchi
 */
public class JsonWriter {
    private final PrintWriter w;
    private boolean first=true;

    public JsonWriter(PrintWriter w) {
        this.w = w;
    }

    private void sep() {
        if(first)
            first = false;
        else
            w.print(',');
    }

    private void quote(Object value) {
        w.print('"');
        w.print(value);
        w.print('"');
    }

    public JsonWriter startArray() {
        sep();
        w.print('[');
        first = true;
        return this;
    }

    public JsonWriter endArray() {
        w.print(']');
        first = false;
        return this;
    }

    public JsonWriter startObject() {
        sep();
        w.print('{');
        first = true;
        return this;
    }

    public JsonWriter endObject() {
        w.print('}');
        first = false;
        return this;
    }

    public JsonWriter property(String key, Object value) {
        key(key);
        first=false;    // cancel first=true in key()
        quote(value);
        return this;
    }

    public JsonWriter property(String key, boolean value) {
        key(key);
        first=false;    // cancel first=true in key()
        w.print(value);
        return this;
    }

    public JsonWriter key(String key) {
        sep();
        quote(key);
        w.print(':');
        first=true; // when the next item is array or object, don't put ','
        return this;
    }

}
