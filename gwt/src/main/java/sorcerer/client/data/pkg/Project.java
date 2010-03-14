package sorcerer.client.data.pkg;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.ScriptElement;
import sorcerer.client.js.JsHash;

/**
 * Data representation of <tt>package-list.js</tt>
 *
 * @author Kohsuke Kawaguchi
 */
public final class Project extends JavaScriptObject {
    protected Project() {}

    public native String name() /*-{ return this.name; }-*/;

    /**
     * Globally unique ID that identifies this project.
     */
    public native String id() /*-{ return this.id; }-*/;
    public native Package rootPackage() /*-{ return this.packages; }-*/;
    // TODO: linker support

    /*package*/ native JsHash<Package> packages() /*-{ return this.packagesByName; }-*/;
    private native void initPackages() /*-{ this.packagesByName={}; }-*/;

    /**
     * Base URL to load JSON files for this project from.
     */
    public native String baseURL() /*-{ return this.baseURL; }-*/;
    public native void baseURL(String value) /*-{ this.baseURL = value; }-*/;

    /**
     * Gets {@link Package} by its fully qualified name. 
     */
    public Package getPackage(String name) {
        return packages().get(name);
    }

    /**
     * Called after the project information is loaded to enrich the data structure.
     */
    /*package*/ void init(String baseURL) {
        baseURL(baseURL);
        initPackages();
        rootPackage().init(this,"");
    }

    /**
     * Gets the project by its ID.
     */
    public static Project get(String id) {
        return ProjectLoader.INSTANCE.get(id);
    }
}
