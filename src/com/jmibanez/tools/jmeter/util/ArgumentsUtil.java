/*
 *
 *
 * See README in the source tree for more info
 *
 */

package com.jmibanez.tools.jmeter.util;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.control.TransactionSampler;
import com.jmibanez.tools.jmeter.RMISampler;

/**
 * Describe class ArgumentsUtil here.
 *
 *
 * Created: Wed Jan 14 15:17:02 2009
 *
 * @author <a href="mailto:jm@jmibanez.com">JM Ibanez</a>
 * @version 1.0
 */
public class ArgumentsUtil {

    /**
     * Creates a new <code>ArgumentsUtil</code> instance.
     *
     */
    private ArgumentsUtil() {
    }


    public static byte[] packArgs(final Object[] args) {
        try {
            ByteArrayOutputStream packOut = new ByteArrayOutputStream();
            ObjectOutputStream ostream = new ObjectOutputStream(packOut);
            ostream.writeObject(args);
            return packOut.toByteArray();
        }
        catch(IOException ign) {
            throw new RuntimeException(ign);
        }
    }

    public static Object[] unpackArgs(final byte[] argsPacked) {
        try {
            ByteArrayInputStream packIn = new ByteArrayInputStream(argsPacked);
            ObjectInputStream istream = new ObjectInputStream(packIn);
            return (Object[]) istream.readObject();
        }
        catch(IOException ign) {
            throw new RuntimeException(ign);
        }
        catch(ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }

    public static void setArguments(Sampler s, Object[] args) {
        if(s instanceof TransactionSampler) {
            s = ((TransactionSampler) s).getSubSampler();
            setArguments(s, args);
            return;
        }

        if(s instanceof RMISampler) {
            ((RMISampler) s).setArguments(args);
        }
    }
}
