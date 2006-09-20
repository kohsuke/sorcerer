package org.jvnet.sorcerer.frame;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import org.jvnet.sorcerer.JavadocErasureVisitor;
import org.jvnet.sorcerer.LinkResolver;
import org.jvnet.sorcerer.LinkResolverFactory;
import org.jvnet.sorcerer.ParsedSourceSet;
import org.jvnet.sorcerer.ResourceResolver;
import org.jvnet.sorcerer.ShortNameVisitor;
import org.jvnet.sorcerer.util.AbstractResourceResolver;
import org.jvnet.sorcerer.util.IOUtil;
import org.jvnet.sorcerer.util.JsonWriter;
import org.jvnet.sorcerer.util.TreeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.SimpleElementVisitor6;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * HTML generator that produces sorcerer report in
 * javadoc-like 3 frame layout.
 *
 * <p>
 * This is the most simple way to use a sorcerer. Just
 * call the {@link #generateAll(File)} method to generate
 * the complete files into the output directory.
 *
 * <p>
 * Most of the generated HTML files use hyphen in the file name,
 * to make sure it doesn't collide with HTMLs generated from classes.
 *
 * @author Kohsuke Kawaguchi
 */
public class FrameSetGenerator {

    private final ParsedSourceSet pss;
    private String title = "Sorcerer report";
    private final LinkResolverFactory linkResolverFactory;
    /**
     * Reference to the unnamed package.
     */
    private final PackageElement unnamed;

    private final TypeVisitor<TypeMirror,Void> javadocErasure;

    public FrameSetGenerator(ParsedSourceSet pss) {
        this.pss = pss;
        this.linkResolverFactory = pss.getLinkResolverFactory();
        this.unnamed = pss.getElements().getPackageElement("");
        this.javadocErasure = new JavadocErasureVisitor(pss.getTypes());
    }

    /**
     * Sets the window title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    private String getPackagePath(PackageElement pe) {
        if(pe.equals(unnamed))
            return ".";
        else
            return pe.getQualifiedName().toString().replace('.','/');
    }

    /**
     * Generates all the HTML files into the given directory.
     */
    public void generateAll(File outDir) throws IOException {
        generateAll(outDir,null);
    }

    /**
     * Generates all the HTML files into the given directory.
     *
     * @param css
     *      If specified, path to CSS will computed by using this resolver
     */
    public void generateAll(File outDir, ResourceResolver css) throws IOException {
        if(css==null) {
            css = new AbstractResourceResolver() {
                public String href(CompilationUnitTree compUnit) {
                    return getRelativePathToTop(compUnit)+"style.css";
                }
            };
        }

        pss.setLinkResolverFactories(linkResolverFactory);
        for (CompilationUnitTree cu : pss.getCompilationUnits()) {
            ExpressionTree packageName = cu.getPackageName();
            String pkg = packageName==null?"":packageName.toString().replace('.','/')+'/';

            String name = cu.getSourceFile().getName();
            int idx = name.lastIndexOf('/');
            if(idx>=0)  name=name.substring(idx+1);
            idx = name.lastIndexOf('\\');
            if(idx>=0)  name=name.substring(idx+1);

            if(name.endsWith(".java"))  name=name.substring(0,name.length()-5);

            System.out.println(pkg+name);
            File out = new File(outDir, pkg + name+".html");
            out.getParentFile().mkdirs();

            FrameHtmlGenerator gen = new FrameHtmlGenerator(pss,cu);
            gen.setCss(css.href(cu));
            gen.write(out);

            File js = new File(outDir, pkg + name + "-outline.js");
            generateClassOutlineJs(cu,new PrintWriter(js));
        }

        generateIndex(new PrintWriter(open(outDir,"index.html")));
        generatePackageListJs(new PrintWriter(openDefault(outDir,"package-list.js")));
        generatePackageList(new PrintWriter(openDefault(outDir,"package-list")));

        for (PackageElement p : pss.getPackageElement()) {
            File dir = new File(outDir,getPackagePath(p));
            dir.mkdirs();

            generateClassListJs(p,new PrintWriter(openDefault(dir,"class-list.js")));
        }

        // other resources from core
        IOUtil.copy("behavior.js",new File(outDir,"behavior.js"));
        IOUtil.copy("sorcerer.js",new File(outDir,"sorcerer.js"));
        IOUtil.copy("style.css",new File(outDir,"style.css"));

        // frameset specific resources
        for (String res : RESOURCES) {
            File o = new File(outDir, res);
            o.getParentFile().mkdirs();
            InputStream in = getClass().getResourceAsStream(res);
            if(in==null)
                throw new Error("Resource "+res+" not found");
            IOUtil.copy(in,o);
        }
    }

    private Writer open(File dir, String fileName) throws IOException {
        return new OutputStreamWriter(
            new FileOutputStream(new File(dir,fileName)),"UTF-8");
    }

    /**
     * Open a file by using the system default encoding.
     * JavaScript files do not have associated encoding on the file system,
     * so the best bet is to use the system default encoding.
     */
    private Writer openDefault(File dir, String fileName) throws IOException {
        return new OutputStreamWriter(
            new FileOutputStream(new File(dir,fileName)));
    }


    public void generateIndex(PrintWriter w) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("index.html")));
        String line;

        while((line=r.readLine())!=null) {
            line = line.replaceAll("\\$\\{title\\}",title);
            w.println(line);
        }

        r.close();
        w.close();
    }

    public void generatePackageListJs(PrintWriter w) throws IOException {
        class PkgInfo implements Comparable<PkgInfo> {
            final String name;
            final Set<PkgInfo> children = new TreeSet<PkgInfo>();
            /**
             * False if this class doesn't have any classes in it (excluding descendants.)
             */
            boolean hasClasses;

            public PkgInfo(String name) {
                this.name = name;
            }

            public int compareTo(PkgInfo that) {
                return this.name.compareTo(that.name);
            }

            public PkgInfo add(String name) {
                if(name.length()==0) {
                    hasClasses =true;
                    return this;
                }

                String head,rest;
                int idx = name.indexOf('.');
                if(idx>=0) {
                    head = name.substring(0,idx);
                    rest = name.substring(idx+1);
                } else {
                    head = name;
                    rest = "";
                }

                for (PkgInfo c : children)
                    if(c.name.equals(head))
                        return c.add(rest);

                PkgInfo c = new PkgInfo(head);
                children.add(c);
                return c.add(rest);
            }

            public void write(JsonWriter js) {
                js.startObject();
                js.property("name",name);
                if(hasClasses)
                    js.property("hasClasses",true);
                js.key("children");
                js.startArray();
                for (PkgInfo c : children)
                    c.write(js);
                js.endArray();
                js.endObject();
            }
        }

        // build package tree info
        PkgInfo root = new PkgInfo("");
        for (PackageElement pe : pss.getPackageElement()) {
            root.add(pe.getQualifiedName().toString());
        }

        try {
            w.println("setPackageList(");
            root.write(new JsonWriter(w));
            w.println(");");
        } finally {
            w.close();
        }
    }

    /**
     * Generates javadoc compatible <tt>package-list</tt>
     */
    public void generatePackageList(PrintWriter w) throws IOException {
        try {
            for (PackageElement pe : pss.getPackageElement()) {
                w.println(pe.getQualifiedName());
            }
        } finally {
            w.close();
        }
    }

    public void generateClassListJs(PackageElement p, PrintWriter w) throws IOException {
        LinkResolver linkResolver = linkResolverFactory.create(p, pss);

        try {
            w.printf("setClassList(\"%s\",",p.getQualifiedName());
            JsonWriter jw = new JsonWriter(w);
            jw.startArray();
            for (TypeElement t : pss.getClassElements(p)) {
                jw.startObject();
                jw.property("name",t.getSimpleName());
                jw.property("kind",getKindString(t.getKind()));
                jw.property("href",linkResolver.href(t));
                jw.property("access",getAccessLevel(t));
                jw.endObject();
            }
            jw.endArray();
            w.println(");");
        } finally {
            w.close();
        }
    }

    public void generateClassOutlineJs(final CompilationUnitTree cu, PrintWriter w) throws IOException {
        try {
            w.printf("loadOutline(");
            final JsonWriter jw = new JsonWriter(w);
            jw.startObject();
            jw.property("packageName", TreeUtil.getPackageName(cu));
            jw.key("children").startArray();

            new TreePathScanner<Void,Void>() {
                private final SourcePositions sourcePositions = pss.getSourcePositions();
                private final LineMap lineMap = cu.getLineMap();

                public Void visitClass(ClassTree ct, Void _) {
                    boolean r = pre(ct);
                    super.visitClass(ct,_);
                    if(r)   post();
                    return _;
                }

                public Void visitMethod(MethodTree mt, Void _) {
                    boolean r = pre(mt);
                    super.visitMethod(mt,_);
                    if(r)   post();
                    return _;
                }

                public Void visitVariable(VariableTree vt, Void _) {
                    boolean r = pre(vt);
                    super.visitVariable(vt,_);
                    if(r)   post();
                    return _;
                }

                boolean pre(Tree t) {
                    Element e = pss.getTrees().getElement(getCurrentPath());
                    if(e==null)     return false;

                    long endPos = sourcePositions.getEndPosition(cu,t);
                    if(endPos<0)    return false; // synthetic

                    switch (e.getKind()) {
                    case ANNOTATION_TYPE:
                    case CLASS:
                    case CONSTRUCTOR:
                    case ENUM:
                    case ENUM_CONSTANT:
                    case FIELD:
                    case INSTANCE_INIT:
                    case INTERFACE:
                    case METHOD:
                    case STATIC_INIT:
                        jw.startObject();
                        jw.property("name",e.accept(nameVisitor,null));
                        jw.property("kind",getKindString(e.getKind()));
                        jw.property("access",getAccessLevel(e).toString());
                        if(e.getModifiers().contains(Modifier.STATIC))
                            jw.property("static",true);
                        if(isLocal(e))
                            jw.property("local",true);

                        long startPos = sourcePositions.getStartPosition(cu, t);
                        jw.property("line",lineMap.getLineNumber(startPos));

                        jw.key("children").startArray();
                        return true;
                    }

                    return false;
                }

                private boolean isLocal(Element e) {
                    switch(e.getEnclosingElement().getKind()) {
                    case CONSTRUCTOR:
                    case METHOD:
                    case INSTANCE_INIT:
                    case STATIC_INIT:
                    case ENUM_CONSTANT:
                    case FIELD:
                        return true;
                    }
                    return false;
                }

                private Void post() {
                    jw.endArray().endObject();
                    return null;
                }

            }.scan(cu,null);

            jw.endArray().endObject();
            w.println(");");
        } finally {
            w.close();
        }
    }


    private static final Modifier[] MODS = new Modifier[] {Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE};
    private Object getAccessLevel(Element e) {
        Set<Modifier> mods = e.getModifiers();
        for (Modifier mod : MODS) {
            if(mods.contains(mod))
                return mod.name().toLowerCase();
        }
        return "default";
    }

    private String getKindString(ElementKind kind) {
        switch (kind) {
        case ANNOTATION_TYPE:
            return "annotation";
        case CLASS:
            return "class";
        case ENUM:
            return "enum";
        case INTERFACE:
            return "interface";
        case CONSTRUCTOR:
        case METHOD:
        case INSTANCE_INIT:
        case STATIC_INIT:
            return "method";
        case ENUM_CONSTANT:
        case FIELD:
            return "field";
        }
        return null;
    }

    /**
     * Computes the display name of the element.
     */
    private final ElementVisitor<String,Void> nameVisitor = new SimpleElementVisitor6<String, Void>() {
        public String visitType(TypeElement t, Void _) {
            return t.getSimpleName().toString();
        }

        public String visitVariable(VariableElement v, Void _) {
            return v.getSimpleName().toString();
        }

        public String visitExecutable(ExecutableElement e, Void _) {
            StringBuilder buf = new StringBuilder();
            if(e.getKind()== ElementKind.CONSTRUCTOR)
                // javadoc uses the class name as the constructor name, not <init>
                buf.append(e.getEnclosingElement().getSimpleName());
            else
                buf.append(e.getSimpleName());

            buf.append('(');

            boolean first=true;

            for (VariableElement p : e.getParameters()) {
                if(first)       first = false;
                else            buf.append(',');

                buf.append(p.asType().accept(ShortNameVisitor.INSTANCE,null));
            }
            buf.append(')');
            if(e.getKind()!=ElementKind.CONSTRUCTOR) {
                buf.append(':');
                buf.append(e.getReturnType().accept(javadocErasure,null));
            }
            return buf.toString();
        }
    };

    private static final List<String> RESOURCES = new ArrayList<String>();

    static {
        RESOURCES.addAll(Arrays.asList( new String[]{
            "package-tree.html",
            "package-toolbar.html",
            "package-container.html",
            "outline-view.html",
            "outline-toolbar.html",
            "outline-container.html",
            "left-pane.js",
            "left-pane.css",
            "resource-files/yahoo.js",
            "resource-files/layout-flat.gif",
            "resource-files/layout-hierarchical.gif",
            "resource-files/tree/folder.css",
            "resource-files/tree/noicon.css",
            "resource-files/tree/ln.gif",
            "resource-files/tree/loading.gif",
            "resource-files/tree/tn.gif",
            "resource-files/tree/vline.gif",
            "resource-files/tree/treeview.js",
            "resource-files/tree/license.txt"
        }));

        for(String name : new String[]{"lm","lp","tm","tp"}) {
            for( String folder : new String[]{"folder","noicon"}) {
                RESOURCES.add("resource-files/tree/"+folder+"/"+name+".gif");
                RESOURCES.add("resource-files/tree/"+folder+"/"+name+"h.gif");
            }
        }

        for(String name : new String[]{"alphab_sort_co.gif","fields_co.gif","localtypes_co.gif","public_co.gif","static_co.gif"}) {
            RESOURCES.add("resource-files/outline-filter/"+name);
        }

        String[] modifiers = new String[]{"public", "protected", "private", "default"};

        for( String kind : new String[]{"annotation","interface","enum","class","field","method"} )
            for( String mod : modifiers)
                RESOURCES.add("resource-files/"+kind+'_'+mod+".gif");
    }
}
