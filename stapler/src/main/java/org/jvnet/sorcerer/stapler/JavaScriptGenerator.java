package org.jvnet.sorcerer.stapler;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class JavaScriptGenerator extends Generator {
    @Override
    String getContentType(String restOfPath) {
        return "application/javascript;charset=UTF-8";
    }
}
