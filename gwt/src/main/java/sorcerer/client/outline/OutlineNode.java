package sorcerer.client.outline;

import com.google.gwt.user.client.History;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.NodeClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeClickHandler;
import sorcerer.client.data.TableItem;

/**
 * {@likn TreeNode} in {@link OutlineTreeWidget}.
 *
 * @author Kohsuke Kawaguchi
 */
public class OutlineNode extends TreeNode implements NodeClickHandler {
    public boolean isStatic;

    private final String kind;
    private final boolean isLocal;
    private final String href;

    /**
     * @param decl
     *      The declaration to be wrapped into a node.
     */
    public OutlineNode(TableItem decl, String href, boolean local) {
        super();
        this.kind = decl.kind().toLowerCase();
        this.isLocal = local;
        this.href = href;
        setAccess("default"); // unless later overridden otherwise
        setTitle("<a href='"+href+"'>"+decl.outlineTitle()+"</a>");
    }

    public void setAccess(String access) {
        setIcon(kind+'_'+access+".gif");
    }

    public void onNodeClick(NodeClickEvent event) {
        History.newItem(href.substring(1),true); // trim off '#'
    }
}
