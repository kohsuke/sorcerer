package sorcerer.client.data;

import sorcerer.client.linker.Linker;
import sorcerer.client.linker.SorcererLinker;

/**
 * Represents a type in AST.
 * 
 * @author Kohsuke Kawaguchi
 */
public class Type extends TableItem {
    /**
     * Encoded name as written in .js
     */
    public final String binaryName;

    /**
     * The last part of the class name identifier. "" if this is anonymous class
     */
    public final String shortName;

    public final String packageName;

    /**
     * Linker to use for declarations in this type.
     * TODO: depending on where we load it, use the right instance.
     */
    public final Linker linker = SorcererLinker.INSTANCE;

    /**
     * Builds a richer in memory information about a type from {@link TypeEntry}
     */
    public Type(TypeEntry e) {
        super(e.css());
        this.binaryName = e.binaryName();

        int idx=binaryName.lastIndexOf('.');
        if(idx<0)
          packageName="";
        else
          packageName=binaryName.substring(0,idx);

        shortName=after(after(binaryName,"."),"$");
    }

    private static String after(String s, String sep) {
        return s.substring(s.lastIndexOf(sep)+sep.length());
    }

    public String displayText() {
        return shortName;
    }

    /**
     * Fully qualified class name all separated by '.' and not '$'.
     */
    public String fullDisplayName() {
        return binaryName.replace('$','.');
    }

    @Override
    public Kind kind() {
        if(this.css.contains("cl"))  return Kind.CLASS;
        if(this.css.contains("an"))  return Kind.ANNOTATION;
        if(this.css.contains("en"))  return Kind.ENUM;
        if(this.css.contains("it"))  return Kind.INTERFACE;
        throw new AssertionError(this.css);
    }

    @Override
    public String href() {
        return linker.href(this);
    }

    @Override
    public String usageKey() {
        return binaryName+"#this";
    }
}
