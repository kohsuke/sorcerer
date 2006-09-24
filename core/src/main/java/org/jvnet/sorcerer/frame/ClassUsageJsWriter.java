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
                root.add(t).getLeaves().add(t);

            // then write it out!
            w.object(root);
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

    private interface ParentNode {
        NodeMap getChildren();
        List<TreePath> getLeaves();
    }

    /**
     * Package tree that has {@link Node}. This tree is like a snowy mountain.
     * There's the top portion of the tree that consists of {@link NodePkgInfo},
     * then there's the lower portion of the tree that consists of {@link Node}.
     */
    class NodePkgInfo extends PkgInfo<NodePkgInfo> implements ParentNode {
        /**
         * Child {@link Node}s keyed by their {@link Node#element}.
         */
        final NodeMap children = new NodeMap();

        /**
         * This is used for use of types in package annotations.
         */
        final List<TreePath> leaves = new ArrayList<TreePath>();


        public NodePkgInfo(String name) {
            super(name);
        }

        /**
         * Adds the given {@link TreePath} to the {@link NodePkgInfo} tree rooted at this object.
         */
        protected ParentNode add(TreePath t) {
            // enter the package portion
            NodePkgInfo leafPkg = super.add(TreeUtil.getPackageName(t.getCompilationUnit()));
            // then the rest
            return addNode(leafPkg, t);
        }

        public NodeMap getChildren() {
            return children;
        }

        public List<TreePath> getLeaves() {
            return leaves;
        }

        public NodePkgInfo create(String name) {
            return new NodePkgInfo(name);
        }

        public void write(JsonWriter js) {
            js.property("kind","package");
            super.write(js);
            for (Node n : children.values()) {
                n.parentIsPackage = true;
            }
            if(!children.isEmpty())
                js.property("classes",children.values());
        }
    }

    /**
     * Adds the given {@link TreePath} to the {@link Node} tree
     * rooted at "root" node, by using the part of the path that
     * falls within the same compilation unit.
     * then return the {@link Node} where
     * the {@link TreePath} is ultimately stored.
     */
    ParentNode addNode(ParentNode root, TreePath t) {
        ParentNode p;
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
    protected class Node implements JsonWriter.Writable, ParentNode {
        /**
         * The program element that represents this node.
         * Null only if this is the root node.
         */
        final Element element;
        /**
         * {@link TreePath} of the element.
         */
        final TreePath path;
        /**
         * Child {@link Node}s keyed by their {@link Node#element}.
         */
        final NodeMap children = new NodeMap();
        final List<TreePath> leaves = new ArrayList<TreePath>();

        /**
         * Set to true before {@link #write(JsonWriter)} if the parent node
         * is {@link NodePkgInfo}.
         */
        boolean parentIsPackage;

        protected Node(Element element, TreePath path) {
            this.element = element;
            this.path = path;
        }

        public NodeMap getChildren() {
            return children;
        }

        public List<TreePath> getLeaves() {
            return leaves;
        }

        /**
         * Writes a JSON object that represents this node.
         */
        public void write(JsonWriter w) {
            if(element!=null) {
                writeOutlineNodeProperties(w,element,path.getCompilationUnit(),path.getLeaf());
            }

            if(parentIsPackage) {
                // if this class is in a compilation unit that's different from the class name,
                // we need to write it out so that we can jump to the right source file at runtime.
                String name = element.getSimpleName().toString();
                if(!TreeUtil.getPrimaryTypeName(path.getCompilationUnit()).equals(name))
                    w.property("source",name);
            }

            if(!children.isEmpty()) {
                w.property("children",children.values());
            }
            if(!leaves.isEmpty()) {
                w.key("leaves");
                w.startArray();
                for (TreePath p : leaves) {
                    // TODO: what shall we write here?
                    long pos = pss.getSourcePositions().getStartPosition(p.getCompilationUnit(), p.getLeaf());
                    w.object((int)p.getCompilationUnit().getLineMap().getLineNumber(pos));
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
