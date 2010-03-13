package sorcerer.client.pkg;

import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.LeafClickEvent;
import com.smartgwt.client.widgets.tree.events.LeafClickHandler;
import sorcerer.client.data.pkg.Klass;

/**
 * Represents a type in a package tree.
 *
 * @author Kohsuke Kawaguchi
 */
class KlassNode extends TreeNode implements LeafClickHandler {
    private final Klass klass;

    public KlassNode(Klass k) {
        super(k.shortName());
        this.klass = k;
        setIcon(k.kind()+'_'+k.access()+".gif");
    }

    public void onLeafClick(LeafClickEvent event) {
        klass.show();
    }
}
