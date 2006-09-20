package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

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
final class RefererFinder extends AbstractReferenceFinder {
    /**
     * @return
     *      keys are the fields and methods defined on the given type.
     *      values are all the tree nodes where it's actually referenced.
     */
    static Map<Element,Set<Tree>> find(ParsedType t) {
        RefererFinder finder = new RefererFinder(t.element);
        for (CompilationUnitTree cu : t.getReferers())
            cu.accept(finder, null);
        return finder.result;
    }

    private final TypeElement type;
    private final Map<Element,Set<Tree>> result = new HashMap<Element,Set<Tree>>();

    private RefererFinder(TypeElement type) {
        this.type = type;
    }

    @Override
    protected void candidate(Tree t, Element e) {
        if(e==null) return;

        switch(e.getKind()) {
        case ANNOTATION_TYPE:
        case CLASS:
        case ENUM:
        case INTERFACE:
            if(type.equals(e)) {
                add(type,t);
            }
            break;
        case ENUM_CONSTANT:
        case METHOD:
        case CONSTRUCTOR:
        case FIELD:
            // if this is field and method, the parent should be a type
            Element p = e.getEnclosingElement();
            if(p!=null && type.equals(p))
                add(e,t);
            break;
        }
    }

    private void add(Element e, Tree t) {
        Set<Tree> trees = result.get(e);
        if(trees==null)
            result.put(e,trees=new HashSet<Tree>());
        trees.add(t);
    }
}
