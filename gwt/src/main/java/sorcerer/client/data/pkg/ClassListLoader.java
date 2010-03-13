package sorcerer.client.data.pkg;

import sorcerer.client.LazyDataLoader;
import sorcerer.client.js.JsArray;

/**
 * Loads contents of <tt>class-list.js</tt>, which is a {@link Klass} array by a package name.
 *
 * <p>
 * Key is a fully-qualified package name.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClassListLoader extends LazyDataLoader<String, JsArray<Klass>> {
    @Override
    protected String href(String key) {
        return "data/"+key.replace('.','/')+"/class-list.js";
    }

    /**
     * Loaded JavaScript will invoke this method.
     */
    public static void define(String packageName, JsArray<Klass> ast) {
        for (Klass k : ast.iterable())
            k.packageName(packageName);
        INSTANCE.onLoaded(packageName,ast);
    }

    public static ClassListLoader INSTANCE = new ClassListLoader();

    public native static void export() /*-{
        $wnd.setClassList = $entry(@sorcerer.client.data.pkg.ClassListLoader::define(Ljava/lang/String;Lsorcerer/client/js/JsArray;));
    }-*/;

    static {
        export();
    }
}

