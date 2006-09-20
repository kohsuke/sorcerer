package org.jvnet.sorcerer;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeInfo;

import javax.lang.model.element.Element;

/**
 * {@link TreeScanner} that works around an issue in {@link Trees#getElement(TreePath)}
 * not always returning the corresponding {@link Element}.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class TreeScanner2<P,R> extends TreeScanner<P,R> {

    /**
     * Returns the {@link Element} that corresponds to the given {@link Tree} node.
     */
    protected final Element getElement(Tree t) {
        return getElement((JCTree)t);
    }

    /**
     * javac stopped giving us {@link Element}s from some tree nodes such as
     * method invocations, so this is the code to work around the issue.
     */
    private Element getElement(JCTree t) {
        t = TreeInfo.skipParens(t);
        switch (t.tag) {
        case JCTree.CLASSDEF:
            return ((JCClassDecl)t).sym;
        case JCTree.METHODDEF:
            return ((JCMethodDecl)t).sym;
        case JCTree.VARDEF:
            return ((JCVariableDecl)t).sym;
        case JCTree.SELECT:
            return ((JCFieldAccess)t).sym;
        case JCTree.APPLY:
            return getElement(((JCMethodInvocation)t).meth);
        case JCTree.IDENT:
            return ((JCIdent)t).sym;
        case JCTree.NEWCLASS:
            return ((JCNewClass)t).constructor;
        default:
            return null;
        }
    }
}
