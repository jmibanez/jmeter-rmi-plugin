package com.jmibanez.tools.jmeter.impl;

import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;

import com.jmibanez.tools.jmeter.InstanceParameterRegistry;
import com.jmibanez.tools.jmeter.InstanceRegistry;
import com.jmibanez.tools.jmeter.RMISampler;
import com.jmibanez.tools.jmeter.RMIRemoteObjectConfig;

public class RemoteRegistry
    implements InstanceRegistry, InstanceParameterRegistry {

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
        log.debug("Register: " + key);
        registerInstanceAtKey(key, instance);
        return key;
    }

    private void registerInstanceAtKey(final String key, final Remote instance)
        throws RemoteException {
        if (!methodTypesMap.containsKey(key)) {
            Map<String, Class[]> instanceMethodTypesMap = configureMethodBindings(instance);
            methodTypesMap.put(key, instanceMethodTypesMap);
        }
        else {
            log.warn("Methods already registered: " + key);
        }
        if (!instanceRef.containsKey(key)) {
            instanceRef.put(key, instance);
            assert (instanceRef.containsKey(key)): "Map should contain key";
        }
        else {
            log.warn("Instance already registered: " + key);
        }
    }

    boolean hasInstance(final String key) {
        return instanceRef.containsKey(key);
    }

    @Override
    public Remote getTarget(final String key) {
        return instanceRef.get(key);
    }

    @Override
    public Class[] getArgumentTypes(final String key, final String methodName) {
        return methodTypesMap.get(key).get(methodName);
    }

    @Override
    public void setArgumentTypes(String key, String methodName,
                                 Class[] argTypes) {
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
