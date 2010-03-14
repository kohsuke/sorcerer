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
public class ClassListLoader extends LazyDataLoader<Package, JsArray<Klass>> {
    @Override
    protected String href(Package key) {
        return key.baseURL()+"/class-list.js";
    }

    /**
     * Loaded JavaScript will invoke this method.
     */
    public static void define(String packageName, String projectId, JsArray<Klass> ast) {
        Project p = Project.get(projectId);
        Package pkg = p.getPackage(packageName);
        for (Klass k : ast.iterable())
            k.pkg(pkg);
        INSTANCE.onLoaded(pkg,ast);
    }

    public static ClassListLoader INSTANCE = new ClassListLoader();

    public native static void export() /*-{
        $wnd.setClassList = $entry(@sorcerer.client.data.pkg.ClassListLoader::define(Ljava/lang/String;Ljava/lang/String;Lsorcerer/client/js/JsArray;));
    }-*/;

    static {
        export();
    }
}

