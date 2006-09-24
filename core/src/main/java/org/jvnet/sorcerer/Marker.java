package org.jvnet.sorcerer;

import antlr.Token;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;

import java.io.PrintWriter;

/**
 * Represents a region of the source file to be annotated.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class Marker implements Comparable<Marker> {
    /**
     * Start position and end poisition as defined in {@link LineMap}.
     */
    final long sp,ep;

    protected Marker(long sp, long ep) {
        this.sp = sp;
        this.ep = ep;
    }

    protected Marker(LineMap lineMap, Token t) {
        sp = lineMap.getPosition(t.getLine(),t.getColumn());
        ep = sp+t.getText().length();
    }

    protected Marker(CompilationUnitTree unitTree, SourcePositions srcPos, Tree r) {
        this.sp = srcPos.getStartPosition(unitTree,r);
        this.ep = srcPos.getEndPosition(unitTree,r);
    }

    public abstract void writeStart(PrintWriter w);
    public abstract void writeEnd(PrintWriter w);

    public final int compareTo(Marker that) {
        long r;

        r= this.sp-that.sp;
        if(r!=0)    return sign(r);

        r = that.ep-this.ep;
        return sign(r);
    }

    private int sign(long r) {
        if(r>0) return 1;
        if(r<0) return -1;
        return 0;
    }

    /**
     * Creates a new marker that represents the anchor ID for this
     * at 10 lines above this marker.
     */
    public Marker createId(final String id, LineMap lineMap) {
        long line = lineMap.getLineNumber(sp);
        line = Math.max(1,line-10);

        long idPos = lineMap.getPosition(line,1);

        return new Marker(idPos,idPos) {
            public void writeStart(PrintWriter w) {
                w.print("<b id='");
                w.print(id);
                w.print("\'></b>");
            }

            public void writeEnd(PrintWriter w) {
            }
        };
    }
}
