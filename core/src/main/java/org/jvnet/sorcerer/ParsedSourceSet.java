package org.jvnet.sorcerer;

import antlr.Token;
import antlr.TokenStreamException;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import org.jvnet.sorcerer.Tag.DeclName;
import org.jvnet.sorcerer.impl.JavaLexer;
import org.jvnet.sorcerer.impl.JavaTokenTypes;
import org.jvnet.sorcerer.util.CharSequenceReader;
import org.jvnet.sorcerer.util.TreeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.DiagnosticListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Represents set of analyzed source code.
 *
 * <p>
 * This object retains the entire parse trees of the source files
 * and type information, as well as enough indexes among them
 * to make page generation fast enough. There's always a tricky trade-off
 * between how much index you retain in memory vs how much you compute
 * dynamically.
 *
 * <p>
 * Instances of this class can be safely accessed from multiple threads
 * concurrently.
 *
 * <p>
 * Normally, {@link Analyzer} is used to create this object, but
 * advanced clients may invoke the constructor with a properly
 * configured {@link JavacTask}.
 *
 * @see Analyzer#analyze
 * @author Kohsuke Kawaguchi
 */
public class ParsedSourceSet {

    // javac related objects
    private final Trees trees;
    private final SourcePositions srcPos;
    private final Elements elements;
    private final Types types;

    private final List<CompilationUnitTree> compilationUnits = new ArrayList<CompilationUnitTree>();

    /**
     * path to {@link ClassTree} keyed by its full-qualified class name.
     */
    private final Map<String,TreePath> classes = new TreeMap<String,TreePath>();

    private final Set<PackageElement> packages = new TreeSet<PackageElement>(PACKAGENAME_COMPARATOR);

    private final List<Dependency> dependencies = new ArrayList<Dependency>();

    private final int tabWidth;

    /**
     * {@link ParsedType}s keyed by its {@link ParsedType#element}.
     */
    /*package*/ final Map<TypeElement,ParsedType> parsedTypes = new HashMap<TypeElement,ParsedType>();

    /**
     * {@link ClassTree}s in the compilation unit to their {@link TreePath}.
     */
    /*package*/ final Map<ClassTree,TreePath> treePathByClass = new HashMap<ClassTree,TreePath>();

    /**
     * Runs <tt>javac</tt> and analyzes the result.
     *
     * <p>
     * Any error found during the analysis will be reported to
     * {@link DiagnosticListener} installed on {@link JavacTask}.
     */
    public ParsedSourceSet(JavacTask javac, int tabWidth) throws IOException {
        trees = Trees.instance(javac);
        elements = javac.getElements();
        types = javac.getTypes();
        srcPos = new SourcePositionsWrapper(trees.getSourcePositions());
        this.tabWidth = tabWidth;

        Iterable<? extends CompilationUnitTree> parsed = javac.parse();
        javac.analyze();

        // used to list up all analyzed classes
        TreePathScanner<?,?> classScanner = new TreePathScanner<Void,Void>() {
            public Void visitClass(ClassTree ct, Void _) {
                TreePath path = getCurrentPath();
                treePathByClass.put(ct,path);
                TypeElement e = (TypeElement) trees.getElement(path);
                if(e!=null) {
                    classes.put(e.getQualifiedName().toString(), path);

                    // make sure we have descendants tree built for all compilation units
                    getParsedType(e);

                    // remember packages that have compilation units in it
                    Element p = e.getEnclosingElement();
                    if(p.getKind()==ElementKind.PACKAGE)
                        packages.add((PackageElement) p);
                }

                return super.visitClass(ct, _);
            }
        };

        for( CompilationUnitTree u : parsed ) {
            compilationUnits.add(u);
            classScanner.scan(u,null);
        }

        // build up index for find usage.
        for( Map.Entry<TypeElement,Set<CompilationUnitTree>> e : ClassReferenceBuilder.build(compilationUnits).entrySet() )
            getParsedType(e.getKey()).referers = e.getValue().toArray(new CompilationUnitTree[e.getValue().size()]);
    }

