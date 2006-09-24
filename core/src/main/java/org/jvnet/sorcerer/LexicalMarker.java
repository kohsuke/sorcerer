package org.jvnet.sorcerer;

import antlr.Token;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;

import java.io.PrintWriter;

/**
 * Mark up a lexically recognizable token that doesn't have any associated
 * intelligence.
 *
 * @author Kohsuke Kawaguchi
 */
class LexicalMarker extends Marker {
    private final String tag;

    public LexicalMarker(CompilationUnitTree unitTree, SourcePositions srcPos, Tree r, String tag) {
        super(unitTree, srcPos, r);
        this.tag = tag;
    }

    public LexicalMarker(long sp, long ep, String tag) {
        super(sp, ep);
        this.tag = tag;
    }

    public LexicalMarker(LineMap lineMap, Token t, String tag) {
        super(lineMap, t);
        this.tag = tag;
    }

    public void writeStart(PrintWriter w) {
        w.print("<b class='");
        w.print(tag);
        w.print("\'>");
    }

    public void writeEnd(PrintWriter w) {
        w.print("</b>");
    }
}
