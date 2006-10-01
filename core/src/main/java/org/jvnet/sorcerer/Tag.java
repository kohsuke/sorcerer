package org.jvnet.sorcerer;

import antlr.Token;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import org.jvnet.sorcerer.ParsedType.Match;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class Tag implements Iterable<Tag>, Comparable<Tag> {
    final long sp,ep;

    Tag firstChild;
    Tag nextSibling;

    protected Tag(long sp, long ep) {
        this.sp = sp;
        this.ep = ep;
    }

    protected Tag(LineMap lineMap, Token t) {
        sp = lineMap.getPosition(t.getLine(),t.getColumn());
        ep = sp+t.getText().length();
    }

    protected Tag(CompilationUnitTree unitTree, SourcePositions srcPos, Tree r) {
        this.sp = srcPos.getStartPosition(unitTree,r);
        this.ep = srcPos.getEndPosition(unitTree,r);
    }

    /**
     * Iterates children.
     */
    public Iterator<Tag> iterator() {
        return new Iterator<Tag>() {
            Tag next = firstChild;
            public boolean hasNext() {
                return next!=null;
            }

            public Tag next() {
                Tag r = next;
                next = next.nextSibling;
                return r;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public final int compareTo(Tag that) {
        long r;

        r= this.sp-that.sp;
        if(r!=0)    return sign(r);

        r = that.ep-this.ep;
        return sign(r);
    }

    private int sign(long r) {
        if(r>0) return 1;
        if(r<0) return -1;
        return 0;
    }

    /**
     * Writes the tag into JavaScript.
     */
    abstract void write(JavaScriptStreamWriter w);

    protected final void writeChildren(String functionName, JavaScriptStreamWriter w) {
        w.beginMethod(functionName);
        for (Tag t : this)
            t.write(w);
        w.endMethod();
    }

    /**
     * Used as the first pass of writing. Collect all symbols into the writer.
     */
    public void collectSymbols(JavaScriptStreamWriter w) {
        for (Tag t : this)
            t.collectSymbols(w);
    }

    /**
     * Reserved keywords like "class", "enum", including all primitive types.
     */
    public static final class ReservedWord extends Tag {
        private final String token;

        public ReservedWord(LineMap lineMap, Token t) {
            super(lineMap, t);
            this.token = t.getText();
        }

        @Override
        void write(JavaScriptStreamWriter w) {
            assert firstChild.nextSibling==null; // must be one SourceText.
            String t = ReservedWords.TOKENMAP.get(token);
            assert t!=null;
            w.sep().print(t);
        }
    }

    /**
     * Comment in the source code
     */
    public static final class Comment extends Tag {
        public Comment(LineMap lineMap, Token t) {
            super(lineMap, t);
        }

        @Override
        void write(JavaScriptStreamWriter w) {
            writeChildren("O",w);
        }
    }

    /**
     * Curly bracket {...}.
     */
    public static final class CurlyBracket extends Tag {
        public CurlyBracket(long sp, long ep) {
            super(sp,ep);
        }

        @Override
        void write(JavaScriptStreamWriter w) {
            writeChildren("B",w);
        }
    }

    /**
     * Parenthesis (...).
     */
    public static final class Parenthesis extends Tag {
        public Parenthesis(long sp, long ep) {
            super(sp,ep);
        }

        @Override
        void write(JavaScriptStreamWriter w) {
            writeChildren("P",w);
        }
    }

    /**
     * Used to scrape off certain lexical tokens.
     */
    public static final class Killer extends Tag {
        public Killer(LineMap lineMap, Token t) {
            super(lineMap, t);
        }

        void write(JavaScriptStreamWriter w) {
            // noop
        }
    }

    /**
     * Created from consecutive text in the source file (not separated
     * by any other tags.)
     */
    public static final class SourceText extends Tag {
        private final String text;
        public SourceText(long sp, long ep, String text) {
            super(sp, ep);
            this.text=text;
        }

        @Override
        void write(JavaScriptStreamWriter w) {
            assert firstChild==null;

            if(text.equals(" ")) {
                w.sep().print("_");
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
                        w.sep();
                        w.string(buf);
                        buf.setLength(0);
                    }
                    w.sep();
                    w.print("nl");
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
                            w.sep();
                            w.string(buf);
                            buf.setLength(0);
                        }
                        w.sep();
                        w.print("w(");
                        w.print(k);
                        w.print(")");
                    } else {
                        // nope. it's cheaper to just inline them
                        for( ; j>i; j-- )
                            buf.append(' ');
                    }
                    i+=k-1; // for loop with add the last +1
                    break;
                case '\t':
                    throw new IllegalStateException(); // should be eliminated by now by tab expansion
                default:
                    buf.append(ch);
                }
            }

            // flush
            if(buf.length()>0) {
                w.sep();
                w.string(buf);
                buf.setLength(0);
            }
        }
    }

    public static final class Literal extends Tag {
        public Literal(CompilationUnitTree unitTree, SourcePositions srcPos, Tree r) {
            super(unitTree, srcPos, r);
        }
        @Override
        void write(JavaScriptStreamWriter w) {
            writeChildren("L",w);
        }
    }

    /**
     * Marks up the method/class name in the method/class declaration,
     * so that they can be obtained easily.
     */
    public static final class DeclName extends Tag {
        Token debug;
        public DeclName(LineMap m,Token token) {
            super(m,token);
            this.debug = token;
        }

        @Override
        void write(JavaScriptStreamWriter w) {
            assert firstChild.nextSibling==null; // there must be just one SourceText child
            w.beginMethod("I");
            firstChild.write(w);
            w.endMethod();
        }
    }

    /**
     * Class declaration.
     */
    public static final class ClassDecl extends Tag {
        private final TypeElement type;
        private final List<ParsedType> descendants;

        public ClassDecl(CompilationUnitTree unitTree, SourcePositions srcPos, ClassTree ct, TypeElement e, List<ParsedType> descendants) {
            super(unitTree, srcPos, ct);
            this.type = e;
            this.descendants = descendants;
        }

        @Override
        void write(JavaScriptStreamWriter w) {
            w.beginMethod("C");
            w.ref(type);
            w.beginArray();
            for (ParsedType d : descendants)
                w.ref(d.element);
            w.endArray();
            writeChildren("$",w);
            w.endMethod();
        }

        @Override
        public void collectSymbols(JavaScriptStreamWriter w) {
            w.collect(type);
            for (ParsedType d : descendants)
                w.collect(d.element);
            super.collectSymbols(w);
        }
    }

    /**
     * Type reference.
     * This corresponds to the source code fragment of the short class name portion
     * (e.g., "Foo" of "org.acme.Foo.class")
     */
    public static final class TypeRef extends Tag {
        private final TypeElement type;

        public TypeRef(CompilationUnitTree unitTree, SourcePositions srcPos, Tree r, TypeElement type) {
            super(unitTree, srcPos, r);
            this.type = type;
        }

        public TypeRef(long sp, long ep, TypeElement type) {
            super(sp, ep);
            this.type = type;
        }

        public void collectSymbols(JavaScriptStreamWriter w) {
            w.collect(type);
        }

        void write(JavaScriptStreamWriter w) {
            assert firstChild.nextSibling==null; // there must be just one SourceText as a child (method name)
            w.beginMethod("T");
            w.ref(type);
            w.endMethod();
        }
    }

    /**
     * Field declaration.
     */
    public static final class FieldDecl extends Tag {
        private final VariableElement var;

        public FieldDecl(CompilationUnitTree cu, SourcePositions srcPos, VariableTree vt, VariableElement var) {
            super(cu,srcPos,vt);
            this.var = var;
        }

        public FieldDecl(LineMap lineMap, Token t, VariableElement var) {
            super(lineMap, t);
            this.var = var;
        }

        @Override
        void write(JavaScriptStreamWriter w) {
            writeChildren("F",w);
        }
    }

    /**
     * Field reference.
     * This corresponds to the source code fragment of the short field name portion
     * (e.g., "abc" of "x.abc.add(3)")
     */
    public static final class FieldRef extends Tag {
        private final VariableElement decl;

        public FieldRef(CompilationUnitTree unitTree, SourcePositions srcPos, Tree r, VariableElement decl) {
            super(unitTree, srcPos, r);
            this.decl = decl;
        }

        public FieldRef(long sp, long ep, VariableElement decl) {
            super(sp, ep);
            this.decl = decl;
        }

        public void collectSymbols(JavaScriptStreamWriter w) {
            w.collect(decl.getEnclosingElement());
        }

        void write(JavaScriptStreamWriter w) {
            assert firstChild.nextSibling==null; // there must be just one SourceText as a child (method name)
            w.beginMethod("G");
            w.ref((TypeElement)decl.getEnclosingElement());
            w.writeModifiers(decl);
            w.sep();
            w.string(decl.getSimpleName());
            w.endMethod();
        }
    }

    /**
     * Local variable/method parameter declaration.
     */
    public static final class LocalVarDecl extends Tag {
        private final VariableElement var;

        public LocalVarDecl(CompilationUnitTree cu, SourcePositions srcPos, VariableTree vt, VariableElement var) {
            super(cu,srcPos,vt);
            this.var = var;
        }

        public LocalVarDecl(LineMap lineMap, Token t, VariableElement var) {
            super(lineMap, t);
            this.var = var;
        }


        public void collectSymbols(JavaScriptStreamWriter w) {
            super.collectSymbols(w);
            w.collect(var);
        }

        @Override
        void write(JavaScriptStreamWriter w) {
            w.beginMethod("V");
            w.ref(var);
            writeChildren("$",w);
            w.endMethod();
        }
    }

    /**
     * Local variable reference.
     * This corresponds to the source code fragment of the local variable name.
     */
    public static final class LocalVarRef extends Tag {
        /**
         * The variable declaration being referenced.
         */
        private final VariableElement decl;

        public LocalVarRef(CompilationUnitTree unitTree, SourcePositions srcPos, Tree r, VariableElement decl) {
            super(unitTree, srcPos, r);
            this.decl = decl;
        }

        public LocalVarRef(long sp, long ep, VariableElement decl) {
            super(sp, ep);
            this.decl = decl;
        }

        void write(JavaScriptStreamWriter w) {
            assert firstChild.nextSibling==null; // there must be just one SourceText as a child (variable name)
            w.beginMethod("W");
            w.ref(decl);
            w.endMethod();
        }
    }

    /**
     * Method declaration.
     */
    public static final class MethodDecl extends Tag {
        private final Set<Match> overridden, overriding;
        private final ExecutableElement method;
        public MethodDecl(CompilationUnitTree unitTree, SourcePositions srcPos, MethodTree r, ExecutableElement e,
                          Set<Match> overridden, Set<Match> overriding) {
            super(unitTree, srcPos, r);
            this.method = e;
            this.overridden=overridden;
            this.overriding=overriding;
        }

        @Override
        public void collectSymbols(JavaScriptStreamWriter w) {
            w.collect(method);
            collectSet(w,overridden);
            collectSet(w,overriding);
            super.collectSymbols(w);
        }

        private void collectSet(JavaScriptStreamWriter w, Set<Match> ms) {
            for (Match m : ms)
                w.collect(m.method);
        }

        @Override
        void write(JavaScriptStreamWriter w) {
            w.beginMethod("M");
            w.ref(method);
            writeSet(w,overridden);
            writeSet(w,overriding);
            writeChildren("$",w);
            w.endMethod();
        }

        private void writeSet(JavaScriptStreamWriter w, Set<Match> ms) {
            w.beginArray();
            for (Match m : ms)
                w.ref(m.method);
            w.endArray();
        }
    }

    /**
     * Method invocation.
     * This corresponds to the source code fragment of the method name portion
     * (e.g., "bar" of "foo.bar(a,b,c)")
     */
    public static final class MethodRef extends Tag {
        private final ExecutableElement method;

        public MethodRef(long sp, long ep, ExecutableElement method) {
            super(sp,ep);
            this.method = method;
        }

        public void collectSymbols(JavaScriptStreamWriter w) {
            w.collect(method);
        }

        void write(JavaScriptStreamWriter w) {
            assert firstChild.nextSibling==null; // there must be just one SourceText as a child (method name)
            w.beginMethod("N");
            w.ref(method);
            w.endMethod();
        }
    }

    /**
     * Root of the tag tree that encompasses the whole compilation unit.
     */
    public static final class Root extends Tag {
        public Root(long length) {
            super(0,length);
        }

        void write(JavaScriptStreamWriter w) {
            writeChildren("classDef",w);
        }
    }
}
