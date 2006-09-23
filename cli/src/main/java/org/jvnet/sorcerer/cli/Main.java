package org.jvnet.sorcerer.cli;

import org.jvnet.sorcerer.Analyzer;
import org.jvnet.sorcerer.InternalLinkResolverFactory;
import org.jvnet.sorcerer.JavadocLinkResolverFactory;
import org.jvnet.sorcerer.LinkResolverFacade;
import org.jvnet.sorcerer.LinkResolverFactory;
import org.jvnet.sorcerer.ParsedSourceSet;
import org.jvnet.sorcerer.frame.FrameSetGenerator;
import org.jvnet.sorcerer.util.CSSHandler;
import org.jvnet.sorcerer.util.DiagnosticPrinter;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * CLI to sorcerer.
 *
 * see http://jackpot.netbeans.org/docs/org-netbeans-libs-javacapi/overview-summary.html
 * see http://download.java.net/jdk6/docs/api/index.html?javax/tools/package-summary.html
 * @author Kohsuke Kawaguchi
 */
public class Main {

    @Argument
    List<String> files = new ArrayList<String>();

    @Option(name="-d",usage="Directory to write all HTML files")
    File outDir;

    @Option(name="-css",usage="CSS file to be used instead of the default one")
    File userCss;

    @Option(name="-javadoc",usage="Link to external javadoc")
    List<String> javadocs = new ArrayList<String>();

    @Option(name="-cp",usage="Classpath for analyzing source file")
    String path = "";

    @Option(name="-auto",usage="Automatically scan jar files and source files.\n" +
                               "Specify the project root directory as the argument.")
    boolean auto = false;

    @Option(name="-debug",usage="Produce output with debug information")
    boolean debug = false;

    /**
     * Entry point from the JVM.
     *
     * This creates another {@link ClassLoader} for loading <tt>tools.jar</tt>
     * and runs the entire program in the new classloader.
     */
    public static void main(String[] args) throws Exception {
        // create a new classloader that loads tools.jar

        // first copy all system classpath
        List<URL> urls = new ArrayList<URL>();
        StringTokenizer tokens = new StringTokenizer(System.getProperty("java.class.path"),File.pathSeparator);
        while(tokens.hasMoreTokens()) {
            urls.add(new File(tokens.nextToken()).toURL());
        }
        // then also add tools.jar
        urls.add(new File(new File(System.getProperty("java.home")),"../lib/tools.jar").toURL());

        ClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]),null);
        Thread.currentThread().setContextClassLoader(cl);
        Class<?> main = cl.loadClass(Main.class.getName());
        System.exit((Integer)main.getMethod("doMain",String[].class).invoke(null,(Object)args));
    }

    /**
     * The entry point that returns the exit code as a return value.
     */
    public static int doMain(String[] args) throws Exception {
        Main m = new Main();
        CmdLineParser p = new CmdLineParser(m);
        try {
            p.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            printUsage(p, System.err);
            return -1;
        }

        if(m.files.isEmpty()) {
            printUsage(p, System.err);
            return -1;
        }

        m.run();
        return 0;
    }

    private static void printUsage(CmdLineParser p, PrintStream out) {
        out.println("Sorcerer: Java source code cross-reference generator");
        out.println("Usage: sorcerer [<options>...] <src files/dirs>... ");
        p.printUsage(out);
    }

    private CSSHandler createCSSHandler() {
        if(userCss!=null)
            return new CSSHandler.User(userCss);
        else
            return new CSSHandler.Default();
    }

    private void run() throws IOException {
        if(debug)
            System.setProperty("sorcerer.debug","true");

        Analyzer a = new Analyzer();

        for (String f : files) {
            File file = new File(f);
            if(file.isDirectory()) {
                if(auto)
                    autoScan(file,a);
                else
                    a.addSourceFolder(file);
            } else
                a.addSourceFile(file);
        }

        StringTokenizer tokens = new StringTokenizer(path,File.pathSeparator);
        while(tokens.hasMoreTokens())
            a.addClasspath(new File(tokens.nextToken()));

        ParsedSourceSet pss = a.analyze(new DiagnosticPrinter());
        pss.setLinkResolverFactory(createLinkResolverFactory());

        CSSHandler css = createCSSHandler();

        new FrameSetGenerator(pss).generateAll(outDir,css);
        css.copy(outDir);
    }

    /**
     * Automatically scan the directories and look for source files.
     */
    private void autoScan(File f,Analyzer a) {
        String name = f.getName();

        if(IGNORABLE.contains(name))
            return; //

        if(f.isDirectory()) {
            File[] files = f.listFiles();
            if(files!=null) {
                for (File file : files)
                    autoScan(file,a);
            }
        }

        if(name.endsWith(".jar")) {
            a.addClasspath(f);
            return;
        }
        if(name.endsWith(".java")) {
            a.addSourceFile(f);
            return;
        }
    }


    private LinkResolverFactory createLinkResolverFactory() throws IOException {
        List<LinkResolverFactory> list = new ArrayList<LinkResolverFactory>();
        for (String url : javadocs) {
            list.add(new JavadocLinkResolverFactory(url));
        }
        list.add(new InternalLinkResolverFactory());

        return new LinkResolverFacade(list.toArray(new LinkResolverFactory[list.size()]));
    }

    /**
     * Folders that are unlikely to contain any source files nor jar files.
     */
    private static final Set<String> IGNORABLE = new HashSet<String>(Arrays.asList(
       "CVS",".svn","classes","test-classes"
    ));
}
