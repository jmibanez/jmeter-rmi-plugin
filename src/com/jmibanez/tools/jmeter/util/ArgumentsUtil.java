/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.jmibanez.tools.jmeter.util;

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
