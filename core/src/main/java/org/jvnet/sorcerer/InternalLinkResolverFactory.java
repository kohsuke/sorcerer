package org.jvnet.sorcerer;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import org.jvnet.sorcerer.util.TreeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.AbstractElementVisitor6;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.HashSet;
import java.util.Set;

/**
 * Generates links within the generated documents.
 *
 * TODO: handle multiple declarations in the same compilation unit.
 * That is, when we see a reference to a type "org/acme/Foo", we somehow
 * need to know that this is defined in org/acme/Bar.html#~Foo" or something.
 * Such information is generally unavailable.
 *
 * @author Kohsuke Kawaguchi
 */
public final class InternalLinkResolverFactory implements LinkResolverFactory {
    public LinkResolver create(CompilationUnitTree currentCompilationUnit, ParsedSourceSet sources) {
        return new InternalLinkResolver(currentCompilationUnit, sources);
    }

    public LinkResolver create(PackageElement pkg, ParsedSourceSet sources) {
        return new InternalLinkResolver(pkg, sources);
    }

    static final class InternalLinkResolver extends AbstractElementVisitor6<StringBuilder,Void> implements LinkResolver {

        /**
         * The 'primary public type in the current compilation unit.
         * This is the class that has the same name as the file name.
         * It can be null.
         */
        private final TypeElement primary;

        /**
         * Other types defined in the current compilation unit
         * as a package-member class. These are all package private.
         */
        private final Set<TypeElement> privateTypes = new HashSet<TypeElement>();

        /**
         * The package in which the current compilation unit is in. Tokenized.
         * e.g., {"org","acme","foo"}. Never null.
         */
        private final String[] pkg;

        private final ParsedSourceSet pss;
        private final Trees trees;
        private final Types types;
        private final Elements elements;

        public InternalLinkResolver(CompilationUnitTree compUnit, ParsedSourceSet pss) {
            this.pss = pss;
            this.trees = pss.getTrees();
            this.elements = pss.getElements();
            this.types = pss.getTypes();

            TypeElement primary = null;

            TreePath cutp = new TreePath(compUnit);

            for (Tree t : compUnit.getTypeDecls()) {
                if (t instanceof ClassTree) {
                    ClassTree ct = (ClassTree) t;
                    TypeElement e = (TypeElement) trees.getElement(new TreePath(cutp, ct));

                    if(ct.getModifiers().getFlags().contains(Modifier.PUBLIC))
                        primary = e;
                    else
                        privateTypes.add(e);
                }
            }

            if(primary==null && privateTypes.size()==1) {
                // promote this private type as the primary, since there's no ambiguity
                primary = privateTypes.iterator().next();
                privateTypes.clear();
            }

            this.primary = primary;
            this.pkg = TreeUtil.getPackageName(compUnit).split("\\.");
        }

        public InternalLinkResolver(PackageElement pkg, ParsedSourceSet pss) {
            this.pss = pss;
            this.trees = pss.getTrees();
            this.elements = pss.getElements();
            this.types = pss.getTypes();
            this.primary = null;
            this.pkg = pkg.getQualifiedName().toString().split("\\.");
        }

        public String href(Element e) {
            StringBuilder buf = visit(e);
            if(buf==null)   return null;
            return buf.toString();
        }

        ///**
        // * List up all the ancestor {@link PackageElement}s in order "java/util/..." order.
        // */
        //private List<PackageElement> getPackageList(Element current) {
        //    List<PackageElement> pkgs = new ArrayList<PackageElement>();
        //    Element o = current;
        //    while(true) {
        //        o = o.getEnclosingElement();
        //        if(o==null || o.getKind()!= ElementKind.PACKAGE)
        //            break;
        //        pkgs.add((PackageElement)o);
        //    }
        //    Collections.reverse(pkgs);
        //    return pkgs;
        //}

        public StringBuilder visitType(TypeElement t, Void _) {
            if(t==primary)
                return new StringBuilder(); // empty name.

            if(trees.getTree(t)==null)
                return null;    // not a part of compiled source files

            switch(t.getNestingKind()) {
            case ANONYMOUS:
                String binaryName = elements.getBinaryName(t).toString();
                int idx = binaryName.lastIndexOf('$');
                String name = '~'+binaryName.substring(idx); // #$1 is ambiguous between field and anonyous type, so use '~' as the prefix for type
                return combine(getEnclosingTypeOrPackage(t).accept(this,null)).append(name);
            case TOP_LEVEL:
                // TODO: does this handle package-local class defined in a file name different from the class name?
                // probably not.
                return recurse(t).append(t.getSimpleName()).append(".html");

            case MEMBER:
            case LOCAL:
                return recurse(t).append('~').append(t.getSimpleName());

            default:
                throw new IllegalStateException(t.getNestingKind().toString());
            }
        }

        private Element getEnclosingTypeOrPackage(Element e) {
            while(true) {
                e = e.getEnclosingElement();
                if (e instanceof TypeElement || e instanceof PackageElement) {
                    return e;
                }
            }
        }

        public StringBuilder visitVariable(VariableElement v, Void _) {
            StringBuilder buf = recurse(v);
            if(buf==null)   return null;
            return buf.append(v.getSimpleName());
        }

        public StringBuilder visitExecutable(ExecutableElement e, Void _) {
            StringBuilder buf = recurse(e);
            if(buf==null)   return null;

            switch(e.getKind()) {
            case METHOD:
            case CONSTRUCTOR:
                return TreeUtil.buildMethodName(buf,types,e);
            default:
                // static/instance initializers.
                return buf.append(getSiblingIndex(e));
            }
        }

        public StringBuilder visitTypeParameter(TypeParameterElement v, Void _) {
            return recurse(v).append(v.getSimpleName());
        }

        public StringBuilder visitPackage(PackageElement p, Void _) {
            if(!pss.getPackageElement().contains(p))
                return null;

            // compare this package with the current package list and compute the list
            String[] to = p.getQualifiedName().toString().split("\\.");

            // skip the common prefix
            int i;
            for( i=0; i<Math.min(to.length,pkg.length); i++ )
                if(!pkg[i].equals(to[i]))
                    break;

            StringBuilder buf = new StringBuilder();
            for( int j=i; j<pkg.length; j++ ) {
                buf.append("../");
            }
            for( int j=i; j<to.length; j++ ) {
                buf.append(to[j]).append('/');
            }

            if(buf.length()==0) buf.append("./");

            return buf;
        }


        private StringBuilder recurse(Element e) {
            return combine(e.getEnclosingElement().accept(this,null));
        }

        private StringBuilder combine(StringBuilder prefix) {
            if(prefix==null)    return null;
            int len = prefix.length();
            if(len>0 && prefix.charAt(len-1)=='/')
                return prefix;
            if(prefix.indexOf("#")>=0)
                return prefix.append('-');
            else
                return prefix.append('#');
        }

        /**
         * Gets the index of this {@link Element} within its siblings.
         */
        private int getSiblingIndex(Element e) {
            int idx = e.getEnclosingElement().getEnclosedElements().indexOf(e);
            assert idx>=0;
            return idx;
        }

    }

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
}