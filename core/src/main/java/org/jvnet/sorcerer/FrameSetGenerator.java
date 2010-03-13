package org.jvnet.sorcerer;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import org.jvnet.sorcerer.util.IOUtil;
import org.jvnet.sorcerer.util.JsonWriter;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
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
import java.util.UUID;

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

    /**
     * Reference to the unnamed package.
     */
    private final PackageElement unnamed;

    private final UUID id = UUID.randomUUID();

    public FrameSetGenerator(ParsedSourceSet pss) {
        super(pss);
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

        for (CompilationUnitTree cu : pss.getCompilationUnits()) {
            AstGenerator gen = new AstGenerator(pss,id,cu);
            gen.write(outDir);
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
                if(pt.isInvalid())
                    continue;

                File out = new File(outDir, cujw.getRelativePath(pt));
                assert out.getParentFile() != null : out;
                out.getParentFile().mkdirs();

                cujw.write(pt,new PrintWriter(out));
            }
        }

        // resource files
        System.out.println("Generating static resource files");
        // frameset specific resources
        for (String res : RESOURCES) {
            File o = new File(outDir, res);
            o.getParentFile().mkdirs();
            IOUtil.copy(res,o);
        }
    }

    public void generateIndex(PrintWriter w) throws IOException {
        generateWithMacro("index.html", w, "title", title);
    }

    private void generateWithMacro(String resourceName, PrintWriter w, String key, String value) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resourceName)));
        String line;

        while((line=r.readLine())!=null) {
            line = line.replaceAll("\\$\\{"+ key +"\\}", value);
            w.println(line);
        }

        r.close();
        w.close();
    }

    public void generatePackageListJs(PrintWriter w) throws IOException {
        PackageSet packageNames = new PackageSet();
        for (PackageElement pe : pss.getPackageElement())
            packageNames.add(pe.getQualifiedName().toString());
        packageNames.sort();

        try {
            w.println("setProject(");
            JsonWriter jw = new JsonWriter(w);
            jw.object(id.toString());
            jw.startArray();
            {// write self
                jw.startObject();
                jw.property("name","Project"); // TODO
                jw.propertyUnquoted("linker","linker.self");
                jw.property("packages",packageNames);
                jw.endObject();
            }
            // write dependencies
            for (Dependency dep : pss.getDependencies())
                jw.object(dep);
            jw.endArray();
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
        try {
            w.printf("setClassList(\"%s\",",p.getQualifiedName());
            JsonWriter jw = new JsonWriter(w);
            jw.object(id.toString());
            jw.startArray();
            for (TypeElement t : pss.getClassElements(p)) {
                ClassTree ct = pss.getTrees().getTree(t);
                if(ct ==null)   continue;   // not a part of the compilation unit
                TreePath tp = pss.getTreePathByClass(ct);

                String sourceName = tp.getCompilationUnit().getSourceFile().getName();
                sourceName = sourceName.substring(sourceName.lastIndexOf('/')+1);
                sourceName = sourceName.substring(sourceName.lastIndexOf('\\')+1);

                jw.startObject();
                jw.property("name",t.getSimpleName());
                jw.property("kind",getKindString(t.getKind()));
                jw.property("file", sourceName);
                jw.property("line", pss.getPositionOf(tp).line);
                jw.property("access",getAccessLevel(t));
                jw.endObject();
            }
            jw.endArray();
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





    public static final List<String> RESOURCES = new ArrayList<String>();

    static {
        RESOURCES.addAll(Arrays.asList(
            "sorcerer.js",
            "style.css",
            "menu/menu.css",
            "menu/rightarrow.gif",
            "menu/menu.js",            
            "package-tree.html",
            "package-toolbar.html",
            "package-container.html",
            "outline-view.html",
            "outline-toolbar.html",
            "outline-container.html",
            "search-pane.html",
            "search-toolbar.html",
            "search-container.html",
            "source-view.html",
            "eclipse-public-license.url",
            "left-pane.js",
            "left-pane.css",
            "resource-files/yahoo.js",
            "resource-files/dom.js",
            "resource-files/event.js",
            "resource-files/container.js",
            "resource-files/close.gif",
            "resource-files/opentype.gif",
            "resource-files/package.gif",
            "resource-files/library.gif",
            "resource-files/layout-flat.gif",
            "resource-files/layout-hierarchical.gif",
            "resource-files/tree/noicon.css",
            "resource-files/tree/ln.gif",
            "resource-files/tree/loading.gif",
            "resource-files/tree/tn.gif",
            "resource-files/tree/vline.gif",
            "resource-files/tree/treeview.js",
            "resource-files/tree/license.txt"
        ));

        for(String name : new String[]{"lm","lp","tm","tp"}) {
            for( String folder : new String[]{"noicon"}) {
                RESOURCES.add("resource-files/tree/"+folder+"/"+name+".gif");
                RESOURCES.add("resource-files/tree/"+folder+"/"+name+"h.gif");
            }
        }

        for(String name : new String[]{"file","package","prj","type"}) {
            RESOURCES.add("resource-files/search/"+name+"_mode.gif");
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
