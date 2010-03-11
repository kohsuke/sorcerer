package sorcerer.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Implementation of the lazy data loading pattern.
 *
 * <p>
 * Conceptually, this is one big map from K to V, where data is stored separately in different JavaScript
 * files. Retrieval would trigger lazy loading of these files, and when the data is ready a callback is invoked.
 *
 * <p>
 * Implementation-wise, each JavaScript file that stores data has to immediately callback the system to report
 * the loaded data, like the following, where 'foo' is subtype-specific. It is possible for one fragment JavaScript
 * to define multiple key/value pairs.
 *
 * <pre>
 * foo(key,value);
 * </pre>
 *
 * <p>
 * The 'foo' call above should be defined separately by each subtype of this class, and it should
 * call into {@link #onLoaded(Object, Object)} method.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class LazyDataLoader<K,V> {
    private final Document doc = Document.get();
    private final Element head = doc.getElementsByTagName("head").getItem(0);

    private HashMap<K,V> loaded = new HashMap<K,V>();
    private HashMap<K, List<Callback<V>>> callbacks = new HashMap<K, List<Callback<V>>>();

    /**
     * Loads the data indicated by the given key and call the callback when ready.
     */
    public void retrieve(K key, Callback<V> callback) {
        V v = loaded.get(key);
        if (v!=null) { // already loaded
            callback.call(v);
        } else {
            List<Callback<V>> l = this.callbacks.get(key);
            if (l==null) {
                // first time we request this. initiate a load
                l = new ArrayList<Callback<V>>();
                callbacks.put(key,l);
                head.appendChild(doc.createScriptElement(href(key)));
            }
            l.add(callback);
        }
    }

    /**
     * Given the key, figure out where to load the data.
     */
    protected abstract String href(K key);

    /**
     * Loaded script should call this method to report that the data is ready.
     */
    protected void onLoaded(K key, V value) {
        loaded.put(key,value);
        List<Callback<V>> l = callbacks.remove(key);
        if (l==null)    return; // no one is waiting

        for (Callback<V> cb : l)
            cb.call(value);
    }

    public interface Callback<V> {
        void call(V value);
    }
}
