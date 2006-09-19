package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import java.util.Collection;

/**
 * {@link LinkResolver} that combines multiple {@link LinkResolver}s behind the scene.
 *
 * <p>
 * {@link LinkResolver} from each bundled {@link LinkResolverFactory} is consulted
 * in the given order, and the first returned link will be used.
 *
 * @author Kohsuke Kawaguchi
 */
public final class LinkResolverFacade implements LinkResolverFactory {
    private final LinkResolverFactory[] factories;

    public LinkResolverFacade(LinkResolverFactory... factories) {
        this.factories = factories;
    }

    public LinkResolverFacade(Collection<? extends LinkResolverFactory> factories) {
        this.factories = factories.toArray(new LinkResolverFactory[factories.size()]);
    }

    public LinkResolver create(CompilationUnitTree currentCompilationUnit, ParsedSourceSet sources) {
        LinkResolver[] resolvers = new LinkResolver[factories.length];
        for( int i=0; i<factories.length; i++ )
            resolvers[i] = factories[i].create(currentCompilationUnit,sources);
        return createFacade(resolvers);
    }

    public LinkResolver create(PackageElement pkg, ParsedSourceSet sources) {
        LinkResolver[] resolvers = new LinkResolver[factories.length];
        for( int i=0; i<factories.length; i++ )
            resolvers[i] = factories[i].create(pkg,sources);
        return createFacade(resolvers);
    }

    private LinkResolver createFacade(final LinkResolver[] resolvers) {
        return new LinkResolver() {
            public String href(Element e) {
                for (LinkResolver r : resolvers) {
                    String href = r.href(e);
                    if(href!=null)  return href;
                }
                return null;
            }
        };
    }
}
