package org.kohsuke.sfx4j;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Work around for a bug in JRE.
 *
 * @author Kohsuke Kawaguchi
 */
final class ChunkedOutputStream extends FilterOutputStream {
    ChunkedOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * To work around a bug in deflator in Java5, feed data in small chunk.
     */
    public void write(byte b[], int off, int len) throws IOException {
        while(len>0) {
            int sz = Math.min(CHUNK,len);
            super.write(b,off,sz);
            off+=sz;
            len-=sz;
        }
    }

    private static final int CHUNK = 1024;
}
