package sorcerer.client.source;

import com.google.gwt.core.client.JsArrayInteger;
import sorcerer.client.data.ASTVisitor;
import sorcerer.client.data.Field;
import sorcerer.client.data.Method;
import sorcerer.client.data.TableItem;
import sorcerer.client.data.Type;
import sorcerer.client.data.Variable;
import sorcerer.client.js.JsArray;
import sorcerer.client.js.JsFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates the annotated source code.
 *
 * @author Kohsuke Kawaguchi
 */
public class SourceBuilder extends ASTVisitor {
    /**
     * HTML fragments to be joined together to form the whole HTML.
     */
    private final JsArray<String> html = JsArray.create();

    /**
     * Current line number.
     */
    private int lineNumber = 1;

    /**
     * End tags that needs to be inserted to be well-formed.
     */
    private final JsArray<String> endTags = JsArray.create();

    private final Map<Integer/*line number*/,JsArray<Bookmark>> bookmarks = new HashMap<Integer, JsArray<Bookmark>>();

    private TableItem currentDecl; // TODO: what's the proper type?

    /**
     * ID generator.
     */
    private int iota = 0;

    public SourceBuilder() {
        html.push("<pre>");
    }

    /*
       Formatting helper methods
       -------------------------
    */
    private SourceBuilder appendText(String html) {
        this.html.push(html);
        return this;
    }

    private SourceBuilder pop() {
        html.push(endTags.pop());
        return this;
    }

    private SourceBuilder css(String clazz) {
        return css(clazz,null);
    }

    private SourceBuilder css(String clazz, String usageKey) {
        String frag = "<span class='" + clazz;
        if (usageKey != null)
            frag += "' u='" + usageKey + "'>";
        else
            frag += "'>";
        html.push(frag);
        endTags.push("</span>");
        return this;
    }

    private SourceBuilder link(String href) {
        if (href != null) {
            html.push("<a href='" + href + "'>");
            endTags.push("</a>");
        }
        return this;
    }

    private SourceBuilder declLink(String href) {
        if (href != null) {
            html.push("<a href='" + href + "' id='" + href.substring(1) + "'>");
            endTags.push("</a>");
        }
        return this;
    }

    private SourceBuilder addBookmark(Bookmark b) {
        JsArray<Bookmark> l = bookmarks.get(lineNumber);
        if (l==null)    bookmarks.put(lineNumber,l = JsArray.create());
        l.push(b);
        return this;
    }

    /**
     * Generates an unique ID.
     */
    private String genId() {
        return "id"+(iota++);
    }

    /**
     * Recursively visits the children while setting the new {@link TableItem} to {@link #currentDecl}.
     */
    private void recurseDecl(TableItem t, JsFunction children) {
        TableItem old = currentDecl;
        currentDecl = t;
        children.invoke();
        currentDecl = old;
    }

    public String toHTML() {
        html.push("</pre>");
        return html.join("");
    }

/*
    Visitor methods
    -------------------------
 */

    @Override
    public void reservedWord(String word) {
        css("rw").appendText(word).pop();
    }

    @Override
    public void primitiveType(String name) {
        css("pr").appendText(name).pop();
    }

    @Override
    public void sourceText(String text) {
        appendText(text);
    }

    @Override
    public void comment(String head, JsFunction children) {
        String style;
        if(head.startsWith("//"))   style = "cs";
        else
        if(head.startsWith("/**"))  style = "cj";
        else                        style = "cm";

        css(style);
        children.invoke();
        pop();
    }

    @Override
    public void whitespace(int n) {
        while (n>8) {
            appendText(WS8);
            n -= 8;
        }
        if (n>0)    appendText(WHITESPACES.get(n));
    }

    @Override
    public void nl() {
        appendText("\r\n");
        lineNumber++;
    }

    @Override
    public void identifier(String text) {
        TableItem d = currentDecl;

        declLink(d.href());
        css(d.css+" d", d.usageKey());
        appendText(d.displayText());
        pop();
        pop();
    }

    @Override
    public void curlyBrace(JsFunction children) {
        paren("{","}",children);
    }

    @Override
    public void parenthesis(JsFunction children) {
        paren("(",")",children);
    }

    private void paren(String open, String close, JsFunction children) {
        String id = genId();

        html.push("<span id="+id+"L>"+open+"</span>");
        children.invoke();
        html.push("<span id="+id+"R>"+close+"</span>");

        /* TODO:
        lazyInitManager.add(function() {
          var highlighter = function(lhs,rhs) {
            lhs.onmouseover = function() {
              lhs.className="highlighted-bracket";
              rhs.className="highlighted-bracket";
            }
            lhs.onmouseout = function() {
              lhs.className="";
              rhs.className="";
            }
          };
          var open = document.getElementById(":"+lid);
          var closed = document.getElementById(":"+rid);
          highlighter(open,closed);
          highlighter(closed,open);
        */
    }

    @Override
    public void typeDef(Type t, JsArrayInteger descendants, JsFunction children) {
        if (descendants.length()>0)
            // TODO: addBookmark(bookmark.makeSubtype(context.me.types,descendants));
            ;

        recurseDecl(t, children);
    }

    @Override
    public void typeRef(Type t) {
        link(t.href()).css(t.css+" r",t.usageKey()).appendText(t.displayText()).pop().pop();
    }

    @Override
    public void methodDef(Method m, JsArrayInteger superMethods, JsArrayInteger subMethods, JsFunction children) {
        /* TODO:
        if(sup.length>0)
          context.addBookmark(bookmark.makeSuperMethods(context.me.methods,sup));
        if(sub.length>0)
          context.addBookmark(bookmark.makeSubMethods(  context.me.methods,sub));
        */
        recurseDecl(m,children);
    }

    @Override
    public void methodRef(Method m) {
        link(m.href()).css(m.css+" r",m.usageKey()).appendText(m.name).pop().pop();
    }

    @Override
    public void fieldDef(String name, JsFunction children) {
        recurseDecl(new Field(currentDecl,name),children);
    }

    @Override
    public void fieldRef(Type owner, String modifiers, String name) {
        /*TODO: link(t.linker.field(t,name)).*/ css(modifiers+" r",owner.fullDisplayName()+'#'+name).appendText(name).pop() /*TODO:.pop()*/;
    }

    @Override
    public void variableDef(Variable v, JsFunction children) {
        recurseDecl(v,children);
    }

    @Override
    public void variableRef(Variable v) {
        link(v.href()).css(v.css+" r",v.usageKey()).appendText(v.name).pop().pop();
    }

    @Override
    public void literal(String s) {
        css("lt").appendText(s).pop();
    }

    private static final JsArray<String> WHITESPACES = JsArray.create();
    private static final String WS8 = "        ";
    static {
        String s = "";
        for (int i=1; i<8; i++) {
            s = s + ' ';
            WHITESPACES.set(i,s);
        }
    }
}
