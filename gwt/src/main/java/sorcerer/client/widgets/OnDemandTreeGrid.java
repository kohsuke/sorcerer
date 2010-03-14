package sorcerer.client.widgets;

import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.FolderOpenedEvent;
import com.smartgwt.client.widgets.tree.events.FolderOpenedHandler;

/**
 * {@link TreeGrid} that loads nodes on demand.
 *
 * @author Kohsuke Kawaguchi
 */
public class OnDemandTreeGrid extends TreeGrid2 {
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
    }
}
