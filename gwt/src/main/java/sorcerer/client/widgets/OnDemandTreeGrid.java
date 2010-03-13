package sorcerer.client.widgets;

import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.FolderOpenedEvent;
import com.smartgwt.client.widgets.tree.events.FolderOpenedHandler;
import com.smartgwt.client.widgets.tree.events.LeafClickEvent;
import com.smartgwt.client.widgets.tree.events.LeafClickHandler;

/**
 * {@link TreeGrid} that loads nodes on demand.
 *
 * @author Kohsuke Kawaguchi
 */
public class OnDemandTreeGrid extends TreeGrid {
    public OnDemandTreeGrid() {
        // dispatch events in more object-oriented manner
        addFolderOpenedHandler(new FolderOpenedHandler() {
            public void onFolderOpened(FolderOpenedEvent event) {
                TreeNode n = event.getNode();
                if (n instanceof OnDemandTreeNode) {
                    ((OnDemandTreeNode)n).loadOnDemand();
                }
            }
        });

        addLeafClickHandler(new LeafClickHandler() {
            public void onLeafClick(LeafClickEvent event) {
                TreeNode l = event.getLeaf();
                if (l instanceof LeafClickHandler)
                    ((LeafClickHandler)l).onLeafClick(event);
            }
        });
    }
}
