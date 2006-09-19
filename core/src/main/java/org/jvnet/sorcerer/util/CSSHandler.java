package org.jvnet.sorcerer.util;

import com.sun.source.tree.CompilationUnitTree;

import java.io.File;
import java.io.IOException;

/**
 * Abstraction for customizing CSS.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class CSSHandler extends AbstractResourceResolver {

    /**
     * Copy any relevant files needed for this stylesheet setting
     * to the output directory.
     */
    public abstract void copy(File outDir) throws IOException;

    /**
     * Handler for the CSS file to be copied into the output directory.
     */
    private static abstract class Copied extends CSSHandler {
        public String href(CompilationUnitTree compUnit) {
            return getRelativePathToTop(compUnit)+"style.css";
        }
    }

    /**
     * Uses the default CSS.
     */
    public static final class Default extends Copied {
        public void copy(File outDir) throws IOException {
            IOUtil.copy("style.css",new File(outDir,"style.css"));
        }
    }

    /**
     * Uses the user-specified CSS file by copying it.
     */
    public static final class User extends Copied {
        private final File src;

        public User(File src) {
            this.src = src;
        }

        public void copy(File outDir) throws IOException {
            IOUtil.copy(src,new File(outDir,"style.css"));
        }
    }

    /**
     * Simply refer to the fixed external URL.
     */
    public static final class Ref extends CSSHandler {
        private final String href;

        public Ref(String href) {
            this.href = href;
        }

        public String href(CompilationUnitTree compUnit) {
            return href;
        }

        public void copy(File outDir) {
            // noop
        }
    }
}
