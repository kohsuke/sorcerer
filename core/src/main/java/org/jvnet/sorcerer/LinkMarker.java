package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;

import java.io.PrintWriter;

/**
 * @author Kohsuke Kawaguchi
 */
class LinkMarker extends Marker {
    private final String href;
    private final String tag;

    public LinkMarker(CompilationUnitTree unitTree, SourcePositions srcPos, Tree tree, String href, String tag) {
        super(unitTree,srcPos,tree);
        this.href = href;
        this.tag = tag;
    }

    public LinkMarker(long sp, long ep, String href, String tag) {
        super(sp, ep);
        this.href = href;
        this.tag = tag;
    }

    public void writeStart(PrintWriter w) {
        if(href!=null) {
            w.print("<a href='");
            w.print(href);
            w.print("'>");
        }

        // a and span needs to be on separate tags,
        // or otherwise applying coloring in CSS becomes
        // fairly tricky because of the selector precedence rules.
        w.print("<span ");

        if(tag!=null) {
            w.print(" class='");
            w.print(tag);
            w.print('\'');
        }
        w.print('>');
    }

    public void writeEnd(PrintWriter w) {
        w.print("</span>");
        if(href!=null)
            w.print("</a>");
    }
}
