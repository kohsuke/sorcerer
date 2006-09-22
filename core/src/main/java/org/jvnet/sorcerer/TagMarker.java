package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;

import java.io.PrintWriter;

/**
 * @author Kohsuke Kawaguchi
 */
class TagMarker extends Marker {
    private final String href;
    private final String tag;
    private final String usage;
    private final String id;

    public TagMarker(CompilationUnitTree unitTree, SourcePositions srcPos, Tree tree,
                     String href, String tag, String id, String usage) {
        super(unitTree,srcPos,tree);
        this.href = href;
        this.tag = tag;
        this.usage = usage;
        this.id = id;
    }

    public TagMarker(long sp, long ep, String href, String tag, String id, String usage) {
        super(sp, ep);
        this.href = href;
        this.tag = tag;
        this.usage = usage;
        this.id = id;
    }

    private void writeAttribute(PrintWriter w,String name,String value) {
        if(value==null) return;
        w.print(' ');
        w.print(name);
        w.print("='");
        w.print(value);
        w.print("'");
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
        writeAttribute(w,"class",tag);
        writeAttribute(w,"u",usage);
        writeAttribute(w,"id",id);
        w.print('>');
    }

    public void writeEnd(PrintWriter w) {
        w.print("</span>");
        if(href!=null)
            w.print("</a>");
    }
}
