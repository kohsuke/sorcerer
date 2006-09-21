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
import org.jvnet.sorcerer.ParsedType.Match;
import org.jvnet.sorcerer.impl.JavaLexer;
import org.jvnet.sorcerer.impl.JavaTokenTypes;
import org.jvnet.sorcerer.util.TreeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.DiagnosticListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private int tabWidth = 8;

    private LinkResolverFactory linkResolverFactory = new InternalLinkResolverFactory();

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
    public ParsedSourceSet(JavacTask javac) throws IOException {
        trees = Trees.instance(javac);
        elements = javac.getElements();
        types = javac.getTypes();
        srcPos = new SourcePositionsWrapper(trees.getSourcePositions());

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
     * Gets all the classes included in the analyzed source files.
     *
     * <p>
     * This includes interfaces, enums, and annotation types.
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
     * Sets the TAB width.
     *
     * Defaults to 8.
     */
    public void setTabWidth(int tabWidth) {
        this.tabWidth = tabWidth;
    }

    /**
     * Sets the {@link LinkResolverFactory} used for generating cross references.
     */
    public void setLinkResolverFactory(LinkResolverFactory f) {
        this.linkResolverFactory = f;
    }

    /**
     * Sets the {@link LinkResolverFactory}s used for generating cross references.
     *
     * <p>
     * Specified factories are consulted in the order they are given,
     * and the first one that produced a link will be used. Even when
     * you are adding extra resolvers, normally
     * you'd still want to add {@link InternalLinkResolverFactory} to the list
     * so that internal references between the generated files are produced.
     */
    public void setLinkResolverFactories(LinkResolverFactory... factories) {
        this.linkResolverFactory = new LinkResolverFacade(factories);
    }

    public LinkResolverFactory getLinkResolverFactory() {
        return linkResolverFactory;
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

        final LinkResolver linkResolver = linkResolverFactory.create(cu,this);

        // add lexical markers
        JavaLexer lexer = new JavaLexer(new StringReader(gen.sourceFile));
        lexer.setTabSize(tabWidth);
        try {
            while(true) {
                Token token = lexer.nextToken();
                int type = token.getType();
                if(type == JavaTokenTypes.EOF)
                    break;
                if(type == JavaTokenTypes.IDENT && ReservedWords.LIST.contains(token.getText()))
                    gen.add(new SpanMarker(lineMap,token,"rw"));
                if(type == JavaTokenTypes.ML_COMMENT
                || type == JavaTokenTypes.SL_COMMENT)
                    gen.add(new CommentMarker(lineMap,token));
            }
        } catch (TokenStreamException e) {
            // the analysis phase should have reported all the errors,
            // so we should ignore any failures at this point.
        }

        // then semantic ones
        new TreeScanner<Void,Void>() {
            private String buildId(Element e) {
                String buf = linkResolver.href(e);
                if(buf.length()==0)
                    return null; // no ID
                if(buf.charAt(0)!='#')
                    throw new IllegalStateException("Computed ID for "+e+" is "+buf);
                return buf.substring(1);
            }

            /**
             * primitive types like int, long, void, etc.
             */
            public Void visitPrimitiveType(PrimitiveTypeTree pt, Void _) {
                gen.add(new SpanMarker(cu,srcPos,pt,"pr"));
                return super.visitPrimitiveType(pt,_);
            }

            /**
             * literal string, int, etc. Null.
             */
            public Void visitLiteral(LiteralTree lit, Void _) {
                gen.add(new SpanMarker(cu,srcPos,lit,"lt"));
                return super.visitLiteral(lit, _);
            }

            /**
             * Definition of a variable, such as parameter, field, and local variables.
             */
            public Void visitVariable(VariableTree vt, Void _) {
                Element e = TreeUtil.getElement(vt);
                if(e!=null) {
                    if(e.getKind()!= ElementKind.ENUM_CONSTANT) {
                        // put the marker just on the variable name.
                        // the token for the variable name is after its type.
                        // note that we need to handle declarations like "int a,b".
                        Token t = gen.findTokenAfter(vt.getType(),vt.getName().toString());
                        if(t!=null) {
                            gen.add(new SpanMarker(lineMap,t,
                                getCssClass(e,"d"),buildId(e)));
                        }
                    } else {
                        // for the enum constant put the anchor around vt
                        gen.add(new SpanMarker(cu,srcPos,vt,getCssClass(e,"d"),buildId(e)));
                    }
                }
                return super.visitVariable(vt,_);
            }

            private void addBookmark(Tree t, Bookmark bookmark) {
                gen.add( lineMap.getLineNumber(srcPos.getStartPosition(cu,t)),bookmark);
            }

            /**
             * Method declaration.
             */
            public Void visitMethod(MethodTree mt, Void _) {
                ExecutableElement e = (ExecutableElement) TreeUtil.getElement(mt);
                if(e!=null) {
                    gen.add(new SpanMarker(cu,srcPos,mt,getCssClass(e,"d"),buildId(e)));

                    ParsedType pt = getParsedType((TypeElement) e.getEnclosingElement());
                    // put overridden bookmark
                    Set<Match> r = pt.findOverriddenMethods(elements, e);
                    if(!r.isEmpty()) {
                        addBookmark(mt,new OverriddenMethodsBookmark(r,linkResolver));
                    }
                    // ... and overriding bookmark
                    r = pt.findOverridingMethods(elements, e);
                    if(!r.isEmpty()) {
                        addBookmark(mt,new OverridingMethodsBookmark(r,linkResolver));
                    }
                }

                return super.visitMethod(mt, _);
            }

            /**
             * Class declaration.
             */
            public Void visitClass(ClassTree ct, Void _) {
                TypeElement e = (TypeElement) TreeUtil.getElement(ct);
                if(e!=null) {
                    gen.add(new SpanMarker(cu,srcPos,ct,getCssClass(e,"d"),buildId(e)));

                    // put subclass bookmark
                    List<ParsedType> descendants = getParsedType(e).descendants;
                    if(!descendants.isEmpty()) {
                        addBookmark(ct,new SubClassBookmark(descendants,linkResolver));
                    }

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
                        // add a marker for syntax coloring and jump to definition
                        gen.add(new LinkMarker(cu,srcPos,id, linkResolver.href(e),
                            getCssClass(e,"r")));
                    }
                }

                return super.visitIdentifier(id,_);
            }

            /**
             * "exp.token"
             */
            public Void visitMemberSelect(MemberSelectTree mst, Void _) {
                long ep = srcPos.getEndPosition(cu,mst);
                long sp = ep-mst.getIdentifier().length();

                // marker for the selected identifier
                Element e = TreeUtil.getElement(mst);
                if(e!=null) {
                    gen.add(new LinkMarker(sp,ep, linkResolver.href(e),
                        getCssClass(e,"r")));
                }
                // TODO: not exactly sure when it can be null

                return super.visitMemberSelect(mst, _);
            }

            public Void visitNewClass(NewClassTree nt, Void _) {
                long ep = srcPos.getEndPosition(cu, nt.getIdentifier());
                long sp = srcPos.getStartPosition(cu, nt.getIdentifier());

                // marker for jumping to the definition
                Element e = TreeUtil.getElement(nt);
                if(e!=null) {// be defensive
                    gen.add(new LinkMarker(sp,ep,linkResolver.href(e),getCssClass(e,"r")));
                }

                scan(nt.getEnclosingExpression());
                scan(nt.getArguments());
                scan(nt.getTypeArguments());
                scan(nt.getClassBody());

                return _;
            }



            /**
             * Method invocation of the form "exp.method()"
             */
            public Void visitMethodInvocation(MethodInvocationTree mi, Void _) {
                ExpressionTree ms = mi.getMethodSelect(); // PRIMARY.methodName portion
                Element e = TreeUtil.getElement(mi);
                if(e!=null) {
                    Name methodName = e.getSimpleName();
                    long ep = srcPos.getEndPosition(cu, ms);
                    if(ep>=0) {
                        // marker for the method name (and jump to definition)
                        gen.add(new LinkMarker(ep-methodName.length(),ep,linkResolver.href(e),
                            getCssClass(e,"r")));
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
            new TreePathScanner<String,Void>() {
                /**
                 * For "a" of "a.b.c"
                 */
                public String visitIdentifier(IdentifierTree id, Void _) {
                    String name = id.getName().toString();
                    PackageElement pe = elements.getPackageElement(name);

                    gen.add(new LinkMarker(cu,srcPos,id,linkResolver.href(pe),
                        getCssClass(pe,"r")));

                    return name;
                }

                public String visitMemberSelect(MemberSelectTree mst, Void _) {
                    String baseName = scan(mst.getExpression(),_);
                    String name = mst.getIdentifier().toString();
                    if(baseName.length()>0) name = baseName+'.'+name;

                    PackageElement pe = elements.getPackageElement(name);

                    long ep = srcPos.getEndPosition(cu,mst);
                    long sp = ep-mst.getIdentifier().length();

                    gen.add(new LinkMarker(sp,ep, linkResolver.href(pe),
                        getCssClass(pe,"r")));

                    return name;
                }
            }.scan(new TreePath(new TreePath(cu),packageName),null);
        }
    }

    protected String getCssClass(Element e) {
        return getCssClass(e,"");
    }
    protected String getCssClass(Element e, String seed) {
        StringBuilder buf = new StringBuilder(seed);

        // static marker
        Set<Modifier> mods = e.getModifiers();
        if(mods.contains(Modifier.STATIC)) {
            if(buf.length()>0)
                buf.append(' ');
            buf.append("st");
        }

        // deprecated marker
        if(elements.isDeprecated(e)) {
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
