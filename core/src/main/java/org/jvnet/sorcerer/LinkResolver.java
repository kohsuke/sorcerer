package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;

import javax.lang.model.element.Element;

/**
 * Computes a relative link to an {@link Element} from the current {@link CompilationUnitTree}
 * (which was given to {@link LinkResolverFactory#create}.)
 *
 * @see LinkResolverFactory
 * @author Kohsuke Kawaguchi
 */
public interface LinkResolver {
    /**
     * Computes the link to the given element.
     *
     * @param e
     *      The generated link should point to this program element.
     * @return null if there's no link.
     */
    String href(Element e);
}
