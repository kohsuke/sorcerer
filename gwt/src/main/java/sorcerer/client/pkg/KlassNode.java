package sorcerer.client.pkg;

import com.google.gwt.user.client.History;
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
        super("<a href='#"+k.fullName()+"'>"+k.shortName()+"</a>");
        this.klass = k;
        setIcon(k.kind()+'_'+k.access()+".gif");
    }

    public void onLeafClick(LeafClickEvent event) {
        History.newItem(klass.fullName(),true);
    }
}
