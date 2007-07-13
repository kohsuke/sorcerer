package sorcerer;

import org.jvnet.sorcerer.stapler.Sorcerer;
import org.jvnet.sorcerer.ParsedSourceSet;
import org.jvnet.sorcerer.util.TabExpandingFileManager;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.JavaFileObject;
import javax.tools.DiagnosticListener;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Collections;

import com.sun.tools.javac.api.JavacTool;
import com.sun.source.util.JavacTask;

/**
 * @author Kohsuke Kawaguchi
 */
public class WebAppMain implements ServletContextListener {

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            DiagnosticListener errorListener = new DiagnosticListener() {
                public void report(Diagnostic diagnostic) {
                    System.out.println(diagnostic);
                }
            };

            JavaCompiler javac = JavacTool.create();
            StandardJavaFileManager fileManager = new TabExpandingFileManager(
                javac.getStandardFileManager(errorListener, null, null),null,4);

            fileManager.setLocation( StandardLocation.CLASS_PATH, Collections.<File>emptyList() );

            List<String> options = Collections.emptyList();
            Iterable<? extends JavaFileObject> files = fileManager.getJavaFileObjectsFromFiles(
                Collections.singletonList(new File("src/main/java"))
            );
            CompilationTask task = javac.getTask(null, fileManager, errorListener, options, null, files);

            Sorcerer app = new Sorcerer(new ParsedSourceSet((JavacTask)task,4));

            servletContextEvent.getServletContext().setAttribute("app",app);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
