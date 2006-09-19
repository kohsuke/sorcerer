package org.jvnet.sorcerer.util;

import org.jvnet.sorcerer.Analyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.InputStreamReader;

/**
 * @author Kohsuke Kawaguchi
 */
public final class IOUtil {
    public static void copy(File input, File output) throws IOException {
        copy(new FileInputStream(input),output);
    }

    public static void copy(String resourceName, File output) throws IOException {
        InputStream i = Analyzer.class.getResourceAsStream(resourceName);
        copy(i,output);
    }

    public static void copy(InputStream i, File output) throws IOException {
        OutputStream o = new FileOutputStream(output);
        byte[] buf = new byte[4096];

        int len;

        while((len=i.read(buf))>=0) {
            o.write(buf,0,len);
        }

        i.close();
        o.close();
    }

    public static String readFully(InputStream i) throws IOException {
        StringBuilder b = new StringBuilder();
        Reader r = new InputStreamReader(i);
        char[] buf = new char[4096];
        int len;

        while((len=r.read(buf))>=0)
            b.append(buf,0,len);

        i.close();
        return b.toString();
    }
}
