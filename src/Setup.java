import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Launcher.
 * 
 * @author
 *     Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Setup implements Runnable {

    /**
     * Written as a preamble to the payload.
     * This signature will be followed by the size of the payload
     * (int), then the payload itself.
     */
    private static final byte[] signature = new byte[]{
        (byte)0xde,(byte)0xad,(byte)0xbe,(byte)0xef,
        (byte)0xde,(byte)0xad,(byte)0xbe,(byte)0xef,
        (byte)0xde,(byte)0xad,(byte)0xbe,(byte)0xef,
        (byte)0xde,(byte)0xad,(byte)0xbe,(byte)0xef
    };
    
    
    public static void main( String[] args ) throws Exception {
        // read the beginning of the class file
        InputStream in = Setup.class.getResourceAsStream("Setup.class");
        byte[] buf = new byte[10240];
        int idx=0;
        while( idx<buf.length ) {
            int len = in.read(buf,idx,buf.length-idx);
            if( len<=0 )    break;
            idx += len;
        }
        
        
        
        // then look for the signature.
        int i;
        for( i=0; i<idx-signature.length-4; i++ ) {
            int j;
            for( j=0; j<signature.length; j++ )
                if( signature[j]!=buf[i+j] )
                    break;
                
            if( j==16 )
                break;
        }
        if( i==idx-signature.length-4 ) {
            System.err.println("The package is broken");
            System.exit(-1);
        }
        
        // read the length
        i += signature.length;
        int payloadSize = (buf[i]<<24)+((buf[i+1]<<16)&0xFF0000)
                            +((buf[i+2]<<8)&0xFF00)+((buf[i+3])&0xFF);
        byte[] payload = new byte[payloadSize];
        // remember the offset
        int start = i+4;
        
        in.close();
        
        // re-open the stream and load the contents
        DataInput din = new DataInputStream(Setup.class.getResourceAsStream("Setup.class"));
        din.skipBytes(start);
        din.readFully(payload);

        // de-scramble
        for( i=0; i<payload.length; i++ )
            payload[i] ^= 0xAA;
        
        // write to a temporary jar file
        File jar = File.createTempFile("installer","jar");
        jar.deleteOnExit();
        OutputStream jarWriter = new FileOutputStream(jar);
        jarWriter.write(payload);
        jarWriter.close();
        payload = null; // releast it to reclaim memory 
        
        // build command line
        List cmds = new ArrayList();
        cmds.add(getJavaExe());
        cmds.add("-jar");
        cmds.add(jar.toString());
        cmds.addAll(Arrays.asList(args));
        
        // launch java
        Process proc = Runtime.getRuntime().exec((String[]) cmds.toArray(new String[cmds.size()]));
        new Thread(new Setup(System.in,proc.getOutputStream(),false)).start();
        new Thread(new Setup(proc.getInputStream(),System.out,true)).start();
        new Thread(new Setup(proc.getErrorStream(),System.err,true)).start();
        System.exit(proc.waitFor());
    }

    private static String getJavaExe() {
        try {
            return new File(System.getProperty("java.home"),"java").getPath();
        } catch (SecurityException e) {
            return "java";
        }
    }


    private void copyStream( InputStream in, OutputStream out, boolean closeOut ) throws IOException {
        if(buffering) {
            byte[] buf = new byte[256];
            int len;
            while((len=in.read(buf))>=0) {
                out.write(buf,0,len);
            }
        } else {
            int ch;
            while((ch=in.read())!=-1) {
                out.write(ch);
                if(ch=='\n')
                    out.flush();
            }
        }
        in.close();
        if(closeOut)
            out.close();
    }

    // as an instance class, it implements CopyThread.
    private final InputStream in;
    private final OutputStream out;
    private final boolean buffering;

    private Setup(InputStream in, OutputStream out, boolean buffering) {
        this.in = in;
        this.out = out;
        this.buffering = buffering;
    }
    
    public void run() {
        try {
            copyStream( in, out, false );
        } catch (IOException e) {
            ; // ignore
        }
    }
}
