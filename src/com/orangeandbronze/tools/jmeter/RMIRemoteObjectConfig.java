/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.orangeandbronze.tools.jmeter;

import org.apache.jmeter.config.ConfigTestElement;
import java.util.Map;
import java.rmi.server.RemoteObject;
import java.rmi.Naming;
import java.util.HashMap;
import java.rmi.Remote;
import org.apache.jmeter.testelement.property.StringProperty;
import com.orangeandbronze.tools.jmeter.gui.RMIRemoteObjectConfigGUI;
import java.lang.reflect.Method;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;

/**
 * Describe class RMIRemoteObjectConfig here.
 *
 *
 * Created: Fri Nov 14 13:08:07 2008
 *
 * @author <a href="mailto:jm@orangeandbronze.com">JM Ibanez</a>
 * @version 1.0
 */
public class RMIRemoteObjectConfig
    extends ConfigTestElement {

    public static final String TARGET_RMI_NAME = "RmiRemoteObjectConfig.target_rmi_name";
    public static final String REMOTE_OBJ = "RMI_RemoteObject";

    private Map<SequenceID, Object[]> argumentsMap;
    private Map<String, Class[]> methodTypesMap;
    private Map<String, Integer> sequenceMap;

    private boolean methodBindingsConfigured = false;


    /**
     * Creates a new <code>RMIRemoteObjectConfig</code> instance.
     *
     */
    public RMIRemoteObjectConfig() {
        argumentsMap = new HashMap<SequenceID, Object[]>();
        sequenceMap = new HashMap<String,Integer>();
        methodTypesMap = new HashMap<String, Class[]>();

        sequenceMap.put("getResource", 1);
        methodTypesMap.put("getResource", new Class[] { String.class, String.class, String.class });
        SequenceID seq = new SequenceID("getResource", Integer.toString(1));
        argumentsMap.put(seq, new Object[] { null, "app.ini", ""});

        System.out.println(argumentsMap);
        System.out.println(sequenceMap);
    }

    @Override
    public boolean expectsModification() {
        return true;
    }

    public Class getGuiClass() {
        return com.orangeandbronze.tools.jmeter.gui.RMIRemoteObjectConfigGUI.class;
    }

    public Object[] getArguments(String methodName, String sequenceId) {
        SequenceID seq = new SequenceID(methodName, sequenceId);
        return argumentsMap.get(seq);
    }

    public Class[] getArgumentTypes(String methodName) {
        if(methodName.equals("setCount")) {
            return new Class[] { int.class };
        }
        return methodTypesMap.get(methodName);
    }

    public void setArgumentTypes(String methodName, Class[] argTypes) {
        methodTypesMap.put(methodName, argTypes);
    }

    // Return sequence ID
    public String addNewArguments(String methodName, Object[] args) {
        Integer seqNo_r = sequenceMap.get(methodName);
        int seqNo = 1;

        if(seqNo_r != null) {
            seqNo = seqNo_r;
            seqNo++;
        }

        sequenceMap.put(methodName, seqNo);

        String sequenceId = Integer.toString(seqNo);
        SequenceID seq = new SequenceID(methodName, sequenceId);
        argumentsMap.put(seq, args);

        return sequenceId;
    }

    public synchronized Remote getTarget() {
        JMeterContext jmctx = JMeterContextService.getContext();
        Remote target = (Remote) jmctx.getVariables().getObject(REMOTE_OBJ);

        if(target == null) {
            try {
                target = (Remote) Naming.lookup(getTargetRmiName());
                if(!methodBindingsConfigured) {
                    configureMethodBindings(target);
                }
                jmctx.getVariables().putObject(REMOTE_OBJ, target);
            }
            catch(Exception ignored) {
                throw new RuntimeException(ignored);
            }
        }

        return target;
    }

    public String getTargetRmiName() {
        return getPropertyAsString(TARGET_RMI_NAME);
    }

    public void setTargetRmiName(String value) {
        setProperty(new StringProperty(TARGET_RMI_NAME, value));
    }

    private void configureMethodBindings(Remote target) {
        Class targetClass = target.getClass();
        Method[] targetMethods = targetClass.getMethods();
        for(Method m : targetMethods) {
            String rawMethodName = m.getName();
            Class[] argTypes = m.getParameterTypes();
            StringBuilder sb = new StringBuilder();
            sb.append(rawMethodName);

            if(argTypes == null || argTypes.length == 0) {
                sb.append(":");
            }
            else {
                sb.append(":");
                for(Class c : argTypes) {
                    sb.append(c.getName());
                    sb.append(",");
                }

                sb.deleteCharAt(sb.length() - 1);
            }

            String methodName = sb.toString();
            methodTypesMap.put(methodName, argTypes);
        }
        methodBindingsConfigured = true;
    }

    public static class SequenceID {
        private String method;
        private String sequenceId;

        public SequenceID(String method, String sequenceId) {
            this.method = method;
            this.sequenceId = sequenceId;
        }

        public String getMethod() {
            return method;
        }

        public String getSequenceId() {
            return sequenceId;
        }

        public boolean equals(Object other) {
            if(! (other instanceof SequenceID)) {
                return false;
            }

            SequenceID otherS = (SequenceID) other;
            return (method.equals(otherS.method)
                    && sequenceId.equals(otherS.sequenceId));
        }
    }
}
