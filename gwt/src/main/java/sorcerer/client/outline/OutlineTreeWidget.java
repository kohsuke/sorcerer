package sorcerer.client.outline;

import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import sorcerer.client.Application;
import sorcerer.client.data.AST;

/**
 * Outline tree view.
 *
 * @author Kohsuke Kawaguchi
 */
public class OutlineTreeWidget extends TreeGrid {
    private Tree tree;
    private TreeNode root;

    public OutlineTreeWidget() {
        setHeight100();
        setWidth100();
        setShowHeader(false);
        setShowSelectedStyle(false);
        setShowOpenIcons(false);
        setLeaveScrollbarGap(false);
        setData(tree = new Tree());
        tree.setShowRoot(false);
        tree.setRoot(root = new TreeNode());
    }

    public void load(AST ast) {
        root.setChildren(new TreeNode[0]);
        ast.accept(new OutlineBuilder(tree));
    }

    public static OutlineTreeWidget get() {
        return Application.get().getOutlineView();
    }
}
