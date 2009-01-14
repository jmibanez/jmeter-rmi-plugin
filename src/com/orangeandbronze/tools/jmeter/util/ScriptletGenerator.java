/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.orangeandbronze.tools.jmeter.util;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.util.Collection;
import java.lang.reflect.Modifier;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map;

/**
 * Describe class ScriptletGenerator here.
 *
 *
 * Created: Tue Jan 13 18:08:30 2009
 *
 * @author <a href="mailto:jm@orangeandbronze.com">JM Ibanez</a>
 * @version 1.0
 */
public class ScriptletGenerator {

    private static ScriptletGenerator instance = null;
    static {
        instance = new ScriptletGenerator();
    }

    /**
     * Creates a new <code>ScriptletGenerator</code> instance.
     *
     */
    ScriptletGenerator() {
    }

    public static ScriptletGenerator getInstance() {
        return instance;
    }

    public String generateScriptletForObject(Object bean, String varname) {
        return generateScriptletForObject(bean, varname, null);
    }

    public String generateScriptletForObject(Object bean, String varname, Class varTypeHint) {
        if(bean == null) {
            if(varTypeHint != null) {
                return varTypeHint.getCanonicalName() + " " + varname + " = null;/* Null */\n";
            }

            return varname + " = null;/* Null */\n";
        }

        Class beanType = bean.getClass();

        // Handle types
        // Array: Unpack
        if(beanType.isArray()) {
            return unpackArray(varname, bean);
        }

        // Collection: Unpack, depending on type
        if(bean instanceof Collection) {
            return unpackCollection(varname, (Collection) bean);
        }

        // Primitives: as-is
        if(beanType == boolean.class
           || beanType == int.class
           || beanType == long.class
           || beanType == float.class
           || beanType == double.class) {
            return beanType.toString() + " " + varname + " = " + bean + ";\n";
        }

        if(beanType == Boolean.class
           || beanType == Integer.class
           || beanType == Long.class
           || beanType == Float.class
           || beanType == Double.class) {
            return beanType.toString() + " " + varname + " = " + bean + ";\n";
        }

        if(beanType == String.class) {
            return "String " + varname + " = " + stringAsScriptlet((String) bean) + ";\n";
        }

        // Object: introspect
        // Assume bean follows standard JavaBean conventions,
        // fallback on using public fields

        StringBuilder scriptlet = new StringBuilder();
        scriptlet.append(beanType.getCanonicalName());
        scriptlet.append(" ");
        scriptlet.append(varname);
        scriptlet.append(" = new ");
        scriptlet.append(beanType.getCanonicalName());
        scriptlet.append("();\n");

        try {
            scriptlet.append(scriptletFromIntrospection(bean, varname));
        }
        catch(IntrospectionException ignored) {
        }

        scriptlet.append(scriptletFromPubFields(bean, varname));

        return scriptlet.toString();
    }

    private String scriptletFromIntrospection(Object bean, String varname)
        throws IntrospectionException {
        StringBuilder scriptlet = new StringBuilder();

        Class beanClass = bean.getClass();
        BeanInfo bi = Introspector.getBeanInfo(beanClass);
        int objCount = 1;
        if(bi != null) {
            PropertyDescriptor[] props = bi.getPropertyDescriptors();
            for(PropertyDescriptor p : props) {
                if("class".equals(p.getName())) {
                    continue;
                }

                if(p.getReadMethod() == null
                   || p.getWriteMethod() == null) {
                    continue;
                } 

                Object val = null;
                try {
                    val = p.getReadMethod().invoke(bean);
                }
                catch(IllegalAccessException accessEx) {
                    // Filler value
                    val = "/* Couldn't populate value */";
                }
                catch(InvocationTargetException invokEx) {
                    val = "/* Couldn't populate value */";
                }

                String varname_subvar = varname + "_beanArg" + objCount;
                objCount++;
                String argScriptlet = generateScriptletForObject(val, varname_subvar);
                scriptlet.append(argScriptlet);
                scriptlet.append(varname);
                scriptlet.append(".");
                scriptlet.append(p.getWriteMethod().getName());

                scriptlet.append("(");
                scriptlet.append(varname_subvar);
                scriptlet.append(");");
                scriptlet.append("\n");
            }
        }

        return scriptlet.toString();
    }

