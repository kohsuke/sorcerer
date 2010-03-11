package sorcerer.client.data;

import com.google.gwt.core.client.JsArray;

import java.util.ArrayList;
import java.util.List;

/**
 * To make the generated JavaScript compact, type, method, and variable are defined once
 * and referenced by their indices.
 *
 * @author Kohsuke Kawaguchi
 */
public class Table {
    public final List<Type> types = new ArrayList<Type>();
    public final List<Method> methods = new ArrayList<Method>();
    public final List<Variable> variables = new ArrayList<Variable>();

    public final Type type(int index) {
        return types.get(index);
    }

    public final Method method(int index) {
        return methods.get(index);
    }

    public final Variable variable(int index) {
        return variables.get(index);
    }
    
    public void typeTable(JsArray<TypeEntry> types) {
        this.types.clear();
        for (int i=0; i<types.length(); i++)
            this.types.add(new Type(types.get(i)));
    }

    public void methodTable(JsArray<MethodEntry> methods) {
        this.methods.clear();
        for (int i=0; i<methods.length(); i++)
            this.methods.add(new Method(this,methods.get(i)));
    }

    public void localVariableTable(JsArray<LocalVariableEntry> variables) {
        this.variables.clear();
        for (int i=0; i<variables.length(); i++)
            this.variables.add(new Variable(variables.get(i)));
    }

}
