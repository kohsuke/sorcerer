package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

/**
 * Working around bug 6472751.
 *
 * @author Kohsuke Kawaguchi
 */
final class SourcePositionsWrapper implements SourcePositions {
    private final SourcePositions pos;

    public SourcePositionsWrapper(SourcePositions pos) {
        this.pos = pos;
    }

    public long getStartPosition(CompilationUnitTree file, Tree tree) {
        long pos = this.pos.getStartPosition(file, tree);
        if(pos==-1 && tree instanceof JCVariableDecl) {
            return ((JCVariableDecl)tree).pos;
        }
        if(pos==-1 && tree instanceof JCMethodDecl) {
            // a bug in the constructor start position detection
            JCMethodDecl mt = (JCMethodDecl) tree;
            if(mt.restype==null)
                return mt.pos;
        }
        return pos;
    }

    public long getEndPosition(CompilationUnitTree file, Tree tree) {
        return pos.getEndPosition(file, tree);
    }
}
