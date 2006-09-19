package org.jvnet.sorcerer.maven_plugin;

import java.io.File;

/**
 * Nested &lt;javadoc> element inside {@link AbstractSorcererReport}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Javadoc {
    /**
     * URL to remote javadoc site.
     *
     * @parameter
     * @required
     */
    protected String href;

    /**
     * Optional local package-list copy.
     *
     * @parameter
     */
    protected File packageList;
}
