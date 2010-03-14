package sorcerer.client.data;

import sorcerer.client.linker.Linker;

/**
 * @author Kohsuke Kawaguchi
 */
public class Variable extends TableItem {
    public final String name;
    private final String href;

    public Variable(LocalVariableEntry e) {
        super("lv");
        this.name = e.name();
        this.href = '#'+e.id();
    }

    @Override
    public Kind kind() {
        return Kind.LOCALVARIABLE;
    }

    @Override
    public String href() {
        // TODO: return linker.href(this);
        return "TODO";
    }

    @Override
    public String usageKey() {
        return href;
    }

    @Override
    public String displayText() {
        return name;
    }
}
