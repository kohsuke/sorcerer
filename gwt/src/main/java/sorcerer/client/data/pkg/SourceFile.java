package sorcerer.client.data.pkg;

import sorcerer.client.LazyDataLoader.Callback;
import sorcerer.client.data.AST;
import sorcerer.client.data.SourceFileLoader;

/**
 * @author Kohsuke Kawaguchi
 */
public final class SourceFile {
    public final Package pkg;
    /**
     * Short file name within the package.
     */
    public final String fileName;

    public SourceFile(Package pkg, String fileName) {
        this.pkg = pkg;
        this.fileName = fileName;
    }

    public String getAstURL() {
        return pkg.baseURL()+'/'+fileName+".js";
    }

    /**
     * Retrieves the AST for this source file and invokes the callback.
     */
    public void retrieveAST(Callback<AST> callback) {
        SourceFileLoader.INSTANCE.retrieve(this,callback);
    }

    @Override
    public boolean equals(Object o) {
        SourceFile that = (SourceFile) o;
        return fileName.equals(that.fileName) && pkg==that.pkg;
    }

    @Override
    public int hashCode() {
        return 31 * pkg.hashCode() + fileName.hashCode();
    }
}
