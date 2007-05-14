package org.jvnet.sorcerer.stapler;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class Generator {
    String getContentType(String restOfPath) {
        if(restOfPath.endsWith(".js"))      return "application/javascript;charset=UTF-8";
        if(restOfPath.endsWith(".html"))    return "text/html;charset=UTF-8";
        if(restOfPath.endsWith(".css"))     return "text/css;charset=UTF-8";
        if(restOfPath.endsWith(".gif"))     return "image/gif";

        throw new AssertionError();
    }
    abstract void doDynamic(StaplerRequest request, StaplerResponse rsp) throws IOException;

    protected PrintWriter open(StaplerResponse rsp) throws IOException {
        return new PrintWriter(rsp.getWriter());
    }
}
