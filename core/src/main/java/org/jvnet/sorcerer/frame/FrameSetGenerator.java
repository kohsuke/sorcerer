package org.jvnet.sorcerer.frame;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import org.jvnet.sorcerer.LinkResolver;
import org.jvnet.sorcerer.LinkResolverFactory;
import org.jvnet.sorcerer.ParsedSourceSet;
import org.jvnet.sorcerer.ParsedType;
import org.jvnet.sorcerer.ResourceResolver;
import org.jvnet.sorcerer.util.AbstractResourceResolver;
import org.jvnet.sorcerer.util.IOUtil;
import org.jvnet.sorcerer.util.JsonWriter;
import org.jvnet.sorcerer.util.TreeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

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
public class FrameSetGenerator extends AbstractWriter {

    private String title = "Sorcerer report";
    private final LinkResolverFactory linkResolverFactory;
    /**
     * Reference to the unnamed package.
     */
    private final PackageElement unnamed;

    public FrameSetGenerator(ParsedSourceSet pss) {
        super(pss);
        this.linkResolverFactory = pss.getLinkResolverFactory();
        this.unnamed = pss.getElements().getPackageElement("");
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

            String name = TreeUtil.getPrimaryTypeName(cu);

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

        System.out.println("Generating usage index");
        {// "find usage" index
            generateProjectUsageJs(new PrintWriter(openDefault(outDir,"project-usage.js")));

            ClassUsageJsWriter cujw = new ClassUsageJsWriter(pss);

            for( ParsedType pt : pss.getParsedTypes() ) {
                if(pt.getReferers().length==0)
                    continue;
                // local types can be only referenced from the same compilation unit.
                if(pt.isLocal())
                    continue;

                File out = new File(outDir, pt.element.getQualifiedName().toString().replace('.','/')+"-usage.js");
                out.getParentFile().mkdirs();

                cujw.write(pt,new PrintWriter(out));
            }
        }

        // other resources from core
        System.out.println("Generating static resource files");
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
        class DefinedPkgInfo extends PkgInfo<DefinedPkgInfo> {
            public DefinedPkgInfo(String name) {
                super(name);
            }

            protected DefinedPkgInfo create(String name) {
                return new DefinedPkgInfo(name);
            }

            /**
             * False if this class doesn't have any classes in it (excluding descendants.)
             */
            boolean hasClasses;

            public void write(JsonWriter js) {
                super.write(js);
                if(hasClasses)
                    js.property("hasClasses",true);
            }
        }

        // build package tree info
        DefinedPkgInfo root = new DefinedPkgInfo("");

        for (PackageElement pe : pss.getPackageElement()) {
            root.add(pe.getQualifiedName().toString()).hasClasses=true;
        }

        try {
            w.println("setPackageList(");
            new JsonWriter(w).object(root);
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

                    if(TreeUtil.OUTLINE_WORTHY_ELEMENT.contains(e.getKind())) {
                        jw.startObject();
                        writeOutlineNodeProperties(jw,e,cu,t);
                        jw.key("children").startArray();
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


    /**
     * Writes out <tt>project-usage.js</tt> that lists all classes for which
     * we have usage index.
     */
    public void generateProjectUsageJs(PrintWriter w) throws IOException {
        Map<PackageElement,Set<ParsedType>> pkgs =
            new TreeMap<PackageElement,Set<ParsedType>>(ParsedSourceSet.PACKAGENAME_COMPARATOR);

        for( ParsedType pt : pss.getParsedTypes() ) {
            if(pt.getReferers().length==0)
                continue;

            PackageElement pkg = pss.getElements().getPackageOf(pt.element);
            Set<ParsedType> types = pkgs.get(pkg);
            if(types==null)
                pkgs.put(pkg,types=new HashSet<ParsedType>());

            if(!pt.isLocal())
                types.add(pt);
        }

        w.println("setProjectUsage(");
        JsonWriter js = new JsonWriter(w);
        js.startArray();
        for (Entry<PackageElement,Set<ParsedType>> pkg : pkgs.entrySet()) {
            js.startObject();
            js.property("package",pkg.getKey().getQualifiedName());
            js.key("classes");
            js.startArray();
            String[] names = new String[pkg.getValue().size()];
            int idx=0;
            for (ParsedType pt : pkg.getValue()) {
                names[idx++] = pt.getPackageLocalName();
            }
            Arrays.sort(names);
            for (String n : names) {
                js.object(n);
            }
            js.endArray();
            js.endObject();
        }
        js.endArray();
        w.println(")");
        w.close();
    }







    private static final List<String> RESOURCES = new ArrayList<String>();

    static {
        RESOURCES.addAll(Arrays.asList( new String[]{
            "package-tree.html",
            "package-toolbar.html",
            "package-container.html",
            "outline-view.html",
            "search-pane.html",
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
