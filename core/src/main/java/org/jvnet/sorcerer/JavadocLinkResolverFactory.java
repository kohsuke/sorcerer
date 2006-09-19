package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.AbstractElementVisitor6;
import javax.lang.model.util.Types;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Generate links to javadoc.
 *
 * @author Kohsuke Kawaguchi
 */
public class JavadocLinkResolverFactory implements LinkResolverFactory {
    private final String baseUrl;
    private final Set<String> packageNames;

    /**
     * @param baseUrl
     *      The top page of the javadoc website, like "http://java.sun.com/j2se/1.5.0/docs/api/"
     */
    public JavadocLinkResolverFactory(String baseUrl) throws IOException {
        this(baseUrl,(URL)null);
    }

    public JavadocLinkResolverFactory(String baseUrl, URL packageList) throws IOException {
        this.baseUrl = fixBaseUrl(baseUrl);
        if(packageList==null)   packageList = new URL(baseUrl+"package-list");
        packageNames = parsePackageInfo(packageList);
    }

    /**
     * Creates javadoc link but without accessing the linked website (analogous to
     * <tt>-linkoffline</tt> in javadoc.)
     *
     * @param localPackageInfo
     *      The local copy of the package-info file.
     */
    public JavadocLinkResolverFactory(String baseUrl, File localPackageInfo) throws IOException {
        this(baseUrl,localPackageInfo==null?null:localPackageInfo.toURI().toURL());
    }

    private static String fixBaseUrl(String baseUrl) {
        if(!baseUrl.endsWith("/"))  baseUrl+='/';
        return baseUrl;
    }

    /**
     * Parses a "package-info" file into a set of package names.
     */
    private Set<String> parsePackageInfo(URL packageInfo) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(packageInfo.openStream(),"UTF-8"));

        Set<String> r = new HashSet<String>();

        String line;
        while((line=in.readLine())!=null) {
            r.add(line);
        }

        return r;
    }

    public LinkResolver create(CompilationUnitTree currentCompilationUnit, ParsedSourceSet sources) {
        return new JavadocLinkResolver(sources.getTypes());
    }

    public LinkResolver create(PackageElement pkg, ParsedSourceSet sources) {
        return new JavadocLinkResolver(sources.getTypes());
    }

    class JavadocLinkResolver extends AbstractElementVisitor6<String,Void> implements LinkResolver {
        private final TypeVisitor<TypeMirror,Void> javadocErasure;

        public JavadocLinkResolver(Types types) {
            javadocErasure = new JavadocErasureVisitor(types);
        }

        public void setCurrent(CompilationUnitTree compUnit) {
            // we don't use the current compilation unit in the computation
        }

        public String href(Element e) {
            return visit(e,null);
        }

        public String visitPackage(PackageElement e, Void _) {
            String fullName = e.getQualifiedName().toString();
            if(packageNames.contains(fullName))
                return baseUrl+'?'+fullName.replace('.','/')+"/package-summary.html";
            else
                return null;
        }

        public String visitType(TypeElement e, Void _) {
            return visitType(e,true);
        }

        public String visitType(TypeElement e, boolean useFrame) {
            if(!isInPackageList(e))
                return null;    // outside the package

            StringBuilder builder = buildTypeHref(e);
            if(builder==null)   return null;
            return builder.append(".html").toString();
        }

        /**
         * Builds the javadoc link to a type.
         *
         * The general form of link is:
         * org/acme/Foo.Bar.html (where Bar is nested inside Foo)
         *
         */
        private StringBuilder buildTypeHref(TypeElement e) {
            StringBuilder builder;
            switch (e.getNestingKind()) {
            case ANONYMOUS:
            case LOCAL:
                // no javadoc for local and anonymous types (TODO:check)
                return null;
            case MEMBER:
                builder = buildTypeHref((TypeElement) e.getEnclosingElement());
                if(builder==null)   return null;
                return builder.append('.').append(e.getSimpleName());
            case TOP_LEVEL:
                String fullName = e.getQualifiedName().toString();
                builder = new StringBuilder(baseUrl);
                return builder.append(fullName.replace('.', '/'));
            default:
                throw new IllegalStateException(e.toString());
            }
        }

        /**
         * Returns true if the given program element ultimately
         * belongs to one of the packages in this javadoc.
         */
        private boolean isInPackageList(Element e) {
            // not available
            // PackageElement pe = elements.getPackageOf(e);

            while(e!=null && !(e instanceof PackageElement))
                e = e.getEnclosingElement();

            if(e==null)        return false;
            return packageNames.contains(((PackageElement)e).getQualifiedName().toString());
        }

        public String visitVariable(VariableElement e, Void _) {
            Element parent = e.getEnclosingElement();
            if(parent instanceof TypeElement) {
                // field
                String typeRef = visitType((TypeElement)parent, false);
                if(typeRef==null)       return null;
                return typeRef +'#'+e.getSimpleName();
            }
            // other variables
            return null;
        }

        public String visitExecutable(ExecutableElement e, Void _) {
            Element parent = e.getEnclosingElement();
            if(parent instanceof TypeElement) {
                // method
                String typeRef = visitType((TypeElement) parent, false);
                if(typeRef==null)       return null;

                StringBuilder buf = new StringBuilder(typeRef).append('#');
                if(e.getKind()== ElementKind.CONSTRUCTOR)
                    // javadoc uses the class name as the constructor name, not <init>
                    buf.append(e.getEnclosingElement().getSimpleName());
                else
                    buf.append(e.getSimpleName());

                buf.append('(');

                boolean first=true;

                for (VariableElement p : e.getParameters()) {
                    if(first)       first = false;
                    else            buf.append(',');

                    buf.append(p.asType().accept(javadocErasure,null).toString());
                }
                buf.append(')');

                return buf.toString();
            }

            // ???
            return null;
        }

        public String visitTypeParameter(TypeParameterElement e, Void _) {
            // javadoc apparently uses the location of the declaration as the link target.
            return visit(e.getGenericElement(),_);
        }
    }

}