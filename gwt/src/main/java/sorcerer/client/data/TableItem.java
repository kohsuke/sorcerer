package sorcerer.client.data;

import sorcerer.client.linker.Linker;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class TableItem {
    /**
     * Tag that identifies the type of this declaration.
     */
    public abstract Kind kind();

    /**
     * CSS classes to be used for referencing this method
     */
    public final String css;

    /**
     * Hyperlink to the definition.
     */
    public abstract String href();

    protected TableItem(String css) {
        this.css = css;
    }

    /**
     * Computes the "find usage" index key.
     */
    public abstract String usageKey();

    /**
     * Computes the text to be displayed in the source view.
     */
    public abstract String displayText();

    /**
     * Compute the text to be displayed in the outline view
     */
    public String outlineTitle() {
        return displayText();
    }
}
