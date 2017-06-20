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
import java.rmi.RemoteException;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.StringProperty;
import com.orangeandbronze.tools.jmeter.gui.RMIRemoteObjectConfigGUI;
import java.lang.reflect.Method;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    extends ConfigTestElement
    implements TestStateListener {

    public static final String TARGET_RMI_NAME = "RmiRemoteObjectConfig.target_rmi_name";
    public static final String REMOTE_INSTANCES = "RMIRemoteObject.instances";

    private transient RemoteRegistry registry;

    private static Log log = LogFactory.getLog(RMISampler.class);

    /**
     * Creates a new <code>RMIRemoteObjectConfig</code> instance.
     *
     */
    public RMIRemoteObjectConfig() {
    }

    @Override
    public boolean expectsModification() {
        return true;
    }

    public Class getGuiClass() {
        return com.orangeandbronze.tools.jmeter.gui.RMIRemoteObjectConfigGUI.class;
    }

    public Class[] getArgumentTypes(final String targetName, final String methodName) {
        return getRegistry().getArgumentTypes(targetName, methodName);
    }

    public void setArgumentTypes(String targetName, String methodName, Class[] argTypes) {
        getRegistry().setArgumentTypes(targetName, methodName, argTypes);
    }

    public void testStarted() {
        testStarted(null);
    }

    public void testStarted(final String host) {
        log.info("Configuring remote stub registry");
        registry = new RemoteRegistry();
        JMeterContext jmctx = JMeterContextService.getContext();
        jmctx.getVariables().putObject(REMOTE_INSTANCES, registry);
    }

    public void testEnded() {
        testEnded(null);
    }

    public void testEnded(final String host) {
        log.info("Removing remote stub registry");
        JMeterContext jmctx = JMeterContextService.getContext();
        jmctx.getVariables().remove(REMOTE_INSTANCES);
    }

    public Remote getTarget(final String targetName) {
        assert (targetName != null && getRegistry().hasInstance(targetName)): "Map should contain key";

        Remote target = getRegistry().getTarget(targetName);
        if(target == null && targetName == null) {
            try {
                target = (Remote) Naming.lookup(getTargetRmiName());
                getRegistry().registerRootRmiInstance(target);
            }
            catch(Exception ignored) {
                throw new RuntimeException(ignored);
            }
        }
        else {
            // log.warn("target == null");
        }

        return target;
    }

    public String getTargetRmiName() {
        return getPropertyAsString(TARGET_RMI_NAME);
    }

    public void setTargetRmiName(String value) {
        setProperty(new StringProperty(TARGET_RMI_NAME, value));
    }

    private RemoteRegistry getRegistry() {
        if (registry == null) {
            JMeterContext jmctx = JMeterContextService.getContext();
            registry = (RemoteRegistry) jmctx.getVariables().getObject(REMOTE_INSTANCES);
        }

        return registry;
    }

    public static class RemoteRegistry
        implements InstanceRegistry {

        private Map<String, Map<String, Class[]>> methodTypesMap = new HashMap<>();
        private Map<String, Remote> instanceRef = new HashMap<>();

        private static Log log = LogFactory.getLog(RemoteRegistry.class);

        @Override
        public void registerRootRmiInstance(final Remote instance)
            throws RemoteException {
            log.info("Register root");
            registerInstanceAtKey(null, instance);
        }

        @Override
        public String registerRmiInstance(final String key, final Remote instance)
            throws RemoteException {
            log.info("Register: " + key);
            registerInstanceAtKey(key, instance);
            return key;
        }

        private void registerInstanceAtKey(final String key, final Remote instance)
            throws RemoteException {
            if (!methodTypesMap.containsKey(key)) {
                Map<String, Class[]> instanceMethodTypesMap = configureMethodBindings(instance);
                methodTypesMap.put(key, instanceMethodTypesMap);
            }
            if (!instanceRef.containsKey(key)) {
                instanceRef.put(key, instance);
                assert (instanceRef.containsKey(key)): "Map should contain key";
            }
        }

        boolean hasInstance(final String key) {
            return instanceRef.containsKey(key);
        }

        @Override
        public Remote getTarget(final String key) {
            return instanceRef.get(key);
        }

        Class[] getArgumentTypes(final String key, final String methodName) {
            return methodTypesMap.get(key).get(methodName);
        }

        void setArgumentTypes(String key, String methodName, Class[] argTypes) {
            methodTypesMap.get(key).put(methodName, argTypes);
        }

        private Map<String, Class[]> configureMethodBindings(Remote target) {
            Map<String, Class[]> instanceMethodTypesMap = new HashMap<String, Class[]>();
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
                instanceMethodTypesMap.put(methodName, argTypes);
            }
            return instanceMethodTypesMap;
        }

    }
}
