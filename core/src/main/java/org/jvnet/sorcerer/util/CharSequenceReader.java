package org.jvnet.sorcerer.util;

import java.io.IOException;
import java.io.Reader;

/**
 * @author Kohsuke Kawaguchi
 */
public class CharSequenceReader extends Reader {
    private CharSequence buf;
    private int len;
    private int pos = 0;

    public CharSequenceReader(CharSequence cs) {
        this.buf = cs;
        this.len = cs.length();
    }

    public int read() throws IOException {
        if (pos >= len)
            return -1;
        return buf.charAt(pos++);
    }

    public int read(char[] out, int start, int len) throws IOException {
        if (pos >= this.len)
            return -1;
        int n = Math.min(this.len - pos, len);
        buf.subSequence(pos,pos+n).toString().getChars(0,n,out,start);
        pos += n;
        return n;
    }

    public void close() throws IOException {
        buf=null;
    }
}
