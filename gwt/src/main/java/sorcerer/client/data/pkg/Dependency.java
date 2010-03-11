package sorcerer.client.data.pkg;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * @author Kohsuke Kawaguchi
 */
public final class Dependency extends JavaScriptObject {
    protected Dependency() {}

    public native String name() /*-{ return this.name; }-*/;
    public native String baseURL() /*-{ return this.baseURL; }-*/;
    public native Package packages() /*-{ return this.packages; }-*/;
    // TODO: linker support
}
