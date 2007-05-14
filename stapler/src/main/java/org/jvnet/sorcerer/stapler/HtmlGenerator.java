package org.jvnet.sorcerer.stapler;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class HtmlGenerator extends Generator {
    @Override
    String getContentType(String restOfPath) {
        return "text/html;charset=UTF-8";
    }
}
