package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;

/**
 * Resolves path to other resorces referenced from HTML files.
 * @author Kohsuke Kawaguchi
 */
public interface ResourceResolver {
    /**
     * Computes a reference to the resource to be baked into the HTML file
     * generated for the given compilation unit.
     */
    String href(CompilationUnitTree compUnit);
}
