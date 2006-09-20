package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;
import org.jvnet.sorcerer.util.TreeUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Date;
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
     * Writes the complete HTML (header, body, then footer.)
     *
     * @param out
     *      The writer to receive HTML. This writer must be closed by the caller.
     */
    public void write(PrintWriter out) throws IOException {
        writeHeader(out);
        writeBody(out);
        writeFooter(out);
    }

    /**
     * Writes the complete HTML (header, body, then footer.)
     *
     * @param os
     *      The writer to receive HTML. This writer must be closed by the caller.
     */
    public void write(OutputStream os) throws IOException {
        PrintWriter w = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, "UTF-8")));
        write(w);
        w.flush();
    }

    /**
     * Writes the complete HTML (header, body, then footer.)
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
     * Just write sthe body annotated source code without a surrounding
     * &lt;body> tag. This method can be invoked directly if the caller
     * wants to embed the generated HTML into a bigger HTML document.
     */
    public void writeBody(PrintWriter out) throws IOException {
        Reader in = new StringReader(sourceFile);
        Collections.sort(markers);

        char[] buf = new char[256];

        MarkerStack opened = new MarkerStack();
        MarkerScanner upcoming = new MarkerScanner();

        long curPos = 0;    // chars from input that are written so far

        writeNewLine(out);

        OUTER:
        while(true) {
            // determine the next marker position
            Marker lhs = opened.peek();
            Marker rhs = upcoming.peek();

            long nextPos = Math.min(getPos(lhs,false),getPos(rhs,true));

            // read until nextPos
            while(curPos<nextPos) {
                int sz = in.read(buf,0, (int)Math.min(nextPos-curPos,buf.length));
                if(sz<0)    break OUTER; // all streams read
                writeSourceCode(out, buf, sz);
                curPos += sz;
            }

            if(getPos(lhs,false)==nextPos) {
                lhs.writeEnd(out);
                opened.pop();
            } else {
                rhs.writeStart(out);
                opened.push(rhs);
                upcoming.pop();
            }
        }

        while(upcoming.peek()!=null) {
            Marker m = upcoming.pop();
            m.writeStart(out);
            opened.push(m);
        }

        while(opened.peek()!=null) {
            opened.pop().writeEnd(out);
        }

        writeEOL(out);

        in.close();
        // don't close out
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
    protected void writeHeader(PrintWriter out) {
        out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
        out.println("<html><head>");
        if(css==null)
            css = relativeLinkToTop+"style.css";
        out.println("<link type='text/css' rel='stylesheet' href='"+css+"' />");

        out.println("<script type='text/javascript' src='"+relativeLinkToTop+"behavior.js'></script>");
        out.println("<script type='text/javascript' src='"+relativeLinkToTop+"sorcerer.js'></script>");
        out.println("</head>");

        writeBodyTag(out);
        out.println("<div style='position:relative;'>");

        // IE6 aligns the absolute positioned box to the first box, so use this dummy div to make it align to
        // the proper palce
        out.print("<div style='height:0px; overflow:hidden'></div>");
        out.print("<pre id=main style='padding-left:3em'>");
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
    protected void writeFooter(PrintWriter out) {
        out.println("</pre>");
        writeLineNumberTable(out);
        out.println("</div>");
        out.printf(
            "<div class=footer>" +
            "Generated by <a id=homelink href='https://sorcerer.dev.java.net/'>Sorcerer</a> on %tc" +
            "</div>",new Date());
        out.println("</body></html>");
    }

    /**
     * Writes the table for line numbers
     */
    private void writeLineNumberTable(PrintWriter out) {
        StringBuffer buf = new StringBuffer();
        out.print("<pre id=lineNumberTable style='position:absolute; left:0; top:0;'>");
        for( int i=1; i<=lineNumber; i++ ) {
            int numBookmark = 0;
            BookmarkSet set = bookmarks.get(i);
            if(set!=null) {
                for (Bookmark bm : set) {
                    bm.writeTo(out);
                    numBookmark++;
                }
            }

            buf.setLength(0);
            buf.append("<a name=").append(i).append(" href=#").append(i).append('>');
            out.print(buf);

            buf.setLength(0);
            buf.append(i);
            buf.insert(0,"    ",0,Math.max(0,4-buf.length()));
            // cut the line number by # of bookmarks
            buf.delete(0,Math.min(numBookmark*2,buf.length()));
            out.print(buf);

            out.write("</a>");
            out.println();
        }
        out.println("</pre>");
    }

    private long getPos(Marker m, boolean start) {
        if(m==null) return Long.MAX_VALUE;
        else        return start?m.sp:m.ep;
    }
}
