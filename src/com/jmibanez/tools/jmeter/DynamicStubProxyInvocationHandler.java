package com.jmibanez.tools.jmeter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jmeter.util.JMeterUtils;


public class DynamicStubProxyInvocationHandler
    implements InvocationHandler,
               MethodInterceptor,
               Serializable
{
    private static final long serialVersionUID = -30090000L;

    private int callIndex = 1;

    private InstanceRegistry instanceRegistry;
    private String instanceName;
    private Object stubInstance;
    private MethodRecorder recorder;
    ProxyObjectGraph graphVisitor;

    private static Log log = LogFactory.getLog(DynamicStubProxyInvocationHandler.class);


    public DynamicStubProxyInvocationHandler(InstanceRegistry instanceRegistry,
                                             Object stubInstance, String instanceName,
                                             MethodRecorder r) {
        this.instanceRegistry = instanceRegistry;
        this.stubInstance = stubInstance;
        this.instanceName = instanceName;
        this.recorder = r;
        this.graphVisitor = new ProxyObjectGraph(instanceRegistry,
                                                 recorder);
    }

    public Remote buildStubProxy(boolean isRoot)
        throws IllegalAccessException,
               InstantiationException,
               InvocationTargetException,
               NoSuchMethodException {
        Class stub = stubInstance.getClass();
        if(stub == null) {
            throw new RuntimeException("Couldn't find stub class");
        }

        log.debug("Stub class: " + stub.getName());
        Class[] stubInterfaces = stub.getInterfaces();

        if (isRoot) {
            Class<?> stubProxyClass = Proxy.getProxyClass(getClass().getClassLoader(),
                                                          stubInterfaces);
            Constructor<?> spCons =
                stubProxyClass.getConstructor(new Class[] { InvocationHandler.class });
            return (Remote) spCons.newInstance(new Object[] { this });
        }
        else {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(UnicastRemoteObject.class);
            enhancer.setInterfaces(stubInterfaces);
            enhancer.setCallback(this);
            return (Remote) enhancer.create();
        }
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

    public Object intercept(Object instance, Method m, Object[] args,
                            MethodProxy methodProxy)
        throws Throwable {
        // Ensure we don't record Object methods such as toString and hashCode
        if ("hashCode".equals(m.getName())
            || "toString".equals(m.getName())) {
            return methodProxy.invokeSuper(instance, args);
        }
        return this.recordMethodCall(instance, m, args, methodProxy);
    }

    public Object invoke(Object instance, Method m, Object[] args)
        throws Throwable {
        return this.recordMethodCall(instance, m, args, null);
    }

    private Object recordMethodCall(Object instance, Method m, Object[] args,
                                    MethodProxy methodProxy)
        throws Throwable {
        log.debug("Calling method " + m.getName());
        MethodCallRecord r = new MethodCallRecord(callIndex++, instanceName, m,
                                                  args);
        log.debug("Record created");

        // Classes might suddenly change state under us when we pack
        // args; recreate them from scratch
        args = r.recreateArguments();
        try {
            Object returnValue = null;
            if (methodProxy != null) {
                // We bypass the method proxy, as we need to call the Java RMI
                // stub directly, not UnicastRemoteObject (which won't have
                // the interface methods anyway)
                returnValue = methodProxy.invoke(stubInstance, args);
            }
            else {
                returnValue = m.invoke(stubInstance, args);
            }
            r.returned(returnValue);

            returnValue = graphVisitor.replaceRemotes(returnValue, r);
            r.setRemotePathsInReturn(graphVisitor.getAndClearRemoteInstanceHandles());
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
