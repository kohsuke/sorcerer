package sorcerer.client.outline;

import com.google.gwt.core.client.JsArrayInteger;
import com.smartgwt.client.widgets.tree.Tree;
import sorcerer.client.data.ASTVisitor;
import sorcerer.client.data.Field;
import sorcerer.client.data.Method;
import sorcerer.client.data.TableItem;
import sorcerer.client.data.Type;
import sorcerer.client.data.Variable;
import sorcerer.client.js.JsFunction;

/**
 * @author Kohsuke Kawaguchi
 */
public class OutlineBuilder extends ASTVisitor {
    private final Tree tree;
    private OutlineNode node;

    /**
     * If we are visiting inside a {@link Type}, set to that type.
     */
    private Type currentType;

    public OutlineBuilder(Tree tree) {
        this.tree = tree;
    }

    @Override
    public void reservedWord(String name) {
        if (name.equals("public") || name.equals("protected") || name.equals("private")) {
            node.setAccess(name);
        } else if (name.equals("static")) {
            node.isStatic = true;
        }
    }

    /**
     * Creates a new tree node for each declaration.
     */
    private void decl(TableItem type, String kind, boolean isLocal, JsFunction children) {
        OutlineNode parent = node;
        node = new OutlineNode(type.outlineTitle(),kind,isLocal);
        tree.add(node, parent!=null ? parent : tree.getRoot());

        children.invoke();

        if (parent==null) // open top-most nodes
            tree.openFolder(node);
        node = parent;
    }

    @Override
    public void typeDef(Type t, JsArrayInteger descendants, JsFunction children) {
        Type old = currentType;
        currentType = t;
        decl(t,t.getType(),false,children);
        currentType = old;
    }

    @Override
    public void methodDef(Method m, JsArrayInteger superMethods, JsArrayInteger subMethods, JsFunction children) {
        decl(m,"method",true,children);
    }

    @Override
    public void fieldDef(String name, JsFunction children) {
        decl(new Field(currentType,name),"field",true,children);
    }

    //
// No-ops
//
    @Override
    public void whitespace(int n) {
    }

    @Override
    public void nl() {
    }

    @Override
    public void parenthesis(JsFunction children) {
        children.invoke();
    }

    @Override
    public void literal(String s) {
    }

    @Override
    public void curlyBrace(JsFunction children) {
        children.invoke();
    }

    @Override
    public void comment(String head, JsFunction children) {
    }

    @Override
    public void primitiveType(String name) {
    }

    @Override
    public void sourceText(String text) {
    }

    @Override
    public void typeRef(Type t) {
    }

    @Override
    public void methodRef(Method m) {
    }

    @Override
    public void variableRef(Variable v) {
    }

    @Override
    public void fieldRef(Type owner, String modifiers, String name) {
    }

    @Override
    public void identifier(String text) {
    }

    @Override
    public void variableDef(Variable v, JsFunction children) {
    }
}
