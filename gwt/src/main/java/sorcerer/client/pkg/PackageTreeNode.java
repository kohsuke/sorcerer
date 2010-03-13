package sorcerer.client.pkg;

import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeNode;
import sorcerer.client.LazyDataLoader.Callback;
import sorcerer.client.data.pkg.*;
import sorcerer.client.data.pkg.Package;
import sorcerer.client.js.JsArray;
import sorcerer.client.widgets.OnDemandTreeNode;

/**
 * {@link TreeNode} for packages.
 */
class PackageTreeNode extends OnDemandTreeNode {
    private final sorcerer.client.data.pkg.Package pkg;
    private final Tree tree;

    PackageTreeNode(Package pkg, Tree tree) {
        super(pkg.fullDisplayName());
        this.pkg = pkg;
        this.tree = tree;
        setIcon("package.gif");
    }

    protected void load() {
        ClassListLoader.INSTANCE.retrieve(pkg.fullName(),new Callback<JsArray<Klass>>() {
            public void call(JsArray<Klass> klasses) {
                for (Klass k : klasses.iterable())
                    tree.add(new KlassNode(k), PackageTreeNode.this);
                open();
            }
        });
    }

    public void open() {
        tree.openFolder(this);
    }
}
