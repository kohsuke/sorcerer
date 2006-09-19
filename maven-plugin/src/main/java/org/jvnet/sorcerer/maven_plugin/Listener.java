package org.jvnet.sorcerer.maven_plugin;

import org.apache.maven.plugin.logging.Log;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;

/**
 * Forwards errors from sorcerer to Maven logger.
 * @author Kohsuke Kawaguchi
 */
final class Listener implements DiagnosticListener {
    private final Log log;

    public Listener(Log log) {
        this.log = log;
    }

    public void report(Diagnostic diagnostic) {
        log.error(diagnostic.toString());
    }
}
