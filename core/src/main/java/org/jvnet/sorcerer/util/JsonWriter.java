package org.jvnet.sorcerer.util;

import java.io.PrintWriter;
import java.util.Collection;

/**
 * Writes text in the JSON format.
 *
 * @author Kohsuke Kawaguchi
 */
public class JsonWriter {
    private final PrintWriter w;
    private boolean first=true;

    private int indentLevel=0;

    public JsonWriter(PrintWriter w) {
        this.w = w;
    }

    /**
     * Interface that represents objects that can be written as JSON.
     */
    public static interface Writable {
        /**
         * The caller invokes start/endObject, so this method
         * should just write properties.
         */
        void write(JsonWriter w);
    }

    /**
     * Adjusts the indent by the given offset and prints a new line.
     */
    private void nl(int diff) {
        if(!INDENT)     return; // that is, if we are indenting.
        w.println();
        indentLevel+=diff;
        for( int i=0; i<indentLevel; i++ )
            w.print("  ");
    }

    private void sep() {
        if(first)
            first = false;
        else {
            w.print(',');
            nl(0);
        }
    }


    private void quote(Object value) {
        w.print('"');
        w.print(value);
        w.print('"');
    }

    public JsonWriter startArray() {
        sep();
        w.print('[');
        nl(1);
        first = true;
        return this;
    }

    public JsonWriter endArray() {
        nl(-1);
        w.print(']');
        first = false;
        return this;
    }

    public JsonWriter startObject() {
        sep();
        w.print('{');
        nl(1);
        first = true;
        return this;
    }

    public JsonWriter endObject() {
        nl(-1);
        w.print('}');
        first = false;
        return this;
    }

    /**
     * Writes the collection as an array.
     */
    public JsonWriter property(String key, Collection<? extends Writable> values) {
        key(key);
        startArray();
        for (Writable v : values) {
            startObject();
            v.write(this);
            endObject();
        }
        return endArray();
    }

    public JsonWriter property(String key, Object value) {
        key_(key);
        quote(value);
        return this;
    }

    public JsonWriter property(String key, boolean value) {
        key_(key);
        w.print(value);
        return this;
    }

    public JsonWriter property(String key, int value) {
        key_(key);
        w.print(value);
        return this;
    }

    public JsonWriter property(String key, long value) {
        key_(key);
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

    /**
     * Writes a key to be immediately followed by a single value.
     */
    private void key_(String key) {
        key(key);
        first=false;
    }

    /**
     * Writes out a string value.
     */
    public JsonWriter object(String str) {
        sep();
        quote(str);
        return this;
    }

    public JsonWriter object(Writable root) {
        startObject();
        root.write(this);
        return endObject();
    }

    private static final boolean INDENT = initIndent();

    static boolean initIndent() {
        try {
            return Boolean.getBoolean("sorcerer.debug");
        } catch (SecurityException e) {
            return false;
        }
    }
}
