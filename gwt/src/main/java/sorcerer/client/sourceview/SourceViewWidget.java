package sorcerer.client.sourceview;

import com.smartgwt.client.widgets.HTMLPane;
import sorcerer.client.Application;
import sorcerer.client.data.AST;
import sorcerer.client.source.SourceBuilder;

/**
 * The main portion that renders the source file.
 *
 * @author Kohsuke Kawaguchi
 */
public class SourceViewWidget extends HTMLPane {
    public void load(AST ast) {
        SourceBuilder b = new SourceBuilder();
        ast.accept(b);
        String html = b.toHTML();
        setContents(html);
    }

    public static SourceViewWidget get() {
        return Application.get().getSourceView();
    }
}
