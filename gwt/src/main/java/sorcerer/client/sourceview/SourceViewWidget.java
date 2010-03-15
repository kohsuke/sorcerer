package sorcerer.client.sourceview;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.smartgwt.client.types.Visibility;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import sorcerer.client.Application;
import sorcerer.client.data.AST;
import sorcerer.client.js.JsArray;
import sorcerer.client.source.SourceBuilder;

import static com.smartgwt.client.types.Visibility.HIDDEN;
import static com.smartgwt.client.types.Visibility.VISIBLE;

/**
 * The main portion that renders the source file.
 *
 * @author Kohsuke Kawaguchi
 */
public class SourceViewWidget extends HTMLPane {
    private AST showing;
    private Menu menu;
    private IButton menuButton;

    public SourceViewWidget() {
        menu = new Menu();
        menu.setShowShadow(true);
        menu.setShadowDepth(10);

        menu.setItems(
            new MenuItem("<a href='#'>Permalink</a>"),
            new MenuItem("Find Usage")
        );

        menuButton = new IButton();
        menuButton.setZIndex(999999);
        menuButton.setWidth(24);
        menuButton.setHeight(24);
        menuButton.setIcon("menu_button.png");
        menuButton.setVisibility(HIDDEN);
        menuButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                menu.showNextTo(menuButton,"bottom");
            }
        });
        menuButton.draw();

        /*
             For whatever reason, I just couldn't manage to get the mousemove event handler
             installed at the widget level --- doing so results in a VM crash.
         */
        installMouseMove(Document.get().getBody());
    }

    private Rect boundingBox = new Rect();

    /**
     * OnMouseMove event handler.
     * Controls the position and visibility of the context menu button. 
     */
    private void onMouseMove(NativeEvent ev) {
        Element e = Element.as(ev.getEventTarget());
        int mx = e.getAbsoluteLeft();
        int my = e.getAbsoluteTop();

        String u = e.getAttribute("u");
        if (u==null || u.length()==0) {// getAttribute seems to return "" instead of null.
            if (!boundingBox.contains(mx,my))
                menuButton.setVisibility(HIDDEN);
            return;
        }

        int w = e.getOffsetWidth();

        // show the menu button right next to the token in the source view.
        menuButton.setVisibility(VISIBLE);
        menuButton.setLeft(mx+w);
        menuButton.setTop(my-(menuButton.getOffsetHeight()-e.getOffsetHeight())/2);

        // if the mouse leaves the bounding box around the token and the menu button, hide the button.
        boundingBox = new Rect(e);
        boundingBox.fatten(16);
        boundingBox.x2 += w; // should include the button
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

        menuButton.setVisibility(HIDDEN);

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
