package org.jvnet.sorcerer.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;

import javax.lang.model.element.Element;

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
}
