package org.jvnet.sorcerer.frame;

import com.sun.source.util.TreePath;
import org.jvnet.sorcerer.ParsedSourceSet;
import org.jvnet.sorcerer.ParsedType;
import org.jvnet.sorcerer.util.JsonWriter;
import org.jvnet.sorcerer.util.TreeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Writes out the "find usage" information of programming elements
 * defined on the given type.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClassUsageJsWriter extends AbstractWriter {
    public ClassUsageJsWriter(ParsedSourceSet pss) {
        super(pss);
    }

    public void write(ParsedType type, PrintWriter pw) {
        pw.println("setClassUsage('"+type.element.getQualifiedName()+"',");
        JsonWriter w = new JsonWriter(pw);
        w.startObject();
        for (Entry<Element, Set<TreePath>> e : type.findReferers().entrySet()) {
            w.key(getKeyName(type,e.getKey()));
            NodePkgInfo root = new NodePkgInfo("");

            // builds a top-down tree.
            for (TreePath t : e.getValue())
                root.add(t).leaves.add(t);

            // then write it out!
            root.write(w);
        }
        w.endObject();
        pw.println(");");
        pw.close();
    }

    protected String getKeyName(ParsedType referencedType, Element e) {
        if(e.equals(referencedType.element)) {
            return "this"; // special key that represents the type itself.
        } else {
            switch(e.getKind()) {
            case FIELD:
            case ENUM_CONSTANT:
                return e.getSimpleName().toString();
            case METHOD:
            case CONSTRUCTOR:
                return TreeUtil.getFullMethodName(pss.getTypes(),(ExecutableElement)e);
            default:
                throw new IllegalStateException(e.toString());
            }
        }
    }

    private final class NodeMap extends HashMap<Element,Node> {
        Node getOrCreate(Element e,TreePath t) {
            Node n = get(e);
            if(n==null)
                put(e,n=createNode(e,t));
            return n;
        }
    }

    private interface NodeMapOwner {
        NodeMap getChildren();
    }


    class NodePkgInfo extends PkgInfo<NodePkgInfo> implements NodeMapOwner {
        /**
         * Child {@link Node}s keyed by their {@link Node#element}.
         */
        final NodeMap children = new NodeMap();

        public NodePkgInfo(String name) {
            super(name);
        }

        /**
         * Adds the given {@link TreePath} to the {@link NodePkgInfo} tree rooted at this object.
         */
        protected Node add(TreePath t) {
            // enter the package portion
            NodePkgInfo leafPkg = super.add(TreeUtil.getPackageName(t.getCompilationUnit()));
            // then the rest
            NodeMapOwner leaf = addNode(leafPkg, t);
            return (Node)leaf;
        }

        public NodeMap getChildren() {
            return children;
        }

        public NodePkgInfo create(String name) {
            return new NodePkgInfo(name);
        }

        public void write(JsonWriter js) {
            super.write(js);
            if(!children.isEmpty())
                js.property("classes",children.values());
        }
    }

    /**
     * Adds the given {@link TreePath} to the {@link Node} tree
     * rooted at "root" node, then return the {@link Node} where
     * the {@link TreePath} is ultimately stored.
     */
    NodeMapOwner addNode(NodeMapOwner root, TreePath t) {
        NodeMapOwner p;
        if(t.getParentPath()!=null)
            p = addNode(root, t.getParentPath());
        else
            p = root;

        if(TreeUtil.OUTLINE_WORTHY_TREE.contains(t.getLeaf().getKind())) {
            Element e = TreeUtil.getElement(t.getLeaf());
            if(e!=null)
                return p.getChildren().getOrCreate(e,t);
        }
        return p;
    }


    /**
     * Represents a set of {@link TreePath}s as a tree of key program
     * elements.
     *
     * Used in {@link ClassUsageJsWriter#write(ParsedType,PrintWriter)}.
     */
    protected class Node implements JsonWriter.Writable, NodeMapOwner {
        /**
         * The program element that represents this node.
         * Null only if this is the root node.
         */
        final Element element;
        /**
         * {@link TreePath} of the element, if available.
         */
        final TreePath path;
        /**
         * Child {@link Node}s keyed by their {@link Node#element}.
         */
        final NodeMap children = new NodeMap();
        final List<TreePath> leaves = new ArrayList<TreePath>();

        protected Node(Element element, TreePath path) {
            this.element = element;
            this.path = path;
        }

        public NodeMap getChildren() {
            return children;
        }

        /**
         * Writes a JSON object that represents this node.
         */
        public void write(JsonWriter w) {
            if(element!=null) {
                if(path==null)
                    writeOutlineNodeProperties(w,element);
                else
                    writeOutlineNodeProperties(w,element,path.getCompilationUnit(),path.getLeaf());
            }
            if(!children.isEmpty()) {
                w.property("children",children.values());
            }
            if(!leaves.isEmpty()) {
                w.key("leaves");
                w.startArray();
                for (TreePath p : leaves) {
                    // TODO: what shall we write here?
                    w.startObject();
                    w.property("code",p.getLeaf().toString());
                    w.endObject();
                }
                w.endArray();
            }
        }
    }

    /**
     * Hook for using a custom {@link Node} class.
     */
    protected Node createNode(Element e, TreePath path) {
        return new Node(e,path);
    }
}
