package sorcerer.client.data.pkg;

import sorcerer.client.LazyDataLoader;
import sorcerer.client.js.JsArray;

/**
 * Loads ClassList array by a package name.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClassListLoader extends LazyDataLoader<String, JsArray<ClassList>> {
    @Override
    protected String href(String key) {
        return key+"/class-list.js";
    }

    /**
     * Loaded JavaScript will invoke this method.
     */
    public static void define(String fileName, JsArray<ClassList> ast) {
        INSTANCE.onLoaded(fileName,ast);
    }

    public static ClassListLoader INSTANCE = new ClassListLoader();

    public native static void export() /*-{
        $wnd.setClassList = $entry(@sorcerer.client.data.pkg.ClassListLoader::define(Ljava/lang/String;Lsorcerer/client/js/JsArray;));
    }-*/;

    static {
        export();
    }
}

