package org.jvnet.sorcerer;

import antlr.Token;
import com.sun.source.tree.LineMap;
import org.jvnet.sorcerer.impl.JavaTokenTypes;

import java.io.PrintWriter;

/**
 * @author Kohsuke Kawaguchi
 */
class CommentMarker extends Marker {
    private final int type;
    private static final int JAVADOC_COMMENT = 999;

    public CommentMarker(LineMap lineMap, Token t) {
        super(lineMap, t);
        this.type = t.getText().startsWith("/**") ? JAVADOC_COMMENT : t.getType();
    }

    public void writeStart(PrintWriter w) {
        w.print("<span class="+getCssClass()+">");
    }

    private String getCssClass() {
        switch(type) {
        case JavaTokenTypes.SL_COMMENT:
            return "cs";
        case JavaTokenTypes.ML_COMMENT:
            return "cm";
        case JAVADOC_COMMENT:
            return "cj";
        }
        throw new IllegalArgumentException("invalid token type "+type);
    }

    public void writeEnd(PrintWriter w) {
        w.print("</span>");
    }
}