    private String scriptletFromPubFields(Object bean, String varname) {
        Class beanClass = bean.getClass();
        StringBuilder scriptlet = new StringBuilder();

        // Get all public fields
        Field[] fields = beanClass.getFields();
        int objCount = 1;
        for(Field f : fields) {
            if(!(Modifier.isPublic(f.getModifiers())
                 && !Modifier.isStatic(f.getModifiers()))) {
                continue;
            }

            Class valType = f.getType();
            Object val = null;
            try {
                val = f.get(bean);
            }
            catch(IllegalAccessException accessEx) {
                // Filler value
                val = "/* Couldn't populate value */";
            }

            String varname_subvar = varname + "_pubArg" + objCount;
            objCount++;
            String argScriptlet = generateScriptletForObject(val, varname_subvar);
            scriptlet.append(argScriptlet);
            scriptlet.append(varname);
            scriptlet.append(".");
            scriptlet.append(f.getName());
            scriptlet.append(" = ");
            scriptlet.append(varname_subvar);
            scriptlet.append(";\n");
        }

        return scriptlet.toString();
    }

    private String stringAsScriptlet(String value) {
        return "\"" + value + "\"";
    }

    private String unpackArray(String varname, Object arrayBean) {
        StringBuilder arrayScr = new StringBuilder();
        int arrLen = Array.getLength(arrayBean);

        for(int i = 0; i < arrLen; i++) {
            Object o = Array.get(arrayBean, i);
            arrayScr.append(generateScriptletForObject(o, varname + "_element" + i, Object.class));
        }

        arrayScr.append("Object[] ");
        arrayScr.append(varname);
        arrayScr.append(" = new Object[] { ");
        for(int i = 0; i < arrLen; i++) {
            arrayScr.append(varname);
            arrayScr.append("_element");
            arrayScr.append(i);
            if(i != arrLen - 1) {
                arrayScr.append(", ");
            }
        }
        arrayScr.append(" };");

        return arrayScr.toString();
    }

    private String unpackCollection(String varname, Collection c) {
        StringBuilder cScr = new StringBuilder();

        if(c instanceof Properties) {
            // Special-case: Properties
            return unpackProperties(varname, (Properties) c);
        }

        if(c instanceof Map) {
            // Special-case: Maps
            return unpackMapAsKeyValue(varname, (Map) c);
        }

        int i = 0;
        for(Iterator ii = c.iterator(); ii.hasNext(); ) {
            Object o = ii.next();
            cScr.append(generateScriptletForObject(o, varname + "_element" + i, Object.class));
            i++;
        }

        cScr.append(c.getClass().getCanonicalName());
        cScr.append(" ");
        cScr.append(varname);
        cScr.append(" = new ");
        cScr.append(c.getClass().getCanonicalName());
        cScr.append("();\n");

        i = 0;
        for(Iterator ii = c.iterator(); ii.hasNext(); ) {
            cScr.append(varname);
            cScr.append(".add(");
            cScr.append(varname);
            cScr.append("_element");
            cScr.append(i);
            cScr.append(");\n");
            i++;
        }

        return cScr.toString();
    }

    private String unpackMapAsKeyValue(String varname, Map m) {
        StringBuilder mapScr = new StringBuilder();

        mapScr.append(m.getClass().getCanonicalName());
        mapScr.append(" ");
        mapScr.append(varname);
        mapScr.append(" = new ");
        mapScr.append(m.getClass().getCanonicalName());
        mapScr.append("();\n");
        

        int i = 0;
        for(Object key : m.keySet()) {
            Object val = m.get(key);

            if(!(key instanceof String)) {
                mapScr.append(generateScriptletForObject(key, varname + "_key" + i));
            }
            if(!(val instanceof String)) {
                mapScr.append(generateScriptletForObject(val, varname + "_val" + i));
            }

            mapScr.append(varname);
            mapScr.append(".put(");
            if(key instanceof String) {
                mapScr.append(stringAsScriptlet((String) key));
            }
            else {
                mapScr.append(varname);
                mapScr.append("_key");
                mapScr.append(i);
            }

            mapScr.append(", ");

            if(val instanceof String) {
                mapScr.append(stringAsScriptlet((String) val));
            }
            else {
                mapScr.append(varname);
                mapScr.append("_val");
                mapScr.append(i);
            }
            mapScr.append(");\n");

            i++;
        }

        return mapScr.toString();
    }

    private String unpackProperties(String varname, Properties p) {
        StringBuilder pScr = new StringBuilder();

        pScr.append("java.util.Properties ");
        pScr.append(varname);
        pScr.append(" = new Properties();\n");

        for(String name : p.stringPropertyNames()) {
            pScr.append(varname);
            pScr.append(".setProperty(");
            pScr.append(stringAsScriptlet(name));
            pScr.append(", ");
            pScr.append(stringAsScriptlet(p.getProperty(name)));
            pScr.append(");\n");
        }

        return pScr.toString();
    }
}
