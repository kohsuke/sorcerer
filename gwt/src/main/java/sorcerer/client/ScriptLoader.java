package sorcerer.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ScriptElement;

/**
 * Loads JavaScript asynchronously.
 *
 * @author Kohsuke Kawaguchi
 */
public class ScriptLoader {
    private static final Document doc = Document.get();
    private static final Element head = doc.getElementsByTagName("head").getItem(0);

    public static void load(String href) {
        ScriptElement s = doc.createScriptElement();
        s.setSrc(href);
        head.appendChild(s);
    }
}
