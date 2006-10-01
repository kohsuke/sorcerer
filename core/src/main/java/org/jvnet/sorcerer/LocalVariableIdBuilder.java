package org.jvnet.sorcerer;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.Trees;
import org.jvnet.sorcerer.util.TreeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.AbstractElementVisitor6;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Computes ID for local variables.
 *
 * <P>
 * This visitor implementation tries to assign semi-unique (but hopefully meaningful and robust)
 * ID to local variables. The actual uniqueness will be guaranteed by using the additional number
 * suffix.
 *
 * @author Kohsuke Kawaguchi
 */
final class LocalVariableIdBuilder extends AbstractElementVisitor6<StringBuilder,Void> {
    private final ParsedSourceSet pss;
    private final Trees trees;
    private final Types types;
    private final Elements elements;

    LocalVariableIdBuilder(ParsedSourceSet pss) {
        this.pss = pss;
        this.trees = pss.getTrees();
        this.elements = pss.getElements();
        this.types = pss.getTypes();
    }

    public String href(Element e) {
        return visit(e).toString();
    }

    public StringBuilder visitType(TypeElement t, Void _) {
        ClassTree ct = trees.getTree(t);
        if(ct ==null)
            return null;    // not a part of compiled source files

        switch(t.getNestingKind()) {
        case ANONYMOUS:
            String binaryName = elements.getBinaryName(t).toString();
            int idx = binaryName.lastIndexOf('$');
            String name = "~"+binaryName.substring(idx); // #$1 is ambiguous between field and anonyous type, so use '~' as the prefix for type
            return combine(getEnclosingTypeOrPackage(t).accept(this,null)).append(name);
        case TOP_LEVEL:
            // check if this class is the 'primary type' of the compilation unit
            CompilationUnitTree owner = pss.getTreePathByClass().get(ct).getCompilationUnit();
            String primaryTypeName = TreeUtil.getPrimaryTypeName(owner);
            String simpleName = ct.getSimpleName().toString();

            StringBuilder buf = new StringBuilder();
            if(!primaryTypeName.equals(simpleName)) {
                buf.append("~").append(simpleName);
            }
            return buf;
        case MEMBER:
        case LOCAL:
            return recurse(t).append('~').append(t.getSimpleName());

        default:
            throw new IllegalStateException(t.getNestingKind().toString());
        }
    }

    private Element getEnclosingTypeOrPackage(Element e) {
        while(true) {
            e = e.getEnclosingElement();
            if (e instanceof TypeElement || e instanceof PackageElement) {
                return e;
            }
        }
    }

    public StringBuilder visitVariable(VariableElement v, Void _) {
        StringBuilder buf = recurse(v);
        if(buf==null)   return null;
        return buf.append(v.getSimpleName());
    }

    public StringBuilder visitExecutable(ExecutableElement e, Void _) {
        StringBuilder buf = recurse(e);
        if(buf==null)   return null;

        switch(e.getKind()) {
        case METHOD:
        case CONSTRUCTOR:
            return TreeUtil.buildMethodName(buf,types,e);
        default:
            // static/instance initializers.
            return buf.append(getSiblingIndex(e));
        }
    }

    public StringBuilder visitTypeParameter(TypeParameterElement v, Void _) {
        // TODO: impossible
        throw new IllegalStateException();
    }

    public StringBuilder visitPackage(PackageElement p, Void _) {
        // TODO: impossible
        throw new IllegalStateException();
    }


    private StringBuilder recurse(Element e) {
        return combine(e.getEnclosingElement().accept(this,null));
    }

    private StringBuilder combine(StringBuilder prefix) {
        if(prefix==null)    return null;
        int len = prefix.length();
        if(len>0 && prefix.charAt(len-1)=='/')
            return prefix;

        if(len>0)
            prefix.append('-');
        return prefix;
    }

    /**
     * Gets the index of this {@link Element} within its siblings.
     */
    private int getSiblingIndex(Element e) {
        int idx = e.getEnclosingElement().getEnclosedElements().indexOf(e);
        assert idx>=0;
        return idx;
    }
}
