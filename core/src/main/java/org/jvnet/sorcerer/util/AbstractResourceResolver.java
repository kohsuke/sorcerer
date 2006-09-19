package  org.jvnet.sorcerer.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import org.jvnet.sorcerer.ResourceResolver;

import java.util.StringTokenizer;

/**
 * {@link ResourceResolver} with convenient helper methods.
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractResourceResolver implements ResourceResolver {
    /**
     * Computes the string that represents "../../" to point to
     * the top output directory.
     *
     * For example, if the <tt>cu</tt> represents <tt>org.acme.Foo</tt>,
     * then this method will return "../../".
     */
    protected String getRelativePathToTop(CompilationUnitTree cu) {
        StringBuilder buf = new StringBuilder("../");
        ExpressionTree packageName = cu.getPackageName();
        if(packageName==null)
            return "./";
        for( int i=new StringTokenizer(packageName.toString(),".").countTokens(); i>1; i-- )
            buf.append("../");
        return buf.toString();
    }
}
