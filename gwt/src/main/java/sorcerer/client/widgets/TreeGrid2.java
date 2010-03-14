package sorcerer.client.widgets;

import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.LeafClickEvent;
import com.smartgwt.client.widgets.tree.events.LeafClickHandler;
import com.smartgwt.client.widgets.tree.events.NodeClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeClickHandler;

/**
 * {@link TreeGrid} with more object-oriented dispatching behavior.
 *
 * @author Kohsuke Kawaguchi
 */
public class TreeGrid2 extends TreeGrid {
    public TreeGrid2() {
        addLeafClickHandler(new LeafClickHandler() {
            public void onLeafClick(LeafClickEvent event) {
                TreeNode l = event.getLeaf();
                if (l instanceof LeafClickHandler)
                    ((LeafClickHandler)l).onLeafClick(event);
            }
        });

        addNodeClickHandler(new NodeClickHandler() {
            public void onNodeClick(NodeClickEvent event) {
                TreeNode n = event.getNode();
                if (n instanceof NodeClickHandler)
                    ((NodeClickHandler)n).onNodeClick(event);
            }
        });
    }
}
