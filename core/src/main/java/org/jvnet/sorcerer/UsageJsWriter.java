package org.jvnet.sorcerer;

import com.sun.source.util.TreePath;

import javax.lang.model.element.Element;
import java.util.Map;
import java.util.Set;

/**
 * Writes out a JSON file that captures the usage of a program element.
 *
 * <p>
 * Generated for each class separately.
 *
 * @author Kohsuke Kawaguchi
 */
public class UsageJsWriter {
    protected final ParsedSourceSet owner;
    protected final ParsedType type;
    protected final Map<Element,Set<TreePath>> referers;

    public UsageJsWriter(ParsedSourceSet owner, ParsedType type) {
        this.owner = owner;
        this.type = type;
        referers = type.findReferers();
    }


}
