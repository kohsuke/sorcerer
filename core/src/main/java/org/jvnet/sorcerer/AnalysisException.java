package org.jvnet.sorcerer;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.List;

/**
 * Indicates a failure to analyze the source code.
 *
 * @author Kohsuke Kawaguchi
 */
public class AnalysisException extends RuntimeException {
    private final List<Diagnostic<? extends JavaFileObject>> diagnostics;

    AnalysisException(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        this.diagnostics = diagnostics;
    }

    /**
     * Gets the list of errors found during the analysis.
     *
     * @return
     *      never null.
     */
    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
        return diagnostics;
    }

    public String getMessage() {
        return diagnostics.toString();
    }
}
