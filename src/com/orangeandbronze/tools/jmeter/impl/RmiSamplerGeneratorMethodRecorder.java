/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.orangeandbronze.tools.jmeter.impl;

import java.rmi.RemoteException;
import java.util.Map;
import com.orangeandbronze.tools.jmeter.MethodCallRecord;
import com.orangeandbronze.tools.jmeter.MethodRecorder;
import com.orangeandbronze.tools.jmeter.NativeRmiProxyController;
import com.orangeandbronze.tools.jmeter.RMISampler;
import com.orangeandbronze.tools.jmeter.gui.RMISamplerGUI;
import org.apache.jmeter.extractor.BeanShellPostProcessor;
import org.apache.jmeter.protocol.java.sampler.BeanShellSampler;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.BooleanProperty;
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

    private String createResultRegisterScript(final MethodCallRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Object ret = prev.getReturnValue();\n");
        sb.append("com.orangeandbronze.tools.jmeter.InstanceRegistry reg = vars.getObject(\"RMIRemoteObject.instances\");");
        sb.append("\n");
        sb.append("// REPLACE_ME: name of remote in registry \n");
        sb.append("// ret: actual object path to Remote \n");
        Map<String, String> paths = r.getRemotePathsInReturn();
        for (String handle: paths.keySet()) {
            String pathKey = paths.get(handle);
            sb.append("reg.registerRmiInstance(\"");
            sb.append(handle);
            sb.append("\", ret");
            sb.append(pathKey);
            sb.append(");");
        }
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
        BeanShellPostProcessor retValProc = null;
        sampler.setProperty(TestElement.TEST_CLASS, RMISampler.class.getName());
        sampler.setProperty(TestElement.GUI_CLASS, RMISamplerGUI.class.getName());
        sampler.setTargetName(r.getTarget());
        sampler.setMethodName(r.getMethod());
        sampler.setArgumentsScript(createArgumentsScript(r));

        if(r.isRemoteReturned()) {
            retValProc = new BeanShellPostProcessor();
            retValProc.setProperty(TestElement.TEST_CLASS,
                                   BeanShellPostProcessor.class.getName());
            retValProc.setProperty(TestElement.GUI_CLASS,
                                   TestBeanGUI.class.getName());
            // Add postprocessor to register remote instance return value
            String script = createResultRegisterScript(r);
            retValProc.setName("Save remote in return value");
            retValProc.setProperty(new BooleanProperty(BeanShellSampler.RESET_INTERPRETER, true));
            retValProc.setProperty("script", script);
            retValProc.setScript(script);
            sampler.addTestElement(retValProc);
        }
        target.deliverSampler(sampler, retValProc, r);
    }

}
