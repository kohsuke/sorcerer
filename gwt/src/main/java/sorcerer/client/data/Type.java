package sorcerer.client.data;

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

    public String fullDisplayName() {
        return binaryName.replace('$','.');
    }

    @Override
    public String kind() {
        return "type";
    }

    @Override
    public String href() {
        // TODO: it'd be nice if the source view page can be loaded on its own.
        // the way it's done today requires package view to be loaded.
//        t.linker = window.top.packageView.main.linker.get(t.packageName);
        // YAHOO.log("linker for ["+t.packageName+"] is "+t.linker.name());

//        t.href = t.linker.type(t);
        // TODO
        return "TODO";
    }

    @Override
    public String usageKey() {
        return binaryName+"#this";
    }

    public String getType() {
        if(this.css.contains("cl"))  return "class";
        if(this.css.contains("an"))  return "annotation";
        if(this.css.contains("en"))  return "enum";
        if(this.css.contains("it"))  return "interface";
        return null;
    }
}
