package com.orangeandbronze.tools.jmeter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import org.apache.log4j.Logger;
import java.lang.reflect.Method;
import java.io.Serializable;

public class DynamicStubProxyInvocationHandler
    implements InvocationHandler, Serializable
{
    private Object stubInstance;
    private MethodRecorder recorder;

    private static Logger log = Logger.getLogger(DynamicStubProxyInvocationHandler.class);

    public DynamicStubProxyInvocationHandler(Object stubInstance, MethodRecorder r) {
        this.stubInstance = stubInstance;
        this.recorder = r;
    }

    private void recordCall(MethodCallRecord r) {
        recorder.recordCall(r);
    }

    public Object invoke(Object instance, Method m, Object[] args) {
        try {
            Object returnValue = m.invoke(stubInstance, args);
            recordCall(new MethodCallRecord(m, args, returnValue));
            return returnValue;
        }
        catch(InvocationTargetException invokEx) {
            throw new RuntimeException(invokEx);
        }
        catch(IllegalAccessException accessEx) {
            throw new RuntimeException(accessEx);
        }
    }
}
