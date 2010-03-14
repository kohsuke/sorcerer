package sorcerer.client.data.pkg;

import com.google.gwt.core.client.JavaScriptObject;
import sorcerer.client.LazyDataLoader.Callback;
import sorcerer.client.js.JsArray;

/**
 * A node in the package tree structure.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Package extends JavaScriptObject {
    protected Package() {}

    public native String shortName() /*-{ return this.name; }-*/;

    /**
     * List of child packages.
     */
    public native JsArray<Package> children() /*-{ return this.children || []; }-*/;
    public native boolean isLeaf() /*-{ return this.leaf || false; }-*/;

//
// inferred attributes that are computed later
//
    public native String fullName() /*-{ return this.fullName; }-*/;
    private native void fullName(String n) /*-{ this.fullName = n; }-*/;
    public String fullDisplayName() {
        String n = fullName();
        if (n.length()==0)  return "(unnamed package)";
        return n;
    }

    /**
     * {@link Project} that owns this package.
     */
    public native Project owner() /*-{ return this.owner; }-*/;
    private native void owner(Project value) /*-{ this.owner = value; }-*/;

    public String baseURL() {
        return owner().baseURL()+'/'+fullName().replace('.','/');
    }

    private native void intermediate(Package child) /*-{ this.intermediate=child; }-*/;

    /**
     * Loads the classes in this package and calls back.
     */
    public void retrieveClassList(Callback<JsArray<Klass>> callback) {
        ClassListLoader.INSTANCE.retrieve(this,callback);
    }


    /*package*/ void init(Project owner, String prefix) {
        owner(owner);
        
        String fn = prefix + shortName();
        fullName(fn);
        owner.packages().put(fn,this);

        if (!isLeaf() && children().length()==1)
            intermediate(children().get(0));

        for (Package c : children().iterable())
            c.init(owner,fn+ (fn.length()>0 ? "." : ""));
    }
}
