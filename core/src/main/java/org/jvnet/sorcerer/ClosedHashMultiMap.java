/*
 * @(#)$Id: ExpressionPool.java,v 1.28 2004/01/26 18:55:10 kohsuke Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */
package org.jvnet.sorcerer;

import java.util.Iterator;

/**
 * Multimap that associates multiple values per one key.
 *
 * <p>
 * This implementation is fairly efficient in memory usage, but it cannot
 * be updated and queried at the same time; all the updating must happen
 * before any querying starts, or else the iteration may fail unexpectedly.
 *
 * This code is copyrighted by Sun Microsystems, under the BSD license.
 */
abstract class ClosedHashMultiMap<K,V> {
    /** The hash table data. */
    private Object[] table;

    /** The total number of mappings in the hash table. */
    private int count;

    /**
     * The table is rehashed when its size exceeds this threshold.  (The
     * value of this field is (int)(capacity * loadFactor).)
     */
    private int threshold;

    /** The load factor for the hashtable. */
    private static final float loadFactor = 0.3f;
    private static final int initialCapacity = 31;

    public ClosedHashMultiMap() {
        table = new Object[initialCapacity];
        threshold = (int) (initialCapacity * loadFactor);
    }

    /**
     * Computes the key for the given value.
     */
    protected abstract K getKey(V value);

    /**
     * rehash.
     *
     * It is possible for one thread to call get method
     * while another thread is performing rehash.
     * Keep this in mind.
     */
    private void rehash() {
        // create a new table first.
        // meanwhile, other threads can safely access get method.
        int oldCapacity = table.length;
        Object[] oldMap = table;

        int newCapacity = oldCapacity * 2 + 1;
        Object[] newMap = new Object[newCapacity];

        for (int i = oldCapacity; i-- > 0;)
            if (oldMap[i] != null) {
                int index = (oldMap[i].hashCode() & 0x7FFFFFFF) % newMap.length;
                while (newMap[index] != null)
                    index = nextHash(index);
                newMap[index] = oldMap[i];
            }

        // threshold is not accessed by get method.
        threshold = (int) (newCapacity * loadFactor);
        // switch!
        table = newMap;
    }

    /**
     * Puts a new value into the hash.
     */
    public void put(V value) {
        if (count >= threshold)
            rehash();

        int index = (getKey(value).hashCode() & 0x7FFFFFFF) % table.length;

        while (table[index] != null)
            index = nextHash(index);
        table[index] = value;

        count++;
    }

    public void putAll( Iterable<? extends V> values ) {
        for (V v : values)
            put(v);
    }

    private int nextHash(int index) {
        index = (index + 1) % table.length;
        return index;
    }

    public Iterable<V> get(final K key) {
        return new Iterable<V>() {
            public Iterator<V> iterator() {
                return new Itr();
            }

            class Itr implements Iterator<V> {
                int index = (key.hashCode() & 0x7FFFFFFF) % table.length;

                public boolean hasNext() {
                    while(table[index]!=null && !getKey((V)table[index]).equals(key))
                        index = nextHash(index);
                    return table[index]!=null;
                }

                public V next() {
                    V r = (V)table[index];
                    index = nextHash(index);
                    return r;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            }
        };
    }
}