    /**
     * Gets all the {@link CompilationUnitTree}s that are analyzed.
     *
     * @return
     *      can be empty but never null.
     */
    public List<CompilationUnitTree> getCompilationUnits() {
        return Collections.unmodifiableList(compilationUnits);
    }

    /**
     * Gets all the {@link TreePath}s to {@link ClassTree}s included
     * in the analyzed source files.
     *
     * @return
     *      can be empty but never null.
     */
    public Collection<TreePath> getClasses() {
        return Collections.unmodifiableCollection(classes.values());
    }

    /**
     * All the {@link ClassTree}s to their {@link TreePath}s.
     */
    public Map<ClassTree,TreePath> getTreePathByClass() {
        return treePathByClass;
    }

    /**
     * Gets the javadoc/sorcerer locations of dependencies.
     */
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void addDependency(Dependency d) {
        dependencies.add(d);
    }

    /**
     * Gets all the classes included in the analyzed source files.
     *
     * <p>
     * This includes interfaces, enums, and annotation types.
     * It also includes types that are defined elsewhere (that are referenced by
     * compiled source code.)
     *
     * @return
     *      can be empty but never null.
     */
    public Collection<TypeElement> getClassElements() {
        return Collections.unmodifiableCollection(parsedTypes.keySet());
    }

    /**
     * Gets all the classes in the given package.
     */
    public Collection<TypeElement> getClassElements(PackageElement pkg) {
        Set<TypeElement> r = new TreeSet<TypeElement>(TYPE_COMPARATOR);
        for (TypeElement e : parsedTypes.keySet()) {
            Element p = e.getEnclosingElement();
            if(p.equals(pkg))
                r.add(e);
        }
        return r;
    }

    /**
     * Gets all the packages of the analyzed source files.
     *
     * <p>
     * This does not include those packages that are just referenced.
     *
     * @return
     *      can be empty but never null.
     */
    public Collection<PackageElement> getPackageElement() {
        return Collections.unmodifiableCollection(packages);
    }

    /**
     * Gets the list of all fully-qualified class names in the analyzed source files.
     *
     * @return
     *      can be empty but never null.
     */
    public Set<String> getClassNames() {
        return Collections.unmodifiableSet(classes.keySet());
    }

    /**
     * Gets the {@link TreePath} by its fully qualified class name.
     */
    public TreePath getClassTreePath(String fullyQualifiedClassName) {
        return classes.get(fullyQualifiedClassName);
    }

    /**
     * Gets the {@link Trees} object that lets you navigate around the tree model.
     *
     * @return
     *      always non-null, same object.
     */
    public Trees getTrees() {
        return trees;
    }

    /**
     * Gets the {@link SourcePositions} object that lets you find the location of objects.
     *
     * @return
     *      always non-null, same object.
     */
    public SourcePositions getSourcePositions() {
        return srcPos;
    }

    /**
     * Gets the {@link Elements} object that lets you navigate around {@link Element}s.
     *
     * @return
     *      always non-null, same object.
     */
    public Elements getElements() {
        return elements;
    }

    /**
     * Gets the {@link Types} object that lets you navigate around {@link TypeMirror}s.
     *
     * @return
     *      always non-null, same object.
     */
    public Types getTypes() {
        return types;
    }

    /**
     * Gets the current TAB width.
     */
    public int getTabWidth() {
        return tabWidth;
    }

    /**
     * Gets or creates a {@link ParsedType} for the given {@link TypeElement}.
     */
    public ParsedType getParsedType(TypeElement e) {
        ParsedType v = parsedTypes.get(e);
        if(v==null)
            return new ParsedType(this,e);   // the constructor will register itself to the map
        else
            return v;
    }

