package org.jvnet.sorcerer;

/**
 * Interface for Visitors that walk the Tag tree.
 */
public interface TagVisitor {

    public void visit(Tag.ReservedWord reservedWord);

    public void visit(Tag.Comment comment);

    public void visit(Tag.CurlyBracket curlyBracket);

    public void visit(Tag.Parenthesis parenthesis);

    public void visit(Tag.Killer killer);

    public void visit(Tag.SourceText sourceText);

    public void visit(Tag.Literal literal);

    public void visit(Tag.DeclName declName);

    public void visit(Tag.ClassDecl classDecl);

    public void visit(Tag.TypeRef typeRef);

    public void visit(Tag.FieldDecl fieldDecl);

    public void visit(Tag.FieldRef fieldRef);

    public void visit(Tag.LocalVarDecl localVarDecl);

    public void visit(Tag.LocalVarRef localVarRef);

    public void visit(Tag.MethodDecl methodDecl);

    public void visit(Tag.MethodRef methodRef);

    public void visit(Tag.Root root);
}
