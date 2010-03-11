package sorcerer.client;

import sorcerer.client.data.Type;

/**
 * Determines where the definition of a symbol exists.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Linker {
    String type(Type t);
}
