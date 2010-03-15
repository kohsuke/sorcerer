package sorcerer.client.sourceview;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Window;
import com.smartgwt.client.types.Visibility;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
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
    private Menu menu;
    private IMenuButton menuButton;

    public SourceViewWidget() {
        menu = new Menu();
        menu.setShowShadow(true);
        menu.setShadowDepth(10);

        menu.setItems(
            new MenuItem("<a href='#'>Permalink</a>"),
            new MenuItem("Find Usage")
        );

        menuButton = new IMenuButton("",menu);
        menuButton.setZIndex(999999);
        menuButton.setWidth(32);
        menuButton.setBackgroundColor("white");

        /*
             For whatever reason, I just couldn't manage to get the mousemove event handler
             installed at the widget level --- doing so results in a VM crash.
         */
        installMouseMove(Document.get().getBody());
    }

    public void postInit() {
        menuButton.draw();
    }

    private void onMouseMove(NativeEvent ev) {
//        Document.get().setTitle(ev.getEventTarget()+" x="+ev.getScreenX());
        Element e = Element.as(ev.getEventTarget());
        String u = e.getAttribute("u");
        if (u==null || u.length()==0) {// getAttribute seems to return "" instead of null.
            menuButton.setVisibility(Visibility.HIDDEN);
            return;
        }

        String html = e.getInnerHTML();
        html = html.substring(0,Math.min(10,html.length()));
        Window.setTitle(e.getTagName()+" u="+u+" html="+html);
        menuButton.setVisibility(Visibility.VISIBLE);
        menuButton.setLeft(e.getAbsoluteLeft()+e.getOffsetWidth());
        menuButton.setTop(e.getAbsoluteTop());
    }

    private native void installMouseMove(Element e) /*-{
        var self = this;
        e.addEventListener("mousemove",function (ev) {
            self.@sorcerer.client.sourceview.SourceViewWidget::onMouseMove(Lcom/google/gwt/dom/client/NativeEvent;)(ev);
        },false);
    }-*/;

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
