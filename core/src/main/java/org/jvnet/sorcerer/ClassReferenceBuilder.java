package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import org.jvnet.sorcerer.util.TreeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builds a map of which class references what.
 *
 * <p>
 * The resulting "visibility" information is used as index for
 * finding references.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ClassReferenceBuilder extends AbstractReferenceFinder {
    /**
     * Builds the index. Sole entry point to this class.
     */
    public static Map<TypeElement,Set<CompilationUnitTree>> build(Collection<CompilationUnitTree> sourceFiles) {
        ClassReferenceBuilder crb = new ClassReferenceBuilder();
        for (CompilationUnitTree cu : sourceFiles) {
            crb.scan(cu);
        }
        return crb.index;
    }

    private final Map<TypeElement,Set<CompilationUnitTree>> index =
        new HashMap<TypeElement,Set<CompilationUnitTree>>();

    /**
     * Currently compilation unit that we are visiting.
     */
    private CompilationUnitTree cu;

    /**
     * Often a single compilation unit has multiple references to the same type,
     * so remember what types have already been recorded to improve the index
     * building performance.
     */
    private final Set<TypeElement> discovered = new HashSet<TypeElement>();

    private ClassReferenceBuilder() {} // no instanciation allowed.

    private void scan(CompilationUnitTree cu) {
        this.cu = cu;
        discovered.clear();
        cu.accept(this,null);
    }

    private void record(TypeElement t) {
        if(discovered.add(t)) {
            Set<CompilationUnitTree> set = index.get(t);
            if(set==null)
                index.put(t,set=new HashSet<CompilationUnitTree>());
            set.add(cu);
        }
    }

    @Override
    protected void candidate(Tree t, Element e) {
        if(e==null) return;

        switch(e.getKind()) {
        case ANNOTATION_TYPE:
        case CLASS:
        case ENUM:
        case INTERFACE:
            record((TypeElement)e);
            break;
        case ENUM_CONSTANT:
        case METHOD:
        case CONSTRUCTOR:
        case FIELD:
            // if this is field and method, the parent should be a type
            Element p = e.getEnclosingElement();
            if(TreeUtil.isType(p))
                record((TypeElement)p);
            break;
        }
    }
}
