package org.jvnet.sorcerer;

import antlr.Token;
import antlr.TokenStreamException;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import org.jvnet.sorcerer.impl.JavaLexer;
import org.jvnet.sorcerer.impl.JavaTokenTypes;
import org.jvnet.sorcerer.util.IOUtil;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates an HTML file from a source file (but for a single class.)
 *
 * <p>
 * This class can be subclassed to perform a completely different kind of
 * HTML generation, if so desired.
 *
 * @author Kohsuke Kawaguchi
 */
public class HtmlGenerator {

    protected final List<Marker> markers = new ArrayList<Marker>();

    /**
     * {@link BookmarkSet} keyed by line number.
     */
    protected final Map<Integer,BookmarkSet> bookmarks = new HashMap<Integer,BookmarkSet>();

    /**
     * Original Java source file to be annotated.
     */
    protected final String sourceFile;

    protected final ParsedSourceSet pss;

    protected final CompilationUnitTree compUnit;


    /**
     * Iterator-like forward scanner.
     */
    protected final class MarkerScanner {
        private int idx;

        public Marker peek() {
            if(idx==markers.size()) return null;
            return markers.get(idx);
        }
        public Marker pop() {
            return markers.get(idx++);
        }
    }

    protected HtmlGenerator(ParsedSourceSet pss, CompilationUnitTree cu) throws IOException {
        this.pss = pss;
        this.compUnit = cu;
        sourceFile = IOUtil.readFully(cu.getSourceFile().openInputStream());

        //long ep = pss.getSourcePositions().getEndPosition(cu, cu);
        //totalLines = cu.getLineMap().getLineNumber(ep);

        // fill in the HtmlGenerator
        pss.configure(cu,this);
    }

    /*package*/ boolean add(Marker o) {
        if(o.ep==-1)    return false;   // synthetic
        return markers.add(o);
    }

    /*package*/ final void add(long lineNumber, Bookmark bookmark) {
        // tree API uses long for line numbers
        add((int)lineNumber,bookmark);
    }

    /*package*/ void add(int lineNumber, Bookmark bookmark) {
        BookmarkSet set = bookmarks.get(lineNumber);
        if(set==null) {
            set = new BookmarkSet(lineNumber);
            bookmarks.put(lineNumber,set);
        }
        set.add(bookmark);
    }

    /**
     * Finds the first identifier token after the given tree.
     *
     * <p>
     * This is used to identify the position of few nodes that are not available
     * from javac.
     *
     * @param t
     *      The first token after the end position of this tree node will be returned.
     * @param id
     *      If non-null, find the first token that has this identifier.
     * @return null
     *      if no such node is found or a syntax error is detected.
     */
    protected final Token findTokenAfter(Tree t, String id) {
        long pos = pss.getSourcePositions().getEndPosition(compUnit, t);
        if(pos<0)   return null;
        JavaLexer lexer = new JavaLexer(new StringReader(sourceFile.substring((int) pos)));
        lexer.setTabSize(pss.getTabWidth());
        try {
            while(true) {
                Token token = lexer.nextToken();
                int type = token.getType();
                if(type == JavaTokenTypes.EOF)
                    break;
                if(type == JavaTokenTypes.IDENT) {
                    if(id!=null && !token.getText().equals(id))
                        continue;
                    LineMap lm = compUnit.getLineMap();
                    token.setLine(token.getLine()+(int)lm.getLineNumber(pos)-1);
                    token.setColumn(token.getColumn()+(int)lm.getColumnNumber(pos)-1);
                    return token;
                }
            }
        } catch (TokenStreamException e) {
            // the analysis phase should have reported all the errors,
            // so we should ignore any failures at this point.
        }
        return null;
    }

}
