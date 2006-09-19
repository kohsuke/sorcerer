package org.jvnet.sorcerer;

import javax.lang.model.element.TypeElement;
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
import javax.lang.model.util.Types;

/**
 * Computes the erasure as Javadoc does.
 *
 * <p>
 * In particular, this leaves the type parameter as-is.
 *
 * @author Kohsuke Kawaguchi
 */
public class JavadocErasureVisitor extends AbstractTypeVisitor6<TypeMirror,Void> {
    private final Types types;

    public JavadocErasureVisitor(Types types) {
        this.types = types;
    }

    public TypeMirror visitPrimitive(PrimitiveType t, Void p) {
        return t;
    }

    public TypeMirror visitNull(NullType t, Void p) {
        return t;
    }

    public TypeMirror visitArray(ArrayType t, Void p) {
        return types.getArrayType(t.getComponentType().accept(this,null));
    }

    public TypeMirror visitDeclared(DeclaredType t, Void p) {
        return types.getDeclaredType((TypeElement)t.asElement());
    }

    public TypeMirror visitError(ErrorType t, Void p) {
        return t;
    }

    public TypeMirror visitTypeVariable(TypeVariable t, Void p) {
        // return the variable as is, instead of using the bound
        return t;
    }

    public TypeMirror visitWildcard(WildcardType t, Void p) {
        throw new IllegalStateException(t.toString());  // should never be called
    }

    public TypeMirror visitExecutable(ExecutableType t, Void p) {
        throw new IllegalStateException(t.toString());  // ditto.
    }

    public TypeMirror visitNoType(NoType t, Void p) {
        return t;
    }
}
