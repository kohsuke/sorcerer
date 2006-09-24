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

    public TagMarker(CompilationUnitTree unitTree, SourcePositions srcPos, Tree tree,
                     String href, String tag, String usage) {
        super(unitTree,srcPos,tree);
        this.href = href;
        this.tag = tag;
        this.usage = usage;
    }

    public TagMarker(long sp, long ep, String href, String tag, String usage) {
        super(sp, ep);
        this.href = href;
        this.tag = tag;
        this.usage = usage;
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
            w.print("<a");
            writeAttribute(w,"href",href);
            writeAttribute(w,"u",usage);
            w.print('>');
        }

        // a and b needs to be on separate tags,
        // or otherwise applying coloring in CSS becomes
        // fairly tricky because of the selector precedence rules.
        w.print("<b");
        writeAttribute(w,"class",tag);
        w.print('>');
    }

    public void writeEnd(PrintWriter w) {
        w.print("</b>");
        if(href!=null)
            w.print("</a>");
    }
}
