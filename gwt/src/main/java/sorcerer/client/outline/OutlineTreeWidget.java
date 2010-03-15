package sorcerer.client.outline;

import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeNode;
import sorcerer.client.Application;
import sorcerer.client.data.AST;
import sorcerer.client.linker.SorcererLinker;
import sorcerer.client.widgets.TreeGrid2;

/**
 * Outline tree view.
 *
 * @author Kohsuke Kawaguchi
 */
public class OutlineTreeWidget extends TreeGrid2 {
    private Tree tree;
    private TreeNode root;
    private AST showing;

    public OutlineTreeWidget() {
        setHeight100();
        setWidth100();
        setShowHeader(false);
        setShowSelectedStyle(false);
        setShowOpenIcons(false);
        setLeaveScrollbarGap(false);
        setShowConnectors(true);
        setData(tree = new Tree());
        tree.setShowRoot(false);
        tree.setRoot(root = new TreeNode());
    }

    public void load(AST ast) {
        if (showing==ast)   return;
        showing = ast;

        root.setChildren(new TreeNode[0]);
        ast.accept(new OutlineBuilder(tree, new SorcererLinker()));
    }

    public static OutlineTreeWidget get() {
        return Application.get().getOutlineView();
    }
}
