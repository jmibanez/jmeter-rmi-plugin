package com.jmibanez.tools.jmeter;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Array;
import java.rmi.Remote;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static com.jmibanez.tools.jmeter.util.InstanceHandleFactory.buildInstanceName;
import static com.jmibanez.tools.jmeter.util.ReflectionUtil.getFieldsUpTo;


public class ProxyObjectGraph {

    private static Log log = LogFactory.getLog(ProxyObjectGraph.class);

    private InstanceRegistry instanceRegistry;
    private MethodRecorder recorder;

    private Set<Object> seenObjects = new HashSet<>();
    private Map<Object, Object> previousRemotes = new HashMap<>();
    private Map<String, String> instances = new HashMap<>();

    public ProxyObjectGraph(final InstanceRegistry instanceRegistry,
                            final MethodRecorder recorder) {
        this.instanceRegistry = instanceRegistry;
        this.recorder = recorder;
    }

    public Map<String, String> getAndClearRemoteInstanceHandles() {
        Map<String, String> mm = new HashMap<>(instances);
        instances.clear();
        return mm;
    }

    public Object replaceRemotes(Object instance, MethodCallRecord record)
        throws Exception {
        return replaceRemotes(instance, record, "");
    }

    public Object replaceRemotes(Object instance, MethodCallRecord record,
                                 String path)
        throws Exception {

        if (instance == null) {
            return null;
        }

        Class<?> clazz = instance.getClass();

        // Handle types
        // Primitives: Passthrough
        if (isPrimitive(clazz)) {
            return instance;
        }

        // Handle cycles by skipping already traversed objects
        if (seenObjects.contains(instance)) {
            if (previousRemotes.get(instance) != null) {
                return previousRemotes.get(instance);
            }
            return instance;
        }

        if (instance instanceof Class<?>) {
            // Skip classes
            return instance;
        }

        seenObjects.add(instance);

        // If instanceof Remote, replace with proxy
        if (instance instanceof Remote) {
            record.setRemoteReturned(true);
            String instanceName = buildInstanceName((Remote) instance,
                                                    record, path);
            DynamicStubProxyInvocationHandler handler =
                new DynamicStubProxyInvocationHandler(instanceRegistry,
                                                      instance,
                                                      instanceName,
                                                      recorder);
            Remote proxy = handler.buildStubProxy(false);
            instanceRegistry.registerRmiInstance(instanceName, proxy);
            instances.put(instanceName, path);
            previousRemotes.put(instance, proxy);
            return proxy;
        }

        // Otherwise, traverse object
        if (clazz.isArray()) {
            Object arr = traverseArrayAndReplaceRemotes(instance, record,
                                                        path);
            return arr;
        }

        if (instance instanceof Collection) {
            Object col = traverseCollectionAndReplaceRemotes((Collection) instance,
                                                             record, path);
            return col;
        }

        for (Field f: getFieldsUpTo(clazz, Object.class)) {
            if (Modifier.isFinal(f.getModifiers())
                || Modifier.isStatic(f.getModifiers())) {
                log.warn("Ignored " + f.toString());
                // Ignore static or final fields
                continue;
            }

            Class<?> valType = f.getType();
            Object val = null;
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            try {
                val = replaceRemotes(f.get(instance),
                                     record, path + "." + f.getName());
            }
            catch (IllegalAccessException accessEx) {
                continue;
            }
            f.set(instance, val);
        }

        return instance;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object traverseArrayAndReplaceRemotes(Object arrayInstance,
                                                  MethodCallRecord record,
                                                  String path)
        throws Exception {
        int arrLen = Array.getLength(arrayInstance);
        for (int i = 0; i < arrLen; i++) {
            Object o = replaceRemotes(Array.get(arrayInstance, i),
                                      record, path + "[" + i + "]");
            Array.set(arrayInstance, i, o);
        }
        return arrayInstance;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object traverseMapKeyValuePairs(Map m, MethodCallRecord record,
                                            String path)
        throws Exception {
        for (Object key: m.keySet()) {
            Object val = replaceRemotes(m.get(key), record,
                                        path + ".get(" + key + ")");
            m.put(key, val);
        }
        return m;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object traverseCollectionAndReplaceRemotes(Collection c,
                                                       MethodCallRecord record,
                                                       String path)
        throws Exception {
        if (c instanceof Map) {
            return traverseMapKeyValuePairs((Map) c, record, path);
        }

        if (c instanceof List) {
            int i = 0;
            for (ListIterator ii = ((List) c).listIterator(); ii.hasNext(); ) {
                Object o = replaceRemotes(ii.next(), record, path + ".get(" + i + ")");
                ii.set(o);
                i++;
            }
        }

        // FIXME: Handle other collection types

        return c;
    }

    private boolean isPrimitive(final Class<?> clazz) {
        return (clazz == boolean.class
                || clazz == char.class
                || clazz == byte.class
                || clazz == short.class
                || clazz == int.class
                || clazz == long.class
                || clazz == float.class
                || clazz == double.class
                || clazz == Character.class
                || clazz == Byte.class
                || clazz == Short.class
                || clazz == Boolean.class
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == Float.class
                || clazz == Double.class
                || clazz == String.class);
    }
}
