package com.orangeandbronze.tools.jmeter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import org.apache.log.Logger;
import java.lang.reflect.Method;
import java.io.Serializable;
import java.rmi.RemoteException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jmeter.util.JMeterUtils;

public class DynamicStubProxyInvocationHandler
    implements InvocationHandler, Serializable
{

    private static final long serialVersionUID = -30090000L;

    private Object stubInstance;
    private MethodRecorder recorder;

    private static Log log = LogFactory.getLog(DynamicStubProxyInvocationHandler.class);

    public DynamicStubProxyInvocationHandler(Object stubInstance, MethodRecorder r) {
        this.stubInstance = stubInstance;
        this.recorder = r;
    }

    private void recordCall(MethodCallRecord r) {
        try {
            recorder.recordCall(r);
        }
        catch(Exception suppress) {
            log.error("Suppressed exception when recording call:", suppress);
            JMeterUtils.reportErrorToUser("Suppressed exception when recording call:" + suppress.getMessage());
        }
    }

    public Object invoke(Object instance, Method m, Object[] args) throws Throwable {
        log.info("Calling method " + m.getName());
        MethodCallRecord r = new MethodCallRecord(m, args);
        log.info("Record created");

        // Classes might suddenly change state under us when we pack
        // args; recreate them from scratch
        args = r.recreateArguments();
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
        catch(Exception other) {
            log.warn("Internal exception: ", other);
            JMeterUtils.reportErrorToUser("Proxy Exception:" + other.getMessage());
            throw new RuntimeException(other);
        }
        finally {
            recordCall(r);
        }
    }
}
