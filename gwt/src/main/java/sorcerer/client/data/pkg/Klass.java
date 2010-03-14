package sorcerer.client.data.pkg;

import com.google.gwt.core.client.JavaScriptObject;
import sorcerer.client.LazyDataLoader.Callback;
import sorcerer.client.data.AST;

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

    /**
     * {@link Package} that this class belongs to.
     */
    public native Package pkg() /*-{ return this.pkg; }-*/;

    native void pkg(Package value) /*-{ this.pkg = value; }-*/;

    public Project project() { return pkg().owner(); }

    /**
     * Gets the {@link SourceFile} that contains this type.
     */
    public SourceFile getSourceFile() {
        return new SourceFile(pkg(),file());
    }

    /**
     * Show this class in the source view.
     */
    public void show() {
        getSourceFile().retrieveAST(new Callback<AST>() {
            public void call(AST ast) {
                ast.show();
            }
        });
    }
}
