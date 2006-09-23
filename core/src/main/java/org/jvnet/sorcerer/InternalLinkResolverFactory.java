package org.jvnet.sorcerer;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.Trees;
import org.jvnet.sorcerer.util.TreeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.AbstractElementVisitor6;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

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
         * The package in which the current compilation unit is in. Tokenized.
         * e.g., {"org","acme","foo"}. Never null.
         */
        private final String[] pkg;

        private final ParsedSourceSet pss;
        private final Trees trees;
        private final Types types;
        private final Elements elements;

        /**
         * If we are generating links into an HTML file that corresponds to a compilation unit,
         * this field is non-null.
         */
        private final CompilationUnitTree compUnit;

        public InternalLinkResolver(CompilationUnitTree compUnit, ParsedSourceSet pss) {
            this.pss = pss;
            this.trees = pss.getTrees();
            this.elements = pss.getElements();
            this.types = pss.getTypes();
            this.compUnit = compUnit;
            this.pkg = TreeUtil.splitPackageName(TreeUtil.getPackageName(compUnit));
        }

        public InternalLinkResolver(PackageElement pkg, ParsedSourceSet pss) {
            this.pss = pss;
            this.compUnit = null;
            this.trees = pss.getTrees();
            this.elements = pss.getElements();
            this.types = pss.getTypes();
            this.pkg = TreeUtil.splitPackageName(pkg);
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
            ClassTree ct = trees.getTree(t);
            if(ct ==null)
                return null;    // not a part of compiled source files

            switch(t.getNestingKind()) {
            case ANONYMOUS:
                String binaryName = elements.getBinaryName(t).toString();
                int idx = binaryName.lastIndexOf('$');
                String name = "~"+binaryName.substring(idx); // #$1 is ambiguous between field and anonyous type, so use '~' as the prefix for type
                return combine(getEnclosingTypeOrPackage(t).accept(this,null)).append(name);
            case TOP_LEVEL:
                // check if this class is the 'primary type' of the compilation unit
                CompilationUnitTree owner = pss.getTreePathByClass().get(ct).getCompilationUnit();
                String primaryTypeName = TreeUtil.getPrimaryTypeName(owner);
                String simpleName = ct.getSimpleName().toString();

                StringBuilder buf;
                if(!owner.equals(compUnit)) {
                    buf = combine(recurse(t)).append(primaryTypeName).append(".html");
                } else {
                    buf = new StringBuilder();
                }
                if(!primaryTypeName.equals(simpleName)) {
                    return buf.append("#~").append(simpleName);
                } else {
                    return buf.append("#this");
                }
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
            String[] to = TreeUtil.splitPackageName(p);

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
}