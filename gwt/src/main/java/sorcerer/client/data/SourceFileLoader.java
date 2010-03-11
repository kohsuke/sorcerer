package sorcerer.client.data;

import sorcerer.client.LazyDataLoader;

/**
 * Loads the encoded Java source code as necessary.
 *
 * <p>
 * Key is a source file name separated by '/' (nor '\\'), like "abc/def/Ghi.java"
 *
 * @author Kohsuke Kawaguchi
 */
public class SourceFileLoader extends LazyDataLoader<String,AST> {
    @Override
    protected String href(String key) {
        return key+".js";
    }

    /**
     * Loaded JavaScript will invoke this method.
     */
    public static void define(String fileName, AST ast) {
        INSTANCE.onLoaded(fileName,ast);
    }

    public static SourceFileLoader INSTANCE = new SourceFileLoader();

    public native static void export() /*-{
        $wnd.defineStructure = $entry(@sorcerer.client.data.SourceFileLoader::define(Ljava/lang/String;Lsorcerer/client/data/AST;));
    }-*/;

    static {
        export();
    }
}
