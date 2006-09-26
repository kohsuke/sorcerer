package org.jvnet.sorcerer.util;

import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * {@link StandardJavaFileManager} that expands all tab character to work
 * around a bug in javac where it mishandles TAB calculation.
 *
 * @author Kohsuke Kawaguchi
 */
public class TabExpandingFileManager extends StandardJavaFileManagerDelegate {

    private final int tabWidth;
    private final Charset charset;

    public TabExpandingFileManager(StandardJavaFileManager core, Charset charset, int tabWidth) {
        super(core);
        this.charset = charset;
        this.tabWidth = tabWidth;
    }

    private Iterable<JavaFileObject> wrap(final Iterable<? extends JavaFileObject> core) {
        return new Iterable<JavaFileObject>() {
            public Iterator<JavaFileObject> iterator() {
                return new IteratorAdapter<JavaFileObject,JavaFileObject>(core.iterator()) {
                    protected JavaFileObject filter(JavaFileObject u) {
                        return new JavaFileObjectImpl(u);
                    }
                };
            }
        };
    }

    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
        return wrap(super.getJavaFileObjectsFromFiles(files));
    }

    public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
        return wrap(super.getJavaFileObjects(files));
    }

    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
        return wrap(super.getJavaFileObjectsFromStrings(names));
    }

    public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
        return wrap(super.getJavaFileObjects(names));
    }

    public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
        return wrap(super.list(location, packageName, kinds, recurse));
    }

    public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
        return new JavaFileObjectImpl(super.getJavaFileForInput(location, className, kind));
    }

    public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
        return super.getFileForInput(location, packageName, relativeName);
    }

    public String inferBinaryName(Location location, JavaFileObject file) {
        return core.inferBinaryName(location, ((JavaFileObjectImpl)file).getCore());
    }


    private final class JavaFileObjectImpl extends JavaFileObjectDelegate {
        public JavaFileObjectImpl(JavaFileObject core) {
            super(core);
        }

        JavaFileObject getCore() {
            return core;
        }

        public InputStream openInputStream() throws IOException {
            // this is apparently only used for binary files.
            return super.openInputStream();
            //StringBuilder sb = getCharContent(true);
            //ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //( charset ==null ? new OutputStreamWriter(baos) : new OutputStreamWriter(baos,charset) )
            //    .write(sb.toString()); // ouch!
            //return new ByteArrayInputStream(baos.toByteArray()); // another ouch!
        }

        public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
            // TODO: implement this method later
            throw new UnsupportedOperationException();
        }

        public StringBuilder getCharContent(boolean ignoreEncodingErrors) throws IOException {
            CharSequence cs = super.getCharContent(ignoreEncodingErrors);
            StringBuilder sb = new StringBuilder(cs.length());

            int pos=0;
            for( int i=0; i<cs.length(); i++ ) {
                char ch = cs.charAt(i);
                if(ch=='\t') {
                    int nextStop = ((pos/tabWidth)+1)*tabWidth;
                    for( ; pos<nextStop; pos++ )
                        sb.append(' ');
                } else {
                    sb.append(ch);
                    if(ch=='\r' || ch=='\n')
                        pos=0;
                    else
                        pos++;
                }
            }

            return sb;
        }
    }
}
