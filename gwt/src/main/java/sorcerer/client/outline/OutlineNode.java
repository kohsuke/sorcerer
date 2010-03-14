package sorcerer.client.outline;

import com.smartgwt.client.widgets.tree.TreeNode;

/**
 * @author Kohsuke Kawaguchi
 */
public class OutlineNode extends TreeNode {
    public boolean isStatic;

    private final String kind;
    private final boolean isLocal;

    public OutlineNode(String title, String kind, boolean local) {
        super(title);
        this.kind = kind;
        this.isLocal = local;
    }

    public void setAccess(String access) {
        setIcon(kind+'_'+access+".gif");
    }
}
