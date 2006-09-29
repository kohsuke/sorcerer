package org.jvnet.sorcerer;

import antlr.Token;
import antlr.TokenStreamException;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import org.jvnet.sorcerer.impl.JavaLexer;
import org.jvnet.sorcerer.impl.JavaTokenTypes;
import org.jvnet.sorcerer.util.CharSequenceReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    protected final List<Tag> tags = new ArrayList<Tag>();

    /**
     * Original Java source file to be annotated.
     */
    protected final CharSequence sourceFile;

    protected final ParsedSourceSet pss;

    protected final CompilationUnitTree compUnit;


    protected HtmlGenerator(ParsedSourceSet pss, CompilationUnitTree cu) throws IOException {
        this.pss = pss;
        this.compUnit = cu;
        sourceFile = cu.getSourceFile().getCharContent(true);

        //long ep = pss.getSourcePositions().getEndPosition(cu, cu);
        //totalLines = cu.getLineMap().getLineNumber(ep);

        // fill in the HtmlGenerator
        pss.configure(cu,this);
    }

    /*package*/ boolean add(Tag o) {
        if(o.ep==-1)    return false;   // synthetic
        return tags.add(o);
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
     * @param findAfterToken
     *      true if the search should begin from the end of the given token.
     *      false to start from the start of the given token.
     * @param id
     *      If non-null, find the first token that has this identifier.
     * @return null
     *      if no such node is found or a syntax error is detected.
     */
    protected final Token findTokenAfter(Tree t, boolean findAfterToken, String id) {
        long pos;
        if(findAfterToken)
            pos = pss.getSourcePositions().getEndPosition(compUnit, t);
        else
            pos = pss.getSourcePositions().getStartPosition(compUnit, t);

        if(pos<0)   return null;
        JavaLexer lexer = new JavaLexer(new CharSequenceReader(sourceFile.subSequence((int)pos, sourceFile.length())));
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
                    if(token.getLine()==1) {
                        token.setColumn(token.getColumn()+(int)lm.getColumnNumber(pos)-1);
                    }
                    token.setLine(token.getLine()+(int)lm.getLineNumber(pos)-1);
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
