package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import org.jvnet.sorcerer.util.TreeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Base class for adding markers to the {@link HtmlGenerator}.
 *
 * @see ParsedSourceSet
 */
abstract class MarkerBuilder<R,P> extends TreeScanner<R,P> {
    private final CompilationUnitTree cu;
    private final LineMap lineMap;
    private final HtmlGenerator gen;
    private final LinkResolver linkResolver;
    private final SourcePositions srcPos;
    private final Types types;

    public MarkerBuilder(CompilationUnitTree cu, HtmlGenerator gen, LinkResolver linkResolver,SourcePositions srcPos,Elements elements,Types types) {
        this.cu = cu;
        this.gen = gen;
        this.lineMap = cu.getLineMap();
        this.linkResolver = linkResolver;
        this.srcPos = srcPos;
        this.types = types;
    }

    private String buildId(Element e) {
        String buf = linkResolver.href(e);
        if(buf.length()==0)
            return null; // no ID
        if(buf.charAt(0)!='#')
            throw new IllegalStateException("Computed ID for "+e+" is "+buf);
        return buf.substring(1);
    }


    /**
     * Computes the string key for the usage search.
     */
    private String buildUsage(Element e) {
        StringBuilder buf = new StringBuilder();
        switch(e.getKind()) {
        case METHOD:
        case CONSTRUCTOR:
            buf.append(((TypeElement)e.getEnclosingElement()).getQualifiedName());
            buf.append('#');
            TreeUtil.buildMethodName(buf,types,(ExecutableElement)e);
            return buf.toString();
        case FIELD:
        case ENUM_CONSTANT:
            buf.append(((TypeElement)e.getEnclosingElement()).getQualifiedName());
            buf.append('#');
            buf.append(e.getSimpleName());
            return buf.toString();
        case ANNOTATION_TYPE:
        case CLASS:
        case INTERFACE:
        case ENUM:
            buf.append(((TypeElement)e).getQualifiedName());
            buf.append("#this");
            return buf.toString();
        default:
            return null;
        }
    }
}
