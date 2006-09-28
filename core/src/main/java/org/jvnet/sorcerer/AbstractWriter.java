package org.jvnet.sorcerer;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import org.jvnet.sorcerer.util.JsonWriter;
import org.jvnet.sorcerer.util.TreeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Set;

/**
 * Defines various low-level write methods.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractWriter {
    protected final ParsedSourceSet pss;

    protected AbstractWriter(ParsedSourceSet pss) {
        this.pss = pss;
    }

    protected Writer open(File dir, String fileName) throws IOException {
        return new OutputStreamWriter(
            new FileOutputStream(new File(dir,fileName)),"UTF-8");
    }

    /**
     * Open a file by using the system default encoding.
     * JavaScript files do not have associated encoding on the file system,
     * so the best bet is to use the system default encoding.
     */
    protected Writer openDefault(File dir, String fileName) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream(new File(dir,fileName))));
    }

    protected void writeOutlineNodeProperties(JsonWriter jw, Element e) {
        jw.property("name",e.accept(OutlineNameVisitor.INSTANCE,null));
        jw.property("kind",getKindString(e.getKind()));
        jw.property("access",getAccessLevel(e).toString());
        if(e.getModifiers().contains(Modifier.STATIC))
            jw.property("static",true);
        if(TreeUtil.isLocal(e))
            jw.property("local",true);
    }

    /**
     * Writes various properties for the outline node.
     */
    protected void writeOutlineNodeProperties(JsonWriter jw, Element e, CompilationUnitTree cu, Tree t) {
        writeOutlineNodeProperties(jw,e);
        long startPos = pss.getSourcePositions().getStartPosition(cu, t);
        jw.property("line",cu.getLineMap().getLineNumber(startPos));
    }

    private static final Modifier[] MODS = new Modifier[] {Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE};
    protected Object getAccessLevel(Element e) {
        Set<Modifier> mods = e.getModifiers();
        for (Modifier mod : MODS) {
            if(mods.contains(mod))
                return mod.name().toLowerCase();
        }
        return "default";
    }

    protected String getKindString(ElementKind kind) {
        switch (kind) {
        case ANNOTATION_TYPE:
            return "annotation";
        case CLASS:
            return "class";
        case ENUM:
            return "enum";
        case INTERFACE:
            return "interface";
        case CONSTRUCTOR:
        case METHOD:
        case INSTANCE_INIT:
        case STATIC_INIT:
            return "method";
        case ENUM_CONSTANT:
        case FIELD:
            return "field";
        }
        return null;
    }
}
