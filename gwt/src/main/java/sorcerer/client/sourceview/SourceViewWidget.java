package sorcerer.client.sourceview;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.InlineLabel;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.events.HoverHandler;
import com.smartgwt.client.widgets.events.MouseMoveEvent;
import com.smartgwt.client.widgets.events.MouseMoveHandler;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import sorcerer.client.Application;
import sorcerer.client.data.AST;
import sorcerer.client.js.Iterables;
import sorcerer.client.js.JsArray;
import sorcerer.client.source.SourceBuilder;

import static sorcerer.client.js.Iterables.$A;

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
        // I just can't seem to get the mousemove event handler installed at the component level
        // and access he necessary detail information
        addMouseMoveHandler(new MouseMoveHandler() {
            public void onMouseMove(MouseMoveEvent ev) {
                Event e = unwrap(ev);
                Window.setTitle(test(e));
//                Element t = e.getTarget();
//                if (t==null) {
//                    return;
//                }
//                String msg = t.getInnerHTML();
//                Window.setTitle(t.getTagName()+":"+msg.substring(0,Math.min(16,msg.length())));
            }
        });
    }

    private native Event unwrap(MouseMoveEvent ev) /*-{
        return ev.@com.smartgwt.client.event.AbstractSmartEvent::jsObj;
    }-*/;

    private native String test(Event ev) /*-{
        return "ev="+ev+" target="+ev.target + " X="+ev.clientX;
    }-*/;


//    @Override
//    protected void onLoad() {
//        super.onLoad();
//        listenMouseMove(getElement());
//    }
//
//    private void onMouseMove(Event ev) {
//        Window.setTitle("X="+ev.getClientX()+" Y="+ev.getClientY());
//    }
//
//    private native void listenMouseMove(Element e) /*-{
//        var self = this;
//        e.addEventListener("mousemove",function (ev) {
//            $doc.title = "foobar";
//            self.@sorcerer.client.sourceview.SourceViewWidget::onMouseMove(Lcom/google/gwt/user/client/Event;)(ev);
//        },false);
//    }-*/;

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

        /*
        menu = new Menu();
        menu.setShowShadow(true);
        menu.setShadowDepth(10);

        menu.setItems(
            new MenuItem("<a href='#'>Permalink</a>"),
            new MenuItem("Find Usage")
        );

        menuButton = new IMenuButton("MenuButton",menu);
        menuButton.draw();
*/

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
