package org.jvnet.sorcerer.util;

import java.util.HashMap;

/**
 * Bi-directional map.
 * @author Kohsuke Kawaguchi
 */
public class BiDiMap<K,V> {
    private final HashMap<K,V> map = new HashMap<K,V>();
    private final HashMap<V,K> rev = new HashMap<V,K>();

    public void put(K k,V v) {
        map.put(k,v);
        rev.put(v,k);
    }

    public boolean containsValue(V v) {
        return rev.containsKey(v);
    }

    public V get(K k) {
        return map.get(k);
    }
}
