package org.jvnet.sorcerer.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Tree/Element related utility code that really should be a part of the tree API.
 *
 * @author Kohsuke Kawaguchi
 */
public class TreeUtil {
    public static boolean isType(Element e) {
        if(e==null) return false;
        switch(e.getKind()) {
        case ANNOTATION_TYPE:
        case CLASS:
        case ENUM:
        case INTERFACE:
            return true;
        }
        return false;
    }

    public static String getPackageName(CompilationUnitTree cu) {
        ExpressionTree packageName = cu.getPackageName();
        if(packageName==null)   return "";
        return packageName.toString();
    }

    public static String getFullMethodName(Types types, ExecutableElement e) {
        return buildMethodName(new StringBuilder(),types,e).toString();
    }

    /**
     * Computes the full method name with the FQCN as parameter names.
     */
    public static StringBuilder buildMethodName(StringBuilder buf, Types types, ExecutableElement e) {
        // use the full parameter list as a part of ID to handle overloading
        buf.append(e.getSimpleName()).append('(');
        boolean first=true;
        List<? extends VariableElement> parameters = safeGetParameters(e);
        for (VariableElement v : parameters) {
            buf.append(types.erasure(v.asType()));
            if(first)   first=false;
            else        buf.append(',');
        }
        return buf.append(')');
    }

    /**
     * To work around a bug in the javac error recovery.
     */
    private static List<? extends VariableElement> safeGetParameters(ExecutableElement e) {
        try {
            return e.getParameters();
        } catch (Exception _) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the {@link Element} that corresponds to the given {@link Tree} node.
     */
    public static Element getElement(Tree t) {
        return getElement((JCTree)t);
    }

    /**
     * javac stopped giving us {@link Element}s from some tree nodes such as
     * method invocations, so this is the code to work around the issue.
     */
    private static Element getElement(JCTree t) {
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

    /**
     * Returns true if the given program element should be mentioned in the outline view.
     */
    public static final EnumSet<ElementKind> OUTLINE_WORTHY = EnumSet.of(
        ElementKind.ANNOTATION_TYPE,
        ElementKind.CLASS,
        ElementKind.CONSTRUCTOR,
        ElementKind.ENUM,
        ElementKind.ENUM_CONSTANT,
        ElementKind.FIELD,
        ElementKind.INSTANCE_INIT,
        ElementKind.INTERFACE,
        ElementKind.METHOD,
        ElementKind.STATIC_INIT
    );

    /**
     * Returns true if the given program element is a local type.
     */
    public static boolean isLocal(Element e) {
        switch(e.getEnclosingElement().getKind()) {
        case CONSTRUCTOR:
        case METHOD:
        case ENUM_CONSTANT:
        case FIELD:
        case INSTANCE_INIT: // following Eclipse. doesn't make much sense to me...
        case STATIC_INIT:   // ditto
            return true;
        }
        return false;
    }
}
