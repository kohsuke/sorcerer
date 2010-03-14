package sorcerer.client.pkg;

import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeNode;
import sorcerer.client.data.pkg.Project;
import sorcerer.client.data.pkg.ProjectLoader;
import sorcerer.client.data.pkg.ProjectLoader.Listener;
import sorcerer.client.widgets.OnDemandTreeGrid;

/**
 * @author Kohsuke Kawaguchi
 */
public class PackageTreeWidget extends OnDemandTreeGrid {
    private Tree tree;
    private TreeNode root;
    private PackageTreeMode mode = PackageTreeMode.FLAT;

    public PackageTreeWidget() {
        setHeight100();
        setWidth100();
        setFolderIcon("icons/folder.png");
        setShowHeader(false);
        setShowSelectedStyle(false); // TODO: if we support this, we need to update this when user navigates to different class
        setShowOpenIcons(true);
        setLeaveScrollbarGap(false);
        setData(tree = new Tree());
        tree.setShowRoot(false);
        tree.setRoot(root = new TreeNode());

        ProjectLoader.INSTANCE.addListener(new Listener() {
            public void onChange(Project prj) {
                mode.load(prj,tree,root);
            }
        });
    }
}
