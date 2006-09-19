package org.jvnet.sorcerer;

import antlr.Token;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;

import java.io.PrintWriter;

/**
 * @author Kohsuke Kawaguchi
 */
class SpanMarker extends Marker {
    private final String tag;
    private String id;

    public SpanMarker(CompilationUnitTree unitTree, SourcePositions srcPos, Tree r, String tag) {
        super(unitTree, srcPos, r);
        this.tag = tag;
    }

    public SpanMarker(CompilationUnitTree unitTree, SourcePositions srcPos, Tree r, String tag, String id) {
        this(unitTree,srcPos,r,tag);
        this.id = id;
    }

    public SpanMarker(long sp, long ep, String tag) {
        super(sp, ep);
        this.tag = tag;
    }

    public SpanMarker(LineMap lineMap, Token t, String tag) {
        super(lineMap, t);
        this.tag = tag;
    }

    public SpanMarker(LineMap lineMap, Token t, String tag, String id) {
        this(lineMap,t,tag);
        this.id = id;
    }

    public void writeStart(PrintWriter w) {
        w.print("<span class='"+tag+'\'');
        if(id!=null)
            w.print(" id='"+id+"'");
        w.print(">");
    }

    public void writeEnd(PrintWriter w) {
        w.print("</span>");
    }
}
