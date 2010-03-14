package sorcerer.client.data;

import sorcerer.client.LazyDataLoader;
import sorcerer.client.data.pkg.Project;
import sorcerer.client.data.pkg.SourceFile;

import static java.lang.Math.max;

/**
 * Loads the encoded Java source code as necessary.
 *
 * <p>
 * Key is a source file name separated by '/' (nor '\\'), like "abc/def/Ghi.java"
 *
 * @author Kohsuke Kawaguchi
 */
public class SourceFileLoader extends LazyDataLoader<SourceFile,AST> {
    @Override
    protected String href(SourceFile key) {
        return key.getAstURL();
    }

    /**
     * Loaded JavaScript will invoke this method.
     */
    public static void define(String fileName, String projectId, AST ast) {
        Project p = Project.get(projectId);
        int idx = fileName.lastIndexOf('/');
        SourceFile s = new SourceFile(
                p.getPackage(fileName.substring(0,max(0,idx)).replace('.','/')),
                fileName.substring(idx+1));
        INSTANCE.onLoaded(s,ast);
    }

    public static SourceFileLoader INSTANCE = new SourceFileLoader();

    public native static void export() /*-{
        $wnd.defineStructure = $entry(@sorcerer.client.data.SourceFileLoader::define(Ljava/lang/String;Ljava/lang/String;Lsorcerer/client/data/AST;));
    }-*/;

    static {
        export();
    }
}
