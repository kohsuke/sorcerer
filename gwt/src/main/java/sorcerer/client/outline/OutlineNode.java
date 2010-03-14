package sorcerer.client.outline;

import com.smartgwt.client.widgets.tree.TreeNode;
import sorcerer.client.data.TableItem;

/**
 * {@likn TreeNode} in {@link OutlineTreeWidget}.
 *
 * @author Kohsuke Kawaguchi
 */
public class OutlineNode extends TreeNode {
    public boolean isStatic;

    private final String kind;
    private final boolean isLocal;

    /**
     * @param decl
     *      The declaration to be wrapped into a node.
     */
    public OutlineNode(TableItem decl, String href, boolean local) {
        super();
        this.kind = decl.kind().toLowerCase();
        this.isLocal = local;
        setAccess("default"); // unless later overridden otherwise
        setTitle("<a href='"+href+"'>"+decl.outlineTitle()+"</a>");
    }

    public void setAccess(String access) {
        setIcon(kind+'_'+access+".gif");
    }
}
