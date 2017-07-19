package com.jmibanez.tools.jmeter.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;

import static com.jmibanez.tools.jmeter.RMIRemoteObjectConfig.OBJENESIS_FACTORY;

public class ReflectionUtil {

    public static List<Field> getFieldsUpTo(Class<?> startClass,
                                            Class<?> exclusiveParent) {

        List<Field> currentClassFields =
            new ArrayList<>(Arrays.asList(startClass.getDeclaredFields()));
        Class<?> parentClass = startClass.getSuperclass();

        if (parentClass != null &&
            (exclusiveParent == null || !(parentClass.equals(exclusiveParent)))) {
            List<Field> parentClassFields =
                getFieldsUpTo(parentClass, exclusiveParent);
            currentClassFields.addAll(parentClassFields);
        }

        return currentClassFields;
    }

    public static <T> T newInstance(Class<T> clazz) {
        JMeterContext jmctx = JMeterContextService.getContext();
        Objenesis objenesis = null;
        if (jmctx.getVariables() != null) {
            objenesis = (Objenesis) jmctx.getVariables().getObject(OBJENESIS_FACTORY);
        }
        else {
            objenesis = new ObjenesisStd();
        }
        ObjectInstantiator factory = objenesis.getInstantiatorOf(clazz);
        return (T) factory.newInstance();
    }
}
