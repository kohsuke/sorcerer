package org.jvnet.sorcerer;

import antlr.Token;
import antlr.TokenStreamException;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import org.jvnet.sorcerer.Tag.Root;
import org.jvnet.sorcerer.impl.JavaLexer;
import org.jvnet.sorcerer.impl.JavaTokenTypes;
import org.jvnet.sorcerer.util.CharSequenceReader;
import org.jvnet.sorcerer.util.TreeUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Generates AST JavaScript file from a compilation unit.
 *
 * @author Kohsuke Kawaguchi
 */
public final class AstGenerator {

    protected final List<Tag> tags = new ArrayList<Tag>();

    /**
     * Original Java source file to be annotated.
     */
    protected final CharSequence sourceFile;

    protected final ParsedSourceSet pss;

    protected final CompilationUnitTree compUnit;

    /**
     * Current line number being written.
     */
    protected int lineNumber = 0;

    /**
     * SP x tabWidth.
     */
    private String tab;

    /**
     * Relative link to the top of the output directory,
     * like "../../".
     */
    protected final String relativeLinkToTop;

    public AstGenerator(ParsedSourceSet pss, CompilationUnitTree cu) throws IOException {
        this.pss = pss;
        this.compUnit = cu;
        sourceFile = cu.getSourceFile().getCharContent(true);

        //long ep = pss.getSourcePositions().getEndPosition(cu, cu);
        //totalLines = cu.getLineMap().getLineNumber(ep);

        // fill in the AstGenerator
        pss.configure(cu,this);

        StringBuilder b = new StringBuilder();
        for( int i=0; i<pss.getTabWidth(); i++ )
            b.append(' ');
        tab = b.toString();

        String pkgName = TreeUtil.getPackageName(cu);
        StringBuilder buf = new StringBuilder();
        for( int i=new StringTokenizer(pkgName,".").countTokens(); i>0; i-- )
            buf.append("../");
        relativeLinkToTop = buf.toString();
    }

    /**
     * Writes the complete structure java script.
     *
     * @param out
     *      The writer to receive JavaScript. This writer must be closed by the caller.
     */
    public void write(JavaScriptStreamWriter out) throws IOException {
        writeHeader(out);
        writeBody(out);
        writeFooter(out);
    }

    /**
     * Writes the complete structure java script.
     *
     * @param os
     *      The writer to receive JavaScript. This writer must be closed by the caller.
     */
    public void write(OutputStream os) throws IOException {
        // TODO: think about encoding.
        Writer w = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        write(w);
    }

    /**
     * Writes the complete structure java script.
     *
     * @param w
     *      The writer to receive JavaScript.
     */
    public void write(Writer w) throws IOException {
        write(new JavaScriptStreamWriter(w,pss));
        w.flush();
    }

    /**
     * Gets the relative path name inside the output directory where
     * the AST JavaScript should be written.
     */
    public String getRelativePath() {
        ExpressionTree packageName = compUnit.getPackageName();
        String pkg = packageName==null?"":packageName.toString().replace('.','/')+'/';

        String name = TreeUtil.getPrimaryTypeName(compUnit);

        return pkg + name+".js";
    }

    /**
     * Writes the complete structure java script.
     */
    public void write(File outDir) throws IOException {
        File out = new File(outDir, getRelativePath());
        File parent = out.getParentFile();
        if(parent!=null)    // null if outDir was "."
            parent.mkdirs();

        FileOutputStream os = new FileOutputStream(out);
        try {
            write(os);
        } finally {
            os.close();
        }
    }


    /**
     * Organizes {@link #tags} to the tree structure.
     */
    public Tag.Root buildTree() throws IOException {
        Reader in = new CharSequenceReader(sourceFile);
        long curPos = 0;    // chars from input that are written so far
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[256];

        Collections.sort(tags);

        TagStack opened = new TagStack();
        // start with the root tag
        Root root = new Root(sourceFile.length());
        opened.push(root);

        TagScanner upcoming = new TagScanner(tags);

        OUTER:
        while(true) {
            // determine the next marker position
            TagStack.Adder lhs = opened.peek();
            Tag rhs = upcoming.peek();

            long nextPos = Math.min(
                lhs==null ? Long.MAX_VALUE : lhs.endPos(),
                rhs==null ? Long.MAX_VALUE : rhs.sp );

            // read until nextPos
            sb.setLength(0);
            while(curPos<nextPos) {
                int sz = in.read(buf,0, (int)Math.min(nextPos-curPos,buf.length));
                if(sz<0)    break OUTER; // all streams read
                sb.append(buf,0,sz);
                curPos += sz;
            }

            assert lhs!=null;   // this is only possible when curPos==sourceFile.length

            if(sb.length()>0) {
                lhs.add(new Tag.SourceText(curPos-sb.length(),curPos,sb.toString()));
            }

            if(lhs.endPos()==nextPos) {
                // the current range closes
                opened.pop();
            } else {
                // new range opens
                lhs.add(rhs);
                opened.push(rhs);
                upcoming.pop();
            }
        }

        while(upcoming.peek()!=null) {
            Tag t = upcoming.pop();
            opened.peek().add(t);
            opened.push(t);
        }

        return root;
    }

    /**
     * Just write sthe body annotated source code without a surrounding
     * &lt;body> tag. This method can be invoked directly if the caller
     * wants to embed the generated HTML into a bigger HTML document.
     */
    public void writeBody(JavaScriptStreamWriter out) throws IOException {
        Root tree = buildTree();
        tree.collectSymbols(out);
        out.resetList();
        out.writeSymbolTable();

        // write the body
        out.println();
        out.resetList();
        out.print("return ");
        tree.write(out);
    }

    /**
     * Writes the bytes from the source code.
     *
     * <p>
     * This is a chance to perform escaping and other things like TAB->SP conversion.
     */
    protected void writeSourceCode(PrintWriter out, char[] buf, int sz) {
        for( int i=0; i<sz; i++ ) {
            char ch = buf[i];
            switch(ch) {
            case '<':
                out.write("&lt;");
                break;
            case '>':
                out.write("&gt;");
                break;
            case '&':
                out.write("&amp;");
                break;
            case '\r':
                // skip the CR. When we write LF, we use the right platform default line-end format.
                // this allows the generated files to be easily checked into SVN, for example.
                break;
            case '\n':
                writeEOL(out);
                writeNewLine(out);
                break;
            case '\t':
                out.write(tab);
                break;
            default:
                out.write(ch);
                break;
            }
        }
    }

    /**
     * Writes the end of the line.
     */
    protected void writeEOL(PrintWriter out) {
        out.println();
    }

    /**
     * Writes the start of a new line.
     */
    protected void writeNewLine(PrintWriter out) {
        lineNumber++;
        // line number is written in a separate table
    }

    /**
     * Writes the preamble.
     */
    protected void writeHeader(JavaScriptStreamWriter out) {
        out.println("defineStructure(");
        out.string(TreeUtil.getPrimaryTypeName(compUnit));
        out.print(',');
        out.print("function(factory){with(factory) { ");
        out.i().nl();
    }

    /**
     * Writes the start tag of BODY.
     *
     * <p>
     * This can be overridden to add attribtues to BODY tag,
     * or generate additional prolog before the code part starts.
     */
    protected void writeBodyTag(PrintWriter out) {
        out.println("<body>");
    }

    /**
     * Writes the footer.
     */
    protected void writeFooter(JavaScriptStreamWriter out) {
        out.o().nl().print(";}});");
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
