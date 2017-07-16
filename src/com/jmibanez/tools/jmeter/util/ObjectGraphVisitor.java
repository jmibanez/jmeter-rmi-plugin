package com.jmibanez.tools.jmeter.util;

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


public class ObjectGraphVisitor<T> {

    private static Log log = LogFactory.getLog(ObjectGraphVisitor.class);

    private ObjectVisitorPolicy visitorPolicy = ObjectVisitorPolicy.DEFAULT;
    private ObjectVisitor<T> objectVisitor;
    private PrimitiveVisitor<T> primitiveVisitor;

    private Set<Object> seenObjects = new HashSet<>();
    private Map<Object, Object> previousReplacements = new HashMap<>();


    public ObjectGraphVisitor(final ObjectVisitor<T> objectVisitor,
                              final PrimitiveVisitor<T> primitiveVisitor) {
        this.objectVisitor = objectVisitor;
        this.primitiveVisitor = primitiveVisitor;
    }

    public ObjectGraphVisitor(final ObjectVisitor<T> objectVisitor,
                              final PrimitiveVisitor<T> primitiveVisitor,
                              final ObjectVisitorPolicy visitorPolicy) {
        this.objectVisitor = objectVisitor;
        this.primitiveVisitor = primitiveVisitor;
        this.visitorPolicy = visitorPolicy;
    }

    public Object visitObject(Object instance, T passedState, String path)
        throws Exception {

        if (instance == null) {
            return null;
        }

        Class clazz = instance.getClass();

        // Handle cycles by skipping already traversed objects
        if (seenObjects.contains(instance)) {
            if (previousReplacements.get(instance) != null) {
                return previousReplacements.get(instance);
            }
            return instance;
        }

        if (instance instanceof Class) {
            // Skip classes
            return instance;
        }

        seenObjects.add(instance);

        // Otherwise, traverse object
        if (clazz.isArray()) {
            Object arr = traverseArray(instance, passedState, path);
            return arr;
        }

        if (instance instanceof Collection) {
            Object col = traverseCollection((Collection) instance,
                                            passedState, path);
            return col;
        }

        // If instanceof Remote, replace with proxy
        Object visitorInstance = objectVisitor.visitObject(instance, passedState,
                                                           path);
        if (visitorInstance != instance) {
            previousReplacements.put(instance, visitorInstance);
        }

        if (visitorPolicy.shouldDescend(instance)) {
            for (Field f: getFieldsUpTo(clazz, Object.class)) {
                if (Modifier.isFinal(f.getModifiers())
                    || Modifier.isStatic(f.getModifiers())) {
                    log.warn("Ignored " + f.toString());
                    // Ignore static or final fields
                    continue;
                }

                Class valType = f.getType();
                Object val = null;
                if (!f.isAccessible()) {
                    f.setAccessible(true);
                }
                try {
                    val = f.get(instance);
                    String fieldPath = path + "." + f.getName();
                    // Handle types
                    // Primitives: Passthrough
                    if (isPrimitive(valType)) {
                        val = visitPrimitive(instance, passedState, fieldPath,
                                             f, valType, val);
                    }
                    else {
                        val = visitObject(val, passedState,
                                          fieldPath);
                    }
                }
                catch (IllegalAccessException accessEx) {
                    continue;
                }
                f.set(instance, val);
            }
        }

        instance = visitorInstance;
        return instance;
    }

    private Object traverseArray(Object arrayInstance,
                                 T passedState,
                                 String path)
        throws Exception {
        int arrLen = Array.getLength(arrayInstance);
        for (int i = 0; i < arrLen; i++) {
            Object o = visitObject(Array.get(arrayInstance, i),
                                   passedState,
                                   path + "[" + i + "]");
            Array.set(arrayInstance, i, o);
        }
        return arrayInstance;
    }

    private Object traverseMapKeyValuePairs(Map m, T passedState,
                                            String path)
        throws Exception {
        for (Object key: m.keySet()) {
            Object val = visitObject(m.get(key),
                                     passedState,
                                     path + ".get(" + key + ")");
            m.put(key, val);
        }
        return m;
    }

