package org.jvnet.sorcerer.util;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

/**
 * @author Kohsuke Kawaguchi
 */
public class JavaFileObjectDelegate implements JavaFileObject {
    protected JavaFileObject core;

    public JavaFileObjectDelegate(JavaFileObject core) {
        this.core = core;
    }

    public URI toUri() {
        return core.toUri();
    }

    public String getName() {
        return core.getName();
    }

    public InputStream openInputStream() throws IOException {
        return core.openInputStream();
    }

    public OutputStream openOutputStream() throws IOException {
        return core.openOutputStream();
    }

    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return core.openReader(ignoreEncodingErrors);
    }

    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return core.getCharContent(ignoreEncodingErrors);
    }

    public Writer openWriter() throws IOException {
        return core.openWriter();
    }

    public long getLastModified() {
        return core.getLastModified();
    }

    public boolean delete() {
        return core.delete();
    }

    public Kind getKind() {
        return core.getKind();
    }

    public boolean isNameCompatible(String simpleName, Kind kind) {
        return core.isNameCompatible(simpleName, kind);
    }

    public NestingKind getNestingKind() {
        return core.getNestingKind();
    }

    public Modifier getAccessLevel() {
        return core.getAccessLevel();
    }
}
