/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.orangeandbronze.tools.jmeter.impl;

import java.rmi.RemoteException;
import com.orangeandbronze.tools.jmeter.MethodCallRecord;
import com.orangeandbronze.tools.jmeter.MethodRecorder;
import com.orangeandbronze.tools.jmeter.NativeRmiProxyController;
import com.orangeandbronze.tools.jmeter.RMISampler;
import org.apache.jmeter.testelement.TestElement;
import com.orangeandbronze.tools.jmeter.gui.RMISamplerGUI;

/**
 * Describe class RmiSamplerGeneratorMethodRecorder here.
 *
 *
 * Created: Sun Nov 16 21:08:26 2008
 *
 * @author <a href="mailto:jm@orangeandbronze.com">JM Ibanez</a>
 * @version 1.0
 */
public class RmiSamplerGeneratorMethodRecorder
    implements MethodRecorder {

    private NativeRmiProxyController target;


    /**
     * Creates a new <code>RmiSamplerGeneratorMethodRecorder</code> instance.
     *
     */
    public RmiSamplerGeneratorMethodRecorder() {

    }


    /**
     * Gets the value of target
     *
     * @return the value of target
     */
    public NativeRmiProxyController getTarget() {
        return this.target;
    }

    /**
     * Sets the value of target
     *
     * @param argTarget Value to assign to this.target
     */
    public void setTarget(NativeRmiProxyController argTarget) {
        this.target = argTarget;
    }



    // Implementation of com.orangeandbronze.tools.jmeter.MethodRecorder

    /**
     * Describe <code>recordCall</code> method here.
     *
     * @param methodCallRecord a <code>MethodCallRecord</code> value
     * @exception RemoteException if an error occurs
     */
    public void recordCall(final MethodCallRecord r)
        throws RemoteException {

        RMISampler sampler = new RMISampler();
        sampler.setProperty(TestElement.GUI_CLASS, RMISamplerGUI.class.getName());
        sampler.setMethodName(r.getMethod());
        target.deliverSampler(sampler, r);
    }

}
