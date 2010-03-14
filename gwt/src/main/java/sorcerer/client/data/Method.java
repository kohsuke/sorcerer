package sorcerer.client.data;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import sorcerer.client.js.JsArray;

/**
 * @author Kohsuke Kawaguchi
 */
public class Method extends TableItem {
    /**
     * Type in which this method is declared.
     */
    public final Type owner;

    /**
     * Short method name.
     */
    public final String name;

    /**
     * Array of strings that represent erased parameter type names in FQCN.
     */
    public final JsArrayString params = (JsArrayString)JavaScriptObject.createArray();


    public Method(Table table, MethodEntry e) {
        super(e.css());

        this.owner = table.type(e.owner());
        this.name = e.name();

        JsArray params = e.params();
        for(int j=0; j< params.length();j++) {
            String s = asString(params,j);
            if (s!=null)    this.params.push(s);
            else            this.params.push(table.type(asInt(params,j)).fullDisplayName());
        }
    }

    private static native String asString(JsArray a, int index) /*-{
        var o = a[index];
        return (typeof o == "string") ? o : null;
    }-*/;

    private static native int asInt(JsArray a, int index) /*-{
        var o = a[index];
        return (typeof o == "number") ? o : null;
    }-*/;


    @Override
    public Kind kind() {
        return Kind.METHOD;
    }

    @Override
    public String href() {
        // TODO
        return "TODO";
    }

    @Override
    public String usageKey() {
        return owner.binaryName+'#'+signature();
    }

    @Override
    public String displayText() {
        return name;
    }

    @Override
    public String outlineTitle() {
        String s = this.name+'(';
        for(int i=0; i<this.params.length(); i++) {
          if(i!=0) s+=',';
          s+=shortName(params.get(i));
        }
        s+=')';
        return s;
    }

    private String shortName(String s) {
        return s.substring(s.lastIndexOf('.')+1);
    }

    /**
     * Compute the method signature of the form "methodName(param1,param2,...)"
     */
    public String signature() {
        String s = name+'(';
        for(int i=0; i<params.length(); i++) {
          if(i!=0) s+=',';
          s+=params.get(i);
        }
        s+=')';
        return s;
    }
}
