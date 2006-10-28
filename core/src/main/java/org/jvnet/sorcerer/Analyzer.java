package org.jvnet.sorcerer;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import org.jvnet.sorcerer.IprParser.Library;
import org.jvnet.sorcerer.util.TabExpandingFileManager;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
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
     * Reads a <tt>*.ipr</tt> from IntelliJ IDEA and
     * sets up classpath and source directories.
     *
     * @param iprFile
     *      The .ipr file to be parsed.
     * @throws IOException
     *      If the parsing of the file fails for some reasons.
     */
    public void parseIpr(File iprFile) throws IOException {
        try {
            iprFile = iprFile.getAbsoluteFile();

            IprParser pp = new IprParser(iprFile.getParentFile());
            SAXParser parser = createXMLReader();

            parser.parse(iprFile,pp);

            for (File module : pp.modules) {
                pp.moduleDir = module.getParentFile();
                parser.parse(module, pp);
            }

            for (File source : pp.sources)
                addSourceFolder(source);
            for (Library library : pp.libraries)
                library.addTo(this);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    /**
     * Reads a <tt>.classpath</tt> file from Eclipse and 
     * sets up classpath and source directories.
     *
     * @param dir
     *      The directory that contains <tt>.classpath</tt>.
     * @throws IOException
     *      If the parsing of the file fails for some reasons. 
     */
    public void parseDotClassPath(File dir) throws IOException {
        // all entries in .classpath are relative to this directory.
        final File baseDir = dir.getAbsoluteFile();

        try {
            createXMLReader().parse(new File(dir,".classpath"),
                new DefaultHandler() {
                    public void startElement(String uri,String localName,String qname, Attributes atts) {
                        if( !localName.equals("classpathentry") )
                            return; // unknown

                        String kind = atts.getValue("kind");
                        if(kind.equals("src"))
                            addSourceFolder(absolutize(atts.getValue("path")));
                        if(kind.equals("lib"))
                            addClasspath(absolutize(atts.getValue("path")));
                    }

                    private File absolutize( String path ) {
                        path = path.replace('/',File.separatorChar);
                        File child = new File(path);
                        if(child.isAbsolute())
                            return child;
                        else
                            return new File(baseDir,path);
                    }
                });
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    private SAXParser createXMLReader() throws SAXException, ParserConfigurationException {
        SAXParserFactory spf =SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        return spf.newSAXParser();
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
