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
import com.orangeandbronze.tools.jmeter.RMIRemoteObjectConfig;
import com.orangeandbronze.tools.jmeter.util.ScriptletGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    private static Log log = LogFactory.getLog(RmiSamplerGeneratorMethodRecorder.class);

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

    private String createArgumentsScript(MethodCallRecord record) {
        log.info("Creating script for method call record");

        Class[] argTypes = record.getArgumentTypes();
        Object[] args = record.getArguments();

        if(argTypes == null || argTypes.length == 0) {
            return "// No arguments\nmethodArgs ( ) { return null; }";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("methodArgs ( ) {\n");
        for(int i = 0; i < args.length; i++) {
            if(args[i] == null) {
                continue;
            }

            sb.append(ScriptletGenerator.getInstance()
                      .generateScriptletForObject(args[i], "args" + i, argTypes[i]));
        }

        sb.append("Object[] args = new Object[] { ");
        for(int i = 0; i < args.length; i++) {
            if(args[i] != null) {
                sb.append("args");
                sb.append(i);
            }
            else {
                sb.append("null");
            }

            if(i != args.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(" };\n");
        sb.append("return args;");
        sb.append("\n}\n");
        return sb.toString();
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
        sampler.setArgumentsScript(createArgumentsScript(r));
        target.deliverSampler(sampler, r);
    }

}
