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

    private Map<String, Class[]> methodTypesMap;

    private boolean methodBindingsConfigured = false;


    /**
     * Creates a new <code>RMIRemoteObjectConfig</code> instance.
     *
     */
    public RMIRemoteObjectConfig() {
        methodTypesMap = new HashMap<String, Class[]>();
    }

    @Override
    public boolean expectsModification() {
        return true;
    }

    public Class getGuiClass() {
        return com.orangeandbronze.tools.jmeter.gui.RMIRemoteObjectConfigGUI.class;
    }

    public Class[] getArgumentTypes(String methodName) {
        return methodTypesMap.get(methodName);
    }

    public void setArgumentTypes(String methodName, Class[] argTypes) {
        methodTypesMap.put(methodName, argTypes);
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
}
