package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;

import javax.lang.model.element.PackageElement;

/**
 * @author Kohsuke Kawaguchi
 */
public interface LinkResolverFactory {
    /**
     * Creates a new {@link LinkResolver} to be used for generating links inside
     * the given {@link CompilationUnitTree}.
     *
     * @param sources
     *      The {@link ParsedSourceSet} object for which the created {@link LinkResolver}
     *      would work.
     */
    LinkResolver create(CompilationUnitTree currentCompilationUnit, ParsedSourceSet sources);

    /**
     * Creates a new {@link LinkResolver} to be used for generating links from
     * the specified package.
     *
     * TODO: explain what it means.
     */
    LinkResolver create(PackageElement pkg, ParsedSourceSet sources);
}
