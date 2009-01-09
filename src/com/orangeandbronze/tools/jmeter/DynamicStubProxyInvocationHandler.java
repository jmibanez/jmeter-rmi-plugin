package com.orangeandbronze.tools.jmeter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import org.apache.log.Logger;
//import org.apache.log4j.Logger;
import java.lang.reflect.Method;
import java.io.Serializable;
import org.apache.jorphan.logging.LoggingManager;
import java.rmi.RemoteException;

public class DynamicStubProxyInvocationHandler
    implements InvocationHandler, Serializable
{

    private static final long serialVersionUID = -30090000L;

    private Object stubInstance;
    private MethodRecorder recorder;

    private static Logger log = LoggingManager.getLoggerForClass(); // Logger.getLogger(DynamicStubProxyInvocationHandler.class);

    public DynamicStubProxyInvocationHandler(Object stubInstance, MethodRecorder r) {
        this.stubInstance = stubInstance;
        this.recorder = r;
    }

    private void recordCall(MethodCallRecord r) {
        try {
            recorder.recordCall(r);
        }
        catch(Exception suppress) {
            log.warn("Suppressed exception when recording call:", suppress);
            suppress.printStackTrace();
        }
    }

    public Object invoke(Object instance, Method m, Object[] args) throws Throwable {
        log.info("Calling method " + m.getName());
        MethodCallRecord r = new MethodCallRecord(m, args);
        log.info("Record created");
        try {
            Object returnValue = m.invoke(stubInstance, args);
            r.returned(returnValue);
            return returnValue;
        }
        catch(InvocationTargetException invokEx) {
            log.debug("Invocation target exception");
            Throwable cause = invokEx.getCause();
            log.debug("Root cause: ", cause);
            r.thrown(cause);
            throw cause;
        }
        catch(IllegalAccessException accessEx) {
            log.warn("Illegal access exception during proxy method invocation", accessEx);
            throw new RuntimeException(accessEx);
        }
        finally {
            recordCall(r);
        }
    }
}
