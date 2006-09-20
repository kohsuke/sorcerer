package org.jvnet.sorcerer;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.SimpleElementVisitor6;

/**
 * Computes the name of a program element suitable to be used in the outline view.
 * @author Kohsuke Kawaguchi
 */
public final class OutlineNameVisitor extends SimpleElementVisitor6<String,Void> {

    private final TypeVisitor<TypeMirror,Void> javadocErasure;

    public OutlineNameVisitor(ParsedSourceSet pss) {
        this.javadocErasure = new JavadocErasureVisitor(pss.getTypes());
    }

    public String visitType(TypeElement t, Void _) {
        return t.getSimpleName().toString();
    }

    public String visitVariable(VariableElement v, Void _) {
        return v.getSimpleName().toString();
    }

    public String visitExecutable(ExecutableElement e, Void _) {
        StringBuilder buf = new StringBuilder();
        if(e.getKind()== ElementKind.CONSTRUCTOR)
            // javadoc uses the class name as the constructor name, not <init>
            buf.append(e.getEnclosingElement().getSimpleName());
        else
            buf.append(e.getSimpleName());

        buf.append('(');

        boolean first=true;

        for (VariableElement p : e.getParameters()) {
            if(first)       first = false;
            else            buf.append(',');

            buf.append(p.asType().accept(ShortNameVisitor.INSTANCE,null));
        }
        buf.append(')');
        if(e.getKind()!=ElementKind.CONSTRUCTOR) {
            buf.append(':');
            buf.append(e.getReturnType().accept(javadocErasure,null));
        }
        return buf.toString();
    }
}
