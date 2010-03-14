package sorcerer.client.linker;

import sorcerer.client.data.Field;
import sorcerer.client.data.Method;
import sorcerer.client.data.Type;

/**
 * Determines where the definition of a symbol exists.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Linker {
    String href(Type t);
    String href(Method m);
    String href(Field f);
}
