package sorcerer.client.widgets;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.widgets.tree.TreeNode;

/**
 * {@link TreeNode} that can load its children on-demand.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class OnDemandTreeNode extends TreeNode {
    private boolean loaded;

    protected OnDemandTreeNode() {
        setIsFolder(true);
    }

    protected OnDemandTreeNode(JavaScriptObject jsObj) {
        super(jsObj);
        setIsFolder(true);
    }

    protected OnDemandTreeNode(String name) {
        super(name);
        setIsFolder(true);
    }

    protected OnDemandTreeNode(String name, TreeNode... children) {
        super(name, children);
        setIsFolder(true);
    }
    
    protected void loadOnDemand() {
        if (!loaded) {
            load();
            setAttribute("isFolder",(Boolean)null);
            markAsLoaded();
        }
    }

    /**
     * Marks this node as "load completed", regardless of whether the on-demand loading has happend or not.
     */
    public void markAsLoaded() {
        loaded = true;
    }

    /**
     * Called to load children of this node.
     */
    protected abstract void load();

    public boolean isLoaded() {
        return loaded;
    }
}
