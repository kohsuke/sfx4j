package org.kohsuke.sfx4j;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 * 
 * @author
 *     Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Util {
    public static void copyStream( InputStream in, OutputStream out, boolean closeOut ) throws IOException {
        byte[] buf = new byte[256];
        int len;
        while((len=in.read(buf))>=0) {
            out.write(buf,0,len);
        }
        in.close();
        if(closeOut)
            out.close();
    }
}
