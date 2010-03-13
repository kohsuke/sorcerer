package sorcerer.client.pkg;

import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeNode;
import sorcerer.client.data.pkg.Package;
import sorcerer.client.data.pkg.Project;

/**
 * @author Kohsuke Kawaguchi
 */
public enum PackageTreeMode {
    FLAT {
        @Override
        public void load(Project p, Tree tree, TreeNode parent) {
            buildTree(p.rootPackage(),tree,parent);
        }

        private void buildTree(final Package pkg, final Tree tree, TreeNode parent) {
            if(pkg.isLeaf())
                tree.add(new PackageTreeNode(pkg, tree),parent);
            for (Package c : pkg.children().iterable())
                buildTree(c,tree,parent);
        }
    },
    HIERARCHICAL {
        @Override
        public void load(Project p, Tree tree, TreeNode parent) {
            // TODO
            throw new UnsupportedOperationException();
        }
    };

    /**
     * Builds the package list into a tree structure and add them under the given parent node.
     */
    public abstract void load(Project p, Tree tree, TreeNode parent);

}