    /**
     * Gets all the {@link ParsedType}s.
     */
    public Collection<ParsedType> getParsedTypes() {
        return parsedTypes.values();
    }

    /**
     * Invoked by {@link HtmlGenerator}'s constructor to complete the initialization.
     * <p>
     * This is where the actual annotation of the source code happens.
     */
    protected void configure(final CompilationUnitTree cu, final HtmlGenerator gen) throws IOException {
        final LineMap lineMap = cu.getLineMap();

        // add lexical markers
        JavaLexer lexer = new JavaLexer(new CharSequenceReader(gen.sourceFile));
        lexer.setTabSize(tabWidth);
        try {
            Stack<Long> openBraces = new Stack<Long>();

            while(true) {
                Token token = lexer.nextToken();
                int type = token.getType();
                if(type == JavaTokenTypes.EOF)
                    break;
                if(type == JavaTokenTypes.IDENT && ReservedWords.LIST.contains(token.getText()))
                    gen.add(new Tag.ReservedWord(lineMap,token));
                if(type == JavaTokenTypes.ML_COMMENT
                || type == JavaTokenTypes.SL_COMMENT)
                    gen.add(new Tag.Comment(lineMap,token));
                if(type == JavaTokenTypes.LCURLY || type==JavaTokenTypes.LPAREN) {
                    openBraces.push(getPosition(lineMap,token));
                    gen.add(new Tag.Killer(lineMap,token)); // CurlyBracket tag yields '{'. so kill this off.
                }
                if(type == JavaTokenTypes.RCURLY) {
                    long sp = openBraces.pop();
                    gen.add(new Tag.CurlyBracket(sp,getPosition(lineMap,token)+1));
                    gen.add(new Tag.Killer(lineMap,token));
                }
                if(type == JavaTokenTypes.RPAREN) {
                    long sp = openBraces.pop();
                    gen.add(new Tag.Parenthesis(sp,getPosition(lineMap,token)+1));
                    gen.add(new Tag.Killer(lineMap,token));
                }
            }
        } catch (TokenStreamException e) {
            // the analysis phase should have reported all the errors,
            // so we should ignore any failures at this point.
        }

        final Name CLASS = elements.getName("class");

        // then semantic ones
        new TreeScanner<Void,Void>() {
            /**
             * primitive types like int, long, void, etc.
             */
            public Void visitPrimitiveType(PrimitiveTypeTree pt, Void _) {
                // all primitives should be marked up by lexer
                // gen.add(new Tag.PrimitiveType(cu,srcPos,pt));
                return super.visitPrimitiveType(pt,_);
            }

            /**
             * literal string, int, etc. Null.
             */
            public Void visitLiteral(LiteralTree lit, Void _) {
                gen.add(new Tag.Literal(cu,srcPos,lit));
                return super.visitLiteral(lit, _);
            }

            /**
             * Definition of a variable, such as parameter, field, and local variables.
             */
            public Void visitVariable(VariableTree vt, Void _) {
                VariableElement e = (VariableElement) TreeUtil.getElement(vt);
                if(e!=null) {
                    switch (e.getKind()) {
                    case ENUM_CONSTANT:
                    case FIELD:
                        gen.add(new Tag.FieldDecl(cu,srcPos,vt,e));
                        break;
                    case EXCEPTION_PARAMETER:
                    case LOCAL_VARIABLE:
                    case PARAMETER:
                        gen.add(new Tag.LocalVarDecl(cu,srcPos,vt,e));
                        break;
                    }

                    Token token;
                    if(e.getKind()!= ElementKind.ENUM_CONSTANT) {
                        // put the marker just on the variable name.
                        // the token for the variable name is after its type.
                        // note that we need to handle declarations like "int a,b".
                        token = gen.findTokenAfter(vt.getType(), true, vt.getName().toString());
                    } else {
                        // for the enum constant put the anchor around vt
                        token = gen.findTokenAfter(vt, false, vt.getName().toString());
                    }
                    gen.add(new Tag.DeclName(lineMap,token));
                }
                return super.visitVariable(vt,_);
            }

            /**
             * Method declaration.
             */
            public Void visitMethod(MethodTree mt, Void _) {
                ExecutableElement e = (ExecutableElement) TreeUtil.getElement(mt);
                if(e!=null) {
                    // mark up the method name
                    Tree prev = mt.getReturnType();
                    String name = mt.getName().toString();
                    Token token;
                    if(prev!=null)
                        token = gen.findTokenAfter(prev, true, name);
                    else
                        token = gen.findTokenAfter(mt,false,name);

                    if(token!=null)
                        gen.add(new Tag.DeclName(lineMap,token));

                    ParsedType pt = getParsedType((TypeElement) e.getEnclosingElement());

                    gen.add(new Tag.MethodDecl(cu, srcPos, mt, e,
                        pt.findOverriddenMethods(elements, e),
                        pt.findOverridingMethods(elements, e)
                        ));
                }

                return super.visitMethod(mt, _);
            }

            /**
             * Class declaration.
             */
            public Void visitClass(ClassTree ct, Void _) {
                TypeElement e = (TypeElement) TreeUtil.getElement(ct);
                if(e!=null) {
                    // put the marker on the class name portion.
                    Token token;
                    if(ct.getModifiers()!=null)
                        token = gen.findTokenAfter(ct.getModifiers(), true, ct.getSimpleName().toString());
                    else
                        token = gen.findTokenAfter(ct, false, ct.getSimpleName().toString());
                    if(token!=null)
                        gen.add(new DeclName(lineMap, token));

                    List<ParsedType> descendants = getParsedType(e).descendants;

                    gen.add(new Tag.ClassDecl(cu, srcPos, ct, e, descendants));

                    if(e.getNestingKind()== NestingKind.ANONYMOUS) {
                        // don't visit the extends and implements clause as
                        // they already show up in the NewClassTree
                        scan(ct.getMembers());
                        return _;
                    }
                }
                return super.visitClass(ct, _);
            }

            /**
             * All the symbols found in the source code.
             */
            public Void visitIdentifier(IdentifierTree id, Void _) {
                if(!ReservedWords.LIST.contains(id.getName().toString())) {
                    Element e = TreeUtil.getElement(id);
                    if(e!=null) {
                        switch (e.getKind()) {
                        case ANNOTATION_TYPE:
                        case CLASS:
                        case ENUM:
                        case INTERFACE:
                            gen.add(new Tag.TypeRef(cu,srcPos,id,(TypeElement)e));
                            break;
                        case FIELD: // such as objects imported by static import.
                        case ENUM_CONSTANT:
                            gen.add(new Tag.FieldRef(cu,srcPos,id,(VariableElement)e));
                            break;
                        case PARAMETER:
                        case EXCEPTION_PARAMETER:
                        case LOCAL_VARIABLE:
                            gen.add(new Tag.LocalVarRef(cu,srcPos,id,(VariableElement)e));
                            break;
                        }
                    }
                }

                return super.visitIdentifier(id,_);
            }

            /**
             * "exp.token"
             */
            public Void visitMemberSelect(MemberSelectTree mst, Void _) {
                // avoid marking 'Foo.class' as static reference
                if(!mst.getIdentifier().equals(CLASS)) {
                    // just select the 'token' portion
                    long ep = srcPos.getEndPosition(cu,mst);
                    long sp = ep-mst.getIdentifier().length();

                    // marker for the selected identifier
                    Element e = TreeUtil.getElement(mst);
                    if(e!=null) {
                        switch(e.getKind()) {
                        case FIELD:
                        case ENUM_CONSTANT:
                            gen.add(new Tag.FieldRef(sp,ep,(VariableElement)e));
                            break;

                        // these show up in the import statement
                        case ANNOTATION_TYPE:
                        case CLASS:
                        case ENUM:
                        case INTERFACE:
                            gen.add(new Tag.TypeRef(sp,ep,(TypeElement)e));
                            break;
                        }
                    }
                }

                return super.visitMemberSelect(mst, _);
            }

            /**
             * Constructor invocation.
             */
            public Void visitNewClass(NewClassTree nt, Void _) {
                long ep = srcPos.getEndPosition(cu, nt.getIdentifier());
                long sp = srcPos.getStartPosition(cu, nt.getIdentifier());

                // marker for jumping to the definition
                Element e = TreeUtil.getElement(nt);
                if(e!=null) {
                    gen.add(new Tag.MethodRef(sp,ep,(ExecutableElement)e));
                }

                scan(nt.getEnclosingExpression());
                scan(nt.getArguments());
                scan(nt.getTypeArguments());
                scan(nt.getClassBody());

                return _;
            }


            // TODO: how is the 'super' or 'this' constructor invocation handled?

            /**
             * Method invocation of the form "exp.method()"
             */
            public Void visitMethodInvocation(MethodInvocationTree mi, Void _) {
                ExpressionTree ms = mi.getMethodSelect(); // PRIMARY.methodName portion
                ExecutableElement e = (ExecutableElement) TreeUtil.getElement(mi);
                if(e!=null) {
                    Name methodName = e.getSimpleName();
                    long ep = srcPos.getEndPosition(cu, ms);
                    if(ep>=0) {
                        // marker for the method name (and jump to definition)
                        gen.add(new Tag.MethodRef(ep-methodName.length(),ep,e));
                    }
                }

                return super.visitMethodInvocation(mi,_);
            }

            // recursively scan trees
            private void scan(List<? extends Tree> list) {
                for (Tree t : list)
                    scan(t);
            }
            private void scan(Tree t) {
                scan(t,null);
            }
        }.scan(cu,null);

        // compilationUnit -> package name consists of member select trees
        // but it fails to create an element, so do this manually
        ExpressionTree packageName = cu.getPackageName();
        if(packageName!=null) {
            new TreeScanner<String,Void>() {
                /**
                 * For "a" of "a.b.c"
                 */
                public String visitIdentifier(IdentifierTree id, Void _) {
                    String name = id.getName().toString();
                    PackageElement pe = elements.getPackageElement(name);
                    // addRef(id,pe);
                    return name;
                }

                public String visitMemberSelect(MemberSelectTree mst, Void _) {
                    String baseName = scan(mst.getExpression(),_);
                    String name = mst.getIdentifier().toString();
                    if(baseName.length()>0) name = baseName+'.'+name;

                    PackageElement pe = elements.getPackageElement(name);

                    long ep = srcPos.getEndPosition(cu,mst);
                    long sp = ep-mst.getIdentifier().length();

                    // addRef(sp,ep,pe);
                    return name;
                }
            }.scan(packageName,null);
        }
    }

    private long getPosition(LineMap lineMap, Token token) {
        return lineMap.getPosition(token.getLine(),token.getColumn());
    }

    public static final Comparator<PackageElement> PACKAGENAME_COMPARATOR = new Comparator<PackageElement>() {
        public int compare(PackageElement lhs, PackageElement rhs) {
            return lhs.getQualifiedName().toString().compareTo(rhs.getQualifiedName().toString());
        }
    };

    public static final Comparator<Element> SIMPLENAME_COMPARATOR = new Comparator<Element>() {
        public int compare(Element lhs, Element rhs) {
            return lhs.getSimpleName().toString().compareTo(rhs.getSimpleName().toString());
        }
    };

    private static final Comparator<TypeElement> TYPE_COMPARATOR = new Comparator<TypeElement>() {
        public int compare(TypeElement lhs, TypeElement rhs) {
            return lhs.getQualifiedName().toString().compareTo(rhs.getQualifiedName().toString());
        }
    };
}
