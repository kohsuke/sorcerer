package org.jvnet.sorcerer;

import org.jvnet.sorcerer.util.BiDiMap;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
        public void write(String functionName) {
            beginMethod(functionName);
            for (E e : keySet())
                writeItem(e);
            endMethod();
        }

        protected abstract void writeItem(E e);
    }

    private final SymbolTable<TypeElement> typeTable = new SymbolTable<TypeElement>() {
        private Map<Element,Integer> localId = new HashMap<Element,Integer>();

        private int getLocalId(Element e) {
            Integer i = localId.get(e);
            if(i==null) {
                localId.put(e,1);
                return 0;
            }
            localId.put(e,i+1);
            return i;
        }

        protected void writeItem(TypeElement e) {
            beginArray();
            sep();
            StringBuilder buf = new StringBuilder();
            calcEncodedName(e,buf);
            // string(e.getQualifiedName());
            string(buf);
            writeModifiers(e);
            endArray();
        }

        private void calcEncodedName(Element e, StringBuilder buf) {
            Element p = e.getEnclosingElement();
            switch (e.getKind()) {
            case PACKAGE:
                buf.append(((PackageElement) e).getQualifiedName());
                return;
            case CLASS:
            case ANNOTATION_TYPE:
            case ENUM:
            case INTERFACE:
                calcEncodedName(p,buf);
                if(buf.length()>0) {
                    if(p.getKind()== ElementKind.PACKAGE)
                        buf.append('.');
                    else
                        buf.append('$');
                }
                if(e.getSimpleName().length()==0)
                    // anonymous class
                    buf.append(getLocalId(p));
                else
                    buf.append(e.getSimpleName());
                return;
            default:
                calcEncodedName(p,buf);
                buf.append('$').append(getLocalId(p));
                return;
            }
        }
    };
    private final SymbolTable<ExecutableElement> methodTable = new SymbolTable<ExecutableElement>() {
        protected void writeItem(ExecutableElement e) {
            beginArray();
            ref((TypeElement)e.getEnclosingElement());
            sep();
            if(e.getKind()== ElementKind.CONSTRUCTOR)
                string(e.getEnclosingElement().getSimpleName());
            else
                string(e.getSimpleName());
            beginArray();
            for (VariableElement v : e.getParameters()) {
                TypeMirror vt = pss.getTypes().erasure(v.asType());
                TypeElement ve = (TypeElement) pss.getTypes().asElement(vt);
                if(ve!=null)
                    ref(ve);
                else // such as primitive types, arrays, etc.
                    sep().string(vt.toString());
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

    private final SymbolTable<VariableElement> localVariableTable = new SymbolTable<VariableElement>() {
        private BiDiMap<VariableElement,String> ids = new BiDiMap<VariableElement,String>();

        public void add(VariableElement ve) {
            super.add(ve);
            String id = new LocalVariableIdBuilder(pss).href(ve);

            if(ids.containsValue(id)) {
                int suffix=2;
                while(ids.containsValue(id+suffix))
                    suffix++;
                id = id+suffix;
            }
            ids.put(ve,id);
        }

        protected void writeItem(VariableElement ve) {
            beginArray();
            sep().string(ve.getSimpleName());
            sep().string(ids.get(ve));
            endArray();
        }
    };

    public JavaScriptStreamWriter(Writer out, ParsedSourceSet pss) {
        super(out);
        this.pss=pss;
    }

    public void writeHeader(String path, UUID id)
    {
        println("defineStructure(");
        string(path);
        print(',');
        string(id.toString());
        print(",function(factory){with(factory) { ");
        i().nl();
    }

    public void writeFooter() {
        o().nl().print(";}});");
    }

    public void writeBody(Tag.Root tree) {

        JSSWTagTableVisitor tagTableVisitor = new JSSWTagTableVisitor(this);
        tagTableVisitor.visit(tree);

        resetList();
        writeSymbolTable();

        // write the body
        println();
        resetList();
        print("return ");
        JSSWTagOutputVisitor tagOutputVisitor = new JSSWTagOutputVisitor(this);
        tagOutputVisitor.visit(tree);
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
            switch(ch){
            case '\\':
                print("\\\\");
                break;
            case '"':
                print("\\\"");
                break;
            default:
                print(ch);
            }
        }
        print('"');
    }

    /**
     * Writes the modifier set for a reference.
     */
    public void writeModifiers(Element e) {
        sep().string(getCssClass(e));
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
        sep().print(methodTable.find(e));
    }

    /**
     * Writes a type reference number.
     */
    public void ref(TypeElement e) {
        sep().print(typeTable.find(e));
    }

    /**
     * Writes a local variable reference number.
     */
    public void ref(VariableElement var) {
        sep().print(localVariableTable.find(var));
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
        case EXCEPTION_PARAMETER:
        case LOCAL_VARIABLE:
        case PARAMETER:
            localVariableTable.add((VariableElement)e);
            break;
        }
    }

    /**
     * Writes out the symbol table.
     */
    public void writeSymbolTable() {
        typeTable.write("typeTable");
        methodTable.write("methodTable");
        localVariableTable.write("localVariableTable");
    }

    /**
     * Computes the CSS class name for the given program element.
     */
    public String getCssClass(Element e) {
        StringBuilder buf = new StringBuilder();

        // static marker
        Set<Modifier> mods = e.getModifiers();
        if(mods.contains(Modifier.STATIC)) {
            if(buf.length()>0)
                buf.append(' ');
            buf.append("st");
        }

        // deprecated marker
        if(pss.getElements().isDeprecated(e)) {
            if(buf.length()>0)
                buf.append(' ');
            buf.append("dp");
        }

        if(buf.length()>0)
            buf.append(' ');

        switch (e.getKind()) {
        case ANNOTATION_TYPE:       buf.append("an"); break;
        case CLASS:                 buf.append("cl"); break;
        case CONSTRUCTOR:           buf.append("co"); break;
        case ENUM:                  buf.append("en"); break;
        case ENUM_CONSTANT:         buf.append("ec"); break;
        case EXCEPTION_PARAMETER:   buf.append("ex"); break;
        case FIELD:                 buf.append("fi"); break;
        case INSTANCE_INIT:         buf.append("ii"); break;
        case INTERFACE:             buf.append("it"); break;
        case LOCAL_VARIABLE:        buf.append("lv"); break;
        case METHOD:                buf.append("me"); break;
        case PACKAGE:               buf.append("pk"); break;
        case PARAMETER:             buf.append("pa"); break;
        case STATIC_INIT:           buf.append("si"); break;
        case TYPE_PARAMETER:        buf.append("tp"); break;
        default:                    break;
        }

        return buf.toString();
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
