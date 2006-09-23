package org.jvnet.sorcerer.frame;

import com.sun.source.tree.CompilationUnitTree;
import org.jvnet.sorcerer.DefaultHtmlGenerator;
import org.jvnet.sorcerer.ParsedSourceSet;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * {@link DefaultHtmlGenerator} with a bit of extension
 * to work inside the frame set.
 *
 * @author Kohsuke Kawaguchi
 */
public class FrameHtmlGenerator extends DefaultHtmlGenerator {
    public FrameHtmlGenerator(ParsedSourceSet pss, CompilationUnitTree cu) throws IOException {
        super(pss, cu);
    }


    public void writeBody(PrintWriter out) throws IOException {
        out.print("<div id=\"menuSelector\"></div>");
        super.writeBody(out);
        // script fragment to sync up the outline view.
        out.println("<script type='text/javascript'>");
        out.println("  var adrs = new String(window.location);");
        out.println("  var hash = adrs.lastIndexOf('#');");
        out.println("  if(hash>=0)  adrs=adrs.substring(0,hash);");
        out.println("  adrs = adrs.substring(0,adrs.length-5)+\"-outline.js\";");
        out.println("  top.outline.main.location.replace(\""+relativeLinkToTop+"outline-view.html?\"+adrs);");
        out.println("</script>");
    }
}
