package sorcerer.client.data.pkg;

import com.google.gwt.core.client.JavaScriptObject;
import sorcerer.client.LazyDataLoader.Callback;
import sorcerer.client.data.AST;
import sorcerer.client.data.SourceFileLoader;
import sorcerer.client.sourceview.SourceViewWidget;

/**
 * Represents a type.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Klass extends JavaScriptObject {
    protected Klass() {}

//
// Properties defined in class-list.js
//
    /**
     * Short class name.
     */
    public native String shortName() /*-{ return this.name; }-*/;

    /**
     * Kind of type. interface, class, enum and annotation.
     */
    public native String kind() /*-{ return this.kind; }-*/;

    /**
     * Access modifier. Either public or protected.
     */
    public native String access() /*-{ return this.access; }-*/;

    /**
     * Source file short name that this class is defined in.
     */
    public native String file() /*-{ return this.file; }-*/;

    /**
     * Line number in {@link #file()} that this type is defined at.
     */
    public native int line() /*-{ return this.line; }-*/;



//
// Properties computed later
//

    public native String packageName() /*-{ return this.packageName; }-*/;

    public native void packageName(String value) /*-{ this.packageName = value; }-*/;

    /**
     * Show this class in the source view.
     */
    public void show() {
        SourceFileLoader.INSTANCE.retrieve(packageName()+'/'+file(),new Callback<AST>() {
            public void call(AST ast) {
                SourceViewWidget.get().load(ast);
            }
        });
    }
}