    private Object traverseCollection(Collection c, T passedState,
                                      String path)
        throws Exception {
        if (c instanceof Map) {
            return traverseMapKeyValuePairs((Map) c, passedState, path);
        }

        if (c instanceof List) {
            int i = 0;
            for (ListIterator ii = ((List) c).listIterator(); ii.hasNext(); ) {
                Object o = visitObject(ii.next(), passedState,
                                       path + ".get(" + i + ")");
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

    private Object visitPrimitive(final Object o, final T passedState,
                                  final String path,
                                  final Field f, final Class<?> clazz,
                                  Object primitiveVal) {
        if (primitiveVal == null) {
            return null;
        }

        if (clazz == boolean.class || clazz == Boolean.class) {
            return primitiveVisitor.visitBooleanField(o, passedState, path,
                                                      f, (boolean) primitiveVal);
        }
        else if (clazz == byte.class || clazz == Byte.class) {
            return primitiveVisitor.visitByteField(o, passedState, path,
                                                   f, (byte) primitiveVal);
        }
        else if (clazz == char.class || clazz == Character.class) {
            return primitiveVisitor.visitCharField(o, passedState, path,
                                                   f, (char) primitiveVal);
        }
        else if (clazz == short.class || clazz == Short.class) {
            return primitiveVisitor.visitShortField(o, passedState, path,
                                                    f, (short) primitiveVal);
        }
        else if (clazz == int.class || clazz == Integer.class) {
            return primitiveVisitor.visitIntegerField(o, passedState, path,
                                                      f, (int) primitiveVal);
        }
        else if (clazz == long.class || clazz == Long.class) {
            return primitiveVisitor.visitLongField(o, passedState, path,
                                                   f, (long) primitiveVal);
        }
        else if (clazz == float.class || clazz == Float.class) {
            return primitiveVisitor.visitFloatField(o, passedState, path,
                                                    f, (long) primitiveVal);
        }
        else if (clazz == double.class || clazz == Double.class) {
            return primitiveVisitor.visitDoubleField(o, passedState, path,
                                                     f, (long) primitiveVal);
        }
        else if(clazz == String.class) {
            return primitiveVisitor.visitStringField(o, passedState, path,
                                                     f, (String) primitiveVal);
        }

        throw new RuntimeException("Invalid state: call to visitPrimitive for non-primitive; should never happen!");
    }


    public interface ObjectVisitorPolicy {
        public boolean shouldDescend(Object o);

        public static final ObjectVisitorPolicy DEFAULT = new ObjectVisitorPolicy() {
                public final boolean shouldDescend(final Object o) {
                    return true;
                }
            };
    }

    public interface ObjectVisitor<T> {
        public Object visitObject(Object o, T passedState, String path);
    }

    public interface PrimitiveVisitor<T> {
        public boolean visitBooleanField(Object o, T passedState, String path,
                                         Field f, boolean b);
        public byte visitByteField(Object o, T passedState, String path,
                                   Field f, byte b);
        public char visitCharField(Object o, T passedState, String path,
                                   Field f, char c);
        public short visitShortField(Object o, T passedState, String path,
                                     Field f, short s);
        public int visitIntegerField(Object o, T passedState, String path,
                                     Field f, int i);
        public long visitLongField(Object o, T passedState, String path,
                                   Field f, long l);
        public float visitFloatField(Object o, T passedState, String path,
                                     Field f, float ff);
        public double visitDoubleField(Object o, T passedState, String path,
                                       Field f, double dd);

        public String visitStringField(Object o, T passedState, String path,
                                       Field f, String str);


        public static <T> PrimitiveVisitor<T> defaultPrimitiveVisitor(Class<T> state) {
            return new PrimitiveVisitor<T>() {
                public boolean visitBooleanField(Object o, T passedState, String path,
                                                 Field f, boolean b) {
                    return b;
                }
                public byte visitByteField(Object o, T passedState, String path,
                                           Field f, byte b) {
                    return b;
                }
                public char visitCharField(Object o, T passedState, String path,
                                           Field f, char c) {
                    return c;
                }
                public short visitShortField(Object o, T passedState, String path,
                                             Field f, short s) {
                    return s;
                }
                public int visitIntegerField(Object o, T passedState, String path,
                                             Field f, int i) {
                    return i;
                }
                public long visitLongField(Object o, T passedState, String path,
                                           Field f, long l) {
                    return l;
                }
                public float visitFloatField(Object o, T passedState, String path,
                                             Field f, float ff) {
                    return ff;
                }
                public double visitDoubleField(Object o, T passedState, String path,
                                               Field f, double dd) {
                    return dd;
                }

                public String visitStringField(Object o, T passedState, String path,
                                               Field f, String str) {
                    return str;
                }
            };
        }

    }
}
