package org.jvnet.sorcerer;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.AbstractTypeVisitor6;

/**
 * Computes a short type name suitable for displaying in a limited space.
 * @author Kohsuke Kawaguchi
 */
public final class ShortNameVisitor extends AbstractTypeVisitor6<String,Void> {
    public static final ShortNameVisitor INSTANCE = new ShortNameVisitor();

    private ShortNameVisitor() {
    }

    public String visitPrimitive(PrimitiveType t, Void p) {
        return t.toString();
    }

    public String visitNull(NullType t, Void p) {
        return t.toString();
    }

    public String visitArray(ArrayType t, Void p) {
        return t.getComponentType().accept(this,null)+"[]";
    }

    public String visitDeclared(DeclaredType t, Void p) {
        return t.asElement().getSimpleName().toString();
    }

    public String visitError(ErrorType t, Void p) {
        return t.toString();
    }

    public String visitTypeVariable(TypeVariable t, Void p) {
        return t.toString();
    }

    public String visitWildcard(WildcardType t, Void p) {
        TypeMirror b = t.getExtendsBound();
        if(b!=null)
            return "? extends "+b.accept(this,null);
        else
            return "? super "+t.getSuperBound().accept(this,null);
    }

    public String visitExecutable(ExecutableType t, Void p) {
        return t.toString();
    }

    public String visitNoType(NoType t, Void p) {
        return t.toString();
    }
}
