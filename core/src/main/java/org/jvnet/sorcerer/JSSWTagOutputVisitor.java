package org.jvnet.sorcerer;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Set;

/**
 * Visitor that walks the Tag tree and outputs structure
 * using a JavaScriptStreamWriter.
 */
public class JSSWTagOutputVisitor implements TagVisitor {

    private final JavaScriptStreamWriter javaScriptStreamWriter;

    public JSSWTagOutputVisitor(JavaScriptStreamWriter javaScriptStreamWriter) {
        this.javaScriptStreamWriter = javaScriptStreamWriter;
    }

    protected void writeChildren(Tag tag, String functionName) {
        javaScriptStreamWriter.beginMethod(functionName);
        for (Tag t : tag) {
            t.writeOut(this);
        }
        javaScriptStreamWriter.endMethod();
    }

    public void visit(Tag.ReservedWord reservedWord) {
        String t = ReservedWords.TOKENMAP.get(reservedWord.token);
        assert t!=null;
        javaScriptStreamWriter.sep().print(t);
    }

    public void visit(Tag.Comment comment) {
        writeChildren(comment, "O");
    }

    public void visit(Tag.CurlyBracket curlyBracket) {
        writeChildren(curlyBracket, "B");
    }

    public void visit(Tag.Parenthesis parenthesis) {
        writeChildren(parenthesis, "P");
    }

    public void visit(Tag.Killer killer) {
        // noop
    }

    public void visit(Tag.SourceText sourceText) {
        String text = sourceText.text;
        
            if(text.equals(" ")) {
                javaScriptStreamWriter.sep().print("_");
                return;
            }

            StringBuilder buf = new StringBuilder();
            int len = text.length();
            for( int i=0; i<len; i++ ) {
                char ch = text.charAt(i);
                switch(ch) {
                case '\r':
                    if(i!=len-1 && text.charAt(i+1)=='\n')
                        break; // combine with the next LF
                    // fall through next
                case '\n':
                    // flush
                    if(buf.length()>0) {
                        javaScriptStreamWriter.sep();
                        javaScriptStreamWriter.string(buf);
                        buf.setLength(0);
                    }
                    javaScriptStreamWriter.sep();
                    javaScriptStreamWriter.print("nl");
                    break;
                case ' ':
                    // if long enough, collapse
                    int j=i;
                    while(j<len && text.charAt(j)==' ')
                        j++;
                    final int k=j-i;

                    if(k>=4) {
                        // flush
                        if(buf.length()>0) {
                            javaScriptStreamWriter.sep();
                            javaScriptStreamWriter.string(buf);
                            buf.setLength(0);
                        }
                        javaScriptStreamWriter.sep();
                        javaScriptStreamWriter.print("w(");
                        javaScriptStreamWriter.print(k);
                        javaScriptStreamWriter.print(")");
                    } else {
                        // nope. it's cheaper to just inline them
                        for( ; j>i; j-- )
                            buf.append(' ');
                    }
                    i+=k-1; // for loop with add the last +1
                    break;
                case '\t':
                    throw new IllegalStateException(); // should be eliminated by now by tab expansion

                // HTML escape
                case '<':
                    buf.append("&lt;");
                    break;
                case '&':
                    buf.append("&amp;");
                    break;
                default:
                    buf.append(ch);
                }
            }

            // flush
            if(buf.length()>0) {
                javaScriptStreamWriter.sep();
                javaScriptStreamWriter.string(buf);
                buf.setLength(0);
            }
    }

    public void visit(Tag.Literal literal) {
        writeChildren(literal, "L");
    }

    public void visit(Tag.DeclName declName) {
        javaScriptStreamWriter.beginMethod("I");
        declName.firstChild.writeOut(this);
        javaScriptStreamWriter.endMethod();
    }

    public void visit(Tag.ClassDecl classDecl) {
        javaScriptStreamWriter.beginMethod("C");
        javaScriptStreamWriter.ref(classDecl.type);
        javaScriptStreamWriter.beginArray();
        for (ParsedType d : classDecl.descendants)
            javaScriptStreamWriter.ref(d.element);
        javaScriptStreamWriter.endArray();
        writeChildren(classDecl, "$");
        javaScriptStreamWriter.endMethod();
    }

    public void visit(Tag.TypeRef typeRef) {
        javaScriptStreamWriter.beginMethod("T");
        javaScriptStreamWriter.ref(typeRef.type);
        javaScriptStreamWriter.endMethod();
    }

    public void visit(Tag.FieldDecl fieldDecl) {
        writeChildren(fieldDecl, "F");
    }

    public void visit(Tag.FieldRef fieldRef) {
        VariableElement decl = fieldRef.decl;
        javaScriptStreamWriter.beginMethod("G");
        javaScriptStreamWriter.ref((TypeElement)decl.getEnclosingElement());
        javaScriptStreamWriter.writeModifiers(decl);
        javaScriptStreamWriter.sep();
        javaScriptStreamWriter.string(decl.getSimpleName());
        javaScriptStreamWriter.endMethod();
    }

    public void visit(Tag.LocalVarDecl localVarDecl) {
        javaScriptStreamWriter.beginMethod("V");
        javaScriptStreamWriter.ref(localVarDecl.var);
        writeChildren(localVarDecl, "$");
        javaScriptStreamWriter.endMethod();
    }

    public void visit(Tag.LocalVarRef localVarRef) {
        javaScriptStreamWriter.beginMethod("W");
        javaScriptStreamWriter.ref(localVarRef.decl);
        javaScriptStreamWriter.endMethod();
    }

    public void visit(Tag.MethodDecl methodDecl) {
        javaScriptStreamWriter.beginMethod("M");
        javaScriptStreamWriter.ref(methodDecl.method);
        writeSet(methodDecl.overridden);
        writeSet(methodDecl.overriding);
        writeChildren(methodDecl, "$");
        javaScriptStreamWriter.endMethod();
    }

    private void writeSet(Set<ParsedType.Match> ms) {
        javaScriptStreamWriter.beginArray();
        for (ParsedType.Match m : ms)
            javaScriptStreamWriter.ref(m.method);
        javaScriptStreamWriter.endArray();
    }

    public void visit(Tag.MethodRef methodRef) {
        javaScriptStreamWriter.beginMethod("N");
        javaScriptStreamWriter.ref(methodRef.method);
        javaScriptStreamWriter.endMethod();
    }

    public void visit(Tag.Root root) {
        writeChildren(root, "classDef");
    }
}
