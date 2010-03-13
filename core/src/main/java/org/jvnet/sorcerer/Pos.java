package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;

/**
 * Line and column position
 *
 * @author Kohsuke Kawaguchi
 */
public class Pos {
    public final long line,column;

    public Pos(long line, long column) {
        this.line = line;
        this.column = column;
    }

    public Pos(CompilationUnitTree cu, long pos) {
        this(cu.getLineMap().getLineNumber(pos),
             cu.getLineMap().getColumnNumber(pos));
    }
}
