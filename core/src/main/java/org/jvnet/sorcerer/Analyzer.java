package org.jvnet.sorcerer;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import org.jvnet.sorcerer.util.TabExpandingFileManager;

import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Entry point to the system.
 *
 * @author Kohsuke Kawaguchi
 */
public class Analyzer {

    private final List<File> sourceFiles = new ArrayList<File>();
    private final List<File> classpath = new ArrayList<File>();
    private Charset encoding;
    private Locale locale;
    private int tabWidth = 8;

    /**
     * Adds a single ".java" file for compilation.
     */
    public void addSourceFile(File file) {
        sourceFiles.add(file);
    }

    /**
     * Adds all source files inside this folder recursively as source files.
     */
    public void addSourceFolder(File dir) {
        File[] files = dir.listFiles();
        if(files==null)
            throw new IllegalArgumentException(dir+" is not a directory");
        for( File f : files ) {
            if(f.isDirectory())
                addSourceFolder(f);
            else
            if(f.getName().endsWith(".java"))
                addSourceFile(f);
        }
    }

    /**
     * Adds a jar file or class folder to the classpath
     * used for compilation.
     */
    public void addClasspath(File f) {
        classpath.add(f);
    }

    /**
     * Sets the encoding of the source files. Default to the system default encoding.
     */
    public void setSourceEncoding(String encoding) {
        this.encoding = Charset.forName(encoding);
    }

    /**
     * Locale used to format error messages found during analysis.
     *
     * Null to use the default locale.
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * Gets the current TAB width.
     */
    public int getTabWidth() {
        return tabWidth;
    }

    /**
     * Sets the TAB width.
     *
     * Defaults to 8.
     */
    public void setTabWidth(int tabWidth) {
        this.tabWidth = tabWidth;
    }

    /**
     * Analyzes all the source files.
     *
     * @return
     *      Always return a non-null object upon a successful completion.
     * @throws IOException
     *      if the underlying file system access fails.
     * @throws AnalysisException
     *      if the source files have errors.
     */
    public ParsedSourceSet analyze() throws IOException, AnalysisException {
        DiagnosticCollector<JavaFileObject> listener = new DiagnosticCollector<JavaFileObject>();
        ParsedSourceSet r = analyze(listener);
        if(listener.getDiagnostics().isEmpty())
            return r;
        else
            throw new AnalysisException(listener.getDiagnostics());
    }

    /**
     * Analyzes all the source files.
     *
     * <p>
     * This method can be overrided to create a custom {@link ParsedSourceSet} derived class. 
     *
     * @param errorListener
     *      Receives errors found during the analysis.
     * @return
     *      Always return a non-null object, even if some errors are found in the analysis.
     * @throws IOException
     *      if the underlying file system access fails.
     */
    public ParsedSourceSet analyze(DiagnosticListener<? super JavaFileObject> errorListener) throws IOException {
        return new ParsedSourceSet(configure(errorListener),tabWidth);
    }

    /**
     * Creates a configured {@link JavacTask}.
     */
    protected JavacTask configure(DiagnosticListener<? super JavaFileObject> errorListener) throws IOException {
        JavaCompiler javac = JavacTool.create();
        StandardJavaFileManager fileManager = new TabExpandingFileManager(
            javac.getStandardFileManager(errorListener, locale, encoding),encoding,tabWidth);

        fileManager.setLocation( StandardLocation.CLASS_PATH, classpath );

        List<String> options = Collections.emptyList();
        Iterable<? extends JavaFileObject> files = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
        CompilationTask task = javac.getTask(null, fileManager, errorListener, options, null, files);
        return (JavacTask)task;
    }
}
