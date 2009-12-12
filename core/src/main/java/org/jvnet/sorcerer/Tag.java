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
import java.lang.reflect.Method;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class Tag implements Iterable<Tag>, Comparable<Tag> {

    public final long sp,ep;

    public Tag firstChild;
    public Tag nextSibling;



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


    public void writeOut(TagVisitor visitor) {
        dynamicDispatch(visitor);
    }

    public void collectSymbols(TagVisitor visitor) {
        dynamicDispatch(visitor);
    }

    /*
     * We could make writeOut and collectSymbols abstract and force
     * all subclasses to override them, which causes a lot of boilerplate code
     * just to keep the compiler happy. Instead we put the generic behaviour
     * in the base class and risk a slight runtime performance hit.
     */
    private void dynamicDispatch(TagVisitor visitor) {
        try {
            Method method = visitor.getClass().getMethod("visit", this.getClass());
            method.invoke(visitor, this);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Reserved keywords like "class", "enum", including all primitive types.
     */
    public static final class ReservedWord extends Tag {
        public final String token;

        public ReservedWord(LineMap lineMap, Token t) {
            super(lineMap, t);
            this.token = t.getText();
        }

        @Override
        public void writeOut(TagVisitor visitor) {
            assert firstChild.nextSibling==null; // must be one SourceText.
            visitor.visit(this);
        }
    }

    /**
     * Comment in the source code
     */
    public static final class Comment extends Tag {
        public Comment(LineMap lineMap, Token t) {
            super(lineMap, t);
        }
    }

    /**
     * Curly bracket {...}.
     */
    public static final class CurlyBracket extends Tag {
        public CurlyBracket(long sp, long ep) {
            super(sp,ep);
        }
    }

    /**
     * Parenthesis (...).
     */
    public static final class Parenthesis extends Tag {
        public Parenthesis(long sp, long ep) {
            super(sp,ep);
        }
    }

    /**
     * Used to scrape off certain lexical tokens.
     */
    public static final class Killer extends Tag {
        public Killer(LineMap lineMap, Token t) {
            super(lineMap, t);
        }
    }

    /**
     * Created from consecutive text in the source file (not separated
     * by any other tags.)
     */
    public static final class SourceText extends Tag {
        public final String text;
        public SourceText(long sp, long ep, String text) {
            super(sp, ep);
            this.text=text;
        }

        @Override
        public void writeOut(TagVisitor visitor) {
            assert firstChild==null;
            visitor.visit(this);
        }
    }

    public static final class Literal extends Tag {
        public Literal(CompilationUnitTree unitTree, SourcePositions srcPos, Tree r) {
            super(unitTree, srcPos, r);
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
        public void writeOut(TagVisitor visitor) {
            assert firstChild.nextSibling==null; // there must be just one SourceText child
            visitor.visit(this);
        }
    }

    /**
     * Class declaration.
     */
    public static final class ClassDecl extends Tag {
        public final TypeElement type;
        public final List<ParsedType> descendants;

        public ClassDecl(CompilationUnitTree unitTree, SourcePositions srcPos, ClassTree ct, TypeElement e, List<ParsedType> descendants) {
            super(unitTree, srcPos, ct);
            this.type = e;
            this.descendants = descendants;
        }
    }

    /**
     * Type reference.
     * This corresponds to the source code fragment of the short class name portion
     * (e.g., "Foo" of "org.acme.Foo.class")
     */
    public static final class TypeRef extends Tag {
        public final TypeElement type;

        public TypeRef(CompilationUnitTree unitTree, SourcePositions srcPos, Tree r, TypeElement type) {
            super(unitTree, srcPos, r);
            this.type = type;
        }

        public TypeRef(long sp, long ep, TypeElement type) {
            super(sp, ep);
            this.type = type;
        }

        @Override
        public void writeOut(TagVisitor visitor) {
            assert firstChild.nextSibling==null; // there must be just one SourceText as a child (method name)
            visitor.visit(this);
        }
    }

    /**
     * Field declaration.
     */
    public static final class FieldDecl extends Tag {
        public FieldDecl(CompilationUnitTree cu, SourcePositions srcPos, VariableTree vt) {
            super(cu,srcPos,vt);
        }

        public FieldDecl(LineMap lineMap, Token t) {
            super(lineMap, t);
        }
    }

    /**
     * Field reference.
     * This corresponds to the source code fragment of the short field name portion
     * (e.g., "abc" of "x.abc.add(3)")
     */
    public static final class FieldRef extends Tag {
        public final VariableElement decl;

        public FieldRef(CompilationUnitTree unitTree, SourcePositions srcPos, Tree r, VariableElement decl) {
            super(unitTree, srcPos, r);
            this.decl = decl;
        }

        public FieldRef(long sp, long ep, VariableElement decl) {
            super(sp, ep);
            this.decl = decl;
        }

        @Override
        public void writeOut(TagVisitor visitor) {
            assert firstChild.nextSibling==null; // there must be just one SourceText as a child (method name)
            visitor.visit(this);
        }
    }

    /**
     * Local variable/method parameter declaration.
     */
    public static final class LocalVarDecl extends Tag {
        public final VariableElement var;

        public LocalVarDecl(CompilationUnitTree cu, SourcePositions srcPos, VariableTree vt, VariableElement var) {
            super(cu,srcPos,vt);
            this.var = var;
        }

        public LocalVarDecl(LineMap lineMap, Token t, VariableElement var) {
            super(lineMap, t);
            this.var = var;
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
        public final VariableElement decl;

        public LocalVarRef(CompilationUnitTree unitTree, SourcePositions srcPos, Tree r, VariableElement decl) {
            super(unitTree, srcPos, r);
            this.decl = decl;
        }

        public LocalVarRef(long sp, long ep, VariableElement decl) {
            super(sp, ep);
            this.decl = decl;
        }

        @Override
        public void writeOut(TagVisitor visitor) {
            assert firstChild.nextSibling==null; // there must be just one SourceText as a child (variable name)
            visitor.visit(this);
        }
    }

    /**
     * Method declaration.
     */
    public static final class MethodDecl extends Tag {
        public final Set<Match> overridden, overriding;
        public final ExecutableElement method;
        public MethodDecl(CompilationUnitTree unitTree, SourcePositions srcPos, MethodTree r, ExecutableElement e,
                          Set<Match> overridden, Set<Match> overriding) {
            super(unitTree, srcPos, r);
            this.method = e;
            this.overridden=overridden;
            this.overriding=overriding;
        }
    }

    /**
     * Method invocation.
     * This corresponds to the source code fragment of the method name portion
     * (e.g., "bar" of "foo.bar(a,b,c)")
     */
    public static final class MethodRef extends Tag {
        public final ExecutableElement method;

        public MethodRef(long sp, long ep, ExecutableElement method) {
            super(sp,ep);
            this.method = method;
        }

        @Override
        public void writeOut(TagVisitor visitor) {
            assert firstChild.nextSibling==null; // there must be just one SourceText as a child (method name)
            visitor.visit(this);
        }
    }

    /**
     * Root of the tag tree that encompasses the whole compilation unit.
     */
    public static final class Root extends Tag {
        public Root(long length) {
            super(0,length);
        }
    }
}
