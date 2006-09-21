package org.jvnet.sorcerer.util;

import java.io.PrintWriter;

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
        else
            w.print(',');
        nl(0);
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

    public JsonWriter property(String key, int value) {
        key(key);
        first=false;    // cancel first=true in key()
        w.print(value);
        return this;
    }

    public JsonWriter property(String key, long value) {
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

    /**
     * Writes out a string value.
     */
    public void string(String str) {
        sep();
        quote(str);
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
