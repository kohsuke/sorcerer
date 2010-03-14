package sorcerer.client.data;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * @author Kohsuke Kawaguchi
 */
public class Field extends TableItem {
    public final Type owner;
    public final String name;

    public Field(TableItem owner,String name) {
        super("fi");
        if (owner instanceof Method)
            // TODO: when can this happen?
            owner = ((Method)owner).owner;
        this.owner = (Type)owner;
        this.name = name;
    }

    private static native String getIdentifier(JavaScriptObject o) /*-{
        return o.identifier ? o.identifier : null;
    }-*/;

    @Override
    public Kind kind() {
        return Kind.FIELD;
    }

    @Override
    public String href() {
        // TODO
        return "TODO";
    }

    @Override
    public String usageKey() {
        return owner.usageKey()+'#'+name;
    }

    @Override
    public String displayText() {
        return name;
    }
}
