package org.jvnet.sorcerer.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;
import org.jvnet.sorcerer.Analyzer;
import org.jvnet.sorcerer.Dependency;
import org.jvnet.sorcerer.FrameSetGenerator;
import org.jvnet.sorcerer.ParsedSourceSet;
import org.jvnet.sorcerer.util.CSSHandler;
import org.jvnet.sorcerer.util.DiagnosticPrinter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Ant task to invoke sorcerer.
 *
 * <p>
 * {@link MatchingTask} that selects Java source files.
 *
 * @author Kohsuke Kawaguchi
 */
public class SorcererTask extends MatchingTask {
    private File dir;
    private File destDir;
    private String encoding;
    private File css;
    private String windowTitle;
    private int tabWidth = 8;

    private final List<Javadoc> javadocs = new ArrayList<Javadoc>();

    private Path classpath = new Path(null);

    /**
     * Nested &lt;classpath> element.
     */
    public Path createClasspath() {
        return classpath.createPath();
    }

    /**
     * Base directory to scan source files.
     */
    public void setSrcdir(File srcdir) {
        this.dir = srcdir;
    }

    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }

    public void setCss(File css) {
        this.css = css;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void addJavadoc(Javadoc j) {
        this.javadocs.add(j);
    }

    /**
     * Sets the tab width.
     */
    public void setTabWidth(int tabWidth) {
        this.tabWidth = tabWidth;
    }

    /**
     * Sets the caption of the generated HTML files.
     */
    public void setWindowTitle(String windowTitle) {
        this.windowTitle = windowTitle;
    }

    public void execute() throws BuildException {
        try {
            classpath.setProject(getProject());

            Analyzer a = new Analyzer();
            if(encoding!=null)
                a.setSourceEncoding(encoding);

            DirectoryScanner ds = getDirectoryScanner(dir);
            for (String f : ds.getIncludedFiles())
                a.addSourceFile(new File(dir,f));

            for (String p : classpath.list())
                a.addClasspath(getProject().resolveFile(p));

            a.setTabWidth(tabWidth);
            ParsedSourceSet pss = a.analyze(new DiagnosticPrinter());
            addDependencies(pss.getDependencies());

            CSSHandler css = createCSSHandler();

            FrameSetGenerator fsg = new FrameSetGenerator(pss);
            if(windowTitle!=null)
                fsg.setTitle(windowTitle);
            fsg.generateAll(destDir,css);
            css.copy(destDir);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    private CSSHandler createCSSHandler() {
        if(css!=null)
            return new CSSHandler.User(css);
        else
            return new CSSHandler.Default();
    }

    private void addDependencies(List<Dependency> dependencies) throws IOException {
        for (Javadoc j : javadocs) {
            dependencies.add(new Dependency.Javadoc(
                j.getTitle(),
                new URL(j.getHref()),
                j.getPackageList()));
        }
    }
}
