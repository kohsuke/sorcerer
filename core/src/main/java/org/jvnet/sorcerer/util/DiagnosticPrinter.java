package org.jvnet.sorcerer.util;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.PrintStream;

/**
 * {@link DiagnosticListener} that prints out errors to {@link PrintStream}.
 * 
 * @author Kohsuke Kawaguchi
 */
public class DiagnosticPrinter implements DiagnosticListener<JavaFileObject> {
    private PrintStream err;

    public DiagnosticPrinter(PrintStream err) {
        this.err = err;
    }

    public DiagnosticPrinter() {
        this(System.err);
    }

    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        err.println(diagnostic);
    }
}
