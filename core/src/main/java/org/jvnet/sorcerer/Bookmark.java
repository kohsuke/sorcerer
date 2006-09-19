package org.jvnet.sorcerer;

import java.io.PrintWriter;

/**
 * Placed on the left hand side of the line number table to
 * show related links for the program element.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class Bookmark {
    protected abstract void writeTo(PrintWriter w);
}
