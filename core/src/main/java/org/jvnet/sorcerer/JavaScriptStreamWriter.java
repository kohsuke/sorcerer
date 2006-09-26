package org.jvnet.sorcerer;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedHashMap;

/**
 * {@link PrintWriter} extended for writing JavaScript in sorcerer.
 *
 * This class provides:
 * <ol>
 *  <li>formatting convenience methods
 *  <li>pretty printing for debugging
 *  <li>Type name interning.
 * </ol>
 *
 * @author Kohsuke Kawaguchi
 */
public class JavaScriptStreamWriter extends PrintWriter {
    private boolean first=false;
    private int indent=0;
    private final ParsedSourceSet pss;

    private abstract class SymbolTable<E> extends LinkedHashMap<E,Integer> {
        public void add(E e) {
            if(e==null)
                throw new IllegalArgumentException();
            if(!containsKey(e))
                put(e,size());
        }
        public int find(E e) {
            Integer v = get(e);
            if(v==null)     throw new IllegalStateException("No such symbol: "+e);
            return v;
        }
        public void write() {
            beginArray();
            for (E e : keySet())
                writeItem(e);
            endArray();
        }

        protected abstract void writeItem(E e);
    }

    private final SymbolTable<TypeElement> typeTable = new SymbolTable<TypeElement>() {
        protected void writeItem(TypeElement e) {
            beginArray();
            sep();
            string(e.getQualifiedName());
            writeModifiers(e);
            endArray();
        }
    };
    private final SymbolTable<ExecutableElement> methodTable = new SymbolTable<ExecutableElement>() {
        protected void writeItem(ExecutableElement e) {
            beginArray();
            sep();
            ref((TypeElement)e.getEnclosingElement());
            sep();
            string(e.getSimpleName());
            beginArray();
            for (VariableElement v : e.getParameters()) {
                sep();
                TypeMirror vt = pss.getTypes().erasure(v.asType());
                TypeElement ve = (TypeElement) pss.getTypes().asElement(vt);
                if(ve!=null)
                    ref(ve);
                else // such as primitive types, arrays, etc.
                    string(vt.toString());
            }
            endArray();
            writeModifiers(e);
            endArray();
        }

        public void add(ExecutableElement e) {
            super.add(e);
            typeTable.add((TypeElement)e.getEnclosingElement());
            for (VariableElement v : e.getParameters()) {
                TypeMirror vt = pss.getTypes().erasure(v.asType());
                TypeElement ve = (TypeElement) pss.getTypes().asElement(vt);
                if(ve!=null)
                    typeTable.add(ve);
            }
        }
    };

    public JavaScriptStreamWriter(Writer out, ParsedSourceSet pss) {
        super(out);
        this.pss=pss;
    }

    public JavaScriptStreamWriter i() {
        indent++;
        return this;
    }
    public JavaScriptStreamWriter o() {
        indent--;
        return this;
    }

    /**
     * Writes a separator if needed
     */
    public JavaScriptStreamWriter sep() {
        if(first) {
            first = false;
        } else {
            print(',');
            nl();
        }
        return this;
    }

    public JavaScriptStreamWriter nl() {
        if(doIdent) {
            println();
            for( int i=0; i<indent; i++ )
                print("  ");
        }
        return this;
    }

    public void resetList() {
        first = true;
    }

    public void beginList() {
        first=true;
        i().nl();
    }

    public void endList() {
        o().nl();
        first=false;
    }

    /**
     * Writes a quoted string value.
     */
    public void string(Object s) {
        print('"');
        String str = s.toString();
        for( int i=0; i<str.length(); i++ ) {
            char ch=str.charAt(i);
            if(ch=='"')
                print("\\\"");
            else
                print(ch);
        }
        print('"');
    }

    /**
     * Writes the modifier set for a reference.
     */
    public void writeModifiers(Element e) {
        StringBuilder b = new StringBuilder();
        if(e.getModifiers().contains(Modifier.STATIC))
            b.append('s');
        if(pss.getElements().isDeprecated(e))
            b.append('d');
        if(b.length()>0)
            sep().string(b);
    }

    /**
     * Starts a new function call.
     */
    public void beginMethod(String methodName) {
        sep().print(methodName);
        print('(');
        beginList();
    }

    public void endMethod() {
        endList();
        print(')');
    }

    public void beginArray() {
        sep().print('[');
        beginList();
    }

    public void endArray() {
        endList();
        print(']');
    }

    /**
     * Writes a method reference number.
     */
    public void ref(ExecutableElement e) {
        print(methodTable.find(e));
    }

    /**
     * Writes a type reference number.
     */
    public void ref(TypeElement e) {
        print(typeTable.find(e));
    }

    /**
     * Collects {@link Element} used in this source file
     * and assign indices to them.
     */
    public void collect(Element e) {
        switch (e.getKind()) {
        case ANNOTATION_TYPE:
        case CLASS:
        case ENUM:
        case INTERFACE:
            typeTable.add((TypeElement)e);
            break;
        case CONSTRUCTOR:
        case METHOD:
            ExecutableElement ee = (ExecutableElement) e;
            methodTable.add(ee);
            break;
        }
    }

    /**
     * Writes out the symbol table.
     */
    public void writeSymbolTable() {
        print("typeTable(");
        beginList();
        typeTable.write();
        print(");");

        print("methodTable(");
        beginList();
        methodTable.write();
        print(");");
    }

    static boolean doIdent=false;
    static {
        try {
            doIdent = Boolean.getBoolean("sorcerer.debug");
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
