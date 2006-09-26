package org.jvnet.sorcerer;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Kohsuke Kawaguchi
 */
public class KeyWordFactoryGen {
    public static void main(String[] args) {
        for (Map.Entry<String,String> e : new TreeMap<String,String>(ReservedWords.TOKENMAP).entrySet()) {
            System.out.printf("f.%s = f.primitive('%s');\n", e.getValue(), e.getKey());
        }
    }
}
