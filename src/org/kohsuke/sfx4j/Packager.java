package org.kohsuke.sfx4j;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Unknown;
import org.apache.bcel.generic.ConstantPoolGen;


/**
 * Generates the installer class file.
 * 
 * @author
 *     Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Packager {
    public static void main( String[] args ) throws IOException {
        if( args.length!=2 ) {
            System.out.println("Usage: Packager <jar file to be packed> <class name>");
            System.exit(-1);
        }

        String className = args[1];
        InputStream contents = new FileInputStream(args[0]);
        
        File setupSourceFile = new File(className+".java");
        setupSourceFile.deleteOnExit();
        
        // create the source file
        copyWithReplace(
                new InputStreamReader(Packager.class.getResourceAsStream("/Setup.java")),
                new FileWriter(setupSourceFile),
                "Setup", className );

         // invoke javac and have it compile the code
        new Proc("javac "+setupSourceFile).join();
        
        File setupClassFile = new File(className+".class");
        
        ClassParser parser = new ClassParser(setupClassFile.toString());
        JavaClass c = parser.parse();
        ConstantPoolGen gen = new ConstantPoolGen(c.getConstantPool());
        int name = gen.addUtf8("installer-contents");
        c.setConstantPool(gen.getFinalConstantPool());
        
        byte[] payload = pack(contents);
        
        Attribute[] atts = c.getAttributes();
        Attribute[] newAtts = new Attribute[atts.length+1];
        System.arraycopy(atts,0,newAtts,0,atts.length);
        newAtts[atts.length] = new Unknown(
                name, payload.length, payload, c.getConstantPool() );
        c.setAttributes(newAtts);

        // overwrite
        c.dump(setupClassFile);
    }

    private static byte[] readStream(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Util.copyStream(in,baos,true);
        return baos.toByteArray();
    }
    
    private static void copyWithReplace( Reader in, Writer out, String oldToken, String newToken ) throws IOException {
        String line;
        BufferedReader r = new BufferedReader(in);
        while((line=r.readLine())!=null) {
            line = line.replaceAll(oldToken,newToken);
            out.write(line);
            out.write('\n');
        }
        in.close();
        out.close();
    }

    private static byte[] pack(InputStream contents ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        byte[] buf = readStream(contents);
        try {
            // write the signature
            for( int i=0; i<4; i++ )
                dos.writeInt(0xDeadBeef);
            // write the length of the contents
            dos.writeInt(buf.length);
            // then the contents
            dos.write(buf);
        } catch( IOException e ) {
            // impossible
            throw new InternalError();
        }
        return baos.toByteArray();
    }
}
