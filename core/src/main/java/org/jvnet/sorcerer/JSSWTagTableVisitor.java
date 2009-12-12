package org.jvnet.sorcerer;

import java.util.Set;

/**
 * Visitor that walks the Tag tree and populates the symbol tables
 * in a JavaScriptStreamWriter.
 */
public class JSSWTagTableVisitor implements TagVisitor {

    private final JavaScriptStreamWriter javaScriptStreamWriter;

    public JSSWTagTableVisitor(JavaScriptStreamWriter javaScriptStreamWriter) {
        this.javaScriptStreamWriter = javaScriptStreamWriter;
    }

    private void collectSymbols(Tag tag) {
        for (Tag t : tag) {
            t.collectSymbols(this);
        }
    }

    public void visit(Tag.ReservedWord reservedWord) {
        collectSymbols(reservedWord);
    }

    public void visit(Tag.Comment comment) {
        collectSymbols(comment);
    }

    public void visit(Tag.CurlyBracket curlyBracket) {
        collectSymbols(curlyBracket);
    }

    public void visit(Tag.Parenthesis parenthesis) {
        collectSymbols(parenthesis);
    }

    public void visit(Tag.Killer killer) {
        // noop
    }

    public void visit(Tag.SourceText sourceText) {
        collectSymbols(sourceText);
    }

    public void visit(Tag.Literal literal) {
        collectSymbols(literal);
    }

    public void visit(Tag.DeclName declName) {
        collectSymbols(declName);
    }

    public void visit(Tag.ClassDecl classDecl) {
        javaScriptStreamWriter.collect(classDecl.type);
        for (ParsedType d : classDecl.descendants)
            javaScriptStreamWriter.collect(d.element);
        collectSymbols(classDecl);
    }

    public void visit(Tag.TypeRef typeRef) {
        javaScriptStreamWriter.collect(typeRef.type);
    }

    public void visit(Tag.FieldDecl fieldDecl) {
        collectSymbols(fieldDecl);
    }

    public void visit(Tag.FieldRef fieldRef) {
        javaScriptStreamWriter.collect(fieldRef.decl.getEnclosingElement());
    }

    public void visit(Tag.LocalVarDecl localVarDecl) {
        collectSymbols(localVarDecl);
        javaScriptStreamWriter.collect(localVarDecl.var);
    }

    public void visit(Tag.LocalVarRef localVarRef) {
        collectSymbols(localVarRef);
    }

    public void visit(Tag.MethodDecl methodDecl) {
        javaScriptStreamWriter.collect(methodDecl.method);
        collectSet(methodDecl.overridden);
        collectSet(methodDecl.overriding);
        collectSymbols(methodDecl);
    }

    private void collectSet(Set<ParsedType.Match> ms) {
        for (ParsedType.Match m : ms)
            javaScriptStreamWriter.collect(m.method);
    }

    public void visit(Tag.MethodRef methodRef) {
        javaScriptStreamWriter.collect(methodRef.method);
    }

    public void visit(Tag.Root root) {
        collectSymbols(root);
    }
}
