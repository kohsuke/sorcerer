package org.jvnet.sorcerer;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

import javax.lang.model.element.Element;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractReferenceFinder extends TreeScanner2<Void,Void> {

    protected abstract void candidate(Tree t, Element e);

    public Void visitIdentifier(IdentifierTree id, Void _) {
        candidate(id, getElement(id));
        return super.visitIdentifier(id,_);
    }

    public Void visitMemberSelect(MemberSelectTree mst, Void _) {
        candidate(mst, getElement(mst));
        return super.visitMemberSelect(mst,_);
    }

    public Void visitMethodInvocation(MethodInvocationTree mi, Void _) {
        candidate(mi, getElement(mi));
        return super.visitMethodInvocation(mi, _);
    }
}
