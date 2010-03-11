package sorcerer.client.js;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * JavaScript function.
 *
 * @author Kohsuke Kawaguchi
 */
public class JsFunction extends JavaScriptObject {
    protected JsFunction() {}

    public final native <T> T invoke() /*-{ return this.call(null); }-*/;
    public final native <T> T invoke(Object a1) /*-{ return this.call(null,a1); }-*/;
    public final native <T> T invoke(Object a1, Object a2) /*-{ return this.call(null,a1,a2); }-*/;
}
