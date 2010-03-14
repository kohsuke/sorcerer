package sorcerer.client.data;

/**
 * @author Kohsuke Kawaguchi
 */
public enum Kind {
    METHOD, FIELD, LOCALVARIABLE, CLASS, ANNOTATION, INTERFACE, ENUM;

    public String toLowerCase() {
        return name().toLowerCase();
    }
}
