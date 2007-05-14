package org.jvnet.sorcerer.stapler;

import com.sun.source.tree.CompilationUnitTree;
import org.jvnet.sorcerer.AstGenerator;
import org.jvnet.sorcerer.FrameSetGenerator;
import org.jvnet.sorcerer.ParsedSourceSet;
import org.jvnet.sorcerer.util.IOUtil;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.lang.model.element.PackageElement;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Stapler model object that serves sorcerer documentation.
 * 
 * @author Kohsuke Kawaguchi
 */
public class Sorcerer {
    private final FrameSetGenerator fsg;
    private final long timestamp;
    private final long expiration;

    private final Map<String,Generator> generators = new HashMap<String, Generator>();

    public Sorcerer(final ParsedSourceSet pss) throws IOException {
        this(pss,new Date().getTime(),0);
    }

    public Sorcerer(final ParsedSourceSet pss, long timestamp, long expiration) throws IOException {
        this.fsg = new FrameSetGenerator(pss);
        this.timestamp = timestamp;
        this.expiration = expiration;

        for (final CompilationUnitTree cu : pss.getCompilationUnits()) {
            generators.put(new AstGenerator(pss,cu).getRelativePath(),
                new JavaScriptGenerator() {
                    void doDynamic(StaplerRequest request, StaplerResponse rsp) throws IOException {
                        new AstGenerator(pss,cu).write(rsp.getWriter());
                    }
                }
            );
        }

        generators.put("",new HtmlGenerator() {
            void doDynamic(StaplerRequest request, StaplerResponse rsp) throws IOException {
                fsg.generateIndex(open(rsp));
            }
        });

        generators.put("package-list.js",new JavaScriptGenerator() {
            void doDynamic(StaplerRequest request, StaplerResponse rsp) throws IOException {
                fsg.generatePackageListJs(open(rsp));
            }
        });

        generators.put("package-list",new Generator() {
            String getContentType(String restOfPath) {
                return "text/plain;charset=UTF-8";
            }

            void doDynamic(StaplerRequest request, StaplerResponse rsp) throws IOException {
                fsg.generatePackageList(open(rsp));
            }
        });

        for (final PackageElement p : pss.getPackageElement()) {
            String path;
            if(p.isUnnamed())
                path="";
            else
                path = p.getQualifiedName().toString().replace('.','/')+'/';
            path += "class-list.js";

            generators.put(path,new JavaScriptGenerator() {
                void doDynamic(StaplerRequest request, StaplerResponse rsp) throws IOException {
                    fsg.generateClassListJs(p,open(rsp));
                }
            });
        }

        for (final String r : FrameSetGenerator.RESOURCES) {
            generators.put(r,new Generator() {
                void doDynamic(StaplerRequest request, StaplerResponse rsp) throws IOException {
                    IOUtil.copy(r,rsp.getOutputStream());
                }
            });
        }
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException {
        String path = req.getRestOfPath();
        Generator g = generators.get(path);
        if(g==null) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if(req.checkIfModified(timestamp,rsp,expiration))
            return;

        g.doDynamic(req,rsp);
    }
}
