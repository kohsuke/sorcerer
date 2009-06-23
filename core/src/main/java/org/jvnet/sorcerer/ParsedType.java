package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import org.jvnet.sorcerer.util.TreeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * {@link TypeElement} with information for cross-referencing.
 *
 * <p>
 * As {@link ClosedHashMultiMap}, it remembers all the methods keyed by their names.
 * Used for override searches.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ParsedType extends ClosedHashMultiMap<Name,ExecutableElement> {
    /**
     * This {@link ParsedType} represents the parsed information of this {@link TypeElement}.
     */
    public final TypeElement element;

    public final ParsedType superClass;
    public final ParsedType[] interfaces;

    /**
     * All direct descendants of this type.
     */
    public final List<ParsedType> descendants = new ArrayList<ParsedType>();

    /**
     * {@link CompilationUnitTree}s that are using fields/methods/classes etc
     * in this type. This is useful index for narrowing down search space
     * for "find usage".
     *
     * <p>
     * Set by the {@link ParsedSourceSet}.
     */
    CompilationUnitTree[] referers = EMPTY_CUT_ARRAY;

    public ParsedType(ParsedSourceSet pss, TypeElement element) {
        pss.parsedTypes.put(element,this);
        this.element = element;
/*
        // let javac completes the symbol, which may discover errors and render this symbol invalid,
        // but javac will be able to recover from that error in the 2nd go.
        try {
            element.getSuperclass();
        } catch (CompletionFailure e) {
            LOGGER.log(Level.FINE, "Symbol completion failed", e);
        }
*/
        TypeMirror sc = element.getSuperclass();
        switch(sc.getKind()) {
        case NONE:
        case ERROR:
            this.superClass = null;
            break;
        case DECLARED:
            this.superClass = pss.getParsedType((TypeElement)((DeclaredType)sc).asElement());
            this.superClass.descendants.add(this);
            break;
        default:
            throw new IllegalStateException("invalid super class: "+sc.toString());
        }

        List<? extends TypeMirror> intf = element.getInterfaces();
        if(intf.isEmpty()) {
            interfaces = EMPTY_ARRAY;
        } else {
            interfaces = new ParsedType[intf.size()];
            int i=0;
            for (TypeMirror m : intf) {
                ParsedType pt = pss.getParsedType((TypeElement) ((DeclaredType) m).asElement());
                pt.descendants.add(this);
                interfaces[i++] = pt;
            }
        }

        putAll(ElementFilter.methodsIn(pss.getElements().getAllMembers(element)));
    }

    /**
     * Gets the package local name of this type.
     *
     * <p>
     * It's a simple name if the type is a package-member type,
     * but for inner types, this name includes simple names of the
     * outer types.
     */
    public String getPackageLocalName() {
        if(element.getEnclosingElement().getKind()==ElementKind.PACKAGE)
            return element.getSimpleName().toString();  // common case

        return buildPackageLocalName(element,new StringBuilder()).toString();
    }

    private static StringBuilder buildPackageLocalName(Element e,StringBuilder buf) {
        Element p = e.getEnclosingElement();
        if(p.getKind()!=ElementKind.PACKAGE)
             buildPackageLocalName(p,buf);
        if(buf.length()>0)  buf.append('.');
        buf.append(e.getSimpleName());
        return buf;
    }

    public boolean isLocal() {
        return TreeUtil.isLocal(element);
    }

    /**
     * Returns true this type represents an error type.
     *
     * <p>
     * javac creates {@link ElementKind#CLASS} with class names like &lt;any> in it
     * to represent an error. So make sure that the token is a valid Java identifier.
     */
    public boolean isInvalid() {
        String s = element.getQualifiedName().toString();
        for( int i=s.length()-1; i>=0; i-- ) {
            char ch=s.charAt(i);
            if(!Character.isJavaIdentifierPart(ch) && ch!='.')
                return true;
        }
        return false;
    }

    /**
     * All the matches discovered.
     */
    private static final class QueryResult {
        /**
         * {@link ParsedType}s that are visited already.
         */
        private final Set<ParsedType> visited = new HashSet<ParsedType>();

        private final Set<Match> result = new HashSet<Match>();

        private final Elements elements;
        private final TypeElement owner;
        private final ExecutableElement method;

        public QueryResult(Elements elements, ExecutableElement method) {
            this.elements = elements;
            this.owner = (TypeElement)method.getEnclosingElement();
            this.method = method;
        }

        /**
         * Checks the given {@link ParsedType} for methods overridden by {@link #method}.
         */
        private void scanOverridden(ParsedType t) {
            if(!visited.add(t))
                return; // already scanned

            OUTER:
            for (ExecutableElement m : t.get(t.getKey(method))) {
                if(elements.overrides(method,m,owner)) {
                    // make sure we don't already have overriding method of this in the results
                    Iterator<Match> itr = result.iterator();
                    while(itr.hasNext()) {
                        Match r = itr.next();
                        if(elements.overrides(r.method,m,owner))
                            continue OUTER; // we have a better match in the result already. ignore this.
                        if(elements.overrides(m,r.method,owner))
                            itr.remove();   // this result supercedes the one in the collection
                    }
                    result.add(new Match(t,m));
                }
            }

            // recursively visit super classes and super interfaces
            scanAncestors(t);
        }

        /**
         * Scans ancestors with {@link #scanOverridden(ParsedType)}.
         */
        void scanAncestors(ParsedType t) {
            if(t.superClass!=null)
                scanOverridden(t.superClass);
            for (ParsedType i : t.interfaces)
                scanOverridden(i);
        }

        /**
         * Checks the given {@link ParsedType} for methods overriding {@link #method}.
         */
        private void scanOverriding(ParsedType t) {
            if(!visited.add(t))
                return; // already scanned

            OUTER:
            for (ExecutableElement m : t.get(t.getKey(method))) {
                if(elements.overrides(m,method,t.element)) {
                    // make m is not indirect overrider
                    Iterator<Match> itr = result.iterator();
                    while(itr.hasNext()) {
                        Match r = itr.next();
                        if(elements.overrides(m,r.method,t.element))
                            continue OUTER; // m is indirect
                        if(elements.overrides(r.method,m,owner))
                            itr.remove();   // r.method is indirect
                    }
                    result.add(new Match(t,m));
                }
            }

            // recursively visit descendants
            scanDescendants(t);
        }

        /**
         * Scans descendants with {@link #scanOverriding(ParsedType)}.
         */
        void scanDescendants(ParsedType t) {
            for (ParsedType i : t.descendants)
                scanOverriding(i);
        }
    }

    /**
     * Represents a found method.
     */
    public static final class Match {
        /**
         * {@link ParsedType} that contains {@link #method}.
         */
        public final ParsedType owner;
        /**
         * Method overriden by the queried method (for {@link ParsedType#findOverriddenMethods(Elements, ExecutableElement)})
         * or methods overriding the queried method (for  {@link ParsedType#findOverridingMethods(Elements, ExecutableElement)})
         */
        public final ExecutableElement method;

        public Match(ParsedType owner, ExecutableElement method) {
            this.owner = owner;
            this.method = method;
        }

        public boolean equals(Object o) { // method is enough for uniqueness
            final Match that = (Match) o;
            return method.equals(that.method);
        }

        public int hashCode() {
            return method.hashCode();
        }
    }

    /**
     * Finds methods in the ancestors of this {@link ParsedType}
     * that the given {@link ExecutableElement} is overriding,
     * and return them all.
     *
     * @param elements
     *      Needed utility object. This value is taken as a parameter to
     *      avoid storing it in every {@link ParsedType}.
     * @param e
     *      The method on this {@link ParsedType} for which the override methods
     *      will be searched.
     * @return
     *      can be empty but never null.
     */
    public Set<Match> findOverriddenMethods(Elements elements, ExecutableElement e) {
        QueryResult r = new QueryResult(elements, e);
        r.scanAncestors(this);
        return r.result;
    }

    /**
     * Finds methods in the descendants of this {@link ParsedType}
     * that overrides the given {@link ExecutableElement},
     * and return them all.
     *
     * @param elements
     *      Needed utility object. This value is taken as a parameter to
     *      avoid storing it in every {@link ParsedType}.
     * @param e
     *      The method on this {@link ParsedType} for which the override methods
     *      will be searched.
     * @return
     *      can be empty but never null.
     */
    public Set<Match> findOverridingMethods(Elements elements, ExecutableElement e) {
        QueryResult r = new QueryResult(elements, e);
        r.scanDescendants(this);
        return r.result;
    }

    /**
     * Gets the list of {@link CompilationUnitTree}s that reference something
     * in this type (field, method, constructor, constant.)
     *
     * @return
     *      always non-null. read-only.
     */
    public CompilationUnitTree[] getReferers() {
        return referers;
    }

    /**
     * Gets the actual {@link Tree} nodes that refer to {@link Element}s
     * (methods, fields, constructors, constants, and the type itself,
     * but excluding nested types) inside this type.
     *
     * <p>
     * This involves a non-trivial computation, but it does use
     * index so it's faster than scanning the whole source tree.
     */
    public Map<Element,Set<TreePath>> findReferers(SourcePositions pos) {
        return RefererFinder.find(this,pos);
    }


    // methods are keyed by their names.
    @Override
    protected Name getKey(ExecutableElement value) {
        return value.getSimpleName();
    }

    private static final ParsedType[] EMPTY_ARRAY = new ParsedType[0];
    private static final CompilationUnitTree[] EMPTY_CUT_ARRAY = new CompilationUnitTree[0];

    private static final Logger LOGGER = Logger.getLogger(ParsedType.class.getName());
}
