package org.jvnet.sorcerer.util;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class IOException2 extends IOException {

    public IOException2(String s) {
        super(s);
    }

    public IOException2(Throwable t) {
        initCause(t);
    }
}
