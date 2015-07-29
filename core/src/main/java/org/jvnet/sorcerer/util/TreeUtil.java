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
import javax.lang.model.type.TypeKind;
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
            TypeKind kind = e.asType().getKind();
            return !kind.isPrimitive() && kind!= TypeKind.VOID; // primitive elements don't work well.
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
     * Computes the full method signature with the FQCN as parameter names.
     * For example, "foo(int,java.lang.String)".
     *
     * @param buf
     *      This method name will be appepended to this buffer.
     */
    public static StringBuilder buildMethodName(StringBuilder buf, Types types, ExecutableElement e) {
        // use the full parameter list as a part of ID to handle overloading
        buf.append(e.getSimpleName()).append('(');
        boolean first=true;
        List<? extends VariableElement> parameters = safeGetParameters(e);
        for (VariableElement v : parameters) {
            if(first)   first=false;
            else        buf.append(',');
            buf.append(types.erasure(v.asType()));
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
        Element r = getElement((JCTree) t);

        // the following two lines causes primitive fields and variables to be ignored.
        // so commented out.
        
        //if(r!=null && r.asType()!=null && r.asType().getKind().isPrimitive())
        //    return null;    // TypeElement for primitives aren't really functioning, so avoid returning it.
        return r;
    }

    /**
     * javac stopped giving us {@link Element}s from some tree nodes such as
     * method invocations, so this is the code to work around the issue.
     */
    private static Element getElement(JCTree t) {
        t = TreeInfo.skipParens(t);
        switch (t.getTag()) {
        case CLASSDEF:
            return ((JCClassDecl)t).sym;
        case METHODDEF:
            return ((JCMethodDecl)t).sym;
        case VARDEF:
            return ((JCVariableDecl)t).sym;
        case SELECT:
            return ((JCFieldAccess)t).sym;
        case APPLY:
            return getElement(((JCMethodInvocation)t).meth);
        case IDENT:
            return ((JCIdent)t).sym;
        case NEWCLASS:
            return ((JCNewClass)t).constructor;
        default:
            return null;
        }
    }

    /**
     * Returns true if the given program element should be mentioned in the outline view.
     */
    public static final EnumSet<ElementKind> OUTLINE_WORTHY_ELEMENT = EnumSet.of(
        ElementKind.ANNOTATION_TYPE,
        ElementKind.CLASS,
        ElementKind.CONSTRUCTOR,
        ElementKind.ENUM,
        ElementKind.ENUM_CONSTANT,
        ElementKind.FIELD,
        ElementKind.INSTANCE_INIT,
        ElementKind.INTERFACE,
        ElementKind.METHOD,
        ElementKind.STATIC_INIT,
        ElementKind.PACKAGE
    );

    public static final EnumSet<Tree.Kind> OUTLINE_WORTHY_TREE = EnumSet.of(
        Tree.Kind.CLASS,
        Tree.Kind.METHOD,
        Tree.Kind.VARIABLE
    );

    /**
     * Returns true if the given program element is a local type.
     */
    public static boolean isLocal(Element e) {
        e = e.getEnclosingElement();
        while(e!=null) {
            switch(e.getKind()) {
            case CONSTRUCTOR:
            case METHOD:
            case ENUM_CONSTANT:
            case FIELD:
            case INSTANCE_INIT: // following Eclipse. doesn't make much sense to me...
            case STATIC_INIT:   // ditto
                return true;
            case PACKAGE:
                return false;
            }
            e = e.getEnclosingElement();
        }
        return false;
    }

    /**
     * Gets the "Foo" portion of "Foo.java" that represents this compilation unit.
     */
    public static String getPrimaryTypeName(CompilationUnitTree cu) {
        String name = cu.getSourceFile().getName();
        int idx = name.lastIndexOf('/');
        if(idx>=0)  name=name.substring(idx+1);
        idx = name.lastIndexOf('\\');
        if(idx>=0)  name=name.substring(idx+1);

        if(name.endsWith(".java"))  name=name.substring(0,name.length()-5);
        return name;
    }
}
