package sorcerer.client.data;

import com.google.gwt.core.client.JsArrayInteger;
import sorcerer.client.js.JsFunction;

/**
 * Visits the Java source file AST.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class ASTVisitor extends Table {
    /**
     * Appearance of a reserved word.
     */
    public abstract void reservedWord(String word);

    /**
     * Reference to the primitive type.
     */
    public abstract void primitiveType(String name);

    /**
     * Other text in the source code.
     */
    public abstract void sourceText(String text);

    /**
     * Comment
     *
     * @param head
     *      String that represents the beginning of the comment. This can be used to distinguish the type of comment.
     */
    public abstract void comment(String head, JsFunction children);

    /**
     * Whitespace.
     *
     * @param n
     *      Number of consecutive whitespace characters.
     */
    public abstract void whitespace(int n);

    /**
     * Newline.
     */
    public abstract void nl();

    /**
     * Identifier in method/class definition.
     *
     * TODO: frontend generates I(I("foo")).
     */
    public abstract void identifier(String text);

    /**
     * Curly bracket pair "{...}"
     */
    public abstract void curlyBrace(JsFunction children);

    /**
     * Parenthesis "(...)"
     */
    public abstract void parenthesis(JsFunction children);

    /**
     * Type definition.
     */
    public abstract void typeDef(Type t, JsArrayInteger descendants, JsFunction children);

    /**
     * Type reference. Just the simple name.
     */
    public abstract void typeRef(Type t);

    /**
     * Method definition.
     *
     * @param superMethods
     *      Methods that this definition overrides. Can be empty.
     * @param subMethods
     *      Methods that overrides this definition. Can be empty.
     */
    public abstract void methodDef(Method m, JsArrayInteger superMethods, JsArrayInteger subMethods, JsFunction children);

    /**
     * Method reference.
     */
    public abstract void methodRef(Method m);

    /**
     * Local variable definition.
     */
    public abstract void variableDef(Variable v, JsFunction children);

    /**
     * Local variable reference.
     */
    public abstract void variableRef(Variable v);

    /**
     * Field definition.
     */
    public abstract void fieldDef(String name, JsFunction children);

    /**
     * Field reference.
     */
    public abstract void fieldRef(Type owner, String modifiers, String name);

    /**
     * Literal
     */
    public abstract void literal(String s);



/*
    Index-based def/ref callbacks
 */

    /**
     * Type definition.
     */
    public final void typeDef(int index, JsArrayInteger descendants, JsFunction children) {
        typeDef(type(index),descendants,children);
    }

    /**
     * Type reference. Just the simple name.
     */
    public final void typeRef(int index) {
        typeRef(type(index));
    }

    /**
     * Method definition.
     *
     * @param superMethods
     *      Methods that this definition overrides. Can be empty.
     * @param subMethods
     *      Methods that overrides this definition. Can be empty.
     */
    public final void methodDef(int index, JsArrayInteger superMethods, JsArrayInteger subMethods, JsFunction children) {
        methodDef(method(index),superMethods,subMethods,children);
    }

    /**
     * Method reference.
     */
    public final void methodRef(int index) {
        methodRef(method(index));
    }

    /**
     * Local variable definition.
     */
    public final void variableDef(int index, JsFunction children) {
        variableDef(variable(index),children);
    }

    /**
     * Local variable reference.
     */
    public final void variableRef(int index) {
        variableRef(variable(index));
    }

    public final void fieldRef(int typeIndex, String modifiers, String name) {
        fieldRef(type(typeIndex),modifiers,name);
    }
}
