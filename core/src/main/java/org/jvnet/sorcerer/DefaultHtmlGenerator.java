package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;
import org.jvnet.sorcerer.Tag.Root;
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
import java.util.Collections;
import java.util.StringTokenizer;

/**
 * Implements the default HTML generation logic.
 *
 * <p>
 * Certain aspects of the HTML generation is customizable via properties
 * of this class. To make a drastic change, you can also choose to
 * derive directly from {@link HtmlGenerator}.
 *
 * @author Kohsuke Kawaguchi
 */
public class DefaultHtmlGenerator extends HtmlGenerator {
    /**
     * Current line number being written.
     */
    protected int lineNumber = 0;

    /**
     * SP x tabWidth.
     */
    private String tab;

    /**
     * Reference to the CSS stylesheet.
     */
    private String css;

    /**
     * Relative link to the top of the output directory,
     * like "../../".
     */
    protected final String relativeLinkToTop;

    public DefaultHtmlGenerator(ParsedSourceSet pss, CompilationUnitTree cu) throws IOException {
        super(pss, cu);

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
     * Sets the reference to the stylesheet.
     *
     * <p>
     * If this value is specified, the generator generates a link to CSS stylesheet,
     * and puts the given string value as-is in the href attribute.
     */
    public void setCss(String css) {
        this.css = css;
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
        write(new JavaScriptStreamWriter(w,pss));
        w.flush();
    }

    /**
     * Writes the complete structure java script.
     */
    public void write(File out) throws IOException {
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
        out.writeSymbolTable();

        // write the body
        out.resetList();
        out.print("return ");
        tree.write(out);
    }

    ///**
    // * Just write sthe body annotated source code without a surrounding
    // * &lt;body> tag. This method can be invoked directly if the caller
    // * wants to embed the generated HTML into a bigger HTML document.
    // */
    //public void writeBody(PrintWriter out) throws IOException {
    //    Reader in = new StringReader(sourceFile);
    //    Collections.sort(tags);
    //
    //    char[] buf = new char[256];
    //
    //    MarkerStack opened = new MarkerStack();
    //    MarkerScanner upcoming = new MarkerScanner();
    //
    //    long curPos = 0;    // chars from input that are written so far
    //
    //    writeNewLine(out);
    //
    //    OUTER:
    //    while(true) {
    //        // determine the next marker position
    //        Marker lhs = opened.peek();
    //        Marker rhs = upcoming.peek();
    //
    //        long nextPos = Math.min(getPos(lhs,false),getPos(rhs,true));
    //
    //        // read until nextPos
    //        while(curPos<nextPos) {
    //            int sz = in.read(buf,0, (int)Math.min(nextPos-curPos,buf.length));
    //            if(sz<0)    break OUTER; // all streams read
    //            writeSourceCode(out, buf, sz);
    //            curPos += sz;
    //        }
    //
    //        if(getPos(lhs,false)==nextPos) {
    //            lhs.writeEnd(out);
    //            opened.pop();
    //        } else {
    //            rhs.writeStart(out);
    //            opened.push(rhs);
    //            upcoming.pop();
    //        }
    //    }
    //
    //    while(upcoming.peek()!=null) {
    //        Marker m = upcoming.pop();
    //        m.writeStart(out);
    //        opened.push(m);
    //    }
    //
    //    while(opened.peek()!=null) {
    //        opened.pop().writeEnd(out);
    //    }
    //
    //    writeEOL(out);
    //
    //    in.close();
    //    // don't close out
    //}

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

        //out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
        //out.println("<html><head>");
        //if(css==null)
        //    css = relativeLinkToTop+"style.css";
        //writeCssTag(out, css);
        //writeCssTag(out,relativeLinkToTop+"menu/menu.css");
        //
        //writeScriptTag(out,"resource-files/yahoo.js");
        //writeScriptTag(out,"resource-files/dom.js");
        //writeScriptTag(out,"resource-files/event.js");
        //writeScriptTag(out,"resource-files/container_core.js");
        //writeScriptTag(out,"menu/menu.js");
        //writeScriptTag(out,"behavior.js");
        //writeScriptTag(out,"sorcerer.js");
        //out.println("</head>");
        //
        //writeBodyTag(out);
        //out.println("<div style='position:relative;'>");
        //
        //// IE6 aligns the absolute positioned box to the first box, so use this dummy div to make it align to
        //// the proper palce
        //out.print("<div style='height:0px; overflow:hidden'></div>");
        //out.print("<pre id=main style='padding-left:3em'>");
    }

    private void writeCssTag(PrintWriter out, String name) {
        out.println("<link type='text/css' rel='stylesheet' href='"+name +"' />");
    }

    private void writeScriptTag(PrintWriter out, String name) {
        out.println("<script type='text/javascript' src='"+relativeLinkToTop+name+"'></script>");
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
}
