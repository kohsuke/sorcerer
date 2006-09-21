package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import org.jvnet.sorcerer.util.TreeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Finds all the actual usage of the field/methods of the given type.
 * @author Kohsuke Kawaguchi
 */
final class RefererFinder extends TreePathScanner<Void,Void> {
    /**
     * @return
     *      keys are the fields and methods defined on the given type.
     *      values are all the tree nodes where it's actually referenced.
     */
    static Map<Element,Set<TreePath>> find(ParsedType t) {
        RefererFinder finder = new RefererFinder(t.element);
        for (CompilationUnitTree cu : t.getReferers())
            finder.scan(cu,null);
        return finder.result;
    }

    private final TypeElement type;
    private final Map<Element,Set<TreePath>> result = new HashMap<Element,Set<TreePath>>();

    private RefererFinder(TypeElement type) {
        this.type = type;
    }

    protected void candidate(Element e) {
        if(e==null) return;

        switch(e.getKind()) {
        case ANNOTATION_TYPE:
        case CLASS:
        case ENUM:
        case INTERFACE:
            if(type.equals(e)) {
                add(type);
            }
            break;
        case ENUM_CONSTANT:
        case METHOD:
        case CONSTRUCTOR:
        case FIELD:
            // if this is field and method, the parent should be a type
            Element p = e.getEnclosingElement();
            if(p!=null && type.equals(p))
                add(e);
            break;
        }
    }

    private void add(Element e) {
        Set<TreePath> trees = result.get(e);
        if(trees==null)
            result.put(e,trees=new HashSet<TreePath>());
        trees.add(getCurrentPath());
    }


    public Void visitIdentifier(IdentifierTree id, Void _) {
        candidate(TreeUtil.getElement(id));
        return super.visitIdentifier(id,_);
    }

    public Void visitMemberSelect(MemberSelectTree mst, Void _) {
        candidate(TreeUtil.getElement(mst));
        return super.visitMemberSelect(mst,_);
    }

    // MethodInvocationTree.getMethodSelect() always find the method that it's referencing,
    // so if we have the following code, we'll be finding a redundant reference.
    //public Void visitMethodInvocation(MethodInvocationTree mi, Void _) {
    //    candidate(TreeUtil.getElement(mi));
    //    return super.visitMethodInvocation(mi, _);
    //}
}
