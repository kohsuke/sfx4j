package org.kohsuke.sfx4j;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * External process wrapper.
 * 
 * <p>
 * stdout/stderr of the process will be sent to the specified stream.
 * 
 * @author
 *     Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class Proc {
    
    private final Process proc;
    
    private Thread out,err;

    /**
     * Launches a process
     */
    public Proc( String cmdLine ) throws IOException {
        this(cmdLine,System.out,System.err);
    }

    /**
     * Launches a process
     */
    public Proc( String cmdLine, OutputStream stdout, OutputStream stderr ) throws IOException {
        proc = Runtime.getRuntime().exec(cmdLine);
        out = new CopyThread(proc.getInputStream(),stdout);
        err = new CopyThread(proc.getErrorStream(),stderr);
        out.start();
        err.start();
    }
    
    public int join() {
        try {
            int r = proc.waitFor();
            out.join();
            err.join();
            return r;
        } catch (InterruptedException e) {
            e.printStackTrace();
            // is this possible? 
            throw new InternalError();
        }
    }

    private static class CopyThread extends Thread {
        private final InputStream in;
        private final OutputStream out;
        
        public CopyThread(InputStream in, OutputStream out) {
            super();
            this.in = in;
            this.out = out;
        }
        
        public void run() {
            try {
                Util.copyStream( in, out, false );
            } catch (IOException e) {
                ; // ignore
            }
        }
    };
}
