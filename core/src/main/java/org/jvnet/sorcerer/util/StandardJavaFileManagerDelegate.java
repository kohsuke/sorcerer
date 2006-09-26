package org.jvnet.sorcerer.util;

import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Kohsuke Kawaguchi
 */
public class StandardJavaFileManagerDelegate implements StandardJavaFileManager {
    protected final StandardJavaFileManager core;

    public StandardJavaFileManagerDelegate(StandardJavaFileManager core) {
        this.core = core;
    }

    public boolean isSameFile(FileObject a, FileObject b) {
        return core.isSameFile(a, b);
    }

    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
        return core.getJavaFileObjectsFromFiles(files);
    }

    public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
        return core.getJavaFileObjects(files);
    }

    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
        return core.getJavaFileObjectsFromStrings(names);
    }

    public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
        return core.getJavaFileObjects(names);
    }

    public void setLocation(Location location, Iterable<? extends File> path) throws IOException {
        core.setLocation(location, path);
    }

    public Iterable<? extends File> getLocation(Location location) {
        return core.getLocation(location);
    }

    public ClassLoader getClassLoader(Location location) {
        return core.getClassLoader(location);
    }

    public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
        return core.list(location, packageName, kinds, recurse);
    }

    public String inferBinaryName(Location location, JavaFileObject file) {
        return core.inferBinaryName(location, file);
    }

    public boolean handleOption(String current, Iterator<String> remaining) {
        return core.handleOption(current, remaining);
    }

    public boolean hasLocation(Location location) {
        return core.hasLocation(location);
    }

    public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
        return core.getJavaFileForInput(location, className, kind);
    }

    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
        return core.getJavaFileForOutput(location, className, kind, sibling);
    }

    public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
        return core.getFileForInput(location, packageName, relativeName);
    }

    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
        return core.getFileForOutput(location, packageName, relativeName, sibling);
    }

    public void flush() throws IOException {
        core.flush();
    }

    public void close() throws IOException {
        core.close();
    }

    public int isSupportedOption(String option) {
        return core.isSupportedOption(option);
    }
}
