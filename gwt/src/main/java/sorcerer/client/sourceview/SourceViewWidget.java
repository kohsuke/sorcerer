package sorcerer.client.sourceview;

import com.smartgwt.client.widgets.HTMLPane;
import sorcerer.client.Application;
import sorcerer.client.data.AST;
import sorcerer.client.js.JsArray;
import sorcerer.client.source.SourceBuilder;

/**
 * The main portion that renders the source file.
 *
 * @author Kohsuke Kawaguchi
 */
public class SourceViewWidget extends HTMLPane {
    private AST showing;

    public void load(AST ast) {
        if (showing==ast)   return;
        showing = ast;

        SourceBuilder b = ast.accept(new SourceBuilder());
        String html = b.toHTML();

        JsArray<String> lnt = JsArray.create();
        int len = b.getLineNumber();
        for (int i=1; i<=len; i++) {
            String lineText = pad(i,4);
            lnt.push("<a name="+i+" href=#"+i+">");
            lnt.push(lineText);
            lnt.push("</a>\n");
        }

        setContents(
            "<div id=main>"+html+"</div>\n" +
            "<pre id=lineNumberTable>"+lnt.join("")+"</pre>");

        // TODO: bookmark processing
/*
    var lineBookmarks = context.bookmarks[i];
    if(lineBookmarks) {
      lineBookmarks.forEach(function(b) {
        window.bookmarks[b.id] = b;
        lnt.push(b.buildAnchor());
      });
      lineText = lineText.substring(lineBookmarks.length*2);
    }
         */
    }

    private String pad(int i, int w) {
        String s = String.valueOf(i);
        while(s.length()<w)
            s+=' ';
        return s;
    }

    public static SourceViewWidget get() {
        return Application.get().getSourceView();
    }
}
