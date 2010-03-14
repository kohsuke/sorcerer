package sorcerer.client.linker;

import sorcerer.client.data.Field;
import sorcerer.client.data.Method;
import sorcerer.client.data.Type;

/**
 * {@link Linker} within the same sorcerer, always producing "#..." as href.
 *
 * @author Kohsuke Kawaguchi
 */
public class SorcererLinker implements Linker {
    public String href(Type t) {
        return '#'+t.fullDisplayName();
    }

    public String href(Field f) {
        return href(f.owner)+'-'+f.name;
    }

    public String href(Method m) {
        return href(m.owner)+'-'+m.signature();
    }

    public static final Linker INSTANCE = new SorcererLinker();
}
